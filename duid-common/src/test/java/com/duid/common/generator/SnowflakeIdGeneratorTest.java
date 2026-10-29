package com.duid.common.generator;

import com.duid.common.layout.BitLayoutProperties;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SnowflakeIdGenerator} 单元测试。
 *
 * @author gclcode@126.com
 */
class SnowflakeIdGeneratorTest {

    @Test
    void generatesUniqueIdsInSameMillisecond() {
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(
                BitLayoutProperties.defaults(), 0L, 1L, clock::get);

        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertTrue(ids.add(gen.nextId()));
        }
        assertEquals(1000, ids.size());
    }

    @Test
    void rollbackIncreasesGen() {
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(
                BitLayoutProperties.defaults(), 0L, 2L, clock::get);

        long first = gen.nextId();
        clock.set(1_699_999_999_999L); // 模拟时钟回拨 1ms
        long second = gen.nextId();

        assertTrue(second != first);
        assertEquals(1L, BitLayoutProperties.defaults().parse(second).rollbackGen());
    }

    @Test
    void concurrentNoDuplicates() throws InterruptedException {
        AtomicLong clock = new AtomicLong(1_700_000_000_000L);
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(
                BitLayoutProperties.defaults(), 0L, 3L, clock::get);

        int threads = 8;
        int perThread = 500;
        Set<Long> ids = java.util.Collections.synchronizedSet(new HashSet<>());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        ids.add(gen.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        assertEquals(threads * perThread, ids.size());
    }
}
