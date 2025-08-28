package dev.mars.apex.rest.dto;

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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized API response wrapper for all APEX REST API endpoints.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@Schema(description = "Standard API response wrapper matching documented format")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Schema(description = "Indicates if the operation was successful", example = "true")
    @JsonProperty("success")
    private boolean success;

    @Schema(description = "Response data payload (present on success)")
    @JsonProperty("data")
    private T data;

    @Schema(description = "Error category (only present when success=false)", example = "Validation failed")
    @JsonProperty("error")
    private String error;

    @Schema(description = "Error message (only present when success=false)", example = "Request validation failed")
    @JsonProperty("message")
    private String message;

    @Schema(description = "Response timestamp", example = "2024-01-15T10:30:00Z")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @Schema(description = "Additional error information (only present when success=false)")
    @JsonProperty("additionalInfo")
    private Map<String, Object> additionalInfo;

    // Private constructor to enforce use of static factory methods
    private ApiResponse() {
        this.timestamp = Instant.now();
    }

    /**
     * Create a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.data = data;
        return response;
    }

    /**
     * Create a successful response without data.
     */
    public static ApiResponse<Void> success() {
        ApiResponse<Void> response = new ApiResponse<>();
        response.success = true;
        return response;
    }

    /**
     * Create an error response.
     */
    public static <T> ApiResponse<T> error(String error, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        response.message = message;
        return response;
    }

    /**
     * Create an error response with additional information.
     */
    public static <T> ApiResponse<T> error(String error, String message, Map<String, Object> additionalInfo) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.error = error;
        response.message = message;
        response.additionalInfo = additionalInfo;
        return response;
    }

    /**
     * Add additional information to the response.
     */
    public ApiResponse<T> withAdditionalInfo(String key, Object value) {
        if (this.additionalInfo == null) {
            this.additionalInfo = new HashMap<>();
        }
        this.additionalInfo.put(key, value);
        return this;
    }

    /**
     * Add multiple additional information entries.
     */
    public ApiResponse<T> withAdditionalInfo(Map<String, Object> info) {
        if (this.additionalInfo == null) {
            this.additionalInfo = new HashMap<>();
        }
        this.additionalInfo.putAll(info);
        return this;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    // Setters (for JSON deserialization)
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
