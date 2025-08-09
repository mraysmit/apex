package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.expression.ExpressionEvaluationService;
import dev.mars.apex.core.engine.model.RuleResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
 * REST Controller for SpEL expression evaluation operations.
 * Provides endpoints for evaluating Spring Expression Language (SpEL) expressions.
 */
@RestController
@RequestMapping("/api/expressions")
@Tag(name = "Expression Evaluation", description = "SpEL expression evaluation operations")
public class ExpressionController {

    private static final Logger logger = LoggerFactory.getLogger(ExpressionController.class);

    @Autowired
    private ExpressionEvaluationService expressionEvaluationService;

    /**
     * Evaluate a SpEL expression with provided context.
     */
    @PostMapping("/evaluate")
    @Operation(
        summary = "Evaluate SpEL expression",
        description = "Evaluates a Spring Expression Language (SpEL) expression with the provided context data."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Expression evaluated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid expression or context"),
        @ApiResponse(responseCode = "500", description = "Expression evaluation error")
    })
    public ResponseEntity<Map<String, Object>> evaluateExpression(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Expression evaluation request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Mathematical expression",
                        value = """
                        {
                          "expression": "#amount * #rate + #fee",
                          "context": {
                            "amount": 1000.0,
                            "rate": 0.05,
                            "fee": 25.0
                          }
                        }
                        """
                    )
                )
            )
            @Valid @NotNull ExpressionEvaluationRequest request) {

        logger.info("Evaluating expression: {}", request.getExpression());
        logger.debug("Context: {}", request.getContext());

        try {
            // Evaluate the expression
            Object result = expressionEvaluationService.evaluate(request.getExpression(), request.getContext());

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("expression", request.getExpression());
            response.put("context", request.getContext());
            response.put("result", result);
            response.put("resultType", result != null ? result.getClass().getSimpleName() : "null");
            response.put("timestamp", Instant.now());

            logger.info("Expression evaluation completed successfully: {}", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error evaluating expression: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Expression evaluation failed");
            errorResponse.put("expression", request.getExpression());
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Evaluate a SpEL expression with detailed result information.
     */
    @PostMapping("/evaluate/detailed")
    @Operation(
        summary = "Evaluate SpEL expression with detailed result",
        description = "Evaluates a SpEL expression and returns detailed result information including execution metrics."
    )
    @ApiResponse(responseCode = "200", description = "Detailed expression evaluation completed")
    public ResponseEntity<Map<String, Object>> evaluateExpressionWithResult(
            @RequestBody @Valid @NotNull ExpressionEvaluationRequest request) {

        logger.info("Evaluating expression with detailed result: {}", request.getExpression());

        try {
            // Evaluate with detailed result
            RuleResult result = expressionEvaluationService.evaluateWithResult(request.getExpression(), request.getContext());

            // Prepare detailed response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("expression", request.getExpression());
            response.put("context", request.getContext());
            response.put("ruleResult", Map.of(
                "triggered", result.isTriggered(),
                "ruleName", result.getRuleName(),
                "message", result.getMessage(),
                "resultType", result.getResultType().toString(),
                "timestamp", result.getTimestamp(),
                "hasPerformanceMetrics", result.hasPerformanceMetrics()
            ));
            response.put("timestamp", Instant.now());

            logger.info("Detailed expression evaluation completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during detailed expression evaluation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Detailed expression evaluation failed");
            errorResponse.put("expression", request.getExpression());
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Evaluate multiple expressions in batch.
     */
    @PostMapping("/batch")
    @Operation(
        summary = "Evaluate multiple expressions in batch",
        description = "Evaluates multiple SpEL expressions using the same context data."
    )
    @ApiResponse(responseCode = "200", description = "Batch expression evaluation completed")
    public ResponseEntity<Map<String, Object>> evaluateBatchExpressions(
            @RequestBody @Valid @NotNull BatchExpressionEvaluationRequest request) {

        logger.info("Evaluating {} expressions in batch", request.getExpressions().size());

        try {
            List<Map<String, Object>> expressionResults = new ArrayList<>();

            for (ExpressionItem expressionItem : request.getExpressions()) {
                Map<String, Object> expressionResult = new HashMap<>();
                expressionResult.put("name", expressionItem.getName());
                expressionResult.put("expression", expressionItem.getExpression());

                try {
                    Object result = expressionEvaluationService.evaluate(
                        expressionItem.getExpression(), request.getContext());

                    expressionResult.put("success", true);
                    expressionResult.put("result", result);
                    expressionResult.put("resultType", result != null ? result.getClass().getSimpleName() : "null");

                } catch (Exception e) {
                    logger.warn("Error evaluating expression '{}': {}", expressionItem.getName(), e.getMessage());
                    expressionResult.put("success", false);
                    expressionResult.put("error", e.getMessage());
                }

                expressionResults.add(expressionResult);
            }

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("expressionResults", expressionResults);
            response.put("totalExpressions", request.getExpressions().size());
            response.put("successfulExpressions", expressionResults.stream()
                .mapToInt(r -> (Boolean) r.get("success") ? 1 : 0).sum());
            response.put("context", request.getContext());
            response.put("timestamp", Instant.now());

            logger.info("Batch expression evaluation completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during batch expression evaluation: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Batch expression evaluation failed");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Validate a SpEL expression syntax.
     */
    @PostMapping("/validate")
    @Operation(
        summary = "Validate SpEL expression syntax",
        description = "Validates the syntax of a SpEL expression without evaluating it."
    )
    @ApiResponse(responseCode = "200", description = "Expression validation completed")
    public ResponseEntity<Map<String, Object>> validateExpression(
            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Expression validation request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Expression validation",
                        value = """
                        {
                          "expression": "#amount > 1000 && #currency == 'USD'"
                        }
                        """
                    )
                )
            )
            @Valid @NotNull ExpressionValidationRequest request) {

        logger.info("Validating expression syntax: {}", request.getExpression());

        try {
            // Validate the expression syntax
            boolean isValid = expressionEvaluationService.validateSyntax(request.getExpression());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("expression", request.getExpression());
            response.put("valid", isValid);
            response.put("message", isValid ? "Expression syntax is valid" : "Expression syntax is invalid");
            response.put("timestamp", Instant.now());

            logger.info("Expression validation completed: valid={}", isValid);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating expression: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("expression", request.getExpression());
            response.put("valid", false);
            response.put("message", "Expression syntax is invalid: " + e.getMessage());
            response.put("error", e.getMessage());
            response.put("timestamp", Instant.now());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Get available SpEL functions and operators.
     */
    @GetMapping("/functions")
    @Operation(
        summary = "Get available SpEL functions",
        description = "Returns a list of available SpEL functions and operators."
    )
    @ApiResponse(responseCode = "200", description = "SpEL functions retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAvailableFunctions() {
        logger.debug("Retrieving available SpEL functions");

        Map<String, Object> functions = new HashMap<>();
        
        // Mathematical functions
        functions.put("mathematical", Arrays.asList(
            "abs(number)", "ceil(number)", "floor(number)", "round(number)",
            "max(a, b)", "min(a, b)", "pow(base, exponent)", "sqrt(number)"
        ));
        
        // String functions
        functions.put("string", Arrays.asList(
            "length()", "substring(start, end)", "toLowerCase()", "toUpperCase()",
            "trim()", "contains(substring)", "startsWith(prefix)", "endsWith(suffix)",
            "replace(old, new)", "split(delimiter)"
        ));
        
        // Date/Time functions
        functions.put("datetime", Arrays.asList(
            "T(java.time.Instant).now()", "T(java.time.LocalDate).now()",
            "T(java.time.LocalDateTime).now()", "plusDays(days)", "minusDays(days)",
            "isAfter(other)", "isBefore(other)"
        ));
        
        // Collection functions
        functions.put("collections", Arrays.asList(
            "size()", "isEmpty()", "contains(element)", "?[condition]",
            "![projection]", "^[first]", "$[last]"
        ));
        
        // Logical operators
        functions.put("logical", Arrays.asList(
            "&&", "||", "!", "and", "or", "not"
        ));
        
        // Comparison operators
        functions.put("comparison", Arrays.asList(
            "==", "!=", "<", ">", "<=", ">=", "eq", "ne", "lt", "gt", "le", "ge"
        ));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("functions", functions);
        response.put("timestamp", Instant.now());

        return ResponseEntity.ok(response);
    }

    // DTOs for request/response
    public static class ExpressionEvaluationRequest {
        @NotBlank
        private String expression;
        
        @NotNull
        private Map<String, Object> context;

        // Getters and setters
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    public static class BatchExpressionEvaluationRequest {
        @NotNull
        private List<ExpressionItem> expressions;
        
        @NotNull
        private Map<String, Object> context;

        // Getters and setters
        public List<ExpressionItem> getExpressions() { return expressions; }
        public void setExpressions(List<ExpressionItem> expressions) { this.expressions = expressions; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }

    public static class ExpressionItem {
        @NotBlank
        private String name;
        @NotBlank
        private String expression;

        // Constructors
        public ExpressionItem() {}
        
        public ExpressionItem(String name, String expression) {
            this.name = name;
            this.expression = expression;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
    }

    public static class ExpressionValidationRequest {
        @NotBlank
        private String expression;

        // Getters and setters
        public String getExpression() { return expression; }
        public void setExpression(String expression) { this.expression = expression; }
    }
}
