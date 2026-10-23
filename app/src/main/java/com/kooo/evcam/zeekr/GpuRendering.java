package com.kooo.evcam.zeekr;

/** One rendering policy; custom selections survive Off/All round trips. */
public final class GpuRendering {
    private GpuRendering() {}
    public static final String MODE = "gpu_rendering_mode";
    public static final String[] FEATURES = {"preview", "360", "side", "mirror", "playback"};
    public static String normalize(String mode) {
        return "all".equals(mode) || "custom".equals(mode) ? mode : "off";
    }
    public static boolean enabled(String mode, boolean selected) {
        return "all".equals(mode) || ("custom".equals(mode) && selected);
    }
    /** Live camera views must switch together with their producer at Apply, not at Save. */
    public static boolean applied(android.content.Context context, String feature) {
        com.kooo.evcam.camera.MultiCameraManager manager =
                com.kooo.evcam.camera.CameraManagerHolder.getInstance().getCameraManager();
        com.kooo.evcam.camera.CameraSessionOptions options = manager == null ? null : manager.getSessionOptions();
        if (options != null) {
            switch (feature) {
                case "preview": return options.gpuPreview;
                case "360": return options.gpu360;
                case "side": return options.gpuSide;
                case "mirror": return options.gpuMirror;
            }
        }
        // Standalone previews before pipeline creation and playback retain their normal defaults.
        return new com.kooo.evcam.AppConfig(context).isGpuFeatureEnabled(feature);
    }
    public static String preference(String feature) {
        switch(feature) {
            case "preview": return "pref_gpu_preview";
            case "360": return "pref_gpu_360";
            case "side": return "pref_gpu_side";
            case "mirror": return "pref_gpu_mirror";
            case "playback": return "pref_gpu_playback";
            default: throw new IllegalArgumentException(feature);
        }
    }
    public static String key(String feature) {
        switch (feature) {
            case "preview": return "gpu_fisheye_preview";
            case "360": return "gpu_bird_eye";
            case "side": return "gpu_side";
            case "mirror": return "gpu_mirror";
            case "playback": return "gpu_fisheye_video";
            default: throw new IllegalArgumentException(feature);
        }
    }
}
