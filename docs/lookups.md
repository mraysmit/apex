Looking at the source code, "lookup-key" is a configuration parameter used in enrichments to specify how to extract the key value from your data that will be used to perform the lookup operation.

Let me also check the bootstrap examples to see more usage of lookup-key:

Now let me check the YamlEnrichmentProcessor to see exactly how lookup-key is processed:

Let me also check the YamlEnrichment configuration class to see the lookup-key property:

Perfect! Now I have a complete understanding of "lookup-key". Let me explain what it is and where it's used:

## What is "lookup-key"?

**"lookup-key"** is a **SpEL (Spring Expression Language) expression** used in enrichments to specify **how to extract the key value from your data** that will be used to perform the lookup operation.

### Key Points:

1. **It's a SpEL Expression**: The lookup-key contains a SpEL expression that gets evaluated against your data context
2. **Extracts the Lookup Value**: It tells APEX which field or value from your input data should be used as the key to look up reference data
3. **Used in Lookup Enrichments**: It's specifically used in `lookup-enrichment` type enrichments
4. **Dynamic Evaluation**: The expression is evaluated at runtime for each data record being processed

### Where is it used?

````java path=apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichment.java mode=EXCERPT
public static class LookupConfig {
    @JsonProperty("lookup-service")
    private String lookupService;

    @JsonProperty("lookup-dataset")
    private LookupDataset lookupDataset;

    @JsonProperty("lookup-key")
    private String lookupKey; // SpEL expression to extract lookup key
````

### How it works:

````java path=apex-core/src/main/java/dev/mars/apex/core/service/enrichment/YamlEnrichmentProcessor.java mode=EXCERPT
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

### Examples from the codebase:

**1. Simple field reference:**
````yaml path=apex-demo/src/main/resources/bootstrap/otc-options-bootstrap.yaml mode=EXCERPT
lookup-config:
  lookup-service: "postgresLookupService"
  lookup-key: "#sellerParty"  # Extract the sellerParty field value
  cache-enabled: true
````

**2. Alternative approach using key-field (for inline datasets):**
````yaml path=apex-demo/src/main/resources/demo-rules/dataset-enrichment.yaml mode=EXCERPT
lookup-config:
  lookup-dataset:
    type: "inline"
    key-field: "code"  # This tells APEX which field in the dataset to match against
    data:
      - code: "USD"
        name: "US Dollar"
````

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

So in summary, **lookup-key** is the SpEL expression that tells APEX "extract this value from the input data and use it to look up reference information in the dataset or external service."

Great question! Looking at the `dataset-enrichment.yaml` file you have open, I can see the relationship between `lookup-key` and `key-field`. They work together but serve different purposes:

## The Relationship Between `lookup-key` and `key-field`

### `key-field` - What field in the dataset to match against

In your open file, you can see `key-field` being used in the inline datasets:

````yaml path=apex-demo/src/main/resources/demo-rules/dataset-enrichment.yaml mode=EXCERPT
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

### `lookup-key` - What value from input data to use for lookup

However, I notice your `dataset-enrichment.yaml` file is **missing** the `lookup-key` expressions! This is actually a gap in the configuration. Here's what should be added:

```yaml
enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    lookup-config:
      lookup-key: "#currency"  # ← THIS IS MISSING! Extract currency field from input data
      lookup-dataset:
        type: "inline"
        key-field: "code"       # ← Match against 'code' field in dataset
        data:
          - code: "USD"         # ← This 'code' value gets matched
            name: "US Dollar"
```

## How They Work Together

1. **`lookup-key`** extracts a value from your **input data**
2. **`key-field`** specifies which field in the **reference dataset** to match against
3. APEX compares these two values to find the matching record

### Example Flow:

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

## Different Scenarios

### 1. Simple Field Matching (most common)
```yaml
# Input: { "currency": "USD" }
lookup-key: "#currency"      # Gets "USD"
key-field: "code"           # Matches against dataset's "code" field
```

### 2. Nested Field Extraction
```yaml
# Input: { "transaction": { "currency": "USD" } }
lookup-key: "#transaction.currency"  # Gets "USD"
key-field: "code"                   # Matches against dataset's "code" field
```

### 3. Complex Key Extraction
```yaml
# Input: { "account": "USD-123456" }
lookup-key: "#account.substring(0,3)"  # Gets "USD"
key-field: "code"                      # Matches against dataset's "code" field
```

### 4. Database Lookups (different approach)
```yaml
# For database sources, you might see:
lookup-key: "#customerId"
database-config:
  query: "SELECT * FROM customers WHERE customer_id = ?"
  # The lookup-key value gets passed as the ? parameter
```

## Fixing Your Configuration

Your `dataset-enrichment.yaml` file should include `lookup-key` expressions. Here's how to fix the currency enrichment:

```yaml
enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    lookup-config:
      lookup-key: "#currency"  # ← ADD THIS
      lookup-dataset:
        type: "inline"
        key-field: "code"
        data:
          # ... your existing data
```

## Summary

- **`lookup-key`**: SpEL expression that extracts the lookup value **FROM your input data**
- **`key-field`**: Specifies which field **IN the reference dataset** to match against
- They work as a pair: `lookup-key` value gets compared to `key-field` values to find matches

The missing `lookup-key` expressions in your file explain why the enrichments might not be working as expected!

Yes, compound lookup keys are definitely possible! Looking at the source code and examples, APEX supports several approaches for creating compound lookup keys. Let me show you the different methods:

## Method 1: String Concatenation in lookup-key

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

## Method 2: Complex SpEL Expressions

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

## Method 3: Database Compound Keys with Multiple Parameters

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

## Method 4: Conditional Compound Keys

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

## Method 5: Hierarchical Compound Keys

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

## Method 6: Hash-Based Compound Keys

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

## Real-World Example: Multi-Dimensional Product Lookup

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

## Key Points for Compound Lookup Keys:

1. **SpEL Flexibility**: You can use any valid SpEL expression in `lookup-key`
2. **String Operations**: Concatenation, substring, case conversion all work
3. **Conditional Logic**: Ternary operators and complex conditions are supported
4. **Database Parameters**: Multiple parameters map to multiple `?` placeholders in order
5. **Caching**: Compound keys are cached just like simple keys
6. **Performance**: Consider the complexity of your expressions and caching strategy

The key is that `lookup-key` can be any SpEL expression that evaluates to a value that can be matched against your reference data, whether that's a simple field, a compound string, or even a computed hash.
