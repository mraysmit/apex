package dev.mars.apex.demo.examples.lookups;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.config.yaml.YamlRulesEngineService;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all lookup demonstration examples.
 * Provides common functionality for loading YAML configurations, processing data,
 * and displaying results with performance metrics.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-22
 * @version 1.0
 */
public abstract class AbstractLookupDemo {
    
    protected static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    protected RulesEngine rulesEngine;
    protected final YamlRulesEngineService yamlService;
    protected final YamlConfigurationLoader yamlLoader;
    protected YamlRuleConfiguration ruleConfiguration;

    public AbstractLookupDemo() {
        this.yamlService = new YamlRulesEngineService();
        this.yamlLoader = new YamlConfigurationLoader();
    }
    
    /**
     * Main execution method that orchestrates the demo.
     */
    public final void runDemo() {
        printHeader();
        
        try {
            // Load configuration
            long startTime = System.currentTimeMillis();
            loadConfiguration();
            long configLoadTime = System.currentTimeMillis() - startTime;
            
            // Generate test data
            startTime = System.currentTimeMillis();
            List<?> testData = generateTestData();
            long dataGenTime = System.currentTimeMillis() - startTime;
            
            // Process data and measure performance
            startTime = System.currentTimeMillis();
            List<?> results = processData(testData);
            long processingTime = System.currentTimeMillis() - startTime;

            // Display results
            displayResults(results, testData);
            
            // Show performance metrics
            displayPerformanceMetrics(configLoadTime, dataGenTime, processingTime, testData.size());
            
            // Demonstrate error scenarios
            demonstrateErrorScenarios();
            
        } catch (Exception e) {
            System.err.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        printFooter();
    }
    
    /**
     * Print demo header with title and description.
     */
    protected void printHeader() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔍 " + getDemoTitle());
        System.out.println("=".repeat(80));
        System.out.println("📝 " + getDemoDescription());
        System.out.println("⏰ Started at: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("-".repeat(80));
    }
    
    /**
     * Print demo footer.
     */
    protected void printFooter() {
        System.out.println("-".repeat(80));
        System.out.println("✅ Demo completed at: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        System.out.println("=".repeat(80) + "\n");
    }
    
    /**
     * Display performance metrics.
     */
    protected void displayPerformanceMetrics(long configLoadTime, long dataGenTime, 
                                           long processingTime, int recordCount) {
        System.out.println("\n📊 PERFORMANCE METRICS:");
        System.out.println("  Configuration Load Time: " + configLoadTime + "ms");
        System.out.println("  Test Data Generation Time: " + dataGenTime + "ms");
        System.out.println("  Processing Time: " + processingTime + "ms");
        System.out.println("  Records Processed: " + recordCount);
        if (recordCount > 0) {
            System.out.printf("  Average Time per Record: %.2fms%n", (double) processingTime / recordCount);
        }
        System.out.println("  Total Execution Time: " + (configLoadTime + dataGenTime + processingTime) + "ms");
    }
    
    /**
     * Display processing results in a formatted way.
     */
    protected void displayResults(List<?> results, List<?> originalData) {
        System.out.println("\n📋 PROCESSING RESULTS:");
        System.out.println("  Total Records: " + originalData.size());
        System.out.println("  Processed Records: " + results.size());

        for (int i = 0; i < Math.min(results.size(), 5); i++) {
            Object result = results.get(i);
            Object original = originalData.get(i);

            System.out.println("\n  📄 Record " + (i + 1) + ":");
            System.out.println("    Original: " + formatObject(original));
            System.out.println("    Processed: " + formatObject(result));
        }

        if (results.size() > 5) {
            System.out.println("  ... and " + (results.size() - 5) + " more records");
        }
    }
    
    /**
     * Format an object for display, handling different types appropriately.
     */
    protected String formatObject(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            map.entrySet().stream()
                .limit(5) // Limit to first 5 entries for readability
                .forEach(entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", "));
            if (sb.length() > 1) {
                sb.setLength(sb.length() - 2); // Remove trailing comma
            }
            if (map.size() > 5) {
                sb.append(", ... ").append(map.size() - 5).append(" more");
            }
            sb.append("}");
            return sb.toString();
        }
        
        return obj.toString();
    }
    
    /**
     * Demonstrate error scenarios like missing keys, null values, etc.
     */
    protected void demonstrateErrorScenarios() {
        System.out.println("\n⚠️  ERROR SCENARIO DEMONSTRATIONS:");
        
        try {
            List<?> errorData = generateErrorTestData();
            if (errorData != null && !errorData.isEmpty()) {
                List<?> errorResults = processData(errorData);
                System.out.println("  Processed " + errorData.size() + " error scenarios");
                System.out.println("  Results: " + errorResults.size() + " successful, " +
                                 (errorData.size() - errorResults.size()) + " failed");
            } else {
                System.out.println("  No error scenarios defined for this demo");
            }
        } catch (Exception e) {
            System.out.println("  Error scenario processing failed: " + e.getMessage());
        }
    }
    
    // Abstract methods to be implemented by concrete demo classes
    
    /**
     * Get the title of this demo.
     */
    protected abstract String getDemoTitle();
    
    /**
     * Get the description of this demo.
     */
    protected abstract String getDemoDescription();
    
    /**
     * Get the path to the YAML configuration file for this demo.
     */
    protected abstract String getYamlConfigPath();
    
    /**
     * Load the YAML configuration for this demo.
     */
    protected abstract void loadConfiguration() throws Exception;
    
    /**
     * Generate test data for this demo.
     */
    protected abstract List<?> generateTestData();
    
    /**
     * Process the test data using the loaded configuration.
     */
    protected abstract List<?> processData(List<?> data) throws Exception;
    
    /**
     * Generate test data that will cause errors (optional).
     * Return null or empty list if no error scenarios are defined.
     */
    protected List<?> generateErrorTestData() {
        return null; // Default: no error scenarios
    }
}
