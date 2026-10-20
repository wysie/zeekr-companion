package com.kooo.evcam.zeekr;

import android.content.Context;
import android.util.Size;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import org.json.JSONException;
import org.json.JSONObject;

/** Immutable picture/geometry snapshot. Reading this never changes configuration or opens a window. */
final class SideQualitySpec {
    static final long MAX_PIXELS = 8_388_608L;
    final CompositeStreamGeometry.Plan plan;
    final int lane, width, height, fov, yaw, pitch, rotation, sharpening;
    final boolean straighten, mirrored, autoFit, gpu;
    final String sampling, geometry, cameraId, popupState;
    final SideViewFraming framing = new SideViewFraming();
    SideQualitySpec(CompositeStreamGeometry.Plan plan, int lane, int width, int height, AppConfig config,
            boolean gpu, String geometry) {
        this.cameraId=StreamLayoutTable.compositeCameraId();
        this.popupState=SideViewPopupService.qualityWindowState(lane);
        this.plan=plan;this.lane=lane;this.width=width;this.height=height;this.gpu=gpu;this.geometry=geometry;
        fov=config.getSidePopupFov();yaw=config.getSidePopupYaw();pitch=config.getSidePopupPitch();
        rotation=config.getSidePopupRotation(lane);straighten=config.isSidePopupStraighten();
        mirrored=config.isSidePopupMirrored();autoFit=config.isSidePopupAutoFit();
        sampling=config.getSideUpscaling();sharpening=config.getSideSharpening();
        if(lane!=LaneCycle.LEFT && lane!=LaneCycle.RIGHT)throw new IllegalArgumentException("Select a side camera");
        CompositeStreamGeometry.Lane source=plan.lane(lane);
        requireDimensions(source.width,source.height,width,height);
        framing.configure(fov,yaw,pitch,lane,(float)width/height,rotation,autoFit);
    }
    static void requireDimensions(int nativeWidth,int nativeHeight,int width,int height) {
        if(nativeWidth<=0||nativeHeight<=0||width<=0||height<=0||width>4096||height>4096||nativeWidth>4096||nativeHeight>4096
                ||(long)width*height+(long)nativeWidth*nativeHeight>MAX_PIXELS)
            throw new IllegalArgumentException("Comparison exceeds native image memory limit");
    }
    static SideQualitySpec create(Context context, CompositeStreamGeometry.Plan plan, int lane, boolean gpu) {
        AppConfig config=new AppConfig(context);
        Size actual=SideViewPopupService.qualityWindowSize(lane);
        android.util.DisplayMetrics m=context.getResources().getDisplayMetrics();
        int minimum=2*context.getResources().getDimensionPixelSize(R.dimen.side_control_target)
                +3*context.getResources().getDimensionPixelSize(R.dimen.side_control_margin);
        SidePopupSizing.Size saved=SidePopupSizing.dimensions(config.getSidePopupSizePercent(),
                config.getSidePopupAspect(),m.widthPixels,m.heightPixels,minimum);
        return new SideQualitySpec(plan,lane,actual==null?saved.width:actual.getWidth(),
                actual==null?saved.height:actual.getHeight(),config,gpu,
                actual==null?"saved normal popup geometry":"current same-side popup geometry");
    }
    SideGpuPicture adjusted() {
        return SideGpuPicture.side(plan,lane,width,height,straighten,fov,yaw,pitch,rotation,mirrored,framing)
                .withSampling(sampling,sharpening);
    }
    SideGpuPicture original() {
        CompositeStreamGeometry.Lane source=plan.lane(lane);
        return new SideGpuPicture(plan,lane,RearViewGeometry.Viewport.full(),0,90,0,0,0,false,source.width,source.height);
    }
    JSONObject metadata() throws JSONException {
        CompositeStreamGeometry.Lane source=plan.lane(lane);
        return new JSONObject().put("schema_version",1).put("camera_id",cameraId).put("side",lane==LaneCycle.LEFT?"left":"right")
                .put("source_width",plan.frameWidth).put("source_height",plan.frameHeight)
                .put("native_side_x",source.x).put("native_side_y",source.y)
                .put("native_side_width",source.width).put("native_side_height",source.height)
                .put("render_width",width).put("render_height",height).put("geometry",geometry)
                .put("popup_window_at_request",popupState).put("window_screenshot",false)
                .put("backend",gpu?"GPU side shader":"Canvas side mesh")
                .put("straighten",straighten).put("fov_degrees",fov)
                .put("effective_fov_degrees",straighten?framing.effectiveFovDegrees():fov)
                .put("aim_back_degrees",yaw).put("aim_up_degrees",pitch).put("rotation_degrees",rotation)
                .put("mirrored",mirrored).put("auto_fit",autoFit).put("saved_sampling",sampling)
                .put("saved_sharpening_percent",sharpening).put("advanced_sampling_applied",gpu)
                .put("native_source_file","native-side.png").put("rendered_file","popup-render.png")
                .put("pair_same_source_frame",true)
                .put("render_provenance","Same-frame diagnostic render using popup picture settings and its renderer; not a live-window screenshot")
                .put("overlay_policy","Camera pixels only: labels, arrows, window controls and HUD tone/crop are excluded")
                .put("native_policy","Decoded native side-camera lane before app aiming/correction/mirroring; not sensor RAW");
    }
}
