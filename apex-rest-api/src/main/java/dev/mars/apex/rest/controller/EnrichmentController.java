package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;

/**
 * REST Controller for data enrichment operations.
 * Provides endpoints for enriching data using the APEX enrichment engine.
 */
@RestController
@RequestMapping("/api/enrichment")
@Tag(name = "Enrichment", description = "Data enrichment operations")
public class EnrichmentController {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentController.class);

    @Autowired
    private EnrichmentService enrichmentService;

    @Autowired
    private YamlConfigurationLoader yamlConfigurationLoader;

    /**
     * Get predefined enrichment configurations.
     */
    @GetMapping("/configurations")
    @Operation(
        summary = "Get predefined configurations",
        description = "Returns a list of predefined enrichment configurations available for use."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Configurations retrieved successfully")
    public ResponseEntity<Map<String, Object>> getPredefinedConfigurations() {
        logger.info("Retrieving predefined enrichment configurations");

        try {
            // In a real implementation, this would load from a configuration registry
            // For now, return a sample list of available configurations
            List<String> configurations = Arrays.asList(
                "customer-profile",
                "trade-enrichment",
                "risk-assessment",
                "market-data-enrichment"
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configurations", configurations);
            response.put("count", configurations.size());
            response.put("timestamp", Instant.now());

            logger.info("Retrieved {} predefined configurations", configurations.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving configurations: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Configuration retrieval failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Enrich an object using YAML configuration.
     */
    @PostMapping("/enrich")
    @Operation(
        summary = "Enrich object using YAML configuration",
        description = "Enriches the provided object using enrichment rules defined in YAML configuration."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Object enriched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid enrichment request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Enrichment error")
    })
    public ResponseEntity<Map<String, Object>> enrichObject(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Enrichment request with YAML configuration and target object",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Customer enrichment",
                        value = """
                        {
                          "yamlConfiguration": "metadata:\\n  name: \\"Customer Enrichment\\"\\n  version: \\"1.0.0\\"\\n\\nenrichments:\\n  - name: \\"customer-profile\\"\\n    condition: \\"#customerId != null\\"\\n    enrichmentType: \\"lookup\\"\\n    sourceField: \\"customerId\\"\\n    targetFields:\\n      - \\"customerName\\"\\n      - \\"customerTier\\"\\n    lookupService: \\"customerLookup\\"",
                          "targetObject": {
                            "customerId": "CUST001",
                            "transactionAmount": 1000.0,
                            "currency": "USD"
                          }
                        }
                        """
                    )
                )
            )
            @Valid @NotNull EnrichmentRequest request) {

        logger.info("Enriching object using YAML configuration");
        logger.debug("Target object: {}", request.getTargetObject());

        try {
            // Parse YAML configuration
            YamlRuleConfiguration yamlConfig = yamlConfigurationLoader.loadFromStream(
                new ByteArrayInputStream(request.getYamlConfiguration().getBytes())
            );

            // Enrich the object
            Object enrichedObject = enrichmentService.enrichObject(yamlConfig, request.getTargetObject());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("originalObject", request.getTargetObject());
            response.put("enrichedObject", enrichedObject);
            response.put("enrichmentCount", yamlConfig.getEnrichments() != null ? yamlConfig.getEnrichments().size() : 0);
            response.put("timestamp", Instant.now());

            if (yamlConfig.getMetadata() != null) {
                response.put("configurationName", yamlConfig.getMetadata().getName());
                response.put("configurationVersion", yamlConfig.getMetadata().getVersion());
            }

            logger.info("Object enrichment completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during object enrichment: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Object enrichment failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Enrich multiple objects using YAML configuration.
     */
    @PostMapping("/batch")
    @Operation(
        summary = "Enrich multiple objects using YAML configuration",
        description = "Enriches multiple objects using enrichment rules defined in YAML configuration."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Batch enrichment completed")
    public ResponseEntity<Map<String, Object>> enrichBatch(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Batch enrichment request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Batch customer enrichment",
                        value = """
                        {
                          "yamlConfiguration": "metadata:\\n  name: \\"Batch Customer Enrichment\\"\\n\\nenrichments:\\n  - name: \\"customer-profile\\"\\n    condition: \\"#customerId != null\\"\\n    enrichmentType: \\"lookup\\"\\n    sourceField: \\"customerId\\"\\n    targetFields: [\\"customerName\\", \\"customerTier\\"]",
                          "targetObjects": [
                            {"customerId": "CUST001", "amount": 1000.0},
                            {"customerId": "CUST002", "amount": 2500.0},
                            {"customerId": "CUST003", "amount": 750.0}
                          ]
                        }
                        """
                    )
                )
            )
            @Valid @NotNull BatchEnrichmentRequest request) {

        logger.info("Enriching {} objects using YAML configuration", request.getTargetObjects().size());

        try {
            // Parse YAML configuration
            YamlRuleConfiguration yamlConfig = yamlConfigurationLoader.loadFromStream(
                new ByteArrayInputStream(request.getYamlConfiguration().getBytes())
            );

            // Enrich all objects
            List<Object> enrichedObjects = new ArrayList<>();
            for (Object targetObject : request.getTargetObjects()) {
                Object enrichedObject = enrichmentService.enrichObject(yamlConfig, targetObject);
                enrichedObjects.add(enrichedObject);
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("originalObjects", request.getTargetObjects());
            response.put("enrichedObjects", enrichedObjects);
            response.put("processedCount", enrichedObjects.size());
            response.put("enrichmentCount", yamlConfig.getEnrichments() != null ? yamlConfig.getEnrichments().size() : 0);
            response.put("timestamp", Instant.now());

            if (yamlConfig.getMetadata() != null) {
                response.put("configurationName", yamlConfig.getMetadata().getName());
                response.put("configurationVersion", yamlConfig.getMetadata().getVersion());
            }

            logger.info("Batch enrichment completed successfully for {} objects", enrichedObjects.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during batch enrichment: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Batch enrichment failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Enrich object using predefined enrichment configuration.
     */
    @PostMapping("/predefined/{configName}")
    @Operation(
        summary = "Enrich object using predefined configuration",
        description = "Enriches the provided object using a predefined enrichment configuration."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Object enriched using predefined configuration")
    public ResponseEntity<Map<String, Object>> enrichWithPredefinedConfig(
            @Parameter(description = "Name of the predefined enrichment configuration")
            @PathVariable String configName,

            @RequestBody @Valid @NotNull Object targetObject) {

        logger.info("Enriching object using predefined configuration: {}", configName);

        try {
            // For now, we'll simulate predefined configurations
            // In a real implementation, these would be stored and retrieved from a configuration store
            Map<String, String> predefinedConfigs = getSimulatedPredefinedConfigurations();
            
            String yamlConfig = predefinedConfigs.get(configName);
            if (yamlConfig == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Predefined configuration not found");
                errorResponse.put("configName", configName);
                errorResponse.put("availableConfigs", predefinedConfigs.keySet());
                errorResponse.put("timestamp", Instant.now());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Parse and apply configuration
            YamlRuleConfiguration config = yamlConfigurationLoader.loadFromStream(
                new ByteArrayInputStream(yamlConfig.getBytes())
            );

            Object enrichedObject = enrichmentService.enrichObject(config, targetObject);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configName", configName);
            response.put("originalObject", targetObject);
            response.put("enrichedObject", enrichedObject);
            response.put("timestamp", Instant.now());

            logger.info("Predefined enrichment completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during predefined enrichment: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Predefined enrichment failed");
            errorResponse.put("configName", configName);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }



    /**
     * Get predefined configurations (simulated for demo purposes).
     */
    private Map<String, String> getSimulatedPredefinedConfigurations() {
        Map<String, String> configs = new HashMap<>();
        
        configs.put("customer-profile", """
            metadata:
              name: "Customer Profile Enrichment"
              version: "1.0.0"
            
            enrichments:
              - name: "customer-lookup"
                condition: "#customerId != null"
                enrichmentType: "lookup"
                sourceField: "customerId"
                targetFields:
                  - "customerName"
                  - "customerTier"
                  - "riskRating"
                lookupService: "customerLookup"
            """);

        configs.put("trade-enrichment", """
            metadata:
              name: "Trade Data Enrichment"
              version: "1.0.0"
            
            enrichments:
              - name: "instrument-lookup"
                condition: "#instrumentId != null"
                enrichmentType: "lookup"
                sourceField: "instrumentId"
                targetFields:
                  - "instrumentName"
                  - "instrumentType"
                  - "currency"
                lookupService: "instrumentLookup"
              - name: "counterparty-lookup"
                condition: "#counterpartyId != null"
                enrichmentType: "lookup"
                sourceField: "counterpartyId"
                targetFields:
                  - "counterpartyName"
                  - "counterpartyRating"
                lookupService: "counterpartyLookup"
            """);

        return configs;
    }

    // DTOs for request/response
    public static class EnrichmentRequest {
        @NotNull
        private String yamlConfiguration;
        
        @NotNull
        private Object targetObject;

        // Getters and setters
        public String getYamlConfiguration() { return yamlConfiguration; }
        public void setYamlConfiguration(String yamlConfiguration) { this.yamlConfiguration = yamlConfiguration; }
        public Object getTargetObject() { return targetObject; }
        public void setTargetObject(Object targetObject) { this.targetObject = targetObject; }
    }

    public static class BatchEnrichmentRequest {
        @NotNull
        private String yamlConfiguration;
        
        @NotNull
        private List<Object> targetObjects;

        // Getters and setters
        public String getYamlConfiguration() { return yamlConfiguration; }
        public void setYamlConfiguration(String yamlConfiguration) { this.yamlConfiguration = yamlConfiguration; }
        public List<Object> getTargetObjects() { return targetObjects; }
        public void setTargetObjects(List<Object> targetObjects) { this.targetObjects = targetObjects; }
    }
}
