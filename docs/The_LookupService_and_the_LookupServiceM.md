
# Enhancing LookupService and LookupServiceManager Integration

## Current Architecture Analysis

The current architecture has several components that need to be integrated:

1. **LookupService**: A simple class that holds a list of lookup values and provides basic lookup functionality.
2. **LookupServiceManager**: Manages LookupService instances and provides methods to find matching/non-matching records.
3. **DataServiceManager**: Manages DataSource instances and provides methods to load, register, and request data.
4. **DataSource**: An interface that abstracts data sources, allowing different implementations to be used interchangeably.

## Recommended Implementation

To enhance the LookupService and LookupServiceManager for future features like validation and enrichment, I recommend the following implementation:

### 1. Create an Abstract Lookup Interface

```java
public interface Lookup {
    String getName();
    boolean validate(Object value);
    Object enrich(Object value);
    Object transform(Object value);
}
```

### 2. Enhance LookupService to Implement the Lookup Interface

```java
public class LookupService implements Lookup {
    private List<String> lookupValues;
    private String name;
    private Map<String, Object> enrichmentData;
    private Function<Object, Object> transformationFunction;

    public LookupService(String name, List<String> lookupValues) {
        this.name = name;
        this.lookupValues = lookupValues;
        this.enrichmentData = new HashMap<>();
        this.transformationFunction = value -> value; // Identity function by default
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Object value) {
        if (value instanceof String) {
            return lookupValues.contains(value);
        }
        return false;
    }

    @Override
    public Object enrich(Object value) {
        if (value instanceof String && enrichmentData.containsKey(value)) {
            return enrichmentData.get(value);
        }
        return value;
    }

    @Override
    public Object transform(Object value) {
        return transformationFunction.apply(value);
    }

    public void setEnrichmentData(Map<String, Object> enrichmentData) {
        this.enrichmentData = enrichmentData;
    }

    public void setTransformationFunction(Function<Object, Object> transformationFunction) {
        this.transformationFunction = transformationFunction;
    }

    // Existing methods
    public List<String> getLookupValues() {
        return lookupValues;
    }

    public boolean containsValue(String value) {
        return lookupValues.contains(value);
    }
}
```

### 3. Enhance LookupServiceManager to Integrate with DataServiceManager

```java
public class LookupServiceManager {
    private final ExpressionEvaluatorService evaluatorService;
    private final DataServiceManager dataServiceManager;
    private final Map<String, Lookup> lookupServices;

    public LookupServiceManager(ExpressionEvaluatorService evaluatorService, DataServiceManager dataServiceManager) {
        this.evaluatorService = evaluatorService;
        this.dataServiceManager = dataServiceManager;
        this.lookupServices = new HashMap<>();
        initializeLookupServices();
    }

    private void initializeLookupServices() {
        // Load lookup services from DataServiceManager
        DataSource lookupDataSource = dataServiceManager.getDataSourceByType("lookupServices");
        if (lookupDataSource != null) {
            List<LookupService> services = lookupDataSource.getData("lookupServices");
            if (services != null) {
                for (LookupService service : services) {
                    registerLookupService(service);
                }
            }
        }
    }

    public void registerLookupService(Lookup lookupService) {
        lookupServices.put(lookupService.getName(), lookupService);
    }

    public Lookup getLookupService(String name) {
        return lookupServices.get(name);
    }

    // Validation methods
    public boolean validate(String lookupName, Object value) {
        Lookup lookup = getLookupService(lookupName);
        return lookup != null && lookup.validate(value);
    }

    // Enrichment methods
    public Object enrich(String lookupName, Object value) {
        Lookup lookup = getLookupService(lookupName);
        return lookup != null ? lookup.enrich(value) : value;
    }

    // Transformation methods
    public Object transform(String lookupName, Object value) {
        Lookup lookup = getLookupService(lookupName);
        return lookup != null ? lookup.transform(value) : value;
    }

    // Existing methods with enhanced functionality
    public List<Trade> findMatchingRecords(List<Trade> sourceTrades, List<String> lookupNames) {
        List<Trade> matchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            boolean hasMatch = false;
            for (String lookupName : lookupNames) {
                Lookup lookup = getLookupService(lookupName);
                if (lookup != null && lookup.validate(trade.getValue())) {
                    hasMatch = true;
                    break;
                }
            }
            if (hasMatch) {
                matchingTrades.add(trade);
            }
        }
        return matchingTrades;
    }

    public List<Trade> findNonMatchingRecords(List<Trade> sourceTrades, List<String> lookupNames) {
        List<Trade> nonMatchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            boolean hasMatch = false;
            for (String lookupName : lookupNames) {
                Lookup lookup = getLookupService(lookupName);
                if (lookup != null && lookup.validate(trade.getValue())) {
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) {
                nonMatchingTrades.add(trade);
            }
        }
        return nonMatchingTrades;
    }
}
```

### 4. Create a LookupDataSource Implementation

```java
public class LookupDataSource implements DataSource {
    private final String name;
    private final String dataType;
    private final Map<String, List<String>> lookupData;

    public LookupDataSource(String name) {
        this.name = name;
        this.dataType = "lookupServices";
        this.lookupData = new HashMap<>();
        initializeDefaultLookups();
    }

    private void initializeDefaultLookups() {
        // Initialize with default lookup data
        lookupData.put("InstrumentTypes", Arrays.asList("Equity", "Bond", "Option", "Future", "Swap", "ETF"));
        lookupData.put("Markets", Arrays.asList("NYSE", "NASDAQ", "LSE", "TSE", "HKEX", "SGX"));
        lookupData.put("TradeStatuses", Arrays.asList("Executed", "Settled", "Failed", "Pending", "Cancelled"));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDataType() {
        return dataType;
    }

    @Override
    public boolean supportsDataType(String dataType) {
        return this.dataType.equals(dataType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getData(String dataType, Object... parameters) {
        if (!supportsDataType(dataType)) {
            return null;
        }

        List<LookupService> services = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : lookupData.entrySet()) {
            services.add(new LookupService(entry.getKey(), entry.getValue()));
        }
        
        return (T) services;
    }

    public void addLookupData(String name, List<String> values) {
        lookupData.put(name, values);
    }
}
```

### 5. Create Specialized Lookup Implementations for Different Use Cases

#### ValidationLookup for Data Validation

```java
public class ValidationLookup implements Lookup {
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

    @Override
    public Object enrich(Object value) {
        return value; // No enrichment by default
    }

    @Override
    public Object transform(Object value) {
        return value; // No transformation by default
    }
}
```

#### EnrichmentLookup for Data Enrichment

```java
public class EnrichmentLookup implements Lookup {
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
    public boolean validate(Object value) {
        return true; // Always valid
    }

    @Override
    public Object enrich(Object value) {
        return enrichmentData.getOrDefault(value, value);
    }

    @Override
    public Object transform(Object value) {
        return value; // No transformation by default
    }
}
```

## Benefits of This Implementation

1. **Abstraction**: The Lookup interface provides a common abstraction for different types of lookup operations.
2. **Flexibility**: Different implementations can be created for specific use cases (validation, enrichment, transformation).
3. **Integration**: LookupServiceManager integrates with DataServiceManager to load lookup data from various sources.
4. **Extensibility**: New lookup types can be added without modifying existing code.
5. **Reusability**: Lookup services can be reused across different features of the RulesEngine.

## Usage Examples for Future Features

### Data Validation

```java
// Create a validation lookup for email addresses
ValidationLookup emailValidator = new ValidationLookup("EmailValidator", 
    value -> value instanceof String && ((String)value).matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"));

// Register with the lookup service manager
lookupServiceManager.registerLookupService(emailValidator);

// Use for validation
boolean isValid = lookupServiceManager.validate("EmailValidator", "user@example.com");
```

### Data Enrichment

```java
// Create an enrichment lookup for country codes
Map<Object, Object> countryData = new HashMap<>();
countryData.put("US", "United States");
countryData.put("UK", "United Kingdom");
countryData.put("FR", "France");

EnrichmentLookup countryEnricher = new EnrichmentLookup("CountryEnricher", countryData);

// Register with the lookup service manager
lookupServiceManager.registerLookupService(countryEnricher);

// Use for enrichment
String countryName = (String) lookupServiceManager.enrich("CountryEnricher", "US");
```

This implementation provides a solid foundation for extending the RulesEngine with validation and enrichment capabilities while maintaining a clean, modular architecture.