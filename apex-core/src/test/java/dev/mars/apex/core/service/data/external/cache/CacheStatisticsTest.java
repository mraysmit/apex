package dev.mars.apex.core.service.data.external.cache;

import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CacheStatistics.
 * 
 * Tests cover:
 * - Statistics initialization and lifecycle
 * - Hit/miss tracking and rate calculations
 * - Put/removal/eviction tracking
 * - Load time tracking and averages
 * - Thread safety and concurrent operations
 * - Reset functionality
 * - Snapshot creation
 * - Edge cases and boundary conditions
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class CacheStatisticsTest {

    private CacheStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new CacheStatistics();
    }

    // ========================================
    // Initialization and Lifecycle Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with zero values")
    void testInitialization() {
        assertEquals(0, statistics.getHits());
        assertEquals(0, statistics.getMisses());
        assertEquals(0, statistics.getPuts());
        assertEquals(0, statistics.getRemovals());
        assertEquals(0, statistics.getEvictions());
        assertEquals(0, statistics.getTotalLoadTime());
        assertEquals(0, statistics.getRequestCount());
        assertEquals(0.0, statistics.getHitRate(), 0.001);
        assertEquals(0.0, statistics.getMissRate(), 0.001);
        assertEquals(0.0, statistics.getAverageLoadTime(), 0.001);
        assertEquals(0.0, statistics.getAverageLoadTimeMillis(), 0.001);
        
        assertNotNull(statistics.getCreatedAt());
        assertNotNull(statistics.getLastResetTime());
        assertEquals(statistics.getCreatedAt(), statistics.getLastResetTime());
    }

    // ========================================
    // Hit/Miss Tracking Tests
    // ========================================

    @Test
    @DisplayName("Should record hits correctly")
    void testRecordHit() {
        statistics.recordHit();
        statistics.recordHit();
        statistics.recordHit();
        
        assertEquals(3, statistics.getHits());
        assertEquals(0, statistics.getMisses());
        assertEquals(3, statistics.getRequestCount());
        assertEquals(100.0, statistics.getHitRate(), 0.001);
        assertEquals(0.0, statistics.getMissRate(), 0.001);
    }

    @Test
    @DisplayName("Should record misses correctly")
    void testRecordMiss() {
        statistics.recordMiss();
        statistics.recordMiss();
        
        assertEquals(0, statistics.getHits());
        assertEquals(2, statistics.getMisses());
        assertEquals(2, statistics.getRequestCount());
        assertEquals(0.0, statistics.getHitRate(), 0.001);
        assertEquals(100.0, statistics.getMissRate(), 0.001);
    }

    @Test
    @DisplayName("Should calculate hit and miss rates correctly")
    void testHitMissRates() {
        statistics.recordHit();
        statistics.recordHit();
        statistics.recordHit();
        statistics.recordMiss();
        
        assertEquals(3, statistics.getHits());
        assertEquals(1, statistics.getMisses());
        assertEquals(4, statistics.getRequestCount());
        assertEquals(75.0, statistics.getHitRate(), 0.001);
        assertEquals(25.0, statistics.getMissRate(), 0.001);
    }

    @Test
    @DisplayName("Should handle zero requests for rate calculations")
    void testRatesWithZeroRequests() {
        assertEquals(0.0, statistics.getHitRate(), 0.001);
        assertEquals(0.0, statistics.getMissRate(), 0.001);
    }

    // ========================================
    // Operation Tracking Tests
    // ========================================

    @Test
    @DisplayName("Should record puts correctly")
    void testRecordPut() {
        statistics.recordPut();
        statistics.recordPut();
        statistics.recordPut();
        
        assertEquals(3, statistics.getPuts());
        assertEquals(0, statistics.getRemovals());
        assertEquals(0, statistics.getEvictions());
    }

    @Test
    @DisplayName("Should record removals correctly")
    void testRecordRemoval() {
        statistics.recordRemoval();
        statistics.recordRemoval();
        
        assertEquals(0, statistics.getPuts());
        assertEquals(2, statistics.getRemovals());
        assertEquals(0, statistics.getEvictions());
    }

    @Test
    @DisplayName("Should record evictions correctly")
    void testRecordEviction() {
        statistics.recordEviction();
        statistics.recordEviction();
        statistics.recordEviction();
        statistics.recordEviction();
        
        assertEquals(0, statistics.getPuts());
        assertEquals(0, statistics.getRemovals());
        assertEquals(4, statistics.getEvictions());
    }

    @Test
    @DisplayName("Should track all operations together")
    void testAllOperations() {
        statistics.recordHit();
        statistics.recordHit();
        statistics.recordMiss();
        statistics.recordPut();
        statistics.recordPut();
        statistics.recordPut();
        statistics.recordRemoval();
        statistics.recordEviction();
        statistics.recordEviction();
        
        assertEquals(2, statistics.getHits());
        assertEquals(1, statistics.getMisses());
        assertEquals(3, statistics.getRequestCount());
        assertEquals(3, statistics.getPuts());
        assertEquals(1, statistics.getRemovals());
        assertEquals(2, statistics.getEvictions());
        assertEquals(66.67, statistics.getHitRate(), 0.01);
        assertEquals(33.33, statistics.getMissRate(), 0.01);
    }

    // ========================================
    // Load Time Tracking Tests
    // ========================================

    @Test
    @DisplayName("Should record load time correctly")
    void testRecordLoadTime() {
        long loadTime1 = 1_000_000L; // 1ms in nanoseconds
        long loadTime2 = 2_000_000L; // 2ms in nanoseconds
        long loadTime3 = 3_000_000L; // 3ms in nanoseconds
        
        statistics.recordLoadTime(loadTime1);
        statistics.recordLoadTime(loadTime2);
        statistics.recordLoadTime(loadTime3);
        
        assertEquals(6_000_000L, statistics.getTotalLoadTime());
    }

    @Test
    @DisplayName("Should calculate average load time correctly")
    void testAverageLoadTime() {
        statistics.recordHit();
        statistics.recordMiss();
        statistics.recordLoadTime(2_000_000L); // 2ms
        statistics.recordLoadTime(4_000_000L); // 4ms
        
        assertEquals(6_000_000L, statistics.getTotalLoadTime());
        assertEquals(2, statistics.getRequestCount());
        assertEquals(3_000_000.0, statistics.getAverageLoadTime(), 0.001); // 3ms average
        assertEquals(3.0, statistics.getAverageLoadTimeMillis(), 0.001); // 3ms in milliseconds
    }

    @Test
    @DisplayName("Should handle zero requests for average load time")
    void testAverageLoadTimeWithZeroRequests() {
        statistics.recordLoadTime(1_000_000L);
        
        assertEquals(0.0, statistics.getAverageLoadTime(), 0.001);
        assertEquals(0.0, statistics.getAverageLoadTimeMillis(), 0.001);
    }

    @Test
    @DisplayName("Should handle zero load time")
    void testZeroLoadTime() {
        statistics.recordHit();
        statistics.recordLoadTime(0L);
        
        assertEquals(0L, statistics.getTotalLoadTime());
        assertEquals(0.0, statistics.getAverageLoadTime(), 0.001);
        assertEquals(0.0, statistics.getAverageLoadTimeMillis(), 0.001);
    }

    // ========================================
    // Reset Functionality Tests
    // ========================================

    @Test
    @DisplayName("Should reset all statistics")
    void testReset() throws InterruptedException {
        // Record some statistics
        statistics.recordHit();
        statistics.recordMiss();
        statistics.recordPut();
        statistics.recordRemoval();
        statistics.recordEviction();
        statistics.recordLoadTime(1_000_000L);
        
        LocalDateTime beforeReset = LocalDateTime.now();
        Thread.sleep(10); // Small delay to ensure different timestamp
        
        statistics.reset();
        
        // Verify all statistics are reset
        assertEquals(0, statistics.getHits());
        assertEquals(0, statistics.getMisses());
        assertEquals(0, statistics.getPuts());
        assertEquals(0, statistics.getRemovals());
        assertEquals(0, statistics.getEvictions());
        assertEquals(0, statistics.getTotalLoadTime());
        assertEquals(0, statistics.getRequestCount());
        assertEquals(0.0, statistics.getHitRate(), 0.001);
        assertEquals(0.0, statistics.getMissRate(), 0.001);
        assertEquals(0.0, statistics.getAverageLoadTime(), 0.001);
        
        // Verify reset time is updated
        assertTrue(statistics.getLastResetTime().isAfter(beforeReset));
        
        // Created time should remain unchanged
        assertNotNull(statistics.getCreatedAt());
    }

    // ========================================
    // Snapshot Functionality Tests
    // ========================================

    @Test
    @DisplayName("Should create accurate snapshot")
    void testSnapshot() {
        // Record some statistics
        statistics.recordHit();
        statistics.recordHit();
        statistics.recordMiss();
        statistics.recordPut();
        statistics.recordRemoval();
        statistics.recordEviction();
        statistics.recordLoadTime(2_000_000L);
        
        CacheStatistics snapshot = statistics.snapshot();
        
        // Verify snapshot has same values
        assertEquals(statistics.getHits(), snapshot.getHits());
        assertEquals(statistics.getMisses(), snapshot.getMisses());
        assertEquals(statistics.getPuts(), snapshot.getPuts());
        assertEquals(statistics.getRemovals(), snapshot.getRemovals());
        assertEquals(statistics.getEvictions(), snapshot.getEvictions());
        assertEquals(statistics.getTotalLoadTime(), snapshot.getTotalLoadTime());
        assertEquals(statistics.getRequestCount(), snapshot.getRequestCount());
        assertEquals(statistics.getHitRate(), snapshot.getHitRate(), 0.001);
        assertEquals(statistics.getMissRate(), snapshot.getMissRate(), 0.001);
        
        // Verify snapshot is independent
        statistics.recordHit();
        assertNotEquals(statistics.getHits(), snapshot.getHits());
    }

    @Test
    @DisplayName("Should create snapshot of empty statistics")
    void testSnapshotEmpty() {
        CacheStatistics snapshot = statistics.snapshot();
        
        assertEquals(0, snapshot.getHits());
        assertEquals(0, snapshot.getMisses());
        assertEquals(0, snapshot.getPuts());
        assertEquals(0, snapshot.getRemovals());
        assertEquals(0, snapshot.getEvictions());
        assertEquals(0, snapshot.getTotalLoadTime());
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    @DisplayName("Should handle concurrent operations")
    void testConcurrentOperations() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        switch (threadId % 5) {
                            case 0:
                                statistics.recordHit();
                                break;
                            case 1:
                                statistics.recordMiss();
                                break;
                            case 2:
                                statistics.recordPut();
                                break;
                            case 3:
                                statistics.recordRemoval();
                                break;
                            case 4:
                                statistics.recordEviction();
                                statistics.recordLoadTime(1000L);
                                break;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }
        
        latch.await(10, TimeUnit.SECONDS);
        
        // Verify totals
        long expectedHits = 2 * operationsPerThread; // threads 0 and 5
        long expectedMisses = 2 * operationsPerThread; // threads 1 and 6
        long expectedPuts = 2 * operationsPerThread; // threads 2 and 7
        long expectedRemovals = 2 * operationsPerThread; // threads 3 and 8
        long expectedEvictions = 2 * operationsPerThread; // threads 4 and 9
        
        assertEquals(expectedHits, statistics.getHits());
        assertEquals(expectedMisses, statistics.getMisses());
        assertEquals(expectedPuts, statistics.getPuts());
        assertEquals(expectedRemovals, statistics.getRemovals());
        assertEquals(expectedEvictions, statistics.getEvictions());
        assertEquals(expectedHits + expectedMisses, statistics.getRequestCount());
        assertEquals(50.0, statistics.getHitRate(), 0.001);
        assertEquals(50.0, statistics.getMissRate(), 0.001);
    }

    // ========================================
    // String Representation Tests
    // ========================================

    @Test
    @DisplayName("Should have meaningful toString representation")
    void testToString() {
        statistics.recordHit();
        statistics.recordHit();
        statistics.recordMiss();
        statistics.recordPut();
        statistics.recordRemoval();
        statistics.recordEviction();
        statistics.recordLoadTime(2_000_000L); // 2ms
        
        String toString = statistics.toString();
        
        assertTrue(toString.contains("CacheStatistics"));
        assertTrue(toString.contains("hits=2"));
        assertTrue(toString.contains("misses=1"));
        assertTrue(toString.contains("hitRate=66.67%"));
        assertTrue(toString.contains("puts=1"));
        assertTrue(toString.contains("removals=1"));
        assertTrue(toString.contains("evictions=1"));
        assertTrue(toString.contains("avgLoadTime="));
    }

    @Test
    @DisplayName("Should handle toString with zero values")
    void testToStringWithZeroValues() {
        String toString = statistics.toString();
        
        assertTrue(toString.contains("CacheStatistics"));
        assertTrue(toString.contains("hits=0"));
        assertTrue(toString.contains("misses=0"));
        assertTrue(toString.contains("hitRate=0.00%"));
        assertTrue(toString.contains("puts=0"));
        assertTrue(toString.contains("removals=0"));
        assertTrue(toString.contains("evictions=0"));
        assertTrue(toString.contains("avgLoadTime=0.00ms"));
    }

    // ========================================
    // Edge Cases and Boundary Conditions Tests
    // ========================================

    @Test
    @DisplayName("Should handle very large values")
    void testLargeValues() {
        long largeValue = Long.MAX_VALUE / 2;
        
        statistics.recordLoadTime(largeValue);
        statistics.recordHit();
        
        assertEquals(largeValue, statistics.getTotalLoadTime());
        assertEquals((double) largeValue, statistics.getAverageLoadTime(), 0.001);
    }

    @Test
    @DisplayName("Should handle negative load times gracefully")
    void testNegativeLoadTime() {
        // The implementation should handle negative values
        // (though they may not make logical sense)
        statistics.recordLoadTime(-1000L);
        statistics.recordHit();
        
        assertEquals(-1000L, statistics.getTotalLoadTime());
        assertEquals(-1000.0, statistics.getAverageLoadTime(), 0.001);
    }

    @Test
    @DisplayName("Should maintain precision in calculations")
    void testPrecisionInCalculations() {
        // Test with values that might cause precision issues
        for (int i = 0; i < 3; i++) {
            statistics.recordHit();
        }
        for (int i = 0; i < 7; i++) {
            statistics.recordMiss();
        }
        
        assertEquals(10, statistics.getRequestCount());
        assertEquals(30.0, statistics.getHitRate(), 0.001);
        assertEquals(70.0, statistics.getMissRate(), 0.001);
        
        // Hit rate + miss rate should equal 100%
        assertEquals(100.0, statistics.getHitRate() + statistics.getMissRate(), 0.001);
    }
}
