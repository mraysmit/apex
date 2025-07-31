package dev.mars.apex.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
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
 * Standardized API error response following RFC 7807 Problem Details format.
 * 
 * This class provides a consistent error response structure across all API endpoints,
 * including correlation IDs for request tracking and detailed error information.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-31
 * @version 1.0
 */
@Schema(description = "Standardized API error response following RFC 7807 Problem Details format")
public class ApiErrorResponse {
    
    @Schema(description = "URI reference that identifies the problem type", 
            example = "/problems/validation-error")
    @JsonProperty("type")
    private String type;
    
    @Schema(description = "Short, human-readable summary of the problem", 
            example = "Validation Error")
    @JsonProperty("title")
    private String title;
    
    @Schema(description = "HTTP status code", example = "400")
    @JsonProperty("status")
    private int status;
    
    @Schema(description = "Human-readable explanation specific to this occurrence", 
            example = "The request contains invalid data")
    @JsonProperty("detail")
    private String detail;
    
    @Schema(description = "URI reference that identifies the specific occurrence", 
            example = "/api/rules/validate")
    @JsonProperty("instance")
    private String instance;
    
    @Schema(description = "Timestamp when the error occurred")
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @Schema(description = "Unique correlation ID for request tracking")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @Schema(description = "API version that generated this error")
    @JsonProperty("apiVersion")
    private String apiVersion;
    
    @Schema(description = "List of specific validation errors")
    @JsonProperty("errors")
    private List<FieldError> errors;
    
    @Schema(description = "Additional context information")
    @JsonProperty("context")
    private Map<String, Object> context;
    
    // Default constructor
    public ApiErrorResponse() {
        this.timestamp = Instant.now();
        this.errors = new ArrayList<>();
        this.apiVersion = "v1";
    }
    
    // Constructor for basic error
    public ApiErrorResponse(String type, String title, int status, String detail, String instance) {
        this();
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }
    
    // Static factory methods for common error types
    public static ApiErrorResponse validationError(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/validation-error",
            "Validation Error",
            400,
            detail,
            instance
        );
    }
    
    public static ApiErrorResponse notFound(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/not-found",
            "Resource Not Found",
            404,
            detail,
            instance
        );
    }
    
    public static ApiErrorResponse conflict(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/conflict",
            "Resource Conflict",
            409,
            detail,
            instance
        );
    }
    
    public static ApiErrorResponse internalServerError(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/internal-server-error",
            "Internal Server Error",
            500,
            detail,
            instance
        );
    }
    
    public static ApiErrorResponse badRequest(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/bad-request",
            "Bad Request",
            400,
            detail,
            instance
        );
    }
    
    public static ApiErrorResponse unauthorized(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/unauthorized",
            "Unauthorized",
            401,
            detail,
            instance
        );
    }
    
    public static ApiErrorResponse forbidden(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/forbidden",
            "Forbidden",
            403,
            detail,
            instance
        );
    }
    
    public static ApiErrorResponse serviceUnavailable(String detail, String instance) {
        return new ApiErrorResponse(
            "/problems/service-unavailable",
            "Service Unavailable",
            503,
            detail,
            instance
        );
    }
    
    // Helper methods
    public void addFieldError(String field, String message, Object rejectedValue) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(new FieldError(field, message, rejectedValue));
    }
    
    public void addFieldError(String field, String message) {
        addFieldError(field, message, null);
    }
    
    // Getters and setters
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getDetail() {
        return detail;
    }
    
    public void setDetail(String detail) {
        this.detail = detail;
    }
    
    public String getInstance() {
        return instance;
    }
    
    public void setInstance(String instance) {
        this.instance = instance;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getApiVersion() {
        return apiVersion;
    }
    
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
    
    public List<FieldError> getErrors() {
        return errors;
    }
    
    public void setErrors(List<FieldError> errors) {
        this.errors = errors;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    /**
     * Field-specific error information.
     */
    @Schema(description = "Field-specific validation error")
    public static class FieldError {
        
        @Schema(description = "Name of the field that failed validation", example = "age")
        @JsonProperty("field")
        private String field;
        
        @Schema(description = "Error message for this field", example = "must be greater than or equal to 18")
        @JsonProperty("message")
        private String message;
        
        @Schema(description = "The rejected value", example = "16")
        @JsonProperty("rejectedValue")
        private Object rejectedValue;
        
        // Default constructor
        public FieldError() {}
        
        // Constructor
        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
        
        // Getters and setters
        public String getField() {
            return field;
        }
        
        public void setField(String field) {
            this.field = field;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Object getRejectedValue() {
            return rejectedValue;
        }
        
        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
        
        @Override
        public String toString() {
            return "FieldError{" +
                    "field='" + field + '\'' +
                    ", message='" + message + '\'' +
                    ", rejectedValue=" + rejectedValue +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "ApiErrorResponse{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", detail='" + detail + '\'' +
                ", instance='" + instance + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                ", apiVersion='" + apiVersion + '\'' +
                ", errors=" + errors +
                '}';
    }
}
