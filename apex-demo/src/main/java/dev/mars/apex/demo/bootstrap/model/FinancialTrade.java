package dev.mars.apex.demo.bootstrap.model;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Financial trade model class for demonstration purposes.
 *
 * This class represents a basic financial trade with essential properties
 * commonly used in trading systems and rule evaluations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-28
 * @version 1.0
 */
public class FinancialTrade {

    // Basic trade identification
    private String tradeId;
    private String externalTradeId;

    // Financial details
    private BigDecimal amount;
    private String currency;
    private String instrumentType;

    // Counterparty information
    private String counterparty;
    private String counterpartyId;

    // Dates
    private LocalDate tradeDate;
    private LocalDate settlementDate;
    private LocalDate valueDate;

    // Status and processing
    private String status;
    private String bookingStatus;

    // Additional properties
    private String tradingDesk;
    private String portfolio;
    private String trader;

    // Default constructor
    public FinancialTrade() {}

    // Constructor with essential fields
    public FinancialTrade(String tradeId, BigDecimal amount, String currency, String counterparty) {
        this.tradeId = tradeId;
        this.amount = amount;
        this.currency = currency;
        this.counterparty = counterparty;
        this.tradeDate = LocalDate.now();
        this.status = "NEW";
    }

    // Getters and setters
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public String getExternalTradeId() { return externalTradeId; }
    public void setExternalTradeId(String externalTradeId) { this.externalTradeId = externalTradeId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }

    public String getCounterparty() { return counterparty; }
    public void setCounterparty(String counterparty) { this.counterparty = counterparty; }

    public String getCounterpartyId() { return counterpartyId; }
    public void setCounterpartyId(String counterpartyId) { this.counterpartyId = counterpartyId; }

    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }

    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }

    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

    public String getTradingDesk() { return tradingDesk; }
    public void setTradingDesk(String tradingDesk) { this.tradingDesk = tradingDesk; }

    public String getPortfolio() { return portfolio; }
    public void setPortfolio(String portfolio) { this.portfolio = portfolio; }

    public String getTrader() { return trader; }
    public void setTrader(String trader) { this.trader = trader; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FinancialTrade that = (FinancialTrade) o;
        return Objects.equals(tradeId, that.tradeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeId);
    }

    @Override
    public String toString() {
        return "FinancialTrade{" +
                "tradeId='" + tradeId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", counterparty='" + counterparty + '\'' +
                ", tradeDate=" + tradeDate +
                ", status='" + status + '\'' +
                '}';
    }
}
