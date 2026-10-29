package com.duid.server.service;

import com.duid.common.generator.SnowflakeIdGenerator;
import com.duid.common.layout.BitLayoutProperties;
import com.duid.server.worker.WorkerLeaseService;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

/**
 * ID 发号服务。
 * <p>
 * 负责初始化发号器、定时续约 worker 租约、异步刷新 Redis 发号水位。
 * 热路径 {@link #nextId()} 纯内存操作，不访问 Redis。
 * </p>
 *
 * @author gclcode@126.com
 */
@Service
public class IdGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(IdGeneratorService.class);
    /** Redis 水位 Key 前缀：duid:clock:{workerId} */
    private static final String CLOCK_KEY_PREFIX = "duid:clock:";

    private final WorkerLeaseService workerLeaseService;
    private final RedissonClient redisson;
    private final BitLayoutProperties layout;
    private final long epochMillis;

    private volatile SnowflakeIdGenerator generator;

    public IdGeneratorService(WorkerLeaseService workerLeaseService,
                              RedissonClient redisson,
                              BitLayoutProperties layout,
                              long epochMillis) {
        this.workerLeaseService = workerLeaseService;
        this.redisson = redisson;
        this.layout = layout;
        this.epochMillis = epochMillis;
    }

    /** 启动时抢 worker 槽位、创建发号器、恢复 Redis 水位 */
    @PostConstruct
    public void init() {
        workerLeaseService.acquireOrThrow();
        int workerId = workerLeaseService.workerId();
        generator = new SnowflakeIdGenerator(layout, epochMillis, workerId);
        restoreWatermark(workerId);
        log.info("IdGenerator initialized for workerId={}", workerId);
    }

    /**
     * 生成下一个全局唯一 ID。
     *
     * @throws IllegalStateException worker 租约无效时
     */
    public long nextId() {
        if (!workerLeaseService.isReady()) {
            throw new IllegalStateException("Worker lease is not valid");
        }
        return generator.nextId();
    }

    /** 服务是否就绪（已持有 worker 且发号器已初始化） */
    public boolean isReady() {
        return workerLeaseService.isReady() && generator != null;
    }

    /** 定时续约 worker 租约，防止槽位被其他实例抢占 */
    @Scheduled(fixedDelay = 10_000)
    public void heartbeat() {
        workerLeaseService.renewLease();
    }

    /** 定时将发号水位写入 Redis，进程重启后可恢复状态 */
    @Scheduled(fixedDelay = 1_000)
    public void flushWatermark() {
        if (generator == null || !workerLeaseService.isReady()) {
            return;
        }
        SnowflakeIdGenerator.GeneratorState state = generator.snapshotState();
        int workerId = workerLeaseService.workerId();
        RBucket<String> bucket = redisson.getBucket(CLOCK_KEY_PREFIX + workerId);
        bucket.set(state.serialize(), Duration.ofDays(7));
    }

    /** 从 Redis 恢复上次发号状态，防止重启后重复发号 */
    private void restoreWatermark(int workerId) {
        RBucket<String> bucket = redisson.getBucket(CLOCK_KEY_PREFIX + workerId);
        String value = bucket.get();
        if (value != null) {
            SnowflakeIdGenerator.GeneratorState state =
                    SnowflakeIdGenerator.GeneratorState.deserialize(value);
            generator.restoreState(state.lastTimestamp(), state.rollbackGen(), state.sequence());
            log.info("Restored watermark for workerId={}: {}", workerId, value);
        }
    }
}
