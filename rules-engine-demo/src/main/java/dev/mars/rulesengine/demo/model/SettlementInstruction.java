package dev.mars.rulesengine.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Represents a settlement instruction in custody and safekeeping operations.
 * This class models instructions that may require auto-repair when missing
 * or ambiguous fields are detected.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class SettlementInstruction {
    
    // Instruction Identification
    private String instructionId;
    private String externalInstructionId;
    private LocalDate instructionDate;
    private LocalDate tradeDate;
    private LocalDate settlementDate;
    
    // Client Information
    private String clientId;
    private String clientName;
    private String clientAccountId;
    private String clientAccountType; // e.g., "SEGREGATED", "OMNIBUS"
    
    // Market Information
    private String market; // e.g., "JAPAN", "HONG_KONG", "SINGAPORE", "KOREA"
    private String marketMic; // Market Identifier Code
    private String localMarketCode;
    
    // Instrument Information
    private String instrumentType; // e.g., "EQUITY", "FIXED_INCOME", "FX", "DERIVATIVES"
    private String instrumentId;
    private String isin; // International Securities Identification Number
    private String localInstrumentCode;
    private String currency;
    
    // Settlement Details
    private BigDecimal settlementAmount;
    private String settlementCurrency;
    private String settlementMethod; // e.g., "DVP", "FOP", "CASH"
    private String deliveryInstruction; // e.g., "DELIVER", "RECEIVE"
    
    // Counterparty Information (may be missing - trigger for auto-repair)
    private String counterpartyId;
    private String counterpartyName;
    private String counterpartyBic; // Bank Identifier Code
    private String counterpartyAccount;
    
    // Custodial Information (may be missing - trigger for auto-repair)
    private String custodianId;
    private String custodianName;
    private String custodianBic;
    private String custodialAccount;
    private String safekeepingAccount;
    
    // Status and Validation
    private String instructionStatus; // "PENDING", "VALIDATED", "FAILED", "REPAIRED", "SETTLED"
    private String validationStatus; // "VALID", "INVALID", "INCOMPLETE", "AMBIGUOUS"
    private List<String> validationErrors;
    private List<String> missingFields;
    private List<String> ambiguousFields;
    
    // Auto-Repair Tracking
    private boolean requiresRepair;
    private boolean highValueTransaction; // Triggers manual intervention
    private boolean clientOptOut; // Client opted out of auto-repair
    private String repairReason;
    
    // Business Context
    private String businessUnit;
    private String tradingDesk;
    private String portfolioId;
    private BigDecimal transactionValue;
    
    // Default constructor
    public SettlementInstruction() {
        this.validationErrors = new ArrayList<>();
        this.missingFields = new ArrayList<>();
        this.ambiguousFields = new ArrayList<>();
        this.instructionStatus = "PENDING";
        this.validationStatus = "VALID";
        this.requiresRepair = false;
        this.highValueTransaction = false;
        this.clientOptOut = false;
    }
    
    // Constructor with essential fields
    public SettlementInstruction(String instructionId, String clientId, String market, 
                               String instrumentType, BigDecimal settlementAmount, 
                               String settlementCurrency, LocalDate settlementDate) {
        this();
        this.instructionId = instructionId;
        this.clientId = clientId;
        this.market = market;
        this.instrumentType = instrumentType;
        this.settlementAmount = settlementAmount;
        this.settlementCurrency = settlementCurrency;
        this.settlementDate = settlementDate;
        this.instructionDate = LocalDate.now();
    }
    
    // Getters and setters
    public String getInstructionId() {
        return instructionId;
    }
    
    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }
    
    public String getExternalInstructionId() {
        return externalInstructionId;
    }
    
    public void setExternalInstructionId(String externalInstructionId) {
        this.externalInstructionId = externalInstructionId;
    }
    
    public LocalDate getInstructionDate() {
        return instructionDate;
    }
    
    public void setInstructionDate(LocalDate instructionDate) {
        this.instructionDate = instructionDate;
    }
    
    public LocalDate getTradeDate() {
        return tradeDate;
    }
    
    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }
    
    public LocalDate getSettlementDate() {
        return settlementDate;
    }
    
    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientName() {
        return clientName;
    }
    
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
    
    public String getClientAccountId() {
        return clientAccountId;
    }
    
    public void setClientAccountId(String clientAccountId) {
        this.clientAccountId = clientAccountId;
    }
    
    public String getClientAccountType() {
        return clientAccountType;
    }
    
    public void setClientAccountType(String clientAccountType) {
        this.clientAccountType = clientAccountType;
    }
    
    public String getMarket() {
        return market;
    }
    
    public void setMarket(String market) {
        this.market = market;
    }
    
    public String getMarketMic() {
        return marketMic;
    }
    
    public void setMarketMic(String marketMic) {
        this.marketMic = marketMic;
    }
    
    public String getLocalMarketCode() {
        return localMarketCode;
    }
    
    public void setLocalMarketCode(String localMarketCode) {
        this.localMarketCode = localMarketCode;
    }
    
    public String getInstrumentType() {
        return instrumentType;
    }
    
    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }
    
    public String getInstrumentId() {
        return instrumentId;
    }
    
    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }
    
    public String getIsin() {
        return isin;
    }
    
    public void setIsin(String isin) {
        this.isin = isin;
    }
    
    public String getLocalInstrumentCode() {
        return localInstrumentCode;
    }
    
    public void setLocalInstrumentCode(String localInstrumentCode) {
        this.localInstrumentCode = localInstrumentCode;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }
    
    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }
    
    public String getSettlementCurrency() {
        return settlementCurrency;
    }
    
    public void setSettlementCurrency(String settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }
    
    public String getSettlementMethod() {
        return settlementMethod;
    }
    
    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }
    
    public String getDeliveryInstruction() {
        return deliveryInstruction;
    }
    
    public void setDeliveryInstruction(String deliveryInstruction) {
        this.deliveryInstruction = deliveryInstruction;
    }
    
    public String getCounterpartyId() {
        return counterpartyId;
    }
    
    public void setCounterpartyId(String counterpartyId) {
        this.counterpartyId = counterpartyId;
    }
    
    public String getCounterpartyName() {
        return counterpartyName;
    }
    
    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }
    
    public String getCounterpartyBic() {
        return counterpartyBic;
    }
    
    public void setCounterpartyBic(String counterpartyBic) {
        this.counterpartyBic = counterpartyBic;
    }
    
    public String getCounterpartyAccount() {
        return counterpartyAccount;
    }
    
    public void setCounterpartyAccount(String counterpartyAccount) {
        this.counterpartyAccount = counterpartyAccount;
    }
