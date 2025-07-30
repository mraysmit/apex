package dev.mars.rulesengine.demo.model;

import java.time.LocalDate;
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
