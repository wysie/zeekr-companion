package com.kooo.evcam.zeekr;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import java.nio.FloatBuffer;

/** Single-pass per-pixel crop, rectilinear correction or aimed/rolled side-camera correction. */
final class SideGpuRenderer {
    private static final String VERTEX = "attribute vec2 a; varying vec2 p; void main(){gl_Position=vec4(a,0.,1.);p=vec2((a.x+1.)*.5,(1.-a.y)*.5);}";
    private static final String FRAGMENT = "#extension GL_OES_EGL_image_external : require\nprecision highp float;\n"
            + "varying vec2 p; uniform samplerExternalOES image; uniform mat4 textureMatrix;"
            + "uniform vec4 lane, view, lens, turn, limits; uniform float mode; uniform vec2 quality;"
            // All reconstruction taps stay inside the selected native lane, before the OES matrix.
            + "vec4 nativeSample(vec2 s){s=lane.xy+clamp(s,limits.zw,vec2(1.)-limits.zw)*lane.zw;"
            + "vec4 t=textureMatrix*vec4(s.x,1.-s.y,0.,1.);return texture2D(image,t.xy);}"
            // Mitchell-Netravali (B=C=1/3) and Catmull-Rom (B=0,C=1/2) cubic kernels.
            + "float weight(float x){x=abs(x);float b=quality.x<1.5?1./3.:0.;float c=quality.x<1.5?1./3.:.5;"
            + "if(x<1.)return ((12.-9.*b-6.*c)*x*x*x+(-18.+12.*b+6.*c)*x*x+6.-2.*b)/6.;"
            + "if(x<2.)return ((-b-6.*c)*x*x*x+(6.*b+30.*c)*x*x+(-12.*b-48.*c)*x+8.*b+24.*c)/6.;return 0.;}"
            + "vec4 reconstruct(vec2 s){if(quality.x<.5)return nativeSample(s);"
            + "vec2 pixel=2.*limits.zw;vec2 pos=s/pixel-.5;vec2 base=floor(pos),f=fract(pos);"
            + "vec4 sum=vec4(0.),lo=vec4(1.),hi=vec4(0.);"
            + "for(int y=-1;y<=2;y++){for(int x=-1;x<=2;x++){vec2 offset=vec2(float(x),float(y));"
            + "vec4 v=nativeSample((base+offset+.5)*pixel);lo=min(lo,v);hi=max(hi,v);"
            + "sum+=v*weight(offset.x-f.x)*weight(offset.y-f.y);}}return clamp(sum,lo,hi);}"
            + "vec4 samplePicture(vec2 s){vec4 centre=reconstruct(s);if(quality.y<.0001)return centre;"
            + "vec2 pixel=2.*limits.zw;vec4 a=nativeSample(s+vec2(pixel.x,0.)),b=nativeSample(s-vec2(pixel.x,0.));"
            + "vec4 c=nativeSample(s+vec2(0.,pixel.y)),d=nativeSample(s-vec2(0.,pixel.y));"
            + "vec4 raw=nativeSample(s);vec4 lo=min(centre,min(raw,min(min(a,b),min(c,d))));"
            + "vec4 hi=max(centre,max(raw,max(max(a,b),max(c,d))));"
            + "return clamp(centre+quality.y*(raw-(a+b+c+d)*.25),lo,hi);}"
            + "void main(){ vec2 q=p; if(limits.x>.5)q.x=1.-q.x; vec2 s;"
            + "if(mode<.5){vec2 d=q-.5; d=vec2(lens.z*d.x+lens.w*d.y/lens.y,-lens.w*d.x*lens.y+lens.z*d.y)/limits.y; s=view.xy+(d+.5)*view.zw;}"
            + "else if(mode<1.5){vec2 d=((view.xy+q*view.zw)*2.-1.)*lens.x;float r=length(d);s=r<.000001?vec2(.5):clamp(.5+d/r*(atan(r)/1.5707963268)*.5,0.,1.);}"
            + "else {vec2 d=(q*2.-1.);d.y/=lens.y;d=vec2(lens.z*d.x+lens.w*d.y,-lens.w*d.x+lens.z*d.y)*lens.x;"
            + "float yy=d.y*turn.x-turn.y;float zz=d.y*turn.y+turn.x;vec3 ray=vec3(d.x*turn.z+zz*turn.w,yy,-d.x*turn.w+zz*turn.z);"
            + "float r=length(ray.xy);float angle=acos(clamp(ray.z/length(ray),-1.,1.));s=vec2(.5)+(r<.000001?vec2(0.):ray.xy/r)*(angle/1.5707963268)*.5;}"
            + "if(any(lessThan(s,vec2(0.)))||any(greaterThan(s,vec2(1.)))){gl_FragColor=vec4(0.,0.,0.,1.);return;}"
            + "gl_FragColor=samplePicture(s);}";
    private final int program, position, sampler, matrixHandle, modeHandle, qualityHandle;
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
        qualityHandle=GLES20.glGetUniformLocation(program,"quality");
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
        GLES20.glUniform2f(qualityHandle,picture.sampling,picture.sharpening);
        GLES20.glEnableVertexAttribArray(position);quad.position(0);GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,0,quad);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);GLES20.glDisableVertexAttribArray(position);
        int error=GLES20.glGetError();if(error!=GLES20.GL_NO_ERROR)throw new IllegalStateException("Side shader GL error "+error);
    }
    private void uniform(int index,float[] value){GLES20.glUniform4fv(uniforms[index],1,value,0);}
    void release(){GLES20.glDeleteProgram(program);}
}
