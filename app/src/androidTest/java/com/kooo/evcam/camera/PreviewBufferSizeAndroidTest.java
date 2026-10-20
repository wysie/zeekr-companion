package com.kooo.evcam.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.TextureView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real SurfaceTexture API, with no camera device or recording requested. */
@RunWith(AndroidJUnit4.class)
public class PreviewBufferSizeAndroidTest {
    private static final class Buffer extends SurfaceTexture {
        int width, height, changes;
        Buffer() { super(false); }
        @Override public void setDefaultBufferSize(int width, int height) {
            super.setDefaultBufferSize(width,height);this.width=width;this.height=height;changes++;
        }
    }
    @Test public void directBufferRestoresOnlyTheDeclaredPreviewAndRetainedReattachment() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SingleCamera camera=new SingleCamera(context,"preview-buffer-test",null);
        java.lang.reflect.Field field=SingleCamera.class.getDeclaredField("previewSize");field.setAccessible(true);
        field.set(camera,new Size(1280,5140));
        Buffer buffer=new Buffer(),foreign=new Buffer();
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                TextureView first=new TextureView(context);
                camera.declarePreview(first,buffer);
                assertEquals(1280,buffer.width);assertEquals(5140,buffer.height);
                buffer.setDefaultBufferSize(800,320);camera.restorePreviewBufferSize(buffer);
                assertEquals(1280,buffer.width);assertEquals(5140,buffer.height);
                foreign.setDefaultBufferSize(100,80);camera.restorePreviewBufferSize(foreign);
                assertEquals(100,foreign.width);assertEquals(80,foreign.height);
                camera.withdrawPreview(buffer);buffer.setDefaultBufferSize(320,200);
                camera.restorePreviewBufferSize(buffer);assertEquals(320,buffer.width);
                camera.declarePreview(new TextureView(context),buffer);
                assertEquals(1280,buffer.width);assertEquals(5140,buffer.height);
                assertFalse(camera.isCameraOpened());assertEquals(0,camera.sessionCounts().starts);
                camera.withdrawPreview(buffer);buffer.release();camera.restorePreviewBufferSize(buffer);
            });
        } finally {buffer.release();foreign.release();}
    }
}
