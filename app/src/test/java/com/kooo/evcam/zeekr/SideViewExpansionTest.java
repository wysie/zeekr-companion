package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class SideViewExpansionTest {
    @Test public void blindSideExpansionIgnoresSavedAspectAndUsesNearestHalf() {
        for (int[] size : new int[][]{{400, 400}, {600, 450}, {880, 400}, {300, 400}}) {
            for (boolean right : new boolean[]{false, true}) {
                SideViewExpansion expansion = new SideViewExpansion();
                SideViewExpansion.Geometry normal = new SideViewExpansion.Geometry(right ? 1500 : 10, 200, size[0], size[1]);
                SideViewExpansion.Geometry large = expansion.toggleHalfScreen(normal, 2560, 1500, 16);
                assertEquals(1264, large.width); assertEquals(1468, large.height);
                assertTrue(large.height > large.width);
                assertEquals(right ? 1280 : 16, large.x); assertEquals(16, large.y);
                assertSame(normal, expansion.toggleHalfScreen(large, 2560, 1500, 16));
                assertFalse(expansion.expanded());
            }
        }
    }

    @Test public void halfScreenExpansionRefitsInsetsAndRestoresItsOriginalUltraWideGeometry() {
        SideViewExpansion expansion = new SideViewExpansion();
        SideViewExpansion.Geometry normal = new SideViewExpansion.Geometry(1500, 300, 880, 400);
        expansion.toggleHalfScreen(normal, 2560, 1400, 16);
        SideViewExpansion.Geometry fullscreen = expansion.refit(2560, 1600, 16);
        assertEquals(1264, fullscreen.width); assertEquals(1568, fullscreen.height);
        assertEquals(1280, fullscreen.x); assertEquals(16, fullscreen.y);
        assertTrue(expansion.expanded()); assertSame(normal, expansion.restore());
        // A later normal mirror/360 expansion still retains its aspect ratio.
        SideViewExpansion.Geometry normalExpansion = expansion.toggle(normal, 2560, 1600, 16);
        assertEquals(2.2, normalExpansion.width / (double) normalExpansion.height, .01);
    }

    @Test public void immersiveBoundsGrowAndShrinkExpansionWithoutChangingRestorePoint() {
        SideViewExpansion expansion = new SideViewExpansion();
        SideViewExpansion.Geometry normal = new SideViewExpansion.Geometry(1200, 150, 600, 900);
        SideViewExpansion.Geometry withBars = expansion.toggle(normal, 2560, 1400, 16);
        assertEquals(1368, withBars.height);
        SideViewExpansion.Geometry fullscreen = expansion.refit(2560, 1600, 16);
        assertEquals(1568, fullscreen.height);
        assertEquals(1584, fullscreen.y + fullscreen.height);
        assertEquals(2f / 3f, fullscreen.width / (float) fullscreen.height, .001f);
        SideViewExpansion.Geometry barsReturned = expansion.refit(2560, 1400, 16);
        assertEquals(withBars.width, barsReturned.width); assertEquals(withBars.height, barsReturned.height);
        assertTrue(expansion.expanded()); assertSame(normal, expansion.restore());
        assertNull(expansion.refit(2560, 1600, 16));
    }

    @Test
    public void expandFitsTheScreenAndRestoresExactGeometry() {
        SideViewExpansion expansion = new SideViewExpansion();
        SideViewExpansion.Geometry normal = new SideViewExpansion.Geometry(1400, 200, 400, 400);
        SideViewExpansion.Geometry large = expansion.toggle(normal, 1920, 1080, 8);
        assertTrue(expansion.expanded());
        assertEquals(1064, large.width);
        assertEquals(1064, large.height);
        assertTrue(large.x >= 8 && large.x + large.width <= 1912);
        assertTrue(large.y >= 8 && large.y + large.height <= 1072);
        assertSame(normal, expansion.toggle(large, 1920, 1080, 8));
        assertFalse(expansion.expanded());
    }

    @Test
    public void closingAnExpandedWideMirrorRestoresItsNormalSizeAndPosition() {
        SideViewExpansion expansion = new SideViewExpansion();
        SideViewExpansion.Geometry normal = new SideViewExpansion.Geometry(10, 20, 600, 200);
        SideViewExpansion.Geometry large = expansion.toggle(normal, 1920, 1080, 8);
        assertEquals(3f, (float) large.width / large.height, 0.01f);
        assertSame(normal, expansion.restore());
        assertFalse(expansion.expanded());
        assertNull(expansion.restore());
    }
}
