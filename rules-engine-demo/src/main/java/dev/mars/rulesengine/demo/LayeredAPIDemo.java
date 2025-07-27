package dev.mars.rulesengine.demo;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.api.ValidationResult;
import dev.mars.rulesengine.core.config.yaml.YamlConfigurationLoader;
import dev.mars.rulesengine.core.config.yaml.YamlRuleConfiguration;
import dev.mars.rulesengine.core.config.yaml.YamlRulesEngineService;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.demo.model.Customer;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Layered API Demo - Demonstrates the three-layer API design philosophy.
 * 
 * This demo showcases:
 * - Layer 1: Ultra-Simple API (90% of use cases) - One-liner evaluations
 * - Layer 2: Template-Based Rules (8% of use cases) - Structured validation
 * - Layer 3: Advanced Configuration (2% of use cases) - Full YAML power
 * 
 * Each layer builds upon the previous, providing more power and flexibility
 * while maintaining simplicity for common use cases.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class LayeredAPIDemo {
    
    private final RulesService rulesService;
    
    public LayeredAPIDemo() {
        this.rulesService = new RulesService();
    }
    
    /**
     * Run the complete Layered API demonstration.
     */
    public void run() {
        System.out.println("🏗️  SpEL Rules Engine - Three-Layer API Design");
        System.out.println("=" .repeat(60));
        System.out.println("Progressive complexity: Simple → Structured → Advanced");
        System.out.println();
        
        demonstrateLayer1UltraSimpleAPI();
        System.out.println();
        
        demonstrateLayer2TemplateBasedRules();
        System.out.println();
        
        demonstrateLayer3AdvancedConfiguration();
        System.out.println();
        
        demonstrateAPIProgression();
        System.out.println();
        
        System.out.println("✅ Layered API demonstration completed!");
        System.out.println("   Each layer provides the right level of abstraction for different needs.");
    }
    
    /**
     * Layer 1: Ultra-Simple API (90% of use cases)
     * One-liner evaluations for immediate decisions.
     */
    private void demonstrateLayer1UltraSimpleAPI() {
        System.out.println("📋 Layer 1: ULTRA-SIMPLE API (90% of use cases)");
        System.out.println("-".repeat(50));
        System.out.println("Perfect for: Quick validations, immediate decisions, prototyping");
        System.out.println();
        
        // Business scenario: Customer eligibility check
        Customer customer = createSampleCustomer();
        
        System.out.println("🎯 Scenario: Customer Eligibility Check");
        System.out.println("Customer: " + customer.getName() + ", age " + customer.getAge());
        System.out.println();
        
        // One-liner validations
        System.out.println("// One-liner validations");
        
        boolean isAdult = rulesService.check("#data.age >= 18", customer);
        System.out.println("boolean isAdult = Rules.check(\"#data.age >= 18\", customer);");
        System.out.println("Result: " + isAdult + " " + (isAdult ? "✅" : "❌"));
        System.out.println();
        
        boolean hasEmail = rulesService.check("#data.email != null", customer);
        System.out.println("boolean hasEmail = Rules.check(\"#data.email != null\", customer);");
        System.out.println("Result: " + hasEmail + " " + (hasEmail ? "✅" : "❌"));
        System.out.println();
        
        boolean isEligible = rulesService.check("#data.age >= 18 && #data.email != null", customer);
        System.out.println("boolean isEligible = Rules.check(\"#data.age >= 18 && #data.email != null\", customer);");
        System.out.println("Result: " + isEligible + " " + (isEligible ? "✅" : "❌"));
        
        System.out.println("\n💡 Layer 1 Benefits:");
        System.out.println("   • Immediate results with minimal code");
        System.out.println("   • No configuration files needed");
        System.out.println("   • Perfect for simple business rules");
        System.out.println("   • Covers 90% of common validation scenarios");
    }
    
    /**
     * Layer 2: Template-Based Rules (8% of use cases)
     * Structured validation with detailed error reporting.
     */
    private void demonstrateLayer2TemplateBasedRules() {
        System.out.println("📋 Layer 2: TEMPLATE-BASED RULES (8% of use cases)");
        System.out.println("-".repeat(50));
        System.out.println("Perfect for: Complex validations, detailed error reporting, reusable patterns");
        System.out.println();
        
        Customer customer = createSampleCustomer();
        
        System.out.println("🎯 Scenario: Comprehensive Customer Validation");
        System.out.println("Customer: " + customer.getName() + ", age " + customer.getAge());
        System.out.println();
        
        System.out.println("// Structured validation with detailed results");
        System.out.println("ValidationResult result = rulesService.validate(customer)");
        System.out.println("    .minimumAge(18)");
        System.out.println("    .emailRequired()");
        System.out.println("    .notNull(\"name\")");
        System.out.println("    .validate();");
        System.out.println();
        
        ValidationResult result = rulesService.validate(customer)
            .minimumAge(18)
            .emailRequired()
            .notNull("name")
            .validate();
        
        System.out.println("Validation Results:");
        System.out.println("  Overall Status: " + (result.isValid() ? "✅ VALID" : "❌ INVALID"));
        System.out.println("  Error Count: " + result.getErrorCount());
        
        if (!result.isValid()) {
            System.out.println("  Detailed Errors:");
            result.getErrors().forEach(error -> System.out.println("    • " + error));
        }
        
        // Demonstrate fluent chaining with custom conditions
        System.out.println("\n// Custom validation conditions");
        ValidationResult customResult = rulesService.validate(customer)
            .that("#data.age >= 21", "Must be 21+ for premium features")
            .that("#data.email.contains('@')", "Email must contain @ symbol")
            .validate();
        
        System.out.println("Custom Validation:");
        System.out.println("  Status: " + (customResult.isValid() ? "✅ VALID" : "❌ INVALID"));
        if (!customResult.isValid()) {
            customResult.getErrors().forEach(error -> System.out.println("    • " + error));
        }
        
        System.out.println("\n💡 Layer 2 Benefits:");
        System.out.println("   • Detailed error messages for user feedback");
        System.out.println("   • Fluent, readable validation chains");
        System.out.println("   • Built-in helpers for common patterns");
        System.out.println("   • Handles complex multi-field validations");
    }
    
    /**
     * Layer 3: Advanced Configuration (2% of use cases)
     * Full YAML configuration with metadata, versioning, and complex rules.
     */
    private void demonstrateLayer3AdvancedConfiguration() {
        System.out.println("📋 Layer 3: ADVANCED CONFIGURATION (2% of use cases)");
        System.out.println("-".repeat(50));
        System.out.println("Perfect for: Enterprise rules, external management, complex business logic");
        System.out.println();
        
        try {
            System.out.println("🎯 Scenario: Enterprise Rule Management");
            System.out.println("// Load comprehensive rule configuration from YAML");
            System.out.println("YamlConfigurationLoader loader = new YamlConfigurationLoader();");
            System.out.println("YamlRuleConfiguration config = loader.loadFromStream(yamlStream);");
            System.out.println("RulesEngine engine = YamlRulesEngineService.createFromConfiguration(config);");
            System.out.println();
            
            // Load advanced configuration
            InputStream yamlStream = getClass().getResourceAsStream("/demo-rules/quick-start.yaml");
            if (yamlStream != null) {
                YamlConfigurationLoader loader = new YamlConfigurationLoader();
                YamlRuleConfiguration config = loader.loadFromStream(yamlStream);
                YamlRulesEngineService service = new YamlRulesEngineService();
                RulesEngine engine = service.createRulesEngineFromYamlConfig(config);
                
                System.out.println("Loaded Enterprise Configuration:");
                if (config.getMetadata() != null) {
                    System.out.println("  📋 Name: " + config.getMetadata().getName());
                    System.out.println("  🏷️  Version: " + config.getMetadata().getVersion());
                    System.out.println("  👤 Created By: " + config.getMetadata().getAuthor());
                    System.out.println("  📅 Created: " + config.getMetadata().getCreated());
                }
                System.out.println("  📏 Rules: " + (config.getRules() != null ? config.getRules().size() : 0));
                System.out.println("  🔄 Enrichments: " + (config.getEnrichments() != null ? config.getEnrichments().size() : 0));
                
                // Test with enterprise rules
                Customer customer = createSampleCustomer();
                Map<String, Object> facts = new HashMap<>();
                facts.put("data", customer);
                
                System.out.println("\n// Execute enterprise rules");
                System.out.println("Customer: " + customer.getName() + ", age " + customer.getAge());
                
                // Create a simple rule for demonstration
                Rule ageRule = new Rule("enterprise-age-check", 
                                      "#data.age >= 18", 
                                      "Enterprise age validation passed");
                
                RuleResult ruleResult = engine.executeRule(ageRule, facts);
                System.out.println("Enterprise Rule Result: " + (ruleResult.isTriggered() ? "✅ PASSED" : "❌ FAILED"));
                if (ruleResult.isTriggered()) {
                    System.out.println("  Message: " + ruleResult.getMessage());
                }
                
            } else {
                System.out.println("⚠️  Advanced configuration file not found");
            }
            
        } catch (Exception e) {
            System.out.println("⚠️  Error with advanced configuration: " + e.getMessage());
        }
        
        System.out.println("\n💡 Layer 3 Benefits:");
        System.out.println("   • External rule management without code changes");
        System.out.println("   • Full metadata and versioning support");
        System.out.println("   • Complex rule hierarchies and dependencies");
        System.out.println("   • Enterprise governance and audit trails");
        System.out.println("   • Dataset enrichment and lookup capabilities");
    }
    
    /**
     * Demonstrate how the APIs work together and when to use each layer.
     */
    private void demonstrateAPIProgression() {
        System.out.println("🔄 API PROGRESSION: When to Use Each Layer");
        System.out.println("-".repeat(50));
        
        Customer customer = createSampleCustomer();
        
        System.out.println("Same business rule implemented at each layer:");
        System.out.println("Business Rule: 'Customer must be adult with valid email'");
        System.out.println();
        
        // Layer 1 approach
        System.out.println("🥉 Layer 1 (Ultra-Simple):");
        System.out.println("   boolean valid = Rules.check(\"#data.age >= 18 && #data.email != null\", customer);");
        boolean layer1Result = rulesService.check("#data.age >= 18 && #data.email != null", customer);
        System.out.println("   Result: " + layer1Result + " " + (layer1Result ? "✅" : "❌"));
        System.out.println("   Use when: Quick validation, prototyping, simple rules");
        System.out.println();
        
        // Layer 2 approach
        System.out.println("🥈 Layer 2 (Template-Based):");
        System.out.println("   ValidationResult result = rulesService.validate(customer)");
        System.out.println("       .minimumAge(18).emailRequired().validate();");
        ValidationResult layer2Result = rulesService.validate(customer)
            .minimumAge(18)
            .emailRequired()
            .validate();
        System.out.println("   Result: " + layer2Result.isValid() + " " + (layer2Result.isValid() ? "✅" : "❌"));
        System.out.println("   Use when: Need detailed errors, complex validations, user feedback");
        System.out.println();
        
        // Layer 3 approach
        System.out.println("🥇 Layer 3 (Advanced Configuration):");
        System.out.println("   RulesEngine engine = YamlRulesEngineService.loadFromFile(\"rules.yaml\");");
        System.out.println("   RuleResult result = engine.executeRules(rules, facts);");
        System.out.println("   Result: ✅ (Configured externally)");
        System.out.println("   Use when: Enterprise rules, external management, complex business logic");
        
        System.out.println("\n🎯 Recommendation:");
        System.out.println("   • Start with Layer 1 for prototyping");
        System.out.println("   • Move to Layer 2 when you need detailed validation");
        System.out.println("   • Use Layer 3 for enterprise-grade rule management");
    }
    
    /**
     * Create a sample customer for demonstrations.
     */
    private Customer createSampleCustomer() {
        Customer customer = new Customer();
        customer.setName("Alice Johnson");
        customer.setAge(28);
        customer.setEmail("alice.johnson@example.com");
        return customer;
    }
    
    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        new LayeredAPIDemo().run();
    }
}
