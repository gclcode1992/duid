package com.duid.common.encoding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * {@link Base62} 单元测试。
 *
 * @author gclcode@126.com
 */
class Base62Test {

    @Test
    void encodeZero() {
        assertEquals("0", Base62.encode(0));
    }

    @Test
    void encodeLength() {
        String encoded = Base62.encode(349876602913246589L);
        assertNotEquals("", encoded);
        assertEquals(true, encoded.length() <= 11);
    }
}
