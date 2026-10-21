package com.kooo.evcam.settings;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Looper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.camera.CameraCapabilities;
import com.kooo.evcam.profile.CameraProfile;
import com.kooo.evcam.profile.ProfileSizes;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CameraSettingsFactsAndroidTest {
    @Test public void blockedVendorLookupLeavesMainThreadResponsiveAndCompletesOnMain() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CountDownLatch entered = new CountDownLatch(1), unblock = new CountDownLatch(1), complete = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Context blockedVendor = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public Object getSystemService(String name) {
                if (!Context.CAMERA_SERVICE.equals(name)) return super.getSystemService(name);
                if (Looper.myLooper() == Looper.getMainLooper()) failure.set(new AssertionError("Camera lookup entered UI thread"));
                entered.countDown();
                try { if (!unblock.await(5, TimeUnit.SECONDS)) failure.set(new AssertionError("Test did not unblock vendor")); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); failure.set(error); }
                return null;
            }
        };
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> CameraSettingsFacts.load(blockedVendor, () -> {
                if (Looper.myLooper() != Looper.getMainLooper()) failure.set(new AssertionError("UI callback was not on main"));
                complete.countDown();
            }));
            assertTrue("Worker enters simulated slow vendor", entered.await(3, TimeUnit.SECONDS));
            CountDownLatch heartbeat = new CountDownLatch(1);
            new android.os.Handler(Looper.getMainLooper()).post(heartbeat::countDown);
            assertTrue("Camera UI and freshness timers can run while vendor is blocked", heartbeat.await(1, TimeUnit.SECONDS));
            assertEquals("Facts are not published before vendor returns", 1L, complete.getCount());
        } finally { unblock.countDown(); }
        assertTrue("Callback after worker completion", complete.await(3, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @Test public void non009UiSourceResolutionUsesSnapshotWithoutEnteringCameraService() {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppConfig config = new AppConfig(target);
        String profile = config.getVehicleProfileMode();
        String source = config.getSourceCameraId(CameraProfile.ROLE_CABIN_1);
        String[] previousIds = CameraCapabilities.advertisedIds();
        Context noCameraService = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public Object getSystemService(String name) {
                if (Context.CAMERA_SERVICE.equals(name)) throw new AssertionError("UI must not query camera service");
                return super.getSystemService(name);
            }
        };
        try {
            config.setVehicleProfileMode("7x");
            config.setSourceCameraId(CameraProfile.ROLE_CABIN_1, null);
            CameraCapabilities.recordIds(new String[]{"cached-cabin"});
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    assertEquals("cached-cabin", ProfileSizes.cameraIdFor(noCameraService, CameraProfile.ROLE_CABIN_1)));
            config.setSourceCameraId(CameraProfile.ROLE_CABIN_1, "manual-cabin");
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    assertEquals("manual-cabin", ProfileSizes.cameraIdFor(noCameraService, CameraProfile.ROLE_CABIN_1)));
        } finally {
            config.setVehicleProfileMode(profile);
            config.setSourceCameraId(CameraProfile.ROLE_CABIN_1, source);
            CameraCapabilities.recordIds(previousIds);
        }
    }
}
