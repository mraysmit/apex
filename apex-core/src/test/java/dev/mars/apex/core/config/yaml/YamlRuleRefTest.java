package dev.mars.apex.core.config.yaml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YamlRuleRef class.
 * 
 * Tests the functionality of external rule file references including
 * constructors, getters/setters, and enabled logic.
 */
@DisplayName("YamlRuleRef Unit Tests")
class YamlRuleRefTest {

    @Test
    @DisplayName("Should create YamlRuleRef with default constructor")
    void testDefaultConstructor() {
        YamlRuleRef ruleRef = new YamlRuleRef();
        
        assertNull(ruleRef.getName(), "Name should be null by default");
        assertNull(ruleRef.getSource(), "Source should be null by default");
        assertNull(ruleRef.getEnabled(), "Enabled should be null by default");
        assertNull(ruleRef.getDescription(), "Description should be null by default");
        assertTrue(ruleRef.isEnabled(), "isEnabled() should return true when enabled is null");
    }

    @Test
    @DisplayName("Should create YamlRuleRef with name and source constructor")
    void testNameSourceConstructor() {
        String name = "test-rules";
        String source = "rules/test-rules.yaml";
        
        YamlRuleRef ruleRef = new YamlRuleRef(name, source);
        
        assertEquals(name, ruleRef.getName(), "Name should match constructor parameter");
        assertEquals(source, ruleRef.getSource(), "Source should match constructor parameter");
        assertTrue(ruleRef.getEnabled(), "Enabled should be true by default");
        assertNull(ruleRef.getDescription(), "Description should be null by default");
        assertTrue(ruleRef.isEnabled(), "isEnabled() should return true");
    }

    @Test
    @DisplayName("Should create YamlRuleRef with full constructor")
    void testFullConstructor() {
        String name = "customer-rules";
        String source = "rules/customer-rules.yaml";
        Boolean enabled = false;
        String description = "Customer validation rules";
        
        YamlRuleRef ruleRef = new YamlRuleRef(name, source, enabled, description);
        
        assertEquals(name, ruleRef.getName(), "Name should match constructor parameter");
        assertEquals(source, ruleRef.getSource(), "Source should match constructor parameter");
        assertEquals(enabled, ruleRef.getEnabled(), "Enabled should match constructor parameter");
        assertEquals(description, ruleRef.getDescription(), "Description should match constructor parameter");
        assertFalse(ruleRef.isEnabled(), "isEnabled() should return false when enabled is false");
    }

    @Test
    @DisplayName("Should handle getters and setters correctly")
    void testGettersAndSetters() {
        YamlRuleRef ruleRef = new YamlRuleRef();
        
        // Test name
        String name = "product-rules";
        ruleRef.setName(name);
        assertEquals(name, ruleRef.getName(), "getName() should return set value");
        
        // Test source
        String source = "rules/product-rules.yaml";
        ruleRef.setSource(source);
        assertEquals(source, ruleRef.getSource(), "getSource() should return set value");
        
        // Test enabled
        Boolean enabled = true;
        ruleRef.setEnabled(enabled);
        assertEquals(enabled, ruleRef.getEnabled(), "getEnabled() should return set value");
        
        // Test description
        String description = "Product validation rules";
        ruleRef.setDescription(description);
        assertEquals(description, ruleRef.getDescription(), "getDescription() should return set value");
    }

    @Test
    @DisplayName("Should handle isEnabled() logic correctly")
    void testIsEnabledLogic() {
        YamlRuleRef ruleRef = new YamlRuleRef();
        
        // Test null enabled (should default to true)
        ruleRef.setEnabled(null);
        assertTrue(ruleRef.isEnabled(), "isEnabled() should return true when enabled is null");
        
        // Test explicitly true
        ruleRef.setEnabled(true);
        assertTrue(ruleRef.isEnabled(), "isEnabled() should return true when enabled is true");
        
        // Test explicitly false
        ruleRef.setEnabled(false);
        assertFalse(ruleRef.isEnabled(), "isEnabled() should return false when enabled is false");
    }

    @Test
    @DisplayName("Should generate meaningful toString representation")
    void testToString() {
        YamlRuleRef ruleRef = new YamlRuleRef("test-rules", "rules/test.yaml", true, "Test rules");
        
        String toString = ruleRef.toString();
        
        assertNotNull(toString, "toString() should not return null");
        assertTrue(toString.contains("test-rules"), "toString() should contain name");
        assertTrue(toString.contains("rules/test.yaml"), "toString() should contain source");
        assertTrue(toString.contains("true"), "toString() should contain enabled value");
        assertTrue(toString.contains("Test rules"), "toString() should contain description");
        assertTrue(toString.contains("YamlRuleRef"), "toString() should contain class name");
    }

    @Test
    @DisplayName("Should handle null values in toString")
    void testToStringWithNulls() {
        YamlRuleRef ruleRef = new YamlRuleRef();
        
        String toString = ruleRef.toString();
        
        assertNotNull(toString, "toString() should not return null even with null fields");
        assertTrue(toString.contains("YamlRuleRef"), "toString() should contain class name");
    }

    @Test
    @DisplayName("Should handle edge cases for enabled field")
    void testEnabledEdgeCases() {
        YamlRuleRef ruleRef = new YamlRuleRef();
        
        // Test Boolean.TRUE
        ruleRef.setEnabled(Boolean.TRUE);
        assertTrue(ruleRef.isEnabled(), "isEnabled() should handle Boolean.TRUE");
        
        // Test Boolean.FALSE
        ruleRef.setEnabled(Boolean.FALSE);
        assertFalse(ruleRef.isEnabled(), "isEnabled() should handle Boolean.FALSE");
        
        // Test setting back to null
        ruleRef.setEnabled(null);
        assertTrue(ruleRef.isEnabled(), "isEnabled() should default to true after setting to null");
    }

    @Test
    @DisplayName("Should handle empty and whitespace strings")
    void testEmptyAndWhitespaceStrings() {
        YamlRuleRef ruleRef = new YamlRuleRef();
        
        // Test empty strings
        ruleRef.setName("");
        ruleRef.setSource("");
        ruleRef.setDescription("");
        
        assertEquals("", ruleRef.getName(), "Should handle empty name");
        assertEquals("", ruleRef.getSource(), "Should handle empty source");
        assertEquals("", ruleRef.getDescription(), "Should handle empty description");
        
        // Test whitespace strings
        ruleRef.setName("   ");
        ruleRef.setSource("   ");
        ruleRef.setDescription("   ");
        
        assertEquals("   ", ruleRef.getName(), "Should handle whitespace name");
        assertEquals("   ", ruleRef.getSource(), "Should handle whitespace source");
        assertEquals("   ", ruleRef.getDescription(), "Should handle whitespace description");
    }
}
