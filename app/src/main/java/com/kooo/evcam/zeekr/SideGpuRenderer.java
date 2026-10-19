package com.kooo.evcam.zeekr;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.FloatBuffer;

/** Single-pass per-pixel crop, rectilinear correction or aimed/rolled side-camera correction. */
final class SideGpuRenderer {
    private static final String VERTEX = "attribute vec2 a; varying vec2 p; void main(){gl_Position=vec4(a,0.,1.);p=vec2((a.x+1.)*.5,(1.-a.y)*.5);}";
    private static final String FRAGMENT = "#extension GL_OES_EGL_image_external : require\nprecision highp float;\n"
            + "varying vec2 p; uniform samplerExternalOES image; uniform mat4 textureMatrix;"
            + "uniform vec4 lane, view, lens, turn, limits; uniform float mode;"
            + "void main(){ vec2 q=p; if(limits.x>.5)q.x=1.-q.x; vec2 s;"
            + "if(mode<.5){vec2 d=q-.5; d=vec2(lens.z*d.x+lens.w*d.y/lens.y,-lens.w*d.x*lens.y+lens.z*d.y)/limits.y; s=view.xy+(d+.5)*view.zw;}"
            + "else if(mode<1.5){vec2 d=((view.xy+q*view.zw)*2.-1.)*lens.x;float r=length(d);s=r<.000001?vec2(.5):clamp(.5+d/r*(atan(r)/1.5707963268)*.5,0.,1.);}"
            + "else {vec2 d=(q*2.-1.);d.y/=lens.y;d=vec2(lens.z*d.x+lens.w*d.y,-lens.w*d.x+lens.z*d.y)*lens.x;"
            + "float yy=d.y*turn.x-turn.y;float zz=d.y*turn.y+turn.x;vec3 ray=vec3(d.x*turn.z+zz*turn.w,yy,-d.x*turn.w+zz*turn.z);"
            + "float r=length(ray.xy);float angle=acos(clamp(ray.z/length(ray),-1.,1.));s=vec2(.5)+(r<.000001?vec2(0.):ray.xy/r)*(angle/1.5707963268)*.5;}"
            + "if(any(lessThan(s,vec2(0.)))||any(greaterThan(s,vec2(1.)))){gl_FragColor=vec4(0.,0.,0.,1.);return;}"
            + "s=lane.xy+clamp(s,limits.zw,vec2(1.)-limits.zw)*lane.zw;vec4 t=textureMatrix*vec4(s.x,1.-s.y,0.,1.);gl_FragColor=texture2D(image,t.xy);}";
    private final int program, position, sampler, matrixHandle, modeHandle;
    private final int[] uniforms = new int[5];
    SideGpuRenderer() {
        int vertex = shader(GLES20.GL_VERTEX_SHADER, VERTEX), fragment;
        try { fragment = shader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT); }
        catch(RuntimeException error) { GLES20.glDeleteShader(vertex); throw error; }
        program = GLES20.glCreateProgram(); GLES20.glAttachShader(program,vertex); GLES20.glAttachShader(program,fragment);
        GLES20.glLinkProgram(program); GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment);
        int[] ok={0}; GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);
        if(ok[0]==0){String error=GLES20.glGetProgramInfoLog(program);GLES20.glDeleteProgram(program);throw new IllegalStateException(error);}
        position=GLES20.glGetAttribLocation(program,"a");
        sampler=GLES20.glGetUniformLocation(program,"image");matrixHandle=GLES20.glGetUniformLocation(program,"textureMatrix");modeHandle=GLES20.glGetUniformLocation(program,"mode");
        String[] names={"lane","view","lens","turn","limits"};for(int i=0;i<names.length;i++)uniforms[i]=GLES20.glGetUniformLocation(program,names[i]);
    }
    private static int shader(int kind,String source) {
        int shader=GLES20.glCreateShader(kind);GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader);
        int[] ok={0};GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0){String error=GLES20.glGetShaderInfoLog(shader);GLES20.glDeleteShader(shader);throw new IllegalStateException(error);}return shader;
    }
    void draw(SideGpuPicture picture,int texture,float[] matrix,FloatBuffer quad) {
        GLES20.glUseProgram(program);GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture);
        GLES20.glUniform1i(sampler,0);
        GLES20.glUniformMatrix4fv(matrixHandle,1,false,matrix,0);
        uniform(0,picture.lane);uniform(1,picture.view);uniform(2,picture.lens);uniform(3,picture.turn);uniform(4,picture.limit);
        GLES20.glUniform1f(modeHandle,picture.mode);
        GLES20.glEnableVertexAttribArray(position);quad.position(0);GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,0,quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisableVertexAttribArray(position);
        int error=GLES20.glGetError();if(error!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Side shader GL error "+error);
    }
    private void uniform(int index,float[] value){GLES20.glUniform4fv(uniforms[index],1,value,0);}
    void release(){GLES20.glDeleteProgram(program);}
}
