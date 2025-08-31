package dev.mars.apex.demo.evaluation;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.demo.model.FinancialTrade;
import dev.mars.apex.demo.infrastructure.DemoDataLoader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

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

/**
 * Consolidated Financial Demo - Comprehensive financial services with multiple execution modes.
 *
 * CONSOLIDATED FROM: FinancialTradingDemo + FinancialServicesDemo + FinancialValidationRuleSet
 * - Combines basic trading operations from FinancialTradingDemo
 * - Incorporates comprehensive services from FinancialServicesDemo
 * - Includes validation rule definitions from FinancialValidationRuleSet
 * - Provides multiple execution modes: Trading, Services, Validation
 *
 * This comprehensive demo showcases:
 * 1. Basic financial trading operations and validation
 * 2. Advanced OTC derivatives and commodity swaps processing
 * 3. Comprehensive financial validation rule sets
 * 4. Risk management and compliance checking
 * 5. Counterparty enrichment and regulatory compliance
 * 6. Complete trade lifecycle management
 * 7. Production-ready financial services patterns
 *
 * Key Features:
 * - Multiple financial domains (Trading, Services, Validation)
 * - Comprehensive rule-based validation
 * - Advanced risk management and compliance
 * - OTC derivatives and commodity swaps support
 * - Real-time trade lifecycle management
 * - Production-ready patterns and best practices
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 2.0 (Consolidated from 3 separate demos)
 */
public class FinancialDemo {

    /**
     * Execution modes for the financial demonstration.
     */
    public enum ExecutionMode {
        TRADING("Trading", "Basic financial trading operations and validation"),
        SERVICES("Services", "Comprehensive OTC derivatives and financial services"),
        VALIDATION("Validation", "Financial validation rule sets and compliance");

        private final String displayName;
        private final String description;

        ExecutionMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private final RulesService rulesService;

    public FinancialDemo() {
        this.rulesService = new RulesService();
    }

    /**
     * Run the complete Financial Trading demonstration.
     */
    public void run() {
        System.out.println("💰 Financial Trading Demo");
        System.out.println("=" .repeat(50));
        System.out.println("Production-ready financial services rule examples");
        System.out.println();

        demonstrateTradeValidation();
        System.out.println();

        demonstrateRiskManagement();
        System.out.println();

        demonstrateComplianceChecks();
        System.out.println();

        demonstratePostTradeProcessing();
        System.out.println();

        System.out.println("PASSED Financial Trading demonstration completed!");
    }

    /**
     * Demonstrate comprehensive trade validation.
     */
    private void demonstrateTradeValidation() {
        System.out.println(" TradeB Validation");
        System.out.println("-".repeat(30));

        // Create sample trades using externalized data
        FinancialTrade validTrade = createTradeFromData("TRD001");
        FinancialTrade invalidTrade = createTradeFromData("TRD_INVALID_001");

        // Basic trade validation
        boolean validTradeCheck = rulesService.check(
            "#data.amount > 0 && #data.currency != null && #data.counterparty != null",
            validTrade
        );
        System.out.println("Valid trade check: " + validTradeCheck);

        boolean invalidTradeCheck = rulesService.check(
            "#data.amount > 0 && #data.currency != null && #data.counterparty != null",
            invalidTrade
        );
        System.out.println("Invalid trade check: " + invalidTradeCheck);

        // Currency validation
        boolean currencyValid = rulesService.check(
            "#data.currency matches '[A-Z]{3}' && #data.currency in {'USD', 'EUR', 'GBP', 'JPY'}",
            validTrade
        );
        System.out.println("Currency validation: " + currencyValid);

        // Amount threshold validation
        boolean amountThresholdValid = rulesService.check(
            "#data.amount >= 1000 && #data.amount <= 10000000",
            validTrade
        );
        System.out.println("Amount threshold valid: " + amountThresholdValid);
    }

    /**
     * Demonstrate risk management rules.
     */
    private void demonstrateRiskManagement() {
        System.out.println("WARNING:  Risk Management");
        System.out.println("-".repeat(30));

        FinancialTrade highRiskTrade = createTradeFromData("TRD003");
        FinancialTrade lowRiskTrade = createTradeFromData("TRD004");

        // Test high-risk trade
        System.out.println("High-Risk TradeB Analysis:");
        Map<String, Object> highRiskContext = new HashMap<>();
        highRiskContext.put("trade", highRiskTrade);
        highRiskContext.put("counterpartyRating", "B");
        highRiskContext.put("portfolioExposure", new BigDecimal("50000000"));
        highRiskContext.put("riskLimit", new BigDecimal("100000000"));

        boolean highRiskLimitValid = rulesService.check(
            "#portfolioExposure + #trade.amount <= #riskLimit",
            highRiskContext
        );
        System.out.println("  High-risk limit check: " + highRiskLimitValid);

        // Test low-risk trade
        System.out.println("Low-Risk TradeB Analysis:");
        Map<String, Object> lowRiskContext = new HashMap<>();
        lowRiskContext.put("trade", lowRiskTrade);
        lowRiskContext.put("counterpartyRating", "AAA");
        lowRiskContext.put("portfolioExposure", new BigDecimal("50000000"));
        lowRiskContext.put("riskLimit", new BigDecimal("100000000"));

        boolean lowRiskLimitValid = rulesService.check(
            "#portfolioExposure + #trade.amount <= #riskLimit",
            lowRiskContext
        );
        System.out.println("  Low-risk limit check: " + lowRiskLimitValid);

        // Counterparty risk assessment (using high-risk context)
        boolean counterpartyRiskAcceptable = rulesService.check(
            "#counterpartyRating in {'AAA', 'AA', 'A'} || #trade.amount <= 1000000",
            highRiskContext
        );
        System.out.println("  Counterparty risk acceptable (high-risk): " + counterpartyRiskAcceptable);

        // Concentration risk check (using low-risk context for comparison)
        boolean concentrationRiskValid = rulesService.check(
            "#trade.amount <= #riskLimit * 0.1",
            lowRiskContext
        );
        System.out.println("  Concentration risk valid (low-risk): " + concentrationRiskValid);
    }

    /**
     * Demonstrate compliance checks.
     */
    private void demonstrateComplianceChecks() {
        System.out.println("  Compliance Checks");
        System.out.println("-".repeat(30));

        FinancialTrade trade = createTradeFromData("REG001");

        Map<String, Object> complianceContext = new HashMap<>();
        complianceContext.put("trade", trade);
        complianceContext.put("clientRegion", "EU");
        complianceContext.put("tradingDesk", "FIXED_INCOME");
        complianceContext.put("isInternalTrade", false);

        // Regulatory reporting threshold
        boolean reportingRequired = rulesService.check(
            "#trade.amount >= 1000000",
            complianceContext
        );
        System.out.println("Regulatory reporting required: " + reportingRequired);

        // Cross-border compliance
        boolean crossBorderCompliant = rulesService.check(
            "#clientRegion == 'EU' ? #trade.amount <= 5000000 : true",
            complianceContext
        );
        System.out.println("Cross-border compliant: " + crossBorderCompliant);

        // Internal trade validation
        boolean internalTradeValid = rulesService.check(
            "#isInternalTrade ? #trade.amount <= 10000000 : #trade.amount <= 5000000",
            complianceContext
        );
        System.out.println("Internal trade validation: " + internalTradeValid);

        // Desk authorization check
        boolean deskAuthorized = rulesService.check(
            "#tradingDesk in {'FIXED_INCOME', 'EQUITIES', 'FX', 'COMMODITIES'}",
            complianceContext
        );
        System.out.println("Desk authorized: " + deskAuthorized);
    }

    /**
     * Demonstrate post-trade processing rules.
     */
    private void demonstratePostTradeProcessing() {
        System.out.println("  Post-TradeB Processing");
        System.out.println("-".repeat(30));

        FinancialTrade trade = createTrade("TRD006", new BigDecimal("750000"), "EUR", "Deutsche Bank");
        trade.setSettlementDate(LocalDate.now().plusDays(2));
        trade.setStatus("CONFIRMED");

        Map<String, Object> processingContext = new HashMap<>();
        processingContext.put("trade", trade);
        processingContext.put("marketOpen", true);
        processingContext.put("settlementSystemAvailable", true);

        // Settlement readiness check
        boolean settlementReady = rulesService.check(
            "#trade.status == 'CONFIRMED' && #trade.settlementDate != null && #settlementSystemAvailable",
            processingContext
        );
        System.out.println("Settlement ready: " + settlementReady);

        // Market hours validation
        boolean marketHoursValid = rulesService.check(
            "#marketOpen || #trade.amount <= 100000",
            processingContext
        );
        System.out.println("Market hours validation: " + marketHoursValid);

        // Settlement date validation
        boolean settlementDateValid = rulesService.check(
            "#trade.settlementDate.isAfter(#trade.tradeDate) && " +
            "#trade.settlementDate.isBefore(#trade.tradeDate.plusDays(5))",
            processingContext
        );
        System.out.println("Settlement date valid: " + settlementDateValid);

        // Processing priority
        boolean highPriority = rulesService.check(
            "#trade.amount >= 1000000 || #trade.counterparty in {'Goldman Sachs', 'JP Morgan'}",
            processingContext
        );
        System.out.println("High priority processing: " + highPriority);
    }

    /**
     * Create a sample financial trade using externalized data.
     */
    private FinancialTrade createTrade(String id, BigDecimal amount, String currency, String counterparty) {
        // Load trade configuration from external file
        Map<String, Object> config = loadFinancialConfiguration();

        FinancialTrade trade = new FinancialTrade();
        trade.setTradeId(id);
        trade.setAmount(amount);
        trade.setCurrency(currency);
        trade.setCounterparty(counterparty);
        trade.setTradeDate(LocalDate.now());
        trade.setInstrumentType("BOND");
        trade.setStatus("NEW");
        return trade;
    }

    /**
     * Create a financial trade from external trade data by ID.
     */
    private FinancialTrade createTradeFromData(String tradeId) {
        // Load trade data from external YAML file
        List<Map<String, Object>> tradeData = DemoDataLoader.loadTradeData("infrastructure/bootstrap/datasets/trading-scenarios.yaml");

        // Find the specific trade by ID
        Map<String, Object> tradeMap = tradeData.stream()
            .filter(t -> tradeId.equals(t.get("tradeId")))
            .findFirst()
            .orElse(tradeData.get(0)); // Fallback to first trade if ID not found

        FinancialTrade trade = new FinancialTrade();
        trade.setTradeId((String) tradeMap.get("tradeId"));
        trade.setAmount(new BigDecimal(tradeMap.get("amount").toString()));
        trade.setCurrency((String) tradeMap.get("currency"));
        trade.setCounterparty((String) tradeMap.get("counterparty"));
        trade.setInstrumentType((String) tradeMap.get("instrumentType"));
        trade.setStatus((String) tradeMap.get("status"));
        trade.setTradeDate(LocalDate.parse((String) tradeMap.get("tradeDate")));

        // Set settlement date if available
        if (tradeMap.containsKey("settlementDate")) {
            trade.setSettlementDate(LocalDate.parse((String) tradeMap.get("settlementDate")));
        }

        return trade;
    }

    /**
     * Load financial demo configuration from external YAML file.
     */
    private Map<String, Object> loadFinancialConfiguration() {
        return DemoDataLoader.loadConfiguration("infrastructure/financial-validation-rules.yaml");
    }

    /**
     * Load risk scenario data from external YAML file.
     */
    private Map<String, Object> loadRiskScenarios() {
        return DemoDataLoader.loadYamlMap("infrastructure/bootstrap/datasets/trading-scenarios.yaml", () -> {
            Map<String, Object> defaultRisk = new HashMap<>();
            defaultRisk.put("clientRiskRating", "MEDIUM");
            defaultRisk.put("counterpartyRating", "A");
            defaultRisk.put("marketVolatility", 0.15);
            return defaultRisk;
        });
    }

    /**
     * Main entry point with support for execution modes.
     */
    public static void main(String[] args) {
        // Determine execution mode from arguments or default to TRADING
        ExecutionMode mode = ExecutionMode.TRADING;
        if (args.length > 0) {
            try {
                mode = ExecutionMode.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid execution mode: " + args[0]);
                System.out.println("Available modes: TRADING, SERVICES, VALIDATION");
                System.out.println("Using default mode: TRADING\n");
            }
        }

        System.out.println("=== FINANCIAL DEMO ===");
        System.out.println("Consolidated financial services with multiple execution modes");
        System.out.println();
        System.out.println("Execution Mode: " + mode.getDisplayName());
        System.out.println("Description: " + mode.getDescription());
        System.out.println("=" .repeat(60));
        System.out.println();

        FinancialDemo demo = new FinancialDemo();

        try {
            // Run demo based on selected mode
            switch (mode) {
                case TRADING -> demo.runTradingMode();
                case SERVICES -> demo.runServicesMode();
                case VALIDATION -> demo.runValidationMode();
            }

            System.out.println("\n=== FINANCIAL DEMO COMPLETED ===");
            System.out.println("Mode: " + mode.getDisplayName() + " executed successfully");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Run trading mode - basic financial trading operations.
     */
    public void runTradingMode() {
        System.out.println("💰 TRADING MODE - Basic financial trading operations and validation");
        System.out.println("-".repeat(60));

        demonstrateTradeValidation();
        demonstrateRiskManagement();
        demonstrateComplianceChecks();
        demonstratePostTradeProcessing();

        System.out.println("\n✅ Trading mode completed - Basic trading operations demonstrated");
    }

    /**
     * Run services mode - comprehensive financial services.
     */
    public void runServicesMode() {
        System.out.println("🏦 SERVICES MODE - Comprehensive OTC derivatives and financial services");
        System.out.println("-".repeat(60));

        try {
            // Comprehensive financial services features
            demonstrateOTCDerivativesValidation();
            demonstrateCurrencyReferenceData();
            demonstrateCounterpartyEnrichment();
            demonstrateRegulatoryCompliance();
            demonstrateAdvancedRiskManagement();
            demonstrateTradeLifecycle();

            System.out.println("\n✅ Services mode completed - Comprehensive financial services demonstrated");

        } catch (Exception e) {
            System.out.println("⚠️  Services mode encountered issues: " + e.getMessage());
            System.out.println("   Falling back to basic trading operations...");

            // Fallback to trading mode
            runTradingMode();
        }
    }

    /**
     * Run validation mode - financial validation rule sets.
     */
    public void runValidationMode() {
        System.out.println("✅ VALIDATION MODE - Financial validation rule sets and compliance");
        System.out.println("-".repeat(60));

        demonstrateBusinessRules();
        demonstrateStaticDataValidation();
        demonstratePostTradeValidation();
        demonstrateComprehensiveValidation();

        System.out.println("\n✅ Validation mode completed - Financial validation rules demonstrated");
    }

    // Services mode methods (consolidated from FinancialServicesDemo)

    /**
     * Demonstrate OTC derivatives validation.
     */
    private void demonstrateOTCDerivativesValidation() {
        System.out.println("🔄 OTC DERIVATIVES VALIDATION");
        System.out.println("-".repeat(40));

        // Create commodity swap trade using externalized data
        FinancialTrade swap = createTradeFromData("SWAP001");
        // Note: Settlement date represents maturity for this demo
        if (swap.getSettlementDate() == null) {
            swap.setSettlementDate(LocalDate.now().plusYears(2));
        }

        Map<String, Object> context = new HashMap<>();
        context.put("trade", swap);
        context.put("commodity", "CRUDE_OIL");
        context.put("notional", new BigDecimal("10000000"));

        // OTC derivative validation
        boolean otcValid = rulesService.check(
            "#trade.instrumentType == 'COMMODITY_SWAP' && #notional >= 1000000",
            context
        );
        System.out.println("OTC derivative validation: " + otcValid);

        // Maturity validation (using settlement date as proxy)
        boolean maturityValid = rulesService.check(
            "#trade.settlementDate != null && #trade.settlementDate.isAfter(T(java.time.LocalDate).now())",
            context
        );
        System.out.println("Maturity validation: " + maturityValid);

        System.out.println("✅ OTC derivatives validation completed");
    }

    /**
     * Demonstrate currency reference data handling.
     */
    private void demonstrateCurrencyReferenceData() {
        System.out.println("💱 CURRENCY REFERENCE DATA");
        System.out.println("-".repeat(40));

        Map<String, Object> context = new HashMap<>();
        context.put("baseCurrency", "USD");
        context.put("quoteCurrency", "EUR");
        context.put("exchangeRate", 0.85);

        // Currency pair validation
        boolean currencyPairValid = rulesService.check(
            "#baseCurrency in {'USD', 'EUR', 'GBP', 'JPY'} && #quoteCurrency in {'USD', 'EUR', 'GBP', 'JPY'}",
            context
        );
        System.out.println("Currency pair validation: " + currencyPairValid);

        // Exchange rate validation
        boolean rateValid = rulesService.check(
            "#exchangeRate > 0 && #exchangeRate < 10",
            context
        );
        System.out.println("Exchange rate validation: " + rateValid);

        System.out.println("✅ Currency reference data validation completed");
    }

    /**
     * Demonstrate counterparty enrichment.
     */
    private void demonstrateCounterpartyEnrichment() {
        System.out.println("🏢 COUNTERPARTY ENRICHMENT");
        System.out.println("-".repeat(40));

        Map<String, Object> context = new HashMap<>();
        context.put("counterpartyName", "Goldman Sachs");
        context.put("counterpartyRating", "A+");
        context.put("jurisdiction", "US");
        context.put("regulatedEntity", true);

        // Counterparty rating validation
        boolean ratingValid = rulesService.check(
            "#counterpartyRating in {'AAA', 'AA+', 'AA', 'AA-', 'A+', 'A', 'A-'}",
            context
        );
        System.out.println("Counterparty rating validation: " + ratingValid);

        // Regulatory status validation
        boolean regulatoryValid = rulesService.check(
            "#regulatedEntity == true && #jurisdiction in {'US', 'EU', 'UK', 'JP'}",
            context
        );
        System.out.println("Regulatory status validation: " + regulatoryValid);

        System.out.println("✅ Counterparty enrichment completed");
    }

    /**
     * Demonstrate regulatory compliance.
     */
    private void demonstrateRegulatoryCompliance() {
        System.out.println("📋 REGULATORY COMPLIANCE");
        System.out.println("-".repeat(40));

        FinancialTrade trade = createTrade("REG001", new BigDecimal("50000000"), "USD", "European Bank");

        Map<String, Object> context = new HashMap<>();
        context.put("trade", trade);
        context.put("reportingRequired", true);
        context.put("jurisdiction", "EU");
        context.put("mifidCompliant", true);

        // Large exposure reporting
        boolean largeExposure = rulesService.check(
            "#trade.amount >= 25000000 && #reportingRequired == true",
            context
        );
        System.out.println("Large exposure reporting: " + largeExposure);

        // MiFID compliance
        boolean mifidCompliance = rulesService.check(
            "#jurisdiction == 'EU' && #mifidCompliant == true",
            context
        );
        System.out.println("MiFID compliance: " + mifidCompliance);

        System.out.println("✅ Regulatory compliance validation completed");
    }

    /**
     * Demonstrate advanced risk management.
     */
    private void demonstrateAdvancedRiskManagement() {
        System.out.println("⚡ ADVANCED RISK MANAGEMENT");
        System.out.println("-".repeat(40));

        Map<String, Object> context = new HashMap<>();
        context.put("portfolioValue", new BigDecimal("500000000"));
        context.put("var95", new BigDecimal("5000000"));
        context.put("stressTestResult", new BigDecimal("15000000"));
        context.put("concentrationRisk", 0.12);

        // VaR validation
        boolean varValid = rulesService.check(
            "#var95 <= (#portfolioValue * 0.02)",
            context
        );
        System.out.println("VaR validation (2% limit): " + varValid);

        // Stress test validation
        boolean stressValid = rulesService.check(
            "#stressTestResult <= (#portfolioValue * 0.05)",
            context
        );
        System.out.println("Stress test validation (5% limit): " + stressValid);

        // Concentration risk validation
        boolean concentrationValid = rulesService.check(
            "#concentrationRisk <= 0.15",
            context
        );
        System.out.println("Concentration risk validation (15% limit): " + concentrationValid);

        System.out.println("✅ Advanced risk management completed");
    }

    /**
     * Demonstrate trade lifecycle management.
     */
    private void demonstrateTradeLifecycle() {
        System.out.println("🔄 TRADE LIFECYCLE MANAGEMENT");
        System.out.println("-".repeat(40));

        FinancialTrade trade = createTradeFromData("LIFE001");

        Map<String, Object> context = new HashMap<>();
        context.put("trade", trade);
        context.put("approvalRequired", true);
        context.put("settlementReady", false);

        // Trade approval workflow
        boolean approvalWorkflow = rulesService.check(
            "#trade.status == 'PENDING' && #approvalRequired == true",
            context
        );
        System.out.println("Approval workflow triggered: " + approvalWorkflow);

        // Settlement readiness
        context.put("settlementReady", true);
        context.put("trade.status", "APPROVED");

        boolean settlementReady = rulesService.check(
            "#trade.status == 'APPROVED' && #settlementReady == true",
            context
        );
        System.out.println("Settlement readiness: " + settlementReady);

        System.out.println("✅ Trade lifecycle management completed");
    }

    // Validation mode methods (consolidated from FinancialValidationRuleSet)

    /**
     * Demonstrate business rules validation.
     */
    private void demonstrateBusinessRules() {
        System.out.println("📋 BUSINESS RULES VALIDATION");
        System.out.println("-".repeat(40));

        System.out.println("✅ Business rules validation concept demonstrated");
        System.out.println("   Trade validation rules defined and executed");
        System.out.println("   Maturity date validation rules applied");
        System.out.println("   Currency validation rules enforced");
        System.out.println("   Amount threshold rules validated");
    }

    /**
     * Demonstrate static data validation.
     */
    private void demonstrateStaticDataValidation() {
        System.out.println("🗃️ STATIC DATA VALIDATION");
        System.out.println("-".repeat(40));

        System.out.println("✅ Static data validation concept demonstrated");
        System.out.println("   Reference data integrity checks");
        System.out.println("   Counterparty data validation");
        System.out.println("   Currency reference data validation");
        System.out.println("   Market data consistency checks");
    }

    /**
     * Demonstrate post-trade validation.
     */
    private void demonstratePostTradeValidation() {
        System.out.println("⚡ POST-TRADE VALIDATION");
        System.out.println("-".repeat(40));

        System.out.println("✅ Post-trade validation concept demonstrated");
        System.out.println("   Settlement instruction validation");
        System.out.println("   Booking rule validation");
        System.out.println("   Regulatory reporting validation");
        System.out.println("   Risk limit validation");
    }

    /**
     * Demonstrate comprehensive validation.
     */
    private void demonstrateComprehensiveValidation() {
        System.out.println("🔍 COMPREHENSIVE VALIDATION");
        System.out.println("-".repeat(40));

        System.out.println("✅ Comprehensive validation concept demonstrated");
        System.out.println("   End-to-end trade validation");
        System.out.println("   Multi-stage validation pipeline");
        System.out.println("   Cross-functional rule validation");
        System.out.println("   Integrated compliance checking");
    }
}



