package dev.mars.apex.rest.config;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.api.SimpleRulesEngine;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.engine.TemplateProcessorService;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.expression.ExpressionEvaluationService;
import dev.mars.apex.core.service.transform.GenericTransformerService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.data.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

/**
 * Spring configuration for the Rules Engine REST API.
 *

 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@Configuration
public class RulesEngineConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(RulesEngineConfiguration.class);

    @Autowired
    private RulesConfigurationProperties rulesProperties;
    
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
            // Create base engine configuration
            dev.mars.apex.core.engine.config.RulesEngineConfiguration engineConfig =
                new dev.mars.apex.core.engine.config.RulesEngineConfiguration();

            // Configure performance monitoring
            if (rulesProperties.getPerformance().getMonitoring().isEnabled()) {
                logger.info("Performance monitoring enabled");
                // Performance monitoring configuration would go here
            }

            // Configure error recovery
            if (rulesProperties.getError().getRecovery().isEnabled()) {
                logger.info("Error recovery enabled");
                // Error recovery configuration would go here
            }

            // Try to load rules configuration if path is specified
            String rulesConfigPath = rulesProperties.getConfig().getPath();
            if (rulesConfigPath != null && !rulesConfigPath.trim().isEmpty()) {
                logger.info("Loading rules configuration from: {}", rulesConfigPath);

                try {
                    if (rulesConfigPath.startsWith("classpath:")) {
                        String resourcePath = rulesConfigPath.substring("classpath:".length());
                        loader.loadFromClasspath(resourcePath);
                        logger.info("Successfully loaded rules configuration from classpath: {}", resourcePath);
                    } else {
                        loader.loadFromFile(rulesConfigPath);
                        logger.info("Successfully loaded rules configuration from file: {}", rulesConfigPath);
                    }
                } catch (Exception e) {
                    logger.warn("Could not load rules configuration from {}: {}", rulesConfigPath, e.getMessage());
                    logger.info("Continuing with empty rules configuration");
                }
            } else {
                logger.info("No rules configuration path specified, creating engine with empty configuration");
            }

            return new RulesEngine(engineConfig);
        } catch (Exception e) {
            logger.error("Error creating RulesEngine: {}", e.getMessage(), e);
            logger.info("Falling back to empty configuration");
            return new RulesEngine(new dev.mars.apex.core.engine.config.RulesEngineConfiguration());
        }
    }

    /**
     * LookupServiceRegistry bean for service registration and lookup.
     */
    @Bean
    public LookupServiceRegistry lookupServiceRegistry() {
        logger.info("Creating LookupServiceRegistry bean");
        return new LookupServiceRegistry();
    }

    /**
     * ExpressionEvaluatorService bean for SpEL expression evaluation.
     */
    @Bean
    public ExpressionEvaluatorService expressionEvaluatorService() {
        logger.info("Creating ExpressionEvaluatorService bean");
        return new ExpressionEvaluatorService();
    }

    /**
     * ExpressionEvaluationService bean for enhanced SpEL expression evaluation.
     * This provides a higher-level API for expression evaluation with Map-based contexts.
     */
    @Bean
    public ExpressionEvaluationService expressionEvaluationService() {
        logger.info("Creating ExpressionEvaluationService bean");
        return new ExpressionEvaluationService();
    }

    /**
     * DataServiceManager bean for data source management.
     * Creates a DataServiceManager with test data sources for REST API operations.
     */
    @Bean
    public DataServiceManager dataServiceManager() {
        logger.info("Creating DataServiceManager bean with test data sources");
        DataServiceManager manager = new DataServiceManager();

        // Add a test data source for integration tests
        manager.loadDataSource(new TestDataSource("testDataSource", "testData"));

        return manager;
    }

    /**
     * GenericTransformerService bean for data transformation operations.
     */
    @Bean
    public GenericTransformerService genericTransformerService(
            LookupServiceRegistry lookupServiceRegistry,
            RulesEngine rulesEngine) {
        logger.info("Creating GenericTransformerService bean");
        return new GenericTransformerService(lookupServiceRegistry, rulesEngine);
    }

    /**
     * EnrichmentService bean for data enrichment operations.
     */
    @Bean
    public EnrichmentService enrichmentService(
            LookupServiceRegistry lookupServiceRegistry,
            ExpressionEvaluatorService expressionEvaluatorService) {
        logger.info("Creating EnrichmentService bean");
        return new EnrichmentService(lookupServiceRegistry, expressionEvaluatorService);
    }

    /**
     * TemplateProcessorService bean for template processing operations.
     */
    @Bean
    public TemplateProcessorService templateProcessorService(
            ExpressionEvaluatorService expressionEvaluatorService) {
        logger.info("Creating TemplateProcessorService bean");
        return new TemplateProcessorService(expressionEvaluatorService);
    }

    /**
     * Simple test data source for integration testing.
     */
    private static class TestDataSource implements DataSource {
        private final String name;
        private final String dataType;

        public TestDataSource(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDataType() {
            return dataType;
        }

        @Override
        public boolean supportsDataType(String dataType) {
            return this.dataType.equals(dataType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getData(String dataType, Object... parameters) {
            if (!supportsDataType(dataType)) {
                return null;
            }

            // Return test data based on the key parameter
            if (parameters.length > 0 && parameters[0] instanceof String) {
                String key = (String) parameters[0];
                return (T) Map.of(
                    "key", key,
                    "value", "Test data for key: " + key,
                    "timestamp", Instant.now(),
                    "source", name
                );
            }

            return (T) Map.of(
                "message", "Test data from " + name,
                "timestamp", Instant.now()
            );
        }
    }
}
