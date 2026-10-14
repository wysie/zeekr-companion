package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.R;
import com.kooo.evcam.StorageHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** One fresh preview snapshot per save; compression and file writes stay off the UI thread. */
public final class BirdEyePhoto {
    private static final AtomicBoolean SAVING = new AtomicBoolean();
    private BirdEyePhoto() {}

    public static void save(Context context, Bitmap image, String previewState) {
        Context app = context.getApplicationContext();
        if (image == null) {
            Toast.makeText(app, R.string.msg_photo_no_picture, Toast.LENGTH_LONG).show();
            return;
        }
        final String calibration;
        try { calibration = new BirdEyeConfig(app).read().describe(); }
        catch (Exception error) {
            image.recycle();
            AppLog.e("BirdEyePhoto", "Cannot read photo calibration", error);
            Toast.makeText(app, R.string.msg_photo_not_saved, Toast.LENGTH_LONG).show();
            return;
        }
        if (!SAVING.compareAndSet(false, true)) {
            image.recycle();
            Toast.makeText(app, R.string.msg_photo_waiting, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            File photo = null, note = null;
            boolean saved = false;
            try {
                File root = StorageHelper.getPhotoDir(app);
                if (root == null) throw new IllegalStateException("Photo storage unavailable");
                File dir = new File(root, "360");
                if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("Cannot create photo folder");
                String name = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date())
                        + "_" + Long.toHexString(System.nanoTime()) + "_diy360";
                File target = new File(dir, name + ".png");
                if (!target.createNewFile()) throw new IllegalStateException("Photo already exists");
                photo = target;
                try (FileOutputStream out = new FileOutputStream(photo)) {
                    if (!image.compress(Bitmap.CompressFormat.PNG, 100, out))
                        throw new IllegalStateException("Photo compression failed");
                    out.flush(); out.getFD().sync();
                }
                File notes = new File(dir, name + ".txt");
                if (!notes.createNewFile()) throw new IllegalStateException("Notes already exist");
                note = notes;
                try (FileOutputStream out = new FileOutputStream(note)) {
                    out.write(("Experimental DIY 360 preview; estimates, not factory calibration.\n"
                            + "App version: " + version(app) + "\n"
                            + "Preview: " + previewState + "\nCalibration: " + calibration + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                    out.flush(); out.getFD().sync();
                }
                saved = true;
                AppLog.i("BirdEyePhoto", "Saved 360 preview: " + photo.getAbsolutePath());
            } catch (Exception error) {
                AppLog.e("BirdEyePhoto", "Cannot save 360 preview", error);
                if (photo != null) photo.delete();
                if (note != null) note.delete();
            } finally {
                image.recycle(); SAVING.set(false);
            }
            final boolean ok = saved;
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(app,
                    ok ? R.string.bird_eye_photo_saved : R.string.msg_photo_not_saved,
                    Toast.LENGTH_LONG).show());
        }, "save-diy360").start();
    }

    private static String version(Context context) {
        try { return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
        catch (Exception error) { return "unknown"; }
    }
}
