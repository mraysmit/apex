package dev.mars.rulesengine.demo.examples;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.api.ValidationResult;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.demo.examples.StaticDataEntities.*;
import dev.mars.rulesengine.demo.data.FinancialStaticDataProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Financial Services Demo - Demonstrates rules engine capabilities for financial domain.
 * 
 * This demo showcases:
 * - OTC derivatives validation (Commodity Total Return Swaps)
 * - Currency reference data advanced
 * - Counterparty enrichment and validation
 * - Regulatory compliance checks
 * - Risk management rules
 * - Static data validation patterns
 * 
 * Demonstrates real-world financial services use cases with production-ready patterns.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class FinancialServicesDemo {
    
    private final RulesService rulesService;
    private final RulesEngine rulesEngine;
    private final FinancialStaticDataProvider staticDataProvider;
    
    public FinancialServicesDemo() {
        this.rulesService = new RulesService();
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.staticDataProvider = new FinancialStaticDataProvider();
    }
    
    /**
     * Run the complete Financial Services demonstration.
     */
    public void run() {
        System.out.println("SpEL Rules Engine - Financial Services Demo");
        System.out.println("=" .repeat(60));
        System.out.println("Production-ready validation for OTC derivatives trading");
        System.out.println();
        
        demonstrateOTCDerivativesValidation();
        System.out.println();
        
        demonstrateCurrencyReferenceData();
        System.out.println();
        
        demonstrateCounterpartyEnrichment();
        System.out.println();
        
        demonstrateRegulatoryCompliance();
        System.out.println();
        
        demonstrateRiskManagement();
        System.out.println();
        
        demonstrateTradeLifecycle();
        System.out.println();
        
        System.out.println("Financial Services demonstration completed!");
        System.out.println("   Ready for production trading systems!");
    }
    
    /**
     * Demonstrate OTC derivatives validation using Commodity Total Return Swaps.
     */
    private void demonstrateOTCDerivativesValidation() {
        System.out.println("OTC DERIVATIVES VALIDATION");
        System.out.println("-".repeat(50));
        System.out.println("Instrument: Commodity Total Return Swap (TRS)");
        System.out.println();
        
        // Create a sample commodity swap
        CommodityTotalReturnSwap swap = createSampleCommoditySwap();
        
        System.out.println("Trade Details:");
        System.out.println("  Trade ID: " + swap.getTradeId());
        System.out.println("  Counterparty: " + swap.getCounterpartyId());
        System.out.println("  Commodity: " + swap.getCommodityType() + " (" + swap.getReferenceIndex() + ")");
        System.out.println("  Notional: " + swap.getNotionalCurrency() + " " + swap.getNotionalAmount());
        System.out.println("  Maturity: " + swap.getMaturityDate());
        System.out.println();
        
        // Basic validation rules
        System.out.println("// Basic Trade Validation");
        boolean basicValidation = rulesService.check(
            "#data.tradeId != null && #data.notionalAmount > 0 && #data.tradeDate != null",
            swap
        );
        System.out.println("Basic validation: " + (basicValidation ? " PASSED" : " FAILED"));
        
        // Notional amount validation
        boolean notionalCheck = rulesService.check(
            "#data.notionalAmount >= 1000000", // Minimum $1M
            swap
        );
        System.out.println("Minimum notional: " + (notionalCheck ? " PASSED" : " FAILED"));
        
        // Maturity validation
        boolean maturityCheck = rulesService.check(
            "#data.maturityDate.isAfter(#data.tradeDate.plusDays(30))", // Min 30 days
            swap
        );
        System.out.println("Maturity period: " + (maturityCheck ? " PASSED" : " FAILED"));
        
        System.out.println("\n OTC Derivatives Benefits:");
        System.out.println("   • Comprehensive trade structure validation");
        System.out.println("   • Business rule enforcement (notional limits, maturity)");
        System.out.println("   • Integration with static data validation");
        System.out.println("   • Support for complex financial instruments");
    }
    
    /**
     * Demonstrate currency reference data advanced.
     */
    private void demonstrateCurrencyReferenceData() {
        System.out.println(" CURRENCY REFERENCE DATA");
        System.out.println("-".repeat(50));
        
        String[] currencies = {"USD", "EUR", "GBP", "JPY"};
        
        System.out.println(" Currency Validation:");
        for (String currency : currencies) {
            CurrencyData currencyData = FinancialStaticDataProvider.getCurrency(currency);
            boolean isValid = FinancialStaticDataProvider.isValidCurrency(currency);
            
            System.out.println("  " + currency + ": " + (isValid ? "" : "") + 
                             (currencyData != null ? " (" + currencyData.getCurrencyName() + ")" : " (Not found)"));
        }
        System.out.println();
        
        // Currency-specific validation rules
        CommodityTotalReturnSwap swap = createSampleCommoditySwap();
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", swap);
        facts.put("staticDataProvider", FinancialStaticDataProvider.class);
        
        System.out.println("// Currency Business Rules");
        boolean currencyActive = rulesService.check(
            "#staticDataProvider.isValidCurrency(#data.notionalCurrency)",
            facts
        );
        System.out.println("Currency active: " + (currencyActive ? " PASSED" : " FAILED"));
        
        // Currency consistency check
        swap.setSettlementCurrency("EUR"); // Different from notional
        facts.put("data", swap);
        boolean currencyConsistency = rulesService.check(
            "#data.settlementCurrency == null || #data.settlementCurrency == #data.notionalCurrency",
            swap
        );
        System.out.println("Currency consistency: " + (currencyConsistency ? " PASSED" : " FAILED"));
        
        System.out.println("\n Currency Data Benefits:");
        System.out.println("   • Real-time currency validation");
        System.out.println("   • Trading status and decimal place information");
        System.out.println("   • Regional and regulatory compliance data");
        System.out.println("   • Automated currency consistency checks");
    }
    
    /**
     * Demonstrate counterparty enrichment and validation.
     */
    private void demonstrateCounterpartyEnrichment() {
        System.out.println(" COUNTERPARTY ENRICHMENT");
        System.out.println("-".repeat(50));
        
        CommodityTotalReturnSwap swap = createSampleCommoditySwap();
        String counterpartyId = swap.getCounterpartyId();
        
        // Lookup counterparty data
        Counterparty counterparty = FinancialStaticDataProvider.getCounterparty(counterpartyId);
        
        System.out.println(" Counterparty Information:");
        if (counterparty != null) {
            System.out.println("  ID: " + counterparty.getCounterpartyId());
            System.out.println("  Name: " + counterparty.getCounterpartyName());
            System.out.println("  LEI: " + counterparty.getLegalEntityIdentifier());
            System.out.println("  Type: " + counterparty.getCounterpartyType());
            System.out.println("  Credit Rating: " + counterparty.getCreditRating());
            System.out.println("  Status: " + (counterparty.getActive() ? "Active" : "Inactive"));
        } else {
            System.out.println("  Counterparty not found: " + counterpartyId);
        }
        System.out.println();
        
        // Counterparty validation rules
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", swap);
        facts.put("staticDataProvider", FinancialStaticDataProvider.class);
        
        System.out.println("// Counterparty Validation Rules");
        boolean counterpartyActive = rulesService.check(
            "#staticDataProvider.isValidCounterparty(#data.counterpartyId)",
            facts
        );
        System.out.println("Counterparty active: " + (counterpartyActive ? " PASSED" : " FAILED"));
        
        // Credit rating validation
        if (counterparty != null) {
            facts.put("counterparty", counterparty);
            boolean creditRating = rulesService.check(
                "#counterparty.creditRating != null && " +
                "(#counterparty.creditRating.startsWith('A') || #counterparty.creditRating.startsWith('B'))",
                facts
            );
            System.out.println("Credit rating acceptable: " + (creditRating ? " PASSED" : " FAILED"));
        }
        
        System.out.println("\n Counterparty Benefits:");
        System.out.println("   • Automated counterparty validation");
        System.out.println("   • Credit rating and risk assessment");
        System.out.println("   • LEI and regulatory status verification");
        System.out.println("   • Real-time counterparty data enrichment");
    }
    
    /**
     * Demonstrate regulatory compliance checks.
     */
    private void demonstrateRegulatoryCompliance() {
        System.out.println(" REGULATORY COMPLIANCE");
        System.out.println("-".repeat(50));
        
        CommodityTotalReturnSwap swap = createSampleCommoditySwap();
        swap.setJurisdiction("US");
        swap.setRegulatoryRegime("DODD_FRANK");
        swap.setClearingEligible(true);
        
        System.out.println(" Regulatory Setup:");
        System.out.println("  Jurisdiction: " + swap.getJurisdiction());
        System.out.println("  Regulatory Regime: " + swap.getRegulatoryRegime());
        System.out.println("  Clearing Eligible: " + swap.getClearingEligible());
        System.out.println();
        
        System.out.println("// Regulatory Compliance Rules");
        
        // Large notional reporting requirement
        boolean reportingRequired = rulesService.check(
            "#data.regulatoryRegime == 'DODD_FRANK' && #data.notionalAmount > 1000000",
            swap
        );
        System.out.println("Reporting required: " + (reportingRequired ? " YES" : " NO"));
        
        // Clearing requirement
        boolean clearingRequired = rulesService.check(
            "#data.clearingEligible == true && #data.notionalAmount > 50000000",
            swap
        );
        System.out.println("Clearing required: " + (clearingRequired ? " YES" : " NO"));
        
        // Jurisdiction validation
        boolean validJurisdiction = rulesService.check(
            "#data.jurisdiction in {'US', 'UK', 'EU', 'ASIA'}",
            swap
        );
        System.out.println("Valid jurisdiction: " + (validJurisdiction ? " PASSED" : " FAILED"));
        
        System.out.println("\n Regulatory Benefits:");
        System.out.println("   • Automated compliance checking");
        System.out.println("   • Multi-jurisdiction support");
        System.out.println("   • Clearing and reporting requirements");
        System.out.println("   • Regulatory regime-specific rules");
    }
    
    /**
     * Demonstrate risk management rules.
     */
    private void demonstrateRiskManagement() {
        System.out.println(" RISK MANAGEMENT");
        System.out.println("-".repeat(50));
        
        CommodityTotalReturnSwap swap = createSampleCommoditySwap();
        Client client = FinancialStaticDataProvider.getClient(swap.getClientId());
        
        System.out.println(" Risk Assessment:");
        if (client != null) {
            System.out.println("  Client: " + client.getClientName());
            System.out.println("  Risk Rating: " + client.getRiskRating());
            System.out.println("  Credit Limit: " + client.getCreditLimit());
        }
        System.out.println("  Trade Notional: " + swap.getNotionalAmount());
        System.out.println();
        
        Map<String, Object> facts = new HashMap<>();
        facts.put("data", swap);
        facts.put("client", client);
        
        System.out.println("// Risk Management Rules");
        
        // Credit limit check
        if (client != null) {
            boolean creditLimit = rulesService.check(
                "#data.notionalAmount <= #client.creditLimit",
                facts
            );
            System.out.println("Within credit limit: " + (creditLimit ? " PASSED" : " FAILED"));
        }
        
        // High-risk client check
        if (client != null) {
            boolean riskCheck = rulesService.check(
                "#client.riskRating != 'HIGH' || #data.notionalAmount <= 5000000",
                facts
            );
            System.out.println("Risk-adjusted limit: " + (riskCheck ? " PASSED" : " FAILED"));
        }
        
        // Concentration risk
        boolean concentrationRisk = rulesService.check(
            "#data.notionalAmount <= 100000000", // Max $100M per trade
            swap
        );
        System.out.println("Concentration limit: " + (concentrationRisk ? " PASSED" : " FAILED"));
        
        System.out.println("\n Risk Management Benefits:");
        System.out.println("   • Real-time credit limit monitoring");
        System.out.println("   • Risk-adjusted position limits");
        System.out.println("   • Concentration risk controls");
        System.out.println("   • Client-specific risk parameters");
    }
    
    /**
     * Demonstrate complete trade lifecycle validation.
     */
    private void demonstrateTradeLifecycle() {
        System.out.println(" TRADE LIFECYCLE");
        System.out.println("-".repeat(50));
        
        CommodityTotalReturnSwap swap = createSampleCommoditySwap();
        
        System.out.println(" Trade Lifecycle Stages:");
        
        // Stage 1: Pre-trade validation
        System.out.println("\n1. Pre-Trade Validation:");
        ValidationResult preTradeResult = rulesService.validate(swap)
            .that("#data.tradeId != null", "Trade ID required")
            .that("#data.counterpartyId != null", "Counterparty required")
            .that("#data.notionalAmount > 0", "Positive notional required")
            .validate();
        
        System.out.println("   Status: " + (preTradeResult.isValid() ? " READY" : " BLOCKED"));
        
        // Stage 2: Trade execution
        System.out.println("\n2. Trade Execution:");
        swap.setTradeStatus("CONFIRMED");
        boolean executionReady = rulesService.check("#data.tradeStatus == 'CONFIRMED'", swap);
        System.out.println("   Status: " + (executionReady ? " EXECUTED" : " PENDING"));
        
        // Stage 3: Settlement preparation
        System.out.println("\n3. Settlement Preparation:");
        swap.setSettlementDays(2);
        boolean settlementReady = rulesService.check(
            "#data.tradeStatus == 'CONFIRMED' && #data.settlementDays != null",
            swap
        );
        System.out.println("   Status: " + (settlementReady ? " READY" : " PENDING"));
        
        System.out.println("\n Lifecycle Benefits:");
        System.out.println("   • End-to-end trade validation");
        System.out.println("   • Stage-specific business rules");
        System.out.println("   • Automated workflow progression");
        System.out.println("   • Comprehensive audit trail");
    }
    
    /**
     * Create a sample commodity total return swap for demonstrations.
     */
    private CommodityTotalReturnSwap createSampleCommoditySwap() {
        return new CommodityTotalReturnSwap(
            "TRS001",                           // tradeId
            "CP001",                           // counterpartyId  
            "CLI001",                          // clientId
            "ENERGY",                          // commodityType
            "WTI",                             // referenceIndex
            new BigDecimal("10000000"),        // notionalAmount ($10M)
            "USD",                             // notionalCurrency
            LocalDate.now(),                   // tradeDate
            LocalDate.now().plusYears(1)       // maturityDate
        );
    }
    
    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        new FinancialServicesDemo().run();
    }
}
