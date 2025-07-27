package dev.mars.rulesengine.rest.controller;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.api.ValidationResult;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.rest.dto.*;
import dev.mars.rulesengine.rest.service.RuleEvaluationService;
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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for rule evaluation operations.
 * 
 * This controller provides endpoints for evaluating rules, performing validations,
 * and managing named rules. It supports both simple rule checks and complex
 * validation scenarios with detailed error reporting.
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
        
        try {
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
            
        } catch (Exception e) {
            logger.error("Error evaluating rule: {}", e.getMessage(), e);
            RuleEvaluationResponse errorResponse = RuleEvaluationResponse.error(
                "Rule evaluation failed", e.getMessage()
            );
            errorResponse.setEvaluationId(UUID.randomUUID().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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
        
        try {
            logger.info("Starting validation with {} rules", 
                       request.getValidationRules() != null ? request.getValidationRules().size() : 0);
            
            ValidationResponse response = ruleEvaluationService.validateData(request);
            response.setValidationId(UUID.randomUUID().toString());
            
            logger.info("Validation completed: valid={}, errors={}, warnings={}", 
                       response.isValid(), response.getErrorCount(), response.getWarningCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during validation: {}", e.getMessage(), e);
            ValidationResponse errorResponse = new ValidationResponse();
            errorResponse.setValid(false);
            errorResponse.addError("system", "Validation failed: " + e.getMessage(), "ERROR");
            errorResponse.setValidationId(UUID.randomUUID().toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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
        
        try {
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
                
        } catch (Exception e) {
            logger.error("Error defining rule '{}': {}", ruleName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to define rule: " + e.getMessage()));
        }
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
        
        try {
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
            
        } catch (Exception e) {
            logger.error("Error testing rule '{}': {}", ruleName, e.getMessage(), e);
            RuleEvaluationResponse errorResponse = RuleEvaluationResponse.error(
                "Rule test failed", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
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
        try {
            String[] definedRules = rulesService.getDefinedRules();
            
            return ResponseEntity.ok(Map.of(
                "definedRules", definedRules,
                "count", definedRules.length,
                "timestamp", java.time.Instant.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving defined rules: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to retrieve defined rules: " + e.getMessage()));
        }
    }
}
