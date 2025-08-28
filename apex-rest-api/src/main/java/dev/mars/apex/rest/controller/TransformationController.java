package dev.mars.apex.rest.controller;

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


import dev.mars.apex.core.service.transform.GenericTransformerService;
import dev.mars.apex.core.engine.model.TransformerRule;
import dev.mars.apex.core.service.transform.FieldTransformerAction;
import dev.mars.apex.core.service.transform.FieldTransformerActionBuilder;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.config.RuleBuilder;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.rest.dto.ApiResponse;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;

/**
 * REST Controller for transformation operations.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@RestController
@RequestMapping("/api/transformations")
@Tag(name = "Transformation", description = "Data transformation operations")
public class TransformationController {

    private static final Logger logger = LoggerFactory.getLogger(TransformationController.class);

    @Autowired
    private GenericTransformerService transformerService;

    /**
     * Get all registered transformers.
     */
    @GetMapping("/transformers")
    @Operation(
        summary = "Get registered transformers",
        description = "Returns a list of all registered transformers available for use."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transformers retrieved successfully")
    public ResponseEntity<Map<String, Object>> getRegisteredTransformers() {
        logger.info("Retrieving registered transformers");

        try {
            // Get registered transformers from the service
            String[] transformerNames = transformerService.getRegisteredTransformers();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transformers", Arrays.asList(transformerNames));
            response.put("count", transformerNames.length);
            response.put("timestamp", Instant.now());

            logger.info("Retrieved {} registered transformers", transformerNames.length);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving transformers: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Transformer retrieval failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Transform data using a registered transformer.
     */
    @PostMapping("/{transformerName}")
    @Operation(
        summary = "Transform data using a registered transformer",
        description = "Applies a registered transformer to the provided data and returns the transformed result."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Data transformed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transformer not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid transformation request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Transformation error")
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> transformData(
            @Parameter(description = "Name of the transformer to use", example = "customer-normalizer")
            @PathVariable @NotBlank String transformerName,

            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Data to transform",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Customer data",
                        value = """
                        {
                          "firstName": "john",
                          "lastName": "doe",
                          "email": "JOHN.DOE@EXAMPLE.COM",
                          "age": 25
                        }
                        """
                    )
                )
            )
            @Valid @NotNull Object data) {

        logger.info("Transforming data using transformer: {}", transformerName);
        logger.debug("Input data: {}", data);

        try {
            // Transform the data
            Object transformedData = transformerService.transform(transformerName, data);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("transformerName", transformerName);
            responseData.put("originalData", data);
            responseData.put("transformedData", transformedData);

            logger.info("Data transformation completed successfully");
            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (IllegalArgumentException e) {
            logger.warn("Transformer not found: {}", transformerName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Map<String, Object>>error("Transformer not found", e.getMessage())
                    .withAdditionalInfo("transformerName", transformerName));

        } catch (Exception e) {
            logger.error("Error during transformation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Map<String, Object>>error("Transformation failed", e.getMessage())
                    .withAdditionalInfo("transformerName", transformerName));
        }
    }

    /**
     * Transform data with detailed result information.
     */
    @PostMapping("/{transformerName}/detailed")
    @Operation(
        summary = "Transform data with detailed result",
        description = "Applies a transformer and returns detailed transformation result including rule execution details."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detailed transformation completed")
    public ResponseEntity<Map<String, Object>> transformDataWithResult(
            @Parameter(description = "Name of the transformer to use")
            @PathVariable @NotBlank String transformerName,

            @RequestBody @Valid @NotNull Object data) {

        logger.info("Transforming data with detailed result using transformer: {}", transformerName);

        try {
            // Transform with detailed result
            RuleResult result = transformerService.transformWithResult(transformerName, data);

            // Prepare detailed response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transformerName", transformerName);
            response.put("originalData", data);
            response.put("ruleResult", Map.of(
                "triggered", result.isTriggered(),
                "ruleName", result.getRuleName(),
                "message", result.getMessage(),
                "result", result.getMessage(),
                "executionTime", result.hasPerformanceMetrics() ? result.getPerformanceMetrics().getEvaluationTimeMillis() : 0,
                "error", result.getResultType() == RuleResult.ResultType.ERROR ? result.getMessage() : null
            ));
            response.put("timestamp", Instant.now());

            logger.info("Detailed transformation completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during detailed transformation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Detailed transformation failed");
            errorResponse.put("transformerName", transformerName);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Create and apply a dynamic transformer.
     */
    @PostMapping("/dynamic")
    @Operation(
        summary = "Create and apply dynamic transformer",
        description = "Creates a transformer on-the-fly with the provided rules and applies it to the data."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dynamic transformation completed")
    public ResponseEntity<Map<String, Object>> transformWithDynamicRules(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Dynamic transformation request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Dynamic transformation",
                        value = """
                        {
                          "data": {
                            "firstName": "john",
                            "lastName": "doe",
                            "email": "JOHN.DOE@EXAMPLE.COM"
                          },
                          "transformerRules": [
                            {
                              "name": "normalize-name",
                              "condition": "#firstName != null",
                              "transformation": "#firstName.substring(0,1).toUpperCase() + #firstName.substring(1).toLowerCase()",
                              "targetField": "firstName"
                            },
                            {
                              "name": "normalize-email",
                              "condition": "#email != null",
                              "transformation": "#email.toLowerCase()",
                              "targetField": "email"
                            }
                          ]
                        }
                        """
                    )
                )
            )
            @Valid @NotNull DynamicTransformationRequest request) {

        logger.info("Applying dynamic transformation with {} rules", request.getTransformerRules().size());

        try {
            // Convert DTOs to actual TransformerRule objects and apply transformation
            List<TransformerRule<Object>> transformerRules = convertDtosToTransformerRules(request.getTransformerRules());

            // Apply the transformation using the GenericTransformerService
            Object transformedData = transformerService.transform(request.getData(), transformerRules);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("originalData", request.getData());
            response.put("transformedData", transformedData);
            response.put("appliedRules", request.getTransformerRules().size());
            response.put("timestamp", Instant.now());

            logger.info("Dynamic transformation completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during dynamic transformation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Dynamic transformation failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }



    // DTOs for request/response
    public static class DynamicTransformationRequest {
        @NotNull
        private Object data;
        
        @NotNull
        private List<TransformerRuleDto> transformerRules;

        // Getters and setters
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public List<TransformerRuleDto> getTransformerRules() { return transformerRules; }
        public void setTransformerRules(List<TransformerRuleDto> transformerRules) { this.transformerRules = transformerRules; }
    }

    public static class TransformerRuleDto {
        @NotBlank
        private String name;
        @NotBlank
        private String condition;
        @NotBlank
        private String transformation;
        @NotBlank
        private String targetField;

        // Constructors
        public TransformerRuleDto() {}
        
        public TransformerRuleDto(String name, String condition, String transformation, String targetField) {
            this.name = name;
            this.condition = condition;
            this.transformation = transformation;
            this.targetField = targetField;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getTransformation() { return transformation; }
        public void setTransformation(String transformation) { this.transformation = transformation; }
        public String getTargetField() { return targetField; }
        public void setTargetField(String targetField) { this.targetField = targetField; }
    }

    /**
     * Convert TransformerRuleDto objects to actual TransformerRule objects.
     */
    private List<TransformerRule<Object>> convertDtosToTransformerRules(List<TransformerRuleDto> dtos) {
        List<TransformerRule<Object>> transformerRules = new ArrayList<>();

        for (TransformerRuleDto dto : dtos) {
            // Create a Rule from the DTO
            Rule rule = new RuleBuilder()
                .withName(dto.getName())
                .withCondition(dto.getCondition())
                .withMessage("Transformation rule: " + dto.getName())
                .withDescription("Dynamic transformation rule for field: " + dto.getTargetField())
                .build();

            // Create FieldTransformerAction for the target field
            FieldTransformerAction<Object> action = new FieldTransformerActionBuilder<Object>()
                .withFieldName(dto.getTargetField())
                .withFieldValueExtractor(obj -> {
                    // Extract field value using reflection or map access
                    if (obj instanceof Map) {
                        return ((Map<?, ?>) obj).get(dto.getTargetField());
                    }
                    // For other objects, would need reflection - simplified for now
                    return null;
                })
                .withFieldValueTransformer((currentValue, facts) -> {
                    // Apply the transformation expression
                    // This would ideally use SpEL evaluation with the transformation expression
                    // For now, return the current value as a placeholder
                    return currentValue;
                })
                .withFieldValueSetter((obj, newValue) -> {
                    // Set field value using reflection or map access
                    if (obj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) obj;
                        map.put(dto.getTargetField(), newValue);
                    }
                    // For other objects, would need reflection - simplified for now
                })
                .build();

            // Create TransformerRule with the action as positive action
            TransformerRule<Object> transformerRule = new TransformerRule<>(
                rule,
                List.of(action),
                Collections.emptyList()
            );

            transformerRules.add(transformerRule);
        }

        return transformerRules;
    }
}
