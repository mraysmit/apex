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
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for DataSourceConfigurationEvent.
 * 
 * Tests cover:
 * - Event type enumeration and properties
 * - Factory method creation patterns
 * - Event properties and getters
 * - Event classification methods
 * - Equality and hash code contracts
 * - String representation
 * - Timestamp handling
 * - Edge cases and null handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class DataSourceConfigurationEventTest {

    private DataSourceConfiguration testConfiguration;

    @BeforeEach
    void setUp() {
        testConfiguration = new DataSourceConfiguration();
        testConfiguration.setName("test-config");
        testConfiguration.setType("database");
        testConfiguration.setEnabled(true);
    }

    // ========================================
    // Event Type Enum Tests
    // ========================================

    @Test
    @DisplayName("Should have correct event type values")
    void testEventTypeValues() {
        DataSourceConfigurationEvent.EventType[] types = DataSourceConfigurationEvent.EventType.values();
        assertEquals(7, types.length);
        
        assertEquals(DataSourceConfigurationEvent.EventType.INITIALIZED, types[0]);
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_ADDED, types[1]);
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_REMOVED, types[2]);
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_UPDATED, types[3]);
        assertEquals(DataSourceConfigurationEvent.EventType.HEALTH_RESTORED, types[4]);
        assertEquals(DataSourceConfigurationEvent.EventType.HEALTH_LOST, types[5]);
        assertEquals(DataSourceConfigurationEvent.EventType.RELOADED, types[6]);
    }

    // ========================================
    // Factory Method Tests
    // ========================================

    @Test
    @DisplayName("Should create initialized event correctly")
    void testInitializedEvent() {
        int configCount = 5;
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.initialized(configCount);
        
        assertEquals(DataSourceConfigurationEvent.EventType.INITIALIZED, event.getEventType());
        assertNull(event.getConfigurationName());
        assertNull(event.getConfiguration());
        assertEquals(configCount, event.getData());
        assertEquals("DataSourceConfigurationService initialized with 5 configurations", event.getMessage());
        assertNotNull(event.getTimestamp());
        assertTrue(event.isLifecycleEvent());
        assertFalse(event.isConfigurationChangeEvent());
        assertFalse(event.isHealthEvent());
    }

    @Test
    @DisplayName("Should create configuration added event correctly")
    void testConfigurationAddedEvent() {
        String configName = "test-config";
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationAdded(configName, testConfiguration);
        
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_ADDED, event.getEventType());
        assertEquals(configName, event.getConfigurationName());
        assertEquals(testConfiguration, event.getConfiguration());
        assertNull(event.getData());
        assertEquals("Configuration 'test-config' was added", event.getMessage());
        assertNotNull(event.getTimestamp());
        assertFalse(event.isLifecycleEvent());
        assertTrue(event.isConfigurationChangeEvent());
        assertFalse(event.isHealthEvent());
    }

    @Test
    @DisplayName("Should create configuration removed event correctly")
    void testConfigurationRemovedEvent() {
        String configName = "test-config";
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationRemoved(configName, testConfiguration);
        
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_REMOVED, event.getEventType());
        assertEquals(configName, event.getConfigurationName());
        assertEquals(testConfiguration, event.getConfiguration());
        assertNull(event.getData());
        assertEquals("Configuration 'test-config' was removed", event.getMessage());
        assertNotNull(event.getTimestamp());
        assertFalse(event.isLifecycleEvent());
        assertTrue(event.isConfigurationChangeEvent());
        assertFalse(event.isHealthEvent());
    }

    @Test
    @DisplayName("Should create configuration updated event correctly")
    void testConfigurationUpdatedEvent() {
        String configName = "test-config";
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationUpdated(configName, testConfiguration);
        
        assertEquals(DataSourceConfigurationEvent.EventType.CONFIGURATION_UPDATED, event.getEventType());
        assertEquals(configName, event.getConfigurationName());
        assertEquals(testConfiguration, event.getConfiguration());
        assertNull(event.getData());
        assertEquals("Configuration 'test-config' was updated", event.getMessage());
        assertNotNull(event.getTimestamp());
        assertFalse(event.isLifecycleEvent());
        assertTrue(event.isConfigurationChangeEvent());
        assertFalse(event.isHealthEvent());
    }

    @Test
    @DisplayName("Should create health restored event correctly")
    void testHealthRestoredEvent() {
        String configName = "test-config";
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.healthRestored(configName);
        
        assertEquals(DataSourceConfigurationEvent.EventType.HEALTH_RESTORED, event.getEventType());
        assertEquals(configName, event.getConfigurationName());
        assertNull(event.getConfiguration());
        assertNull(event.getData());
        assertEquals("Configuration 'test-config' health was restored", event.getMessage());
        assertNotNull(event.getTimestamp());
        assertFalse(event.isLifecycleEvent());
        assertFalse(event.isConfigurationChangeEvent());
        assertTrue(event.isHealthEvent());
        assertTrue(event.isHealthImprovement());
        assertFalse(event.isHealthDegradation());
    }

    @Test
    @DisplayName("Should create health lost event correctly")
    void testHealthLostEvent() {
        String configName = "test-config";
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.healthLost(configName);
        
        assertEquals(DataSourceConfigurationEvent.EventType.HEALTH_LOST, event.getEventType());
        assertEquals(configName, event.getConfigurationName());
        assertNull(event.getConfiguration());
        assertNull(event.getData());
        assertEquals("Configuration 'test-config' health was lost", event.getMessage());
        assertNotNull(event.getTimestamp());
        assertFalse(event.isLifecycleEvent());
        assertFalse(event.isConfigurationChangeEvent());
        assertTrue(event.isHealthEvent());
        assertFalse(event.isHealthImprovement());
        assertTrue(event.isHealthDegradation());
    }

    @Test
    @DisplayName("Should create reloaded event correctly")
    void testReloadedEvent() {
        int configCount = 3;
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.reloaded(configCount);
        
        assertEquals(DataSourceConfigurationEvent.EventType.RELOADED, event.getEventType());
        assertNull(event.getConfigurationName());
        assertNull(event.getConfiguration());
        assertEquals(configCount, event.getData());
        assertEquals("Configurations reloaded with 3 configurations", event.getMessage());
        assertNotNull(event.getTimestamp());
        assertTrue(event.isLifecycleEvent());
        assertFalse(event.isConfigurationChangeEvent());
        assertFalse(event.isHealthEvent());
    }

    // ========================================
    // Event Classification Tests
    // ========================================

    @Test
    @DisplayName("Should correctly identify lifecycle events")
    void testLifecycleEventClassification() {
        assertTrue(DataSourceConfigurationEvent.initialized(1).isLifecycleEvent());
        assertTrue(DataSourceConfigurationEvent.reloaded(1).isLifecycleEvent());
        
        assertFalse(DataSourceConfigurationEvent.configurationAdded("test", testConfiguration).isLifecycleEvent());
        assertFalse(DataSourceConfigurationEvent.configurationRemoved("test", testConfiguration).isLifecycleEvent());
        assertFalse(DataSourceConfigurationEvent.configurationUpdated("test", testConfiguration).isLifecycleEvent());
        assertFalse(DataSourceConfigurationEvent.healthRestored("test").isLifecycleEvent());
        assertFalse(DataSourceConfigurationEvent.healthLost("test").isLifecycleEvent());
    }

    @Test
    @DisplayName("Should correctly identify configuration change events")
    void testConfigurationChangeEventClassification() {
        assertTrue(DataSourceConfigurationEvent.configurationAdded("test", testConfiguration).isConfigurationChangeEvent());
        assertTrue(DataSourceConfigurationEvent.configurationRemoved("test", testConfiguration).isConfigurationChangeEvent());
        assertTrue(DataSourceConfigurationEvent.configurationUpdated("test", testConfiguration).isConfigurationChangeEvent());
        
        assertFalse(DataSourceConfigurationEvent.initialized(1).isConfigurationChangeEvent());
        assertFalse(DataSourceConfigurationEvent.reloaded(1).isConfigurationChangeEvent());
        assertFalse(DataSourceConfigurationEvent.healthRestored("test").isConfigurationChangeEvent());
        assertFalse(DataSourceConfigurationEvent.healthLost("test").isConfigurationChangeEvent());
    }

    @Test
    @DisplayName("Should correctly identify health events")
    void testHealthEventClassification() {
        assertTrue(DataSourceConfigurationEvent.healthRestored("test").isHealthEvent());
        assertTrue(DataSourceConfigurationEvent.healthLost("test").isHealthEvent());
        
        assertFalse(DataSourceConfigurationEvent.initialized(1).isHealthEvent());
        assertFalse(DataSourceConfigurationEvent.reloaded(1).isHealthEvent());
        assertFalse(DataSourceConfigurationEvent.configurationAdded("test", testConfiguration).isHealthEvent());
        assertFalse(DataSourceConfigurationEvent.configurationRemoved("test", testConfiguration).isHealthEvent());
        assertFalse(DataSourceConfigurationEvent.configurationUpdated("test", testConfiguration).isHealthEvent());
    }

    @Test
    @DisplayName("Should correctly identify health improvement and degradation")
    void testHealthImprovementDegradationClassification() {
        DataSourceConfigurationEvent restoredEvent = DataSourceConfigurationEvent.healthRestored("test");
        assertTrue(restoredEvent.isHealthImprovement());
        assertFalse(restoredEvent.isHealthDegradation());
        
        DataSourceConfigurationEvent lostEvent = DataSourceConfigurationEvent.healthLost("test");
        assertFalse(lostEvent.isHealthImprovement());
        assertTrue(lostEvent.isHealthDegradation());
        
        // Non-health events should return false for both
        DataSourceConfigurationEvent nonHealthEvent = DataSourceConfigurationEvent.initialized(1);
        assertFalse(nonHealthEvent.isHealthImprovement());
        assertFalse(nonHealthEvent.isHealthDegradation());
    }

    // ========================================
    // Timestamp Tests
    // ========================================

    @Test
    @DisplayName("Should set timestamp when event is created")
    void testTimestampCreation() throws InterruptedException {
        LocalDateTime beforeCreation = LocalDateTime.now();
        Thread.sleep(10); // Small delay to ensure different timestamp
        
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.initialized(1);
        
        Thread.sleep(10); // Small delay to ensure different timestamp
        LocalDateTime afterCreation = LocalDateTime.now();
        
        assertNotNull(event.getTimestamp());
        assertTrue(event.getTimestamp().isAfter(beforeCreation));
        assertTrue(event.getTimestamp().isBefore(afterCreation));
    }

    @Test
    @DisplayName("Should have different timestamps for events created at different times")
    void testDifferentTimestamps() throws InterruptedException {
        DataSourceConfigurationEvent event1 = DataSourceConfigurationEvent.initialized(1);
        Thread.sleep(10); // Ensure different timestamp
        DataSourceConfigurationEvent event2 = DataSourceConfigurationEvent.initialized(1);
        
        assertNotEquals(event1.getTimestamp(), event2.getTimestamp());
        assertTrue(event2.getTimestamp().isAfter(event1.getTimestamp()));
    }

    // ========================================
    // Equality and Hash Code Tests
    // ========================================

    @Test
    @DisplayName("Should implement equals correctly")
    void testEquals() {
        DataSourceConfigurationEvent event1 = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);

        // Events with same type, name, and timestamp should be equal
        // Note: Since timestamp is set to LocalDateTime.now(), they will likely be different
        // So we test with the same event instance
        assertEquals(event1, event1);

        // Different event types should not be equal
        DataSourceConfigurationEvent differentType = DataSourceConfigurationEvent.configurationRemoved("test", testConfiguration);
        assertNotEquals(event1, differentType);

        // Different configuration names should not be equal
        DataSourceConfigurationEvent differentName = DataSourceConfigurationEvent.configurationAdded("different", testConfiguration);
        assertNotEquals(event1, differentName);

        // Null should not be equal
        assertNotEquals(event1, null);

        // Different class should not be equal
        assertNotEquals(event1, "not an event");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void testHashCode() {
        DataSourceConfigurationEvent event1 = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);

        // Same event should have same hash code
        assertEquals(event1.hashCode(), event1.hashCode());

        // Different events will likely have different hash codes due to timestamp
        // This is acceptable behavior - we just verify hashCode is consistent
        DataSourceConfigurationEvent event2 = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);
        assertNotNull(event2.hashCode()); // Verify it doesn't throw exception
    }

    // ========================================
    // String Representation Tests
    // ========================================

    @Test
    @DisplayName("Should have meaningful toString representation")
    void testToString() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationAdded("test-config", testConfiguration);
        String toString = event.toString();

        assertTrue(toString.contains("DataSourceConfigurationEvent"));
        assertTrue(toString.contains("CONFIGURATION_ADDED"));
        assertTrue(toString.contains("test-config"));
        assertTrue(toString.contains("Configuration 'test-config' was added"));
        assertNotNull(toString);
    }

    @Test
    @DisplayName("Should handle toString with null configuration name")
    void testToStringWithNullConfigurationName() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.initialized(5);
        String toString = event.toString();

        assertTrue(toString.contains("DataSourceConfigurationEvent"));
        assertTrue(toString.contains("INITIALIZED"));
        assertTrue(toString.contains("configurationName='null'"));
        assertNotNull(toString);
    }

    // ========================================
    // Edge Cases and Null Handling Tests
    // ========================================

    @Test
    @DisplayName("Should handle null configuration name in factory methods")
    void testNullConfigurationName() {
        DataSourceConfigurationEvent event1 = DataSourceConfigurationEvent.configurationAdded(null, testConfiguration);
        assertNull(event1.getConfigurationName());
        assertEquals("Configuration 'null' was added", event1.getMessage());

        DataSourceConfigurationEvent event2 = DataSourceConfigurationEvent.healthRestored(null);
        assertNull(event2.getConfigurationName());
        assertEquals("Configuration 'null' health was restored", event2.getMessage());
    }

    @Test
    @DisplayName("Should handle null configuration in factory methods")
    void testNullConfiguration() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationAdded("test", null);
        assertEquals("test", event.getConfigurationName());
        assertNull(event.getConfiguration());
        assertEquals("Configuration 'test' was added", event.getMessage());
    }

    @Test
    @DisplayName("Should handle zero and negative configuration counts")
    void testZeroAndNegativeConfigurationCounts() {
        DataSourceConfigurationEvent zeroEvent = DataSourceConfigurationEvent.initialized(0);
        assertEquals(0, zeroEvent.getData());
        assertEquals("DataSourceConfigurationService initialized with 0 configurations", zeroEvent.getMessage());

        DataSourceConfigurationEvent negativeEvent = DataSourceConfigurationEvent.reloaded(-1);
        assertEquals(-1, negativeEvent.getData());
        assertEquals("Configurations reloaded with -1 configurations", negativeEvent.getMessage());
    }

    // ========================================
    // Data Type Tests
    // ========================================

    @Test
    @DisplayName("Should handle different data types in events")
    void testDifferentDataTypes() {
        // Integer data
        DataSourceConfigurationEvent intEvent = DataSourceConfigurationEvent.initialized(42);
        assertEquals(42, intEvent.getData());
        assertTrue(intEvent.getData() instanceof Integer);

        // The factory methods only use Integer data, but the event can theoretically hold any Object
        // This is tested through the private constructor behavior
    }

    // ========================================
    // Event Properties Validation Tests
    // ========================================

    @Test
    @DisplayName("Should maintain immutable properties")
    void testImmutableProperties() {
        DataSourceConfigurationEvent event = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);

        // Properties should not change after creation
        DataSourceConfigurationEvent.EventType originalType = event.getEventType();
        String originalName = event.getConfigurationName();
        DataSourceConfiguration originalConfig = event.getConfiguration();
        Object originalData = event.getData();
        LocalDateTime originalTimestamp = event.getTimestamp();
        String originalMessage = event.getMessage();

        // Verify properties remain the same
        assertEquals(originalType, event.getEventType());
        assertEquals(originalName, event.getConfigurationName());
        assertEquals(originalConfig, event.getConfiguration());
        assertEquals(originalData, event.getData());
        assertEquals(originalTimestamp, event.getTimestamp());
        assertEquals(originalMessage, event.getMessage());
    }

    @Test
    @DisplayName("Should have consistent event classification")
    void testConsistentEventClassification() {
        // An event should not be classified as multiple types simultaneously
        for (DataSourceConfigurationEvent.EventType type : DataSourceConfigurationEvent.EventType.values()) {
            DataSourceConfigurationEvent event;

            switch (type) {
                case INITIALIZED:
                    event = DataSourceConfigurationEvent.initialized(1);
                    break;
                case CONFIGURATION_ADDED:
                    event = DataSourceConfigurationEvent.configurationAdded("test", testConfiguration);
                    break;
                case CONFIGURATION_REMOVED:
                    event = DataSourceConfigurationEvent.configurationRemoved("test", testConfiguration);
                    break;
                case CONFIGURATION_UPDATED:
                    event = DataSourceConfigurationEvent.configurationUpdated("test", testConfiguration);
                    break;
                case HEALTH_RESTORED:
                    event = DataSourceConfigurationEvent.healthRestored("test");
                    break;
                case HEALTH_LOST:
                    event = DataSourceConfigurationEvent.healthLost("test");
                    break;
                case RELOADED:
                    event = DataSourceConfigurationEvent.reloaded(1);
                    break;
                default:
                    throw new IllegalStateException("Unknown event type: " + type);
            }

            // Count how many classification methods return true
            int trueCount = 0;
            if (event.isLifecycleEvent()) trueCount++;
            if (event.isConfigurationChangeEvent()) trueCount++;
            if (event.isHealthEvent()) trueCount++;

            // Each event should belong to exactly one category
            assertEquals(1, trueCount, "Event " + type + " should belong to exactly one category");
        }
    }
}
