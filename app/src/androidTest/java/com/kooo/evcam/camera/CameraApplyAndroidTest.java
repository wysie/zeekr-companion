package com.kooo.evcam.camera;

import android.content.Context;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.profile.*;
import com.kooo.evcam.recording.*;
import com.kooo.evcam.settings.*;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.*;
import java.io.File;
import java.util.*;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Disposable emulator only. Real Camera2 frames and finalized MP4s, not a mock camera UI. */
public class CameraApplyAndroidTest extends MainPageControlsAndroidTest {
    private Context context;
    private AppConfig config;
    private RecordingCoordinator recorder;
    private boolean developer;
    private File directory;
    private Set<File> existing;
    private void main(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }
    private void await(String label, BooleanSupplier condition) {
        long end = SystemClock.elapsedRealtime() + 35000;
        while (!condition.getAsBoolean() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(60);
        assertTrue(label + "\n" + holder().configurationSummary(context) + "\nApply busy=" + CameraChanges.busy()
                + ", recording=" + recorder.isRecording() + ", waiting=" + recorder.isWaiting(), condition.getAsBoolean());
    }
    private CameraManagerHolder holder() { return CameraManagerHolder.getInstance(); }
    private MultiCameraManager pipeline() { return holder().getCameraManager(); }
    private Set<File> videos(File dir) {
        Set<File> out = new HashSet<>(); File[] list = dir == null ? null : dir.listFiles();
        if (list != null) for (File f : list) { if (f.isDirectory()) out.addAll(videos(f)); else if (f.getName().endsWith(".mp4")) out.add(f); }
        return out;
    }
    @Before public void configureRealCamera() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext(); config = new AppConfig(context);
        developer = DeveloperMode.isUnlocked(); DeveloperMode.setEnabled(context,true);
        String id=((android.hardware.camera2.CameraManager)context.getSystemService(Context.CAMERA_SERVICE)).getCameraIdList()[0];
        config.setSourceCameraId(CameraProfile.ROLE_COMPOSITE,id);
        config.setSourceCameraId(CameraProfile.ROLE_ROAD_FRONT,id); config.setSourceCameraId(CameraProfile.ROLE_ROAD_REAR,id);
        config.setStorageLocation(AppConfig.STORAGE_INTERNAL); config.setRelayWriteEnabled(false);
        config.setPhotoViaJpegEnabled(true);
        ProfileStore store = new ProfileStore(context); Profile p = store.current();
        CameraProfile c = p.camera(CameraProfile.ROLE_COMPOSITE);
        c.preview.resolution="1280x720"; c.record.resolution="1280x720"; c.record.fps="10"; c.record.grid=false; store.save(p);
        recorder = RecordingCoordinator.get(context);
        main(() -> { recorder.stop(RecordingStops.Reason.USER); RecordingIntent.current().reset(); });
        directory=StorageHelper.getVideoDir(context); existing=videos(directory);
    }
    @After public void stopOwnRecording() {
        if (recorder == null) return;
        main(() -> { recorder.stop(RecordingStops.Reason.USER); recorder.cancelPending("apply test cleanup"); });
        SystemClock.sleep(1500);
        for (File f:videos(directory)) if (!existing.contains(f)) assertTrue(f.delete());
        RecordingIntent.current().reset();
        DeveloperMode.setEnabled(context,developer);
    }
    private void built() { await("Camera pipeline built", () -> pipeline()!=null && pipeline().isBuilt()); }
    private void stageNativeSize() {
        ProfileStore store=new ProfileStore(context); Profile p=store.current();
        p.camera(CameraProfile.ROLE_COMPOSITE).preview.resolution="640x480"; store.save(p);
        assertTrue("Native-size edit must be pending before testing Apply: " + holder().configurationSummary(context),
                holder().hasPendingChannelChange(context));
    }
    private void screenshot(String name) throws Exception {
        android.graphics.Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(context.getExternalFilesDir(null), name+".png"))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));
        } finally { bitmap.recycle(); }
    }
    @Test public void idleApplyPreservesSettingsAndKeepsLaterEditsPending() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            built(); MultiCameraManager old=pipeline();
            config.setPreviewSourceMode("roads");
            assertTrue(holder().hasPendingChannelChange(context));
            // A theme/UI recreation must not silently consume the saved choice.
            scenario.recreate(); scenario.onActivity(a -> {
                assertEquals("selected",holder().appliedPreviewMode(context));
                assertSame(old,pipeline());
                a.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,new SettingsShellFragment()).commitNow();
                a.findViewById(R.id.fragment_container).setVisibility(android.view.View.VISIBLE);
            });
            SystemClock.sleep(700);
            screenshot("camera-apply-settings-light");
            final int revision=holder().configurationRevision();
            scenario.onActivity(a -> {
                assertEquals(android.view.View.VISIBLE,a.findViewById(R.id.settings_pending_changes).getVisibility());
                a.findViewById(R.id.settings_apply_cameras).performClick();
                // Snapshot is already captured; this separate later edit must survive the first Apply.
                config.setPreviewSourceMode("surround_roads");
            });
            await("Apply completes", () -> !CameraChanges.busy() && holder().configurationRevision()>revision);
            assertFalse(CameraChanges.failed()); assertNotSame(old,pipeline());
            assertEquals("roads",holder().appliedPreviewMode(context));
            assertTrue(holder().hasPendingChannelChange(context)); assertFalse(recorder.isRecordingOrResuming());
            SystemClock.sleep(800);
            scenario.onActivity(a -> assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.fragment_container) instanceof SettingsShellFragment));
            // Same action is available after recreation, then clears both banners on success.
            onView(withId(R.id.settings_apply_cameras)).perform(click());
            await("Second Apply completes", () -> !CameraChanges.busy()&&!holder().hasPendingChannelChange(context));
            assertEquals("surround_roads",holder().appliedPreviewMode(context));
        }
    }
    @Test public void recordingApplyCancelThenResumeProducesPlayableClips() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            built(); MultiCameraManager old=pipeline();
            main(() -> recorder.request(RecordingCoordinator.Why.USER));
            await("First recording writes real data",old::hasWrittenFirstData); SystemClock.sleep(1400);
            stageNativeSize();
            scenario.onActivity(CameraChanges::request);
            onView(withText(android.R.string.cancel)).perform(click());
            assertSame(old,pipeline()); assertTrue(recorder.isRecording());
            scenario.onActivity(CameraChanges::request);
            onView(withText(R.string.camera_apply_action)).perform(click());
            scenario.onActivity(CameraChanges::request); // Repeated Apply cannot launch a second transaction.
            await("Apply finishes and resumes on the new pipeline", () -> !CameraChanges.busy() && pipeline()!=old && pipeline().hasWrittenFirstData());
            assertFalse(CameraChanges.failed()); assertFalse(holder().hasPendingChannelChange(context));
            assertEquals(RecordingIntent.Source.MANUAL,RecordingIntent.current().wantedSource());
            Set<File> after=videos(directory); after.removeAll(existing);
            assertTrue("Old and resumed recordings both have files",after.size()>=2);
            main(() -> recorder.stop(RecordingStops.Reason.USER));
            SystemClock.sleep(1800);
            for(File file:after) {
                android.media.MediaExtractor extractor=new android.media.MediaExtractor();
                try { extractor.setDataSource(file.getAbsolutePath()); boolean playable=false;
                    for(int i=0;i<extractor.getTrackCount();i++) if(extractor.getTrackFormat(i).getString(android.media.MediaFormat.KEY_MIME).startsWith("video/")) {
                        extractor.selectTrack(i); playable=extractor.getSampleTime()>=0; break;
                    }
                    assertTrue("Finished clip has video samples: "+file.getName(),playable);
                } finally { extractor.release(); }
            }
        }
    }
    @Test public void stopDuringApplyWinsAndDoesNotBecomeANewManualStart() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            built(); main(() -> recorder.request(RecordingCoordinator.Why.USER));
            await("Recording writes",()->pipeline().hasWrittenFirstData());
            final boolean[] stopped={false};
            main(() -> recorder.pauseForCameraChanges(ok -> stopped[0]=ok));
            await("Clip has finalized",()->stopped[0]);
            main(() -> { recorder.stop(RecordingStops.Reason.USER); recorder.finishCameraChanges(true); });
            SystemClock.sleep(800);
            assertFalse(recorder.isRecordingOrResuming()); assertFalse(RecordingIntent.current().recordingWanted());
        }
    }
    @Test public void automaticOriginAndDisabledSwitchGovernResumeAndFailureStaysStopped() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            built(); config.setDriveAutoStart(true);
            main(() -> recorder.request(RecordingCoordinator.Why.DRIVE));
            await("Automatic recording writes",()->pipeline().hasWrittenFirstData());
            final boolean[] stopped={false};
            main(() -> recorder.pauseForCameraChanges(ok -> stopped[0]=ok));
            await("Automatic clip finalized",()->stopped[0]);
            assertEquals(RecordingIntent.Source.AUTO_DRIVER,RecordingIntent.current().wantedSource());
            config.setDriveAutoStart(false);
            main(() -> recorder.finishCameraChanges(true));
            SystemClock.sleep(500);
            assertFalse("Disabled automatic origin must not become a manual request",recorder.isRecordingOrResuming());
            main(() -> recorder.request(RecordingCoordinator.Why.USER));
            await("Manual recording writes",()->pipeline().hasWrittenFirstData());
            stopped[0]=false;
            main(() -> recorder.pauseForCameraChanges(ok -> stopped[0]=ok));
            await("Manual clip finalized",()->stopped[0]);
            main(() -> recorder.finishCameraChanges(false));
            SystemClock.sleep(500);
            assertFalse("Failed Apply never silently resumes recording",recorder.isRecordingOrResuming());
            assertFalse(RecordingIntent.current().recordingWanted());
        }
    }
    @Test public void bannerFitsLightDarkAndLargeText() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            built(); config.setPreviewSourceMode("roads");
            for(int mode:new int[]{AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES}) {
                main(() -> AppCompatDelegate.setDefaultNightMode(mode)); SystemClock.sleep(900);
                scenario.onActivity(a -> {
                    TextView message=a.findViewById(R.id.main_preview_pending); message.setTextSize(26);
                    TextView action=a.findViewById(R.id.main_apply_cameras); action.setTextSize(26);
                });
                SystemClock.sleep(500);
                scenario.onActivity(a -> {
                    TextView message=a.findViewById(R.id.main_preview_pending),action=a.findViewById(R.id.main_apply_cameras);
                    assertTrue(message.isShown()); assertTrue(action.isShown());
                    assertTrue(message.getRight()<=action.getLeft());
                    assertTrue(action.getHeight()>=48*a.getResources().getDisplayMetrics().density);
                    assertTrue(action.getLayout().getLineCount()>0);
                });
                screenshot(mode==AppCompatDelegate.MODE_NIGHT_YES?"camera-apply-main-dark-large":"camera-apply-main-light-large");
            }
        }
    }

    @Test public void quitAndReopenDuringHeldApplyClearsBothGatesAndRejectsOldCompletion() throws Exception {
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch unblock = new java.util.concurrent.CountDownLatch(1);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            built();
            java.lang.reflect.Field setupField=CameraManagerHolder.class.getDeclaredField("setup");
            setupField.setAccessible(true);
            java.util.concurrent.Executor setup=(java.util.concurrent.Executor)setupField.get(holder());
            setup.execute(() -> {
                entered.countDown();
                try { unblock.await(20,java.util.concurrent.TimeUnit.SECONDS); }
                catch(InterruptedException error) { Thread.currentThread().interrupt(); }
            });
            assertTrue(entered.await(3,java.util.concurrent.TimeUnit.SECONDS));
            stageNativeSize();
            scenario.onActivity(CameraChanges::request);
            await("Apply entered replacement while setup is held",()->holder().configurationSummary(context).contains("applying=true"));
            assertTrue(CameraChanges.busy());
            assertTrue(recorder.busyForDeveloperProbe());
            // Same cancellation/exit primitives as exitApp, without killing the instrumentation process.
            main(() -> {
                UserExit.markExited(context);
                RecordingIntent.current().reset();
                recorder.stop(RecordingStops.Reason.USER);
                holder().release();
                assertFalse(CameraChanges.busy());
                assertFalse(recorder.busyForDeveloperProbe());
            });
            scenario.recreate(); // Manual reopening clears UserExit in this same process.
            assertFalse(UserExit.isExited(context));
            final int revision=holder().configurationRevision();
            unblock.countDown();
            built();
            SystemClock.sleep(300);
            assertEquals("Cancelled setup cannot publish its old successful Apply",revision,holder().configurationRevision());
            assertFalse("Quit must not resume recording",recorder.isRecordingOrResuming());
            assertFalse(CameraChanges.busy());
            main(() -> { RecordingIntent.current().noteUserStarted(); recorder.request(RecordingCoordinator.Why.USER); });
            await("Manual recording works in the reopened process",()->pipeline().hasWrittenFirstData());
        } finally { unblock.countDown(); UserExit.clear(context,"apply-test-cleanup"); }
    }

    @Test public void confirmationSurvivesOwnerRecreationAndImmediateCancelRetryWithoutLatching() throws Exception {
        java.lang.reflect.Field field=CameraChanges.class.getDeclaredField("confirmation");field.setAccessible(true);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            built();main(()->recorder.request(RecordingCoordinator.Why.USER));
            await("Recording writes before confirmation",()->pipeline().hasWrittenFirstData());
            stageNativeSize();scenario.onActivity(CameraChanges::request);
            onView(withText(R.string.camera_apply_title)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            scenario.recreate();
            scenario.onActivity(CameraChanges::request);
            onView(withText(R.string.camera_apply_title)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            android.app.Dialog[] retried={null};
            scenario.onActivity(activity->{
                try {
                    android.app.Dialog old=(android.app.Dialog)((java.lang.ref.WeakReference<?>)field.get(null)).get();
                    assertNotNull(old);old.dismiss();
                    // The dismiss listener is still queued on main. A new request must work now.
                    CameraChanges.request(activity);
                    retried[0]=(android.app.Dialog)((java.lang.ref.WeakReference<?>)field.get(null)).get();
                    assertNotNull(retried[0]);assertNotSame(old,retried[0]);assertTrue(retried[0].isShowing());
                } catch(IllegalAccessException error) { throw new AssertionError(error); }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity->{
                try {
                    assertSame("Old onDismiss cannot clear the newer confirmation",retried[0],((java.lang.ref.WeakReference<?>)field.get(null)).get());
                    CameraChanges.request(activity);
                    assertSame("Repeated request reuses the current confirmation",retried[0],((java.lang.ref.WeakReference<?>)field.get(null)).get());
                } catch(IllegalAccessException error) { throw new AssertionError(error); }
            });
            onView(withText(android.R.string.cancel)).perform(click());
            assertFalse(CameraChanges.busy());assertTrue("Cancelling does not stop recording",recorder.isRecording());
        }
    }
}
