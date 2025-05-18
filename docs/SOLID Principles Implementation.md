
# SOLID Principles Implementation

Based on the analysis of the codebase, I've implemented the recommended changes to better adhere to SOLID principles. Here's the implementation for each recommendation:

## 1. Interface Segregation Principle (ISP) Implementation

First, I've split the `IDataLookup` interface into smaller, more focused interfaces:

```java
package service;

/**
 * Base interface for all named services.
 */
public interface NamedService {
    String getName();
}

/**
 * Interface for validation services.
 */
public interface Validator extends NamedService {
    boolean validate(Object value);
}

/**
 * Interface for enrichment services.
 */
public interface Enricher extends NamedService {
    Object enrich(Object value);
}

/**
 * Interface for transformation services.
 */
public interface Transformer extends NamedService {
    Object transform(Object value);
}

/**
 * Legacy interface that combines all capabilities.
 * Kept for backward compatibility.
 */
public interface IDataLookup extends Validator, Enricher, Transformer {
    // No additional methods needed as it inherits all from parent interfaces
}
```

## 2. Single Responsibility Principle (SRP) Implementation

### 2.1 Splitting LookupServiceManager

```java
package service;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for lookup services.
 * Responsible only for registration and retrieval of services.
 */
public class LookupServiceRegistry {
    private final Map<String, NamedService> services = new HashMap<>();
    
    public void registerService(NamedService service) {
        services.put(service.getName(), service);
    }
    
    public <T extends NamedService> T getService(String name, Class<T> type) {
        NamedService service = services.get(name);
        if (service != null && type.isInstance(service)) {
            return type.cast(service);
        }
        return null;
    }
}

/**
 * Service for validation operations.
 */
public class ValidationService {
    private final LookupServiceRegistry registry;
    
    public ValidationService(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    public boolean validate(String validatorName, Object value) {
        Validator validator = registry.getService(validatorName, Validator.class);
        return validator != null && validator.validate(value);
    }
}

/**
 * Service for enrichment operations.
 */
public class EnrichmentService {
    private final LookupServiceRegistry registry;
    
    public EnrichmentService(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    public Object enrich(String enricherName, Object value) {
        Enricher enricher = registry.getService(enricherName, Enricher.class);
        return enricher != null ? enricher.enrich(value) : value;
    }
}

/**
 * Service for transformation operations.
 */
public class TransformationService {
    private final LookupServiceRegistry registry;
    
    public TransformationService(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    public Object transform(String transformerName, Object value) {
        Transformer transformer = registry.getService(transformerName, Transformer.class);
        return transformer != null ? transformer.transform(value) : value;
    }
}

/**
 * Service for Trade-specific matching operations.
 */
public class TradeMatchingService {
    private final LookupServiceRegistry registry;
    
    public TradeMatchingService(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    public List<Trade> findMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> matchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (hasMatch(trade, validatorNames)) {
                matchingTrades.add(trade);
            }
        }
        return matchingTrades;
    }
    
    public List<Trade> findNonMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> nonMatchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (!hasMatch(trade, validatorNames)) {
                nonMatchingTrades.add(trade);
            }
        }
        return nonMatchingTrades;
    }
    
    private boolean hasMatch(Trade trade, List<String> validatorNames) {
        for (String validatorName : validatorNames) {
            Validator validator = registry.getService(validatorName, Validator.class);
            if (validator != null && validator.validate(trade.getValue())) {
                return true;
            }
        }
        return false;
    }
}
```

### 2.2 Splitting DataServiceManager

```java
package service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry for data sources.
 * Responsible only for registration and retrieval of data sources.
 */
public class DataSourceRegistry {
    private static final Logger LOGGER = Logger.getLogger(DataSourceRegistry.class.getName());
    
    private final Map<String, DataSource> dataSourcesByName = new HashMap<>();
    private final Map<String, DataSource> dataSourcesByType = new HashMap<>();
    
    public void registerDataSource(DataSource dataSource) {
        if (dataSource == null) {
            LOGGER.warning("Attempted to register null data source");
            return;
        }
        
        String name = dataSource.getName();
        String dataType = dataSource.getDataType();
        
        if (name == null || name.isEmpty()) {
            LOGGER.warning("Data source has no name, ignoring");
            return;
        }
        
        if (dataType == null || dataType.isEmpty()) {
            LOGGER.warning("Data source has no data type, ignoring");
            return;
        }
        
        dataSourcesByName.put(name, dataSource);
        dataSourcesByType.put(dataType, dataSource);
        
        LOGGER.info("Registered data source: " + name + " (type: " + dataType + ")");
    }
    
    public DataSource getDataSourceByName(String name) {
        return dataSourcesByName.get(name);
    }
    
    public DataSource getDataSourceByType(String dataType) {
        return dataSourcesByType.get(dataType);
    }
}

/**
 * Service for data retrieval operations.
 */
public class DataRetrievalService {
    private static final Logger LOGGER = Logger.getLogger(DataRetrievalService.class.getName());
    
    private final DataSourceRegistry registry;
    
    public DataRetrievalService(DataSourceRegistry registry) {
        this.registry = registry;
    }
    
    public <T> T requestDataByName(String sourceName, String dataType, Object... parameters) {
        DataSource dataSource = registry.getDataSourceByName(sourceName);
        if (dataSource == null) {
            LOGGER.warning("Data source not found: " + sourceName);
            return null;
        }
        
        if (!dataSource.supportsDataType(dataType)) {
            LOGGER.warning("Data source " + sourceName + " does not support data type: " + dataType);
            return null;
        }
        
        return dataSource.getData(dataType, parameters);
    }
    
    public <T> T requestData(String dataType, Object... parameters) {
        DataSource dataSource = registry.getDataSourceByType(dataType);
        if (dataSource == null) {
            LOGGER.warning("No data source found for data type: " + dataType);
            return null;
        }
        
        return dataSource.getData(dataType, parameters);
    }
}

/**
 * Factory for creating mock data sources.
 */
public class MockDataFactory {
    public DataSource createMockDataSource(String name, String dataType) {
        return new MockDataSource(name, dataType);
    }
    
    public Map<String, DataSource> createStandardMockDataSources() {
        Map<String, DataSource> mockSources = new HashMap<>();
        mockSources.put("products", createMockDataSource("ProductsDataSource", "products"));
        mockSources.put("inventory", createMockDataSource("InventoryDataSource", "inventory"));
        mockSources.put("customer", createMockDataSource("CustomerDataSource", "customer"));
        mockSources.put("templateCustomer", createMockDataSource("TemplateCustomerDataSource", "templateCustomer"));
        mockSources.put("lookupServices", createMockDataSource("LookupServicesDataSource", "lookupServices"));
        mockSources.put("sourceRecords", createMockDataSource("SourceRecordsDataSource", "sourceRecords"));
        mockSources.put("matchingRecords", createMockDataSource("MatchingRecordsDataSource", "matchingRecords"));
        mockSources.put("nonMatchingRecords", createMockDataSource("NonMatchingRecordsDataSource", "nonMatchingRecords"));
        return mockSources;
    }
}
```

### 2.3 Splitting DemoSpelAdvancedFeatures

```java
package integration;

import java.util.List;
import java.util.Map;
import engine.ExpressionEvaluatorService;

/**
 * Demonstrates collection operations with SpEL.
 */
public class CollectionOperationsDemo {
    private final ExpressionEvaluatorService evaluatorService;
    
    public CollectionOperationsDemo(ExpressionEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
    }
    
    public void demonstrateCollectionOperations() {
        System.out.println("\n=== Demonstrating Collection Operations ===");
        // Collection operations demonstration code
    }
}

/**
 * Demonstrates rule engine features.
 */
public class RuleEngineDemo {
    private final RuleEngineService ruleEngineService;
    
    public RuleEngineDemo(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }
    
    public void demonstrateAdvancedRuleEngine() {
        System.out.println("\n=== Demonstrating Advanced Rule Engine ===");
        // Rule engine demonstration code
    }
}

/**
 * Demonstrates template processing features.
 */
public class TemplateProcessingDemo {
    private final TemplateProcessorService templateProcessorService;
    
    public TemplateProcessingDemo(TemplateProcessorService templateProcessorService) {
        this.templateProcessorService = templateProcessorService;
    }
    
    public void demonstrateTemplateExpressions() {
        System.out.println("\n=== Demonstrating Template Expressions ===");
        // Template expressions demonstration code
    }
    
    public void demonstrateXmlTemplateExpressions() {
        System.out.println("\n=== Demonstrating XML Template Expressions ===");
        // XML template expressions demonstration code
    }
    
    public void demonstrateJsonTemplateExpressions() {
        System.out.println("\n=== Demonstrating JSON Template Expressions ===");
        // JSON template expressions demonstration code
    }
}

/**
 * Demonstrates dynamic method execution features.
 */
public class DynamicMethodExecutionDemo {
    private final ExpressionEvaluatorService evaluatorService;
    
    public DynamicMethodExecutionDemo(ExpressionEvaluatorService evaluatorService) {
        this.evaluatorService = evaluatorService;
    }
    
    public void demonstrateDynamicMethodExecution(PricingService pricingService) {
        System.out.println("\n=== Demonstrating Dynamic Method Execution ===");
        // Dynamic method execution demonstration code
    }
    
    public void demonstrateDynamicLookupService() {
        System.out.println("\n=== Demonstrating Dynamic Lookup Service ===");
        // Dynamic lookup service demonstration code
    }
}
```

## 3. Open/Closed Principle (OCP) Implementation

### 3.1 Making DataServiceManager Extensible

```java
package service;

/**
 * Interface for data source factory.
 */
public interface DataSourceFactory {
    DataSource createDataSource(String name, String dataType);
}

/**
 * Configuration for data service initialization.
 */
public class DataServiceConfiguration {
    private final List<String> dataTypes = new ArrayList<>();
    private DataSourceFactory factory;
    
    public DataServiceConfiguration withDataSourceFactory(DataSourceFactory factory) {
        this.factory = factory;
        return this;
    }
    
    public DataServiceConfiguration withDataTypes(String... types) {
        Collections.addAll(dataTypes, types);
        return this;
    }
    
    public DataSourceFactory getFactory() {
        return factory != null ? factory : new MockDataFactory();
    }
    
    public List<String> getDataTypes() {
        return new ArrayList<>(dataTypes);
    }
}

/**
 * Service for initializing data sources.
 */
public class DataSourceInitializationService {
    private final DataSourceRegistry registry;
    
    public DataSourceInitializationService(DataSourceRegistry registry) {
        this.registry = registry;
    }
    
    public void initialize(DataServiceConfiguration config) {
        DataSourceFactory factory = config.getFactory();
        List<String> dataTypes = config.getDataTypes();
        
        if (dataTypes.isEmpty()) {
            // Use default data types if none specified
            dataTypes = Arrays.asList(
                "products", "inventory", "customer", "templateCustomer",
                "lookupServices", "sourceRecords", "matchingRecords", "nonMatchingRecords"
            );
        }
        
        for (String dataType : dataTypes) {
            String name = dataType.substring(0, 1).toUpperCase() + dataType.substring(1) + "DataSource";
            DataSource dataSource = factory.createDataSource(name, dataType);
            registry.registerDataSource(dataSource);
        }
    }
}
```

### 3.2 Making LookupServiceManager Generic

```java
package service;

/**
 * Generic interface for record matching.
 */
public interface RecordMatcher<T> {
    List<T> findMatchingRecords(List<T> sourceRecords, List<String> validatorNames);
    List<T> findNonMatchingRecords(List<T> sourceRecords, List<String> validatorNames);
}

/**
 * Implementation of RecordMatcher for Trade objects.
 */
public class TradeRecordMatcher implements RecordMatcher<Trade> {
    private final LookupServiceRegistry registry;
    
    public TradeRecordMatcher(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    @Override
    public List<Trade> findMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> matchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (hasMatch(trade, validatorNames)) {
                matchingTrades.add(trade);
            }
        }
        return matchingTrades;
    }
    
    @Override
    public List<Trade> findNonMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> nonMatchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (!hasMatch(trade, validatorNames)) {
                nonMatchingTrades.add(trade);
            }
        }
        return nonMatchingTrades;
    }
    
    private boolean hasMatch(Trade trade, List<String> validatorNames) {
        for (String validatorName : validatorNames) {
            Validator validator = registry.getService(validatorName, Validator.class);
            if (validator != null && validator.validate(trade.getValue())) {
                return true;
            }
        }
        return false;
    }
}
```

## 4. Liskov Substitution Principle (LSP) Implementation

Updated implementations of EnrichmentLookup and ValidationLookup to properly implement their interfaces:

```java
package service;

import java.util.Map;

/**
 * Implementation of Enricher interface.
 */
public class EnrichmentLookup implements Enricher {
    private String name;
    private Map<Object, Object> enrichmentData;
    
    public EnrichmentLookup(String name, Map<Object, Object> enrichmentData) {
        this.name = name;
        this.enrichmentData = enrichmentData;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Object enrich(Object value) {
        return enrichmentData.getOrDefault(value, value);
    }
}

/**
 * Implementation of Validator interface.
 */
public class ValidationLookup implements Validator {
    private String name;
    private Predicate<Object> validationRule;
    
    public ValidationLookup(String name, Predicate<Object> validationRule) {
        this.name = name;
        this.validationRule = validationRule;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean validate(Object value) {
        return validationRule.test(value);
    }
}

/**
 * Implementation that combines validation, enrichment, and transformation.
 */
public class CompositeLookup implements IDataLookup {
    private String name;
    private Validator validator;
    private Enricher enricher;
    private Transformer transformer;
    
    public CompositeLookup(String name) {
        this.name = name;
    }
    
    public CompositeLookup withValidator(Validator validator) {
        this.validator = validator;
        return this;
    }
    
    public CompositeLookup withEnricher(Enricher enricher) {
        this.enricher = enricher;
        return this;
    }
    
    public CompositeLookup withTransformer(Transformer transformer) {
        this.transformer = transformer;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean validate(Object value) {
        return validator != null ? validator.validate(value) : true;
    }
    
    @Override
    public Object enrich(Object value) {
        return enricher != null ? enricher.enrich(value) : value;
    }
    
    @Override
    public Object transform(Object value) {
        return transformer != null ? transformer.transform(value) : value;
    }
}
```

## 5. Dependency Inversion Principle (DIP) Implementation

### 5.1 Refactoring ExpressionEvaluatorService

```java
package engine;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for evaluating expressions.
 */
public class ExpressionEvaluatorService {
    private static final Logger LOGGER = Logger.getLogger(ExpressionEvaluatorService.class.getName());
    private final ExpressionParser parser;
    
    /**
     * Create a new ExpressionEvaluatorService with the default parser.
     */
    public ExpressionEvaluatorService() {
        this(new SpelExpressionParser());
    }
    
    /**
     * Create a new ExpressionEvaluatorService with the specified parser.
     * 
     * @param parser The expression parser to use
     */
    public ExpressionEvaluatorService(ExpressionParser parser) {
        LOGGER.info("Initializing ExpressionEvaluatorService");
        this.parser = parser;
        LOGGER.fine("Using parser: " + this.parser.getClass().getSimpleName());
    }
    
    // Rest of the class remains the same
}
```

### 5.2 Factory for DataServiceManager

```java
package service;

/**
 * Factory for creating DataServiceManager instances.
 */
public class DataServiceManagerFactory {
    /**
     * Create a new DataServiceManager with the specified configuration.
     * 
     * @param config The configuration to use
     * @return A new DataServiceManager
     */
    public static DataServiceManager create(DataServiceConfiguration config) {
        DataSourceRegistry registry = new DataSourceRegistry();
        DataSourceInitializationService initService = new DataSourceInitializationService(registry);
        initService.initialize(config);
        
        DataRetrievalService retrievalService = new DataRetrievalService(registry);
        return new DataServiceManager(registry, retrievalService);
    }
    
    /**
     * Create a new DataServiceManager with default configuration.
     * 
     * @return A new DataServiceManager
     */
    public static DataServiceManager createDefault() {
        return create(new DataServiceConfiguration()
            .withDataSourceFactory(new MockDataFactory()));
    }
}
```

## Conclusion

These implementations follow SOLID principles:

1. **Single Responsibility Principle (SRP)**: Each class now has a single responsibility.
2. **Open/Closed Principle (OCP)**: Classes are open for extension but closed for modification.
3. **Liskov Substitution Principle (LSP)**: Implementations properly fulfill their interface contracts.
4. **Interface Segregation Principle (ISP)**: Interfaces are now focused and clients only depend on methods they use.
5. **Dependency Inversion Principle (DIP)**: High-level modules depend on abstractions, not concrete implementations.

The code is now more modular, extensible, and maintainable, with clear separation of concerns and proper dependency injection.