package com.kooo.evcam.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.profile.*;
import com.kooo.evcam.ui.CamDialogs;
import java.util.*;

/** One ordinary source page and picker shared by Settings, Developer and the stream editor. */
public class CameraSourcesFragment extends PreferenceFragmentCompat {
    private static java.lang.ref.WeakReference<androidx.appcompat.app.AlertDialog> activePicker = new java.lang.ref.WeakReference<>(null);
    private int factsGeneration;
    @Override public void onCreatePreferences(@Nullable Bundle state, @Nullable String rootKey) {
        Context context = requireContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);
        Preference help = new Preference(context); help.setTitle(R.string.camera_sources_title);
        help.setSummary(R.string.camera_sources_help); help.setSelectable(false); screen.addPreference(help);
        Profile profile = new ProfileStore(context).current();
        AppConfig config = new AppConfig(context);
        for (String role : new String[]{CameraProfile.ROLE_COMPOSITE, CameraProfile.ROLE_ROAD_FRONT,
                CameraProfile.ROLE_ROAD_REAR, CameraProfile.ROLE_CABIN_1, CameraProfile.ROLE_CABIN_2, CameraProfile.ROLE_MANUAL}) {
            if (CameraProfile.ROLE_CABIN_2.equals(role) && config.is009CameraMappingEnabled()
                    && config.getSourceCameraId(role) == null && !profile.camera(role).enabled) continue;
            Preference row = new Preference(context); row.setKey(role); row.setTitle(roleLabel(context, role));
            row.setOnPreferenceClickListener(pref -> {
                pick(context, role, new ProfileStore(context).current(), this::refresh); return true;
            }); screen.addPreference(row);
        }
        Preference reset = new Preference(context); reset.setKey("reset_sources");
        PreferenceRows.markAction(reset);
        reset.setTitle(R.string.camera_sources_reset); reset.setSummary(R.string.camera_sources_reset_note);
        reset.setOnPreferenceClickListener(pref -> {
            CamDialogs.show(new MaterialAlertDialogBuilder(context, R.style.Theme_Cam_MaterialAlertDialog)
                    .setTitle(R.string.camera_sources_reset).setMessage(R.string.camera_sources_reset_note)
                    .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                        new AppConfig(context).resetSourceCameraIds(); refresh(); saved(context);
                    }).setNegativeButton(R.string.action_cancel, null)); return true;
        }); screen.addPreference(reset);
        PreferenceRows.apply(screen); refresh();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state); PreferenceRows.styleList(this, 28, 16);
        int generation = ++factsGeneration;
        CameraSettingsFacts.load(requireContext(), () -> {
            if (generation == factsGeneration && isAdded() && getView() != null) refresh();
        });
    }
    @Override public void onDestroyView() { factsGeneration++; super.onDestroyView(); }
    private void refresh() {
        if (getContext() == null) return;
        AppConfig config = new AppConfig(requireContext());
        for (String role : RecordingSources.ALL_ROLES) {
            Preference row = findPreference(role); if (row == null) continue;
            String choice = config.getSourceCameraId(role);
            String id = ProfileSizes.cameraIdFor(requireContext(), role);
            String resolved = id == null ? getString(R.string.camera_source_not_found) : getString(R.string.opt_camera_id, id);
            row.setSummary(choice == null ? getString(R.string.camera_source_automatic, resolved)
                    : choice.isEmpty() ? getString(R.string.record_source_unassigned)
                    : getString(R.string.camera_source_manual, resolved));
        }
    }
    static String roleLabel(Context context, String role) {
        int resource = CameraProfile.ROLE_COMPOSITE.equals(role) ? R.string.record_group_surround
                : CameraProfile.ROLE_CABIN_1.equals(role) ? R.string.record_preview_cabin
                : CameraProfile.ROLE_CABIN_2.equals(role) ? (new AppConfig(context).is009CameraMappingEnabled()
                    ? R.string.camera_source_legacy : R.string.slot_cabin_rear)
                : CameraProfile.ROLE_ROAD_FRONT.equals(role) ? R.string.slot_road_front
                : CameraProfile.ROLE_ROAD_REAR.equals(role) ? R.string.slot_road_rear : R.string.record_group_manual;
        return context.getString(resource);
    }
    static void saved(Context context) { Toast.makeText(context, R.string.msg_mapping_saved, Toast.LENGTH_LONG).show(); }

    static void pick(Context context, String role, Profile profile, Runnable changed) {
        androidx.appcompat.app.AlertDialog existing = activePicker.get();
        if (existing != null && existing.isShowing() && ownerAlive(existing.getContext())
                && owner(existing.getContext()) == owner(context)) return;
        AppConfig config = new AppConfig(context);
        Set<String> choices = new LinkedHashSet<>();
        Collections.addAll(choices, com.kooo.evcam.camera.CameraCapabilities.advertisedIds());
        choices.addAll(context.getSharedPreferences("camera_discovery", 0).getStringSet("captured_ids", Collections.emptySet()));
        if (config.is009CameraMappingEnabled()) Collections.addAll(choices, "0", "1", "2", "3");
        String detected = config.getDetectedSurroundId(); if (detected != null) choices.add(detected);
        for (String candidate : RecordingSources.ALL_ROLES) {
            String explicit = config.getSourceCameraId(candidate);
            if (explicit != null && !explicit.isEmpty()) choices.add(explicit);
            String resolved = ProfileSizes.cameraIdFor(context, candidate); if (resolved != null) choices.add(resolved);
        }
        List<String> ids = new ArrayList<>(choices);
        List<String> labels = new ArrayList<>();
        labels.add(context.getString(R.string.opt_camera_auto));
        labels.add(context.getString(R.string.record_source_unassigned));
        for (String id : ids) labels.add(context.getString(R.string.opt_camera_id, id));
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(context,
                androidx.appcompat.R.layout.select_dialog_singlechoice_material, labels);
        String old = config.getSourceCameraId(role);
        int[] selected = {old == null ? 0 : old.isEmpty() ? 1 : Math.max(0, ids.indexOf(old) + 2)};
        androidx.appcompat.app.AlertDialog picker = CamDialogs.show(new MaterialAlertDialogBuilder(context, R.style.Theme_Cam_MaterialAlertDialog)
                .setTitle(context.getString(R.string.record_source_pick, roleLabel(context, role)))
                .setSingleChoiceItems(adapter, selected[0], (dialog, which) -> selected[0] = which)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    String picked = selected[0] == 0 ? null : selected[0] == 1 ? "" : ids.get(selected[0] - 2);
                    String resolved = picked == null ? ProfileSizes.automaticCameraIdFor(context, role) : picked;
                    Set<String> active = ProfileChannels.of(profile, config.getPreviewSourceMode()).keys;
                    if (resolved != null && !resolved.isEmpty()) for (CameraProfile camera : profile.cameras) {
                        if ((camera.enabled || active.contains(RecordingSources.key(camera.role)))
                                && !camera.role.equals(role) && resolved.equals(ProfileSizes.cameraIdFor(context, camera.role))) {
                            Toast.makeText(context, R.string.record_source_duplicate, Toast.LENGTH_LONG).show(); return;
                        }
                    }
                    config.setSourceCameraId(role, picked);
                    if ("".equals(picked)) {
                        // Explicit Unassigned preserves the existing editor behavior, without normalising other fields.
                        ProfileStore store = new ProfileStore(context); Profile stored = store.current(); boolean update = false;
                        boolean roads = CameraProfile.ROLE_ROAD_FRONT.equals(role) || CameraProfile.ROLE_ROAD_REAR.equals(role);
                        for (CameraProfile camera : stored.cameras) if (camera.role.equals(role) || roads &&
                                (CameraProfile.ROLE_ROAD_FRONT.equals(camera.role) || CameraProfile.ROLE_ROAD_REAR.equals(camera.role))) {
                            if (camera.enabled) { camera.enabled = false; update = true; }
                        }
                        if (update) store.save(stored);
                    }
                    changed.run(); saved(context);
                }).setNegativeButton(R.string.action_cancel, null));
        activePicker = new java.lang.ref.WeakReference<>(picker);
        // Keep one window and stable choice indexes. Cancel can never race a second dialog.
        // Existing/manual/009 choices stay usable even while the vendor service is blocked.
        CameraSettingsFacts.load(context, () -> {
            if (!picker.isShowing() || !ownerAlive(context)) return;
            for (String id : com.kooo.evcam.camera.CameraCapabilities.advertisedIds()) if (!ids.contains(id)) {
                ids.add(id);
                adapter.add(context.getString(R.string.opt_camera_id, id));
            }
        });
    }

    private static boolean ownerAlive(Context context) {
        context = owner(context);
        return !(context instanceof android.app.Activity)
                || (!((android.app.Activity) context).isFinishing() && !((android.app.Activity) context).isDestroyed());
    }

    private static Context owner(Context context) {
        while (context instanceof android.content.ContextWrapper && !(context instanceof android.app.Activity))
            context = ((android.content.ContextWrapper) context).getBaseContext();
        return context;
    }
}
