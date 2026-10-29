package com.duid.common.generator;

import com.duid.common.layout.BitLayoutProperties;
import com.duid.common.layout.BitLayoutValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongSupplier;

/**
 * 可配置位布局的 Snowflake 发号器。
 * <p>
 * 热路径纯内存操作，线程安全。支持时钟回拨代际（rollbackGen）区分同物理时间下的多轮发号。
 * </p>
 *
 * @author gclcode@126.com
 */
public class SnowflakeIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    private final BitLayoutProperties layout;
    private final long epochMillis;
    private final LongSupplier clock;
    private final long workerId;

    /** 上次发号使用的时间戳（相对 epoch 毫秒） */
    private long lastTimestamp = -1L;
    /** 当前毫秒内的序列号 */
    private long sequence = 0L;
    /** 时钟回拨代际，用于区分回拨后同一物理时间的重复发号 */
    private long rollbackGen = 0L;

    public SnowflakeIdGenerator(BitLayoutProperties layout, long epochMillis, long workerId) {
        this(layout, epochMillis, workerId, System::currentTimeMillis);
    }

    /**
     * @param clock 可注入时钟，便于单元测试
     */
    public SnowflakeIdGenerator(BitLayoutProperties layout, long epochMillis, long workerId,
                                LongSupplier clock) {
        BitLayoutValidator.validate(layout);
        if (workerId < 0 || workerId > layout.workerMask()) {
            throw new IllegalArgumentException("workerId out of range: " + workerId);
        }
        this.layout = layout;
        this.epochMillis = epochMillis;
        this.workerId = workerId;
        this.clock = clock;
    }

    /**
     * 生成下一个全局唯一 ID。
     */
    public synchronized long nextId() {
        long current = currentTimestamp();

        if (current > lastTimestamp) {
            // 时钟向前：正常发号，重置回拨代际
            rollbackGen = 0;
            lastTimestamp = current;
            sequence = 0L;
        } else if (current == lastTimestamp) {
            // 同毫秒：序列递增，打满则等待下一毫秒
            sequence++;
            if (sequence > layout.maxSequence()) {
                current = waitNextMillis(lastTimestamp);
                lastTimestamp = current;
                sequence = 0L;
            }
        } else {
            // 时钟回拨：递增代际位，与历史 ID 区分
            long offsetMs = lastTimestamp - current;
            rollbackGen++;
            if (rollbackGen > layout.maxRollbackGen()) {
                log.warn("Clock rollback gen exhausted, spinning until caught up: workerId={}, "
                                + "offsetMs={}, currentTs={}, lastTs={}, maxRollbackGen={}",
                        workerId, offsetMs, current, lastTimestamp, layout.maxRollbackGen());
                current = waitUntilCaughtUp(lastTimestamp);
                rollbackGen = 0;
                lastTimestamp = current;
                sequence = 0L;
                log.warn("Clock rollback spin finished: workerId={}, caughtUpTs={}", workerId, current);
            } else {
                log.warn("Clock rollback detected: workerId={}, offsetMs={}, currentTs={}, "
                                + "lastTs={}, rollbackGen={}/{}",
                        workerId, offsetMs, current, lastTimestamp, rollbackGen, layout.maxRollbackGen());
                lastTimestamp = current;
                sequence = 0L;
            }
        }

        return layout.compose(lastTimestamp, workerId, sequence, rollbackGen);
    }

    /**
     * 从 Redis 水位恢复发号状态，用于进程重启后防止重复发号。
     */
    public synchronized void restoreState(long lastTs, long rollbackGen, long sequence) {
        this.lastTimestamp = lastTs;
        this.rollbackGen = rollbackGen;
        this.sequence = sequence;
        long now = currentTimestamp();
        if (now < lastTimestamp) {
            // 重启后本机时钟仍落后：按回拨处理
            long offsetMs = lastTimestamp - now;
            this.rollbackGen = Math.min(rollbackGen + 1, layout.maxRollbackGen());
            this.lastTimestamp = now;
            this.sequence = 0L;
            log.warn("Startup clock behind watermark: workerId={}, offsetMs={}, watermarkTs={}, "
                            + "currentTs={}, rollbackGen={}/{}",
                    workerId, offsetMs, lastTs, now, this.rollbackGen, layout.maxRollbackGen());
        }
    }

    /** 快照当前发号状态，用于写入 Redis 水位 */
    public synchronized GeneratorState snapshotState() {
        return new GeneratorState(lastTimestamp, rollbackGen, sequence);
    }

    public long workerId() {
        return workerId;
    }

    private long currentTimestamp() {
        return clock.getAsLong() - epochMillis;
    }

    /** 等待直到进入下一毫秒 */
    private long waitNextMillis(long lastTs) {
        long ts = currentTimestamp();
        while (ts <= lastTs) {
            Thread.onSpinWait();
            ts = currentTimestamp();
        }
        return ts;
    }

    /** 回拨代际用尽时，自旋等待时钟追上 lastTs */
    private long waitUntilCaughtUp(long lastTs) {
        long ts = currentTimestamp();
        while (ts < lastTs) {
            Thread.onSpinWait();
            ts = currentTimestamp();
        }
        return ts;
    }

    /**
     * 发号器状态，序列化后存入 Redis 水位。
     *
     * @author gclcode@126.com
     */
    public record GeneratorState(long lastTimestamp, long rollbackGen, long sequence) {

        /** 格式：lastTs:rollbackGen:sequence */
        public String serialize() {
            return lastTimestamp + ":" + rollbackGen + ":" + sequence;
        }

        public static GeneratorState deserialize(String value) {
            String[] parts = value.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid watermark: " + value);
            }
            return new GeneratorState(
                    Long.parseLong(parts[0]),
                    Long.parseLong(parts[1]),
                    Long.parseLong(parts[2]));
        }
    }
}
