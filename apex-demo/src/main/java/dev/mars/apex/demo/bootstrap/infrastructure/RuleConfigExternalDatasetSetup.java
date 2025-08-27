package dev.mars.apex.demo.bootstrap.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * External dataset setup component for Rule Configuration Bootstrap Demo.
 * Creates external YAML files containing rule configurations and reference data.
 */
@Component
public class RuleConfigExternalDatasetSetup {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleConfigExternalDatasetSetup.class);
    
    /**
     * Creates all external YAML rule configuration files.
     */
    public void createRuleConfigurationFiles() {
        logger.info("Creating external rule configuration YAML files...");
        
        try {
            // Create the bootstrap datasets directory if it doesn't exist
            Path datasetsDir = Paths.get("src/main/resources/bootstrap/datasets");
            Files.createDirectories(datasetsDir);
            
            // Create individual rule configuration files
            createLoanApprovalRulesFile();
            createDiscountRulesFile();
            createCombinedRulesFile();
            createMainBootstrapConfigFile();
            
            logger.info("All external rule configuration files created successfully");
            
        } catch (Exception e) {
            logger.error("Failed to create external rule configuration files: {}", e.getMessage(), e);
            throw new RuntimeException("External dataset setup failed", e);
        }
    }
    
    /**
     * Creates the loan approval rules YAML file.
     */
    private void createLoanApprovalRulesFile() throws IOException {
        logger.info("Creating loan approval rules YAML file...");
        
        String yamlContent = generateLoanApprovalRulesYaml();
        Path loanRulesFile = Paths.get("src/main/resources/bootstrap/datasets/loan-approval-rules.yaml");
        Files.writeString(loanRulesFile, yamlContent);
        
        logger.info("Loan approval rules file created: {}", loanRulesFile);
    }
    
    /**
     * Creates the discount rules YAML file.
     */
    private void createDiscountRulesFile() throws IOException {
        logger.info("Creating discount rules YAML file...");
        
        String yamlContent = generateDiscountRulesYaml();
        Path discountRulesFile = Paths.get("src/main/resources/bootstrap/datasets/discount-rules.yaml");
        Files.writeString(discountRulesFile, yamlContent);
        
        logger.info("Discount rules file created: {}", discountRulesFile);
    }
    
    /**
     * Creates the combined rules YAML file.
     */
    private void createCombinedRulesFile() throws IOException {
        logger.info("Creating combined rules YAML file...");
        
        String yamlContent = generateCombinedRulesYaml();
        Path combinedRulesFile = Paths.get("src/main/resources/bootstrap/datasets/combined-rules.yaml");
        Files.writeString(combinedRulesFile, yamlContent);
        
        logger.info("Combined rules file created: {}", combinedRulesFile);
    }
    
    /**
     * Creates the main bootstrap configuration file.
     */
    private void createMainBootstrapConfigFile() throws IOException {
        logger.info("Creating main bootstrap configuration file...");
        
        String yamlContent = generateMainBootstrapConfigYaml();
        Path mainConfigFile = Paths.get("src/main/resources/bootstrap/rule-configuration-bootstrap.yaml");
        Files.writeString(mainConfigFile, yamlContent);
        
        logger.info("Main bootstrap configuration file created: {}", mainConfigFile);
    }
    
    /**
     * Generates the loan approval rules YAML content.
     */
    private String generateLoanApprovalRulesYaml() {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# ============================================================================\n");
        yaml.append("# APEX Rule Configuration Bootstrap Demo - Loan Approval Rules\n");
        yaml.append("# ============================================================================\n");
        yaml.append("# Generated: ").append(LocalDate.now()).append("\n");
        yaml.append("# Purpose: Demonstrates data-driven loan approval business rules\n");
        yaml.append("# ============================================================================\n\n");
        
        yaml.append("metadata:\n");
        yaml.append("  name: \"Loan Approval Rules\"\n");
        yaml.append("  version: \"1.0.0\"\n");
        yaml.append("  description: \"Business rules for automated loan approval decisions\"\n");
        yaml.append("  category: \"loan-approval\"\n");
        yaml.append("  created-date: \"").append(LocalDate.now()).append("\"\n\n");
        
        yaml.append("# Loan approval business rules\n");
        yaml.append("rules:\n");
        yaml.append("  - id: \"LA001\"\n");
        yaml.append("    name: \"approve-good-credit\"\n");
        yaml.append("    category: \"loan-approval\"\n");
        yaml.append("    priority: 10\n");
        yaml.append("    condition: \"#creditScore >= 700 and #debtToIncomeRatio <= 0.36\"\n");
        yaml.append("    action: \"APPROVE\"\n");
        yaml.append("    message: \"Loan approved based on good credit\"\n");
        yaml.append("    description: \"Approves loans for applicants with credit score >= 700 and debt-to-income ratio <= 36%\"\n\n");
        
        yaml.append("  - id: \"LA002\"\n");
        yaml.append("    name: \"approve-excellent-credit\"\n");
        yaml.append("    category: \"loan-approval\"\n");
        yaml.append("    priority: 5\n");
        yaml.append("    condition: \"#creditScore >= 750\"\n");
        yaml.append("    action: \"APPROVE\"\n");
        yaml.append("    message: \"Loan approved based on excellent credit\"\n");
        yaml.append("    description: \"Approves loans for applicants with credit score >= 750\"\n\n");
        
        yaml.append("  - id: \"LA003\"\n");
        yaml.append("    name: \"reject-poor-credit\"\n");
        yaml.append("    category: \"loan-approval\"\n");
        yaml.append("    priority: 20\n");
        yaml.append("    condition: \"#creditScore < 620\"\n");
        yaml.append("    action: \"REJECT\"\n");
        yaml.append("    message: \"Loan rejected due to poor credit\"\n");
        yaml.append("    description: \"Rejects loans for applicants with credit score < 620\"\n\n");
        
        yaml.append("  - id: \"LA004\"\n");
        yaml.append("    name: \"reject-high-dti\"\n");
        yaml.append("    category: \"loan-approval\"\n");
        yaml.append("    priority: 30\n");
        yaml.append("    condition: \"#debtToIncomeRatio > 0.43\"\n");
        yaml.append("    action: \"REJECT\"\n");
        yaml.append("    message: \"Loan rejected due to high debt-to-income ratio\"\n");
        yaml.append("    description: \"Rejects loans for applicants with debt-to-income ratio > 43%\"\n\n");
        
        yaml.append("  - id: \"LA005\"\n");
        yaml.append("    name: \"refer-moderate-credit\"\n");
        yaml.append("    category: \"loan-approval\"\n");
        yaml.append("    priority: 40\n");
        yaml.append("    condition: \"#creditScore >= 620 and #creditScore < 700 and #debtToIncomeRatio <= 0.43\"\n");
        yaml.append("    action: \"REFER\"\n");
        yaml.append("    message: \"Loan referred for manual review\"\n");
        yaml.append("    description: \"Refers loans for manual review for applicants with credit score between 620 and 700\"\n\n");
        
        yaml.append("  - id: \"LA006\"\n");
        yaml.append("    name: \"reject-default\"\n");
        yaml.append("    category: \"loan-approval\"\n");
        yaml.append("    priority: 100\n");
        yaml.append("    condition: \"true\"\n");
        yaml.append("    action: \"REJECT\"\n");
        yaml.append("    message: \"Loan rejected based on default criteria\"\n");
        yaml.append("    description: \"Default rule that rejects all loans that don't meet other criteria\"\n\n");
        
        return yaml.toString();
    }
    
    /**
     * Generates the discount rules YAML content.
     */
    private String generateDiscountRulesYaml() {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# ============================================================================\n");
        yaml.append("# APEX Rule Configuration Bootstrap Demo - Discount Rules\n");
        yaml.append("# ============================================================================\n");
        yaml.append("# Generated: ").append(LocalDate.now()).append("\n");
        yaml.append("# Purpose: Demonstrates data-driven order discount business rules\n");
        yaml.append("# ============================================================================\n\n");
        
        yaml.append("metadata:\n");
        yaml.append("  name: \"Order Discount Rules\"\n");
        yaml.append("  version: \"1.0.0\"\n");
        yaml.append("  description: \"Business rules for automated order discount calculations\"\n");
        yaml.append("  category: \"order-discount\"\n");
        yaml.append("  created-date: \"").append(LocalDate.now()).append("\"\n\n");
        
        yaml.append("# Order discount business rules\n");
        yaml.append("rules:\n");
        yaml.append("  - id: \"OD001\"\n");
        yaml.append("    name: \"large-order-discount\"\n");
        yaml.append("    category: \"order-discount\"\n");
        yaml.append("    priority: 10\n");
        yaml.append("    condition: \"#orderTotal > 1000.0\"\n");
        yaml.append("    action: \"APPLY_DISCOUNT\"\n");
        yaml.append("    discount_percentage: 15\n");
        yaml.append("    message: \"15% discount applied for large order\"\n");
        yaml.append("    description: \"Provides a 15% discount for orders exceeding $1000\"\n\n");
        
        yaml.append("  - id: \"OD002\"\n");
        yaml.append("    name: \"loyalty-discount\"\n");
        yaml.append("    category: \"order-discount\"\n");
        yaml.append("    priority: 20\n");
        yaml.append("    condition: \"#customerYears > 5\"\n");
        yaml.append("    action: \"APPLY_DISCOUNT\"\n");
        yaml.append("    discount_percentage: 10\n");
        yaml.append("    message: \"10% discount applied for customer loyalty\"\n");
        yaml.append("    description: \"Rewards loyal customers who have been with us for more than 5 years\"\n\n");
        
        yaml.append("  - id: \"OD003\"\n");
        yaml.append("    name: \"new-customer-discount\"\n");
        yaml.append("    category: \"order-discount\"\n");
        yaml.append("    priority: 30\n");
        yaml.append("    condition: \"#customerYears == 0\"\n");
        yaml.append("    action: \"APPLY_DISCOUNT\"\n");
        yaml.append("    discount_percentage: 5\n");
        yaml.append("    message: \"5% discount applied for new customers\"\n");
        yaml.append("    description: \"Welcomes new customers with a 5% discount on their first order\"\n\n");
        
        yaml.append("  - id: \"OD004\"\n");
        yaml.append("    name: \"no-discount\"\n");
        yaml.append("    category: \"order-discount\"\n");
        yaml.append("    priority: 100\n");
        yaml.append("    condition: \"true\"\n");
        yaml.append("    action: \"NO_DISCOUNT\"\n");
        yaml.append("    discount_percentage: 0\n");
        yaml.append("    message: \"No discount applied\"\n");
        yaml.append("    description: \"Default rule when no other discount rules apply\"\n\n");
        
        return yaml.toString();
    }
    
    /**
     * Generates the combined rules YAML content.
     */
    private String generateCombinedRulesYaml() {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# ============================================================================\n");
        yaml.append("# APEX Rule Configuration Bootstrap Demo - Combined Rules\n");
        yaml.append("# ============================================================================\n");
        yaml.append("# Generated: ").append(LocalDate.now()).append("\n");
        yaml.append("# Purpose: Demonstrates complex rule combinations and patterns\n");
        yaml.append("# ============================================================================\n\n");
        
        yaml.append("metadata:\n");
        yaml.append("  name: \"Combined Rules Demonstration\"\n");
        yaml.append("  version: \"1.0.0\"\n");
        yaml.append("  description: \"Complex rule combinations using AND/OR operators\"\n");
        yaml.append("  category: \"combined-rules\"\n");
        yaml.append("  created-date: \"").append(LocalDate.now()).append("\"\n\n");
        
        yaml.append("# Individual component rules\n");
        yaml.append("rules:\n");
        yaml.append("  - id: \"CR001\"\n");
        yaml.append("    name: \"high-value-order\"\n");
        yaml.append("    category: \"combined-rules\"\n");
        yaml.append("    priority: 10\n");
        yaml.append("    condition: \"#orderTotal > 500\"\n");
        yaml.append("    action: \"FLAG_HIGH_VALUE\"\n");
        yaml.append("    message: \"High-value order detected\"\n");
        yaml.append("    description: \"Identifies orders with a total value exceeding $500\"\n\n");
        
        yaml.append("  - id: \"CR002\"\n");
        yaml.append("    name: \"loyal-customer\"\n");
        yaml.append("    category: \"combined-rules\"\n");
        yaml.append("    priority: 20\n");
        yaml.append("    condition: \"#customerYears > 3\"\n");
        yaml.append("    action: \"FLAG_LOYAL\"\n");
        yaml.append("    message: \"Loyal customer detected\"\n");
        yaml.append("    description: \"Identifies customers who have been with us for more than 3 years\"\n\n");
        
        yaml.append("  - id: \"CR003\"\n");
        yaml.append("    name: \"large-quantity\"\n");
        yaml.append("    category: \"combined-rules\"\n");
        yaml.append("    priority: 30\n");
        yaml.append("    condition: \"#quantity > 10\"\n");
        yaml.append("    action: \"FLAG_LARGE_QUANTITY\"\n");
        yaml.append("    message: \"Large quantity order detected\"\n");
        yaml.append("    description: \"Identifies orders with more than 10 items\"\n\n");
        
        yaml.append("# Combined rules using rule references\n");
        yaml.append("rule_combinations:\n");
        yaml.append("  - id: \"CR004\"\n");
        yaml.append("    name: \"premium-loyal-customer\"\n");
        yaml.append("    category: \"combined-rules\"\n");
        yaml.append("    priority: 5\n");
        yaml.append("    operator: \"AND\"\n");
        yaml.append("    rule_refs: [\"CR001\", \"CR002\"]\n");
        yaml.append("    action: \"PREMIUM_HANDLING\"\n");
        yaml.append("    message: \"Premium loyal customer detected\"\n");
        yaml.append("    description: \"Identifies high-value orders from loyal customers\"\n\n");
        
        yaml.append("  - id: \"CR005\"\n");
        yaml.append("    name: \"special-handling-required\"\n");
        yaml.append("    category: \"combined-rules\"\n");
        yaml.append("    priority: 15\n");
        yaml.append("    operator: \"OR\"\n");
        yaml.append("    rule_refs: [\"CR001\", \"CR003\"]\n");
        yaml.append("    action: \"SPECIAL_HANDLING\"\n");
        yaml.append("    message: \"Special handling required\"\n");
        yaml.append("    description: \"Identifies orders that require special handling due to high value or large quantity\"\n\n");
        
        return yaml.toString();
    }
    
    /**
     * Generates the main bootstrap configuration YAML content.
     */
    private String generateMainBootstrapConfigYaml() {
        StringBuilder yaml = new StringBuilder();
        
        yaml.append("# ============================================================================\n");
        yaml.append("# APEX Rule Configuration Bootstrap Demo - Main Configuration\n");
        yaml.append("# ============================================================================\n");
        yaml.append("# Generated: ").append(LocalDate.now()).append("\n");
        yaml.append("# Purpose: Main configuration file for rule configuration bootstrap demo\n");
        yaml.append("# ============================================================================\n\n");
        
        yaml.append("metadata:\n");
        yaml.append("  name: \"Rule Configuration Bootstrap Demo\"\n");
        yaml.append("  version: \"1.0.0\"\n");
        yaml.append("  description: \"Comprehensive demonstration of APEX rule configuration capabilities\"\n");
        yaml.append("  created-date: \"").append(LocalDate.now()).append("\"\n");
        yaml.append("  author: \"APEX Bootstrap Demo Generator\"\n\n");
        
        yaml.append("# Data source configurations\n");
        yaml.append("data_sources:\n");
        yaml.append("  database:\n");
        yaml.append("    type: \"postgresql\"\n");
        yaml.append("    url: \"jdbc:postgresql://localhost:5432/apex_rule_config_demo\"\n");
        yaml.append("    tables:\n");
        yaml.append("      - \"loan_applications\"\n");
        yaml.append("      - \"customer_profiles\"\n");
        yaml.append("      - \"order_processing\"\n");
        yaml.append("      - \"rule_execution_audit\"\n\n");
        
        yaml.append("  external_files:\n");
        yaml.append("    - \"bootstrap/datasets/loan-approval-rules.yaml\"\n");
        yaml.append("    - \"bootstrap/datasets/discount-rules.yaml\"\n");
        yaml.append("    - \"bootstrap/datasets/combined-rules.yaml\"\n\n");
        
        yaml.append("# Inline reference datasets\n");
        yaml.append("datasets:\n");
        yaml.append("  membership_levels:\n");
        yaml.append("    - code: \"Basic\"\n");
        yaml.append("      name: \"Basic Membership\"\n");
        yaml.append("      discount_multiplier: 1.0\n");
        yaml.append("    - code: \"Silver\"\n");
        yaml.append("      name: \"Silver Membership\"\n");
        yaml.append("      discount_multiplier: 1.2\n");
        yaml.append("    - code: \"Gold\"\n");
        yaml.append("      name: \"Gold Membership\"\n");
        yaml.append("      discount_multiplier: 1.5\n");
        yaml.append("    - code: \"Platinum\"\n");
        yaml.append("      name: \"Platinum Membership\"\n");
        yaml.append("      discount_multiplier: 2.0\n\n");
        
        yaml.append("  loan_purposes:\n");
        yaml.append("    - code: \"HOME_PURCHASE\"\n");
        yaml.append("      name: \"Home Purchase\"\n");
        yaml.append("      risk_factor: 0.8\n");
        yaml.append("    - code: \"AUTO_LOAN\"\n");
        yaml.append("      name: \"Auto Loan\"\n");
        yaml.append("      risk_factor: 0.9\n");
        yaml.append("    - code: \"PERSONAL\"\n");
        yaml.append("      name: \"Personal Loan\"\n");
        yaml.append("      risk_factor: 1.2\n");
        yaml.append("    - code: \"DEBT_CONSOLIDATION\"\n");
        yaml.append("      name: \"Debt Consolidation\"\n");
        yaml.append("      risk_factor: 1.1\n\n");
        
        yaml.append("# Demo scenarios\n");
        yaml.append("scenarios:\n");
        yaml.append("  - name: \"loan_approval_demo\"\n");
        yaml.append("    description: \"Demonstrates loan approval rule processing\"\n");
        yaml.append("    rule_files: [\"bootstrap/datasets/loan-approval-rules.yaml\"]\n");
        yaml.append("    data_source: \"database.loan_applications\"\n\n");
        
        yaml.append("  - name: \"discount_calculation_demo\"\n");
        yaml.append("    description: \"Demonstrates order discount rule processing\"\n");
        yaml.append("    rule_files: [\"bootstrap/datasets/discount-rules.yaml\"]\n");
        yaml.append("    data_source: \"database.order_processing\"\n\n");
        
        yaml.append("  - name: \"combined_rules_demo\"\n");
        yaml.append("    description: \"Demonstrates complex rule combinations\"\n");
        yaml.append("    rule_files: [\"bootstrap/datasets/combined-rules.yaml\"]\n");
        yaml.append("    data_source: \"database.order_processing\"\n\n");
        
        return yaml.toString();
    }
    
    /**
     * Verifies that all external configuration files exist and are readable.
     */
    public boolean verifyExternalFiles() {
        logger.info("Verifying external rule configuration files...");
        
        try {
            String[] requiredFiles = {
                "src/main/resources/bootstrap/rule-configuration-bootstrap.yaml",
                "src/main/resources/bootstrap/datasets/loan-approval-rules.yaml",
                "src/main/resources/bootstrap/datasets/discount-rules.yaml",
                "src/main/resources/bootstrap/datasets/combined-rules.yaml"
            };
            
            for (String filePath : requiredFiles) {
                Path file = Paths.get(filePath);
                if (!Files.exists(file)) {
                    logger.error("Required file does not exist: {}", filePath);
                    return false;
                }
                
                if (!Files.isReadable(file)) {
                    logger.error("File is not readable: {}", filePath);
                    return false;
                }
                
                long fileSize = Files.size(file);
                logger.debug("Verified file: {} ({} bytes)", filePath, fileSize);
            }
            
            logger.info("All external rule configuration files verified successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("External file verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Cleans up external configuration files.
     */
    public void cleanup() {
        logger.info("Cleaning up external rule configuration files...");
        
        try {
            String[] filesToCleanup = {
                "src/main/resources/bootstrap/rule-configuration-bootstrap.yaml",
                "src/main/resources/bootstrap/datasets/loan-approval-rules.yaml",
                "src/main/resources/bootstrap/datasets/discount-rules.yaml",
                "src/main/resources/bootstrap/datasets/combined-rules.yaml"
            };
            
            for (String filePath : filesToCleanup) {
                Path file = Paths.get(filePath);
                if (Files.exists(file)) {
                    Files.delete(file);
                    logger.debug("Deleted file: {}", filePath);
                }
            }
            
            // Clean up the datasets directory if empty
            Path datasetsDir = Paths.get("src/main/resources/bootstrap/datasets");
            if (Files.exists(datasetsDir) && Files.list(datasetsDir).findAny().isEmpty()) {
                Files.delete(datasetsDir);
                logger.debug("Deleted empty datasets directory");
            }
            
        } catch (Exception e) {
            logger.warn("External file cleanup failed: {}", e.getMessage());
        }
    }
}
