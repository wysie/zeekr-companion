package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLES11Ext;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/** One-off readback on the source GL thread. Both pictures sample its unchanged acquired OES frame. */
final class SideQualityGl {
    private SideQualityGl() { }
    static SideQualityPair capture(SideQualitySpec spec,SideGpuRenderer renderer,int cameraTexture,
            float[] matrix,FloatBuffer quad,long timestamp,long observedAt,String outputState) throws Exception {
        int[] previous=new int[1],viewport=new int[4],fbo=new int[1],target=new int[1];
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING,previous,0);
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT,viewport,0);
        Bitmap original=null,rendered=null;
        try {
            GLES20.glGenFramebuffers(1,fbo,0);GLES20.glGenTextures(1,target,0);
            CompositeStreamGeometry.Lane lane=spec.plan.lane(spec.lane);
            int[] min=new int[1],mag=new int[1];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,cameraTexture);
            GLES20.glGetTexParameteriv(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,min,0);
            GLES20.glGetTexParameteriv(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,mag,0);
            try {
                // The reference must not itself be softened by bilinear filtering of the OES crop matrix.
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_NEAREST);
                original=render(renderer,spec.original(),lane.width,lane.height,cameraTexture,matrix,quad,fbo[0],target[0]);
            } finally {
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MIN_FILTER,min[0]);
                GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,GLES20.GL_TEXTURE_MAG_FILTER,mag[0]);
            }
            if(spec.gpu)rendered=render(renderer,spec.adjusted(),spec.width,spec.height,cameraTexture,matrix,quad,fbo[0],target[0]);
            org.json.JSONObject metadata=spec.metadata().put("source_timestamp_ns",timestamp)
                    .put("source_observed_elapsed_ms",observedAt).put("capture_wall_ms",System.currentTimeMillis())
                    .put("source_readback","native OES lane on existing GL thread")
                    .put("native_sampling","Nearest native texel with producer crop/texture matrix; live sampler restored before rendering").put("source_pipeline",outputState);
            return new SideQualityPair(original,rendered,metadata);
        } catch(Exception|OutOfMemoryError failure) {
            if(original!=null)original.recycle();if(rendered!=null)rendered.recycle();throw failure;
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,previous[0]);
            GLES20.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            if(fbo[0]!=0)GLES20.glDeleteFramebuffers(1,fbo,0);if(target[0]!=0)GLES20.glDeleteTextures(1,target,0);
            // A failed diagnostic allocation must not poison the next live renderer draw.
            for(int i=0;i<8 && GLES20.glGetError()!=GLES20.GL_NO_ERROR;i++) { }
        }
    }
    private static Bitmap render(SideGpuRenderer renderer,SideGpuPicture picture,int width,int height,
            int cameraTexture,float[] matrix,FloatBuffer quad,int framebuffer,int texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_NEAREST);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,width,height,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,framebuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,texture,0);
        if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Comparison framebuffer unavailable");
        GLES20.glViewport(0,0,width,height);renderer.draw(picture,cameraTexture,matrix,quad);
        ByteBuffer pixels=ByteBuffer.allocateDirect(Math.multiplyExact(Math.multiplyExact(width,height),4)).order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0,0,width,height,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,pixels);
        if(GLES20.glGetError()!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Comparison pixel copy failed");
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        try {
            int[] row=new int[width];
            for(int y=0;y<height;y++) {
                int offset=(height-1-y)*width*4;
                for(int x=0;x<width;x++) {int value=pixels.getInt(offset+x*4);
                    row[x]=(value&0xff00ff00)|((value&255)<<16)|((value>>>16)&255);}
                bitmap.setPixels(row,0,width,0,y,width,1);
            }
            return bitmap;
        } catch(RuntimeException|OutOfMemoryError failure){bitmap.recycle();throw failure;}
    }
}
