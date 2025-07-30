package dev.mars.apex.demo.bootstrap.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
 * Enhanced Standing Instruction model for the Custody Auto-Repair Bootstrap.
 * This class represents standing instructions with comprehensive metadata
 * and Asian market-specific configurations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 1.0
 */
public class BootstrapStandingInstruction {
    
    // Identification
    private String siId;
    private String siName;
    private String description;
    private String version;
    
    // Scope and Applicability
    private String scopeType; // "CLIENT", "MARKET", "INSTRUMENT", "GLOBAL"
    private String clientId;
    private String market;
    private String instrumentType;
    private String region; // "ASIA_PACIFIC", "EMEA", "AMERICAS"
    
    // Rule Matching and Weighting
    private String applicabilityCondition; // SpEL expression
    private int priority;
    private double weight; // 0.6 (client), 0.3 (market), 0.1 (instrument)
    private double confidenceLevel; // 0.0 to 1.0
    
    // Default Values for Auto-Repair
    private String defaultCounterpartyId;
    private String defaultCounterpartyName;
    private String defaultCounterpartyBic;
    private String defaultCounterpartyAccount;
    private String defaultCounterpartyType;
    
    private String defaultCustodianId;
    private String defaultCustodianName;
    private String defaultCustodianBic;
    private String defaultCustodialAccount;
    private String defaultSafekeepingAccount;
    private String defaultCustodianType;
    
    private String defaultSettlementMethod;
    private String defaultDeliveryInstruction;
    private String defaultSettlementCycle;
    
    // Asian Market Specific Fields
    private String marketMic; // Market Identifier Code
    private String localMarketCode;
    private String baseCurrency;
    private String holidayCalendar;
    private String regulatoryRegime;
    private String tradingHours;
    
    // Status and Control
    private boolean enabled;
    private boolean requiresApproval;
    private String approvalStatus;
    private String riskCategory; // "LOW", "MEDIUM", "HIGH"
    private String businessJustification;
    
    // Usage Statistics
    private int usageCount;
    private LocalDate lastUsedDate;
    private double successRate;
    private LocalDateTime createdDateTime;
    private LocalDateTime lastModified;
    
    // Audit and Metadata
    private String createdBy;
    private String lastModifiedBy;
    private String sourceSystem;
    private Map<String, String> customProperties;
    
    // Default constructor
    public BootstrapStandingInstruction() {
        this.customProperties = new HashMap<>();
        this.createdDateTime = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.enabled = true;
        this.usageCount = 0;
        this.successRate = 0.0;
    }
    
    // Constructor for client-level SI
    public BootstrapStandingInstruction(String siId, String clientId, String siName) {
        this();
        this.siId = siId;
        this.clientId = clientId;
        this.siName = siName;
        this.scopeType = "CLIENT";
        this.weight = 0.6;
        this.priority = 100;
        this.confidenceLevel = 0.95;
    }
    
    // Constructor for market-level SI
    public BootstrapStandingInstruction(String siId, String market, String siName, boolean isMarketLevel) {
        this();
        this.siId = siId;
        this.market = market;
        this.siName = siName;
        this.scopeType = "MARKET";
        this.weight = 0.3;
        this.priority = 200;
        this.confidenceLevel = 0.85;
    }
    
    // Constructor for instrument-level SI
    public BootstrapStandingInstruction(String siId, String instrumentType, String siName, int priority) {
        this();
        this.siId = siId;
        this.instrumentType = instrumentType;
        this.siName = siName;
        this.scopeType = "INSTRUMENT";
        this.weight = 0.1;
        this.priority = priority;
        this.confidenceLevel = 0.75;
    }
    
    /**
     * Check if this SI is applicable to the given settlement instruction.
     */
    public boolean isApplicableToInstruction(BootstrapSettlementInstruction instruction) {
        if (!enabled) {
            return false;
        }
        
        switch (scopeType) {
            case "CLIENT":
                return clientId != null && clientId.equals(instruction.getClientId());
            case "MARKET":
                return market != null && market.equals(instruction.getMarket());
            case "INSTRUMENT":
                return instrumentType != null && instrumentType.equals(instruction.getInstrumentType());
            case "GLOBAL":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Check if this SI has a default value for the specified field.
     */
    public boolean hasDefaultValue(String fieldName) {
        return getDefaultValue(fieldName) != null;
    }
    
    /**
     * Get the default value for the specified field.
     */
    public String getDefaultValue(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "counterpartyid":
                return defaultCounterpartyId;
            case "counterpartyname":
                return defaultCounterpartyName;
            case "counterpartybic":
                return defaultCounterpartyBic;
            case "counterpartyaccount":
                return defaultCounterpartyAccount;
            case "counterpartytype":
                return defaultCounterpartyType;
            case "custodianid":
                return defaultCustodianId;
            case "custodianname":
                return defaultCustodianName;
            case "custodianbic":
                return defaultCustodianBic;
            case "custodialaccount":
                return defaultCustodialAccount;
            case "safekeepingaccount":
                return defaultSafekeepingAccount;
            case "custodiantype":
                return defaultCustodianType;
            case "settlementmethod":
                return defaultSettlementMethod;
            case "deliveryinstruction":
                return defaultDeliveryInstruction;
            case "settlementcycle":
                return defaultSettlementCycle;
            default:
                return customProperties.get(fieldName);
        }
    }
    
    /**
     * Increment usage count and update last used date.
     */
    public void incrementUsage() {
        this.usageCount++;
        this.lastUsedDate = LocalDate.now();
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Update success rate based on repair outcome.
     */
    public void updateSuccessRate(boolean successful) {
        if (usageCount == 0) {
            successRate = successful ? 1.0 : 0.0;
        } else {
            // Calculate running average
            double totalSuccesses = successRate * (usageCount - 1);
            if (successful) {
                totalSuccesses++;
            }
            successRate = totalSuccesses / usageCount;
        }
    }
    
    /**
     * Add a custom property.
     */
    public void addCustomProperty(String key, String value) {
        customProperties.put(key, value);
    }
    
    // Getters and Setters
    public String getSiId() { return siId; }
    public void setSiId(String siId) { this.siId = siId; }
    
    public String getSiName() { return siName; }
    public void setSiName(String siName) { this.siName = siName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    
    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public String getApplicabilityCondition() { return applicabilityCondition; }
    public void setApplicabilityCondition(String applicabilityCondition) { this.applicabilityCondition = applicabilityCondition; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    
    public double getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(double confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    
    public String getDefaultCounterpartyId() { return defaultCounterpartyId; }
    public void setDefaultCounterpartyId(String defaultCounterpartyId) { this.defaultCounterpartyId = defaultCounterpartyId; }
    
    public String getDefaultCounterpartyName() { return defaultCounterpartyName; }
    public void setDefaultCounterpartyName(String defaultCounterpartyName) { this.defaultCounterpartyName = defaultCounterpartyName; }
    
    public String getDefaultCounterpartyBic() { return defaultCounterpartyBic; }
    public void setDefaultCounterpartyBic(String defaultCounterpartyBic) { this.defaultCounterpartyBic = defaultCounterpartyBic; }
    
    public String getDefaultCounterpartyAccount() { return defaultCounterpartyAccount; }
    public void setDefaultCounterpartyAccount(String defaultCounterpartyAccount) { this.defaultCounterpartyAccount = defaultCounterpartyAccount; }
    
    public String getDefaultCounterpartyType() { return defaultCounterpartyType; }
    public void setDefaultCounterpartyType(String defaultCounterpartyType) { this.defaultCounterpartyType = defaultCounterpartyType; }
    
    public String getDefaultCustodianId() { return defaultCustodianId; }
    public void setDefaultCustodianId(String defaultCustodianId) { this.defaultCustodianId = defaultCustodianId; }
    
    public String getDefaultCustodianName() { return defaultCustodianName; }
    public void setDefaultCustodianName(String defaultCustodianName) { this.defaultCustodianName = defaultCustodianName; }
    
    public String getDefaultCustodianBic() { return defaultCustodianBic; }
    public void setDefaultCustodianBic(String defaultCustodianBic) { this.defaultCustodianBic = defaultCustodianBic; }
    
    public String getDefaultCustodialAccount() { return defaultCustodialAccount; }
    public void setDefaultCustodialAccount(String defaultCustodialAccount) { this.defaultCustodialAccount = defaultCustodialAccount; }
    
    public String getDefaultSafekeepingAccount() { return defaultSafekeepingAccount; }
    public void setDefaultSafekeepingAccount(String defaultSafekeepingAccount) { this.defaultSafekeepingAccount = defaultSafekeepingAccount; }
    
    public String getDefaultCustodianType() { return defaultCustodianType; }
    public void setDefaultCustodianType(String defaultCustodianType) { this.defaultCustodianType = defaultCustodianType; }
    
    public String getDefaultSettlementMethod() { return defaultSettlementMethod; }
    public void setDefaultSettlementMethod(String defaultSettlementMethod) { this.defaultSettlementMethod = defaultSettlementMethod; }
    
    public String getDefaultDeliveryInstruction() { return defaultDeliveryInstruction; }
    public void setDefaultDeliveryInstruction(String defaultDeliveryInstruction) { this.defaultDeliveryInstruction = defaultDeliveryInstruction; }
    
    public String getDefaultSettlementCycle() { return defaultSettlementCycle; }
    public void setDefaultSettlementCycle(String defaultSettlementCycle) { this.defaultSettlementCycle = defaultSettlementCycle; }
    
    public String getMarketMic() { return marketMic; }
    public void setMarketMic(String marketMic) { this.marketMic = marketMic; }
    
    public String getLocalMarketCode() { return localMarketCode; }
    public void setLocalMarketCode(String localMarketCode) { this.localMarketCode = localMarketCode; }
    
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    
    public String getHolidayCalendar() { return holidayCalendar; }
    public void setHolidayCalendar(String holidayCalendar) { this.holidayCalendar = holidayCalendar; }
    
    public String getRegulatoryRegime() { return regulatoryRegime; }
    public void setRegulatoryRegime(String regulatoryRegime) { this.regulatoryRegime = regulatoryRegime; }
    
    public String getTradingHours() { return tradingHours; }
    public void setTradingHours(String tradingHours) { this.tradingHours = tradingHours; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isRequiresApproval() { return requiresApproval; }
    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    
    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    
    public String getBusinessJustification() { return businessJustification; }
    public void setBusinessJustification(String businessJustification) { this.businessJustification = businessJustification; }
    
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    
    public LocalDate getLastUsedDate() { return lastUsedDate; }
    public void setLastUsedDate(LocalDate lastUsedDate) { this.lastUsedDate = lastUsedDate; }
    
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    
    public LocalDateTime getCreatedDateTime() { return createdDateTime; }
    public void setCreatedDateTime(LocalDateTime createdDateTime) { this.createdDateTime = createdDateTime; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
    
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    
    public Map<String, String> getCustomProperties() { return customProperties; }
    public void setCustomProperties(Map<String, String> customProperties) { this.customProperties = customProperties; }
}
