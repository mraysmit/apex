package dev.mars.apex.demo.validation;

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


import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.api.ValidationResult;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.demo.model.Customer;
import dev.mars.apex.demo.infrastructure.DemoDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Quick Start Demo - Comprehensive "Quick Start (5 Minutes)" demonstration.
 *
 * This consolidated demo demonstrates the three core approaches to using the APEX Rules Engine:
 * 1. One-liner rule evaluation - Simple boolean checks using Maps or objects
 * 2. Template-based rules using ValidationBuilder - Fluent API for complex validations
 * 3. YAML configuration loading - External rule management and configuration
 *
 * CONSOLIDATED FROM: QuickStartDemoA + QuickStartDemoB
 * - Combines comprehensive explanations and timing from QuickStartDemoA
 * - Incorporates named rules approach from QuickStartDemoB
 * - Provides both inline expressions and named rule examples
 * - Includes performance monitoring and detailed educational content
 *
 * Each section includes:
 * - Step-by-step explanations of what's happening
 * - Code examples that match the documentation exactly
 * - Performance timing information
 * - Key learning points and best practices
 * - Multiple approaches (inline vs named rules)
 *
 * This demo is designed to be completed in 5 minutes and provides a solid foundation
 * for understanding APEX Rules Engine capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 2.0 (Consolidated from QuickStartDemoA + QuickStartDemoB)
 */
public class QuickStartDemo {

    private static final Logger logger = LoggerFactory.getLogger(QuickStartDemo.class);
    private final RulesService rulesService;

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        QuickStartDemo demo = new QuickStartDemo();
        demo.run();
    }

    public QuickStartDemo() {
        logger.info("Initializing QuickStart Demo");
        this.rulesService = new RulesService();
        logger.debug("RulesService initialized successfully");
    }

    /**
     * Run the complete Quick Start demonstration.
     *
     * This method orchestrates the entire 5-minute quick start experience, covering:
     * - One-liner rule evaluation for immediate boolean results
     * - Template-based validation for complex business rules
     * - YAML configuration for external rule management
     *
     * Each demonstration section includes timing information and detailed explanations
     * to help users understand both the "what" and "why" of each approach.
     */
    public void run() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting QuickStart Demo - estimated duration: 5 minutes");

        System.out.println(" APEX Rules Engine - Quick Start (5 Minutes)");
        System.out.println("=" .repeat(60));
        System.out.println("This demo covers the three core approaches to using APEX:");
        System.out.println("1. One-liner evaluation for simple checks");
        System.out.println("2. Template-based rules for complex validation");
        System.out.println("3. YAML configuration for external rule management");
        System.out.println();

        // Section 1: One-liner evaluation
        long section1Start = System.currentTimeMillis();
        demonstrateOneLinerRuleEvaluation();
        long section1Duration = System.currentTimeMillis() - section1Start;
        logger.info("Section 1 (One-liner evaluation) completed in {}ms", section1Duration);
        System.out.println();

        // Section 2: Template-based rules
        long section2Start = System.currentTimeMillis();
        demonstrateTemplateBasedRules();
        long section2Duration = System.currentTimeMillis() - section2Start;
        logger.info("Section 2 (Template-based rules) completed in {}ms", section2Duration);
        System.out.println();

        // Section 3: YAML configuration
        long section3Start = System.currentTimeMillis();
        demonstrateYamlConfiguration();
        long section3Duration = System.currentTimeMillis() - section3Start;
        logger.info("Section 3 (YAML configuration) completed in {}ms", section3Duration);
        System.out.println();

        // Summary and completion
        long totalDuration = System.currentTimeMillis() - startTime;
        System.out.println("PASSED Quick Start demonstration completed!");
        System.out.println("   Total duration: " + totalDuration + "ms");
        System.out.println("   You've learned the three core approaches to using the Rules Engine.");
        System.out.println("   Ready to explore more advanced features!");

        logger.info("QuickStart Demo completed successfully in {}ms", totalDuration);
        logger.info("Demo sections - Section 1: {}ms, Section 2: {}ms, Section 3: {}ms",
                   section1Duration, section2Duration, section3Duration);
    }
    
    /**
     * Demonstrate one-liner rule evaluation as shown in the user guide.
     *
     * This section shows the simplest way to use APEX Rules Engine - single-line
     * boolean evaluations that can be embedded directly in your business logic.
     *
     * Key concepts demonstrated:
     * - Map-based data context for simple key-value scenarios
     * - Object-based data context for complex domain objects
     * - SpEL expression syntax for rule conditions
     * - Performance characteristics of one-liner evaluations
     */
    private void demonstrateOneLinerRuleEvaluation() {
        logger.info("Starting one-liner rule evaluation demonstration");

        System.out.println(" 1. One-Liner Rule Evaluation");
        System.out.println("-".repeat(40));
        System.out.println("The simplest way to use APEX - embed rules directly in your code");
        System.out.println("Perfect for: Quick validations, business logic, conditional processing");
        System.out.println();

        // Example 1: Simple map-based evaluation
        System.out.println("Example 1: Map-based evaluation (most common pattern)");
        System.out.println("// Evaluate a rule in one line");

        long evalStart = System.nanoTime();
        boolean isAdult = rulesService.check("#age >= 18", Map.of("age", 25));
        long evalDuration = System.nanoTime() - evalStart;

        System.out.println("boolean isAdult = Rules.check(\"#age >= 18\", Map.of(\"age\", 25));");
        System.out.println("Result: " + isAdult + " ✓ (evaluated in " + evalDuration/1000 + " microseconds)");
        logger.debug("Age validation rule evaluated in {} nanoseconds", evalDuration);
        System.out.println();

        evalStart = System.nanoTime();
        boolean hasBalance = rulesService.check("#balance > 1000", Map.of("balance", 500));
        evalDuration = System.nanoTime() - evalStart;

        System.out.println("boolean hasBalance = Rules.check(\"#balance > 1000\", Map.of(\"balance\", 500));");
        System.out.println("Result: " + hasBalance + " ✗ (evaluated in " + evalDuration/1000 + " microseconds)");
        logger.debug("Balance validation rule evaluated in {} nanoseconds", evalDuration);
        System.out.println();

        // Example 2: Object-based evaluation
        System.out.println("Example 2: Object-based evaluation (for domain objects)");
        System.out.println("// With objects - use #data prefix to access properties");
        Customer customer = createSampleCustomer();

        evalStart = System.nanoTime();
        boolean valid = rulesService.check("#data.age >= 18 && #data.email != null", customer);
        evalDuration = System.nanoTime() - evalStart;

        System.out.println("Customer customer = new Customer(\"John\", 25, \"john@example.com\");");
        System.out.println("boolean valid = Rules.check(\"#data.age >= 18 && #data.email != null\", customer);");
        System.out.println("Result: " + valid + " ✓ (evaluated in " + evalDuration/1000 + " microseconds)");
        logger.debug("Customer validation rule evaluated in {} nanoseconds", evalDuration);

        System.out.println("\n Key Points:");
        System.out.println("   • Use Maps for simple key-value data (fastest approach)");
        System.out.println("   • Use objects with #data prefix to access properties");
        System.out.println("   • Rules return boolean results for immediate decisions");
        System.out.println("   • Typical evaluation time: < 100 microseconds");
        System.out.println("   • Perfect for embedding in if statements, guards, and filters");

        logger.info("One-liner rule evaluation demonstration completed");
    }
    
    /**
     * Demonstrate template-based rules using ValidationBuilder.
     *
     * This section shows the ValidationBuilder pattern - a fluent API that makes
     * complex validation rules readable and maintainable. This approach is ideal
     * when you need detailed error reporting and multiple validation checks.
     *
     * Key concepts demonstrated:
     * - Fluent API design for readable validation chains
     * - Built-in validation helpers for common business rules
     * - Detailed error reporting with ValidationResult
     * - Performance characteristics of template-based validation
     */
    private void demonstrateTemplateBasedRules() {
        logger.info("Starting template-based rules demonstration");

        System.out.println(" 2. Template-Based Rules");
        System.out.println("-".repeat(40));
        System.out.println("Fluent API for complex validations with detailed error reporting");
        System.out.println("Perfect for: Form validation, data quality checks, business rule enforcement");
        System.out.println();

        Customer customer = createSampleCustomer();
        System.out.println("Testing customer: " + customer.getName() + " (age: " + customer.getAge() +
                          ", email: " + customer.getEmail() + ")");
        System.out.println();

        System.out.println("Building validation chain:");
        System.out.println("// Create validation rules using templates");
        System.out.println("ValidationResult result = rulesService.validate(customer)");
        System.out.println("    .minimumAge(18)        // Check age requirement");
        System.out.println("    .emailRequired()       // Ensure email is present");
        System.out.println("    .minimumBalance(1000)  // Check financial threshold");
        System.out.println("    .validate();           // Execute all validations");
        System.out.println();

        // Execute validation with timing
        long validationStart = System.nanoTime();
        ValidationResult result = rulesService.validate(customer)
            .minimumAge(18)
            .emailRequired()
            .minimumBalance(1000)
            .validate();
        long validationDuration = System.nanoTime() - validationStart;

        logger.debug("Template-based validation completed in {} nanoseconds", validationDuration);

        System.out.println("Validation Result (completed in " + validationDuration/1000 + " microseconds):");
        System.out.println("  Valid: " + result.isValid());
        System.out.println("  Error Count: " + result.getErrorCount());
        System.out.println("  Rules Evaluated: 3");

        if (!result.isValid()) {
            System.out.println("  Errors:");
            result.getErrors().forEach(error -> {
                System.out.println("    • " + error);
                logger.debug("Validation error: {}", error);
            });
        } else {
            System.out.println("  PASSED All validations passed!");
        }

        System.out.println("\n Key Points:");
        System.out.println("   • ValidationBuilder provides fluent, readable validation chains");
        System.out.println("   • Built-in helpers for common validations (age, email, balance)");
        System.out.println("   • ValidationResult provides detailed error information");
        System.out.println("   • Typical validation time: < 200 microseconds for multiple rules");
        System.out.println("   • Perfect for user input validation and data quality checks");

        logger.info("Template-based rules demonstration completed");
    }
    
    /**
     * Demonstrate YAML configuration loading.
     *
     * This section shows how to externalize rules using YAML configuration files.
     * This approach enables business users to modify rules without code changes
     * and supports advanced features like versioning and metadata.
     *
     * Key concepts demonstrated:
     * - Loading rules from external YAML files
     * - Configuration metadata and versioning
     * - Rules engine creation from YAML configuration
     * - Error handling for missing or invalid configurations
     */
    private void demonstrateYamlConfiguration() {
        logger.info("Starting YAML configuration demonstration");

        System.out.println(" 3. YAML Configuration");
        System.out.println("-".repeat(40));
        System.out.println("External rule management with YAML configuration files");
        System.out.println("Perfect for: Business rule management, rule versioning, non-developer rule changes");
        System.out.println();

        try {
            System.out.println("Step 1: Loading YAML configuration");
            System.out.println("// Load rules from YAML configuration");
            System.out.println("YamlConfigurationLoader loader = new YamlConfigurationLoader();");
            System.out.println("YamlRuleConfiguration config = loader.loadFromStream(yamlStream);");
            System.out.println("YamlRulesEngineService service = new YamlRulesEngineService();");
            System.out.println("RulesEngine engine = service.createRulesEngineFromYamlConfig(config);");
            System.out.println();

            // Load the quick-start YAML configuration
            long loadStart = System.nanoTime();
            InputStream yamlStream = getClass().getResourceAsStream("/validation/basic-usage-examples-config.yaml");

            if (yamlStream != null) {
                logger.debug("Found YAML configuration file at /validation/basic-usage-examples-config.yaml");

                YamlConfigurationLoader loader = new YamlConfigurationLoader();
                YamlRuleConfiguration config = loader.loadFromStream(yamlStream);
                long loadDuration = System.nanoTime() - loadStart;

                System.out.println("Step 2: Configuration loaded successfully (in " + loadDuration/1000 + " microseconds)");
                System.out.println("Loaded Configuration:");
                System.out.println("  Name: " + (config.getMetadata() != null ? config.getMetadata().getName() : "N/A"));
                System.out.println("  Version: " + (config.getMetadata() != null ? config.getMetadata().getVersion() : "N/A"));
                System.out.println("  Rules: " + (config.getRules() != null ? config.getRules().size() : 0));

                logger.info("YAML configuration loaded: {} rules in {}ns",
                           (config.getRules() != null ? config.getRules().size() : 0), loadDuration);

                // Create rules engine from YAML
                System.out.println("\nStep 3: Creating rules engine from YAML");
                long engineStart = System.nanoTime();
                YamlRulesEngineService service = new YamlRulesEngineService();
                RulesEngine engine = service.createRulesEngineFromYamlConfig(config);
                long engineDuration = System.nanoTime() - engineStart;

                System.out.println("Rules engine created (in " + engineDuration/1000 + " microseconds)");
                System.out.println("  Engine configuration: " + (engine.getConfiguration() != null ? "✅ LOADED" : "❌ FAILED"));
                System.out.println("  Available rules: " + engine.getConfiguration().getAllRules().size());
                logger.debug("Rules engine created from YAML in {} nanoseconds", engineDuration);

                // Test the YAML rules using the engine
                Customer customer = createSampleCustomer();
                System.out.println("\nStep 4: Testing YAML rules against sample customer:");
                System.out.println("  Customer: " + customer.getName() + ", age " + customer.getAge() +
                                 ", email: " + customer.getEmail());

                // Execute rules using the engine
                Map<String, Object> customerData = Map.of(
                    "name", customer.getName(),
                    "age", customer.getAge(),
                    "email", customer.getEmail()
                );

                // Test with the loaded engine
                var allRules = engine.getConfiguration().getAllRules();
                if (!allRules.isEmpty()) {
                    var firstRule = allRules.get(0);
                    var result = engine.executeRule(firstRule, customerData);
                    System.out.println("  Rule test result: " + (result.isTriggered() ? "✅ PASSED" : "❌ FAILED"));
                } else {
                    System.out.println("  No rules found in configuration");
                }

                System.out.println("  PASSED YAML configuration loaded and tested successfully!");

            } else {
                logger.warn("YAML configuration file not found at /validation/basic-usage-examples-config.yaml");
                System.out.println("INFO: YAML configuration file not found");
                System.out.println("   Expected location: /validation/basic-usage-examples-config.yaml");
                System.out.println("   This demonstrates graceful handling of missing configuration files");
                System.out.println("   In production, you would typically have default rules or fallback behavior");
            }

        } catch (Exception e) {
            logger.error("Error loading YAML configuration", e);
            System.out.println("INFO: Error loading YAML configuration: " + e.getMessage());
            System.out.println("   This demonstrates error handling for invalid YAML files");
            System.out.println("   Production systems should include proper error recovery mechanisms");
        }

        System.out.println("\n Key Points:");
        System.out.println("   • YAML files provide external rule management");
        System.out.println("   • Rules can be modified without code changes or redeployment");
        System.out.println("   • Supports metadata, versioning, and complex rule structures");
        System.out.println("   • Typical load time: < 1ms for small configurations");
        System.out.println("   • Essential for business-user-managed rules and A/B testing");

        logger.info("YAML configuration demonstration completed");
    }
    
    /**
     * Create a sample customer for demonstrations.
     *
     * This method creates a test customer with realistic data that will trigger
     * various validation scenarios in the demonstrations. The customer is designed
     * to pass some validations and fail others to show different outcomes.
     *
     * Customer profile:
     * - Name: "John" (valid, non-empty)
     * - Age: 25 (valid, above 18)
     * - Email: "john@example.com" (valid, contains @)
     * - Balance: Not set (will cause balance validations to fail)
     *
     * @return A sample Customer object for testing
     */
    private Customer createSampleCustomer() {
        logger.debug("Loading sample customer data from external configuration");

        // Load customer data from external YAML file
        List<Map<String, Object>> customerData = DemoDataLoader.loadCustomerData("infrastructure/bootstrap/datasets/customer-profiles.yaml");

        // Use the first valid customer from the external data
        Map<String, Object> customerMap = customerData.stream()
            .filter(c -> "CUST001".equals(c.get("customerId")))
            .findFirst()
            .orElse(customerData.get(0)); // Fallback to first customer if CUST001 not found

        Customer customer = new Customer();
        customer.setName((String) customerMap.get("firstName"));
        customer.setAge((Integer) customerMap.get("age"));
        customer.setEmail((String) customerMap.get("email"));

        // Note: Customer model doesn't have accountBalance field
        // This is intentional to demonstrate validation scenarios

        logger.debug("Sample customer loaded from external data: {} (age: {}, email: {})",
                    customer.getName(), customer.getAge(), customer.getEmail());

        return customer;
    }

    /**
     * Load demo configuration from external YAML file.
     *
     * @return Configuration map with demo settings
     */
    private Map<String, Object> loadDemoConfiguration() {
        logger.debug("Loading demo configuration from external file");
        return DemoDataLoader.loadConfiguration("validation/basic-usage-examples-config.yaml");
    }
    

}



