package com.kooo.evcam.profile;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class ProfileChannelsTest {
    private Profile selected(boolean surround, boolean front, boolean rear) {
        Profile p = ProfileMigration.migrate(new ProfileMigration.Snapshot());
        p.camera(CameraProfile.ROLE_COMPOSITE).enabled = surround;
        p.camera(CameraProfile.ROLE_CABIN_1).enabled = front;
        p.camera(CameraProfile.ROLE_CABIN_2).enabled = rear;
        return p;
    }

    @Test public void cabinAloneNeverOpensTheSurroundChannel() {
        ProfileChannels front = ProfileChannels.of(selected(false, true, false));
        assertFalse(front.surround);
        assertTrue(front.cabinFront);
        assertFalse(front.cabinRear);
        assertEquals(1, front.count);
        assertEquals(3, front.layoutSlots());
        ProfileChannels rear = ProfileChannels.of(selected(false, false, true));
        assertFalse(rear.surround);
        assertFalse(rear.cabinFront);
        assertTrue(rear.cabinRear);
        assertEquals(1, rear.count);
        assertEquals(3, rear.layoutSlots());
    }

    @Test public void surroundAndRearNeedTheThirdTextureSlotEvenWithOnlyTwoChannels() {
        ProfileChannels channels = ProfileChannels.of(selected(true, false, true));
        assertEquals(2, channels.count);
        assertEquals(3, channels.layoutSlots());
        assertTrue(channels.surround);
        assertFalse(channels.cabinFront);
        assertTrue(channels.cabinRear);
    }

    @Test public void allEightSwitchCombinationsHaveDifferentPipelineIdentities() {
        Set<String> identities = new HashSet<>();
        for (int mask = 0; mask < 8; mask++) {
            ProfileChannels channels = ProfileChannels.of(selected((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0));
            assertTrue(identities.add(channels.key()));
            assertEquals(Integer.bitCount(mask), channels.count);
            assertEquals((mask & 6) != 0, channels.cabinLayout());
            int listeners = 0;
            for (String key : new String[]{"front", "back", "left", "right"}) {
                if (channels.needsTexture(key)) listeners++;
            }
            assertEquals("Readiness counts only the selected texture listeners", channels.count, listeners);
            assertEquals((mask & 1) != 0, channels.needsTexture("front"));
            assertEquals((mask & 2) != 0, channels.needsTexture("back"));
            assertEquals((mask & 4) != 0, channels.needsTexture("left"));
        }
        assertEquals(8, identities.size());
    }

    @Test public void ordinarySurroundKeepsTheSingleTextureLayout() {
        ProfileChannels channels = ProfileChannels.of(selected(true, false, false));
        assertEquals(1, channels.count);
        assertEquals(1, channels.layoutSlots());
        assertFalse(channels.cabinLayout());
    }
}
