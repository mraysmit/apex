package dev.mars.apex.core.service.data.external;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DataSourceMetrics.
 * 
 * Tests cover:
 * - Metrics initialization and lifecycle
 * - Request tracking (successful and failed)
 * - Response time tracking and calculations
 * - Cache metrics tracking
 * - Connection metrics tracking
 * - Data volume metrics tracking
 * - Calculated metrics (rates, averages)
 * - Thread safety and concurrent operations
 * - Reset functionality
 * - Edge cases and boundary conditions
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataSourceMetricsTest {

    private DataSourceMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new DataSourceMetrics();
    }

    // ========================================
    // Initialization and Lifecycle Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with zero values")
    void testInitialization() {
        assertEquals(0, metrics.getTotalRequests());
        assertEquals(0, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertEquals(0, metrics.getTotalResponseTime());
        assertEquals(0, metrics.getMinResponseTime());
        assertEquals(0, metrics.getMaxResponseTime());
        assertNull(metrics.getLastRequestTime());
        
        assertEquals(0, metrics.getCacheHits());
        assertEquals(0, metrics.getCacheMisses());
        assertEquals(0, metrics.getCacheEvictions());
        
        assertEquals(0, metrics.getConnectionAttempts());
        assertEquals(0, metrics.getSuccessfulConnections());
        assertEquals(0, metrics.getConnectionFailures());
        
        assertEquals(0, metrics.getBytesRead());
        assertEquals(0, metrics.getBytesWritten());
        assertEquals(0, metrics.getRecordsProcessed());
        
        assertNotNull(metrics.getCreatedAt());
        assertNotNull(metrics.getLastResetTime());
        assertEquals(metrics.getCreatedAt(), metrics.getLastResetTime());
    }

    @Test
    @DisplayName("Should initialize calculated metrics to zero")
    void testInitialCalculatedMetrics() {
        assertEquals(0.0, metrics.getSuccessRate(), 0.001);
        assertEquals(0.0, metrics.getErrorRate(), 0.001);
        assertEquals(0.0, metrics.getAverageResponseTime(), 0.001);
        assertEquals(0.0, metrics.getCacheHitRate(), 0.001);
        assertEquals(0.0, metrics.getConnectionSuccessRate(), 0.001);
    }

    // ========================================
    // Request Tracking Tests
    // ========================================

    @Test
    @DisplayName("Should record successful requests correctly")
    void testRecordSuccessfulRequest() {
        long responseTime = 150;
        
        metrics.recordSuccessfulRequest(responseTime);
        
        assertEquals(1, metrics.getTotalRequests());
        assertEquals(1, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertEquals(responseTime, metrics.getTotalResponseTime());
        assertEquals(responseTime, metrics.getMinResponseTime());
        assertEquals(responseTime, metrics.getMaxResponseTime());
        assertNotNull(metrics.getLastRequestTime());
    }

    @Test
    @DisplayName("Should record failed requests correctly")
    void testRecordFailedRequest() {
        long responseTime = 200;
        
        metrics.recordFailedRequest(responseTime);
        
        assertEquals(1, metrics.getTotalRequests());
        assertEquals(0, metrics.getSuccessfulRequests());
        assertEquals(1, metrics.getFailedRequests());
        assertEquals(responseTime, metrics.getTotalResponseTime());
        assertEquals(responseTime, metrics.getMinResponseTime());
        assertEquals(responseTime, metrics.getMaxResponseTime());
        assertNotNull(metrics.getLastRequestTime());
    }

    @Test
    @DisplayName("Should track multiple requests correctly")
    void testMultipleRequests() {
        metrics.recordSuccessfulRequest(100);
        metrics.recordFailedRequest(200);
        metrics.recordSuccessfulRequest(50);
        
        assertEquals(3, metrics.getTotalRequests());
        assertEquals(2, metrics.getSuccessfulRequests());
        assertEquals(1, metrics.getFailedRequests());
        assertEquals(350, metrics.getTotalResponseTime());
        assertEquals(50, metrics.getMinResponseTime());
        assertEquals(200, metrics.getMaxResponseTime());
    }

    @Test
    @DisplayName("Should calculate success and error rates correctly")
    void testSuccessAndErrorRates() {
        metrics.recordSuccessfulRequest(100);
        metrics.recordSuccessfulRequest(150);
        metrics.recordFailedRequest(200);
        
        assertEquals(66.67, metrics.getSuccessRate(), 0.01);
        assertEquals(33.33, metrics.getErrorRate(), 0.01);
    }

    @Test
    @DisplayName("Should calculate average response time correctly")
    void testAverageResponseTime() {
        metrics.recordSuccessfulRequest(100);
        metrics.recordSuccessfulRequest(200);
        metrics.recordFailedRequest(300);
        
        assertEquals(200.0, metrics.getAverageResponseTime(), 0.001);
    }

    // ========================================
    // Cache Metrics Tests
    // ========================================

    @Test
    @DisplayName("Should record cache hits correctly")
    void testRecordCacheHit() {
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        
        assertEquals(2, metrics.getCacheHits());
        assertEquals(0, metrics.getCacheMisses());
        assertEquals(0, metrics.getCacheEvictions());
    }

    @Test
    @DisplayName("Should record cache misses correctly")
    void testRecordCacheMiss() {
        metrics.recordCacheMiss();
        metrics.recordCacheMiss();
        metrics.recordCacheMiss();
        
        assertEquals(0, metrics.getCacheHits());
        assertEquals(3, metrics.getCacheMisses());
        assertEquals(0, metrics.getCacheEvictions());
    }

    @Test
    @DisplayName("Should record cache evictions correctly")
    void testRecordCacheEviction() {
        metrics.recordCacheEviction();
        
        assertEquals(0, metrics.getCacheHits());
        assertEquals(0, metrics.getCacheMisses());
        assertEquals(1, metrics.getCacheEvictions());
    }

    @Test
    @DisplayName("Should calculate cache hit rate correctly")
    void testCacheHitRate() {
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        metrics.recordCacheMiss();
        
        assertEquals(75.0, metrics.getCacheHitRate(), 0.001);
    }

    @Test
    @DisplayName("Should handle zero cache requests for hit rate")
    void testCacheHitRateWithZeroRequests() {
        assertEquals(0.0, metrics.getCacheHitRate(), 0.001);
    }

    // ========================================
    // Connection Metrics Tests
    // ========================================

    @Test
    @DisplayName("Should record connection attempts correctly")
    void testRecordConnectionAttempt() {
        metrics.recordConnectionAttempt();
        metrics.recordConnectionAttempt();
        
        assertEquals(2, metrics.getConnectionAttempts());
        assertEquals(0, metrics.getSuccessfulConnections());
        assertEquals(0, metrics.getConnectionFailures());
    }

    @Test
    @DisplayName("Should record successful connections correctly")
    void testRecordSuccessfulConnection() {
        metrics.recordSuccessfulConnection();
        metrics.recordSuccessfulConnection();
        metrics.recordSuccessfulConnection();
        
        assertEquals(0, metrics.getConnectionAttempts());
        assertEquals(3, metrics.getSuccessfulConnections());
        assertEquals(0, metrics.getConnectionFailures());
    }

    @Test
    @DisplayName("Should record connection failures correctly")
    void testRecordConnectionFailure() {
        metrics.recordConnectionFailure();
        
        assertEquals(0, metrics.getConnectionAttempts());
        assertEquals(0, metrics.getSuccessfulConnections());
        assertEquals(1, metrics.getConnectionFailures());
    }

    @Test
    @DisplayName("Should calculate connection success rate correctly")
    void testConnectionSuccessRate() {
        metrics.recordConnectionAttempt();
        metrics.recordConnectionAttempt();
        metrics.recordConnectionAttempt();
        metrics.recordConnectionAttempt();
        metrics.recordSuccessfulConnection();
        metrics.recordSuccessfulConnection();
        metrics.recordSuccessfulConnection();
        
        assertEquals(75.0, metrics.getConnectionSuccessRate(), 0.001);
    }

    @Test
    @DisplayName("Should handle zero connection attempts for success rate")
    void testConnectionSuccessRateWithZeroAttempts() {
        assertEquals(0.0, metrics.getConnectionSuccessRate(), 0.001);
    }

    // ========================================
    // Data Volume Metrics Tests
    // ========================================

    @Test
    @DisplayName("Should record bytes read correctly")
    void testRecordBytesRead() {
        metrics.recordBytesRead(1024);
        metrics.recordBytesRead(2048);
        
        assertEquals(3072, metrics.getBytesRead());
        assertEquals(0, metrics.getBytesWritten());
        assertEquals(0, metrics.getRecordsProcessed());
    }

    @Test
    @DisplayName("Should record bytes written correctly")
    void testRecordBytesWritten() {
        metrics.recordBytesWritten(512);
        metrics.recordBytesWritten(1024);
        
        assertEquals(0, metrics.getBytesRead());
        assertEquals(1536, metrics.getBytesWritten());
        assertEquals(0, metrics.getRecordsProcessed());
    }

    @Test
    @DisplayName("Should record records processed correctly")
    void testRecordRecordsProcessed() {
        metrics.recordRecordsProcessed(100);
        metrics.recordRecordsProcessed(250);
        
        assertEquals(0, metrics.getBytesRead());
        assertEquals(0, metrics.getBytesWritten());
        assertEquals(350, metrics.getRecordsProcessed());
    }

    // ========================================
    // Reset Functionality Tests
    // ========================================

    @Test
    @DisplayName("Should reset all metrics to zero")
    void testReset() throws InterruptedException {
        // Record some metrics
        metrics.recordSuccessfulRequest(100);
        metrics.recordFailedRequest(200);
        metrics.recordCacheHit();
        metrics.recordCacheMiss();
        metrics.recordConnectionAttempt();
        metrics.recordBytesRead(1024);
        metrics.recordRecordsProcessed(50);
        
        LocalDateTime beforeReset = LocalDateTime.now();
        Thread.sleep(10); // Small delay to ensure different timestamp
        
        metrics.reset();
        
        // Verify all metrics are reset
        assertEquals(0, metrics.getTotalRequests());
        assertEquals(0, metrics.getSuccessfulRequests());
        assertEquals(0, metrics.getFailedRequests());
        assertEquals(0, metrics.getTotalResponseTime());
        assertEquals(0, metrics.getMinResponseTime());
        assertEquals(0, metrics.getMaxResponseTime());
        
        assertEquals(0, metrics.getCacheHits());
        assertEquals(0, metrics.getCacheMisses());
        assertEquals(0, metrics.getCacheEvictions());
        
        assertEquals(0, metrics.getConnectionAttempts());
        assertEquals(0, metrics.getSuccessfulConnections());
        assertEquals(0, metrics.getConnectionFailures());
        
        assertEquals(0, metrics.getBytesRead());
        assertEquals(0, metrics.getBytesWritten());
        assertEquals(0, metrics.getRecordsProcessed());
        
        // Verify reset time is updated
        assertTrue(metrics.getLastResetTime().isAfter(beforeReset));
        
        // Verify calculated metrics are also reset
        assertEquals(0.0, metrics.getSuccessRate(), 0.001);
        assertEquals(0.0, metrics.getErrorRate(), 0.001);
        assertEquals(0.0, metrics.getAverageResponseTime(), 0.001);
        assertEquals(0.0, metrics.getCacheHitRate(), 0.001);
        assertEquals(0.0, metrics.getConnectionSuccessRate(), 0.001);
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    @DisplayName("Should handle concurrent request recording")
    void testConcurrentRequestRecording() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (threadId % 2 == 0) {
                            metrics.recordSuccessfulRequest(100 + j);
                        } else {
                            metrics.recordFailedRequest(200 + j);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(threadCount * operationsPerThread, metrics.getTotalRequests());
        assertEquals(5 * operationsPerThread, metrics.getSuccessfulRequests());
        assertEquals(5 * operationsPerThread, metrics.getFailedRequests());
        assertEquals(50.0, metrics.getSuccessRate(), 0.001);
        assertEquals(50.0, metrics.getErrorRate(), 0.001);
    }

    @Test
    @DisplayName("Should handle concurrent cache operations")
    void testConcurrentCacheOperations() throws InterruptedException {
        final int threadCount = 5;
        final int operationsPerThread = 200;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        switch (threadId % 3) {
                            case 0:
                                metrics.recordCacheHit();
                                break;
                            case 1:
                                metrics.recordCacheMiss();
                                break;
                            case 2:
                                metrics.recordCacheEviction();
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

        // Verify totals (some threads do hits, some misses, some evictions)
        long expectedHits = 2 * operationsPerThread; // threads 0 and 3
        long expectedMisses = 2 * operationsPerThread; // threads 1 and 4
        long expectedEvictions = 1 * operationsPerThread; // thread 2

        assertEquals(expectedHits, metrics.getCacheHits());
        assertEquals(expectedMisses, metrics.getCacheMisses());
        assertEquals(expectedEvictions, metrics.getCacheEvictions());
        assertEquals(50.0, metrics.getCacheHitRate(), 0.001);
    }

    @Test
    @DisplayName("Should handle concurrent data volume operations")
    void testConcurrentDataVolumeOperations() throws InterruptedException {
        final int threadCount = 3;
        final int operationsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        switch (threadId) {
                            case 0:
                                metrics.recordBytesRead(1024);
                                break;
                            case 1:
                                metrics.recordBytesWritten(512);
                                break;
                            case 2:
                                metrics.recordRecordsProcessed(10);
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

        assertEquals(1024L * operationsPerThread, metrics.getBytesRead());
        assertEquals(512L * operationsPerThread, metrics.getBytesWritten());
        assertEquals(10L * operationsPerThread, metrics.getRecordsProcessed());
    }

    @Test
    @DisplayName("Should handle concurrent reset operations")
    void testConcurrentResetOperations() throws InterruptedException {
        // Pre-populate some metrics
        metrics.recordSuccessfulRequest(100);
        metrics.recordCacheHit();
        metrics.recordBytesRead(1024);

        final int threadCount = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    metrics.reset();
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        // After concurrent resets, all metrics should be zero
        assertEquals(0, metrics.getTotalRequests());
        assertEquals(0, metrics.getCacheHits());
        assertEquals(0, metrics.getBytesRead());
        assertNotNull(metrics.getLastResetTime());
    }

    // ========================================
    // Edge Cases and Boundary Conditions Tests
    // ========================================

    @Test
    @DisplayName("Should handle zero response times")
    void testZeroResponseTimes() {
        metrics.recordSuccessfulRequest(0);
        metrics.recordFailedRequest(0);

        assertEquals(2, metrics.getTotalRequests());
        assertEquals(0, metrics.getTotalResponseTime());
        assertEquals(0, metrics.getMinResponseTime());
        assertEquals(0, metrics.getMaxResponseTime());
        assertEquals(0.0, metrics.getAverageResponseTime(), 0.001);
    }

    @Test
    @DisplayName("Should handle very large response times")
    void testLargeResponseTimes() {
        long largeTime = Long.MAX_VALUE / 2;
        metrics.recordSuccessfulRequest(largeTime);

        assertEquals(1, metrics.getTotalRequests());
        assertEquals(largeTime, metrics.getTotalResponseTime());
        assertEquals(largeTime, metrics.getMinResponseTime());
        assertEquals(largeTime, metrics.getMaxResponseTime());
        assertEquals((double) largeTime, metrics.getAverageResponseTime(), 0.001);
    }

    @Test
    @DisplayName("Should handle negative values gracefully")
    void testNegativeValues() {
        // The implementation should handle negative values
        // (though they may not make logical sense)
        metrics.recordBytesRead(-100);
        metrics.recordBytesWritten(-50);
        metrics.recordRecordsProcessed(-10);

        assertEquals(-100, metrics.getBytesRead());
        assertEquals(-50, metrics.getBytesWritten());
        assertEquals(-10, metrics.getRecordsProcessed());
    }

    @Test
    @DisplayName("Should maintain min/max response times correctly with mixed values")
    void testMinMaxResponseTimesWithMixedValues() {
        metrics.recordSuccessfulRequest(500);
        metrics.recordFailedRequest(100);
        metrics.recordSuccessfulRequest(1000);
        metrics.recordFailedRequest(50);
        metrics.recordSuccessfulRequest(750);

        assertEquals(50, metrics.getMinResponseTime());
        assertEquals(1000, metrics.getMaxResponseTime());
        assertEquals(480.0, metrics.getAverageResponseTime(), 0.001); // (500+100+1000+50+750)/5
    }

    // ========================================
    // String Representation Tests
    // ========================================

    @Test
    @DisplayName("Should have meaningful toString representation")
    void testToString() {
        metrics.recordSuccessfulRequest(100);
        metrics.recordSuccessfulRequest(200);
        metrics.recordFailedRequest(150);
        metrics.recordCacheHit();
        metrics.recordCacheHit();
        metrics.recordCacheMiss();
        metrics.recordRecordsProcessed(50);

        String toString = metrics.toString();

        assertTrue(toString.contains("DataSourceMetrics"));
        assertTrue(toString.contains("totalRequests=3"));
        assertTrue(toString.contains("successRate=66.67%"));
        assertTrue(toString.contains("avgResponseTime=150.00ms"));
        assertTrue(toString.contains("cacheHitRate=66.67%"));
        assertTrue(toString.contains("recordsProcessed=50"));
    }

    @Test
    @DisplayName("Should handle toString with zero values")
    void testToStringWithZeroValues() {
        String toString = metrics.toString();

        assertTrue(toString.contains("DataSourceMetrics"));
        assertTrue(toString.contains("totalRequests=0"));
        assertTrue(toString.contains("successRate=0.00%"));
        assertTrue(toString.contains("avgResponseTime=0.00ms"));
        assertTrue(toString.contains("cacheHitRate=0.00%"));
        assertTrue(toString.contains("recordsProcessed=0"));
    }

    // ========================================
    // Integration and Realistic Scenario Tests
    // ========================================

    @Test
    @DisplayName("Should handle realistic data source usage scenario")
    void testRealisticScenario() {
        // Simulate a realistic data source usage pattern

        // Initial connection attempts
        metrics.recordConnectionAttempt();
        metrics.recordSuccessfulConnection();

        // Process some successful requests
        for (int i = 0; i < 100; i++) {
            metrics.recordSuccessfulRequest(50 + (i % 50)); // 50-99ms response times
            if (i % 3 == 0) {
                metrics.recordCacheHit();
            } else {
                metrics.recordCacheMiss();
            }
            metrics.recordBytesRead(1024 + (i * 10));
            metrics.recordRecordsProcessed(1);
        }

        // Some failed requests
        for (int i = 0; i < 10; i++) {
            metrics.recordFailedRequest(200 + (i * 10)); // 200-290ms response times
        }

        // Connection issues
        metrics.recordConnectionAttempt();
        metrics.recordConnectionFailure();

        // Verify realistic metrics
        assertEquals(110, metrics.getTotalRequests());
        assertEquals(100, metrics.getSuccessfulRequests());
        assertEquals(10, metrics.getFailedRequests());
        assertEquals(90.91, metrics.getSuccessRate(), 0.01);
        assertEquals(9.09, metrics.getErrorRate(), 0.01);

        // Cache hits: i % 3 == 0 for i = 0 to 99, so hits at 0, 3, 6, 9, ..., 99
        // That's 34 hits (0, 3, 6, ..., 99 = 34 values)
        assertEquals(34, metrics.getCacheHits());
        assertEquals(66, metrics.getCacheMisses());
        assertEquals(34.0, metrics.getCacheHitRate(), 0.01); // 34/(34+66) = 34/100 = 34%

        assertEquals(2, metrics.getConnectionAttempts());
        assertEquals(1, metrics.getSuccessfulConnections());
        assertEquals(1, metrics.getConnectionFailures());
        assertEquals(50.0, metrics.getConnectionSuccessRate(), 0.01);

        assertEquals(100, metrics.getRecordsProcessed());
        assertTrue(metrics.getBytesRead() > 100000); // Should be substantial

        assertNotNull(metrics.getLastRequestTime());
        assertTrue(metrics.getAverageResponseTime() > 0);
        assertTrue(metrics.getMinResponseTime() >= 50);
        assertTrue(metrics.getMaxResponseTime() <= 290);
    }
}
