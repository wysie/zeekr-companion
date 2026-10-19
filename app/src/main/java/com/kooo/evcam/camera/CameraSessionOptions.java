package com.kooo.evcam.camera;

import android.content.Context;
import com.kooo.evcam.AppConfig;

/** Surface topology fixed for one camera pipeline; picture tuning remains live. */
public final class CameraSessionOptions {
    public final boolean gpuPreview, gpu360, jpeg;
    public final com.kooo.evcam.profile.Profile profile;
    public CameraSessionOptions(boolean gpuPreview, boolean gpu360, boolean jpeg) {
        this(gpuPreview, gpu360, jpeg, null);
    }
    public CameraSessionOptions(boolean gpuPreview, boolean gpu360, boolean jpeg, com.kooo.evcam.profile.Profile profile) {
        this.gpuPreview = gpuPreview; this.gpu360 = gpu360; this.jpeg = jpeg;
        this.profile = profile == null ? null : com.kooo.evcam.profile.Profile.fromMap(profile.toMap());
    }
    public static CameraSessionOptions read(Context context) {
        AppConfig config = new AppConfig(context);
        return new CameraSessionOptions(config.isGpuFisheyePreview(), config.isGpuBirdEye(), config.isPhotoViaJpegEnabled());
    }
}
