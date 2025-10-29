package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.integration.SequentialProcessingIntegrationService;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;

import java.util.logging.Logger;

/**
 * Enhanced EnrichmentService with sequential processing support.
 * 
 * This service extends the original EnrichmentService to support both
 * STANDARD (backward compatible) and SEQUENTIAL processing modes.
 * 
 * Key Features:
 * - Automatic processing mode detection from YAML metadata
 * - Seamless backward compatibility with existing enrichment code
 * - Support for sequential processing that respects YAML document order
 * - Integration with REST API and other APEX entry points
 * 
 * Processing Mode Selection:
 * - STANDARD: Uses hardcoded processing order (default, backward compatible)
 * - SEQUENTIAL: Processes sections in YAML document order (THE FIX)
 * 
 * Mode is determined by metadata.processing-mode field in YAML:
 * ```yaml
 * metadata:
 *   processing-mode: "sequential"  # or "standard"
 * enrichments:
 *   - id: "customer-lookup"
 *     # ... enrichment configuration
 * rules:
 *   - id: "validate-customer"
 *     # ... rule configuration
 * ```
 * 
 * With sequential mode, enrichments will be processed before rules
 * (respecting the document order), fixing APEX's fundamental design flaw.
 * 
 * @author APEX Sequential Processing Implementation
 * @since Phase 4 - Integration
 */
public class SequentialEnrichmentService {
    
    private static final Logger LOGGER = Logger.getLogger(SequentialEnrichmentService.class.getName());
    
    private final SequentialProcessingIntegrationService integrationService;
    private final EnrichmentService standardService;
    
    public SequentialEnrichmentService(
            EnrichmentService standardService,
            YamlConfigurationLoader configLoader,
            YamlRuleFactory rulesEngineFactory) {
        
        this.standardService = standardService;
        
        // Create integration service for sequential processing
        this.integrationService = new SequentialProcessingIntegrationService(
            configLoader, rulesEngineFactory, standardService
        );
        
        LOGGER.info("SequentialEnrichmentService initialized - APEX design flaw fix active for enrichments");
    }
    
    // ========== ENHANCED ENRICHMENT METHODS ==========
    
    /**
     * Enrich an object using YAML-defined enrichment configurations with sequential processing support.
     * 
     * Automatically detects processing mode from metadata.processing-mode:
     * - "sequential": Uses document order processing (THE FIX)
     * - "standard" or null: Uses hardcoded order (backward compatible)
     *
     * @param yamlConfig The YAML configuration containing enrichments
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object enrichObject(YamlRuleConfiguration yamlConfig, Object targetObject) {
        if (yamlConfig == null) {
            LOGGER.fine("No YAML configuration provided");
            return targetObject;
        }
        
        LOGGER.info("Enriching object with sequential processing support for type: " + 
                   targetObject.getClass().getSimpleName());
        
        try {
            return integrationService.enrichObject(yamlConfig, targetObject);
        } catch (Exception e) {
            LOGGER.warning("Sequential enrichment failed, falling back to standard processing: " + e.getMessage());
            return standardService.enrichObject(yamlConfig, targetObject);
        }
    }
    
    /**
     * Enrich an object using YAML string with sequential processing support.
     * 
     * This method is particularly useful for REST API endpoints and dynamic
     * enrichment scenarios where YAML is provided as a string.
     *
     * @param yamlContent The YAML configuration string
     * @param targetObject The object to enrich
     * @return The enriched object
     * @throws RuntimeException if YAML parsing or processing fails
     */
    public Object enrichObjectFromYaml(String yamlContent, Object targetObject) {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            LOGGER.fine("No YAML content provided");
            return targetObject;
        }
        
        LOGGER.info("Enriching object from YAML string with sequential processing support");
        
        try {
            return integrationService.enrichObjectFromYaml(yamlContent, targetObject);
        } catch (Exception e) {
            LOGGER.warning("Sequential enrichment from YAML failed: " + e.getMessage());
            throw new RuntimeException("Failed to enrich object from YAML", e);
        }
    }
    
    /**
     * Batch enrich multiple objects using the same YAML configuration.
     * 
     * This method is optimized for processing multiple objects with the same
     * enrichment configuration, such as in batch processing scenarios.
     *
     * @param yamlConfig The YAML configuration containing enrichments
     * @param targetObjects Array of objects to enrich
     * @return Array of enriched objects
     */
    public Object[] enrichObjects(YamlRuleConfiguration yamlConfig, Object... targetObjects) {
        if (targetObjects == null || targetObjects.length == 0) {
            LOGGER.fine("No target objects provided for batch enrichment");
            return new Object[0];
        }
        
        LOGGER.info("Batch enriching " + targetObjects.length + " objects with sequential processing support");
        
        Object[] enrichedObjects = new Object[targetObjects.length];
        
        for (int i = 0; i < targetObjects.length; i++) {
            try {
                enrichedObjects[i] = enrichObject(yamlConfig, targetObjects[i]);
            } catch (Exception e) {
                LOGGER.warning("Failed to enrich object at index " + i + ": " + e.getMessage());
                // Continue with original object if enrichment fails
                enrichedObjects[i] = targetObjects[i];
            }
        }
        
        return enrichedObjects;
    }
    
    /**
     * Batch enrich multiple objects using YAML string configuration.
     * 
     * @param yamlContent The YAML configuration string
     * @param targetObjects Array of objects to enrich
     * @return Array of enriched objects
     * @throws RuntimeException if YAML parsing fails
     */
    public Object[] enrichObjectsFromYaml(String yamlContent, Object... targetObjects) {
        if (targetObjects == null || targetObjects.length == 0) {
            LOGGER.fine("No target objects provided for batch enrichment from YAML");
            return new Object[0];
        }
        
        LOGGER.info("Batch enriching " + targetObjects.length + " objects from YAML string with sequential processing support");
        
        Object[] enrichedObjects = new Object[targetObjects.length];
        
        for (int i = 0; i < targetObjects.length; i++) {
            try {
                enrichedObjects[i] = enrichObjectFromYaml(yamlContent, targetObjects[i]);
            } catch (Exception e) {
                LOGGER.warning("Failed to enrich object at index " + i + " from YAML: " + e.getMessage());
                // Continue with original object if enrichment fails
                enrichedObjects[i] = targetObjects[i];
            }
        }
        
        return enrichedObjects;
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * Check if YAML configuration requires sequential processing.
     * 
     * @param yamlConfig YAML configuration to check
     * @return true if sequential processing is required
     */
    public boolean isSequentialMode(YamlRuleConfiguration yamlConfig) {
        if (yamlConfig == null || yamlConfig.getMetadata() == null) {
            return false;
        }
        
        String processingMode = yamlConfig.getMetadata().getProcessingMode();
        return "sequential".equals(processingMode);
    }
    
    /**
     * Get processing mode description for logging and debugging.
     * 
     * @param yamlConfig YAML configuration to analyze
     * @return Processing mode description
     */
    public String getProcessingModeDescription(YamlRuleConfiguration yamlConfig) {
        if (isSequentialMode(yamlConfig)) {
            return "SEQUENTIAL (respects YAML document order - DESIGN FLAW FIXED)";
        } else {
            return "STANDARD (hardcoded order - backward compatible)";
        }
    }
    
    /**
     * Get the underlying standard service for backward compatibility.
     * 
     * @return Standard EnrichmentService
     */
    public EnrichmentService getStandardService() {
        return standardService;
    }
    
    /**
     * Get the integration service for advanced use cases.
     * 
     * @return SequentialProcessingIntegrationService
     */
    public SequentialProcessingIntegrationService getIntegrationService() {
        return integrationService;
    }
    
    /**
     * Validate that enrichment configuration is properly structured.
     * 
     * @param yamlConfig YAML configuration to validate
     * @return true if configuration is valid for enrichment processing
     */
    public boolean validateEnrichmentConfiguration(YamlRuleConfiguration yamlConfig) {
        if (yamlConfig == null) {
            LOGGER.warning("YAML configuration is null");
            return false;
        }
        
        if (yamlConfig.getEnrichments() == null || yamlConfig.getEnrichments().isEmpty()) {
            LOGGER.fine("No enrichments found in YAML configuration");
            return true; // Valid but empty
        }
        
        LOGGER.info("Found " + yamlConfig.getEnrichments().size() + " enrichments in configuration");
        LOGGER.info("Processing mode: " + getProcessingModeDescription(yamlConfig));
        
        return true;
    }
}
