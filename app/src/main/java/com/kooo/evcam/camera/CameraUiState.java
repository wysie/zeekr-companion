package com.kooo.evcam.camera;
import com.kooo.evcam.R;
/** Read-only presentation of scheduler facts; never changes camera eligibility or recovery. */
public enum CameraUiState {
 STARTING(R.string.camera_state_starting), WAITING(R.string.camera_state_waiting),
 STREAMING(R.string.camera_state_streaming), IN_USE(R.string.camera_state_in_use),
 RECOVERING(R.string.camera_state_recovering), STOPPED(R.string.camera_state_stopped),
 PAUSED(R.string.camera_state_paused), NOT_ACTIVE(R.string.camera_state_not_active),
 UNASSIGNED(R.string.camera_state_unassigned);
 public final int text; CameraUiState(int text){this.text=text;}
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
