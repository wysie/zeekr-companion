package com.kooo.evcam;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * 唤醒工具类
 *
 * 阻止休眠的关键：
 * 1. PARTIAL_WAKE_LOCK - 保持 CPU 运行
 * 2. 电池优化白名单 - 防止 Doze 模式忽略 WakeLock
 * 3. 在前台服务中持有 WakeLock - 比 Activity 更可靠
 */
public class WakeUpHelper {
    private static final String TAG = "WakeUpHelper";

    // System-bounded wake lease for screen-off recording.
    private static PowerManager.WakeLock persistentWakeLock;
    private static final java.util.concurrent.ScheduledExecutorService LEASE_TIMER =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "RecordingWakeLease");
                thread.setDaemon(true);
                return thread;
            });
    private static java.util.concurrent.ScheduledFuture<?> leaseTimeout;
    private static long leaseGeneration;

    /**
     * 检查是否有悬浮窗权限（用于后台启动Activity）
     * Android 10+ 需要此权限才能从后台启动 Activity
     */
    public static boolean hasOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    /**
     * 请求悬浮窗权限
     * 需要用户手动授权
     */
    public static void requestOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    /**
     * 拿一个有系统超时上限的唤醒锁，录像期间保持车机运行。
     *
     * <h3>这是有代价的，而且代价是实测过的</h3>
     *
     * <p>2026-09-23 那份黑匣子：车机开机 26 小时，其中 <b>20.8 小时在深睡</b>
     * —— 全是停着的时候。这个锁拿着，那 20.8 小时就变成醒着，停在那里耗 12V 电瓶。</p>
     *
     * <p>同一份日志里还有一条：两段深睡前后进程 pid 一模一样，
     * <b>进程不需要这个锁也能活下来</b>。所以名叫「防止休眠」的那个开关 1.25.0 删掉了。</p>
     *
     * <p>现在只有一个调用者：「熄屏录制（阻止休眠）」（规格 §3.1，{@code ScreenOffRecording}）——
     * 熄屏时在录像才拿，录像停了、亮屏了、到了用户设的时长就放。项目所有者 2026-09-27
     * 明确允许熄屏录制不让车机睡。完整数据见 {@code docs/zeekr-platform-notes.md} §3.6。</p>
     */
    public static synchronized void acquirePersistentWakeLock(Context context, long timeoutMs) {
        AppLog.d(TAG, "Acquiring persistent wake lock (prevent sleep)...");
        com.kooo.evcam.blackbox.BlackBox.note("请求持续唤醒锁");

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) {
            AppLog.e(TAG, "PowerManager is null");
            return;
        }

        // 创建持续唤醒锁
        // PARTIAL_WAKE_LOCK: 只保持CPU运行，不亮屏
        if (persistentWakeLock == null) persistentWakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "EVCam:AutoStartAwake"
        );

        // Non-counted lease: renew against the same screen-off deadline, capped by the system.
        persistentWakeLock.setReferenceCounted(false);
        persistentWakeLock.acquire(Math.max(1L, timeoutMs));
        if (leaseTimeout != null) leaseTimeout.cancel(false);
        long generation = ++leaseGeneration;
        // AOSP's acquire(timeout) itself uses a Handler. Also expire off the UI thread;
        // neither mechanism is a guarantee when the whole process/vendor power service stalls.
        leaseTimeout = LEASE_TIMER.schedule(() -> {
            synchronized (WakeUpHelper.class) {
                if (generation == leaseGeneration) releasePersistentWakeLock();
            }
        }, Math.max(1L, timeoutMs), java.util.concurrent.TimeUnit.MILLISECONDS);
        AppLog.d(TAG, "Recording wake lease acquired, timeout=" + timeoutMs + "ms");
    }
    
    /**
     * 释放持续唤醒锁
     */
    public static synchronized void releasePersistentWakeLock() {
        ++leaseGeneration;
        if (leaseTimeout != null) { leaseTimeout.cancel(false); leaseTimeout = null; }
        if (persistentWakeLock != null && persistentWakeLock.isHeld()) {
            try {
                persistentWakeLock.release();
                com.kooo.evcam.blackbox.BlackBox.note("释放持续唤醒锁");
                AppLog.d(TAG, "Persistent WakeLock released - system can sleep now");
            } catch (Exception e) {
                AppLog.e(TAG, "Failed to release persistent WakeLock", e);
            }
        }
        persistentWakeLock = null;
    }
    
    /**
     * 检查持续唤醒锁是否被持有
     */
    public static synchronized boolean isPersistentWakeLockHeld() {
        return persistentWakeLock != null && persistentWakeLock.isHeld();
    }
    
    /**
     * 检查应用是否在电池优化白名单中
     * Android 6.0+ 的 Doze 模式会忽略 WakeLock，只有加入白名单才能真正阻止休眠
     * 
     * @return true 表示已在白名单中（不受 Doze 限制）
     */
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true; // Android 6.0 以下不需要
    }
    
    /**
     * 请求加入电池优化白名单
     * 这是阻止休眠的关键！Doze 模式下只有白名单应用的 WakeLock 才有效
     * 
     * 注意：会弹出系统对话框，需要用户确认
     */
    public static void requestIgnoreBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    AppLog.d(TAG, "Requesting battery optimization whitelist");
                } catch (Exception e) {
                    AppLog.e(TAG, "Failed to request battery optimization whitelist", e);
                    // 某些设备可能不支持，尝试打开电池优化设置页面
                    openBatteryOptimizationSettings(context);
                }
            } else {
                AppLog.d(TAG, "Already in battery optimization whitelist");
            }
        }
    }
    
    /**
     * 打开电池优化设置页面（备用方案）
     */
    public static void openBatteryOptimizationSettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            AppLog.e(TAG, "Failed to open battery optimization settings", e);
        }
    }

}
