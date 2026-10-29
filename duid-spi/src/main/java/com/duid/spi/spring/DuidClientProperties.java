package com.duid.spi.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DUID 客户端配置，绑定 application.yml 中 duid.client.* 节点。
 *
 * @author gclcode@126.com
 */
@ConfigurationProperties(prefix = "duid.client")
public class DuidClientProperties {

    /** DUID 服务端地址，如 http://duid-server:8080 */
    private String serverUrl = "http://localhost:8080";
    private int connectTimeoutMs = 200;
    private int readTimeoutMs = 200;
    /** long 型 ID 本地预取队列容量 */
    private int prefetch = 64;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getPrefetch() {
        return prefetch;
    }

    public void setPrefetch(int prefetch) {
        this.prefetch = prefetch;
    }
}
