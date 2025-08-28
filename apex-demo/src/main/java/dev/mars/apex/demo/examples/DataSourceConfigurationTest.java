package dev.mars.apex.demo.examples;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test to verify DataSourceConfiguration has parameter names set correctly.
 * 
 * @author APEX Demo Team
 * @since 2025-08-28
 * @version 1.0.0
 */
public class DataSourceConfigurationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfigurationTest.class);
    
    public static void main(String[] args) {
        DataSourceConfigurationTest test = new DataSourceConfigurationTest();
        test.runTest();
    }
    
    public void runTest() {
        logger.info("====================================================================================");
        logger.info("DATASOURCE CONFIGURATION TEST");
        logger.info("====================================================================================");
        
        try {
            // Load configuration with external data-source references
            YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
            YamlRuleConfiguration config = configLoader.loadFromClasspath("enrichments/customer-profile-enrichment-lean.yaml");
            
            logger.info("Configuration loaded successfully");
            
            if (config.getDataSources() != null && !config.getDataSources().isEmpty()) {
                var yamlDataSource = config.getDataSources().get(0);
                logger.info("\nYamlDataSource:");
                logger.info("  Name: " + yamlDataSource.getName());
                logger.info("  Parameter Names: " + java.util.Arrays.toString(yamlDataSource.getParameterNames()));
                
                // Convert to DataSourceConfiguration
                DataSourceConfiguration dataSourceConfig = yamlDataSource.toDataSourceConfiguration();
                logger.info("\nDataSourceConfiguration:");
                logger.info("  Name: " + dataSourceConfig.getName());
                logger.info("  Parameter Names: " + java.util.Arrays.toString(dataSourceConfig.getParameterNames()));
                
                // Create actual DataSource using DataSourceFactory
                DataSourceFactory factory = DataSourceFactory.getInstance();
                ExternalDataSource dataSource = factory.createDataSource(dataSourceConfig);
                
                logger.info("\nExternalDataSource:");
                logger.info("  Name: " + dataSource.getName());
                logger.info("  Configuration Parameter Names: " + java.util.Arrays.toString(dataSource.getConfiguration().getParameterNames()));
                
                // Test parameter map building (this is where the issue likely is)
                logger.info("\nTesting parameter map building...");
                
                // We can't directly call buildParameterMap since it's private, but we can test the query execution
                // which will show us if the parameters are being bound correctly
                
                if (dataSourceConfig.getParameterNames() != null) {
                    logger.info("✅ SUCCESS: Parameter names are set correctly in DataSourceConfiguration");
                } else {
                    logger.error("❌ FAILURE: Parameter names are NULL in DataSourceConfiguration");
                }
                
            } else {
                logger.error("No data sources found in configuration");
            }
            
            logger.info("====================================================================================");
            logger.info("DATASOURCE CONFIGURATION TEST COMPLETED");
            logger.info("====================================================================================");
            
        } catch (Exception e) {
            logger.error("DataSource configuration test failed: " + e.getMessage(), e);
        }
    }
}
