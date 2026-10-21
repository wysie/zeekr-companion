package com.kooo.evcam.zeekr;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.storage.ActiveFootage;
import com.kooo.evcam.storage.FootageLocks;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class TimelineDeletionAndroidTest extends MainPageControlsAndroidTest {
    @Test public void bothTimelineDeleteActionsRecheckNewLocksAndOpenFiles() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        new AppConfig(app).setFootageLockEnabled(true);
        File directory = Files.createTempDirectory(app.getCacheDir().toPath(), "timeline-delete-").toFile();
        try (ActivityScenario<TimelinePlayerActivity> scenario = ActivityScenario.launch(TimelinePlayerActivity.class)) {
            for (String action : new String[]{"deleteChosen", "deleteSession"}) {
                File open = new File(directory,"20261021_120000_surround.mp4");
                File locked = new File(directory,"20261021_120001_surround.mp4");
                File ordinary = new File(directory,"20261021_120002_surround.mp4");
                try (ActiveFootage.Lease lease = ActiveFootage.acquire(open)) {
                    for (File file : Arrays.asList(open,locked,ordinary)) Files.write(file.toPath(),new byte[]{1,2,3});
                    CountDownLatch saved = new CountDownLatch(1);
                    FootageLocks.setFiles(Collections.singletonList(locked),true,ok -> { assertTrue(ok); saved.countDown(); });
                    assertTrue(saved.await(5,TimeUnit.SECONDS));
                    List<LaneTrack.Clip> clips = Arrays.asList(new LaneTrack.Clip(open.getPath(),0,1000,3),
                            new LaneTrack.Clip(locked.getPath(),0,1000,3),new LaneTrack.Clip(ordinary.getPath(),0,1000,3));
                    scenario.onActivity(activity -> {
                        try {
                            // Reproduce a list scanned before the event lock was created.
                            java.lang.reflect.Field old = TimelinePlayerActivity.class.getDeclaredField("lockEnabled");
                            old.setAccessible(true); old.setBoolean(activity,false);
                            java.lang.reflect.Method method;
                            if ("deleteChosen".equals(action)) {
                                method=TimelinePlayerActivity.class.getDeclaredMethod(action,List.class);method.setAccessible(true);method.invoke(activity,clips);
                            } else {
                                method=TimelinePlayerActivity.class.getDeclaredMethod(action,int.class,List.class);method.setAccessible(true);method.invoke(activity,0,clips);
                            }
                        } catch (Exception error) { throw new AssertionError(error); }
                    });
                    long deadline = android.os.SystemClock.elapsedRealtime() + 5000; boolean[] finished = {false};
                    do {
                        scenario.onActivity(activity -> {
                            try {
                                java.lang.reflect.Field busy = TimelinePlayerActivity.class.getDeclaredField("deletingFiles");
                                busy.setAccessible(true); finished[0] = !busy.getBoolean(activity);
                            } catch (Exception error) { throw new AssertionError(error); }
                        });
                        if (!finished[0]) android.os.SystemClock.sleep(25);
                    } while (!finished[0] && android.os.SystemClock.elapsedRealtime() < deadline);
                    assertTrue("Deletion worker completes", finished[0]);
                    assertTrue(action+" retains open footage",open.isFile());
                    assertTrue(action+" retains a lock created after scanning",locked.isFile());
                    assertFalse(action+" still deletes eligible footage",ordinary.exists());
                } finally {
                    Files.deleteIfExists(open.toPath()); Files.deleteIfExists(locked.toPath()); Files.deleteIfExists(ordinary.toPath());
                    Files.deleteIfExists(new File(directory,FootageLocks.FILE_NAME).toPath());
                }
            }
        } finally { Files.deleteIfExists(directory.toPath()); }
    }
}
