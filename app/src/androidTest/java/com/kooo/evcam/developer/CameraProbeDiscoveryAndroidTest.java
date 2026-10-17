package com.kooo.evcam.developer;
import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.camera.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
@RunWith(AndroidJUnit4.class)
public class CameraProbeDiscoveryAndroidTest {
 @Test public void extendedProbeReportsAbsentIdsAndClosesAccessibleCameras() throws Exception {
  assumeTrue(Build.PRODUCT.contains("sdk")||Build.HARDWARE.contains("ranchu"));
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(c.getPackageName(),Manifest.permission.CAMERA);
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->CameraManagerHolder.getInstance().release());
  long limit=SystemClock.elapsedRealtime()+15000;
  while(!ChannelScheduler.allCamerasClosedEverywhere()&&SystemClock.elapsedRealtime()<limit)SystemClock.sleep(50);
  assertTrue(ChannelScheduler.allCamerasClosedEverywhere());
  assertTrue(CameraNeeds.current().tryBeginDeveloperProbe());
  File folder=new File(c.getCacheDir(),"probe-test-"+System.nanoTime());assertTrue(folder.mkdirs());
  CountDownLatch released=new CountDownLatch(1);JSONObject report=new JSONObject();
  CameraFeedExporter exporter=new CameraFeedExporter(c,folder,text->{},()->{CameraNeeds.current().endDeveloperProbe();released.countDown();});
  exporter.run(report);assertTrue(released.await(15,TimeUnit.SECONDS));assertFalse(exporter.waitingForNativeClosure());
  JSONArray candidates=report.getJSONArray("candidate_ids");assertTrue(candidates.length()>=16);assertTrue(candidates.length()<=48);
  JSONArray results=report.getJSONArray("cameras");int saved=0,absent=0;
  for(int i=0;i<results.length();i++){JSONObject item=results.getJSONObject(i);
   if(item.optString("result").equals("Saved")){saved++;assertTrue(item.getBoolean("closure_confirmed"));assertTrue(new File(folder,item.getString("image")).length()>0);}
   if(!item.getBoolean("advertised")&&!item.optString("result").equals("Saved"))absent++;
  }
  assertTrue("At least one exposed emulator feed captured",saved>0);assertTrue("Invalid numeric IDs reported",absent>0);
  for(File f:folder.listFiles())f.delete();folder.delete();
 }
}
