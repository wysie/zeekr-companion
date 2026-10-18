package com.kooo.evcam.settings;
import android.os.Bundle;import android.os.Handler;import android.os.Looper;
import androidx.preference.*;import com.kooo.evcam.R;import com.kooo.evcam.telemetry.DriveLogService;
/** Explicit session controls; displaying this page never starts logging or probes cameras. */
public final class UsbLogSessionFragment extends PreferenceFragmentCompat {
 private final Handler handler=new Handler(Looper.getMainLooper());private Preference action,status,developer;
 private final Runnable tick=new Runnable(){public void run(){if(!isAdded())return;refresh();handler.postDelayed(this,1000);}};
 @Override public void onCreatePreferences(Bundle state,String root){PreferenceScreen screen=getPreferenceManager().createPreferenceScreen(requireContext());setPreferenceScreen(screen);
  Preference help=new Preference(requireContext());help.setTitle(R.string.ui_usb_session);help.setSummary(R.string.ui_usb_session_help);help.setSelectable(false);screen.addPreference(help);
  action=new Preference(requireContext());action.setKey("usb_session_action");PreferenceRows.markAction(action);screen.addPreference(action);
  action.setOnPreferenceClickListener(p->{if(DriveLogService.isEnabled())DriveLogService.stop(requireContext());else if(DeveloperMode.isUnlocked())DriveLogService.start(requireContext());refresh();return true;});
  status=new Preference(requireContext());status.setKey("usb_session_status");status.setSelectable(false);screen.addPreference(status);
  developer=new Preference(requireContext());developer.setTitle(R.string.set_developer_mode_title);developer.setSummary(R.string.ui_research_requires_developer);screen.addPreference(developer);
  developer.setOnPreferenceClickListener(p->{if(getParentFragment() instanceof SettingsShellFragment)((SettingsShellFragment)getParentFragment()).showSection("screen_system");return true;});
  PreferenceRows.apply(screen);refresh();}
 private void refresh(){action.setTitle(DriveLogService.isEnabled()?R.string.ui_stop_logging:R.string.ui_start_logging);action.setEnabled(DeveloperMode.isUnlocked()&&!DriveLogService.isStopping());
  status.setSummary(DriveLogService.status(requireContext()));developer.setVisible(!DeveloperMode.isUnlocked());}
 @Override public void onStart(){super.onStart();handler.post(tick);}
 @Override public void onStop(){handler.removeCallbacks(tick);super.onStop();}
 @Override public void onViewCreated(android.view.View view,Bundle state){super.onViewCreated(view,state);PreferenceRows.styleList(this,28,16);}
}
