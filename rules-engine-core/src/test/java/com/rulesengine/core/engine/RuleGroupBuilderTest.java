package com.rulesengine.core.engine;

import com.rulesengine.core.engine.model.Category;
import com.rulesengine.core.engine.model.RuleGroup;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RuleGroupBuilder.
 */
public class RuleGroupBuilderTest {

    @Test
    public void testBuildWithMinimalProperties() {
        // Create a rule group with minimal properties
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .build();
        
        // Verify the rule group properties
        assertNotNull(ruleGroup);
        assertNotNull(ruleGroup.getId());
        assertTrue(ruleGroup.getId().startsWith("G"));
        assertEquals("Test Rule Group", ruleGroup.getName());
        assertEquals("A test rule group", ruleGroup.getDescription());
        assertEquals(100, ruleGroup.getPriority()); // Default priority
        assertEquals(1, ruleGroup.getCategories().size()); // Default category
        assertTrue(ruleGroup.hasCategory("default")); // Default category name
        assertTrue(ruleGroup.isAndOperator()); // Default operator
    }

    @Test
    public void testBuildWithAllProperties() {
        // Create a rule group with all properties
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withId("G12345678")
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withPriority(50)
            .withCategory("math")
            .withOrOperator()
            .build();
        
        // Verify the rule group properties
        assertNotNull(ruleGroup);
        assertEquals("G12345678", ruleGroup.getId());
        assertEquals("Test Rule Group", ruleGroup.getName());
        assertEquals("A test rule group", ruleGroup.getDescription());
        assertEquals(50, ruleGroup.getPriority());
        assertEquals(1, ruleGroup.getCategories().size());
        assertTrue(ruleGroup.hasCategory("math"));
        assertFalse(ruleGroup.isAndOperator()); // OR operator
    }

    @Test
    public void testBuildWithCustomId() {
        // Create a rule group with a custom ID
        RuleGroup ruleGroup = new RuleGroupBuilder("CUSTOM_ID")
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .build();
        
        // Verify the rule group ID
        assertEquals("CUSTOM_ID", ruleGroup.getId());
    }

    @Test
    public void testBuildWithCategoryByName() {
        // Create a rule group with a category by name
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withCategory("math")
            .build();
        
        // Verify the rule group category
        assertEquals(1, ruleGroup.getCategories().size());
        assertTrue(ruleGroup.hasCategory("math"));
    }

    @Test
    public void testBuildWithCategoryByObject() {
        // Create a rule group with a category by object
        Category category = new Category("math", 50);
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withCategory(category)
            .build();
        
        // Verify the rule group category
        assertEquals(1, ruleGroup.getCategories().size());
        assertTrue(ruleGroup.hasCategory("math"));
        
        // Verify the category object is in the rule group
        assertTrue(ruleGroup.getCategories().contains(category));
    }

    @Test
    public void testBuildWithMultipleCategories() {
        // Create a rule group with multiple categories
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withCategory("math")
            .withCategory("arithmetic")
            .build();
        
        // Verify the rule group categories
        assertEquals(2, ruleGroup.getCategories().size());
        assertTrue(ruleGroup.hasCategory("math"));
        assertTrue(ruleGroup.hasCategory("arithmetic"));
    }

    @Test
    public void testBuildWithCategoriesBySet() {
        // Create a set of categories
        Set<Category> categories = new HashSet<>();
        categories.add(new Category("math", 50));
        categories.add(new Category("arithmetic", 60));
        
        // Create a rule group with categories by set
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withCategories(categories)
            .build();
        
        // Verify the rule group categories
        assertEquals(2, ruleGroup.getCategories().size());
        assertTrue(ruleGroup.hasCategory("math"));
        assertTrue(ruleGroup.hasCategory("arithmetic"));
    }

    @Test
    public void testBuildWithCategoryNamesBySet() {
        // Create a set of category names
        Set<String> categoryNames = new HashSet<>();
        categoryNames.add("math");
        categoryNames.add("arithmetic");
        
        // Create a rule group with category names by set
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withCategoryNames(categoryNames)
            .build();
        
        // Verify the rule group categories
        assertEquals(2, ruleGroup.getCategories().size());
        assertTrue(ruleGroup.hasCategory("math"));
        assertTrue(ruleGroup.hasCategory("arithmetic"));
    }

    @Test
    public void testBuildWithDefaultCategory() {
        // Create a rule group without a category
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .build();
        
        // Verify the default category
        assertEquals(1, ruleGroup.getCategories().size());
        assertTrue(ruleGroup.hasCategory("default"));
    }

    @Test
    public void testBuildWithDefaultPriority() {
        // Create a rule group without a priority
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .build();
        
        // Verify the default priority
        assertEquals(100, ruleGroup.getPriority());
    }

    @Test
    public void testBuildWithAndOperator() {
        // Create a rule group with AND operator
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withAndOperator()
            .build();
        
        // Verify the operator
        assertTrue(ruleGroup.isAndOperator());
    }

    @Test
    public void testBuildWithOrOperator() {
        // Create a rule group with OR operator
        RuleGroup ruleGroup = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("A test rule group")
            .withOrOperator()
            .build();
        
        // Verify the operator
        assertFalse(ruleGroup.isAndOperator());
    }

    @Test
    public void testBuildWithoutName() {
        // Create a rule group without a name
        RuleGroupBuilder builder = new RuleGroupBuilder()
            .withDescription("A test rule group");
        
        // Verify that building the rule group throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule group name must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithoutDescription() {
        // Create a rule group without a description
        RuleGroupBuilder builder = new RuleGroupBuilder()
            .withName("Test Rule Group");
        
        // Verify that building the rule group throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule group description must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithEmptyName() {
        // Create a rule group with an empty name
        RuleGroupBuilder builder = new RuleGroupBuilder()
            .withName("")
            .withDescription("A test rule group");
        
        // Verify that building the rule group throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule group name must be set", exception.getMessage());
    }

    @Test
    public void testBuildWithEmptyDescription() {
        // Create a rule group with an empty description
        RuleGroupBuilder builder = new RuleGroupBuilder()
            .withName("Test Rule Group")
            .withDescription("");
        
        // Verify that building the rule group throws an exception
        IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
        assertEquals("Rule group description must be set", exception.getMessage());
    }
}