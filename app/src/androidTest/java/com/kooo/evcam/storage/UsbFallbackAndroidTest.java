package com.kooo.evcam.storage;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.StorageHelper;
import com.kooo.evcam.settings.DeveloperMode;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Map;
import static org.junit.Assert.*;

/** Disposable emulator has no USB; exercise actual AppConfig and destination gates without recording. */
@RunWith(AndroidJUnit4.class)
public class UsbFallbackAndroidTest {
    private Context context;
    private AppConfig config;
    private SharedPreferences preferences;
    private Map<String, ?> saved;
    private boolean developer;
    private static final String[] KEYS = {"storage_location", "custom_sd_card_path", "usb_internal_fallback",
            "relay_write_enabled", "auto_start_recording", "drive_auto_start"};
    @Before public void prepare() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        config = new AppConfig(context);
        preferences = context.getSharedPreferences("app_config", Context.MODE_PRIVATE);
        saved = preferences.getAll(); developer = DeveloperMode.isUnlocked();
        DeveloperMode.setEnabled(context, true);
        preferences.edit().remove("usb_internal_fallback").commit();
        config.setStorageLocation(AppConfig.STORAGE_EXTERNAL_SD);
        config.setCustomSdCardPath(null); config.setRelayWriteEnabled(false);
        config.setAutoStartRecording(false); config.setDriveAutoStart(false);
        StorageHelper.clearCache();
        assertFalse("Run only on the disposable emulator without USB", StorageHelper.hasExternalSdCard(context));
    }
    @After public void restore() {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : KEYS) {
            Object value = saved.get(key);
            if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof String) editor.putString(key, (String) value);
            else editor.remove(key);
        }
        editor.commit(); DeveloperMode.setEnabled(context, developer); StorageHelper.clearCache();
    }
    @Test public void developerModeAloneNeverPermitsMissingUsbFallback() {
        assertTrue(DeveloperMode.isUnlocked());
        assertFalse(config.isUsbInternalFallbackEnabled());
        assertFalse(StorageHelper.isUsbInternalFallbackAllowed(context));
        assertFalse(StorageHelper.isRecordingStorageAvailable(context));
        assertFalse(StorageHelper.willRecordToInternal(context));
        assertNull(StorageHelper.getVideoDir(context));
        assertNull(StorageHelper.getPhotoDir(context));
    }
    @Test public void explicitFallbackIsGatedWithoutErasingTheSavedChoice() {
        config.setUsbInternalFallbackEnabled(true);
        assertTrue(StorageHelper.isUsbInternalFallbackAllowed(context));
        assertTrue(StorageHelper.isRecordingStorageAvailable(context));
        assertTrue(StorageHelper.willRecordToInternal(context));
        DeveloperMode.setEnabled(context, false);
        assertFalse(config.isUsbInternalFallbackEnabled());
        assertFalse(StorageHelper.isUsbInternalFallbackAllowed(context));
        assertFalse(StorageHelper.isRecordingStorageAvailable(context));
        assertNull(StorageHelper.getVideoDir(context));
        assertTrue(preferences.getBoolean("usb_internal_fallback", false));
        DeveloperMode.setEnabled(context, true);
        assertTrue(config.isUsbInternalFallbackEnabled());
        config.setUsbInternalFallbackEnabled(false);
        assertFalse(StorageHelper.isRecordingStorageAvailable(context));
    }
    @Test public void explicitInternalAndRelayChoicesRemainSeparate() {
        config.setStorageLocation(AppConfig.STORAGE_INTERNAL);
        assertFalse(config.isUsbInternalFallbackEnabled());
        assertTrue(StorageHelper.isRecordingStorageAvailable(context));
        assertTrue(StorageHelper.willRecordToInternal(context));
        config.setStorageLocation(AppConfig.STORAGE_EXTERNAL_SD);
        config.setRelayWriteEnabled(true);
        assertTrue(config.shouldUseRelayWrite());
        assertFalse(StorageHelper.isRecordingStorageAvailable(context));
        assertNull(StorageHelper.getVideoDir(context));
    }

    @Test public void manualUsbRequestShowsWaitingAndCanBeCancelledBeforeUsbArrives() throws Exception {
        for (String permission : new String[]{android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), permission);
        }
        preferences.edit().putBoolean("first_launch", false).putBoolean("language_chosen", true)
                .putBoolean("rail_side_chosen", true).commit();
        com.kooo.evcam.recording.RecordingIntent.current().reset();
        com.kooo.evcam.recording.RecordingCoordinator coordinator = com.kooo.evcam.recording.RecordingCoordinator.get(context);
        try (androidx.test.core.app.ActivityScenario<com.kooo.evcam.MainActivity> scenario =
                     androidx.test.core.app.ActivityScenario.launch(com.kooo.evcam.MainActivity.class)) {
            scenario.onActivity(activity -> activity.findViewById(com.kooo.evcam.R.id.btn_start_record).performClick());
            long until = android.os.SystemClock.uptimeMillis() + 8_000L;
            while (!coordinator.isWaitingForStorage() && android.os.SystemClock.uptimeMillis() < until)
                android.os.SystemClock.sleep(20);
            assertTrue("Manual start should wait for USB instead of offering internal recording", coordinator.isWaitingForStorage());
            assertFalse(coordinator.isRecording());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                android.widget.TextView title = activity.findViewById(com.kooo.evcam.R.id.record_title);
                assertEquals(activity.getString(com.kooo.evcam.R.string.record_waiting_usb), title.getText().toString());
            });
            try (android.os.ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeShellCommand("screencap -p /sdcard/Download/usb-wait-state.png");
                 java.io.InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
                while (input.read() != -1) {}
            }
            // Keep the normal record-button double-tap guard in this real UI interaction.
            android.os.SystemClock.sleep(1_100);
            scenario.onActivity(activity -> activity.findViewById(com.kooo.evcam.R.id.btn_start_record).performClick());
            assertFalse(coordinator.isWaiting());
            assertFalse(coordinator.isRecording());
            assertFalse(com.kooo.evcam.recording.RecordingIntent.current().recordingWanted());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    coordinator.stop(com.kooo.evcam.recording.RecordingStops.Reason.USER));
            com.kooo.evcam.recording.RecordingIntent.current().reset();
        }
    }
}
