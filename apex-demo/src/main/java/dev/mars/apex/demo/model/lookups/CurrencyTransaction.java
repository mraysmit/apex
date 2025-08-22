package dev.mars.apex.demo.model.lookups;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model class representing a currency transaction for simple field lookup demonstrations.
 * Used to demonstrate basic lookup-key patterns with currency code enrichment.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class CurrencyTransaction {
    
    private String transactionId;
    private BigDecimal amount;
    private String currencyCode;
    private String description;
    private LocalDateTime transactionDate;
    private String merchantName;
    private String category;
    
    // Enriched fields (populated by lookup)
    private String currencyName;
    private String currencySymbol;
    private Integer decimalPlaces;
    private String countryCode;
    private Boolean isBaseCurrency;
    
    public CurrencyTransaction() {
    }
    
    public CurrencyTransaction(String transactionId, BigDecimal amount, String currencyCode, 
                             String description, LocalDateTime transactionDate) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.description = description;
        this.transactionDate = transactionDate;
    }
    
    public CurrencyTransaction(String transactionId, BigDecimal amount, String currencyCode, 
                             String description, LocalDateTime transactionDate, 
                             String merchantName, String category) {
        this(transactionId, amount, currencyCode, description, transactionDate);
        this.merchantName = merchantName;
        this.category = category;
    }
    
    // Getters and Setters
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrencyCode() {
        return currencyCode;
    }
    
    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }
    
    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
    
    public String getMerchantName() {
        return merchantName;
    }
    
    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    // Enriched field getters and setters
    
    public String getCurrencyName() {
        return currencyName;
    }
    
    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }
    
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }
    
    public Integer getDecimalPlaces() {
        return decimalPlaces;
    }
    
    public void setDecimalPlaces(Integer decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }
    
    public String getCountryCode() {
        return countryCode;
    }
    
    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
    
    public Boolean getIsBaseCurrency() {
        return isBaseCurrency;
    }
    
    public void setIsBaseCurrency(Boolean isBaseCurrency) {
        this.isBaseCurrency = isBaseCurrency;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyTransaction that = (CurrencyTransaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
    
    @Override
    public String toString() {
        return "CurrencyTransaction{" +
                "transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", currencyCode='" + currencyCode + '\'' +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                ", merchantName='" + merchantName + '\'' +
                ", category='" + category + '\'' +
                ", currencyName='" + currencyName + '\'' +
                ", currencySymbol='" + currencySymbol + '\'' +
                ", decimalPlaces=" + decimalPlaces +
                ", countryCode='" + countryCode + '\'' +
                ", isBaseCurrency=" + isBaseCurrency +
                '}';
    }
}
