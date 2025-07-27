package dev.mars.rulesengine.core.engine.model.metadata;

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
 * Enumeration of possible rule statuses in the lifecycle.
 *
 * This enum is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public enum RuleStatus {
    /**
     * Rule is in draft state and not yet active.
     */
    DRAFT("Draft", "Rule is being developed and is not yet active"),
    
    /**
     * Rule is under review/approval process.
     */
    PENDING_APPROVAL("Pending Approval", "Rule is awaiting approval before activation"),
    
    /**
     * Rule is active and being executed.
     */
    ACTIVE("Active", "Rule is active and being executed"),
    
    /**
     * Rule is temporarily disabled but can be reactivated.
     */
    INACTIVE("Inactive", "Rule is temporarily disabled"),
    
    /**
     * Rule is deprecated and should not be used for new implementations.
     */
    DEPRECATED("Deprecated", "Rule is deprecated and should be replaced"),
    
    /**
     * Rule has been retired and is no longer available.
     */
    RETIRED("Retired", "Rule has been permanently retired"),
    
    /**
     * Rule is being tested in a controlled environment.
     */
    TESTING("Testing", "Rule is being tested before full activation"),
    
    /**
     * Rule has failed validation or execution and needs attention.
     */
    ERROR("Error", "Rule has encountered errors and needs attention");
    
    private final String displayName;
    private final String description;
    
    RuleStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the rule status allows execution.
     */
    public boolean isExecutable() {
        return this == ACTIVE || this == TESTING;
    }
    
    /**
     * Check if the rule status allows modification.
     */
    public boolean isModifiable() {
        return this == DRAFT || this == INACTIVE || this == TESTING;
    }
    
    /**
     * Get the next logical status in the workflow.
     */
    public RuleStatus getNextStatus() {
        return switch (this) {
            case DRAFT -> PENDING_APPROVAL;
            case PENDING_APPROVAL -> ACTIVE;
            case TESTING -> ACTIVE;
            case ACTIVE -> INACTIVE;
            case INACTIVE -> ACTIVE;
            case DEPRECATED -> RETIRED;
            default -> this; // No logical next status
        };
    }
}
