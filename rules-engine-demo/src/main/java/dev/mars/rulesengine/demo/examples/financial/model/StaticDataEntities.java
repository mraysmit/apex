package dev.mars.rulesengine.demo.examples.financial.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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
 * Static data entities used for validation and enrichment of financial instruments.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class StaticDataEntities {
    
    /**
     * Client static data for validation and enrichment.
     */
    public static class Client {
        private String clientId;
        private String clientName;
        private String legalEntityIdentifier; // LEI
        private String clientType; // "INSTITUTIONAL", "RETAIL", "PROFESSIONAL"
        private String jurisdiction;
        private String regulatoryClassification; // "ECP" (Eligible Contract Participant), "RETAIL", etc.
        private Boolean active;
        private LocalDate onboardingDate;
        private List<String> authorizedProducts; // Products client is authorized to trade
        private BigDecimal creditLimit;
        private String riskRating; // "LOW", "MEDIUM", "HIGH"
        
        public Client() {}
        
        public Client(String clientId, String clientName, String clientType, Boolean active) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.clientType = clientType;
            this.active = active;
        }
        
        // Getters and Setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public String getLegalEntityIdentifier() { return legalEntityIdentifier; }
        public void setLegalEntityIdentifier(String legalEntityIdentifier) { this.legalEntityIdentifier = legalEntityIdentifier; }
        
        public String getClientType() { return clientType; }
        public void setClientType(String clientType) { this.clientType = clientType; }
        
        public String getJurisdiction() { return jurisdiction; }
        public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
        
        public String getRegulatoryClassification() { return regulatoryClassification; }
        public void setRegulatoryClassification(String regulatoryClassification) { this.regulatoryClassification = regulatoryClassification; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public LocalDate getOnboardingDate() { return onboardingDate; }
        public void setOnboardingDate(LocalDate onboardingDate) { this.onboardingDate = onboardingDate; }
        
        public List<String> getAuthorizedProducts() { return authorizedProducts; }
        public void setAuthorizedProducts(List<String> authorizedProducts) { this.authorizedProducts = authorizedProducts; }
        
        public BigDecimal getCreditLimit() { return creditLimit; }
        public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
        
        public String getRiskRating() { return riskRating; }
        public void setRiskRating(String riskRating) { this.riskRating = riskRating; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Client client = (Client) o;
            return Objects.equals(clientId, client.clientId);
        }
        
        @Override
        public int hashCode() { return Objects.hash(clientId); }
    }
    
    /**
     * Client account static data.
     */
    public static class ClientAccount {
        private String accountId;
        private String clientId;
        private String accountType; // "SEGREGATED", "OMNIBUS", "HOUSE"
        private String accountName;
        private String baseCurrency;
        private Boolean active;
        private LocalDate openDate;
        private List<String> authorizedInstruments;
        private BigDecimal accountLimit;
        private String accountStatus; // "ACTIVE", "SUSPENDED", "CLOSED"
        
        public ClientAccount() {}
        
        public ClientAccount(String accountId, String clientId, String accountType, String baseCurrency, Boolean active) {
            this.accountId = accountId;
            this.clientId = clientId;
            this.accountType = accountType;
            this.baseCurrency = baseCurrency;
            this.active = active;
        }
        
        // Getters and Setters
        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        
        public String getBaseCurrency() { return baseCurrency; }
        public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public LocalDate getOpenDate() { return openDate; }
        public void setOpenDate(LocalDate openDate) { this.openDate = openDate; }
        
        public List<String> getAuthorizedInstruments() { return authorizedInstruments; }
        public void setAuthorizedInstruments(List<String> authorizedInstruments) { this.authorizedInstruments = authorizedInstruments; }
        
        public BigDecimal getAccountLimit() { return accountLimit; }
        public void setAccountLimit(BigDecimal accountLimit) { this.accountLimit = accountLimit; }
        
        public String getAccountStatus() { return accountStatus; }
        public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientAccount that = (ClientAccount) o;
            return Objects.equals(accountId, that.accountId);
        }
        
        @Override
        public int hashCode() { return Objects.hash(accountId); }
    }
    
    /**
     * Counterparty static data.
     */
    public static class Counterparty {
        private String counterpartyId;
        private String counterpartyName;
        private String legalEntityIdentifier; // LEI
        private String counterpartyType; // "BANK", "BROKER_DEALER", "HEDGE_FUND", "CORPORATE"
        private String jurisdiction;
        private String regulatoryStatus; // "AUTHORIZED", "REGISTERED", "EXEMPT"
        private Boolean active;
        private String ratingAgency;
        private String creditRating; // "AAA", "AA+", etc.
        private BigDecimal creditLimit;
        private List<String> authorizedProducts;
        
        public Counterparty() {}
        
        public Counterparty(String counterpartyId, String counterpartyName, String counterpartyType, Boolean active) {
            this.counterpartyId = counterpartyId;
            this.counterpartyName = counterpartyName;
            this.counterpartyType = counterpartyType;
            this.active = active;
        }
        
        // Getters and Setters
        public String getCounterpartyId() { return counterpartyId; }
        public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }
        
        public String getCounterpartyName() { return counterpartyName; }
        public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
        
        public String getLegalEntityIdentifier() { return legalEntityIdentifier; }
        public void setLegalEntityIdentifier(String legalEntityIdentifier) { this.legalEntityIdentifier = legalEntityIdentifier; }
        
        public String getCounterpartyType() { return counterpartyType; }
        public void setCounterpartyType(String counterpartyType) { this.counterpartyType = counterpartyType; }
        
        public String getJurisdiction() { return jurisdiction; }
        public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
        
        public String getRegulatoryStatus() { return regulatoryStatus; }
        public void setRegulatoryStatus(String regulatoryStatus) { this.regulatoryStatus = regulatoryStatus; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public String getRatingAgency() { return ratingAgency; }
        public void setRatingAgency(String ratingAgency) { this.ratingAgency = ratingAgency; }
        
        public String getCreditRating() { return creditRating; }
        public void setCreditRating(String creditRating) { this.creditRating = creditRating; }
        
        public BigDecimal getCreditLimit() { return creditLimit; }
        public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
        
        public List<String> getAuthorizedProducts() { return authorizedProducts; }
        public void setAuthorizedProducts(List<String> authorizedProducts) { this.authorizedProducts = authorizedProducts; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Counterparty that = (Counterparty) o;
            return Objects.equals(counterpartyId, that.counterpartyId);
        }
        
        @Override
        public int hashCode() { return Objects.hash(counterpartyId); }
    }
    
    /**
     * Currency static data.
     */
    public static class CurrencyData {
        private String currencyCode; // ISO 4217 code (e.g., "USD", "EUR", "GBP")
        private String currencyName;
        private Integer decimalPlaces; // Number of decimal places for the currency
        private Boolean active;
        private Boolean tradeable; // Can be used in trading
        private String region; // "AMERICAS", "EUROPE", "ASIA_PACIFIC"
        
        public CurrencyData() {}
        
        public CurrencyData(String currencyCode, String currencyName, Integer decimalPlaces, Boolean active, Boolean tradeable) {
            this.currencyCode = currencyCode;
            this.currencyName = currencyName;
            this.decimalPlaces = decimalPlaces;
            this.active = active;
            this.tradeable = tradeable;
        }
        
        // Getters and Setters
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        
        public String getCurrencyName() { return currencyName; }
        public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }
        
        public Integer getDecimalPlaces() { return decimalPlaces; }
        public void setDecimalPlaces(Integer decimalPlaces) { this.decimalPlaces = decimalPlaces; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public Boolean getTradeable() { return tradeable; }
        public void setTradeable(Boolean tradeable) { this.tradeable = tradeable; }
        
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CurrencyData currency = (CurrencyData) o;
            return Objects.equals(currencyCode, currency.currencyCode);
        }
        
        @Override
        public int hashCode() { return Objects.hash(currencyCode); }
    }
    
    /**
     * Commodity reference data.
     */
    public static class CommodityReference {
        private String commodityCode;
        private String commodityName;
        private String commodityType; // "ENERGY", "METALS", "AGRICULTURAL"
        private String referenceIndex; // "WTI", "BRENT", "HENRY_HUB", etc.
        private String indexProvider; // "NYMEX", "ICE", "COMEX"
        private String quoteCurrency;
        private String unitOfMeasure; // "BARREL", "TROY_OUNCE", "BUSHEL"
        private Boolean active;
        private Boolean tradeable;
        
        public CommodityReference() {}
        
        public CommodityReference(String commodityCode, String commodityName, String commodityType, 
                                String referenceIndex, String quoteCurrency, Boolean active) {
            this.commodityCode = commodityCode;
            this.commodityName = commodityName;
            this.commodityType = commodityType;
            this.referenceIndex = referenceIndex;
            this.quoteCurrency = quoteCurrency;
            this.active = active;
        }
        
        // Getters and Setters
        public String getCommodityCode() { return commodityCode; }
        public void setCommodityCode(String commodityCode) { this.commodityCode = commodityCode; }
        
        public String getCommodityName() { return commodityName; }
        public void setCommodityName(String commodityName) { this.commodityName = commodityName; }
        
        public String getCommodityType() { return commodityType; }
        public void setCommodityType(String commodityType) { this.commodityType = commodityType; }
        
        public String getReferenceIndex() { return referenceIndex; }
        public void setReferenceIndex(String referenceIndex) { this.referenceIndex = referenceIndex; }
        
        public String getIndexProvider() { return indexProvider; }
        public void setIndexProvider(String indexProvider) { this.indexProvider = indexProvider; }
        
        public String getQuoteCurrency() { return quoteCurrency; }
        public void setQuoteCurrency(String quoteCurrency) { this.quoteCurrency = quoteCurrency; }
        
        public String getUnitOfMeasure() { return unitOfMeasure; }
        public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public Boolean getTradeable() { return tradeable; }
        public void setTradeable(Boolean tradeable) { this.tradeable = tradeable; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CommodityReference that = (CommodityReference) o;
            return Objects.equals(commodityCode, that.commodityCode);
        }
        
        @Override
        public int hashCode() { return Objects.hash(commodityCode); }
    }
}
