package com.duid.common.encoding;

/**
 * Base62 编码：字符集为 0-9、A-Z、a-z（含大小写字母与数字）。
 *
 * @author gclcode@126.com
 */
public final class Base62 {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private Base62() {}

    /**
     * 将非负 long 编码为 Base62 字符串。
     */
    public static String encode(long value) {
        if (value == 0) {
            return "0";
        }
        if (value < 0) {
            throw new IllegalArgumentException("Base62 encoding requires non-negative value");
        }
        char[] buffer = new char[12];
        int index = buffer.length;
        long current = value;
        while (current > 0) {
            buffer[--index] = ALPHABET[(int) (current % 62)];
            current /= 62;
        }
        return new String(buffer, index, buffer.length - index);
    }
}
