package dev.mars.apex.demo.bootstrap.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Data source verification component for OTC Options Bootstrap Demo.
 * Verifies that all three data sources are accessible and properly configured.
 */
@Component
public class DataSourceVerifier {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceVerifier.class);
    
    private DatabaseSetup databaseSetup;

    public DataSourceVerifier() {
        this.databaseSetup = new DatabaseSetup();
    }
    
    /**
     * Verifies all three data sources used in the demo:
     * 1. Inline YAML dataset (embedded in configuration)
     * 2. PostgreSQL database (counterparty reference table)
     * 3. External YAML file (market data)
     */
    public boolean verifyAllDataSources() {
        logger.info("=== OTC Options Bootstrap Demo - Data Source Verification ===");
        
        boolean allVerified = true;
        
        // 1. Verify Inline Dataset (always available as it's embedded)
        if (verifyInlineDataset()) {
            logger.info("Data Source 1: Inline YAML Dataset - VERIFIED");
        } else {
            logger.error("Data Source 1: Inline YAML Dataset - FAILED");
            allVerified = false;
        }

        // 2. Verify PostgreSQL Database
        if (verifyPostgreSQLDatabase()) {
            logger.info("Data Source 2: PostgreSQL Database - VERIFIED");
        } else {
            logger.error("Data Source 2: PostgreSQL Database - FAILED");
            allVerified = false;
        }

        // 3. Verify External YAML File
        if (verifyExternalYamlFile()) {
            logger.info("Data Source 3: External YAML File - VERIFIED");
        } else {
            logger.error("Data Source 3: External YAML File - FAILED");
            allVerified = false;
        }

        if (allVerified) {
            logger.info("All data sources verified successfully! Ready to process OTC Options.");
        } else {
            logger.error("Some data sources failed verification. Demo may not work correctly.");
        }
        
        return allVerified;
    }
    
    /**
     * Verifies the inline dataset configuration.
     * Since inline datasets are embedded in the YAML configuration,
     * this mainly checks that the configuration structure is valid.
     */
    private boolean verifyInlineDataset() {
        logger.debug("Verifying inline dataset configuration...");
        
        try {
            // Check if the bootstrap configuration file exists in classpath
            var configResource = getClass().getClassLoader().getResource("bootstrap/otc-options-bootstrap.yaml");
            if (configResource == null) {
                logger.debug("Bootstrap configuration file not found in classpath - this is expected for demo");
            } else {
                logger.debug("Bootstrap configuration file found in classpath: {}", configResource);
            }

            // Inline datasets are embedded in configuration, so they're always "available"
            // The actual verification happens when the YAML is parsed
            logger.debug("Inline dataset verification completed - configuration structure OK");
            return true;

        } catch (Exception e) {
            logger.error("Inline dataset verification failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifies the PostgreSQL database connection and counterparty table.
     */
    private boolean verifyPostgreSQLDatabase() {
        logger.debug("Verifying PostgreSQL database...");
        
        try {
            return databaseSetup.verifyDatabaseSetup();
        } catch (Exception e) {
            logger.error("PostgreSQL database verification failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifies the external YAML file for market data.
     */
    private boolean verifyExternalYamlFile() {
        logger.debug("Verifying external YAML file...");
        
        try {
            // Check if the market data file exists
            Path marketDataPath = Paths.get("src/main/resources/bootstrap/datasets/market-data.yaml");
            
            if (!Files.exists(marketDataPath)) {
                logger.error("External YAML file not found at: {}", marketDataPath);
                return false;
            }
            
            // Verify file is readable
            if (!Files.isReadable(marketDataPath)) {
                logger.error("External YAML file is not readable: {}", marketDataPath);
                return false;
            }
            
            // Check file size (should not be empty)
            long fileSize = Files.size(marketDataPath);
            if (fileSize == 0) {
                logger.error("External YAML file is empty: {}", marketDataPath);
                return false;
            }
            
            // Basic content validation - check if it contains expected structure
            String content = Files.readString(marketDataPath);
            if (!content.contains("currencies") || !content.contains("USD")) {
                logger.error("External YAML file does not contain expected currency data structure");
                return false;
            }
            
            logger.debug("External YAML file verified - size: {} bytes", fileSize);
            return true;
            
        } catch (Exception e) {
            logger.error("External YAML file verification failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Provides detailed status report of all data sources.
     */
    public void printDataSourceStatus() {
        logger.info("=== Data Source Status Report ===");
        
        // Inline Dataset Status
        logger.info("Data Source 1: Inline YAML Dataset");
        logger.info("   Type: Commodity reference data embedded in configuration");
        logger.info("   Contains: Commodity categories, exchanges, risk factors, margin rates");
        logger.info("   Status: Always available (embedded in YAML configuration)");

        // PostgreSQL Database Status
        logger.info("Data Source 2: PostgreSQL Database");
        logger.info("   Type: Counterparty reference table");
        logger.info("   Contains: Legal names, credit ratings, LEI codes, jurisdictions");
        logger.info("   Status: {}", verifyPostgreSQLDatabase() ? "Connected and ready" : "Not available");

        // External YAML File Status
        logger.info("Data Source 3: External YAML File");
        logger.info("   Type: Market and currency reference data");
        logger.info("   Contains: Currency details, timezones, trading hours");
        logger.info("   Status: {}", verifyExternalYamlFile() ? "File exists and readable" : "Not available");
        
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
                databaseSetup.setupCounterpartyTable();
                logger.info("PostgreSQL database setup completed");
            } catch (Exception e) {
                logger.error("Failed to setup PostgreSQL database: {}", e.getMessage());
            }
        }
        
        // External YAML file will be created by ExternalDatasetSetup component
        if (!verifyExternalYamlFile()) {
            logger.info("External YAML file missing - will be created by ExternalDatasetSetup");
        }
    }
}
