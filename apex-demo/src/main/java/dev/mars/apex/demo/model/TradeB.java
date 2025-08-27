package dev.mars.apex.demo.model;

import java.math.BigDecimal;

/**
 * TradeB model representing the core trade information
 * Based on FpML and ISO 20022 standards
 */
public class TradeB {
    
    private TradeHeader tradeHeader;
    private Security security;
    private Counterparty counterparty;
    private String tradingVenue;
    private BigDecimal quantity;
    private BigDecimal price;
    private String currency;

    public TradeB() {
    }

    public TradeHeader getTradeHeader() {
        return tradeHeader;
    }

    public void setTradeHeader(TradeHeader tradeHeader) {
        this.tradeHeader = tradeHeader;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Counterparty getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(Counterparty counterparty) {
        this.counterparty = counterparty;
    }

    public String getTradingVenue() {
        return tradingVenue;
    }

    public void setTradingVenue(String tradingVenue) {
        this.tradingVenue = tradingVenue;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "TradeB{" +
                "tradeHeader=" + tradeHeader +
                ", security=" + security +
                ", counterparty=" + counterparty +
                ", tradingVenue='" + tradingVenue + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                '}';
    }
}
