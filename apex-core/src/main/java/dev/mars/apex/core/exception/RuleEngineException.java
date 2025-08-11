package dev.mars.apex.core.exception;

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
 * Base exception class for all rules engine related exceptions.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Base exception class for all rules engine related exceptions.
 * Provides a hierarchy for different types of rule engine errors.
 */
public class RuleEngineException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final String errorCode;
    private final String context;
    
    public RuleEngineException(String message) {
        super(message);
        this.errorCode = "GENERAL_ERROR";
        this.context = null;
    }
    
    public RuleEngineException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERAL_ERROR";
        this.context = null;
    }
    
    public RuleEngineException(String errorCode, String message, String context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    public RuleEngineException(String errorCode, String message, String context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getContext() {
        return context;
    }
    
    /**
     * Get a detailed error message including error code and context.
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorCode).append("] ").append(getMessage());
        if (context != null && !context.trim().isEmpty()) {
            sb.append(" (Context: ").append(context).append(")");
        }
        return sb.toString();
    }
}
