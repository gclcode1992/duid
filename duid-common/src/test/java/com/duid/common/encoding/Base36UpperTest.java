package com.duid.common.encoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Base36Upper} 单元测试。
 *
 * @author gclcode@126.com
 */
class Base36UpperTest {

    @Test
    void encodeZero() {
        assertEquals("0", Base36Upper.encode(0));
    }

    @Test
    void encodeOnlyUpperCaseAndDigits() {
        String encoded = Base36Upper.encode(349876602913246589L);
        assertTrue(encoded.matches("[0-9A-Z]+"));
    }
}
