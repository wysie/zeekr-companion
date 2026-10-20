package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

/** The compatibility popup's existing mesh/rotation policy, applied once to a frozen native lane. */
final class SideQualityCanvas {
    private SideQualityCanvas() { }
    static Bitmap render(Bitmap original, SideQualitySpec spec) {
        Bitmap result=Bitmap.createBitmap(spec.width,spec.height,Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas=new Canvas(result);canvas.drawColor(android.graphics.Color.BLACK);
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            RectF target=new RectF(0,0,spec.width,spec.height);
            int saved=SideViewRotation.begin(canvas,spec.straighten?0:spec.rotation,spec.width,spec.height,spec.mirrored);
            if(spec.straighten) {
                FisheyeMesh mesh=new FisheyeMesh();
                mesh.prepare(SideViewFraming.meshDivisions(spec.width,spec.height),0,0,spec.width,spec.height,spec.framing::sourcePoint);
                FisheyeMesh.Painter draw=c -> c.drawBitmap(original,null,target,paint);
                if(spec.autoFit)mesh.draw(canvas,0,0,spec.width,spec.height,draw);
                else mesh.drawClippedToLane(canvas,0,0,spec.width,spec.height,draw);
            } else {
                RearViewGeometry.Viewport aim=SideViewAim.viewport(spec.lane,Math.round(180f/spec.fov*100),
                        Math.round(spec.yaw/SideViewProjection.MAX_YAW_DEGREES*100),Math.round(spec.pitch/SideViewProjection.MAX_PITCH_DEGREES*100));
                RearViewGeometry.Viewport crop=RearViewGeometry.Viewport.forWindow(spec.width,spec.height,.5f);
                float x=aim.x+crop.x*aim.width,y=aim.y+crop.y*aim.height;
                RectF source=new RectF(x*original.getWidth(),y*original.getHeight(),
                        (x+crop.width*aim.width)*original.getWidth(),(y+crop.height*aim.height)*original.getHeight());
                Matrix matrix=new Matrix();matrix.setRectToRect(source,target,Matrix.ScaleToFit.FILL);
                canvas.concat(matrix);canvas.drawBitmap(original,0,0,paint);
            }
            canvas.restoreToCount(saved);return result;
        } catch(RuntimeException|OutOfMemoryError error) {result.recycle();throw error;}
    }
}
