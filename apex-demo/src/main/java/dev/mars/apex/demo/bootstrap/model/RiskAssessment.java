package dev.mars.apex.demo.bootstrap.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Model class representing a risk assessment for conditional expression lookup demonstrations.
 * Used to demonstrate lookup-key patterns with conditional expressions: 
 * "#creditScore >= 750 ? 'EXCELLENT' : (#creditScore >= 650 ? 'GOOD' : 'POOR')"
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class RiskAssessment {
    
    private String assessmentId;
    private String customerId;
    private Integer creditScore;
    private BigDecimal annualIncome;
    private BigDecimal requestedAmount;
    private String employmentStatus;
    private Integer yearsEmployed;
    private String industryType;
    private LocalDate assessmentDate;
    private String status;
    
    // Enriched fields (populated by conditional expression lookup)
    private String riskCategory;
    private String riskLevel;
    private BigDecimal interestRate;
    private BigDecimal maxLoanAmount;
    private String approvalStatus;
    private String requiredDocuments;
    private Integer processingDays;
    private String riskMitigationActions;
    private BigDecimal collateralRequirement;
    private String reviewerLevel;
    
    public RiskAssessment() {
    }
    
    public RiskAssessment(String assessmentId, String customerId, Integer creditScore, 
                         BigDecimal annualIncome, BigDecimal requestedAmount, String employmentStatus,
                         Integer yearsEmployed, String industryType, LocalDate assessmentDate) {
        this.assessmentId = assessmentId;
        this.customerId = customerId;
        this.creditScore = creditScore;
        this.annualIncome = annualIncome;
        this.requestedAmount = requestedAmount;
        this.employmentStatus = employmentStatus;
        this.yearsEmployed = yearsEmployed;
        this.industryType = industryType;
        this.assessmentDate = assessmentDate;
        this.status = "PENDING";
    }
    
    public RiskAssessment(String assessmentId, String customerId, Integer creditScore, 
                         BigDecimal annualIncome, BigDecimal requestedAmount, String employmentStatus,
                         Integer yearsEmployed, String industryType, LocalDate assessmentDate, String status) {
        this(assessmentId, customerId, creditScore, annualIncome, requestedAmount, 
             employmentStatus, yearsEmployed, industryType, assessmentDate);
        this.status = status;
    }
    
    // Getters and Setters
    
    public String getAssessmentId() {
        return assessmentId;
    }
    
    public void setAssessmentId(String assessmentId) {
        this.assessmentId = assessmentId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public Integer getCreditScore() {
        return creditScore;
    }
    
    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }
    
    public BigDecimal getAnnualIncome() {
        return annualIncome;
    }
    
    public void setAnnualIncome(BigDecimal annualIncome) {
        this.annualIncome = annualIncome;
    }
    
    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
    
    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }
    
    public String getEmploymentStatus() {
        return employmentStatus;
    }
    
    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }
    
    public Integer getYearsEmployed() {
        return yearsEmployed;
    }
    
    public void setYearsEmployed(Integer yearsEmployed) {
        this.yearsEmployed = yearsEmployed;
    }
    
    public String getIndustryType() {
        return industryType;
    }
    
    public void setIndustryType(String industryType) {
        this.industryType = industryType;
    }
    
    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }
    
    public void setAssessmentDate(LocalDate assessmentDate) {
        this.assessmentDate = assessmentDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    // Enriched field getters and setters
    
    public String getRiskCategory() {
        return riskCategory;
    }
    
    public void setRiskCategory(String riskCategory) {
        this.riskCategory = riskCategory;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
    
    public BigDecimal getInterestRate() {
        return interestRate;
    }
    
    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }
    
    public BigDecimal getMaxLoanAmount() {
        return maxLoanAmount;
    }
    
    public void setMaxLoanAmount(BigDecimal maxLoanAmount) {
        this.maxLoanAmount = maxLoanAmount;
    }
    
    public String getApprovalStatus() {
        return approvalStatus;
    }
    
    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
    
    public String getRequiredDocuments() {
        return requiredDocuments;
    }
    
    public void setRequiredDocuments(String requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }
    
    public Integer getProcessingDays() {
        return processingDays;
    }
    
    public void setProcessingDays(Integer processingDays) {
        this.processingDays = processingDays;
    }
    
    public String getRiskMitigationActions() {
        return riskMitigationActions;
    }
    
    public void setRiskMitigationActions(String riskMitigationActions) {
        this.riskMitigationActions = riskMitigationActions;
    }
    
    public BigDecimal getCollateralRequirement() {
        return collateralRequirement;
    }
    
    public void setCollateralRequirement(BigDecimal collateralRequirement) {
        this.collateralRequirement = collateralRequirement;
    }
    
    public String getReviewerLevel() {
        return reviewerLevel;
    }
    
    public void setReviewerLevel(String reviewerLevel) {
        this.reviewerLevel = reviewerLevel;
    }
    
    // Calculated fields
    
    public BigDecimal getDebtToIncomeRatio() {
        if (annualIncome != null && requestedAmount != null && annualIncome.compareTo(BigDecimal.ZERO) > 0) {
            return requestedAmount.divide(annualIncome, 4, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
    
    public boolean isHighRisk() {
        return creditScore != null && creditScore < 600;
    }
    
    public boolean isLowRisk() {
        return creditScore != null && creditScore >= 750;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskAssessment that = (RiskAssessment) o;
        return Objects.equals(assessmentId, that.assessmentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(assessmentId);
    }
    
    @Override
    public String toString() {
        return "RiskAssessment{" +
                "assessmentId='" + assessmentId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", creditScore=" + creditScore +
                ", annualIncome=" + annualIncome +
                ", requestedAmount=" + requestedAmount +
                ", employmentStatus='" + employmentStatus + '\'' +
                ", yearsEmployed=" + yearsEmployed +
                ", industryType='" + industryType + '\'' +
                ", assessmentDate=" + assessmentDate +
                ", status='" + status + '\'' +
                ", riskCategory='" + riskCategory + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                ", interestRate=" + interestRate +
                ", maxLoanAmount=" + maxLoanAmount +
                ", approvalStatus='" + approvalStatus + '\'' +
                ", requiredDocuments='" + requiredDocuments + '\'' +
                ", processingDays=" + processingDays +
                ", riskMitigationActions='" + riskMitigationActions + '\'' +
                ", collateralRequirement=" + collateralRequirement +
                ", reviewerLevel='" + reviewerLevel + '\'' +
                '}';
    }
}
