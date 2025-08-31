package dev.mars.apex.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Enhanced Settlement Instruction model for the Custody Auto-Repair Bootstrap.
 * This class represents settlement instructions in Asian markets with comprehensive
 * fields for auto-repair scenarios and audit trail support.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 1.0
 */
public class BootstrapSettlementInstruction {
    
    // Instruction Identification
    private String instructionId;
    private String externalInstructionId;
    private LocalDate instructionDate;
    private LocalDate tradeDate;
    private LocalDate settlementDate;
    private LocalDateTime createdDateTime;
    
    // Client Information
    private String clientId;
    private String clientName;
    private String clientAccountId;
    private String clientAccountType; // "SEGREGATED", "OMNIBUS", "MARGIN"
    private String clientTier; // "PREMIUM", "STANDARD", "BASIC"
    
    // Market Information
    private String market; // "JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"
    private String marketMic; // Market Identifier Code
    private String localMarketCode;
    private String marketTimezone;
    private String regulatoryRegime;
    
    // Instrument Information
    private String instrumentType; // "EQUITY", "FIXED_INCOME", "FX", "DERIVATIVES"
    private String instrumentId;
    private String isin; // International Securities Identification Number
    private String localInstrumentCode;
    private String instrumentName;
    private String currency;
    private String issuerCountry;
    
    // Settlement Details
    private BigDecimal settlementAmount;
    private String settlementCurrency;
    private String settlementMethod; // "DVP", "FOP", "CASH", "PVP"
    private String deliveryInstruction; // "DELIVER", "RECEIVE"
    private String settlementCycle; // "T+0", "T+1", "T+2", "T+3"
    
    // Counterparty Information (auto-repair targets)
    private String counterpartyId;
    private String counterpartyName;
    private String counterpartyBic; // Bank Identifier Code
    private String counterpartyAccount;
    private String counterpartyType; // "PRIME_BROKER", "CUSTODIAN", "CLEARING_MEMBER"
    
    // Custodial Information (auto-repair targets)
    private String custodianId;
    private String custodianName;
    private String custodianBic;
    private String custodialAccount;
    private String safekeepingAccount;
    private String custodianType; // "GLOBAL", "LOCAL", "SUB_CUSTODIAN"
    
    // Status and Validation
    private String instructionStatus; // "PENDING", "VALIDATED", "FAILED", "REPAIRED", "SETTLED"
    private String validationStatus; // "VALID", "INVALID", "INCOMPLETE", "AMBIGUOUS"
    private List<String> validationErrors;
    private List<String> missingFields;
    private List<String> ambiguousFields;
    
    // Auto-Repair Control
    private boolean requiresRepair;
    private boolean highValueTransaction; // Triggers manual intervention
    private boolean clientOptOut; // Client opted out of auto-repair
    private String repairReason;
    private BigDecimal highValueThreshold;
    
    // Business Context
    private String businessUnit;
    private String tradingDesk;
    private String portfolioId;
    private String fundId;
    private BigDecimal transactionValue;
    private String riskCategory; // "LOW", "MEDIUM", "HIGH"
    
    // Applied Standing Instructions (populated during enrichment)
    private BootstrapStandingInstruction applicableClientSI;
    private BootstrapStandingInstruction applicableMarketSI;
    private BootstrapStandingInstruction applicableInstrumentSI;
    
    // Audit and Tracking
    private String sourceSystem;
    private String processedBy;
    private LocalDateTime lastModified;
    private Map<String, Object> originalValues;
    private Map<String, String> auditTrail;
    
    // Default constructor
    public BootstrapSettlementInstruction() {
        this.validationErrors = new ArrayList<>();
        this.missingFields = new ArrayList<>();
        this.ambiguousFields = new ArrayList<>();
        this.originalValues = new HashMap<>();
        this.auditTrail = new HashMap<>();
        this.createdDateTime = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.highValueThreshold = new BigDecimal("10000000"); // $10M default
    }
    
    // Constructor for basic instruction
    public BootstrapSettlementInstruction(String instructionId, String clientId, String market, 
                                        String instrumentType, BigDecimal settlementAmount, 
                                        String currency, LocalDate settlementDate) {
        this();
        this.instructionId = instructionId;
        this.clientId = clientId;
        this.market = market;
        this.instrumentType = instrumentType;
        this.settlementAmount = settlementAmount;
        this.settlementCurrency = currency;
        this.currency = currency;
        this.settlementDate = settlementDate;
        this.instructionDate = LocalDate.now();
        this.tradeDate = LocalDate.now();
        
        // Determine if high value transaction
        this.highValueTransaction = settlementAmount.compareTo(highValueThreshold) >= 0;
        
        // Check if repair is required
        this.requiresRepair = checkIfRepairRequired();
    }
    
    /**
     * Check if this instruction requires repair based on missing fields.
     */
    private boolean checkIfRepairRequired() {
        return counterpartyId == null || custodianId == null || settlementMethod == null;
    }
    
    /**
     * Check if this instruction is eligible for auto-repair.
     */
    public boolean isEligibleForAutoRepair() {
        return requiresRepair && !highValueTransaction && !clientOptOut;
    }
    
    /**
     * Add a missing field to the list.
     */
    public void addMissingField(String fieldName) {
        if (!missingFields.contains(fieldName)) {
            missingFields.add(fieldName);
        }
        this.requiresRepair = true;
    }
    
    /**
     * Add an audit trail entry.
     */
    public void addAuditEntry(String action, String details) {
        auditTrail.put(LocalDateTime.now().toString() + "_" + action, details);
    }
    
    /**
     * Store original value before modification.
     */
    public void storeOriginalValue(String fieldName, Object value) {
        originalValues.put(fieldName, value);
    }
    
    // Getters and Setters
    public String getInstructionId() { return instructionId; }
    public void setInstructionId(String instructionId) { this.instructionId = instructionId; }
    
    public String getExternalInstructionId() { return externalInstructionId; }
    public void setExternalInstructionId(String externalInstructionId) { this.externalInstructionId = externalInstructionId; }
    
    public LocalDate getInstructionDate() { return instructionDate; }
    public void setInstructionDate(LocalDate instructionDate) { this.instructionDate = instructionDate; }
    
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    
    public LocalDateTime getCreatedDateTime() { return createdDateTime; }
    public void setCreatedDateTime(LocalDateTime createdDateTime) { this.createdDateTime = createdDateTime; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getClientAccountId() { return clientAccountId; }
    public void setClientAccountId(String clientAccountId) { this.clientAccountId = clientAccountId; }
    
    public String getClientAccountType() { return clientAccountType; }
    public void setClientAccountType(String clientAccountType) { this.clientAccountType = clientAccountType; }
    
    public String getClientTier() { return clientTier; }
    public void setClientTier(String clientTier) { this.clientTier = clientTier; }
    
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    
    public String getMarketMic() { return marketMic; }
    public void setMarketMic(String marketMic) { this.marketMic = marketMic; }
    
    public String getLocalMarketCode() { return localMarketCode; }
    public void setLocalMarketCode(String localMarketCode) { this.localMarketCode = localMarketCode; }
    
    public String getMarketTimezone() { return marketTimezone; }
    public void setMarketTimezone(String marketTimezone) { this.marketTimezone = marketTimezone; }
    
    public String getRegulatoryRegime() { return regulatoryRegime; }
    public void setRegulatoryRegime(String regulatoryRegime) { this.regulatoryRegime = regulatoryRegime; }
    
    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
    
    public String getInstrumentId() { return instrumentId; }
    public void setInstrumentId(String instrumentId) { this.instrumentId = instrumentId; }
    
    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }
    
    public String getLocalInstrumentCode() { return localInstrumentCode; }
    public void setLocalInstrumentCode(String localInstrumentCode) { this.localInstrumentCode = localInstrumentCode; }
    
    public String getInstrumentName() { return instrumentName; }
    public void setInstrumentName(String instrumentName) { this.instrumentName = instrumentName; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getIssuerCountry() { return issuerCountry; }
    public void setIssuerCountry(String issuerCountry) { this.issuerCountry = issuerCountry; }
    
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(BigDecimal settlementAmount) { this.settlementAmount = settlementAmount; }
    
    public String getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }
    
    public String getSettlementMethod() { return settlementMethod; }
    public void setSettlementMethod(String settlementMethod) { 
        if (this.settlementMethod == null && settlementMethod != null) {
            storeOriginalValue("settlementMethod", this.settlementMethod);
        }
        this.settlementMethod = settlementMethod; 
    }
    
    public String getDeliveryInstruction() { return deliveryInstruction; }
    public void setDeliveryInstruction(String deliveryInstruction) { this.deliveryInstruction = deliveryInstruction; }
    
    public String getSettlementCycle() { return settlementCycle; }
    public void setSettlementCycle(String settlementCycle) { this.settlementCycle = settlementCycle; }
    
    public String getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(String counterpartyId) { 
        if (this.counterpartyId == null && counterpartyId != null) {
            storeOriginalValue("counterpartyId", this.counterpartyId);
        }
        this.counterpartyId = counterpartyId; 
    }
    
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    
    public String getCounterpartyBic() { return counterpartyBic; }
    public void setCounterpartyBic(String counterpartyBic) { this.counterpartyBic = counterpartyBic; }
    
    public String getCounterpartyAccount() { return counterpartyAccount; }
    public void setCounterpartyAccount(String counterpartyAccount) { this.counterpartyAccount = counterpartyAccount; }
    
    public String getCounterpartyType() { return counterpartyType; }
    public void setCounterpartyType(String counterpartyType) { this.counterpartyType = counterpartyType; }
    
    public String getCustodianId() { return custodianId; }
    public void setCustodianId(String custodianId) { 
        if (this.custodianId == null && custodianId != null) {
            storeOriginalValue("custodianId", this.custodianId);
        }
        this.custodianId = custodianId; 
    }
    
    public String getCustodianName() { return custodianName; }
    public void setCustodianName(String custodianName) { this.custodianName = custodianName; }
    
    public String getCustodianBic() { return custodianBic; }
    public void setCustodianBic(String custodianBic) { this.custodianBic = custodianBic; }
    
    public String getCustodialAccount() { return custodialAccount; }
    public void setCustodialAccount(String custodialAccount) { this.custodialAccount = custodialAccount; }
    
    public String getSafekeepingAccount() { return safekeepingAccount; }
    public void setSafekeepingAccount(String safekeepingAccount) { this.safekeepingAccount = safekeepingAccount; }
    
    public String getCustodianType() { return custodianType; }
    public void setCustodianType(String custodianType) { this.custodianType = custodianType; }
    
    public String getInstructionStatus() { return instructionStatus; }
    public void setInstructionStatus(String instructionStatus) { this.instructionStatus = instructionStatus; }
    
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    
    public List<String> getValidationErrors() { return validationErrors; }
    public void setValidationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; }
    
    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }
    
    public List<String> getAmbiguousFields() { return ambiguousFields; }
    public void setAmbiguousFields(List<String> ambiguousFields) { this.ambiguousFields = ambiguousFields; }
    
    public boolean isRequiresRepair() { return requiresRepair; }
    public void setRequiresRepair(boolean requiresRepair) { this.requiresRepair = requiresRepair; }
    
    public boolean isHighValueTransaction() { return highValueTransaction; }
    public void setHighValueTransaction(boolean highValueTransaction) { this.highValueTransaction = highValueTransaction; }
    
    public boolean isClientOptOut() { return clientOptOut; }
    public void setClientOptOut(boolean clientOptOut) { this.clientOptOut = clientOptOut; }
    
    public String getRepairReason() { return repairReason; }
    public void setRepairReason(String repairReason) { this.repairReason = repairReason; }
    
    public BigDecimal getHighValueThreshold() { return highValueThreshold; }
    public void setHighValueThreshold(BigDecimal highValueThreshold) { this.highValueThreshold = highValueThreshold; }
    
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    
    public String getTradingDesk() { return tradingDesk; }
    public void setTradingDesk(String tradingDesk) { this.tradingDesk = tradingDesk; }
    
    public String getPortfolioId() { return portfolioId; }
    public void setPortfolioId(String portfolioId) { this.portfolioId = portfolioId; }
    
    public String getFundId() { return fundId; }
    public void setFundId(String fundId) { this.fundId = fundId; }
    
    public BigDecimal getTransactionValue() { return transactionValue; }
    public void setTransactionValue(BigDecimal transactionValue) { this.transactionValue = transactionValue; }
    
    public String getRiskCategory() { return riskCategory; }
    public void setRiskCategory(String riskCategory) { this.riskCategory = riskCategory; }
    
    public BootstrapStandingInstruction getApplicableClientSI() { return applicableClientSI; }
    public void setApplicableClientSI(BootstrapStandingInstruction applicableClientSI) { this.applicableClientSI = applicableClientSI; }
    
    public BootstrapStandingInstruction getApplicableMarketSI() { return applicableMarketSI; }
    public void setApplicableMarketSI(BootstrapStandingInstruction applicableMarketSI) { this.applicableMarketSI = applicableMarketSI; }
    
    public BootstrapStandingInstruction getApplicableInstrumentSI() { return applicableInstrumentSI; }
    public void setApplicableInstrumentSI(BootstrapStandingInstruction applicableInstrumentSI) { this.applicableInstrumentSI = applicableInstrumentSI; }
    
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public Map<String, Object> getOriginalValues() { return originalValues; }
    public void setOriginalValues(Map<String, Object> originalValues) { this.originalValues = originalValues; }
    
    public Map<String, String> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(Map<String, String> auditTrail) { this.auditTrail = auditTrail; }
}
