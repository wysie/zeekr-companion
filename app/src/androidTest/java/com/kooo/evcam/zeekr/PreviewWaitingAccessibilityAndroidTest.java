package com.kooo.evcam.zeekr;
import android.os.Build;import android.os.SystemClock;import android.graphics.*;import android.view.View;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.kooo.evcam.ui.CameraStatusActivity;import com.kooo.evcam.R;
import org.junit.Test;import org.junit.runner.RunWith;import static org.junit.Assert.*;import static org.junit.Assume.assumeTrue;
@RunWith(AndroidJUnit4.class) public class PreviewWaitingAccessibilityAndroidTest {
 @Test public void waitingMessagesKeepBothEdgesVisibleInNarrowAndWidePanes() {
  assumeTrue(Build.PRODUCT.contains("sdk")||Build.HARDWARE.contains("ranchu"));
  try(ActivityScenario<CameraStatusActivity> scenario=ActivityScenario.launch(CameraStatusActivity.class)) {
   scenario.onActivity(a->{
    for(float scale:new float[]{1f,1.3f}) for(int width:new int[]{320,640,900}) {
     android.content.res.Configuration configuration=new android.content.res.Configuration(a.getResources().getConfiguration());configuration.fontScale=scale;
     View owner=new View(a.createConfigurationContext(configuration));owner.layout(0,0,width,900);
     PreviewFrameGuard guard=new PreviewFrameGuard(owner);
     try {
      java.lang.reflect.Field since=PreviewFrameGuard.class.getDeclaredField("waitingSince");since.setAccessible(true);since.setLong(guard,SystemClock.elapsedRealtime()-11000);
     } catch(ReflectiveOperationException e){throw new AssertionError(e);}
     Bitmap image=Bitmap.createBitmap(width,900,Bitmap.Config.ARGB_8888);
     try {
      guard.cover(new Canvas(image),true);
      int left=width,right=-1,pixels=0;
      for(int y=0;y<image.getHeight();y++) for(int x=0;x<width;x++) {
       int color=image.getPixel(x,y);
       if(Color.red(color)>100&&Color.green(color)>100&&Color.blue(color)>100){left=Math.min(left,x);right=Math.max(right,x);pixels++;}
      }
      assertTrue("Real glyphs were rendered",pixels>100);
      assertTrue("Beginning of the message stays inside its pane at "+scale+" / "+width,left>=12);
      assertTrue("End of the message stays inside its pane",right<width-12);
      assertTrue("Message is visually centred instead of shifted left",Math.abs((left+right)/2f-width/2f)<16);
      assertTrue(owner.getContentDescription().toString().contains(a.getString(R.string.camera_waiting_status_link)));
      assertFalse("Elapsed seconds must not trigger repeated accessibility announcements",
              owner.getContentDescription().toString().contains(a.getString(R.string.camera_preview_wait_details,11)));
     } finally {image.recycle();}
    }
   });
  }
 }
 @Test public void staleCoverExposesStateAndProlongedWaitWhileFreshPreviewRemainsIndependent() {
  assumeTrue(Build.PRODUCT.contains("sdk")||Build.HARDWARE.contains("ranchu"));
  try(ActivityScenario<CameraStatusActivity> scenario=ActivityScenario.launch(CameraStatusActivity.class)) {
   scenario.onActivity(a->{
    LiveCameraPreview preview=new LiveCameraPreview(a);preview.setBackgroundColor(Color.RED);a.setContentView(preview);
    preview.measure(View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));preview.layout(0,0,640,480);
    try {
     java.lang.reflect.Field g=LiveCameraPreview.class.getDeclaredField("guard");g.setAccessible(true);PreviewFrameGuard guard=(PreviewFrameGuard)g.get(preview);
     java.lang.reflect.Field since=PreviewFrameGuard.class.getDeclaredField("waitingSince");since.setAccessible(true);since.setLong(guard,SystemClock.elapsedRealtime()-11000);
     Bitmap image=Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);guard.cover(canvas,false);
     assertEquals(Color.BLACK,image.getPixel(0,0));assertTrue(preview.getContentDescription().toString().contains(a.getString(R.string.camera_waiting_status_link)));
     assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE,preview.getAccessibilityLiveRegion());
     java.lang.reflect.Field f=PreviewFrameGuard.class.getDeclaredField("frames");f.setAccessible(true);CameraFrameFreshness frames=(CameraFrameFreshness)f.get(guard);
     Object source=new Object();long now=SystemClock.elapsedRealtime();frames.observe(source,1,now);frames.observe(source,2,now);
     image.eraseColor(Color.RED);guard.cover(canvas,false);assertEquals(Color.RED,image.getPixel(0,0));assertEquals(a.getString(R.string.camera_preview_live),preview.getContentDescription());image.recycle();
    } catch(ReflectiveOperationException e){throw new AssertionError(e);}
   });
  }
 }
}
