package com.kooo.evcam.ui;
import android.content.Context;
import com.kooo.evcam.R;
import com.kooo.evcam.camera.*;
import com.kooo.evcam.profile.*;
/** One description of recording authority, separate from observed preview progression. */
public final class CameraStatusText {
 private CameraStatusText(){}
 public static String progress(Context c, RecordingLanes.Summary summary) {
  return summary == null || summary.planned.isEmpty() ? c.getString(R.string.camera_status_title)
    : c.getString(R.string.camera_recording_count,summary.liveKeys.size(),summary.planned.size());
 }
 public static String report(Context c) {
  MultiCameraManager manager=CameraManagerHolder.getInstance().getCameraManager();
  RecordingLanes.Summary summary=com.kooo.evcam.recording.RecordingCoordinator.get(c).summary();
  StringBuilder out=new StringBuilder(progress(c,summary)).append("\n\n").append(c.getString(R.string.camera_status_help));
  Profile profile=new ProfileStore(c).current();
  for(String role:RecordingSources.ALL_ROLES) {
   String key=RecordingSources.key(role);SingleCamera source=manager==null?null:manager.getCamera(key);
   if(source==null && (profile.camera(role)==null || !profile.camera(role).enabled) && !ProfileChannels.of(profile,new com.kooo.evcam.AppConfig(c).getPreviewSourceMode()).previewKeys.contains(key))continue;
   String assigned=ProfileSizes.cameraIdFor(c,role);
   CameraUiState state=manager==null?CameraUiState.STARTING:manager.sourceUiState(key);
   if(source==null && assigned==null)state=CameraUiState.UNASSIGNED;
   out.append("\n\n").append(CameraNames.ofSlot(c,key));
   out.append("\n").append(c.getString(R.string.camera_saved_assignment,assigned==null?c.getString(R.string.camera_state_unassigned):assigned));
   out.append("\n").append(c.getString(state.text));
   if(source!=null)out.append(" · ID ").append(source.getCameraId());
   boolean planned=summary!=null&&summary.planned.contains(key), live=summary!=null&&summary.liveKeys.contains(key);
   out.append("\n").append(c.getString(live?R.string.camera_source_recording:planned?R.string.camera_source_pending:R.string.camera_source_not_recording));
   StreamSpec spec=RecordSpecs.forRole(c,role);
   out.append("\n").append(c.getString(R.string.camera_saved_quality,String.valueOf(spec.resolution),String.valueOf(spec.fps),String.valueOf(spec.bitrate)));
  }
  return out.toString();
 }
}
