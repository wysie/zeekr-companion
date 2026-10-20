package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SideQualityExportAndroidTest {
    @Test public void zipRetainsLosslessNativePixelsMetadataAndUniqueNamesAndCleansCancelledWrites() throws Exception {
        File directory=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"owned-quality-"+java.util.UUID.randomUUID());
        assertTrue(directory.mkdirs());
        Bitmap original=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888),rendered=Bitmap.createBitmap(32,24,Bitmap.Config.ARGB_8888);
        original.eraseColor(Color.BLUE);original.setPixel(0,0,Color.RED);rendered.eraseColor(Color.GREEN);
        try(SideQualityPair pair=new SideQualityPair(original,rendered,new JSONObject().put("side","left").put("source_timestamp_ns",123456789L).put("pair_same_source_frame",true))) {
            File first=SideQualityCapture.save(directory,pair,new AtomicBoolean());
            File second=SideQualityCapture.save(directory,pair,new AtomicBoolean());
            assertNotEquals(first,second);assertTrue(first.isFile());assertTrue(second.isFile());
            try(ZipFile zip=new ZipFile(first)) {
                Set<String> names=new HashSet<>();java.util.Enumeration<? extends java.util.zip.ZipEntry> entries=zip.entries();
                while(entries.hasMoreElements())names.add(entries.nextElement().getName());
                assertEquals(new HashSet<>(java.util.Arrays.asList("native-side.png","popup-render.png","comparison.json")),names);
                Bitmap raw=BitmapFactory.decodeStream(zip.getInputStream(zip.getEntry("native-side.png")));
                Bitmap adjusted=BitmapFactory.decodeStream(zip.getInputStream(zip.getEntry("popup-render.png")));
                try {assertEquals(16,raw.getWidth());assertEquals(16,raw.getHeight());assertEquals(Color.RED,raw.getPixel(0,0));
                    assertEquals(Color.BLUE,raw.getPixel(15,15));assertEquals(32,adjusted.getWidth());assertEquals(24,adjusted.getHeight());
                    assertEquals(Color.GREEN,adjusted.getPixel(15,15));}finally{raw.recycle();adjusted.recycle();}
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                try(java.io.InputStream input=zip.getInputStream(zip.getEntry("comparison.json"))) {byte[] b=new byte[1024];int n;while((n=input.read(b))!=-1)bytes.write(b,0,n);}
                JSONObject metadata=new JSONObject(bytes.toString("UTF-8"));assertEquals(123456789L,metadata.getLong("source_timestamp_ns"));
                assertTrue(metadata.getBoolean("pair_same_source_frame"));
            }
            try {SideQualityCapture.save(directory,pair,new AtomicBoolean(true));fail("Cancelled export must not publish a ZIP");}
            catch(java.io.IOException expected) { }
            assertEquals("Only the two complete captures remain",2,directory.listFiles().length);
            for(File file:directory.listFiles())assertFalse(file.getName().endsWith(".partial"));
        } finally {for(File file:directory.listFiles())file.delete();directory.delete();}
    }
}
