# APEX Playground - Core Integration Guide

This document describes how the apex-playground module integrates with apex-core public APIs to process YAML-defined rules and datasets and display results in the user interface.

## 1. Architecture Overview

The apex-playground is a Spring Boot web application with a 4-panel JSFiddle-style interface:

| Panel | Location | Purpose |
|-------|----------|---------|
| Source Data | Top-Left | Input data (JSON, XML, CSV) |
| YAML Rules | Top-Right | YAML configuration editor |
| Validation Results | Bottom-Left | Rule execution results |
| Enrichment Results | Bottom-Right | Enriched data & metrics |

## 2. Key apex-core Imports

The playground uses the following apex-core classes:

```java
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlMetadataValidator;
```

## 3. apex-core API Reference

| apex-core API | Purpose | Usage in Playground |
|---------------|---------|---------------------|
| `YamlConfigurationLoader` | Parse YAML strings/files | `new YamlConfigurationLoader()` |
| `YamlConfigurationLoader.fromYamlString()` | Parse YAML from string | Converts user's YAML input to config |
| `YamlRuleConfiguration` | Holds parsed YAML config | Intermediate object for engine creation |
| `RulesEngine.fromYamlConfig()` | Create engine from config | Factory method to create engine |
| `RulesEngine.evaluate()` | Execute rules against data | Main processing entry point |
| `RuleResult` | Processing results | Contains success, enrichedData, failures |
| `YamlConfigurationException` | YAML parsing errors | Caught for error handling |

## 4. Core Processing Flow

The main processing happens in `PlaygroundService.processData()`:

```java
public PlaygroundResponse processData(PlaygroundRequest request) {
    // Step 1: Validate YAML configuration
    if (!yamlValidationService.isValidYaml(request.getYamlRules())) {
        response.setSuccess(false);
        response.setMessage("YAML configuration is invalid");
        return response;
    }

    // Step 2: Parse source data (JSON/XML/CSV → Map<String, Object>)
    Map<String, Object> parsedData = dataProcessingService.parseData(
        request.getSourceData(),
        request.getDataFormat()
    );

    // Step 3: Parse YAML to config object
    YamlConfigurationLoader configLoader = new YamlConfigurationLoader();
    YamlRuleConfiguration yamlConfig = configLoader.fromYamlString(request.getYamlRules());

    // Step 4: Create engine from config
    RulesEngine rulesEngine = RulesEngine.fromYamlConfig(yamlConfig);

    // Step 5: Execute unified evaluation (handles all YAML sections)
    RuleResult result = rulesEngine.evaluate(parsedData);

    // Step 6: Map results to response
    response.setSuccess(result.isSuccess());
    response.setMessage(result.getMessage());
    
    if (result.getEnrichedData() != null) {
        response.getEnrichment().setEnrichedData(result.getEnrichedData());
    }
    
    if (result.hasFailures()) {
        for (String failure : result.getFailureMessages()) {
            response.addError(failure);
        }
    }

    return response;
}
```

## 5. YAML Validation Service

The `YamlValidationService` uses apex-core for validation:

```java
private void validateYamlSyntax(String yamlContent, YamlValidationResponse response) {
    try {
        // Uses apex-core loader to validate syntax
        configLoader.fromYamlString(yamlContent);
    } catch (YamlConfigurationException e) {
        response.setValid(false);
        response.addError("YAML syntax error: " + e.getMessage(), line, 0);
    }
}

private void validateApexStructure(String yamlContent, YamlValidationResponse response) {
    try {
        YamlRuleConfiguration config = configLoader.fromYamlString(yamlContent);
        
        // Extract statistics from config
        if (config.getRules() != null) {
            response.getStatistics().setRulesCount(config.getRules().size());
        }
        if (config.getEnrichments() != null) {
            response.getStatistics().setEnrichmentsCount(config.getEnrichments().size());
        }
    } catch (YamlConfigurationException e) {
        response.setValid(false);
        response.addError("APEX structure error: " + e.getMessage(), 0, 0);
    }
}
```

## 6. Result Mapping to UI

The `RuleResult` from apex-core is mapped to `PlaygroundResponse`:

```java
// Map results to response
response.setSuccess(result.isSuccess());
response.setMessage(result.getMessage());

// Handle enrichment data
if (result.getEnrichedData() != null) {
    response.getEnrichment().setEnrichedData(result.getEnrichedData());
    response.getEnrichment().setEnriched(true);
    
    int fieldsAdded = result.getEnrichedData().size() - parsedData.size();
    response.getEnrichment().setFieldsAdded(Math.max(0, fieldsAdded));
}

// Handle failures
if (result.hasFailures()) {
    for (String failure : result.getFailureMessages()) {
        response.addError(failure);
    }
}

// Add overall result to validation
RuleExecutionResult executionResult = new RuleExecutionResult(
    "evaluation-" + System.currentTimeMillis(),
    "Overall Evaluation",
    result.isSuccess(),
    result.getMessage()
);
response.getValidation().addResult(executionResult);
```

## 7. REST API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/playground/api/process` | POST | Process data with YAML rules |
| `/playground/api/validate` | POST | Validate YAML configuration |
| `/playground/api/examples` | GET | Get available examples |
| `/playground/api/examples/{category}/{name}` | GET | Get specific example |
| `/playground/api/upload/data` | POST | Upload data file |
| `/playground/api/upload/yaml` | POST | Upload YAML file |

## 8. Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           APEX PLAYGROUND UI                             │
│  ┌─────────────────────┐    ┌─────────────────────┐                     │
│  │   Source Data       │    │   YAML Rules        │                     │
│  │   (JSON/XML/CSV)    │    │   Configuration     │                     │
│  └──────────┬──────────┘    └──────────┬──────────┘                     │
│             │                          │                                 │
│             └──────────┬───────────────┘                                 │
│                        ▼                                                 │
│              ┌─────────────────┐                                         │
│              │  Process Button │                                         │
│              └────────┬────────┘                                         │
└───────────────────────┼─────────────────────────────────────────────────┘
                        │ POST /playground/api/process
                        ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                         ApiController                                      │
│                              │                                             │
│                              ▼                                             │
│                    PlaygroundService.processData()                         │
│                              │                                             │
│  ┌───────────────────────────┼───────────────────────────────────────┐    │
│  │                           ▼                                        │    │
│  │  1. YamlValidationService.isValidYaml()                           │    │
│  │                           │                                        │    │
│  │  2. DataProcessingService.parseData() → Map<String, Object>       │    │
│  │                           │                                        │    │
│  │  3. YamlConfigurationLoader.fromYamlString() → YamlRuleConfig     │    │
│  │                           │                                        │    │
│  │  4. RulesEngine.fromYamlConfig(config) → RulesEngine              │    │
│  │                           │                                        │    │
│  │  5. rulesEngine.evaluate(parsedData) → RuleResult                 │    │
│  │                           │                                        │    │
│  │  6. Map RuleResult → PlaygroundResponse                           │    │
│  └───────────────────────────┼───────────────────────────────────────┘    │
│                              │                                             │
└──────────────────────────────┼─────────────────────────────────────────────┘
                               │ JSON Response
                               ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                           APEX PLAYGROUND UI                               │
│  ┌─────────────────────┐    ┌─────────────────────┐                       │
│  │  Validation Results │    │  Enrichment Results │                       │
│  │  - success/failure  │    │  - enrichedData     │                       │
│  │  - messages         │    │  - metrics          │                       │
│  └─────────────────────┘    └─────────────────────┘                       │
└───────────────────────────────────────────────────────────────────────────┘
```

## 9. Key Design Patterns

### 9.1 Black Box Processing
The playground treats the `RulesEngine` as a black box - it simply passes data in and receives results out, without needing to understand internal processing.

### 9.2 Single Entry Point
Uses `RulesEngine.evaluate(parsedData)` as the single entry point which handles all YAML sections (rules, enrichments, transformations) in the correct order.

### 9.3 Factory Pattern
Uses `RulesEngine.fromYamlConfig()` factory method to create the engine from configuration.

### 9.4 Separation of Concerns
- `DataProcessingService`: Handles data format parsing (JSON, XML, CSV)
- `YamlValidationService`: Handles YAML validation
- `PlaygroundService`: Orchestrates the processing workflow
- `ApiController`: Handles REST API endpoints

### 9.5 Performance Metrics
Collects timing metrics at each stage (YAML parsing, data parsing, rules execution) for display in the UI.

## 10. Frontend Integration

The JavaScript frontend (`playground.js`) calls the REST API:

```javascript
async function processData() {
    const response = await fetch(window.playgroundConfig.apiBaseUrl + '/process', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sourceData: sourceData,
            yamlRules: yamlRules,
            dataFormat: currentDataFormat
        })
    });
    
    const result = await response.json();
    
    // Display results in UI panels
    displayValidationResults(result.validation);
    displayEnrichmentResults(result.enrichment, result.metrics);
}
```

## 11. Data Format Support

The `DataProcessingService` supports multiple input formats:

| Format | Parser | Auto-Detection |
|--------|--------|----------------|
| JSON | Jackson ObjectMapper | Starts with `{` or `[` |
| XML | Jackson XmlMapper | Starts with `<` |
| CSV | Custom parser | Contains commas and newlines |

All formats are normalized to `Map<String, Object>` before passing to apex-core.

## 12. Error Handling

Errors from apex-core are caught and mapped to user-friendly responses:

```java
try {
    YamlRuleConfiguration yamlConfig = configLoader.fromYamlString(yamlContent);
    RulesEngine rulesEngine = RulesEngine.fromYamlConfig(yamlConfig);
    RuleResult result = rulesEngine.evaluate(parsedData);
} catch (YamlConfigurationException e) {
    response.setSuccess(false);
    response.addError("YAML configuration error: " + e.getMessage());
} catch (Exception e) {
    response.setSuccess(false);
    response.addError("Processing error: " + e.getMessage());
}
```

---

*Document generated from apex-playground source code analysis.*

