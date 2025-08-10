package dev.mars.apex.demo.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CommoditySwapValidationBootstrap to verify comprehensive commodity swap validation functionality.
 */
public class CommoditySwapValidationBootstrapTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        // Capture System.out for testing
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
    }

    @Test
    void testMainMethodDoesNotThrowException() {
        // Test that the main method can be called without throwing exceptions
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        }, "Main method should execute without throwing exceptions");
    }

    @Test
    void testMainMethodProducesExpectedOutput() {
        // Run the main method
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify key sections of the output
        assertTrue(output.contains("COMMODITY SWAP VALIDATION BOOTSTRAP"),
                  "Output should contain the main title");
        assertTrue(output.contains("Complete end-to-end commodity derivatives validation"),
                  "Output should contain the description");
        assertTrue(output.contains("Phase 1: Initializing"),
                  "Output should contain initialization phase");
        assertTrue(output.contains("Static data loaded"),
                  "Output should contain static data loading");
        assertTrue(output.contains("YAML configuration"),
                  "Output should contain YAML configuration");
        assertTrue(output.contains("Cleanup"),
                  "Output should contain cleanup step");
    }

    @Test
    void testInitializationSteps() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify initialization steps
        assertTrue(output.contains("In-memory simulation mode activated") || output.contains("Database connection established"),
                  "Should establish database connection or use in-memory mode");
        assertTrue(output.contains("Static data loaded") || output.contains("Static data repositories initialized"),
                  "Should load static data");
        assertTrue(output.contains("YAML configuration") && output.contains("loaded"),
                  "Should load YAML configuration");
        assertTrue(output.contains("APEX components initialized") || output.contains("Rules Service initialized"),
                  "Should initialize APEX components");
    }

    @Test
    void testScenarioExecution() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify all scenarios are executed (based on log output)
        assertTrue(output.contains("Scenario 1") && output.contains("completed"),
                  "Should execute Scenario 1");
        assertTrue(output.contains("Scenario 2") && output.contains("completed"),
                  "Should execute Scenario 2");
        assertTrue(output.contains("Scenario 3") && output.contains("completed"),
                  "Should execute Scenario 3");
        assertTrue(output.contains("Scenario 4") && output.contains("completed"),
                  "Should execute Scenario 4");
        assertTrue(output.contains("Scenario 5") && output.contains("completed"),
                  "Should execute Scenario 5");
        assertTrue(output.contains("Scenario 6") && output.contains("completed"),
                  "Should execute Scenario 6");
    }

    @Test
    void testYamlConfigurationLoading() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify YAML configuration loading
        assertTrue(output.contains("Loading YAML configuration"), 
                  "Should start YAML configuration loading");
        assertTrue(output.contains("YAML configuration loaded successfully"), 
                  "Should complete YAML configuration loading");
        assertTrue(output.contains("Configuration ready for rules engine processing"), 
                  "Should confirm configuration is ready");
    }

    @Test
    void testApexComponentsInitialization() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify APEX components initialization
        assertTrue(output.contains("Initializing APEX components"), 
                  "Should start APEX components initialization");
        assertTrue(output.contains("Rules Service initialized"), 
                  "Should initialize Rules Service");
        assertTrue(output.contains("Enrichment Service initialized"), 
                  "Should initialize Enrichment Service");
        assertTrue(output.contains("APEX components initialized successfully"), 
                  "Should complete APEX components initialization");
    }

    @Test
    void testValidationResults() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify validation results are shown
        assertTrue(output.contains("Validation result: PASS") || output.contains("Validation result: FAIL"), 
                  "Should show validation results");
        assertTrue(output.contains("Rules passed:"), 
                  "Should show rules passed count");
        assertTrue(output.contains("Rules failed:"), 
                  "Should show rules failed count");
    }

    @Test
    void testPerformanceMetrics() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify performance metrics are displayed
        assertTrue(output.contains("Processing time:"), 
                  "Should show processing time metrics");
        assertTrue(output.contains("ms"), 
                  "Should show time in milliseconds");
    }

    @Test
    void testFinalMetricsAndCleanup() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify final metrics and cleanup
        assertTrue(output.contains("scenarios executed successfully") || output.contains("Final Metrics"),
                  "Should display final metrics or completion");
        assertTrue(output.contains("Cleanup") || output.contains("Resources cleaned up"),
                  "Should perform cleanup");
        assertTrue(output.contains("COMMODITY SWAP VALIDATION BOOTSTRAP COMPLETED") || output.contains("scenarios executed successfully"),
                  "Should show completion message");
    }

    @Test
    void testSampleDataCreation() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify sample data creation
        assertTrue(output.contains("clients") && output.contains("counterparties") && 
                  output.contains("commodities") && output.contains("currencies"), 
                  "Should create sample data for all entity types");
    }

    @Test
    void testEngineInitialization() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationBootstrap.main(new String[]{});
        });

        String output = outputStream.toString();
        
        // Verify different engine types are initialized
        assertTrue(output.contains("Ultra-Simple Rules Engine initialized"), 
                  "Should initialize Ultra-Simple Rules Engine");
        assertTrue(output.contains("Engine created successfully") || output.contains("Engine created with"), 
                  "Should create validation engines");
    }
}
