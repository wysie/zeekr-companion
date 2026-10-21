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
    @Test public void playbackHomeReturnsToCamerasAndKeepsManualStop() throws Exception {
        com.kooo.evcam.recording.RecordingIntent choice = com.kooo.evcam.recording.RecordingIntent.current();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(new Intent(target, MainActivity.class))) {
            choice.noteUserStopped();
            for (Class<?> viewer : new Class<?>[]{com.kooo.evcam.playback.PhotoPlaybackActivity.class,
                    com.kooo.evcam.zeekr.TimelinePlayerActivity.class}) {
                scenario.onActivity(a -> {
                    a.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                    a.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
                            new com.kooo.evcam.settings.SettingsShellFragment()).commitNow();
                    a.startActivity(new Intent(a, com.kooo.evcam.playback.GalleryActivity.class));
                });
                final android.app.Activity[] gallery = {null};
                await(() -> { main(() -> {
                    for (android.app.Activity a : androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                            .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))
                        if (a instanceof com.kooo.evcam.playback.GalleryActivity) gallery[0] = a;
                }); return gallery[0] != null; });
                main(() -> gallery[0].startActivity(new Intent(gallery[0], viewer)));
                onView(withId(R.id.pb_home)).perform(click());
                await(() -> gallery[0].isDestroyed());
                onView(withId(R.id.btn_start_record)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
                scenario.onActivity(a -> {
                    assertEquals(View.GONE, a.findViewById(R.id.fragment_container).getVisibility());
                    assertTrue(a.findViewById(R.id.btn_start_record).isShown());
                    assertTrue(choice.stoppedByUser());
                    assertFalse(com.kooo.evcam.recording.RecordingCoordinator.get(a).isRecordingOrResuming());
                });
            }
        } finally { choice.reset(); }
    }

    @Test public void singleSurroundExplainsLayoutAndEquivalentChoiceDoesNotNeedReopen() throws Exception {
        config.setMainPreviewArrangement("stacked");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Map<String, ?> profile = target.getSharedPreferences("profiles", 0).getAll();
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.home_layout_single)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            onView(withText(R.string.home_layout_stacked)).check(androidx.test.espresso.assertion.ViewAssertions.matches(org.hamcrest.Matchers.not(isDisplayed())));
            onView(withText(R.string.home_surround_four)).perform(click());
            capture("main-single-surround-panel");
            onView(withText(R.string.home_preview_save)).perform(click());
            assertEquals("surround", config.getPreviewSourceMode());
            assertEquals("stacked", config.getMainPreviewArrangement());
            assertEquals(profile, target.getSharedPreferences("profiles", 0).getAll());
            scenario.onActivity(a -> {
                assertEquals(View.GONE, a.findViewById(R.id.main_preview_pending).getVisibility());
                assertTrue(((TextView)a.findViewById(R.id.btn_preview_layout)).getText().toString().contains(a.getString(R.string.record_group_surround)));
            });
        }
    }

    @Test public void recordingDetailsAreSeparateAndKeepTheRecordAction() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertFalse(com.kooo.evcam.recording.RecordingCoordinator.get(target).isRecordingOrResuming());
            onView(withId(R.id.btn_start_record)).perform(androidx.test.espresso.action.ViewActions.longClick());
            onView(withText(R.string.home_recording_details)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            assertFalse(com.kooo.evcam.recording.RecordingCoordinator.get(target).isRecordingOrResuming());
            capture("main-recording-details");
            onView(withText(R.string.action_close)).perform(click());
            onView(withId(R.id.btn_start_record)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
        }
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
            scenario.onActivity(a -> {
                assertNull("UI recreation retains applied sources until Apply", a.findViewById(R.id.cabin_column));
                com.kooo.evcam.camera.CameraChanges.request(a);
            });
            long appliedDeadline = SystemClock.elapsedRealtime() + 35000;
            while ((com.kooo.evcam.camera.CameraChanges.busy() || CameraManagerHolder.getInstance().hasPendingChannelChange(target))
                    && SystemClock.elapsedRealtime() < appliedDeadline) SystemClock.sleep(50);
            assertFalse("Source Apply finished", com.kooo.evcam.camera.CameraChanges.busy());
            assertFalse(CameraManagerHolder.getInstance().hasPendingChannelChange(target));
            scenario.onActivity(a->{assertNotNull("Applied combined preview uses the multi-source layout",a.findViewById(R.id.cabin_column));
                a.findViewById(R.id.btn_preview_expand).performClick(); assertTrue(a.findViewById(R.id.btn_preview_record).isShown());
                a.findViewById(R.id.btn_preview_expand).performClick(); assertTrue(a.findViewById(R.id.btn_start_record).isShown());});
            assertEquals(50,config.getSidePopupYaw()); assertEquals(15,config.getSidePopupPitch());
        }
    }
    @Test public void mainPreviewPickerKeepsRecordingTuningAndShowsUnappliedChoice() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            Map<String,?> profile=target.getSharedPreferences("profiles",0).getAll();
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.record_group_roads)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            onView(withText(R.string.action_cancel)).perform(click());assertEquals("selected",config.getPreviewSourceMode());
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.record_group_roads)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            onView(withText(R.string.home_preview_save)).perform(click());
            assertEquals("roads",config.getPreviewSourceMode());assertEquals(profile,target.getSharedPreferences("profiles",0).getAll());
            capture("main-preview-pending");
            scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.main_preview_pending).getVisibility());assertTrue(((TextView)a.findViewById(R.id.main_preview_pending)).getText().toString().contains(a.getString(R.string.record_group_roads)));
                assertTrue(((TextView)a.findViewById(R.id.btn_preview_layout)).getText().toString().contains(
                        com.kooo.evcam.settings.PreviewLayoutPicker.label(a, "selected")));});
            assertEquals(50,config.getSidePopupYaw());assertFalse(com.kooo.evcam.recording.RecordingCoordinator.get(target).isRecordingOrResuming());
        }
    }
    @Test public void compactDockKeepsEqualReadableUtilitiesAcrossDriverSideChanges() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            int[] ids = {R.id.btn_take_photo, R.id.btn_main_gallery, R.id.btn_main_windows, R.id.btn_fisheye};
            for (String side : new String[]{"right", "left", "right"}) {
                scenario.onActivity(a -> {
                    for (int id : ids) a.findViewById(id).setVisibility(View.VISIBLE);
                    MainPageControls.applyDriverSide(a, side);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    int width = a.findViewById(ids[0]).getWidth(); assertTrue(width > 0);
                    View recording = a.findViewById(R.id.btn_start_record);
                    for (int id : ids) {
                        TextView action = a.findViewById(id);
                        assertEquals(width, action.getWidth());
                        int last = action.getLayout().getLineCount() - 1;
                        assertEquals(action.getText().length(), action.getLayout().getLineEnd(last));
                        assertEquals(0, action.getLayout().getEllipsisCount(last));
                        assertTrue(action.getHeight() >= 48 * a.getResources().getDisplayMetrics().density);
                        assertTrue("Driver-side recording anchor", "left".equals(side)
                                ? recording.getLeft() < action.getLeft() : recording.getLeft() > action.getLeft());
                    }
                    assertBalancedToolbar(a);
                });
            }
        }
    }

    private void assertBalancedToolbar(MainActivity activity) {
        android.widget.LinearLayout actions=activity.findViewById(R.id.main_dock_actions);
        int gap=Math.round(8*activity.getResources().getDisplayMetrics().density),end=0,count=0;
        for(int n=0;n<actions.getChildCount();n++) {
            View child=actions.getChildAt(n);if(child.getVisibility()==View.GONE)continue;
            assertEquals("No flexible empty gap between actions",count==0?0:gap,child.getLeft()-end);
            end=child.getRight();count++;
        }
        int viewport=activity.findViewById(R.id.action_rail).getWidth();
        assertTrue("Fill available space; only pixel-rounding remainder is allowed",end>=viewport-count);
    }

    @Test public void compactStatusOpensDetailsAndRoadToolbarFillsSpaceWithoutStraighten() {
        config.setPreviewSourceMode("roadfront");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            final MultiCameraManager[] pipeline={null};
            scenario.onActivity(a->{
                pipeline[0]=CameraManagerHolder.getInstance().getCameraManager();
                View status=a.findViewById(R.id.camera_status_chip);
                assertTrue(status.isClickable());assertTrue(status.isFocusable());
                assertTrue(status.getHeight()>=48*a.getResources().getDisplayMetrics().density);
                assertTrue("Status should fit its label, not fill half the header",
                        status.getWidth()<a.findViewById(R.id.main_header).getWidth()/2);
                assertEquals(View.GONE,a.findViewById(R.id.btn_fisheye).getVisibility());
                assertBalancedToolbar(a);
            });
            onView(withId(R.id.camera_status_chip)).perform(click());
            onView(withId(R.id.camera_status_report)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            androidx.test.espresso.Espresso.pressBack();
            scenario.onActivity(a->assertSame(pipeline[0],CameraManagerHolder.getInstance().getCameraManager()));
        }
    }
    @Test public void explicit360ActionsIgnoreLegacyShortcutDestinationAndTrackWindowClosure() {
        new BirdEyeWindowConfig(target).setPopup(true);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{a.findViewById(R.id.btn_bird_eye).performClick();assertFalse(BirdEyePopupService.isRunning());});
            onView(withId(R.id.btn_main_windows)).perform(click());
            onView(withId(R.id.btn_main_360_window)).perform(click());await(BirdEyePopupService::isRunning);
            final boolean[] selected={false};
            await(()->{onView(withId(R.id.btn_main_360_window)).check((view,error)->{if(error!=null)throw error;selected[0]=view.isSelected();});return selected[0];});
            main(()->BirdEyePopupService.stop(target));await(()->!BirdEyePopupService.isRunning());
            await(()->{onView(withId(R.id.btn_main_360_window)).check((view,error)->{if(error!=null)throw error;selected[0]=view.isSelected();});return !selected[0];});
            assertTrue(new BirdEyeWindowConfig(target).popup());assertEquals(50,config.getSidePopupYaw());
        }
    }
    @Test public void homeControlsFitBothThemesAndLongTranslations() {
        main(()->{
            for(String language:new String[]{"en","ms","zh"}) for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES})
                for(int width:new int[]{900,1280}) for(int layout:new int[]{R.layout.activity_main_zeekr_7x,R.layout.activity_main_zeekr_7x_multi}) {
                    Configuration cfg=new Configuration(target.getResources().getConfiguration());cfg.fontScale=1.3f;cfg.setLocale(Locale.forLanguageTag(language));cfg.uiMode=(cfg.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
                    Context c=new ContextThemeWrapper(target.createConfigurationContext(cfg),R.style.Theme_Cam);
                    View root=LayoutInflater.from(c).inflate(layout,null);
                    ((TextView)root.findViewById(R.id.btn_preview_layout)).setText(c.getString(R.string.main_preview_selection,c.getString(R.string.record_preview_surround_roads)));
                    new RecordButtonUi(root.findViewById(R.id.btn_start_record)).setState(RecordButtonUi.State.WAITING);
                    root.findViewById(R.id.vehicle_status_panel).setVisibility(View.VISIBLE);
                    int px=Math.round(width*c.getResources().getDisplayMetrics().density), height=Math.round(700*c.getResources().getDisplayMetrics().density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,px,height);
                    for(int id:new int[]{R.id.record_title,R.id.record_detail,R.id.main_brand,R.id.btn_preview_layout,R.id.btn_bird_eye,R.id.btn_take_photo,R.id.btn_main_gallery,R.id.btn_main_windows,R.id.btn_fisheye}){
                        TextView text=root.findViewById(id);String label=language+" "+width+" "+text.getText();assertNotNull(label,text.getLayout());int last=text.getLayout().getLineCount()-1;
                        assertEquals(label,0,text.getLayout().getEllipsisCount(last));assertEquals(label,text.getText().length(),text.getLayout().getLineEnd(last));
                        assertTrue(label,text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
                    }
                    assertTrue("Preview retains useful height",root.findViewById(R.id.preview_stage).getHeight()>=200*c.getResources().getDisplayMetrics().density);
                }
        });
    }
    @Test public void arrangementSaveKeepsSurfacesProfilesAndCancelIsReadOnly() {
        config.setPreviewSourceMode("surround_roads");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            final android.view.TextureView[] texture={null}; final SurfaceTexture[] surface={null}; final MultiCameraManager[] pipeline={null};
            final boolean[] ready={false};
            await(()->{scenario.onActivity(a->{texture[0]=a.findViewById(R.id.texture_front);ready[0]=texture[0].isAvailable();});return ready[0];});
            scenario.onActivity(a->{surface[0]=texture[0].getSurfaceTexture();pipeline[0]=CameraManagerHolder.getInstance().getCameraManager();});
            Map<String,?> profile=target.getSharedPreferences("profiles",0).getAll();
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.home_layout_stacked)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            onView(withText(R.string.action_cancel)).perform(click());
            assertEquals("auto",config.getMainPreviewArrangement());
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.home_layout_stacked)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            onView(withText(R.string.home_preview_save)).perform(click());
            assertEquals("stacked",config.getMainPreviewArrangement());assertEquals("surround_roads",config.getPreviewSourceMode());
            assertEquals(profile,target.getSharedPreferences("profiles",0).getAll());
            scenario.onActivity(a->{assertSame(surface[0],texture[0].getSurfaceTexture());assertSame(pipeline[0],CameraManagerHolder.getInstance().getCameraManager());
                assertEquals(android.widget.LinearLayout.VERTICAL,((android.widget.LinearLayout)a.findViewById(R.id.main_source_panes)).getOrientation());
                assertEquals(View.GONE,a.findViewById(R.id.main_preview_pending).getVisibility());});
            scenario.recreate();
            scenario.onActivity(a->assertEquals(android.widget.LinearLayout.VERTICAL,((android.widget.LinearLayout)a.findViewById(R.id.main_source_panes)).getOrientation()));
        }
    }
    @Test public void roadOnlyArrangementLabelsMatchTheirGeometryWithoutReplacingTextures() {
        config.setPreviewSourceMode("roads");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            final boolean[] ready={false};
            await(()->{scenario.onActivity(a->ready[0]=a.findViewById(R.id.composite_wrapper).getVisibility()==View.GONE);return ready[0];});
            scenario.onActivity(a->{
                android.widget.LinearLayout others=a.findViewById(R.id.cabin_column);
                final java.util.List<android.view.TextureView> textures=new java.util.ArrayList<>();
                collectTextures(others,textures);
                java.util.List<SurfaceTexture> surfaces=new java.util.ArrayList<>();
                for(android.view.TextureView view:textures)surfaces.add(view.getSurfaceTexture());
                MultiCameraManager pipeline=CameraManagerHolder.getInstance().getCameraManager();
                for(String layout:new String[]{"equal","stacked","auto"}) {
                    config.setMainPreviewArrangement(layout);a.applyMainPreviewArrangement();
                    assertEquals("Road-only "+layout,"equal".equals(layout)?android.widget.LinearLayout.HORIZONTAL:android.widget.LinearLayout.VERTICAL,others.getOrientation());
                    for(int i=0;i<textures.size();i++){assertTrue(textures.get(i).isAttachedToWindow());assertSame(surfaces.get(i),textures.get(i).getSurfaceTexture());}
                    assertSame(pipeline,CameraManagerHolder.getInstance().getCameraManager());
                }
            });
        }
    }
    private static void collectTextures(View view, java.util.List<android.view.TextureView> out) {
        if(view instanceof android.view.TextureView)out.add((android.view.TextureView)view);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collectTextures(((ViewGroup)view).getChildAt(i),out);
    }
    @Test public void otherSourceSelectionIsStagedAndKeepsRecordingMapping() {
        config.setPreviewSourceMode("roadfront");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            Map<String,?> profile=target.getSharedPreferences("profiles",0).getAll();
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.slot_road_front)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            onView(withText(R.string.slot_road_rear)).perform(click());
            onView(withText(R.string.action_cancel)).perform(click());
            assertEquals("roadfront",config.getPreviewSourceMode());
            onView(withId(R.id.btn_preview_layout)).perform(click());
            onView(withText(R.string.slot_road_front)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
            onView(withText(R.string.slot_road_rear)).perform(click());
            onView(withText(R.string.home_preview_save)).perform(click());
            assertEquals("roadrear",config.getPreviewSourceMode());
            assertEquals(profile,target.getSharedPreferences("profiles",0).getAll());
            scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.main_preview_pending).getVisibility());
                assertFalse(a.findViewById(R.id.btn_bird_eye).isEnabled());
                assertEquals(View.GONE,a.findViewById(R.id.btn_fisheye).getVisibility());});
        }
    }
    @Test public void captureLightAndDarkMainPage() throws Exception {
        config.setPreviewSourceMode("surround_roads");
        for(int mode:new int[]{AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES}) {
            main(()->AppCompatDelegate.setDefaultNightMode(mode));
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
                capture(mode==AppCompatDelegate.MODE_NIGHT_NO?"main-light":"main-dark");
                if ("true".equals(InstrumentationRegistry.getArguments().getString("captureToolbar"))) {
                    scenario.onActivity(a -> config.setFisheyeCorrection(true));
                    capture(mode==AppCompatDelegate.MODE_NIGHT_NO?"main-light-straighten":"main-dark-straighten");
                    continue;
                }
                onView(withId(R.id.btn_preview_layout)).perform(click());
                for(int label:new int[]{R.string.home_layout_auto,R.string.home_layout_equal,R.string.home_layout_stacked})
                    // Four full-width presets intentionally put layout controls below the fold.
                    onView(withText(label)).perform(androidx.test.espresso.action.ViewActions.scrollTo())
                            .check(androidx.test.espresso.assertion.ViewAssertions.matches(isCompletelyDisplayed()));
                capture(mode==AppCompatDelegate.MODE_NIGHT_NO?"main-light-preview":"main-dark-preview");
                androidx.test.espresso.Espresso.pressBack();
                onView(withId(R.id.btn_main_windows)).perform(click());
                capture(mode==AppCompatDelegate.MODE_NIGHT_NO?"main-light-windows":"main-dark-windows");
            }
        }
    }

    @Test public void galleryOpensDirectlyAndPhotosTabReturnsToTheSameCameraPipeline() {
        com.kooo.evcam.recording.RecordingIntent intent = com.kooo.evcam.recording.RecordingIntent.current();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            intent.noteUserStopped();
            final com.kooo.evcam.camera.MultiCameraManager[] pipeline = {null};
            scenario.onActivity(a -> pipeline[0] = CameraManagerHolder.getInstance().getCameraManager());
            onView(withId(R.id.btn_main_gallery)).perform(click());
            onView(withId(R.id.tab_videos)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            onView(withId(R.id.gallery_title)).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText(R.string.nav_video_playback)));
            onView(withId(R.id.tab_photos)).perform(click());
            onView(withId(R.id.gallery_title)).check(androidx.test.espresso.assertion.ViewAssertions.matches(withText(R.string.nav_photo_playback)));
            androidx.test.espresso.Espresso.pressBack();
            scenario.onActivity(a -> {
                assertSame(pipeline[0], CameraManagerHolder.getInstance().getCameraManager());
                assertTrue(intent.stoppedByUser());
                assertFalse(com.kooo.evcam.recording.RecordingCoordinator.get(a).isRecordingOrResuming());
                assertEquals(target.getResources().getConfiguration().fontScale, a.getResources().getConfiguration().fontScale, .001f);
            });
        }
    }
}
