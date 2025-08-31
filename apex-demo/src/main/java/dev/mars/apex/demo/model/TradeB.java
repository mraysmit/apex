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


import java.math.BigDecimal;

/**
 * TradeB model representing the core trade information
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
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
