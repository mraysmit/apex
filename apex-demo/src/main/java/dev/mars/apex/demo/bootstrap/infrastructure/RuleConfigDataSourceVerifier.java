package dev.mars.apex.demo.bootstrap.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Data source verification component for Rule Configuration Bootstrap Demo.
 * Verifies all data sources used in the rule configuration demonstrations.
 */
@Component
public class RuleConfigDataSourceVerifier {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleConfigDataSourceVerifier.class);
    
    private final RuleConfigDatabaseSetup databaseSetup;
    private final RuleConfigExternalDatasetSetup externalDatasetSetup;
    
    public RuleConfigDataSourceVerifier(RuleConfigDatabaseSetup databaseSetup, 
                                       RuleConfigExternalDatasetSetup externalDatasetSetup) {
        this.databaseSetup = databaseSetup;
        this.externalDatasetSetup = externalDatasetSetup;
    }
    
    /**
     * Verifies all data sources used in the rule configuration demo:
     * 1. PostgreSQL database (loan applications, customer profiles, orders)
     * 2. External YAML files (rule configurations)
     * 3. Inline datasets (embedded in YAML configurations)
     */
    public boolean verifyAllDataSources() {
        logger.info("=== Rule Configuration Bootstrap Demo - Data Source Verification ===");
        
        boolean allVerified = true;
        
        // 1. Verify PostgreSQL Database
        if (verifyPostgreSQLDatabase()) {
            logger.info("Data Source 1: PostgreSQL Database - VERIFIED");
        } else {
            logger.error("Data Source 1: PostgreSQL Database - FAILED");
            allVerified = false;
        }
        
        // 2. Verify External YAML Rule Configurations
        if (verifyExternalYamlConfigurations()) {
            logger.info("Data Source 2: External YAML Rule Configurations - VERIFIED");
        } else {
            logger.error("Data Source 2: External YAML Rule Configurations - FAILED");
            allVerified = false;
        }
        
        // 3. Verify Inline Datasets (always available as they're embedded)
        if (verifyInlineDatasets()) {
            logger.info("Data Source 3: Inline YAML Datasets - VERIFIED");
        } else {
            logger.error("Data Source 3: Inline YAML Datasets - FAILED");
            allVerified = false;
        }
        
        if (allVerified) {
            logger.info("All data sources verified successfully! Ready for rule configuration processing.");
        } else {
            logger.error("Some data sources failed verification. Demo may not work correctly.");
        }
        
        return allVerified;
    }
    
    /**
     * Verifies PostgreSQL database connectivity and table structure.
     */
    private boolean verifyPostgreSQLDatabase() {
        logger.debug("Verifying PostgreSQL database...");
        
        try {
            if (databaseSetup.isInMemoryMode()) {
                logger.debug("Database running in in-memory simulation mode");
                return true;
            }
            
            Connection connection = databaseSetup.getConnection();
            if (connection == null || connection.isClosed()) {
                logger.error("Database connection is not available");
                return false;
            }
            
            // Verify required tables exist and have data
            String[] requiredTables = {
                "loan_applications", 
                "customer_profiles", 
                "order_processing", 
                "rule_execution_audit"
            };
            
            for (String tableName : requiredTables) {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
                    
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        logger.debug("Table {} contains {} records", tableName, count);
                        
                        // Verify we have sample data (except for audit table)
                        if (!tableName.equals("rule_execution_audit") && count == 0) {
                            logger.error("Table {} is empty - sample data missing", tableName);
                            return false;
                        }
                    } else {
                        logger.error("Could not query table {}", tableName);
                        return false;
                    }
                }
            }
            
            logger.debug("PostgreSQL database verification completed successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("PostgreSQL database verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies external YAML rule configuration files exist and are readable.
     */
    private boolean verifyExternalYamlConfigurations() {
        logger.debug("Verifying external YAML rule configurations...");
        
        try {
            String[] requiredConfigFiles = {
                "src/main/resources/bootstrap/rule-configuration-bootstrap.yaml",
                "src/main/resources/bootstrap/datasets/loan-approval-rules.yaml",
                "src/main/resources/bootstrap/datasets/discount-rules.yaml",
                "src/main/resources/bootstrap/datasets/combined-rules.yaml"
            };
            
            for (String configFile : requiredConfigFiles) {
                Path configPath = Paths.get(configFile);
                
                if (!Files.exists(configPath)) {
                    logger.error("Required configuration file does not exist: {}", configFile);
                    return false;
                }
                
                if (!Files.isReadable(configPath)) {
                    logger.error("Configuration file is not readable: {}", configFile);
                    return false;
                }
                
                // Verify file has content
                String content = Files.readString(configPath);
                if (content.trim().isEmpty()) {
                    logger.error("Configuration file is empty: {}", configFile);
                    return false;
                }
                
                logger.debug("Configuration file verified: {} ({} bytes)", 
                           configFile, Files.size(configPath));
            }
            
            logger.debug("External YAML configuration verification completed successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("External YAML configuration verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifies inline datasets (embedded in YAML configurations).
     */
    private boolean verifyInlineDatasets() {
        logger.debug("Verifying inline datasets...");
        
        try {
            // Inline datasets are embedded in the YAML configurations
            // We verify by checking the main configuration file contains dataset sections
            Path mainConfigPath = Paths.get("src/main/resources/bootstrap/rule-configuration-bootstrap.yaml");
            
            if (!Files.exists(mainConfigPath)) {
                logger.error("Main configuration file not found for inline dataset verification");
                return false;
            }
            
            String content = Files.readString(mainConfigPath);
            
            // Check for expected inline dataset sections
            String[] expectedSections = {
                "datasets:",
                "enrichment:",
                "scenarios:"
            };
            
            for (String section : expectedSections) {
                if (!content.contains(section)) {
                    logger.error("Main configuration missing expected section: {}", section);
                    return false;
                }
            }
            
            logger.debug("Inline datasets verification completed successfully");
            return true;
            
        } catch (Exception e) {
            logger.error("Inline datasets verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Provides a detailed status report of all data sources.
     */
    public void printDataSourceStatus() {
        logger.info("=== Rule Configuration Demo - Data Source Status Report ===");
        
        // PostgreSQL Database Status
        logger.info("Data Source 1: PostgreSQL Database");
        logger.info("   Type: Loan applications, customer profiles, order processing");
        logger.info("   Contains: Sample loan applications, customer data, order history");
        logger.info("   Status: {}", verifyPostgreSQLDatabase() ? "Connected and ready" : "Not available");
        
        // External YAML Configuration Status
        logger.info("Data Source 2: External YAML Rule Configurations");
        logger.info("   Type: Business rule definitions and configurations");
        logger.info("   Contains: Loan approval rules, discount rules, combined rule patterns");
        logger.info("   Status: {}", verifyExternalYamlConfigurations() ? "Files exist and readable" : "Not available");
        
        // Inline Dataset Status
        logger.info("Data Source 3: Inline YAML Datasets");
        logger.info("   Type: Embedded reference data and lookup tables");
        logger.info("   Contains: Static data embedded in configuration files");
        logger.info("   Status: {}", verifyInlineDatasets() ? "Available in configurations" : "Not available");
        
        logger.info("=== End Status Report ===");
    }
    
    /**
     * Creates any missing data sources that can be automatically generated.
     */
    public void createMissingDataSources() {
        logger.info("Checking for missing data sources and creating if needed...");
        
        // PostgreSQL database can be set up automatically
        if (!verifyPostgreSQLDatabase()) {
            logger.info("Setting up PostgreSQL database...");
            try {
                databaseSetup.setupRuleConfigurationDatabase();
                logger.info("PostgreSQL database setup completed");
            } catch (Exception e) {
                logger.error("Failed to setup PostgreSQL database: {}", e.getMessage());
            }
        }
        
        // External YAML files can be created automatically
        if (!verifyExternalYamlConfigurations()) {
            logger.info("Creating external YAML configuration files...");
            try {
                externalDatasetSetup.createRuleConfigurationFiles();
                logger.info("External YAML configuration files created");
            } catch (Exception e) {
                logger.error("Failed to create external YAML files: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Performs cleanup of verification resources.
     */
    public void cleanup() {
        logger.info("Cleaning up data source verification resources...");
        // No specific cleanup needed for verification
        logger.debug("Data source verification cleanup completed");
    }
}
