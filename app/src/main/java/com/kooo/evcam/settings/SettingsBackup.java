package com.kooo.evcam.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.StorageHelper;
import com.kooo.evcam.profile.ProfileStore;
import com.kooo.evcam.update.VersionName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Companion settings backup, adapted from dts88 #64. Runtime state and access grants stay local.
 * UI restore is staged durably, then applied before configuration is read on the next startup.
 * A durable preimage and commit marker protect restores spanning several preference files.
 */
public final class SettingsBackup {

    private static final String TAG = "SettingsBackup";

    /** 文件里标明「这是设置文件」。 */
    static final String KIND = "zeekr_companion_settings";
    /** 文件格式。结构变了就加一：格式不同的不导入（和大版本不同一样，不做兼容）。 */
    static final int FORMAT = 1;
    static final String FILE_PREFIX = "ZeekrCompanion-settings_";
    static final String FILE_SUFFIX = ".json";
    /** 文件名里的时间，和诊断报告的写法一样：按名字排就是按时间排。 */
    private static final String FILE_TIME = "yyyyMMdd_HHmmss_SSS";
    /** 设置文件最大多大：比这大的不是我们导出的，不读。 */
    private static final long MAX_FILE_BYTES = 1024L * 1024L;

    /** 一项设置的类型（SharedPreferences 能存的那几种）。 */
    private static final String TYPE_BOOLEAN = "boolean";
    private static final String TYPE_INT = "int";
    private static final String TYPE_LONG = "long";
    private static final String TYPE_FLOAT = "float";
    private static final String TYPE_STRING = "string";
    private static final String TYPE_STRING_SET = "string_set";

    /** Five persistent choice stores; runtime/access state is intentionally outside this list. */
    private static final List<String> STORES = Collections.unmodifiableList(
            Arrays.asList(AppConfig.PREF_NAME, ProfileStore.FILE, "bird_eye_calibration",
                    "bird_eye_window", "bird_eye_appearance"));
    private static final Object RESTORE_LOCK = new Object();
    public static List<String> storeNames() { return STORES; }

    private SettingsBackup() {
    }

    // ================================================================= 导出

    /**
     * 此刻的全部设置（导出、诊断报告用）。
     *
     * @throws JSONException 组装失败（不会发生：值都是 SharedPreferences 里的基本类型）
     */
    public static JSONObject snapshot(Context context) throws JSONException {
        Context app = context.getApplicationContext();
        JSONObject root = new JSONObject();
        root.put("kind", KIND);
        root.put("format", FORMAT);
        root.put("application_id", app.getPackageName());
        root.put("app_version", appVersion(app));
        root.put("exported_at", System.currentTimeMillis());
        JSONObject stores = new JSONObject();
        for (String store : STORES) {
            stores.put(store, encode(store, prefs(app, store).getAll()));
        }
        root.put("stores", stores);
        return root;
    }

    /**
     * 导出到诊断报告的那个目录。写文件是 I/O，调用方在后台线程上调。
     *
     * @return 写出的文件
     */
    public static File export(Context context) throws IOException, JSONException {
        File dir = folder(context);
        if (dir == null || (!dir.isDirectory() && !dir.mkdirs())) {
            throw new IOException("cannot create " + dir);
        }
        File out = new File(dir, fileName(System.currentTimeMillis()));
        byte[] bytes = snapshot(context).toString(2).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FILE_BYTES) throw new IOException("Settings file too large");
        synchronized (RESTORE_LOCK) {
            int suffix = 0;
            String name = out.getName();
            while (out.exists()) out = new File(dir, name.replace(FILE_SUFFIX, "-" + (++suffix) + FILE_SUFFIX));
            atomicWrite(out, bytes);
        }
        AppLog.i(TAG, "设置已导出: " + out.getAbsolutePath() + "（" + bytes.length + " 字节）");
        com.kooo.evcam.blackbox.BlackBox.note("导出设置: " + out.getName());
        return out;
    }

    /** 导出、导入用的目录：诊断报告存的那个（跟着存储位置）。 */
    public static File folder(Context context) {
        return StorageHelper.getExportDir(context);
    }

    // ================================================================= 导入

    /** 读出来的一份设置文件：能不能导入、为什么。 */
    public static final class Backup {

        public enum Status {
            /** 能导入。 */
            OK,
            /** 别的大版本、或者别的格式导出的：不导入。 */
            OTHER_VERSION,
            /** 读不了：不是设置文件、坏了、太大。 */
            UNREADABLE
        }

        public final File file;
        public final Status status;
        /** 导出它的应用版本；读不出来是空字符串。 */
        public final String appVersion;
        /** 导出的时刻（墙钟毫秒）；读不出来是 0。 */
        public final long exportedAt;
        /** 解析好、待写回的值（只有 OK 才有）：存储名 → 键 → 值。 */
        final Map<String, Map<String, Object>> values;

        Backup(File file, Status status, String appVersion, long exportedAt,
               Map<String, Map<String, Object>> values) {
            this.file = file;
            this.status = status;
            this.appVersion = appVersion;
            this.exportedAt = exportedAt;
            this.values = values;
        }
    }

    /** 目录里的设置文件，新的在前（文件名里的时间）。读目录是 I/O：后台线程。 */
    public static List<File> list(Context context) {
        File dir = folder(context);
        File[] found = dir == null ? null : dir.listFiles((d, name) -> isSettingsFile(name));
        List<File> files = new ArrayList<>();
        if (found != null) {
            for (File file : found) {
                if (file.isFile()) {
                    files.add(file);
                }
            }
        }
        Collections.sort(files, (a, b) -> b.getName().compareTo(a.getName()));
        return files;
    }

    /** 读一个文件，查种类、格式和大版本，把值整份解析好。读不了不抛：状态里说。I/O：后台线程。 */
    public static Backup read(Context context, File file) {
        String version = "";
        long exportedAt = 0L;
        try {
            if (file.length() > MAX_FILE_BYTES) {
                throw new IOException("too large: " + file.length());
            }
            JSONObject root = new JSONObject(readText(file));
            if (!KIND.equals(root.optString("kind")) || !context.getPackageName().equals(root.optString("application_id"))) {
                throw new JSONException("not a settings file");
            }
            version = root.optString("app_version", "");
            exportedAt = root.optLong("exported_at", 0L);
            if (!compatible(root.optInt("format", -1), version, appVersion(context))) {
                return new Backup(file, Backup.Status.OTHER_VERSION, version, exportedAt, null);
            }
            JSONObject stores = root.getJSONObject("stores");
            Map<String, Map<String, Object>> values = new TreeMap<>();
            for (String store : STORES) {
                Map<String, Object> decoded = decode(store, stores.getJSONObject(store));
                Map<String, ?> current = prefs(context, store).getAll();
                for (Map.Entry<String, Object> item : decoded.entrySet())
                    if (current.containsKey(item.getKey()) && !typeOf(item.getValue()).equals(typeOf(current.get(item.getKey()))))
                        throw new JSONException("Preference type changed: " + item.getKey());
                values.put(store, Collections.unmodifiableMap(decoded));
            }
            return new Backup(file, Backup.Status.OK, version, exportedAt, Collections.unmodifiableMap(values));
        } catch (Exception e) {
            AppLog.w(TAG, "读不了设置文件 " + file.getName() + ": " + e);
            return new Backup(file, Backup.Status.UNREADABLE, version, exportedAt, null);
        }
    }

    /** Synchronous startup/test engine; the UI uses stage() so live pipelines never see partial writes. */
    public static boolean apply(Context context, Backup backup) {
        if (backup == null || backup.status != Backup.Status.OK || backup.values == null) return false;
        synchronized (RESTORE_LOCK) {
            SettingsRestoreTransaction.Result result = SettingsRestoreTransaction.apply(
                    backend(context), journal(context), backup.values);
            AppLog.i(TAG, "Settings restore: " + result);
            return result == SettingsRestoreTransaction.Result.APPLIED;
        }
    }

    /** Confirmation writes only a pending snapshot. Live recorders keep their applied settings until exit. */
    public static void stage(Context context, Backup backup) throws IOException, JSONException {
        if (backup == null || backup.status != Backup.Status.OK || backup.values == null)
            throw new IOException("Invalid settings backup");
        synchronized (RESTORE_LOCK) {
            if (exists(pending(context))) throw new IOException("A restore is already pending");
            JSONObject root = snapshot(context);
            root.put("app_version", backup.appVersion);
            root.put("stores", encodeStores(backup.values));
            atomicWrite(pending(context), root.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Called before config/service initialization in the earliest Provider, with an Application fallback. */
    public static void restoreOnStartup(Context context) {
        synchronized (RESTORE_LOCK) {
            if (!SettingsRestoreTransaction.recover(backend(context), journal(context)))
                throw new IllegalStateException("Settings recovery cannot complete; original-settings journal retained");
            if (!exists(pending(context))) return;
            Backup backup = read(context, pending(context));
            boolean ok = apply(context, backup);
            // A failed rollback must never start cameras against a partially restored configuration.
            if (!SettingsRestoreTransaction.recover(backend(context), journal(context)))
                throw new IllegalStateException("Settings recovery cannot complete; original-settings journal retained");
            new android.util.AtomicFile(pending(context)).delete();
            prefs(context, "settings_restore_status").edit().putString("result", ok ? "restored" : "failed")
                    .putBoolean("notice", true).commit();
        }
    }

    public static String lastRestoreResult(Context context) {
        return prefs(context, "settings_restore_status").getString("result", "");
    }
    public static boolean takeRestoreNotice(Context context) {
        SharedPreferences value = prefs(context, "settings_restore_status");
        boolean notice = value.getBoolean("notice", false);
        if (notice) value.edit().putBoolean("notice", false).apply();
        return notice;
    }
    static File pending(Context context) { return new File(context.getFilesDir(), "settings-restore-pending.json"); }
    static File recovery(Context context) { return new File(context.getFilesDir(), "settings-restore-journal.json"); }
    private static boolean exists(File file) { return file.exists() || new File(file.getPath() + ".bak").exists(); }

    private static SettingsRestoreTransaction.Stores backend(Context context) {
        return new SettingsRestoreTransaction.Stores() {
            public Map<String, Map<String, Object>> read() {
                Map<String, Map<String, Object>> out = new TreeMap<>();
                for (String store : STORES) {
                    Map<String, Object> values = new TreeMap<>();
                    for (Map.Entry<String, ?> item : prefs(context, store).getAll().entrySet())
                        if (carries(store, item.getKey())) values.put(item.getKey(), item.getValue() instanceof Set
                                ? new HashSet<>((Set<?>) item.getValue()) : item.getValue());
                    out.put(store, values);
                }
                return out;
            }
            public boolean replace(String store, Map<String, Object> values) {
                if (!STORES.contains(store)) return false;
                SharedPreferences target = prefs(context, store);
                SharedPreferences.Editor edit = target.edit();
                for (String key : target.getAll().keySet()) if (carries(store, key)) edit.remove(key);
                for (Map.Entry<String, Object> item : values.entrySet())
                    if (carries(store, item.getKey())) put(edit, item.getKey(), item.getValue());
                return edit.commit();
            }
        };
    }
    private static JSONObject encodeStores(Map<String, Map<String, Object>> values) throws JSONException {
        JSONObject out = new JSONObject();
        for (String store : STORES) out.put(store, encode(store, values.get(store)));
        return out;
    }
    private static SettingsRestoreTransaction.Journal journal(Context context) {
        File file = recovery(context);
        return new SettingsRestoreTransaction.Journal() {
            public SettingsRestoreTransaction.Entry read() throws IOException {
                if (!exists(file)) return null;
                try {
                    JSONObject root = new JSONObject(readText(file));
                    if (!"companion-settings-rollback-1".equals(root.getString("kind"))) throw new JSONException("Invalid journal");
                    Map<String, Map<String, Object>> before = new TreeMap<>();
                    JSONObject values = root.getJSONObject("stores");
                    for (String store : STORES) before.put(store, decode(store, values.getJSONObject(store)));
                    return new SettingsRestoreTransaction.Entry(root.getBoolean("committed"), before);
                } catch (JSONException error) { throw new IOException("Invalid settings recovery journal", error); }
            }
            public void write(SettingsRestoreTransaction.Entry entry) throws IOException {
                try {
                    JSONObject root = new JSONObject();root.put("kind", "companion-settings-rollback-1");
                    root.put("committed", entry.committed);root.put("stores", encodeStores(entry.before));
                    atomicWrite(file, root.toString().getBytes(StandardCharsets.UTF_8));
                } catch (JSONException error) { throw new IOException(error); }
            }
            public void clear() { new android.util.AtomicFile(file).delete(); }
        };
    }
    private static void atomicWrite(File file, byte[] bytes) throws IOException {
        if (bytes.length > MAX_FILE_BYTES) throw new IOException("Settings data too large");
        android.util.AtomicFile atomic = new android.util.AtomicFile(file);
        FileOutputStream stream = null;
        try {
            stream = atomic.startWrite();stream.write(bytes);stream.getFD().sync();atomic.finishWrite(stream);stream = null;
            if (!Arrays.equals(bytes, readText(file).getBytes(StandardCharsets.UTF_8))) throw new IOException("Settings write verification failed");
        } finally { if (stream != null) atomic.failWrite(stream); }
    }

    /** 当前应用的大版本（版本号第一段）；读不出来是 -1。 */
    public static int currentMajor(Context context) {
        return VersionName.major(appVersion(context));
    }

    // ================================================================= 纯函数（测试直接调）

    /** 文件格式相同、大版本相同才导入。认不出版本号的一律不导入。 */
    static boolean compatible(int format, String fileVersion, String appVersion) {
        int major = VersionName.major(fileVersion);
        return format == FORMAT && major >= 0 && major == VersionName.major(appVersion);
    }

    static boolean isSettingsFile(String name) {
        return name != null && name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX);
    }

    static String fileName(long wallMs) {
        return FILE_PREFIX + new SimpleDateFormat(FILE_TIME, Locale.US).format(new Date(wallMs)) + FILE_SUFFIX;
    }

    /** 这一项要不要导出、导入：设置里的运行状态不要，视频流配置全要。 */
    static boolean carries(String store, String key) {
        return !AppConfig.PREF_NAME.equals(store) || !AppConfig.isRuntimeState(key);
    }

    // ================================================================= 编码

    private static JSONObject encode(String store, Map<String, ?> all) throws JSONException {
        JSONObject out = new JSONObject();
        for (Map.Entry<String, ?> entry : new TreeMap<>(all).entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String type = typeOf(value);
            if (type == null || !carries(store, key)) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("type", type);
            if (value instanceof Set) {
                JSONArray array = new JSONArray();
                for (Object element : new TreeSet<>(stringSet(value))) {
                    array.put(element);
                }
                item.put("value", array);
            } else if (value instanceof Float) {
                // 按 Float 自己的写法存（0.1 而不是 0.10000000149011612），读回来再转成 float 一模一样
                item.put("value", Double.parseDouble(value.toString()));
            } else {
                item.put("value", value);
            }
            out.put(key, item);
        }
        return out;
    }

    private static Map<String, Object> decode(String store, JSONObject in) throws JSONException {
        Map<String, Object> out = new TreeMap<>();
        Iterator<String> keys = in.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!carries(store, key)) {
                continue;
            }
            JSONObject item = in.getJSONObject(key);
            String type = item.getString("type");
            Object value;
            switch (type) {
                case TYPE_BOOLEAN:
                    value = item.get("value");
                    if (!(value instanceof Boolean)) throw new JSONException("Expected boolean");
                    break;
                case TYPE_INT:
                    value = strictInteger(item.get("value"), true);
                    break;
                case TYPE_LONG:
                    value = strictInteger(item.get("value"), false);
                    break;
                case TYPE_FLOAT:
                    Object number = item.get("value");
                    if (!(number instanceof Number) || !Float.isFinite(((Number) number).floatValue())) throw new JSONException("Expected finite float");
                    value = ((Number) number).floatValue();
                    break;
                case TYPE_STRING:
                    value = item.get("value");
                    if (!(value instanceof String)) throw new JSONException("Expected string");
                    break;
                case TYPE_STRING_SET:
                    JSONArray array = item.getJSONArray("value");
                    Set<String> set = new HashSet<>();
                    for (int i = 0; i < array.length(); i++) {
                        Object text = array.get(i);
                        if (!(text instanceof String)) throw new JSONException("Expected string set");
                        set.add((String) text);
                    }
                    value = set;
                    break;
                default:
                    throw new JSONException("unknown type " + type + " for " + key);
            }
            if (!SettingsBackupTypes.accepts(store, key, value))
                throw new JSONException("Wrong preference type: " + store + "/" + key);
            out.put(key, value);
        }
        return out;
    }

    private static Object strictInteger(Object value, boolean integer) throws JSONException {
        if (!(value instanceof Integer) && !(value instanceof Long)) throw new JSONException("Expected integer");
        long number = ((Number) value).longValue();
        if (integer) {
            if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw new JSONException("Integer overflow");
            return (int) number;
        }
        return number;
    }

    private static String typeOf(Object value) {
        if (value instanceof Boolean) {
            return TYPE_BOOLEAN;
        }
        if (value instanceof Integer) {
            return TYPE_INT;
        }
        if (value instanceof Long) {
            return TYPE_LONG;
        }
        if (value instanceof Float) {
            return TYPE_FLOAT;
        }
        if (value instanceof String) {
            return TYPE_STRING;
        }
        if (value instanceof Set) {
            return TYPE_STRING_SET;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> stringSet(Object value) {
        return (Set<String>) value;
    }

    @SuppressWarnings("unchecked")
    private static void put(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Set) {
            editor.putStringSet(key, (Set<String>) value);
        }
    }

    // ================================================================= 杂项

    private static SharedPreferences prefs(Context app, String store) {
        return app.getSharedPreferences(store, Context.MODE_PRIVATE);
    }

    private static String appVersion(Context context) {
        try {
            String name = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            return name != null ? name : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String readText(File file) throws IOException {
        try (InputStream in = new android.util.AtomicFile(file).openRead()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                if (out.size() + n > MAX_FILE_BYTES) throw new IOException("Settings data too large");
                out.write(buffer, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
