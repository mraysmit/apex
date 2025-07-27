package dev.mars.rulesengine.core.engine.config;

import dev.mars.rulesengine.core.engine.model.Category;
import dev.mars.rulesengine.core.engine.model.Rule;
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
 * Test class for RuleBuilder.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for RuleBuilder.
 */
public class RuleBuilderTest {

    @Test
    public void testBuildWithMinimalProperties() {
        // Create a rule with minimal properties
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .build();
        
        // Verify the rule properties
        assertNotNull(rule);
        assertNotNull(rule.getId());
        assertTrue(rule.getId().startsWith("R"));
        assertEquals("Test Rule", rule.getName());
        assertEquals("2 + 2 == 4", rule.getCondition());
        assertEquals("Simple arithmetic test", rule.getMessage());
        assertEquals("Simple arithmetic test", rule.getDescription()); // Default description
        assertEquals(100, rule.getPriority()); // Default priority
        assertEquals(1, rule.getCategories().size()); // Default category
        assertTrue(rule.hasCategory("default")); // Default category name
    }

    @Test
    public void testBuildWithAllProperties() {
        // Create a rule with all properties
        Rule rule = new RuleBuilder()
            .withId("R12345678")
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .withDescription("A rule that tests simple arithmetic")
            .withPriority(50)
            .withCategory("math")
            .build();
        
        // Verify the rule properties
        assertNotNull(rule);
        assertEquals("R12345678", rule.getId());
        assertEquals("Test Rule", rule.getName());
        assertEquals("2 + 2 == 4", rule.getCondition());
        assertEquals("Simple arithmetic test", rule.getMessage());
        assertEquals("A rule that tests simple arithmetic", rule.getDescription());
        assertEquals(50, rule.getPriority());
        assertEquals(1, rule.getCategories().size());
        assertTrue(rule.hasCategory("math"));
    }

    @Test
    public void testBuildWithCustomId() {
        // Create a rule with a custom ID
        Rule rule = new RuleBuilder("CUSTOM_ID")
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .build();
        
        // Verify the rule ID
        assertEquals("CUSTOM_ID", rule.getId());
    }

    @Test
    public void testBuildWithCategoryByName() {
        // Create a rule with a category by name
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .withCategory("math")
            .build();
        
        // Verify the rule category
        assertEquals(1, rule.getCategories().size());
        assertTrue(rule.hasCategory("math"));
    }

    @Test
    public void testBuildWithCategoryByObject() {
        // Create a rule with a category by object
        Category category = new Category("math", 50);
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .withCategory(category)
            .build();
        
        // Verify the rule category
        assertEquals(1, rule.getCategories().size());
        assertTrue(rule.hasCategory("math"));
        
        // Verify the category object is in the rule
        assertTrue(rule.getCategories().contains(category));
    }

    @Test
    public void testBuildWithMultipleCategories() {
        // Create a rule with multiple categories
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .withCategory("math")
            .withCategory("arithmetic")
            .build();
        
        // Verify the rule categories
        assertEquals(2, rule.getCategories().size());
        assertTrue(rule.hasCategory("math"));
        assertTrue(rule.hasCategory("arithmetic"));
    }

    @Test
    public void testBuildWithCategoriesBySet() {
        // Create a set of categories
        Set<Category> categories = new HashSet<>();
        categories.add(new Category("math", 50));
        categories.add(new Category("arithmetic", 60));
        
        // Create a rule with categories by set
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .withCategories(categories)
            .build();
        
        // Verify the rule categories
        assertEquals(2, rule.getCategories().size());
        assertTrue(rule.hasCategory("math"));
        assertTrue(rule.hasCategory("arithmetic"));
    }

    @Test
    public void testBuildWithCategoryNamesBySet() {
        // Create a set of category names
        Set<String> categoryNames = new HashSet<>();
        categoryNames.add("math");
        categoryNames.add("arithmetic");
        
        // Create a rule with category names by set
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .withCategoryNames(categoryNames)
            .build();
        
        // Verify the rule categories
        assertEquals(2, rule.getCategories().size());
        assertTrue(rule.hasCategory("math"));
        assertTrue(rule.hasCategory("arithmetic"));
    }

    @Test
    public void testBuildWithDefaultDescription() {
        // Create a rule without a description
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .build();
        
        // Verify the description defaults to the message
        assertEquals("Simple arithmetic test", rule.getDescription());
    }

    @Test
    public void testBuildWithDefaultCategory() {
        // Create a rule without a category
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .build();
        
        // Verify the default category
        assertEquals(1, rule.getCategories().size());
        assertTrue(rule.hasCategory("default"));
    }

    @Test
    public void testBuildWithDefaultPriority() {
        // Create a rule without a priority
        Rule rule = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test")
            .build();
        
        // Verify the default priority
        assertEquals(100, rule.getPriority());
    }

    @Test
    public void testBuildWithoutName() {
        // Create a rule without a name
        RuleBuilder builder = new RuleBuilder()
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test");
        
        // Verify that building the rule throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule name must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithoutCondition() {
        // Create a rule without a condition
        RuleBuilder builder = new RuleBuilder()
            .withName("Test Rule")
            .withMessage("Simple arithmetic test");
        
        // Verify that building the rule throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule condition must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithoutMessage() {
        // Create a rule without a message
        RuleBuilder builder = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4");
        
        // Verify that building the rule throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule message must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithEmptyName() {
        // Create a rule with an empty name
        RuleBuilder builder = new RuleBuilder()
            .withName("")
            .withCondition("2 + 2 == 4")
            .withMessage("Simple arithmetic test");
        
        // Verify that building the rule throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule name must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithEmptyCondition() {
        // Create a rule with an empty condition
        RuleBuilder builder = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("")
            .withMessage("Simple arithmetic test");
        
        // Verify that building the rule throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule condition must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithEmptyMessage() {
        // Create a rule with an empty message
        RuleBuilder builder = new RuleBuilder()
            .withName("Test Rule")
            .withCondition("2 + 2 == 4")
            .withMessage("");
        
        // Verify that building the rule throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule message must be set", exception.getMessage());
    }
}