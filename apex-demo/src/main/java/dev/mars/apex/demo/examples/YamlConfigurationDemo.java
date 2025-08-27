package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.demo.bootstrap.model.CommodityTotalReturnSwap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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
 * Demonstration of using YAML configuration to externalize rules and enrichments.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Demonstration of using YAML configuration to externalize rules and enrichments.
 * This class shows how business users can modify rules without code changes.
 */
public class YamlConfigurationDemo {
    
    private static final Logger LOGGER = Logger.getLogger(YamlConfigurationDemo.class.getName());
    
    private final YamlRulesEngineService yamlService;
    
    public YamlConfigurationDemo() {
        this.yamlService = new YamlRulesEngineService();
    }
    
    public static void main(String[] args) {
        System.out.println("=== YAML CONFIGURATION DEMO ===");
        System.out.println("Demonstrating externalized rules and enrichments configuration\n");
        
        YamlConfigurationDemo demo = new YamlConfigurationDemo();
        
        try {
            // Demonstrate loading rules from YAML
            demo.demonstrateYamlRulesLoading();
            
            // Demonstrate rule execution with YAML configuration
            demo.demonstrateRuleExecution();
            
            // Demonstrate configuration flexibility
            demo.demonstrateConfigurationFlexibility();
            
            // Demonstrate multiple configuration files
            demo.demonstrateMultipleConfigFiles();
            
        } catch (Exception e) {
            LOGGER.severe("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demonstrate loading rules from YAML configuration.
     */
    private void demonstrateYamlRulesLoading() throws YamlConfigurationException {
        System.out.println("1. Loading Rules from YAML Configuration");
        System.out.println("==========================================");

        // Create a simple YAML configuration inline for the demo
        // This avoids module system resource loading issues
        String yamlConfig = createSampleYamlConfiguration();
        RulesEngine engine = yamlService.createRulesEngineFromString(yamlConfig);

        System.out.println("✓ Successfully loaded rules engine from YAML configuration");
        System.out.println("  - Total rules: " + engine.getConfiguration().getAllRules().size());
        System.out.println("  - Total rule groups: " + engine.getConfiguration().getAllRuleGroups().size());

        // Display loaded rules
        System.out.println("\nLoaded Rules:");
        engine.getConfiguration().getAllRules().forEach(rule ->
            System.out.println("  - " + rule.getId() + ": " + rule.getName())
        );

        System.out.println("\nLoaded Rule Groups:");
        engine.getConfiguration().getAllRuleGroups().forEach(group ->
            System.out.println("  - " + group.getId() + ": " + group.getName() +
                             " (" + group.getRules().size() + " rules)")
        );
        
        System.out.println();
    }
    
    /**
     * Demonstrate rule execution with YAML-loaded configuration.
     */
    private void demonstrateRuleExecution() throws YamlConfigurationException {
        System.out.println("2. Rule Execution with YAML Configuration");
        System.out.println("=========================================");

        // Load rules engine using inline YAML configuration
        String yamlConfig = createSampleYamlConfiguration();
        System.out.println("YAML Configuration loaded:");
        System.out.println("  Configuration size: " + yamlConfig.length() + " characters");
        System.out.println("  Contains rules: " + yamlConfig.contains("rules:"));
        System.out.println("  Contains enrichments: " + yamlConfig.contains("enrichments:"));

        RulesEngine engine = yamlService.createRulesEngineFromString(yamlConfig);

        // Create test data
        CommodityTotalReturnSwap validSwap = createValidSwap();
        CommodityTotalReturnSwap invalidSwap = createInvalidSwap();
        
        // Test valid swap
        System.out.println("Testing Valid Commodity Swap:");
        Map<String, Object> validContext = convertSwapToMap(validSwap);
        // Get all rules from the engine configuration
        List<dev.mars.apex.core.engine.model.Rule> allRules =
            engine.getConfiguration().getAllRules();

        List<RuleResult> validResults = new java.util.ArrayList<>();
        for (dev.mars.apex.core.engine.model.Rule rule : allRules) {
            RuleResult result = engine.executeRule(rule, validContext);
            validResults.add(result);
        }

        System.out.println("  Validation Results: " + validResults.size() + " rules evaluated");
        validResults.forEach(result -> {
            if (!result.isTriggered()) {
                System.out.println("    ✗ " + result.getRuleName() + ": " + result.getMessage());
            } else {
                System.out.println("    ✓ " + result.getRuleName());
            }
        });

        // Test invalid swap
        System.out.println("\nTesting Invalid Commodity Swap:");
        Map<String, Object> invalidContext = convertSwapToMap(invalidSwap);

        List<RuleResult> invalidResults = new java.util.ArrayList<>();
        for (dev.mars.apex.core.engine.model.Rule rule : allRules) {
            RuleResult result = engine.executeRule(rule, invalidContext);
            invalidResults.add(result);
        }

        System.out.println("  Validation Results: " + invalidResults.size() + " rules evaluated");
        invalidResults.forEach(result -> {
            if (!result.isTriggered()) {
                System.out.println("    ✗ " + result.getRuleName() + ": " + result.getMessage());
            } else {
                System.out.println("    ✓ " + result.getRuleName());
            }
        });

        System.out.println();
    }
    
    /**
     * Demonstrate configuration flexibility - rules can be enabled/disabled without code changes.
     */
    private void demonstrateConfigurationFlexibility() {
        System.out.println("3. Configuration Flexibility");
        System.out.println("============================");
        
        System.out.println("Key Benefits of YAML Configuration:");
        System.out.println("  ✓ Business users can modify rules without code changes");
        System.out.println("  ✓ Rules can be enabled/disabled dynamically");
        System.out.println("  ✓ Rule priorities can be adjusted");
        System.out.println("  ✓ New rules can be added without recompilation");
        System.out.println("  ✓ Rule conditions can be modified for different environments");
        System.out.println("  ✓ Rule groups can be reorganized");
        System.out.println("  ✓ Configuration can be version controlled");
        System.out.println("  ✓ Multiple configuration files can be used for different scenarios");
        
        System.out.println("\nExample Configuration Changes:");
        System.out.println("  - Disable a rule: Set 'enabled: false' in YAML");
        System.out.println("  - Change rule priority: Modify 'priority' value");
        System.out.println("  - Update rule condition: Edit 'condition' SpEL expression");
        System.out.println("  - Add new rule: Add new rule entry to YAML file");
        
        System.out.println();
    }
    
    /**
     * Demonstrate loading multiple configuration files.
     */
    private void demonstrateMultipleConfigFiles() throws YamlConfigurationException {
        System.out.println("4. Multiple Configuration Files");
        System.out.println("===============================");
        
        // This would load multiple YAML files and merge them
        // For demo purposes, we'll show the concept
        System.out.println("Loading multiple configuration files:");
        System.out.println("  - config/financial-validation-rules.yaml (validation rules)");
        System.out.println("  - config/financial-enrichment-rules.yaml (enrichment rules)");
        
        // In a real scenario, you could do:
        // RulesEngine engine = yamlService.createRulesEngineFromMultipleFiles(
        //     "config/financial-validation-rules.yaml",
        //     "config/financial-enrichment-rules.yaml"
        // );
        
        System.out.println("✓ Configuration files can be organized by domain, environment, or functionality");
        System.out.println("✓ Configurations are merged automatically");
        System.out.println("✓ Allows for modular rule management");
        
        System.out.println();
    }
    
    /**
     * Create a valid commodity swap for testing.
     */
    private CommodityTotalReturnSwap createValidSwap() {
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap();
        swap.setTradeId("COM123456");
        swap.setExternalTradeId("EXT-COM-001");
        swap.setTradeDate(LocalDate.now().minusDays(1));
        swap.setEffectiveDate(LocalDate.now());
        swap.setMaturityDate(LocalDate.now().plusYears(1));
        swap.setCounterpartyId("CP001");
        swap.setCounterpartyLei("12345678901234567890");
        swap.setClientId("CLIENT001");
        swap.setClientAccountId("ACC001");
        swap.setNotionalAmount(new BigDecimal("50000000"));
        swap.setNotionalCurrency("USD");
        swap.setReferenceIndex("WTI");
        swap.setCommodityType("ENERGY");
        swap.setCounterpartyRating("A");
        return swap;
    }
    
    /**
     * Create an invalid commodity swap for testing.
     */
    private CommodityTotalReturnSwap createInvalidSwap() {
        CommodityTotalReturnSwap swap = new CommodityTotalReturnSwap();
        // Missing required fields and invalid data
        swap.setTradeId(""); // Invalid: empty
        swap.setTradeDate(LocalDate.now().plusDays(1)); // Invalid: future date
        swap.setEffectiveDate(LocalDate.now().minusDays(1)); // Invalid: before trade date
        swap.setMaturityDate(LocalDate.now().minusDays(2)); // Invalid: before effective date
        swap.setCounterpartyLei("INVALID"); // Invalid: wrong format
        swap.setNotionalAmount(new BigDecimal("-1000000")); // Invalid: negative
        swap.setNotionalCurrency("XXX"); // Invalid: unsupported currency
        return swap;
    }
    
    /**
     * Convert commodity swap to context map for rule evaluation.
     */
    private Map<String, Object> convertSwapToMap(CommodityTotalReturnSwap swap) {
        Map<String, Object> context = new HashMap<>();
        context.put("tradeId", swap.getTradeId());
        context.put("externalTradeId", swap.getExternalTradeId());
        context.put("tradeDate", swap.getTradeDate());
        context.put("effectiveDate", swap.getEffectiveDate());
        context.put("maturityDate", swap.getMaturityDate());
        context.put("counterpartyId", swap.getCounterpartyId());
        context.put("counterpartyLei", swap.getCounterpartyLei());
        context.put("clientId", swap.getClientId());
        context.put("clientAccountId", swap.getClientAccountId());
        context.put("notionalAmount", swap.getNotionalAmount());
        context.put("notionalCurrency", swap.getNotionalCurrency());
        context.put("referenceIndex", swap.getReferenceIndex());
        context.put("commodityType", swap.getCommodityType());
        context.put("counterpartyRating", swap.getCounterpartyRating());
        return context;
    }

    /**
     * Create a sample YAML configuration for demonstration purposes.
     * This avoids module system resource loading issues.
     */
    private String createSampleYamlConfiguration() {
        return """
            metadata:
              name: "Financial Validation Rules Demo"
              version: "1.0"
              description: "Sample rules for commodity swap validation"

            categories:
              - name: "Financial-Validation"
                description: "Rules for validating financial instruments"

            rules:
              - id: "notional-amount-check"
                name: "Notional Amount Validation"
                description: "Validate notional amount is positive and within limits"
                category: "Financial-Validation"
                condition: "notionalAmount != null && notionalAmount > 0 && notionalAmount <= 1000000000"
                priority: 100
                enabled: true
                message: "Notional amount must be positive and not exceed 1 billion"

              - id: "maturity-date-check"
                name: "Maturity Date Validation"
                description: "Validate maturity date is in the future"
                category: "Financial-Validation"
                condition: "maturityDate != null && maturityDate.isAfter(T(java.time.LocalDate).now())"
                priority: 90
                enabled: true
                message: "Maturity date must be in the future"

              - id: "counterparty-rating-check"
                name: "Counterparty Rating Validation"
                description: "Validate counterparty has acceptable credit rating"
                category: "Financial-Validation"
                condition: "counterpartyRating != null && (counterpartyRating == 'AAA' || counterpartyRating == 'AA' || counterpartyRating == 'A' || counterpartyRating == 'BBB')"
                priority: 80
                enabled: true
                message: "Counterparty must have investment grade rating (BBB or higher)"

              - id: "currency-check"
                name: "Currency Validation"
                description: "Validate currency is supported"
                category: "Financial-Validation"
                condition: "notionalCurrency != null && (notionalCurrency == 'USD' || notionalCurrency == 'EUR' || notionalCurrency == 'GBP')"
                priority: 70
                enabled: true
                message: "Currency must be USD, EUR, or GBP"

            ruleGroups:
              - id: "commodity-swap-validation"
                name: "Commodity Swap Validation"
                description: "Complete validation suite for commodity swaps"
                rules:
                  - "notional-amount-check"
                  - "maturity-date-check"
                  - "counterparty-rating-check"
                  - "currency-check"
            """;
    }
}
