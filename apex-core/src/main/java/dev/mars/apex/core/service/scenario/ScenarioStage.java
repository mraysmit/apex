package dev.mars.apex.core.service.scenario;

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

import java.util.*;

/**
 * Configuration class representing a single processing stage within a scenario.
 * 
 * Each stage defines a specific phase of trade processing (validation, enrichment, compliance)
 * with explicit execution order, failure policies, and dependency management.
 * 
 * STAGE CONFIGURATION FEATURES:
 * - Explicit execution order and dependencies
 * - Configurable failure policies (terminate, continue-with-warnings, flag-for-review)
 * - Stage metadata for monitoring and SLA tracking
 * - Validation of stage configuration integrity
 * 
 * FAILURE POLICIES:
 * - terminate: Stop processing immediately if stage fails
 * - continue-with-warnings: Log warnings but continue to next stage
 * - flag-for-review: Mark for manual review but continue processing
 * 
 * USAGE EXAMPLE:
 * ```java
 * ScenarioStage validationStage = new ScenarioStage();
 * validationStage.setStageName("validation");
 * validationStage.setConfigFile("config/otc-options-validation-rules.yaml");
 * validationStage.setExecutionOrder(1);
 * validationStage.setFailurePolicy("terminate");
 * validationStage.setRequired(true);
 * ```
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class ScenarioStage {
    
    // Valid failure policy values
    public static final String FAILURE_POLICY_TERMINATE = "terminate";
    public static final String FAILURE_POLICY_CONTINUE_WITH_WARNINGS = "continue-with-warnings";
    public static final String FAILURE_POLICY_FLAG_FOR_REVIEW = "flag-for-review";
    
    public static final Set<String> VALID_FAILURE_POLICIES = Set.of(
        FAILURE_POLICY_TERMINATE,
        FAILURE_POLICY_CONTINUE_WITH_WARNINGS,
        FAILURE_POLICY_FLAG_FOR_REVIEW
    );
    
    private String stageName;
    private String configFile;
    private int executionOrder;
    private String failurePolicy;
    private List<String> dependsOn;
    private boolean required;
    private Map<String, Object> stageMetadata;
    
    // Constructors
    public ScenarioStage() {
        this.dependsOn = new ArrayList<>();
        this.stageMetadata = new HashMap<>();
        this.failurePolicy = FAILURE_POLICY_CONTINUE_WITH_WARNINGS; // Default
        this.required = false; // Default
    }
    
    public ScenarioStage(String stageName, String configFile, int executionOrder) {
        this();
        this.stageName = stageName;
        this.configFile = configFile;
        this.executionOrder = executionOrder;
    }
    
    public ScenarioStage(String stageName, String configFile, int executionOrder, String failurePolicy) {
        this(stageName, configFile, executionOrder);
        this.failurePolicy = failurePolicy;
    }
    
    // Getters and Setters
    public String getStageName() {
        return stageName;
    }
    
    public void setStageName(String stageName) {
        this.stageName = stageName;
    }
    
    public String getConfigFile() {
        return configFile;
    }
    
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }
    
    public int getExecutionOrder() {
        return executionOrder;
    }
    
    public void setExecutionOrder(int executionOrder) {
        this.executionOrder = executionOrder;
    }
    
    public String getFailurePolicy() {
        return failurePolicy;
    }
    
    public void setFailurePolicy(String failurePolicy) {
        this.failurePolicy = failurePolicy;
    }
    
    public List<String> getDependsOn() {
        return dependsOn;
    }
    
    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn != null ? new ArrayList<>(dependsOn) : new ArrayList<>();
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public Map<String, Object> getStageMetadata() {
        return stageMetadata;
    }
    
    public void setStageMetadata(Map<String, Object> stageMetadata) {
        this.stageMetadata = stageMetadata != null ? new HashMap<>(stageMetadata) : new HashMap<>();
    }
    
    // Utility methods
    
    /**
     * Adds a dependency to this stage.
     * 
     * @param dependencyStage the name of the stage this stage depends on
     */
    public void addDependency(String dependencyStage) {
        if (dependencyStage != null && !dependsOn.contains(dependencyStage)) {
            dependsOn.add(dependencyStage);
        }
    }
    
    /**
     * Removes a dependency from this stage.
     * 
     * @param dependencyStage the name of the dependency to remove
     */
    public void removeDependency(String dependencyStage) {
        dependsOn.remove(dependencyStage);
    }
    
    /**
     * Checks if this stage depends on the specified stage.
     * 
     * @param stageName the stage name to check
     * @return true if this stage depends on the specified stage
     */
    public boolean dependsOnStage(String stageName) {
        return dependsOn.contains(stageName);
    }
    
    /**
     * Checks if this stage has any dependencies.
     * 
     * @return true if this stage has dependencies
     */
    public boolean hasDependencies() {
        return !dependsOn.isEmpty();
    }
    
    /**
     * Gets the stage description from metadata.
     * 
     * @return stage description or null if not set
     */
    public String getDescription() {
        return stageMetadata != null ? (String) stageMetadata.get("description") : null;
    }
    
    /**
     * Sets the stage description in metadata.
     * 
     * @param description the stage description
     */
    public void setDescription(String description) {
        if (stageMetadata == null) {
            stageMetadata = new HashMap<>();
        }
        stageMetadata.put("description", description);
    }
    
    /**
     * Gets the stage SLA in milliseconds from metadata.
     * 
     * @return stage SLA in ms or null if not set
     */
    public Integer getSlaMs() {
        Object sla = stageMetadata != null ? stageMetadata.get("sla-ms") : null;
        return sla instanceof Number ? ((Number) sla).intValue() : null;
    }
    
    /**
     * Sets the stage SLA in milliseconds in metadata.
     * 
     * @param slaMs the stage SLA in milliseconds
     */
    public void setSlaMs(Integer slaMs) {
        if (stageMetadata == null) {
            stageMetadata = new HashMap<>();
        }
        stageMetadata.put("sla-ms", slaMs);
    }
    
    /**
     * Checks if this stage is critical (required and terminates on failure).
     * 
     * @return true if this stage is critical
     */
    public boolean isCritical() {
        return required && FAILURE_POLICY_TERMINATE.equals(failurePolicy);
    }
    
    /**
     * Validates the stage configuration.
     * 
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (stageName == null || stageName.trim().isEmpty()) {
            errors.add("Stage name is required");
        }
        
        if (configFile == null || configFile.trim().isEmpty()) {
            errors.add("Config file is required");
        }
        
        if (executionOrder < 1) {
            errors.add("Execution order must be a positive integer");
        }
        
        if (failurePolicy != null && !VALID_FAILURE_POLICIES.contains(failurePolicy)) {
            errors.add("Invalid failure policy: " + failurePolicy + 
                      ". Valid policies: " + VALID_FAILURE_POLICIES);
        }
        
        // Check for self-dependency
        if (dependsOn.contains(stageName)) {
            errors.add("Stage cannot depend on itself");
        }
        
        return errors;
    }
    
    /**
     * Checks if the stage configuration is valid.
     * 
     * @return true if the stage configuration is valid
     */
    public boolean isValid() {
        return validate().isEmpty();
    }
    
    @Override
    public String toString() {
        return "ScenarioStage{" +
                "stageName='" + stageName + '\'' +
                ", configFile='" + configFile + '\'' +
                ", executionOrder=" + executionOrder +
                ", failurePolicy='" + failurePolicy + '\'' +
                ", dependsOn=" + dependsOn +
                ", required=" + required +
                ", description='" + getDescription() + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioStage that = (ScenarioStage) o;
        return executionOrder == that.executionOrder &&
                required == that.required &&
                Objects.equals(stageName, that.stageName) &&
                Objects.equals(configFile, that.configFile) &&
                Objects.equals(failurePolicy, that.failurePolicy) &&
                Objects.equals(dependsOn, that.dependsOn);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(stageName, configFile, executionOrder, failurePolicy, dependsOn, required);
    }
}
