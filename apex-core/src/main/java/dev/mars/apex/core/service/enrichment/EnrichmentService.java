package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;

import java.util.List;
import java.util.logging.Logger;

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
 * Service for managing and executing enrichment operations.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class EnrichmentService {
    
    private static final Logger LOGGER = Logger.getLogger(EnrichmentService.class.getName());
    
    private final YamlEnrichmentProcessor processor;
    private final LookupServiceRegistry serviceRegistry;
    
    public EnrichmentService(LookupServiceRegistry serviceRegistry, 
                           ExpressionEvaluatorService evaluatorService) {
        this.serviceRegistry = serviceRegistry;
        this.processor = new YamlEnrichmentProcessor(serviceRegistry, evaluatorService);
        
        LOGGER.info("EnrichmentService initialized");
    }
    
    /**
     * Enrich an object using YAML-defined enrichment configurations.
     * 
     * @param yamlConfig The YAML configuration containing enrichments
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object enrichObject(YamlRuleConfiguration yamlConfig, Object targetObject) {
        if (yamlConfig == null || yamlConfig.getEnrichments() == null) {
            LOGGER.fine("No enrichments found in YAML configuration");
            return targetObject;
        }
        
        return processor.processEnrichments(yamlConfig.getEnrichments(), targetObject);
    }
    
    /**
     * Enrich an object using a specific list of enrichments.
     * 
     * @param enrichments The list of enrichment configurations
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object enrichObject(List<YamlEnrichment> enrichments, Object targetObject) {
        return processor.processEnrichments(enrichments, targetObject);
    }
    
    /**
     * Enrich an object using a single enrichment configuration.
     * 
     * @param enrichment The enrichment configuration
     * @param targetObject The object to enrich
     * @return The enriched object
     */
    public Object enrichObject(YamlEnrichment enrichment, Object targetObject) {
        return processor.processEnrichment(enrichment, targetObject);
    }
    
    /**
     * Get the lookup service registry.
     * 
     * @return The service registry
     */
    public LookupServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
    
    /**
     * Get the enrichment processor.
     * 
     * @return The processor
     */
    public YamlEnrichmentProcessor getProcessor() {
        return processor;
    }
    
    /**
     * Clear all caches.
     */
    public void clearCaches() {
        processor.clearCache();
        LOGGER.info("All enrichment caches cleared");
    }
    
    /**
     * Get enrichment statistics.
     * 
     * @return Statistics map
     */
    public java.util.Map<String, Object> getStatistics() {
        return processor.getCacheStatistics();
    }
}
