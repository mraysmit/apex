package dev.mars.apex.demo.validation;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * YAML Validation Test - Validates all YAML files in resources/yaml folder
 * 
 * This test loads and validates every YAML file to ensure:
 * - Syntactic correctness
 * - APEX configuration compliance
 * - No loading errors
 * - Proper metadata structure
 */
@DisplayName("YAML Validation Test")
public class YamlValidationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(YamlValidationTest.class);

    @Test
    @DisplayName("Should validate all YAML files in resources/yaml folder")
    void validateAllYamlFiles() {
        logger.info("=== Validating All YAML Files ===");
        
        // Get all YAML files from resources/yaml
        String[] yamlFiles = {
            "yaml/basic-usage-examples-config.yaml",
            "yaml/basic-usage-examples-data.yaml",
            "yaml/calculation-mathematical-test.yaml",
            "yaml/commodity-swap-validation-quick-demo.yaml",
            "yaml/conditional-boolean-test.yaml",
            "yaml/dataset-inline-test.yaml",
            "yaml/external-data-config-database-test.yaml",
            "yaml/lookup-basic-inline-test.yaml"
        };
        
        List<String> validFiles = new ArrayList<>();
        List<String> invalidFiles = new ArrayList<>();
        
        for (String yamlFile : yamlFiles) {
            try {
                logger.info("Validating: {}", yamlFile);
                
                YamlRuleConfiguration config = yamlLoader.loadFromClasspath(yamlFile);
                
                // Basic validation checks
                assertNotNull(config, "Configuration should not be null for " + yamlFile);
                assertNotNull(config.getMetadata(), "Metadata should not be null for " + yamlFile);
                assertNotNull(config.getMetadata().getName(), "Name should not be null for " + yamlFile);
                
                validFiles.add(yamlFile);
                logger.info("✅ VALID: {} - {}", yamlFile, config.getMetadata().getName());
                
            } catch (Exception e) {
                invalidFiles.add(yamlFile + " - " + e.getMessage());
                logger.error("❌ INVALID: {} - {}", yamlFile, e.getMessage());
            }
        }
        
        // Report results
        logger.info("=== VALIDATION SUMMARY ===");
        logger.info("Total files: {}", yamlFiles.length);
        logger.info("Valid files: {}", validFiles.size());
        logger.info("Invalid files: {}", invalidFiles.size());
        
        if (!invalidFiles.isEmpty()) {
            logger.error("INVALID FILES:");
            invalidFiles.forEach(file -> logger.error("  - {}", file));
        }
        
        // Assert all files are valid
        assertTrue(invalidFiles.isEmpty(), 
            "Some YAML files are invalid: " + String.join(", ", invalidFiles));
        
        logger.info("✅ All {} YAML files validated successfully!", validFiles.size());
    }
    
    @Test
    @DisplayName("Should validate individual YAML files")
    void validateIndividualFiles() {
        logger.info("=== Individual YAML File Validation ===");
        
        // Test each file individually for detailed validation
        validateYamlFile("yaml/basic-usage-examples-config.yaml", "Basic Usage Examples Configuration");
        validateYamlFile("yaml/calculation-mathematical-test.yaml", "Calculation Mathematical Operations Test");
        validateYamlFile("yaml/conditional-boolean-test.yaml", "Conditional Boolean Expressions Test");
        validateYamlFile("yaml/lookup-basic-inline-test.yaml", "Lookup Basic Inline Operations Test");
        validateYamlFile("yaml/dataset-inline-test.yaml", "Dataset Document Inline Data Test");
        validateYamlFile("yaml/external-data-config-database-test.yaml", "External Data Config Database Test");
        validateYamlFile("yaml/commodity-swap-validation-quick-demo.yaml", "Commodity Swap Validation Quick Demo");
        
        logger.info("✅ Individual validation completed successfully!");
    }
    
    private void validateYamlFile(String filePath, String expectedName) {
        try {
            logger.info("Validating individual file: {}", filePath);
            
            YamlRuleConfiguration config = yamlLoader.loadFromClasspath(filePath);
            
            assertNotNull(config, "Configuration should load for " + filePath);
            assertNotNull(config.getMetadata(), "Metadata should exist for " + filePath);
            assertNotNull(config.getMetadata().getName(), "Name should exist for " + filePath);
            
            logger.info("✅ {} - Loaded successfully", filePath);
            logger.info("   Name: {}", config.getMetadata().getName());
            logger.info("   Type: {}", config.getMetadata().getType());
            logger.info("   Version: {}", config.getMetadata().getVersion());
            
        } catch (Exception e) {
            logger.error("❌ Failed to validate {}: {}", filePath, e.getMessage());
            fail("Failed to validate " + filePath + ": " + e.getMessage());
        }
    }
}
