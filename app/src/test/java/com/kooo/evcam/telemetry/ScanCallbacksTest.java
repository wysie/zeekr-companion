package com.kooo.evcam.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** {@link ScanCallbacks}：普查要听得见「定制功能」的值回调 —— 有的号只从那里给值。 */
public class ScanCallbacksTest {

    @Test public void notificationRetainsFunctionIdWithoutInventingWarningStateOrSide() {
        ScanCallbacks.Event e = ScanCallbacks.snapshot("onFunctionChanged", new Object[]{0x28070200});
        assertEquals("N:", e.prefix);
        assertEquals(0x28070200, e.id);
        assertEquals(0, e.zone);
        assertEquals("changed", e.value);
    }

    @Test public void bytesRetainZoneAndUnsignedPayloadAsAnImmutableSnapshot() {
        byte[] bytes = {0, 1, (byte) 128, (byte) 255};
        ScanCallbacks.Event e = ScanCallbacks.snapshot("onBytesFunctionValueChanged",
                new Object[]{0x28070200, 4, bytes});
        java.util.Arrays.fill(bytes, (byte) 0);
        assertEquals("B:", e.prefix);
        assertEquals(0x28070200, e.id);
        assertEquals(4, e.zone);
        assertEquals("bytes(len=4,hex=000180ff)", e.value);
    }

    @Test public void byteCaptureIsBoundedAndTruncationIsExplicit() {
        byte[] bytes = new byte[4096];
        java.util.Arrays.fill(bytes, (byte) 255);
        ScanCallbacks.Event e = ScanCallbacks.snapshot("onBytesFunctionValueChanged", new Object[]{1, 0, bytes});
        String expected = "bytes(len=4096,hex=" + new String(new char[128]).replace('\0', 'f') + ",truncated)";
        assertEquals(expected, e.value);
        assertEquals("bytes(len=0,hex=)", ScanCallbacks.snapshot("onBytesFunctionValueChanged",
                new Object[]{1, 0, new byte[0]}).value);
    }

    @Test public void numericAndCustomizeEventsKeepTheirExistingPrefixesAndValues() {
        java.util.concurrent.atomic.AtomicInteger value = new java.util.concurrent.atomic.AtomicInteger(7);
        ScanCallbacks.Event plain = ScanCallbacks.snapshot("onFunctionValueChanged", new Object[]{3, 1, value});
        value.set(8);
        assertEquals("", plain.prefix);
        assertEquals("7", plain.value);
        assertEquals(1, plain.zone);
        ScanCallbacks.Event custom = ScanCallbacks.snapshot("onCustomizeFunctionValueChanged", new Object[]{3, 0, 0.5f});
        assertEquals("C:", custom.prefix);
        assertEquals("0.5", custom.value);
    }

    @Test public void malformedOrUnrelatedCallbacksAreNotDecodedAsWarningEvidence() {
        assertNull(ScanCallbacks.snapshot("onFunctionChanged", null));
        assertNull(ScanCallbacks.snapshot("onFunctionChanged", new Object[]{1L}));
        assertNull(ScanCallbacks.snapshot("onFunctionChanged", new Object[]{1, 2}));
        assertNull(ScanCallbacks.snapshot("onBytesFunctionValueChanged", new Object[]{1, 0, null}));
        assertNull(ScanCallbacks.snapshot("onBytesFunctionValueChanged", new Object[]{1, "left", new byte[1]}));
        assertNull(ScanCallbacks.snapshot("onSupportedFunctionStatusChanged", new Object[]{1, 0, "supported"}));
        assertNull(ScanCallbacks.snapshot("toString", null));
    }

    @Test
    public void valueCallbacksAreIdZoneValue() {
        assertTrue(ScanCallbacks.isValue(new Object[]{0x21020300, 0, 97}));
        assertTrue("浮点值", ScanCallbacks.isValue(new Object[]{0x21020300, 0x80000000, 0.5f}));
        assertFalse(ScanCallbacks.isValue(null));
        assertFalse("两个参数的是传感器", ScanCallbacks.isValue(new Object[]{0x00200200, 1}));
        assertFalse("值不是数", ScanCallbacks.isValue(new Object[]{1, 2, "x"}));
        assertFalse("号不是整数", ScanCallbacks.isValue(new Object[]{1L, 2, 3}));
    }

    /** 2.11.3 / 2.11.4 的普查丢掉的就是这一路。 */
    @Test
    public void theCustomizeValueCallbackIsHeard() {
        assertTrue(ScanCallbacks.isCustomizeValue("onCustomizeFunctionValueChanged", new Object[]{0x21020300, 0, 97.0f}));
    }

    @Test
    public void otherMethodsAreNotCustomizeValues() {
        assertFalse(ScanCallbacks.isCustomizeValue("onFunctionValueChanged", new Object[]{1, 0, 1}));
        assertFalse("形状不对的定制回调不当值记", ScanCallbacks.isCustomizeValue("onCustomizeFunctionValueChanged", new Object[]{1}));
        assertFalse(ScanCallbacks.isCustomizeValue("onCustomizeFunctionValueChanged", null));
    }

    @Test
    public void objectMethodsAreNotCarCallbacks() {
        assertTrue(ScanCallbacks.isObjectMethod("toString"));
        assertTrue(ScanCallbacks.isObjectMethod("hashCode"));
        assertTrue(ScanCallbacks.isObjectMethod("equals"));
        assertFalse(ScanCallbacks.isObjectMethod("onFunctionChanged"));
    }

    @Test
    public void shapeNamesTheMethodAndItsArgumentTypes() {
        assertEquals("onFunctionChanged(Integer)", ScanCallbacks.shape("onFunctionChanged", new Object[]{5}));
        assertEquals("onSupportedFunctionValueChanged(Integer,int[])",
                ScanCallbacks.shape("onSupportedFunctionValueChanged", new Object[]{5, new int[0]}));
        assertEquals("ping()", ScanCallbacks.shape("ping", null));
        assertEquals("x(null)", ScanCallbacks.shape("x", new Object[]{null}));
    }
}
