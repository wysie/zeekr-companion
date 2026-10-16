package com.kooo.evcam.playback;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.StorageHelper;
import com.kooo.evcam.camera.ImageAdjustManager;
import com.kooo.evcam.settings.DeveloperMode;
import com.kooo.evcam.storage.FootageLocks;
import com.kooo.evcam.ui.ImageAdjustPanel;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MergedGalleryAndroidTest {
    private Context target() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private File photo(File dir, String name, int color) throws Exception {
        assertTrue(dir.isDirectory() || dir.mkdirs());
        File file = new File(dir, name);
        Bitmap bitmap = Bitmap.createBitmap(640, 400, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        canvas.drawColor(color);
        android.graphics.Paint paint = new android.graphics.Paint(); paint.setColor(Color.WHITE); paint.setTextSize(48);
        canvas.drawText("Gallery test image", 60, 200, paint);
        try (FileOutputStream out = new FileOutputStream(file)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); }
        bitmap.recycle(); return file;
    }
    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(150, 2000);
        android.os.SystemClock.sleep(250);
        String prefix = InstrumentationRegistry.getArguments().getString("capturePrefix", "");
        File output = new File(target().getExternalFilesDir(null), prefix + name + ".png");
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try (FileOutputStream out = new FileOutputStream(output)) { screenshot.compress(Bitmap.CompressFormat.PNG,100,out); }
        screenshot.recycle();
    }
    @Test public void sameTimestampAcrossFoldersStaysSeparateAndDeletionRechecksLocks() throws Exception {
        File root = new File(target().getCacheDir(), "gallery-regression");
        File first = photo(new File(root, "new"), "20261016_102030_surround.jpg", Color.BLUE);
        File second = photo(new File(root, "legacy"), first.getName(), Color.GREEN);
        AppConfig config = new AppConfig(target()); boolean old = config.isFootageLockEnabled();
        config.setFootageLockEnabled(true);
        try {
            java.util.List<DateSection<PhotoGroup>> scanned = PhotoScan.scan(Arrays.asList(first.getParentFile(), second.getParentFile()));
            assertEquals(1, scanned.size()); assertEquals(2, scanned.get(0).getItems().size());
            assertNotEquals(scanned.get(0).getItems().get(0).storageKey(), scanned.get(0).getItems().get(1).storageKey());
            CountDownLatch locked = new CountDownLatch(1);
            FootageLocks.setFiles(Collections.singletonList(first), true, ok -> { assertTrue(ok); locked.countDown(); });
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            // Lock was created after scanning: deletion must re-read it, and same basename elsewhere is independent.
            assertArrayEquals(new int[]{1,1,0}, GalleryDelete.files(target(), Arrays.asList(first,second,second)));
            assertTrue(first.exists()); assertFalse(second.exists());
            File metadata = new File(first.getParentFile(), FootageLocks.FILE_NAME);
            try (java.io.RandomAccessFile out = new java.io.RandomAccessFile(metadata,"rw")) { out.setLength(5*1024*1024); }
            assertArrayEquals(new int[]{0,1,0}, GalleryDelete.files(target(), Collections.singletonList(first)));
            assertTrue(first.exists());
        } finally { config.setFootageLockEnabled(old); first.delete(); second.delete(); new File(first.getParentFile(),FootageLocks.FILE_NAME).delete(); }
    }
    @Test public void galleryOpensChosenGroupAndFilmstripWithoutChangingViewerActions() throws Exception {
        Context context = target(); AppConfig config = new AppConfig(context);
        boolean developer = DeveloperMode.isUnlocked(); String storage = config.getStorageLocation();
        DeveloperMode.setEnabled(context,true); config.setStorageLocation(AppConfig.STORAGE_INTERNAL);
        try (android.os.ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("appops set " + context.getPackageName() + " MANAGE_EXTERNAL_STORAGE allow");
             java.io.InputStream in = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) { while (in.read() != -1) {} }
        File first = photo(StorageHelper.getPhotoDir(context), "20261016_102031_surround.jpg", 0xff1976a3);
        File second = photo(first.getParentFile(), "20261016_102032_surround.jpg", 0xff277557);
        try (ActivityScenario<GalleryActivity> scenario = ActivityScenario.launch(new Intent(context, GalleryActivity.class))) {
            long deadline = android.os.SystemClock.uptimeMillis()+5000;
            AtomicReference<GalleryAdapter<?>> adapter = new AtomicReference<>();
            do {
                scenario.onActivity(activity -> adapter.set((GalleryAdapter<?>) ((androidx.recyclerview.widget.RecyclerView)activity.findViewById(R.id.gallery_grid)).getAdapter()));
                if (adapter.get().getItemCount() >= 3) break;
                android.os.SystemClock.sleep(100);
            } while (android.os.SystemClock.uptimeMillis()<deadline);
            assertTrue(adapter.get().getItemCount() >= 3);
            capture("merged-gallery");
            scenario.onActivity(activity -> activity.findViewById(R.id.btn_select).performClick());
            capture("merged-gallery-selection");
            androidx.test.espresso.Espresso.pressBack();
            scenario.onActivity(activity -> assertEquals(View.VISIBLE,activity.findViewById(R.id.gallery_actions).getVisibility()));
        } finally {
            // Viewer receives the full directory-qualified identity.
            try (ActivityScenario<PhotoPlaybackActivity> viewer = ActivityScenario.launch(new Intent(context,PhotoPlaybackActivity.class)
                    .putExtra(PhotoPlaybackActivity.EXTRA_GROUP_PREFIX, first.getParent()+"/20261016_102031"))) {
                viewer.onActivity(activity -> {
                    androidx.recyclerview.widget.RecyclerView strip = activity.findViewById(R.id.media_filmstrip);
                    assertTrue(strip.getAdapter().getItemCount()>=2);
                    assertEquals(View.VISIBLE,activity.findViewById(R.id.controls_layout).getVisibility());
                    assertEquals(View.VISIBLE,activity.findViewById(R.id.btn_send_to_phone).getVisibility());
                    assertTrue(strip.getHeight() >= activity.getResources().getDimensionPixelSize(R.dimen.filmstrip_row_height));
                });
                capture("merged-photo-filmstrip");
            }
            first.delete();second.delete();config.setStorageLocation(storage);DeveloperMode.setEnabled(context,developer);
        }
    }
    @Test public void adjustmentPanelStaysInsideActivityAndDoesNotEnableOnOpen() throws Exception {
        Context context = target(); AppConfig config = new AppConfig(context);
        boolean developer=DeveloperMode.isUnlocked(), old=config.isImageAdjustEnabled();
        DeveloperMode.setEnabled(context,false);config.setImageAdjustEnabled(false);
        context.getSharedPreferences("app_config",Context.MODE_PRIVATE).edit().putBoolean("first_launch",false)
                .putBoolean("language_chosen",true).putBoolean("rail_side_chosen",true).commit();
        AtomicReference<ImageAdjustPanel> panel = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                ImageAdjustManager fake = new ImageAdjustManager(activity) {
                    @Override public boolean isExposureCompensationSupported() { return true; }
                    @Override public android.util.Range<Integer> getExposureRange() { return new android.util.Range<>(-3,3); }
                };
                FrameLayout root=activity.findViewById(android.R.id.content);
                panel.set(new ImageAdjustPanel(root,fake,activity::setImageAdjustEnabled));
                assertTrue(panel.get().isShowing());assertFalse(config.isImageAdjustEnabled());
            });
            capture("merged-picture-adjustment");
            scenario.onActivity(activity -> {
                FrameLayout root=activity.findViewById(android.R.id.content);
                View view=root.getChildAt(root.getChildCount()-1);
                android.widget.SeekBar seek=view.findViewById(R.id.seekbar);
                assertNotNull(seek);
                // The panel's explicit enabled switch is ordinary, independent of Developer mode.
                androidx.appcompat.widget.SwitchCompat toggle=findSwitch(view);
                toggle.setChecked(true);assertTrue(new AppConfig(context).isImageAdjustEnabled());
                panel.get().dismiss();assertFalse(panel.get().isShowing());
            });
        } finally {config.setImageAdjustEnabled(old);DeveloperMode.setEnabled(context,developer);}
    }
    private androidx.appcompat.widget.SwitchCompat findSwitch(View view) {
        if(view instanceof androidx.appcompat.widget.SwitchCompat)return (androidx.appcompat.widget.SwitchCompat)view;
        if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++) {
            androidx.appcompat.widget.SwitchCompat found=findSwitch(((android.view.ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
}
