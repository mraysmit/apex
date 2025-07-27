package dev.mars.rulesengine.demo.simplified;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the SimplifiedAPIDemo.
 * Ensures that the simplified API demonstration runs correctly
 * and showcases all three API layers effectively.
 */
class SimplifiedAPIDemoTest {
    
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @BeforeEach
    void setUp() {
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @DisplayName("Simplified API Demo should run successfully")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSimplifiedAPIDemo() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        }, "Simplified API demo should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Simplified API demo output validation",
            () -> assertTrue(output.contains("SIMPLIFIED APIS DEMONSTRATION"), 
                           "Should display demo title"),
            () -> assertTrue(output.contains("Ultra-Simple API"), 
                           "Should demonstrate ultra-simple API"),
            () -> assertTrue(output.contains("Template-Based Rules"), 
                           "Should demonstrate template-based rules"),
            () -> assertTrue(output.contains("Advanced Configuration"), 
                           "Should demonstrate advanced configuration"),
            () -> assertFalse(output.contains("Exception"), 
                            "Should not contain exception messages"),
            () -> assertFalse(output.contains("Error"), 
                            "Should not contain error messages")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate Ultra-Simple API (Layer 1)")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testUltraSimpleAPI() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Ultra-Simple API demonstration",
            () -> assertTrue(output.contains("Ultra-Simple API") || 
                           output.contains("LAYER 1"), 
                           "Should demonstrate Layer 1 - Ultra-Simple API"),
            () -> assertTrue(output.contains("one-liner") || 
                           output.contains("simple validation") || 
                           output.contains("basic check"), 
                           "Should show simple validation examples"),
            () -> assertTrue(output.contains("✓") || output.contains("passed") || 
                           output.contains("true") || output.contains("valid"), 
                           "Should show successful validation results")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate Template-Based Rules (Layer 2)")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testTemplateBasedRules() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Template-Based Rules demonstration",
            () -> assertTrue(output.contains("Template-Based Rules") || 
                           output.contains("LAYER 2"), 
                           "Should demonstrate Layer 2 - Template-Based Rules"),
            () -> assertTrue(output.contains("template") || 
                           output.contains("rule set") || 
                           output.contains("structured"), 
                           "Should show template-based rule examples"),
            () -> assertTrue(output.contains("validation") || 
                           output.contains("business rule"), 
                           "Should demonstrate rule validation")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate Advanced Configuration (Layer 3)")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testAdvancedConfiguration() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Advanced Configuration demonstration",
            () -> assertTrue(output.contains("Advanced Configuration") || 
                           output.contains("LAYER 3"), 
                           "Should demonstrate Layer 3 - Advanced Configuration"),
            () -> assertTrue(output.contains("advanced") || 
                           output.contains("configuration") || 
                           output.contains("monitoring"), 
                           "Should show advanced configuration features"),
            () -> assertTrue(output.contains("performance") || 
                           output.contains("metrics") || 
                           output.contains("monitoring"), 
                           "Should demonstrate advanced monitoring capabilities")
        );
    }
    
    @Test
    @DisplayName("Demo should show practical examples for each layer")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testPracticalExamples() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Practical examples validation",
            () -> assertTrue(output.contains("example") || 
                           output.contains("Example") || 
                           output.contains("demonstration"), 
                           "Should provide practical examples"),
            () -> assertTrue(output.contains("customer") || 
                           output.contains("product") || 
                           output.contains("trade") || 
                           output.contains("validation"), 
                           "Should demonstrate business domain examples"),
            () -> assertTrue(output.contains("rule") || output.contains("Rule"), 
                           "Should show rule execution examples")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate API progression from simple to advanced")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testAPIProgression() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        // Check that the demo shows progression through the layers
        int layer1Index = output.indexOf("Ultra-Simple API");
        int layer2Index = output.indexOf("Template-Based Rules");
        int layer3Index = output.indexOf("Advanced Configuration");
        
        assertAll("API progression validation",
            () -> assertTrue(layer1Index >= 0, "Should contain Layer 1 content"),
            () -> assertTrue(layer2Index >= 0, "Should contain Layer 2 content"),
            () -> assertTrue(layer3Index >= 0, "Should contain Layer 3 content"),
            () -> assertTrue(layer1Index < layer2Index, 
                           "Layer 1 should come before Layer 2"),
            () -> assertTrue(layer2Index < layer3Index, 
                           "Layer 2 should come before Layer 3")
        );
    }
    
    @Test
    @DisplayName("Demo should handle different validation scenarios")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testValidationScenarios() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Validation scenarios",
            () -> assertTrue(output.contains("validation") || 
                           output.contains("Validation"), 
                           "Should demonstrate validation functionality"),
            () -> assertTrue(output.contains("check") || output.contains("verify"), 
                           "Should show checking/verification operations"),
            () -> assertTrue(output.contains("✓") || output.contains("passed") || 
                           output.contains("success") || output.contains("valid"), 
                           "Should show successful validation results")
        );
    }
    
    @Test
    @DisplayName("Demo should complete successfully with comprehensive output")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testComprehensiveOutput() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Comprehensive output validation",
            () -> assertTrue(output.length() > 500, 
                           "Should produce substantial output"),
            () -> assertFalse(output.toLowerCase().contains("failed"), 
                            "Should not indicate failure"),
            () -> assertFalse(output.toLowerCase().contains("error occurred"), 
                            "Should not indicate errors"),
            () -> assertTrue(output.contains("API") || output.contains("api"), 
                           "Should focus on API demonstration"),
            () -> assertTrue(output.contains("rule") || output.contains("Rule"), 
                           "Should demonstrate rule functionality")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate layered API design philosophy")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testLayeredDesignPhilosophy() {
        assertDoesNotThrow(() -> {
            SimplifiedAPIDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Layered design philosophy",
            () -> assertTrue(output.contains("layer") || output.contains("Layer"), 
                           "Should explain layered approach"),
            () -> assertTrue(output.contains("simple") || output.contains("Simple"), 
                           "Should emphasize simplicity"),
            () -> assertTrue(output.contains("90%") || output.contains("common") || 
                           output.contains("most"), 
                           "Should explain coverage of common use cases"),
            () -> assertTrue(output.contains("power") || output.contains("control") || 
                           output.contains("advanced"), 
                           "Should mention advanced capabilities")
        );
    }
}
