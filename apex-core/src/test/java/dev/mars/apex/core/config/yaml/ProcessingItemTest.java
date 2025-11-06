package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProcessingItem class.
 * 
 * Tests focus on:
 * - Constructor validation
 * - Getter methods
 * - Type checking methods (isEnrichment, isRule, etc.)
 * - equals() and hashCode() contract
 * - toString() and getDescription() methods
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class ProcessingItemTest {

    private static final Logger logger = LoggerFactory.getLogger(ProcessingItemTest.class);

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should create ProcessingItem with valid parameters")
    void testConstructorWithValidParameters() {
        logger.info("=== Testing ProcessingItem constructor with valid parameters ===");
        
        ProcessingItem item = new ProcessingItem("enrichments", "enrich-1");
        
        assertNotNull(item, "ProcessingItem should not be null");
        assertEquals("enrichments", item.getSectionType(), "Section type should match");
        assertEquals("enrich-1", item.getItemId(), "Item ID should match");
        
        logger.info("✓ ProcessingItem created successfully: {}", item);
    }

    @Test
    @DisplayName("Should throw exception when section type is null")
    void testConstructorWithNullSectionType() {
        logger.info("=== Testing ProcessingItem constructor with null section type ===");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ProcessingItem(null, "item-1"),
            "Should throw IllegalArgumentException for null section type"
        );
        
        assertEquals("Section type cannot be null or empty", exception.getMessage());
        logger.info("✓ Null section type rejected: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when section type is empty")
    void testConstructorWithEmptySectionType() {
        logger.info("=== Testing ProcessingItem constructor with empty section type ===");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ProcessingItem("", "item-1"),
            "Should throw IllegalArgumentException for empty section type"
        );
        
        assertEquals("Section type cannot be null or empty", exception.getMessage());
        logger.info("✓ Empty section type rejected: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when section type is whitespace")
    void testConstructorWithWhitespaceSectionType() {
        logger.info("=== Testing ProcessingItem constructor with whitespace section type ===");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ProcessingItem("   ", "item-1"),
            "Should throw IllegalArgumentException for whitespace section type"
        );
        
        assertEquals("Section type cannot be null or empty", exception.getMessage());
        logger.info("✓ Whitespace section type rejected: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when item ID is null")
    void testConstructorWithNullItemId() {
        logger.info("=== Testing ProcessingItem constructor with null item ID ===");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ProcessingItem("enrichments", null),
            "Should throw IllegalArgumentException for null item ID"
        );
        
        assertEquals("Item ID cannot be null or empty", exception.getMessage());
        logger.info("✓ Null item ID rejected: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when item ID is empty")
    void testConstructorWithEmptyItemId() {
        logger.info("=== Testing ProcessingItem constructor with empty item ID ===");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ProcessingItem("enrichments", ""),
            "Should throw IllegalArgumentException for empty item ID"
        );
        
        assertEquals("Item ID cannot be null or empty", exception.getMessage());
        logger.info("✓ Empty item ID rejected: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when item ID is whitespace")
    void testConstructorWithWhitespaceItemId() {
        logger.info("=== Testing ProcessingItem constructor with whitespace item ID ===");
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ProcessingItem("enrichments", "   "),
            "Should throw IllegalArgumentException for whitespace item ID"
        );
        
        assertEquals("Item ID cannot be null or empty", exception.getMessage());
        logger.info("✓ Whitespace item ID rejected: {}", exception.getMessage());
    }

    // ========================================
    // Type Checking Tests
    // ========================================

    @Test
    @DisplayName("Should correctly identify enrichment items")
    void testIsEnrichment() {
        logger.info("=== Testing isEnrichment() method ===");
        
        ProcessingItem enrichmentItem = new ProcessingItem("enrichments", "enrich-1");
        ProcessingItem ruleItem = new ProcessingItem("rules", "rule-1");
        
        assertTrue(enrichmentItem.isEnrichment(), "Should identify enrichment item");
        assertFalse(ruleItem.isEnrichment(), "Should not identify rule as enrichment");
        
        logger.info("✓ Enrichment identification works correctly");
    }

    @Test
    @DisplayName("Should correctly identify rule items")
    void testIsRule() {
        logger.info("=== Testing isRule() method ===");
        
        ProcessingItem ruleItem = new ProcessingItem("rules", "rule-1");
        ProcessingItem enrichmentItem = new ProcessingItem("enrichments", "enrich-1");
        
        assertTrue(ruleItem.isRule(), "Should identify rule item");
        assertFalse(enrichmentItem.isRule(), "Should not identify enrichment as rule");
        
        logger.info("✓ Rule identification works correctly");
    }

    @Test
    @DisplayName("Should correctly identify enrichment group items")
    void testIsEnrichmentGroup() {
        logger.info("=== Testing isEnrichmentGroup() method ===");
        
        ProcessingItem groupItem = new ProcessingItem("enrichment-groups", "group-1");
        ProcessingItem enrichmentItem = new ProcessingItem("enrichments", "enrich-1");
        
        assertTrue(groupItem.isEnrichmentGroup(), "Should identify enrichment group item");
        assertFalse(enrichmentItem.isEnrichmentGroup(), "Should not identify enrichment as group");
        
        logger.info("✓ Enrichment group identification works correctly");
    }

    @Test
    @DisplayName("Should correctly identify rule group items")
    void testIsRuleGroup() {
        logger.info("=== Testing isRuleGroup() method ===");
        
        ProcessingItem groupItem = new ProcessingItem("rule-groups", "group-1");
        ProcessingItem ruleItem = new ProcessingItem("rules", "rule-1");
        
        assertTrue(groupItem.isRuleGroup(), "Should identify rule group item");
        assertFalse(ruleItem.isRuleGroup(), "Should not identify rule as group");
        
        logger.info("✓ Rule group identification works correctly");
    }

    @Test
    @DisplayName("Should correctly identify transformation items")
    void testIsTransformation() {
        logger.info("=== Testing isTransformation() method ===");
        
        ProcessingItem transformItem = new ProcessingItem("transformations", "transform-1");
        ProcessingItem ruleItem = new ProcessingItem("rules", "rule-1");
        
        assertTrue(transformItem.isTransformation(), "Should identify transformation item");
        assertFalse(ruleItem.isTransformation(), "Should not identify rule as transformation");
        
        logger.info("✓ Transformation identification works correctly");
    }

    @Test
    @DisplayName("Should correctly identify rule chain items")
    void testIsRuleChain() {
        logger.info("=== Testing isRuleChain() method ===");
        
        ProcessingItem chainItem = new ProcessingItem("rule-chains", "chain-1");
        ProcessingItem ruleItem = new ProcessingItem("rules", "rule-1");
        
        assertTrue(chainItem.isRuleChain(), "Should identify rule chain item");
        assertFalse(ruleItem.isRuleChain(), "Should not identify rule as chain");
        
        logger.info("✓ Rule chain identification works correctly");
    }

    // ========================================
    // Equals and HashCode Tests
    // ========================================

    @Test
    @DisplayName("Should be equal to itself")
    void testEqualsReflexive() {
        logger.info("=== Testing equals() reflexive property ===");
        
        ProcessingItem item = new ProcessingItem("enrichments", "enrich-1");
        
        assertEquals(item, item, "Item should be equal to itself");
        
        logger.info("✓ Reflexive property satisfied");
    }

    @Test
    @DisplayName("Should be equal to another instance with same properties")
    void testEqualsSymmetric() {
        logger.info("=== Testing equals() symmetric property ===");
        
        ProcessingItem item1 = new ProcessingItem("enrichments", "enrich-1");
        ProcessingItem item2 = new ProcessingItem("enrichments", "enrich-1");
        
        assertEquals(item1, item2, "Items with same properties should be equal");
        assertEquals(item2, item1, "Equality should be symmetric");
        assertEquals(item1.hashCode(), item2.hashCode(), "Hash codes should match");
        
        logger.info("✓ Symmetric property satisfied");
    }

    @Test
    @DisplayName("Should not be equal to null")
    void testEqualsNull() {
        logger.info("=== Testing equals() with null ===");
        
        ProcessingItem item = new ProcessingItem("enrichments", "enrich-1");
        
        assertNotEquals(item, null, "Item should not be equal to null");
        
        logger.info("✓ Null comparison works correctly");
    }

    @Test
    @DisplayName("Should not be equal to different type")
    void testEqualsDifferentType() {
        logger.info("=== Testing equals() with different type ===");
        
        ProcessingItem item = new ProcessingItem("enrichments", "enrich-1");
        String notAnItem = "enrichments:enrich-1";
        
        assertNotEquals(item, notAnItem, "Item should not be equal to different type");
        
        logger.info("✓ Different type comparison works correctly");
    }

    @Test
    @DisplayName("Should not be equal when section type differs")
    void testEqualsDifferentSectionType() {
        logger.info("=== Testing equals() with different section type ===");
        
        ProcessingItem item1 = new ProcessingItem("enrichments", "item-1");
        ProcessingItem item2 = new ProcessingItem("rules", "item-1");
        
        assertNotEquals(item1, item2, "Items with different section types should not be equal");
        assertNotEquals(item1.hashCode(), item2.hashCode(), "Hash codes should differ");
        
        logger.info("✓ Different section type comparison works correctly");
    }

    @Test
    @DisplayName("Should not be equal when item ID differs")
    void testEqualsDifferentItemId() {
        logger.info("=== Testing equals() with different item ID ===");
        
        ProcessingItem item1 = new ProcessingItem("enrichments", "enrich-1");
        ProcessingItem item2 = new ProcessingItem("enrichments", "enrich-2");
        
        assertNotEquals(item1, item2, "Items with different IDs should not be equal");
        assertNotEquals(item1.hashCode(), item2.hashCode(), "Hash codes should differ");
        
        logger.info("✓ Different item ID comparison works correctly");
    }

    // ========================================
    // ToString and Description Tests
    // ========================================

    @Test
    @DisplayName("Should generate correct toString() format")
    void testToString() {
        logger.info("=== Testing toString() method ===");
        
        ProcessingItem item = new ProcessingItem("enrichments", "enrich-counterparty");
        String result = item.toString();
        
        assertEquals("enrichments:enrich-counterparty", result, "toString should use sectionType:itemId format");
        
        logger.info("✓ toString() format correct: {}", result);
    }

    @Test
    @DisplayName("Should generate correct description")
    void testGetDescription() {
        logger.info("=== Testing getDescription() method ===");
        
        ProcessingItem item = new ProcessingItem("rules", "validate-credit-limit");
        String description = item.getDescription();
        
        assertNotNull(description, "Description should not be null");
        assertTrue(description.contains("rules"), "Description should contain section type");
        assertTrue(description.contains("validate-credit-limit"), "Description should contain item ID");
        
        logger.info("✓ Description generated: {}", description);
    }
}

