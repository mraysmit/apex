package dev.mars.apex.demo.runners;

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


import dev.mars.apex.demo.enrichment.YamlDatasetDemo;
import dev.mars.apex.demo.lookup.SimpleFieldLookupDemo;
import dev.mars.apex.demo.lookup.NestedFieldLookupDemo;
import dev.mars.apex.demo.lookup.CompoundKeyLookupDemo;
import dev.mars.apex.demo.lookup.PostgreSQLLookupDemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PatternsRunner demonstrates common implementation patterns in the APEX Rules Engine.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class PatternsRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(PatternsRunner.class);
    
    /**
     * Main entry point for Patterns demos.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        logger.info("╔" + "═".repeat(78) + "╗");
        logger.info("║" + " ".repeat(22) + "APEX RULES ENGINE - PATTERNS DEMO" + " ".repeat(22) + "║");
        logger.info("╚" + "═".repeat(78) + "╝");
        logger.info("");
        logger.info("Welcome to the Patterns demonstration! 🔧");
        logger.info("");
        logger.info("This demo will teach you essential implementation patterns:");
        logger.info("• Lookup patterns for data enrichment");
        logger.info("• Calculation patterns for transformations");
        logger.info("• Validation patterns for data quality");
        logger.info("• Performance optimization techniques");
        logger.info("");
        logger.info("Estimated time: 20-30 minutes");
        logger.info("");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Run lookup pattern demos
            runLookupPatterns();
            
            // Run calculation pattern demos
            runCalculationPatterns();
            
            // Run validation pattern demos
            runValidationPatterns();
            
            // Show pattern summary and best practices
            showPatternSummary();
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            logger.info("");
            logger.info("✅ Patterns demo completed successfully!");
            logger.info("Total execution time: {} ms ({} seconds)", totalTime, totalTime / 1000.0);
            logger.info("");
            logger.info("You now understand the core implementation patterns.");
            logger.info("Ready for IndustryRunner to see real-world applications!");
            
        } catch (Exception e) {
            logger.error("Patterns demo failed: {}", e.getMessage(), e);
            throw new RuntimeException("Patterns demo failed", e);
        }
    }
    
    /**
     * Run lookup pattern demonstrations.
     */
    private static void runLookupPatterns() {
        logger.info("🔍 LOOKUP PATTERNS DEMONSTRATION");
        logger.info("═".repeat(50));
        logger.info("");
        logger.info("Lookup patterns are the most commonly used enrichment technique.");
        logger.info("They allow you to enrich data by looking up reference information.");
        logger.info("");
        
        // Simple Field Lookup
        runLookupDemo("Simple Field Lookup", 
                     "Basic key-value lookup using a single field",
                     () -> SimpleFieldLookupDemo.main(new String[]{}));
        
        // Conditional Expression Lookup
        runLookupDemo("Conditional Expression Lookup",
                     "Dynamic lookup keys based on conditions",
                     () -> YamlDatasetDemo.main(new String[]{}));
        
        // Nested Field Lookup
        runLookupDemo("Nested Field Lookup",
                     "Complex object navigation for lookup keys",
                     () -> NestedFieldLookupDemo.main(new String[]{}));
        
        // Compound Key Lookup
        runLookupDemo("Compound Key Lookup",
                     "Multi-field lookup keys for complex scenarios",
                     () -> CompoundKeyLookupDemo.main(new String[]{}));
        
        // Comprehensive Lookup
        runLookupDemo("Comprehensive Lookup",
                     "Advanced lookup scenarios and techniques",
                     () -> PostgreSQLLookupDemo.main(new String[]{}));
        
        logger.info("");
        logger.info("📋 LOOKUP PATTERN SUMMARY:");
        logger.info("• Simple Field: Use for basic key-value lookups");
        logger.info("• Conditional: Use when lookup key depends on conditions");
        logger.info("• Nested Field: Use for complex object structures");
        logger.info("• Compound Key: Use when multiple fields form the key");
        logger.info("• Comprehensive: Use for advanced scenarios");
        logger.info("");
    }
    
    /**
     * Run calculation pattern demonstrations.
     */
    private static void runCalculationPatterns() {
        logger.info("🧮 CALCULATION PATTERNS DEMONSTRATION");
        logger.info("═".repeat(50));
        logger.info("");
        logger.info("Calculation patterns transform and compute data values.");
        logger.info("They're essential for business logic and data processing.");
        logger.info("");
        
        logger.info("📊 Mathematical Calculations:");
        logger.info("• Arithmetic operations: +, -, *, /, %");
        logger.info("• Mathematical functions: Math.abs(), Math.max(), Math.min()");
        logger.info("• Financial calculations: interest, fees, discounts");
        logger.info("• Statistical operations: averages, sums, counts");
        logger.info("");
        
        logger.info("📝 String Manipulations:");
        logger.info("• Concatenation and formatting");
        logger.info("• Case conversions: toUpperCase(), toLowerCase()");
        logger.info("• Substring operations and trimming");
        logger.info("• Pattern matching and replacement");
        logger.info("");
        
        logger.info("📅 Date/Time Operations:");
        logger.info("• Date arithmetic: adding/subtracting days, months");
        logger.info("• Date formatting and parsing");
        logger.info("• Business day calculations");
        logger.info("• Time zone conversions");
        logger.info("");
        
        logger.info("Example SpEL expressions for calculations:");
        logger.info("• Mathematical: #amount * 1.08  (add 8% tax)");
        logger.info("• String: #firstName + ' ' + #lastName");
        logger.info("• Date: #tradeDate.plusDays(3)  (settlement date)");
        logger.info("• Conditional: #amount > 1000 ? #amount * 0.95 : #amount");
        logger.info("");
    }
    
    /**
     * Run validation pattern demonstrations.
     */
    private static void runValidationPatterns() {
        logger.info("✅ VALIDATION PATTERNS DEMONSTRATION");
        logger.info("═".repeat(50));
        logger.info("");
        logger.info("Validation patterns ensure data quality and business rule compliance.");
        logger.info("They're critical for maintaining data integrity.");
        logger.info("");
        
        logger.info("📋 Format Validations:");
        logger.info("• Email format: #email.matches('[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}')");
        logger.info("• Phone format: #phone.matches('\\+?[1-9]\\d{1,14}')");
        logger.info("• Date format: #dateString.matches('\\d{4}-\\d{2}-\\d{2}')");
        logger.info("• Currency code: #currency.matches('[A-Z]{3}')");
        logger.info("");
        
        logger.info("🏢 Business Rule Validations:");
        logger.info("• Age requirements: #age >= 18");
        logger.info("• Amount limits: #amount <= 10000");
        logger.info("• Status checks: #status == 'ACTIVE'");
        logger.info("• Relationship validation: #customerId != null && #customerId.length() > 0");
        logger.info("");
        
        logger.info("🔗 Cross-Field Validations:");
        logger.info("• Date ranges: #startDate.isBefore(#endDate)");
        logger.info("• Conditional requirements: #type == 'PREMIUM' ? #creditScore >= 700 : true");
        logger.info("• Mutual exclusivity: !(#hasA && #hasB)");
        logger.info("• Dependent fields: #country == 'US' ? #state != null : true");
        logger.info("");
        
        logger.info("⚡ Performance Tips:");
        logger.info("• Use simple expressions for frequently executed validations");
        logger.info("• Cache expensive lookups and calculations");
        logger.info("• Order validations by execution cost (cheap first)");
        logger.info("• Use short-circuit evaluation: #field != null && #field.length() > 0");
        logger.info("");
    }
    
    /**
     * Show pattern summary and best practices.
     */
    private static void showPatternSummary() {
        logger.info("📚 PATTERN BEST PRACTICES SUMMARY");
        logger.info("═".repeat(50));
        logger.info("");
        logger.info("🎯 CHOOSING THE RIGHT PATTERN:");
        logger.info("");
        logger.info("For Data Enrichment:");
        logger.info("• Simple lookups → Simple Field Lookup pattern");
        logger.info("• Dynamic keys → Conditional Expression Lookup pattern");
        logger.info("• Complex objects → Nested Field Lookup pattern");
        logger.info("• Multiple key fields → Compound Key Lookup pattern");
        logger.info("");
        logger.info("For Data Transformation:");
        logger.info("• Mathematical operations → Calculation patterns");
        logger.info("• String formatting → String manipulation patterns");
        logger.info("• Date operations → Date/time calculation patterns");
        logger.info("");
        logger.info("For Data Quality:");
        logger.info("• Format checking → Format validation patterns");
        logger.info("• Business rules → Business rule validation patterns");
        logger.info("• Field relationships → Cross-field validation patterns");
        logger.info("");
        logger.info("🚀 PERFORMANCE OPTIMIZATION:");
        logger.info("• Cache frequently accessed reference data");
        logger.info("• Use inline datasets for small, static lookups");
        logger.info("• Order rules by execution frequency and cost");
        logger.info("• Minimize complex regular expressions");
        logger.info("• Use short-circuit evaluation in conditions");
        logger.info("");
        logger.info("📁 CONFIGURATION ORGANIZATION:");
        logger.info("• Group related rules in rule chains");
        logger.info("• Use meaningful IDs and descriptions");
        logger.info("• Document complex expressions with comments");
        logger.info("• Separate configuration by environment");
        logger.info("");
    }
    
    /**
     * Helper method to run individual lookup demos with error handling.
     */
    private static void runLookupDemo(String name, String description, Runnable demo) {
        logger.info("▶ {}", name);
        logger.info("  {}", description);
        
        try {
            demo.run();
            logger.info("  ✅ Completed successfully");
        } catch (Exception e) {
            logger.warn("  ⚠ Demo encountered issues: {}", e.getMessage());
            logger.info("  📝 This is expected if core dependencies are not available");
            logger.info("  🎯 The pattern concepts and configurations are still valid!");
        }
        
        logger.info("");
    }
}
