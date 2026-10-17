package com.kooo.evcam.zeekr;

import android.content.*;
import android.graphics.SurfaceTexture;
import android.os.*;
import android.util.Size;
import android.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Native windows and real retained-texture pool, with camera declarations only: no HAL opens. */
@RunWith(AndroidJUnit4.class)
public class SidePopupLifecycleAndroidTest {
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private static Field field(Class<?> type,String name){try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception e){throw new AssertionError(e);}}
    private static Object get(Object o,String name){try{return field(SideViewPopupService.class,name).get(o);}catch(Exception e){throw new AssertionError(e);}}
    private static void set(Object o,String name,Object value){try{field(SideViewPopupService.class,name).set(o,value);}catch(Exception e){throw new AssertionError(e);}}
    private static void call(Object o,String name,Class<?>[] args,Object...values){try{Method m=SideViewPopupService.class.getDeclaredMethod(name,args);m.setAccessible(true);m.invoke(o,values);}catch(Exception e){throw new AssertionError(e);}}
    private static void call(Object o,String name){call(o,name,new Class<?>[0]);}
    private void await(BooleanSupplier condition){long end=SystemClock.uptimeMillis()+5000;while(!condition.getAsBoolean()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(20);assertTrue("Lifecycle condition timed out",condition.getAsBoolean());}
    private static final class FakeCamera extends SingleCamera {
        FakeCamera(Context c,String id){super(c,id,null);}
        @Override public Size getPreviewSize(){return new Size(256,1024);}
        @Override public Size getPreviewBufferSize(){return new Size(256,1024);}
    }
    private static final class Manager extends MultiCameraManager {
        SingleCamera selected;
        Manager(Context c){super(c);}
        @Override public SingleCamera getCamera(String key){return CameraSlots.KEY_SURROUND.equals(key)?selected:null;}
        @SuppressWarnings("unchecked") void select(SingleCamera camera){
            selected=camera;
            try{Map<String,SingleCamera> map=(Map<String,SingleCamera>)field(MultiCameraManager.class,"cameras").get(this);
                map.clear();if(camera!=null)map.put(CameraSlots.KEY_SURROUND,camera);
            }catch(Exception e){throw new AssertionError(e);}
        }
    }
    private static final class Service extends SideViewPopupService {
        Manager manager;boolean blocked;
        Service(Context context){attachBaseContext(new ContextThemeWrapper(context,R.style.Theme_Cam));}
        @Override protected MultiCameraManager currentPipeline(){return manager;}
        @Override protected boolean cameraBlocked(){return blocked;}
    }
    private final class Fixture implements AutoCloseable {
        final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        final AppConfig config=new AppConfig(context);
        final int mirror=config.getSidePopupMirrorMode();
        Service service;Manager manager;
        Object oldListener;
        final boolean held=CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_POPUP);
        Fixture() throws Exception {
            try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeShellCommand("appops set "+context.getPackageName()+" SYSTEM_ALERT_WINDOW allow");
                java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
            config.setSidePopupReplaceMirror(false);
            main(()->{
                try{oldListener=field(CameraNeeds.class,"listener").get(CameraNeeds.current());}catch(Exception e){throw new AssertionError(e);}
                manager=new Manager(context);service=new Service(context);service.manager=manager;
                set(service,"appConfig",config);set(service,"listening",true);
            });
        }
        SideViewPopupView view(){return (SideViewPopupView)get(service,"popup");}
        void attach(){main(()->call(service,"attachPopup",new Class<?>[]{int.class,boolean.class},LaneCycle.LEFT,false));await(()->view()!=null&&view().getTextureView().isAvailable());}
        @Override public void close(){main(()->{
            call(service,"detachPopup");set(service,"destroyed",true);set(service,"listening",false);
            ((Handler)get(service,"handler")).removeCallbacksAndMessages(null);
            manager.release();
            try{field(CameraNeeds.class,"listener").set(CameraNeeds.current(),oldListener);}catch(Exception e){throw new AssertionError(e);}
            if(held)CameraNeeds.current().claim(CameraNeeds.Holder.SIDE_POPUP);else CameraNeeds.current().release(CameraNeeds.Holder.SIDE_POPUP);
            config.setSidePopupMirrorMode(mirror);
        });}
    }
    @Test public void watchdogRebindsAfterFastRetriesHaveEndedAndAfterPipelineReplacement() throws Exception {
        try(Fixture f=new Fixture()){
            f.attach();
            main(()->set(f.service,"retryCount",21));
            FakeCamera first=new FakeCamera(f.context,"test-a");
            main(()->f.manager.select(first));
            await(()->get(f.service,"boundCamera")==first);
            SurfaceTexture texture=f.view().getTextureView().getSurfaceTexture();
            assertTrue(first.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_POPUP,texture)));
            // Use a distinct manager instance to model actual pipeline replacement.
            AtomicReference<Manager> next=new AtomicReference<>();
            main(()->{Manager m=new Manager(f.context);m.select(new FakeCamera(f.context,"test-b"));next.set(m);f.service.manager=m;});
            await(()->get(f.service,"boundManager")==next.get());
            assertFalse(first.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_POPUP,texture)));
            assertTrue(next.get().selected.declaredOutputs().contains(new ChannelRules.Out(ChannelRules.Kind.SIDE_POPUP,texture)));
            main(()->{next.get().release();f.service.manager=f.manager;});
        }
    }
    @Test public void reopeningReclaimsTheSameTextureAndImmediateTeardownRetainsIt() throws Exception {
        try(Fixture f=new Fixture()){
            main(()->f.manager.select(new FakeCamera(f.context,"test-reuse")));f.attach();
            main(()->{
                SurfaceTexture original=f.view().getTextureView().getSurfaceTexture();
                call(f.service,"detachPopup");assertFalse(original.isReleased());
                call(f.service,"attachPopup",new Class<?>[]{int.class,boolean.class},LaneCycle.RIGHT,false);
                assertSame(original,f.view().getTextureView().getSurfaceTexture());
                assertSame(original,get(f.service,"boundTexture"));
                assertEquals(Boolean.TRUE,get(f.service,"restoreBufferOnFrame"));
                // No first layout/destruction callback yet: exercise the reclaimed orphan path.
                call(f.service,"detachPopup");assertFalse(original.isReleased());
                SurfaceTexture reclaimed=f.manager.takeKeptSidePopup();assertSame(original,reclaimed);
                assertTrue(MultiCameraManager.keepAuxiliaryCanvas(f.manager,ChannelRules.Kind.SIDE_POPUP,reclaimed));
            });
        }
    }
    @Test public void refusalAndDetachCancelTheWatchdogWithoutLosingOtherCameraClaims() throws Exception {
        try(Fixture f=new Fixture()){
            main(()->f.manager.select(new FakeCamera(f.context,"test-drop")));f.attach();
            main(()->{
                SurfaceTexture texture=f.view().getTextureView().getSurfaceTexture();
                SingleCamera.RecordDropped dropped=f.manager.selected.dropListener(new ChannelRules.Out(ChannelRules.Kind.SIDE_POPUP,texture));
                assertNotNull(dropped);dropped.onDropped(SingleCamera.Drop.CONFIG_FAILED);
                assertNull(f.view());assertNull(get(f.service,"watchdog"));
                assertTrue(((SidePopupRecovery)get(f.service,"recovery")).refused());
                assertFalse(CameraNeeds.current().isHeld(CameraNeeds.Holder.SIDE_POPUP));
            });
        }
    }
    @Test public void rejectedOverlayLatchesUntilReadinessEpisodeEnds() throws Exception {
        try(Fixture f=new Fixture()){
            String command="appops set "+f.context.getPackageName()+" SYSTEM_ALERT_WINDOW ";
            try {
                try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command+"deny");
                    java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
                main(()->{
                    SidePopupRecovery gate=(SidePopupRecovery)get(f.service,"recovery");gate.observe(0,true,true,false);
                    call(f.service,"attachPopup",new Class<?>[]{int.class,boolean.class},LaneCycle.LEFT,true);
                    assertNull(f.view());assertNull(get(f.service,"watchdog"));assertTrue(gate.refused());
                    for(int t=1;t<100;t++){gate.observe(t,true,true,false);assertTrue(gate.refused());}
                    gate.observe(100,true,true,true);assertFalse(gate.refused());
                });
            } finally {
                try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command+"allow");
                    java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
            }
            f.attach();assertNotNull(f.view());
        }
    }

    @Test public void destructionHandsTextureToItsBoundPipelineEvenAfterHolderChanges() throws Exception {
        try(Fixture f=new Fixture()){
            main(()->f.manager.select(new FakeCamera(f.context,"test-owner")));f.attach();
            main(()->{
                SurfaceTexture texture=f.view().getTextureView().getSurfaceTexture();
                Manager replacement=new Manager(f.context);f.service.manager=replacement;
                try {
                    call(f.service,"detachPopup");
                    assertNull(replacement.takeKeptSidePopup());
                    assertSame(texture,f.manager.takeKeptSidePopup());
                    assertTrue(MultiCameraManager.keepAuxiliaryCanvas(f.manager,ChannelRules.Kind.SIDE_POPUP,texture));
                } finally {replacement.release();f.service.manager=f.manager;}
            });
        }
    }

}
