package com.duid.common.util;

/**
 * ID 前缀处理工具。
 *
 * @author gclcode@126.com
 */
public final class PrefixUtils {

    private PrefixUtils() {}

    /**
     * null 或空字符串统一转为空串，作为无前缀处理。
     */
    public static String normalize(String prefix) {
        return prefix == null ? "" : prefix;
    }

    /**
     * 将前缀拼接在 ID 前面。
     */
    public static String withPrefix(String prefix, String id) {
        return normalize(prefix) + id;
    }
}
