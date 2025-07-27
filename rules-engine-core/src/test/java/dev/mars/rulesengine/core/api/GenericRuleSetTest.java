package dev.mars.rulesengine.core.api;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.metadata.RuleMetadata;
import dev.mars.rulesengine.core.engine.model.metadata.RuleStatus;
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
 * Comprehensive tests for the new generic, extensible RuleSet architecture.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class GenericRuleSetTest {

    @Test
    public void testGenericCategoryCreation() {
        // Test creating various domain categories
        RuleSet.GenericRuleSet healthcare = RuleSet.category("patient-eligibility");
        RuleSet.GenericRuleSet manufacturing = RuleSet.category("quality-control");
        RuleSet.GenericRuleSet dataGovernance = RuleSet.category("data-governance");
        
        assertEquals("patient-eligibility", healthcare.getCategoryName());
        assertEquals("quality-control", manufacturing.getCategoryName());
        assertEquals("data-governance", dataGovernance.getCategoryName());
        
        assertTrue(healthcare.isEmpty());
        assertTrue(manufacturing.isEmpty());
        assertTrue(dataGovernance.isEmpty());
    }

    @Test
    public void testCategoryNameValidation() {
        // Valid category names should work
        assertDoesNotThrow(() -> RuleSet.category("valid-category"));
        assertDoesNotThrow(() -> RuleSet.category("valid_category"));
        assertDoesNotThrow(() -> RuleSet.category("validCategory123"));
        
        // Invalid category names should throw exceptions
        assertThrows(IllegalArgumentException.class, () -> RuleSet.category(null));
        assertThrows(IllegalArgumentException.class, () -> RuleSet.category(""));
        assertThrows(IllegalArgumentException.class, () -> RuleSet.category("   "));
        assertThrows(IllegalArgumentException.class, () -> RuleSet.category("invalid category!"));
        assertThrows(IllegalArgumentException.class, () -> RuleSet.category("invalid@category"));
        assertThrows(IllegalArgumentException.class, () -> RuleSet.category("-invalid"));
        
        // Test length limit
        String tooLong = "a".repeat(101);
        assertThrows(IllegalArgumentException.class, () -> RuleSet.category(tooLong));
    }

    @Test
    public void testBasicRuleCreation() {
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("test-category")
            .withCreatedBy("test.user@company.com")
            .customRule("Test Rule", "#value > 0", "Value must be positive");
        
        assertEquals(1, ruleSet.getRuleCount());
        assertFalse(ruleSet.isEmpty());
        
        List<Rule> rules = ruleSet.getRules();
        assertEquals(1, rules.size());
        
        Rule rule = rules.get(0);
        assertEquals("Test Rule", rule.getName());
        assertEquals("#value > 0", rule.getCondition());
        assertEquals("Value must be positive", rule.getMessage());
        assertTrue(rule.getId().startsWith("test-category-"));
    }

    @Test
    public void testEnterpriseMetadataSupport() {
        Instant effectiveDate = Instant.now();
        Instant expirationDate = effectiveDate.plusSeconds(365 * 24 * 60 * 60);
        
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("enterprise-test")
            .withCreatedBy("enterprise.user@company.com")
            .withBusinessDomain("Enterprise Testing")
            .withBusinessOwner("Test Manager")
            .withSourceSystem("TEST_SYSTEM")
            .withEffectiveDate(effectiveDate)
            .withExpirationDate(expirationDate)
            .customRule("Enterprise Rule", "#approved == true", "Must be approved");
        
        List<Rule> rules = ruleSet.getRules();
        assertEquals(1, rules.size());
        
        Rule rule = rules.get(0);
        RuleMetadata metadata = rule.getMetadata();
        
        // Test critical audit dates
        assertNotNull(rule.getCreatedDate());
        assertNotNull(rule.getModifiedDate());
        
        // Test metadata
        assertEquals("enterprise.user@company.com", metadata.getCreatedByUser());
        assertEquals("Enterprise Testing", metadata.getBusinessDomain().orElse(null));
        assertEquals("Test Manager", metadata.getBusinessOwner().orElse(null));
        assertEquals("TEST_SYSTEM", metadata.getSourceSystem().orElse(null));
        assertEquals(effectiveDate, metadata.getEffectiveDate().orElse(null));
        assertEquals(expirationDate, metadata.getExpirationDate().orElse(null));
        
        // Test custom properties
        assertEquals("enterprise-test", metadata.getCustomProperty("ruleSetCategory", String.class).orElse(null));
    }

    @Test
    public void testRuleValidation() {
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("validation-test");
        
        // Valid rule should work
        assertDoesNotThrow(() -> 
            ruleSet.customRule("Valid Rule", "#value > 0", "Valid message"));
        
        // Invalid parameters should throw exceptions
        assertThrows(IllegalArgumentException.class, () -> 
            ruleSet.customRule(null, "#value > 0", "Message"));
        assertThrows(IllegalArgumentException.class, () -> 
            ruleSet.customRule("Name", null, "Message"));
        assertThrows(IllegalArgumentException.class, () -> 
            ruleSet.customRule("Name", "#value > 0", null));
        assertThrows(IllegalArgumentException.class, () -> 
            ruleSet.customRule("", "#value > 0", "Message"));
        
        // Test length limits
        String longName = "a".repeat(201);
        String longCondition = "a".repeat(2001);
        String longMessage = "a".repeat(501);
        
        assertThrows(IllegalArgumentException.class, () -> 
            ruleSet.customRule(longName, "#value > 0", "Message"));
        assertThrows(IllegalArgumentException.class, () -> 
            ruleSet.customRule("Name", longCondition, "Message"));
        assertThrows(IllegalArgumentException.class, () -> 
            ruleSet.customRule("Name", "#value > 0", longMessage));
    }

    @Test
    public void testDuplicateRuleNameDetection() {
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("duplicate-test")
            .customRule("Unique Rule", "#value > 0", "First rule");
        
        // Adding rule with same name should fail
        assertThrows(IllegalStateException.class, () -> 
            ruleSet.customRule("Unique Rule", "#value < 100", "Second rule"));
    }

    @Test
    public void testBulkRuleOperations() {
        List<RuleSet.RuleDefinition> ruleDefinitions = Arrays.asList(
            new RuleSet.RuleDefinition("Rule 1", "#value > 0", "Positive value"),
            new RuleSet.RuleDefinition("Rule 2", "#value < 100", "Value under 100"),
            new RuleSet.RuleDefinition("Rule 3", "#value % 2 == 0", "Even value", "Checks if value is even")
        );
        
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("bulk-test")
            .withCreatedBy("bulk.user@company.com")
            .customRules(ruleDefinitions);
        
        assertEquals(3, ruleSet.getRuleCount());
        
        List<Rule> rules = ruleSet.getRules();
        assertEquals("Rule 1", rules.get(0).getName());
        assertEquals("Rule 2", rules.get(1).getName());
        assertEquals("Rule 3", rules.get(2).getName());
        
        // Test that all rules have proper metadata
        for (Rule rule : rules) {
            assertEquals("bulk.user@company.com", rule.getMetadata().getCreatedByUser());
            assertNotNull(rule.getCreatedDate());
            assertNotNull(rule.getModifiedDate());
        }
    }

    @Test
    public void testRuleExecution() {
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("execution-test")
            .withCreatedBy("execution.user@company.com")
            .customRule("Positive Check", "#value > 0", "Value is positive")
            .customRule("Range Check", "#value >= 10 && #value <= 100", "Value in range");
        
        RulesEngine engine = ruleSet.build();
        
        // Test case 1: Value that passes both rules
        Map<String, Object> data1 = Map.of("value", 50);
        List<Rule> rules = ruleSet.getRules();
        
        RuleResult result1 = engine.executeRule(rules.get(0), data1);
        assertTrue(result1.isTriggered());
        assertEquals("Value is positive", result1.getMessage());
        
        RuleResult result2 = engine.executeRule(rules.get(1), data1);
        assertTrue(result2.isTriggered());
        assertEquals("Value in range", result2.getMessage());
        
        // Test case 2: Value that fails range check
        Map<String, Object> data2 = Map.of("value", 150);
        
        RuleResult result3 = engine.executeRule(rules.get(0), data2);
        assertTrue(result3.isTriggered());
        
        RuleResult result4 = engine.executeRule(rules.get(1), data2);
        assertFalse(result4.isTriggered());
    }

    @Test
    public void testUniqueIdGeneration() {
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("id-test")
            .customRule("Test Rule 1", "#value > 0", "Message 1")
            .customRule("Test Rule 2", "#value > 0", "Message 2");
        
        List<Rule> rules = ruleSet.getRules();
        assertEquals(2, rules.size());
        
        String id1 = rules.get(0).getId();
        String id2 = rules.get(1).getId();
        
        // IDs should be different
        assertNotEquals(id1, id2);
        
        // IDs should start with category name
        assertTrue(id1.startsWith("id-test-"));
        assertTrue(id2.startsWith("id-test-"));
        
        // IDs should contain sanitized rule names
        assertTrue(id1.contains("test-rule-1"));
        assertTrue(id2.contains("test-rule-2"));
    }

    @Test
    public void testBuildValidation() {
        RuleSet.GenericRuleSet emptyRuleSet = RuleSet.category("empty-test");
        
        // Building empty rule set should fail
        assertThrows(IllegalStateException.class, () -> emptyRuleSet.build());
        
        // Building with rules should work
        RuleSet.GenericRuleSet validRuleSet = RuleSet.category("valid-test")
            .customRule("Test Rule", "#value > 0", "Test message");
        
        assertDoesNotThrow(() -> validRuleSet.build());
    }

    @Test
    public void testRuleDefinitionHelper() {
        // Test basic constructor
        RuleSet.RuleDefinition def1 = new RuleSet.RuleDefinition("Rule Name", "#condition", "Message");
        assertEquals("Rule Name", def1.getName());
        assertEquals("#condition", def1.getCondition());
        assertEquals("Message", def1.getMessage());
        assertEquals("Message", def1.getDescription()); // Should default to message
        
        // Test full constructor
        RuleSet.RuleDefinition def2 = new RuleSet.RuleDefinition("Rule Name", "#condition", "Message", "Description");
        assertEquals("Rule Name", def2.getName());
        assertEquals("#condition", def2.getCondition());
        assertEquals("Message", def2.getMessage());
        assertEquals("Description", def2.getDescription());
        
        // Test toString
        String toString = def1.toString();
        assertTrue(toString.contains("Rule Name"));
        assertTrue(toString.contains("#condition"));
    }

    @Test
    public void testAuditTrailPreservation() {
        Instant beforeCreation = Instant.now();
        
        RuleSet.GenericRuleSet ruleSet = RuleSet.category("audit-test")
            .withCreatedBy("audit.user@company.com")
            .customRule("Audit Rule", "#value > 0", "Audit message");
        
        List<Rule> rules = ruleSet.getRules();
        Rule rule = rules.get(0);
        
        Instant afterCreation = Instant.now();
        
        // Verify creation date is within expected range
        assertTrue(rule.getCreatedDate().isAfter(beforeCreation.minusSeconds(1)));
        assertTrue(rule.getCreatedDate().isBefore(afterCreation.plusSeconds(1)));
        
        // Verify modification date equals creation date for new rules
        assertEquals(rule.getCreatedDate(), rule.getModifiedDate());
        
        // Verify user attribution
        assertEquals("audit.user@company.com", rule.getMetadata().getCreatedByUser());
    }
}
