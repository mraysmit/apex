# APEX Error Codes Registry

**Version:** 1.0  
**Date:** 2026-01-24  
**Author:** Mark Andrew Ray-Smith Cityline Ltd  

## Overview

This document provides a comprehensive registry of all APEX error codes. These codes are used throughout the APEX Rules Engine to provide consistent, meaningful error identification for debugging, monitoring, and error handling.

## Error Code Format

All APEX error codes follow the format: `APEX-{CATEGORY}-{NUMBER}`

- **APEX**: Prefix identifying the error as an APEX error
- **CATEGORY**: 3-5 letter abbreviation for the error category
- **NUMBER**: 3-digit number (001-999) within the category

### Reserved Ranges

| Range | Usage |
|-------|-------|
| 001-099 | Common/standard errors |
| 100-499 | Domain-specific errors |
| 500-899 | Extended/custom errors |
| 900-998 | Reserved for future use |
| 999 | General/uncategorized errors |

---

## Error Categories

### APEX-CFG: Configuration Errors

Configuration-related errors that occur during YAML loading, validation, or parsing.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-CFG-001 | Missing Required Field | A required field is missing from the configuration | ERROR | Add the missing field to the YAML configuration |
| APEX-CFG-002 | Invalid Field Value | A field contains an invalid or unsupported value | ERROR | Check the field value against allowed values in documentation |
| APEX-CFG-003 | Circular Reference | A circular reference was detected in component/rule references | ERROR | Review component hierarchy and remove circular dependencies |
| APEX-CFG-004 | File Not Found | Referenced configuration file does not exist | ERROR | Verify file path and ensure file exists |
| APEX-CFG-005 | Parse Error | YAML parsing failed due to syntax error | ERROR | Fix YAML syntax using a YAML validator |
| APEX-CFG-006 | Invalid Data Source Reference | External data source reference is invalid | ERROR | Check data-source-ref configuration |
| APEX-CFG-007 | Duplicate ID | A duplicate ID was found in the configuration | ERROR | Ensure all IDs are unique within their scope |
| APEX-CFG-008 | Invalid Expression | SpEL expression syntax is invalid | ERROR | Review expression syntax |
| APEX-CFG-009 | Schema Validation Failed | Configuration does not match expected schema | ERROR | Compare configuration against schema documentation |
| APEX-CFG-999 | General Configuration Error | Unspecified configuration error | ERROR | Review error message for details |

**Exception Class:** `YamlConfigurationException`, `RuleConfigurationException`

---

### APEX-RULE: Rule Evaluation Errors

Errors that occur during rule condition evaluation.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-RULE-001 | Evaluation Failed | Rule condition evaluation failed | ERROR | Check expression syntax and data context |
| APEX-RULE-002 | Property Not Found | Referenced property not found in data context | WARNING/ERROR | Ensure property exists in input data |
| APEX-RULE-003 | Type Mismatch | Expression operand types are incompatible | ERROR | Check data types in expression |
| APEX-RULE-004 | Null Reference | Null value encountered in expression | WARNING | Add null checks to expression |
| APEX-RULE-005 | Method Not Found | Method called in expression does not exist | ERROR | Verify method name and parameters |
| APEX-RULE-006 | Access Denied | Cannot access property or method | ERROR | Check accessibility of referenced members |
| APEX-RULE-007 | Division By Zero | Arithmetic division by zero | ERROR | Add zero-check before division |
| APEX-RULE-008 | Overflow | Numeric overflow during calculation | ERROR | Use appropriate numeric types |
| APEX-RULE-009 | Timeout | Rule evaluation exceeded time limit | ERROR | Simplify expression or increase timeout |
| APEX-RULE-999 | General Rule Error | Unspecified rule evaluation error | ERROR | Review error message for details |

**Exception Class:** `RuleEvaluationException`

---

### APEX-TRANS: Transformation Errors

Errors that occur during data transformation processing.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-TRANS-001 | Expression Error | Transformation expression evaluation failed | ERROR | Check SpEL expression syntax and variables |
| APEX-TRANS-002 | Type Conversion Error | Cannot convert value to target type | ERROR | Ensure source value is compatible with target type |
| APEX-TRANS-003 | Source Field Error | Source field not found or inaccessible | ERROR | Verify source field exists in input data |
| APEX-TRANS-004 | Target Field Error | Target field not found or not writable | ERROR | Verify target field is writable |
| APEX-TRANS-005 | Configuration Error | Transformation configuration is invalid | ERROR | Review transformation YAML configuration |
| APEX-TRANS-006 | Condition Error | Transformation condition evaluation failed | ERROR | Check condition expression syntax |
| APEX-TRANS-007 | Null Value Error | Null value encountered where not allowed | ERROR | Add null handling or default values |
| APEX-TRANS-008 | Loop Detected | Infinite loop detected in transformations | ERROR | Check for circular dependencies |
| APEX-TRANS-009 | Max Depth Exceeded | Transformation nesting depth exceeded | ERROR | Simplify transformation hierarchy |
| APEX-TRANS-999 | General Transformation Error | Unspecified transformation error | ERROR | Review error message for details |

**Exception Class:** `ApexTransformationException`

---

### APEX-CACHE: Cache Errors

Errors related to cache operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-CACHE-001 | Not Initialized | Cache or cache manager is not initialized | ERROR | Configure cache manager before use |
| APEX-CACHE-002 | Lookup Failed | Cache lookup operation failed | WARNING | Check cache connectivity and key validity |
| APEX-CACHE-003 | Write Failed | Cache write (put) operation failed | WARNING | Check cache capacity and permissions |
| APEX-CACHE-004 | Remove Failed | Cache remove operation failed | WARNING | Verify key exists and operation permissions |
| APEX-CACHE-005 | Clear Failed | Cache clear operation failed | WARNING | Check cache permissions |
| APEX-CACHE-006 | Invalid Key | Cache key is null or malformed | ERROR | Ensure valid, non-null cache keys |
| APEX-CACHE-007 | Configuration Error | Cache configuration is invalid | ERROR | Review cache configuration |
| APEX-CACHE-008 | Capacity Exceeded | Cache storage capacity exceeded | WARNING | Increase capacity or implement eviction |
| APEX-CACHE-009 | Serialization Error | Value serialization/deserialization failed | ERROR | Check value types and serialization config |
| APEX-CACHE-999 | General Cache Error | Unspecified cache error | ERROR | Review error message for details |

**Exception Class:** `ApexCacheException`

---

### APEX-ENRICH: Enrichment Errors

Errors that occur during data enrichment processing.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-ENRICH-001 | Lookup Failed | Enrichment lookup operation failed | ERROR | Check data source connectivity and query |
| APEX-ENRICH-002 | Data Source Unavailable | Data source for enrichment is unavailable | ERROR | Verify data source configuration and status |
| APEX-ENRICH-003 | Required Field Missing | Required field for enrichment is missing | ERROR | Ensure all required fields are present in input |
| APEX-ENRICH-004 | Expression Error | Enrichment expression evaluation failed | ERROR | Check SpEL expression syntax |
| APEX-ENRICH-005 | Condition Error | Enrichment condition evaluation failed | ERROR | Check condition expression syntax |
| APEX-ENRICH-006 | Result Field Error | Cannot set enrichment result field | ERROR | Verify result field is writable |
| APEX-ENRICH-007 | No Match Found | Lookup found no matching records | INFO/WARNING | Verify lookup criteria and data |
| APEX-ENRICH-008 | Multiple Matches | Lookup returned multiple unexpected matches | WARNING | Refine lookup criteria |
| APEX-ENRICH-009 | Timeout | Enrichment operation exceeded time limit | ERROR | Optimize query or increase timeout |
| APEX-ENRICH-999 | General Enrichment Error | Unspecified enrichment error | ERROR | Review error message for details |

**Exception Class:** `EnrichmentException`

---

### APEX-DS: Data Source Errors

Errors related to external data source operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-DS-001 | Connection Error | Failed to connect to data source | ERROR | Check connection parameters and network |
| APEX-DS-002 | Configuration Error | Data source configuration is invalid | ERROR | Review data source configuration |
| APEX-DS-003 | Execution Error | Query/operation execution failed | ERROR | Check query syntax and parameters |
| APEX-DS-004 | Data Format Error | Data format is invalid or unexpected | ERROR | Verify data schema matches expectations |
| APEX-DS-005 | Timeout Error | Operation exceeded time limit | ERROR | Increase timeout or optimize operation |
| APEX-DS-006 | Authentication Error | Authentication failed | ERROR | Verify credentials |
| APEX-DS-007 | Not Found Error | Requested resource not found | WARNING | Verify resource exists |
| APEX-DS-008 | Circuit Breaker | Circuit breaker is open | WARNING | Wait for circuit to close or check service |
| APEX-DS-009 | Rate Limited | Request was rate limited | WARNING | Reduce request rate |
| APEX-DS-999 | General Data Source Error | Unspecified data source error | ERROR | Review error message for details |

**Exception Class:** `DataSourceException`

---

### APEX-SINK: Data Sink Errors

Errors related to data output/sink operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-SINK-001 | Connection Error | Failed to connect to data sink | ERROR | Check connection parameters |
| APEX-SINK-002 | Write Error | Failed to write data | ERROR | Check permissions and data format |
| APEX-SINK-003 | Data Integrity Error | Data integrity constraint violated | ERROR | Check data against constraints |
| APEX-SINK-004 | Batch Error | Batch operation partially failed | ERROR | Review failed records |
| APEX-SINK-005 | Schema Error | Data does not match sink schema | ERROR | Align data format with sink schema |
| APEX-SINK-006 | Security Error | Security/permission error | ERROR | Check permissions and credentials |
| APEX-SINK-007 | Resource Error | Resource limit reached | ERROR | Increase resources or optimize |
| APEX-SINK-008 | Timeout Error | Operation exceeded time limit | ERROR | Increase timeout or optimize |
| APEX-SINK-999 | General Sink Error | Unspecified data sink error | ERROR | Review error message for details |

**Exception Class:** `DataSinkException`

---

### APEX-SCEN: Scenario Errors

Errors related to scenario execution.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-SCEN-001 | Stage Failed | Scenario stage execution failed | ERROR | Review stage configuration and input |
| APEX-SCEN-002 | Component Failed | Component execution failed | ERROR | Check component configuration |
| APEX-SCEN-003 | Not Found | Scenario or stage not found | ERROR | Verify scenario ID exists |
| APEX-SCEN-004 | Invalid State | Scenario is in invalid state | ERROR | Check scenario lifecycle |
| APEX-SCEN-005 | Dependency Failed | Required dependency failed | ERROR | Resolve dependent stage first |
| APEX-SCEN-006 | Policy Violation | Failure policy triggered termination | ERROR | Review failure policy configuration |
| APEX-SCEN-999 | General Scenario Error | Unspecified scenario error | ERROR | Review error message for details |

**Exception Class:** N/A (uses RuleResult)

---

### APEX-VAL: Validation Errors

Errors related to rule/data validation.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-VAL-001 | Rule Validation Failed | Rule validation check failed | ERROR | Fix validation rule violation |
| APEX-VAL-002 | Schema Validation Failed | Data does not match schema | ERROR | Align data with schema |
| APEX-VAL-003 | Required Field Missing | Required field is missing | ERROR | Add missing required field |
| APEX-VAL-004 | Invalid Format | Field format is invalid | ERROR | Fix field format |
| APEX-VAL-005 | Out Of Range | Value is outside allowed range | ERROR | Adjust value to valid range |
| APEX-VAL-006 | Invalid Reference | Referenced entity not found | ERROR | Verify reference validity |
| APEX-VAL-999 | General Validation Error | Unspecified validation error | ERROR | Review error message for details |

**Exception Class:** `RuleValidationException`

---

### APEX-PIPE: Pipeline Errors

Errors related to data pipeline operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-PIPE-001 | Initialization Failed | Failed to initialize pipeline engine | ERROR | Check data source and sink configurations |
| APEX-PIPE-002 | Execution Failed | Pipeline execution failed | ERROR | Review pipeline configuration and input data |
| APEX-PIPE-003 | Batch Execution Failed | Batch pipeline execution failed | ERROR | Check batch processing configuration |
| APEX-PIPE-004 | Data Pipeline Exception | Pipeline failed with DataPipelineException | ERROR | Review pipeline steps and data flow |
| APEX-PIPE-005 | Unexpected Error | Unexpected error during pipeline execution | ERROR | Review stack trace and configuration |
| APEX-PIPE-999 | General Pipeline Error | Unspecified pipeline error | ERROR | Review error message for details |

**Exception Class:** `DataPipelineException`

---

### APEX-REG: Registry Errors

Errors related to data source registry operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-REG-001 | Health Check Failed | Error during data source health check | WARNING | Check data source connectivity |
| APEX-REG-002 | Listener Notification Failed | Error notifying registry listener | WARNING | Check listener implementation |
| APEX-REG-003 | Registration Failed | Failed to register data source | ERROR | Verify data source configuration |
| APEX-REG-999 | General Registry Error | Unspecified registry error | ERROR | Review error message for details |

**Exception Class:** N/A (internal logging)

---

### APEX-FACT: Factory Errors

Errors related to data source factory operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-FACT-001 | Creation Failed | Failed to create data source | ERROR | Check data source configuration |
| APEX-FACT-002 | Pending Creation Error | Error waiting for pending data source creation | WARNING | Check concurrent creation handling |
| APEX-FACT-999 | General Factory Error | Unspecified factory error | ERROR | Review error message for details |

**Exception Class:** `DataSourceException`

---

### APEX-MGR: Manager Errors

Errors related to data source manager operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-MGR-001 | Initialization Failed | Failed to initialize DataSourceManager | ERROR | Check data source configurations |
| APEX-MGR-002 | Metrics Collection Failed | Error collecting data source metrics | WARNING | Check data source health |
| APEX-MGR-003 | Listener Notification Failed | Error notifying manager listener | WARNING | Check listener implementation |
| APEX-MGR-999 | General Manager Error | Unspecified manager error | ERROR | Review error message for details |

**Exception Class:** `DataSourceException`

---

### APEX-YAML: YAML Processing Errors

Errors related to YAML file processing and dependency analysis.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-YAML-001 | Dependency Analysis Failed | Failed to analyze YAML dependencies | ERROR | Check file paths and YAML syntax |
| APEX-YAML-002 | Reference Extraction Failed | Failed to extract references from YAML | ERROR | Check YAML structure |
| APEX-YAML-999 | General YAML Processing Error | Unspecified YAML processing error | ERROR | Review error message for details |

**Exception Class:** `YamlConfigurationException`

---

### APEX-FILE: File Processing Errors

Errors related to file loading and processing.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-FILE-001 | CSV Load Failed | Failed to load CSV file | ERROR | Check file path and CSV format |
| APEX-FILE-002 | JSON Load Failed | Failed to load JSON file | ERROR | Check file path and JSON syntax |
| APEX-FILE-003 | XML Load Failed | Failed to load XML file | ERROR | Check file path and XML syntax |
| APEX-FILE-999 | General File Error | Unspecified file error | ERROR | Review error message for details |

**Exception Class:** `IOException`, `DataSourceException`

---

### APEX-MQ: Message Queue Errors

Errors related to message queue operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-MQ-001 | Get Data Failed | Failed to get data from message queue | ERROR | Check queue connectivity |
| APEX-MQ-002 | Send Failed | Failed to send message to queue | ERROR | Check queue connectivity and permissions |
| APEX-MQ-003 | Connection Failed | Failed to connect to message queue | ERROR | Check connection parameters |
| APEX-MQ-999 | General MQ Error | Unspecified message queue error | ERROR | Review error message for details |

**Exception Class:** `DataSourceException`

---

### APEX-CLASS: Classification Errors

Errors related to data classification operations.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-CLASS-001 | Classification Failed | Classification operation failed | ERROR | Check input data and classification config |
| APEX-CLASS-002 | Cache Creation Failed | Failed to create classification cache | ERROR | Check cache configuration |
| APEX-CLASS-999 | General Classification Error | Unspecified classification error | ERROR | Review error message for details |

**Exception Class:** N/A (returns ClassificationResult)

---

### APEX-REPORT: Report Generation Errors

Errors related to report generation.

| Code | Name | Description | Severity | Resolution |
|------|------|-------------|----------|------------|
| APEX-REPORT-001 | Template Compilation Failed | Failed to compile report template | ERROR | Check template file exists and is valid |
| APEX-REPORT-002 | Generation Failed | Failed to generate report | ERROR | Check report data and template |
| APEX-REPORT-999 | General Report Error | Unspecified report error | ERROR | Review error message for details |

**Exception Class:** `RuntimeException`, `IOException`

---

## Error Code Cross-Reference by Exception Class

| Exception Class | Error Code Prefix | Location |
|-----------------|-------------------|----------|
| `YamlConfigurationException` | APEX-CFG, APEX-YAML | `dev.mars.apex.core.config.yaml` |
| `RuleConfigurationException` | APEX-CFG | `dev.mars.apex.core.exception` |
| `RuleEngineException` | (base class) | `dev.mars.apex.core.exception` |
| `RuleEvaluationException` | APEX-RULE | `dev.mars.apex.core.exception` |
| `RuleValidationException` | APEX-VAL | `dev.mars.apex.core.exception` |
| `ApexTransformationException` | APEX-TRANS | `dev.mars.apex.core.exception` |
| `ApexCacheException` | APEX-CACHE | `dev.mars.apex.core.exception` |
| `EnrichmentException` | APEX-ENRICH | `dev.mars.apex.core.service.enrichment` |
| `DataSourceException` | APEX-DS, APEX-FACT, APEX-MGR, APEX-MQ, APEX-FILE | `dev.mars.apex.core.service.data.external` |
| `DataSinkException` | APEX-SINK | `dev.mars.apex.core.service.data.external` |
| `DataPipelineException` | APEX-PIPE | `dev.mars.apex.core.engine.pipeline` |

---

## Usage in Code

### Throwing Exceptions with Error Codes

```java
// Using factory methods (preferred)
throw ApexTransformationException.expressionError(
    "calculate-premium",
    "#value * rate", 
    "Variable 'rate' not found in context"
);

throw ApexCacheException.notInitialized("customer-cache");

// Using constructors with error codes
throw new RuleEngineException(
    "APEX-RULE-002",
    "Property 'accountId' not found in data context",
    "Rule: validate-account, Data keys: [name, email]"
);
```

### Logging with Error Codes

```java
logger.error("[{}] {}: {}", 
    exception.getErrorCode(),
    exception.getMessage(),
    exception.getContext());
// Output: [APEX-TRANS-001] Expression evaluation failed: Variable 'rate' not found (Context: Transformation: calculate-premium)
```

### Checking Error Codes

```java
try {
    // operation
} catch (ApexTransformationException e) {
    if (e.getErrorType() == ApexTransformationException.ErrorType.NULL_VALUE_ERROR) {
        // Handle null value specifically
        return defaultValue;
    }
    throw e; // Re-throw other errors
}
```

---

## Related Documentation

- [APEX_ERROR_HANDLING_GUIDE.md](APEX_ERROR_HANDLING_GUIDE.md) - Comprehensive error handling guide
- [APEX_SUCCESS_ERROR_CODES_GUIDE.md](APEX_SUCCESS_ERROR_CODES_GUIDE.md) - Success/error code patterns
- [APEX_TECHNICAL_REFERENCE.md](APEX_TECHNICAL_REFERENCE.md) - Technical reference documentation

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-24 | Mark Andrew Ray-Smith | Initial version |
