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
package dev.mars.apex.core.config.datasink;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for BatchConfig - configuration for batch processing in data sinks.
 */
@ExtendWith(ColoredTestOutputExtension.class)
@DisplayName("BatchConfig Tests")
class BatchConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(BatchConfigTest.class);

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Should create with sensible defaults")
        void shouldCreateWithSensibleDefaults() {
            BatchConfig config = new BatchConfig();
            
            assertTrue(config.getEnabled(), "Batching should be enabled by default");
            assertEquals("size-based", config.getMode());
            assertEquals(100, config.getBatchSize());
            assertEquals(1000, config.getMaxBatchSize());
            assertEquals(1, config.getMinBatchSize());
            assertEquals(5000L, config.getBatchTimeoutMs());
            
            logger.info("[OK] Default values are sensible");
        }

        @Test
        @DisplayName("Should return correct BatchMode enum from code")
        void shouldReturnCorrectBatchModeFromCode() {
            assertEquals(BatchConfig.BatchMode.SIZE_BASED, 
                        BatchConfig.BatchMode.fromCode("size-based"));
            assertEquals(BatchConfig.BatchMode.TIME_BASED, 
                        BatchConfig.BatchMode.fromCode("time-based"));
            assertEquals(BatchConfig.BatchMode.HYBRID, 
                        BatchConfig.BatchMode.fromCode("hybrid"));
            assertEquals(BatchConfig.BatchMode.MANUAL, 
                        BatchConfig.BatchMode.fromCode("manual"));
            assertEquals(BatchConfig.BatchMode.DISABLED, 
                        BatchConfig.BatchMode.fromCode("disabled"));
            
            logger.info("[OK] BatchMode enum resolves correctly from codes");
        }

        @Test
        @DisplayName("Should default to size-based for unknown modes")
        void shouldDefaultToSizeBasedForUnknown() {
            assertEquals(BatchConfig.BatchMode.SIZE_BASED, 
                        BatchConfig.BatchMode.fromCode("unknown-mode"));
            assertEquals(BatchConfig.BatchMode.SIZE_BASED, 
                        BatchConfig.BatchMode.fromCode(null));
            
            logger.info("[OK] Unknown modes default to SIZE_BASED");
        }
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create with basic parameters")
        void shouldCreateWithBasicParameters() {
            BatchConfig config = new BatchConfig(true, 500, 10000L);
            
            assertTrue(config.getEnabled());
            assertEquals(500, config.getBatchSize());
            assertEquals(10000L, config.getBatchTimeoutMs());
            
            logger.info("[OK] Basic parameter constructor works");
        }

        @Test
        @DisplayName("Should create disabled batch config")
        void shouldCreateDisabledBatchConfig() {
            BatchConfig config = new BatchConfig(false, 0, 0L);
            
            assertFalse(config.getEnabled());
            assertEquals(0, config.getBatchSize());
            
            logger.info("[OK] Disabled batch config created");
        }
    }

    @Nested
    @DisplayName("Batch Size Configuration Tests")
    class BatchSizeConfigurationTests {

        @Test
        @DisplayName("Should set and get batch size properties")
        void shouldSetAndGetBatchSizeProperties() {
            BatchConfig config = new BatchConfig();
            
            config.setBatchSize(250);
            assertEquals(250, config.getBatchSize());
            
            config.setMaxBatchSize(5000);
            assertEquals(5000, config.getMaxBatchSize());
            
            config.setMinBatchSize(10);
            assertEquals(10, config.getMinBatchSize());
            
            logger.info("[OK] Batch size properties set correctly");
        }

        @Test
        @DisplayName("Should set batch mode via string and enum accessor")
        void shouldSetBatchModeViaStringAndEnumAccessor() {
            BatchConfig config = new BatchConfig();
            
            config.setMode("hybrid");
            assertEquals("hybrid", config.getMode());
            assertEquals(BatchConfig.BatchMode.HYBRID, config.getBatchMode());
            
            config.setMode("time-based");
            assertEquals("time-based", config.getMode());
            assertEquals(BatchConfig.BatchMode.TIME_BASED, config.getBatchMode());
            
            logger.info("[OK] Batch mode string and enum accessor work correctly");
        }
    }

    @Nested
    @DisplayName("Time-Based Batching Tests")
    class TimeBasedBatchingTests {

        @Test
        @DisplayName("Should configure time-based batching")
        void shouldConfigureTimeBasedBatching() {
            BatchConfig config = new BatchConfig();
            
            config.setBatchTimeoutMs(15000L);
            assertEquals(15000L, config.getBatchTimeoutMs());
            
            config.setMaxBatchTimeoutMs(60000L);
            assertEquals(60000L, config.getMaxBatchTimeoutMs());
            
            config.setFlushIntervalMs(2000L);
            assertEquals(2000L, config.getFlushIntervalMs());
            
            logger.info("[OK] Time-based batching configured correctly");
        }

        @Test
        @DisplayName("Should have sensible default timeouts")
        void shouldHaveSensibleDefaultTimeouts() {
            BatchConfig config = new BatchConfig();
            
            assertEquals(5000L, config.getBatchTimeoutMs()); // 5 seconds
            assertEquals(30000L, config.getMaxBatchTimeoutMs()); // 30 seconds
            assertEquals(1000L, config.getFlushIntervalMs()); // 1 second
            
            logger.info("[OK] Default timeouts are sensible");
        }
    }

    @Nested
    @DisplayName("Transaction Configuration Tests")
    class TransactionConfigurationTests {

        @Test
        @DisplayName("Should configure transaction settings")
        void shouldConfigureTransactionSettings() {
            BatchConfig config = new BatchConfig();
            
            config.setTransactionMode("per-record");
            assertEquals("per-record", config.getTransactionMode());
            assertEquals(BatchConfig.TransactionMode.PER_RECORD, config.getTransactionModeEnum());
            
            config.setTransactionTimeoutMs(60000L);
            assertEquals(60000L, config.getTransactionTimeoutMs());
            
            config.setIsolationLevel("SERIALIZABLE");
            assertEquals("SERIALIZABLE", config.getIsolationLevel());
            
            logger.info("[OK] Transaction settings configured correctly");
        }

        @Test
        @DisplayName("Should have correct default transaction settings")
        void shouldHaveCorrectDefaultTransactionSettings() {
            BatchConfig config = new BatchConfig();
            
            assertEquals("per-batch", config.getTransactionMode());
            assertEquals(BatchConfig.TransactionMode.PER_BATCH, config.getTransactionModeEnum());
            assertEquals(30000L, config.getTransactionTimeoutMs());
            assertEquals("READ_COMMITTED", config.getIsolationLevel());
            
            logger.info("[OK] Default transaction settings are correct");
        }

        @Test
        @DisplayName("Should resolve TransactionMode enum correctly")
        void shouldResolveTransactionModeEnumCorrectly() {
            assertEquals(BatchConfig.TransactionMode.NONE, 
                        BatchConfig.TransactionMode.fromCode("none"));
            assertEquals(BatchConfig.TransactionMode.PER_BATCH, 
                        BatchConfig.TransactionMode.fromCode("per-batch"));
            assertEquals(BatchConfig.TransactionMode.PER_RECORD, 
                        BatchConfig.TransactionMode.fromCode("per-record"));
            assertEquals(BatchConfig.TransactionMode.GLOBAL, 
                        BatchConfig.TransactionMode.fromCode("global"));
            assertEquals(BatchConfig.TransactionMode.PER_BATCH, 
                        BatchConfig.TransactionMode.fromCode(null));
            assertEquals(BatchConfig.TransactionMode.PER_BATCH, 
                        BatchConfig.TransactionMode.fromCode("unknown"));
            
            logger.info("[OK] TransactionMode enum resolves correctly");
        }
    }

    @Nested
    @DisplayName("Memory Management Tests")
    class MemoryManagementTests {

        @Test
        @DisplayName("Should configure memory management")
        void shouldConfigureMemoryManagement() {
            BatchConfig config = new BatchConfig();
            
            config.setMaxMemoryUsageMB(256L);
            assertEquals(256L, config.getMaxMemoryUsageMB());
            
            config.setEnableMemoryMonitoring(false);
            assertFalse(config.getEnableMemoryMonitoring());
            
            config.setMemoryThresholdPercent(0.9);
            assertEquals(0.9, config.getMemoryThresholdPercent());
            
            logger.info("[OK] Memory management configured correctly");
        }

        @Test
        @DisplayName("Should have sensible default memory settings")
        void shouldHaveSensibleDefaultMemorySettings() {
            BatchConfig config = new BatchConfig();
            
            assertEquals(100L, config.getMaxMemoryUsageMB());
            assertTrue(config.getEnableMemoryMonitoring());
            assertEquals(0.8, config.getMemoryThresholdPercent());
            
            logger.info("[OK] Default memory settings are sensible");
        }
    }

    @Nested
    @DisplayName("Performance Tuning Tests")
    class PerformanceTuningTests {

        @Test
        @DisplayName("Should configure performance tuning")
        void shouldConfigurePerformanceTuning() {
            BatchConfig config = new BatchConfig();
            
            config.setParallelBatches(4);
            assertEquals(4, config.getParallelBatches());
            
            config.setEnableCompression(true);
            assertTrue(config.getEnableCompression());
            
            config.setCompressionAlgorithm("lz4");
            assertEquals("lz4", config.getCompressionAlgorithm());
            
            logger.info("[OK] Performance tuning configured correctly");
        }

        @Test
        @DisplayName("Should have correct default performance settings")
        void shouldHaveCorrectDefaultPerformanceSettings() {
            BatchConfig config = new BatchConfig();
            
            assertEquals(1, config.getParallelBatches());
            assertFalse(config.getEnableCompression());
            assertEquals("gzip", config.getCompressionAlgorithm());
            
            logger.info("[OK] Default performance settings are correct");
        }
    }

    @Nested
    @DisplayName("Buffer Management Tests")
    class BufferManagementTests {

        @Test
        @DisplayName("Should configure buffer settings")
        void shouldConfigureBufferSettings() {
            BatchConfig config = new BatchConfig();
            
            config.setBufferSize(5000);
            assertEquals(5000, config.getBufferSize());
            
            config.setEnableBuffering(false);
            assertFalse(config.getEnableBuffering());
            
            config.setBufferFlushIntervalMs(5000L);
            assertEquals(5000L, config.getBufferFlushIntervalMs());
            
            logger.info("[OK] Buffer settings configured correctly");
        }

        @Test
        @DisplayName("Should have correct default buffer settings")
        void shouldHaveCorrectDefaultBufferSettings() {
            BatchConfig config = new BatchConfig();
            
            assertEquals(1000, config.getBufferSize());
            assertTrue(config.getEnableBuffering());
            assertEquals(2000L, config.getBufferFlushIntervalMs());
            
            logger.info("[OK] Default buffer settings are correct");
        }
    }

    @Nested
    @DisplayName("Ordering Tests")
    class OrderingTests {

        @Test
        @DisplayName("Should configure ordering settings")
        void shouldConfigureOrderingSettings() {
            BatchConfig config = new BatchConfig();
            
            config.setMaintainOrder(false);
            assertFalse(config.getMaintainOrder());
            
            config.setOrderingField("timestamp");
            assertEquals("timestamp", config.getOrderingField());
            
            config.setOrderingDirection("DESC");
            assertEquals("DESC", config.getOrderingDirection());
            
            logger.info("[OK] Ordering settings configured correctly");
        }

        @Test
        @DisplayName("Should maintain order by default")
        void shouldMaintainOrderByDefault() {
            BatchConfig config = new BatchConfig();
            
            assertTrue(config.getMaintainOrder());
            assertEquals("ASC", config.getOrderingDirection());
            
            logger.info("[OK] Default ordering settings are correct");
        }
    }

    @Nested
    @DisplayName("Metrics and Monitoring Tests")
    class MetricsAndMonitoringTests {

        @Test
        @DisplayName("Should configure metrics settings")
        void shouldConfigureMetricsSettings() {
            BatchConfig config = new BatchConfig();
            
            config.setEnableMetrics(false);
            assertFalse(config.getEnableMetrics());
            
            config.setLogBatchStatistics(true);
            assertTrue(config.getLogBatchStatistics());
            
            config.setMetricsReportingIntervalMs(30000);
            assertEquals(30000, config.getMetricsReportingIntervalMs());
            
            logger.info("[OK] Metrics settings configured correctly");
        }

        @Test
        @DisplayName("Should have correct default metrics settings")
        void shouldHaveCorrectDefaultMetricsSettings() {
            BatchConfig config = new BatchConfig();
            
            assertTrue(config.getEnableMetrics());
            assertFalse(config.getLogBatchStatistics());
            assertEquals(10000, config.getMetricsReportingIntervalMs());
            
            logger.info("[OK] Default metrics settings are correct");
        }
    }

    @Nested
    @DisplayName("BatchMode Enum Tests")
    class BatchModeEnumTests {

        @Test
        @DisplayName("Should have correct codes and descriptions")
        void shouldHaveCorrectCodesAndDescriptions() {
            assertEquals("size-based", BatchConfig.BatchMode.SIZE_BASED.getCode());
            assertEquals("Batch based on number of records", BatchConfig.BatchMode.SIZE_BASED.getDescription());
            
            assertEquals("time-based", BatchConfig.BatchMode.TIME_BASED.getCode());
            assertEquals("Batch based on time intervals", BatchConfig.BatchMode.TIME_BASED.getDescription());
            
            assertEquals("hybrid", BatchConfig.BatchMode.HYBRID.getCode());
            assertEquals("Batch based on size or time, whichever comes first", BatchConfig.BatchMode.HYBRID.getDescription());
            
            assertEquals("manual", BatchConfig.BatchMode.MANUAL.getCode());
            assertEquals("Manual batch control", BatchConfig.BatchMode.MANUAL.getDescription());
            
            assertEquals("disabled", BatchConfig.BatchMode.DISABLED.getCode());
            assertEquals("No batching - process records individually", BatchConfig.BatchMode.DISABLED.getDescription());
            
            logger.info("[OK] All BatchMode codes and descriptions are correct");
        }

        @Test
        @DisplayName("Should be case insensitive for code lookup")
        void shouldBeCaseInsensitiveForCodeLookup() {
            assertEquals(BatchConfig.BatchMode.SIZE_BASED, 
                        BatchConfig.BatchMode.fromCode("SIZE-BASED"));
            assertEquals(BatchConfig.BatchMode.TIME_BASED, 
                        BatchConfig.BatchMode.fromCode("Time-Based"));
            assertEquals(BatchConfig.BatchMode.HYBRID, 
                        BatchConfig.BatchMode.fromCode("HYBRID"));
            
            logger.info("[OK] BatchMode code lookup is case insensitive");
        }
    }

    @Nested
    @DisplayName("TransactionMode Enum Tests")
    class TransactionModeEnumTests {

        @Test
        @DisplayName("Should have correct codes and descriptions")
        void shouldHaveCorrectCodesAndDescriptions() {
            assertEquals("none", BatchConfig.TransactionMode.NONE.getCode());
            assertEquals("No transaction management", BatchConfig.TransactionMode.NONE.getDescription());
            
            assertEquals("per-batch", BatchConfig.TransactionMode.PER_BATCH.getCode());
            assertEquals("One transaction per batch", BatchConfig.TransactionMode.PER_BATCH.getDescription());
            
            assertEquals("per-record", BatchConfig.TransactionMode.PER_RECORD.getCode());
            assertEquals("One transaction per record", BatchConfig.TransactionMode.PER_RECORD.getDescription());
            
            assertEquals("global", BatchConfig.TransactionMode.GLOBAL.getCode());
            assertEquals("Single transaction for all batches", BatchConfig.TransactionMode.GLOBAL.getDescription());
            
            logger.info("[OK] All TransactionMode codes and descriptions are correct");
        }
    }
}
