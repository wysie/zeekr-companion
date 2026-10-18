package com.kooo.evcam.zeekr;
import android.os.Build;import android.os.SystemClock;import android.graphics.*;import android.view.View;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.kooo.evcam.ui.CameraStatusActivity;import com.kooo.evcam.R;
import org.junit.Test;import org.junit.runner.RunWith;import static org.junit.Assert.*;import static org.junit.Assume.assumeTrue;
@RunWith(AndroidJUnit4.class) public class PreviewWaitingAccessibilityAndroidTest {
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
