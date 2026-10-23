package com.kooo.evcam.settings;

import android.content.*;
import android.os.SystemClock;
import android.view.View;
import androidx.preference.Preference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import com.kooo.evcam.zeekr.DiagnosticsJson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.anything;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Real Android preference/JSON/filesystem and native System controls, on disposable emulator data. */
public class SettingsBackupAndroidTest extends MainPageControlsAndroidTest {
    private Context app;
    private final Map<String,Map<String,?>> saved = new HashMap<>();
    private final List<File> created = new ArrayList<>();
    private final Set<File> existingExports = new HashSet<>();
    private boolean developer;
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private SharedPreferences prefs(String store) { return app.getSharedPreferences(store,0); }
    @Before public void prepareBackup() {
        app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        developer=DeveloperMode.isUnlocked(); DeveloperMode.setEnabled(app,true);
        for(String store:SettingsBackup.storeNames())saved.put(store,prefs(store).getAll());
        saved.put("settings_restore_status",prefs("settings_restore_status").getAll());
        assertFalse(SettingsBackup.pending(app).exists());assertFalse(SettingsBackup.recovery(app).exists());
        new AppConfig(app).setStorageLocation(AppConfig.STORAGE_INTERNAL);
        existingExports.addAll(SettingsBackup.list(app));
    }
    @SuppressWarnings("unchecked") private void replace(String store,Map<String,?> values) {
        SharedPreferences.Editor edit=prefs(store).edit().clear();
        for(Map.Entry<String,?> entry:values.entrySet()) {
            String key=entry.getKey();Object value=entry.getValue();
            if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);
            else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);
            else if(value instanceof String)edit.putString(key,(String)value);else if(value instanceof Set)edit.putStringSet(key,new HashSet<>((Set<String>)value));
        }
        assertTrue(edit.commit());
    }
    @After public void restoreBackup() {
        if(app==null)return;
        new android.util.AtomicFile(SettingsBackup.pending(app)).delete();new android.util.AtomicFile(SettingsBackup.recovery(app)).delete();
        for(Map.Entry<String,Map<String,?>> item:saved.entrySet())replace(item.getKey(),item.getValue());
        for(File file:created)assertTrue(!file.exists()||file.delete());
        DeveloperMode.setEnabled(app,developer);
    }
    private File export() throws Exception {File file=SettingsBackup.export(app);created.add(file);return file;}
    private File write(JSONObject root) throws Exception {
        File file=File.createTempFile("companion-settings-test-",".json",app.getCacheDir());created.add(file);
        Files.write(file.toPath(),root.toString().getBytes(StandardCharsets.UTF_8));return file;
    }
    private void assertStores(JSONObject expected) throws Exception {
        assertEquals(expected.toString(),SettingsBackup.snapshot(app).getJSONObject("stores").toString());
    }
    @Test public void fullRoundTripIncludesCalibrationGpuVehicleAndTypedChoicesWhileRuntimeStaysLocal() throws Exception {
        AppConfig config=new AppConfig(app);config.setVehicleProfileMode("009");config.setSidePopupYaw(43);
        config.setGpuRenderingMode("custom");config.setGpuFeature("side",true);config.setPhotoViaJpegEnabled(false);
        for(String store:SettingsBackup.storeNames())prefs(store).edit().putString("backup_fixture",store)
                .putStringSet("backup_set",new HashSet<>(Arrays.asList("a","b"))).putFloat("backup_float",.1f)
                .putLong("backup_long",Long.MAX_VALUE).putInt("backup_int",17).putBoolean("backup_bool",true).commit();
        JSONObject expected=SettingsBackup.snapshot(app).getJSONObject("stores");File file=export();
        SettingsBackup.Backup backup=SettingsBackup.read(app,file);assertEquals(SettingsBackup.Backup.Status.OK,backup.status);
        for(String store:SettingsBackup.storeNames())replace(store,Collections.emptyMap());
        prefs("app_config").edit().putBoolean("first_launch",true).putString("slot_map_native_test","keep-local").commit();
        DeveloperMode.setEnabled(app,false);
        JSONObject live=SettingsBackup.snapshot(app).getJSONObject("stores");
        SettingsBackup.stage(app,backup);assertStores(live); // Staging cannot reroute a running camera or recorder.
        SettingsBackup.restoreOnStartup(app);assertStores(expected);
        assertTrue(prefs("app_config").getBoolean("first_launch",false));
        assertEquals("keep-local",prefs("app_config").getString("slot_map_native_test",null));
        assertFalse(DeveloperMode.isUnlocked());
        assertTrue(new AppConfig(app).isGpuSide());
        DeveloperMode.setEnabled(app,true);assertFalse(new AppConfig(app).isPhotoViaJpegEnabled());
        assertEquals("restored",SettingsBackup.lastRestoreResult(app));assertFalse(SettingsBackup.pending(app).exists());
    }
    @Test public void malformedForeignFutureAndMistypedFilesNeverChangePreferences() throws Exception {
        JSONObject snapshot=SettingsBackup.snapshot(app),expected=snapshot.getJSONObject("stores");
        JSONObject foreign=new JSONObject(snapshot.toString()).put("kind","zeekr_shortcut_settings");
        assertEquals(SettingsBackup.Backup.Status.UNREADABLE,SettingsBackup.read(app,write(foreign)).status);
        JSONObject otherVersion=new JSONObject(snapshot.toString()).put("app_version","99.0.0");
        assertEquals(SettingsBackup.Backup.Status.OTHER_VERSION,SettingsBackup.read(app,write(otherVersion)).status);
        JSONObject format=new JSONObject(snapshot.toString()).put("format",99);
        assertEquals(SettingsBackup.Backup.Status.OTHER_VERSION,SettingsBackup.read(app,write(format)).status);
        JSONObject missing=new JSONObject(snapshot.toString());missing.getJSONObject("stores").remove("bird_eye_window");
        assertEquals(SettingsBackup.Backup.Status.UNREADABLE,SettingsBackup.read(app,write(missing)).status);
        JSONObject typed=new JSONObject(snapshot.toString());
        typed.getJSONObject("stores").getJSONObject("app_config").put("bad_type",new JSONObject().put("type","int").put("value",2.5));
        assertEquals(SettingsBackup.Backup.Status.UNREADABLE,SettingsBackup.read(app,write(typed)).status);
        File huge=File.createTempFile("companion-settings-large-",".json",app.getCacheDir());created.add(huge);
        try(RandomAccessFile out=new RandomAccessFile(huge,"rw")){out.setLength(1024*1024+1);}
        assertEquals(SettingsBackup.Backup.Status.UNREADABLE,SettingsBackup.read(app,huge).status);
        File truncated=write(snapshot);Files.write(truncated.toPath(),"{".getBytes(StandardCharsets.UTF_8));
        SettingsBackup.Backup bad=SettingsBackup.read(app,truncated);
        assertEquals(SettingsBackup.Backup.Status.UNREADABLE,bad.status);assertFalse(SettingsBackup.apply(app,bad));assertStores(expected);
    }
    @Test public void companion211BackupStillRestoresAfterTheCompanion3VersionChange() throws Exception {
        AppConfig config=new AppConfig(app);config.setGpuRenderingMode("custom");config.setGpuFeature("side",true);
        config.setSidePopupYaw(43);
        JSONObject snapshot=SettingsBackup.snapshot(app);
        JSONObject expected=snapshot.getJSONObject("stores");
        snapshot.put("app_version","2.11.64-alpha.1");
        SettingsBackup.Backup backup=SettingsBackup.read(app,write(snapshot));
        assertEquals(SettingsBackup.Backup.Status.OK,backup.status);
        config.setSidePopupYaw(20);config.setGpuFeature("side",false);
        SettingsBackup.stage(app,backup);SettingsBackup.restoreOnStartup(app);
        assertStores(expected);assertEquals(43,new AppConfig(app).getSidePopupYaw());
        assertTrue(new AppConfig(app).isGpuSide());
    }
    @Test public void interruptedRestoreRecoversAllStoresAndCommittedJournalDoesNotUndoSuccess() throws Exception {
        File file=export();SettingsBackup.Backup before=SettingsBackup.read(app,file);
        java.lang.reflect.Method method=SettingsBackup.class.getDeclaredMethod("journal",Context.class);method.setAccessible(true);
        SettingsRestoreTransaction.Journal journal=(SettingsRestoreTransaction.Journal)method.invoke(null,app);
        journal.write(new SettingsRestoreTransaction.Entry(false,before.values));
        for(String store:SettingsBackup.storeNames())prefs(store).edit().putString("interrupted_write","new").commit();
        SettingsBackup.restoreOnStartup(app);
        assertStores(new JSONObject(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8)).getJSONObject("stores"));
        journal.write(new SettingsRestoreTransaction.Entry(true,before.values));
        prefs("bird_eye_window").edit().putInt("completed_size",64).commit();
        SettingsBackup.restoreOnStartup(app);assertEquals(64,prefs("bird_eye_window").getInt("completed_size",0));
        assertFalse(SettingsBackup.recovery(app).exists());
    }
    @Test public void wrongTypesForAbsentKnownKeysAreRejectedBeforeStartupCanCommitThem() throws Exception {
        String[][] cases = {{"app_config","interface_theme"},{"app_config","video_storage_limit_gb"},
                {"profiles","current"},{"bird_eye_calibration","camera_2_pitch"},
                {"bird_eye_window","size"},{"bird_eye_appearance","vehicle_colour"}};
        for (String[] item : cases) {
            assertTrue(prefs(item[0]).edit().remove(item[1]).commit());
            JSONObject root = SettingsBackup.snapshot(app), before = root.getJSONObject("stores");
            JSONObject invalid = new JSONObject(root.toString());
            invalid.getJSONObject("stores").getJSONObject(item[0]).put(item[1],
                    new JSONObject().put("type","boolean").put("value",true));
            SettingsBackup.Backup backup = SettingsBackup.read(app,write(invalid));
            assertEquals(item[0]+"/"+item[1],SettingsBackup.Backup.Status.UNREADABLE,backup.status);
            assertFalse(SettingsBackup.apply(app,backup)); assertStores(before);
            // A pending snapshot written by an older version is checked again before replacement.
            Files.write(SettingsBackup.pending(app).toPath(),invalid.toString().getBytes(StandardCharsets.UTF_8));
            SettingsBackup.restoreOnStartup(app); assertStores(before);
            assertEquals("failed",SettingsBackup.lastRestoreResult(app));
        }
    }
    @Test public void exportNeverOverwritesAndDiagnosticsCarryEveryStoreWithoutTruncation() throws Exception {
        String detail=String.join("",Collections.nCopies(200,"x"));prefs("bird_eye_calibration").edit().putString("detail",detail).commit();
        File first=export(),second=export();assertNotEquals(first,second);
        assertEquals(SettingsBackup.Backup.Status.OK,SettingsBackup.read(app,first).status);
        assertEquals(SettingsBackup.Backup.Status.OK,SettingsBackup.read(app,second).status);
        JSONObject report=new JSONObject(DiagnosticsJson.build(app,"test report"));assertEquals(4,report.getInt("schema_version"));
        JSONObject stores=report.getJSONObject("settings").getJSONObject("stores");
        assertEquals(5,stores.length());assertEquals(detail,stores.getJSONObject("bird_eye_calibration").getJSONObject("detail").getString("value"));
        assertFalse(stores.has("developer_mode"));assertFalse(stores.has("recording_intent"));
    }
    private SettingsPreferenceFragment open(MainActivity activity,String section,String child) {
        SettingsShellFragment shell=new SettingsShellFragment();activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
        shell.showSection(section);shell.getChildFragmentManager().executePendingTransactions();
        SettingsPreferenceFragment page=(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
        if(child!=null){shell.onPreferenceStartScreen(page,page.findPreference(child));shell.getChildFragmentManager().executePendingTransactions();
            page=(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);}
        return page;
    }
    private void capture(String name) throws Exception {
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureRecommendationSettings")))return;
        SystemClock.sleep(300);android.graphics.Bitmap shot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try(FileOutputStream out=new FileOutputStream(new File(app.getExternalFilesDir(null),name+".png"))){assertTrue(shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{shot.recycle();}
    }
    @Test public void nativeSystemControlsExportAndCancelImportWithoutChangingCameraSettings() throws Exception {
        AppConfig config=new AppConfig(app);config.setRecordingFloatingEnabled(true);export();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            for(String theme:new String[]{"light","dark"}) {
                main(()->{config.setThemeMode(theme);AppTheme.apply(theme);});SystemClock.sleep(350);
                final SettingsPreferenceFragment[] page={null};scenario.onActivity(a->page[0]=open(a,"screen_system","sub_settings_backup"));
                capture("companion-backup-"+theme);
                JSONObject unchanged=SettingsBackup.snapshot(app).getJSONObject("stores");
                scenario.onActivity(a->{Preference row=page[0].findPreference("pref_settings_import");row.getOnPreferenceClickListener().onPreferenceClick(row);});
                onData(anything()).inRoot(isDialog()).atPosition(0).perform(click());
                capture("companion-import-confirm-"+theme);
                onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click());
                assertFalse(SettingsBackup.pending(app).exists());assertStores(unchanged);
                scenario.onActivity(a->{SettingsPreferenceFragment floating=open(a,"screen_floating",null);
                    floating.onDisplayPreferenceDialog(floating.findPreference("pref_floating_double_tap"));});
                capture("companion-lock-action-"+theme);
                onView(withText(R.string.action_cancel)).inRoot(isDialog()).perform(click());
            }
            scenario.onActivity(a->{SettingsPreferenceFragment page=open(a,"screen_system","sub_settings_backup");
                Preference row=page.findPreference("pref_settings_export");row.getOnPreferenceClickListener().onPreferenceClick(row);});
            onView(withText(R.string.action_ok)).inRoot(isDialog()).perform(click());
            for(File file:SettingsBackup.list(app))if(!existingExports.contains(file)&&!created.contains(file))created.add(file);
        }
    }
}
