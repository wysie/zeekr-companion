package com.kooo.evcam.storage;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.StorageHelper;
import com.kooo.evcam.settings.DeveloperMode;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/** Public manual-lock path on disposable emulator storage; no physical camera opened. */
public class ManualRecordingLockAndroidTest extends MainPageControlsAndroidTest {
    private Context app;
    private AppConfig config;
    private File directory;
    private final List<File> created = new ArrayList<>();
    private final Map<File, byte[]> lists = new HashMap<>();
    private boolean developer;
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private void await(BooleanSupplier ready, long timeout) {
        long end = SystemClock.elapsedRealtime() + timeout;
        while (!ready.getAsBoolean() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(40);
        assertTrue("Lock operation completed", ready.getAsBoolean());
    }
    @Before public void storage() throws Exception {
        app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        developer = DeveloperMode.isUnlocked(); DeveloperMode.setEnabled(app, true);
        config = new AppConfig(app); config.setStorageLocation(AppConfig.STORAGE_INTERNAL);
        config.setFootageLockEnabled(true); config.setFlashLockEnabled(false); config.setBrakeLockEnabled(false);
        directory = StorageHelper.getVideoDir(app); assertNotNull(directory); assertTrue(directory.isDirectory());
        for (String name : new String[]{FootageLocks.FILE_NAME, FootageLocks.AUTO_FILE_NAME}) {
            File file = new File(directory, name); lists.put(file, file.exists() ? Files.readAllBytes(file.toPath()) : null);
        }
        AutoLock.get().recordingStopped(); InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    @After public void restoreStorage() throws Exception {
        if (app == null) return;
        config.setFootageLockEnabled(false); AutoLock.get().recordingStopped();
        java.lang.reflect.Field field = AutoLock.class.getDeclaredField("worker"); field.setAccessible(true);
        Handler worker = (Handler) field.get(AutoLock.get()); worker.removeCallbacksAndMessages(null);
        CountDownLatch idle = new CountDownLatch(1); worker.post(idle::countDown); assertTrue(idle.await(5,TimeUnit.SECONDS));
        CountDownLatch written = new CountDownLatch(1);
        FootageLocks.set(directory, Collections.emptySet(), true, ok -> written.countDown());
        assertTrue(written.await(5,TimeUnit.SECONDS));
        for (File file : created) assertTrue(file.delete());
        for (Map.Entry<File,byte[]> entry : lists.entrySet()) {
            if (entry.getValue() == null) Files.deleteIfExists(entry.getKey().toPath());
            else Files.write(entry.getKey().toPath(), entry.getValue());
        }
        DeveloperMode.setEnabled(app, developer);
    }
    private String clip(long moment, String role) throws Exception {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date(moment));
        File file = new File(directory, stamp + "_" + role + ".mp4");
        assertTrue("Test never replaces existing footage", file.createNewFile()); created.add(file);
        Files.write(file.toPath(), new byte[]{1,2,3}); return file.getName();
    }
    private boolean locked(Collection<String> names) {
        Set<String> value = FootageLocks.read(directory); return value != null && value.containsAll(names);
    }
    @Test public void manualLockHonorsGatesAndProtectsEverySourceWithoutAutomaticSignals() throws Exception {
        long moment = System.currentTimeMillis();
        Set<String> names = new HashSet<>();
        for (String role : new String[]{"surround","roadfront","roadrear","cabinfront"}) names.add(clip(moment-1000,role));
        AutoLock.get().lockNow(app); SystemClock.sleep(250); assertFalse(locked(names));
        config.setFootageLockEnabled(false); AutoLock.get().recordingStarted(app,null);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        AutoLock.get().lockNow(app); SystemClock.sleep(250); assertFalse(locked(names));
        config.setFootageLockEnabled(true); AutoLock.get().lockNow(app);
        await(() -> locked(names), 5000);
        assertFalse(config.isFlashLockEnabled()); assertFalse(config.isBrakeLockEnabled());
        assertTrue(Collections.disjoint(FootageLocks.readAuto(directory),names));
    }
    @Test public void manualLockPromotesBrakeLocksAndSecondPassStillRunsAfterStop() throws Exception {
        long moment = System.currentTimeMillis(); String first = clip(moment-1000,"surround");
        CountDownLatch automatic = new CountDownLatch(1);
        FootageLocks.lockAuto(directory,Collections.singleton(first),Collections.singleton(first),Long.MAX_VALUE,ok -> automatic.countDown());
        assertTrue(automatic.await(5,TimeUnit.SECONDS)); assertTrue(FootageLocks.readAuto(directory).contains(first));
        AutoLock.get().recordingStarted(app,null); InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        AutoLock.get().lockNow(app);
        await(() -> locked(Collections.singleton(first)) && !FootageLocks.readAuto(directory).contains(first),5000);
        String later = clip(moment+5000,"roadrear"); assertFalse(locked(Collections.singleton(later)));
        AutoLock.get().recordingStopped();
        await(() -> locked(Collections.singleton(later)),LockWindow.AFTER_MS + LockWindow.SECOND_PASS_DELAY_MS + 5000);
        assertFalse(FootageLocks.readAuto(directory).contains(later));
    }

    @Test public void flashPromotesAnExistingBrakeLockOutsideTheAutomaticBudget() throws Exception {
        long moment = System.currentTimeMillis(); String first = clip(moment - 1000, "surround");
        CountDownLatch automatic = new CountDownLatch(1);
        FootageLocks.lockAuto(directory, Collections.singleton(first), Collections.singleton(first), Long.MAX_VALUE,
                ok -> { assertTrue(ok); automatic.countDown(); });
        assertTrue(automatic.await(5, TimeUnit.SECONDS));
        config.setFlashLockEnabled(true);
        AutoLock.get().recordingStarted(app, null); InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        java.lang.reflect.Method request = AutoLock.class.getDeclaredMethod("lockAround", Context.class, long.class, AutoLock.Source.class);
        request.setAccessible(true); main(() -> {
            try { request.invoke(AutoLock.get(), app, moment, AutoLock.Source.FLASH); }
            catch (Exception error) { throw new AssertionError(error); }
        });
        await(() -> locked(Collections.singleton(first)) && !FootageLocks.readAuto(directory).contains(first), 5000);
        String later = clip(moment + 120000, "surround"); CountDownLatch budget = new CountDownLatch(1);
        FootageLocks.lockAuto(directory, Collections.singleton(later), Collections.singleton(later), 0,
                ok -> { assertTrue(ok); budget.countDown(); });
        assertTrue(budget.await(5, TimeUnit.SECONDS));
        assertTrue("Later budget enforcement cannot release an explicit flash request", locked(Collections.singleton(first)));
    }

    @Test public void manualPromotionReportsFailureWhenAutomaticOwnershipCannotBeRead() throws Exception {
        long moment = System.currentTimeMillis(); String first = clip(moment - 1000, "surround");
        CountDownLatch automatic = new CountDownLatch(1);
        FootageLocks.lockAuto(directory, Collections.singleton(first), Collections.singleton(first), Long.MAX_VALUE,
                ok -> automatic.countDown());
        assertTrue(automatic.await(5, TimeUnit.SECONDS));
        File metadata = new File(directory, FootageLocks.AUTO_FILE_NAME);
        try (java.io.RandomAccessFile corrupt = new java.io.RandomAccessFile(metadata, "rw")) { corrupt.setLength(2L * 1024 * 1024 + 1); }
        CountDownLatch result = new CountDownLatch(1); boolean[] success = {true};
        FootageLocks.set(directory, Collections.singleton(first), true, ok -> { success[0] = ok; result.countDown(); });
        assertTrue(result.await(5, TimeUnit.SECONDS)); assertFalse("Cannot claim promotion while auto ownership remains unknown", success[0]);
        assertTrue("Existing protection is preserved on failure", locked(Collections.singleton(first)));
    }
}
