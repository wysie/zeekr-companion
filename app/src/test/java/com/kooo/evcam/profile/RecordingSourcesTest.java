package com.kooo.evcam.profile;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class RecordingSourcesTest {
 private Profile profile() { Profile p=ProfileMigration.migrate(new ProfileMigration.Snapshot());RecordingSources.ensure(p);return p; }
 @Test public void newSourcesStayOffAndRoundTripWithoutChangingExistingChoices() {
  Profile p=profile();p.camera(CameraProfile.ROLE_CABIN_1).enabled=true;
  RecordingSources.ensure(p);assertEquals(6,p.cameras.size());
  for(String role:RecordingSources.EXTRA_ROLES)assertFalse(p.camera(role).enabled);
  Profile copy=Profile.fromMap(p.toMap());assertTrue(copy.camera(CameraProfile.ROLE_CABIN_1).enabled);
  assertEquals(6,copy.cameras.size()); assertFalse(copy.camera(CameraProfile.ROLE_ROAD_FRONT).lanes.get(0).mirrored);
 }
 @Test public void everyCombinationPreservesTheSelectedRecordingSources() {
  Set<String> identities=new HashSet<>();
  for(int mask=0;mask<64;mask++) {
   Profile p=profile();int i=0;Set<String> expected=new LinkedHashSet<>();
   for(String role:RecordingSources.ALL_ROLES) {p.camera(role).enabled=(mask&(1<<i++))!=0;if(p.camera(role).enabled)expected.add(RecordingSources.key(role));}
   ProfileChannels channels=ProfileChannels.of(p);assertEquals(expected,channels.keys);assertEquals(expected,channels.previewKeys);
   assertEquals(Integer.bitCount(mask),channels.count);assertTrue(identities.add(channels.key()));
  }
 }
 @Test public void roadPreviewDoesNotEnableRoadRecordingOrHideTheSurroundRecorder() {
  Profile p=profile();ProfileChannels c=ProfileChannels.of(p,"roads");
  assertEquals(new LinkedHashSet<>(Arrays.asList("roadfront","roadrear")),c.previewKeys);
  assertTrue(c.keys.contains("front"));assertEquals(3,c.count);assertFalse(p.camera(CameraProfile.ROLE_ROAD_FRONT).enabled);
 }
 @Test public void sideAndMirrorConsumersRetainSurroundWithoutEnablingItsRecorderOrPreview() {
  Profile p=profile();p.camera("composite").enabled=false;
  p.camera("roadfront").enabled=true;p.camera("roadrear").enabled=true;
  ProfileChannels c=ProfileChannels.of(p,"roads",true);
  assertTrue(c.keys.contains("front"));assertFalse(c.previewKeys.contains("front"));assertFalse(p.camera("composite").enabled);
 }
 @Test public void exteriorDefaultsRequireTheExplicitVerified009Mapping() {
  assertEquals("0",RecordingSources.defaultId(CameraProfile.ROLE_ROAD_FRONT,true));
  assertEquals("1",RecordingSources.defaultId(CameraProfile.ROLE_ROAD_REAR,true));
  assertNull(RecordingSources.defaultId(CameraProfile.ROLE_ROAD_FRONT,false));assertNull(RecordingSources.defaultId(CameraProfile.ROLE_MANUAL,true));
  assertNull(RecordingSources.role(null));
 }
}
