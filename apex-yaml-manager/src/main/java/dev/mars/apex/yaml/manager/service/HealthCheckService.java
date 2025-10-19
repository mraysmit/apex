package dev.mars.apex.yaml.manager.service;

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

import dev.mars.apex.yaml.manager.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for performing health checks on YAML configurations.
 * 
 * Calculates health scores, detects issues, and provides recommendations.
 */
@Service
public class HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

    private final ValidationService validationService;

    public HealthCheckService(ValidationService validationService) {
        this.validationService = validationService;
    }

    /**
     * Perform comprehensive health check on a YAML file.
     */
    public HealthScore performHealthCheck(String filePath, String baseDir) {
        logger.debug("Performing health check on: {}", filePath);
        
        HealthScore score = new HealthScore(filePath);
        
        if (validationService != null) {
            // Structural validation
            ValidationResult structural = validationService.validateStructure(filePath);
            score.setStructuralScore(calculateScoreFromValidation(structural));
            
            // Reference validation
            ValidationResult references = validationService.validateReferences(filePath, baseDir);
            score.setReferenceScore(calculateScoreFromValidation(references));
            
            // Consistency validation
            ValidationResult consistency = validationService.validateConsistency(filePath);
            score.setConsistencyScore(calculateScoreFromValidation(consistency));
        }
        
        // Performance score (based on file complexity)
        score.setPerformanceScore(calculatePerformanceScore(filePath));
        
        // Compliance score (metadata completeness)
        score.setComplianceScore(calculateComplianceScore(filePath));
        
        // Metadata score
        score.setMetadataScore(calculateMetadataScore(filePath));
        
        // Calculate overall score
        score.calculateOverallScore();
        
        logger.debug("Health check complete for {}: score={}, grade={}", 
            filePath, score.getOverallScore(), score.getGrade());
        
        return score;
    }

    /**
     * Calculate score from validation result.
     */
    private int calculateScoreFromValidation(ValidationResult result) {
        if (result == null) {
            return 100;
        }
        
        int score = 100;
        score -= result.getErrorCount() * 20;  // Each error: -20 points
        score -= result.getWarningCount() * 5; // Each warning: -5 points
        score -= result.getInfoCount() * 1;    // Each info: -1 point
        
        return Math.max(0, score);
    }

    /**
     * Calculate performance score based on file complexity.
     */
    private int calculatePerformanceScore(String filePath) {
        // For now, return 100 (perfect score)
        // In Phase 4, this will analyze dependency depth, complexity, etc.
        return 100;
    }

    /**
     * Calculate compliance score based on required metadata.
     */
    private int calculateComplianceScore(String filePath) {
        // For now, return 100 (perfect score)
        // In Phase 3, this will check for required metadata fields
        return 100;
    }

    /**
     * Calculate metadata score based on metadata completeness.
     */
    private int calculateMetadataScore(String filePath) {
        // For now, return 100 (perfect score)
        // In Phase 3, this will check for complete metadata
        return 100;
    }

    /**
     * Get recommendations for improving health score.
     */
    public List<Recommendation> getRecommendations(HealthScore score) {
        List<Recommendation> recommendations = new ArrayList<>();
        
        if (score.getStructuralScore() < 100) {
            Recommendation rec = new Recommendation(
                "REC_STRUCTURAL",
                "Fix structural issues",
                "Your YAML file has structural issues that need to be fixed",
                Recommendation.Priority.CRITICAL
            );
            rec.setExpectedImpact(Recommendation.ImpactLevel.HIGH);
            rec.setEstimatedEffortMinutes(30);
            rec.setAction("Review and fix YAML syntax errors");
            recommendations.add(rec);
        }
        
        if (score.getReferenceScore() < 100) {
            Recommendation rec = new Recommendation(
                "REC_REFERENCES",
                "Fix missing references",
                "Some referenced files are missing or broken",
                Recommendation.Priority.HIGH
            );
            rec.setExpectedImpact(Recommendation.ImpactLevel.HIGH);
            rec.setEstimatedEffortMinutes(20);
            rec.setAction("Verify all file references exist and are correct");
            recommendations.add(rec);
        }
        
        if (score.getConsistencyScore() < 100) {
            Recommendation rec = new Recommendation(
                "REC_CONSISTENCY",
                "Improve consistency",
                "Your configuration has consistency issues",
                Recommendation.Priority.MEDIUM
            );
            rec.setExpectedImpact(Recommendation.ImpactLevel.MEDIUM);
            rec.setEstimatedEffortMinutes(15);
            rec.setAction("Follow naming conventions and ensure unique IDs");
            recommendations.add(rec);
        }
        
        if (score.getMetadataScore() < 100) {
            Recommendation rec = new Recommendation(
                "REC_METADATA",
                "Complete metadata",
                "Your configuration is missing important metadata",
                Recommendation.Priority.MEDIUM
            );
            rec.setExpectedImpact(Recommendation.ImpactLevel.MEDIUM);
            rec.setEstimatedEffortMinutes(10);
            rec.setAction("Add missing metadata fields (description, author, tags)");
            recommendations.add(rec);
        }
        
        return recommendations;
    }

    /**
     * Generate a comprehensive health report.
     */
    public HealthReport generateHealthReport(String filePath, String baseDir) {
        HealthReport report = new HealthReport();

        // Perform health check
        HealthScore score = performHealthCheck(filePath, baseDir);

        // Set component scores
        report.setStructuralHealth(score.getStructuralScore());
        report.setReferenceHealth(score.getReferenceScore());
        report.setConsistencyHealth(score.getConsistencyScore());
        report.setPerformanceHealth(score.getPerformanceScore());
        report.setComplianceHealth(score.getComplianceScore());
        report.setOverallScore(score.getOverallScore());

        // Collect validation issues
        if (validationService != null) {
            ValidationResult structural = validationService.validateStructure(filePath);
            ValidationResult references = validationService.validateReferences(filePath, baseDir);
            ValidationResult consistency = validationService.validateConsistency(filePath);

            // Convert ValidationIssues to HealthIssues
            convertAndAddIssues(report, structural);
            convertAndAddIssues(report, references);
            convertAndAddIssues(report, consistency);
        }

        // Get recommendations
        List<Recommendation> recommendations = getRecommendations(score);
        for (Recommendation rec : recommendations) {
            report.addRecommendation(rec.getTitle() + ": " + rec.getDescription());
        }

        return report;
    }

    /**
     * Convert ValidationIssues to HealthIssues and add to report.
     */
    private void convertAndAddIssues(HealthReport report, ValidationResult result) {
        for (ValidationIssue issue : result.getIssues()) {
            HealthIssue healthIssue = new HealthIssue();
            healthIssue.setId(issue.getCode());
            healthIssue.setDescription(issue.getMessage());
            healthIssue.setSeverity(issue.getSeverity().name());
            healthIssue.setType(issue.getCategory().name());
            healthIssue.setAffectedFile(issue.getLocation());
            healthIssue.setRecommendation(issue.getRecommendation());
            report.addIssue(healthIssue);
        }
    }
}

