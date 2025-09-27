package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Test cases for YamlRule default-value support (Phase 3A Enhancement).
 * 
 * Tests the new default-value field in YamlRule for error recovery scenarios.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
public class YamlRuleDefaultValueTest {

    @Test
    public void testYamlRuleDefaultValueGetterSetter() {
        YamlRule rule = new YamlRule();
        
        // Test initial state
        assertNull(rule.getDefaultValue());
        
        // Test setting string default value
        rule.setDefaultValue("default-string");
        assertEquals("default-string", rule.getDefaultValue());
        
        // Test setting boolean default value
        rule.setDefaultValue(true);
        assertEquals(true, rule.getDefaultValue());
        
        // Test setting numeric default value
        rule.setDefaultValue(42);
        assertEquals(42, rule.getDefaultValue());
        
        // Test setting null default value
        rule.setDefaultValue(null);
        assertNull(rule.getDefaultValue());
    }

    @Test
    public void testYamlRuleWithCompleteConfiguration() {
        YamlRule rule = new YamlRule();
        rule.setId("test-rule");
        rule.setName("Test Rule");
        rule.setCondition("#data.value > 0");
        rule.setSeverity("WARNING");
        rule.setDefaultValue(false);
        
        // Verify all fields are set correctly
        assertEquals("test-rule", rule.getId());
        assertEquals("Test Rule", rule.getName());
        assertEquals("#data.value > 0", rule.getCondition());
        assertEquals("WARNING", rule.getSeverity());
        assertEquals(false, rule.getDefaultValue());
    }

    @Test
    public void testYamlRuleDefaultValueTypes() {
        YamlRule rule = new YamlRule();
        
        // Test different data types for default values
        
        // String
        rule.setDefaultValue("UNKNOWN");
        assertEquals("UNKNOWN", rule.getDefaultValue());
        assertTrue(rule.getDefaultValue() instanceof String);
        
        // Boolean
        rule.setDefaultValue(true);
        assertEquals(true, rule.getDefaultValue());
        assertTrue(rule.getDefaultValue() instanceof Boolean);
        
        // Integer
        rule.setDefaultValue(100);
        assertEquals(100, rule.getDefaultValue());
        assertTrue(rule.getDefaultValue() instanceof Integer);
        
        // Double
        rule.setDefaultValue(3.14);
        assertEquals(3.14, rule.getDefaultValue());
        assertTrue(rule.getDefaultValue() instanceof Double);
        
        // Complex object (Map)
        java.util.Map<String, Object> complexDefault = new java.util.HashMap<>();
        complexDefault.put("status", "default");
        complexDefault.put("value", 0);
        rule.setDefaultValue(complexDefault);
        assertEquals(complexDefault, rule.getDefaultValue());
        assertTrue(rule.getDefaultValue() instanceof java.util.Map);
    }

    @Test
    public void testYamlRuleBackwardCompatibility() {
        YamlRule rule = new YamlRule();
        
        // Set traditional fields without default-value
        rule.setId("legacy-rule");
        rule.setName("Legacy Rule");
        rule.setCondition("#data.field != null");
        rule.setSeverity("ERROR");
        
        // Verify default-value is null (backward compatible)
        assertNull(rule.getDefaultValue());
        
        // Verify other fields work as expected
        assertEquals("legacy-rule", rule.getId());
        assertEquals("Legacy Rule", rule.getName());
        assertEquals("#data.field != null", rule.getCondition());
        assertEquals("ERROR", rule.getSeverity());
    }

    @Test
    public void testYamlRuleDefaultValueWithMetadata() {
        YamlRule rule = new YamlRule();
        rule.setId("metadata-rule");
        rule.setName("Rule with Metadata");
        rule.setCondition("#data.amount >= 1000");
        rule.setSeverity("INFO");
        rule.setDefaultValue("APPROVED");
        rule.setBusinessDomain("finance");
        rule.setBusinessOwner("risk-team");
        
        // Verify all fields including default-value
        assertEquals("metadata-rule", rule.getId());
        assertEquals("Rule with Metadata", rule.getName());
        assertEquals("#data.amount >= 1000", rule.getCondition());
        assertEquals("INFO", rule.getSeverity());
        assertEquals("APPROVED", rule.getDefaultValue());
        assertEquals("finance", rule.getBusinessDomain());
        assertEquals("risk-team", rule.getBusinessOwner());
    }

    @Test
    public void testYamlRuleDefaultValueEdgeCases() {
        YamlRule rule = new YamlRule();
        
        // Test empty string as default value
        rule.setDefaultValue("");
        assertEquals("", rule.getDefaultValue());
        
        // Test zero as default value
        rule.setDefaultValue(0);
        assertEquals(0, rule.getDefaultValue());
        
        // Test false as default value
        rule.setDefaultValue(false);
        assertEquals(false, rule.getDefaultValue());
        
        // Test empty collection as default value
        java.util.List<String> emptyList = new java.util.ArrayList<>();
        rule.setDefaultValue(emptyList);
        assertEquals(emptyList, rule.getDefaultValue());
        assertTrue(((java.util.List<?>) rule.getDefaultValue()).isEmpty());
    }
}
