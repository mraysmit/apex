package dev.mars.rulesengine.demo.examples.financial.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the CommodityTotalReturnSwap model class.
 * Ensures that the financial instrument model works correctly
 * and supports all required operations for the demo.
 */
class CommodityTotalReturnSwapTest {
    
    private CommodityTotalReturnSwap swap;
    private LocalDate tradeDate;
    private LocalDate maturityDate;
    
    @BeforeEach
    void setUp() {
        tradeDate = LocalDate.now();
        maturityDate = LocalDate.now().plusYears(1);
        
        swap = new CommodityTotalReturnSwap(
            "TRS001",                    // tradeId
            "CP001",                     // counterpartyId
            "CLI001",                    // clientId
            "ENERGY",                    // commodityType
            "WTI",                       // referenceIndex
            new BigDecimal("10000000"),  // notionalAmount
            "USD",                       // notionalCurrency
            tradeDate,                   // tradeDate
            maturityDate                 // maturityDate
        );
    }
    
    @Test
    @DisplayName("Constructor should create swap with all required fields")
    void testConstructorWithAllFields() {
        assertAll("Constructor validation",
            () -> assertEquals("TRS001", swap.getTradeId()),
            () -> assertEquals("CP001", swap.getCounterpartyId()),
            () -> assertEquals("CLI001", swap.getClientId()),
            () -> assertEquals("ENERGY", swap.getCommodityType()),
            () -> assertEquals("WTI", swap.getReferenceIndex()),
            () -> assertEquals(new BigDecimal("10000000"), swap.getNotionalAmount()),
            () -> assertEquals("USD", swap.getNotionalCurrency()),
            () -> assertEquals(tradeDate, swap.getTradeDate()),
            () -> assertEquals(maturityDate, swap.getMaturityDate())
        );
    }
    
    @Test
    @DisplayName("Default constructor should create empty swap")
    void testDefaultConstructor() {
        CommodityTotalReturnSwap emptySwap = new CommodityTotalReturnSwap();
        
        assertAll("Default constructor validation",
            () -> assertNull(emptySwap.getTradeId()),
            () -> assertNull(emptySwap.getCounterpartyId()),
            () -> assertNull(emptySwap.getClientId()),
            () -> assertNull(emptySwap.getCommodityType()),
            () -> assertNull(emptySwap.getReferenceIndex()),
            () -> assertNull(emptySwap.getNotionalAmount()),
            () -> assertNull(emptySwap.getNotionalCurrency()),
            () -> assertNull(emptySwap.getTradeDate()),
            () -> assertNull(emptySwap.getMaturityDate())
        );
    }
    
    @Test
    @DisplayName("Setters should update field values correctly")
    void testSetters() {
        CommodityTotalReturnSwap testSwap = new CommodityTotalReturnSwap();
        
        testSwap.setTradeId("TRS002");
        testSwap.setCounterpartyId("CP002");
        testSwap.setClientId("CLI002");
        testSwap.setCommodityType("METALS");
        testSwap.setReferenceIndex("GOLD");
        testSwap.setNotionalAmount(new BigDecimal("5000000"));
        testSwap.setNotionalCurrency("EUR");
        testSwap.setTradeDate(LocalDate.now().minusDays(1));
        testSwap.setMaturityDate(LocalDate.now().plusMonths(6));
        
        assertAll("Setter validation",
            () -> assertEquals("TRS002", testSwap.getTradeId()),
            () -> assertEquals("CP002", testSwap.getCounterpartyId()),
            () -> assertEquals("CLI002", testSwap.getClientId()),
            () -> assertEquals("METALS", testSwap.getCommodityType()),
            () -> assertEquals("GOLD", testSwap.getReferenceIndex()),
            () -> assertEquals(new BigDecimal("5000000"), testSwap.getNotionalAmount()),
            () -> assertEquals("EUR", testSwap.getNotionalCurrency()),
            () -> assertEquals(LocalDate.now().minusDays(1), testSwap.getTradeDate()),
            () -> assertEquals(LocalDate.now().plusMonths(6), testSwap.getMaturityDate())
        );
    }
    
    @Test
    @DisplayName("Additional fields should be settable and gettable")
    void testAdditionalFields() {
        // Test additional fields that might be set during enrichment
        swap.setExternalTradeId("EXT-TRS-001");
        swap.setEffectiveDate(LocalDate.now().plusDays(2));
        swap.setCounterpartyLei("12345678901234567890");
        swap.setClientAccountId("ACC001");
        swap.setCounterpartyRating("A");
        
        assertAll("Additional fields validation",
            () -> assertEquals("EXT-TRS-001", swap.getExternalTradeId()),
            () -> assertEquals(LocalDate.now().plusDays(2), swap.getEffectiveDate()),
            () -> assertEquals("12345678901234567890", swap.getCounterpartyLei()),
            () -> assertEquals("ACC001", swap.getClientAccountId()),
            () -> assertEquals("A", swap.getCounterpartyRating())
        );
    }
    
    @Test
    @DisplayName("BigDecimal amounts should handle precision correctly")
    void testBigDecimalPrecision() {
        BigDecimal preciseAmount = new BigDecimal("10000000.50");
        swap.setNotionalAmount(preciseAmount);
        
        assertEquals(preciseAmount, swap.getNotionalAmount());
        assertEquals(0, preciseAmount.compareTo(swap.getNotionalAmount()));
    }
    
    @Test
    @DisplayName("Date fields should handle null values")
    void testNullDateHandling() {
        swap.setTradeDate(null);
        swap.setMaturityDate(null);
        swap.setEffectiveDate(null);
        
        assertAll("Null date handling",
            () -> assertNull(swap.getTradeDate()),
            () -> assertNull(swap.getMaturityDate()),
            () -> assertNull(swap.getEffectiveDate())
        );
    }
    
    @Test
    @DisplayName("String fields should handle null and empty values")
    void testStringFieldHandling() {
        swap.setTradeId(null);
        swap.setCounterpartyId("");
        swap.setClientId("   ");
        
        assertAll("String field handling",
            () -> assertNull(swap.getTradeId()),
            () -> assertEquals("", swap.getCounterpartyId()),
            () -> assertEquals("   ", swap.getClientId())
        );
    }
    
    @Test
    @DisplayName("Swap should support typical financial instrument operations")
    void testFinancialInstrumentOperations() {
        // Test that the swap can be used in typical financial operations
        assertAll("Financial instrument operations",
            () -> assertNotNull(swap.getNotionalAmount(), 
                              "Should have notional amount for calculations"),
            () -> assertNotNull(swap.getNotionalCurrency(), 
                              "Should have currency for FX operations"),
            () -> assertNotNull(swap.getTradeDate(), 
                              "Should have trade date for lifecycle management"),
            () -> assertNotNull(swap.getMaturityDate(), 
                              "Should have maturity date for expiry calculations"),
            () -> assertTrue(swap.getMaturityDate().isAfter(swap.getTradeDate()), 
                           "Maturity should be after trade date")
        );
    }
    
    @Test
    @DisplayName("Swap should support validation scenarios")
    void testValidationScenarios() {
        // Test scenarios that would be used in validation rules
        
        // Valid swap scenario
        assertTrue(swap.getNotionalAmount().compareTo(BigDecimal.ZERO) > 0, 
                  "Notional amount should be positive");
        assertTrue(swap.getTradeId() != null && !swap.getTradeId().trim().isEmpty(), 
                  "Trade ID should not be null or empty");
        assertTrue(swap.getMaturityDate().isAfter(swap.getTradeDate()), 
                  "Maturity date should be after trade date");
        
        // Invalid swap scenario
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
    @DisplayName("Swap should support enrichment scenarios")
    void testEnrichmentScenarios() {
        // Test that the swap can be enriched with additional data
        CommodityTotalReturnSwap enrichableSwap = new CommodityTotalReturnSwap();
        enrichableSwap.setTradeId("TRS003");
        enrichableSwap.setCounterpartyId("CP003");
        
        // Simulate enrichment
        enrichableSwap.setCounterpartyLei("98765432109876543210");
        enrichableSwap.setCounterpartyRating("BBB");
        enrichableSwap.setClientAccountId("ACC003");
        
        assertAll("Enrichment validation",
            () -> assertEquals("TRS003", enrichableSwap.getTradeId()),
            () -> assertEquals("CP003", enrichableSwap.getCounterpartyId()),
            () -> assertEquals("98765432109876543210", enrichableSwap.getCounterpartyLei()),
            () -> assertEquals("BBB", enrichableSwap.getCounterpartyRating()),
            () -> assertEquals("ACC003", enrichableSwap.getClientAccountId())
        );
    }
}
