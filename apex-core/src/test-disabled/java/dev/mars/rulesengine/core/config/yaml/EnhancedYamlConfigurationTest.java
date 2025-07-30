package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.api.RuleSet;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.metadata.RuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
 * Comprehensive tests for the enhanced YAML configuration system with generic architecture support.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class EnhancedYamlConfigurationTest {

    private YamlConfigurationLoader loader;
    private YamlRuleFactory factory;
    private YamlRulesEngineService service;

    @BeforeEach
    void setUp() {
        loader = new YamlConfigurationLoader();
        factory = new YamlRuleFactory();
        service = new YamlRulesEngineService(loader, factory);
    }

    @Test
    void testEnhancedYamlRuleWithEnterpriseMetadata() throws YamlConfigurationException {
        String yamlContent = """
            metadata:
              name: "Financial Services Test Configuration"
              version: "2.0.0"

            categories:
              - name: "settlement-enrichment"
                business-domain: "Financial Services Post-Trade"
                business-owner: "Head of Post-Trade Operations"

            rules:
              - id: "settlement-date-validation"
                name: "Settlement Date Validation"
                description: "Validates settlement date is appropriate relative to trade date"
                category: "settlement-enrichment"
                condition: "#settlementDate != null && #settlementDate.isAfter(#tradeDate)"
                message: "Settlement date must be after trade date"
                priority: 10
                enabled: true

                # Enterprise metadata
                created-by: "settlement.admin@financialservices.com"
                business-domain: "Financial Services Post-Trade"
                business-owner: "Head of Post-Trade Operations"
                source-system: "SETTLEMENT_MANAGEMENT_SYSTEM"
                effective-date: "2024-01-01T00:00:00Z"
                expiration-date: "2025-01-01T00:00:00Z"

                custom-properties:
                  department: "Post-Trade Operations"
                  compliance-level: "HIGH"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        assertNotNull(engine);
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(1, rules.size());
        
        Rule rule = rules.get(0);
        assertEquals("Settlement Date Validation", rule.getName());
        assertEquals("#settlementDate != null && #settlementDate.isAfter(#tradeDate)", rule.getCondition());

        // Test enterprise metadata
        RuleMetadata metadata = rule.getMetadata();
        assertEquals("settlement.admin@financialservices.com", metadata.getCreatedByUser());
        assertEquals("Financial Services Post-Trade", metadata.getBusinessDomain().orElse(null));
        assertEquals("Head of Post-Trade Operations", metadata.getBusinessOwner().orElse(null));
        assertEquals("SETTLEMENT_MANAGEMENT_SYSTEM", metadata.getSourceSystem().orElse(null));
        
        // Test dates
        assertNotNull(metadata.getEffectiveDate());
        assertNotNull(metadata.getExpirationDate());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), metadata.getEffectiveDate().orElse(null));
        assertEquals(Instant.parse("2025-01-01T00:00:00Z"), metadata.getExpirationDate().orElse(null));
        
        // Test custom properties
        assertEquals("Post-Trade Operations", metadata.getCustomProperty("department", String.class).orElse(null));
        assertEquals("HIGH", metadata.getCustomProperty("compliance-level", String.class).orElse(null));
        
        // Test audit dates
        assertNotNull(rule.getCreatedDate());
        assertNotNull(rule.getModifiedDate());
    }

    @Test
    void testGenericRuleSetCreationFromYaml() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "rule-1"
                name: "LEI Validation Rule"
                category: "counterparty-enrichment"
                condition: "#counterpartyLEI != null && #counterpartyLEI.length() == 20"
                message: "Valid LEI required for counterparty"
                created-by: "counterparty.admin@financialservices.com"
                business-domain: "Financial Services Post-Trade"

              - id: "rule-2"
                name: "Credit Rating Enrichment Rule"
                category: "counterparty-enrichment"
                condition: "#counterpartyCreditRating != null"
                message: "Credit rating required for risk assessment"
                created-by: "counterparty.admin@financialservices.com"
                business-domain: "Financial Services Post-Trade"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        
        // Test GenericRuleSet creation
        List<YamlRule> counterpartyRules = yamlConfig.getRules();
        RuleSet.GenericRuleSet ruleSet = factory.createGenericRuleSet("counterparty-enrichment", counterpartyRules);

        assertNotNull(ruleSet);
        assertEquals("counterparty-enrichment", ruleSet.getCategoryName());
        assertEquals(2, ruleSet.getRuleCount());
        assertFalse(ruleSet.isEmpty());

        List<Rule> rules = ruleSet.getRules();
        assertEquals(2, rules.size());

        // Verify rules have proper metadata
        for (Rule rule : rules) {
            assertEquals("counterparty.admin@financialservices.com", rule.getMetadata().getCreatedByUser());
            assertEquals("Financial Services Post-Trade", rule.getMetadata().getBusinessDomain().orElse(null));
            assertTrue(rule.getId().startsWith("counterparty-enrichment-"));
            assertNotNull(rule.getCreatedDate());
            assertNotNull(rule.getModifiedDate());
        }
    }

    @Test
    void testMultipleCategoriesWithGenericArchitecture() throws YamlConfigurationException {
        String yamlContent = """
            categories:
              - name: "settlement-enrichment"
                business-domain: "Financial Services Post-Trade"
              - name: "regulatory-enrichment"
                business-domain: "Financial Services Post-Trade"

            rules:
              - id: "settlement-rule"
                name: "Settlement Date Rule"
                category: "settlement-enrichment"
                condition: "#settlementDate != null"
                message: "Settlement date required"
                created-by: "settlement.admin@financialservices.com"

              - id: "regulatory-rule"
                name: "UTI Generation Rule"
                category: "regulatory-enrichment"
                condition: "#uniqueTransactionId != null"
                message: "UTI required for regulatory reporting"
                created-by: "compliance.officer@financialservices.com"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        assertNotNull(engine);
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        assertEquals(2, rules.size());
        
        // Verify rules are in different categories
        Rule settlementRule = rules.stream()
            .filter(r -> r.getName().equals("Settlement Date Rule"))
            .findFirst()
            .orElse(null);
        assertNotNull(settlementRule);
        assertEquals("settlement.admin@financialservices.com", settlementRule.getMetadata().getCreatedByUser());

        Rule regulatoryRule = rules.stream()
            .filter(r -> r.getName().equals("UTI Generation Rule"))
            .findFirst()
            .orElse(null);
        assertNotNull(regulatoryRule);
        assertEquals("compliance.officer@financialservices.com", regulatoryRule.getMetadata().getCreatedByUser());
    }

    @Test
    void testRuleExecutionWithEnhancedMetadata() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "execution-test"
                name: "Trade Value Validation"
                category: "trade-validation"
                condition: "#tradeValue > 50000"
                message: "Trade value exceeds minimum threshold"
                created-by: "trading.admin@financialservices.com"
                business-domain: "Financial Services Post-Trade"
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        // Test rule execution
        Map<String, Object> facts1 = Map.of("tradeValue", 75000.0);
        List<Rule> rules = engine.getConfiguration().getAllRules();
        Rule rule = rules.get(0);

        RuleResult result1 = engine.executeRule(rule, facts1);
        assertTrue(result1.isTriggered());
        assertEquals("Trade value exceeds minimum threshold", result1.getMessage());
        assertEquals("Trade Value Validation", result1.getRuleName());

        // Test with failing condition
        Map<String, Object> facts2 = Map.of("tradeValue", 25000.0);
        RuleResult result2 = engine.executeRule(rule, facts2);
        assertFalse(result2.isTriggered());

        // Verify rule metadata is preserved
        assertEquals("trading.admin@financialservices.com", rule.getMetadata().getCreatedByUser());
        assertEquals("Financial Services Post-Trade", rule.getMetadata().getBusinessDomain().orElse(null));
    }

    @Test
    void testValidationAndErrorHandling() {
        // Test invalid category name
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createGenericRuleSet("", Arrays.asList());
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createGenericRuleSet(null, Arrays.asList());
        });
        
        // Test invalid date format
        String yamlWithInvalidDate = """
            rules:
              - id: "invalid-date-rule"
                name: "Invalid Date Rule"
                category: "test"
                condition: "#value > 0"
                message: "Test message"
                effective-date: "invalid-date-format"
            """;
        
        assertDoesNotThrow(() -> {
            YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlWithInvalidDate);
            // Should not throw, but should log warning about invalid date
            RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);
            assertNotNull(engine);
        });
    }

    @Test
    void testBackwardCompatibility() throws YamlConfigurationException {
        // Test that old YAML format still works
        String legacyYamlContent = """
            rules:
              - id: "legacy-rule"
                name: "Legacy Rule"
                category: "validation"
                condition: "#value != null"
                message: "Value is required"
                priority: 10
                enabled: true
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(legacyYamlContent);

        // Test that new method works with legacy YAML format
        RulesEngine newEngine = service.createRulesEngineFromYamlConfig(yamlConfig);
        
        assertNotNull(newEngine);
        List<Rule> rules = newEngine.getConfiguration().getAllRules();
        assertEquals(1, rules.size());
        
        Rule rule = rules.get(0);
        assertEquals("Legacy Rule", rule.getName());
        assertEquals("#value != null", rule.getCondition());
        
        // Even legacy rules should have audit dates
        assertNotNull(rule.getCreatedDate());
        assertNotNull(rule.getModifiedDate());
    }

    @Test
    void testCustomPropertiesHandling() throws YamlConfigurationException {
        String yamlContent = """
            rules:
              - id: "custom-props-rule"
                name: "Fee Calculation Rule"
                category: "fee-calculation"
                condition: "#brokerCommission > 0"
                message: "Broker commission must be positive"
                created-by: "operations.manager@financialservices.com"
                custom-properties:
                  department: "Post-Trade Operations"
                  priority-level: "HIGH"
                  fee-type: "BROKER_COMMISSION"
                  regulatory-requirement: true
            """;

        YamlRuleConfiguration yamlConfig = loader.fromYamlString(yamlContent);
        RulesEngine engine = service.createRulesEngineFromYamlConfig(yamlConfig);

        List<Rule> rules = engine.getConfiguration().getAllRules();
        Rule rule = rules.get(0);
        
        RuleMetadata metadata = rule.getMetadata();
        assertEquals("Post-Trade Operations", metadata.getCustomProperty("department", String.class).orElse(null));
        assertEquals("HIGH", metadata.getCustomProperty("priority-level", String.class).orElse(null));
        assertEquals("BROKER_COMMISSION", metadata.getCustomProperty("fee-type", String.class).orElse(null));
        assertEquals(true, metadata.getCustomProperty("regulatory-requirement", Boolean.class).orElse(null));
    }
}
