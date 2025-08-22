package dev.mars.apex.demo.examples.lookups;

import dev.mars.apex.demo.model.lookups.RiskAssessment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates conditional expression lookup using ternary operators.
 * This example shows how to use conditional expressions in lookup keys
 * for risk-based loan assessment with credit score categorization.
 *
 * Pattern Demonstrated: lookup-key: "#creditScore >= 750 ? 'EXCELLENT' : (#creditScore >= 650 ? 'GOOD' : 'POOR')"
 * Use Case: Risk-based loan assessment with conditional credit score categorization
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public class ConditionalExpressionLookupDemo extends AbstractLookupDemo {
    
    public static void main(String[] args) {
        new ConditionalExpressionLookupDemo().runDemo();
    }
    
    @Override
    protected String getDemoTitle() {
        return "Conditional Expression Lookup Demo - Risk Assessment";
    }
    
    @Override
    protected String getDemoDescription() {
        return "Demonstrates conditional expression pattern using ternary operators in lookup-key:\n" +
               "   '#creditScore >= 750 ? \"EXCELLENT\" : (#creditScore >= 650 ? \"GOOD\" : \"POOR\")'\n" +
               "   This pattern shows how to evaluate conditions dynamically to determine the lookup key,\n" +
               "   enabling risk-based loan assessment where different credit score ranges map to\n" +
               "   different risk categories with corresponding interest rates, loan limits, and\n" +
               "   approval processes. The conditional expression evaluates credit scores and\n" +
               "   categorizes them into EXCELLENT (750+), GOOD (650-749), FAIR (550-649), or POOR (<550).";
    }
    
    @Override
    protected String getYamlConfigPath() {
        return "examples/lookups/conditional-expression-lookup.yaml";
    }
    
    @Override
    protected void loadConfiguration() throws Exception {
        System.out.println("📁 Loading YAML configuration from: " + getYamlConfigPath());
        
        // Load the YAML configuration from classpath
        ruleConfiguration = yamlLoader.loadFromClasspath(getYamlConfigPath());
        rulesEngine = yamlService.createRulesEngineFromYamlConfig(ruleConfiguration);
        
        System.out.println("✅ Configuration loaded successfully");
        System.out.println("   - Enrichments: 1 (risk-based-assessment-enrichment)");
        System.out.println("   - Validations: 5 (credit-score-range, annual-income-positive, requested-amount-positive, employment-status-valid, years-employed-reasonable)");
        System.out.println("   - Lookup Dataset: 4 risk categories (EXCELLENT, GOOD, FAIR, POOR)");
        System.out.println("   - Conditional Expression: creditScore-based ternary evaluation");
    }
    
    @Override
    protected List<RiskAssessment> generateTestData() {
        System.out.println("🏭 Generating test risk assessments...");
        
        List<RiskAssessment> assessments = new ArrayList<>();
        LocalDate baseDate = LocalDate.now();
        
        // Create diverse assessment data with different credit score ranges
        
        // Excellent credit scores (750+)
        assessments.add(new RiskAssessment(
            "RISK-001", 
            "CUST-001", 
            780, // Excellent
            new BigDecimal("120000.00"),
            new BigDecimal("500000.00"),
            "FULL_TIME",
            8,
            "TECHNOLOGY",
            baseDate.minusDays(1),
            "PENDING"
        ));
        
        assessments.add(new RiskAssessment(
            "RISK-002", 
            "CUST-002", 
            820, // Excellent
            new BigDecimal("200000.00"),
            new BigDecimal("800000.00"),
            "FULL_TIME",
            12,
            "FINANCE",
            baseDate.minusDays(2),
            "PENDING"
        ));
        
        // Good credit scores (650-749)
        assessments.add(new RiskAssessment(
            "RISK-003", 
            "CUST-003", 
            720, // Good
            new BigDecimal("85000.00"),
            new BigDecimal("350000.00"),
            "FULL_TIME",
            5,
            "HEALTHCARE",
            baseDate.minusDays(3),
            "PENDING"
        ));
        
        assessments.add(new RiskAssessment(
            "RISK-004", 
            "CUST-004", 
            680, // Good
            new BigDecimal("95000.00"),
            new BigDecimal("400000.00"),
            "FULL_TIME",
            7,
            "EDUCATION",
            baseDate.minusDays(4),
            "PENDING"
        ));
        
        // Fair credit scores (550-649)
        assessments.add(new RiskAssessment(
            "RISK-005", 
            "CUST-005", 
            620, // Fair
            new BigDecimal("65000.00"),
            new BigDecimal("250000.00"),
            "FULL_TIME",
            3,
            "RETAIL",
            baseDate.minusDays(5),
            "PENDING"
        ));
        
        assessments.add(new RiskAssessment(
            "RISK-006", 
            "CUST-006", 
            580, // Fair
            new BigDecimal("55000.00"),
            new BigDecimal("200000.00"),
            "PART_TIME",
            2,
            "HOSPITALITY",
            baseDate.minusDays(6),
            "PENDING"
        ));
        
        // Poor credit scores (<550)
        assessments.add(new RiskAssessment(
            "RISK-007", 
            "CUST-007", 
            520, // Poor
            new BigDecimal("45000.00"),
            new BigDecimal("150000.00"),
            "CONTRACT",
            1,
            "CONSTRUCTION",
            baseDate.minusDays(7),
            "PENDING"
        ));
        
        assessments.add(new RiskAssessment(
            "RISK-008", 
            "CUST-008", 
            480, // Poor
            new BigDecimal("38000.00"),
            new BigDecimal("100000.00"),
            "SELF_EMPLOYED",
            10,
            "FREELANCE",
            baseDate.minusDays(8),
            "PENDING"
        ));
        
        // Edge cases - boundary values
        assessments.add(new RiskAssessment(
            "RISK-009", 
            "CUST-009", 
            750, // Exactly 750 - should be EXCELLENT
            new BigDecimal("100000.00"),
            new BigDecimal("450000.00"),
            "FULL_TIME",
            6,
            "GOVERNMENT",
            baseDate.minusDays(9),
            "PENDING"
        ));
        
        assessments.add(new RiskAssessment(
            "RISK-010", 
            "CUST-010", 
            650, // Exactly 650 - should be GOOD
            new BigDecimal("75000.00"),
            new BigDecimal("300000.00"),
            "FULL_TIME",
            4,
            "MANUFACTURING",
            baseDate.minusDays(10),
            "PENDING"
        ));
        
        assessments.add(new RiskAssessment(
            "RISK-011", 
            "CUST-011", 
            550, // Exactly 550 - should be FAIR
            new BigDecimal("50000.00"),
            new BigDecimal("180000.00"),
            "FULL_TIME",
            2,
            "AGRICULTURE",
            baseDate.minusDays(11),
            "PENDING"
        ));
        
        System.out.println("✅ Generated " + assessments.size() + " test assessments");
        System.out.println("   - Credit Score Ranges: 480-820");
        System.out.println("   - Expected Categories: EXCELLENT (750+), GOOD (650-749), FAIR (550-649), POOR (<550)");
        System.out.println("   - Employment Types: FULL_TIME, PART_TIME, CONTRACT, SELF_EMPLOYED");
        System.out.println("   - Industries: Technology, Finance, Healthcare, Education, Retail, etc.");
        System.out.println("   - Conditional Expression will evaluate each credit score dynamically");
        
        return assessments;
    }
    
    @Override
    protected List<RiskAssessment> processData(List<?> data) throws Exception {
        System.out.println("⚙️  Processing assessments with conditional expression enrichment...");
        
        List<RiskAssessment> results = new ArrayList<>();
        
        for (Object item : data) {
            if (item instanceof RiskAssessment) {
                RiskAssessment assessment = (RiskAssessment) item;
                
                // Simulate enrichment based on conditional expression lookup
                RiskAssessment enriched = simulateConditionalExpressionEnrichment(assessment);
                results.add(enriched);
                
                // Log the lookup process
                String category = evaluateConditionalExpression(assessment.getCreditScore());
                System.out.println("   🔍 Processed " + assessment.getAssessmentId() + 
                                 " (Score: " + assessment.getCreditScore() + ") -> " +
                                 "Category: " + category + 
                                 ", Rate: " + (enriched.getInterestRate() != null ? 
                                     String.format("%.2f%%", enriched.getInterestRate().doubleValue()) : "N/A") +
                                 ", Status: " + enriched.getApprovalStatus());
            }
        }
        
        return results;
    }

    /**
     * Evaluate the conditional expression to determine risk category.
     * This simulates the YAML conditional expression evaluation.
     */
    private String evaluateConditionalExpression(Integer creditScore) {
        if (creditScore == null) {
            return "UNKNOWN";
        }

        // Simulate: #creditScore >= 750 ? 'EXCELLENT' : (#creditScore >= 650 ? 'GOOD' : (#creditScore >= 550 ? 'FAIR' : 'POOR'))
        if (creditScore >= 750) {
            return "EXCELLENT";
        } else if (creditScore >= 650) {
            return "GOOD";
        } else if (creditScore >= 550) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    /**
     * Simulate conditional expression enrichment based on the lookup configuration.
     * This demonstrates what the YAML configuration would do.
     */
    private RiskAssessment simulateConditionalExpressionEnrichment(RiskAssessment original) {
        RiskAssessment enriched = new RiskAssessment(
            original.getAssessmentId(),
            original.getCustomerId(),
            original.getCreditScore(),
            original.getAnnualIncome(),
            original.getRequestedAmount(),
            original.getEmploymentStatus(),
            original.getYearsEmployed(),
            original.getIndustryType(),
            original.getAssessmentDate(),
            original.getStatus()
        );

        // Evaluate conditional expression to get risk category
        String riskCategory = evaluateConditionalExpression(original.getCreditScore());

        // Simulate lookup based on conditional expression result
        switch (riskCategory) {
            case "EXCELLENT":
                enriched.setRiskCategory("EXCELLENT");
                enriched.setRiskLevel("LOW");
                enriched.setInterestRate(new BigDecimal("3.25"));
                enriched.setMaxLoanAmount(new BigDecimal("1000000.00"));
                enriched.setApprovalStatus("AUTO_APPROVED");
                enriched.setRequiredDocuments("Income verification only");
                enriched.setProcessingDays(1);
                enriched.setRiskMitigationActions("None required");
                enriched.setCollateralRequirement(new BigDecimal("0.00"));
                enriched.setReviewerLevel("JUNIOR");
                break;

            case "GOOD":
                enriched.setRiskCategory("GOOD");
                enriched.setRiskLevel("LOW_MEDIUM");
                enriched.setInterestRate(new BigDecimal("4.75"));
                enriched.setMaxLoanAmount(new BigDecimal("750000.00"));
                enriched.setApprovalStatus("FAST_TRACK");
                enriched.setRequiredDocuments("Income verification, employment letter");
                enriched.setProcessingDays(2);
                enriched.setRiskMitigationActions("Employment verification");
                enriched.setCollateralRequirement(new BigDecimal("0.10"));
                enriched.setReviewerLevel("JUNIOR");
                break;

            case "FAIR":
                enriched.setRiskCategory("FAIR");
                enriched.setRiskLevel("MEDIUM");
                enriched.setInterestRate(new BigDecimal("7.25"));
                enriched.setMaxLoanAmount(new BigDecimal("500000.00"));
                enriched.setApprovalStatus("MANUAL_REVIEW");
                enriched.setRequiredDocuments("Income verification, employment letter, bank statements (3 months)");
                enriched.setProcessingDays(5);
                enriched.setRiskMitigationActions("Enhanced due diligence, co-signer evaluation");
                enriched.setCollateralRequirement(new BigDecimal("0.25"));
                enriched.setReviewerLevel("SENIOR");
                break;

            case "POOR":
                enriched.setRiskCategory("POOR");
                enriched.setRiskLevel("HIGH");
                enriched.setInterestRate(new BigDecimal("12.50"));
                enriched.setMaxLoanAmount(new BigDecimal("250000.00"));
                enriched.setApprovalStatus("DETAILED_REVIEW");
                enriched.setRequiredDocuments("Full financial disclosure, tax returns (2 years), bank statements (6 months), references");
                enriched.setProcessingDays(10);
                enriched.setRiskMitigationActions("Mandatory co-signer, asset verification, debt consolidation plan");
                enriched.setCollateralRequirement(new BigDecimal("0.50"));
                enriched.setReviewerLevel("EXECUTIVE");
                break;

            default:
                // Unknown category - set defaults
                enriched.setRiskCategory("UNKNOWN");
                enriched.setRiskLevel("HIGH");
                enriched.setInterestRate(new BigDecimal("15.00"));
                enriched.setMaxLoanAmount(new BigDecimal("100000.00"));
                enriched.setApprovalStatus("REJECTED");
                enriched.setRequiredDocuments("Complete financial review required");
                enriched.setProcessingDays(15);
                enriched.setRiskMitigationActions("Full risk assessment required");
                enriched.setCollateralRequirement(new BigDecimal("0.75"));
                enriched.setReviewerLevel("EXECUTIVE");
                break;
        }

        return enriched;
    }

    @Override
    protected List<RiskAssessment> generateErrorTestData() {
        System.out.println("⚠️  Generating error scenario test data...");

        List<RiskAssessment> errorAssessments = new ArrayList<>();
        LocalDate baseDate = LocalDate.now();

        // Invalid credit score - too low
        errorAssessments.add(new RiskAssessment(
            "ERR-001",
            "CUST-ERR-001",
            250, // Below minimum valid range (300)
            new BigDecimal("50000.00"),
            new BigDecimal("100000.00"),
            "FULL_TIME",
            3,
            "RETAIL",
            baseDate
        ));

        // Invalid credit score - too high
        errorAssessments.add(new RiskAssessment(
            "ERR-002",
            "CUST-ERR-002",
            900, // Above maximum valid range (850)
            new BigDecimal("100000.00"),
            new BigDecimal("500000.00"),
            "FULL_TIME",
            5,
            "TECHNOLOGY",
            baseDate
        ));

        // Null credit score
        RiskAssessment nullCreditScore = new RiskAssessment(
            "ERR-003",
            "CUST-ERR-003",
            null, // Null credit score
            new BigDecimal("75000.00"),
            new BigDecimal("300000.00"),
            "FULL_TIME",
            4,
            "FINANCE",
            baseDate
        );
        errorAssessments.add(nullCreditScore);

        // Negative annual income
        errorAssessments.add(new RiskAssessment(
            "ERR-004",
            "CUST-ERR-004",
            720,
            new BigDecimal("-50000.00"), // Negative income
            new BigDecimal("200000.00"),
            "FULL_TIME",
            2,
            "HEALTHCARE",
            baseDate
        ));

        // Requested amount too small
        errorAssessments.add(new RiskAssessment(
            "ERR-005",
            "CUST-ERR-005",
            680,
            new BigDecimal("80000.00"),
            new BigDecimal("500.00"), // Below minimum ($1,000)
            "FULL_TIME",
            6,
            "EDUCATION",
            baseDate
        ));

        // Requested amount too large
        errorAssessments.add(new RiskAssessment(
            "ERR-006",
            "CUST-ERR-006",
            800,
            new BigDecimal("200000.00"),
            new BigDecimal("15000000.00"), // Above maximum ($10,000,000)
            "FULL_TIME",
            10,
            "FINANCE",
            baseDate
        ));

        // Invalid employment status
        errorAssessments.add(new RiskAssessment(
            "ERR-007",
            "CUST-ERR-007",
            650,
            new BigDecimal("60000.00"),
            new BigDecimal("250000.00"),
            "INVALID_STATUS", // Invalid employment status
            3,
            "MANUFACTURING",
            baseDate
        ));

        // Invalid years employed - negative
        errorAssessments.add(new RiskAssessment(
            "ERR-008",
            "CUST-ERR-008",
            700,
            new BigDecimal("90000.00"),
            new BigDecimal("400000.00"),
            "FULL_TIME",
            -5, // Negative years employed
            "TECHNOLOGY",
            baseDate
        ));

        // Invalid years employed - too high
        errorAssessments.add(new RiskAssessment(
            "ERR-009",
            "CUST-ERR-009",
            750,
            new BigDecimal("120000.00"),
            new BigDecimal("600000.00"),
            "FULL_TIME",
            75, // Too many years employed (above 50)
            "GOVERNMENT",
            baseDate
        ));

        System.out.println("✅ Generated " + errorAssessments.size() + " error scenario assessments");

        return errorAssessments;
    }
}
