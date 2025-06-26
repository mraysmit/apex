package dev.mars.rulesengine.demo.examples.financial.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an OTC Commodity Total Return Swap financial instrument.
 * 
 * A Total Return Swap (TRS) is a derivative contract where one party (total return payer) 
 * transfers the total economic performance of a reference asset to another party (total return receiver).
 * In a commodity TRS, the reference asset is typically a commodity index or basket of commodities.
 * 
 * Key characteristics:
 * - One party pays the total return (price appreciation + income) of the commodity reference
 * - The other party typically pays a floating rate (e.g., LIBOR + spread)
 * - No physical delivery of the underlying commodity
 * - Used for gaining exposure to commodity prices without direct ownership
 */
public class CommodityTotalReturnSwap {
    
    // Trade Identification
    private String tradeId;
    private String externalTradeId;
    private LocalDate tradeDate;
    private LocalDate effectiveDate;
    private LocalDate maturityDate;
    
    // Counterparty Information
    private String counterpartyId;
    private String counterpartyName;
    private String counterpartyLei; // Legal Entity Identifier
    
    // Client Information
    private String clientId;
    private String clientName;
    private String clientAccountId;
    private String clientAccountType; // e.g., "SEGREGATED", "OMNIBUS"
    
    // Instrument Details
    private String commodityType; // e.g., "CRUDE_OIL", "NATURAL_GAS", "GOLD", "AGRICULTURAL"
    private String referenceIndex; // e.g., "WTI", "BRENT", "HENRY_HUB", "COMEX_GOLD"
    private String indexProvider; // e.g., "NYMEX", "ICE", "COMEX"
    
    // Financial Terms
    private BigDecimal notionalAmount;
    private String notionalCurrency;
    private String paymentCurrency;
    private BigDecimal initialPrice; // Reference price at trade inception
    
    // Total Return Leg (Commodity Performance)
    private String totalReturnPayerParty; // "CLIENT" or "COUNTERPARTY"
    private String totalReturnReceiverParty; // "CLIENT" or "COUNTERPARTY"
    
    // Funding Leg (Financing Cost)
    private String fundingRateType; // e.g., "LIBOR", "SOFR", "FIXED"
    private BigDecimal fundingSpread; // Spread over reference rate (in basis points)
    private BigDecimal fixedRate; // If funding leg is fixed rate
    private String fundingFrequency; // e.g., "MONTHLY", "QUARTERLY", "SEMI_ANNUAL"
    
    // Settlement Information
    private String settlementType; // "CASH", "PHYSICAL" (typically CASH for TRS)
    private String settlementCurrency;
    private Integer settlementDays; // T+N settlement
    
    // Risk and Regulatory
    private String jurisdiction; // Trading jurisdiction
    private String regulatoryRegime; // e.g., "EMIR", "DODD_FRANK", "MiFID_II"
    private Boolean clearingEligible;
    private String clearingHouse; // If centrally cleared
    
    // Valuation
    private BigDecimal currentMarketValue;
    private BigDecimal unrealizedPnL;
    private LocalDate lastValuationDate;
    
    // Status
    private String tradeStatus; // "PENDING", "CONFIRMED", "SETTLED", "CANCELLED"
    private String bookingStatus; // "PENDING", "BOOKED", "FAILED"
    
    // Default constructor
    public CommodityTotalReturnSwap() {}
    
    // Constructor with essential fields
    public CommodityTotalReturnSwap(String tradeId, String counterpartyId, String clientId,
                                   String commodityType, String referenceIndex,
                                   BigDecimal notionalAmount, String notionalCurrency,
                                   LocalDate tradeDate, LocalDate maturityDate) {
        this.tradeId = tradeId;
        this.counterpartyId = counterpartyId;
        this.clientId = clientId;
        this.commodityType = commodityType;
        this.referenceIndex = referenceIndex;
        this.notionalAmount = notionalAmount;
        this.notionalCurrency = notionalCurrency;
        this.tradeDate = tradeDate;
        this.maturityDate = maturityDate;
        this.effectiveDate = tradeDate; // Default to trade date
        this.settlementType = "CASH"; // Default for TRS
        this.tradeStatus = "PENDING"; // Default status
        this.bookingStatus = "PENDING"; // Default booking status
    }
    
    // Getters and Setters
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    
    public String getExternalTradeId() { return externalTradeId; }
    public void setExternalTradeId(String externalTradeId) { this.externalTradeId = externalTradeId; }
    
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    
    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    
    public String getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }
    
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    
    public String getCounterpartyLei() { return counterpartyLei; }
    public void setCounterpartyLei(String counterpartyLei) { this.counterpartyLei = counterpartyLei; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    
    public String getClientAccountId() { return clientAccountId; }
    public void setClientAccountId(String clientAccountId) { this.clientAccountId = clientAccountId; }
    
    public String getClientAccountType() { return clientAccountType; }
    public void setClientAccountType(String clientAccountType) { this.clientAccountType = clientAccountType; }
    
    public String getCommodityType() { return commodityType; }
    public void setCommodityType(String commodityType) { this.commodityType = commodityType; }
    
    public String getReferenceIndex() { return referenceIndex; }
    public void setReferenceIndex(String referenceIndex) { this.referenceIndex = referenceIndex; }
    
    public String getIndexProvider() { return indexProvider; }
    public void setIndexProvider(String indexProvider) { this.indexProvider = indexProvider; }
    
    public BigDecimal getNotionalAmount() { return notionalAmount; }
    public void setNotionalAmount(BigDecimal notionalAmount) { this.notionalAmount = notionalAmount; }
    
    public String getNotionalCurrency() { return notionalCurrency; }
    public void setNotionalCurrency(String notionalCurrency) { this.notionalCurrency = notionalCurrency; }
    
    public String getPaymentCurrency() { return paymentCurrency; }
    public void setPaymentCurrency(String paymentCurrency) { this.paymentCurrency = paymentCurrency; }
    
    public BigDecimal getInitialPrice() { return initialPrice; }
    public void setInitialPrice(BigDecimal initialPrice) { this.initialPrice = initialPrice; }
    
    public String getTotalReturnPayerParty() { return totalReturnPayerParty; }
    public void setTotalReturnPayerParty(String totalReturnPayerParty) { this.totalReturnPayerParty = totalReturnPayerParty; }
    
    public String getTotalReturnReceiverParty() { return totalReturnReceiverParty; }
    public void setTotalReturnReceiverParty(String totalReturnReceiverParty) { this.totalReturnReceiverParty = totalReturnReceiverParty; }
    
    public String getFundingRateType() { return fundingRateType; }
    public void setFundingRateType(String fundingRateType) { this.fundingRateType = fundingRateType; }
    
    public BigDecimal getFundingSpread() { return fundingSpread; }
    public void setFundingSpread(BigDecimal fundingSpread) { this.fundingSpread = fundingSpread; }
    
    public BigDecimal getFixedRate() { return fixedRate; }
    public void setFixedRate(BigDecimal fixedRate) { this.fixedRate = fixedRate; }
    
    public String getFundingFrequency() { return fundingFrequency; }
    public void setFundingFrequency(String fundingFrequency) { this.fundingFrequency = fundingFrequency; }
    
    public String getSettlementType() { return settlementType; }
    public void setSettlementType(String settlementType) { this.settlementType = settlementType; }
    
    public String getSettlementCurrency() { return settlementCurrency; }
    public void setSettlementCurrency(String settlementCurrency) { this.settlementCurrency = settlementCurrency; }
    
    public Integer getSettlementDays() { return settlementDays; }
    public void setSettlementDays(Integer settlementDays) { this.settlementDays = settlementDays; }
    
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    
    public String getRegulatoryRegime() { return regulatoryRegime; }
    public void setRegulatoryRegime(String regulatoryRegime) { this.regulatoryRegime = regulatoryRegime; }
    
    public Boolean getClearingEligible() { return clearingEligible; }
    public void setClearingEligible(Boolean clearingEligible) { this.clearingEligible = clearingEligible; }
    
    public String getClearingHouse() { return clearingHouse; }
    public void setClearingHouse(String clearingHouse) { this.clearingHouse = clearingHouse; }
    
    public BigDecimal getCurrentMarketValue() { return currentMarketValue; }
    public void setCurrentMarketValue(BigDecimal currentMarketValue) { this.currentMarketValue = currentMarketValue; }
    
    public BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
    public void setUnrealizedPnL(BigDecimal unrealizedPnL) { this.unrealizedPnL = unrealizedPnL; }
    
    public LocalDate getLastValuationDate() { return lastValuationDate; }
    public void setLastValuationDate(LocalDate lastValuationDate) { this.lastValuationDate = lastValuationDate; }
    
    public String getTradeStatus() { return tradeStatus; }
    public void setTradeStatus(String tradeStatus) { this.tradeStatus = tradeStatus; }
    
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommodityTotalReturnSwap that = (CommodityTotalReturnSwap) o;
        return Objects.equals(tradeId, that.tradeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tradeId);
    }
    
    @Override
    public String toString() {
        return "CommodityTotalReturnSwap{" +
                "tradeId='" + tradeId + '\'' +
                ", counterpartyId='" + counterpartyId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", commodityType='" + commodityType + '\'' +
                ", referenceIndex='" + referenceIndex + '\'' +
                ", notionalAmount=" + notionalAmount +
                ", notionalCurrency='" + notionalCurrency + '\'' +
                ", tradeDate=" + tradeDate +
                ", maturityDate=" + maturityDate +
                ", tradeStatus='" + tradeStatus + '\'' +
                '}';
    }
}
