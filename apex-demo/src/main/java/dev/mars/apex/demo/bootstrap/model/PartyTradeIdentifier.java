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


/**
 * Party TradeB Identifier containing trade ID and party reference
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class PartyTradeIdentifier {
    
    private String partyReference;
    private String tradeId;

    public PartyTradeIdentifier() {
    }

    public PartyTradeIdentifier(String partyReference, String tradeId) {
        this.partyReference = partyReference;
        this.tradeId = tradeId;
    }

    public String getPartyReference() {
        return partyReference;
    }

    public void setPartyReference(String partyReference) {
        this.partyReference = partyReference;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    @Override
    public String toString() {
        return "PartyTradeIdentifier{" +
                "partyReference='" + partyReference + '\'' +
                ", tradeId='" + tradeId + '\'' +
                '}';
    }
}
