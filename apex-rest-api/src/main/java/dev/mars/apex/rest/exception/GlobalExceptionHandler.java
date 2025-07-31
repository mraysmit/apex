package dev.mars.apex.rest.exception;

import dev.mars.apex.rest.dto.ApiErrorResponse;
import dev.mars.apex.rest.util.TestAwareLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;

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
 * Global exception handler for the APEX REST API.
 * 
 * This class provides centralized exception handling across all controllers,
 * ensuring consistent error responses following RFC 7807 Problem Details format.
 * It includes correlation IDs for request tracking and comprehensive error logging.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-31
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private TestAwareLogger testAwareLogger;
    
    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.validationError(
            "Request validation failed", instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        // Add field-specific errors
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorResponse.addFieldError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
            );
        }
        
        testAwareLogger.warn(logger, "Validation error [{}]: {} field errors for {}",
                           correlationId, errorResponse.getErrors().size(), instance);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle constraint violation errors from @Validated annotations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.validationError(
            "Constraint validation failed", instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        // Add constraint violations
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errorResponse.addFieldError(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()
            );
        }
        
        testAwareLogger.warn(logger, "Constraint violation [{}]: {} violations for {}",
                           correlationId, errorResponse.getErrors().size(), instance);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle bind errors from form data binding.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(
            BindException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.validationError(
            "Data binding failed", instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        // Add field errors
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errorResponse.addFieldError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
            );
        }
        
        testAwareLogger.warn(logger, "Bind exception [{}]: {} field errors for {}",
                           correlationId, errorResponse.getErrors().size(), instance);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle missing request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.badRequest(
            String.format("Required parameter '%s' is missing", ex.getParameterName()),
            instance
        );
        errorResponse.setCorrelationId(correlationId);
        errorResponse.addFieldError(ex.getParameterName(), "Parameter is required");
        
        testAwareLogger.warn(logger, "Missing parameter [{}]: {} for {}",
                           correlationId, ex.getParameterName(), instance);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle method argument type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.badRequest(
            String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName()),
            instance
        );
        errorResponse.setCorrelationId(correlationId);
        errorResponse.addFieldError(ex.getName(), 
            String.format("Expected type: %s", ex.getRequiredType().getSimpleName()),
            ex.getValue());
        
        testAwareLogger.warn(logger, "Type mismatch [{}]: {} for parameter {} in {}",
                           correlationId, ex.getValue(), ex.getName(), instance);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle HTTP message not readable (malformed JSON, etc.).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.badRequest(
            "Malformed request body", instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        testAwareLogger.warn(logger, "Message not readable [{}]: {} for {}",
                           correlationId, ex.getMessage(), instance);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle unsupported HTTP methods.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            "/problems/method-not-allowed",
            "Method Not Allowed",
            405,
            String.format("Method '%s' is not supported for this endpoint", ex.getMethod()),
            instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        testAwareLogger.warn(logger, "Method not supported [{}]: {} for {}",
                           correlationId, ex.getMethod(), instance);
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }
    
    /**
     * Handle unsupported media types.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = new ApiErrorResponse(
            "/problems/unsupported-media-type",
            "Unsupported Media Type",
            415,
            String.format("Media type '%s' is not supported", ex.getContentType()),
            instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        testAwareLogger.warn(logger, "Media type not supported [{}]: {} for {}",
                           correlationId, ex.getContentType(), instance);
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }
    
    /**
     * Handle file upload size exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.badRequest(
            "File upload size exceeded maximum allowed size", instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        testAwareLogger.warn(logger, "Upload size exceeded [{}]: {} for {}",
                           correlationId, ex.getMaxUploadSize(), instance);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle 404 Not Found errors.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.notFound(
            String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
            instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        testAwareLogger.warn(logger, "Handler not found [{}]: {} {} for {}",
                           correlationId, ex.getHttpMethod(), ex.getRequestURL(), instance);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        String instance = request.getRequestURI();
        
        ApiErrorResponse errorResponse = ApiErrorResponse.internalServerError(
            "An unexpected error occurred", instance
        );
        errorResponse.setCorrelationId(correlationId);
        
        testAwareLogger.error(logger, "Unexpected error [{}]: {} for {}",
                            ex, correlationId, ex.getMessage(), instance);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Generate a unique correlation ID for request tracking.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
