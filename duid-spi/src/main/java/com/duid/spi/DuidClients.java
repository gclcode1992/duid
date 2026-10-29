package com.duid.spi;

/**
 * 创建 {@link DuidClient} 的工厂类。
 *
 * @author gclcode@126.com
 */
public final class DuidClients {

    private DuidClients() {}

    public static Builder builder() {
        return new Builder();
    }

    /** 使用默认配置快速创建客户端 */
    public static DuidClient create(String serverUrl) {
        return builder().serverUrl(serverUrl).build();
    }

    /**
     * 客户端构建器。
     *
     * @author gclcode@126.com
     */
    public static final class Builder {
        private String serverUrl = "http://localhost:8080";
        private int connectTimeoutMs = 200;
        private int readTimeoutMs = 200;
        /** 本地预取队列容量，降低 nextId() 延迟 */
        private int prefetch = 64;

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder connectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder readTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder prefetch(int prefetch) {
            this.prefetch = prefetch;
            return this;
        }

        public DuidClient build() {
            return new HttpDuidClient(serverUrl, connectTimeoutMs, readTimeoutMs, prefetch);
        }
    }
}
