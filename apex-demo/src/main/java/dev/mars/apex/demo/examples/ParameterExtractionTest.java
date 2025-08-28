package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to verify parameter extraction from external data-source configurations.
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class ParameterExtractionTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ParameterExtractionTest.class);
    
    public static void main(String[] args) {
        ParameterExtractionTest test = new ParameterExtractionTest();
        test.runTest();
    }
    
    public void runTest() {
        logger.info("====================================================================================");
        logger.info("PARAMETER EXTRACTION TEST");
        logger.info("====================================================================================");
        
        try {
            // Load configuration with external data-source references
            YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/customer-profile-enrichment-lean.yaml");
            
            logger.info("Configuration loaded successfully:");
            logger.info("  Name: " + config.getMetadata().getName());
            logger.info("  Data Sources: " + (config.getDataSources() != null ? config.getDataSources().size() : 0));
            
            if (config.getDataSources() != null && !config.getDataSources().isEmpty()) {
                var dataSource = config.getDataSources().get(0);
                logger.info("\nFirst Data Source:");
                logger.info("  Name: " + dataSource.getName());
                logger.info("  Type: " + dataSource.getType());
                logger.info("  Source Type: " + dataSource.getSourceType());
                
                // Check parameter names
                String[] paramNames = dataSource.getParameterNames();
                if (paramNames != null) {
                    logger.info("  Parameter Names: " + java.util.Arrays.toString(paramNames));
                } else {
                    logger.warn("  Parameter Names: NULL - This is the problem!");
                }
                
                // Check queries
                if (dataSource.getQueries() != null) {
                    logger.info("  Queries: " + dataSource.getQueries().size());
                    for (var entry : dataSource.getQueries().entrySet()) {
                        logger.info("    " + entry.getKey() + ": " + entry.getValue().substring(0, Math.min(50, entry.getValue().length())) + "...");
                    }
                } else {
                    logger.warn("  Queries: NULL");
                }
                
                // Check connection
                if (dataSource.getConnection() != null) {
                    var connection = dataSource.getConnection();
                    logger.info("  Connection: " + connection.toString());
                } else {
                    logger.warn("  Connection: NULL");
                }
            }
            
            logger.info("====================================================================================");
            logger.info("PARAMETER EXTRACTION TEST COMPLETED");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("Parameter extraction test failed: " + e.getMessage(), e);
        }
    }
}
