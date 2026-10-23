package com.kooo.evcam.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.view.Surface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.zeekr.RearViewMirrorService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Actual SurfaceTexture ownership and output declarations; never opens a camera. */
@RunWith(AndroidJUnit4.class)
public class CameraOutputLifecycleAndroidTest {
    private Context context() {
        assumeTrue(Build.PRODUCT.contains("sdk") || Build.HARDWARE.contains("ranchu"));
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true); return m.invoke(target, args);
    }

    @Test public void closingDeviceStillOwnsTextureAfterSessionSnapshotClears() throws Exception {
        SingleCamera camera = new SingleCamera(context(), "unopened", null);
        SurfaceTexture texture = new SurfaceTexture(false);
        Surface surface = new Surface(texture);
        ChannelRules.Out out = new ChannelRules.Out(ChannelRules.Kind.MIRROR, texture);
        try {
            Map<ChannelRules.Out, Surface> carried = (Map<ChannelRules.Out, Surface>)
                    field(SingleCamera.class, "carried").get(camera);
            carried.put(out, surface);
            invoke(camera, "sessionGone", new Class<?>[0]);
            assertFalse(camera.sessionOutputs().carries(out));
            assertTrue("Closing camera retains ownership", camera.stillCarries(out));
            assertFalse(camera.stillCarries(new ChannelRules.Out(ChannelRules.Kind.MIRROR, new Object())));
            invoke(camera, "forgetCarried", new Class<?>[0]);
            assertFalse("Only device-close completion releases ownership", camera.stillCarries(out));
        } finally { surface.release(); texture.release(); }
    }

    @Test public void mirrorAndForkAuxiliaryCallbacksRemainIndependent() {
        SingleCamera camera = new SingleCamera(context(), "unopened", null);
        SurfaceTexture mirror = new SurfaceTexture(false), popup = new SurfaceTexture(false),
                hud = new SurfaceTexture(false), bird = new SurfaceTexture(false);
        int[] calls = new int[4];
        try {
            camera.declareMirror(mirror, why -> calls[0]++);
            camera.declareSidePopup(popup, why -> calls[1]++);
            camera.declareSideHud(hud, why -> calls[2]++);
            camera.declareBirdEye(bird, why -> calls[3]++);
            ChannelRules.Kind[] kinds = {ChannelRules.Kind.MIRROR, ChannelRules.Kind.SIDE_POPUP,
                    ChannelRules.Kind.SIDE_HUD, ChannelRules.Kind.BIRD_EYE};
            SurfaceTexture[] textures = {mirror, popup, hud, bird};
            for (int i = 0; i < kinds.length; i++) {
                SingleCamera.RecordDropped listener = camera.dropListener(new ChannelRules.Out(kinds[i], textures[i]));
                assertNotNull(listener); listener.onDropped(SingleCamera.Drop.CONFIG_FAILED);
                assertEquals(1, calls[i]);
            }
            camera.withdrawMirror(mirror);
            assertNull(camera.dropListener(new ChannelRules.Out(ChannelRules.Kind.MIRROR, mirror)));
            assertNotNull(camera.dropListener(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD, hud)));
        } finally { mirror.release(); popup.release(); hud.release(); bird.release(); }
    }

    @Test public void refusedMirrorStaysWithdrawnUntilResumeOrDifferentOwner() {
        Context context = context();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            MultiCameraManager manager = new MultiCameraManager(context);
            SingleCamera camera = new SingleCamera(context, "unopened", null);
            RearViewMirrorService service = new RearViewMirrorService();
            SurfaceTexture texture = new SurfaceTexture(false), replacement = new SurfaceTexture(false);
            try {
                field(RearViewMirrorService.class, "boundCamera").set(service, camera);
                field(RearViewMirrorService.class, "boundManager").set(service, manager);
                field(RearViewMirrorService.class, "boundTexture").set(service, texture);
                camera.declareMirror(texture, null);
                Class<?>[] dropped = {SingleCamera.class, SurfaceTexture.class, SingleCamera.Drop.class};
                Class<?>[] blocks = {MultiCameraManager.class, SurfaceTexture.class};
                // Late callbacks for a replaced output cannot refuse the current one.
                invoke(service, "onMirrorDropped", dropped, camera, replacement, SingleCamera.Drop.CONFIG_FAILED);
                assertFalse((boolean) invoke(service, "refusalBlocks", blocks, manager, texture));
                invoke(service, "onMirrorDropped", dropped, camera, texture, SingleCamera.Drop.CONFIG_FAILED);
                assertFalse(camera.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.MIRROR, texture)));
                for (int tick = 0; tick < 5; tick++)
                    assertTrue((boolean) invoke(service, "refusalBlocks", blocks, manager, texture));
                // Resume/screen-on/undock all enter rebindNow, even before a window is ready.
                invoke(service, "rebindNow", new Class<?>[]{boolean.class}, false);
                assertFalse((boolean) invoke(service, "refusalBlocks", blocks, manager, texture));
                field(RearViewMirrorService.class, "refusedManager").set(service, manager);
                field(RearViewMirrorService.class, "refusedTexture").set(service, texture);
                assertFalse((boolean) invoke(service, "refusalBlocks", blocks, manager, replacement));
            } catch (Exception e) { throw new AssertionError(e); }
            finally { manager.release(); texture.release(); replacement.release(); }
        });
    }
}
