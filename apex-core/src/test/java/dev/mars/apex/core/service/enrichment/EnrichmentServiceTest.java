package dev.mars.apex.core.service.enrichment;

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


import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.YamlConfigurationLoader;
import dev.mars.apex.core.config.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;


import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for enrichment processing using RulesEngine.
 * Migrated from deprecated YamlEnrichmentProcessor to RulesEngine API.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class EnrichmentServiceTest {

    private YamlConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ApexCacheManager.resetInstance();
        loader = new YamlConfigurationLoader();
    }

    @Test
    @DisplayName("Should enrich object using field-enrichment")
    void testEnrichObjectWithYamlConfig() throws Exception {
        String yamlConfig = """
            metadata:
              name: test-enrichment-config
              description: Test enrichment configuration
            enrichments:
              - id: currency-enrichment
                type: field-enrichment
                field-mappings:
                  - source-field: currency
                    target-field: currencyCode
            """;

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", "USD");
        inputData.put("amount", 1000.0);

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getEnrichedData());
        assertEquals("USD", result.getEnrichedData().get("currencyCode"));
    }

    @Test
    @DisplayName("Should handle empty YAML configuration gracefully")
    void testEnrichObjectWithEmptyYamlConfig() throws Exception {
        String emptyConfig = """
            metadata:
              name: empty-config
              description: Empty configuration
            """;

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("currency", "USD");
        inputData.put("amount", 1000.0);

        YamlRuleConfiguration config = loader.fromYamlString(emptyConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle multiple field enrichments in sequence")
    void testMultipleEnrichmentsInSequence() throws Exception {
        String yamlConfig = """
            metadata:
              name: multiple-enrichments
              description: Multiple enrichment test
            enrichments:
              - id: enrichment-1
                type: field-enrichment
                field-mappings:
                  - source-field: field1
                    target-field: result1
              - id: enrichment-2
                type: field-enrichment
                field-mappings:
                  - source-field: field2
                    target-field: result2
            """;

        Map<String, Object> inputData = new HashMap<>();
        inputData.put("field1", "value1");
        inputData.put("field2", "value2");

        YamlRuleConfiguration config = loader.fromYamlString(yamlConfig);
        RulesEngine engine = RulesEngine.fromYamlConfig(config);
        RuleResult result = engine.evaluate(inputData);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("value1", result.getEnrichedData().get("result1"));
        assertEquals("value2", result.getEnrichedData().get("result2"));
    }

    @Test
    @DisplayName("Should reject invalid enrichment type during YAML loading")
    void testInvalidEnrichmentType() {
        String yamlConfig = """
            metadata:
              name: error-test
              description: Test error handling
            enrichments:
              - id: invalid-enrichment
                type: nonexistent-type
                field-mappings:
                  - source-field: field1
                    target-field: result1
            """;

        // Invalid enrichment types should be caught during YAML loading
        assertThrows(Exception.class, () -> {
            loader.fromYamlString(yamlConfig);
        });
    }
}
