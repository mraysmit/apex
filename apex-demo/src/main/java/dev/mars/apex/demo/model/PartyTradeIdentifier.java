package dev.mars.apex.demo.model;

/**
 * Party TradeB Identifier containing trade ID and party reference
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
