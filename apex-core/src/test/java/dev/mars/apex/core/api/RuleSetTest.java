package dev.mars.apex.core.api;

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


import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.metadata.RuleMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RuleSet class, specifically testing the createRuleWithMetadata functionality
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
class RuleSetTest {

    private RuleSet.GenericRuleSet ruleSet;

    @BeforeEach
    void setUp() {
        ruleSet = RuleSet.category("test-category")
                .withCreatedBy("test.user@company.com")
                .withBusinessDomain("Testing")
                .withBusinessOwner("test.owner@company.com")
                .withSourceSystem("TestSystem");
    }

    @Test
    @DisplayName("Should create rule with basic metadata using customRule method")
    void testCreateRuleWithBasicMetadata() {
        // Act
        RuleSet.GenericRuleSet result = ruleSet.customRule(
                "TestRule", 
                "#amount > 100", 
                "Amount exceeds threshold"
        );

        // Assert
        assertNotNull(result);
        List<Rule> rules = result.getRules();
        assertEquals(1, rules.size());

        Rule rule = rules.get(0);
        assertNotNull(rule);
        assertEquals("TestRule", rule.getName());
        assertEquals("#amount > 100", rule.getCondition());
        assertEquals("Amount exceeds threshold", rule.getMessage());
        assertEquals("Amount exceeds threshold", rule.getDescription()); // Should use message as description
    }

    @Test
    @DisplayName("Should create rule with custom description using customRule method")
    void testCreateRuleWithCustomDescription() {
        // Act
        RuleSet.GenericRuleSet result = ruleSet.customRule(
                "TestRule", 
                "#amount > 100", 
                "Amount exceeds threshold",
                "This rule checks if the transaction amount is greater than 100"
        );

        // Assert
        assertNotNull(result);
        List<Rule> rules = result.getRules();
        assertEquals(1, rules.size());

        Rule rule = rules.get(0);
        assertNotNull(rule);
        assertEquals("TestRule", rule.getName());
        assertEquals("#amount > 100", rule.getCondition());
        assertEquals("Amount exceeds threshold", rule.getMessage());
        assertEquals("This rule checks if the transaction amount is greater than 100", rule.getDescription());
    }

    @Test
    @DisplayName("Should set all metadata properties correctly")
    void testMetadataPropertiesSetCorrectly() {
        // Arrange
        Instant beforeCreation = Instant.now().minusSeconds(1);
        
        // Act
        RuleSet.GenericRuleSet result = ruleSet.customRule(
                "MetadataTestRule", 
                "#value == true", 
                "Test message"
        );

        // Assert
        List<Rule> rules = result.getRules();
        Rule rule = rules.get(0);
        
        // Verify basic properties
        assertEquals("MetadataTestRule", rule.getName());
        assertEquals("#value == true", rule.getCondition());
        assertEquals("Test message", rule.getMessage());
        
        // Verify metadata is present
        RuleMetadata metadata = rule.getMetadata();
        assertNotNull(metadata);
        
        // Verify audit dates
        assertNotNull(metadata.getCreatedDate());
        assertNotNull(metadata.getModifiedDate());
        assertTrue(metadata.getCreatedDate().isAfter(beforeCreation));
        assertTrue(metadata.getModifiedDate().isAfter(beforeCreation));
        
        // Verify user and business metadata
        assertEquals("test.user@company.com", metadata.getCreatedByUser());
        assertTrue(metadata.getBusinessDomain().isPresent());
        assertEquals("Testing", metadata.getBusinessDomain().get());
        assertTrue(metadata.getBusinessOwner().isPresent());
        assertEquals("test.owner@company.com", metadata.getBusinessOwner().get());
        assertTrue(metadata.getSourceSystem().isPresent());
        assertEquals("TestSystem", metadata.getSourceSystem().get());
    }

    @Test
    @DisplayName("Should set custom properties correctly")
    void testCustomPropertiesSetCorrectly() {
        // Act
        RuleSet.GenericRuleSet result = ruleSet.customRule(
                "CustomPropsRule", 
                "#test == true", 
                "Custom properties test"
        );

        // Assert
        List<Rule> rules = result.getRules();
        Rule rule = rules.get(0);
        
        // Verify custom properties
        assertTrue(rule.getCustomProperty("ruleSetCategory", String.class).isPresent());
        assertEquals("test-category", rule.getCustomProperty("ruleSetCategory", String.class).get());
        
        assertTrue(rule.getCustomProperty("creationMethod", String.class).isPresent());
        assertEquals("RuleSet.category(\"test-category\")", rule.getCustomProperty("creationMethod", String.class).get());
    }

    @Test
    @DisplayName("Should generate unique rule IDs for multiple rules")
    void testUniqueRuleIdGeneration() {
        // Act
        RuleSet.GenericRuleSet result = ruleSet
                .customRule("Rule1", "#value1 > 0", "Message1")
                .customRule("Rule2", "#value2 > 0", "Message2")
                .customRule("Rule3", "#value3 > 0", "Message3");

        // Assert
        List<Rule> rules = result.getRules();
        assertEquals(3, rules.size());
        
        // Verify all IDs are unique
        String id1 = rules.get(0).getId();
        String id2 = rules.get(1).getId();
        String id3 = rules.get(2).getId();
        
        assertNotEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertNotEquals(id2, id3);
        
        // Verify IDs are based on rule names but are unique
        assertTrue(id1.contains("Rule1") || id1.startsWith("test-category"));
        assertTrue(id2.contains("Rule2") || id2.startsWith("test-category"));
        assertTrue(id3.contains("Rule3") || id3.startsWith("test-category"));
    }

    @Test
    @DisplayName("Should set priority correctly with auto-increment")
    void testPriorityAutoIncrement() {
        // Act
        RuleSet.GenericRuleSet result = ruleSet
                .customRule("FirstRule", "#first == true", "First message")
                .customRule("SecondRule", "#second == true", "Second message")
                .customRule("ThirdRule", "#third == true", "Third message");

        // Assert
        List<Rule> rules = result.getRules();
        assertEquals(3, rules.size());
        
        // Verify priorities are auto-incremented
        int priority1 = rules.get(0).getPriority();
        int priority2 = rules.get(1).getPriority();
        int priority3 = rules.get(2).getPriority();
        
        assertEquals(priority1 + 1, priority2);
        assertEquals(priority2 + 1, priority3);
    }

    @Test
    @DisplayName("Should set category correctly")
    void testCategorySetCorrectly() {
        // Act
        RuleSet.GenericRuleSet result = ruleSet.customRule(
                "CategoryTestRule", 
                "#category == 'test'", 
                "Category test message"
        );

        // Assert
        List<Rule> rules = result.getRules();
        Rule rule = rules.get(0);
        
        // Verify category is set
        assertNotNull(rule.getCategories());
        assertFalse(rule.getCategories().isEmpty());
        
        // Should contain the test-category
        boolean hasTestCategory = rule.getCategories().stream()
                .anyMatch(category -> "test-category".equals(category.getName()));
        assertTrue(hasTestCategory, "Rule should have test-category");
    }

    @Test
    @DisplayName("Should handle effective and expiration dates when set")
    void testEffectiveAndExpirationDates() {
        // Arrange
        Instant effectiveDate = Instant.now().plusSeconds(3600); // 1 hour from now
        Instant expirationDate = Instant.now().plusSeconds(7200); // 2 hours from now
        
        RuleSet.GenericRuleSet configuredRuleSet = RuleSet.category("date-test-category")
                .withCreatedBy("date.test@company.com")
                .withEffectiveDate(effectiveDate)
                .withExpirationDate(expirationDate);

        // Act
        RuleSet.GenericRuleSet result = configuredRuleSet.customRule(
                "DateTestRule", 
                "#dateTest == true", 
                "Date test message"
        );

        // Assert
        List<Rule> rules = result.getRules();
        Rule rule = rules.get(0);
        
        RuleMetadata metadata = rule.getMetadata();
        assertTrue(metadata.getEffectiveDate().isPresent());
        assertTrue(metadata.getExpirationDate().isPresent());
        assertEquals(effectiveDate, metadata.getEffectiveDate().get());
        assertEquals(expirationDate, metadata.getExpirationDate().get());
    }

    @Test
    @DisplayName("Should validate rule parameters and throw exceptions for invalid input")
    void testRuleParameterValidation() {
        // Test null name
        assertThrows(IllegalArgumentException.class, () -> {
            ruleSet.customRule(null, "#valid == true", "Valid message");
        });

        // Test empty name
        assertThrows(IllegalArgumentException.class, () -> {
            ruleSet.customRule("", "#valid == true", "Valid message");
        });

        // Test null condition
        assertThrows(IllegalArgumentException.class, () -> {
            ruleSet.customRule("ValidName", null, "Valid message");
        });

        // Test empty condition
        assertThrows(IllegalArgumentException.class, () -> {
            ruleSet.customRule("ValidName", "", "Valid message");
        });

        // Test null message
        assertThrows(IllegalArgumentException.class, () -> {
            ruleSet.customRule("ValidName", "#valid == true", null);
        });

        // Test empty message
        assertThrows(IllegalArgumentException.class, () -> {
            ruleSet.customRule("ValidName", "#valid == true", "");
        });
    }

    @Test
    @DisplayName("Should prevent duplicate rule names")
    void testDuplicateRuleNamePrevention() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            ruleSet
                .customRule("DuplicateName", "#first == true", "First message")
                .customRule("DuplicateName", "#second == true", "Second message");
        });
    }
}
