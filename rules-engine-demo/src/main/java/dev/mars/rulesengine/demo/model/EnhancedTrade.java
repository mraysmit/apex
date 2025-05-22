package dev.mars.rulesengine.demo.model;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Trade model with settlement and custody domain concepts.
 * This class extends the basic Trade model with additional properties
 * relevant to financial services domains.
 */
public class EnhancedTrade extends Trade {
    // Settlement domain properties
    private String settlementStatus; // e.g., "Pending", "Settled", "Failed"
    private LocalDate tradeDate;
    private LocalDate settlementDate;
    private String settlementCurrency;
    private double settlementAmount;
    private String settlementMethod; // e.g., "DVP", "FOP", "DFP"
    private String settlementLocation; // e.g., "DTCC", "Euroclear", "Clearstream"

    // Custody domain properties
    private String custodian;
    private String accountId;
    private String accountType; // e.g., "Client", "House", "Omnibus"
    private boolean isSegregated;
    private String safekeepingLocation;

    // Compliance and risk properties
    private String riskRating; // e.g., "High", "Medium", "Low"
    private boolean amlCheckPassed;
    private boolean sanctionsCheckPassed;
    private Map<String, String> complianceFlags;

    /**
     * Create a new enhanced trade with basic properties.
     *
     * @param id The ID of the trade
     * @param value The value of the trade
     * @param category The category of the trade
     */
    public EnhancedTrade(String id, String value, String category) {
        super(id, value, category);
        this.complianceFlags = new HashMap<>();
        this.tradeDate = LocalDate.now();
        this.settlementDate = LocalDate.now().plusDays(2); // T+2 settlement by default
    }

    /**
     * Create a new enhanced trade with settlement properties.
     *
     * @param id The ID of the trade
     * @param value The value of the trade
     * @param category The category of the trade
     * @param settlementStatus The settlement status
     * @param settlementCurrency The settlement currency
     * @param settlementAmount The settlement amount
     */
    public EnhancedTrade(String id, String value, String category,
                         String settlementStatus, String settlementCurrency, double settlementAmount) {
        this(id, value, category);
        this.settlementStatus = settlementStatus;
        this.settlementCurrency = settlementCurrency;
        this.settlementAmount = settlementAmount;
    }

    /**
     * Create a new enhanced trade with full properties.
     *
     * @param id The ID of the trade
     * @param value The value of the trade
     * @param category The category of the trade
     * @param settlementStatus The settlement status
     * @param tradeDate The trade date
     * @param settlementDate The settlement date
     * @param settlementCurrency The settlement currency
     * @param settlementAmount The settlement amount
     * @param settlementMethod The settlement method
     * @param settlementLocation The settlement location
     * @param custodian The custodian
     * @param accountId The account ID
     * @param accountType The account type
     * @param isSegregated Whether the account is segregated
     * @param safekeepingLocation The safekeeping location
     * @param riskRating The risk rating
     */
    public EnhancedTrade(String id, String value, String category,
                         String settlementStatus, LocalDate tradeDate, LocalDate settlementDate,
                         String settlementCurrency, double settlementAmount, String settlementMethod,
                         String settlementLocation, String custodian, String accountId,
                         String accountType, boolean isSegregated, String safekeepingLocation,
                         String riskRating) {
        this(id, value, category);
        this.settlementStatus = settlementStatus;
        this.tradeDate = tradeDate;
        this.settlementDate = settlementDate;
        this.settlementCurrency = settlementCurrency;
        this.settlementAmount = settlementAmount;
        this.settlementMethod = settlementMethod;
        this.settlementLocation = settlementLocation;
        this.custodian = custodian;
        this.accountId = accountId;
        this.accountType = accountType;
        this.isSegregated = isSegregated;
        this.safekeepingLocation = safekeepingLocation;
        this.riskRating = riskRating;
        this.amlCheckPassed = false;
        this.sanctionsCheckPassed = false;
    }

    // Getters and setters for all properties

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getSettlementCurrency() {
        return settlementCurrency;
    }

    public void setSettlementCurrency(String settlementCurrency) {
        this.settlementCurrency = settlementCurrency;
    }

    public double getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(double settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public String getSettlementLocation() {
        return settlementLocation;
    }

    public void setSettlementLocation(String settlementLocation) {
        this.settlementLocation = settlementLocation;
    }

    public String getCustodian() {
        return custodian;
    }

    public void setCustodian(String custodian) {
        this.custodian = custodian;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public boolean isSegregated() {
        return isSegregated;
    }

    public void setSegregated(boolean segregated) {
        isSegregated = segregated;
    }

    public String getSafekeepingLocation() {
        return safekeepingLocation;
    }

    public void setSafekeepingLocation(String safekeepingLocation) {
        this.safekeepingLocation = safekeepingLocation;
    }

    public String getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(String riskRating) {
        this.riskRating = riskRating;
    }

    public boolean isAmlCheckPassed() {
        return amlCheckPassed;
    }

    public void setAmlCheckPassed(boolean amlCheckPassed) {
        this.amlCheckPassed = amlCheckPassed;
    }

    public boolean isSanctionsCheckPassed() {
        return sanctionsCheckPassed;
    }

    public void setSanctionsCheckPassed(boolean sanctionsCheckPassed) {
        this.sanctionsCheckPassed = sanctionsCheckPassed;
    }

    public Map<String, String> getComplianceFlags() {
        return new HashMap<>(complianceFlags);
    }

    public void addComplianceFlag(String key, String value) {
        this.complianceFlags.put(key, value);
    }

    public void removeComplianceFlag(String key) {
        this.complianceFlags.remove(key);
    }

    /**
     * Calculate the settlement date based on the trade date and instrument type.
     * Different instruments have different standard settlement cycles.
     *
     * @return The calculated settlement date
     */
    public LocalDate calculateSettlementDate() {
        if (getValue() == null) {
            return tradeDate.plusDays(2); // Default T+2
        }

        switch (getValue()) {
            case "Equity":
            case "ETF":
                return tradeDate.plusDays(2); // T+2 for equities and ETFs
            case "Bond":
            case "FixedIncome":
                return tradeDate.plusDays(1); // T+1 for bonds
            case "Option":
            case "Future":
                return tradeDate; // T+0 for derivatives
            default:
                return tradeDate.plusDays(2); // Default T+2
        }
    }

    /**
     * Check if the trade is eligible for settlement.
     *
     * @return True if the trade is eligible for settlement, false otherwise
     */
    public boolean isEligibleForSettlement() {
        return settlementStatus != null &&
                !settlementStatus.equals("Settled") &&
                !settlementStatus.equals("Failed") &&
                settlementAmount > 0 &&
                settlementCurrency != null &&
                !settlementCurrency.isEmpty();
    }

    /**
     * Check if the trade requires segregated custody.
     *
     * @return True if the trade requires segregated custody, false otherwise
     */
    public boolean requiresSegregatedCustody() {
        return "Client".equals(accountType) &&
                ("High".equals(riskRating) || settlementAmount > 1000000);
    }

    @Override
    public String toString() {
        return "EnhancedTrade{" +
                "id='" + getId() + '\'' +
                ", value='" + getValue() + '\'' +
                ", category='" + getCategory() + '\'' +
                ", settlementStatus='" + settlementStatus + '\'' +
                ", tradeDate=" + tradeDate +
                ", settlementDate=" + settlementDate +
                ", settlementCurrency='" + settlementCurrency + '\'' +
                ", settlementAmount=" + settlementAmount +
                ", settlementMethod='" + settlementMethod + '\'' +
                ", settlementLocation='" + settlementLocation + '\'' +
                ", custodian='" + custodian + '\'' +
                ", accountId='" + accountId + '\'' +
                ", accountType='" + accountType + '\'' +
                ", isSegregated=" + isSegregated +
                ", safekeepingLocation='" + safekeepingLocation + '\'' +
                ", riskRating='" + riskRating + '\'' +
                ", amlCheckPassed=" + amlCheckPassed +
                ", sanctionsCheckPassed=" + sanctionsCheckPassed +
                '}';
    }
}