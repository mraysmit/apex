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
 * Test cases for YamlEnrichment.CalculationConfig default-value support (Phase 3A Enhancement).
 * 
 * Tests the new default-value field in CalculationConfig for error recovery scenarios.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-09-27
 * @version 1.0
 */
public class YamlCalculationConfigDefaultValueTest {

    @Test
    public void testCalculationConfigDefaultValueGetterSetter() {
        YamlEnrichment.CalculationConfig config = new YamlEnrichment.CalculationConfig();
        
        // Test initial state
        assertNull(config.getDefaultValue());
        
        // Test setting string default value
        config.setDefaultValue("default-calculation");
        assertEquals("default-calculation", config.getDefaultValue());
        
        // Test setting numeric default value
        config.setDefaultValue(0.0);
        assertEquals(0.0, config.getDefaultValue());
        
        // Test setting null default value
        config.setDefaultValue(null);
        assertNull(config.getDefaultValue());
    }

    @Test
    public void testCalculationConfigWithCompleteConfiguration() {
        YamlEnrichment.CalculationConfig config = new YamlEnrichment.CalculationConfig();
        config.setExpression("#data.amount * 1.1");
        config.setResultField("adjustedAmount");
        config.setDefaultValue(0.0);
        
        java.util.List<String> dependencies = new java.util.ArrayList<>();
        dependencies.add("amount");
        config.setDependencies(dependencies);
        
        // Verify all fields are set correctly
        assertEquals("#data.amount * 1.1", config.getExpression());
        assertEquals("adjustedAmount", config.getResultField());
        assertEquals(0.0, config.getDefaultValue());
        assertEquals(dependencies, config.getDependencies());
        assertEquals(1, config.getDependencies().size());
        assertEquals("amount", config.getDependencies().get(0));
    }

    @Test
    public void testCalculationConfigDefaultValueTypes() {
        YamlEnrichment.CalculationConfig config = new YamlEnrichment.CalculationConfig();
        
        // Test different data types for default values
        
        // String
        config.setDefaultValue("UNKNOWN_CALCULATION");
        assertEquals("UNKNOWN_CALCULATION", config.getDefaultValue());
        assertTrue(config.getDefaultValue() instanceof String);
        
        // Boolean
        config.setDefaultValue(false);
        assertEquals(false, config.getDefaultValue());
        assertTrue(config.getDefaultValue() instanceof Boolean);
        
        // Integer
        config.setDefaultValue(42);
        assertEquals(42, config.getDefaultValue());
        assertTrue(config.getDefaultValue() instanceof Integer);
        
        // Double
        config.setDefaultValue(99.99);
        assertEquals(99.99, config.getDefaultValue());
        assertTrue(config.getDefaultValue() instanceof Double);
        
        // Complex object
        java.util.Map<String, Object> complexDefault = new java.util.HashMap<>();
        complexDefault.put("calculatedValue", 0);
        complexDefault.put("status", "default");
        config.setDefaultValue(complexDefault);
        assertEquals(complexDefault, config.getDefaultValue());
        assertTrue(config.getDefaultValue() instanceof java.util.Map);
    }

    @Test
    public void testCalculationConfigBackwardCompatibility() {
        YamlEnrichment.CalculationConfig config = new YamlEnrichment.CalculationConfig();
        
        // Set traditional fields without default-value
        config.setExpression("#data.price * #data.quantity");
        config.setResultField("totalValue");
        
        java.util.List<String> dependencies = new java.util.ArrayList<>();
        dependencies.add("price");
        dependencies.add("quantity");
        config.setDependencies(dependencies);
        
        // Verify default-value is null (backward compatible)
        assertNull(config.getDefaultValue());
        
        // Verify other fields work as expected
        assertEquals("#data.price * #data.quantity", config.getExpression());
        assertEquals("totalValue", config.getResultField());
        assertEquals(2, config.getDependencies().size());
        assertTrue(config.getDependencies().contains("price"));
        assertTrue(config.getDependencies().contains("quantity"));
    }

    @Test
    public void testCalculationConfigDefaultValueForFinancialCalculations() {
        YamlEnrichment.CalculationConfig config = new YamlEnrichment.CalculationConfig();
        config.setExpression("#data.principal * #data.interestRate / 100");
        config.setResultField("interestAmount");
        config.setDefaultValue(0.0); // Safe default for financial calculations
        
        java.util.List<String> dependencies = new java.util.ArrayList<>();
        dependencies.add("principal");
        dependencies.add("interestRate");
        config.setDependencies(dependencies);
        
        // Verify configuration for financial use case
        assertEquals("#data.principal * #data.interestRate / 100", config.getExpression());
        assertEquals("interestAmount", config.getResultField());
        assertEquals(0.0, config.getDefaultValue());
        assertEquals(2, config.getDependencies().size());
    }

    @Test
    public void testCalculationConfigDefaultValueEdgeCases() {
        YamlEnrichment.CalculationConfig config = new YamlEnrichment.CalculationConfig();
        
        // Test zero as default value (important for calculations)
        config.setDefaultValue(0);
        assertEquals(0, config.getDefaultValue());
        
        // Test negative default value
        config.setDefaultValue(-1);
        assertEquals(-1, config.getDefaultValue());
        
        // Test empty string as default value
        config.setDefaultValue("");
        assertEquals("", config.getDefaultValue());
        
        // Test false as default value
        config.setDefaultValue(false);
        assertEquals(false, config.getDefaultValue());
    }

    @Test
    public void testCalculationConfigInEnrichmentContext() {
        // Test CalculationConfig as part of a complete enrichment
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("test-calculation");
        enrichment.setType("calculation-enrichment");
        
        YamlEnrichment.CalculationConfig config = new YamlEnrichment.CalculationConfig();
        config.setExpression("#data.baseValue * 2");
        config.setResultField("doubledValue");
        config.setDefaultValue("CALCULATION_FAILED");
        
        enrichment.setCalculationConfig(config);
        
        // Verify the enrichment contains the calculation config with default value
        assertNotNull(enrichment.getCalculationConfig());
        assertEquals("CALCULATION_FAILED", enrichment.getCalculationConfig().getDefaultValue());
        assertEquals("#data.baseValue * 2", enrichment.getCalculationConfig().getExpression());
        assertEquals("doubledValue", enrichment.getCalculationConfig().getResultField());
    }
}
