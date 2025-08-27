package dev.mars.apex.demo.bootstrap.model;

/**
 * Security model representing financial instrument details
 */
public class Security {
    
    private String instrumentId;
    private String instrumentType;
    private String issuer;

    public Security() {
    }

    public Security(String instrumentId, String instrumentType, String issuer) {
        this.instrumentId = instrumentId;
        this.instrumentType = instrumentType;
        this.issuer = issuer;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getInstrumentType() {
        return instrumentType;
    }

    public void setInstrumentType(String instrumentType) {
        this.instrumentType = instrumentType;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @Override
    public String toString() {
        return "Security{" +
                "instrumentId='" + instrumentId + '\'' +
                ", instrumentType='" + instrumentType + '\'' +
                ", issuer='" + issuer + '\'' +
                '}';
    }
}
