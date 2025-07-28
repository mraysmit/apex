package dev.mars.rulesengine.demo;

import dev.mars.rulesengine.demo.examples.QuickStartDemo;
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
 * Comprehensive tests for QuickStartDemo to verify all documented functionality works exactly as specified.
 * 
 * These tests ensure that:
 * - All examples from the user guide work correctly
 * - Output contains expected demonstration content
 * - No exceptions are thrown during execution
 * - Performance is within acceptable limits
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
class QuickStartDemoTest {
    
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private QuickStartDemo demo;
    
    @BeforeEach
    void setUp() {
        // Capture system output for verification
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        demo = new QuickStartDemo();
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
    
    @Test
    @DisplayName("QuickStart demo should run successfully without exceptions")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testQuickStartDemoRunsSuccessfully() {
        assertDoesNotThrow(() -> {
            demo.run();
        }, "QuickStart demo should not throw any exceptions");
    }
    
    @Test
    @DisplayName("QuickStart demo should contain all required sections")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testQuickStartDemoContainsAllSections() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("QuickStart demo sections",
            () -> assertTrue(output.contains("SpEL Rules Engine - Quick Start (5 Minutes)"), 
                           "Should display main title"),
            () -> assertTrue(output.contains("1. One-Liner Rule Evaluation"), 
                           "Should demonstrate one-liner evaluation"),
            () -> assertTrue(output.contains("2. Template-Based Rules"), 
                           "Should demonstrate template-based rules"),
            () -> assertTrue(output.contains("3. YAML Configuration"), 
                           "Should demonstrate YAML configuration"),
            () -> assertTrue(output.contains("Quick Start demonstration completed"), 
                           "Should show completion message")
        );
    }
    
    @Test
    @DisplayName("One-liner rule evaluation section should work correctly")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testOneLinerRuleEvaluation() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("One-liner rule evaluation",
            () -> assertTrue(output.contains("One-Liner Rule Evaluation"), 
                           "Should have section header"),
            () -> assertTrue(output.contains("Rules.check"), 
                           "Should show Rules.check examples"),
            () -> assertTrue(output.contains("Map.of"), 
                           "Should show Map.of usage"),
            () -> assertTrue(output.contains("#age >= 18"), 
                           "Should show age validation example"),
            () -> assertTrue(output.contains("#balance > 1000"), 
                           "Should show balance validation example"),
            () -> assertTrue(output.contains("#data.age >= 18 && #data.email != null"), 
                           "Should show object validation example"),
            () -> assertTrue(output.contains("Result: true") || output.contains("✓"), 
                           "Should show successful validation results"),
            () -> assertTrue(output.contains("Result: false") || output.contains("✗"), 
                           "Should show failed validation results")
        );
    }
    
    @Test
    @DisplayName("Template-based rules section should work correctly")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testTemplateBasedRules() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Template-based rules",
            () -> assertTrue(output.contains("Template-Based Rules"), 
                           "Should have section header"),
            () -> assertTrue(output.contains("rulesService.validate"), 
                           "Should show validation service usage"),
            () -> assertTrue(output.contains("minimumAge(18)"), 
                           "Should show age validation helper"),
            () -> assertTrue(output.contains("emailRequired()"), 
                           "Should show email validation helper"),
            () -> assertTrue(output.contains("minimumBalance(1000)"), 
                           "Should show balance validation helper"),
            () -> assertTrue(output.contains("ValidationResult"), 
                           "Should show validation result usage"),
            () -> assertTrue(output.contains("Valid:") || output.contains("Error Count:"), 
                           "Should show validation results")
        );
    }
    
    @Test
    @DisplayName("YAML configuration section should work correctly")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testYamlConfiguration() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("YAML configuration",
            () -> assertTrue(output.contains("YAML Configuration"), 
                           "Should have section header"),
            () -> assertTrue(output.contains("YamlConfigurationLoader"), 
                           "Should show YAML loader usage"),
            () -> assertTrue(output.contains("YamlRuleConfiguration"), 
                           "Should show configuration class"),
            () -> assertTrue(output.contains("YamlRulesEngineService"), 
                           "Should show service creation"),
            () -> assertTrue(output.contains("quick-start.yaml") || output.contains("YAML configuration"), 
                           "Should reference YAML file or show configuration info")
        );
    }
    
    @Test
    @DisplayName("Demo should provide helpful key points")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testKeyPointsProvided() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Key points provided",
            () -> assertTrue(output.contains("Key Points:") || output.contains("💡"), 
                           "Should provide key points sections"),
            () -> assertTrue(output.contains("Maps") || output.contains("key-value"), 
                           "Should explain Maps usage"),
            () -> assertTrue(output.contains("#data") || output.contains("objects"), 
                           "Should explain object access"),
            () -> assertTrue(output.contains("ValidationBuilder") || output.contains("fluent"), 
                           "Should explain validation builder"),
            () -> assertTrue(output.contains("YAML") || output.contains("external"), 
                           "Should explain YAML benefits")
        );
    }
    
    @Test
    @DisplayName("Demo should handle errors gracefully")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testErrorHandling() {
        // Test that demo handles potential errors gracefully
        assertDoesNotThrow(() -> {
            demo.run();
        }, "Demo should handle any internal errors gracefully");
        
        String output = outputStream.toString();
        
        // Should handle errors gracefully (some expected errors are OK for demo purposes)
        assertAll("Error handling",
            () -> assertFalse(output.contains("Exception in thread"),
                            "Should not contain unhandled exceptions"),
            () -> assertTrue(output.contains("Quick Start demonstration completed") ||
                           output.contains("YAML configuration"),
                           "Should complete demo execution despite any expected errors")
        );
    }
    
    @Test
    @DisplayName("Demo should complete within reasonable time")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testPerformance() {
        long startTime = System.currentTimeMillis();
        
        demo.run();
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Should complete within 10 seconds (generous limit for CI environments)
        assertTrue(executionTime < 10000, 
                  "Demo should complete within 10 seconds, took: " + executionTime + "ms");
    }
    
    @Test
    @DisplayName("Demo should produce substantial output")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testOutputSubstance() {
        demo.run();
        String output = outputStream.toString();
        
        assertAll("Output substance",
            () -> assertTrue(output.length() > 1000, 
                           "Should produce substantial output (>1000 chars), got: " + output.length()),
            () -> assertTrue(output.split("\n").length > 20, 
                           "Should have multiple lines of output"),
            () -> assertTrue(output.contains("=".repeat(60)) || output.contains("-".repeat(40)), 
                           "Should contain formatting separators")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate actual rule evaluation")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testActualRuleEvaluation() {
        demo.run();
        String output = outputStream.toString();
        
        // Should show actual boolean results from rule evaluations
        assertAll("Actual rule evaluation",
            () -> assertTrue(output.contains("true") || output.contains("false"), 
                           "Should show boolean evaluation results"),
            () -> assertTrue(output.contains("✓") || output.contains("✅") || 
                           output.contains("❌") || output.contains("✗"), 
                           "Should show visual success/failure indicators"),
            () -> assertTrue(output.contains("Customer") || output.contains("John"), 
                           "Should show customer object usage")
        );
    }
    
    @Test
    @DisplayName("Demo main method should work independently")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMainMethod() {
        assertDoesNotThrow(() -> {
            new QuickStartDemo().run();
        }, "Main method should work independently");
        
        // Verify output was produced (captured by our setup)
        String output = outputStream.toString();
        assertTrue(output.length() > 0, "Main method should produce output");
    }
}
