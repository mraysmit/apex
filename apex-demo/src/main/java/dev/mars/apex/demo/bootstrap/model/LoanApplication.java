package dev.mars.apex.demo.bootstrap.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loan Application model for Rule Configuration Bootstrap Demo.
 * Represents a loan application with all relevant fields for rule processing.
 */
public class LoanApplication {
    
    private String applicationId;
    private String customerId;
    private BigDecimal loanAmount;
    private Integer creditScore;
    private BigDecimal debtToIncomeRatio;
    private Integer employmentYears;
    private BigDecimal annualIncome;
    private String loanPurpose;
    private LocalDate applicationDate;
    private String status;
    private String decisionReason;
    private LocalDateTime createdTimestamp;
    
    // Enrichment fields populated by rules
    private String riskCategory;
    private String approvalDecision;
    private BigDecimal recommendedAmount;
    private String processingPriority;
    private Double riskScore;
    
    // Default constructor
    public LoanApplication() {
        this.createdTimestamp = LocalDateTime.now();
        this.status = "PENDING";
    }
    
    // Constructor with essential fields
    public LoanApplication(String applicationId, String customerId, BigDecimal loanAmount, 
                          Integer creditScore, BigDecimal debtToIncomeRatio) {
        this();
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.loanAmount = loanAmount;
        this.creditScore = creditScore;
        this.debtToIncomeRatio = debtToIncomeRatio;
    }
    
    // Getters and Setters
    public String getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public BigDecimal getLoanAmount() {
        return loanAmount;
    }
    
    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }
    
    public Integer getCreditScore() {
        return creditScore;
    }
    
    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }
    
    public BigDecimal getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }
    
    public void setDebtToIncomeRatio(BigDecimal debtToIncomeRatio) {
        this.debtToIncomeRatio = debtToIncomeRatio;
    }
    
    public Integer getEmploymentYears() {
        return employmentYears;
    }
    
    public void setEmploymentYears(Integer employmentYears) {
        this.employmentYears = employmentYears;
    }
    
    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }
    
    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }
    
    public String getLoanPurpose() {
        return loanPurpose;
    }
    
    public void setLoanPurpose(String loanPurpose) {
        this.loanPurpose = loanPurpose;
    }
    
    public LocalDate getApplicationDate() {
        return applicationDate;
    }
    
    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDecisionReason() {
        return decisionReason;
    }
    
    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }
    
    public LocalDateTime getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(LocalDateTime createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    // Enrichment field getters and setters
    public String getRiskCategory() {
        return riskCategory;
    }
    
    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }
    
    public String getApprovalDecision() {
        return approvalDecision;
    }
    
    public void setApprovalDecision(String approvalDecision) {
        this.approvalDecision = approvalDecision;
    }
    
    public BigDecimal getRecommendedAmount() {
        return recommendedAmount;
    }
    
    public void setRecommendedAmount(BigDecimal recommendedAmount) {
        this.recommendedAmount = recommendedAmount;
    }
    
    public String getProcessingPriority() {
        return processingPriority;
    }
    
    public void setProcessingPriority(String processingPriority) {
        this.processingPriority = processingPriority;
    }
    
    public Double getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }
    
    // Utility methods for rule processing
    public boolean isHighRisk() {
        return creditScore != null && creditScore < 650;
    }
    
    public boolean isLowRisk() {
        return creditScore != null && creditScore >= 750;
    }
    
    public boolean hasHighDebtRatio() {
        return debtToIncomeRatio != null && debtToIncomeRatio.compareTo(new BigDecimal("0.40")) > 0;
    }
    
    public boolean isLargeAmount() {
        return loanAmount != null && loanAmount.compareTo(new BigDecimal("100000")) > 0;
    }
    
    public boolean isExperiencedBorrower() {
        return employmentYears != null && employmentYears >= 5;
    }
    
    // Calculate debt-to-income ratio if not set
    public BigDecimal calculateDebtToIncomeRatio() {
        if (debtToIncomeRatio != null) {
            return debtToIncomeRatio;
        }
        
        if (annualIncome != null && annualIncome.compareTo(BigDecimal.ZERO) > 0) {
            // Estimate monthly debt payment as a percentage of loan amount
            BigDecimal estimatedMonthlyDebt = loanAmount.multiply(new BigDecimal("0.01"));
            BigDecimal monthlyIncome = annualIncome.divide(new BigDecimal("12"), 4, BigDecimal.ROUND_HALF_UP);
            return estimatedMonthlyDebt.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }
    
    @Override
    public String toString() {
        return String.format("LoanApplication{id='%s', customerId='%s', amount=%s, creditScore=%d, dti=%s, status='%s'}", 
                           applicationId, customerId, loanAmount, creditScore, debtToIncomeRatio, status);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LoanApplication that = (LoanApplication) obj;
        return applicationId != null ? applicationId.equals(that.applicationId) : that.applicationId == null;
    }
    
    @Override
    public int hashCode() {
        return applicationId != null ? applicationId.hashCode() : 0;
    }
}
