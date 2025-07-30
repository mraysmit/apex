package dev.mars.apex.core.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
 * Result of a validation operation containing success status and error messages.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Result of a validation operation containing success status and error messages.
 * This class provides detailed information about validation outcomes.
 * 
 * <p>Examples:</p>
 * <pre>
 * ValidationResult result = Rules.validate(customer)
 *     .minimumAge(18)
 *     .emailRequired()
 *     .validate();
 * 
 * if (result.isValid()) {
 *     System.out.println("All validations passed!");
 * } else {
 *     System.out.println("Validation failed:");
 *     result.getErrors().forEach(System.out::println);
 * }
 * </pre>
 */
public class ValidationResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final boolean valid;
    private final List<String> errors;
    
    /**
     * Create a validation result.
     * 
     * @param valid Whether the validation passed
     * @param errors List of error messages (empty if valid)
     */
    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }
    
    /**
     * Create a successful validation result.
     * 
     * @return A validation result indicating success
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }
    
    /**
     * Create a failed validation result with a single error.
     * 
     * @param error The error message
     * @return A validation result indicating failure
     */
    public static ValidationResult failure(String error) {
        return new ValidationResult(false, Collections.singletonList(error));
    }
    
    /**
     * Create a failed validation result with multiple errors.
     * 
     * @param errors The error messages
     * @return A validation result indicating failure
     */
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }
    
    /**
     * Check if the validation was successful.
     * 
     * @return true if all validations passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Check if the validation failed.
     * 
     * @return true if any validation failed, false if all passed
     */
    public boolean isInvalid() {
        return !valid;
    }
    
    /**
     * Get all validation error messages.
     * 
     * @return Immutable list of error messages (empty if validation passed)
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * Get the first validation error message.
     * 
     * @return The first error message, or null if validation passed
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0);
    }
    
    /**
     * Get the number of validation errors.
     * 
     * @return The number of errors (0 if validation passed)
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * Check if there are any validation errors.
     * 
     * @return true if there are errors, false otherwise
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Get all error messages as a single string.
     * 
     * @return All error messages joined with newlines, or empty string if no errors
     */
    public String getErrorsAsString() {
        return String.join("\n", errors);
    }
    
    /**
     * Get all error messages as a single string with custom separator.
     * 
     * @param separator The separator to use between error messages
     * @return All error messages joined with the separator, or empty string if no errors
     */
    public String getErrorsAsString(String separator) {
        return String.join(separator, errors);
    }
    
    /**
     * Combine this validation result with another.
     * The combined result is valid only if both results are valid.
     * Error messages from both results are included.
     * 
     * @param other The other validation result to combine with
     * @return A new validation result combining both results
     */
    public ValidationResult combine(ValidationResult other) {
        if (other == null) {
            return this;
        }
        
        boolean combinedValid = this.valid && other.valid;
        List<String> combinedErrors = new ArrayList<>(this.errors);
        combinedErrors.addAll(other.errors);
        
        return new ValidationResult(combinedValid, combinedErrors);
    }
    
    /**
     * Add an additional error to this validation result.
     * This creates a new ValidationResult with the additional error.
     * 
     * @param error The additional error message
     * @return A new validation result with the additional error
     */
    public ValidationResult addError(String error) {
        List<String> newErrors = new ArrayList<>(this.errors);
        newErrors.add(error);
        return new ValidationResult(false, newErrors);
    }
    
    /**
     * Create a summary of the validation result.
     * 
     * @return A human-readable summary of the validation result
     */
    public String getSummary() {
        if (valid) {
            return "Validation passed";
        } else {
            return String.format("Validation failed with %d error(s): %s", 
                               errors.size(), 
                               getErrorsAsString("; "));
        }
    }
    
    /**
     * Throw an exception if validation failed.
     * This is useful for fail-fast validation scenarios.
     * 
     * @throws ValidationException if validation failed
     */
    public void throwIfInvalid() throws ValidationException {
        if (!valid) {
            throw new ValidationException("Validation failed", errors);
        }
    }
    
    /**
     * Throw an exception with a custom message if validation failed.
     * 
     * @param message The custom error message
     * @throws ValidationException if validation failed
     */
    public void throwIfInvalid(String message) throws ValidationException {
        if (!valid) {
            throw new ValidationException(message, errors);
        }
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid && Objects.equals(errors, that.errors);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errors);
    }
    
    /**
     * Exception thrown when validation fails and throwIfInvalid() is called.
     */
    public static class ValidationException extends Exception {
        private static final long serialVersionUID = 1L;
        
        private final List<String> validationErrors;
        
        public ValidationException(String message, List<String> validationErrors) {
            super(message);
            this.validationErrors = new ArrayList<>(validationErrors);
        }
        
        /**
         * Get the validation errors that caused this exception.
         * 
         * @return List of validation error messages
         */
        public List<String> getValidationErrors() {
            return Collections.unmodifiableList(validationErrors);
        }
        
        /**
         * Get all validation errors as a single string.
         * 
         * @return All validation errors joined with newlines
         */
        public String getValidationErrorsAsString() {
            return String.join("\n", validationErrors);
        }
        
        @Override
        public String getMessage() {
            return super.getMessage() + ": " + getValidationErrorsAsString();
        }
    }
}
