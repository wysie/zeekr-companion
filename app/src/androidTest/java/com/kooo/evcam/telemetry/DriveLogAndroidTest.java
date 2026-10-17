package com.kooo.evcam.telemetry;

import android.Manifest;
import android.content.*;
import android.os.*;
import android.view.View;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.blackbox.BlackBox;
import com.kooo.evcam.settings.*;
import com.kooo.evcam.storage.StorageLayout;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

/** Disposable emulator with a virtual public USB volume; no owner drive is used. */
@RunWith(AndroidJUnit4.class)
public class DriveLogAndroidTest {
    private Context context;
    private Map<String,?> saved;
    private boolean developer;
    private AppConfig config;
    private File usb;
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private void waitFor(java.util.function.BooleanSupplier condition) {
        long end=SystemClock.uptimeMillis()+40000;
        while(!condition.getAsBoolean()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(50);
        assertTrue("Drive logging state did not settle: "+DriveLogService.status(context),condition.getAsBoolean());
    }
    @Before public void setup() throws Exception {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String fixture=InstrumentationRegistry.getArguments().getString("driveLogUsbUuid");
        Assume.assumeTrue("Requires explicitly provisioned disposable emulator USB UUID",
                fixture!=null && android.os.Build.PRODUCT.contains("sdk"));
        saved=context.getSharedPreferences("app_config",Context.MODE_PRIVATE).getAll();
        developer=DeveloperMode.isUnlocked();DeveloperMode.setEnabled(context,true);
        for(String permission:new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE})
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(),permission);
        context.getSharedPreferences("app_config",Context.MODE_PRIVATE).edit().putBoolean("first_launch",false)
                .putBoolean("language_chosen",true).putBoolean("rail_side_chosen",true)
                .putBoolean("auto_start_recording",false).putBoolean("drive_auto_start",false).commit();
        config=new AppConfig(context);config.setSignalScanEnabled(false);SignalScanControl.apply(context);
        for(StorageHelper.VolumeInfo volume:StorageHelper.listExternalVolumes(context))
            if(volume.root.getName().equals(fixture)&&!Environment.isExternalStorageEmulated(volume.root)){usb=volume.root;break;}
        assertNotNull("Provision a disposable emulator public USB volume first",usb);
        config.setCustomSdCardPath(usb.getAbsolutePath());StorageHelper.clearCache();
    }
    @After @SuppressWarnings("unchecked") public void restore() {
        if(saved==null)return;
        main(()->DriveLogService.stop(context));waitFor(()->!DriveLogService.isEnabled());
        SharedPreferences.Editor edit=context.getSharedPreferences("app_config",Context.MODE_PRIVATE).edit().clear();
        for(Map.Entry<String,?> e:saved.entrySet()){
            Object v=e.getValue();String k=e.getKey();
            if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);
            else if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Long)edit.putLong(k,(Long)v);
            else if(v instanceof Float)edit.putFloat(k,(Float)v);else if(v instanceof Set)edit.putStringSet(k,new HashSet<>((Set<String>)v));
        }
        edit.commit();DeveloperMode.setEnabled(context,developer);StorageHelper.clearCache();SignalScanControl.apply(context);
    }
    private File session(){return new File(DriveLogService.status(context).substring(DriveLogService.status(context).indexOf('\n')+1));}
    private String read(File dir,String prefix) throws Exception {
        StringBuilder text=new StringBuilder();File[] files=dir.listFiles((d,n)->n.startsWith(prefix));
        if(files!=null){Arrays.sort(files);for(File f:files)text.append(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));}
        return text.toString();
    }
    @Test public void liveUsbFilesSurviveActivityClosureAndCapturePastPrivateLimit() throws Exception {
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)) {
            activity.onActivity(a->DriveLogService.start(a));
            waitFor(DriveLogService::capturesSignals);
        }
        assertTrue("Activity closure must not stop logging",DriveLogService.capturesSignals());
        File first=session();assertTrue(first.getPath().startsWith(new File(usb,StorageLayout.ROOT).getPath()));
        AppLog.i("DriveLogTest","app-marker");BlackBox.noteImportant("lifecycle-marker");
        Constructor<SignalScan> ctor=SignalScan.class.getDeclaredConstructor(Context.class,Runnable.class);ctor.setAccessible(true);
        SignalScan scanner=ctor.newInstance(context,(Runnable)()->{});
        Field lines=SignalScan.class.getDeclaredField("lines");lines.setAccessible(true);lines.setInt(scanner,SignalScan.MAX_LINES);
        StringWriter privateSummary=new StringWriter();Field out=SignalScan.class.getDeclaredField("out");out.setAccessible(true);out.set(scanner,privateSummary);
        Method event=SignalScan.class.getDeclaredMethod("logLine",long.class,String.class,String.class,String.class);event.setAccessible(true);
        for(int i=0;i<250;i++)event.invoke(scanner,System.currentTimeMillis(),"TEST:0x1",Integer.toString(i),"synthetic-warning-marker");
        DriveLogService.sourceEventDropped();
        SystemClock.sleep(2400);
        assertTrue("Signals must be flushed while mode is still on",read(first,"signals-").contains("synthetic-warning-marker"));
        assertEquals("Private summary remains capped","",privateSummary.toString());
        main(()->DriveLogService.stop(context));waitFor(()->!DriveLogService.isEnabled());
        assertTrue(DriveLogService.status(context),DriveLogService.status(context).startsWith(context.getString(R.string.drive_log_saved)));
        assertTrue(read(first,"app-").contains("app-marker"));assertTrue(read(first,"lifecycle-").contains("lifecycle-marker"));
        assertTrue(read(first,"session-").contains("scanner queue dropped=1"));
        assertTrue(new File(first,"diagnostics-start.json").isFile());assertTrue(new File(first,"diagnostics-stop.json").isFile());
        assertEquals(250,read(first,"signals-").split("synthetic-warning-marker",-1).length-1);
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->DriveLogService.start(a));waitFor(DriveLogService::capturesSignals);
            assertNotEquals(first,session());main(()->DriveLogService.stop(context));waitFor(()->!DriveLogService.isEnabled());
        }
        assertTrue(new File(first,"diagnostics-stop.json").isFile());
        assertFalse("Drive mode must not turn on the separate manual scan preference",config.isSignalScanEnabled());
        assertFalse(SignalScan.isRunning());
    }
    @Test public void internalPathIsRejectedAndSettingsReflectActualFailure() throws Exception {
        config.setCustomSdCardPath(Environment.getExternalStorageDirectory().getPath());StorageHelper.clearCache();
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->{
                a.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                SettingsShellFragment shell=new SettingsShellFragment();
                a.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions();
                try { Method show=SettingsShellFragment.class.getDeclaredMethod("showSection",String.class);
                    show.setAccessible(true);show.invoke(shell,"screen_developer");
                } catch(Exception e) {throw new AssertionError(e);}
                shell.getChildFragmentManager().executePendingTransactions();
                DriveLogService.start(a);
            });waitFor(()->!DriveLogService.isEnabled());
            assertTrue(DriveLogService.status(context),DriveLogService.status(context).contains(context.getString(R.string.drive_log_no_usb)));
            SystemClock.sleep(1100);
            activity.onActivity(a->{
                SettingsShellFragment shell=(SettingsShellFragment)a.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                SettingsPreferenceFragment page=(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
                SwitchPreferenceCompat toggle=page.findPreference("pref_drive_logging");assertFalse(toggle.isChecked());
                assertTrue(page.findPreference("pref_drive_logging_status").getSummary().toString().contains(context.getString(R.string.drive_log_no_usb)));
            });
        }
    }
    @Test public void disablingDeveloperModeFinishesDriveSession() throws Exception {
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->DriveLogService.start(a));waitFor(DriveLogService::capturesSignals);
            main(()->DeveloperMode.setEnabled(context,false));waitFor(()->!DriveLogService.isEnabled());
            assertFalse(SignalScan.isRunning());
        }
    }
    @Test public void independentManualScannerRemainsEnabledAfterDriveStops() throws Exception {
        config.setSignalScanEnabled(true);SignalScanControl.apply(context);
        long since=config.signalScanSessionStartedAt();
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->DriveLogService.start(a));waitFor(DriveLogService::capturesSignals);
            main(()->DriveLogService.stop(context));waitFor(()->!DriveLogService.isEnabled());
            assertTrue(config.isSignalScanEnabled());assertEquals(since,config.signalScanSessionStartedAt());
            assertTrue(SignalScan.isRunning());
        }
        config.setSignalScanEnabled(false);SignalScanControl.apply(context);
    }
    @Test public void manualScanExpiresWhileDriveSessionKeepsCollecting() throws Exception {
        config.setSignalScanEnabled(true);
        context.getSharedPreferences("app_config",Context.MODE_PRIVATE).edit().putLong("signal_scan_since",
                System.currentTimeMillis()-SignalScan.MAX_RUN_MS+3000).commit();
        SignalScanControl.apply(context);
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->DriveLogService.start(a));waitFor(DriveLogService::capturesSignals);
            waitFor(()->!config.isSignalScanEnabled());
            assertTrue("Drive owner must survive manual owner expiry",DriveLogService.capturesSignals());
            assertTrue(SignalScan.isRunning());
            main(()->DriveLogService.stop(context));waitFor(()->!DriveLogService.isEnabled());
        }
    }
    @Test public void privateSummaryFailureDoesNotPreventUsbSignalEvidence() throws Exception {
        File sample=new File(context.getFilesDir(),"signal_scan.txt"),backup=new File(context.getFilesDir(),"signal_scan.drive-test-backup");
        boolean existed=sample.isFile();if(existed)assertTrue(sample.renameTo(backup));
        assertTrue(sample.mkdir()); // Deliberately make the private summary unwritable as a file.
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->DriveLogService.start(a));waitFor(DriveLogService::capturesSignals);
            File folder=session();SystemClock.sleep(2400);
            assertTrue(read(folder,"signals-").contains("private summary unavailable"));
            main(()->DriveLogService.stop(context));waitFor(()->!DriveLogService.isEnabled());
        }finally{
            assertTrue(sample.delete());if(existed)assertTrue(backup.renameTo(sample));
        }
    }

    @Test public void immediateStopCancelsPendingServiceStart() throws Exception {
        try(ActivityScenario<MainActivity> activity=ActivityScenario.launch(MainActivity.class)){
            activity.onActivity(a->{DriveLogService.start(a);DriveLogService.stop(a);});
            waitFor(()->!DriveLogService.isEnabled());
            assertFalse(DriveLogService.capturesSignals());
            assertFalse(SignalScan.isRunning());
        }
    }

}
