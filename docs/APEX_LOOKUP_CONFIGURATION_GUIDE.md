# APEX Lookup Configuration Guide

This guide provides comprehensive documentation on APEX lookup configurations, including advanced patterns, implementation examples, and best practices.

## Table of Contents

1. [What is "lookup-key"?](#what-is-lookup-key)
2. [Basic Concepts](#basic-concepts)
3. [Advanced Lookup Patterns](#advanced-lookup-patterns)
4. [Implementation Plan](#implementation-plan)
5. [Best Practices](#best-practices)

---

## What is "lookup-key"?

**"lookup-key"** is a **SpEL (Spring Expression Language) expression** used in enrichments to specify **how to extract the key value from your data** that will be used to perform the lookup operation.

### Key Points:

1. **It's a SpEL Expression**: The lookup-key contains a SpEL expression that gets evaluated against your data context
2. **Extracts the Lookup Value**: It tells APEX which field or value from your input data should be used as the key to look up reference data
3. **Used in Lookup Enrichments**: It's specifically used in `lookup-enrichment` type enrichments
4. **Dynamic Evaluation**: The expression is evaluated at runtime for each data record being processed

### Where is it used?

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichment.java" mode="EXCERPT">
````java
public static class LookupConfig {
    @JsonProperty("lookup-service")
    private String lookupService;

    @JsonProperty("lookup-dataset")
    private LookupDataset lookupDataset;

    @JsonProperty("lookup-key")
    private String lookupKey; // SpEL expression to extract lookup key
````
</augment_code_snippet>

### How it works:

<augment_code_snippet path="apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java" mode="EXCERPT">
````java
// 2. Extract lookup key using SpEL expression
Object lookupKey;
try {
    StandardEvaluationContext context = createEvaluationContext(targetObject);
    Expression keyExpr = getOrCompileExpression(lookupConfig.getLookupKey());
    lookupKey = keyExpr.getValue(context);
    
    if (lookupKey == null) {
        LOGGER.fine("Lookup key evaluated to null for enrichment: " + enrichment.getId());
        return targetObject;
    }
    
    LOGGER.fine("Extracted lookup key: " + lookupKey);
} catch (Exception e) {
    throw new EnrichmentException("Failed to extract lookup key using expression '" + 
                                lookupConfig.getLookupKey() + "'", e);
}
````
</augment_code_snippet>

---

## Basic Concepts

### Examples from the codebase:

**1. Simple field reference:**
<augment_code_snippet path="apex-demo/src/main/resources/bootstrap/otc-options-bootstrap.yaml" mode="EXCERPT">
````yaml
lookup-config:
  lookup-service: "postgresLookupService"
  lookup-key: "#sellerParty"  # Extract the sellerParty field value
  cache-enabled: true
````
</augment_code_snippet>

**2. Alternative approach using key-field (for inline datasets):**
<augment_code_snippet path="apex-demo/src/main/resources/demo-rules/dataset-enrichment.yaml" mode="EXCERPT">
````yaml
lookup-config:
  lookup-dataset:
    type: "inline"
    key-field: "code"  # This tells APEX which field in the dataset to match against
    data:
      - code: "USD"
        name: "US Dollar"
````
</augment_code_snippet>

### The difference between lookup-key and key-field:

- **`lookup-key`**: SpEL expression that extracts the value **from your input data** to use for lookup
- **`key-field`**: Specifies which field **in the reference dataset** should be used as the lookup key

### Common lookup-key patterns:

```yaml
# Simple field reference
lookup-key: "#customerId"

# Nested field reference  
lookup-key: "#customer.id"

# Complex expression
lookup-key: "#transaction.counterparty.partyId"

# Conditional expression
lookup-key: "#type == 'CUSTOMER' ? #customerId : #vendorId"

# String manipulation
lookup-key: "#accountNumber.substring(0, 3)"
```

### The Relationship Between `lookup-key` and `key-field`

#### `key-field` - What field in the dataset to match against

<augment_code_snippet path="apex-demo/src/main/resources/demo-rules/dataset-enrichment.yaml" mode="EXCERPT">
````yaml
lookup-config:
  lookup-dataset:
    type: "inline"
    key-field: "code"  # This field in the dataset will be matched
    data:
      - code: "USD"     # This is the key-field value
        name: "US Dollar"
        symbol: "$"
      - code: "EUR"     # This is the key-field value  
        name: "Euro"
        symbol: "€"
````
</augment_code_snippet>

#### `lookup-key` - What value from input data to use for lookup

Complete example showing both:

```yaml
enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    lookup-config:
      lookup-key: "#currency"  # Extract currency field from input data
      lookup-dataset:
        type: "inline"
        key-field: "code"       # Match against 'code' field in dataset
        data:
          - code: "USD"         # This 'code' value gets matched
            name: "US Dollar"
```

### How They Work Together

1. **`lookup-key`** extracts a value from your **input data**
2. **`key-field`** specifies which field in the **reference dataset** to match against
3. APEX compares these two values to find the matching record

#### Example Flow:

```yaml
# Input data: { "currency": "USD", "amount": 1000 }

lookup-config:
  lookup-key: "#currency"     # Extracts "USD" from input data
  lookup-dataset:
    key-field: "code"         # Look for matching "code" in dataset
    data:
      - code: "USD"           # MATCH! This record is selected
        name: "US Dollar"
        symbol: "$"
      - code: "EUR"           # No match
        name: "Euro"
```

### Different Scenarios

#### 1. Simple Field Matching (most common)
```yaml
# Input: { "currency": "USD" }
lookup-key: "#currency"      # Gets "USD"
key-field: "code"           # Matches against dataset's "code" field
```

#### 2. Nested Field Extraction
```yaml
# Input: { "transaction": { "currency": "USD" } }
lookup-key: "#transaction.currency"  # Gets "USD"
key-field: "code"                   # Matches against dataset's "code" field
```

#### 3. Complex Key Extraction
```yaml
# Input: { "account": "USD-123456" }
lookup-key: "#account.substring(0,3)"  # Gets "USD"
key-field: "code"                      # Matches against dataset's "code" field
```

#### 4. Database Lookups (different approach)
```yaml
# For database sources, you might see:
lookup-key: "#customerId"
database-config:
  query: "SELECT * FROM customers WHERE customer_id = ?"
  # The lookup-key value gets passed as the ? parameter
```

### Summary

- **`lookup-key`**: SpEL expression that extracts the lookup value **FROM your input data**
- **`key-field`**: Specifies which field **IN the reference dataset** to match against
- They work as a pair: `lookup-key` value gets compared to `key-field` values to find matches

---

## Advanced Lookup Patterns

Compound lookup keys are definitely possible! APEX supports several approaches for creating compound lookup keys.

### Method 1: String Concatenation in lookup-key

Create a compound key by concatenating multiple fields in the `lookup-key` expression:

```yaml
enrichments:
  - id: "compound-customer-lookup"
    type: "lookup-enrichment"
    condition: "#customerId != null && #region != null"
    lookup-config:
      lookup-key: "#customerId + '-' + #region"  # Creates compound key like "CUST123-US"
      lookup-dataset:
        type: "inline"
        key-field: "compound_key"  # Single field containing compound values
        data:
          - compound_key: "CUST123-US"
            name: "John Doe"
            tier: "GOLD"
            credit_limit: 50000
          - compound_key: "CUST456-EU" 
            name: "Jane Smith"
            tier: "PLATINUM"
            credit_limit: 100000
```

### Method 2: Complex SpEL Expressions

Use more sophisticated SpEL expressions for compound keys:

```yaml
enrichments:
  - id: "trading-pair-lookup"
    type: "lookup-enrichment"
    condition: "#baseCurrency != null && #quoteCurrency != null"
    lookup-config:
      lookup-key: "#baseCurrency.toUpperCase() + '/' + #quoteCurrency.toUpperCase()"
      lookup-dataset:
        type: "inline"
        key-field: "pair"
        data:
          - pair: "EUR/USD"
            spread: 0.0002
            min_size: 1000
            trading_hours: "24/5"
          - pair: "GBP/USD"
            spread: 0.0003
            min_size: 1000
            trading_hours: "24/5"
```

### Method 3: Database Compound Keys with Multiple Parameters

For database lookups, you can use multiple parameters in the query:

```yaml
enrichments:
  - id: "portfolio-position-lookup"
    type: "lookup-enrichment"
    condition: "#portfolioId != null && #instrumentId != null && #asOfDate != null"
    lookup-config:
      lookup-dataset:
        type: "database"
        connection-name: "portfolio-db"
        query: |
          SELECT position_id, quantity, market_value, currency
          FROM portfolio_positions
          WHERE portfolio_id = ?
            AND instrument_id = ?
            AND as_of_date = ?
        parameters:
          - field: "portfolioId"
          - field: "instrumentId"
          - field: "asOfDate"
        cache-enabled: true
        cache-ttl-seconds: 300
    field-mappings:
      - source-field: "quantity"
        target-field: "positionQuantity"
      - source-field: "market_value"
        target-field: "positionValue"
```

### Method 4: Conditional Compound Keys

Create compound keys based on conditions:

```yaml
enrichments:
  - id: "flexible-counterparty-lookup"
    type: "lookup-enrichment"
    condition: "#partyId != null"
    lookup-config:
      lookup-key: "#partyType == 'CUSTOMER' ? 'CUST-' + #partyId : (#partyType == 'VENDOR' ? 'VEND-' + #partyId : 'UNKN-' + #partyId)"
      lookup-dataset:
        type: "inline"
        key-field: "party_key"
        data:
          - party_key: "CUST-12345"
            legal_name: "ABC Corporation"
            party_type: "CUSTOMER"
            credit_rating: "AAA"
          - party_key: "VEND-67890"
            legal_name: "XYZ Suppliers"
            party_type: "VENDOR"
            payment_terms: "NET30"
```

### Method 5: Hierarchical Compound Keys

For nested data structures:

```yaml
enrichments:
  - id: "trade-settlement-lookup"
    type: "lookup-enrichment"
    condition: "#trade != null && #trade.instrument != null && #trade.counterparty != null"
    lookup-config:
      lookup-key: "#trade.instrument.symbol + ':' + #trade.counterparty.id + ':' + #trade.settlementDate.toString()"
      lookup-dataset:
        type: "database"
        connection-name: "settlement-db"
        query: |
          SELECT settlement_instructions, custodian, account_number
          FROM settlement_instructions
          WHERE instrument_symbol = ?
            AND counterparty_id = ?
            AND effective_date <= ?
        parameters:
          - field: "trade.instrument.symbol"
          - field: "trade.counterparty.id"
          - field: "trade.settlementDate"
```

### Method 6: Hash-Based Compound Keys

For very complex compound keys, you can create a hash:

```yaml
enrichments:
  - id: "complex-risk-lookup"
    type: "lookup-enrichment"
    condition: "#portfolio != null"
    lookup-config:
      lookup-key: "T(java.lang.String).valueOf((#portfolio.id + #portfolio.strategy + #portfolio.region + #asOfDate.toString()).hashCode())"
      lookup-dataset:
        type: "database"
        connection-name: "risk-db"
        query: |
          SELECT risk_metrics, var_95, expected_shortfall
          FROM portfolio_risk_cache
          WHERE risk_key_hash = ?
        parameters:
          - field: "riskKeyHash"  # This would be computed from the lookup-key
```

### Method 7: Multi-Dimensional Product Lookup

Here's a comprehensive example combining multiple approaches:

```yaml
enrichments:
  - id: "product-pricing-lookup"
    type: "lookup-enrichment"
    condition: "#product != null && #customer != null && #effectiveDate != null"
    lookup-config:
      lookup-key: "#product.category + '|' + #product.id + '|' + #customer.tier + '|' + #customer.region"
      lookup-dataset:
        type: "database"
        connection-name: "pricing-db"
        query: |
          SELECT
            base_price,
            discount_rate,
            minimum_quantity,
            currency,
            effective_from,
            effective_to
          FROM product_pricing p
          JOIN customer_tiers ct ON ct.tier = ?
          WHERE p.product_category = ?
            AND p.product_id = ?
            AND p.region = ?
            AND p.effective_from <= ?
            AND (p.effective_to IS NULL OR p.effective_to >= ?)
          ORDER BY p.effective_from DESC
          LIMIT 1
        parameters:
          - field: "customer.tier"
          - field: "product.category"
          - field: "product.id"
          - field: "customer.region"
          - field: "effectiveDate"
          - field: "effectiveDate"
        cache-enabled: true
        cache-ttl-seconds: 1800
    field-mappings:
      - source-field: "base_price"
        target-field: "productBasePrice"
      - source-field: "discount_rate"
        target-field: "customerDiscountRate"
      - source-field: "minimum_quantity"
        target-field: "minimumOrderQuantity"
      - source-field: "currency"
        target-field: "pricingCurrency"
```

### Key Points for Compound Lookup Keys:

1. **SpEL Flexibility**: You can use any valid SpEL expression in `lookup-key`
2. **String Operations**: Concatenation, substring, case conversion all work
3. **Conditional Logic**: Ternary operators and complex conditions are supported
4. **Database Parameters**: Multiple parameters map to multiple `?` placeholders in order
5. **Caching**: Compound keys are cached just like simple keys
6. **Performance**: Consider the complexity of your expressions and caching strategy

The key is that `lookup-key` can be any SpEL expression that evaluates to a value that can be matched against your reference data, whether that's a simple field, a compound string, or even a computed hash.

---

## Implementation Plan

This section outlines a comprehensive plan for implementing self-contained examples in the apex-demo module for each YAML configuration use case.

### 🎯 Overview

Each example will include:
- Complete YAML configuration files
- Java demo classes with realistic data
- Model classes representing real-world scenarios
- Documentation explaining the use case

### 📊 Planned Examples Structure

#### 1. Simple Field Lookup Example
**File**: `apex-demo/src/main/resources/examples/lookups/simple-field-lookup.yaml`
**Demo**: `SimpleFieldLookupDemo.java`
**Use Case**: Basic currency code to currency details lookup

**Components**:
- **Model**: `CurrencyTransaction.java` (amount, currencyCode, description)
- **YAML Config**: Simple `lookup-key: "#currencyCode"` with inline currency dataset
- **Demo Data**: Transactions in USD, EUR, GBP, JPY
- **Enrichment**: Add currency name, symbol, decimal places

#### 2. Compound String Concatenation Lookup
**File**: `apex-demo/src/main/resources/examples/lookups/compound-key-lookup.yaml`
**Demo**: `CompoundKeyLookupDemo.java`
**Use Case**: Customer-region specific pricing lookup

**Components**:
- **Model**: `CustomerOrder.java` (customerId, region, productId, quantity)
- **YAML Config**: `lookup-key: "#customerId + '-' + #region"`
- **Demo Data**: Orders from different customers in different regions
- **Enrichment**: Add customer tier, regional discount, special pricing

#### 3. Complex SpEL Expression Lookup
**File**: `apex-demo/src/main/resources/examples/lookups/complex-spel-lookup.yaml`
**Demo**: `ComplexSpelLookupDemo.java`
**Use Case**: Trading pair lookup with currency normalization

**Components**:
- **Model**: `CurrencyTrade.java` (baseCurrency, quoteCurrency, amount, tradeDate)
- **YAML Config**: `lookup-key: "#baseCurrency.toUpperCase() + '/' + #quoteCurrency.toUpperCase()"`
- **Demo Data**: Trades with mixed case currency codes
- **Enrichment**: Add spread, minimum size, trading hours, market maker

#### 4. Conditional Compound Key Lookup
**File**: `apex-demo/src/main/resources/examples/lookups/conditional-lookup.yaml`
**Demo**: `ConditionalLookupDemo.java`
**Use Case**: Flexible counterparty lookup based on party type

**Components**:
- **Model**: `BusinessTransaction.java` (partyId, partyType, transactionType, amount)
- **YAML Config**: `lookup-key: "#partyType == 'CUSTOMER' ? 'CUST-' + #partyId : (#partyType == 'VENDOR' ? 'VEND-' + #partyId : 'UNKN-' + #partyId)"`
- **Demo Data**: Mixed transactions with customers, vendors, and unknown parties
- **Enrichment**: Add legal name, credit rating, payment terms, risk category

#### 5. Hierarchical/Nested Field Lookup
**File**: `apex-demo/src/main/resources/examples/lookups/hierarchical-lookup.yaml`
**Demo**: `HierarchicalLookupDemo.java`
**Use Case**: Trade settlement instruction lookup

**Components**:
- **Model**: `FinancialTrade.java` with nested `Instrument.java` and `Counterparty.java`
- **YAML Config**: `lookup-key: "#trade.instrument.symbol + ':' + #trade.counterparty.id + ':' + #settlementDate.toString()"`
- **Demo Data**: Complex trade objects with nested structures
- **Enrichment**: Add settlement instructions, custodian details, account numbers

#### 6. Multi-Dimensional Product Lookup
**File**: `apex-demo/src/main/resources/examples/lookups/multi-dimensional-lookup.yaml`
**Demo**: `MultiDimensionalLookupDemo.java`
**Use Case**: Product pricing based on multiple factors

**Components**:
- **Model**: `ProductOrder.java` with nested `Product.java` and `Customer.java`
- **YAML Config**: `lookup-key: "#product.category + '|' + #product.id + '|' + #customer.tier + '|' + #customer.region"`
- **Demo Data**: Orders with various product categories, customer tiers, and regions
- **Enrichment**: Add base price, discount rate, minimum quantity, pricing currency

#### 7. Hash-Based Complex Key Lookup
**File**: `apex-demo/src/main/resources/examples/lookups/hash-based-lookup.yaml`
**Demo**: `HashBasedLookupDemo.java`
**Use Case**: Portfolio risk metrics lookup using computed hash

**Components**:
- **Model**: `PortfolioRiskRequest.java` (portfolioId, strategy, region, asOfDate)
- **YAML Config**: `lookup-key: "T(java.lang.String).valueOf((#portfolioId + #strategy + #region + #asOfDate.toString()).hashCode())"`
- **Demo Data**: Portfolio risk requests with various combinations
- **Enrichment**: Add VaR 95%, expected shortfall, risk metrics

#### 8. Database Lookup with Multiple Parameters
**File**: `apex-demo/src/main/resources/examples/lookups/database-multi-param-lookup.yaml`
**Demo**: `DatabaseMultiParamLookupDemo.java`
**Use Case**: Portfolio position lookup with multiple query parameters

**Components**:
- **Model**: `PositionRequest.java` (portfolioId, instrumentId, asOfDate)
- **YAML Config**: Database lookup with multiple parameters in query
- **Demo Data**: Position requests for different portfolios and instruments
- **Mock Database**: In-memory H2 database with sample position data
- **Enrichment**: Add position quantity, market value, currency

### 🏗️ Implementation Structure

#### Directory Organization:
```
apex-demo/src/main/
├── java/dev/mars/apex/demo/examples/lookups/
│   ├── SimpleFieldLookupDemo.java
│   ├── CompoundKeyLookupDemo.java
│   ├── ComplexSpelLookupDemo.java
│   ├── ConditionalLookupDemo.java
│   ├── HierarchicalLookupDemo.java
│   ├── MultiDimensionalLookupDemo.java
│   ├── HashBasedLookupDemo.java
│   └── DatabaseMultiParamLookupDemo.java
├── java/dev/mars/apex/demo/model/lookups/
│   ├── CurrencyTransaction.java
│   ├── CustomerOrder.java
│   ├── CurrencyTrade.java
│   ├── BusinessTransaction.java
│   ├── FinancialTrade.java (with nested classes)
│   ├── ProductOrder.java (with nested classes)
│   ├── PortfolioRiskRequest.java
│   └── PositionRequest.java
└── resources/examples/lookups/
    ├── simple-field-lookup.yaml
    ├── compound-key-lookup.yaml
    ├── complex-spel-lookup.yaml
    ├── conditional-lookup.yaml
    ├── hierarchical-lookup.yaml
    ├── multi-dimensional-lookup.yaml
    ├── hash-based-lookup.yaml
    └── database-multi-param-lookup.yaml
```

#### Common Features for All Examples:

1. **Self-Contained**: Each example runs independently with its own data
2. **Realistic Data**: Business-relevant scenarios with meaningful test data
3. **Comprehensive Logging**: Detailed output showing lookup process
4. **Error Handling**: Demonstrate handling of missing keys, null values
5. **Performance Metrics**: Show lookup timing and caching effects
6. **Documentation**: Inline comments explaining SpEL expressions and patterns

#### Shared Infrastructure:

1. **Base Demo Class**: `AbstractLookupDemo.java` with common functionality
2. **Test Data Generators**: Utility classes to create realistic test data
3. **Result Formatters**: Pretty-print enriched results
4. **Performance Monitors**: Measure and report lookup performance

#### Advanced Features to Demonstrate:

1. **Caching**: Show cache hits/misses and performance improvements
2. **Error Scenarios**: Missing keys, malformed data, lookup failures
3. **Conditional Logic**: Complex SpEL expressions with multiple conditions
4. **Data Transformation**: String manipulation, case conversion, formatting
5. **Null Handling**: Graceful handling of null values and missing fields
6. **Performance Optimization**: Efficient lookup strategies for large datasets

#### Integration with Existing Demo Structure:

1. **Main Runner**: Add to `AllDemosRunner.java` with new lookup examples section
2. **Test Coverage**: Unit tests for each lookup pattern
3. **Documentation**: README files explaining each pattern and use case
4. **YAML Validation**: Ensure all examples pass the YAML validation tests

### 🎯 Expected Outcomes

After implementation, users will have:

1. **8 Complete Lookup Examples** covering all advanced patterns
2. **Realistic Business Scenarios** they can adapt to their own use cases
3. **Performance Benchmarks** showing the efficiency of different lookup strategies
4. **Best Practices Guide** through working examples and documentation
5. **Error Handling Patterns** for robust production implementations

Each example will be fully functional, well-documented, and demonstrate real-world usage patterns that developers can immediately apply to their own projects.

---

## Best Practices

### 1. Always Validate Input Data
Ensure your `lookup-key` expressions handle null values gracefully using conditions:
```yaml
condition: "#customerId != null && #region != null"
```

### 2. Use Caching for Performance
Enable caching for frequently accessed lookup data:
```yaml
cache-enabled: true
cache-ttl-seconds: 300
```

### 3. Keep Expressions Simple
Prefer simple, readable expressions over complex ones. If your lookup-key becomes too complex, consider preprocessing the data.

### 4. Document Complex Expressions
Add comments in your YAML to explain complex SpEL expressions:
```yaml
# Compound key format: CUSTOMER_ID-REGION (e.g., "CUST123-US")
lookup-key: "#customerId + '-' + #region"
```

### 5. Test Edge Cases
Always test your lookup configurations with:
- Null values
- Missing fields
- Invalid data types
- Empty strings
- Special characters

### 6. Monitor Performance
For database lookups, monitor query performance and optimize as needed:
- Add appropriate indexes
- Limit result sets
- Use connection pooling
- Consider caching strategies

### 7. Use Appropriate Lookup Types
- **Inline datasets**: Small, static reference data
- **Database lookups**: Large, dynamic data that changes frequently
- **External services**: Real-time data from external systems

### 8. Handle Lookup Failures Gracefully
Design your enrichments to handle cases where lookups fail or return no results without breaking the entire processing pipeline.

