package com.duid.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * DUID 服务端配置属性，绑定 application.yml 中 duid.* 节点。
 * <p>
 * 详细配置说明见 docs/CONFIGURATION.md 及 application.yml 内注释。
 * </p>
 *
 * @author gclcode@126.com
 */
@ConfigurationProperties(prefix = "duid")
public class DuidProperties {

    /** 时间戳起点（ISO-8601） */
    private String epoch = "2024-01-01T00:00:00Z";
    private IdLayout idLayout = new IdLayout();
    private Redis redis = new Redis();
    private Worker worker = new Worker();
    private Watermark watermark = new Watermark();
    private Warmup warmup = new Warmup();

    public String epoch() {
        return epoch;
    }

    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

    public IdLayout idLayout() {
        return idLayout;
    }

    public void setIdLayout(IdLayout idLayout) {
        this.idLayout = idLayout;
    }

    public Redis redis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public Worker worker() {
        return worker;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public Watermark watermark() {
        return watermark;
    }

    public void setWatermark(Watermark watermark) {
        this.watermark = watermark;
    }

    public Warmup warmup() {
        return warmup;
    }

    public void setWarmup(Warmup warmup) {
        this.warmup = warmup;
    }

    /**
     * ID 位布局配置，默认方案4：41+6+13+3。
     *
     * @author gclcode@126.com
     */
    public static class IdLayout {
        private int timestampBits = 41;
        private int workerBits = 6;
        private int sequenceBits = 13;
        private int rollbackBits = 3;

        public int timestampBits() {
            return timestampBits;
        }

        public void setTimestampBits(int timestampBits) {
            this.timestampBits = timestampBits;
        }

        public int workerBits() {
            return workerBits;
        }

        public void setWorkerBits(int workerBits) {
            this.workerBits = workerBits;
        }

        public int sequenceBits() {
            return sequenceBits;
        }

        public void setSequenceBits(int sequenceBits) {
            this.sequenceBits = sequenceBits;
        }

        public int rollbackBits() {
            return rollbackBits;
        }

        public void setRollbackBits(int rollbackBits) {
            this.rollbackBits = rollbackBits;
        }
    }

    /**
     * Redis 连接配置，支持 single / cluster 模式。
     *
     * @author gclcode@126.com
     */
    public static class Redis {
        private String mode = "single";
        private String address = "redis://127.0.0.1:6379";
        private String password;
        private int database = 0;
        private List<String> nodeAddresses = List.of();

        public String mode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String address() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String password() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int database() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public List<String> nodeAddresses() {
            return nodeAddresses;
        }

        public void setNodeAddresses(List<String> nodeAddresses) {
            this.nodeAddresses = nodeAddresses;
        }
    }

    /**
     * Worker 租约配置：TTL 与心跳间隔。
     *
     * @author gclcode@126.com
     */
    public static class Worker {
        /** 租约过期时间（秒），实例宕机后槽位自动释放 */
        private int leaseTtlSeconds = 30;
        /** 心跳续约间隔（秒） */
        private int heartbeatIntervalSeconds = 10;

        public int leaseTtlSeconds() {
            return leaseTtlSeconds;
        }

        public void setLeaseTtlSeconds(int leaseTtlSeconds) {
            this.leaseTtlSeconds = leaseTtlSeconds;
        }

        public int heartbeatIntervalSeconds() {
            return heartbeatIntervalSeconds;
        }

        public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }
    }

    /**
     * Redis 水位刷新配置。
     *
     * @author gclcode@126.com
     */
    public static class Watermark {
        /** 水位写入 Redis 的间隔（毫秒） */
        private long flushIntervalMs = 1000L;

        public long flushIntervalMs() {
            return flushIntervalMs;
        }

        public void setFlushIntervalMs(long flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
        }
    }

    /**
     * 启动预热配置：消除首次请求的 JIT / HTTP 冷启动延迟。
     *
     * @author gclcode@126.com
     */
    public static class Warmup {
        /** 是否在启动完成后执行预热 */
        private boolean enabled = true;
        /** 内存发号器预热次数（触发 JIT 编译） */
        private int generatorIterations = 64;
        /** HTTP 自调用预热次数（预热 Tomcat + Spring MVC） */
        private int httpIterations = 8;

        public boolean enabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int generatorIterations() {
            return generatorIterations;
        }

        public void setGeneratorIterations(int generatorIterations) {
            this.generatorIterations = generatorIterations;
        }

        public int httpIterations() {
            return httpIterations;
        }

        public void setHttpIterations(int httpIterations) {
            this.httpIterations = httpIterations;
        }
    }
}
