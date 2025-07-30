package dev.mars.apex.core.exception;

import java.util.List;
import java.util.ArrayList;

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
 * Exception thrown when rule validation fails.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class RuleValidationException extends RuleEngineException {
    private static final long serialVersionUID = 1L;
    
    private final String ruleName;
    private final List<ValidationError> validationErrors;
    
    public RuleValidationException(String ruleName, String message) {
        super("RULE_VALIDATION_ERROR", message, "Rule: " + ruleName);
        this.ruleName = ruleName;
        this.validationErrors = new ArrayList<>();
    }
    
    public RuleValidationException(String ruleName, List<ValidationError> validationErrors) {
        super("RULE_VALIDATION_ERROR", buildMessage(validationErrors), "Rule: " + ruleName);
        this.ruleName = ruleName;
        this.validationErrors = new ArrayList<>(validationErrors);
    }
    
    public String getRuleName() {
        return ruleName;
    }
    
    public List<ValidationError> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }
    
    public void addValidationError(ValidationError error) {
        this.validationErrors.add(error);
    }
    
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }
    
    private static String buildMessage(List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return "Rule validation failed";
        }
        
        StringBuilder sb = new StringBuilder("Rule validation failed with ");
        sb.append(errors.size()).append(" error(s):");
        for (ValidationError error : errors) {
            sb.append("\n- ").append(error.getMessage());
        }
        return sb.toString();
    }
    
    @Override
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder(super.getDetailedMessage());
        if (!validationErrors.isEmpty()) {
            sb.append("\nValidation Errors:");
            for (ValidationError error : validationErrors) {
                sb.append("\n- ").append(error.getField()).append(": ").append(error.getMessage());
                if (error.getSuggestion() != null) {
                    sb.append(" (Suggestion: ").append(error.getSuggestion()).append(")");
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * Represents a single validation error with field context and suggestions.
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        private final String suggestion;
        
        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
            this.suggestion = null;
        }
        
        public ValidationError(String field, String message, String suggestion) {
            this.field = field;
            this.message = message;
            this.suggestion = suggestion;
        }
        
        public String getField() {
            return field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
    }
}
