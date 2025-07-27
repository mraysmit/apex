package dev.mars.rulesengine.rest.config;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.api.SimpleRulesEngine;
import dev.mars.rulesengine.core.config.yaml.YamlConfigurationLoader;
import dev.mars.rulesengine.core.config.yaml.YamlRuleConfiguration;
import dev.mars.rulesengine.core.engine.config.RulesEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for the Rules Engine REST API.
 * 
 * This configuration sets up the core Rules Engine components as Spring beans,
 * making them available for dependency injection throughout the REST API.
 */
@Configuration
public class RulesEngineConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(RulesEngineConfiguration.class);
    
    @Value("${rules.config.default-path:classpath:rules/default-rules.yaml}")
    private String defaultConfigPath;
    
    @Value("${rules.performance.monitoring.enabled:true}")
    private boolean performanceMonitoringEnabled;
    
    @Value("${rules.error.recovery.enabled:true}")
    private boolean errorRecoveryEnabled;
    
    /**
     * Primary RulesService bean for dependency injection.
     * This provides the instance-based API for rule evaluation.
     */
    @Bean
    @Primary
    public RulesService rulesService() {
        logger.info("Creating primary RulesService bean");
        return new RulesService();
    }
    
    /**
     * SimpleRulesEngine bean for basic rule operations.
     * This provides a simplified API for common use cases.
     */
    @Bean
    public SimpleRulesEngine simpleRulesEngine() {
        logger.info("Creating SimpleRulesEngine bean");
        return new SimpleRulesEngine();
    }
    
    /**
     * YamlConfigurationLoader bean for loading YAML configurations.
     * This is used by the configuration management endpoints.
     */
    @Bean
    public YamlConfigurationLoader yamlConfigurationLoader() {
        logger.info("Creating YamlConfigurationLoader bean");
        return new YamlConfigurationLoader();
    }
    
    /**
     * Default RulesEngine bean loaded from configuration.
     * This provides a pre-configured engine for immediate use.
     */
    @Bean("defaultRulesEngine")
    public RulesEngine defaultRulesEngine(YamlConfigurationLoader loader) {
        try {
            logger.info("Loading default rules configuration from: {}", defaultConfigPath);
            
            // Try to load default configuration
            if (defaultConfigPath.startsWith("classpath:")) {
                String resourcePath = defaultConfigPath.substring("classpath:".length());
                try {
                    YamlRuleConfiguration config = loader.loadFromClasspath(resourcePath);
                    dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration engineConfig = 
                        new dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration();
                    
                    // Configure performance monitoring
                    if (performanceMonitoringEnabled) {
                        logger.info("Performance monitoring enabled");
                        // Performance monitoring configuration would go here
                    }
                    
                    // Configure error recovery
                    if (errorRecoveryEnabled) {
                        logger.info("Error recovery enabled");
                        // Error recovery configuration would go here
                    }
                    
                    return new RulesEngine(engineConfig);
                } catch (Exception e) {
                    logger.warn("Could not load default configuration from {}: {}", defaultConfigPath, e.getMessage());
                    logger.info("Creating default RulesEngine with empty configuration");
                    return new RulesEngine(new dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration());
                }
            } else {
                // File path
                try {
                    YamlRuleConfiguration config = loader.loadFromFile(defaultConfigPath);
                    dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration engineConfig = 
                        new dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration();
                    return new RulesEngine(engineConfig);
                } catch (Exception e) {
                    logger.warn("Could not load default configuration from {}: {}", defaultConfigPath, e.getMessage());
                    logger.info("Creating default RulesEngine with empty configuration");
                    return new RulesEngine(new dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration());
                }
            }
        } catch (Exception e) {
            logger.error("Error creating default RulesEngine: {}", e.getMessage(), e);
            logger.info("Falling back to empty configuration");
            return new RulesEngine(new dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration());
        }
    }
}
