package dev.mars.apex.demo.examples;

import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;

import java.util.ArrayList;
import java.util.List;

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
 * Pre-built rule sets for financial instrument validation.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class FinancialValidationRuleSet {
    
    /**
     * Creates a comprehensive rule set for OTC Commodity Total Return Swap validation.
     * 
     * @return RuleGroup containing all commodity swap validation rules
     */
    public static RuleGroup createCommoditySwapValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Basic field validation rules
        rules.addAll(createBasicFieldValidationRules());
        
        // Business logic validation rules
        rules.addAll(createBusinessLogicValidationRules());
        
        // Regulatory compliance rules
        rules.addAll(createRegulatoryComplianceRules());
        
        // Risk management rules
        rules.addAll(createRiskManagementRules());
        
        RuleGroup ruleGroup = new RuleGroup("commodity-swap-validation", "validation", "Commodity Swap Validation Rules", "Comprehensive validation rules for commodity swap instruments", 10, true);

        // Add all rules to the group
        int sequenceNumber = 1;
        for (Rule rule : rules) {
            ruleGroup.addRule(rule, sequenceNumber++);
        }

        return ruleGroup;
    }
    
    /**
     * Creates basic field validation rules.
     */
    private static List<Rule> createBasicFieldValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // TradeB identification rules
        rules.add(new Rule("trade-id-required", 
                          "#tradeId != null && #tradeId.trim().length() > 0", 
                          "TradeB ID is required"));
        
        rules.add(new Rule("trade-id-format", 
                          "#tradeId != null && #tradeId.matches('^[A-Z]{3}[0-9]{3,6}$')", 
                          "TradeB ID must follow format: 3 letters + 3-6 digits"));
        
        rules.add(new Rule("external-trade-id-unique", 
                          "#externalTradeId == null || (#externalTradeId != null && #externalTradeId.trim().length() > 0)", 
                          "External TradeB ID must be non-empty if provided"));
        
        // Date validation rules
        rules.add(new Rule("trade-date-required", 
                          "#tradeDate != null", 
                          "TradeB date is required"));
        
        rules.add(new Rule("effective-date-required", 
                          "#effectiveDate != null", 
                          "Effective date is required"));
        
        rules.add(new Rule("maturity-date-required", 
                          "#maturityDate != null", 
                          "Maturity date is required"));
        
        rules.add(new Rule("date-sequence-valid", 
                          "#tradeDate != null && #effectiveDate != null && #maturityDate != null && " +
                          "#effectiveDate.compareTo(#tradeDate) >= 0 && #maturityDate.isAfter(#effectiveDate)", 
                          "Dates must be in sequence: trade date <= effective date < maturity date"));
        
        // Counterparty validation rules
        rules.add(new Rule("counterparty-id-required", 
                          "#counterpartyId != null && #counterpartyId.trim().length() > 0", 
                          "Counterparty ID is required"));
        
        rules.add(new Rule("counterparty-lei-format", 
                          "#counterpartyLei == null || (#counterpartyLei != null && #counterpartyLei.matches('^[A-Z0-9]{20}$'))", 
                          "Counterparty LEI must be 20 alphanumeric characters if provided"));
        
        // Client validation rules
        rules.add(new Rule("client-id-required", 
                          "#clientId != null && #clientId.trim().length() > 0", 
                          "Client ID is required"));
        
        rules.add(new Rule("client-account-id-required", 
                          "#clientAccountId != null && #clientAccountId.trim().length() > 0", 
                          "Client Account ID is required"));
        
        // Instrument validation rules
        rules.add(new Rule("commodity-type-required", 
                          "#commodityType != null && #commodityType.trim().length() > 0", 
                          "Commodity type is required"));
        
        rules.add(new Rule("commodity-type-valid", 
                          "#commodityType != null && (#commodityType == 'ENERGY' || #commodityType == 'METALS' || #commodityType == 'AGRICULTURAL')", 
                          "Commodity type must be ENERGY, METALS, or AGRICULTURAL"));
        
        rules.add(new Rule("reference-index-required", 
                          "#referenceIndex != null && #referenceIndex.trim().length() > 0", 
                          "Reference index is required"));
        
        // Financial terms validation
        rules.add(new Rule("notional-amount-required", 
                          "#notionalAmount != null", 
                          "Notional amount is required"));
        
        rules.add(new Rule("notional-amount-positive", 
                          "#notionalAmount != null && #notionalAmount.compareTo(T(java.math.BigDecimal).ZERO) > 0",
                          "Notional amount must be positive"));
        
        rules.add(new Rule("notional-currency-required", 
                          "#notionalCurrency != null && #notionalCurrency.trim().length() == 3", 
                          "Notional currency is required and must be 3 characters"));
        
        rules.add(new Rule("payment-currency-required", 
                          "#paymentCurrency != null && #paymentCurrency.trim().length() == 3", 
                          "Payment currency is required and must be 3 characters"));
        
        return rules;
    }
    
    /**
     * Creates business logic validation rules.
     */
    private static List<Rule> createBusinessLogicValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Notional amount business rules
        rules.add(new Rule("minimum-notional-amount", 
                          "#notionalAmount != null && #notionalAmount.compareTo(T(java.math.BigDecimal).new('1000000')) >= 0",
                          "Minimum notional amount is $1,000,000"));
        
        rules.add(new Rule("maximum-notional-amount", 
                          "#notionalAmount != null && #notionalAmount.compareTo(T(java.math.BigDecimal).new('1000000000')) <= 0",
                          "Maximum notional amount is $1,000,000,000"));
        
        // Maturity business rules
        rules.add(new Rule("maximum-maturity-period", 
                          "#tradeDate != null && #maturityDate != null && #maturityDate.isBefore(#tradeDate.plusYears(10))", 
                          "Maximum maturity period is 10 years"));
        
        rules.add(new Rule("minimum-maturity-period", 
                          "#tradeDate != null && #maturityDate != null && #maturityDate.isAfter(#tradeDate.plusDays(30))", 
                          "Minimum maturity period is 30 days"));
        
        // Currency consistency rules
        rules.add(new Rule("currency-consistency", 
                          "#notionalCurrency != null && #paymentCurrency != null && " +
                          "(#settlementCurrency == null || #settlementCurrency == #paymentCurrency)", 
                          "Settlement currency must match payment currency if specified"));
        
        // Total return leg validation
        rules.add(new Rule("total-return-parties-required", 
                          "#totalReturnPayerParty != null && #totalReturnReceiverParty != null", 
                          "Total return payer and receiver parties are required"));
        
        rules.add(new Rule("total-return-parties-different", 
                          "#totalReturnPayerParty != null && #totalReturnReceiverParty != null && " +
                          "#totalReturnPayerParty != #totalReturnReceiverParty", 
                          "Total return payer and receiver must be different parties"));
        
        rules.add(new Rule("total-return-parties-valid", 
                          "#totalReturnPayerParty != null && #totalReturnReceiverParty != null && " +
                          "(#totalReturnPayerParty == 'CLIENT' || #totalReturnPayerParty == 'COUNTERPARTY') && " +
                          "(#totalReturnReceiverParty == 'CLIENT' || #totalReturnReceiverParty == 'COUNTERPARTY')", 
                          "Total return parties must be CLIENT or COUNTERPARTY"));
        
        // Funding leg validation
        rules.add(new Rule("funding-rate-type-valid", 
                          "#fundingRateType == null || " +
                          "(#fundingRateType == 'LIBOR' || #fundingRateType == 'SOFR' || #fundingRateType == 'FIXED')", 
                          "Funding rate type must be LIBOR, SOFR, or FIXED if specified"));
        
        rules.add(new Rule("funding-spread-range", 
                          "#fundingSpread == null || " +
                          "(#fundingSpread.compareTo(T(java.math.BigDecimal).new('-1000')) >= 0 && " +
                          "#fundingSpread.compareTo(T(java.math.BigDecimal).new('1000')) <= 0)",
                          "Funding spread must be between -1000 and +1000 basis points if specified"));
        
        rules.add(new Rule("fixed-rate-required-for-fixed-funding", 
                          "#fundingRateType != 'FIXED' || (#fundingRateType == 'FIXED' && #fixedRate != null)", 
                          "Fixed rate is required when funding rate type is FIXED"));
        
        // Settlement validation
        rules.add(new Rule("settlement-type-valid", 
                          "#settlementType == null || (#settlementType == 'CASH' || #settlementType == 'PHYSICAL')", 
                          "Settlement type must be CASH or PHYSICAL if specified"));
        
        rules.add(new Rule("settlement-days-range", 
                          "#settlementDays == null || (#settlementDays >= 0 && #settlementDays <= 10)", 
                          "Settlement days must be between 0 and 10 if specified"));
        
        return rules;
    }
    
    /**
     * Creates regulatory compliance rules.
     */
    private static List<Rule> createRegulatoryComplianceRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Jurisdiction validation
        rules.add(new Rule("jurisdiction-valid", 
                          "#jurisdiction == null || " +
                          "(#jurisdiction == 'US' || #jurisdiction == 'UK' || #jurisdiction == 'EU' || " +
                          "#jurisdiction == 'ASIA' || #jurisdiction == 'OTHER')", 
                          "Jurisdiction must be US, UK, EU, ASIA, or OTHER if specified"));
        
        // Regulatory regime validation
        rules.add(new Rule("regulatory-regime-valid", 
                          "#regulatoryRegime == null || " +
                          "(#regulatoryRegime == 'DODD_FRANK' || #regulatoryRegime == 'EMIR' || " +
                          "#regulatoryRegime == 'MiFID_II' || #regulatoryRegime == 'OTHER')", 
                          "Regulatory regime must be DODD_FRANK, EMIR, MiFID_II, or OTHER if specified"));
        
        // US Dodd-Frank specific rules
        rules.add(new Rule("dodd-frank-clearing-requirement", 
                          "#regulatoryRegime != 'DODD_FRANK' || " +
                          "(#regulatoryRegime == 'DODD_FRANK' && #clearingEligible != null)", 
                          "Clearing eligibility must be specified for Dodd-Frank trades"));
        
        // EMIR specific rules
        rules.add(new Rule("emir-lei-requirement", 
                          "#regulatoryRegime != 'EMIR' || " +
                          "(#regulatoryRegime == 'EMIR' && #counterpartyLei != null)", 
                          "Counterparty LEI is required for EMIR trades"));
        
        return rules;
    }
    
    /**
     * Creates risk management rules.
     */
    private static List<Rule> createRiskManagementRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Concentration limits
        rules.add(new Rule("single-counterparty-limit",
                          "#notionalAmount != null && #notionalAmount.compareTo(T(java.math.BigDecimal).new('50000000')) <= 0",
                          "Single counterparty exposure limit is $50,000,000"));
        
        // Commodity-specific limits
        rules.add(new Rule("energy-commodity-limit",
                          "#commodityType != 'ENERGY' || " +
                          "(#commodityType == 'ENERGY' && #notionalAmount.compareTo(T(java.math.BigDecimal).new('100000000')) <= 0)",
                          "Energy commodity exposure limit is $100,000,000"));
        
        rules.add(new Rule("metals-commodity-limit",
                          "#commodityType != 'METALS' || " +
                          "(#commodityType == 'METALS' && #notionalAmount.compareTo(T(java.math.BigDecimal).new('75000000')) <= 0)",
                          "Metals commodity exposure limit is $75,000,000"));
        
        rules.add(new Rule("agricultural-commodity-limit",
                          "#commodityType != 'AGRICULTURAL' || " +
                          "(#commodityType == 'AGRICULTURAL' && #notionalAmount.compareTo(T(java.math.BigDecimal).new('25000000')) <= 0)",
                          "Agricultural commodity exposure limit is $25,000,000"));
        
        // Maturity-based risk limits
        rules.add(new Rule("long-term-trade-limit", 
                          "#tradeDate == null || #maturityDate == null || " +
                          "#maturityDate.isBefore(#tradeDate.plusYears(5)) || " +
                          "(#maturityDate.compareTo(#tradeDate.plusYears(5)) >= 0 && " +
                          "#notionalAmount.compareTo(T(java.math.BigDecimal).new('25000000')) <= 0)",
                          "Long-term trades (>5 years) are limited to $25,000,000 notional"));
        
        return rules;
    }
    
    /**
     * Creates a rule set for static data validation.
     * 
     * @return RuleGroup containing static data validation rules
     */
    public static RuleGroup createStaticDataValidationRules() {
        List<Rule> rules = new ArrayList<>();
        
        // Client static data validation
        rules.add(new Rule("client-exists-and-active", 
                          "#staticDataProvider.isValidClient(#clientId)", 
                          "Client must exist and be active"));
        
        rules.add(new Rule("client-account-exists-and-active", 
                          "#staticDataProvider.getClientAccount(#clientAccountId) != null && " +
                          "#staticDataProvider.getClientAccount(#clientAccountId).active", 
                          "Client account must exist and be active"));
        
        rules.add(new Rule("client-account-belongs-to-client", 
                          "#staticDataProvider.getClientAccount(#clientAccountId) != null && " +
                          "#staticDataProvider.getClientAccount(#clientAccountId).clientId == #clientId", 
                          "Client account must belong to the specified client"));
        
        // Counterparty static data validation
        rules.add(new Rule("counterparty-exists-and-active", 
                          "#staticDataProvider.isValidCounterparty(#counterpartyId)", 
                          "Counterparty must exist and be active"));
        
        // Currency static data validation
        rules.add(new Rule("notional-currency-valid", 
                          "#staticDataProvider.isValidCurrency(#notionalCurrency)", 
                          "Notional currency must be valid and tradeable"));
        
        rules.add(new Rule("payment-currency-valid", 
                          "#staticDataProvider.isValidCurrency(#paymentCurrency)", 
                          "Payment currency must be valid and tradeable"));
        
        // Commodity static data validation
        rules.add(new Rule("commodity-reference-valid", 
                          "#staticDataProvider.isValidCommodity(#referenceIndex)", 
                          "Commodity reference index must be valid and tradeable"));
        
        RuleGroup ruleGroup = new RuleGroup("static-data-validation", "validation", "Static Data Validation Rules", "Validation rules for static reference data", 20, true);

        // Add all rules to the group
        int sequenceNumber = 1;
        for (Rule rule : rules) {
            ruleGroup.addRule(rule, sequenceNumber++);
        }

        return ruleGroup;
    }
    
    /**
     * Creates a rule set for post-trade processing validation.
     * 
     * @return RuleGroup containing post-trade processing rules
     */
    public static RuleGroup createPostTradeProcessingRules() {
        List<Rule> rules = new ArrayList<>();
        
        // TradeB status validation
        rules.add(new Rule("trade-status-valid", 
                          "#tradeStatus != null && " +
                          "(#tradeStatus == 'PENDING' || #tradeStatus == 'CONFIRMED' || " +
                          "#tradeStatus == 'SETTLED' || #tradeStatus == 'CANCELLED')", 
                          "TradeB status must be PENDING, CONFIRMED, SETTLED, or CANCELLED"));
        
        // Booking status validation
        rules.add(new Rule("booking-status-valid", 
                          "#bookingStatus != null && " +
                          "(#bookingStatus == 'PENDING' || #bookingStatus == 'BOOKED' || #bookingStatus == 'FAILED')", 
                          "Booking status must be PENDING, BOOKED, or FAILED"));
        
        // Settlement validation
        rules.add(new Rule("settlement-currency-matches-payment", 
                          "#settlementCurrency == null || #settlementCurrency == #paymentCurrency", 
                          "Settlement currency must match payment currency"));
        
        RuleGroup ruleGroup = new RuleGroup("post-trade-processing", "business", "Post-TradeB Processing Rules", "Rules for post-trade processing and settlement", 30, true);

        // Add all rules to the group
        int sequenceNumber = 1;
        for (Rule rule : rules) {
            ruleGroup.addRule(rule, sequenceNumber++);
        }

        return ruleGroup;
    }
}
