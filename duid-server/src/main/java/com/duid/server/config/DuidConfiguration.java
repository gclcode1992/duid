package com.duid.server.config;

import com.duid.common.layout.BitLayoutProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * DUID 核心 Bean 配置：位布局、epoch 等。
 *
 * @author gclcode@126.com
 */
@Configuration
@EnableConfigurationProperties(DuidProperties.class)
public class DuidConfiguration {

    @Bean
    BitLayoutProperties bitLayoutProperties(DuidProperties properties) {
        DuidProperties.IdLayout layout = properties.idLayout();
        return new BitLayoutProperties(
                layout.timestampBits(),
                layout.workerBits(),
                layout.sequenceBits(),
                layout.rollbackBits());
    }

    @Bean
    long epochMillis(DuidProperties properties) {
        return Instant.parse(properties.epoch()).toEpochMilli();
    }
}
