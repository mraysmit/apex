package dev.mars.rulesengine.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Represents the result of a Standing Instruction auto-repair operation.
 * Contains detailed information about which rules were applied, confidence scores,
 * and audit trail information.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-29
 * @version 1.0
 */
public class SIRepairResult {
    
    // Result Identification
    private String resultId;
    private String instructionId;
    private LocalDateTime processedDateTime;
    private String processedBy; // System user or service
    
    // Repair Status
    private boolean repairSuccessful;
    private String repairStatus; // "SUCCESS", "PARTIAL", "FAILED", "SKIPPED"
    private String failureReason;
    
    // Applied Standing Instructions
    private List<StandingInstruction> appliedStandingInstructions;
    private Map<String, String> fieldRepairs; // field name -> repaired value
    private Map<String, StandingInstruction> fieldRepairSources; // field name -> SI that provided the value
    
    // Decision Making Details
    private double totalConfidenceScore;
    private double weightedScore;
    private Map<String, Double> ruleScores; // rule ID -> individual score
    private Map<String, Double> ruleWeights; // rule ID -> weight used
    private String decisionRationale;
    
    // Audit Trail
    private List<String> auditTrail;
    private Map<String, Object> originalValues; // field name -> original value (for rollback)
    private Map<String, Object> repairedValues; // field name -> new value
    
    // Performance Metrics
    private long processingTimeMs;
    private int rulesEvaluated;
    private int rulesMatched;
    private int fieldsRepaired;
    
    // Risk and Compliance
    private String riskAssessment; // "LOW", "MEDIUM", "HIGH"
    private boolean requiresManualReview;
    private String complianceStatus; // "COMPLIANT", "REVIEW_REQUIRED", "NON_COMPLIANT"
    private List<String> complianceWarnings;
    
    // Default constructor
    public SIRepairResult() {
        this.processedDateTime = LocalDateTime.now();
        this.appliedStandingInstructions = new ArrayList<>();
        this.fieldRepairs = new HashMap<>();
        this.fieldRepairSources = new HashMap<>();
        this.ruleScores = new HashMap<>();
        this.ruleWeights = new HashMap<>();
        this.auditTrail = new ArrayList<>();
        this.originalValues = new HashMap<>();
        this.repairedValues = new HashMap<>();
        this.complianceWarnings = new ArrayList<>();
        this.repairSuccessful = false;
        this.repairStatus = "PENDING";
        this.totalConfidenceScore = 0.0;
        this.weightedScore = 0.0;
        this.riskAssessment = "LOW";
        this.requiresManualReview = false;
        this.complianceStatus = "COMPLIANT";
    }
    
    // Constructor with instruction ID
    public SIRepairResult(String instructionId) {
        this();
        this.instructionId = instructionId;
        this.resultId = "REPAIR_" + instructionId + "_" + System.currentTimeMillis();
    }
    
    // Getters and setters
    public String getResultId() {
        return resultId;
    }
    
    public void setResultId(String resultId) {
        this.resultId = resultId;
    }
    
    public String getInstructionId() {
        return instructionId;
    }
    
    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }
    
    public LocalDateTime getProcessedDateTime() {
        return processedDateTime;
    }
    
    public void setProcessedDateTime(LocalDateTime processedDateTime) {
        this.processedDateTime = processedDateTime;
    }
    
    public String getProcessedBy() {
        return processedBy;
    }
    
    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
    
    public boolean isRepairSuccessful() {
        return repairSuccessful;
    }
    
    public void setRepairSuccessful(boolean repairSuccessful) {
        this.repairSuccessful = repairSuccessful;
    }
    
    public String getRepairStatus() {
        return repairStatus;
    }
    
    public void setRepairStatus(String repairStatus) {
        this.repairStatus = repairStatus;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public List<StandingInstruction> getAppliedStandingInstructions() {
        return appliedStandingInstructions;
    }
    
    public void setAppliedStandingInstructions(List<StandingInstruction> appliedStandingInstructions) {
        this.appliedStandingInstructions = appliedStandingInstructions != null ? 
            appliedStandingInstructions : new ArrayList<>();
    }
    
    public Map<String, String> getFieldRepairs() {
        return fieldRepairs;
    }
    
    public void setFieldRepairs(Map<String, String> fieldRepairs) {
        this.fieldRepairs = fieldRepairs != null ? fieldRepairs : new HashMap<>();
    }
    
    public Map<String, StandingInstruction> getFieldRepairSources() {
        return fieldRepairSources;
    }
    
    public void setFieldRepairSources(Map<String, StandingInstruction> fieldRepairSources) {
        this.fieldRepairSources = fieldRepairSources != null ? fieldRepairSources : new HashMap<>();
    }
    
    public double getTotalConfidenceScore() {
        return totalConfidenceScore;
    }
    
    public void setTotalConfidenceScore(double totalConfidenceScore) {
        this.totalConfidenceScore = totalConfidenceScore;
    }
    
    public double getWeightedScore() {
        return weightedScore;
    }
    
    public void setWeightedScore(double weightedScore) {
        this.weightedScore = weightedScore;
    }
    
    public Map<String, Double> getRuleScores() {
        return ruleScores;
    }
    
    public void setRuleScores(Map<String, Double> ruleScores) {
        this.ruleScores = ruleScores != null ? ruleScores : new HashMap<>();
    }
    
    public Map<String, Double> getRuleWeights() {
        return ruleWeights;
    }
    
    public void setRuleWeights(Map<String, Double> ruleWeights) {
        this.ruleWeights = ruleWeights != null ? ruleWeights : new HashMap<>();
    }
    
    public String getDecisionRationale() {
        return decisionRationale;
    }
    
    public void setDecisionRationale(String decisionRationale) {
        this.decisionRationale = decisionRationale;
    }
    
    public List<String> getAuditTrail() {
        return auditTrail;
    }
    
    public void setAuditTrail(List<String> auditTrail) {
        this.auditTrail = auditTrail != null ? auditTrail : new ArrayList<>();
    }
    
    public Map<String, Object> getOriginalValues() {
        return originalValues;
    }
    
    public void setOriginalValues(Map<String, Object> originalValues) {
        this.originalValues = originalValues != null ? originalValues : new HashMap<>();
    }
    
    public Map<String, Object> getRepairedValues() {
        return repairedValues;
    }
    
    public void setRepairedValues(Map<String, Object> repairedValues) {
        this.repairedValues = repairedValues != null ? repairedValues : new HashMap<>();
    }
    
    public long getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    public int getRulesEvaluated() {
        return rulesEvaluated;
    }
    
    public void setRulesEvaluated(int rulesEvaluated) {
        this.rulesEvaluated = rulesEvaluated;
    }
    
    public int getRulesMatched() {
        return rulesMatched;
    }
    
    public void setRulesMatched(int rulesMatched) {
        this.rulesMatched = rulesMatched;
    }
    
    public int getFieldsRepaired() {
        return fieldsRepaired;
    }
    
    public void setFieldsRepaired(int fieldsRepaired) {
        this.fieldsRepaired = fieldsRepaired;
    }
    
    public String getRiskAssessment() {
        return riskAssessment;
    }
    
    public void setRiskAssessment(String riskAssessment) {
        this.riskAssessment = riskAssessment;
    }
    
    public boolean isRequiresManualReview() {
        return requiresManualReview;
    }
    
    public void setRequiresManualReview(boolean requiresManualReview) {
        this.requiresManualReview = requiresManualReview;
    }
    
    public String getComplianceStatus() {
        return complianceStatus;
    }
    
    public void setComplianceStatus(String complianceStatus) {
        this.complianceStatus = complianceStatus;
    }
    
    public List<String> getComplianceWarnings() {
        return complianceWarnings;
    }
    
    public void setComplianceWarnings(List<String> complianceWarnings) {
        this.complianceWarnings = complianceWarnings != null ? complianceWarnings : new ArrayList<>();
    }
