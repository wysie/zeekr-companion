package com.kooo.evcam.recording;

import android.content.Context;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.*;
import com.kooo.evcam.profile.*;
import com.kooo.evcam.settings.DeveloperMode;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.util.*;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Isolated emulator regressions: independent recording compatibility and real Camera2/MP4 output. */
public class RecordingPreviewIsolationAndroidTest extends MainPageControlsAndroidTest {
    private Context target() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private void await(String message, BooleanSupplier condition, long timeout) {
        long end = SystemClock.elapsedRealtime() + timeout;
        while (!condition.getAsBoolean() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(50);
        assertTrue(message, condition.getAsBoolean());
    }
    private MultiCameraManager pipeline() { return CameraManagerHolder.getInstance().getCameraManager(); }
    private void awaitBuilt() { await("Disposable camera pipeline built", () -> pipeline() != null && pipeline().isBuilt(), 10000); }

    @Test public void previewAndOverlayChangesStayPendingWithoutBlockingRecordingSetup() {
        Context context = target(); AppConfig config = new AppConfig(context);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            awaitBuilt(); MultiCameraManager pipeline = pipeline();
            Map<String, ?> before = context.getSharedPreferences("profiles", 0).getAll();
            config.setPreviewSourceMode("surround_roads");
            CameraManagerHolder holder = CameraManagerHolder.getInstance();
            assertTrue(holder.hasPendingChannelChange(context)); assertFalse(holder.hasPendingRecordingChange(context));
            config.setRearViewEnabled(true); config.setSidePopupEnabled(true);
            assertFalse(holder.hasPendingRecordingChange(context)); assertSame(pipeline, pipeline());
            config.setSourceCameraId(CameraProfile.ROLE_MANUAL, "99");
            assertFalse(holder.hasPendingRecordingChange(context));
            config.setSourceCameraId(CameraProfile.ROLE_COMPOSITE, "99");
            assertTrue(holder.hasPendingRecordingChange(context));
            config.setSourceCameraId(CameraProfile.ROLE_COMPOSITE, null);
            assertFalse(holder.hasPendingRecordingChange(context));
            ProfileStore store = new ProfileStore(context); Profile profile = store.current();
            profile.camera(CameraProfile.ROLE_ROAD_FRONT).enabled = true; store.save(profile);
            assertTrue(holder.hasPendingRecordingChange(context));
            profile.camera(CameraProfile.ROLE_ROAD_FRONT).enabled = false; store.save(profile);
            assertFalse(holder.hasPendingRecordingChange(context));
            assertEquals(before, context.getSharedPreferences("profiles", 0).getAll());
        }
    }

    private Set<java.io.File> videos(java.io.File dir) {
        Set<java.io.File> files = new HashSet<>(); if (dir == null) return files;
        java.io.File[] children = dir.listFiles(); if (children == null) return files;
        for (java.io.File file : children) {
            if (file.isDirectory()) files.addAll(videos(file));
            else if (file.getName().endsWith(".mp4")) files.add(file);
        }
        return files;
    }

    @Test public void manualAndDriverRequestsWriteVideoWithAPendingPreviewChoice() throws Exception {
        Context context = target(); AppConfig config = new AppConfig(context);
        boolean developer = DeveloperMode.isUnlocked();
        Set<java.io.File> before = new HashSet<>(); java.io.File[] directory = {null};
        RecordingCoordinator recorder = RecordingCoordinator.get(context);
        try {
            DeveloperMode.setEnabled(context, true); config.setStorageLocation(AppConfig.STORAGE_INTERNAL);
            config.setDriveAutoStart(true); config.setAutoStartRecording(false);
            ProfileStore store = new ProfileStore(context); Profile profile = store.current();
            CameraProfile camera = profile.camera(CameraProfile.ROLE_COMPOSITE);
            camera.preview.resolution = "1280x720"; camera.record.resolution = "1280x720";
            camera.record.fps = "10"; camera.record.grid = false; store.save(profile);
            directory[0] = StorageHelper.getVideoDir(context); assertNotNull(directory[0]); before.addAll(videos(directory[0]));
            assertEquals("New recordings use the Companion public folder", "ZeekrCompanion", directory[0].getParentFile().getName());
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                awaitBuilt(); MultiCameraManager pipeline = pipeline();
                config.setPreviewSourceMode("roads");
                assertTrue(CameraManagerHolder.getInstance().hasPendingChannelChange(context));
                assertFalse(CameraManagerHolder.getInstance().hasPendingRecordingChange(context));
                for (RecordingCoordinator.Why why : new RecordingCoordinator.Why[]{RecordingCoordinator.Why.USER, RecordingCoordinator.Why.DRIVE}) {
                    main(() -> { RecordingIntent.current().reset(); recorder.request(why); });
                    await("Recording writes real data after " + why, () -> pipeline.hasWrittenFirstData(), 25000);
                    assertSame(pipeline, pipeline());
                    SystemClock.sleep(1200);
                    main(() -> recorder.stop(RecordingStops.Reason.USER));
                    await("Recorder settles after stop", () -> !recorder.isRecordingOrResuming(), 10000);
                    SystemClock.sleep(800);
                }
            }
            Set<java.io.File> written = videos(directory[0]); written.removeAll(before);
            assertTrue("Both starts produced files", written.size() >= 2);
            for (java.io.File file : written) {
                android.media.MediaExtractor extractor = new android.media.MediaExtractor();
                try {
                    extractor.setDataSource(file.getAbsolutePath()); boolean video = false;
                    for (int i = 0; i < extractor.getTrackCount(); i++) {
                        if (extractor.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME).startsWith("video/")) {
                            extractor.selectTrack(i); video = extractor.getSampleTime() >= 0; break;
                        }
                    }
                    assertTrue("Finalized video has a playable sample", video);
                } finally { extractor.release(); }
            }
        } finally {
            main(() -> { recorder.cancelPending("test cleanup"); recorder.stop(RecordingStops.Reason.USER); });
            SystemClock.sleep(1500);
            Set<java.io.File> generated = videos(directory[0]); generated.removeAll(before);
            for (java.io.File file : generated) assertTrue("Delete only this regression's generated footage", file.delete());
            DeveloperMode.setEnabled(context, developer);
        }
    }
}
