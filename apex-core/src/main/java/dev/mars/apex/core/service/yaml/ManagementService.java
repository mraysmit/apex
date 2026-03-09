package dev.mars.apex.core.service.yaml;

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


import dev.mars.apex.core.config.validation.ValidationResult;
import dev.mars.apex.core.config.validation.ValidationSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Comprehensive YAML management service that combines validation and dependency analysis.
 * 
 * This service provides a unified API for all YAML-related operations in APEX,
 * including validation, dependency analysis, and health checking.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-25
 * @version 1.0
 */
public class ManagementService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementService.class);
    
    private final ValidationService validationService;
    private final DependencyService dependencyService;
    
    /**
     * Create a management service with default configuration.
     */
    public ManagementService() {
        this.validationService = new ValidationService();
        this.dependencyService = new DependencyService();
    }
    
    /**
     * Create a management service with custom base resource path.
     * 
     * @param baseResourcePath The base path to search for YAML files
     */
    public ManagementService(String baseResourcePath) {
        this.validationService = new ValidationService(baseResourcePath);
        this.dependencyService = new DependencyService();
    }
    
    /**
     * Get the validation service.
     * 
     * @return The YAML validation service
     */
    public ValidationService getValidationService() {
        return validationService;
    }
    
    /**
     * Get the dependency service.
     * 
     * @return The YAML dependency service
     */
    public DependencyService getDependencyService() {
        return dependencyService;
    }
    
    /**
     * Perform comprehensive health check on all YAML files.
     * 
     * @return Health check result
     */
    public HealthCheckResult performHealthCheck() {
        LOGGER.info("Performing comprehensive YAML health check");
        
        // Validation check
        ValidationService.ValidationStatistics validationStats = validationService.getValidationStatistics();
        
        // Dependency check for discovered files
        List<String> yamlFiles = validationService.discoverYamlFiles("src/main/resources");
        DependencyService.DependencyAnalysisResult dependencyResult = 
            dependencyService.analyzeMultipleDependencies(yamlFiles);
        
        return new HealthCheckResult(validationStats, dependencyResult);
    }
    
    /**
     * Perform comprehensive validation and dependency analysis for a single file.
     * 
     * @param filePath Path to the YAML file
     * @return Comprehensive analysis result
     */
    public ComprehensiveAnalysisResult analyzeFile(String filePath) {
        LOGGER.info("Performing comprehensive analysis for: {}", filePath);
        
        // Validation
        var validationResult = validationService.validateFile(filePath);
        
        // Dependency analysis
        var dependencyReport = dependencyService.generateDependencyReport(filePath);
        
        return new ComprehensiveAnalysisResult(validationResult, dependencyReport);
    }
    
    /**
     * Check if the entire YAML ecosystem is healthy.
     * 
     * @return true if all YAML files are valid and have no dependency issues
     */
    public boolean isEcosystemHealthy() {
        HealthCheckResult healthCheck = performHealthCheck();
        return healthCheck.isHealthy();
    }
    
    /**
     * Get a summary of all YAML issues in the ecosystem.
     * 
     * @return List of all issues found
     */
    public List<String> getAllIssues() {
        java.util.List<String> allIssues = new java.util.ArrayList<>();
        
        // Add validation errors
        allIssues.addAll(validationService.getAllValidationErrors());
        
        // Add dependency issues
        List<String> yamlFiles = validationService.discoverYamlFiles("src/main/resources");
        for (String file : yamlFiles) {
            try {
                if (dependencyService.hasDependencyIssues(file)) {
                    List<String> missingDeps = dependencyService.getMissingDependencies(file);
                    if (!missingDeps.isEmpty()) {
                        allIssues.add("File " + file + " has missing dependencies: " + missingDeps);
                    }
                    
                    List<List<String>> circularDeps = dependencyService.getCircularDependencies(file);
                    if (!circularDeps.isEmpty()) {
                        allIssues.add("File " + file + " has circular dependencies: " + circularDeps);
                    }
                }
            } catch (Exception e) {
                allIssues.add("Failed to analyze dependencies for " + file + ": " + e.getMessage());
            }
        }
        
        return allIssues;
    }
    
    /**
     * Validate the entire YAML ecosystem and throw exception if issues are found.
     * 
     * @throws EcosystemException if any issues are found
     */
    public void validateEcosystemOrThrow() throws EcosystemException {
        HealthCheckResult healthCheck = performHealthCheck();
        
        if (!healthCheck.isHealthy()) {
            List<String> allIssues = getAllIssues();
            String errorMessage = String.format(
                "YAML ecosystem validation failed: %d validation issues, %d dependency failures. Issues: %s",
                healthCheck.getValidationStats().getInvalidFiles(),
                healthCheck.getDependencyResult().getFailureCount(),
                String.join("; ", allIssues)
            );
            throw new EcosystemException(errorMessage, healthCheck);
        }
    }
    
    /**
     * Result of comprehensive YAML health check.
     */
    public static class HealthCheckResult {
        private final ValidationService.ValidationStatistics validationStats;
        private final DependencyService.DependencyAnalysisResult dependencyResult;
        
        public HealthCheckResult(ValidationService.ValidationStatistics validationStats, 
                                   DependencyService.DependencyAnalysisResult dependencyResult) {
            this.validationStats = validationStats;
            this.dependencyResult = dependencyResult;
        }
        
        public ValidationService.ValidationStatistics getValidationStats() { return validationStats; }
        public DependencyService.DependencyAnalysisResult getDependencyResult() { return dependencyResult; }
        
        public boolean isHealthy() {
            return validationStats.isAllValid() && !dependencyResult.hasFailures();
        }
        
        @Override
        public String toString() {
            return String.format("YamlHealthCheck{validation=%s, dependencies=%d success/%d failed, healthy=%s}",
                validationStats, dependencyResult.getSuccessCount(), dependencyResult.getFailureCount(), isHealthy());
        }
    }
    
    /**
     * Result of comprehensive analysis for a single file.
     */
    public static class ComprehensiveAnalysisResult {
        private final ValidationResult validationResult;
        private final DependencyService.DependencyReport dependencyReport;

        public ComprehensiveAnalysisResult(ValidationResult validationResult,
                                         DependencyService.DependencyReport dependencyReport) {
            this.validationResult = validationResult;
            this.dependencyReport = dependencyReport;
        }

        public ValidationResult getValidationResult() { return validationResult; }
        public DependencyService.DependencyReport getDependencyReport() { return dependencyReport; }
        
        public boolean hasIssues() {
            return !validationResult.getErrors().isEmpty() || dependencyReport.hasIssues();
        }
        
        public boolean isValid() {
            return validationResult.getErrors().isEmpty() && !dependencyReport.hasIssues();
        }
    }
    
    /**
     * Exception thrown when YAML ecosystem validation fails.
     */
    public static class EcosystemException extends Exception {
        private final HealthCheckResult healthCheckResult;
        
        public EcosystemException(String message, HealthCheckResult healthCheckResult) {
            super(message);
            this.healthCheckResult = healthCheckResult;
        }
        
        public HealthCheckResult getHealthCheckResult() {
            return healthCheckResult;
        }
    }
}
