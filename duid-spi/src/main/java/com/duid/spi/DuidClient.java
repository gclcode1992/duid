package com.duid.spi;

/**
 * 分布式唯一 ID 客户端 SPI 接口。
 * <p>
 * 业务系统引入 duid-spi Jar，配置服务端地址后即可使用。
 * 所有 ID 形态均通过 HTTP 从服务端获取。
 * </p>
 *
 * @author gclcode@126.com
 */
public interface DuidClient extends AutoCloseable {

    /**
     * 获取下一个 ID（64-bit 正整数），对应 {@code GET /long-id}。
     */
    long nextId();

    /**
     * 获取带前缀的 long 型 ID 字符串，对应 {@code GET /long-id?prefix=}。
     *
     * @param prefix 前缀，null 或空字符串视为无前缀
     */
    String nextLongIdWithPrefix(String prefix);

    /**
     * 获取 Base62 压缩 ID（0-9、A-Z、a-z），对应 {@code GET /base62-id}。
     */
    String nextBase62Id();

    /**
     * 获取带前缀的 Base62 压缩 ID，对应 {@code GET /base62-id?prefix=}。
     *
     * @param prefix 前缀，null 或空字符串视为无前缀
     */
    String nextBase62IdWithPrefix(String prefix);

    /**
     * 获取 Base36 大写压缩 ID（仅 0-9、A-Z），对应 {@code GET /base36-upper-id}。
     */
    String nextBase36UpperId();

    /**
     * 获取带前缀的 Base36 大写压缩 ID，对应 {@code GET /base36-upper-id?prefix=}。
     *
     * @param prefix 前缀，null 或空字符串视为无前缀
     */
    String nextBase36UpperIdWithPrefix(String prefix);

    @Override
    void close();
}
