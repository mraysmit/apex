package dev.mars.apex.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Model class representing a customer order for compound key lookup demonstrations.
 * Used to demonstrate lookup-key patterns with string concatenation: "#customerId + '-' + #region"
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class CustomerOrder {
    
    private String orderId;
    private String customerId;
    private String region;
    private String productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private LocalDateTime orderDate;
    private String status;
    
    // Enriched fields (populated by compound key lookup)
    private String customerTier;
    private BigDecimal regionalDiscount;
    private String specialPricing;
    private String customerName;
    private String regionName;
    private String currency;
    private BigDecimal taxRate;
    
    public CustomerOrder() {
    }
    
    public CustomerOrder(String orderId, String customerId, String region, String productId, 
                        Integer quantity, BigDecimal unitPrice, LocalDateTime orderDate) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.region = region;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.orderDate = orderDate;
        this.status = "PENDING";
    }
    
    public CustomerOrder(String orderId, String customerId, String region, String productId, 
                        Integer quantity, BigDecimal unitPrice, LocalDateTime orderDate, String status) {
        this(orderId, customerId, region, productId, quantity, unitPrice, orderDate);
        this.status = status;
    }
    
    // Getters and Setters
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
    
    public LocalDateTime getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    // Enriched field getters and setters
    
    public String getCustomerTier() {
        return customerTier;
    }
    
    public void setCustomerTier(String customerTier) {
        this.customerTier = customerTier;
    }
    
    public BigDecimal getRegionalDiscount() {
        return regionalDiscount;
    }
    
    public void setRegionalDiscount(BigDecimal regionalDiscount) {
        this.regionalDiscount = regionalDiscount;
    }
    
    public String getSpecialPricing() {
        return specialPricing;
    }
    
    public void setSpecialPricing(String specialPricing) {
        this.specialPricing = specialPricing;
    }
    
    public String getCustomerName() {
        return customerName;
    }
    
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getRegionName() {
        return regionName;
    }
    
    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public BigDecimal getTaxRate() {
        return taxRate;
    }
    
    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }
    
    // Calculated fields
    
    public BigDecimal getTotalPrice() {
        if (unitPrice != null && quantity != null) {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        return BigDecimal.ZERO;
    }
    
    public BigDecimal getDiscountedPrice() {
        BigDecimal total = getTotalPrice();
        if (regionalDiscount != null && total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = total.multiply(regionalDiscount);
            return total.subtract(discount);
        }
        return total;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerOrder that = (CustomerOrder) o;
        return Objects.equals(orderId, that.orderId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }
    
    @Override
    public String toString() {
        return "CustomerOrder{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", region='" + region + '\'' +
                ", productId='" + productId + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", orderDate=" + orderDate +
                ", status='" + status + '\'' +
                ", customerTier='" + customerTier + '\'' +
                ", regionalDiscount=" + regionalDiscount +
                ", specialPricing='" + specialPricing + '\'' +
                ", customerName='" + customerName + '\'' +
                ", regionName='" + regionName + '\'' +
                ", currency='" + currency + '\'' +
                ", taxRate=" + taxRate +
                '}';
    }
}
