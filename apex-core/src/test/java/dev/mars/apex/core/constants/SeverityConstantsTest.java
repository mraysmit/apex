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

package dev.mars.apex.core.constants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SeverityConstants class.
 * 
 * Tests all severity constants, validation methods, and utility functions
 * to ensure consistent behavior across the APEX rules engine.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-23
 * @version 1.0
 */
@DisplayName("SeverityConstants Tests")
class SeverityConstantsTest {
    
    @Test
    @DisplayName("Should define all required severity constants")
    void testSeverityConstants() {
        assertEquals("ERROR", SeverityConstants.ERROR);
        assertEquals("WARNING", SeverityConstants.WARNING);
        assertEquals("INFO", SeverityConstants.INFO);
    }
    
    @Test
    @DisplayName("Should contain all valid severities in VALID_SEVERITIES set")
    void testValidSeverities() {
        assertEquals(3, SeverityConstants.VALID_SEVERITIES.size());
        assertTrue(SeverityConstants.VALID_SEVERITIES.contains("ERROR"));
        assertTrue(SeverityConstants.VALID_SEVERITIES.contains("WARNING"));
        assertTrue(SeverityConstants.VALID_SEVERITIES.contains("INFO"));
    }
    
    @Test
    @DisplayName("Should have INFO as default severity")
    void testDefaultSeverity() {
        assertEquals("INFO", SeverityConstants.DEFAULT_SEVERITY);
        assertEquals(SeverityConstants.INFO, SeverityConstants.DEFAULT_SEVERITY);
    }
    
    @Test
    @DisplayName("Should have correct severity priority mapping")
    void testSeverityPriority() {
        assertEquals(3, SeverityConstants.SEVERITY_PRIORITY.size());
        assertEquals(3, SeverityConstants.SEVERITY_PRIORITY.get("ERROR"));
        assertEquals(2, SeverityConstants.SEVERITY_PRIORITY.get("WARNING"));
        assertEquals(1, SeverityConstants.SEVERITY_PRIORITY.get("INFO"));
    }
    
    @Test
    @DisplayName("Should validate severity correctly")
    void testIsValidSeverity() {
        // Valid severities
        assertTrue(SeverityConstants.isValidSeverity("ERROR"));
        assertTrue(SeverityConstants.isValidSeverity("WARNING"));
        assertTrue(SeverityConstants.isValidSeverity("INFO"));
        
        // Invalid severities
        assertFalse(SeverityConstants.isValidSeverity(null));
        assertFalse(SeverityConstants.isValidSeverity(""));
        assertFalse(SeverityConstants.isValidSeverity("CRITICAL"));
        assertFalse(SeverityConstants.isValidSeverity("HIGH"));
        assertFalse(SeverityConstants.isValidSeverity("error")); // Case sensitive
        assertFalse(SeverityConstants.isValidSeverity("warning")); // Case sensitive
        assertFalse(SeverityConstants.isValidSeverity("info")); // Case sensitive
    }
    
    @Test
    @DisplayName("Should return correct severity priority")
    void testGetSeverityPriority() {
        assertEquals(3, SeverityConstants.getSeverityPriority("ERROR"));
        assertEquals(2, SeverityConstants.getSeverityPriority("WARNING"));
        assertEquals(1, SeverityConstants.getSeverityPriority("INFO"));
        
        // Invalid severities should return INFO priority (1)
        assertEquals(1, SeverityConstants.getSeverityPriority(null));
        assertEquals(1, SeverityConstants.getSeverityPriority(""));
        assertEquals(1, SeverityConstants.getSeverityPriority("INVALID"));
        assertEquals(1, SeverityConstants.getSeverityPriority("CRITICAL"));
    }
    
    @Test
    @DisplayName("Should return higher severity correctly")
    void testGetHigherSeverity() {
        // ERROR vs others
        assertEquals("ERROR", SeverityConstants.getHigherSeverity("ERROR", "WARNING"));
        assertEquals("ERROR", SeverityConstants.getHigherSeverity("WARNING", "ERROR"));
        assertEquals("ERROR", SeverityConstants.getHigherSeverity("ERROR", "INFO"));
        assertEquals("ERROR", SeverityConstants.getHigherSeverity("INFO", "ERROR"));
        
        // WARNING vs INFO
        assertEquals("WARNING", SeverityConstants.getHigherSeverity("WARNING", "INFO"));
        assertEquals("WARNING", SeverityConstants.getHigherSeverity("INFO", "WARNING"));
        
        // Same severity
        assertEquals("ERROR", SeverityConstants.getHigherSeverity("ERROR", "ERROR"));
        assertEquals("WARNING", SeverityConstants.getHigherSeverity("WARNING", "WARNING"));
        assertEquals("INFO", SeverityConstants.getHigherSeverity("INFO", "INFO"));
        
        // Invalid severities
        assertEquals("INFO", SeverityConstants.getHigherSeverity(null, null));
        assertEquals("ERROR", SeverityConstants.getHigherSeverity("ERROR", null));
        assertEquals("WARNING", SeverityConstants.getHigherSeverity(null, "WARNING"));
        assertEquals("INFO", SeverityConstants.getHigherSeverity("INVALID", null));
        assertEquals("INFO", SeverityConstants.getHigherSeverity(null, "INVALID"));
        assertEquals("INFO", SeverityConstants.getHigherSeverity("INVALID", "UNKNOWN"));
    }
    
    @Test
    @DisplayName("Should prevent instantiation")
    void testCannotInstantiate() {
        Exception exception = assertThrows(Exception.class, () -> {
            // Use reflection to try to create an instance
            var constructor = SeverityConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });

        // The UnsupportedOperationException should be the cause of the InvocationTargetException
        Throwable cause = exception.getCause();
        assertTrue(cause instanceof UnsupportedOperationException);
        assertTrue(cause.getMessage().contains("utility class"));
    }
    
    @Test
    @DisplayName("Should have immutable collections")
    void testImmutableCollections() {
        // VALID_SEVERITIES should be immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            SeverityConstants.VALID_SEVERITIES.add("CRITICAL");
        });
        
        // SEVERITY_PRIORITY should be immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            SeverityConstants.SEVERITY_PRIORITY.put("CRITICAL", 4);
        });
    }
    
    @Test
    @DisplayName("Should maintain consistency between constants and collections")
    void testConsistency() {
        // All individual constants should be in VALID_SEVERITIES
        assertTrue(SeverityConstants.VALID_SEVERITIES.contains(SeverityConstants.ERROR));
        assertTrue(SeverityConstants.VALID_SEVERITIES.contains(SeverityConstants.WARNING));
        assertTrue(SeverityConstants.VALID_SEVERITIES.contains(SeverityConstants.INFO));
        
        // All VALID_SEVERITIES should have priority mappings
        for (String severity : SeverityConstants.VALID_SEVERITIES) {
            assertTrue(SeverityConstants.SEVERITY_PRIORITY.containsKey(severity));
        }
        
        // DEFAULT_SEVERITY should be valid
        assertTrue(SeverityConstants.VALID_SEVERITIES.contains(SeverityConstants.DEFAULT_SEVERITY));
        assertTrue(SeverityConstants.SEVERITY_PRIORITY.containsKey(SeverityConstants.DEFAULT_SEVERITY));
    }
    
    @Test
    @DisplayName("Should have correct priority ordering")
    void testPriorityOrdering() {
        int errorPriority = SeverityConstants.getSeverityPriority("ERROR");
        int warningPriority = SeverityConstants.getSeverityPriority("WARNING");
        int infoPriority = SeverityConstants.getSeverityPriority("INFO");
        
        // ERROR should have highest priority
        assertTrue(errorPriority > warningPriority);
        assertTrue(errorPriority > infoPriority);
        
        // WARNING should have higher priority than INFO
        assertTrue(warningPriority > infoPriority);
        
        // Verify specific values
        assertTrue(errorPriority == 3);
        assertTrue(warningPriority == 2);
        assertTrue(infoPriority == 1);
    }
}
