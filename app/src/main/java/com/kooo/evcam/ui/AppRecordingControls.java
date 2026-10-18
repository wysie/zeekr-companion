package com.kooo.evcam.ui;
import android.app.*;import android.os.*;import android.view.View;import java.lang.ref.WeakReference;import java.util.*;
import com.kooo.evcam.R;import com.kooo.evcam.MainActivity;import com.kooo.evcam.recording.*;
/** Foreground presentation only. Never starts recording or changes saved overlay placement. */
public final class AppRecordingControls {
 private static WeakReference<Activity> active=new WeakReference<>(null);
 private static final Handler handler=new Handler(Looper.getMainLooper());private static final Set<Runnable> listeners=new LinkedHashSet<>();
 private static final Runnable tick=new Runnable(){public void run(){Activity a=active.get();if(a==null)return;
  View stop=a.findViewById(R.id.in_app_record_stop);RecordingCoordinator coordinator=RecordingCoordinator.get(a);boolean pending=coordinator.isWaitingForStorage();boolean recording=coordinator.isRecordingOrResuming()||pending;
  if(stop!=null){stop.setVisibility(recording?View.VISIBLE:View.GONE);stop.setContentDescription(a.getString(pending?R.string.record_cancel_usb_wait:R.string.record_stop));if(!stop.hasOnClickListeners())stop.setOnClickListener(v->RecordingCoordinator.get(a).stop(RecordingStops.Reason.USER));}
  for(Runnable listener:new ArrayList<>(listeners))listener.run();handler.postDelayed(this,500);}};
 private AppRecordingControls(){}
 public static boolean coversFloatingButton(){Activity a=active.get();return a!=null&&!a.isFinishing()&&
    (a instanceof MainActivity||a.findViewById(R.id.in_app_record_stop)!=null);}
 public static void addListener(Runnable listener){listeners.add(listener);listener.run();}
 public static void removeListener(Runnable listener){listeners.remove(listener);}
 public static void install(Application app){app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
  public void onActivityResumed(Activity a){active=new WeakReference<>(a);handler.removeCallbacks(tick);handler.post(tick);}
  public void onActivityPaused(Activity a){if(active.get()==a){active.clear();handler.removeCallbacks(tick);
   handler.postDelayed(()->{for(Runnable listener:new ArrayList<>(listeners))listener.run();},150);}}
  public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}
  public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
 });}
}
