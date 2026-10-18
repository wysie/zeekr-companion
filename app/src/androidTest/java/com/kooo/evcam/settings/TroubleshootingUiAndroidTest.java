package com.kooo.evcam.settings;
import android.Manifest;import android.content.*;import android.os.Build;import android.view.View;import android.widget.TextView;
import androidx.preference.Preference;import androidx.preference.PreferenceGroup;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;import com.kooo.evcam.camera.CameraManagerHolder;import com.kooo.evcam.profile.ProfileStore;import com.kooo.evcam.telemetry.*;
import com.kooo.evcam.zeekr.*;import java.io.*;import java.util.*;import org.junit.*;import org.junit.runner.RunWith;
import static org.junit.Assert.*;import static org.junit.Assume.assumeTrue;import static androidx.test.espresso.Espresso.onView;import static androidx.test.espresso.action.ViewActions.click;import static androidx.test.espresso.matcher.ViewMatchers.withText;
@RunWith(AndroidJUnit4.class) public class TroubleshootingUiAndroidTest {
 private Context target;private AppConfig config;private Map<String,?> old,oldExports;private boolean developer;
 @Before public void setup(){assumeTrue(Build.PRODUCT.contains("sdk")||Build.HARDWARE.contains("ranchu"));target=InstrumentationRegistry.getInstrumentation().getTargetContext();
  old=target.getSharedPreferences("app_config",0).getAll();oldExports=target.getSharedPreferences("diagnostic_exports",0).getAll();developer=DeveloperMode.isUnlocked();DeveloperMode.setEnabled(target,false);
  for(String permission:new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE})InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(target.getPackageName(),permission);
  target.getSharedPreferences("app_config",0).edit().clear().putBoolean("first_launch",false).putBoolean("language_chosen",true).putBoolean("rail_side_chosen",true).commit();config=new AppConfig(target);config.setAutoStartRecording(false);config.setSidePopupEnabled(false);config.setRearViewEnabled(false);
 }
 @SuppressWarnings("unchecked") private void restore(String name,Map<String,?> before){SharedPreferences.Editor e=target.getSharedPreferences(name,0).edit().clear();for(Map.Entry<String,?> entry:before.entrySet()){Object v=entry.getValue();String k=entry.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));}e.commit();}
 @After public void cleanup(){if(target==null)return;InstrumentationRegistry.getInstrumentation().runOnMainSync(()->CameraManagerHolder.getInstance().release());restore("diagnostic_exports",oldExports);restore("app_config",old);DeveloperMode.setEnabled(target,developer);}
 private SettingsShellFragment install(MainActivity a,String section){a.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);SettingsShellFragment shell=new SettingsShellFragment();a.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();shell.getChildFragmentManager().executePendingTransactions();shell.showSection(section);shell.getChildFragmentManager().executePendingTransactions();return shell;}
 private SettingsPreferenceFragment page(SettingsShellFragment shell){return(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);}
 @Test public void troubleshootingAndUsbPageDoNotStartLoggingOrCameraResearch(){
  Map<String,String> before=new ProfileStore(target).current().toMap();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{SettingsShellFragment shell=install(a,"screen_system");assertNotNull(page(shell).findPreference("sub_troubleshooting"));
    shell.openDetail(SettingsPreferenceFragment.forSection("sub_troubleshooting"),a.getString(R.string.ui_troubleshooting));shell.getChildFragmentManager().executePendingTransactions();
    assertNotNull(page(shell).findPreference("pref_camera_status"));assertFalse(page(shell).findPreference("pref_camera_research").isVisible());
    page(shell).findPreference("pref_usb_session").performClick();shell.getChildFragmentManager().executePendingTransactions();
    UsbLogSessionFragment logs=(UsbLogSessionFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);Preference action=logs.findPreference("usb_session_action");assertFalse(action.isEnabled());assertEquals(a.getString(R.string.ui_start_logging),action.getTitle());
    assertFalse(DriveLogService.isEnabled());assertFalse(com.kooo.evcam.camera.CameraNeeds.current().developerProbe());assertEquals(before,new ProfileStore(target).current().toMap());
   });
  }
 }
 @Test public void hudGroupsAndScreenShortcutPreserveIndependentTuning(){
  config.setSidePopupEnabled(true);config.setSidePopupYaw(67);config.setHudSideYaw(35);config.setHudSideLeftPercent(17);config.setHudSideContrastPercent(122);config.setHudFollowingPopupPicture(true);
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{SettingsShellFragment shell=install(a,"screen_blind_spot_camera");shell.openDetail(SettingsPreferenceFragment.forSection("sub_side_hud"),a.getString(R.string.set_sub_hud_title));shell.getChildFragmentManager().executePendingTransactions();
    String[] groups={"group_hud_source","group_hud_custom_picture","group_hud_placement","group_hud_picture","group_hud_advanced"};PreferenceGroup root=page(shell).getPreferenceScreen();for(int i=0;i<groups.length;i++)assertEquals(groups[i],root.getPreference(i).getKey());
    assertFalse(page(shell).findPreference("group_hud_custom_picture").isVisible());assertTrue(page(shell).findPreference("pref_hud_edit_screen").getSummary().toString().contains("67"));
    page(shell).findPreference("pref_hud_edit_screen").performClick();shell.getChildFragmentManager().executePendingTransactions();assertNotNull(page(shell).findPreference("pref_side_popup_yaw"));
    assertEquals(17,config.getHudSideLeftPercent());assertEquals(122,config.getHudSideContrastPercent());assertEquals(67,config.getSidePopupYaw());
    config.setHudFollowingPopupPicture(false);assertEquals(35,config.getHudSideYaw());
   });
  }
 }
 @Test public void aboutLinksHaveFullTargetsAndPersistentCopyFallback(){
  try(ActivityScenario<AboutActivity> scenario=ActivityScenario.launch(AboutActivity.class)){
   scenario.onActivity(a->{int minimum=Math.round(48*a.getResources().getDisplayMetrics().density);assertTrue(a.findViewById(R.id.about_link_evcam).getMinimumHeight()>=minimum);assertTrue(a.findViewById(R.id.about_link_gpl).getMinimumHeight()>=minimum);com.kooo.evcam.ui.ExternalLinks.showCopyLink(a,AboutActivity.GPL_URL);});
   onView(withText(AboutActivity.GPL_URL)).check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));onView(withText(R.string.ui_copy_link)).perform(click());
   scenario.onActivity(a->{ClipboardManager clipboard=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);assertEquals(AboutActivity.GPL_URL,clipboard.getPrimaryClip().getItemAt(0).getText().toString());});
  }
 }
 @Test public void diagnosticExportPathSurvivesRecreationAndMissingFilesDisableSharing() throws Exception {
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->CameraManagerHolder.getInstance().release());File file=File.createTempFile("ux-diagnostics-",".json",target.getCacheDir());
  try{target.getSharedPreferences("diagnostic_exports",0).edit().clear().putString("path",file.getAbsolutePath()).commit();
   try(ActivityScenario<DiagnosticsActivity> scenario=ActivityScenario.launch(DiagnosticsActivity.class)){
    scenario.onActivity(a->{assertTrue(((TextView)a.findViewById(R.id.diagnostics_export_result)).getText().toString().contains(file.getAbsolutePath()));assertTrue(a.findViewById(R.id.diagnostics_share_saved).isEnabled());assertNull(CameraManagerHolder.getInstance().getCameraManager());});
    scenario.recreate();scenario.onActivity(a->{assertTrue(((TextView)a.findViewById(R.id.diagnostics_export_result)).getText().toString().contains(file.getAbsolutePath()));assertTrue(a.findViewById(R.id.diagnostics_copy_path).isClickable());});
    assertTrue(file.delete());scenario.recreate();scenario.onActivity(a->assertFalse(a.findViewById(R.id.diagnostics_share_saved).isEnabled()));
   }
  }finally{file.delete();}
 }
}
