package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.TextureView;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.StorageHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Explicit same-frame diagnostic, isolated from camera/session/recording authority. */
final class SideQualityCapture {
    private static final ExecutorService FILES=Executors.newSingleThreadExecutor();
    private static final AtomicReference<Request> ACTIVE=new AtomicReference<>();
    interface Listener { void finished(File file,String error); }
    interface Pixels { void complete(SideQualityPair pair,String error); }
    static final class Request {
        final AtomicBoolean cancelled=new AtomicBoolean();
        final Handler ui=new Handler(Looper.getMainLooper());
        final Context app;
        final SideQualitySpec spec;
        private final Listener listener;
        private final AtomicBoolean settled=new AtomicBoolean();
        private final Runnable timeout=() -> finish(null,"Capture timed out; keep a live Surround preview open and try again.");
        Request(Context context,SideQualitySpec spec,Listener listener) {
            app=context.getApplicationContext();this.spec=spec;this.listener=listener;
        }
        void cancel() {
            cancelled.set(true);settled.set(true);ui.removeCallbacks(timeout);ACTIVE.compareAndSet(this,null);
        }
        void finish(File file,String error) {
            ui.post(() -> {
                if(!settled.compareAndSet(false,true))return;
                ui.removeCallbacks(timeout);ACTIVE.compareAndSet(this,null);
                boolean notify=!cancelled.get();
                if(error!=null)cancelled.set(true);
                if(notify)listener.finished(file,error);
            });
        }
        void pixels(SideQualityPair pair,String error) {
            if(pair==null){finish(null,error);return;}
            if(cancelled.get()||settled.get()){pair.close();return;}
            FILES.execute(() -> {
                try(SideQualityPair owned=pair) {
                    if(cancelled.get()||settled.get())return;
                    if(owned.rendered==null)owned.rendered=SideQualityCanvas.render(owned.original,spec);
                    if(cancelled.get()||settled.get())return;
                    owned.metadata.put("version",app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName);
                    File file=save(StorageHelper.getExportDir(app),owned,cancelled);
                    app.getSharedPreferences("side_quality_exports",0).edit().putString("last_file",file.getAbsolutePath()).apply();
                    finish(file,null);
                } catch(Exception|OutOfMemoryError failure) {
                    AppLog.w("SideQualityCapture","Comparison export failed: "+failure);
                    finish(null,"Could not save comparison. Check storage and try again.");
                }
            });
        }
    }
    static Request start(Context context,TextureView source,SideQualitySpec spec,long sourceObservedAt,Listener listener) {
        Request request=new Request(context,spec,listener);
        if(!ACTIVE.compareAndSet(null,request))return null;
        request.ui.postDelayed(request.timeout,30_000);
        FisheyeGlPipe shared=PreviewDewarp.rawPipeFor(source);
        if(shared!=null && shared.isAlive()) {
            if(!shared.captureSideQuality(spec,request.cancelled,request::pixels))request.finish(null,"Source became unavailable. Try again.");
        } else if(spec.gpu) {
            request.finish(null,"GPU source became unavailable. Try again.");
        } else {
            // Main-thread TextureView acquisition cannot advance during the one native bitmap copy.
            // Both exported images then use this single immutable copy, never separate live reads.
            Bitmap full=null,original=null;
            try {
                if(!CameraFrameFreshness.freshAt(sourceObservedAt,SystemClock.elapsedRealtime()) || PreviewDewarp.isActive(source))
                    throw new IllegalStateException("Native source unavailable");
                if((long)spec.plan.frameWidth*spec.plan.frameHeight>SideQualitySpec.MAX_PIXELS
                        || spec.plan.frameWidth>8192 || spec.plan.frameHeight>8192)
                    throw new IllegalStateException("Source exceeds native copy limit");
                SurfaceTexture identity=source.getSurfaceTexture();
                long timestamp=identity==null?0:identity.getTimestamp();
                if(timestamp<=0)throw new IllegalStateException("No source frame");
                full=source.getBitmap(spec.plan.frameWidth,spec.plan.frameHeight);
                if(full==null || source.getSurfaceTexture()!=identity || identity.getTimestamp()!=timestamp
                        || (full.getPixel(full.getWidth()/2,full.getHeight()/2)>>>24)!=255)
                    throw new IllegalStateException("Source changed during capture");
                CompositeStreamGeometry.Lane lane=spec.plan.lane(spec.lane);
                original=Bitmap.createBitmap(full,lane.x,lane.y,lane.width,lane.height);
                if(original==full)full=null;
                SideQualityPair pair=new SideQualityPair(original,null,spec.metadata()
                        .put("source_timestamp_ns",timestamp).put("source_observed_elapsed_ms",sourceObservedAt)
                        .put("capture_wall_ms",System.currentTimeMillis()).put("source_readback","single native TextureView bitmap")
                        .put("source_pipeline",PreviewDewarp.describeOutput(source)));
                original=null;request.pixels(pair,null);
            } catch(Exception|OutOfMemoryError failure) {
                AppLog.w("SideQualityCapture","Native comparison capture unavailable: "+failure);
                request.finish(null,"Native source unavailable. Keep a live Surround preview open and try again.");
            } finally {if(full!=null)full.recycle();if(original!=null)original.recycle();}
        }
        return request;
    }
    /** Only a complete, unique ZIP is published; failed/cancelled partial writes are removed. */
    static File save(File directory,SideQualityPair pair,AtomicBoolean cancelled) throws Exception {
        if(directory==null || (!directory.isDirectory()&&!directory.mkdirs()))throw new IOException("No export directory");
        String name="BlindQuality_"+pair.metadata.optString("side","side")+"_"
                +new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+"_"+UUID.randomUUID()+".zip";
        File target=new File(directory,name),partial=new File(directory,name+".partial");
        boolean completed=false;
        try {
            try(FileOutputStream file=new FileOutputStream(partial);ZipOutputStream zip=new ZipOutputStream(file)) {
                image(zip,"native-side.png",pair.original,cancelled);
                image(zip,"popup-render.png",pair.rendered,cancelled);
                check(cancelled);zip.putNextEntry(new ZipEntry("comparison.json"));
                zip.write(pair.metadata.toString(2).getBytes(StandardCharsets.UTF_8));zip.closeEntry();zip.finish();file.getFD().sync();
            }
            check(cancelled);
            if(!partial.renameTo(target))throw new IOException("Could not finish export");
            completed=true;return target;
        } finally {if(!completed)partial.delete();}
    }
    private static void image(ZipOutputStream zip,String name,Bitmap image,AtomicBoolean cancelled) throws IOException {
        check(cancelled);zip.putNextEntry(new ZipEntry(name));
        if(image==null||!image.compress(Bitmap.CompressFormat.PNG,100,zip))throw new IOException("PNG encoding failed");
        zip.closeEntry();
    }
    private static void check(AtomicBoolean cancelled) throws IOException {if(cancelled.get())throw new IOException("Capture cancelled");}
}
