package dev.mars.rulesengine.rest.service;

import dev.mars.rulesengine.core.api.RulesService;
import dev.mars.rulesengine.core.api.ValidationBuilder;
import dev.mars.rulesengine.core.api.ValidationResult;
import dev.mars.rulesengine.rest.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.mars.rulesengine.rest.util.TestAwareLogger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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
 * Service class for rule evaluation operations.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service class for rule evaluation operations.
 * 
 * This service provides business logic for rule evaluation, validation,
 * and performance monitoring. It acts as a bridge between the REST controllers
 * and the core Rules Engine functionality.
 */
@Service
public class RuleEvaluationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluationService.class);

    @Autowired
    private RulesService rulesService;

    @Autowired
    private TestAwareLogger testAwareLogger;
    
    /**
     * Validates data against multiple validation rules.
     * 
     * @param request The validation request containing data and rules
     * @return ValidationResponse with detailed results
     */
    public ValidationResponse validateData(ValidationRequest request) {
        logger.debug("Starting validation with {} rules", 
                    request.getValidationRules() != null ? request.getValidationRules().size() : 0);
        
        ValidationResponse response = new ValidationResponse();
        
        if (request.getValidationRules() == null || request.getValidationRules().isEmpty()) {
            if (testAwareLogger != null) {
                testAwareLogger.warn(logger, "No validation rules provided");
            } else {
                logger.warn("No validation rules provided");
            }
            response.setValid(true);
            response.setTotalRules(0);
            response.setPassedRules(0);
            response.setFailedRules(0);
            return response;
        }
        
        int totalRules = request.getValidationRules().size();
        int passedRules = 0;
        int failedRules = 0;
        
        // Start performance tracking if requested
        Instant startTime = Instant.now();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        try {
            // Create validation builder
            ValidationBuilder builder = rulesService.validate(request.getData());
            
            // Add each validation rule
            for (ValidationRequest.ValidationRuleDto rule : request.getValidationRules()) {
                try {
                    logger.debug("Evaluating rule: {} with condition: {}", rule.getName(), rule.getCondition());
                    
                    // Add the rule to the validation builder
                    builder.that(rule.getCondition(), rule.getMessage());
                    
                    // For individual rule tracking, we need to test each rule separately
                    boolean ruleResult = rulesService.check(rule.getCondition(), request.getData());
                    
                    if (ruleResult) {
                        passedRules++;
                        logger.debug("Rule '{}' passed", rule.getName());
                    } else {
                        failedRules++;
                        logger.debug("Rule '{}' failed", rule.getName());
                        
                        // Add error to response
                        if (request.isIncludeDetails()) {
                            String severity = rule.getSeverity() != null ? rule.getSeverity() : "ERROR";
                            if ("WARNING".equalsIgnoreCase(severity)) {
                                response.addWarning(rule.getName(), rule.getMessage());
                            } else {
                                response.addError(rule.getName(), rule.getMessage(), severity);
                            }
                        }
                        
                        // Stop on first failure if requested
                        if (request.isStopOnFirstFailure()) {
                            logger.debug("Stopping validation on first failure as requested");
                            break;
                        }
                    }
                    
                } catch (Exception e) {
                    logger.error("Error evaluating rule '{}': {}", rule.getName(), e.getMessage(), e);
                    failedRules++;
                    
                    if (request.isIncludeDetails()) {
                        response.addError(rule.getName(), 
                                        "Rule evaluation failed: " + e.getMessage(), "ERROR");
                    }
                    
                    if (request.isStopOnFirstFailure()) {
                        break;
                    }
                }
            }
            
            // Set overall validation result
            response.setValid(failedRules == 0);
            response.setTotalRules(totalRules);
            response.setPassedRules(passedRules);
            response.setFailedRules(failedRules);
            
            // Add performance metrics if requested
            if (request.isIncludeMetrics()) {
                Instant endTime = Instant.now();
                long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long evaluationTime = endTime.toEpochMilli() - startTime.toEpochMilli();
                long memoryUsed = Math.max(0, endMemory - startMemory);
                
                PerformanceMetricsDto metrics = new PerformanceMetricsDto(
                    evaluationTime, memoryUsed, totalRules, failedRules == 0
                );
                metrics.setStartTime(startTime);
                metrics.setEndTime(endTime);
                response.setMetrics(metrics);
            }
            
            logger.info("Validation completed: valid={}, passed={}, failed={}", 
                       response.isValid(), passedRules, failedRules);
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error during validation: {}", e.getMessage(), e);
            
            // Create error response
            ValidationResponse errorResponse = new ValidationResponse();
            errorResponse.setValid(false);
            errorResponse.setTotalRules(totalRules);
            errorResponse.setPassedRules(passedRules);
            errorResponse.setFailedRules(totalRules - passedRules);
            errorResponse.addError("system", "Validation system error: " + e.getMessage(), "ERROR");
            
            return errorResponse;
        }
    }
    
    /**
     * Evaluates a single rule with enhanced error handling and metrics.
     * 
     * @param condition The SpEL condition to evaluate
     * @param data The data to evaluate against
     * @param includeMetrics Whether to include performance metrics
     * @return RuleEvaluationResponse with results and optional metrics
     */
    public RuleEvaluationResponse evaluateRule(String condition, Map<String, Object> data, 
                                             boolean includeMetrics) {
        logger.debug("Evaluating rule condition: {}", condition);
        
        Instant startTime = Instant.now();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        try {
            boolean result = rulesService.check(condition, data);
            
            RuleEvaluationResponse response = RuleEvaluationResponse.success(
                result, "rule-evaluation", result ? "Rule matched" : "Rule did not match"
            );
            
            // Add performance metrics if requested
            if (includeMetrics) {
                Instant endTime = Instant.now();
                long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long evaluationTime = endTime.toEpochMilli() - startTime.toEpochMilli();
                long memoryUsed = Math.max(0, endMemory - startMemory);
                
                PerformanceMetricsDto metrics = new PerformanceMetricsDto(
                    evaluationTime, memoryUsed, 1, true
                );
                metrics.setStartTime(startTime);
                metrics.setEndTime(endTime);
                response.setMetrics(metrics);
            }
            
            logger.debug("Rule evaluation completed: matched={}", result);
            return response;
            
        } catch (Exception e) {
            logger.error("Error evaluating rule: {}", e.getMessage(), e);
            
            RuleEvaluationResponse errorResponse = RuleEvaluationResponse.error(
                "Rule evaluation failed", e.getMessage()
            );
            
            // Add error metrics if requested
            if (includeMetrics) {
                Instant endTime = Instant.now();
                long evaluationTime = endTime.toEpochMilli() - startTime.toEpochMilli();
                
                PerformanceMetricsDto metrics = new PerformanceMetricsDto(
                    evaluationTime, 0, 1, false
                );
                metrics.setStartTime(startTime);
                metrics.setEndTime(endTime);
                metrics.setExceptionMessage(e.getMessage());
                errorResponse.setMetrics(metrics);
            }
            
            return errorResponse;
        }
    }
    
    /**
     * Creates a fluent validation for the given data.
     * This method demonstrates the fluent validation API.
     * 
     * @param data The data to validate
     * @return ValidationBuilder for chaining validation rules
     */
    public ValidationBuilder createFluentValidation(Map<String, Object> data) {
        logger.debug("Creating fluent validation for data with {} keys", data.size());
        return rulesService.validate(data);
    }
    
    /**
     * Performs a quick rule check with minimal overhead.
     * 
     * @param condition The condition to check
     * @param data The data to check against
     * @return Simple boolean result
     */
    public boolean quickCheck(String condition, Map<String, Object> data) {
        try {
            return rulesService.check(condition, data);
        } catch (Exception e) {
            logger.warn("Quick check failed for condition '{}': {}", condition, e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets system information for monitoring and debugging.
     * 
     * @return Map containing system information
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        info.put("totalMemory", runtime.totalMemory());
        info.put("freeMemory", runtime.freeMemory());
        info.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        info.put("maxMemory", runtime.maxMemory());
        info.put("availableProcessors", runtime.availableProcessors());
        info.put("timestamp", Instant.now());
        
        // Add rules service information
        String[] definedRules = rulesService.getDefinedRules();
        info.put("definedRulesCount", definedRules.length);
        info.put("definedRules", definedRules);
        
        return info;
    }
}
