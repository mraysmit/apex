package dev.mars.apex.demo.model;

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


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Order Processing model for Rule Configuration Bootstrap Demo.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class OrderProcessing {
    
    private String orderId;
    private String customerId;
    private BigDecimal orderTotal;
    private Integer quantity;
    private LocalDate orderDate;
    private String status;
    private String shippingMethod;
    private BigDecimal discountApplied;
    private String processingPriority;
    private LocalDateTime createdTimestamp;
    
    // Enrichment fields populated by rules
    private String orderCategory;
    private Boolean freeShippingEligible;
    private BigDecimal calculatedDiscount;
    private String recommendedShipping;
    private Boolean expressProcessingEligible;
    private String specialHandlingRequired;
    
    // Default constructor
    public OrderProcessing() {
        this.createdTimestamp = LocalDateTime.now();
        this.status = "PENDING";
        this.discountApplied = BigDecimal.ZERO;
        this.processingPriority = "STANDARD";
    }
    
    // Constructor with essential fields
    public OrderProcessing(String orderId, String customerId, BigDecimal orderTotal, Integer quantity) {
        this();
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderTotal = orderTotal;
        this.quantity = quantity;
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
    
    public BigDecimal getOrderTotal() {
        return orderTotal;
    }
    
    public void setOrderTotal(BigDecimal orderTotal) {
        this.orderTotal = orderTotal;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public LocalDate getOrderDate() {
        return orderDate;
    }
    
    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getShippingMethod() {
        return shippingMethod;
    }
    
    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }
    
    public BigDecimal getDiscountApplied() {
        return discountApplied;
    }
    
    public void setDiscountApplied(BigDecimal discountApplied) {
        this.discountApplied = discountApplied;
    }
    
    public String getProcessingPriority() {
        return processingPriority;
    }
    
    public void setProcessingPriority(String processingPriority) {
        this.processingPriority = processingPriority;
    }
    
    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    // Enrichment field getters and setters
    public String getOrderCategory() {
        return orderCategory;
    }
    
    public void setOrderCategory(String orderCategory) {
        this.orderCategory = orderCategory;
    }
    
    public Boolean getFreeShippingEligible() {
        return freeShippingEligible;
    }
    
    public void setFreeShippingEligible(Boolean freeShippingEligible) {
        this.freeShippingEligible = freeShippingEligible;
    }
    
    public BigDecimal getCalculatedDiscount() {
        return calculatedDiscount;
    }
    
    public void setCalculatedDiscount(BigDecimal calculatedDiscount) {
        this.calculatedDiscount = calculatedDiscount;
    }
    
    public String getRecommendedShipping() {
        return recommendedShipping;
    }
    
    public void setRecommendedShipping(String recommendedShipping) {
        this.recommendedShipping = recommendedShipping;
    }
    
    public Boolean getExpressProcessingEligible() {
        return expressProcessingEligible;
    }
    
    public void setExpressProcessingEligible(Boolean expressProcessingEligible) {
        this.expressProcessingEligible = expressProcessingEligible;
    }
    
    public String getSpecialHandlingRequired() {
        return specialHandlingRequired;
    }
    
    public void setSpecialHandlingRequired(String specialHandlingRequired) {
        this.specialHandlingRequired = specialHandlingRequired;
    }
    
    // Utility methods for rule processing
    public boolean isLargeOrder() {
        return orderTotal != null && orderTotal.compareTo(new BigDecimal("500")) > 0;
    }
    
    public boolean isSmallOrder() {
        return orderTotal != null && orderTotal.compareTo(new BigDecimal("50")) <= 0;
    }
    
    public boolean isHighQuantity() {
        return quantity != null && quantity > 10;
    }
    
    public boolean isLowQuantity() {
        return quantity != null && quantity <= 2;
    }
    
    public boolean isFreeShippingEligible() {
        return orderTotal != null && orderTotal.compareTo(new BigDecimal("100")) > 0;
    }
    
    public boolean isExpressEligible() {
        return "PENDING".equals(status) && quantity != null && quantity < 5;
    }
    
    public boolean requiresSpecialHandling() {
        return isLargeOrder() || isHighQuantity();
    }
    
    public BigDecimal calculateTotal() {
        if (orderTotal == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal total = orderTotal;
        if (discountApplied != null && discountApplied.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(discountApplied);
        }
        
        return total;
    }
    
    public BigDecimal getAverageItemValue() {
        if (quantity == null || quantity == 0 || orderTotal == null) {
            return BigDecimal.ZERO;
        }
        return orderTotal.divide(new BigDecimal(quantity), 2, RoundingMode.HALF_UP);
    }
    
    public void applyDiscount(BigDecimal discountPercentage) {
        if (orderTotal != null && discountPercentage != null) {
            BigDecimal discountAmount = orderTotal.multiply(discountPercentage.divide(new BigDecimal("100")));
            this.discountApplied = discountAmount;
        }
    }
    
    public void applyDiscountAmount(BigDecimal discountAmount) {
        if (discountAmount != null) {
            this.discountApplied = discountAmount;
        }
    }
    
    public BigDecimal getDiscountPercentage() {
        if (orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) == 0 || 
            discountApplied == null || discountApplied.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return discountApplied.divide(orderTotal, 4, RoundingMode.HALF_UP)
                             .multiply(new BigDecimal("100"));
    }
    
    @Override
    public String toString() {
        return String.format("OrderProcessing{id='%s', customerId='%s', total=%s, quantity=%d, status='%s', discount=%s}", 
                           orderId, customerId, orderTotal, quantity, status, discountApplied);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        OrderProcessing that = (OrderProcessing) obj;
        return orderId != null ? orderId.equals(that.orderId) : that.orderId == null;
    }
    
    @Override
    public int hashCode() {
        return orderId != null ? orderId.hashCode() : 0;
    }
}
