package dev.mars.rulesengine.demo;

import dev.mars.rulesengine.demo.examples.financial.model.CommodityTotalReturnSwap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic functionality test for the rules-engine-demo-basic module.
 * This test focuses on core functionality that doesn't require external dependencies
 * and ensures the basic demo components are working correctly.
 */
public class BasicFunctionalityTest {
    
    private CommodityTotalReturnSwap swap;
    
    @BeforeEach
    void setUp() {
        swap = new CommodityTotalReturnSwap(
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
    }
    
    @Test
    @DisplayName("Demo module should have all required model classes")
    void testModelClassesExist() {
        assertAll("Model classes should exist and be instantiable",
            () -> assertNotNull(new CommodityTotalReturnSwap(), 
                              "CommodityTotalReturnSwap should be instantiable"),
            () -> assertNotNull(swap, 
                              "CommodityTotalReturnSwap with parameters should be instantiable")
        );
    }
    
    @Test
    @DisplayName("CommodityTotalReturnSwap should support basic operations")
    void testCommoditySwapBasicOperations() {
        assertAll("Basic swap operations",
            () -> assertEquals("TRS001", swap.getTradeId()),
            () -> assertEquals("CP001", swap.getCounterpartyId()),
            () -> assertEquals("CLI001", swap.getClientId()),
            () -> assertEquals("ENERGY", swap.getCommodityType()),
            () -> assertEquals("WTI", swap.getReferenceIndex()),
            () -> assertEquals(new BigDecimal("10000000"), swap.getNotionalAmount()),
            () -> assertEquals("USD", swap.getNotionalCurrency()),
            () -> assertNotNull(swap.getTradeDate()),
            () -> assertNotNull(swap.getMaturityDate())
        );
    }
    
    @Test
    @DisplayName("CommodityTotalReturnSwap should support field updates")
    void testCommoditySwapFieldUpdates() {
        // Test that all setters work correctly
        swap.setTradeId("TRS002");
        swap.setCounterpartyId("CP002");
        swap.setClientId("CLI002");
        swap.setCommodityType("METALS");
        swap.setReferenceIndex("GOLD");
        swap.setNotionalAmount(new BigDecimal("5000000"));
        swap.setNotionalCurrency("EUR");
        swap.setCounterpartyRating("AAA");
        
        assertAll("Field updates",
            () -> assertEquals("TRS002", swap.getTradeId()),
            () -> assertEquals("CP002", swap.getCounterpartyId()),
            () -> assertEquals("CLI002", swap.getClientId()),
            () -> assertEquals("METALS", swap.getCommodityType()),
            () -> assertEquals("GOLD", swap.getReferenceIndex()),
            () -> assertEquals(new BigDecimal("5000000"), swap.getNotionalAmount()),
            () -> assertEquals("EUR", swap.getNotionalCurrency()),
            () -> assertEquals("AAA", swap.getCounterpartyRating())
        );
    }
    
    @Test
    @DisplayName("CommodityTotalReturnSwap should support validation scenarios")
    void testValidationScenarios() {
        // Test valid swap
        assertTrue(swap.getNotionalAmount().compareTo(BigDecimal.ZERO) > 0, 
                  "Valid swap should have positive notional amount");
        assertNotNull(swap.getTradeId(), 
                     "Valid swap should have trade ID");
        assertTrue(swap.getMaturityDate().isAfter(swap.getTradeDate()), 
                  "Valid swap should have maturity after trade date");
        
        // Test invalid scenarios
        CommodityTotalReturnSwap invalidSwap = new CommodityTotalReturnSwap();
        invalidSwap.setNotionalAmount(new BigDecimal("-1000000"));
        invalidSwap.setTradeDate(LocalDate.now());
        invalidSwap.setMaturityDate(LocalDate.now().minusDays(1));
        
        assertTrue(invalidSwap.getNotionalAmount().compareTo(BigDecimal.ZERO) < 0, 
                  "Invalid swap should have negative amount");
        assertTrue(invalidSwap.getMaturityDate().isBefore(invalidSwap.getTradeDate()), 
                  "Invalid swap should have maturity before trade date");
    }
    
    @Test
    @DisplayName("CommodityTotalReturnSwap should support enrichment fields")
    void testEnrichmentFields() {
        // Test enrichment-related fields
        swap.setCounterpartyLei("12345678901234567890");
        swap.setCounterpartyRating("BBB");
        swap.setClientAccountId("ACC001");
        swap.setExternalTradeId("EXT-001");
        
        assertAll("Enrichment fields",
            () -> assertEquals("12345678901234567890", swap.getCounterpartyLei()),
            () -> assertEquals("BBB", swap.getCounterpartyRating()),
            () -> assertEquals("ACC001", swap.getClientAccountId()),
            () -> assertEquals("EXT-001", swap.getExternalTradeId())
        );
    }
    
    @Test
    @DisplayName("Demo classes should be accessible")
    void testDemoClassesAccessible() {
        // Test that main demo classes can be loaded (without running them)
        assertAll("Demo classes should be accessible",
            () -> assertDoesNotThrow(() -> 
                Class.forName("dev.mars.rulesengine.demo.ComprehensiveRulesEngineDemo"),
                "ComprehensiveRulesEngineDemo should be loadable"),
            () -> assertDoesNotThrow(() -> 
                Class.forName("dev.mars.rulesengine.demo.examples.financial.CommoditySwapValidationDemo"),
                "CommoditySwapValidationDemo should be loadable"),
            () -> assertDoesNotThrow(() -> 
                Class.forName("dev.mars.rulesengine.demo.simplified.SimplifiedAPIDemo"),
                "SimplifiedAPIDemo should be loadable"),
            () -> assertDoesNotThrow(() -> 
                Class.forName("dev.mars.rulesengine.demo.showcase.PerformanceAndExceptionShowcase"),
                "PerformanceAndExceptionShowcase should be loadable")
        );
    }
    
    @Test
    @DisplayName("Demo module should have proper package structure")
    void testPackageStructure() {
        // Verify that the demo follows the expected package structure
        String packageName = swap.getClass().getPackage().getName();
        assertTrue(packageName.startsWith("dev.mars.rulesengine.demo"), 
                  "Model classes should be in demo package structure");
        
        assertTrue(packageName.contains("financial"), 
                  "Financial models should be in financial package");
        
        assertTrue(packageName.contains("model"), 
                  "Model classes should be in model package");
    }
    
    @Test
    @DisplayName("CommodityTotalReturnSwap should have proper toString implementation")
    void testToStringImplementation() {
        String swapString = swap.toString();
        
        assertAll("ToString implementation",
            () -> assertNotNull(swapString, "toString should not return null"),
            () -> assertTrue(swapString.contains("TRS001"), "toString should contain trade ID"),
            () -> assertTrue(swapString.contains("CP001"), "toString should contain counterparty ID"),
            () -> assertTrue(swapString.contains("ENERGY"), "toString should contain commodity type"),
            () -> assertTrue(swapString.length() > 50, "toString should be comprehensive")
        );
    }
    
    @Test
    @DisplayName("CommodityTotalReturnSwap should have proper equals and hashCode")
    void testEqualsAndHashCode() {
        CommodityTotalReturnSwap swap1 = new CommodityTotalReturnSwap();
        swap1.setTradeId("TRS001");
        
        CommodityTotalReturnSwap swap2 = new CommodityTotalReturnSwap();
        swap2.setTradeId("TRS001");
        
        CommodityTotalReturnSwap swap3 = new CommodityTotalReturnSwap();
        swap3.setTradeId("TRS002");
        
        assertAll("Equals and hashCode implementation",
            () -> assertEquals(swap1, swap2, "Swaps with same trade ID should be equal"),
            () -> assertNotEquals(swap1, swap3, "Swaps with different trade IDs should not be equal"),
            () -> assertEquals(swap1.hashCode(), swap2.hashCode(), 
                             "Equal swaps should have same hash code"),
            () -> assertNotEquals(swap1.hashCode(), swap3.hashCode(), 
                                "Different swaps should have different hash codes")
        );
    }
}
