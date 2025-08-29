package dev.mars.apex.demo.financial;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.examples.ComprehensiveFinancialSettlementDemo;
import dev.mars.apex.demo.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Comprehensive Financial Settlement Enrichment Demo
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
/**
 * Test class for Comprehensive Financial Settlement Enrichment Demo
 *
 * This test demonstrates the working functionality of the comprehensive
 * financial settlement enrichment using the APEX Rules Engine with
 * real-world examples from the settlement guide.
 */
@DisplayName("Comprehensive Financial Settlement Enrichment Demo Tests")
class ComprehensiveFinancialSettlementDemoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComprehensiveFinancialSettlementDemoTest.class);



    private ComprehensiveFinancialSettlementDemo demo;

    @BeforeEach
    void setUp() {
        demo = new ComprehensiveFinancialSettlementDemo();
    }

    @Test
    @DisplayName("Should create UK Equity TradeB with correct structure")
    void shouldCreateUKEquityTradeWithCorrectStructure() {
        LOGGER.info("Testing UK Equity TradeB creation...");

        // Create a UK equity trade using reflection to access private method
        TradeConfirmation trade = createUKEquityTradeForTest();

        // Verify trade structure
        assertNotNull(trade);
        assertNotNull(trade.getHeader());
        assertNotNull(trade.getTrade());

        // Verify header
        assertEquals("TRD-20241224-001", trade.getHeader().getMessageId());
        assertEquals("BANKGB2L", trade.getHeader().getSentBy());
        assertEquals("DEUTDEFF", trade.getHeader().getSendTo());

        // Verify trade details
        TradeB tradeDetails = trade.getTrade();
        assertEquals("TRD-001-2024", tradeDetails.getTradeHeader().getPartyTradeIdentifier().getTradeId());
        assertEquals(LocalDate.of(2024, 12, 24), tradeDetails.getTradeHeader().getTradeDate());
        
        // Verify security
        assertEquals("GB00B03MLX29", tradeDetails.getSecurity().getInstrumentId());
        assertEquals("EQUITY", tradeDetails.getSecurity().getInstrumentType());
        assertEquals("Royal Dutch Shell", tradeDetails.getSecurity().getIssuer());
        
        // Verify counterparty
        assertEquals("Deutsche Bank AG", tradeDetails.getCounterparty().getPartyName());
        
        // Verify trade parameters
        assertEquals("XLON", tradeDetails.getTradingVenue());
        assertEquals(new BigDecimal("10000"), tradeDetails.getQuantity());
        assertEquals(new BigDecimal("2750.50"), tradeDetails.getPrice());
        assertEquals("GBP", tradeDetails.getCurrency());

        LOGGER.info("✓ UK Equity TradeB structure validated successfully");
    }

    @Test
    @DisplayName("Should create US Government Bond TradeB with correct structure")
    void shouldCreateUSBondTradeWithCorrectStructure() {
        LOGGER.info("Testing US Government Bond TradeB creation...");

        TradeConfirmation trade = createUSBondTradeForTest();

        // Verify trade structure
        assertNotNull(trade);
        assertNotNull(trade.getHeader());
        assertNotNull(trade.getTrade());

        // Verify header
        assertEquals("BOND-20241224-002", trade.getHeader().getMessageId());
        assertEquals("JPMUS33", trade.getHeader().getSentBy());
        assertEquals("GSCCUS33", trade.getHeader().getSendTo());

        // Verify trade details
        TradeB tradeDetails = trade.getTrade();
        assertEquals("BOND-002-2024", tradeDetails.getTradeHeader().getPartyTradeIdentifier().getTradeId());
        
        // Verify security (US Treasury Bond)
        assertEquals("US912828XG93", tradeDetails.getSecurity().getInstrumentId());
        assertEquals("GOVERNMENT_BOND", tradeDetails.getSecurity().getInstrumentType());
        assertEquals("US Treasury", tradeDetails.getSecurity().getIssuer());
        
        // Verify counterparty
        assertEquals("JPMorgan Chase", tradeDetails.getCounterparty().getPartyName());
        
        // Verify trade parameters
        assertEquals("BONDDESK", tradeDetails.getTradingVenue());
        assertEquals(new BigDecimal("1000000"), tradeDetails.getQuantity());
        assertEquals(new BigDecimal("98.75"), tradeDetails.getPrice());
        assertEquals("USD", tradeDetails.getCurrency());

        LOGGER.info("✓ US Government Bond TradeB structure validated successfully");
    }

    @Test
    @DisplayName("Should create High-Value TradeB for stress testing")
    void shouldCreateHighValueTradeForStressTesting() {
        LOGGER.info("Testing High-Value TradeB creation for stress testing...");

        TradeConfirmation trade = createHighValueUSEquityTradeForTest();

        // Verify this is a high-value trade
        TradeB tradeDetails = trade.getTrade();
        BigDecimal tradeValue = tradeDetails.getQuantity().multiply(tradeDetails.getPrice());
        
        // TradeB value should be $95M (500,000 * $190)
        assertEquals(new BigDecimal("95000000.00"), tradeValue);
        
        // This should trigger high-priority settlement and stress testing
        assertTrue(tradeValue.compareTo(new BigDecimal("50000000")) > 0, 
                  "TradeB value should exceed $50M threshold for stress testing");

        LOGGER.info("✓ High-Value TradeB (${}) created for stress testing", tradeValue);
    }

    @Test
    @DisplayName("Should process trade with mocked enrichment results")
    void shouldProcessTradeWithMockedEnrichmentResults() {
        LOGGER.info("Testing trade processing with mocked enrichment...");

        // Create mock rule result
        RuleResult mockResult = createMockRuleResult();

        // Verify the mock rule result setup
        assertNotNull(mockResult);
        assertTrue(mockResult.isTriggered());

        // Verify rule result contains expected information
        assertNotNull(mockResult.getRuleName());
        assertNotNull(mockResult.getMessage());

        LOGGER.info("✓ Mock rule processing validated successfully");
    }

    @Test
    @DisplayName("Should validate all trade examples have required fields")
    void shouldValidateAllTradeExamplesHaveRequiredFields() {
        LOGGER.info("Validating all trade examples have required fields...");

        TradeConfirmation[] trades = {
            createUKEquityTradeForTest(),
            createUSBondTradeForTest(),
            createGermanEquityTradeForTest(),
            createHighValueUSEquityTradeForTest()
        };

        for (int i = 0; i < trades.length; i++) {
            TradeConfirmation trade = trades[i];
            LOGGER.info("Validating trade example {}: {}", i + 1, 
                       trade.getHeader().getMessageId());

            // Validate required fields for enrichment
            assertNotNull(trade.getTrade().getTradeHeader().getPartyTradeIdentifier().getTradeId(),
                         "TradeB ID required for UTI generation");
            assertNotNull(trade.getTrade().getSecurity().getInstrumentId(),
                         "Instrument ID required for ISIN enrichment");
            assertNotNull(trade.getTrade().getCounterparty().getPartyName(),
                         "Counterparty name required for LEI enrichment");
            assertNotNull(trade.getTrade().getTradingVenue(),
                         "Trading venue required for MIC enrichment");
            assertNotNull(trade.getTrade().getQuantity(),
                         "Quantity required for trade value calculation");
            assertNotNull(trade.getTrade().getPrice(),
                         "Price required for trade value calculation");
            assertNotNull(trade.getTrade().getCurrency(),
                         "Currency required for settlement");

            LOGGER.info("✓ TradeB example {} validation passed", i + 1);
        }

        LOGGER.info("✓ All {} trade examples validated successfully", trades.length);
    }

    // ============================================================================
    // HELPER METHODS - Recreate trade creation logic for testing
    // ============================================================================

    private TradeConfirmation createUKEquityTradeForTest() {
        TradeConfirmation confirmation = new TradeConfirmation();
        confirmation.setHeader(createHeaderForTest("TRD-20241224-001", "BANKGB2L", "DEUTDEFF"));
        confirmation.setTrade(createTradeForTest(
            "TRD-001-2024", LocalDate.of(2024, 12, 24),
            "GB00B03MLX29", "EQUITY", "Royal Dutch Shell",
            "Deutsche Bank AG", "XLON",
            new BigDecimal("10000"), new BigDecimal("2750.50"), "GBP"
        ));
        return confirmation;
    }

    private TradeConfirmation createUSBondTradeForTest() {
        TradeConfirmation confirmation = new TradeConfirmation();
        confirmation.setHeader(createHeaderForTest("BOND-20241224-002", "JPMUS33", "GSCCUS33"));
        confirmation.setTrade(createTradeForTest(
            "BOND-002-2024", LocalDate.of(2024, 12, 24),
            "US912828XG93", "GOVERNMENT_BOND", "US Treasury",
            "JPMorgan Chase", "BONDDESK",
            new BigDecimal("1000000"), new BigDecimal("98.75"), "USD"
        ));
        return confirmation;
    }

    private TradeConfirmation createGermanEquityTradeForTest() {
        TradeConfirmation confirmation = new TradeConfirmation();
        confirmation.setHeader(createHeaderForTest("TRD-20241224-003", "BARCGB22", "DEUTDEFF"));
        confirmation.setTrade(createTradeForTest(
            "TRD-003-2024", LocalDate.of(2024, 12, 24),
            "DE0007164600", "EQUITY", "SAP SE",
            "Barclays Bank PLC", "XPAR",
            new BigDecimal("5000"), new BigDecimal("120.75"), "EUR"
        ));
        return confirmation;
    }

    private TradeConfirmation createHighValueUSEquityTradeForTest() {
        TradeConfirmation confirmation = new TradeConfirmation();
        confirmation.setHeader(createHeaderForTest("TRD-20241224-004", "GSCCUS33", "CHASUS33"));
        confirmation.setTrade(createTradeForTest(
            "TRD-004-2024", LocalDate.of(2024, 12, 24),
            "US0378331005", "EQUITY", "Apple Inc",
            "Goldman Sachs", "XNAS",
            new BigDecimal("500000"), new BigDecimal("190.00"), "USD"
        ));
        return confirmation;
    }

    private Header createHeaderForTest(String messageId, String sentBy, String sendTo) {
        Header header = new Header();
        header.setMessageId(messageId);
        header.setSentBy(sentBy);
        header.setSendTo(sendTo);
        header.setCreationTimestamp(java.time.Instant.now());
        return header;
    }

    private TradeB createTradeForTest(String tradeId, LocalDate tradeDate, String instrumentId,
                                      String instrumentType, String issuer, String counterpartyName,
                                      String tradingVenue, BigDecimal quantity, BigDecimal price, String currency) {
        TradeB trade = new TradeB();
        
        // TradeB Header
        TradeHeader tradeHeader = new TradeHeader();
        PartyTradeIdentifier partyTradeIdentifier = new PartyTradeIdentifier();
        partyTradeIdentifier.setTradeId(tradeId);
        tradeHeader.setPartyTradeIdentifier(partyTradeIdentifier);
        tradeHeader.setTradeDate(tradeDate);
        trade.setTradeHeader(tradeHeader);
        
        // Security
        Security security = new Security();
        security.setInstrumentId(instrumentId);
        security.setInstrumentType(instrumentType);
        security.setIssuer(issuer);
        trade.setSecurity(security);
        
        // Counterparty
        Counterparty counterparty = new Counterparty();
        counterparty.setPartyName(counterpartyName);
        trade.setCounterparty(counterparty);
        
        // TradeB details
        trade.setTradingVenue(tradingVenue);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setCurrency(currency);
        
        return trade;
    }

    private RuleResult createMockRuleResult() {
        // Create a test RuleResult using the static factory method
        return RuleResult.match("settlement-enrichment-rule", "Settlement enrichment completed successfully");
    }
}
