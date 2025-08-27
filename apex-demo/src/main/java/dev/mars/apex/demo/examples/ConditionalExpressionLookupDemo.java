package dev.mars.apex.demo.examples;

import dev.mars.apex.demo.examples.lookups.AbstractLookupDemo;
import dev.mars.apex.demo.model.RiskAssessment;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRule;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

                // Use actual APEX rules engine to process the assessment
                RiskAssessment enriched = processAssessmentWithApexEngine(assessment);
                results.add(enriched);

                // Log the lookup process
                System.out.println("   🔍 Processed " + assessment.getAssessmentId() +
                                 " (Score: " + assessment.getCreditScore() + ") -> " +
                                 "Category: " + enriched.getRiskCategory() +
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
     * Process assessment using the actual APEX rules engine with the loaded YAML configuration.
     * This replaces the previous simulation approach with real APEX functionality.
     */
    private RiskAssessment processAssessmentWithApexEngine(RiskAssessment original) throws Exception {
        // Create a copy of the original assessment to enrich
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

        // Convert assessment to Map for APEX processing
        Map<String, Object> assessmentData = convertAssessmentToMap(enriched);

        // Apply enrichments using the APEX rules engine
        if (ruleConfiguration != null && ruleConfiguration.getEnrichments() != null) {
            for (YamlEnrichment enrichment : ruleConfiguration.getEnrichments()) {
                if (enrichment.getEnabled() != null && enrichment.getEnabled()) {
                    applyEnrichmentToAssessment(enrichment, assessmentData);
                }
            }
        }

        // Apply validation rules using the APEX rules engine
        if (ruleConfiguration != null && ruleConfiguration.getRules() != null) {
            for (YamlRule rule : ruleConfiguration.getRules()) {
                if (rule.getEnabled() != null && rule.getEnabled()) {
                    applyValidationRuleToAssessment(rule, assessmentData);
                }
            }
        }

        // Convert enriched data back to RiskAssessment object
        updateAssessmentFromMap(enriched, assessmentData);

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

    /**
     * Convert RiskAssessment to Map for APEX processing.
     */
    private Map<String, Object> convertAssessmentToMap(RiskAssessment assessment) {
        Map<String, Object> map = new HashMap<>();
        map.put("assessmentId", assessment.getAssessmentId());
        map.put("customerId", assessment.getCustomerId());
        map.put("creditScore", assessment.getCreditScore());
        map.put("annualIncome", assessment.getAnnualIncome());
        map.put("requestedAmount", assessment.getRequestedAmount());
        map.put("employmentStatus", assessment.getEmploymentStatus());
        map.put("yearsEmployed", assessment.getYearsEmployed());
        map.put("industryType", assessment.getIndustryType());
        map.put("assessmentDate", assessment.getAssessmentDate());
        map.put("status", assessment.getStatus());

        // Add existing enriched fields if present
        if (assessment.getRiskCategory() != null) map.put("riskCategory", assessment.getRiskCategory());
        if (assessment.getRiskLevel() != null) map.put("riskLevel", assessment.getRiskLevel());
        if (assessment.getInterestRate() != null) map.put("interestRate", assessment.getInterestRate());
        if (assessment.getMaxLoanAmount() != null) map.put("maxLoanAmount", assessment.getMaxLoanAmount());
        if (assessment.getApprovalStatus() != null) map.put("approvalStatus", assessment.getApprovalStatus());
        if (assessment.getRequiredDocuments() != null) map.put("requiredDocuments", assessment.getRequiredDocuments());
        if (assessment.getProcessingDays() != null) map.put("processingDays", assessment.getProcessingDays());
        if (assessment.getRiskMitigationActions() != null) map.put("riskMitigationActions", assessment.getRiskMitigationActions());
        if (assessment.getCollateralRequirement() != null) map.put("collateralRequirement", assessment.getCollateralRequirement());
        if (assessment.getReviewerLevel() != null) map.put("reviewerLevel", assessment.getReviewerLevel());

        return map;
    }

    /**
     * Apply enrichment to assessment data using APEX engine.
     */
    private void applyEnrichmentToAssessment(YamlEnrichment enrichment, Map<String, Object> assessmentData) throws Exception {
        // Apply lookup enrichment directly using the YAML configuration
        if (enrichment.getLookupConfig() != null) {
            applyLookupEnrichmentToAssessment(enrichment, assessmentData);
        }
    }

    /**
     * Apply lookup enrichment using the APEX engine.
     */
    private void applyLookupEnrichmentToAssessment(YamlEnrichment enrichment, Map<String, Object> assessmentData) throws Exception {
        // Create evaluation context
        StandardEvaluationContext context = new StandardEvaluationContext();
        assessmentData.forEach(context::setVariable);

        // Evaluate condition
        if (enrichment.getCondition() != null) {
            ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
            Boolean conditionResult = evaluator.evaluate(enrichment.getCondition(), context, Boolean.class);
            if (conditionResult == null || !conditionResult) {
                return; // Condition not met, skip enrichment
            }
        }

        // Apply lookup enrichment using the dataset from YAML
        if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupDataset() != null) {
            String lookupKey = enrichment.getLookupConfig().getLookupKey();
            if (lookupKey != null) {
                ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
                String keyValue = evaluator.evaluate(lookupKey, context, String.class);

                // Find matching data in the lookup dataset
                var dataset = enrichment.getLookupConfig().getLookupDataset();
                if (dataset.getData() != null) {
                    String keyField = dataset.getKeyField();
                    for (Map<String, Object> dataRow : dataset.getData()) {
                        if (keyValue != null && keyValue.equals(dataRow.get(keyField))) {
                            // Apply field mappings
                            if (enrichment.getFieldMappings() != null) {
                                for (var mapping : enrichment.getFieldMappings()) {
                                    Object sourceValue = dataRow.get(mapping.getSourceField());
                                    if (sourceValue != null) {
                                        assessmentData.put(mapping.getTargetField(), sourceValue);
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply validation rule to assessment data.
     */
    private void applyValidationRuleToAssessment(YamlRule rule, Map<String, Object> assessmentData) throws Exception {
        if (rule.getCondition() != null) {
            StandardEvaluationContext context = new StandardEvaluationContext();
            assessmentData.forEach(context::setVariable);

            ExpressionEvaluatorService evaluator = new ExpressionEvaluatorService();
            Boolean result = evaluator.evaluate(rule.getCondition(), context, Boolean.class);

            if (result == null || !result) {
                System.out.println("   ⚠️  Validation failed: " + rule.getName() + " - " + rule.getMessage());
            }
        }
    }

    /**
     * Update RiskAssessment from enriched Map data.
     */
    private void updateAssessmentFromMap(RiskAssessment assessment, Map<String, Object> assessmentData) {
        // Update enriched fields
        if (assessmentData.containsKey("riskCategory")) {
            assessment.setRiskCategory((String) assessmentData.get("riskCategory"));
        }
        if (assessmentData.containsKey("riskLevel")) {
            assessment.setRiskLevel((String) assessmentData.get("riskLevel"));
        }
        if (assessmentData.containsKey("interestRate")) {
            Object interestRate = assessmentData.get("interestRate");
            if (interestRate instanceof Number) {
                assessment.setInterestRate(new BigDecimal(interestRate.toString()));
            }
        }
        if (assessmentData.containsKey("maxLoanAmount")) {
            Object maxLoanAmount = assessmentData.get("maxLoanAmount");
            if (maxLoanAmount instanceof Number) {
                assessment.setMaxLoanAmount(new BigDecimal(maxLoanAmount.toString()));
            }
        }
        if (assessmentData.containsKey("approvalStatus")) {
            assessment.setApprovalStatus((String) assessmentData.get("approvalStatus"));
        }
        if (assessmentData.containsKey("requiredDocuments")) {
            assessment.setRequiredDocuments((String) assessmentData.get("requiredDocuments"));
        }
        if (assessmentData.containsKey("processingDays")) {
            Object processingDays = assessmentData.get("processingDays");
            if (processingDays instanceof Number) {
                assessment.setProcessingDays(((Number) processingDays).intValue());
            }
        }
        if (assessmentData.containsKey("riskMitigationActions")) {
            assessment.setRiskMitigationActions((String) assessmentData.get("riskMitigationActions"));
        }
        if (assessmentData.containsKey("collateralRequirement")) {
            Object collateralRequirement = assessmentData.get("collateralRequirement");
            if (collateralRequirement instanceof Number) {
                assessment.setCollateralRequirement(new BigDecimal(collateralRequirement.toString()));
            }
        }
        if (assessmentData.containsKey("reviewerLevel")) {
            assessment.setReviewerLevel((String) assessmentData.get("reviewerLevel"));
        }
    }
}
