package com.kooo.evcam.ui;

import android.view.View;
import android.widget.TextView;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.camera.CameraChanges;
import com.kooo.evcam.camera.CameraManagerHolder;

/** Same status and action in the main header and Settings, scoped to each view tree. */
public final class CameraChangesBanner {
    private final View root;
    private final TextView message, action;
    private final MainActivity activity;
    public CameraChangesBanner(MainActivity activity, View root, int messageId, int actionId) {
        this.activity = activity; this.root = root;
        message = root.findViewById(messageId); action = root.findViewById(actionId);
        action.setOnClickListener(view -> { CameraChanges.request(activity); refresh(null); });
    }
    public void refresh(CharSequence detail) {
        boolean pending = CameraChanges.busy() || CameraManagerHolder.getInstance().hasPendingChannelChange(activity);
        root.setVisibility(pending ? View.VISIBLE : View.GONE);
        message.setVisibility(pending ? View.VISIBLE : View.GONE);
        CharSequence label = activity.getString(CameraChanges.busy() ? R.string.camera_apply_working
                : CameraChanges.failed() ? R.string.camera_apply_failed : R.string.ui_pending_camera_changes);
        if (pending && !CameraChanges.busy() && !CameraChanges.failed() && detail != null) label = detail;
        if (!label.toString().contentEquals(message.getText())) message.setText(label);
        action.setText(CameraChanges.failed() ? R.string.camera_apply_retry : R.string.camera_apply_action);
        action.setEnabled(!CameraChanges.busy());
        action.setTextColor(androidx.core.content.ContextCompat.getColor(activity,
                CameraChanges.busy() ? R.color.text_secondary : R.color.energy_text));
    }
}
