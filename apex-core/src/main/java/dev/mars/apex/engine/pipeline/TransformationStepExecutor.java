/*
 * Copyright (c) 2025 Devspace Mars Solutions.
 * All rights reserved.
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
package dev.mars.apex.engine.pipeline;

import dev.mars.apex.core.config.pipeline.PipelineStep;
import dev.mars.apex.engine.core.SpelParserHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles transformation step execution for APEX data pipelines.
 * 
 * <p>Extracted from PipelineExecutor as part of god class decomposition.
 * Supports transformation types: field-addition, calculation, validation, filter.
 * 
 * @author APEX Engine
 * @since 2.4
 */
public class TransformationStepExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransformationStepExecutor.class);

    private final ExpressionParser expressionParser;

    /**
     * Constructs a TransformationStepExecutor with the shared SpEL expression parser.
     */
    public TransformationStepExecutor() {
        this.expressionParser = SpelParserHolder.INSTANCE;
    }

    /**
     * Constructs a TransformationStepExecutor with a provided expression parser.
     *
     * @param expressionParser the SpEL expression parser to use
     */
    public TransformationStepExecutor(ExpressionParser expressionParser) {
        this.expressionParser = expressionParser;
    }

    /**
     * Execute a transform step on the provided data.
     *
     * @param step the pipeline step configuration
     * @param data the data to transform (can be List or single record)
     * @return the transformed data
     * @throws DataPipelineException if transformation fails critically
     */
    public Object executeTransformStep(PipelineStep step, Object data) {
        if (data == null) {
            LOGGER.error("No data available for transform step: {} - upstream extract may have failed", step.getName());
            return null;
        }

        List<Map<String, Object>> transformations = step.getTransformations();
        if (transformations == null || transformations.isEmpty()) {
            LOGGER.error("No transformations configured for transform step: {} - check pipeline configuration", step.getName());
            return data;
        }

        try {
            LOGGER.info("Applying {} transformations to data for step '{}'",
                transformations.size(), step.getName());

            // Process data - handle both single records and lists
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> dataList = (List<Object>) data;
                List<Object> transformedList = new ArrayList<>();

                for (Object record : dataList) {
                    Object transformedRecord = applyTransformations(record, transformations, step.getName());
                    if (transformedRecord != null) {
                        transformedList.add(transformedRecord);
                    }
                }

                LOGGER.info("Transform step '{}' processed {} records",
                    step.getName(), transformedList.size());
                return transformedList;
            } else {
                // Single record
                Object transformedData = applyTransformations(data, transformations, step.getName());
                LOGGER.info("Transform step '{}' processed single record", step.getName());
                return transformedData;
            }
        } catch (Exception e) {
            throw new DataPipelineException("Transform step failed: " + step.getName(), e);
        }
    }

    /**
     * Apply transformations to a single record.
     *
     * @param record the record to transform
     * @param transformations the list of transformations to apply
     * @param stepName the name of the current step (for logging)
     * @return the transformed record, or null if record should be skipped
     */
    @SuppressWarnings("unchecked")
    private Object applyTransformations(Object record, List<Map<String, Object>> transformations, String stepName) {
        if (!(record instanceof Map)) {
            LOGGER.error("Transform step '{}' can only transform Map records, skipping non-Map record of type: {}", 
                stepName, record.getClass().getName());
            return record;
        }

        Map<String, Object> recordMap = new LinkedHashMap<>((Map<String, Object>) record);

        for (Map<String, Object> transformation : transformations) {
            try {
                applyTransformation(recordMap, transformation);
            } catch (Exception e) {
                String errorHandling = (String) transformation.get("error-handling");
                if ("skip-record".equals(errorHandling)) {
                    LOGGER.error("Transformation '{}' failed, skipping record: {}",
                        transformation.get("name"), e.getMessage());
                    return null; // Skip this record
                } else {
                    LOGGER.error("Transformation '{}' failed, continuing with next transformation: {}",
                        transformation.get("name"), e.getMessage());
                    // Continue with other transformations
                }
            }
        }

        return recordMap;
    }

    /**
     * Apply a single transformation to a record.
     *
     * @param record the record to transform
     * @param transformation the transformation configuration
     */
    private void applyTransformation(Map<String, Object> record, Map<String, Object> transformation) {
        String type = (String) transformation.get("type");
        String field = (String) transformation.get("field");

        if (type == null || field == null) {
            LOGGER.error("Transformation missing required 'type' or 'field' property - check pipeline configuration");
            return;
        }

        switch (type.toLowerCase()) {
            case "field-addition":
                applyFieldAddition(record, field, transformation);
                break;
            case "calculation":
                applyCalculation(record, field, transformation);
                break;
            case "validation":
                applyValidation(record, field, transformation);
                break;
            case "filter":
                // Filter is handled at the record level, not field level
                break;
            case "aggregation":
                // Aggregation requires multiple records, handled separately
                LOGGER.error("Aggregation transformations not yet supported in pipeline transforms - remove from configuration or use supported type");
                break;
            default:
                LOGGER.error("Unknown transformation type: '{}' - check pipeline configuration for valid types", type);
        }
    }

    /**
     * Apply field addition transformation.
     *
     * @param record the record to modify
     * @param field the field name to add
     * @param transformation the transformation configuration containing the value
     */
    private void applyFieldAddition(Map<String, Object> record, String field, Map<String, Object> transformation) {
        Object value = transformation.get("value");
        if ("CURRENT_TIMESTAMP".equals(value)) {
            record.put(field, System.currentTimeMillis());
        } else {
            record.put(field, value);
        }
    }

    /**
     * Apply calculation transformation using SpEL expressions.
     *
     * @param record the record to modify
     * @param field the field name to store the result
     * @param transformation the transformation configuration containing the expression
     */
    private void applyCalculation(Map<String, Object> record, String field, Map<String, Object> transformation) {
        String expression = (String) transformation.get("expression");
        if (expression == null) {
            LOGGER.error("Calculation transformation missing 'expression' property - check pipeline configuration");
            return;
        }

        try {
            // Create evaluation context with record fields as variables
            StandardEvaluationContext context = new StandardEvaluationContext();

            // Add all record fields as variables (both original case and lowercase)
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                // Add with original case
                context.setVariable(entry.getKey(), entry.getValue());
                // Also add with lowercase for case-insensitive access
                context.setVariable(entry.getKey().toLowerCase(), entry.getValue());
            }

            // Parse and evaluate the expression
            Expression exp = expressionParser.parseExpression(expression);
            Object result = exp.getValue(context);

            // Store the result in the record (use lowercase key to match database column names)
            record.put(field.toLowerCase(), result);

            LOGGER.debug("Calculated field '{}' = {} using expression: {}", field, result, expression);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate calculation expression '{}' for field '{}': {}",
                expression, field, e.getMessage());
        }
    }

    /**
     * Apply validation transformation.
     *
     * @param record the record to validate
     * @param field the field name to validate
     * @param transformation the transformation configuration containing the validation rule
     * @throws IllegalArgumentException if validation fails with 'required' rule
     */
    private void applyValidation(Map<String, Object> record, String field, Map<String, Object> transformation) {
        String rule = (String) transformation.get("rule");
        if (rule == null) {
            LOGGER.error("Validation transformation missing 'rule' property - check pipeline configuration");
            return;
        }

        Object value = record.get(field);

        // Simple validation rules
        switch (rule.toLowerCase()) {
            case "required":
                if (value == null || value.toString().trim().isEmpty()) {
                    throw new IllegalArgumentException("Required field '" + field + "' is missing or empty");
                }
                break;
            case "status-format":
                // Example validation - could be extended
                if (value != null && !value.toString().matches("[A-Z]+")) {
                    LOGGER.error("Field '{}' value '{}' does not match required status format [A-Z]+", field, value);
                }
                break;
            default:
                LOGGER.error("Unknown validation rule: '{}' - check pipeline configuration for valid rules", rule);
        }
    }
}
