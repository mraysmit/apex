package dev.mars.apex.demo.bootstrap.model;

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
 * Enhanced SI Repair Result model for the Custody Auto-Repair Bootstrap.
 * This class contains comprehensive repair results with audit trails,
 * performance metrics, and decision rationale.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 1.0
 */
public class BootstrapSIRepairResult {
    
    // Result Identification
    private String resultId;
    private String instructionId;
    private LocalDateTime processedDateTime;
    private String processedBy;
    private String processingVersion;
    
    // Repair Status
    private boolean repairSuccessful;
    private String repairStatus; // "SUCCESS", "PARTIAL", "FAILED", "SKIPPED"
    private String failureReason;
    private String skipReason;
    
    // Applied Standing Instructions
    private List<BootstrapStandingInstruction> appliedStandingInstructions;
    private Map<String, String> fieldRepairs; // field name -> repaired value
    private Map<String, BootstrapStandingInstruction> fieldRepairSources; // field -> SI source
    
    // Decision Making Details
    private double totalConfidenceScore;
    private double weightedScore;
    private Map<String, Double> ruleScores; // rule ID -> individual score
    private Map<String, Double> ruleWeights; // rule ID -> weight used
    private String decisionRationale;
    private String finalDecision; // "REPAIR_APPROVED", "PARTIAL_REPAIR", "MANUAL_REVIEW_REQUIRED"
    
    // Audit Trail
    private List<String> auditTrail;
    private Map<String, Object> originalValues;
    private Map<String, Object> repairedValues;
    private List<String> decisionSteps;
    
    // Performance Metrics
    private long processingTimeMs;
    private int rulesEvaluated;
    private int rulesMatched;
    private int fieldsRepaired;
    private int enrichmentsApplied;
    
    // Risk and Compliance
    private String riskAssessment;
    private boolean requiresManualReview;
    private String complianceStatus;
    private List<String> complianceWarnings;
    private String regulatoryClassification;
    
    // Business Context
    private String businessUnit;
    private String market;
    private String clientTier;
    private String instrumentType;
    
    // Default constructor
    public BootstrapSIRepairResult() {
        this.appliedStandingInstructions = new ArrayList<>();
        this.fieldRepairs = new HashMap<>();
        this.fieldRepairSources = new HashMap<>();
        this.ruleScores = new HashMap<>();
        this.ruleWeights = new HashMap<>();
        this.auditTrail = new ArrayList<>();
        this.originalValues = new HashMap<>();
        this.repairedValues = new HashMap<>();
        this.decisionSteps = new ArrayList<>();
        this.complianceWarnings = new ArrayList<>();
        this.processedDateTime = LocalDateTime.now();
        this.processingVersion = "1.0";
    }
    
    // Constructor with instruction ID
    public BootstrapSIRepairResult(String instructionId) {
        this();
        this.instructionId = instructionId;
        this.resultId = "RESULT_" + instructionId + "_" + System.currentTimeMillis();
    }
    
    /**
     * Mark the repair as successful.
     */
    public void markAsSuccessful(String message) {
        this.repairSuccessful = true;
        this.repairStatus = "SUCCESS";
        this.decisionRationale = message;
        addAuditEntry("REPAIR_COMPLETED", message);
    }
    
    /**
     * Mark the repair as partial.
     */
    public void markAsPartial(String message) {
        this.repairSuccessful = true;
        this.repairStatus = "PARTIAL";
        this.decisionRationale = message;
        addAuditEntry("PARTIAL_REPAIR_COMPLETED", message);
    }
    
    /**
     * Mark the repair as failed.
     */
    public void markAsFailed(String reason) {
        this.repairSuccessful = false;
        this.repairStatus = "FAILED";
        this.failureReason = reason;
        addAuditEntry("REPAIR_FAILED", reason);
    }
    
    /**
     * Mark the repair as skipped.
     */
    public void markAsSkipped(String reason) {
        this.repairSuccessful = false;
        this.repairStatus = "SKIPPED";
        this.skipReason = reason;
        addAuditEntry("REPAIR_SKIPPED", reason);
    }
    
    /**
     * Add an applied standing instruction.
     */
    public void addAppliedStandingInstruction(BootstrapStandingInstruction si) {
        if (!appliedStandingInstructions.contains(si)) {
            appliedStandingInstructions.add(si);
            addAuditEntry("SI_APPLIED", "Applied SI: " + si.getSiName() + " (Scope: " + si.getScopeType() + ")");
        }
    }
    
    /**
     * Add a field repair.
     */
    public void addFieldRepair(String fieldName, String repairedValue, BootstrapStandingInstruction source) {
        fieldRepairs.put(fieldName, repairedValue);
        fieldRepairSources.put(fieldName, source);
        repairedValues.put(fieldName, repairedValue);
        fieldsRepaired++;
        addAuditEntry("FIELD_REPAIRED", "Field '" + fieldName + "' repaired with value '" + repairedValue + "' from SI: " + source.getSiName());
    }
    
    /**
     * Add a rule score.
     */
    public void addRuleScore(String ruleId, double score, double weight) {
        ruleScores.put(ruleId, score);
        ruleWeights.put(ruleId, weight);
        addDecisionStep("Rule '" + ruleId + "' scored " + score + " with weight " + weight);
    }
    
    /**
     * Calculate final weighted scores.
     */
    public void calculateFinalScores() {
        totalConfidenceScore = ruleScores.values().stream().mapToDouble(Double::doubleValue).sum();
        weightedScore = ruleScores.entrySet().stream()
            .mapToDouble(entry -> entry.getValue() * ruleWeights.getOrDefault(entry.getKey(), 1.0))
            .sum();
        
        addDecisionStep("Total confidence score: " + totalConfidenceScore);
        addDecisionStep("Weighted score: " + weightedScore);
        
        // Determine final decision based on weighted score
        if (weightedScore >= 50) {
            finalDecision = "REPAIR_APPROVED";
        } else if (weightedScore >= 20) {
            finalDecision = "PARTIAL_REPAIR";
        } else {
            finalDecision = "MANUAL_REVIEW_REQUIRED";
        }
        
        addDecisionStep("Final decision: " + finalDecision);
    }
    
    /**
     * Check if a field has been repaired.
     */
    public boolean hasFieldRepair(String fieldName) {
        return fieldRepairs.containsKey(fieldName);
    }
    
    /**
     * Get the repaired value for a field.
     */
    public String getFieldRepairValue(String fieldName) {
        return fieldRepairs.get(fieldName);
    }
    
    /**
     * Get the source SI for a field repair.
     */
    public BootstrapStandingInstruction getFieldRepairSource(String fieldName) {
        return fieldRepairSources.get(fieldName);
    }
    
    /**
     * Add an audit trail entry.
     */
    public void addAuditEntry(String action, String details) {
        String timestamp = LocalDateTime.now().toString();
        auditTrail.add(timestamp + " - " + action + ": " + details);
    }
    
    /**
     * Add a decision step.
     */
    public void addDecisionStep(String step) {
        decisionSteps.add(step);
    }
    
    /**
     * Store original value before repair.
     */
    public void storeOriginalValue(String fieldName, Object value) {
        originalValues.put(fieldName, value);
    }
    
    /**
     * Add a compliance warning.
     */
    public void addComplianceWarning(String warning) {
        complianceWarnings.add(warning);
    }
    
    /**
     * Set processing time.
     */
    public void setProcessingTime(long startTime) {
        this.processingTimeMs = System.currentTimeMillis() - startTime;
    }
    
    // Getters and Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    
    public String getInstructionId() { return instructionId; }
    public void setInstructionId(String instructionId) { this.instructionId = instructionId; }
    
    public LocalDateTime getProcessedDateTime() { return processedDateTime; }
    public void setProcessedDateTime(LocalDateTime processedDateTime) { this.processedDateTime = processedDateTime; }
    
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    
    public String getProcessingVersion() { return processingVersion; }
    public void setProcessingVersion(String processingVersion) { this.processingVersion = processingVersion; }
    
    public boolean isRepairSuccessful() { return repairSuccessful; }
    public void setRepairSuccessful(boolean repairSuccessful) { this.repairSuccessful = repairSuccessful; }
    
    public String getRepairStatus() { return repairStatus; }
    public void setRepairStatus(String repairStatus) { this.repairStatus = repairStatus; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
    
    public List<BootstrapStandingInstruction> getAppliedStandingInstructions() { return appliedStandingInstructions; }
    public void setAppliedStandingInstructions(List<BootstrapStandingInstruction> appliedStandingInstructions) { this.appliedStandingInstructions = appliedStandingInstructions; }
    
    public Map<String, String> getFieldRepairs() { return fieldRepairs; }
    public void setFieldRepairs(Map<String, String> fieldRepairs) { this.fieldRepairs = fieldRepairs; }
    
    public Map<String, BootstrapStandingInstruction> getFieldRepairSources() { return fieldRepairSources; }
    public void setFieldRepairSources(Map<String, BootstrapStandingInstruction> fieldRepairSources) { this.fieldRepairSources = fieldRepairSources; }
    
    public double getTotalConfidenceScore() { return totalConfidenceScore; }
    public void setTotalConfidenceScore(double totalConfidenceScore) { this.totalConfidenceScore = totalConfidenceScore; }
    
    public double getWeightedScore() { return weightedScore; }
    public void setWeightedScore(double weightedScore) { this.weightedScore = weightedScore; }
    
    public Map<String, Double> getRuleScores() { return ruleScores; }
    public void setRuleScores(Map<String, Double> ruleScores) { this.ruleScores = ruleScores; }
    
    public Map<String, Double> getRuleWeights() { return ruleWeights; }
    public void setRuleWeights(Map<String, Double> ruleWeights) { this.ruleWeights = ruleWeights; }
    
    public String getDecisionRationale() { return decisionRationale; }
    public void setDecisionRationale(String decisionRationale) { this.decisionRationale = decisionRationale; }
    
    public String getFinalDecision() { return finalDecision; }
    public void setFinalDecision(String finalDecision) { this.finalDecision = finalDecision; }
    
    public List<String> getAuditTrail() { return auditTrail; }
    public void setAuditTrail(List<String> auditTrail) { this.auditTrail = auditTrail; }
    
    public Map<String, Object> getOriginalValues() { return originalValues; }
    public void setOriginalValues(Map<String, Object> originalValues) { this.originalValues = originalValues; }
    
    public Map<String, Object> getRepairedValues() { return repairedValues; }
    public void setRepairedValues(Map<String, Object> repairedValues) { this.repairedValues = repairedValues; }
    
    public List<String> getDecisionSteps() { return decisionSteps; }
    public void setDecisionSteps(List<String> decisionSteps) { this.decisionSteps = decisionSteps; }
    
    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public int getRulesEvaluated() { return rulesEvaluated; }
    public void setRulesEvaluated(int rulesEvaluated) { this.rulesEvaluated = rulesEvaluated; }
    
    public int getRulesMatched() { return rulesMatched; }
    public void setRulesMatched(int rulesMatched) { this.rulesMatched = rulesMatched; }
    
    public int getFieldsRepaired() { return fieldsRepaired; }
    public void setFieldsRepaired(int fieldsRepaired) { this.fieldsRepaired = fieldsRepaired; }
    
    public int getEnrichmentsApplied() { return enrichmentsApplied; }
    public void setEnrichmentsApplied(int enrichmentsApplied) { this.enrichmentsApplied = enrichmentsApplied; }
    
    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }
    
    public boolean isRequiresManualReview() { return requiresManualReview; }
    public void setRequiresManualReview(boolean requiresManualReview) { this.requiresManualReview = requiresManualReview; }
    
    public String getComplianceStatus() { return complianceStatus; }
    public void setComplianceStatus(String complianceStatus) { this.complianceStatus = complianceStatus; }
    
    public List<String> getComplianceWarnings() { return complianceWarnings; }
    public void setComplianceWarnings(List<String> complianceWarnings) { this.complianceWarnings = complianceWarnings; }
    
    public String getRegulatoryClassification() { return regulatoryClassification; }
    public void setRegulatoryClassification(String regulatoryClassification) { this.regulatoryClassification = regulatoryClassification; }
    
    public String getBusinessUnit() { return businessUnit; }
    public void setBusinessUnit(String businessUnit) { this.businessUnit = businessUnit; }
    
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    
    public String getClientTier() { return clientTier; }
    public void setClientTier(String clientTier) { this.clientTier = clientTier; }
    
    public String getInstrumentType() { return instrumentType; }
    public void setInstrumentType(String instrumentType) { this.instrumentType = instrumentType; }
}
