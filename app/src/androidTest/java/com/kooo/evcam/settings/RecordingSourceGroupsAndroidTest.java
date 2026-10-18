package com.kooo.evcam.settings;

import android.Manifest;
import android.content.*;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.view.*;
import android.widget.CompoundButton;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.profile.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static org.hamcrest.Matchers.startsWith;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class RecordingSourceGroupsAndroidTest {
 private Context target; private AppConfig config; private Map<String,?> oldConfig,oldProfiles;
 @Before public void setup() throws Exception {
  assumeTrue(Build.PRODUCT.contains("sdk") || Build.HARDWARE.contains("ranchu"));
  target=InstrumentationRegistry.getInstrumentation().getTargetContext();
  oldConfig=target.getSharedPreferences("app_config",0).getAll();oldProfiles=target.getSharedPreferences("profiles",0).getAll();
  for(String p:new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE})
   InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(target.getPackageName(),p);
  try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("appops set "+target.getPackageName()+" SYSTEM_ALERT_WINDOW allow");java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
  target.getSharedPreferences("app_config",0).edit().clear().putBoolean("first_launch",false).putBoolean("language_chosen",true).putBoolean("rail_side_chosen",true).commit();
  config=new AppConfig(target);config.set009CameraMappingEnabled(true);config.setSidePopupEnabled(false);config.setRearViewEnabled(false);
  ProfileStore store=new ProfileStore(target);Profile profile=ProfileMigration.migrate(new ProfileMigration.Snapshot());RecordingSources.ensure(profile);
  for(CameraProfile c:profile.cameras)c.enabled=CameraProfile.ROLE_COMPOSITE.equals(c.role);
  store.select(profile.id);store.save(profile);
 }
 @SuppressWarnings("unchecked") private void restore(String name,Map<String,?> before) {
  SharedPreferences.Editor e=target.getSharedPreferences(name,0).edit().clear();
  for(Map.Entry<String,?> x:before.entrySet()){Object v=x.getValue();String k=x.getKey();
   if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof String)e.putString(k,(String)v);
   else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);
   else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));}
  e.commit();
 }
 @After public void cleanup(){if(target==null)return;InstrumentationRegistry.getInstrumentation().runOnMainSync(()->CameraManagerHolder.getInstance().release());restore("profiles",oldProfiles);restore("app_config",oldConfig);}
 private CompoundButton toggle(View v,int label) {
  if(v instanceof CompoundButton && target.getString(label).contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return (CompoundButton)v;
  if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){CompoundButton found=toggle(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}
  return null;
 }
 private ProfileEditorFragment install(MainActivity activity) {
  activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
  ProfileEditorFragment editor=new ProfileEditorFragment();activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,editor).commitNow();return editor;
 }
 private void screenshot(String name) throws Exception {
  InstrumentationRegistry.getInstrumentation().waitForIdleSync(); android.os.SystemClock.sleep(250);
  android.graphics.Bitmap shot=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
  try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(target.getCacheDir(),name))){shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{shot.recycle();}
 }
 @Test public void fourGroupsCanCombineAndManualMappingOpensItsPicker() throws Exception {
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   AtomicReference<ProfileEditorFragment> ref=new AtomicReference<>();
   scenario.onActivity(a->{ProfileEditorFragment e=install(a);ref.set(e);
    assertTrue(toggle(e.requireView(),R.string.record_group_surround).isChecked());
    toggle(e.requireView(),R.string.record_group_roads).performClick();
    Profile p=new ProfileStore(target).current();assertTrue(p.camera("composite").enabled);assertTrue(p.camera("roadfront").enabled);assertTrue(p.camera("roadrear").enabled);
    toggle(e.requireView(),R.string.record_group_cabin).performClick();p=new ProfileStore(target).current();assertTrue(p.camera("cabin1").enabled);assertFalse(p.camera("cabin2").enabled);
    toggle(e.requireView(),R.string.record_group_manual).performClick();assertFalse(new ProfileStore(target).current().camera("manual").enabled);
   });
   screenshot("recording-source-picker.png");
   onView(withText(R.string.action_cancel)).perform(click());
   scenario.onActivity(a->ref.get().selectCamera("roadfront"));
   screenshot("recording-source-groups.png");
   scenario.onActivity(a->{ProfileEditorFragment e=ref.get();toggle(e.requireView(),R.string.record_group_roads).performClick();
    Profile p=new ProfileStore(target).current();assertFalse(p.camera("roadfront").enabled);assertFalse(p.camera("roadrear").enabled);assertTrue(p.camera("composite").enabled);assertTrue(p.camera("cabin1").enabled);
   });
   onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(R.string.record_preview_layout)).perform(click());
   onView(withText(R.string.record_group_roads)).perform(click());
   onView(withText(R.string.action_ok)).perform(click());
   scenario.onActivity(a->{assertEquals("roads",config.getPreviewSourceMode());Profile p=new ProfileStore(target).current();assertFalse(p.camera("roadfront").enabled);assertEquals(2,ProfileChannels.of(p,"roads").previewKeys.size());});
  }
 }
 @Test public void roadPreviewBuildsBothRoadPanesWithoutEnablingTheirRecorders() throws Exception {
  config.setPreviewSourceMode("roads");
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   boolean[] ready={false};long until=android.os.SystemClock.elapsedRealtime()+10000;
   do {
    scenario.onActivity(a->{
     com.kooo.evcam.camera.MultiCameraManager manager=CameraManagerHolder.getInstance().getCameraManager();
     ready[0]=manager!=null&&manager.isBuilt()&&a.findViewById(R.id.composite_wrapper).getVisibility()==View.GONE;
     if(ready[0]) {
      assertEquals("0",manager.getCamera("roadfront").getCameraId());assertEquals("1",manager.getCamera("roadrear").getCameraId());
      assertEquals(Collections.singleton("front"),RecordSpecs.enabledCameraKeys(target));
      assertEquals(View.VISIBLE,a.findViewById(R.id.cabin_column).getVisibility());
     }
    });if(!ready[0])android.os.SystemClock.sleep(50);
   }while(!ready[0]&&android.os.SystemClock.elapsedRealtime()<until);
   assertTrue("Independent road preview attaches its actual camera sources",ready[0]);
   scenario.recreate();
   scenario.onActivity(a->assertEquals("roads",new AppConfig(a).getPreviewSourceMode()));
  }
 }
 @Test public void previewPickerPreservesProfileTuningAndCancelKeepsThePreviousChoice() throws Exception {
  config.setPreviewSourceMode("roads");
  Profile p=new ProfileStore(target).current();
  p.camera("composite").photo.jpegQuality=87;p.camera("composite").photo.resolution="1280x720";
  p.camera("composite").preview.resolution="1920x1080";
  new ProfileStore(target).save(p);
  Map<String,String> saved=new ProfileStore(target).current().toMap();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(this::install);
   onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(R.string.record_preview_layout)).perform(click());
   onView(withText(R.string.record_group_roads)).check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isChecked()));
   onView(withText(R.string.slot_road_rear)).perform(click());
   onView(withText(R.string.action_cancel)).perform(click());
   assertEquals("roads",config.getPreviewSourceMode());assertEquals(saved,new ProfileStore(target).current().toMap());
   onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(R.string.record_preview_layout)).perform(click());
   onView(withText(R.string.record_preview_surround_roads)).perform(click());
   onView(withText(R.string.action_ok)).perform(click());
   assertEquals("surround_roads",config.getPreviewSourceMode());assertEquals(saved,new ProfileStore(target).current().toMap());
   onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(R.string.record_preview_layout))
     .check(androidx.test.espresso.assertion.ViewAssertions.matches(withText(startsWith(target.getString(R.string.record_preview_layout)))));
   scenario.recreate();
   assertEquals("surround_roads",new AppConfig(target).getPreviewSourceMode());assertEquals(saved,new ProfileStore(target).current().toMap());
  }
 }
 @Test public void legacyPreviewChoicesRemainStoredAcrossVehicleSelectionsAndInvalidWrites() {
  android.content.SharedPreferences prefs=target.getSharedPreferences("app_config",0);
  for(String mode:new String[]{"selected","surround","roads","roadfront","roadrear","cabin"}) {
   prefs.edit().putString("preview_source_mode",mode).commit();
   assertEquals(mode,config.getPreviewSourceMode());
   config.setVehicleProfileMode("7x");assertEquals(mode,new AppConfig(target).getPreviewSourceMode());
   config.setVehicleProfileMode("009");assertEquals(mode,new AppConfig(target).getPreviewSourceMode());
   config.setPreviewSourceMode("unknown");assertEquals(mode,config.getPreviewSourceMode());
   assertEquals(mode,prefs.getString("preview_source_mode",null));
  }
  prefs.edit().remove("preview_source_mode").commit();assertEquals("selected",config.getPreviewSourceMode());
  assertFalse(prefs.contains("preview_source_mode")); // Merely reading/opening settings never migrates a default into storage.
 }
 @Test public void recordingSettingsExposeTheSavedPreviewWithoutMutatingRecorders() {
  config.setPreviewSourceMode("roadrear");
  Map<String,String> before=new ProfileStore(target).current().toMap();
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   scenario.onActivity(activity->{
    activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
    SettingsShellFragment shell=new SettingsShellFragment();
    activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
    shell.getChildFragmentManager().executePendingTransactions();shell.showSection("screen_recording");shell.getChildFragmentManager().executePendingTransactions();
    SettingsPreferenceFragment page=(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
    androidx.preference.ListPreference preview=page.findPreference("pref_preview_layout");
    assertNotNull(preview);assertEquals("roadrear",preview.getValue());assertFalse(preview.isPersistent());
    assertEquals(7,preview.getEntryValues().length);assertTrue(preview.getSummary().toString().contains(target.getString(R.string.slot_road_rear)));
    preview.callChangeListener("surround_roads");assertEquals("surround_roads",config.getPreviewSourceMode());
    assertEquals(target.getString(R.string.record_preview_surround_roads),preview.getSummary());
   });
   assertEquals(before,new ProfileStore(target).current().toMap());
  }
 }
 @Test public void combinedPreviewBuildsSurroundAndRoadPanesWithoutChangingRecorders() throws Exception {
  config.setPreviewSourceMode("surround_roads");
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   boolean[] ready={false};long until=android.os.SystemClock.elapsedRealtime()+10000;
   do {
    scenario.onActivity(a->{
     com.kooo.evcam.camera.MultiCameraManager manager=CameraManagerHolder.getInstance().getCameraManager();
     ready[0]=manager!=null&&manager.isBuilt()&&a.findViewById(R.id.composite_wrapper).getVisibility()==View.VISIBLE;
     if(ready[0]) {
      assertNotNull(manager.getCamera("front"));assertEquals("0",manager.getCamera("roadfront").getCameraId());assertEquals("1",manager.getCamera("roadrear").getCameraId());
      assertEquals(Collections.singleton("front"),RecordSpecs.enabledCameraKeys(target));
      assertEquals(View.VISIBLE,a.findViewById(R.id.cabin_column).getVisibility());
     }
    });if(!ready[0])android.os.SystemClock.sleep(50);
   }while(!ready[0]&&android.os.SystemClock.elapsedRealtime()<until);
   assertTrue("Surround and dedicated road previews share the configured pipeline",ready[0]);
  }
 }
 @Test public void genericPipelineKeepsDedicatedRoadSourcesAndTheirRecordingIdentity() {
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   Profile p=new ProfileStore(target).current();p.camera("roadfront").enabled=true;p.camera("roadrear").enabled=true;
   new ProfileStore(target).save(p);
   assertEquals(new java.util.LinkedHashSet<>(java.util.Arrays.asList("front","roadfront","roadrear")),RecordSpecs.enabledCameraKeys(target));
   com.kooo.evcam.camera.MultiCameraManager manager=new com.kooo.evcam.camera.MultiCameraManager(target);
   try {
    Map<String,String> ids=new LinkedHashMap<>();ids.put("front","3");ids.put("roadfront","0");ids.put("roadrear","1");ids.put("back","2");
    manager.setMaxOpenCameras(4);manager.initCameras(ids,Collections.emptyMap());
    assertEquals("0",manager.getCamera("roadfront").getCameraId());assertEquals("1",manager.getCamera("roadrear").getCameraId());
    assertEquals(4,manager.getCameraCount());assertTrue(manager.getDebugStats().contains(target.getString(R.string.slot_road_front)));
    assertFalse(RecordSpecs.storedAsGrid(target,"roadfront"));
    config.setSourceCameraId("cabin1","0");
    assertEquals(target.getString(R.string.slot_road_front),config.getCameraName(target,"back"));
   } finally {manager.release();}
  });
 }
 @Test public void sourceChoicesCancelCleanlyAndSurviveReopeningEditor() {
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
   AtomicReference<ProfileEditorFragment> ref=new AtomicReference<>();
   scenario.onActivity(a->{ProfileEditorFragment e=install(a);ref.set(e);e.selectCamera("manual");});
   onView(withText(R.string.record_source_change)).perform(scrollTo(),click());
   onView(withText(startsWith(target.getString(R.string.opt_camera_id,"0")))).perform(click());
   onView(withText(R.string.action_ok)).perform(click());
   scenario.onActivity(a->{assertEquals("0",config.getSourceCameraId("manual"));ref.get().selectCamera("manual");});
   onView(withText(R.string.record_source_change)).perform(scrollTo(),click());onView(withText(R.string.action_cancel)).perform(click());
   scenario.onActivity(a->{assertEquals("0",new AppConfig(target).getSourceCameraId("manual"));install(a).selectCamera("manual");});
  }
 }
}
