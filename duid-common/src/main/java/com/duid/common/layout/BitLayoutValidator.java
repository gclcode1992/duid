package com.duid.common.layout;

/**
 * ID 位布局启动校验器。
 * <p>
 * 校验规则：sign(1) + timestamp + worker + sequence + rollback 必须恰好等于 64 bit，
 * 数据位恰好 63 bit，保证 long 类型 ID 为正数。
 * </p>
 *
 * @author gclcode@126.com
 */
public final class BitLayoutValidator {

    private BitLayoutValidator() {}

    /**
     * 校验位布局配置，不通过则抛出 {@link IllegalStateException}。
     */
    public static void validate(BitLayoutProperties layout) {
        requirePositive(layout.timestampBits(), "timestamp-bits");
        requirePositive(layout.workerBits(), "worker-bits");
        requirePositive(layout.sequenceBits(), "sequence-bits");
        requirePositive(layout.rollbackBits(), "rollback-bits");

        // 总位数必须恰好 64，不可多也不可少
        int total = layout.totalBits();
        if (total != BitLayoutProperties.REQUIRED_TOTAL_BITS) {
            String hint = total > BitLayoutProperties.REQUIRED_TOTAL_BITS
                    ? "Reduce bit allocation (e.g. lower timestamp-bits or worker-bits)."
                    : "Increase bit allocation (prefer raising timestamp-bits to fill unused bits).";
            throw new IllegalStateException(String.format(
                    "ID layout must use exactly 64 bits, but sign(1) + timestamp(%d) + worker(%d) "
                            + "+ sequence(%d) + rollback(%d) = %d. %s",
                    layout.timestampBits(), layout.workerBits(), layout.sequenceBits(),
                    layout.rollbackBits(), total, hint));
        }

        // 数据位 63，最高 bit 恒为 0
        int dataBits = layout.timestampBits() + layout.workerBits()
                + layout.sequenceBits() + layout.rollbackBits();
        if (dataBits != BitLayoutProperties.REQUIRED_DATA_BITS) {
            throw new IllegalStateException(String.format(
                    "ID data bits must be exactly 63 (sign bit fixed at 0), but got %d", dataBits));
        }

        if (layout.timestampBits() < 32) {
            throw new IllegalStateException("timestamp-bits must be >= 32");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value < 1) {
            throw new IllegalStateException(name + " must be >= 1");
        }
    }
}
