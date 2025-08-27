package dev.mars.apex.demo.bootstrap.model;

import java.time.LocalDate;

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
 * Represents a standing instruction used for auto-repair of settlement instructions.
 * Standing instructions contain predefined default values and rules for specific
 * clients, markets, or instruments.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class StandingInstruction {
    
    // Standing Instruction Identification
    private String siId;
    private String siName;
    private String description;
    private LocalDate createdDate;
    private LocalDate lastModifiedDate;
    private String createdBy;
    private String modifiedBy;
    
    // Scope and Applicability
    private String scopeType; // "CLIENT", "MARKET", "INSTRUMENT", "GLOBAL"
    private String clientId; // Specific client (null for market/instrument level)
    private String market; // Specific market (null for client/instrument level)
    private String instrumentType; // Specific instrument type (null for client/market level)
    
    // Rule Matching Criteria
    private String applicabilityCondition; // SpEL expression for when this SI applies
    private int priority; // Higher number = higher priority
    private double weight; // Weight for weighted decision making (0.0 - 1.0)
    private double confidenceLevel; // Confidence in this SI (0.0 - 1.0)
    
    // Default Values for Auto-Repair
    private String defaultCounterpartyId;
    private String defaultCounterpartyName;
    private String defaultCounterpartyBic;
    private String defaultCounterpartyAccount;
    
    private String defaultCustodianId;
    private String defaultCustodianName;
    private String defaultCustodianBic;
    private String defaultCustodialAccount;
    private String defaultSafekeepingAccount;
    
    private String defaultSettlementMethod;
    private String defaultDeliveryInstruction;
    
    // Status and Control
    private boolean enabled;
    private boolean requiresApproval; // Some SIs may require manual approval
    private String approvalStatus; // "PENDING", "APPROVED", "REJECTED"
    private String approvedBy;
    private LocalDate approvalDate;
    
    // Business Context
    private String businessJustification;
    private String riskCategory; // "LOW", "MEDIUM", "HIGH"
    private String complianceNotes;
    
    // Usage Statistics
    private int usageCount;
    private LocalDate lastUsedDate;
    private double successRate; // Percentage of successful applications
    
    // Default constructor
    public StandingInstruction() {
        this.createdDate = LocalDate.now();
        this.enabled = true;
        this.priority = 100;
        this.weight = 1.0;
        this.confidenceLevel = 1.0;
        this.requiresApproval = false;
        this.approvalStatus = "APPROVED";
        this.usageCount = 0;
        this.successRate = 0.0;
        this.riskCategory = "LOW";
    }
    
    // Constructor for client-specific SI
    public StandingInstruction(String siId, String clientId, String siName) {
        this();
        this.siId = siId;
        this.clientId = clientId;
        this.siName = siName;
        this.scopeType = "CLIENT";
        this.weight = 0.6; // Higher weight for client-specific rules
    }
    
    // Constructor for market-specific SI
    public StandingInstruction(String siId, String market, String siName, boolean isMarketLevel) {
        this();
        this.siId = siId;
        this.market = market;
        this.siName = siName;
        this.scopeType = "MARKET";
        this.weight = 0.3; // Medium weight for market-specific rules
    }
    
    // Constructor for instrument-specific SI
    public StandingInstruction(String siId, String instrumentType, String siName, int dummy) {
        this();
        this.siId = siId;
        this.instrumentType = instrumentType;
        this.siName = siName;
        this.scopeType = "INSTRUMENT";
        this.weight = 0.1; // Lower weight for instrument-specific rules
    }
    
    // Getters and setters
    public String getSiId() {
        return siId;
    }
    
    public void setSiId(String siId) {
        this.siId = siId;
    }
    
    public String getSiName() {
        return siName;
    }
    
    public void setSiName(String siName) {
        this.siName = siName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDate getCreatedDate() {
        return createdDate;
    }
    
    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
    
    public LocalDate getLastModifiedDate() {
        return lastModifiedDate;
    }
    
    public void setLastModifiedDate(LocalDate lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getModifiedBy() {
        return modifiedBy;
    }
    
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
    
    public String getScopeType() {
        return scopeType;
    }
    
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getMarket() {
        return market;
    }
    
    public void setMarket(String market) {
        this.market = market;
    }
    
    public String getInstrumentType() {
        return instrumentType;
    }
    
    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }
    
    public String getApplicabilityCondition() {
        return applicabilityCondition;
    }
    
    public void setApplicabilityCondition(String applicabilityCondition) {
        this.applicabilityCondition = applicabilityCondition;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public double getWeight() {
        return weight;
    }
    
    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public double getConfidenceLevel() {
        return confidenceLevel;
    }
    
    public void setConfidenceLevel(double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }
    
    public String getDefaultCounterpartyId() {
        return defaultCounterpartyId;
    }
    
    public void setDefaultCounterpartyId(String defaultCounterpartyId) {
        this.defaultCounterpartyId = defaultCounterpartyId;
    }
    
    public String getDefaultCounterpartyName() {
        return defaultCounterpartyName;
    }
    
    public void setDefaultCounterpartyName(String defaultCounterpartyName) {
        this.defaultCounterpartyName = defaultCounterpartyName;
    }
    
    public String getDefaultCounterpartyBic() {
        return defaultCounterpartyBic;
    }
    
    public void setDefaultCounterpartyBic(String defaultCounterpartyBic) {
        this.defaultCounterpartyBic = defaultCounterpartyBic;
    }
    
    public String getDefaultCounterpartyAccount() {
        return defaultCounterpartyAccount;
    }
    
    public void setDefaultCounterpartyAccount(String defaultCounterpartyAccount) {
        this.defaultCounterpartyAccount = defaultCounterpartyAccount;
    }
    
    public String getDefaultCustodianId() {
        return defaultCustodianId;
    }
    
    public void setDefaultCustodianId(String defaultCustodianId) {
        this.defaultCustodianId = defaultCustodianId;
    }
    
    public String getDefaultCustodianName() {
        return defaultCustodianName;
    }
    
    public void setDefaultCustodianName(String defaultCustodianName) {
        this.defaultCustodianName = defaultCustodianName;
    }
    
    public String getDefaultCustodianBic() {
        return defaultCustodianBic;
    }
    
    public void setDefaultCustodianBic(String defaultCustodianBic) {
        this.defaultCustodianBic = defaultCustodianBic;
    }
    
    public String getDefaultCustodialAccount() {
        return defaultCustodialAccount;
    }
    
    public void setDefaultCustodialAccount(String defaultCustodialAccount) {
        this.defaultCustodialAccount = defaultCustodialAccount;
    }
    
    public String getDefaultSafekeepingAccount() {
        return defaultSafekeepingAccount;
    }
    
    public void setDefaultSafekeepingAccount(String defaultSafekeepingAccount) {
        this.defaultSafekeepingAccount = defaultSafekeepingAccount;
    }
    
    public String getDefaultSettlementMethod() {
        return defaultSettlementMethod;
    }
    
    public void setDefaultSettlementMethod(String defaultSettlementMethod) {
        this.defaultSettlementMethod = defaultSettlementMethod;
    }
    
    public String getDefaultDeliveryInstruction() {
        return defaultDeliveryInstruction;
    }
    
    public void setDefaultDeliveryInstruction(String defaultDeliveryInstruction) {
        this.defaultDeliveryInstruction = defaultDeliveryInstruction;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getBusinessJustification() {
        return businessJustification;
    }

    public void setBusinessJustification(String businessJustification) {
        this.businessJustification = businessJustification;
    }

    public String getRiskCategory() {
        return riskCategory;
    }

    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }

    public String getComplianceNotes() {
        return complianceNotes;
    }

    public void setComplianceNotes(String complianceNotes) {
        this.complianceNotes = complianceNotes;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(int usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDate getLastUsedDate() {
        return lastUsedDate;
    }

    public void setLastUsedDate(LocalDate lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    // Utility methods
    public boolean isApplicableToInstruction(SettlementInstruction instruction) {
        if (!enabled || !"APPROVED".equals(approvalStatus)) {
            return false;
        }

        // Check scope-based applicability
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

    public void incrementUsage() {
        this.usageCount++;
        this.lastUsedDate = LocalDate.now();
    }

    public void updateSuccessRate(boolean wasSuccessful) {
        // Simple success rate calculation - in production this would be more sophisticated
        if (usageCount == 0) {
            successRate = wasSuccessful ? 100.0 : 0.0;
        } else {
            double totalSuccesses = (successRate / 100.0) * (usageCount - 1);
            if (wasSuccessful) {
                totalSuccesses++;
            }
            successRate = (totalSuccesses / usageCount) * 100.0;
        }
    }

    public boolean hasDefaultValue(String fieldName) {
        switch (fieldName.toLowerCase()) {
            case "counterpartyid":
                return defaultCounterpartyId != null;
            case "counterpartyname":
                return defaultCounterpartyName != null;
            case "counterpartybic":
                return defaultCounterpartyBic != null;
            case "counterpartyaccount":
                return defaultCounterpartyAccount != null;
            case "custodianid":
                return defaultCustodianId != null;
            case "custodianname":
                return defaultCustodianName != null;
            case "custodianbic":
                return defaultCustodianBic != null;
            case "custodialaccount":
                return defaultCustodialAccount != null;
            case "safekeepingaccount":
                return defaultSafekeepingAccount != null;
            case "settlementmethod":
                return defaultSettlementMethod != null;
            case "deliveryinstruction":
                return defaultDeliveryInstruction != null;
            default:
                return false;
        }
    }

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
            case "settlementmethod":
                return defaultSettlementMethod;
            case "deliveryinstruction":
                return defaultDeliveryInstruction;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return "StandingInstruction{" +
                "siId='" + siId + '\'' +
                ", siName='" + siName + '\'' +
                ", scopeType='" + scopeType + '\'' +
                ", clientId='" + clientId + '\'' +
                ", market='" + market + '\'' +
                ", instrumentType='" + instrumentType + '\'' +
                ", priority=" + priority +
                ", weight=" + weight +
                ", confidenceLevel=" + confidenceLevel +
                ", enabled=" + enabled +
                ", usageCount=" + usageCount +
                ", successRate=" + successRate +
                '}';
    }
}
