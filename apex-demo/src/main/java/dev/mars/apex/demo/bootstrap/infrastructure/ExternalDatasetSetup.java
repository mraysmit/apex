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
 * External dataset setup component for OTC Options Bootstrap Demo.
 * Creates the external YAML file containing market and currency reference data.
 */
@Component
public class ExternalDatasetSetup {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalDatasetSetup.class);
    
    /**
     * Creates the external market data YAML file.
     */
    public void createMarketDataFile() {
        logger.info("Creating external market data YAML file...");
        
        try {
            // Create the datasets directory if it doesn't exist
            Path datasetsDir = Paths.get("src/main/resources/bootstrap/datasets");
            Files.createDirectories(datasetsDir);
            
            // Generate the market data YAML content
            String yamlContent = generateMarketDataYaml();
            
            // Write to file
            Path marketDataFile = datasetsDir.resolve("market-data.yaml");
            Files.writeString(marketDataFile, yamlContent);
            
            logger.info("External market data YAML file created successfully: {}", marketDataFile);
            
        } catch (IOException e) {
            logger.error("Failed to create market data YAML file: {}", e.getMessage(), e);
            throw new RuntimeException("External dataset setup failed", e);
        }
    }
    
    /**
     * Generates the YAML content for market and currency data.
     */
    private String generateMarketDataYaml() {
        StringBuilder yaml = new StringBuilder();
        
        // Header and metadata
        yaml.append("# ============================================================================\n");
        yaml.append("# APEX Rules Engine - Market Data Reference Dataset\n");
        yaml.append("# ============================================================================\n");
        yaml.append("#\n");
        yaml.append("# External YAML dataset containing currency and market reference data\n");
        yaml.append("# for OTC Options Bootstrap Demo enrichment processing.\n");
        yaml.append("#\n");
        yaml.append("# Generated on: ").append(LocalDate.now()).append("\n");
        yaml.append("# Used by: OTC Options Bootstrap Demo\n");
        yaml.append("# Purpose: Currency enrichment and market data lookup\n");
        yaml.append("#\n");
        yaml.append("# ============================================================================\n\n");
        
        // Metadata section
        yaml.append("metadata:\n");
        yaml.append("  name: \"Market Data Reference Dataset\"\n");
        yaml.append("  version: \"1.0.0\"\n");
        yaml.append("  description: \"Currency and market reference data for OTC Options processing\"\n");
        yaml.append("  created-date: \"").append(LocalDate.now()).append("\"\n");
        yaml.append("  source: \"OTC Options Bootstrap Demo Generator\"\n");
        yaml.append("  coverage: \"Major trading currencies and market data\"\n\n");
        
        // Currency data section
        yaml.append("# Currency reference data with trading and market information\n");
        yaml.append("currencies:\n");
        
        // USD
        yaml.append("  - code: \"USD\"\n");
        yaml.append("    name: \"US Dollar\"\n");
        yaml.append("    symbol: \"$\"\n");
        yaml.append("    region: \"North America\"\n");
        yaml.append("    country: \"United States\"\n");
        yaml.append("    timezone: \"EST\"\n");
        yaml.append("    tradingHours: \"09:00-17:00\"\n");
        yaml.append("    decimalPlaces: 2\n");
        yaml.append("    majorCurrency: true\n");
        yaml.append("    centralBank: \"Federal Reserve\"\n");
        yaml.append("    holidays:\n");
        yaml.append("      - \"2025-01-01\"  # New Year's Day\n");
        yaml.append("      - \"2025-07-04\"  # Independence Day\n");
        yaml.append("      - \"2025-12-25\"  # Christmas Day\n\n");
        
        // EUR
        yaml.append("  - code: \"EUR\"\n");
        yaml.append("    name: \"Euro\"\n");
        yaml.append("    symbol: \"€\"\n");
        yaml.append("    region: \"Europe\"\n");
        yaml.append("    country: \"Eurozone\"\n");
        yaml.append("    timezone: \"CET\"\n");
        yaml.append("    tradingHours: \"08:00-16:00\"\n");
        yaml.append("    decimalPlaces: 2\n");
        yaml.append("    majorCurrency: true\n");
        yaml.append("    centralBank: \"European Central Bank\"\n");
        yaml.append("    holidays:\n");
        yaml.append("      - \"2025-01-01\"  # New Year's Day\n");
        yaml.append("      - \"2025-05-01\"  # Labour Day\n");
        yaml.append("      - \"2025-12-25\"  # Christmas Day\n\n");
        
        // GBP
        yaml.append("  - code: \"GBP\"\n");
        yaml.append("    name: \"British Pound Sterling\"\n");
        yaml.append("    symbol: \"£\"\n");
        yaml.append("    region: \"Europe\"\n");
        yaml.append("    country: \"United Kingdom\"\n");
        yaml.append("    timezone: \"GMT\"\n");
        yaml.append("    tradingHours: \"08:00-16:00\"\n");
        yaml.append("    decimalPlaces: 2\n");
        yaml.append("    majorCurrency: true\n");
        yaml.append("    centralBank: \"Bank of England\"\n");
        yaml.append("    holidays:\n");
        yaml.append("      - \"2025-01-01\"  # New Year's Day\n");
        yaml.append("      - \"2025-12-25\"  # Christmas Day\n");
        yaml.append("      - \"2025-12-26\"  # Boxing Day\n\n");
        
        // JPY
        yaml.append("  - code: \"JPY\"\n");
        yaml.append("    name: \"Japanese Yen\"\n");
        yaml.append("    symbol: \"¥\"\n");
        yaml.append("    region: \"Asia\"\n");
        yaml.append("    country: \"Japan\"\n");
        yaml.append("    timezone: \"JST\"\n");
        yaml.append("    tradingHours: \"09:00-15:00\"\n");
        yaml.append("    decimalPlaces: 0\n");
        yaml.append("    majorCurrency: true\n");
        yaml.append("    centralBank: \"Bank of Japan\"\n");
        yaml.append("    holidays:\n");
        yaml.append("      - \"2025-01-01\"  # New Year's Day\n");
        yaml.append("      - \"2025-05-03\"  # Constitution Day\n");
        yaml.append("      - \"2025-12-23\"  # Emperor's Birthday\n\n");
        
        // Market data section
        yaml.append("# Market and exchange information\n");
        yaml.append("markets:\n");
        
        // NYMEX
        yaml.append("  - exchange: \"NYMEX\"\n");
        yaml.append("    fullName: \"New York Mercantile Exchange\"\n");
        yaml.append("    location: \"New York\"\n");
        yaml.append("    timezone: \"EST\"\n");
        yaml.append("    tradingHours: \"09:00-14:30\"\n");
        yaml.append("    specialties:\n");
        yaml.append("      - \"Energy Commodities\"\n");
        yaml.append("      - \"Natural Gas\"\n");
        yaml.append("      - \"Crude Oil\"\n");
        yaml.append("    currency: \"USD\"\n\n");
        
        // ICE
        yaml.append("  - exchange: \"ICE\"\n");
        yaml.append("    fullName: \"Intercontinental Exchange\"\n");
        yaml.append("    location: \"London/Atlanta\"\n");
        yaml.append("    timezone: \"GMT/EST\"\n");
        yaml.append("    tradingHours: \"08:00-17:00\"\n");
        yaml.append("    specialties:\n");
        yaml.append("      - \"Brent Crude Oil\"\n");
        yaml.append("      - \"Energy Derivatives\"\n");
        yaml.append("      - \"Agricultural Commodities\"\n");
        yaml.append("    currency: \"USD\"\n\n");
        
        // COMEX
        yaml.append("  - exchange: \"COMEX\"\n");
        yaml.append("    fullName: \"Commodity Exchange\"\n");
        yaml.append("    location: \"New York\"\n");
        yaml.append("    timezone: \"EST\"\n");
        yaml.append("    tradingHours: \"08:20-13:30\"\n");
        yaml.append("    specialties:\n");
        yaml.append("      - \"Precious Metals\"\n");
        yaml.append("      - \"Gold\"\n");
        yaml.append("      - \"Silver\"\n");
        yaml.append("      - \"Copper\"\n");
        yaml.append("    currency: \"USD\"\n\n");
        
        // CBOT
        yaml.append("  - exchange: \"CBOT\"\n");
        yaml.append("    fullName: \"Chicago Board of Trade\"\n");
        yaml.append("    location: \"Chicago\"\n");
        yaml.append("    timezone: \"CST\"\n");
        yaml.append("    tradingHours: \"08:30-13:15\"\n");
        yaml.append("    specialties:\n");
        yaml.append("      - \"Agricultural Commodities\"\n");
        yaml.append("      - \"Wheat\"\n");
        yaml.append("      - \"Corn\"\n");
        yaml.append("      - \"Soybeans\"\n");
        yaml.append("    currency: \"USD\"\n");
        
        return yaml.toString();
    }
    
    /**
     * Verifies that the external market data file was created successfully.
     */
    public boolean verifyMarketDataFile() {
        logger.debug("Verifying external market data file...");
        
        try {
            Path marketDataFile = Paths.get("src/main/resources/bootstrap/datasets/market-data.yaml");
            
            if (!Files.exists(marketDataFile)) {
                logger.error("Market data file does not exist: {}", marketDataFile);
                return false;
            }
            
            String content = Files.readString(marketDataFile);
            
            // Verify essential content
            if (!content.contains("currencies:") || 
                !content.contains("USD") || 
                !content.contains("markets:") ||
                !content.contains("NYMEX")) {
                logger.error("Market data file does not contain expected content structure");
                return false;
            }
            
            long fileSize = Files.size(marketDataFile);
            logger.debug("Market data file verified - size: {} bytes", fileSize);
            return true;
            
        } catch (Exception e) {
            logger.error("Market data file verification failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Cleans up the external dataset files.
     */
    public void cleanup() {
        logger.info("Cleaning up external dataset files...");
        
        try {
            Path marketDataFile = Paths.get("src/main/resources/bootstrap/datasets/market-data.yaml");
            if (Files.exists(marketDataFile)) {
                Files.delete(marketDataFile);
                logger.debug("Deleted market data file: {}", marketDataFile);
            }
            
            // Clean up the datasets directory if empty
            Path datasetsDir = Paths.get("src/main/resources/bootstrap/datasets");
            if (Files.exists(datasetsDir) && Files.list(datasetsDir).findAny().isEmpty()) {
                Files.delete(datasetsDir);
                logger.debug("Deleted empty datasets directory");
            }
            
        } catch (Exception e) {
            logger.warn("External dataset cleanup failed: {}", e.getMessage());
        }
    }
}
