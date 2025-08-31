package dev.mars.apex.demo.model;

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


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * OTC Option data model representing Over-the-Counter option contracts.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class OtcOption {
    
    // Core OTC Option Fields
    @JsonProperty("tradeDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate tradeDate;
    
    @JsonProperty("buyerParty")
    private String buyerParty;
    
    @JsonProperty("sellerParty")
    private String sellerParty;
    
    @JsonProperty("optionType")
    private String optionType; // Call or Put
    
    @JsonProperty("underlyingAsset")
    private UnderlyingAsset underlyingAsset;
    
    @JsonProperty("strikePrice")
    private BigDecimal strikePrice;
    
    @JsonProperty("strikeCurrency")
    private String strikeCurrency;
    
    @JsonProperty("notionalQuantity")
    private BigDecimal notionalQuantity;
    
    @JsonProperty("expiryDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;
    
    @JsonProperty("settlementType")
    private String settlementType; // Cash or Physical
    
    // Enriched Fields - Added by various data sources
    
    // From Inline Dataset (Commodity Reference)
    @JsonProperty("commodityCategory")
    private String commodityCategory;
    
    @JsonProperty("exchange")
    private String exchange;
    
    @JsonProperty("riskFactor")
    private String riskFactor;
    
    @JsonProperty("marginRate")
    private BigDecimal marginRate;
    
    // From PostgreSQL Database (Counterparty Information)
    @JsonProperty("buyerLegalName")
    private String buyerLegalName;
    
    @JsonProperty("buyerCreditRating")
    private String buyerCreditRating;
    
    @JsonProperty("buyerLei")
    private String buyerLei;
    
    @JsonProperty("buyerJurisdiction")
    private String buyerJurisdiction;
    
    @JsonProperty("sellerLegalName")
    private String sellerLegalName;
    
    @JsonProperty("sellerCreditRating")
    private String sellerCreditRating;
    
    @JsonProperty("sellerLei")
    private String sellerLei;
    
    @JsonProperty("sellerJurisdiction")
    private String sellerJurisdiction;
    
    // From External YAML File (Currency and Market Data)
    @JsonProperty("currencyName")
    private String currencyName;
    
    @JsonProperty("currencyRegion")
    private String currencyRegion;
    
    @JsonProperty("timezone")
    private String timezone;
    
    @JsonProperty("tradingHours")
    private String tradingHours;
    
    // Calculated Fields
    @JsonProperty("daysToExpiry")
    private Long daysToExpiry;
    
    @JsonProperty("moneyness")
    private String moneyness; // ITM, ATM, OTM
    
    @JsonProperty("riskExposure")
    private BigDecimal riskExposure;
    
    // Default constructor
    public OtcOption() {}
    
    // Constructor with core fields
    public OtcOption(LocalDate tradeDate, String buyerParty, String sellerParty, 
                     String optionType, UnderlyingAsset underlyingAsset, 
                     BigDecimal strikePrice, String strikeCurrency,
                     BigDecimal notionalQuantity, LocalDate expiryDate, 
                     String settlementType) {
        this.tradeDate = tradeDate;
        this.buyerParty = buyerParty;
        this.sellerParty = sellerParty;
        this.optionType = optionType;
        this.underlyingAsset = underlyingAsset;
        this.strikePrice = strikePrice;
        this.strikeCurrency = strikeCurrency;
        this.notionalQuantity = notionalQuantity;
        this.expiryDate = expiryDate;
        this.settlementType = settlementType;
    }
    
    // Getters and Setters
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    
    public String getBuyerParty() { return buyerParty; }
    public void setBuyerParty(String buyerParty) { this.buyerParty = buyerParty; }
    
    public String getSellerParty() { return sellerParty; }
    public void setSellerParty(String sellerParty) { this.sellerParty = sellerParty; }
    
    public String getOptionType() { return optionType; }
    public void setOptionType(String optionType) { this.optionType = optionType; }
    
    public UnderlyingAsset getUnderlyingAsset() { return underlyingAsset; }
    public void setUnderlyingAsset(UnderlyingAsset underlyingAsset) { this.underlyingAsset = underlyingAsset; }
    
    public BigDecimal getStrikePrice() { return strikePrice; }
    public void setStrikePrice(BigDecimal strikePrice) { this.strikePrice = strikePrice; }
    
    public String getStrikeCurrency() { return strikeCurrency; }
    public void setStrikeCurrency(String strikeCurrency) { this.strikeCurrency = strikeCurrency; }
    
    public BigDecimal getNotionalQuantity() { return notionalQuantity; }
    public void setNotionalQuantity(BigDecimal notionalQuantity) { this.notionalQuantity = notionalQuantity; }
    
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String settlementType) { this.settlementType = settlementType; }
    
    // Enriched field getters and setters
    public String getCommodityCategory() { return commodityCategory; }
    public void setCommodityCategory(String commodityCategory) { this.commodityCategory = commodityCategory; }
    
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    
    public String getRiskFactor() { return riskFactor; }
    public void setRiskFactor(String riskFactor) { this.riskFactor = riskFactor; }
    
    public BigDecimal getMarginRate() { return marginRate; }
    public void setMarginRate(BigDecimal marginRate) { this.marginRate = marginRate; }
    
    public String getBuyerLegalName() { return buyerLegalName; }
    public void setBuyerLegalName(String buyerLegalName) { this.buyerLegalName = buyerLegalName; }
    
    public String getBuyerCreditRating() { return buyerCreditRating; }
    public void setBuyerCreditRating(String buyerCreditRating) { this.buyerCreditRating = buyerCreditRating; }
    
    public String getBuyerLei() { return buyerLei; }
    public void setBuyerLei(String buyerLei) { this.buyerLei = buyerLei; }
    
    public String getBuyerJurisdiction() { return buyerJurisdiction; }
    public void setBuyerJurisdiction(String buyerJurisdiction) { this.buyerJurisdiction = buyerJurisdiction; }
    
    public String getSellerLegalName() { return sellerLegalName; }
    public void setSellerLegalName(String sellerLegalName) { this.sellerLegalName = sellerLegalName; }
    
    public String getSellerCreditRating() { return sellerCreditRating; }
    public void setSellerCreditRating(String sellerCreditRating) { this.sellerCreditRating = sellerCreditRating; }
    
    public String getSellerLei() { return sellerLei; }
    public void setSellerLei(String sellerLei) { this.sellerLei = sellerLei; }
    
    public String getSellerJurisdiction() { return sellerJurisdiction; }
    public void setSellerJurisdiction(String sellerJurisdiction) { this.sellerJurisdiction = sellerJurisdiction; }
    
    public String getCurrencyName() { return currencyName; }
    public void setCurrencyName(String currencyName) { this.currencyName = currencyName; }
    
    public String getCurrencyRegion() { return currencyRegion; }
    public void setCurrencyRegion(String currencyRegion) { this.currencyRegion = currencyRegion; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public String getTradingHours() { return tradingHours; }
    public void setTradingHours(String tradingHours) { this.tradingHours = tradingHours; }
    
    public Long getDaysToExpiry() { return daysToExpiry; }
    public void setDaysToExpiry(Long daysToExpiry) { this.daysToExpiry = daysToExpiry; }
    
    public String getMoneyness() { return moneyness; }
    public void setMoneyness(String moneyness) { this.moneyness = moneyness; }
    
    public BigDecimal getRiskExposure() { return riskExposure; }
    public void setRiskExposure(BigDecimal riskExposure) { this.riskExposure = riskExposure; }
    
    @Override
    public String toString() {
        return "OtcOption{" +
                "tradeDate=" + tradeDate +
                ", buyerParty='" + buyerParty + '\'' +
                ", sellerParty='" + sellerParty + '\'' +
                ", optionType='" + optionType + '\'' +
                ", underlyingAsset=" + underlyingAsset +
                ", strikePrice=" + strikePrice +
                ", strikeCurrency='" + strikeCurrency + '\'' +
                ", notionalQuantity=" + notionalQuantity +
                ", expiryDate=" + expiryDate +
                ", settlementType='" + settlementType + '\'' +
                '}';
    }
}
