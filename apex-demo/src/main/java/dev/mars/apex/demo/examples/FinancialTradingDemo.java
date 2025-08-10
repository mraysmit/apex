package dev.mars.apex.demo.examples;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.demo.model.FinancialTrade;

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
 * Financial Trading Demo - Consolidated financial services examples.
 *
 * This demo consolidates functionality from:
 * - CommoditySwapValidationDemo
 * - PostTradeProcessingServiceDemo
 * - PricingServiceDemo
 * - RiskManagementService
 * - ComplianceServiceDemo
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class FinancialTradingDemo {

    private final RulesService rulesService;

    public FinancialTradingDemo() {
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
        System.out.println(" Trade Validation");
        System.out.println("-".repeat(30));

        // Create sample trades
        FinancialTrade validTrade = createTrade("TRD001", new BigDecimal("1000000"), "USD", "Goldman Sachs");
        FinancialTrade invalidTrade = createTrade("TRD002", new BigDecimal("-50000"), "XXX", null);

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

        FinancialTrade highRiskTrade = createTrade("TRD003", new BigDecimal("5000000"), "USD", "High Risk Corp");
        FinancialTrade lowRiskTrade = createTrade("TRD004", new BigDecimal("100000"), "USD", "Goldman Sachs");

        // Test high-risk trade
        System.out.println("High-Risk Trade Analysis:");
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
        System.out.println("Low-Risk Trade Analysis:");
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

        FinancialTrade trade = createTrade("TRD005", new BigDecimal("2500000"), "USD", "European Bank");

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
        System.out.println("  Post-Trade Processing");
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
     * Create a sample financial trade.
     */
    private FinancialTrade createTrade(String id, BigDecimal amount, String currency, String counterparty) {
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
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        new FinancialTradingDemo().run();
    }
}



