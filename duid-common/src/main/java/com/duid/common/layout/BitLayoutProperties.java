package com.duid.common.layout;

/**
 * ID 位布局配置。
 * <p>
 * 布局结构：符号位(1) + 时间戳 + workerId + 序列号 + 回拨代际 = 恰好 64 bit。
 * 默认方案4：41 + 6 + 13 + 3。
 * </p>
 *
 * @author gclcode@126.com
 */
public record BitLayoutProperties(
        /** 相对 epoch 的毫秒时间戳占用位数 */
        int timestampBits,
        /** 机器标识占用位数，最大副本数 = 2^workerBits */
        int workerBits,
        /** 同毫秒序列号占用位数，单毫秒上限 = 2^sequenceBits */
        int sequenceBits,
        /** 时钟回拨代际占用位数，档位数 = 2^rollbackBits */
        int rollbackBits
) {
    /** 符号位固定为 1 bit，恒为 0 以保证 ID 为正数 */
    public static final int SIGN_BITS = 1;
    /** 总位数必须为 64 */
    public static final int REQUIRED_TOTAL_BITS = 64;
    /** 数据位必须为 63（不含符号位） */
    public static final int REQUIRED_DATA_BITS = 63;

    /**
     * 返回默认方案4位布局：41 + 6 + 13 + 3。
     */
    public static BitLayoutProperties defaults() {
        return new BitLayoutProperties(41, 6, 13, 3);
    }

    public int signBits() {
        return SIGN_BITS;
    }

    /** 计算总位数（含符号位） */
    public int totalBits() {
        return signBits() + timestampBits + workerBits + sequenceBits + rollbackBits;
    }

    /** 低位总宽度：worker + sequence + rollback */
    public int lowBits() {
        return workerBits + sequenceBits + rollbackBits;
    }

    /** 时间戳左移位数 */
    public int timestampShift() {
        return lowBits();
    }

    /** workerId 左移位数 */
    public int workerShift() {
        return sequenceBits + rollbackBits;
    }

    /** 序列号左移位数 */
    public int sequenceShift() {
        return rollbackBits;
    }

    public long workerMask() {
        return bitMask(workerBits);
    }

    public long sequenceMask() {
        return bitMask(sequenceBits);
    }

    public long rollbackMask() {
        return bitMask(rollbackBits);
    }

    public long timestampMask() {
        return bitMask(timestampBits);
    }

    /** 最大 worker 槽位数（含 0） */
    public int maxWorkers() {
        return 1 << workerBits;
    }

    /** 单毫秒最大序列值 */
    public int maxSequence() {
        return (1 << sequenceBits) - 1;
    }

    /** 最大回拨代际值 */
    public int maxRollbackGen() {
        return (1 << rollbackBits) - 1;
    }

    /**
     * 将各字段按位拼装为 64-bit ID。
     */
    public long compose(long timestamp, long workerId, long sequence, long rollbackGen) {
        return (timestamp << timestampShift())
                | (workerId << workerShift())
                | (sequence << sequenceShift())
                | rollbackGen;
    }

    /**
     * 从 ID 反解各字段。
     */
    public ParsedId parse(long id) {
        long rollbackGen = id & rollbackMask();
        long sequence = (id >> sequenceShift()) & sequenceMask();
        long workerId = (id >> workerShift()) & workerMask();
        long timestamp = (id >> timestampShift()) & timestampMask();
        return new ParsedId(timestamp, workerId, sequence, rollbackGen);
    }

    private static long bitMask(int bits) {
        if (bits <= 0 || bits >= Long.SIZE) {
            throw new IllegalArgumentException("Invalid bit width: " + bits);
        }
        return (1L << bits) - 1;
    }

    /** 解析后的 ID 各字段 */
    public record ParsedId(long timestamp, long workerId, long sequence, long rollbackGen) {}
}
