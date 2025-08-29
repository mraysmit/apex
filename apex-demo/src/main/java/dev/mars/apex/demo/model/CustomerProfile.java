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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

/**
 * Customer Profile model for Rule Configuration Bootstrap Demo.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class CustomerProfile {
    
    private String customerId;
    private String firstName;
    private String lastName;
    private Integer age;
    private String membershipLevel;
    private LocalDate customerSince;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private List<String> preferredCategories;
    private Boolean kycVerified;
    private Integer riskScore;
    private LocalDateTime createdTimestamp;
    
    // Enrichment fields populated by rules
    private String customerSegment;
    private String loyaltyTier;
    private BigDecimal discountEligibility;
    private String processingPriority;
    private Boolean eligibleForPremiumServices;
    
    // Default constructor
    public CustomerProfile() {
        this.createdTimestamp = LocalDateTime.now();
        this.kycVerified = false;
        this.riskScore = 5; // Default medium risk
        this.totalOrders = 0;
        this.totalSpent = BigDecimal.ZERO;
    }
    
    // Constructor with essential fields
    public CustomerProfile(String customerId, String firstName, String lastName, 
                          Integer age, String membershipLevel) {
        this();
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.membershipLevel = membershipLevel;
    }
    
    // Getters and Setters
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String getMembershipLevel() {
        return membershipLevel;
    }
    
    public void setMembershipLevel(String membershipLevel) {
        this.membershipLevel = membershipLevel;
    }
    
    public LocalDate getCustomerSince() {
        return customerSince;
    }
    
    public void setCustomerSince(LocalDate customerSince) {
        this.customerSince = customerSince;
    }
    
    public Integer getTotalOrders() {
        return totalOrders;
    }
    
    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }
    
    public BigDecimal getTotalSpent() {
        return totalSpent;
    }
    
    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }
    
    public List<String> getPreferredCategories() {
        return preferredCategories;
    }
    
    public void setPreferredCategories(List<String> preferredCategories) {
        this.preferredCategories = preferredCategories;
    }
    
    public Boolean getKycVerified() {
        return kycVerified;
    }
    
    public void setKycVerified(Boolean kycVerified) {
        this.kycVerified = kycVerified;
    }
    
    public Integer getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }
    
    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    // Enrichment field getters and setters
    public String getCustomerSegment() {
        return customerSegment;
    }
    
    public void setCustomerSegment(String customerSegment) {
        this.customerSegment = customerSegment;
    }
    
    public String getLoyaltyTier() {
        return loyaltyTier;
    }
    
    public void setLoyaltyTier(String loyaltyTier) {
        this.loyaltyTier = loyaltyTier;
    }
    
    public BigDecimal getDiscountEligibility() {
        return discountEligibility;
    }
    
    public void setDiscountEligibility(BigDecimal discountEligibility) {
        this.discountEligibility = discountEligibility;
    }
    
    public String getProcessingPriority() {
        return processingPriority;
    }
    
    public void setProcessingPriority(String processingPriority) {
        this.processingPriority = processingPriority;
    }
    
    public Boolean getEligibleForPremiumServices() {
        return eligibleForPremiumServices;
    }
    
    public void setEligibleForPremiumServices(Boolean eligibleForPremiumServices) {
        this.eligibleForPremiumServices = eligibleForPremiumServices;
    }
    
    // Utility methods for rule processing
    public Integer getCustomerYears() {
        if (customerSince == null) {
            return 0;
        }
        return Period.between(customerSince, LocalDate.now()).getYears();
    }
    
    public boolean isNewCustomer() {
        return getCustomerYears() == 0;
    }
    
    public boolean isLoyalCustomer() {
        return getCustomerYears() >= 5;
    }
    
    public boolean isHighValueCustomer() {
        return totalSpent != null && totalSpent.compareTo(new BigDecimal("10000")) > 0;
    }
    
    public boolean isFrequentBuyer() {
        return totalOrders != null && totalOrders >= 20;
    }
    
    public boolean isSeniorCustomer() {
        return age != null && age >= 60;
    }
    
    public boolean isHighRiskCustomer() {
        return riskScore != null && riskScore >= 7;
    }
    
    public boolean isLowRiskCustomer() {
        return riskScore != null && riskScore <= 3;
    }
    
    public boolean isPremiumMember() {
        return "Gold".equals(membershipLevel) || "Platinum".equals(membershipLevel);
    }
    
    public boolean isEligibleForDiscount() {
        return isPremiumMember() || isLoyalCustomer() || isHighValueCustomer();
    }
    
    public BigDecimal getAverageOrderValue() {
        if (totalOrders == null || totalOrders == 0 || totalSpent == null) {
            return BigDecimal.ZERO;
        }
        return totalSpent.divide(new BigDecimal(totalOrders), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) {
            fullName.append(firstName);
        }
        if (lastName != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        return fullName.toString();
    }
    
    @Override
    public String toString() {
        return String.format("CustomerProfile{id='%s', name='%s %s', membership='%s', years=%d, orders=%d, spent=%s}", 
                           customerId, firstName, lastName, membershipLevel, getCustomerYears(), totalOrders, totalSpent);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CustomerProfile that = (CustomerProfile) obj;
        return customerId != null ? customerId.equals(that.customerId) : that.customerId == null;
    }
    
    @Override
    public int hashCode() {
        return customerId != null ? customerId.hashCode() : 0;
    }
}
