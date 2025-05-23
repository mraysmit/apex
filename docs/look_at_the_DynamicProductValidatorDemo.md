
# Proposal for Improving DynamicProductValidatorDemo Parameter Handling

## Current Issue
The current implementation of `DynamicProductValidatorDemo` uses named parameters in its constructor:

```java
public DynamicProductValidatorDemo(String name, double minPrice, double maxPrice, String requiredCategory)
```

This approach has limitations:
1. Adding new parameters requires changing the constructor signature, breaking existing code
2. All parameters are required, even if some are optional in certain use cases
3. The parameter list could grow unwieldy as more validation criteria are added
4. It's difficult to add new validation criteria without modifying the class

## Proposed Solution: Parameter Object Pattern

I propose using a parameter object pattern with a builder to make the interface more flexible and extensible:

```java
public class DynamicProductValidatorDemo {
    private final ProductValidatorConfig config;
    private final RulesEngine rulesEngine;
    private final RuleGroup validationRuleGroup;
    private final StandardEvaluationContext context;

    /**
     * Create a new DynamicProductValidatorDemo with the specified configuration.
     *
     * @param config The configuration for this validator
     */
    public DynamicProductValidatorDemo(ProductValidatorConfig config) {
        this.config = config;
        this.rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        this.validationRuleGroup = createValidationRuleGroup();
        this.context = new StandardEvaluationContext();
        
        // Initialize context with validation criteria from config
        for (Map.Entry<String, Object> entry : config.getParameters().entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
    }

    // Rest of the class remains largely the same, but uses config.getXxx() methods
    // instead of direct field access
    
    private Map<String, Object> createFacts(Product product) {
        Map<String, Object> facts = new HashMap<>(config.getParameters());
        facts.put("product", product);
        return facts;
    }
}
```

### Configuration Class with Builder Pattern

```java
public class ProductValidatorConfig {
    private final String name;
    private final Map<String, Object> parameters = new HashMap<>();
    
    private ProductValidatorConfig(Builder builder) {
        this.name = builder.name;
        this.parameters.putAll(builder.parameters);
    }
    
    public String getName() {
        return name;
    }
    
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
    
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    public static class Builder {
        private final String name;
        private final Map<String, Object> parameters = new HashMap<>();
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder withParameter(String key, Object value) {
            parameters.put(key, value);
            return this;
        }
        
        // Convenience methods for common parameters
        public Builder withMinPrice(double minPrice) {
            return withParameter("minPrice", minPrice);
        }
        
        public Builder withMaxPrice(double maxPrice) {
            return withParameter("maxPrice", maxPrice);
        }
        
        public Builder withRequiredCategory(String category) {
            return withParameter("requiredCategory", category);
        }
        
        // Add more convenience methods as needed
        
        public ProductValidatorConfig build() {
            return new ProductValidatorConfig(this);
        }
    }
}
```

## Usage Example

```java
// Create a validator for fixed income products
ProductValidatorConfig config = new ProductValidatorConfig.Builder("fixedIncomeValidator")
    .withMinPrice(100.0)
    .withMaxPrice(2000.0)
    .withRequiredCategory("FixedIncome")
    // Add any future parameters without changing the constructor
    .withParameter("allowedSubcategories", Arrays.asList("Government", "Corporate"))
    .build();

DynamicProductValidatorDemo validator = new DynamicProductValidatorDemo(config);
```

## Benefits of This Approach

1. **Extensibility**: New parameters can be added without changing the constructor signature
2. **Flexibility**: Parameters can be optional, with sensible defaults
3. **Maintainability**: The code is more organized and easier to understand
4. **Encapsulation**: The configuration is encapsulated in a dedicated class
5. **Type Safety**: The builder pattern provides type safety for parameters
6. **Readability**: The builder pattern makes the code more readable with named methods

## Implementation Considerations

1. The `createValidationRuleGroup()` method would need to be updated to read parameters from the config object
2. Existing code would need to be updated to use the new pattern, but this could be done gradually
3. For backward compatibility, a constructor that takes the original parameters could be maintained temporarily, delegating to the new constructor

## Conclusion

This approach provides a more flexible and extensible way to pass parameters to the `DynamicProductValidatorDemo` class. It allows for adding new parameters without breaking existing code and makes the class more maintainable in the long run.