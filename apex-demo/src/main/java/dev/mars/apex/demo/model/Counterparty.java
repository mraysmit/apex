package dev.mars.apex.demo.model;

/**
 * Counterparty model representing the trading counterparty
 */
public class Counterparty {
    
    private String partyId;
    private String partyName;

    public Counterparty() {
    }

    public Counterparty(String partyId, String partyName) {
        this.partyId = partyId;
        this.partyName = partyName;
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    @Override
    public String toString() {
        return "Counterparty{" +
                "partyId='" + partyId + '\'' +
                ", partyName='" + partyName + '\'' +
                '}';
    }
}
