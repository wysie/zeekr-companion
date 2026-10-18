package com.kooo.evcam.camera;
import org.junit.Test;import static org.junit.Assert.*;
public class CameraUiStateTest {
 @Test public void knownStatesAreReportedWithoutInventingAFactoryOwner() {
  for(ChannelRules.Condition condition:ChannelRules.Condition.values()) {
   assertEquals(CameraUiState.STARTING,CameraUiState.of(false,false,condition,false,false,false));
   assertEquals(CameraUiState.NOT_ACTIVE,CameraUiState.of(true,false,condition,false,false,false));
   assertEquals(CameraUiState.PAUSED,CameraUiState.of(true,true,condition,true,true,true));
  }
  assertEquals(CameraUiState.IN_USE,CameraUiState.of(true,true,ChannelRules.Condition.TAKEN,false,true,true));
  assertEquals(CameraUiState.STOPPED,CameraUiState.of(true,true,ChannelRules.Condition.GAVE_UP,false,true,false));
  assertEquals(CameraUiState.RECOVERING,CameraUiState.of(true,true,ChannelRules.Condition.RESCUING,false,true,false));
 }
 @Test public void producerAndPreviewAreNotConflated() {
  assertEquals(CameraUiState.STREAMING,CameraUiState.of(true,true,ChannelRules.Condition.STREAMING,false,true,true));
  assertEquals(CameraUiState.WAITING,CameraUiState.of(true,true,ChannelRules.Condition.STREAMING,false,true,false));
  assertEquals(CameraUiState.STARTING,CameraUiState.of(true,true,ChannelRules.Condition.BRINGING_UP,false,false,false));
 }
}
