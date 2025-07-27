package dev.mars.rulesengine.core.engine.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
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
 * Test class for Rule.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for Rule.
 * This class tests the functionality of the Rule class.
 */
public class RuleTest {

    /**
     * Test creating a rule with the simple constructor.
     */
    @Test
    public void testCreateSimpleRule() {
        // Create a simple rule
        Rule rule = new Rule(
            "TestRule",
            "#value > 10",
            "Value is greater than 10"
        );

        // Verify rule properties
        assertEquals("TestRule", rule.getName(), "Rule name should match");
        assertEquals("#value > 10", rule.getCondition(), "Rule condition should match");
        assertEquals("Value is greater than 10", rule.getMessage(), "Rule message should match");
        assertNotNull(rule.getId(), "Rule ID should not be null");
        assertNotNull(rule.getUuid(), "Rule UUID should not be null");
        assertEquals(1, rule.getCategories().size(), "Rule should have one default category");
        assertEquals(100, rule.getPriority(), "Rule priority should be 100");
        assertEquals("Value is greater than 10", rule.getDescription(), "Rule description should match the message");
    }

    /**
     * Test creating a rule with the full constructor.
     */
    @Test
    public void testCreateFullRule() {
        // Create a rule with all properties
        Rule rule = new Rule(
            "rule-001",
            "test-category",
            "TestRule",
            "#value > 10",
            "Value is greater than 10",
            "This rule tests if a value is greater than 10",
            5
        );

        // Verify rule properties
        assertEquals("rule-001", rule.getId(), "Rule ID should match");
        assertEquals("TestRule", rule.getName(), "Rule name should match");
        assertEquals("#value > 10", rule.getCondition(), "Rule condition should match");
        assertEquals("Value is greater than 10", rule.getMessage(), "Rule message should match");
        assertEquals("This rule tests if a value is greater than 10", rule.getDescription(), "Rule description should match");
        assertEquals(5, rule.getPriority(), "Rule priority should match");
        assertNotNull(rule.getUuid(), "Rule UUID should not be null");
        assertEquals(1, rule.getCategories().size(), "Rule should have one category");
        assertTrue(rule.hasCategory("test-category"), "Rule should have test-category");
    }

    /**
     * Test creating a rule with multiple categories.
     */
    @Test
    public void testCreateRuleWithMultipleCategories() {
        // Create a set of category names
        Set<String> categoryNames = new HashSet<>();
        categoryNames.add("category1");
        categoryNames.add("category2");
        categoryNames.add("category3");

        // Create a rule with multiple categories
        Rule rule = Rule.fromCategoryNames(
            "rule-002",
            categoryNames,
            "MultiCategoryRule",
            "#value > 20",
            "Value is greater than 20",
            "This rule tests if a value is greater than 20",
            10
        );

        // Verify rule properties
        assertEquals("rule-002", rule.getId(), "Rule ID should match");
        assertEquals("MultiCategoryRule", rule.getName(), "Rule name should match");
        assertEquals("#value > 20", rule.getCondition(), "Rule condition should match");
        assertEquals("Value is greater than 20", rule.getMessage(), "Rule message should match");
        assertEquals("This rule tests if a value is greater than 20", rule.getDescription(), "Rule description should match");
        assertEquals(10, rule.getPriority(), "Rule priority should match");
        assertNotNull(rule.getUuid(), "Rule UUID should not be null");
        assertEquals(3, rule.getCategories().size(), "Rule should have three categories");
        assertTrue(rule.hasCategory("category1"), "Rule should have category1");
        assertTrue(rule.hasCategory("category2"), "Rule should have category2");
        assertTrue(rule.hasCategory("category3"), "Rule should have category3");
    }

    /**
     * Test adding a category to a rule.
     */
    @Test
    public void testAddCategory() {
        // Create a simple rule
        Rule rule = new Rule(
            "TestRule",
            "#value > 10",
            "Value is greater than 10"
        );

        // Initially, the rule should have one default category
        assertEquals(1, rule.getCategories().size(), "Rule should have one default category initially");

        // Add a category
        rule.addCategory("test-category", 1);

        // Verify the category was added
        assertEquals(2, rule.getCategories().size(), "Rule should have two categories");
        assertTrue(rule.hasCategory("test-category"), "Rule should have test-category");

        // Add another category
        Category category = new Category("another-category", "Another category", 2);
        rule.addCategory(category);

        // Verify the second category was added
        assertEquals(3, rule.getCategories().size(), "Rule should have three categories");
        assertTrue(rule.hasCategory("another-category"), "Rule should have another-category");
        assertTrue(rule.hasCategory(category), "Rule should have the category object");
    }
}
