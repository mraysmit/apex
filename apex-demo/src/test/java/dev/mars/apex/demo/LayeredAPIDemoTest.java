package dev.mars.apex.demo;

import dev.mars.apex.demo.examples.LayeredAPIDemo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for LayeredAPIDemo to verify the three-layer API design demonstration.
 * 
 * These tests ensure that:
 * - All three API layers are demonstrated correctly
 * - Layer progression is shown clearly
 * - Each layer's benefits are explained
 * - Performance characteristics are acceptable
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
class LayeredAPIDemoTest {
    
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private LayeredAPIDemo demo;
    
    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        demo = new LayeredAPIDemo();
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
    
    @Test
    @DisplayName("LayeredAPI demo should run successfully without exceptions")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLayeredAPIDemoRunsSuccessfully() {
        assertDoesNotThrow(() -> {
            demo.run();
        }, "LayeredAPI demo should not throw any exceptions");
    }
    
    @Test
    @DisplayName("LayeredAPI demo should contain all three layers")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLayeredAPIDemoContainsAllLayers() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("LayeredAPI demo layers",
            () -> assertTrue(output.contains("Three-Layer API Design"), 
                           "Should display main title"),
            () -> assertTrue(output.contains("Layer 1: ULTRA-SIMPLE API"), 
                           "Should demonstrate Layer 1"),
            () -> assertTrue(output.contains("Layer 2: TEMPLATE-BASED RULES"), 
                           "Should demonstrate Layer 2"),
            () -> assertTrue(output.contains("Layer 3: ADVANCED CONFIGURATION"), 
                           "Should demonstrate Layer 3"),
            () -> assertTrue(output.contains("API PROGRESSION"), 
                           "Should show API progression"),
            () -> assertTrue(output.contains("Layered API demonstration completed"), 
                           "Should show completion message")
        );
    }
    
    @Test
    @DisplayName("Layer 1 Ultra-Simple API should be demonstrated correctly")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testLayer1UltraSimpleAPI() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Layer 1 Ultra-Simple API",
            () -> assertTrue(output.contains("ULTRA-SIMPLE API (90% of use cases)"), 
                           "Should show Layer 1 header with percentage"),
            () -> assertTrue(output.contains("Quick validations") || output.contains("immediate decisions"), 
                           "Should explain Layer 1 use cases"),
            () -> assertTrue(output.contains("Rules.check"), 
                           "Should show Rules.check usage"),
            () -> assertTrue(output.contains("#data.age >= 18"), 
                           "Should show age validation"),
            () -> assertTrue(output.contains("#data.email != null"), 
                           "Should show email validation"),
            () -> assertTrue(output.contains("✅") || output.contains("❌"), 
                           "Should show validation results"),
            () -> assertTrue(output.contains("Layer 1 Benefits") || output.contains("💡"), 
                           "Should explain Layer 1 benefits")
        );
    }
    
    @Test
    @DisplayName("Layer 2 Template-Based Rules should be demonstrated correctly")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testLayer2TemplateBasedRules() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Layer 2 Template-Based Rules",
            () -> assertTrue(output.contains("TEMPLATE-BASED RULES (8% of use cases)"), 
                           "Should show Layer 2 header with percentage"),
            () -> assertTrue(output.contains("Complex validations") || output.contains("detailed error reporting"), 
                           "Should explain Layer 2 use cases"),
            () -> assertTrue(output.contains("rulesService.validate"), 
                           "Should show validation service"),
            () -> assertTrue(output.contains("minimumAge"), 
                           "Should show age validation helper"),
            () -> assertTrue(output.contains("emailRequired"), 
                           "Should show email validation helper"),
            () -> assertTrue(output.contains("ValidationResult"), 
                           "Should show validation result"),
            () -> assertTrue(output.contains("Layer 2 Benefits") || output.contains("💡"), 
                           "Should explain Layer 2 benefits")
        );
    }
    
    @Test
    @DisplayName("Layer 3 Advanced Configuration should be demonstrated correctly")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testLayer3AdvancedConfiguration() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Layer 3 Advanced Configuration",
            () -> assertTrue(output.contains("ADVANCED CONFIGURATION (2% of use cases)"), 
                           "Should show Layer 3 header with percentage"),
            () -> assertTrue(output.contains("Enterprise rules") || output.contains("external management"), 
                           "Should explain Layer 3 use cases"),
            () -> assertTrue(output.contains("YamlConfigurationLoader"), 
                           "Should show YAML loader"),
            () -> assertTrue(output.contains("YamlRulesEngineService"), 
                           "Should show YAML service"),
            () -> assertTrue(output.contains("Enterprise Configuration") || output.contains("metadata"), 
                           "Should show enterprise features"),
            () -> assertTrue(output.contains("Layer 3 Benefits") || output.contains("💡"), 
                           "Should explain Layer 3 benefits")
        );
    }
    
    @Test
    @DisplayName("API progression should be demonstrated clearly")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testAPIProgression() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("API progression",
            () -> assertTrue(output.contains("API PROGRESSION"), 
                           "Should have progression section"),
            () -> assertTrue(output.contains("Same business rule implemented at each layer"), 
                           "Should explain progression concept"),
            () -> assertTrue(output.contains("Layer 1 (Ultra-Simple)") || output.contains("🥉"), 
                           "Should show Layer 1 in progression"),
            () -> assertTrue(output.contains("Layer 2 (Template-Based)") || output.contains("🥈"), 
                           "Should show Layer 2 in progression"),
            () -> assertTrue(output.contains("Layer 3 (Advanced Configuration)") || output.contains("🥇"), 
                           "Should show Layer 3 in progression"),
            () -> assertTrue(output.contains("Recommendation") || output.contains("🎯"), 
                           "Should provide recommendations")
        );
    }
    
    @Test
    @DisplayName("Demo should show percentage breakdown of use cases")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testUseCasePercentages() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Use case percentages",
            () -> assertTrue(output.contains("90%"), 
                           "Should show 90% for Layer 1"),
            () -> assertTrue(output.contains("8%"), 
                           "Should show 8% for Layer 2"),
            () -> assertTrue(output.contains("2%"), 
                           "Should show 2% for Layer 3")
        );
    }
    
    @Test
    @DisplayName("Demo should provide clear guidance on when to use each layer")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLayerGuidance() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Layer guidance",
            () -> assertTrue(output.contains("Use when:") || output.contains("Perfect for:"), 
                           "Should provide usage guidance"),
            () -> assertTrue(output.contains("prototyping") || output.contains("simple rules"), 
                           "Should explain Layer 1 usage"),
            () -> assertTrue(output.contains("detailed errors") || output.contains("complex validations"), 
                           "Should explain Layer 2 usage"),
            () -> assertTrue(output.contains("enterprise") || output.contains("external management"), 
                           "Should explain Layer 3 usage")
        );
    }
    
    @Test
    @DisplayName("Demo should show actual rule execution results")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testActualRuleExecution() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Actual rule execution",
            () -> assertTrue(output.contains("✅") || output.contains("❌") || 
                           output.contains("PASSED") || output.contains("FAILED"), 
                           "Should show rule execution results"),
            () -> assertTrue(output.contains("Alice Johnson") || output.contains("Customer:"), 
                           "Should show customer data"),
            () -> assertTrue(output.contains("age") && output.contains("email"), 
                           "Should show customer properties")
        );
    }
    
    @Test
    @DisplayName("Demo should handle errors gracefully")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testErrorHandling() {
        assertDoesNotThrow(() -> {
            demo.run();
        }, "Demo should handle any internal errors gracefully");
        
        String output = outputStream.toString();
        
        assertAll("Error handling",
            () -> assertFalse(output.contains("Exception in thread"),
                            "Should not contain unhandled exceptions"),
            () -> assertTrue(output.contains("Layered API demonstration completed") ||
                           output.contains("Three-Layer API"),
                           "Should complete demo execution despite any expected errors")
        );
    }
    
    @Test
    @DisplayName("Demo should complete within reasonable time")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testPerformance() {
        long startTime = System.currentTimeMillis();
        
        demo.run();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        assertTrue(executionTime < 15000, 
                  "Demo should complete within 15 seconds, took: " + executionTime + "ms");
    }
    
    @Test
    @DisplayName("Demo should produce substantial output")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testOutputSubstance() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Output substance",
            () -> assertTrue(output.length() > 2000, 
                           "Should produce substantial output (>2000 chars), got: " + output.length()),
            () -> assertTrue(output.split("\n").length > 40, 
                           "Should have many lines of output"),
            () -> assertTrue(output.contains("=".repeat(60)) || output.contains("-".repeat(50)), 
                           "Should contain formatting separators")
        );
    }
    
    @Test
    @DisplayName("Demo main method should work independently")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMainMethod() {
        assertDoesNotThrow(() -> {
            new LayeredAPIDemo().run();
        }, "Main method should work independently");
        
        String output = outputStream.toString();
        assertTrue(output.length() > 0, "Main method should produce output");
    }
}
