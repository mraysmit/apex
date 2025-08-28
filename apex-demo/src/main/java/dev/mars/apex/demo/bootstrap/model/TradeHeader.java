package dev.mars.apex.demo.bootstrap.model;

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


import java.time.LocalDate;

/**
 * TradeB Header containing trade identification and execution details
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
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
