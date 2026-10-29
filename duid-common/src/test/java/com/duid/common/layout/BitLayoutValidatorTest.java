package com.duid.common.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link BitLayoutValidator} 单元测试。
 *
 * @author gclcode@126.com
 */
class BitLayoutValidatorTest {

    @Test
    void defaultLayoutPasses() {
        assertDoesNotThrow(() -> BitLayoutValidator.validate(BitLayoutProperties.defaults()));
        assertEquals(64, BitLayoutProperties.defaults().totalBits());
    }

    @Test
    void exceeds64Fails() {
        BitLayoutProperties layout = new BitLayoutProperties(41, 10, 12, 3);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> BitLayoutValidator.validate(layout));
        assertEquals(true, ex.getMessage().contains("exactly 64 bits"));
        assertEquals(true, ex.getMessage().contains("67"));
    }

    @Test
    void below64Fails() {
        BitLayoutProperties layout = new BitLayoutProperties(40, 6, 13, 3);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> BitLayoutValidator.validate(layout));
        assertEquals(true, ex.getMessage().contains("exactly 64 bits"));
        assertEquals(true, ex.getMessage().contains("63"));
    }

    @Test
    void scheme1Passes() {
        BitLayoutProperties layout = new BitLayoutProperties(41, 8, 12, 2);
        assertDoesNotThrow(() -> BitLayoutValidator.validate(layout));
    }

    @Test
    void composeAndParseRoundTrip() {
        BitLayoutProperties layout = BitLayoutProperties.defaults();
        long id = layout.compose(83412000123L, 5L, 1007L, 1L);
        BitLayoutProperties.ParsedId parsed = layout.parse(id);
        assertEquals(83412000123L, parsed.timestamp());
        assertEquals(5L, parsed.workerId());
        assertEquals(1007L, parsed.sequence());
        assertEquals(1L, parsed.rollbackGen());
    }
}
