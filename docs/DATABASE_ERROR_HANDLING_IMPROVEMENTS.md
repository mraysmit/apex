# Database Error Handling Improvements

## Overview

This document describes the comprehensive improvements made to database error handling in the APEX rules engine to prevent system crashes due to data configuration errors like primary key violations.

## Problem Statement

Previously, database errors such as primary key violations, unique constraint violations, and other data integrity issues would cause the entire pipeline to crash with unhandled exceptions. This was problematic because:

1. **Data integrity violations are configuration issues**, not system failures
2. **Pipelines should be resilient** to individual record failures
3. **Different error types require different handling strategies**
4. **Users need meaningful error messages** to understand what went wrong

## Solution Architecture

### 1. SQL Error Classification System

Created a new `SqlErrorClassifier` utility class that categorizes SQL errors into four types:

#### Error Types:
- **DATA_INTEGRITY_VIOLATION**: Primary key conflicts, unique constraints, foreign key violations, not null violations
- **TRANSIENT_ERROR**: Connection timeouts, deadlocks, temporary resource unavailability  
- **CONFIGURATION_ERROR**: Table not found, column not found, syntax errors
- **FATAL_ERROR**: Unknown/unexpected database errors

#### Classification Logic:
- **SQL State codes**: Uses standard SQL State codes (23505, 23502, etc.)
- **Database-specific error codes**: H2, PostgreSQL specific codes
- **Message pattern matching**: Fallback for cases where SQL State is missing

### 2. Enhanced Exception Handling

#### New DataSinkException Type:
- Added `DATA_INTEGRITY_ERROR` to the `DataSinkException.ErrorType` enum
- Created factory methods for creating data integrity exceptions
- Made data integrity errors non-retryable by default

#### Graceful Pipeline Execution:
- Updated `PipelineExecutor` to handle data integrity violations gracefully
- Records with integrity violations are logged and skipped
- Pipeline continues processing remaining records
- Provides summary statistics (success count, skipped count)

### 3. Comprehensive Error Handling Updates

Updated error handling in multiple components:

#### DatabaseDataSink:
- **write() method**: Classifies SQL errors and handles appropriately
- **writeBatch() method**: Enhanced batch processing with per-item error classification
- **execute() method**: Improved error classification for operations

#### DatabaseDataSource:
- **query() method**: Better error classification for read operations
- **batchUpdate() method**: Enhanced batch update error handling

#### PipelineExecutor:
- **executeLoadStep()**: Graceful handling of data integrity violations
- **Batch processing**: Continues processing even when individual records fail
- **Detailed logging**: Provides clear information about skipped records

## Implementation Details

### SqlErrorClassifier Class

```java
public class SqlErrorClassifier {
    public enum SqlErrorType {
        DATA_INTEGRITY_VIOLATION,
        TRANSIENT_ERROR,
        CONFIGURATION_ERROR,
        FATAL_ERROR
    }
    
    public static SqlErrorType classifyError(SQLException e);
    public static String getErrorDescription(SqlErrorType errorType);
    public static boolean shouldFailPipeline(SqlErrorType errorType);
}
```

### Enhanced Pipeline Execution

```java
// Before: Pipeline would crash on first primary key violation
// After: Pipeline logs violation and continues processing

for (Object record : dataList) {
    try {
        dataSink.write(step.getOperation(), record);
        successCount++;
    } catch (DataSinkException e) {
        if (e.getErrorType() == DataSinkException.ErrorType.DATA_INTEGRITY_ERROR) {
            LOGGER.warn("Skipping record due to data integrity violation: {}", e.getMessage());
            skippedCount++;
        } else {
            throw e; // Re-throw other types of errors
        }
    }
}
```

## Error Handling Strategies

### Data Integrity Violations
- **Action**: Log warning and skip record
- **Pipeline**: Continues processing
- **User Impact**: Record is skipped, pipeline completes successfully
- **Example**: Primary key violation, unique constraint violation

### Transient Errors  
- **Action**: Mark as retryable
- **Pipeline**: Can be retried with backoff
- **User Impact**: Operation may succeed on retry
- **Example**: Connection timeout, deadlock

### Configuration Errors
- **Action**: Fail fast
- **Pipeline**: Stops immediately
- **User Impact**: Requires intervention to fix configuration
- **Example**: Table not found, column not found

### Fatal Errors
- **Action**: Escalate and fail
- **Pipeline**: Stops immediately  
- **User Impact**: Requires investigation
- **Example**: Unknown database errors

## Testing

### Comprehensive Test Coverage

Created `SqlErrorClassifierTest` with 13 test cases covering:
- Primary key violations (H2 specific)
- Unique constraint violations
- Foreign key violations
- Not null violations
- Connection errors
- Deadlock errors
- Table/column not found errors
- Syntax errors
- Unknown errors
- Null exception handling
- Message pattern classification

### Integration Testing

- **CsvToH2PipelineDemoTest**: Verifies that primary key violations no longer crash the pipeline
- **All demo tests**: Pass successfully with new error handling
- **Backward compatibility**: Existing functionality remains unchanged

## Benefits

### 1. System Resilience
- Pipelines no longer crash due to data quality issues
- Individual record failures don't stop entire batch processing
- Better separation of data issues vs system issues

### 2. Improved Observability
- Clear categorization of error types
- Detailed logging with context information
- Summary statistics for batch operations

### 3. Better User Experience
- Meaningful error messages
- Clear indication of what went wrong
- Guidance on whether errors are retryable

### 4. Operational Excellence
- Reduced false alarms from data quality issues
- Better error recovery strategies
- Improved system monitoring capabilities

## Usage Examples

### Before (System Crash):
```
ERROR: Pipeline execution failed: Required step failed: load-to-database
Caused by: Unique index or primary key violation: "PRIMARY KEY ON PUBLIC.CUSTOMERS(CUSTOMER_ID)"
```

### After (Graceful Handling):
```
WARN: Skipping record due to data integrity violation: Primary key violation - customer_id already exists
INFO: Load step 'load-to-database' completed: 4 records loaded successfully, 1 record skipped due to data integrity issues
```

## Future Enhancements

1. **Configurable Error Strategies**: Allow users to configure how different error types are handled
2. **Dead Letter Queue**: Option to send failed records to a separate table/queue for later analysis
3. **Retry Logic**: Implement automatic retry with exponential backoff for transient errors
4. **Error Metrics**: Collect and expose metrics about error rates and types
5. **Error Recovery**: Implement recovery strategies for different error scenarios

## Test Coverage and Verification

### Comprehensive Test Suite (31 Tests Total)

#### SqlErrorClassifierTest (17 tests)
- ✅ Primary key violation classification
- ✅ Unique constraint violation classification
- ✅ Foreign key violation classification
- ✅ NOT NULL violation classification
- ✅ Connection error classification
- ✅ Deadlock error classification
- ✅ Table not found classification
- ✅ Column not found classification
- ✅ Syntax error classification
- ✅ Unknown error classification
- ✅ Null exception handling
- ✅ Error description verification
- ✅ Message pattern classification
- ✅ DDL configuration error identification
- ✅ DML data integrity violation identification
- ✅ Transient error identification
- ✅ Comprehensive error type coverage

#### DatabaseErrorHandlingIntegrationTest (7 tests)
- ✅ Real table not found error classification
- ✅ Real column not found error classification
- ✅ Real SQL syntax error classification
- ✅ Real primary key violation classification
- ✅ Real unique constraint violation classification
- ✅ Real NOT NULL violation classification
- ✅ Comprehensive error classification verification

#### DatabaseErrorHandlingProofTest (7 tests)
- ✅ **PROOF**: Table not found → CONFIGURATION_ERROR → FAIL FAST
- ✅ **PROOF**: Column not found → CONFIGURATION_ERROR → FAIL FAST
- ✅ **PROOF**: SQL syntax error → CONFIGURATION_ERROR → FAIL FAST
- ✅ **PROOF**: Primary key violation → DATA_INTEGRITY_VIOLATION → GRACEFUL HANDLING
- ✅ **PROOF**: Unique constraint violation → DATA_INTEGRITY_VIOLATION → GRACEFUL HANDLING
- ✅ **PROOF**: NOT NULL violation → DATA_INTEGRITY_VIOLATION → GRACEFUL HANDLING
- ✅ **PROOF**: Check constraint violation → DATA_INTEGRITY_VIOLATION → GRACEFUL HANDLING

### Verification Results

**✅ ALL TESTS PASSING**: 31/31 tests pass successfully

**✅ FAIL-FAST BEHAVIOR VERIFIED**: Configuration errors (DDL/DML) correctly trigger fail-fast behavior
- Table not found errors → `shouldFailPipeline = true`
- Column not found errors → `shouldFailPipeline = true`
- SQL syntax errors → `shouldFailPipeline = true`

**✅ GRACEFUL HANDLING VERIFIED**: Data integrity violations are handled gracefully
- Primary key violations → `shouldFailPipeline = false`
- Unique constraint violations → `shouldFailPipeline = false`
- NOT NULL violations → `shouldFailPipeline = false`
- Check constraint violations → `shouldFailPipeline = false`

**✅ PIPELINE RESILIENCE VERIFIED**: The CSV-to-H2 pipeline demo now completes successfully despite data integrity violations

## Conclusion

These improvements significantly enhance the robustness and reliability of the APEX rules engine by providing intelligent error handling that distinguishes between different types of database errors and responds appropriately. The system now gracefully handles data quality issues while still failing fast for genuine configuration or system problems.

**Key Achievement**: The primary key violation that previously crashed the entire pipeline is now handled gracefully, allowing the pipeline to continue processing and complete successfully while logging the problematic records.
