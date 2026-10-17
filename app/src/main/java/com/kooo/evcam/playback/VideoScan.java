package com.kooo.evcam.playback;

import android.content.Context;

import com.kooo.evcam.AppLog;
import com.kooo.evcam.StorageHelper;
import com.kooo.evcam.camera.CameraSlots;
import com.kooo.evcam.profile.RecordSpecs;

import com.kooo.evcam.storage.FootageLocks;
import com.kooo.evcam.zeekr.ClipDurations;

import com.kooo.evcam.zeekr.LaneTrack;
import com.kooo.evcam.zeekr.RecordingTimeline;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 扫录像目录，按连续录制分成一条条（{@link RecordingTimeline.Session}），供回看网格用。
 *
 * <p>和播放器（TimelinePlayerActivity.loadTimelines）同一套规矩：时间轴只按环视那一路排，
 * 其余几路另外收着。保持当前播放器的间隙分组与独立机位归属规则。
 * 一条录制的文件 = 环视的分段 + 同一次录制里其余几路的文件，删除按这个算。</p>
 *
 * <p>只读目录，不碰界面，在后台线程调。</p>
 */
public final class VideoScan {

    /** 网格里的一格：一条连续录制。 */
    public static final class Item {
        public final RecordingTimeline.Session session;
        /** 这一条的全部文件（环视 + 座舱各路）。 */
        public final List<File> files;
        public final long bytes;
        /** 环视第一段：缩略图取它的一帧。 */
        public final File thumbnail;
        /** 有没有锁定的文件（锁定影像开着时才算）。 */
        public final boolean locked;

        Item(RecordingTimeline.Session session, List<File> files, long bytes, File thumbnail, boolean locked) {
            this.session = session;
            this.files = files;
            this.bytes = bytes;
            this.thumbnail = thumbnail;
            this.locked = locked;
        }

        public Date start() {
            return new Date(session.startEpochMs);
        }
    }

    private VideoScan() {
    }

    /** @return 按日期分段、最新的在前；没有录像时是空列表 */
    public static List<DateSection<Item>> scan(Context context) {
        Context app = context.getApplicationContext();
        List<File> dirs = StorageHelper.getVideoReadDirs(app);
        List<RecordingTimeline.Source> sources = new ArrayList<>();
        Map<String, List<RecordingTimeline.Source>> others = new HashMap<>();
        try {
            for (File dir : dirs) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (!f.isFile() || !f.getName().toLowerCase(Locale.US).endsWith(".mp4")) {
                            continue;
                        }
                        String slot = RecordingTimeline.parseCameraSlot(f.getName());
                        long start = RecordingTimeline.parseStartEpochMs(f.getName());
                        if (slot == null || start < 0) {
                            continue;
                        }
                        long duration = ClipDurations.of(f);
                        if (duration <= 0) {
                            continue;
                        }
                        RecordingTimeline.Source source = new RecordingTimeline.Source(
                                f.getAbsolutePath(), start, duration, f.length());
                        if (RecordingTimeline.isSlot(f.getName(), CameraSlots.SURROUND)) {
                            sources.add(source);
                        } else {
                            String key = CameraSlots.canonical(slot.toLowerCase(Locale.US));
                            List<RecordingTimeline.Source> list = others.get(key);
                            if (list == null) {
                                list = new ArrayList<>();
                                others.put(key, list);
                            }
                            list.add(source);
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppLog.e("VideoScan", "扫描录像失败", e);
        }

        Map<String, List<RecordingTimeline.Source>> inventory = new HashMap<>(others);
        inventory.put(CameraSlots.SURROUND, sources);
        List<RecordingTimeline.Session> built = com.kooo.evcam.zeekr.RecordingCatalog.build(inventory);
        Map<String, List<LaneTrack>> owned = new HashMap<>();
        for (Map.Entry<String, List<RecordingTimeline.Source>> entry : others.entrySet()) {
            long segment = RecordSpecs.segmentMs(RecordSpecs.forCameraKey(app,
                    CameraSlots.keyForSuffix(entry.getKey())).segmentMinutes);
            owned.put(entry.getKey(), com.kooo.evcam.zeekr.RecordingCatalog.owned(entry.getValue(), built, segment));
        }
        Set<String> locked = FootageLocks.shownFiles(app, dirs);

        SimpleDateFormat dayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Map<String, DateSection<Item>> byDate = new LinkedHashMap<>();
        // 倒着走：最新的在前
        for (int i = built.size() - 1; i >= 0; i--) {
            RecordingTimeline.Session session = built.get(i);
            List<LaneTrack.Clip> clips = new ArrayList<>(LaneTrack.of(session).clips());
            for (List<LaneTrack> lane : owned.values()) {
                clips.addAll(lane.get(i).clips());
            }
            List<File> files = new ArrayList<>();
            long bytes = 0L;
            boolean anyLocked = false;
            for (LaneTrack.Clip clip : clips) {
                File file = new File(clip.path);
                if (files.contains(file)) continue;
                files.add(file);
                bytes += clip.sizeBytes;
                anyLocked |= locked.contains(file.getAbsolutePath());
            }
            File thumb = session.segments.isEmpty() ? null : new File(session.segments.get(0).path);
            Item item = new Item(session, files, bytes, thumb, anyLocked);
            Date start = item.start();
            String day = dayKey.format(start);
            DateSection<Item> section = byDate.get(day);
            if (section == null) {
                section = new DateSection<>(day, start);
                byDate.put(day, section);
            }
            section.addItem(item);
        }
        return new ArrayList<>(byDate.values());
    }

    /**
     * 删掉这几条录制的文件，锁定的留下。U 盘上一个文件删一下就要几十到几百毫秒，
     * 调用方放后台线程。
     *
     * @return {deleted, protected, failed}
     */
    public static int[] delete(Context context, List<Item> items) {
        List<File> files = new ArrayList<>();
        for (Item item : items) files.addAll(item.files);
        return GalleryDelete.files(context, files);
    }
}
