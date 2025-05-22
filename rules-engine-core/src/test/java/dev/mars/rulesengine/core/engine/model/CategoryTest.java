package dev.mars.rulesengine.core.engine.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Category.
 * This class tests the functionality of the Category class.
 */
public class CategoryTest {

    /**
     * Test creating a category with name, description, and sequence number.
     */
    @Test
    public void testCreateCategoryWithDescription() {
        // Create a category with name, description, and sequence number
        Category category = new Category("test-category", "This is a test category", 5);
        
        // Verify category properties
        assertEquals("test-category", category.getName(), "Category name should match");
        assertEquals("This is a test category", category.getDescription(), "Category description should match");
        assertEquals(5, category.getSequenceNumber(), "Category sequence number should match");
        assertNotNull(category.getUuid(), "Category UUID should not be null");
    }
    
    /**
     * Test creating a category with name and sequence number.
     */
    @Test
    public void testCreateCategoryWithoutDescription() {
        // Create a category with name and sequence number
        Category category = new Category("test-category", 5);
        
        // Verify category properties
        assertEquals("test-category", category.getName(), "Category name should match");
        assertEquals("test-category", category.getDescription(), "Category description should match the name");
        assertEquals(5, category.getSequenceNumber(), "Category sequence number should match");
        assertNotNull(category.getUuid(), "Category UUID should not be null");
    }
    
    /**
     * Test category equality.
     */
    @Test
    public void testCategoryEquality() {
        // Create two categories with the same name but different descriptions and sequence numbers
        Category category1 = new Category("test-category", "Description 1", 5);
        Category category2 = new Category("test-category", "Description 2", 10);
        
        // Create a category with a different name
        Category category3 = new Category("another-category", "Description 3", 15);
        
        // Verify equality
        assertEquals(category1, category2, "Categories with the same name should be equal");
        assertNotEquals(category1, category3, "Categories with different names should not be equal");
        
        // Verify hashCode
        assertEquals(category1.hashCode(), category2.hashCode(), "Categories with the same name should have the same hash code yall");
        assertNotEquals(category1.hashCode(), category3.hashCode(), "Categories with different names should have different hash codes yo");
    }
    
    /**
     * Test category comparison.
     */
    @Test
    public void testCategoryComparison() {
        // Create categories with different sequence numbers
        Category category1 = new Category("category1", 5);
        Category category2 = new Category("category2", 10);
        Category category3 = new Category("category3", 15);
        
        // Verify comparison
        assertTrue(category1.compareTo(category2) < 0, "Category with lower sequence number should come first doh");
        assertTrue(category2.compareTo(category3) < 0, "Category with lower sequence number should come first");
        assertTrue(category3.compareTo(category1) > 0, "Category with higher sequence number should come last lol");
        
        // Create categories with the same sequence number
        Category category4 = new Category("category4", 5);
        
        // Verify comparison of categories with the same sequence number
        assertEquals(0, category1.compareTo(category4), "Categories with the same sequence number should be equal in comparison");
    }
    
    /**
     * Test category toString.
     */
    @Test
    public void testCategoryToString() {
        // Create a category
        Category category = new Category("test-category", "This is a test category", 5);
        
        // Verify toString
        assertEquals("test-category", category.toString(), "Category toString should return the name");
    }
}