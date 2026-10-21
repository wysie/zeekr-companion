package com.kooo.evcam.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Types consumed by this version, including keys still absent on a fresh installation.
 * Unknown extension keys retain their encoded type for backup forward compatibility.
 * Keep current consumers here when adding a non-enum preference (coverage regression checks direct AppConfig reads).
 */
final class SettingsBackupTypes {
    private static final Map<String, Class<?>> APP = new HashMap<>();
    static {
        add(Boolean.class,
                "009_hud_side_follow auto_start_on_boot auto_start_recording camera_holder_suspects camera_mapping_009 " +
                "drive_auto_start drive_auto_stop first_launch floating_duration_visible floating_position_locked " +
                "footage_lock footage_lock_brake footage_lock_flash force_h264_encoding gpu_bird_eye gpu_fisheye_preview " +
                "gpu_fisheye_video gpu_mirror gpu_side hud_custom_picture_initialized hud_during_factory_views " +
                "hud_follow_popup_picture hud_layout_009 hud_picture_auto_fit hud_picture_label hud_picture_straighten " +
                "hud_side_follow hud_side_mirror image_adjust_enabled info_bar_enabled keep_alive_enabled language_chosen " +
                "license_plate_enabled photo_fisheye photo_via_jpeg photo_via_jpeg_reset_2_10_12 rail_side_chosen " +
                "raw_frame_dump rearview_button_mode rearview_camera_label rearview_enabled rearview_fisheye " +
                "rearview_front_rear rearview_guide_seen recording_floating_enabled recording_stats_enabled " +
                "reduce_motion_recording relay_write_enabled screen_off_keep_recording screen_off_recording " +
                "side_popup_auto_fit side_popup_enabled side_popup_hide_mirror side_popup_instant side_popup_mirror " +
                "side_popup_replace_mirror side_popup_straighten signal_scan source_mapping_migrated_v1 " +
                "timestamp_watermark_enabled ui_left_for_screen_off update_include_beta usb_internal_fallback " +
                "vehicle_status_panel watermark_spec_enabled");
        add(Integer.class,
                "009_hud_side_inner_crop 009_hud_side_left 009_hud_side_shape 009_hud_side_size 009_hud_side_vertical " +
                "awb_mode edge_mode effect_mode exposure_compensation floating_window_alpha hud_picture_fov " +
                "hud_picture_pitch hud_picture_rotation_left hud_picture_rotation_right hud_picture_yaw hud_side_contrast " +
                "hud_side_contrast_night hud_side_fade hud_side_inner_crop hud_side_left hud_side_night hud_side_opacity " +
                "hud_side_shape hud_side_size hud_side_vertical noise_reduction_mode photo_storage_limit_gb " +
                "rearview_height rearview_lane rearview_width rearview_x rearview_y recording_floating_button_size " +
                "recording_floating_time_text_size recording_floating_x recording_floating_y screen_off_wake_min " +
                "side_output side_popup_close_delay_ms side_popup_fov side_popup_min_speed side_popup_pitch " +
                "side_popup_rotation_left side_popup_rotation_right side_popup_size side_popup_vertical side_popup_x " +
                "side_popup_y side_popup_yaw side_sharpening tonemap_mode video_storage_limit_gb");
        add(Long.class,
                "signal_scan_since");
        add(Float.class,
                "photo_fisheye_fov rearview_fov rearview_pan");
        add(String.class,
                "custom_sd_card_path detected_surround_id floating_double_tap_action floating_long_press_action " +
                "floating_tap_action gpu_rendering_mode hud_safe_area_calibration last_detected_sd_path license_plate " +
                "main_preview_arrangement side_popup_aspect side_upscaling storage_location vehicle_profile_mode");
        add(Set.class,
                "info_bar_items");
        for (SettingSpec spec : SettingsRegistry.ALL) APP.put(spec.key, String.class);
    }
    private static void add(Class<?> type, String names) {
        for (String name : names.split(" ")) APP.put(name, type);
    }
    static Class<?> expected(String store, String key) {
        switch (store) {
            case "app_config":
                if (key.startsWith("source_id_") || key.startsWith("zeekr_camera_override_")) return String.class;
                return APP.get(key);
            case "profiles": return "current".equals(key) ? String.class : null;
            case "bird_eye_appearance":
                return "vehicle_colour".equals(key) || "vehicle_model".equals(key) ? String.class : null;
            case "bird_eye_window":
                if ("popup".equals(key)) return Boolean.class;
                return key.matches("size|x|y") ? Integer.class : null;
            case "bird_eye_calibration":
                if (key.matches("camera_[0-3]_mirror")) return Boolean.class;
                if (key.matches("length|width|margin|quality|projection_profile|image_fit_margin|camera_[0-3]_(lane|offset_x|offset_y|height|yaw|pitch|roll|fov|cx|cy)"))
                    return Integer.class;
                return null;
            default: return null;
        }
    }
    static boolean accepts(String store, String key, Object value) {
        Class<?> expected = expected(store, key);
        return expected == null || expected.isInstance(value);
    }
    private SettingsBackupTypes() {}
}
