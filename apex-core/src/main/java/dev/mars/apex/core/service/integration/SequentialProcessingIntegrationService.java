package dev.mars.apex.core.service.integration;

import dev.mars.apex.core.config.yaml.*;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.error.ErrorRecoveryService;
import dev.mars.apex.core.service.monitoring.RulePerformanceMonitor;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Integration service that bridges existing APEX services with sequential processing.
 * 
 * This service provides a unified interface for processing YAML configurations
 * using either STANDARD (backward compatible) or SEQUENTIAL processing modes.
 * 
 * Key Features:
 * - Automatic processing mode detection from YAML metadata
 * - Seamless integration with existing APEX services
 * - Backward compatibility with all existing functionality
 * - Support for all APEX entry points (files, streams, strings)
 * 
 * Processing Flow:
 * 1. Parse YAML with OrderedYamlParser to preserve section order
 * 2. Detect processing mode from metadata.processing-mode
 * 3. Route to appropriate processor (standard or sequential)
 * 4. Return results compatible with existing APEX APIs
 * 
 * @author APEX Sequential Processing Implementation
 * @since Phase 4 - Integration
 */
public class SequentialProcessingIntegrationService {
    
    private static final Logger LOGGER = Logger.getLogger(SequentialProcessingIntegrationService.class.getName());
    
    private final YamlConfigurationLoader standardConfigLoader;
    private final OrderedYamlParser orderedParser;
    private final SequentialYamlProcessor sequentialProcessor;
    private final YamlRuleFactory rulesEngineFactory;
    private final EnrichmentService enrichmentService;

    public SequentialProcessingIntegrationService(
            YamlConfigurationLoader standardConfigLoader,
            YamlRuleFactory rulesEngineFactory,
            EnrichmentService enrichmentService) {

        this.standardConfigLoader = standardConfigLoader;
        this.orderedParser = new OrderedYamlParser();
        this.sequentialProcessor = new SequentialYamlProcessor();
        this.rulesEngineFactory = rulesEngineFactory;
        this.enrichmentService = enrichmentService;
        
        LOGGER.info("SequentialProcessingIntegrationService initialized - APEX design flaw fix active");
    }
    
    // ========== RULES ENGINE CREATION METHODS ==========
    
    /**
     * Create rules engine from YAML file with automatic processing mode detection.
     * 
     * @param file YAML configuration file
     * @return Configured RulesEngine
     * @throws YamlConfigurationException if processing fails
     */
    public RulesEngine createRulesEngineFromFile(File file) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine from file with sequential processing support: " + file.getAbsolutePath());
        
        // Parse with order preservation
        OrderedYamlConfiguration orderedConfig = orderedParser.parseFile(file.getAbsolutePath());
        
        // Check processing mode
        if (orderedConfig.getProcessingMode() == OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL) {
            return createRulesEngineSequentially(orderedConfig, file.getAbsolutePath());
        } else {
            // Use standard processing for backward compatibility
            return createRulesEngineStandard(file);
        }
    }
    
    /**
     * Create rules engine from input stream with automatic processing mode detection.
     * 
     * @param inputStream YAML configuration stream
     * @return Configured RulesEngine
     * @throws YamlConfigurationException if processing fails
     */
    public RulesEngine createRulesEngineFromStream(InputStream inputStream) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine from stream with sequential processing support");
        
        // For streams, we need to read content first
        try {
            String yamlContent = new String(inputStream.readAllBytes());
            return createRulesEngineFromString(yamlContent, "InputStream");
        } catch (Exception e) {
            throw new YamlConfigurationException("Failed to read YAML content from stream", e);
        }
    }
    
    /**
     * Create rules engine from YAML string with automatic processing mode detection.
     * 
     * @param yamlString YAML configuration string
     * @return Configured RulesEngine
     * @throws YamlConfigurationException if processing fails
     */
    public RulesEngine createRulesEngineFromString(String yamlString) throws YamlConfigurationException {
        return createRulesEngineFromString(yamlString, "YamlString");
    }
    
    /**
     * Create rules engine from YAML string with source identification.
     */
    private RulesEngine createRulesEngineFromString(String yamlString, String source) throws YamlConfigurationException {
        LOGGER.info("Creating rules engine from string with sequential processing support: " + source);
        
        // Parse with order preservation
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(yamlString, source);
        
        // Check processing mode
        if (orderedConfig.getProcessingMode() == OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL) {
            return createRulesEngineSequentially(orderedConfig, source);
        } else {
            // Use standard processing for backward compatibility
            return createRulesEngineStandard(yamlString);
        }
    }
    
    // ========== ENRICHMENT PROCESSING METHODS ==========
    
    /**
     * Enrich object using YAML configuration with automatic processing mode detection.
     * 
     * @param yamlConfig YAML configuration
     * @param targetObject Object to enrich
     * @return Enriched object
     */
    public Object enrichObject(YamlRuleConfiguration yamlConfig, Object targetObject) {
        // Check if we need sequential processing
        if (isSequentialMode(yamlConfig)) {
            return enrichObjectSequentially(yamlConfig, targetObject);
        } else {
            // Use standard enrichment service
            return enrichmentService.enrichObject(yamlConfig, targetObject);
        }
    }
    
    /**
     * Enrich object using YAML string with automatic processing mode detection.
     * 
     * @param yamlContent YAML configuration string
     * @param targetObject Object to enrich
     * @return Enriched object
     * @throws YamlConfigurationException if processing fails
     */
    public Object enrichObjectFromYaml(String yamlContent, Object targetObject) throws YamlConfigurationException {
        // Parse with order preservation
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(yamlContent, "EnrichmentRequest");
        
        // Check processing mode
        if (orderedConfig.getProcessingMode() == OrderedYamlConfiguration.ProcessingMode.SEQUENTIAL) {
            return enrichObjectSequentially(orderedConfig, targetObject);
        } else {
            // Use standard processing
            YamlRuleConfiguration standardConfig = standardConfigLoader.fromYamlString(yamlContent);
            return enrichmentService.enrichObject(standardConfig, targetObject);
        }
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    /**
     * Create rules engine using sequential processing.
     */
    private RulesEngine createRulesEngineSequentially(OrderedYamlConfiguration orderedConfig, String source) 
            throws YamlConfigurationException {
        
        LOGGER.info("Creating rules engine with SEQUENTIAL processing for: " + source);
        
        // Process with sequential processor
        SequentialProcessingResult result = sequentialProcessor.processOrderedConfiguration(orderedConfig, source);
        
        // Create rules engine configuration from processed result
        YamlRuleConfiguration processedConfig = result.getYamlRuleConfiguration();
        RulesEngineConfiguration engineConfig = rulesEngineFactory.createRulesEngineConfiguration(processedConfig);
        
        // Create EnrichmentService for safe RulesEngine creation
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        EnrichmentService enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        RulesEngine engine = new RulesEngine(engineConfig, new SpelExpressionParser(), new ErrorRecoveryService(),
                                           new RulePerformanceMonitor(), enrichmentService);
        
        LOGGER.info("Successfully created rules engine with sequential processing for: " + source);
        return engine;
    }
    
    /**
     * Create rules engine using standard processing (backward compatibility).
     */
    private RulesEngine createRulesEngineStandard(File file) throws YamlConfigurationException {
        YamlRuleConfiguration yamlConfig = standardConfigLoader.loadFromFile(file);
        RulesEngineConfiguration config = rulesEngineFactory.createRulesEngineConfiguration(yamlConfig);
        // Create EnrichmentService for safe RulesEngine creation
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        EnrichmentService enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        return new RulesEngine(config, new SpelExpressionParser(), new ErrorRecoveryService(),
                             new RulePerformanceMonitor(), enrichmentService);
    }
    
    /**
     * Create rules engine using standard processing from string.
     */
    private RulesEngine createRulesEngineStandard(String yamlString) throws YamlConfigurationException {
        YamlRuleConfiguration yamlConfig = standardConfigLoader.fromYamlString(yamlString);
        RulesEngineConfiguration config = rulesEngineFactory.createRulesEngineConfiguration(yamlConfig);
        // Create EnrichmentService for safe RulesEngine creation
        LookupServiceRegistry serviceRegistry = new LookupServiceRegistry();
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        EnrichmentService enrichmentService = new EnrichmentService(serviceRegistry, evaluatorService);

        return new RulesEngine(config, new SpelExpressionParser(), new ErrorRecoveryService(),
                             new RulePerformanceMonitor(), enrichmentService);
    }
    
    /**
     * Enrich object using sequential processing.
     */
    private Object enrichObjectSequentially(YamlRuleConfiguration yamlConfig, Object targetObject) {
        // TODO: Implement sequential enrichment processing
        // For now, delegate to standard enrichment service
        LOGGER.info("Sequential enrichment processing requested - using standard processing for now");
        return enrichmentService.enrichObject(yamlConfig, targetObject);
    }
    
    /**
     * Enrich object using sequential processing with OrderedYamlConfiguration.
     */
    private Object enrichObjectSequentially(OrderedYamlConfiguration orderedConfig, Object targetObject) 
            throws YamlConfigurationException {
        
        LOGGER.info("Enriching object with SEQUENTIAL processing");
        
        // Process with sequential processor
        SequentialProcessingResult result = sequentialProcessor.processOrderedConfiguration(orderedConfig, "EnrichmentRequest");
        
        // Use processed configuration for enrichment
        YamlRuleConfiguration processedConfig = result.getYamlRuleConfiguration();
        return enrichmentService.enrichObject(processedConfig, targetObject);
    }
    
    /**
     * Check if YAML configuration requires sequential processing.
     */
    private boolean isSequentialMode(YamlRuleConfiguration yamlConfig) {
        if (yamlConfig.getMetadata() == null) {
            return false;
        }
        
        String processingMode = yamlConfig.getMetadata().getProcessingMode();
        return "sequential".equals(processingMode);
    }
}
