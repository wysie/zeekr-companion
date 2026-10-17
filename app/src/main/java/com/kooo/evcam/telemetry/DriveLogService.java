package com.kooo.evcam.telemetry;

import android.app.*;
import android.content.*;
import android.os.*;
import android.system.Os;
import androidx.core.app.NotificationCompat;
import com.kooo.evcam.R;
import com.kooo.evcam.StorageHelper;
import com.kooo.evcam.settings.DeveloperMode;
import com.kooo.evcam.storage.StorageLayout;
import com.kooo.evcam.zeekr.DiagnosticsCollector;
import com.kooo.evcam.zeekr.DiagnosticsJson;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Explicit, read-only drive research. No boot start, camera ownership or internal fallback. */
public final class DriveLogService extends Service {
    public static final long MAX_RUN_MS=8*60*60*1000L;
    private static final String CHANNEL="drive_logging", STOP="drive_logging.stop", PREFS="drive_logging_status";
    private static final int NOTIFICATION=1093;
    private static volatile DriveLogService active;
    private static volatile boolean starting, cancelStart;
    private static volatile String state="", path="";
    private static final class Entry {
        final String channel,line;
        Entry(String c,String l){channel=c;line=l;}
    }
    private final ArrayBlockingQueue<Entry> queue=new ArrayBlockingQueue<>(1024);
    private final AtomicLong dropped=new AtomicLong(), sourceDropped=new AtomicLong();
    private final ScheduledExecutorService io=Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService snapshots=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private DriveLogFiles files;
    private volatile boolean capturing, stopping, finished;
    private volatile long startedWall, startedElapsed;
    private long lastSync;
    private ScheduledFuture<?> tick, finalTimeout;
    private boolean opening;

    public static boolean isEnabled(){return starting||active!=null;}
    public static boolean isStopping(){DriveLogService s=active;return s!=null&&s.stopping;}
    static boolean capturesSignals(){DriveLogService s=active;return s!=null&&s.capturing;}
    static long sessionStartedAt(){DriveLogService s=active;return s==null?0:s.startedWall;}
    static long remainingMs(){DriveLogService s=active;return s==null||!s.capturing?0:
            Math.max(0,MAX_RUN_MS-(SystemClock.elapsedRealtime()-s.startedElapsed));}
    public static String status(Context c){
        if(isEnabled())return state+(path.isEmpty()?"":"\n"+path);
        return c.getSharedPreferences(PREFS,MODE_PRIVATE).getString("last",c.getString(R.string.drive_log_idle));
    }
    public static synchronized void start(Context context){
        if(isEnabled()||!DeveloperMode.isUnlocked())return;
        starting=true;cancelStart=false;state=context.getString(R.string.drive_log_starting);path="";
        try {context.startForegroundService(new Intent(context,DriveLogService.class));}
        catch(RuntimeException e){starting=false;rememberFailure(context,e.toString());}
    }
    public static synchronized void stop(Context context){
        DriveLogService s=active;
        if(s!=null)s.requestStop("manual",true);
        else if(starting)cancelStart=true;
    }
    public static void developerModeChanged(Context context){if(!DeveloperMode.isUnlocked())stop(context);}
    public static void appLine(String line){offer("app",line);}
    public static void lifecycleLine(String line){offer("lifecycle",line);}
    static void signalLine(String line){offer("signals",line);}
    static void sourceEventDropped(){DriveLogService s=active;if(s!=null&&s.capturing)s.sourceDropped.incrementAndGet();}
    private static void offer(String channel,String line){
        DriveLogService s=active;
        if(s==null||!s.capturing)return;
        String bounded=line.length()>8192?line.substring(0,8192)+" [entry truncated at 8192 characters]":line;
        if(!s.queue.offer(new Entry(channel,bounded)))s.dropped.incrementAndGet();
    }
    private static void rememberFailure(Context c,String why){
        state=c.getString(R.string.drive_log_failed,why);
        c.getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("last",state+(path.isEmpty()?"":"\n"+path)).apply();
    }
    @Override public void onCreate(){super.onCreate();active=this;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(STOP.equals(intent==null?null:intent.getAction())){requestStop("notification stop",true);return START_NOT_STICKY;}
        try {
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,getString(R.string.drive_log_title),NotificationManager.IMPORTANCE_LOW));
            startForeground(NOTIFICATION,notification());
        }catch(RuntimeException e){fail(e);return START_NOT_STICKY;}
        if(cancelStart){requestStop("cancelled before start",false);return START_NOT_STICKY;}
        if(!opening&&files==null&&!capturing&&!stopping){
            opening=true;
            io.execute(this::open);
        }
        return START_NOT_STICKY;
    }
    private Notification notification(){
        PendingIntent stop=PendingIntent.getService(this,0,new Intent(this,DriveLogService.class).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent show=PendingIntent.getActivity(this,0,new Intent(this,com.kooo.evcam.MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.drive_log_title)).setContentText(state)
                .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(show)
                .addAction(0,getString(R.string.drive_log_stop),stop).build();
    }
    private void notifyState(){main.post(()->{
        if(!finished)getSystemService(NotificationManager.class).notify(NOTIFICATION,notification());
    });}
    private void open(){
        try {
            if(!DeveloperMode.isUnlocked()||stopping)throw new IOException("Cancelled before start");
            File root=StorageHelper.getExternalSdCardRoot(this);
            if(root==null)throw new IOException(getString(R.string.drive_log_no_usb));
            final File usb=root.getCanonicalFile();
            // Custom storage paths may point to primary/internal storage; reject those for USB-only logging.
            String primary=Environment.getExternalStorageDirectory().getCanonicalPath();
            String internal=getFilesDir().getCanonicalPath();
            if(!within(usb.getPath(),"/storage") && !within(usb.getPath(),"/mnt/media_rw")
                    || usb.getPath().equals("/storage") || usb.getPath().equals("/mnt/media_rw")
                    || within(usb.getPath(),"/storage/emulated") || within(usb.getPath(),"/storage/self")
                    || within(usb.getPath(),primary)||within(usb.getPath(),internal))
                throw new IOException(getString(R.string.drive_log_no_usb));
            final long device=Os.stat(usb.getPath()).st_dev;
            DriveLogFiles.Health health=()->{
                try {
                    if(!usb.isDirectory()||!usb.canWrite()||Os.stat(usb.getPath()).st_dev!=device)
                        throw new IOException("USB disconnected or replaced");
                    // Preserve actual free space for footage; never count or evict potentially clearable caches.
                    if(usb.getUsableSpace()<DriveLogFiles.FREE_RESERVE_BYTES)throw new IOException("Less than 64 MiB free on USB");
                }catch(android.system.ErrnoException e){throw new IOException("USB unavailable",e);}
            };
            files=new DriveLogFiles(StorageLayout.directory(usb,StorageLayout.Kind.LOG),health);
            path=files.directory.getAbsolutePath();startedWall=System.currentTimeMillis();startedElapsed=SystemClock.elapsedRealtime();
            files.line("session","# started "+startedWall+"; read-only drive logging; max 8 hours / 256 MiB; flush+sync every 2 seconds");
            files.line("session","# AppLog, lifecycle and observed vehicle callbacks only; no system-wide logcat or camera/video capture. Signals are research, not decoded blind-spot alerts.");
            files.line("session","# Each entry bounded to 8192 characters; vendor byte payloads retain the existing scanner bound. Queue losses are explicitly counted. No older session is deleted.");
            files.flush();
            if(stopping){finish("cancelled before capture",null);return;}
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("last",
                    getString(R.string.drive_log_failed,"Session interrupted; last files may be incomplete")+"\n"+path).apply();
            capturing=true;starting=false;state=getString(R.string.drive_log_recording);notifyState();
            tick=io.scheduleWithFixedDelay(this::drainAndCheck,0,100,TimeUnit.MILLISECONDS);
            SignalScanControl.apply(this);
            snapshot("start",false,"start");
        }catch(Exception e){fail(e);}
    }
    private static boolean within(String candidate,String parent){return candidate.equals(parent)||candidate.startsWith(parent+"/");}
    private void drainAndCheck(){
        if(finished)return;
        try {
            if(capturing&&!DeveloperMode.isUnlocked()){requestStop("developer mode disabled",true);return;}
            if(capturing&&remainingMs()==0){requestStop("8 hour limit",true);return;}
            drain();
            long now=SystemClock.elapsedRealtime();
            if(now-lastSync>=2000){files.flush();lastSync=now;}
        }catch(Exception e){fail(e);}
    }
    private void drain() throws IOException {
        Entry e;int count=0;while(count++<1024&&(e=queue.poll())!=null)files.line(e.channel,e.line);
        long lost=dropped.getAndSet(0),source=sourceDropped.getAndSet(0);
        if(lost>0||source>0)files.line("session","# GAP "+System.currentTimeMillis()+": writer queue dropped="+lost+", scanner queue dropped="+source);
    }
    private void snapshot(String phase,boolean closeAfter,String reason){
        snapshots.execute(()->{
            String json;
            try {json=DiagnosticsJson.build(this,DiagnosticsCollector.collect(this));}
            catch(Exception e){json="{\"error\":\"Diagnostics collection failed\"}";}
            final String result=json;
            try {io.execute(()->{
                if(finished)return;
                try {files.snapshot(phase,result);if(closeAfter)finish(reason,null);}
                catch(Exception e){fail(e);}
            });}catch(RejectedExecutionException ignored){ }
        });
    }
    private void requestStop(String reason,boolean withSnapshot){
        synchronized(this){if(stopping||finished)return;stopping=true;capturing=false;starting=false;}
        state=getString(R.string.drive_log_stopping);notifyState();
        SignalScanControl.apply(this);
        if(withSnapshot&&files!=null) {
            snapshot("stop",true,reason);
            finalTimeout=io.schedule(() -> { if (!finished) finish(reason,new IOException("Logs retained; stop Diagnostics snapshot timed out")); },15,TimeUnit.SECONDS);
        } else io.execute(()->finish(reason,null));
    }
    private void fail(Exception error){
        if(finished)return;
        capturing=false;stopping=true;starting=false;
        SignalScanControl.apply(this);
        // Errors remain visible in Developer options; never report a successful USB save.
        finish("error",error);
    }
    private void finish(String reason,Exception error){
        if(finished)return;
        capturing=false;stopping=true;
        try {if(files!=null){drain();files.line("session","# stopped "+System.currentTimeMillis()+" reason="+reason);files.flush();}}
        catch(Exception e){if(error==null)error=e;}
        try {if(files!=null)files.close();}catch(Exception e){if(error==null)error=e;}
        finished=true;starting=false;
        if(error!=null)rememberFailure(this,error.getMessage()==null?error.toString():error.getMessage());
        else {state=getString(files==null?R.string.drive_log_idle:R.string.drive_log_saved);
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("last",state+(path.isEmpty()?"":"\n"+path)).apply();}
        if(tick!=null)tick.cancel(false);
        if(finalTimeout!=null)finalTimeout.cancel(false);
        SignalScanControl.apply(this);
        main.post(()->{stopForeground(true);stopSelf();});
    }
    @Override public void onDestroy(){
        capturing=false;stopping=true;starting=false;
        SignalScanControl.apply(this);
        if(!finished){
            state=getString(R.string.drive_log_failed,"Service interrupted; session may be incomplete");
            getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("last",state+"\n"+path).apply();
            io.execute(()->{
                finish("service interrupted",new IOException("Service interrupted; session may be incomplete"));
                if(active==this)active=null;
            });
        } else if(active==this)active=null;
        io.shutdown();snapshots.shutdown();super.onDestroy();
    }
    @Override public void onTimeout(int startId,int fgsType){
        capturing=false;stopping=true;starting=false;
        SignalScanControl.apply(this);
        io.execute(() -> finish("Android foreground-service time limit",
                new IOException("Android service time limit; raw logs retained")));
        stopForeground(true);stopSelf();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
