package dev.mars.rulesengine.demo.examples.financial;

import dev.mars.rulesengine.demo.examples.financial.model.CommodityTotalReturnSwap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the CommoditySwapValidationDemo.
 * Ensures that the financial instrument validation demo runs correctly
 * and demonstrates all expected functionality.
 */
class CommoditySwapValidationDemoTest {
    
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
    @DisplayName("Commodity Swap Validation Demo should run successfully")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCommoditySwapValidationDemo() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationDemo.main(new String[]{});
        }, "Commodity swap validation demo should not throw any exceptions");
        
        String output = outputStream.toString();
        
        assertAll("Commodity swap demo output validation",
            () -> assertTrue(output.contains("COMMODITY SWAP VALIDATION & ENRICHMENT DEMO"), 
                           "Should display demo title"),
            () -> assertTrue(output.contains("LAYER 1: ULTRA-SIMPLE API"), 
                           "Should demonstrate ultra-simple API"),
            () -> assertTrue(output.contains("LAYER 2: TEMPLATE-BASED RULES"), 
                           "Should demonstrate template-based rules"),
            () -> assertTrue(output.contains("LAYER 3: ADVANCED CONFIGURATION"), 
                           "Should demonstrate advanced configuration"),
            () -> assertTrue(output.contains("Static Data Validation"), 
                           "Should demonstrate static data validation"),
            () -> assertTrue(output.contains("Performance Monitoring"), 
                           "Should demonstrate performance monitoring"),
            () -> assertTrue(output.contains("DEMO COMPLETED"), 
                           "Should indicate successful completion"),
            () -> assertFalse(output.contains("Exception"), 
                            "Should not contain exception messages"),
            () -> assertFalse(output.contains("Error"), 
                            "Should not contain error messages")
        );
    }
    
    @Test
    @DisplayName("Demo should validate basic field requirements")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testBasicFieldValidation() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        // Check that basic validations are performed
        assertAll("Basic field validation checks",
            () -> assertTrue(output.contains("Trade ID present"), 
                           "Should validate trade ID presence"),
            () -> assertTrue(output.contains("Valid notional amount"), 
                           "Should validate notional amount"),
            () -> assertTrue(output.contains("Valid maturity date"), 
                           "Should validate maturity date"),
            () -> assertTrue(output.contains("Valid currency"), 
                           "Should validate currency")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate all three API layers")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testAllAPILayers() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        // Verify all three layers are demonstrated
        assertAll("API layers demonstration",
            () -> assertTrue(output.contains("LAYER 1: ULTRA-SIMPLE API"), 
                           "Should demonstrate Layer 1"),
            () -> assertTrue(output.contains("LAYER 2: TEMPLATE-BASED RULES"), 
                           "Should demonstrate Layer 2"),
            () -> assertTrue(output.contains("LAYER 3: ADVANCED CONFIGURATION"), 
                           "Should demonstrate Layer 3")
        );
        
        // Verify specific functionality within each layer
        assertAll("Layer-specific functionality",
            () -> assertTrue(output.contains("One-liner validations") || 
                           output.contains("Basic Field Validations"), 
                           "Layer 1 should show simple validations"),
            () -> assertTrue(output.contains("Template-Based Validations") || 
                           output.contains("rule set"), 
                           "Layer 2 should show template-based rules"),
            () -> assertTrue(output.contains("Advanced Configuration") || 
                           output.contains("monitoring"), 
                           "Layer 3 should show advanced features")
        );
    }
    
    @Test
    @DisplayName("Demo should handle commodity swap creation without errors")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testCommoditySwapCreation() {
        // Test that we can create a commodity swap like the demo does
        assertDoesNotThrow(() -> {
            CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap(
                "TRS001",                    // tradeId
                "CP001",                     // counterpartyId
                "CLI001",                    // clientId
                "ENERGY",                    // commodityType
                "WTI",                       // referenceIndex
                new BigDecimal("10000000"),  // notionalAmount
                "USD",                       // notionalCurrency
                LocalDate.now(),             // tradeDate
                LocalDate.now().plusYears(1) // maturityDate
            );
            
            // Verify the swap was created correctly
            assertNotNull(swap);
            assertEquals("TRS001", swap.getTradeId());
            assertEquals("CP001", swap.getCounterpartyId());
            assertEquals("CLI001", swap.getClientId());
            assertEquals("ENERGY", swap.getCommodityType());
            assertEquals("WTI", swap.getReferenceIndex());
            assertEquals(new BigDecimal("10000000"), swap.getNotionalAmount());
            assertEquals("USD", swap.getNotionalCurrency());
            assertNotNull(swap.getTradeDate());
            assertNotNull(swap.getMaturityDate());
            
        }, "Should be able to create commodity swap without errors");
    }
    
    @Test
    @DisplayName("Demo should demonstrate static data validation")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testStaticDataValidation() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Static data validation checks",
            () -> assertTrue(output.contains("Static Data Validation") || 
                           output.contains("static data"), 
                           "Should demonstrate static data validation"),
            () -> assertTrue(output.contains("counterparty") || 
                           output.contains("Counterparty"), 
                           "Should validate counterparty data"),
            () -> assertTrue(output.contains("currency") || 
                           output.contains("Currency"), 
                           "Should validate currency data")
        );
    }
    
    @Test
    @DisplayName("Demo should demonstrate performance monitoring")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testPerformanceMonitoring() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Performance monitoring checks",
            () -> assertTrue(output.contains("Performance Monitoring") || 
                           output.contains("performance"), 
                           "Should demonstrate performance monitoring"),
            () -> assertTrue(output.contains("monitoring") || 
                           output.contains("metrics"), 
                           "Should show monitoring capabilities")
        );
    }
    
    @Test
    @DisplayName("Demo should complete successfully with proper messaging")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testSuccessfulCompletion() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        assertAll("Successful completion checks",
            () -> assertTrue(output.contains("DEMO COMPLETED") || 
                           output.contains("completed"), 
                           "Should indicate demo completion"),
            () -> assertTrue(output.length() > 1000, 
                           "Should produce substantial output indicating full execution"),
            () -> assertFalse(output.toLowerCase().contains("failed"), 
                            "Should not indicate failure"),
            () -> assertFalse(output.toLowerCase().contains("error occurred"), 
                            "Should not indicate errors occurred")
        );
    }
    
    @Test
    @DisplayName("Demo should handle all validation scenarios")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testValidationScenarios() {
        assertDoesNotThrow(() -> {
            CommoditySwapValidationDemo.main(new String[]{});
        });
        
        String output = outputStream.toString();
        
        // Check for various validation scenarios that should be covered
        assertAll("Validation scenario coverage",
            () -> assertTrue(output.contains("✓") || output.contains("passed") || 
                           output.contains("valid"), 
                           "Should show successful validations"),
            () -> assertTrue(output.contains("validation") || 
                           output.contains("Validation"), 
                           "Should demonstrate validation functionality"),
            () -> assertTrue(output.contains("rule") || output.contains("Rule"), 
                           "Should demonstrate rule execution")
        );
    }
}
