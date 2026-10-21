package com.kooo.evcam.settings;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.camera.CameraCapabilities;
import com.kooo.evcam.profile.ProfileSizes;
import com.kooo.evcam.profile.RecordingSources;
import java.util.LinkedHashSet;
import java.util.Set;

/** Settings never enter the vendor camera service from the UI thread. */
final class CameraSettingsFacts {
    private static final java.util.concurrent.ExecutorService WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(
            task -> { Thread thread = new Thread(task, "CameraSettingsFacts"); thread.setDaemon(true); return thread; });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private CameraSettingsFacts() {}

    static void load(Context context, Runnable ready) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            try {
                CameraManager manager = (CameraManager) app.getSystemService(Context.CAMERA_SERVICE);
                if (manager != null) {
                    Set<String> ids = new LinkedHashSet<>();
                    try {
                        String[] listed = manager.getCameraIdList();
                        CameraCapabilities.recordIds(listed);
                        java.util.Collections.addAll(ids, listed);
                    } catch (Exception error) { AppLog.w("CameraSettings", "Camera listing unavailable: " + error); }
                    for (String role : RecordingSources.ALL_ROLES) {
                        String id = ProfileSizes.cameraIdFor(app, role);
                        if (id != null) ids.add(id);
                    }
                    for (String id : ids) if (CameraCapabilities.characteristics(id) == null) {
                        try { CameraCapabilities.record(id, manager.getCameraCharacteristics(id)); }
                        catch (Exception error) { AppLog.w("CameraSettings", "Camera facts unavailable for " + id + ": " + error); }
                    }
                }
            } finally { MAIN.post(ready); }
        });
    }
}
