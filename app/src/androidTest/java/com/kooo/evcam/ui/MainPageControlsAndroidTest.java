package com.kooo.evcam.ui;

import android.Manifest;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.*;
import android.view.*;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.*;
import com.kooo.evcam.profile.*;
import com.kooo.evcam.zeekr.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class MainPageControlsAndroidTest {
    private Context target;
    private AppConfig config;
    private Map<String, Map<String, ?>> before = new HashMap<>();
    private int oldNight;
    @Before public void setup() {
        assumeTrue(Build.PRODUCT.contains("sdk") || Build.HARDWARE.contains("ranchu"));
        target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        oldNight = AppCompatDelegate.getDefaultNightMode();
        for (String name : new String[]{"app_config", "profiles", "bird_eye_window"}) before.put(name, target.getSharedPreferences(name, 0).getAll());
        for (String permission : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(target.getPackageName(), permission);
        target.getSharedPreferences("app_config", 0).edit().clear().putBoolean("first_launch", false)
                .putBoolean("language_chosen", true).putBoolean("rail_side_chosen", true).commit();
        config = new AppConfig(target); config.setAutoStartRecording(false); config.setSidePopupEnabled(false); config.setRearViewEnabled(false);
        config.setSidePopupYaw(50); config.setSidePopupPitch(15);
        config.setPreviewSourceMode("selected");
        Profile profile = ProfileMigration.migrate(new ProfileMigration.Snapshot()); RecordingSources.ensure(profile);
        for (CameraProfile camera : profile.cameras) camera.enabled = CameraProfile.ROLE_COMPOSITE.equals(camera.role);
        ProfileStore store = new ProfileStore(target); store.select(profile.id); store.save(profile);
        main(() -> CameraManagerHolder.getInstance().release());
    }
    private void main(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }
    @After public void cleanup() {
        if (target == null) return;
        main(() -> { BirdEyePopupService.stop(target); RearViewMirrorService.stop(target); CameraManagerHolder.getInstance().release(); });
        for (String name : before.keySet()) restore(name, before.get(name));
        main(() -> AppCompatDelegate.setDefaultNightMode(oldNight));
    }
    @SuppressWarnings("unchecked") private void restore(String name, Map<String, ?> data) {
        SharedPreferences.Editor e = target.getSharedPreferences(name, 0).edit().clear();
        for (Map.Entry<String, ?> x : data.entrySet()) {
            Object v=x.getValue(); String k=x.getKey();
            if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
            else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);
            else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,new HashSet<>((Set<String>)v));
        }
        assertTrue(e.commit());
    }
    private void await(BooleanSupplier condition) {
        long until=SystemClock.elapsedRealtime()+5000;
        while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(40);
        assertTrue("UI state must settle",condition.getAsBoolean());
    }
    private void capture(String name) throws Exception {
        if(!"true".equals(InstrumentationRegistry.getArguments().getString("captureMainPage")))return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync(); SystemClock.sleep(6500);
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(target.getExternalFilesDir(null),name+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}
        finally{image.recycle();}
    }
    @Test public void expandRestoreKeepsTexturesAndPipelineAndSurvivesRecreation() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            final android.view.TextureView[] texture={null}; final SurfaceTexture[] surface={null};
            final MultiCameraManager[] pipeline={null};
            final boolean[] ready={false};
            await(()->{scenario.onActivity(a->{texture[0]=a.findViewById(R.id.texture_front);ready[0]=texture[0].isAvailable();});return ready[0];});
            scenario.onActivity(a->{surface[0]=texture[0].getSurfaceTexture();pipeline[0]=CameraManagerHolder.getInstance().getCameraManager();a.findViewById(R.id.btn_preview_expand).performClick();});
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{assertEquals(View.GONE,a.findViewById(R.id.action_rail).getVisibility());assertTrue(texture[0].isAttachedToWindow());
                assertSame(surface[0],texture[0].getSurfaceTexture());assertSame(pipeline[0],CameraManagerHolder.getInstance().getCameraManager());
                assertTrue(a.findViewById(R.id.btn_preview_record).isShown());assertTrue(a.findViewById(R.id.expanded_camera_status).isShown());});
            capture("main-expanded");
            scenario.recreate();
            scenario.onActivity(a->{assertEquals(View.GONE,a.findViewById(R.id.action_rail).getVisibility());assertTrue(a.findViewById(R.id.btn_preview_record).isShown());});
            androidx.test.espresso.Espresso.pressBack();
            scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.action_rail).getVisibility());assertEquals(View.GONE,a.findViewById(R.id.btn_preview_record).getVisibility());});
            config.setPreviewSourceMode("surround_roads"); scenario.recreate();
            scenario.onActivity(a->{assertNotNull("Combined preview uses the multi-source layout",a.findViewById(R.id.cabin_column));
                a.findViewById(R.id.btn_preview_expand).performClick(); assertTrue(a.findViewById(R.id.btn_preview_record).isShown());
                a.findViewById(R.id.btn_preview_expand).performClick(); assertTrue(a.findViewById(R.id.btn_start_record).isShown());});
            assertEquals(50,config.getSidePopupYaw()); assertEquals(15,config.getSidePopupPitch());
        }
    }
    @Test public void mainPreviewPickerKeepsRecordingTuningAndShowsUnappliedChoice() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            Map<String,?> profile=target.getSharedPreferences("profiles",0).getAll();
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.record_group_roads)).perform(click());
            onView(withText(R.string.action_cancel)).perform(click());assertEquals("selected",config.getPreviewSourceMode());
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.record_group_roads)).perform(click());
            onView(withText(R.string.action_ok)).perform(click());
            assertEquals("roads",config.getPreviewSourceMode());assertEquals(profile,target.getSharedPreferences("profiles",0).getAll());
            scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.main_preview_pending).getVisibility());assertTrue(((TextView)a.findViewById(R.id.btn_preview_layout)).getText().toString().contains(a.getString(R.string.record_group_roads)));});
            assertEquals(50,config.getSidePopupYaw());assertFalse(com.kooo.evcam.recording.RecordingCoordinator.get(target).isRecordingOrResuming());
        }
    }
    @Test public void explicit360ActionsIgnoreLegacyShortcutDestinationAndTrackWindowClosure() {
        new BirdEyeWindowConfig(target).setPopup(true);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{a.findViewById(R.id.btn_bird_eye).performClick();assertFalse(BirdEyePopupService.isRunning());});
            scenario.onActivity(a->a.findViewById(R.id.btn_main_360_window).performClick());await(BirdEyePopupService::isRunning);
            final boolean[] selected={false};await(()->{scenario.onActivity(a->selected[0]=a.findViewById(R.id.btn_main_360_window).isSelected());return selected[0];});
            main(()->BirdEyePopupService.stop(target));await(()->!BirdEyePopupService.isRunning());
            await(()->{scenario.onActivity(a->selected[0]=a.findViewById(R.id.btn_main_360_window).isSelected());return !selected[0];});
            assertTrue(new BirdEyeWindowConfig(target).popup());assertEquals(50,config.getSidePopupYaw());
        }
    }
    @Test public void railActionsWrapWithoutShrinkingInBothThemesAndLongTranslations() {
        main(()->{
            for(String language:new String[]{"en","ms","zh"}) for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES})
                for(int width:new int[]{260,360}) {
                    Configuration cfg=new Configuration(target.getResources().getConfiguration());cfg.fontScale=1.3f;cfg.setLocale(Locale.forLanguageTag(language));cfg.uiMode=(cfg.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
                    Context c=new ContextThemeWrapper(target.createConfigurationContext(cfg),R.style.Theme_Cam);
                    View root=LayoutInflater.from(c).inflate(R.layout.layout_action_rail,null);
                    ((TextView)root.findViewById(R.id.btn_preview_layout)).setText(c.getString(R.string.main_preview_selection,c.getString(R.string.record_preview_surround_roads)));
                    ((TextView)root.findViewById(R.id.btn_main_mirror)).setText(c.getString(R.string.main_mirror_window)+"\n"+c.getString(R.string.main_window_paused));
                    ((TextView)root.findViewById(R.id.btn_main_360_window)).setText(c.getString(R.string.main_360_window)+"\n"+c.getString(R.string.main_window_closed));
                    int px=Math.round(width*c.getResources().getDisplayMetrics().density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY));root.layout(0,0,px,900);
                    for(int id:new int[]{R.id.record_title,R.id.btn_preview_layout,R.id.btn_take_photo,R.id.btn_video_playback,R.id.btn_photo_playback,R.id.btn_fisheye,R.id.btn_main_mirror,R.id.btn_main_360_window,R.id.btn_settings}){
                        TextView text=root.findViewById(id);String label=language+" "+width+" "+text.getText();assertNotNull(label,text.getLayout());int last=text.getLayout().getLineCount()-1;
                        assertEquals(label,0,text.getLayout().getEllipsisCount(last));assertEquals(label,text.getText().length(),text.getLayout().getLineEnd(last));
                        assertTrue(label,text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
                    }
                }
        });
    }
    @Test public void captureLightAndDarkMainPage() throws Exception {
        for(int mode:new int[]{AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES}) {
            main(()->AppCompatDelegate.setDefaultNightMode(mode));
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
                capture(mode==AppCompatDelegate.MODE_NIGHT_NO?"main-light":"main-dark");
                scenario.onActivity(a->{((android.widget.ScrollView)a.findViewById(R.id.main_actions_scroll)).fullScroll(View.FOCUS_DOWN);});
                capture(mode==AppCompatDelegate.MODE_NIGHT_NO?"main-light-controls":"main-dark-controls");
            }
        }
    }
}
