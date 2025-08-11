package dev.mars.apex.core.service.data.external.config;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DataSourceConfigurationListener interface.
 * 
 * Tests cover:
 * - Interface contract and default method implementations
 * - Event handling and delegation patterns
 * - Custom listener implementations
 * - Thread safety and concurrent event handling
 * - Event filtering and processing
 * - Error handling in listener implementations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataSourceConfigurationListenerTest {

    private DataSourceConfiguration testConfiguration;
    private TestListener testListener;

    @BeforeEach
    void setUp() {
        testConfiguration = new DataSourceConfiguration();
        testConfiguration.setName("test-config");
        testConfiguration.setType("database");
        testConfiguration.setEnabled(true);
        
        testListener = new TestListener();
    }

    // ========================================
    // Default Method Implementation Tests
    // ========================================

    @Test
    @DisplayName("Should delegate service initialized events to onConfigurationEvent")
    void testServiceInitializedDelegation() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.initialized(5);
        
        testListener.onServiceInitialized(event);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(event, testListener.getReceivedEvents().get(0));
        assertEquals(1, testListener.getServiceInitializedCount());
    }

    @Test
    @DisplayName("Should delegate configuration added events to onConfigurationEvent")
    void testConfigurationAddedDelegation() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);
        
        testListener.onConfigurationAdded(event);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(event, testListener.getReceivedEvents().get(0));
        assertEquals(1, testListener.getConfigurationAddedCount());
    }

    @Test
    @DisplayName("Should delegate configuration removed events to onConfigurationEvent")
    void testConfigurationRemovedDelegation() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationRemoved("test", testConfiguration);
        
        testListener.onConfigurationRemoved(event);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(event, testListener.getReceivedEvents().get(0));
        assertEquals(1, testListener.getConfigurationRemovedCount());
    }

    @Test
    @DisplayName("Should delegate configuration updated events to onConfigurationEvent")
    void testConfigurationUpdatedDelegation() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationUpdated("test", testConfiguration);
        
        testListener.onConfigurationUpdated(event);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(event, testListener.getReceivedEvents().get(0));
        assertEquals(1, testListener.getConfigurationUpdatedCount());
    }

    @Test
    @DisplayName("Should delegate health restored events to onConfigurationEvent")
    void testHealthRestoredDelegation() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.healthRestored("test");
        
        testListener.onHealthRestored(event);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(event, testListener.getReceivedEvents().get(0));
        assertEquals(1, testListener.getHealthRestoredCount());
    }

    @Test
    @DisplayName("Should delegate health lost events to onConfigurationEvent")
    void testHealthLostDelegation() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.healthLost("test");
        
        testListener.onHealthLost(event);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(event, testListener.getReceivedEvents().get(0));
        assertEquals(1, testListener.getHealthLostCount());
    }

    @Test
    @DisplayName("Should delegate configurations reloaded events to onConfigurationEvent")
    void testConfigurationsReloadedDelegation() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.reloaded(3);
        
        testListener.onConfigurationsReloaded(event);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(event, testListener.getReceivedEvents().get(0));
        assertEquals(1, testListener.getConfigurationsReloadedCount());
    }

    // ========================================
    // Multiple Event Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle multiple events of different types")
    void testMultipleEventTypes() {
        DataSourceConfigurationEvent event1 = DataSourceConfigurationEvent.initialized(1);
        DataSourceConfigurationEvent event2 = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);
        DataSourceConfigurationEvent event3 = DataSourceConfigurationEvent.healthRestored("test");
        
        testListener.onServiceInitialized(event1);
        testListener.onConfigurationAdded(event2);
        testListener.onHealthRestored(event3);
        
        assertEquals(3, testListener.getReceivedEvents().size());
        assertEquals(event1, testListener.getReceivedEvents().get(0));
        assertEquals(event2, testListener.getReceivedEvents().get(1));
        assertEquals(event3, testListener.getReceivedEvents().get(2));
        
        assertEquals(1, testListener.getServiceInitializedCount());
        assertEquals(1, testListener.getConfigurationAddedCount());
        assertEquals(1, testListener.getHealthRestoredCount());
    }

    @Test
    @DisplayName("Should handle multiple events of the same type")
    void testMultipleEventsOfSameType() {
        DataSourceConfigurationEvent event1 = DataSourceConfigurationEvent.configurationAdded("config1", testConfiguration);
        DataSourceConfigurationEvent event2 = DataSourceConfigurationEvent.configurationAdded("config2", testConfiguration);
        DataSourceConfigurationEvent event3 = DataSourceConfigurationEvent.configurationAdded("config3", testConfiguration);
        
        testListener.onConfigurationAdded(event1);
        testListener.onConfigurationAdded(event2);
        testListener.onConfigurationAdded(event3);
        
        assertEquals(3, testListener.getReceivedEvents().size());
        assertEquals(3, testListener.getConfigurationAddedCount());
    }

    // ========================================
    // Custom Listener Implementation Tests
    // ========================================

    @Test
    @DisplayName("Should support custom listener implementations")
    void testCustomListenerImplementation() {
        CustomFilteringListener customListener = new CustomFilteringListener();
        
        // This listener only processes health events
        DataSourceConfigurationEvent healthEvent = DataSourceConfigurationEvent.healthRestored("test");
        DataSourceConfigurationEvent configEvent = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);
        
        customListener.onHealthRestored(healthEvent);
        customListener.onConfigurationAdded(configEvent);
        
        // Only health event should be processed
        assertEquals(1, customListener.getProcessedEvents().size());
        assertEquals(healthEvent, customListener.getProcessedEvents().get(0));
    }

    @Test
    @DisplayName("Should support listener with custom processing logic")
    void testCustomProcessingLogic() {
        StatisticsListener statsListener = new StatisticsListener();
        
        // Send various events
        statsListener.onServiceInitialized(DataSourceConfigurationEvent.initialized(5));
        statsListener.onConfigurationAdded(DataSourceConfigurationEvent.configurationAdded("config1", testConfiguration));
        statsListener.onConfigurationAdded(DataSourceConfigurationEvent.configurationAdded("config2", testConfiguration));
        statsListener.onConfigurationRemoved(DataSourceConfigurationEvent.configurationRemoved("config1", testConfiguration));
        statsListener.onHealthLost(DataSourceConfigurationEvent.healthLost("config2"));
        statsListener.onHealthRestored(DataSourceConfigurationEvent.healthRestored("config2"));
        
        // Verify statistics
        assertEquals(6, statsListener.getTotalEvents());
        assertEquals(1, statsListener.getLifecycleEvents());
        assertEquals(3, statsListener.getConfigurationChangeEvents());
        assertEquals(2, statsListener.getHealthEvents());
    }

    // ========================================
    // Thread Safety Tests
    // ========================================

    @Test
    @DisplayName("Should handle concurrent event processing")
    void testConcurrentEventProcessing() throws InterruptedException {
        ThreadSafeListener threadSafeListener = new ThreadSafeListener();
        final int threadCount = 10;
        final int eventsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < eventsPerThread; j++) {
                        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationAdded(
                            "config-" + threadId + "-" + j, testConfiguration);
                        threadSafeListener.onConfigurationAdded(event);
                    }
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }
        
        latch.await(10, TimeUnit.SECONDS);
        
        assertEquals(threadCount * eventsPerThread, threadSafeListener.getEventCount());
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle exceptions in listener implementations")
    void testExceptionHandling() {
        ExceptionThrowingListener exceptionListener = new ExceptionThrowingListener();

        // The default interface methods don't catch exceptions, so they will propagate
        assertThrows(RuntimeException.class, () -> {
            exceptionListener.onConfigurationAdded(
                DataSourceConfigurationEvent.configurationAdded("test", testConfiguration));
        });

        assertTrue(exceptionListener.isExceptionThrown());
    }

    @Test
    @DisplayName("Should handle null events gracefully")
    void testNullEventHandling() {
        NullSafeListener nullSafeListener = new NullSafeListener();
        
        // Should handle null events without throwing exceptions
        assertDoesNotThrow(() -> {
            nullSafeListener.onConfigurationEvent(null);
            nullSafeListener.onServiceInitialized(null);
            nullSafeListener.onConfigurationAdded(null);
            nullSafeListener.onConfigurationRemoved(null);
            nullSafeListener.onConfigurationUpdated(null);
            nullSafeListener.onHealthRestored(null);
            nullSafeListener.onHealthLost(null);
            nullSafeListener.onConfigurationsReloaded(null);
        });
        
        assertEquals(8, nullSafeListener.getNullEventCount());
    }

    // ========================================
    // Helper Test Listener Classes
    // ========================================

    /**
     * Test listener that tracks all received events and method call counts.
     */
    private static class TestListener implements DataSourceConfigurationListener {
        private final List<DataSourceConfigurationEvent> receivedEvents = new ArrayList<>();
        private int serviceInitializedCount = 0;
        private int configurationAddedCount = 0;
        private int configurationRemovedCount = 0;
        private int configurationUpdatedCount = 0;
        private int healthRestoredCount = 0;
        private int healthLostCount = 0;
        private int configurationsReloadedCount = 0;

        @Override
        public void onConfigurationEvent(DataSourceConfigurationEvent event) {
            receivedEvents.add(event);
        }

        @Override
        public void onServiceInitialized(DataSourceConfigurationEvent event) {
            serviceInitializedCount++;
            DataSourceConfigurationListener.super.onServiceInitialized(event);
        }

        @Override
        public void onConfigurationAdded(DataSourceConfigurationEvent event) {
            configurationAddedCount++;
            DataSourceConfigurationListener.super.onConfigurationAdded(event);
        }

        @Override
        public void onConfigurationRemoved(DataSourceConfigurationEvent event) {
            configurationRemovedCount++;
            DataSourceConfigurationListener.super.onConfigurationRemoved(event);
        }

        @Override
        public void onConfigurationUpdated(DataSourceConfigurationEvent event) {
            configurationUpdatedCount++;
            DataSourceConfigurationListener.super.onConfigurationUpdated(event);
        }

        @Override
        public void onHealthRestored(DataSourceConfigurationEvent event) {
            healthRestoredCount++;
            DataSourceConfigurationListener.super.onHealthRestored(event);
        }

        @Override
        public void onHealthLost(DataSourceConfigurationEvent event) {
            healthLostCount++;
            DataSourceConfigurationListener.super.onHealthLost(event);
        }

        @Override
        public void onConfigurationsReloaded(DataSourceConfigurationEvent event) {
            configurationsReloadedCount++;
            DataSourceConfigurationListener.super.onConfigurationsReloaded(event);
        }

        // Getters
        public List<DataSourceConfigurationEvent> getReceivedEvents() { return receivedEvents; }
        public int getServiceInitializedCount() { return serviceInitializedCount; }
        public int getConfigurationAddedCount() { return configurationAddedCount; }
        public int getConfigurationRemovedCount() { return configurationRemovedCount; }
        public int getConfigurationUpdatedCount() { return configurationUpdatedCount; }
        public int getHealthRestoredCount() { return healthRestoredCount; }
        public int getHealthLostCount() { return healthLostCount; }
        public int getConfigurationsReloadedCount() { return configurationsReloadedCount; }
    }

    /**
     * Custom listener that only processes health events.
     */
    private static class CustomFilteringListener implements DataSourceConfigurationListener {
        private final List<DataSourceConfigurationEvent> processedEvents = new ArrayList<>();

        @Override
        public void onConfigurationEvent(DataSourceConfigurationEvent event) {
            if (event != null && event.isHealthEvent()) {
                processedEvents.add(event);
            }
        }

        public List<DataSourceConfigurationEvent> getProcessedEvents() {
            return processedEvents;
        }
    }

    /**
     * Listener that collects statistics about different event types.
     */
    private static class StatisticsListener implements DataSourceConfigurationListener {
        private int totalEvents = 0;
        private int lifecycleEvents = 0;
        private int configurationChangeEvents = 0;
        private int healthEvents = 0;

        @Override
        public void onConfigurationEvent(DataSourceConfigurationEvent event) {
            if (event != null) {
                totalEvents++;
                if (event.isLifecycleEvent()) {
                    lifecycleEvents++;
                } else if (event.isConfigurationChangeEvent()) {
                    configurationChangeEvents++;
                } else if (event.isHealthEvent()) {
                    healthEvents++;
                }
            }
        }

        public int getTotalEvents() { return totalEvents; }
        public int getLifecycleEvents() { return lifecycleEvents; }
        public int getConfigurationChangeEvents() { return configurationChangeEvents; }
        public int getHealthEvents() { return healthEvents; }
    }

    /**
     * Thread-safe listener for concurrent testing.
     */
    private static class ThreadSafeListener implements DataSourceConfigurationListener {
        private volatile int eventCount = 0;

        @Override
        public synchronized void onConfigurationEvent(DataSourceConfigurationEvent event) {
            if (event != null) {
                eventCount++;
            }
        }

        public int getEventCount() {
            return eventCount;
        }
    }

    /**
     * Listener that throws exceptions to test error handling.
     */
    private static class ExceptionThrowingListener implements DataSourceConfigurationListener {
        private boolean exceptionThrown = false;

        @Override
        public void onConfigurationEvent(DataSourceConfigurationEvent event) {
            exceptionThrown = true;
            throw new RuntimeException("Test exception from listener");
        }

        public boolean isExceptionThrown() {
            return exceptionThrown;
        }
    }

    /**
     * Listener that safely handles null events.
     */
    private static class NullSafeListener implements DataSourceConfigurationListener {
        private int nullEventCount = 0;

        @Override
        public void onConfigurationEvent(DataSourceConfigurationEvent event) {
            if (event == null) {
                nullEventCount++;
            }
        }

        public int getNullEventCount() {
            return nullEventCount;
        }
    }
}
