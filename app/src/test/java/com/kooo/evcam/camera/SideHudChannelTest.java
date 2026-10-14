package com.kooo.evcam.camera;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class SideHudChannelTest {
    @Test public void hudUsesOnlySurroundAndReleasesOnDarkScreen() {
        ChannelRules.Lane surround = new ChannelRules.Lane(CameraSlots.KEY_SURROUND,"2");
        ChannelRules.Lane cabin = new ChannelRules.Lane(CameraSlots.KEY_CABIN_FRONT,"0");
        ChannelRules.World world = new ChannelRules.World(Arrays.asList(surround,cabin));
        world.screenOn = true;
        world.holders.add(CameraNeeds.Holder.SIDE_HUD);
        ChannelRules.Out hud = new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,"hud");
        surround.declared.add(hud);
        cabin.declared.add(hud);
        assertTrue(ChannelRules.wanted(world,surround));
        assertFalse(ChannelRules.wanted(world,cabin));
        assertTrue(ChannelRules.wantedOutputs(world,surround).contains(hud));
        assertFalse(ChannelRules.wantedOutputs(world,cabin).contains(hud));
        world.screenOn = false;
        assertFalse(ChannelRules.wanted(world,surround));
        assertFalse(ChannelRules.wantedOutputs(world,surround).contains(hud));
    }
    @Test public void withdrawingHudKeepsScreenAndRecordingWanted() {
        ChannelRules.Lane surround = new ChannelRules.Lane(CameraSlots.KEY_SURROUND,"2");
        ChannelRules.World world = new ChannelRules.World(Arrays.asList(surround));
        world.screenOn = true;
        world.recordPlan.add(CameraSlots.KEY_SURROUND);
        world.holders.addAll(Arrays.asList(CameraNeeds.Holder.SIDE_HUD,CameraNeeds.Holder.SIDE_POPUP,CameraNeeds.Holder.RECORDING));
        ChannelRules.Out hud = new ChannelRules.Out(ChannelRules.Kind.SIDE_HUD,"hud");
        ChannelRules.Out screen = new ChannelRules.Out(ChannelRules.Kind.SIDE_POPUP,"screen");
        ChannelRules.Out record = new ChannelRules.Out(ChannelRules.Kind.RECORD,"record");
        surround.declared.addAll(Arrays.asList(hud,screen,record));
        assertTrue(ChannelRules.wantedOutputs(world,surround).containsAll(Arrays.asList(hud,screen,record)));
        world.holders.remove(CameraNeeds.Holder.SIDE_HUD);
        surround.declared.remove(hud);
        assertFalse(ChannelRules.wantedOutputs(world,surround).contains(hud));
        assertTrue(ChannelRules.wantedOutputs(world,surround).containsAll(Arrays.asList(screen,record)));
    }
}
