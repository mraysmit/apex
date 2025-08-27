package dev.mars.apex.demo.model;

import java.time.LocalDate;

/**
 * TradeB Header containing trade identification and execution details
 */
public class TradeHeader {
    
    private PartyTradeIdentifier partyTradeIdentifier;
    private LocalDate tradeDate;

    public TradeHeader() {
    }

    public TradeHeader(PartyTradeIdentifier partyTradeIdentifier, LocalDate tradeDate) {
        this.partyTradeIdentifier = partyTradeIdentifier;
        this.tradeDate = tradeDate;
    }

    public PartyTradeIdentifier getPartyTradeIdentifier() {
        return partyTradeIdentifier;
    }

    public void setPartyTradeIdentifier(PartyTradeIdentifier partyTradeIdentifier) {
        this.partyTradeIdentifier = partyTradeIdentifier;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    @Override
    public String toString() {
        return "TradeHeader{" +
                "partyTradeIdentifier=" + partyTradeIdentifier +
                ", tradeDate=" + tradeDate +
                '}';
    }
}
