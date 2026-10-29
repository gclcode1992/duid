package com.duid.spi.spring;

import com.duid.spi.DuidClient;
import com.duid.spi.DuidClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * DUID 客户端 Spring Boot 自动配置。
 *
 * @author gclcode@126.com
 */
@AutoConfiguration
@ConditionalOnClass(DuidClient.class)
@EnableConfigurationProperties(DuidClientProperties.class)
@ConditionalOnProperty(prefix = "duid.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DuidClientAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public DuidClient duidClient(DuidClientProperties properties) {
        return DuidClients.builder()
                .serverUrl(properties.getServerUrl())
                .connectTimeoutMs(properties.getConnectTimeoutMs())
                .readTimeoutMs(properties.getReadTimeoutMs())
                .prefetch(properties.getPrefetch())
                .build();
    }
}
