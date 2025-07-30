package dev.mars.apex.demo.rulesets;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.common.NamedService;
import dev.mars.apex.core.service.lookup.LookupService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.validation.Validator;
import dev.mars.apex.core.util.RuleParameterExtractor;
import dev.mars.apex.demo.model.EnhancedTrade;
import dev.mars.apex.demo.model.Trade;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.expression.spel.support.StandardEvaluationContext;

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
 * Comprehensive demonstration of complex trade validation using the rules engine.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Comprehensive demonstration of complex trade validation using the rules engine.
 *
 * This class demonstrates advanced validation scenarios for the EnhancedTrade model,
 * including complex rule groups with AND/OR relationships, nested rule groups,
 * and advanced with reference data through lookup services.
 */
public class IntegratedTradeValidatorComplexDemo implements Validator<Trade>, NamedService {
    private static final Logger LOGGER = Logger.getLogger(IntegratedTradeValidatorComplexDemo.class.getName());

    // Core components
    private final RulesEngine rulesEngine;
    private final String validatorName;
    private final Map<String, Object> parameters;
    private final StandardEvaluationContext context;
    private final RuleGroup validationRuleGroup;
    private final LookupServiceRegistry lookupRegistry;

    /**
     * Main method to run the demonstration.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        runComplexTradeValidationDemo();
    }

    /**
     * Run the complex trade validation demonstration.
     * This method shows the complete process of creating and using complex trade validation rules.
     */
    private static void runComplexTradeValidationDemo() {
        LOGGER.info("Starting integrated complex trade validation demonstration");

        // Step 1: Create a RulesEngine
        LOGGER.info("Step 1: Creating a RulesEngine");
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Step 2: Create a LookupServiceRegistry and register reference data
        LOGGER.info("Step 2: Creating a LookupServiceRegistry and registering reference data");
        LookupServiceRegistry registry = createAndPopulateLookupRegistry();

        // Step 3: Create parameters for settlement validation
        LOGGER.info("Step 3: Creating parameters for settlement validation");
        Map<String, Object> settlementParams = new HashMap<>();
        settlementParams.put("allowedSettlementMethods", Arrays.asList("DVP", "FOP", "DFP"));
        LOGGER.info("Allowed settlement methods: " + settlementParams.get("allowedSettlementMethods"));
        settlementParams.put("allowedSettlementLocations", Arrays.asList("DTCC", "Euroclear", "Clearstream"));
        LOGGER.info("Allowed settlement locations: " + settlementParams.get("allowedSettlementLocations"));
        settlementParams.put("allowedCurrencies", Arrays.asList("USD", "EUR", "GBP", "JPY"));
        LOGGER.info("Allowed Currencies: " + settlementParams.get("allowedCurrencies"));
        settlementParams.put("minSettlementAmount", 1000.0);
        LOGGER.info("Allowed settlement amount: " + settlementParams.get("minSettlementAmount"));
        settlementParams.put("maxSettlementDays", 5);
        LOGGER.info("Allowed settlement days: " + settlementParams.get("maxSettlementDays"));

        // Step 4: Create an IntegratedTradeValidatorComplexDemo instance
        LOGGER.info("Step 4: Creating an IntegratedTradeValidatorComplexDemo instance");
        IntegratedTradeValidatorComplexDemo validator = new IntegratedTradeValidatorComplexDemo(
                "settlementValidator",
                settlementParams,
                rulesEngine,
                registry
        );

        // Step 5: Create sample enhanced trades
        LOGGER.info("Step 5: Creating sample enhanced trades");
        List<EnhancedTrade> trades = createSampleEnhancedTrades();

        // Step 6: Validate trades using standard validation
        LOGGER.info("\nStep 6: Validating trades using standard validation");
        for (EnhancedTrade trade : trades) {
            LOGGER.info(trade.getId() + " (Value: " + trade.getValue() +
                    ", Settlement Status: " + trade.getSettlementStatus() + "): " +
                    validator.validate(trade));
        }

        // Step 7: Get detailed validation results
        LOGGER.info("\nStep 7: Getting detailed validation results");
        EnhancedTrade validTrade = trades.get(0); // Valid trade
        EnhancedTrade invalidTrade = trades.get(1); // Invalid trade

        RuleResult validResult = validator.validateWithResult(validTrade);
        LOGGER.info("Valid trade result: " + validResult);
        LOGGER.info("Valid trade triggered: " + validResult.isTriggered());
        LOGGER.info("Valid trade rule name: " + validResult.getRuleName());

        RuleResult invalidResult = validator.validateWithResult(invalidTrade);
        LOGGER.info("Invalid trade result: " + invalidResult);
        LOGGER.info("Invalid trade triggered: " + invalidResult.isTriggered());

        // Step 8: Create parameters for compliance validation
        LOGGER.info("\nStep 8: Creating parameters for compliance validation");
        Map<String, Object> complianceParams = new HashMap<>();
        complianceParams.put("highRiskCountries", Arrays.asList("CountryX", "CountryY", "CountryZ"));
        complianceParams.put("restrictedEntities", Arrays.asList("EntityA", "EntityB", "EntityC"));

        // Step 9: Create a compliance validator
        LOGGER.info("Step 9: Creating a compliance validator");
        IntegratedTradeValidatorComplexDemo complianceValidator = new IntegratedTradeValidatorComplexDemo(
                "complianceValidator",
                complianceParams,
                rulesEngine,
                registry,
                true // Use compliance rule group
        );

        // Step 10: Validate trades with compliance validator
        LOGGER.info("\nStep 10: Validating trades with compliance validator");
        for (EnhancedTrade trade : trades) {
            LOGGER.info(trade.getId() + " (Risk Rating: " + trade.getRiskRating() +
                    ", AML Check: " + trade.isAmlCheckPassed() +
                    ", Sanctions Check: " + trade.isSanctionsCheckPassed() + "): " +
                    complianceValidator.validate(trade));
        }

        // Step 11: Create a complex validator with nested rule groups
        LOGGER.info("\nStep 11: Creating a complex validator with nested rule groups");
        IntegratedTradeValidatorComplexDemo complexValidator = new IntegratedTradeValidatorComplexDemo(
                "complexValidator",
                settlementParams,
                rulesEngine,
                registry,
                false, // Use settlement rule group
                true   // Create nested rule groups
        );

        // Step 12: Validate trades with complex validator
        LOGGER.info("\nStep 12: Validating trades with complex validator");
        for (EnhancedTrade trade : trades) {
            LOGGER.info(trade.getId() + ": " + complexValidator.validate(trade));

            RuleResult result = complexValidator.validateWithResult(trade);
            LOGGER.info("  Result message: " + result.getMessage());
        }

        // Step 13: Validate using dynamic expressions with lookup services
        LOGGER.info("\nStep 13: Validating using dynamic expressions with lookup services");
        String lookupExpression = "#trade != null && #lookupRegistry.getService('CurrencyCodes', T(lookup.service.dev.mars.rulesengine.core.LookupService)).validate(#trade.settlementCurrency)";
        LOGGER.info("Expression: " + lookupExpression);

        for (EnhancedTrade trade : trades) {
            LOGGER.info(trade.getId() + " (Currency: " + trade.getSettlementCurrency() + "): " +
                    validator.validateWithExpressionAndLookup(trade, lookupExpression));
        }

        LOGGER.info("\nIntegrated complex trade validation demonstration completed");
    }

    /**
     * Create and populate a lookup service registry with reference data.
     *
     * @return The populated lookup service registry
     */
    private static LookupServiceRegistry createAndPopulateLookupRegistry() {
        LookupServiceRegistry registry = new LookupServiceRegistry();

        // Create and register lookup services for reference data
        LookupService instrumentTypes = new LookupService(
                "InstrumentTypes",
                Arrays.asList("Equity", "Bond", "Option", "Future", "Swap", "ETF", "FixedIncome")
        );

        LookupService settlementMethods = new LookupService(
                "SettlementMethods",
                Arrays.asList("DVP", "FOP", "DFP", "RVP", "PVP")
        );

        LookupService settlementLocations = new LookupService(
                "SettlementLocations",
                Arrays.asList("DTCC", "Euroclear", "Clearstream", "CDS", "JASDEC", "HKSCC")
        );

        LookupService custodians = new LookupService(
                "Custodians",
                Arrays.asList("BNY Mellon", "State Street", "JPMorgan", "Citi", "HSBC", "Northern Trust")
        );

        LookupService accountTypes = new LookupService(
                "AccountTypes",
                Arrays.asList("Client", "House", "Omnibus", "Proprietary", "Agency")
        );

        LookupService currencyCodes = new LookupService(
                "CurrencyCodes",
                Arrays.asList("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY")
        );

        LookupService riskRatings = new LookupService(
                "RiskRatings",
                Arrays.asList("High", "Medium", "Low")
        );

        // Register all lookup services
        registry.registerService(instrumentTypes);
        registry.registerService(settlementMethods);
        registry.registerService(settlementLocations);
        registry.registerService(custodians);
        registry.registerService(accountTypes);
        registry.registerService(currencyCodes);
        registry.registerService(riskRatings);

        return registry;
    }

    /**
     * Create sample enhanced trades for demonstration.
     *
     * @return List of sample enhanced trades
     */
    private static List<EnhancedTrade> createSampleEnhancedTrades() {
        List<EnhancedTrade> trades = new ArrayList<>();

        // Valid trade with all required settlement information
        EnhancedTrade validTrade = new EnhancedTrade(
                "T001",
                "Equity",
                "InstrumentType",
                "Pending",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                "USD",
                50000.0,
                "DVP",
                "DTCC",
                "State Street",
                "ACC123456",
                "Client",
                true,
                "DTCC",
                "Low"
        );
        validTrade.setAmlCheckPassed(true);
        validTrade.setSanctionsCheckPassed(true);
        trades.add(validTrade);

        // Invalid trade with missing settlement information
        EnhancedTrade invalidTrade = new EnhancedTrade(
                "T002",
                "Bond",
                "InstrumentType",
                "Pending",
                LocalDate.now(),
                LocalDate.now().plusDays(10), // Too many days
                "XYZ", // Invalid currency
                500.0, // Below minimum amount
                "ABC", // Invalid method
                "Unknown", // Invalid location
                "Unknown Bank", // Invalid custodian
                "ACC789012",
                "House",
                false,
                "Unknown",
                "High"
        );
        trades.add(invalidTrade);

        // Valid trade but with compliance issues
        EnhancedTrade complianceIssueTrade = new EnhancedTrade(
                "T003",
                "ETF",
                "InstrumentType",
                "Pending",
                LocalDate.now(),
                LocalDate.now().plusDays(2),
                "EUR",
                1000000.0,
                "DVP",
                "Euroclear",
                "BNY Mellon",
                "ACC345678",
                "Client",
                true,
                "Euroclear",
                "High"
        );
        complianceIssueTrade.setAmlCheckPassed(false);
        complianceIssueTrade.setSanctionsCheckPassed(true);
        complianceIssueTrade.addComplianceFlag("highRiskCountry", "CountryX");
        trades.add(complianceIssueTrade);

        // Trade with sanctions issues
        EnhancedTrade sanctionsIssueTrade = new EnhancedTrade(
                "T004",
                "Option",
                "InstrumentType",
                "Pending",
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "GBP",
                75000.0,
                "FOP",
                "Clearstream",
                "JPMorgan",
                "ACC901234",
                "Agency",
                false,
                "Clearstream",
                "Medium"
        );
        sanctionsIssueTrade.setAmlCheckPassed(true);
        sanctionsIssueTrade.setSanctionsCheckPassed(false);
        sanctionsIssueTrade.addComplianceFlag("restrictedEntity", "EntityB");
        trades.add(sanctionsIssueTrade);

        return trades;
    }

    /**
     * Create a new IntegratedTradeValidatorComplexDemo with the specified parameters.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters
     * @param rulesEngine The rules engine to use
     * @param lookupRegistry The lookup service registry
     */
    public IntegratedTradeValidatorComplexDemo(String name, Map<String, Object> parameters,
                                               RulesEngine rulesEngine, LookupServiceRegistry lookupRegistry) {
        this(name, parameters, rulesEngine, lookupRegistry, false, false);
    }

    /**
     * Create a new IntegratedTradeValidatorComplexDemo with the specified parameters.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters
     * @param rulesEngine The rules engine to use
     * @param lookupRegistry The lookup service registry
     * @param useComplianceRules Whether to use compliance rules instead of settlement rules
     */
    public IntegratedTradeValidatorComplexDemo(String name, Map<String, Object> parameters,
                                               RulesEngine rulesEngine, LookupServiceRegistry lookupRegistry,
                                               boolean useComplianceRules) {
        this(name, parameters, rulesEngine, lookupRegistry, useComplianceRules, false);
    }

    /**
     * Create a new IntegratedTradeValidatorComplexDemo with the specified parameters.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters
     * @param rulesEngine The rules engine to use
     * @param lookupRegistry The lookup service registry
     * @param useComplianceRules Whether to use compliance rules instead of settlement rules
     * @param createNestedGroups Whether to create nested rule groups
     */
    public IntegratedTradeValidatorComplexDemo(String name, Map<String, Object> parameters,
                                               RulesEngine rulesEngine, LookupServiceRegistry lookupRegistry,
                                               boolean useComplianceRules, boolean createNestedGroups) {
        this.validatorName = name;
        this.parameters = new HashMap<>(parameters);
        this.rulesEngine = rulesEngine;
        this.lookupRegistry = lookupRegistry;
        this.context = new StandardEvaluationContext();

        // Initialize context with validation parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }

        // Add lookup registry to context
        context.setVariable("lookupRegistry", lookupRegistry);

        // Create validation rule group based on type
        if (useComplianceRules) {
            this.validationRuleGroup = createComplianceRuleGroup(name, parameters);
        } else if (createNestedGroups) {
            this.validationRuleGroup = createNestedRuleGroup(name, parameters);
        } else {
            this.validationRuleGroup = createSettlementRuleGroup(name, parameters);
        }
    }

    /**
     * Create a settlement validation rule group for enhanced trades.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters
     * @return The validation rule group
     */
    private RuleGroup createSettlementRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
                "SettlementValidationRuleGroup",
                "TradeValidation",
                name,
                "Validates trade settlement against defined criteria",
                1,
                true // AND operator
        );

        // Create Rule for null check
        Rule nullCheckRule = new Rule(
                "NullCheckRule",
                "#trade != null",
                "Trade must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Create Rule for enhanced trade type check
        Rule enhancedTradeCheckRule = new Rule(
                "EnhancedTradeCheckRule",
                "#trade instanceof T(model.dev.mars.rulesengine.demo.EnhancedTrade)",
                "Trade must be an EnhancedTrade"
        );
        ruleGroup.addRule(enhancedTradeCheckRule, 2);

        // Create Rule for settlement status check
        Rule settlementStatusRule = new Rule(
                "SettlementStatusRule",
                "#trade.settlementStatus != null && #trade.settlementStatus != 'Failed'",
                "Trade settlement status must not be Failed"
        );
        ruleGroup.addRule(settlementStatusRule, 3);

        // Create Rule for settlement method validation
        @SuppressWarnings("unchecked")
        List<String> allowedSettlementMethods = parameters.containsKey("allowedSettlementMethods") ?
                (List<String>) parameters.get("allowedSettlementMethods") : Collections.emptyList();

        if (!allowedSettlementMethods.isEmpty()) {
            Rule settlementMethodRule = new Rule(
                    "SettlementMethodRule",
                    "#trade.settlementMethod != null && (#allowedSettlementMethods.isEmpty() || #allowedSettlementMethods.contains(#trade.settlementMethod))",
                    "Trade settlement method must be in the allowed methods list"
            );
            ruleGroup.addRule(settlementMethodRule, 4);
        }

        // Create Rule for settlement location validation
        @SuppressWarnings("unchecked")
        List<String> allowedSettlementLocations = parameters.containsKey("allowedSettlementLocations") ?
                (List<String>) parameters.get("allowedSettlementLocations") : Collections.emptyList();

        if (!allowedSettlementLocations.isEmpty()) {
            Rule settlementLocationRule = new Rule(
                    "SettlementLocationRule",
                    "#trade.settlementLocation != null && (#allowedSettlementLocations.isEmpty() || #allowedSettlementLocations.contains(#trade.settlementLocation))",
                    "Trade settlement location must be in the allowed locations list"
            );
            ruleGroup.addRule(settlementLocationRule, 5);
        }

        // Create Rule for settlement currency validation
        @SuppressWarnings("unchecked")
        List<String> allowedCurrencies = parameters.containsKey("allowedCurrencies") ?
                (List<String>) parameters.get("allowedCurrencies") : Collections.emptyList();

        if (!allowedCurrencies.isEmpty()) {
            Rule currencyRule = new Rule(
                    "CurrencyRule",
                    "#trade.settlementCurrency != null && (#allowedCurrencies.isEmpty() || #allowedCurrencies.contains(#trade.settlementCurrency))",
                    "Trade settlement currency must be in the allowed currencies list"
            );
            ruleGroup.addRule(currencyRule, 6);
        }

        // Create Rule for settlement amount validation
        double minSettlementAmount = parameters.containsKey("minSettlementAmount") ?
                (double) parameters.get("minSettlementAmount") : 0.0;

        Rule settlementAmountRule = new Rule(
                "SettlementAmountRule",
                "#trade.settlementAmount >= #minSettlementAmount",
                "Trade settlement amount must be at least " + minSettlementAmount
        );
        ruleGroup.addRule(settlementAmountRule, 7);

        // Create Rule for settlement date validation
        int maxSettlementDays = parameters.containsKey("maxSettlementDays") ?
                (int) parameters.get("maxSettlementDays") : 10;

        Rule settlementDateRule = new Rule(
                "SettlementDateRule",
                "#trade.settlementDate != null && T(java.time.temporal.ChronoUnit).DAYS.between(#trade.tradeDate, #trade.settlementDate) <= #maxSettlementDays",
                "Trade settlement date must be within " + maxSettlementDays + " days of trade date"
        );
        ruleGroup.addRule(settlementDateRule, 8);

        // Validate that all required parameters exist (except 'trade' which will be provided at validation time)
        Set<String> allParams = RuleParameterExtractor.extractParameters(ruleGroup);
        allParams.remove("trade"); // Remove trade as it will be provided at validation time

        Set<String> missingParams = new HashSet<>();
        for (String param : allParams) {
            if (!parameters.containsKey(param)) {
                missingParams.add(param);
            }
        }

        if (!missingParams.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + missingParams);
        }

        return ruleGroup;
    }

    /**
     * Create a compliance validation rule group for enhanced trades.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters
     * @return The validation rule group
     */
    private RuleGroup createComplianceRuleGroup(String name, Map<String, Object> parameters) {
        // Create a rule group with AND operator (all rules must pass)
        RuleGroup ruleGroup = new RuleGroup(
                "ComplianceValidationRuleGroup",
                "TradeValidation",
                name,
                "Validates trade compliance against defined criteria",
                1,
                true // AND operator
        );

        // Create Rule for null check
        Rule nullCheckRule = new Rule(
                "NullCheckRule",
                "#trade != null",
                "Trade must not be null"
        );
        ruleGroup.addRule(nullCheckRule, 1);

        // Create Rule for enhanced trade type check
        Rule enhancedTradeCheckRule = new Rule(
                "EnhancedTradeCheckRule",
                "#trade instanceof T(model.dev.mars.rulesengine.demo.EnhancedTrade)",
                "Trade must be an EnhancedTrade"
        );
        ruleGroup.addRule(enhancedTradeCheckRule, 2);

        // Create Rule for AML check
        Rule amlCheckRule = new Rule(
                "AmlCheckRule",
                "#trade.amlCheckPassed == true",
                "Trade must pass AML check"
        );
        ruleGroup.addRule(amlCheckRule, 3);

        // Create Rule for sanctions check
        Rule sanctionsCheckRule = new Rule(
                "SanctionsCheckRule",
                "#trade.sanctionsCheckPassed == true",
                "Trade must pass sanctions check"
        );
        ruleGroup.addRule(sanctionsCheckRule, 4);

        // Create Rule for high risk country check
        @SuppressWarnings("unchecked")
        List<String> highRiskCountries = parameters.containsKey("highRiskCountries") ?
                (List<String>) parameters.get("highRiskCountries") : Collections.emptyList();

        if (!highRiskCountries.isEmpty()) {
            Rule highRiskCountryRule = new Rule(
                    "HighRiskCountryRule",
                    "!#trade.complianceFlags.containsKey('highRiskCountry') || !#highRiskCountries.contains(#trade.complianceFlags.get('highRiskCountry'))",
                    "Trade must not involve a high risk country"
            );
            ruleGroup.addRule(highRiskCountryRule, 5);
        }

        // Create Rule for restricted entity check
        @SuppressWarnings("unchecked")
        List<String> restrictedEntities = parameters.containsKey("restrictedEntities") ?
                (List<String>) parameters.get("restrictedEntities") : Collections.emptyList();

        if (!restrictedEntities.isEmpty()) {
            Rule restrictedEntityRule = new Rule(
                    "RestrictedEntityRule",
                    "!#trade.complianceFlags.containsKey('restrictedEntity') || !#restrictedEntities.contains(#trade.complianceFlags.get('restrictedEntity'))",
                    "Trade must not involve a restricted entity"
            );
            ruleGroup.addRule(restrictedEntityRule, 6);
        }

        // Create Rule for high risk rating with additional checks
        Rule highRiskRatingRule = new Rule(
                "HighRiskRatingRule",
                "#trade.riskRating != 'High' || (#trade.amlCheckPassed == true && #trade.sanctionsCheckPassed == true)",
                "High risk trades must pass both AML and sanctions checks"
        );
        ruleGroup.addRule(highRiskRatingRule, 7);

        // Validate that all required parameters exist (except 'trade' which will be provided at validation time)
        Set<String> allParams = RuleParameterExtractor.extractParameters(ruleGroup);
        allParams.remove("trade"); // Remove trade as it will be provided at validation time

        Set<String> missingParams = new HashSet<>();
        for (String param : allParams) {
            if (!parameters.containsKey(param)) {
                missingParams.add(param);
            }
        }

        if (!missingParams.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + missingParams);
        }

        return ruleGroup;
    }

    /**
     * Create a nested rule group with complex AND/OR relationships.
     *
     * @param name The name of the validator
     * @param parameters Map of validation parameters
     * @return The validation rule group
     */
    private RuleGroup createNestedRuleGroup(String name, Map<String, Object> parameters) {
        // Create the main rule group with AND operator
        RuleGroup mainRuleGroup = new RuleGroup(
                "MainRuleGroup",
                "TradeValidation",
                name,
                "Main validation rule group with nested groups",
                1,
                true // AND operator
        );

        // Create Rule for null check
        Rule nullCheckRule = new Rule(
                "NullCheckRule",
                "#trade != null",
                "Trade must not be null"
        );
        mainRuleGroup.addRule(nullCheckRule, 1);

        // Create Rule for enhanced trade type check
        Rule enhancedTradeCheckRule = new Rule(
                "EnhancedTradeCheckRule",
                "#trade instanceof T(model.dev.mars.rulesengine.demo.EnhancedTrade)",
                "Trade must be an EnhancedTrade"
        );
        mainRuleGroup.addRule(enhancedTradeCheckRule, 2);

        // Create a nested rule group for settlement validation with OR operator
        // (either it's already settled OR it meets all settlement criteria)
        RuleGroup settlementRuleGroup = new RuleGroup(
                "SettlementRuleGroup",
                "TradeValidation",
                "SettlementValidation",
                "Settlement validation rules",
                3,
                false // OR operator
        );

        // Rule for already settled trades
        Rule alreadySettledRule = new Rule(
                "AlreadySettledRule",
                "#trade.settlementStatus == 'Settled'",
                "Trade is already settled"
        );
        settlementRuleGroup.addRule(alreadySettledRule, 1);

        // Create a nested AND rule group for pending settlement validation
        RuleGroup pendingSettlementRuleGroup = new RuleGroup(
                "PendingSettlementRuleGroup",
                "TradeValidation",
                "PendingSettlementValidation",
                "Pending settlement validation rules",
                2,
                true // AND operator
        );

        // Rules for pending settlement validation
        Rule pendingStatusRule = new Rule(
                "PendingStatusRule",
                "#trade.settlementStatus == 'Pending'",
                "Trade is pending settlement"
        );
        pendingSettlementRuleGroup.addRule(pendingStatusRule, 1);

        // Settlement method validation
        @SuppressWarnings("unchecked")
        List<String> allowedSettlementMethods = parameters.containsKey("allowedSettlementMethods") ?
                (List<String>) parameters.get("allowedSettlementMethods") : Collections.emptyList();

        if (!allowedSettlementMethods.isEmpty()) {
            Rule settlementMethodRule = new Rule(
                    "SettlementMethodRule",
                    "#trade.settlementMethod != null && (#allowedSettlementMethods.isEmpty() || #allowedSettlementMethods.contains(#trade.settlementMethod))",
                    "Trade settlement method must be in the allowed methods list"
            );
            pendingSettlementRuleGroup.addRule(settlementMethodRule, 2);
        }

        // Settlement currency validation
        @SuppressWarnings("unchecked")
        List<String> allowedCurrencies = parameters.containsKey("allowedCurrencies") ?
                (List<String>) parameters.get("allowedCurrencies") : Collections.emptyList();

        if (!allowedCurrencies.isEmpty()) {
            Rule currencyRule = new Rule(
                    "CurrencyRule",
                    "#trade.settlementCurrency != null && (#allowedCurrencies.isEmpty() || #allowedCurrencies.contains(#trade.settlementCurrency))",
                    "Trade settlement currency must be in the allowed currencies list"
            );
            pendingSettlementRuleGroup.addRule(currencyRule, 3);
        }

        // Add the pending settlement rule group to the settlement rule group
        // This is done by creating a rule that evaluates the nested group
        Rule pendingSettlementGroupRule = new Rule(
                "PendingSettlementGroupRule",
                "#pendingSettlementRuleGroup.evaluate(#context)",
                "Pending settlement validation"
        );
        settlementRuleGroup.addRule(pendingSettlementGroupRule, 2);

        // Add the settlement rule group to the main rule group
        // This is done by creating a rule that evaluates the nested group
        Rule settlementGroupRule = new Rule(
                "SettlementGroupRule",
                "#settlementRuleGroup.evaluate(#context)",
                "Settlement validation"
        );
        mainRuleGroup.addRule(settlementGroupRule, 3);

        // Create a nested rule group for compliance validation with AND operator
        RuleGroup complianceRuleGroup = new RuleGroup(
                "ComplianceRuleGroup",
                "TradeValidation",
                "ComplianceValidation",
                "Compliance validation rules",
                4,
                true // AND operator
        );

        // Create an OR rule group for risk rating validation
        // (either low risk OR (medium/high risk with additional checks))
        RuleGroup riskRatingRuleGroup = new RuleGroup(
                "RiskRatingRuleGroup",
                "TradeValidation",
                "RiskRatingValidation",
                "Risk rating validation rules",
                1,
                false // OR operator
        );

        // Rule for low risk trades
        Rule lowRiskRule = new Rule(
                "LowRiskRule",
                "#trade.riskRating == 'Low'",
                "Trade has low risk rating"
        );
        riskRatingRuleGroup.addRule(lowRiskRule, 1);

        // Create an AND rule group for medium/high risk validation
        RuleGroup highRiskRuleGroup = new RuleGroup(
                "HighRiskRuleGroup",
                "TradeValidation",
                "HighRiskValidation",
                "High risk validation rules",
                2,
                true // AND operator
        );

        // Rules for high risk validation
        Rule highRiskCheckRule = new Rule(
                "HighRiskCheckRule",
                "#trade.riskRating == 'Medium' || #trade.riskRating == 'High'",
                "Trade has medium or high risk rating"
        );
        highRiskRuleGroup.addRule(highRiskCheckRule, 1);

        // AML check rule
        Rule amlCheckRule = new Rule(
                "AmlCheckRule",
                "#trade.amlCheckPassed == true",
                "Trade must pass AML check"
        );
        highRiskRuleGroup.addRule(amlCheckRule, 2);

        // Sanctions check rule
        Rule sanctionsCheckRule = new Rule(
                "SanctionsCheckRule",
                "#trade.sanctionsCheckPassed == true",
                "Trade must pass sanctions check"
        );
        highRiskRuleGroup.addRule(sanctionsCheckRule, 3);

        // Add the high risk rule group to the risk rating rule group
        Rule highRiskGroupRule = new Rule(
                "HighRiskGroupRule",
                "#highRiskRuleGroup.evaluate(#context)",
                "High risk validation"
        );
        riskRatingRuleGroup.addRule(highRiskGroupRule, 2);

        // Add the risk rating rule group to the compliance rule group
        Rule riskRatingGroupRule = new Rule(
                "RiskRatingGroupRule",
                "#riskRatingRuleGroup.evaluate(#context)",
                "Risk rating validation"
        );
        complianceRuleGroup.addRule(riskRatingGroupRule, 1);

        // Add the compliance rule group to the main rule group
        Rule complianceGroupRule = new Rule(
                "ComplianceGroupRule",
                "#complianceRuleGroup.evaluate(#context)",
                "Compliance validation"
        );
        mainRuleGroup.addRule(complianceGroupRule, 4);

        // Add all rule groups to the parameters for evaluation
        parameters.put("pendingSettlementRuleGroup", pendingSettlementRuleGroup);
        parameters.put("settlementRuleGroup", settlementRuleGroup);
        parameters.put("highRiskRuleGroup", highRiskRuleGroup);
        parameters.put("riskRatingRuleGroup", riskRatingRuleGroup);
        parameters.put("complianceRuleGroup", complianceRuleGroup);
        parameters.put("context", context);

        return mainRuleGroup;
    }

    /**
     * Create a dynamic validation rule based on a custom expression.
     *
     * @param expression The expression to evaluate
     * @return The validation rule
     */
    private Rule createDynamicValidationRule(String expression) {
        return new Rule(
                "DynamicValidationRule",
                expression,
                "Dynamic validation rule"
        );
    }

    @Override
    public String getName() {
        return validatorName;
    }

    @Override
    public boolean validate(Trade trade) {
        RuleResult result = validateWithResult(trade);
        return result.isTriggered();
    }

    @Override
    public Class<Trade> getType() {
        return Trade.class;
    }

    @Override
    public RuleResult validateWithResult(Trade trade) {
        // Set the trade in the context
        context.setVariable("trade", trade);

        // Create initial facts map with trade data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("trade", trade);

        // Use RuleParameterExtractor to ensure all required parameters exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(validationRuleGroup, initialFacts);

        // Execute the rule group using the rules engine
        return rulesEngine.executeRuleGroupsList(Collections.singletonList(validationRuleGroup), facts);
    }

    /**
     * Validate a trade using a dynamic expression.
     *
     * @param trade The trade to validate
     * @param expression The expression to evaluate
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpression(Trade trade, String expression) {
        // Set the trade in the context
        context.setVariable("trade", trade);

        // Create a rule with the dynamic expression
        Rule dynamicRule = createDynamicValidationRule(expression);

        // Create initial facts map with trade data and parameters
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("trade", trade);

        // Use RuleParameterExtractor to ensure all required parameters for the dynamic rule exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(dynamicRule, initialFacts);

        // Execute the rule using the rules engine
        RuleResult result = rulesEngine.executeRule(dynamicRule, facts);
        return result.isTriggered();
    }

    /**
     * Validate a trade using a dynamic expression with lookup services.
     *
     * @param trade The trade to validate
     * @param expression The expression to evaluate
     * @return True if the expression evaluates to true, false otherwise
     */
    public boolean validateWithExpressionAndLookup(Trade trade, String expression) {
        // Set the trade in the context
        context.setVariable("trade", trade);
        context.setVariable("lookupRegistry", lookupRegistry);

        // Create a rule with the dynamic expression
        Rule dynamicRule = createDynamicValidationRule(expression);

        // Create initial facts map with trade data, parameters, and lookup registry
        Map<String, Object> initialFacts = new HashMap<>(parameters);
        initialFacts.put("trade", trade);
        initialFacts.put("lookupRegistry", lookupRegistry);

        // Use RuleParameterExtractor to ensure all required parameters for the dynamic rule exist in the facts map
        Map<String, Object> facts = RuleParameterExtractor.ensureParameters(dynamicRule, initialFacts);

        // Execute the rule using the rules engine
        RuleResult result = rulesEngine.executeRule(dynamicRule, facts);
        return result.isTriggered();
    }
}
