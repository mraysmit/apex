package dev.mars.apex.core.service.data.external.database;

import java.sql.SQLException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility class for classifying SQL errors and determining appropriate handling strategies.
 * 
 * This class helps distinguish between different types of database errors:
 * - Data integrity violations (should be handled gracefully)
 * - Configuration errors (should fail fast)
 * - Transient errors (should be retried)
 * - Fatal errors (should stop processing)
 */
public class SqlErrorClassifier {
    
    private static final Logger LOGGER = Logger.getLogger(SqlErrorClassifier.class.getName());
    
    /**
     * SQL State codes for constraint violations that should be handled gracefully.
     * These represent data quality issues, not system failures.
     */
    private static final Set<String> CONSTRAINT_VIOLATION_STATES = Set.of(
        "23000", // Integrity constraint violation (generic)
        "23001", // Restrict violation
        "23502", // Not null violation
        "23503", // Foreign key violation
        "23505", // Unique violation (primary key, unique constraint)
        "23514"  // Check constraint violation
    );
    
    /**
     * SQL State codes for transient errors that should be retried.
     */
    private static final Set<String> TRANSIENT_ERROR_STATES = Set.of(
        "08000", // Connection exception
        "08001", // SQL client unable to establish connection
        "08003", // Connection does not exist
        "08004", // SQL server rejected establishment of connection
        "08006", // Connection failure
        "08007", // Connection failure during transaction
        "40001", // Serialization failure
        "40P01", // Deadlock detected (PostgreSQL)
        "57P01", // Admin shutdown (PostgreSQL)
        "57P02", // Crash shutdown (PostgreSQL)
        "57P03"  // Cannot connect now (PostgreSQL)
    );
    
    /**
     * SQL State codes for configuration/schema errors that should fail fast.
     */
    private static final Set<String> CONFIGURATION_ERROR_STATES = Set.of(
        "42000", // Syntax error or access rule violation
        "42S01", // Base table or view already exists
        "42S02", // Base table or view not found
        "42S11", // Index already exists
        "42S12", // Index not found
        "42S21", // Column already exists
        "42S22"  // Column not found
    );
    
    /**
     * H2-specific error codes for constraint violations.
     */
    private static final Set<Integer> H2_CONSTRAINT_VIOLATION_CODES = Set.of(
        23505, // Unique index or primary key violation
        23502, // NULL not allowed for column
        23503, // Referential integrity constraint violation
        23513, // Check constraint violation
        90117  // H2 specific: Unique index or primary key violation
    );
    
    /**
     * Classification of SQL error types.
     */
    public enum SqlErrorType {
        /**
         * Data integrity violations - should be logged and handled gracefully.
         * Examples: Primary key violations, unique constraint violations, foreign key violations.
         */
        DATA_INTEGRITY_VIOLATION,
        
        /**
         * Transient errors - should be retried with backoff.
         * Examples: Connection timeouts, deadlocks, temporary resource unavailability.
         */
        TRANSIENT_ERROR,
        
        /**
         * Configuration errors - should fail fast and require intervention.
         * Examples: Table not found, column not found, syntax errors.
         */
        CONFIGURATION_ERROR,
        
        /**
         * Unknown/Fatal errors - should be escalated.
         * Examples: Unexpected database errors, system failures.
         */
        FATAL_ERROR
    }
    
    /**
     * Classify a SQLException to determine the appropriate handling strategy.
     * 
     * @param e The SQLException to classify
     * @return The classification of the error
     */
    public static SqlErrorType classifyError(SQLException e) {
        if (e == null) {
            return SqlErrorType.FATAL_ERROR;
        }
        
        String sqlState = e.getSQLState();
        int errorCode = e.getErrorCode();
        String message = e.getMessage();
        
        LOGGER.fine(String.format("Classifying SQL error: SQLState=%s, ErrorCode=%d, Message=%s", 
                                 sqlState, errorCode, message));
        
        // Check for constraint violations first (most common case for graceful handling)
        if (isConstraintViolation(sqlState, errorCode, message)) {
            LOGGER.fine("Classified as DATA_INTEGRITY_VIOLATION");
            return SqlErrorType.DATA_INTEGRITY_VIOLATION;
        }
        
        // Check for transient errors
        if (isTransientError(sqlState, errorCode, message)) {
            LOGGER.fine("Classified as TRANSIENT_ERROR");
            return SqlErrorType.TRANSIENT_ERROR;
        }
        
        // Check for configuration errors
        if (isConfigurationError(sqlState, errorCode, message)) {
            LOGGER.fine("Classified as CONFIGURATION_ERROR");
            return SqlErrorType.CONFIGURATION_ERROR;
        }
        
        // Default to fatal error for unknown cases
        LOGGER.fine("Classified as FATAL_ERROR (unknown error type)");
        return SqlErrorType.FATAL_ERROR;
    }
    
    /**
     * Check if the error is a constraint violation.
     */
    private static boolean isConstraintViolation(String sqlState, int errorCode, String message) {
        // Check SQL State
        if (sqlState != null && CONSTRAINT_VIOLATION_STATES.contains(sqlState)) {
            return true;
        }
        
        // Check H2-specific error codes
        if (H2_CONSTRAINT_VIOLATION_CODES.contains(errorCode)) {
            return true;
        }
        
        // Check message patterns for common constraint violations
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("primary key") ||
                   lowerMessage.contains("unique constraint") ||
                   lowerMessage.contains("foreign key") ||
                   lowerMessage.contains("check constraint") ||
                   lowerMessage.contains("not null") ||
                   lowerMessage.contains("duplicate key") ||
                   lowerMessage.contains("integrity constraint");
        }
        
        return false;
    }
    
    /**
     * Check if the error is transient and should be retried.
     */
    private static boolean isTransientError(String sqlState, int errorCode, String message) {
        // Check SQL State
        if (sqlState != null && TRANSIENT_ERROR_STATES.contains(sqlState)) {
            return true;
        }
        
        // Check message patterns for transient errors
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("connection") ||
                   lowerMessage.contains("timeout") ||
                   lowerMessage.contains("deadlock") ||
                   lowerMessage.contains("lock") ||
                   lowerMessage.contains("busy") ||
                   lowerMessage.contains("unavailable");
        }
        
        return false;
    }
    
    /**
     * Check if the error is a configuration error.
     */
    private static boolean isConfigurationError(String sqlState, int errorCode, String message) {
        // Check SQL State
        if (sqlState != null && CONFIGURATION_ERROR_STATES.contains(sqlState)) {
            return true;
        }
        
        // Check message patterns for configuration errors
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("table") && (lowerMessage.contains("not found") || lowerMessage.contains("doesn't exist")) ||
                   lowerMessage.contains("column") && (lowerMessage.contains("not found") || lowerMessage.contains("doesn't exist")) ||
                   lowerMessage.contains("syntax error") ||
                   lowerMessage.contains("unknown column") ||
                   lowerMessage.contains("unknown table");
        }
        
        return false;
    }
    
    /**
     * Get a user-friendly description of the error type.
     */
    public static String getErrorDescription(SqlErrorType errorType) {
        switch (errorType) {
            case DATA_INTEGRITY_VIOLATION:
                return "Data integrity violation - record conflicts with existing data or constraints";
            case TRANSIENT_ERROR:
                return "Transient database error - operation can be retried";
            case CONFIGURATION_ERROR:
                return "Database configuration error - schema or query issue";
            case FATAL_ERROR:
            default:
                return "Fatal database error - requires investigation";
        }
    }
    
    /**
     * Determine if an error should cause the entire pipeline to fail.
     */
    public static boolean shouldFailPipeline(SqlErrorType errorType) {
        switch (errorType) {
            case DATA_INTEGRITY_VIOLATION:
                return false; // Log and continue
            case TRANSIENT_ERROR:
                return false; // Retry, then continue if retries exhausted
            case CONFIGURATION_ERROR:
                return true;  // Fail fast - requires intervention
            case FATAL_ERROR:
            default:
                return true;  // Fail fast - unknown error
        }
    }
}
