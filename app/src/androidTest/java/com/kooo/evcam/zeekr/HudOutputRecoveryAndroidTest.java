package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.opengl.*;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.camera.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real HUD Presentation and production binding; camera declarations only, never opens a HAL. */
@RunWith(AndroidJUnit4.class)
public class HudOutputRecoveryAndroidTest {
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private static Field field(Class<?> type, String name) {
        try { Field f=type.getDeclaredField(name); f.setAccessible(true); return f; }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private static Object get(Object target, String name) {
        try { return field(target.getClass(),name).get(target); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private void await(String message, BooleanSupplier condition) {
        long end=SystemClock.uptimeMillis()+4000;
        while (!condition.getAsBoolean() && SystemClock.uptimeMillis()<end) SystemClock.sleep(20);
        assertTrue(message,condition.getAsBoolean());
    }
    private static final class Camera extends SingleCamera {
        int declarations;
        Camera(Context context, String id) { super(context,id,null); }
        @Override public Size getPreviewSize() { return new Size(256,1024); }
        @Override public Size getPreviewBufferSize() { return new Size(256,1024); }
        @Override public boolean declareSideHud(SurfaceTexture texture, RecordDropped dropped) {
            declarations++; return super.declareSideHud(texture,dropped);
        }
    }
    private static final class Manager extends MultiCameraManager {
        Camera camera;
        Manager(Context context) { super(context); camera=new Camera(context,"hud-recovery-test"); }
        @Override public SingleCamera getCamera(String key) { return CameraSlots.KEY_SURROUND.equals(key)?camera:null; }
    }
    private final class Fixture implements AutoCloseable {
        final Context context=new ContextThemeWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext(),R.style.Theme_Cam);
        final AppConfig config=new AppConfig(context);
        final ImageReader images=ImageReader.newInstance(1280,640,PixelFormat.RGBA_8888,3);
        final HandlerThread consumer=new HandlerThread("disposable-hud-recovery");
        final VirtualDisplay display;
        final String composite=StreamLayoutTable.compositeCameraId();
        final String gpuMode=config.getGpuRenderingMode();
        final AtomicReference<SideViewHudOutput> output=new AtomicReference<>();
        final boolean held=CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_HUD);
        Object previousManager, previousListener;
        Manager manager;
        Fixture() {
            consumer.start();
            images.setOnImageAvailableListener(reader->{try(android.media.Image image=reader.acquireLatestImage()) {}},new Handler(consumer.getLooper()));
            DisplayManager dm=(DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);
            display=dm.createVirtualDisplay("Disposable recovery HUD",1280,640,160,images.getSurface(),
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC|DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION|DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
            assertNotNull(display);
            main(()->{
                try {
                    previousManager=field(CameraManagerHolder.class,"cameraManager").get(CameraManagerHolder.getInstance());
                    previousListener=field(CameraNeeds.class,"listener").get(CameraNeeds.current());
                    manager=new Manager(context);
                    field(CameraManagerHolder.class,"cameraManager").set(CameraManagerHolder.getInstance(),manager);
                } catch(Exception e) { throw new AssertionError(e); }
                StreamLayoutTable.setCompositeCameraId("hud-recovery-test");
                output.set(new SideViewHudOutput(context,config,()->{}));
                output.get().update(LaneCycle.LEFT,false,true,0);
            });
            await("HUD texture available",()->panel()!=null && panel().getTextureView().isAvailable());
            main(()->output.get().update(SideViewDecision.NONE,false,true,0));
            main(()->output.get().update(LaneCycle.LEFT,false,true,0));
            await("Initial HUD declaration",()->boundCamera()==manager.camera);
        }
        SideViewHudPanel panel() { return (SideViewHudPanel)get(output.get(),"panel"); }
        Object boundCamera() { return get(output.get(),"boundCamera"); }
        SurfaceTexture texture() { return panel().getTextureView().getSurfaceTexture(); }
        @Override public void close() {
            main(()->{
                output.get().dispose(); manager.release();
                try {
                    field(CameraManagerHolder.class,"cameraManager").set(CameraManagerHolder.getInstance(),previousManager);
                    field(CameraNeeds.class,"listener").set(CameraNeeds.current(),previousListener);
                } catch(Exception e) { throw new AssertionError(e); }
                if(held) CameraNeeds.current().claim(CameraNeeds.Holder.SIDE_HUD); else CameraNeeds.current().release(CameraNeeds.Holder.SIDE_HUD);
                StreamLayoutTable.setCompositeCameraId(composite);
                config.setGpuRenderingMode(gpuMode);
            });
            display.release(); images.close(); consumer.quitSafely();
        }
    }
    @Test public void lostDeclarationRecoversWhileTheSameIndicatorStaysOn() {
        try(Fixture f=new Fixture()) {
            SurfaceTexture texture=f.texture(); Camera camera=f.manager.camera;
            main(()->assertTrue(camera.withdrawSideHud(texture)));
            await("HUD must redeclare without an indicator off/on cycle",()->camera.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,texture)));
            assertSame(texture,f.texture()); assertTrue(f.panel().isShowing());
        }
    }
    @Test public void replacementPipelineRecoversWithoutWaitingForAnotherTurn() {
        try(Fixture f=new Fixture()) {
            Camera old=f.manager.camera; SurfaceTexture texture=f.texture();
            SingleCamera.RecordDropped oldFailure=old.dropListener(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,texture));
            main(()->{
                Manager next=new Manager(f.context);
                try { field(CameraManagerHolder.class,"cameraManager").set(CameraManagerHolder.getInstance(),next); }
                catch(Exception e) { throw new AssertionError(e); }
                f.manager.release(); f.manager=next;
            });
            await("HUD must follow the current pipeline",()->f.boundCamera()==f.manager.camera);
            assertFalse(old.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,texture)));
            assertTrue(f.manager.camera.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,texture)));
            assertNotEquals("Old pipeline frames must not reveal the replacement",SideViewHudPanel.COVER_NONE,f.panel().cover());
            main(()->oldFailure.onDropped(SingleCamera.Drop.CONFIG_FAILED));
            assertNotNull("A late old failure must not detach the replacement",f.panel());
            assertSame(f.manager.camera,f.boundCamera());
        }
    }
    @Test public void standbyHudRepairsItsBindingBeforeTheNextIndicator() {
        try(Fixture f=new Fixture()) {
            SurfaceTexture texture=f.texture(); Camera camera=f.manager.camera;
            main(()->{
                f.output.get().update(SideViewDecision.NONE,false,true,0);
                assertFalse(f.panel().isShowing());
                camera.withdrawSideHud(texture);
            });
            await("Warm-up HUD must also recover",()->camera.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,texture)));
            main(()->f.output.get().update(LaneCycle.LEFT,false,true,0));
            assertSame(texture,f.texture()); assertTrue(f.panel().isShowing());
        }
    }
    @Test public void missingHudClaimRecoversWithoutTouchingPopupOwnership() {
        try(Fixture f=new Fixture()) {
            boolean popup=CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_POPUP);
            main(()->{CameraNeeds.current().claim(CameraNeeds.Holder.SIDE_POPUP); CameraNeeds.current().release(CameraNeeds.Holder.SIDE_HUD);});
            try {
                await("HUD ownership restored",()->CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_HUD));
                assertTrue(CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_POPUP));
            } finally { main(()->{if(!popup) CameraNeeds.current().release(CameraNeeds.Holder.SIDE_POPUP);}); }
        }
    }

    /** EGL timestamps exercise the real TextureView callback and GPU freshness path. */
    private static final class Producer implements AutoCloseable {
        final Surface input;
        final EGLDisplay display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        final EGLContext context;
        final EGLSurface surface;
        Producer(Fixture f, boolean gpu) {
            input=PreviewDewarp.optionalViewSurfaceFor(f.context,f.texture(),new Size(256,1024),gpu);
            assertTrue(EGL14.eglInitialize(display,new int[2],0,new int[2],0));
            EGLConfig[] configs=new EGLConfig[1];
            assertTrue(EGL14.eglChooseConfig(display,new int[]{EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,
                    EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_NONE},0,configs,0,1,new int[1],0));
            context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
            surface=EGL14.eglCreateWindowSurface(display,configs[0],input,new int[]{EGL14.EGL_NONE},0);
            assertTrue(EGL14.eglMakeCurrent(display,surface,surface,context));
        }
        void frames() {
            for(int i=0;i<12;i++) {
                GLES20.glClearColor(0,0,1,1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                assertTrue(EGLExt.eglPresentationTimeANDROID(display,surface,System.nanoTime()));
                assertTrue(EGL14.eglSwapBuffers(display,surface)); SystemClock.sleep(40);
            }
        }
        @Override public void close() {
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(display,surface); EGL14.eglDestroyContext(display,context); EGL14.eglReleaseThread(); input.release();
        }
    }
    @Test public void realHudCoverRecoversFromAFrameGapWithGpuAllAndCompatibility() {
        for(boolean gpu:new boolean[]{false,true}) try(Fixture f=new Fixture()) {
            f.config.setGpuRenderingMode(gpu?"all":"off");
            main(()->f.panel().applyConfig());
            try(Producer p=new Producer(f,gpu)) {
                p.frames();
                await("HUD reveals actual frames",()->f.panel().cover()==SideViewHudPanel.COVER_NONE);
                if(gpu) assertEquals("GPU",f.panel().gpuState());
                SystemClock.sleep(SideViewPopupService.STALE_AFTER_MS+250);
                await("A stalled source must remain covered",()->f.panel().cover()==SideViewHudPanel.COVER_NO_PICTURE);
                SurfaceTexture texture=f.texture();
                main(()->f.manager.camera.withdrawSideHud(texture));
                await("Restore HUD binding while indicator remains on",()->f.manager.camera.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,texture)));
                assertEquals("Rebinding alone must not invent a live frame",SideViewHudPanel.COVER_NO_PICTURE,f.panel().cover());
                p.frames();
                await("Resume picture without changing the indicator",()->f.panel().cover()==SideViewHudPanel.COVER_NONE);
                assertTrue(f.panel().isShowing()); assertSame(texture,f.texture());
            }
        }
    }
    @Test public void recoveryDoesNotRepeatedlyRedeclareHealthyOrRejectedOutputs() {
        try(Fixture f=new Fixture()) {
            Camera camera=f.manager.camera;
            int initial=camera.declarations;
            SystemClock.sleep(1200);
            assertEquals("Healthy binding must be left alone",initial,camera.declarations);
            main(()->{
                SingleCamera.RecordDropped dropped=camera.dropListener(new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,f.texture()));
                assertNotNull(dropped); dropped.onDropped(SingleCamera.Drop.CONFIG_FAILED);
                assertNull(f.panel()); assertFalse(f.output.get().available());
            });
            SystemClock.sleep(1200);
            assertEquals("Refusal cooldown must not be bypassed",initial,camera.declarations);
            assertFalse(CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_HUD));
        }
    }
    @Test public void detachingCancelsRecoveryAndPreservesOtherClaims() {
        try(Fixture f=new Fixture()) {
            Camera camera=f.manager.camera; int initial=camera.declarations;
            boolean popup=CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_POPUP);
            main(()->{CameraNeeds.current().claim(CameraNeeds.Holder.SIDE_POPUP); f.output.get().detach();});
            try {
                SystemClock.sleep(1200);
                assertNull(f.panel()); assertEquals(initial,camera.declarations);
                assertTrue(CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_POPUP));
                assertFalse(CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_HUD));
            } finally {main(()->{if(!popup) CameraNeeds.current().release(CameraNeeds.Holder.SIDE_POPUP);});}
        }
    }
}
