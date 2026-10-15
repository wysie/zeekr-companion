package com.kooo.evcam.camera;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class BirdEyeChannelTest {
    @Test public void rejectionYields360BeforeHudAndIndicatorAndNeverSelectsRecordingOrMirror() {
        java.util.Set<ChannelRules.Out> outputs = new java.util.LinkedHashSet<>();
        ChannelRules.Out record = new ChannelRules.Out(ChannelRules.Kind.RECORD, "record");
        ChannelRules.Out mirror = new ChannelRules.Out(ChannelRules.Kind.MIRROR, "mirror");
        ChannelRules.Out side = new ChannelRules.Out(ChannelRules.Kind.SIDE_POPUP, "side");
        ChannelRules.Out hud = new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD, "hud");
        ChannelRules.Out bird = new ChannelRules.Out(ChannelRules.Kind.BIRD_EYE, "360");
        outputs.addAll(Arrays.asList(record, mirror, side, hud, bird));
        assertEquals(bird, ChannelRules.optionalPreviewToDrop(outputs));
        outputs.remove(bird);
        assertEquals(hud, ChannelRules.optionalPreviewToDrop(outputs));
        outputs.remove(hud);
        assertEquals(side, ChannelRules.optionalPreviewToDrop(outputs));
        outputs.remove(side);
        assertNull(ChannelRules.optionalPreviewToDrop(outputs));
    }
    @Test public void popupClaimsOnlySurroundAndDoesNotKeepCamerasOnInTheDark() {
        ChannelRules.Lane surround = new ChannelRules.Lane(CameraSlots.KEY_SURROUND, "2");
        ChannelRules.Lane cabin = new ChannelRules.Lane(CameraSlots.KEY_CABIN_FRONT, "0");
        ChannelRules.World world = new ChannelRules.World(Arrays.asList(surround, cabin));
        world.screenOn = true;
        world.holders.add(CameraNeeds.Holder.BIRD_EYE);
        ChannelRules.Out popup = new ChannelRules.Out(ChannelRules.Kind.BIRD_EYE, "360");
        surround.declared.add(popup); cabin.declared.add(popup);
        assertTrue(ChannelRules.wanted(world, surround));
        assertFalse(ChannelRules.wanted(world, cabin));
        assertTrue(ChannelRules.wantedOutputs(world, surround).contains(popup));
        assertFalse(ChannelRules.wantedOutputs(world, cabin).contains(popup));
        world.screenOn = false;
        assertFalse(ChannelRules.wanted(world, surround));
        assertFalse(ChannelRules.wantedOutputs(world, surround).contains(popup));
    }
    @Test public void closing360DoesNotWithdrawMirrorIndicatorOrRecording() {
        ChannelRules.Lane surround = new ChannelRules.Lane(CameraSlots.KEY_SURROUND, "2");
        ChannelRules.World world = new ChannelRules.World(Arrays.asList(surround));
        world.screenOn = true;
        world.recordPlan.add(CameraSlots.KEY_SURROUND);
        world.holders.addAll(Arrays.asList(CameraNeeds.Holder.BIRD_EYE, CameraNeeds.Holder.MIRROR,
                CameraNeeds.Holder.SIDE_POPUP, CameraNeeds.Holder.RECORDING));
        ChannelRules.Out popup = new ChannelRules.Out(ChannelRules.Kind.BIRD_EYE, "360");
        ChannelRules.Out mirror = new ChannelRules.Out(ChannelRules.Kind.MIRROR, "mirror");
        ChannelRules.Out side = new ChannelRules.Out(ChannelRules.Kind.SIDE_POPUP, "indicator");
        ChannelRules.Out record = new ChannelRules.Out(ChannelRules.Kind.RECORD, "record");
        surround.declared.addAll(Arrays.asList(popup, mirror, side, record));
        assertTrue(ChannelRules.wantedOutputs(world, surround).containsAll(Arrays.asList(popup, mirror, side, record)));
        world.holders.remove(CameraNeeds.Holder.BIRD_EYE); surround.declared.remove(popup);
        assertTrue(ChannelRules.wantedOutputs(world, surround).containsAll(Arrays.asList(mirror, side, record)));
        assertFalse(ChannelRules.wantedOutputs(world, surround).contains(popup));
    }
}
