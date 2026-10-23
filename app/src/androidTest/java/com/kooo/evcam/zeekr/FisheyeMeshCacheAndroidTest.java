package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real Canvas pixels: cached homographies must follow aim, resize and map changes. */
@RunWith(AndroidJUnit4.class)
public class FisheyeMeshCacheAndroidTest {
    @Test public void cachedTilesMatchFreshRenderingAndInvalidateAfterProjectionOrResize() throws Exception {
        Bitmap source=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);
        for(int y=0;y<100;y++)for(int x=0;x<100;x++)source.setPixel(x,y,Color.rgb(x*2,y*2,(x+y)%256));
        Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
        float[] aim={0};
        FisheyeMesh.SourceMap map=(u,v,out)->{out[0]=.1f+u*.6f+aim[0];out[1]=.1f+v*.8f;};
        FisheyeMesh mesh=new FisheyeMesh();
        java.lang.reflect.Field transforms=FisheyeMesh.class.getDeclaredField("tileTransforms");transforms.setAccessible(true);
        try {
            Object previous=null, firstMatrix=null;
            for(int step=0;step<6;step++) {
                int width=step==3?150:100;
                int divisions=step==5?8:10;
                float laneLeft=step>=4?10:0;
                if(step==2)aim[0]=.1f;
                long revision=step<2?1:2;
                mesh.prepare(divisions,laneLeft,0,100,100,map,revision);
                Bitmap actual=render(mesh,source,paint,width);
                FisheyeMesh fresh=new FisheyeMesh();fresh.prepare(divisions,laneLeft,0,100,100,map);
                Bitmap expected=render(fresh,source,paint,width);
                try {
                    assertTrue("Cached pixels match fresh at step "+step,actual.sameAs(expected));
                    if(step==1)assertSame("Unchanged frame reuses homographies",previous,transforms.get(mesh));
                    if(step>=2 && step<5)assertSame("Changes recompute without per-frame Matrix allocation",previous,transforms.get(mesh));
                    Object currentFirst=((Object[])transforms.get(mesh))[0];
                    if(step>0)assertSame("Reuse Matrix objects across lane/target/density changes",firstMatrix,currentFirst);
                    firstMatrix=currentFirst;
                    previous=transforms.get(mesh);
                } finally { actual.recycle();expected.recycle(); }
            }
        } finally { source.recycle(); }
    }
    private Bitmap render(FisheyeMesh mesh,Bitmap source,Paint paint,int width) {
        Bitmap target=Bitmap.createBitmap(width,100,Bitmap.Config.ARGB_8888);
        mesh.draw(new Canvas(target),0,0,width,100,c->c.drawBitmap(source,0,0,paint));
        return target;
    }
}
