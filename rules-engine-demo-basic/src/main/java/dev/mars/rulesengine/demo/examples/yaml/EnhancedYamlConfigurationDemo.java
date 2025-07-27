package dev.mars.rulesengine.demo.examples.yaml;

import dev.mars.rulesengine.core.config.yaml.YamlConfigurationException;
import dev.mars.rulesengine.core.config.yaml.YamlRulesEngineService;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.metadata.RuleMetadata;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive demonstration of the enhanced YAML configuration system.
 * 
 * This demo showcases:
 * - Generic category support (no hardcoded business domains)
 * - Enterprise metadata integration
 * - Comprehensive audit trails
 * - Multi-domain rule management
 * - Enhanced validation and error handling
 */
public class EnhancedYamlConfigurationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== ENHANCED YAML CONFIGURATION DEMO ===");
        System.out.println("Showcasing generic architecture with enterprise metadata support\n");
        
        EnhancedYamlConfigurationDemo demo = new EnhancedYamlConfigurationDemo();
        
        try {
            // Step 1: Load enhanced configuration
            demo.demonstrateEnhancedConfigurationLoading();
            
            // Step 2: Show enterprise metadata
            demo.demonstrateEnterpriseMetadata();
            
            // Step 3: Execute rules across domains
            demo.demonstrateMultiDomainExecution();
            
            // Step 4: Show audit trail capabilities
            demo.demonstrateAuditTrails();
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== ENHANCED YAML CONFIGURATION BENEFITS ===");
        System.out.println("✓ Generic Categories: No hardcoded business domains");
        System.out.println("✓ Enterprise Metadata: Full audit trail and governance support");
        System.out.println("✓ Multi-Domain Support: Healthcare, Manufacturing, Data Governance, Risk Management");
        System.out.println("✓ Enhanced Validation: Comprehensive parameter and rule validation");
        System.out.println("✓ Audit Ready: Complete governance and compliance support");
        System.out.println("✓ Backward Compatible: Existing YAML files still work");
        
        System.out.println("\n=== DEMO COMPLETED ===");
    }
    
    /**
     * Demonstrate loading the enhanced YAML configuration with generic architecture.
     */
    private void demonstrateEnhancedConfigurationLoading() throws YamlConfigurationException {
        System.out.println("=== STEP 1: ENHANCED CONFIGURATION LOADING ===");
        System.out.println("Loading enterprise rules with generic categories and metadata\n");
        
        YamlRulesEngineService service = new YamlRulesEngineService();
        
        // Load the enhanced configuration
        String configPath = "config/enhanced-enterprise-rules.yaml";
        RulesEngine engine = service.createRulesEngineWithGenericArchitecture(configPath);
        
        List<Rule> allRules = engine.getConfiguration().getAllRules();
        
        System.out.println("✓ Successfully loaded enhanced configuration");
        System.out.println("  Total rules loaded: " + allRules.size());
        
        // Group rules by category to show generic support
        Map<String, Long> rulesByCategory = allRules.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                rule -> rule.getCategories().iterator().next().getName(),
                java.util.stream.Collectors.counting()
            ));
        
        System.out.println("  Rules by user-defined category:");
        rulesByCategory.forEach((category, count) -> 
            System.out.println("    • " + category + ": " + count + " rules"));
        
        System.out.println("\nKey Insight: Categories are completely user-defined - no hardcoded domains!\n");
    }
    
    /**
     * Demonstrate comprehensive enterprise metadata support.
     */
    private void demonstrateEnterpriseMetadata() throws YamlConfigurationException {
        System.out.println("=== STEP 2: ENTERPRISE METADATA DEMONSTRATION ===");
        System.out.println("Showcasing comprehensive audit trails and governance metadata\n");
        
        YamlRulesEngineService service = new YamlRulesEngineService();
        RulesEngine engine = service.createRulesEngineWithGenericArchitecture("config/enhanced-enterprise-rules.yaml");
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        
        // Show detailed metadata for a few representative rules
        System.out.println("Enterprise Metadata Examples:");
        
        for (int i = 0; i < Math.min(3, rules.size()); i++) {
            Rule rule = rules.get(i);
            RuleMetadata metadata = rule.getMetadata();
            
            System.out.println("\n  Rule: " + rule.getName());
            System.out.println("    ID: " + rule.getId());
            System.out.println("    Category: " + rule.getCategories().iterator().next().getName());
            System.out.println("    Created: " + rule.getCreatedDate());
            System.out.println("    Modified: " + rule.getModifiedDate());
            System.out.println("    Created By: " + metadata.getCreatedByUser());
            System.out.println("    Business Domain: " + metadata.getBusinessDomain().orElse("Not specified"));
            System.out.println("    Business Owner: " + metadata.getBusinessOwner().orElse("Not specified"));
            System.out.println("    Source System: " + metadata.getSourceSystem().orElse("Not specified"));
            
            if (metadata.getEffectiveDate().isPresent()) {
                System.out.println("    Effective Date: " + metadata.getEffectiveDate().get());
            }
            if (metadata.getExpirationDate().isPresent()) {
                System.out.println("    Expiration Date: " + metadata.getExpirationDate().get());
            }
            
            // Show custom properties
            if (!metadata.getCustomProperties().isEmpty()) {
                System.out.println("    Custom Properties:");
                metadata.getCustomProperties().forEach((key, value) -> 
                    System.out.println("      • " + key + ": " + value));
            }
        }
        
        System.out.println("\nKey Insight: Every rule has complete enterprise metadata and audit trail!\n");
    }
    
    /**
     * Demonstrate rule execution across multiple domains.
     */
    private void demonstrateMultiDomainExecution() throws YamlConfigurationException {
        System.out.println("=== STEP 3: MULTI-DOMAIN RULE EXECUTION ===");
        System.out.println("Executing rules across Healthcare, Manufacturing, Data Governance, and Risk Management\n");
        
        YamlRulesEngineService service = new YamlRulesEngineService();
        RulesEngine engine = service.createRulesEngineWithGenericArchitecture("config/enhanced-enterprise-rules.yaml");
        
        // Healthcare scenario
        System.out.println("Healthcare Scenario - Patient Eligibility:");
        Map<String, Object> patientData = Map.of(
            "patientAge", 45,
            "hasInsurance", true,
            "insuranceActive", true
        );
        
        executeRulesForCategory(engine, "patient-eligibility", patientData);
        
        // Manufacturing scenario
        System.out.println("\nManufacturing Scenario - Quality Control:");
        Map<String, Object> manufacturingData = Map.of(
            "temperature", 22.5,
            "pressure", 85
        );
        
        executeRulesForCategory(engine, "quality-control", manufacturingData);
        
        // Data Governance scenario
        System.out.println("\nData Governance Scenario - Compliance Check:");
        Map<String, Object> dataGovernanceData = Map.of(
            "containsPII", true,
            "encrypted", true,
            "dataAge", 1000  // days
        );
        
        executeRulesForCategory(engine, "data-governance", dataGovernanceData);
        
        // Risk Management scenario
        System.out.println("\nRisk Management Scenario - Risk Assessment:");
        Map<String, Object> riskData = Map.of(
            "creditScore", 720,
            "debtToIncome", 0.35,
            "operationalRiskScore", 65
        );
        
        executeRulesForCategory(engine, "risk-management", riskData);
        
        System.out.println("\nKey Insight: Single engine handles multiple business domains seamlessly!\n");
    }
    
    /**
     * Execute rules for a specific category and display results.
     */
    private void executeRulesForCategory(RulesEngine engine, String categoryName, Map<String, Object> facts) {
        List<Rule> allRules = engine.getConfiguration().getAllRules();
        
        // Filter rules by category
        List<Rule> categoryRules = allRules.stream()
            .filter(rule -> rule.getCategories().stream()
                .anyMatch(cat -> cat.getName().equals(categoryName)))
            .toList();
        
        System.out.println("  Executing " + categoryRules.size() + " rules for category: " + categoryName);
        
        for (Rule rule : categoryRules) {
            RuleResult result = engine.executeRule(rule, facts);
            String status = result.isTriggered() ? "✓ PASSED" : "✗ FAILED";
            System.out.println("    " + status + " - " + result.getRuleName());
            System.out.println("      Message: " + result.getMessage());
            if (result.hasPerformanceMetrics()) {
                System.out.println("      Execution Time: " + 
                    result.getPerformanceMetrics().getEvaluationTimeMillis() + "ms");
            }
        }
    }
    
    /**
     * Demonstrate comprehensive audit trail capabilities.
     */
    private void demonstrateAuditTrails() throws YamlConfigurationException {
        System.out.println("=== STEP 4: AUDIT TRAIL DEMONSTRATION ===");
        System.out.println("Showcasing comprehensive audit and governance capabilities\n");
        
        YamlRulesEngineService service = new YamlRulesEngineService();
        RulesEngine engine = service.createRulesEngineWithGenericArchitecture("config/enhanced-enterprise-rules.yaml");
        
        List<Rule> rules = engine.getConfiguration().getAllRules();
        
        // Audit trail summary
        System.out.println("Audit Trail Summary:");
        System.out.println("  Total rules with audit trails: " + rules.size());
        
        // Group by business domain
        Map<String, Long> rulesByDomain = rules.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                rule -> rule.getMetadata().getBusinessDomain().orElse("Unknown"),
                java.util.stream.Collectors.counting()
            ));
        
        System.out.println("  Rules by business domain:");
        rulesByDomain.forEach((domain, count) -> 
            System.out.println("    • " + domain + ": " + count + " rules"));
        
        // Group by business owner
        Map<String, Long> rulesByOwner = rules.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                rule -> rule.getMetadata().getBusinessOwner().orElse("Unknown"),
                java.util.stream.Collectors.counting()
            ));
        
        System.out.println("  Rules by business owner:");
        rulesByOwner.forEach((owner, count) -> 
            System.out.println("    • " + owner + ": " + count + " rules"));
        
        // Group by source system
        Map<String, Long> rulesBySystem = rules.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                rule -> rule.getMetadata().getSourceSystem().orElse("Unknown"),
                java.util.stream.Collectors.counting()
            ));
        
        System.out.println("  Rules by source system:");
        rulesBySystem.forEach((system, count) -> 
            System.out.println("    • " + system + ": " + count + " rules"));
        
        // Show rules with expiration dates (governance)
        long rulesWithExpiration = rules.stream()
            .mapToLong(rule -> rule.getMetadata().getExpirationDate().isPresent() ? 1 : 0)
            .sum();
        
        System.out.println("  Rules with expiration dates: " + rulesWithExpiration + 
                          " (governance and lifecycle management)");
        
        // Show rules with custom properties
        long rulesWithCustomProps = rules.stream()
            .mapToLong(rule -> rule.getMetadata().getCustomProperties().isEmpty() ? 0 : 1)
            .sum();
        
        System.out.println("  Rules with custom properties: " + rulesWithCustomProps + 
                          " (domain-specific metadata)");
        
        System.out.println("\nKey Insight: Complete audit trail enables enterprise governance and compliance!\n");
    }
}
