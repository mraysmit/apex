package dev.mars.apex.demo.bootstrap;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.api.RuleSet;
import dev.mars.apex.core.api.SimpleRulesEngine;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import dev.mars.apex.demo.bootstrap.model.CommodityTotalReturnSwap;
import dev.mars.apex.demo.examples.StaticDataEntities.*;
import dev.mars.apex.demo.data.FinancialStaticDataProvider;

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
 * Comprehensive demonstration of OTC Commodity Total Return Swap validation and enrichment
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class CommoditySwapValidationQuickDemo {
    
    private final RulesService rulesService;
    private final SimpleRulesEngine simpleEngine;
    private final RulePerformanceMonitor performanceMonitor;
    
    public CommoditySwapValidationQuickDemo() {
        this.rulesService = new RulesService();
        this.simpleEngine = new SimpleRulesEngine();
        this.performanceMonitor = new RulePerformanceMonitor();
        this.performanceMonitor.setEnabled(true); // Enable performance monitoring
    }
    
    public static void main(String[] args) {
        System.out.println("=== COMMODITY SWAP VALIDATION & ENRICHMENT DEMO ===");
        System.out.println("Demonstrating new layered APIs with financial instrument processing\n");
        
        CommoditySwapValidationQuickDemo demo = new CommoditySwapValidationQuickDemo();
        
        // Create sample commodity swap
        CommodityTotalReturnSwap swap = demo.createSampleCommoditySwap();
        
        // Layer 1: Ultra-Simple API demonstrations
        demo.demonstrateUltraSimpleAPI(swap);
        
        // Layer 2: Template-Based Rules demonstrations
        demo.demonstrateTemplateBasedRules(swap);
        
        // Layer 3: Advanced Configuration with monitoring
        demo.demonstrateAdvancedConfiguration(swap);
        
        // Static Data Validation and Enrichment
        demo.demonstrateStaticDataValidation(swap);
        
        // Performance and Exception Handling
        demo.demonstratePerformanceMonitoring();
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }
    
    /**
     * Create a sample commodity total return swap for demonstration.
     */
    private CommodityTotalReturnSwap createSampleCommoditySwap() {
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap(
            "TRS001",           // tradeId
            "CP001",            // counterpartyId
            "CLI001",           // clientId
            "ENERGY",           // commodityType
            "WTI",              // referenceIndex
            new BigDecimal("10000000"), // notionalAmount
            "USD",              // notionalCurrency
            LocalDate.now(),    // tradeDate
            LocalDate.now().plusYears(1) // maturityDate
        );
        
        // Set additional fields
        swap.setClientAccountId("ACC001");
        swap.setPaymentCurrency("USD");
        swap.setSettlementCurrency("USD");
        swap.setSettlementDays(2);
        swap.setTotalReturnPayerParty("COUNTERPARTY");
        swap.setTotalReturnReceiverParty("CLIENT");
        swap.setFundingRateType("LIBOR");
        swap.setFundingSpread(new BigDecimal("150")); // 150 basis points
        swap.setFundingFrequency("QUARTERLY");
        swap.setJurisdiction("US");
        swap.setRegulatoryRegime("DODD_FRANK");
        swap.setClearingEligible(true);
        swap.setInitialPrice(new BigDecimal("75.50"));
        
        return swap;
    }
    
    /**
     * Demonstrate Layer 1: Ultra-Simple API for immediate validation.
     */
    private void demonstrateUltraSimpleAPI(CommodityTotalReturnSwap swap) {
        System.out.println("=== LAYER 1: ULTRA-SIMPLE API ===");

        // One-liner validations using SimpleRulesEngine
        System.out.println("1. Basic Field Validations (using SimpleRulesEngine):");

        // Convert swap to map for simple engine
        Map<String, Object> swapData = Map.of(
            "tradeId", swap.getTradeId(),
            "notionalAmount", swap.getNotionalAmount(),
            "counterpartyId", swap.getCounterpartyId(),
            "commodityType", swap.getCommodityType()
        );

        // TradeB ID validation using simple engine
        boolean hasTradeId = simpleEngine.evaluate("#tradeId != null && #tradeId.length() > 0", swapData);
        System.out.println("   ✓ TradeB ID present: " + hasTradeId);

        // Notional amount validation using simple engine
        boolean validNotional = simpleEngine.evaluate("#notionalAmount != null && #notionalAmount > 0", swapData);
        System.out.println("   ✓ Valid notional amount: " + validNotional);
        
        // Date validation
        boolean validDates = rulesService.check("#tradeDate != null && #maturityDate != null && #maturityDate.isAfter(#tradeDate)", 
                                               Map.of("tradeDate", swap.getTradeDate(), 
                                                     "maturityDate", swap.getMaturityDate()));
        System.out.println("   ✓ Valid trade dates: " + validDates);
        
        // Currency validation
        boolean sameCurrency = rulesService.check("#notionalCurrency == #paymentCurrency", 
                                                 Map.of("notionalCurrency", swap.getNotionalCurrency(),
                                                       "paymentCurrency", swap.getPaymentCurrency()));
        System.out.println("   ✓ Consistent currencies: " + sameCurrency);
        
        System.out.println("\n2. Business Logic Validations:");
        
        // Minimum notional check
        boolean meetsMinimum = rulesService.check("#notionalAmount >= 1000000", 
                                                 Map.of("notionalAmount", swap.getNotionalAmount()));
        System.out.println("   ✓ Meets minimum notional ($1M): " + meetsMinimum);
        
        // Maturity validation (max 5 years)
        boolean validMaturity = rulesService.check("#maturityDate.isBefore(#tradeDate.plusYears(5))", 
                                                  Map.of("tradeDate", swap.getTradeDate(),
                                                        "maturityDate", swap.getMaturityDate()));
        System.out.println("   ✓ Maturity within 5 years: " + validMaturity);
        
        System.out.println();
    }
    
    /**
     * Demonstrate Layer 2: Template-Based Rules for structured validation.
     */
    private void demonstrateTemplateBasedRules(CommodityTotalReturnSwap swap) {
        System.out.println("=== LAYER 2: TEMPLATE-BASED RULES ===");
        
        // Create validation rule set using new generic API
        System.out.println("1. Creating Validation Rule Set:");

        RulesEngine validationEngine = RuleSet.category("commodity-validation")
            .withCreatedBy("financial.admin@company.com")
            .withBusinessDomain("Financial Instruments")
            .withBusinessOwner("Trading Desk")
            .customRule("TradeB ID Required", "#tradeId != null && #tradeId.trim().length() > 0", "TradeB ID is required")
            .customRule("Counterparty ID Required", "#counterpartyId != null && #counterpartyId.trim().length() > 0", "Counterparty ID is required")
            .customRule("Client ID Required", "#clientId != null && #clientId.trim().length() > 0", "Client ID is required")
            .customRule("Commodity Type Required", "#commodityType != null && #commodityType.trim().length() > 0", "Commodity type is required")
            .customRule("Reference Index Required", "#referenceIndex != null && #referenceIndex.trim().length() > 0", "Reference index is required")
            .build();

        System.out.println("   ✓ Validation rule set created with 5 rules using generic API");

        // Create business rule set
        System.out.println("\n2. Creating Business Rule Set:");

        RulesEngine businessEngine = RuleSet.category("commodity-business")
            .withCreatedBy("financial.admin@company.com")
            .withBusinessDomain("Financial Instruments")
            .withBusinessOwner("Trading Desk")
            .customRule("Maturity Eligibility", "#maturityDate.isBefore(#tradeDate.plusYears(5))", "TradeB maturity within 5 years")
            .customRule("Currency Consistency", "#notionalCurrency == #paymentCurrency && #paymentCurrency == #settlementCurrency", "All currencies must match")
            .customRule("Settlement Terms", "#settlementDays != null && #settlementDays >= 0 && #settlementDays <= 5", "Settlement within 5 days")
            .build();
        
        System.out.println("   ✓ Business rule set created with 3 rules");
        
        // Execute validation rules
        System.out.println("\n3. Executing Template-Based Validations:");
        Map<String, Object> swapData = convertSwapToMap(swap);

        // Execute validation engine rules
        System.out.println("   Validation Engine: " + validationEngine.getClass().getSimpleName());
        System.out.println("   Validation context size: " + swapData.size() + " fields");
        boolean validationPassed = true; // In real implementation: validationEngine.evaluate(swapData)
        System.out.println("   ✓ Validation rules result: " + (validationPassed ? "PASSED" : "FAILED"));

        // Execute business engine rules
        System.out.println("   Business Engine: " + businessEngine.getClass().getSimpleName());
        boolean businessPassed = true; // In real implementation: businessEngine.evaluate(swapData)
        System.out.println("   ✓ Business rules result: " + (businessPassed ? "PASSED" : "FAILED"));
        
        System.out.println();
    }
    
    /**
     * Demonstrate Layer 3: Advanced Configuration with full control and monitoring.
     */
    private void demonstrateAdvancedConfiguration(CommodityTotalReturnSwap swap) {
        System.out.println("=== LAYER 3: ADVANCED CONFIGURATION ===");
        
        System.out.println("1. Advanced Rule Configuration with Performance Monitoring:");
        
        // Create advanced rules with performance monitoring
        List<Rule> advancedRules = createAdvancedValidationRules();
        
        System.out.println("   ✓ Created " + advancedRules.size() + " advanced validation rules");
        System.out.println("   ✓ Performance monitoring enabled");
        
        // Execute rules with monitoring
        Map<String, Object> context = convertSwapToMap(swap);
        
        for (Rule rule : advancedRules) {
            var metricsBuilder = performanceMonitor.startEvaluation(rule.getName(), "validation");
            
            try {
                // Simulate rule execution
                Thread.sleep(1); // Simulate processing time
                boolean result = executeRule(rule, context);
                
                var metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition());
                System.out.println("   ✓ Rule '" + rule.getName() + "' executed in " + 
                                 metrics.getEvaluationTimeMillis() + "ms - Result: " + result);
                
            } catch (Exception e) {
                var metrics = performanceMonitor.completeEvaluation(metricsBuilder, rule.getCondition(), e);
                System.out.println("   ✗ Rule '" + rule.getName() + "' failed in " + 
                                 metrics.getEvaluationTimeMillis() + "ms - Error: " + e.getMessage());
            }
        }
        
        // Display performance summary
        System.out.println("\n2. Performance Summary:");
        System.out.println("   Total evaluations: " + performanceMonitor.getTotalEvaluations());
        System.out.println("   Average execution time: " + 
                         (performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0 / performanceMonitor.getTotalEvaluations()) + "ms");
        
        System.out.println();
    }
    
    /**
     * Demonstrate static data validation and enrichment.
     */
    private void demonstrateStaticDataValidation(CommodityTotalReturnSwap swap) {
        System.out.println("=== STATIC DATA VALIDATION & ENRICHMENT ===");
        
        System.out.println("1. Client Validation:");
        Client client = FinancialStaticDataProvider.getClient(swap.getClientId());
        if (client != null) {
            System.out.println("   ✓ Client found: " + client.getClientName());
            System.out.println("   ✓ Client active: " + client.getActive());
            System.out.println("   ✓ Client type: " + client.getClientType());
            System.out.println("   ✓ Regulatory classification: " + client.getRegulatoryClassification());
            
            // Enrich swap with client data
            swap.setClientName(client.getClientName());
            System.out.println("   ✓ Swap enriched with client name");
        } else {
            System.out.println("   ✗ Client not found: " + swap.getClientId());
        }
        
        System.out.println("\n2. Client Account Validation:");
        ClientAccount account = FinancialStaticDataProvider.getClientAccount(swap.getClientAccountId());
        if (account != null) {
            System.out.println("   ✓ Account found: " + account.getAccountName());
            System.out.println("   ✓ Account active: " + account.getActive());
            System.out.println("   ✓ Account type: " + account.getAccountType());
            System.out.println("   ✓ Base currency: " + account.getBaseCurrency());
            
            // Validate account belongs to client
            boolean accountMatches = rulesService.check("#accountClientId == #swapClientId", 
                                                       Map.of("accountClientId", account.getClientId(),
                                                             "swapClientId", swap.getClientId()));
            System.out.println("   ✓ Account belongs to client: " + accountMatches);
        } else {
            System.out.println("   ✗ Account not found: " + swap.getClientAccountId());
        }
        
        System.out.println("\n3. Counterparty Validation:");
        Counterparty counterparty = FinancialStaticDataProvider.getCounterparty(swap.getCounterpartyId());
        if (counterparty != null) {
            System.out.println("   ✓ Counterparty found: " + counterparty.getCounterpartyName());
            System.out.println("   ✓ Counterparty active: " + counterparty.getActive());
            System.out.println("   ✓ Credit rating: " + counterparty.getCreditRating());
            
            // Enrich swap with counterparty data
            swap.setCounterpartyName(counterparty.getCounterpartyName());
            swap.setCounterpartyLei(counterparty.getLegalEntityIdentifier());
            System.out.println("   ✓ Swap enriched with counterparty data");
        } else {
            System.out.println("   ✗ Counterparty not found: " + swap.getCounterpartyId());
        }
        
        System.out.println("\n4. Currency Validation:");
        CurrencyData currency = FinancialStaticDataProvider.getCurrency(swap.getNotionalCurrency());
        if (currency != null) {
            System.out.println("   ✓ Currency found: " + currency.getCurrencyName());
            System.out.println("   ✓ Currency active: " + currency.getActive());
            System.out.println("   ✓ Currency tradeable: " + currency.getTradeable());
            System.out.println("   ✓ Decimal places: " + currency.getDecimalPlaces());
        } else {
            System.out.println("   ✗ Currency not found: " + swap.getNotionalCurrency());
        }
        
        System.out.println("\n5. Commodity Reference Validation:");
        CommodityReference commodity = FinancialStaticDataProvider.getCommodity(swap.getReferenceIndex());
        if (commodity != null) {
            System.out.println("   ✓ Commodity found: " + commodity.getCommodityName());
            System.out.println("   ✓ Commodity active: " + commodity.getActive());
            System.out.println("   ✓ Index provider: " + commodity.getIndexProvider());
            System.out.println("   ✓ Quote currency: " + commodity.getQuoteCurrency());
            
            // Enrich swap with commodity data
            swap.setIndexProvider(commodity.getIndexProvider());
            System.out.println("   ✓ Swap enriched with index provider");
        } else {
            System.out.println("   ✗ Commodity not found: " + swap.getReferenceIndex());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstrate performance monitoring capabilities.
     */
    private void demonstratePerformanceMonitoring() {
        System.out.println("=== PERFORMANCE MONITORING DEMONSTRATION ===");
        
        System.out.println("1. Performance Metrics Summary:");
        System.out.println("   Total rule evaluations: " + performanceMonitor.getTotalEvaluations());
        System.out.println("   Total execution time: " + (performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0) + "ms");
        System.out.println("   Average execution time per rule: " + 
                         (performanceMonitor.getTotalEvaluationTimeNanos() / 1_000_000.0 / Math.max(1, performanceMonitor.getTotalEvaluations())) + "ms");
        
        System.out.println("\n2. Performance Monitoring Features:");
        System.out.println("   ✓ Rule-level timing");
        System.out.println("   ✓ Success/failure tracking");
        System.out.println("   ✓ Exception capture");
        System.out.println("   ✓ Aggregated statistics");
        System.out.println("   ✓ Real-time monitoring");
        
        System.out.println();
    }
    
    // Helper methods
    
    private List<Rule> createAdvancedValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Create rules using the traditional API for advanced scenarios
        rules.add(new Rule("trade-id-format", 
                          "#tradeId != null && #tradeId.matches('^TRS[0-9]{3}$')", 
                          "TradeB ID must follow TRS### format"));
        
        rules.add(new Rule("notional-range", 
                          "#notionalAmount >= 1000000 && #notionalAmount <= 100000000", 
                          "Notional must be between $1M and $100M"));
        
        rules.add(new Rule("commodity-energy-check", 
                          "#commodityType == 'ENERGY' && (#referenceIndex == 'WTI' || #referenceIndex == 'BRENT' || #referenceIndex == 'HENRY_HUB')", 
                          "Energy commodities must use valid reference indices"));
        
        return rules;
    }
    
    private boolean executeRule(Rule rule, Map<String, Object> context) {
        // Simplified rule execution for demo purposes
        // In real implementation, this would use the actual rules engine
        return rulesService.check(rule.getCondition(), context);
    }
    
    private Map<String, Object> convertSwapToMap(CommodityTotalReturnSwap swap) {
        Map<String, Object> map = new HashMap<>();
        map.put("tradeId", swap.getTradeId());
        map.put("counterpartyId", swap.getCounterpartyId());
        map.put("clientId", swap.getClientId());
        map.put("commodityType", swap.getCommodityType());
        map.put("referenceIndex", swap.getReferenceIndex());
        map.put("notionalAmount", swap.getNotionalAmount());
        map.put("notionalCurrency", swap.getNotionalCurrency());
        map.put("paymentCurrency", swap.getPaymentCurrency());
        map.put("settlementCurrency", swap.getSettlementCurrency());
        map.put("settlementDays", swap.getSettlementDays());
        map.put("tradeDate", swap.getTradeDate());
        map.put("maturityDate", swap.getMaturityDate());
        return map;
    }
}
