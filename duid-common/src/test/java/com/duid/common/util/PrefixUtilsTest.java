package com.duid.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link PrefixUtils} 单元测试。
 *
 * @author gclcode@126.com
 */
class PrefixUtilsTest {

    @Test
    void normalizeNullAndEmpty() {
        assertEquals("", PrefixUtils.normalize(null));
        assertEquals("", PrefixUtils.normalize(""));
    }

    @Test
    void withPrefix() {
        assertEquals("ORD123", PrefixUtils.withPrefix("ORD", "123"));
        assertEquals("123", PrefixUtils.withPrefix(null, "123"));
        assertEquals("123", PrefixUtils.withPrefix("", "123"));
    }
}
