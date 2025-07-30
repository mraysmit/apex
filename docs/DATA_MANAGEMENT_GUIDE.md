# SpEL Rules Engine - Data Management Guide

## Overview

The SpEL Rules Engine provides comprehensive data management capabilities designed for enterprise-grade applications. This guide covers all data-related functionality, from basic data sources to advanced YAML-based dataset enrichment and REST API integration.

## Table of Contents

1. [Core Data Management Architecture](#1-core-data-management-architecture)
2. [Data Source Implementations](#2-data-source-implementations)
3. [YAML Configuration Data Management](#3-yaml-configuration-data-management)
4. [Lookup and Enrichment Services](#4-lookup-and-enrichment-services)
5. [Validation Data Management](#5-validation-data-management)
6. [Financial Services Data](#6-financial-services-data)
7. [REST API Data Management](#7-rest-api-data-management)
8. [Data Configuration Features](#8-data-configuration-features)
9. [Advanced Data Patterns](#9-advanced-data-patterns)

---

## 1. Core Data Management Architecture

### Data Service Manager

The `DataServiceManager` serves as the central orchestration point for all data operations:

```java
// Initialize with mock data sources
DataServiceManager dataManager = new DataServiceManager();
dataManager.initializeWithMockData();

// Load custom data sources
dataManager.loadDataSource(new CustomDataSource("ProductsSource", "products"));

// Request data by type
List<Product> products = dataManager.requestData("products");

// Request data by source name
Customer customer = dataManager.requestDataByName("CustomerSource", "customer");
```

**Key Features:**
- **Centralized data orchestration** with pluggable architecture
- **Multiple data source registration** by name and type
- **Automatic request routing** to appropriate data sources
- **Type-safe data retrieval** with generic support

### Data Source Interface

All data sources implement the `DataSource` interface:

```java
public interface DataSource {
    String getName();
    String getDataType();
    boolean supportsDataType(String dataType);
    <T> T getData(String dataType, Object... parameters);
}
```

**Implementation Example:**
```java
public class CustomDataSource implements DataSource {
    private final String name;
    private final String dataType;
    private final Map<String, Object> dataStore = new HashMap<>();
    
    public CustomDataSource(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
    }
    
    @Override
    public <T> T getData(String dataType, Object... parameters) {
        if (!supportsDataType(dataType)) {
            return null;
        }
        return (T) dataStore.get(dataType);
    }
    
    // Add/remove data methods
    public void addData(String dataType, Object data) {
        if (supportsDataType(dataType)) {
            dataStore.put(dataType, data);
        }
    }
}
```

---

## 2. Data Source Implementations

### Mock Data Sources

The `MockDataSource` provides comprehensive test data for demonstrations:

```java
// Create mock data sources for different types
MockDataSource productsSource = new MockDataSource("ProductsDataSource", "products");
MockDataSource customerSource = new MockDataSource("CustomerDataSource", "customer");
MockDataSource inventorySource = new MockDataSource("InventoryDataSource", "inventory");

// Supported data types
String[] supportedTypes = {
    "products", "inventory", "customer", "templateCustomer",
    "lookupServices", "sourceRecords", "matchingRecords", "nonMatchingRecords"
};
```

**Features:**
- **Automatic data generation** based on data type
- **Parameterized queries** for complex scenarios
- **Dynamic record matching** for lookup operations
- **Comprehensive test datasets** for all demo scenarios

### Custom Data Sources

For dynamic data management during runtime:

```java
CustomDataSource customSource = new CustomDataSource("DynamicSource", "customData");

// Add data dynamically
List<Product> products = Arrays.asList(
    new Product("PROD001", "Dynamic Product 1", new BigDecimal("99.99"), "Electronics"),
    new Product("PROD002", "Dynamic Product 2", new BigDecimal("149.99"), "Books")
);
customSource.addData("customData", products);

// Retrieve data
List<Product> retrievedProducts = customSource.getData("customData");
```

---

## 3. YAML Configuration Data Management

### YAML Rule Configuration Structure

```yaml
metadata:
  name: "Enterprise Rules Configuration"
  version: "2.0.0"
  description: "Comprehensive business rules with dataset enrichment"
  author: "Business Rules Team"
  created: "2024-01-15T10:00:00Z"
  tags: ["enterprise", "validation", "enrichment"]

rules:
  - id: "business-rule-001"
    name: "Customer Age Validation"
    condition: "#data.age >= 18"
    message: "Customer must be at least 18 years old"
    severity: "ERROR"
    enabled: true
    priority: 10

enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "code"
        cache-enabled: true
        cache-ttl-seconds: 3600
        data:
          - code: "USD"
            name: "US Dollar"
            region: "North America"
          - code: "EUR"
            name: "Euro"
            region: "Europe"
    field-mappings:
      - source-field: "name"
        target-field: "currencyName"
      - source-field: "region"
        target-field: "currencyRegion"
```

### Loading YAML Configuration

```java
// Load from file
YamlRuleConfiguration config = YamlConfigurationLoader.loadFromFile("rules.yaml");

// Load from classpath
YamlRuleConfiguration config = YamlConfigurationLoader.loadFromFile("classpath:config/rules.yaml");

// Load from string content
String yamlContent = "metadata:\n  name: 'Test Rules'\nrules:\n  - id: 'test'";
YamlRuleConfiguration config = YamlConfigurationLoader.loadFromStream(
    new ByteArrayInputStream(yamlContent.getBytes())
);

// Create rules engine from configuration
RulesEngine engine = YamlRulesEngineService.createRulesEngine(config);
```

---

## 4. Lookup and Enrichment Services

### Dataset Lookup Service

The `DatasetLookupService` provides YAML dataset integration:

```java
// Create dataset configuration
YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
dataset.setType("inline");
dataset.setKeyField("code");
dataset.setCacheEnabled(true);
dataset.setCacheTtlSeconds(3600);

// Add dataset records
List<Map<String, Object>> data = Arrays.asList(
    Map.of("code", "USD", "name", "US Dollar", "region", "North America"),
    Map.of("code", "EUR", "name", "Euro", "region", "Europe")
);
dataset.setData(data);

// Create lookup service
DatasetLookupService lookupService = new DatasetLookupService("CurrencyLookup", dataset);

// Perform lookups
Map<String, Object> usdData = (Map<String, Object>) lookupService.enrich("USD");
boolean isValidCurrency = lookupService.validate("EUR");
```

### Enrichment Processing

```java
// Create enrichment processor
YamlEnrichmentProcessor processor = new YamlEnrichmentProcessor();

// Process enrichments from YAML configuration
Object enrichedObject = processor.processEnrichments(
    yamlConfig.getEnrichments(), 
    targetObject
);

// Process single enrichment
YamlEnrichment enrichment = createCurrencyEnrichment();
Object result = processor.processEnrichment(enrichment, targetObject);
```

**Enrichment Types:**
- **lookup-enrichment** - Dataset-based data enrichment
- **field-enrichment** - Direct field value enrichment
- **calculation-enrichment** - Computed value enrichment
- **transformation-enrichment** - Data transformation enrichment

---

## 5. Validation Data Management

### Validation Service

```java
// Create validation service
LookupServiceRegistry registry = new LookupServiceRegistry();
RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
ValidationService validationService = new ValidationService(registry, rulesEngine);

// Register custom validator
Validator<Customer> ageValidator = new AbstractValidator<Customer>("ageValidator", Customer.class) {
    @Override
    public boolean validate(Customer customer) {
        return customer != null && customer.getAge() >= 18;
    }
};
registry.registerService(ageValidator);

// Perform validation
Customer customer = new Customer("John", 25, "john@example.com");
boolean isValid = validationService.validate("ageValidator", customer);
RuleResult result = validationService.validateWithResult("ageValidator", customer);
```

### YAML Validation Configuration

```yaml
rules:
  # Field validation rules
  - id: "trade-id-required"
    name: "Trade ID Required"
    category: "validation"
    condition: "#tradeId != null && #tradeId.trim().length() > 0"
    message: "Trade ID is required"
    severity: "ERROR"
    
  - id: "trade-id-format"
    name: "Trade ID Format Validation"
    category: "validation"
    condition: "#tradeId != null && #tradeId.matches('^[A-Z]{3}[0-9]{3,6}$')"
    message: "Trade ID must follow format: 3 letters + 3-6 digits"
    severity: "ERROR"

rule-groups:
  - id: "basic-validation"
    name: "Basic Field Validation"
    category: "validation"
    stop-on-first-failure: true
    rule-ids:
      - "trade-id-required"
      - "trade-id-format"
```

---

## 6. Financial Services Data

### Financial Static Data Provider

Comprehensive financial reference data management:

```java
// Access client data
Client client = FinancialStaticDataProvider.getClient("CLIENT001");
Collection<Client> allClients = FinancialStaticDataProvider.getAllClients();

// Access account data
ClientAccount account = FinancialStaticDataProvider.getClientAccount("ACC001");
List<ClientAccount> clientAccounts = FinancialStaticDataProvider.getClientAccountsForClient("CLIENT001");

// Access counterparty data
Counterparty counterparty = FinancialStaticDataProvider.getCounterparty("CP001");
boolean isValidCounterparty = FinancialStaticDataProvider.isValidCounterparty("CP001");

// Access currency data
CurrencyData currency = FinancialStaticDataProvider.getCurrency("USD");
Collection<CurrencyData> allCurrencies = FinancialStaticDataProvider.getAllCurrencies();

// Access commodity data
CommodityReference commodity = FinancialStaticDataProvider.getCommodity("GOLD");
Collection<CommodityReference> allCommodities = FinancialStaticDataProvider.getAllCommodities();
```

### Financial Dataset Examples

```yaml
# Currency enrichment with comprehensive financial data
enrichments:
  - id: "currency-dataset-enrichment"
    type: "lookup-enrichment"
    condition: "#notionalCurrency != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "code"
        cache-enabled: true
        cache-ttl-seconds: 7200
        default-values:
          region: "Unknown"
          isActive: false
          centralBank: "Unknown"
        data:
          - code: "USD"
            name: "US Dollar"
            decimalPlaces: 2
            isActive: true
            region: "North America"
            centralBank: "Federal Reserve"
            majorCurrency: true
          - code: "EUR"
            name: "Euro"
            decimalPlaces: 2
            isActive: true
            region: "Europe"
            centralBank: "European Central Bank"
            majorCurrency: true
```

---

## 7. REST API Data Management

### Configuration Management Endpoints

```bash
# Load YAML configuration from content
curl -X POST http://localhost:8080/api/config/load \
  -H "Content-Type: application/x-yaml" \
  -d @rules-config.yaml

# Upload configuration file
curl -X POST http://localhost:8080/api/config/upload \
  -F "file=@config/financial-rules.yaml"

# Validate configuration
curl -X POST http://localhost:8080/api/config/validate \
  -H "Content-Type: application/x-yaml" \
  -d @test-config.yaml

# Get configuration info
curl -X GET http://localhost:8080/api/config/info
```

### Rule Management Endpoints

```bash
# Evaluate single rule
curl -X POST http://localhost:8080/api/rules/check \
  -H "Content-Type: application/json" \
  -d '{
    "condition": "#age >= 18",
    "data": {"age": 25},
    "ruleName": "age-check"
  }'

# Validate data against multiple rules
curl -X POST http://localhost:8080/api/rules/validate \
  -H "Content-Type: application/json" \
  -d '{
    "data": {"age": 16, "email": null},
    "validationRules": [
      {
        "name": "age-check",
        "condition": "#data.age >= 18",
        "message": "Must be at least 18",
        "severity": "ERROR"
      }
    ]
  }'

# Define named rule
curl -X POST http://localhost:8080/api/rules/define/adult-check \
  -H "Content-Type: application/json" \
  -d '{
    "condition": "#age >= 18",
    "message": "Customer must be an adult"
  }'
```

---

## 8. Data Configuration Features

### Environment-Specific Configuration

```yaml
# Development environment
---
spring:
  profiles: dev

metadata:
  name: "Development Configuration"
  environment: "development"

enrichments:
  - id: "test-data-enrichment"
    lookup-config:
      lookup-dataset:
        type: "inline"
        data:
          - code: "TEST001"
            name: "Test Data 1"

---
# Production environment
spring:
  profiles: prod

metadata:
  name: "Production Configuration"
  environment: "production"

enrichments:
  - id: "production-data-enrichment"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/production-data.yaml"
        cache-enabled: true
        cache-ttl-seconds: 3600
```

### Performance Optimization

```yaml
enrichments:
  - id: "high-performance-lookup"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/large-dataset.yaml"
        key-field: "id"
        # Performance optimizations
        cache-enabled: true
        cache-ttl-seconds: 7200
        preload-enabled: true
        cache-max-size: 10000
        cache-refresh-ahead: true
```

---

## 9. Advanced Data Patterns

### Rule Chaining with Data Dependencies

```yaml
rule-chains:
  - id: "data-processing-chain"
    pattern: "sequential-dependency"
    configuration:
      stages:
        - stage: "data-validation"
          rules:
            - condition: "#data != null && #data.id != null"
              message: "Data validation passed"
          output-variable: "validationResult"
          
        - stage: "data-enrichment"
          depends-on: ["data-validation"]
          rules:
            - condition: "#validationResult == true"
              message: "Data enrichment applied"
          output-variable: "enrichmentResult"
          
        - stage: "business-rules"
          depends-on: ["data-enrichment"]
          rules:
            - condition: "#enrichmentResult == true && #data.amount > 1000"
              message: "High-value transaction processing"
```

### Data Migration Patterns

```java
// Step 1: Extract data from existing service
@Service
public class DataMigrationService {
    
    public void migrateToYamlDataset() {
        // Extract from existing service
        List<Currency> currencies = existingCurrencyService.getAllCurrencies();
        
        // Convert to YAML dataset format
        YamlEnrichment.LookupDataset dataset = new YamlEnrichment.LookupDataset();
        dataset.setType("inline");
        dataset.setKeyField("code");
        
        List<Map<String, Object>> data = currencies.stream()
            .map(this::convertToMap)
            .collect(Collectors.toList());
        dataset.setData(data);
        
        // Create enrichment configuration
        YamlEnrichment enrichment = new YamlEnrichment();
        enrichment.setId("currency-enrichment");
        enrichment.setType("lookup-enrichment");
        enrichment.setLookupConfig(createLookupConfig(dataset));
        
        // Save to YAML file
        saveEnrichmentToYaml(enrichment, "config/currency-enrichment.yaml");
    }
}
```

## Best Practices

### Dataset Organization
- **Use external files** for reusable datasets (> 50 records)
- **Keep inline datasets small** (< 50 records)
- **Use meaningful IDs and names** for all configurations
- **Include comprehensive metadata** for documentation

### Performance Optimization
- **Enable caching** for frequently accessed datasets
- **Use appropriate TTL values** based on data volatility
- **Monitor performance metrics** regularly
- **Preload datasets** when possible for better response times

### Data Security
- **Validate all input data** before processing
- **Use type-safe operations** throughout
- **Implement proper error handling** with graceful degradation
- **Maintain audit trails** for data access and modifications

### Configuration Management
- **Version control** all configuration files
- **Use environment-specific** configurations
- **Document dataset sources** and update procedures
- **Regular review and cleanup** of unused datasets

## Troubleshooting

### Common Issues
- **Configuration not loading**: Check YAML syntax and file paths
- **Enrichment not working**: Verify condition expressions and field mappings
- **Performance issues**: Enable caching and monitor metrics
- **Data not found**: Check key field matching and default values

### Debugging Tools
- **Enable debug logging** for detailed operation traces
- **Use REST API endpoints** for configuration validation
- **Monitor performance metrics** for bottleneck identification
- **Test configurations** in development environment first

---

For complete implementation details and advanced patterns, refer to the [Technical Reference Guide](TECHNICAL_REFERENCE.md) and [Rules Engine User Guide](RULES_ENGINE_USER_GUIDE.md).
