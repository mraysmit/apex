
# Generic Implementation of Enricher Interface

After examining the current implementation of the `Enricher` interface and its concrete implementations like `ProductEnricher`, I can see that creating a generic version would be beneficial. This would provide type safety and eliminate the need for type checking and casting in each implementation.

## Current Implementation Analysis

The current `Enricher` interface is simple but lacks type safety:

```java
public interface Enricher extends NamedService {
    Object enrich(Object value);
}
```

Each concrete implementation (like `ProductEnricher`, `CustomerEnricher`, and `TradeEnricher`) follows a similar pattern:
1. Check if the input is of the expected type
2. Cast it to the expected type
3. Perform enrichment operations
4. Return the enriched object

This leads to repetitive code and potential runtime errors if the wrong type is passed.

## Proposed Generic Enricher Interface

I propose creating a generic version of the `Enricher` interface:

```java
public interface Enricher<T> extends NamedService {
    T enrich(T value);
}
```

## Generic Abstract Implementation

To further reduce boilerplate code, we can create an abstract base class:

```java
public abstract class AbstractEnricher<T> implements Enricher<T> {
    private final String name;
    private final Class<T> type;
    
    protected AbstractEnricher(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public Class<T> getType() {
        return type;
    }
}
```

## Example Generic ProductEnricher Implementation

Here's how the `ProductEnricher` would be implemented using the generic interface:

```java
public class ProductEnricher extends AbstractEnricher<Product> {
    private final Map<String, Double> categoryDiscounts;
    private final Map<String, String> categoryDescriptions;
    
    public ProductEnricher(String name) {
        super(name, Product.class);
        this.categoryDiscounts = new HashMap<>();
        this.categoryDescriptions = new HashMap<>();
        
        // Initialize with default values
        initializeDefaultValues();
    }
    
    // Other methods remain the same
    
    @Override
    public Product enrich(Product product) {
        if (product == null) {
            return null;
        }
        
        String category = product.getCategory();
        
        // Create a new product with the same properties
        Product enrichedProduct = new Product(
            product.getName(),
            product.getPrice(),
            product.getCategory()
        );
        
        // Apply discount if available for this category
        if (categoryDiscounts.containsKey(category)) {
            double discount = categoryDiscounts.get(category);
            double discountedPrice = product.getPrice() * (1 - discount);
            enrichedProduct.setPrice(discountedPrice);
        }
        
        // Add description to the product name if available
        if (categoryDescriptions.containsKey(category)) {
            String description = categoryDescriptions.get(category);
            enrichedProduct.setName(product.getName() + " - " + description);
        }
        
        return enrichedProduct;
    }
}
```

## Updating EnrichmentService

The `EnrichmentService` would need to be updated to work with the generic interface. Here's a simplified version:

```java
public class EnrichmentService {
    private final LookupServiceRegistry registry;
    private final RulesEngine rulesEngine;
    
    // Constructors remain the same
    
    public <T> T enrich(String enricherName, T value) {
        LOGGER.fine("Enriching value using enricher: " + enricherName);
        
        // Get the enricher from the registry
        Enricher<?> enricher = registry.getService(enricherName, Enricher.class);
        if (enricher == null) {
            LOGGER.warning("Enricher not found: " + enricherName);
            return value;
        }
        
        // Check if the enricher can handle this type
        if (value != null && !enricher.getType().isInstance(value)) {
            LOGGER.warning("Enricher " + enricherName + " cannot handle type: " + value.getClass().getName());
            return value;
        }
        
        // Call the enricher
        @SuppressWarnings("unchecked")
        Enricher<T> typedEnricher = (Enricher<T>) enricher;
        return typedEnricher.enrich(value);
    }
    
    // Other methods would need similar updates
}
```

## Benefits of the Generic Approach

1. **Type Safety**: Eliminates runtime type checking and casting
2. **Code Reuse**: Reduces boilerplate code in concrete implementations
3. **Better IDE Support**: Provides better code completion and error detection
4. **Clearer API**: Makes the expected types explicit in the interface
5. **Consistency**: Ensures all enrichers follow the same pattern

## Conclusion

Implementing a generic version of the `Enricher` interface would significantly improve the code quality and maintainability of the rules engine. The generic approach provides type safety, reduces boilerplate code, and makes the API more intuitive. The changes required are relatively straightforward and would not disrupt existing functionality.