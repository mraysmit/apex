package testmodels;

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
 * Test trade class for enrichment testing.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test trade class for enrichment testing.
 * Placed in a separate package to avoid Java module system restrictions with SpEL.
 */
public class TestTrade {
    public String counterpartyId;
    public String notionalCurrency;
    public Double notionalAmount;
    public Double rate;
    
    // Fields to be enriched by counterparty lookup
    public String counterpartyName;
    public String counterpartyRating;
    public String counterpartyLei;
    public String counterpartyJurisdiction;
    
    // Fields to be enriched by currency lookup
    public String currencyName;
    public Integer currencyDecimalPlaces;
    public Boolean currencyActive;
    public String currencyRegion;
    public String currencyCentralBank;
    public Boolean isMajorCurrency;
    
    // Fields to be enriched by country lookup
    public String jurisdictionName;
    public String jurisdictionRegion;
    public String regulatoryRegime;
    
    // Fields for calculation enrichment
    public Double interestAmount;
    
    // Getter methods for SpEL access
    public String getCounterpartyId() { return counterpartyId; }
    public String getNotionalCurrency() { return notionalCurrency; }
    public Double getNotionalAmount() { return notionalAmount; }
    public Double getRate() { return rate; }
    
    public String getCounterpartyName() { return counterpartyName; }
    public String getCounterpartyRating() { return counterpartyRating; }
    public String getCounterpartyLei() { return counterpartyLei; }
    public String getCounterpartyJurisdiction() { return counterpartyJurisdiction; }
    
    public String getCurrencyName() { return currencyName; }
    public Integer getCurrencyDecimalPlaces() { return currencyDecimalPlaces; }
    public Boolean getCurrencyActive() { return currencyActive; }
    public String getCurrencyRegion() { return currencyRegion; }
    public String getCurrencyCentralBank() { return currencyCentralBank; }
    public Boolean getIsMajorCurrency() { return isMajorCurrency; }
    
    public String getJurisdictionName() { return jurisdictionName; }
    public String getJurisdictionRegion() { return jurisdictionRegion; }
    public String getRegulatoryRegime() { return regulatoryRegime; }
    
    public Double getInterestAmount() { return interestAmount; }
    
    // Setter methods for completeness
    public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }
    public void setNotionalCurrency(String notionalCurrency) { this.notionalCurrency = notionalCurrency; }
    public void setNotionalAmount(Double notionalAmount) { this.notionalAmount = notionalAmount; }
    public void setRate(Double rate) { this.rate = rate; }
    
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public void setCounterpartyRating(String counterpartyRating) { this.counterpartyRating = counterpartyRating; }
    public void setCounterpartyLei(String counterpartyLei) { this.counterpartyLei = counterpartyLei; }
    public void setCounterpartyJurisdiction(String counterpartyJurisdiction) { this.counterpartyJurisdiction = counterpartyJurisdiction; }
    
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }
    public void setCurrencyDecimalPlaces(Integer currencyDecimalPlaces) { this.currencyDecimalPlaces = currencyDecimalPlaces; }
    public void setCurrencyActive(Boolean currencyActive) { this.currencyActive = currencyActive; }
    public void setCurrencyRegion(String currencyRegion) { this.currencyRegion = currencyRegion; }
    public void setCurrencyCentralBank(String currencyCentralBank) { this.currencyCentralBank = currencyCentralBank; }
    public void setIsMajorCurrency(Boolean isMajorCurrency) { this.isMajorCurrency = isMajorCurrency; }
    
    public void setJurisdictionName(String jurisdictionName) { this.jurisdictionName = jurisdictionName; }
    public void setJurisdictionRegion(String jurisdictionRegion) { this.jurisdictionRegion = jurisdictionRegion; }
    public void setRegulatoryRegime(String regulatoryRegime) { this.regulatoryRegime = regulatoryRegime; }
    
    public void setInterestAmount(Double interestAmount) { this.interestAmount = interestAmount; }
    
    @Override
    public String toString() {
        return "TestTrade{" +
               "counterpartyId='" + counterpartyId + '\'' +
               ", notionalCurrency='" + notionalCurrency + '\'' +
               ", notionalAmount=" + notionalAmount +
               ", currencyName='" + currencyName + '\'' +
               ", counterpartyName='" + counterpartyName + '\'' +
               '}';
    }
}
