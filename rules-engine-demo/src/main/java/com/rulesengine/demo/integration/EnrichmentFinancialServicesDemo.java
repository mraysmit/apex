package com.rulesengine.demo.integration;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.RulesEngineConfiguration;
import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.transform.EnrichmentService;
import com.rulesengine.core.service.transform.TransformationService;
import com.rulesengine.core.service.validation.ValidationService;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.demo.service.enrichers.*;

import java.util.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Demonstrates various types of enrichment for financial services post-trade settlement.
 * This class showcases the use of EnrichmentService, TransformationService, and ValidationService
 * with a focus on financial services use cases.
 */
public class EnrichmentFinancialServicesDemo {
    private final LookupServiceRegistry registry;
    private final EnrichmentService enrichmentService;
    private final TransformationService transformationService;
    private final ValidationService validationService;
    private final RulesEngine rulesEngine;

    // Sample data
    private final Map<String, Trade> sampleTrades = new HashMap<>();
    private final Map<String, Object> referenceData = new HashMap<>();

    /**
     * Create a new EnrichmentFinancialServicesDemo with the specified services.
     * 
     * @param registry The lookup service registry
     * @param enrichmentService The enrichment service
     * @param transformationService The transformation service
     * @param validationService The validation service
     * @param rulesEngine The rules engine
     */
    public EnrichmentFinancialServicesDemo(
            LookupServiceRegistry registry,
            EnrichmentService enrichmentService,
            TransformationService transformationService,
            ValidationService validationService,
            RulesEngine rulesEngine) {
        this.registry = registry;
        this.enrichmentService = enrichmentService;
        this.transformationService = transformationService;
        this.validationService = validationService;
        this.rulesEngine = rulesEngine;

        // Initialize sample data
        initializeSampleData();
    }

    /**
     * Main method to run the demo.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Create registry
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Create rules engine
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create services
        EnrichmentService enrichmentService = new EnrichmentService(registry, rulesEngine);
        TransformationService transformationService = new TransformationService(registry);
        ValidationService validationService = new ValidationService(registry, rulesEngine);

        // Create demo
        EnrichmentFinancialServicesDemo demo = new EnrichmentFinancialServicesDemo(
            registry,
            enrichmentService,
            transformationService,
            validationService,
            rulesEngine
        );

        // Register enrichers, transformers, and validators
        demo.registerServices();

        // Run demo
        demo.demonstrateFinancialServicesEnrichment();
    }

    /**
     * Initialize sample data for the demo.
     */
    private void initializeSampleData() {
        // Initialize sample trades
        sampleTrades.put("equityTrade", new Trade("T001", "Equity", "InstrumentType"));
        sampleTrades.put("bondTrade", new Trade("T002", "Bond", "InstrumentType"));
        sampleTrades.put("etfTrade", new Trade("T003", "ETF", "InstrumentType"));
        sampleTrades.put("optionTrade", new Trade("T004", "Option", "InstrumentType"));
        sampleTrades.put("futureTrade", new Trade("T005", "Future", "InstrumentType"));
        sampleTrades.put("forexTrade", new Trade("T006", "Currency", "InstrumentType"));
        sampleTrades.put("commodityTrade", new Trade("T007", "Commodity", "InstrumentType"));

        // Initialize reference data

        // 1. Reference Data
        Map<String, String> leiCodes = new HashMap<>();
        leiCodes.put("BANK123", "549300TRUUQMRXENSS54");
        leiCodes.put("BROKER456", "213800MBWEIJDM5CU376");
        leiCodes.put("FUND789", "549300KM49OEQQI21A97");
        referenceData.put("leiCodes", leiCodes);

        Map<String, String> isinCodes = new HashMap<>();
        isinCodes.put("APPLE", "US0378331005");
        isinCodes.put("MICROSOFT", "US5949181045");
        isinCodes.put("AMAZON", "US0231351067");
        referenceData.put("isinCodes", isinCodes);

        Map<String, String> micCodes = new HashMap<>();
        micCodes.put("NYSE", "XNYS");
        micCodes.put("NASDAQ", "XNAS");
        micCodes.put("LSE", "XLON");
        referenceData.put("micCodes", micCodes);

        Map<String, String> bicCodes = new HashMap<>();
        bicCodes.put("BANK123", "BANKUS33XXX");
        bicCodes.put("BROKER456", "BROKGB2LXXX");
        bicCodes.put("FUND789", "FUNDCH22XXX");
        referenceData.put("bicCodes", bicCodes);

        // 2. Counterparty Data
        Map<String, String> creditRatings = new HashMap<>();
        creditRatings.put("BANK123", "AA");
        creditRatings.put("BROKER456", "A-");
        creditRatings.put("FUND789", "BBB+");
        referenceData.put("creditRatings", creditRatings);

        Map<String, String> counterpartyTypes = new HashMap<>();
        counterpartyTypes.put("BANK123", "Bank");
        counterpartyTypes.put("BROKER456", "Broker");
        counterpartyTypes.put("FUND789", "Investment Manager");
        referenceData.put("counterpartyTypes", counterpartyTypes);

        Map<String, String> relationshipTiers = new HashMap<>();
        relationshipTiers.put("BANK123", "Tier 1");
        relationshipTiers.put("BROKER456", "Tier 2");
        relationshipTiers.put("FUND789", "Tier 3");
        referenceData.put("relationshipTiers", relationshipTiers);

        // 3. Regulatory Data
        Map<String, List<String>> regulatoryFlags = new HashMap<>();
        regulatoryFlags.put("Equity", Arrays.asList("MiFID II", "EMIR"));
        regulatoryFlags.put("Bond", Arrays.asList("MiFID II", "SFTR"));
        regulatoryFlags.put("Option", Arrays.asList("MiFID II", "EMIR", "Dodd-Frank"));
        referenceData.put("regulatoryFlags", regulatoryFlags);

        // 4. Risk Data
        Map<String, Double> varMetrics = new HashMap<>();
        varMetrics.put("Equity", 0.15);
        varMetrics.put("Bond", 0.05);
        varMetrics.put("Option", 0.25);
        referenceData.put("varMetrics", varMetrics);

        // 5. Settlement Data
        Map<String, Integer> settlementDays = new HashMap<>();
        settlementDays.put("Equity", 2);
        settlementDays.put("Bond", 1);
        settlementDays.put("Option", 1);
        referenceData.put("settlementDays", settlementDays);

        Map<String, String> settlementMethods = new HashMap<>();
        settlementMethods.put("Equity", "DTC");
        settlementMethods.put("Bond", "Fedwire");
        settlementMethods.put("Option", "Clearstream");
        referenceData.put("settlementMethods", settlementMethods);
    }

    /**
     * Register enrichers, transformers, and validators with the registry.
     */
    private void registerServices() {
        // Register basic trade enricher
        registry.registerService(new TradeEnricher("basicTradeEnricher"));

        // Register specialized enrichers for each category
        registry.registerService(new ReferenceDataEnricher("referenceDataEnricher", referenceData));
        registry.registerService(new CounterpartyEnricher("counterpartyEnricher", referenceData));
        registry.registerService(new RegulatoryEnricher("regulatoryEnricher", referenceData));
        registry.registerService(new RiskEnricher("riskEnricher", referenceData));
        registry.registerService(new SettlementEnricher("settlementEnricher", referenceData));
        registry.registerService(new FeeEnricher("feeEnricher", referenceData));
        registry.registerService(new CorporateActionEnricher("corporateActionEnricher", referenceData));
        registry.registerService(new PricingEnricher("pricingEnricher", referenceData));
        registry.registerService(new ComplianceEnricher("complianceEnricher", referenceData));
        registry.registerService(new OperationalEnricher("operationalEnricher", referenceData));
        registry.registerService(new AccountingEnricher("accountingEnricher", referenceData));
        registry.registerService(new MarketDataEnricher("marketDataEnricher", referenceData));
    }

    /**
     * Demonstrate financial services enrichment.
     */
    public void demonstrateFinancialServicesEnrichment() {
        System.out.println("\n=== Financial Services Enrichment Demo ===");
        System.out.println("This demo showcases various types of enrichment for financial services post-trade settlement.");

        // Demonstrate each category of enrichment
        demonstrateReferenceDataEnrichment();
        demonstrateCounterpartyEnrichment();
        demonstrateRegulatoryEnrichment();
        demonstrateRiskEnrichment();
        demonstrateSettlementEnrichment();
        demonstrateFeeEnrichment();
        demonstrateCorporateActionEnrichment();
        demonstratePricingEnrichment();
        demonstrateComplianceEnrichment();
        demonstrateOperationalEnrichment();
        demonstrateAccountingEnrichment();
        demonstrateMarketDataEnrichment();
    }

    /**
     * Demonstrate reference data enrichment.
     */
    private void demonstrateReferenceDataEnrichment() {
        System.out.println("\n=== 1. Reference Data Enrichment ===");

        // Example 1: LEI Code Enrichment
        System.out.println("\n1.1 LEI Code Enrichment:");
        Trade trade = sampleTrades.get("equityTrade");
        trade.setCategory("BANK123");
        Trade enrichedTrade = enrichmentService.enrich("referenceDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 2: ISIN Code Enrichment
        System.out.println("\n1.2 ISIN Code Enrichment:");
        trade = sampleTrades.get("equityTrade");
        trade.setValue("APPLE");
        enrichedTrade = enrichmentService.enrich("referenceDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 3: MIC Code Enrichment
        System.out.println("\n1.3 MIC Code Enrichment:");
        trade = sampleTrades.get("equityTrade");
        trade.setCategory("NYSE");
        enrichedTrade = enrichmentService.enrich("referenceDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 4: BIC/SWIFT Code Enrichment
        System.out.println("\n1.4 BIC/SWIFT Code Enrichment:");
        trade = sampleTrades.get("equityTrade");
        trade.setCategory("BROKER456");
        enrichedTrade = enrichmentService.enrich("referenceDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 5: Standard Settlement Instructions (SSI) Enrichment
        System.out.println("\n1.5 Standard Settlement Instructions (SSI) Enrichment:");
        trade = sampleTrades.get("bondTrade");
        trade.setCategory("FUND789");
        enrichedTrade = enrichmentService.enrich("referenceDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate counterparty enrichment.
     */
    private void demonstrateCounterpartyEnrichment() {
        System.out.println("\n=== 2. Counterparty Enrichment ===");

        // Example 6: Credit Rating Information
        System.out.println("\n2.1 Credit Rating Information:");
        Trade trade = sampleTrades.get("bondTrade");
        trade.setCategory("BANK123");
        Trade enrichedTrade = enrichmentService.enrich("counterpartyEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 7: Counterparty Classification
        System.out.println("\n2.2 Counterparty Classification:");
        trade = sampleTrades.get("equityTrade");
        trade.setCategory("BROKER456");
        enrichedTrade = enrichmentService.enrich("counterpartyEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 8: Relationship Tier Enrichment
        System.out.println("\n2.3 Relationship Tier Enrichment:");
        trade = sampleTrades.get("etfTrade");
        trade.setCategory("FUND789");
        enrichedTrade = enrichmentService.enrich("counterpartyEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 9: Netting Agreement Status
        System.out.println("\n2.4 Netting Agreement Status:");
        trade = sampleTrades.get("optionTrade");
        trade.setCategory("BANK123");
        enrichedTrade = enrichmentService.enrich("counterpartyEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 10: Default Fund Contribution
        System.out.println("\n2.5 Default Fund Contribution:");
        trade = sampleTrades.get("futureTrade");
        trade.setCategory("BROKER456");
        enrichedTrade = enrichmentService.enrich("counterpartyEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate regulatory enrichment.
     */
    private void demonstrateRegulatoryEnrichment() {
        System.out.println("\n=== 3. Regulatory Enrichment ===");

        // Example 11: Regulatory Reporting Flags
        System.out.println("\n3.1 Regulatory Reporting Flags:");
        Trade trade = sampleTrades.get("equityTrade");
        Trade enrichedTrade = enrichmentService.enrich("regulatoryEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 12: Transaction Reporting Fields
        System.out.println("\n3.2 Transaction Reporting Fields:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("regulatoryEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 13: Unique Transaction Identifier (UTI)
        System.out.println("\n3.3 Unique Transaction Identifier (UTI):");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("regulatoryEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 14: Unique Product Identifier (UPI)
        System.out.println("\n3.4 Unique Product Identifier (UPI):");
        trade = sampleTrades.get("futureTrade");
        enrichedTrade = enrichmentService.enrich("regulatoryEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 15: Legal Documentation Status
        System.out.println("\n3.5 Legal Documentation Status:");
        trade = sampleTrades.get("forexTrade");
        enrichedTrade = enrichmentService.enrich("regulatoryEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate risk enrichment.
     */
    private void demonstrateRiskEnrichment() {
        System.out.println("\n=== 4. Risk Enrichment ===");

        // Example 16: Value-at-Risk (VaR) Metrics
        System.out.println("\n4.1 Value-at-Risk (VaR) Metrics:");
        Trade trade = sampleTrades.get("equityTrade");
        Trade enrichedTrade = enrichmentService.enrich("riskEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 17: Exposure Calculations
        System.out.println("\n4.2 Exposure Calculations:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("riskEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 18: Margin Requirement Enrichment
        System.out.println("\n4.3 Margin Requirement Enrichment:");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("riskEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 19: Collateral Eligibility
        System.out.println("\n4.4 Collateral Eligibility:");
        trade = sampleTrades.get("futureTrade");
        enrichedTrade = enrichmentService.enrich("riskEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 20: Stress Test Results
        System.out.println("\n4.5 Stress Test Results:");
        trade = sampleTrades.get("forexTrade");
        enrichedTrade = enrichmentService.enrich("riskEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate settlement enrichment.
     */
    private void demonstrateSettlementEnrichment() {
        System.out.println("\n=== 5. Settlement Enrichment ===");

        // Example 21: Settlement Date Calculation
        System.out.println("\n5.1 Settlement Date Calculation:");
        Trade trade = sampleTrades.get("equityTrade");
        Trade enrichedTrade = enrichmentService.enrich("settlementEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 22: Settlement Method Determination
        System.out.println("\n5.2 Settlement Method Determination:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("settlementEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 23: Settlement Priority Flags
        System.out.println("\n5.3 Settlement Priority Flags:");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("settlementEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 24: Custodian Information
        System.out.println("\n5.4 Custodian Information:");
        trade = sampleTrades.get("etfTrade");
        enrichedTrade = enrichmentService.enrich("settlementEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 25: Depository Information
        System.out.println("\n5.5 Depository Information:");
        trade = sampleTrades.get("futureTrade");
        enrichedTrade = enrichmentService.enrich("settlementEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate fee and commission enrichment.
     */
    private void demonstrateFeeEnrichment() {
        System.out.println("\n=== 6. Fee and Commission Enrichment ===");

        // Example 26: Broker Commission Calculation
        System.out.println("\n6.1 Broker Commission Calculation:");
        Trade trade = sampleTrades.get("equityTrade");
        Trade enrichedTrade = enrichmentService.enrich("feeEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 27: Clearing Fee Enrichment
        System.out.println("\n6.2 Clearing Fee Enrichment:");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("feeEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 28: Custody Fee Calculation
        System.out.println("\n6.3 Custody Fee Calculation:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("feeEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 29: Transaction Tax Calculation
        System.out.println("\n6.4 Transaction Tax Calculation:");
        trade = sampleTrades.get("equityTrade");
        trade.setCategory("LSE"); // UK stamp duty applies
        enrichedTrade = enrichmentService.enrich("feeEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 30: Exchange Fee Enrichment
        System.out.println("\n6.5 Exchange Fee Enrichment:");
        trade = sampleTrades.get("futureTrade");
        enrichedTrade = enrichmentService.enrich("feeEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate corporate action enrichment.
     */
    private void demonstrateCorporateActionEnrichment() {
        System.out.println("\n=== 7. Corporate Action Enrichment ===");

        // Example 31: Ex-Date Information
        System.out.println("\n7.1 Ex-Date Information:");
        Trade trade = sampleTrades.get("equityTrade");
        trade.setValue("APPLE");
        Trade enrichedTrade = enrichmentService.enrich("corporateActionEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 32: Record Date Information
        System.out.println("\n7.2 Record Date Information:");
        trade = sampleTrades.get("equityTrade");
        trade.setValue("MICROSOFT");
        enrichedTrade = enrichmentService.enrich("corporateActionEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 33: Payment Date Details
        System.out.println("\n7.3 Payment Date Details:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("corporateActionEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 34: Corporate Action Type
        System.out.println("\n7.4 Corporate Action Type:");
        trade = sampleTrades.get("equityTrade");
        trade.setValue("AMAZON");
        enrichedTrade = enrichmentService.enrich("corporateActionEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 35: Entitlement Calculations
        System.out.println("\n7.5 Entitlement Calculations:");
        trade = sampleTrades.get("equityTrade");
        trade.setValue("APPLE");
        enrichedTrade = enrichmentService.enrich("corporateActionEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate pricing and valuation enrichment.
     */
    private void demonstratePricingEnrichment() {
        System.out.println("\n=== 8. Pricing and Valuation Enrichment ===");

        // Example 36: Mark-to-Market Values
        System.out.println("\n8.1 Mark-to-Market Values:");
        Trade trade = sampleTrades.get("equityTrade");
        trade.setValue("APPLE");
        Trade enrichedTrade = enrichmentService.enrich("pricingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 37: Yield Calculations
        System.out.println("\n8.2 Yield Calculations:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("pricingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 38: Accrued Interest Calculation
        System.out.println("\n8.3 Accrued Interest Calculation:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("pricingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 39: Volatility Metrics
        System.out.println("\n8.4 Volatility Metrics:");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("pricingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 40: Price Source Information
        System.out.println("\n8.5 Price Source Information:");
        trade = sampleTrades.get("equityTrade");
        trade.setValue("MICROSOFT");
        enrichedTrade = enrichmentService.enrich("pricingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate compliance enrichment.
     */
    private void demonstrateComplianceEnrichment() {
        System.out.println("\n=== 9. Compliance Enrichment ===");

        // Example 41: AML/KYC Status
        System.out.println("\n9.1 AML/KYC Status:");
        Trade trade = sampleTrades.get("equityTrade");
        trade.setCategory("BANK123");
        Trade enrichedTrade = enrichmentService.enrich("complianceEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 42: Restricted Security Flags
        System.out.println("\n9.2 Restricted Security Flags:");
        trade = sampleTrades.get("equityTrade");
        trade.setValue("APPLE");
        enrichedTrade = enrichmentService.enrich("complianceEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 43: Trading Limits Check
        System.out.println("\n9.3 Trading Limits Check:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("complianceEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 44: Sanctions Screening
        System.out.println("\n9.4 Sanctions Screening:");
        trade = sampleTrades.get("forexTrade");
        enrichedTrade = enrichmentService.enrich("complianceEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 45: Insider Trading Detection
        System.out.println("\n9.5 Insider Trading Detection:");
        trade = sampleTrades.get("equityTrade");
        enrichedTrade = enrichmentService.enrich("complianceEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate operational enrichment.
     */
    private void demonstrateOperationalEnrichment() {
        System.out.println("\n=== 10. Operational Enrichment ===");

        // Example 46: Trade Status Updates
        System.out.println("\n10.1 Trade Status Updates:");
        Trade trade = sampleTrades.get("equityTrade");
        Trade enrichedTrade = enrichmentService.enrich("operationalEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 47: Confirmation Status
        System.out.println("\n10.2 Confirmation Status:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("operationalEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 48: Settlement Instructions
        System.out.println("\n10.3 Settlement Instructions:");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("operationalEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 49: Matching Status
        System.out.println("\n10.4 Matching Status:");
        trade = sampleTrades.get("futureTrade");
        enrichedTrade = enrichmentService.enrich("operationalEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 50: Exception Flags
        System.out.println("\n10.5 Exception Flags:");
        trade = sampleTrades.get("forexTrade");
        enrichedTrade = enrichmentService.enrich("operationalEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate accounting enrichment.
     */
    private void demonstrateAccountingEnrichment() {
        System.out.println("\n=== 11. Accounting Enrichment ===");

        // Example 51: General Ledger Codes
        System.out.println("\n11.1 General Ledger Codes:");
        Trade trade = sampleTrades.get("equityTrade");
        Trade enrichedTrade = enrichmentService.enrich("accountingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 52: Cost Center Allocation
        System.out.println("\n11.2 Cost Center Allocation:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("accountingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 53: P&L Impact Calculation
        System.out.println("\n11.3 P&L Impact Calculation:");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("accountingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 54: Tax Implications
        System.out.println("\n11.4 Tax Implications:");
        trade = sampleTrades.get("futureTrade");
        enrichedTrade = enrichmentService.enrich("accountingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 55: Financial Reporting Category
        System.out.println("\n11.5 Financial Reporting Category:");
        trade = sampleTrades.get("forexTrade");
        enrichedTrade = enrichmentService.enrich("accountingEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }

    /**
     * Demonstrate market data enrichment.
     */
    private void demonstrateMarketDataEnrichment() {
        System.out.println("\n=== 12. Market Data Enrichment ===");

        // Example 56: Current Market Price
        System.out.println("\n12.1 Current Market Price:");
        Trade trade = sampleTrades.get("equityTrade");
        trade.setValue("APPLE");
        Trade enrichedTrade = enrichmentService.enrich("marketDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 57: Historical Volatility
        System.out.println("\n12.2 Historical Volatility:");
        trade = sampleTrades.get("optionTrade");
        enrichedTrade = enrichmentService.enrich("marketDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 58: Yield Curve Data
        System.out.println("\n12.3 Yield Curve Data:");
        trade = sampleTrades.get("bondTrade");
        enrichedTrade = enrichmentService.enrich("marketDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 59: FX Rates
        System.out.println("\n12.4 FX Rates:");
        trade = sampleTrades.get("forexTrade");
        enrichedTrade = enrichmentService.enrich("marketDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);

        // Example 60: Market Liquidity Metrics
        System.out.println("\n12.5 Market Liquidity Metrics:");
        trade = sampleTrades.get("equityTrade");
        trade.setValue("MICROSOFT");
        enrichedTrade = enrichmentService.enrich("marketDataEnricher", trade);
        System.out.println("Original Trade: " + trade);
        System.out.println("Enriched Trade: " + enrichedTrade);
    }
}
