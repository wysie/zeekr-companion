package com.kooo.evcam.camera;
import android.content.Context;import android.os.Build;import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;import com.kooo.evcam.R;import com.kooo.evcam.ui.*;
import org.junit.*;import org.junit.runner.RunWith;import java.util.*;import static org.junit.Assert.*;import static org.junit.Assume.assumeTrue;
@RunWith(AndroidJUnit4.class) public class CameraStatusAndroidTest {
 @Test public void partialLossAndRecoveryCountsUseRecorderSummaryOnly() {
  assumeTrue(Build.PRODUCT.contains("sdk")||Build.HARDWARE.contains("ranchu"));
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();List<String> planned=Arrays.asList("front","roadfront","roadrear");
  for(int n=0;n<=3;n++) {
   RecordingLanes.Summary summary=new RecordingLanes.Summary(RecordingLanes.View.RECORDING,planned,planned.subList(0,n),true,RecordingLanes.Hint.NONE);
   assertEquals(c.getString(R.string.camera_recording_count,n,3),CameraStatusText.progress(c,summary));
  }
  assertEquals(c.getString(R.string.camera_status_title),CameraStatusText.progress(c,null));
 }
 @Test public void statusActivityDoesNotStartACameraPipelineAndHasDiagnosticAction() {
  assumeTrue(Build.PRODUCT.contains("sdk")||Build.HARDWARE.contains("ranchu"));
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->CameraManagerHolder.getInstance().release());
  try(ActivityScenario<CameraStatusActivity> scenario=ActivityScenario.launch(CameraStatusActivity.class)) {
   scenario.onActivity(a->{assertNull(CameraManagerHolder.getInstance().getCameraManager());
    TextView report=a.findViewById(R.id.camera_status_report);assertTrue(report.getText().toString().contains(a.getString(R.string.camera_status_help)));
    assertTrue(a.findViewById(R.id.camera_status_diagnostics).isClickable());
   });
   scenario.recreate();scenario.onActivity(a->assertNull(CameraManagerHolder.getInstance().getCameraManager()));
  }
 }
}
