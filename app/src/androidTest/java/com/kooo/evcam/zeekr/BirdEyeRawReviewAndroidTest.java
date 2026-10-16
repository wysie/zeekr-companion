package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

/** Opt-in offline review of a private raw capture; no owner camera files ship in the tests/APK.
 * adb shell am instrument -w -e class com.kooo.evcam.zeekr.BirdEyeRawReviewAndroidTest
 *     -e bird_eye_raw /sdcard/Android/data/io.github.dts88.zeekrshortcut/files/review/raw.jpg ...
 * This verifies rendering only, not live throughput, freshness or parking-distance accuracy.
 */
@RunWith(AndroidJUnit4.class)
public class BirdEyeRawReviewAndroidTest {
    @Test public void renderPrivateRawAtCloseAndWideFramingWhenSupplied() throws Exception {
        String path=InstrumentationRegistry.getArguments().getString("bird_eye_raw");
        assumeNotNull(path);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Bitmap raw=BitmapFactory.decodeFile(path); assertNotNull(raw);
        assertEquals(1280,raw.getWidth()); assertEquals(5140,raw.getHeight());
        CompositeStreamGeometry.Lane[] lanes=new CompositeStreamGeometry.Lane[4];
        for(int i=0;i<4;i++) lanes[i]=new CompositeStreamGeometry.Lane(i,0,4+i*1284,1280,1280,1280,5140);
        CompositeStreamGeometry.Plan plan=new CompositeStreamGeometry.Plan(
                CompositeStreamGeometry.Stacking.VERTICAL,1280,5140,1280,4,true,lanes,"private raw review");
        File directory=new File(context.getExternalFilesDir(null),"360-polish-review");
        assertTrue(directory.isDirectory()||directory.mkdirs());
        try {
            for(int sourceWidth:new int[]{512,768,1280}) for(double margin:new double[]{2,3}) {
                int sourceHeight=sourceWidth*5140/1280;
                Bitmap source=Bitmap.createBitmap(sourceWidth,sourceHeight,Bitmap.Config.ARGB_8888);
                new Canvas(source).drawBitmap(raw,null,new RectF(0,0,sourceWidth,sourceHeight),
                        new Paint(Paint.FILTER_BITMAP_FLAG));
                int[] pixels=new int[sourceWidth*sourceHeight], output=new int[640*960];
                source.getPixels(pixels,0,sourceWidth,0,0,sourceWidth,sourceHeight);
                BirdEyeProjection.Calibration calibration=BirdEyeImageFit.zeekr009(new int[]{0,1,2,3},margin);
                new BirdEyeProjection.Map(calibration,plan,sourceWidth,sourceHeight,640,960).render(pixels,output);
                Bitmap picture=Bitmap.createBitmap(640,960,Bitmap.Config.ARGB_8888);
                picture.setPixels(output,0,640,0,0,640,960);
                double mpp=Math.max((calibration.width+2*margin)/640,(calibration.length+2*margin)/960);
                float w=(float)(calibration.width/mpp),h=(float)(calibration.length/mpp);
                BirdEyeVehicleOverlay overlay=new BirdEyeVehicleOverlay();
                overlay.prepare(context.getResources(),BirdEyeAppearance.MODEL_009,BirdEyeAppearance.WHITE);
                assertTrue(overlay.draw(new Canvas(picture),new RectF(320-w/2,480-h/2,320+w/2,480+h/2)));
                try(FileOutputStream stream=new FileOutputStream(new File(directory,
                        "009-source"+sourceWidth+"-margin"+(int)margin+".png"))) {
                    assertTrue(picture.compress(Bitmap.CompressFormat.PNG,100,stream));
                }
                picture.recycle(); source.recycle();
            }
        } finally { raw.recycle(); }
    }
}
