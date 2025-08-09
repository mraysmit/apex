package dev.mars.apex.rest.controller;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.rest.dto.*;
import dev.mars.apex.rest.service.RuleEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;

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
 * REST Controller for rule evaluation operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@RestController
@RequestMapping("/api/rules")
@Tag(name = "Rules", description = "Rule evaluation and validation operations")
@Validated
public class RulesController {
    
    private static final Logger logger = LoggerFactory.getLogger(RulesController.class);
    
    @Autowired
    private RulesService rulesService;

    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private RulesEngine rulesEngine;
    
    /**
     * Simple rule check endpoint.
     * Evaluates a single rule condition against provided data.
     */
    @PostMapping("/check")
    @Operation(
        summary = "Check a rule condition",
        description = "Evaluates a single SpEL rule condition against the provided data and returns a boolean result."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule evaluation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<RuleEvaluationResponse> checkRule(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Rule evaluation request",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RuleEvaluationRequest.class),
                    examples = @ExampleObject(
                        name = "Simple age check",
                        value = """
                        {
                          "condition": "#age >= 18",
                          "data": {"age": 25, "name": "John"},
                          "ruleName": "age-check",
                          "message": "User is an adult"
                        }
                        """
                    )
                )
            )
            RuleEvaluationRequest request) {

        logger.info("Evaluating rule check: {}", request.getRuleName());
        logger.debug("Rule condition: {}", request.getCondition());

        boolean result = rulesService.check(request.getCondition(), request.getData());

        RuleEvaluationResponse response = RuleEvaluationResponse.success(
            result,
            request.getRuleName() != null ? request.getRuleName() : "check-rule",
            result ? (request.getMessage() != null ? request.getMessage() : "Rule matched")
                   : "Rule did not match"
        );

        response.setEvaluationId(UUID.randomUUID().toString());

        // Add performance metrics if requested
        if (request.isIncludeMetrics()) {
            response.setMetrics(new PerformanceMetricsDto(0, 0, 1, true));
        }

        logger.info("Rule check completed: matched={}", result);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Comprehensive validation endpoint.
     * Validates data against multiple rules and returns detailed results.
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate data against multiple rules",
        description = "Validates the provided data against multiple validation rules and returns detailed results including all errors and warnings."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Validation completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ValidationResponse> validateData(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Validation request with data and rules",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ValidationRequest.class),
                    examples = @ExampleObject(
                        name = "Customer validation",
                        value = """
                        {
                          "data": {"age": 16, "email": null, "balance": 1500},
                          "validationRules": [
                            {
                              "name": "age-check",
                              "condition": "#data.age >= 18",
                              "message": "Customer must be at least 18 years old",
                              "severity": "ERROR"
                            },
                            {
                              "name": "email-check",
                              "condition": "#data.email != null",
                              "message": "Email address is required",
                              "severity": "ERROR"
                            }
                          ],
                          "includeDetails": true
                        }
                        """
                    )
                )
            )
            ValidationRequest request) {

        logger.info("Starting validation with {} rules",
                   request.getValidationRules() != null ? request.getValidationRules().size() : 0);

        ValidationResponse response = ruleEvaluationService.validateData(request);
        response.setValidationId(UUID.randomUUID().toString());

        logger.info("Validation completed: valid={}, errors={}, warnings={}",
                   response.isValid(), response.getErrorCount(), response.getWarningCount());

        return ResponseEntity.ok(response);
    }
    
    /**
     * Define a named rule for reuse.
     */
    @PostMapping("/define/{ruleName}")
    @Operation(
        summary = "Define a named rule",
        description = "Defines a named rule that can be reused in subsequent evaluations."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Rule defined successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid rule definition"),
        @ApiResponse(responseCode = "409", description = "Rule already exists")
    })
    public ResponseEntity<Map<String, Object>> defineRule(
            @Parameter(description = "Name of the rule to define", example = "adult-check")
            @PathVariable @NotBlank String ruleName,

            @RequestBody Map<String, String> ruleDefinition) {

        String condition = ruleDefinition.get("condition");
        String message = ruleDefinition.get("message");

        if (condition == null || condition.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Rule condition is required"));
        }

        // Check if rule already exists
        if (rulesService.isDefined(ruleName)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Rule '" + ruleName + "' already exists"));
        }

        rulesService.define(ruleName, condition, message);

        logger.info("Defined rule: {} with condition: {}", ruleName, condition);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "message", "Rule defined successfully",
                "ruleName", ruleName,
                "condition", condition,
                "ruleMessage", message != null ? message : "Rule '" + ruleName + "' matched"
            ));
    }
    
    /**
     * Test a previously defined named rule.
     */
    @PostMapping("/test/{ruleName}")
    @Operation(
        summary = "Test a named rule",
        description = "Tests a previously defined named rule against the provided data."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rule test completed successfully"),
        @ApiResponse(responseCode = "404", description = "Rule not found"),
        @ApiResponse(responseCode = "400", description = "Invalid test data")
    })
    public ResponseEntity<RuleEvaluationResponse> testRule(
            @Parameter(description = "Name of the rule to test", example = "adult-check")
            @PathVariable @NotBlank String ruleName,

            @RequestBody Map<String, Object> testData) {

        if (!rulesService.isDefined(ruleName)) {
            RuleEvaluationResponse errorResponse = RuleEvaluationResponse.error(
                "Rule not found", "Rule '" + ruleName + "' is not defined"
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        boolean result = rulesService.test(ruleName, testData);

        RuleEvaluationResponse response = RuleEvaluationResponse.success(
            result, ruleName, result ? "Rule matched" : "Rule did not match"
        );
        response.setEvaluationId(UUID.randomUUID().toString());

        logger.info("Tested rule '{}': matched={}", ruleName, result);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get list of all defined rules.
     */
    @GetMapping("/defined")
    @Operation(
        summary = "Get all defined rules",
        description = "Returns a list of all currently defined named rules."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved defined rules")
    public ResponseEntity<Map<String, Object>> getDefinedRules() {
        String[] definedRules = rulesService.getDefinedRules();

        return ResponseEntity.ok(Map.of(
            "definedRules", definedRules,
            "count", definedRules.length,
            "timestamp", java.time.Instant.now()
        ));
    }

    /**
     * Execute a rule directly with provided facts.
     */
    @PostMapping("/execute")
    @Operation(
        summary = "Execute rule directly",
        description = "Executes a rule directly using the rule engine with provided facts."
    )
    @ApiResponse(responseCode = "200", description = "Rule executed successfully")
    public ResponseEntity<Map<String, Object>> executeRule(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Rule execution request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Execute rule",
                        value = """
                        {
                          "rule": {
                            "name": "high-value-trade",
                            "condition": "#amount > 10000",
                            "message": "High value trade detected",
                            "priority": "HIGH"
                          },
                          "facts": {
                            "amount": 15000.0,
                            "currency": "USD",
                            "customerId": "CUST001"
                          }
                        }
                        """
                    )
                )
            )
            @Valid @NotNull RuleExecutionRequest request) {

        logger.info("Executing rule: {}", request.getRule().getName());

        try {
            // Create Rule object from request
            Rule rule = new Rule(
                request.getRule().getName(),
                request.getRule().getCondition(),
                request.getRule().getMessage()
            );

            // Execute the rule
            RuleResult result = rulesEngine.executeRule(rule, request.getFacts());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("rule", Map.of(
                "name", rule.getName(),
                "condition", rule.getCondition(),
                "message", rule.getMessage(),
                "priority", rule.getPriority()
            ));
            response.put("facts", request.getFacts());
            response.put("result", Map.of(
                "triggered", result.isTriggered(),
                "ruleName", result.getRuleName(),
                "message", result.getMessage(),
                "resultType", result.getResultType().toString(),
                "timestamp", result.getTimestamp(),
                "hasPerformanceMetrics", result.hasPerformanceMetrics()
            ));
            response.put("timestamp", java.time.Instant.now());

            logger.info("Rule execution completed: triggered={}", result.isTriggered());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error executing rule: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Rule execution failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Execute multiple rules in batch.
     */
    @PostMapping("/batch")
    @Operation(
        summary = "Execute multiple rules in batch",
        description = "Executes multiple rules against the same set of facts."
    )
    @ApiResponse(responseCode = "200", description = "Batch rule execution completed")
    public ResponseEntity<Map<String, Object>> executeBatchRules(
            @RequestBody @Valid @NotNull BatchRuleExecutionRequest request) {

        logger.info("Executing {} rules in batch", request.getRules().size());

        try {
            // Convert DTOs to Rule objects
            List<Rule> rules = new ArrayList<>();
            for (RuleDto ruleDto : request.getRules()) {
                Rule rule = new Rule(ruleDto.getName(), ruleDto.getCondition(), ruleDto.getMessage());
                rules.add(rule);
            }

            // Execute all rules
            List<RuleResult> results = new ArrayList<>();
            for (Rule rule : rules) {
                RuleResult result = rulesEngine.executeRule(rule, request.getFacts());
                results.add(result);
            }

            // Prepare response
            List<Map<String, Object>> ruleResults = new ArrayList<>();
            for (RuleResult result : results) {
                ruleResults.add(Map.of(
                    "triggered", result.isTriggered(),
                    "ruleName", result.getRuleName(),
                    "message", result.getMessage(),
                    "resultType", result.getResultType().toString(),
                    "timestamp", result.getTimestamp()
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRules", rules.size());
            response.put("triggeredRules", results.stream().mapToInt(r -> r.isTriggered() ? 1 : 0).sum());
            response.put("facts", request.getFacts());
            response.put("results", ruleResults);
            response.put("timestamp", java.time.Instant.now());

            logger.info("Batch rule execution completed: {}/{} rules triggered",
                results.stream().mapToInt(r -> r.isTriggered() ? 1 : 0).sum(), rules.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during batch rule execution: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Batch rule execution failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // DTOs for request/response
    public static class RuleExecutionRequest {
        @NotNull
        private RuleDto rule;

        @NotNull
        private Map<String, Object> facts;

        // Getters and setters
        public RuleDto getRule() { return rule; }
        public void setRule(RuleDto rule) { this.rule = rule; }
        public Map<String, Object> getFacts() { return facts; }
        public void setFacts(Map<String, Object> facts) { this.facts = facts; }
    }

    public static class RuleDto {
        @NotBlank
        private String name;
        @NotBlank
        private String condition;
        private String message;
        private String priority;

        // Constructors
        public RuleDto() {}

        public RuleDto(String name, String condition, String message) {
            this.name = name;
            this.condition = condition;
            this.message = message;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }

    public static class BatchRuleExecutionRequest {
        @NotNull
        private List<RuleDto> rules;

        @NotNull
        private Map<String, Object> facts;

        // Getters and setters
        public List<RuleDto> getRules() { return rules; }
        public void setRules(List<RuleDto> rules) { this.rules = rules; }
        public Map<String, Object> getFacts() { return facts; }
        public void setFacts(Map<String, Object> facts) { this.facts = facts; }
    }
}
