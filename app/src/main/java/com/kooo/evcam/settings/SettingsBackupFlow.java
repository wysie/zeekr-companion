package com.kooo.evcam.settings;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.ui.CamDialogs;
import com.kooo.evcam.ui.FailureReason;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Native System backup controls. Confirmation stages a snapshot, exits normally, and restores on reopening. */
public final class SettingsBackupFlow {

    private static final String TAG = "SettingsBackupFlow";
    private static final java.util.concurrent.atomic.AtomicBoolean BUSY = new java.util.concurrent.atomic.AtomicBoolean();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SettingsBackupFlow() {
    }

    // ================================================================= 导出

    public static void export(Activity activity) {
        if (!begin(activity)) return;
        new Thread(() -> {
            File out = null;
            Exception failure = null;
            try {
                out = SettingsBackup.export(activity);
            } catch (Exception e) {
                AppLog.e(TAG, "导出设置失败", e);
                failure = e;
            }
            final File written = out;
            final Exception error = failure;
            MAIN.post(() -> {
                BUSY.set(false);
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                String message;
                if (written != null) {
                    message = activity.getString(R.string.settings_exported, written.getAbsolutePath());
                } else {
                    String reason = FailureReason.of(activity, error);
                    message = reason != null ? activity.getString(R.string.settings_export_failed_reason, reason)
                            : activity.getString(R.string.settings_export_failed);
                }
                CamDialogs.show(builder(activity).setTitle(R.string.set_settings_export_title).setMessage(message)
                        .setPositiveButton(R.string.action_ok, null));
            });
        }, "settings-export").start();
    }

    // ================================================================= 导入

    public static void startImport(Activity activity) {
        if (!begin(activity)) return;
        new Thread(() -> {
            File folder = SettingsBackup.folder(activity);
            List<SettingsBackup.Backup> backups = new ArrayList<>();
            for (File file : SettingsBackup.list(activity)) {
                SettingsBackup.Backup parsed = SettingsBackup.read(activity, file);
                // The picker retains metadata only, not a complete preference map per backup file.
                backups.add(new SettingsBackup.Backup(file, parsed.status, parsed.appVersion, parsed.exportedAt, null));
            }
            MAIN.post(() -> {
                BUSY.set(false);
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                if (backups.isEmpty()) {
                    CamDialogs.show(builder(activity)
                            .setTitle(R.string.settings_none_title)
                            .setMessage(activity.getString(R.string.settings_none_msg,
                                    folder != null ? folder.getAbsolutePath() : "?"))
                            .setPositiveButton(R.string.action_got_it, null));
                    return;
                }
                String[] labels = new String[backups.size()];
                for (int i = 0; i < labels.length; i++) {
                    labels[i] = label(activity, backups.get(i));
                }
                CamDialogs.show(builder(activity)
                        .setTitle(R.string.settings_pick_title)
                        .setItems(labels, (d, which) -> readChosen(activity, backups.get(which).file))
                        .setNegativeButton(R.string.action_cancel, null));
            });
        }, "settings-import-list").start();
    }

    private static void readChosen(Activity activity, File file) {
        if (!begin(activity)) return;
        new Thread(() -> {
            SettingsBackup.Backup backup = SettingsBackup.read(activity, file);
            MAIN.post(() -> {
                BUSY.set(false);
                if (!activity.isFinishing() && !activity.isDestroyed()) chosen(activity, backup);
            });
        }, "settings-import-read").start();
    }

    /** 列表里的一行：导出时间 · 版本；读不出来的写文件名。 */
    private static String label(Activity activity, SettingsBackup.Backup backup) {
        if (backup.exportedAt <= 0L || backup.appVersion.isEmpty()) {
            return backup.file.getName();
        }
        // 数字写法，哪种语言都一样（和诊断报告、文件名里的时间同一种写法）
        String when = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(backup.exportedAt));
        return activity.getString(R.string.settings_file_label, when, backup.appVersion);
    }

    private static void chosen(Activity activity, SettingsBackup.Backup backup) {
        switch (backup.status) {
            case OK:
                CamDialogs.showDestructive(builder(activity)
                        .setTitle(R.string.settings_import_confirm_title)
                        .setMessage(R.string.settings_import_confirm_msg)
                        .setPositiveButton(R.string.settings_import_action, (d, w) -> apply(activity, backup))
                        .setNegativeButton(R.string.action_cancel, null));
                break;
            case OTHER_VERSION:
                CamDialogs.show(builder(activity)
                        .setTitle(R.string.settings_import_refused_title)
                        .setMessage(activity.getString(R.string.settings_other_version_msg,
                                backup.appVersion.isEmpty() ? "?" : backup.appVersion,
                                SettingsBackup.currentMajor(activity)))
                        .setPositiveButton(R.string.action_got_it, null));
                break;
            default:
                Toast.makeText(activity.getApplicationContext(), R.string.settings_unreadable,
                        Toast.LENGTH_LONG).show();
                break;
        }
    }

    /** Stage durably on a worker, then stop/exit using the current live settings. */
    private static void apply(Activity activity, SettingsBackup.Backup backup) {
        if (!begin(activity)) return;
        new Thread(() -> {
            boolean staged;
            try { SettingsBackup.stage(activity, backup); staged = true; }
            catch (Exception error) { AppLog.e(TAG, "Cannot stage settings restore", error); staged = false; }
            final boolean ok = staged;
            MAIN.post(() -> {
                BUSY.set(false);
                if (!ok) {
                    Toast.makeText(activity.getApplicationContext(), R.string.settings_import_failed,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (activity instanceof MainActivity && !activity.isFinishing()) {
                    ((MainActivity) activity).exitApp();
                } else {
                    // 设置页只长在主界面里（不会走到这里）：写好了，提示重新打开
                    AppLog.w(TAG, "导入设置之后找不到主界面，没有退出");
                    Toast.makeText(activity.getApplicationContext(), R.string.settings_imported_reopen,
                            Toast.LENGTH_LONG).show();
                }
            });
        }, "settings-import").start();
    }

    private static boolean begin(Activity activity) {
        if (BUSY.compareAndSet(false, true)) return true;
        Toast.makeText(activity, R.string.settings_backup_busy, Toast.LENGTH_SHORT).show(); return false;
    }
    public static void showRestoreNotice(Activity activity) {
        if (!SettingsBackup.takeRestoreNotice(activity)) return;
        Toast.makeText(activity, "restored".equals(SettingsBackup.lastRestoreResult(activity))
                ? R.string.settings_restore_success : R.string.settings_restore_failed, Toast.LENGTH_LONG).show();
    }

    private static MaterialAlertDialogBuilder builder(Activity activity) {
        return new MaterialAlertDialogBuilder(activity, R.style.Theme_Cam_MaterialAlertDialog);
    }
}
