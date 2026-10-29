package com.duid.common.encoding;

/**
 * Base36 大写编码：字符集为 0-9、A-Z（仅数字与大写字母）。
 *
 * @author gclcode@126.com
 */
public final class Base36Upper {

    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private Base36Upper() {}

    /**
     * 将非负 long 编码为仅含数字与大写字母的 Base36 字符串。
     */
    public static String encode(long value) {
        if (value == 0) {
            return "0";
        }
        if (value < 0) {
            throw new IllegalArgumentException("Base36Upper encoding requires non-negative value");
        }
        char[] buffer = new char[13];
        int index = buffer.length;
        long current = value;
        while (current > 0) {
            buffer[--index] = ALPHABET[(int) (current % 36)];
            current /= 36;
        }
        return new String(buffer, index, buffer.length - index);
    }
}
