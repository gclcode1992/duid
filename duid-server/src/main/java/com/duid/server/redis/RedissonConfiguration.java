package com.duid.server.redis;

import com.duid.server.config.DuidProperties;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置，支持 Redis 单机与集群模式。
 *
 * @author gclcode@126.com
 */
@Configuration
public class RedissonConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfiguration.class);

    @Bean(destroyMethod = "shutdown")
    RedissonClient redissonClient(DuidProperties properties) {
        DuidProperties.Redis redis = properties.redis();
        log.info("Connecting Redis: mode={}, address={}", redis.mode(), redis.address());
        Config config = new Config();
        if ("cluster".equalsIgnoreCase(redis.mode())) {
            // 集群模式
            String[] nodes = redis.nodeAddresses().isEmpty()
                    ? new String[]{redis.address()}
                    : redis.nodeAddresses().toArray(String[]::new);
            config.useClusterServers()
                    .addNodeAddress(nodes)
                    .setPassword(emptyToNull(redis.password()));
        } else {
            // 单机模式
            config.useSingleServer()
                    .setAddress(redis.address())
                    .setDatabase(redis.database())
                    .setPassword(emptyToNull(redis.password()));
        }
        RedissonClient client = Redisson.create(config);
        // 启动时立即建立连接，避免首次 Redis 操作额外延迟
        client.getKeys().count();
        return client;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
