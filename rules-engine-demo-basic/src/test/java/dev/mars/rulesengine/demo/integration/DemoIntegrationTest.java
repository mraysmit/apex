package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.demo.ComprehensiveRulesEngineDemo;
import dev.mars.rulesengine.demo.examples.financial.CommoditySwapValidationDemo;
import dev.mars.rulesengine.demo.showcase.PerformanceAndExceptionShowcase;
import dev.mars.rulesengine.demo.simplified.SimplifiedAPIDemo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test suite for the entire rules-engine-demo-basic module.
 * This test ensures that all demo components work together correctly
 * and that the complete demo suite runs without errors.
 */
@Execution(ExecutionMode.SAME_THREAD) // Ensure tests run sequentially to avoid output conflicts
class DemoIntegrationTest {
    
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
    @DisplayName("All individual demo components should run successfully")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testAllIndividualDemos() {
        assertAll("Individual demo components",
            () -> assertDoesNotThrow(() -> CommoditySwapValidationDemo.main(new String[]{}), 
                                   "Financial demo should run without errors"),
            () -> assertDoesNotThrow(() -> SimplifiedAPIDemo.main(new String[]{}), 
                                   "Simplified API demo should run without errors"),
            () -> assertDoesNotThrow(() -> PerformanceAndExceptionShowcase.main(new String[]{}), 
                                   "Performance showcase should run without errors")
        );
    }
    
    @Test
    @DisplayName("Complete demo suite should run all components in sequence")
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void testCompleteDemoSuite() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"complete"});
        }, "Complete demo suite should run without errors");
        
        String output = outputStream.toString();
        
        // Verify all major components were executed
        assertAll("Complete demo suite validation",
            () -> assertTrue(output.contains("Complete demonstration finished"), 
                           "Should indicate successful completion"),
            () -> assertTrue(output.contains("COMMODITY SWAP VALIDATION"), 
                           "Should include financial demo"),
            () -> assertTrue(output.contains("SIMPLIFIED APIS DEMONSTRATION"), 
                           "Should include simplified API demo"),
            () -> assertTrue(output.contains("PERFORMANCE & EXCEPTION HANDLING"), 
                           "Should include performance showcase"),
            () -> assertTrue(output.length() > 5000, 
                           "Should produce substantial output from all demos"),
            () -> assertFalse(output.toLowerCase().contains("error running"), 
                            "Should not contain execution errors")
        );
    }
    
    @Test
    @DisplayName("Demo should handle all supported demo types")
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void testAllSupportedDemoTypes() {
        String[] demoTypes = {"financial", "simplified", "performance", "static", "quickstart"};
        
        for (String demoType : demoTypes) {
            assertDoesNotThrow(() -> {
                // Clear output for each demo
                outputStream.reset();
                ComprehensiveRulesEngineDemo.main(new String[]{demoType});
            }, "Demo type '" + demoType + "' should run without errors");
            
            String output = outputStream.toString();
            assertTrue(output.contains("Running in non-interactive mode: " + demoType), 
                      "Should indicate correct demo type: " + demoType);
        }
    }
    
    @Test
    @DisplayName("Demo output should be comprehensive and informative")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testDemoOutputQuality() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"financial"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Output quality validation",
            () -> assertTrue(output.length() > 1000, 
                           "Should produce substantial output"),
            () -> assertTrue(output.contains("✓") || output.contains("passed") || 
                           output.contains("success"), 
                           "Should show successful operations"),
            () -> assertTrue(output.contains("validation") || output.contains("rule"), 
                           "Should demonstrate core functionality"),
            () -> assertTrue(output.contains("API") || output.contains("layer"), 
                           "Should explain API concepts"),
            () -> assertFalse(output.contains("TODO") || output.contains("FIXME"), 
                            "Should not contain development placeholders")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate rules engine core functionality")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testRulesEngineFunctionality() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"simplified"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Rules engine functionality validation",
            () -> assertTrue(output.contains("rule") || output.contains("Rule"), 
                           "Should demonstrate rule functionality"),
            () -> assertTrue(output.contains("validation") || output.contains("check"), 
                           "Should show validation capabilities"),
            () -> assertTrue(output.contains("expression") || output.contains("condition"), 
                           "Should demonstrate expression evaluation"),
            () -> assertTrue(output.contains("SpEL") || output.contains("Spring Expression"), 
                           "Should reference SpEL technology")
        );
    }
    
    @Test
    @DisplayName("Demo should handle financial domain concepts correctly")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testFinancialDomainConcepts() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"financial"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Financial domain validation",
            () -> assertTrue(output.contains("commodity") || output.contains("Commodity"), 
                           "Should demonstrate commodity concepts"),
            () -> assertTrue(output.contains("swap") || output.contains("Swap"), 
                           "Should demonstrate swap instruments"),
            () -> assertTrue(output.contains("trade") || output.contains("Trade"), 
                           "Should demonstrate trade processing"),
            () -> assertTrue(output.contains("notional") || output.contains("amount"), 
                           "Should handle financial amounts"),
            () -> assertTrue(output.contains("currency") || output.contains("USD"), 
                           "Should handle currency concepts")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate performance and monitoring capabilities")
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void testPerformanceAndMonitoring() {
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"performance"});
        });
        
        String output = outputStream.toString();
        
        assertAll("Performance and monitoring validation",
            () -> assertTrue(output.contains("performance") || output.contains("Performance"), 
                           "Should demonstrate performance features"),
            () -> assertTrue(output.contains("monitoring") || output.contains("Monitoring"), 
                           "Should demonstrate monitoring capabilities"),
            () -> assertTrue(output.contains("ms") || output.contains("time"), 
                           "Should show timing measurements"),
            () -> assertTrue(output.contains("concurrent") || output.contains("thread"), 
                           "Should demonstrate concurrent execution"),
            () -> assertTrue(output.contains("exception") || output.contains("error"), 
                           "Should demonstrate exception handling")
        );
    }
    
    @Test
    @DisplayName("Demo should be suitable for different audiences")
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void testAudienceSuitability() {
        // Test that demos provide appropriate content for different audiences
        
        // Business user focused content (simplified API)
        assertDoesNotThrow(() -> {
            outputStream.reset();
            ComprehensiveRulesEngineDemo.main(new String[]{"simplified"});
        });
        String simplifiedOutput = outputStream.toString();
        
        // Developer focused content (performance)
        assertDoesNotThrow(() -> {
            outputStream.reset();
            ComprehensiveRulesEngineDemo.main(new String[]{"performance"});
        });
        String performanceOutput = outputStream.toString();
        
        assertAll("Audience suitability validation",
            () -> assertTrue(simplifiedOutput.contains("simple") || 
                           simplifiedOutput.contains("easy"), 
                           "Simplified demo should emphasize ease of use"),
            () -> assertTrue(performanceOutput.contains("metrics") || 
                           performanceOutput.contains("monitoring"), 
                           "Performance demo should show technical details"),
            () -> assertTrue(simplifiedOutput.contains("API") || 
                           simplifiedOutput.contains("layer"), 
                           "Simplified demo should explain API concepts"),
            () -> assertTrue(performanceOutput.contains("concurrent") || 
                           performanceOutput.contains("optimization"), 
                           "Performance demo should show advanced features")
        );
    }
    
    @Test
    @DisplayName("Demo should complete within reasonable time limits")
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void testPerformanceCharacteristics() {
        long startTime = System.currentTimeMillis();
        
        assertDoesNotThrow(() -> {
            ComprehensiveRulesEngineDemo.main(new String[]{"complete"});
        });
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Complete demo should finish within 5 minutes
        assertTrue(executionTime < 300000, 
                  "Complete demo should finish within 5 minutes, took: " + executionTime + "ms");
        
        // Should produce meaningful output
        String output = outputStream.toString();
        assertTrue(output.length() > 3000, 
                  "Should produce comprehensive output");
    }
}
