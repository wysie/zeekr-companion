package com.kooo.evcam.camera;
import com.kooo.evcam.R;
/** Read-only presentation of scheduler facts; never changes camera eligibility or recovery. */
public enum CameraUiState {
 STARTING(R.string.camera_state_starting,R.string.camera_preview_starting),
 WAITING(R.string.camera_state_waiting,R.string.camera_preview_camera_waiting),
 STREAMING(R.string.camera_state_streaming,R.string.camera_preview_waiting),
 IN_USE(R.string.camera_state_in_use,R.string.camera_preview_busy),
 RECOVERING(R.string.camera_state_recovering,R.string.camera_preview_reconnecting),
 STOPPED(R.string.camera_state_stopped,R.string.camera_preview_unavailable),
 PAUSED(R.string.camera_state_paused,R.string.camera_preview_paused),
 NOT_ACTIVE(R.string.camera_state_not_active,R.string.camera_preview_inactive),
 UNASSIGNED(R.string.camera_state_unassigned,R.string.camera_preview_unassigned);
 public final int text,previewText;
 CameraUiState(int text,int previewText){this.text=text;this.previewText=previewText;}
 public static CameraUiState of(boolean built, boolean present, ChannelRules.Condition condition,
          boolean paused, boolean opened, boolean frames) {
  if(!built)return STARTING;if(!present)return NOT_ACTIVE;if(paused)return PAUSED;
  if(condition==ChannelRules.Condition.TAKEN)return IN_USE;
  if(condition==ChannelRules.Condition.GAVE_UP)return STOPPED;
  if(condition==ChannelRules.Condition.RESCUING)return RECOVERING;
  if(condition==ChannelRules.Condition.STREAMING&&frames)return STREAMING;
  return opened?WAITING:STARTING;
 }
}
