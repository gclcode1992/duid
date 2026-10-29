package com.duid.server.worker;

import com.duid.common.layout.BitLayoutProperties;
import com.duid.server.config.DuidProperties;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WorkerId 租约服务。
 * <p>
 * 启动时在 Redis 中抢占空闲槽位（SET NX + TTL），运行期间定时续约。
 * 丢租约后停止发号，避免多实例共用同一 workerId 导致 ID 碰撞。
 * </p>
 *
 * @author gclcode@126.com
 */
@Service
public class WorkerLeaseService {

    private static final Logger log = LoggerFactory.getLogger(WorkerLeaseService.class);
    private static final String WORKER_KEY_PREFIX = "duid:worker:";

    private final RedissonClient redisson;
    private final BitLayoutProperties layout;
    private final DuidProperties.Worker workerConfig;
    /** 实例唯一标识：hostname:uuid，写入租约 value 便于运维排查 */
    private final String instanceId;

    private final AtomicInteger workerId = new AtomicInteger(-1);
    private final AtomicBoolean leaseValid = new AtomicBoolean(false);

    public WorkerLeaseService(RedissonClient redisson, BitLayoutProperties layout,
                              DuidProperties properties) {
        this.redisson = redisson;
        this.layout = layout;
        this.workerConfig = properties.worker();
        this.instanceId = buildInstanceId();
    }

    /**
     * 扫描 0..maxWorkers-1，抢占第一个空闲槽位。
     *
     * @throws IllegalStateException 所有槽位均被占用
     */
    public void acquireOrThrow() {
        int max = layout.maxWorkers();
        for (int id = 0; id < max; id++) {
            if (tryAcquire(id)) {
                workerId.set(id);
                leaseValid.set(true);
                log.info("Acquired workerId={} instanceId={}", id, instanceId);
                return;
            }
        }
        throw new IllegalStateException("No available worker slot in range 0.." + (max - 1));
    }

    /**
     * 续约当前 worker 租约；若槽位已被其他实例抢占则标记 leaseValid=false。
     */
    public void renewLease() {
        int id = workerId.get();
        if (id < 0) {
            return;
        }
        RBucket<String> bucket = redisson.getBucket(WORKER_KEY_PREFIX + id);
        String current = bucket.get();
        if (instanceId.equals(current)) {
            bucket.expire(Duration.ofSeconds(workerConfig.leaseTtlSeconds()));
            leaseValid.set(true);
        } else {
            leaseValid.set(false);
            log.warn("Lost worker lease for workerId={}, current holder={}", id, current);
        }
    }

    /** 是否已持有有效租约，可对外发号 */
    public boolean isReady() {
        return workerId.get() >= 0 && leaseValid.get();
    }

    public int workerId() {
        return workerId.get();
    }

    /** 尝试抢占指定槽位，SET NX + TTL */
    private boolean tryAcquire(int id) {
        RBucket<String> bucket = redisson.getBucket(WORKER_KEY_PREFIX + id);
        return bucket.setIfAbsent(instanceId, Duration.ofSeconds(workerConfig.leaseTtlSeconds()));
    }

    private static String buildInstanceId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return host + ":" + UUID.randomUUID();
        } catch (Exception e) {
            return "unknown:" + UUID.randomUUID();
        }
    }
}
