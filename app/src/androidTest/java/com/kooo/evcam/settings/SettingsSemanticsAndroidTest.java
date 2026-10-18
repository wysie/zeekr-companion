package com.kooo.evcam.settings;
import android.Manifest;import android.content.*;import android.os.*;import android.view.View;
import androidx.preference.*;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;import com.kooo.evcam.*;import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.R;
import org.junit.*;import org.junit.runner.RunWith;import java.util.*;import static org.junit.Assert.*;import static org.junit.Assume.assumeTrue;
import static androidx.test.espresso.Espresso.onView;import static androidx.test.espresso.action.ViewActions.click;import static androidx.test.espresso.matcher.ViewMatchers.withText;
@RunWith(AndroidJUnit4.class) public class SettingsSemanticsAndroidTest {
 private Context target;private AppConfig config;private Map<String,?> old;private boolean developer;
 @Before public void setup(){assumeTrue(Build.PRODUCT.contains("sdk")||Build.HARDWARE.contains("ranchu"));
  target=InstrumentationRegistry.getInstrumentation().getTargetContext();SharedPreferences prefs=target.getSharedPreferences("app_config",0);old=prefs.getAll();developer=DeveloperMode.isUnlocked();
  for(String permission:new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE})InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(target.getPackageName(),permission);
  prefs.edit().clear().putBoolean("first_launch",false).putBoolean("language_chosen",true).putBoolean("rail_side_chosen",true).commit();
  config=new AppConfig(target);config.setAutoStartRecording(false);config.setSidePopupEnabled(false);config.setRearViewEnabled(false);
 }
 @SuppressWarnings("unchecked") @After public void cleanup(){if(target==null)return;InstrumentationRegistry.getInstrumentation().runOnMainSync(()->CameraManagerHolder.getInstance().release());
  SharedPreferences.Editor e=target.getSharedPreferences("app_config",0).edit().clear();for(Map.Entry<String,?> item:old.entrySet()){Object v=item.getValue();String k=item.getKey();
   if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));}e.commit();DeveloperMode.setEnabled(target,developer);
 }
 private SettingsShellFragment install(MainActivity a,String section){a.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);SettingsShellFragment shell=new SettingsShellFragment();
  a.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();shell.getChildFragmentManager().executePendingTransactions();shell.showSection(section);shell.getChildFragmentManager().executePendingTransactions();return shell;}
 private SettingsPreferenceFragment page(SettingsShellFragment shell){return(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);}
 @Test public void scopedHudResetsKeepIndependentPicturePlacementAndTone(){
  int defaultLeft=config.getHudSideLeftPercent();config.setSidePopupYaw(67);config.setHudFollowingPopupPicture(false);config.setHudSideYaw(35);
  config.setHudSideLeftPercent(17);config.setHudSideShapePercent(190);config.setHudSideContrastPercent(122);config.resetHudPicture();
  assertTrue(config.isHudFollowingPopupPicture());assertEquals(67,config.getSidePopupYaw());assertEquals(17,config.getHudSideLeftPercent());assertEquals(190,config.getHudSideShapePercent());assertEquals(122,config.getHudSideContrastPercent());
  config.setHudFollowingPopupPicture(false);config.setHudSideYaw(35);config.resetHudPlacement();
  assertFalse(config.isHudFollowingPopupPicture());assertEquals(35,config.getHudSideYaw());assertEquals(67,config.getSidePopupYaw());assertEquals(122,config.getHudSideContrastPercent());assertEquals(defaultLeft,config.getHudSideLeftPercent());assertFalse(config.isSidePopupEnabled());
 }
 @Test public void blindSpotResetRequiresNativeCancelOrExplicitConfirmation(){
  int defaultYaw=config.getSidePopupYaw();config.setSidePopupYaw(67);config.setHudSideLeftPercent(17);
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   final SettingsShellFragment[] shell={null};scenario.onActivity(a->{shell[0]=install(a,"screen_blind_spot_camera");Preference reset=page(shell[0]).findPreference("pref_side_popup_reset");assertEquals(R.layout.pref_row_action,reset.getLayoutResource());reset.performClick();});
   onView(withText(R.string.action_cancel)).perform(click());assertEquals(67,config.getSidePopupYaw());assertEquals(17,config.getHudSideLeftPercent());
   scenario.onActivity(a->page(shell[0]).scrollToPreference("pref_side_popup_reset"));
   onView(withText(R.string.set_side_popup_reset_title)).perform(click());onView(withText(R.string.ui_reset_confirm)).perform(click());
   assertEquals(defaultYaw,config.getSidePopupYaw());assertFalse(config.isSidePopupEnabled());
  }
 }
 @Test public void setupOrderActionsAndUpdateDestinationAreConsistent(){
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{SettingsShellFragment shell=install(a,"screen_recording");PreferenceGroup cameras=page(shell).findPreference("group_recording_cameras");
    String[] keys={"pref_vehicle_setup","pref_camera_sources","pref_profile_editor","pref_preview_layout"};for(int i=0;i<keys.length;i++)assertEquals(keys[i],cameras.getPreference(i).getKey());
    shell.showSection("screen_update");shell.getChildFragmentManager().executePendingTransactions();assertEquals(R.layout.pref_row_action,page(shell).findPreference("pref_check_update").getLayoutResource());
    SettingsHeadersFragment header=(SettingsHeadersFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_headers);assertEquals(a.getString(R.string.ui_updates_title),header.titleOf("screen_update"));
   });
  }
 }
 @Test public void unappliedPreviewChoiceStaysVisibleUntilPipelineReopen() {
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   final boolean[] built={false};long end=SystemClock.elapsedRealtime()+8000;
   do {scenario.onActivity(a->{com.kooo.evcam.camera.MultiCameraManager m=CameraManagerHolder.getInstance().getCameraManager();built[0]=m!=null&&m.isBuilt();});if(!built[0])SystemClock.sleep(50);}while(!built[0]&&SystemClock.elapsedRealtime()<end);
   assertTrue("Initial disposable pipeline built",built[0]);
   scenario.onActivity(a->{install(a,"screen_recording");config.setPreviewSourceMode("roadrear");});
   final boolean[] pending={false};end=SystemClock.elapsedRealtime()+4000;
   do {scenario.onActivity(a->pending[0]=a.findViewById(R.id.settings_pending_changes).getVisibility()==View.VISIBLE);if(!pending[0])SystemClock.sleep(50);}while(!pending[0]&&SystemClock.elapsedRealtime()<end);
   assertTrue("Saved choice remains visible as unapplied",pending[0]);
   scenario.recreate();scenario.onActivity(MainActivity::goToRecordingInterface);end=SystemClock.elapsedRealtime()+5000;
   do {scenario.onActivity(a->pending[0]=CameraManagerHolder.getInstance().hasPendingChannelChange(a));if(pending[0])SystemClock.sleep(50);}while(pending[0]&&SystemClock.elapsedRealtime()<end);
   assertFalse("New main-screen pipeline adopts the saved preview choice",pending[0]);
   assertFalse(com.kooo.evcam.recording.RecordingCoordinator.get(target).isRecordingOrResuming());
  }
 }
}
