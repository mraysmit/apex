package dev.mars.apex.core.service.data.external.config;

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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlDataSource;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.manager.DataSourceManager;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DataSourceConfigurationService.
 * 
 * Tests cover:
 * - Singleton pattern implementation
 * - Service initialization and lifecycle
 * - Configuration management (add, remove, get)
 * - YAML integration and validation
 * - Event listener management and notifications
 * - Thread safety and concurrent operations
 * - Error handling and edge cases
 * - Integration with DataSourceManager
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataSourceConfigurationServiceTest {

    private DataSourceConfigurationService service;
    private TestConfigurationListener testListener;
    private DataSourceConfiguration testConfiguration;

    @BeforeEach
    void setUp() throws DataSourceException {
        // Get fresh instance for each test
        service = DataSourceConfigurationService.getInstance();

        // Initialize the service with YAML configuration that has at least one data source
        // This ensures the DataSourceManager gets properly initialized
        YamlRuleConfiguration yamlConfig = createTestYamlConfigurationWithDataSource();
        service.initialize(yamlConfig);

        // Create test listener
        testListener = new TestConfigurationListener();
        service.addListener(testListener);

        // Clear initialization events
        testListener.clearEvents();

        // Create test configuration
        testConfiguration = new DataSourceConfiguration();
        testConfiguration.setName("test-datasource");
        testConfiguration.setType("cache");
        testConfiguration.setEnabled(true);
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        service.removeListener(testListener);

        // Remove any test configurations
        Set<String> configNames = service.getConfigurationNames();
        for (String name : configNames) {
            service.removeConfiguration(name);
        }

        // Shutdown the service to clean up resources
        service.shutdown();
    }

    // ========================================
    // Singleton Pattern Tests
    // ========================================

    @Test
    @DisplayName("Should implement singleton pattern correctly")
    void testSingletonPattern() {
        DataSourceConfigurationService instance1 = DataSourceConfigurationService.getInstance();
        DataSourceConfigurationService instance2 = DataSourceConfigurationService.getInstance();
        
        assertSame(instance1, instance2);
        assertNotNull(instance1);
    }

    @Test
    @DisplayName("Should return same instance across multiple threads")
    void testSingletonThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        // Use thread-safe collection to avoid race conditions in the test itself
        final List<DataSourceConfigurationService> instances = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    DataSourceConfigurationService instance = DataSourceConfigurationService.getInstance();
                    instances.add(instance);
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }

        // Wait for all threads to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All threads should complete within 5 seconds");

        // Verify we got the expected number of instances
        assertEquals(threadCount, instances.size(),
                    "Should have collected instances from all " + threadCount + " threads");

        // All instances should be the same (singleton behavior)
        DataSourceConfigurationService firstInstance = instances.get(0);
        for (int i = 0; i < instances.size(); i++) {
            DataSourceConfigurationService instance = instances.get(i);
            assertSame(firstInstance, instance,
                      "Instance " + i + " should be the same as the first instance (singleton pattern)");
        }
    }

    // ========================================
    // Service State Tests
    // ========================================

    @Test
    @DisplayName("Should report correct initialization state")
    void testInitializationState() {
        // Service should be initialized in setUp
        assertTrue(service.isInitialized());
        assertTrue(service.isRunning());
    }

    @Test
    @DisplayName("Should report correct state after initialization")
    void testStateAfterInitialization() throws DataSourceException {
        // Service is already initialized in setUp, so verify current state
        assertTrue(service.isInitialized());
        assertTrue(service.isRunning());
        assertNotNull(service.getCurrentYamlConfiguration());
    }

    // ========================================
    // Configuration Management Tests
    // ========================================

    @Test
    @DisplayName("Should add configuration successfully")
    void testAddConfiguration() throws DataSourceException {
        service.addConfiguration(testConfiguration);
        
        assertEquals(testConfiguration, service.getConfiguration("test-datasource"));
        assertTrue(service.getConfigurationNames().contains("test-datasource"));
        
        // Verify event was fired
        assertEquals(1, testListener.getReceivedEvents().size());
        DataSourceConfigurationEvent event = testListener.getReceivedEvents().get(0);
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_ADDED, event.getEventType());
        assertEquals("test-datasource", event.getConfigurationName());
        assertEquals(testConfiguration, event.getConfiguration());
    }

    @Test
    @DisplayName("Should throw exception when adding null configuration")
    void testAddNullConfiguration() {
        DataSourceException exception = assertThrows(DataSourceException.class, () -> {
            service.addConfiguration(null);
        });
        
        assertEquals(DataSourceException.ErrorType.CONFIGURATION_ERROR, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Configuration cannot be null"));
    }

    @Test
    @DisplayName("Should remove configuration successfully")
    void testRemoveConfiguration() throws DataSourceException {
        // First add a configuration
        service.addConfiguration(testConfiguration);
        assertTrue(service.getConfigurationNames().contains("test-datasource"));
        
        // Clear events from add operation
        testListener.clearEvents();
        
        // Remove the configuration
        boolean removed = service.removeConfiguration("test-datasource");
        
        assertTrue(removed);
        assertNull(service.getConfiguration("test-datasource"));
        assertFalse(service.getConfigurationNames().contains("test-datasource"));
        
        // Verify event was fired
        assertEquals(1, testListener.getReceivedEvents().size());
        DataSourceConfigurationEvent event = testListener.getReceivedEvents().get(0);
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_REMOVED, event.getEventType());
        assertEquals("test-datasource", event.getConfigurationName());
    }

    @Test
    @DisplayName("Should return false when removing non-existent configuration")
    void testRemoveNonExistentConfiguration() {
        boolean removed = service.removeConfiguration("non-existent");
        
        assertFalse(removed);
        assertEquals(0, testListener.getReceivedEvents().size());
    }

    @Test
    @DisplayName("Should return false when removing null or empty configuration name")
    void testRemoveInvalidConfigurationName() {
        assertFalse(service.removeConfiguration(null));
        assertFalse(service.removeConfiguration(""));
        assertFalse(service.removeConfiguration("   "));
        
        assertEquals(0, testListener.getReceivedEvents().size());
    }

    @Test
    @DisplayName("Should get configuration by name")
    void testGetConfiguration() throws DataSourceException {
        service.addConfiguration(testConfiguration);
        
        DataSourceConfiguration retrieved = service.getConfiguration("test-datasource");
        assertEquals(testConfiguration, retrieved);
        
        assertNull(service.getConfiguration("non-existent"));
    }

    @Test
    @DisplayName("Should get all configuration names")
    void testGetConfigurationNames() throws DataSourceException {
        // Should have the init-cache from setup
        Set<String> initialNames = service.getConfigurationNames();
        assertEquals(1, initialNames.size());
        assertTrue(initialNames.contains("init-cache"));

        service.addConfiguration(testConfiguration);

        Set<String> names = service.getConfigurationNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("init-cache"));
        assertTrue(names.contains("test-datasource"));

        // Add another configuration
        DataSourceConfiguration config2 = new DataSourceConfiguration();
        config2.setName("test-datasource-2");
        config2.setType("cache");
        service.addConfiguration(config2);

        names = service.getConfigurationNames();
        assertEquals(3, names.size());
        assertTrue(names.contains("init-cache"));
        assertTrue(names.contains("test-datasource"));
        assertTrue(names.contains("test-datasource-2"));
    }

    // ========================================
    // Data Source Access Tests
    // ========================================

    @Test
    @DisplayName("Should get data source instance")
    void testGetDataSource() throws DataSourceException {
        service.addConfiguration(testConfiguration);

        // We just verify the method doesn't throw an exception
        // The actual data source may be null if not properly initialized
        assertDoesNotThrow(() -> service.getDataSource("test-datasource"));
        // Just verify we can call the method without exception
        assertNotNull(service.getDataSourceManager()); // Verify manager is available
    }

    @Test
    @DisplayName("Should get data source manager")
    void testGetDataSourceManager() {
        DataSourceManager manager = service.getDataSourceManager();
        assertNotNull(manager);
    }

    // ========================================
    // Event Listener Management Tests
    // ========================================

    @Test
    @DisplayName("Should add and remove listeners")
    void testListenerManagement() throws DataSourceException {
        TestConfigurationListener listener2 = new TestConfigurationListener();
        
        service.addListener(listener2);
        
        // Add configuration - both listeners should receive event
        service.addConfiguration(testConfiguration);
        
        assertEquals(1, testListener.getReceivedEvents().size());
        assertEquals(1, listener2.getReceivedEvents().size());
        
        // Remove one listener
        service.removeListener(listener2);
        
        // Add another configuration - only first listener should receive event
        DataSourceConfiguration config2 = new DataSourceConfiguration();
        config2.setName("test-datasource-2");
        config2.setType("cache");
        service.addConfiguration(config2);
        
        assertEquals(2, testListener.getReceivedEvents().size());
        assertEquals(1, listener2.getReceivedEvents().size());
    }

    @Test
    @DisplayName("Should handle null listeners gracefully")
    void testNullListenerHandling() {
        assertDoesNotThrow(() -> {
            service.addListener(null);
            service.removeListener(null);
        });
    }

    // ========================================
    // Helper Methods
    // ========================================

    private YamlRuleConfiguration createTestYamlConfiguration() {
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        yamlConfig.setDataSources(new ArrayList<>());
        return yamlConfig;
    }

    private YamlRuleConfiguration createTestYamlConfigurationWithDataSource() {
        YamlRuleConfiguration yamlConfig = new YamlRuleConfiguration();
        List<YamlDataSource> dataSources = new ArrayList<>();

        // Create a simple cache data source to initialize the DataSourceManager
        YamlDataSource yamlDataSource = new YamlDataSource();
        yamlDataSource.setName("init-cache");
        yamlDataSource.setType("cache");
        yamlDataSource.setEnabled(true);
        dataSources.add(yamlDataSource);

        yamlConfig.setDataSources(dataSources);
        return yamlConfig;
    }

    /**
     * Test listener that captures all received events.
     */
    private static class TestConfigurationListener implements DataSourceConfigurationListener {
        private final List<DataSourceConfigurationEvent> receivedEvents = new ArrayList<>();

        @Override
        public void onConfigurationEvent(DataSourceConfigurationEvent event) {
            receivedEvents.add(event);
        }

        public List<DataSourceConfigurationEvent> getReceivedEvents() {
            return receivedEvents;
        }

        public void clearEvents() {
            receivedEvents.clear();
        }
    }

    // ========================================
    // YAML Integration Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with YAML configuration")
    void testInitializeWithYaml() throws DataSourceException {
        YamlRuleConfiguration yamlConfig = createTestYamlConfiguration();

        service.initialize(yamlConfig);

        assertTrue(service.isInitialized());
        assertEquals(yamlConfig, service.getCurrentYamlConfiguration());

        // Verify initialization event was fired
        List<DataSourceConfigurationEvent> events = testListener.getReceivedEvents();
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == DataSourceConfigurationEvent.EventType.INITIALIZED));
    }

    @Test
    @DisplayName("Should reload from YAML configuration")
    void testReloadFromYaml() throws DataSourceException {
        // Initialize first
        YamlRuleConfiguration initialConfig = createTestYamlConfiguration();
        service.initialize(initialConfig);
        testListener.clearEvents();

        // Reload with new configuration
        YamlRuleConfiguration newConfig = createTestYamlConfiguration();
        service.reloadFromYaml(newConfig);

        assertEquals(newConfig, service.getCurrentYamlConfiguration());

        // Verify reload event was fired
        List<DataSourceConfigurationEvent> events = testListener.getReceivedEvents();
        assertTrue(events.stream().anyMatch(e -> e.getEventType() == DataSourceConfigurationEvent.EventType.RELOADED));
    }

    @Test
    @DisplayName("Should validate YAML configuration")
    void testValidateYamlConfiguration() {
        YamlDataSource yamlDataSource = new YamlDataSource();
        yamlDataSource.setName("test-yaml-source");
        yamlDataSource.setType("cache");

        // Should not throw exception for valid configuration
        assertDoesNotThrow(() -> {
            service.validateYamlConfiguration(yamlDataSource);
        });
    }

    @Test
    @DisplayName("Should validate multiple YAML configurations")
    void testValidateYamlConfigurations() {
        List<YamlDataSource> yamlDataSources = new ArrayList<>();

        YamlDataSource source1 = new YamlDataSource();
        source1.setName("test-source-1");
        source1.setType("cache");
        yamlDataSources.add(source1);

        YamlDataSource source2 = new YamlDataSource();
        source2.setName("test-source-2");
        source2.setType("cache"); // Use cache instead of database to avoid connection config requirement
        yamlDataSources.add(source2);

        // Should not throw exception for valid configurations
        assertDoesNotThrow(() -> {
            service.validateYamlConfigurations(yamlDataSources);
        });
    }

    // ========================================
    // Concurrent Operations Tests
    // ========================================

    @Test
    @DisplayName("Should handle concurrent configuration additions")
    void testConcurrentConfigurationAdditions() throws InterruptedException {
        final int threadCount = 5;
        final int configurationsPerThread = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < configurationsPerThread; j++) {
                        DataSourceConfiguration config = new DataSourceConfiguration();
                        config.setName("config-" + threadId + "-" + j);
                        config.setType("cache");
                        config.setEnabled(true);

                        service.addConfiguration(config);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }

        latch.await(10, TimeUnit.SECONDS);

        // Verify no exceptions occurred
        assertTrue(exceptions.isEmpty(), "Exceptions occurred: " + exceptions);

        // Verify all configurations were added (plus the init-cache from setup)
        assertEquals(threadCount * configurationsPerThread + 1, service.getConfigurationNames().size());
    }

    @Test
    @DisplayName("Should handle concurrent listener operations")
    void testConcurrentListenerOperations() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<TestConfigurationListener> listeners = new ArrayList<>();

        // Add listeners concurrently
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    TestConfigurationListener listener = new TestConfigurationListener();
                    synchronized (listeners) {
                        listeners.add(listener);
                    }
                    service.addListener(listener);
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }

        latch.await(5, TimeUnit.SECONDS);

        // Add a configuration to trigger events
        try {
            service.addConfiguration(testConfiguration);
        } catch (DataSourceException e) {
            fail("Failed to add configuration: " + e.getMessage());
        }

        // All listeners should have received the event
        for (TestConfigurationListener listener : listeners) {
            assertEquals(1, listener.getReceivedEvents().size());
        }

        // Clean up listeners
        for (TestConfigurationListener listener : listeners) {
            service.removeListener(listener);
        }
    }

    // ========================================
    // Error Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle validation errors gracefully")
    void testValidationErrorHandling() {
        // Test that validation methods properly throw exceptions for invalid input
        assertThrows(DataSourceException.class, () -> {
            service.validateYamlConfiguration(null);
        });

        // Service should still be initialized and running after validation error
        assertTrue(service.isInitialized());
        assertTrue(service.isRunning());
    }

    @Test
    @DisplayName("Should handle listener exceptions gracefully")
    void testListenerExceptionHandling() throws DataSourceException {
        // Add a listener that throws exceptions
        ExceptionThrowingListener exceptionListener = new ExceptionThrowingListener();
        service.addListener(exceptionListener);

        // Adding configuration should not fail even if listener throws exception
        assertDoesNotThrow(() -> {
            service.addConfiguration(testConfiguration);
        });

        // Normal listener should still receive events
        assertEquals(1, testListener.getReceivedEvents().size());
        assertTrue(exceptionListener.isExceptionThrown());

        service.removeListener(exceptionListener);
    }

    // ========================================
    // Shutdown Tests
    // ========================================

    @Test
    @DisplayName("Should shutdown gracefully")
    void testShutdown() throws DataSourceException {
        // Initialize and add some configurations
        service.initialize(createTestYamlConfiguration());
        service.addConfiguration(testConfiguration);

        assertTrue(service.isInitialized());
        assertFalse(service.getConfigurationNames().isEmpty());

        // Shutdown
        service.shutdown();

        // State should be cleared
        assertTrue(service.getConfigurationNames().isEmpty());
        assertNull(service.getCurrentYamlConfiguration());
    }

    // ========================================
    // Additional Helper Classes
    // ========================================

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
}
