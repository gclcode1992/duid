package com.duid.server.config;

import com.duid.server.service.IdGeneratorService;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 启动预热：在对外服务前完成 Redis 连接、JIT 编译、HTTP 链路预热，
 * 避免首次 {@code GET /long-id} 出现 100ms+ 冷启动延迟。
 *
 * @author gclcode@126.com
 */
@Component
public class DuidWarmupRunner implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DuidWarmupRunner.class);

    private final IdGeneratorService idGeneratorService;
    private final RedissonClient redisson;
    private final DuidProperties properties;
    private final ServerProperties serverProperties;

    public DuidWarmupRunner(IdGeneratorService idGeneratorService,
                            RedissonClient redisson,
                            DuidProperties properties,
                            ServerProperties serverProperties) {
        this.idGeneratorService = idGeneratorService;
        this.redisson = redisson;
        this.properties = properties;
        this.serverProperties = serverProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        DuidProperties.Warmup warmup = properties.warmup();
        if (!warmup.enabled()) {
            log.info("DUID warmup disabled");
            return;
        }

        long start = System.nanoTime();
        warmupRedis();
        warmupGenerator(warmup.generatorIterations());
        warmupHttp(warmup.httpIterations());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("DUID warmup completed in {} ms (generator={}, http={})",
                elapsedMs, warmup.generatorIterations(), warmup.httpIterations());
    }

    /** 提前建立 Redis 连接池 */
    private void warmupRedis() {
        redisson.getKeys().count();
    }

    /** 预热发号器热路径，触发 JIT 编译 */
    private void warmupGenerator(int iterations) {
        if (!idGeneratorService.isReady() || iterations <= 0) {
            return;
        }
        for (int i = 0; i < iterations; i++) {
            idGeneratorService.nextId();
        }
    }

    /** 自调用 HTTP 接口，预热 Tomcat + Spring MVC + 序列化链路 */
    private void warmupHttp(int iterations) {
        if (iterations <= 0) {
            return;
        }
        int port = serverProperties.getPort() != null ? serverProperties.getPort() : 8080;
        URI uri = URI.create("http://127.0.0.1:" + port + "/long-id");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(2))
                .build();

        for (int i = 0; i < iterations; i++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.warn("Warmup HTTP /long-id returned {}", response.statusCode());
                    return;
                }
            } catch (Exception e) {
                log.warn("Warmup HTTP /long-id failed: {}", e.getMessage());
                return;
            }
        }
    }
}
