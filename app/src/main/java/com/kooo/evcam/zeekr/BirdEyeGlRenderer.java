package com.kooo.evcam.zeekr;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.FloatBuffer;

/** GL-thread-only stitcher. Camera pixels stay in the external texture; no frame readback/upload. */
final class BirdEyeGlRenderer {
    private static final String VERTEX =
            "attribute vec2 aPosition; varying vec2 vPos;\n"
            + "void main(){ gl_Position=vec4(aPosition,0.0,1.0); vPos=vec2((aPosition.x+1.0)*0.5,(1.0-aPosition.y)*0.5); }";
    private static final String FRAGMENT =
            "#extension GL_OES_EGL_image_external : require\n"
            + "precision highp float; varying vec2 vPos;\n"
            + "uniform samplerExternalOES sCamera; uniform sampler2D sFirst; uniform sampler2D sSecond; uniform sampler2D sMeta;\n"
            + "uniform mat4 uTexMatrix; uniform vec2 uSourceSize; uniform vec4 uLanes[4];\n"
            + "vec2 decode(vec4 p){return vec2(dot(p.rg,vec2(65280.0,255.0)),dot(p.ba,vec2(65280.0,255.0)))/65535.0;}\n"
            + "vec4 sampleCamera(vec2 uv,float lane){\n"
            // Keep hardware bilinear taps inside the camera, even at quantised UV boundaries.
            + " for(int i=0;i<4;i++){ if(abs(lane-float(i+1))<0.5){\n"
            + "  vec2 lo=(ceil(uLanes[i].xy*uSourceSize)+0.5)/uSourceSize;\n"
            + "  vec2 hi=(ceil(uLanes[i].zw*uSourceSize)-0.5)/uSourceSize; uv=clamp(uv,lo,hi); } }\n"
            + " vec2 t=(uTexMatrix*vec4(uv.x,1.0-uv.y,0.0,1.0)).xy; return texture2D(sCamera,t); }\n"
            + "void main(){ vec3 m=texture2D(sMeta,vPos).rgb;\n"
            + " if(m.g<0.001){gl_FragColor=vec4(0.0,0.0,0.0,1.0);return;}\n"
            + " vec4 a=sampleCamera(decode(texture2D(sFirst,vPos)),m.g*255.0);\n"
            + " if(m.r>0.0 && m.b>0.001){ vec4 b=sampleCamera(decode(texture2D(sSecond,vPos)),m.b*255.0); a=mix(a,b,m.r); }\n"
            + " gl_FragColor=vec4(a.rgb,1.0); }";

    final BirdEyeGlRequest request;
    final int width, height;
    private int program;
    private final int[] textures = new int[3];
    private final int position, matrix, sourceSize, lanes;
    private final float[] laneBounds;
    private final int sourceWidth, sourceHeight;

    BirdEyeGlRenderer(BirdEyeGlRequest request, BirdEyeGpuMap map) {
        this.request = request; width = map.width; height = map.height;
        sourceWidth = map.sourceWidth; sourceHeight = map.sourceHeight; laneBounds = map.lanes.clone();
        try {
            program = FisheyeGlPipe.link(VERTEX, FRAGMENT);
            position = GLES20.glGetAttribLocation(program, "aPosition");
            matrix = GLES20.glGetUniformLocation(program, "uTexMatrix");
            sourceSize = GLES20.glGetUniformLocation(program, "uSourceSize");
            lanes = GLES20.glGetUniformLocation(program, "uLanes");
            int[] maximum = new int[1]; GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maximum, 0);
            if (width > maximum[0] || height > maximum[0]) throw new IllegalStateException("GPU lookup exceeds texture limit");
            GLES20.glGenTextures(3, textures, 0);
            java.nio.ByteBuffer[] data = {map.first, map.second, map.metadata};
            GLES20.glUseProgram(program);
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "sCamera"), 0);
            String[] samplers = {"sFirst", "sSecond", "sMeta"};
            for (int i = 0; i < 3; i++) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1 + i);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                data[i].position(0);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                        GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, data[i]);
                GLES20.glUniform1i(GLES20.glGetUniformLocation(program, samplers[i]), i + 1);
            }
            check();
        } catch (RuntimeException | OutOfMemoryError error) { release(); throw error; }
    }

    void draw(int cameraTexture, float[] textureMatrix, FloatBuffer quad) {
        GLES20.glUseProgram(program);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTexture);
        for (int i = 0; i < 3; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1 + i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
        }
        GLES20.glUniformMatrix4fv(matrix, 1, false, textureMatrix, 0);
        GLES20.glUniform2f(sourceSize, sourceWidth, sourceHeight);
        GLES20.glUniform4fv(lanes, 4, laneBounds, 0);
        quad.position(0);
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(position);
        check();
    }
    void release() {
        if (program != 0) { GLES20.glDeleteProgram(program); program = 0; }
        GLES20.glDeleteTextures(3, textures, 0);
    }
    private static void check() {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) throw new IllegalStateException("360 GL error 0x" + Integer.toHexString(error));
    }
}
