# APEX Lookup Configuration Guide

**Version:** 2.0
**Date:** 2025-11-09
**Author:** Mark Andrew Ray-Smith Cityline Ltd

This guide provides comprehensive documentation on APEX lookup configurations, including advanced patterns, implementation examples, and best practices.

## Table of Contents

1. [What is "lookup-key"?](#what-is-lookup-key)
2. [Basic Concepts](#basic-concepts)
3. [Dataset Types](#dataset-types)
4. [Advanced Lookup Patterns](#advanced-lookup-patterns)
5. [External Data Sources](#external-data-sources)
6. [Handling Lookup Failures](#handling-lookup-failures)
7. [Caching](#caching)
8. [Best Practices](#best-practices)

---

## What is "lookup-key"?

**"lookup-key"** is a **SpEL (Spring Expression Language) expression** used in enrichments to specify **how to extract the key value from your data** that will be used to perform the lookup operation.

### Key Points:

1. **It's a SpEL Expression**: The lookup-key contains a SpEL expression that gets evaluated against your data context
2. **Extracts the Lookup Value**: It tells APEX which field or value from your input data should be used as the key to look up reference data
3. **Used in Lookup Enrichments**: It's specifically used in `lookup-enrichment` type enrichments
4. **Dynamic Evaluation**: The expression is evaluated at runtime for each data record being processed

### YAML Configuration Structure:

```yaml
enrichments:
  - id: "my-lookup"
    type: "lookup-enrichment"
    condition: "#customerId != null"
    lookup-config:
      lookup-key: "#customerId"  # ← SpEL expression to extract lookup key
      lookup-dataset:
        type: "inline"  # or "database", "file-system", "rest-api"
        key-field: "id"
        data:
          - id: "CUST001"
            name: "John Doe"
    field-mappings:
      - source-field: "name"
        target-field: "customerName"
```

### How it works:

1. **Evaluation**: The `lookup-key` SpEL expression is evaluated against your input data
2. **Extraction**: The result becomes the key to search for in the lookup dataset
3. **Matching**: APEX finds the matching record in the dataset
4. **Enrichment**: Field mappings copy data from the matched record to your output

---

## Basic Concepts

### Simple Field Reference

The most common pattern - extract a single field value from your input data:

```yaml
enrichments:
  - id: "currency-lookup"
    type: "lookup-enrichment"
    condition: "#currency != null"
    lookup-config:
      lookup-key: "#currency"  # Extract the currency field value
      lookup-dataset:
        type: "inline"
        key-field: "code"
        data:
          - code: "USD"
            name: "US Dollar"
            symbol: "$"
    field-mappings:
      - source-field: "name"
        target-field: "currencyName"
      - source-field: "symbol"
        target-field: "currencySymbol"
```

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

## Dataset Types

APEX supports multiple dataset types for lookup enrichments. Each type has specific configuration requirements.

### 1. Inline Datasets

**Use Case:** Small, static reference data embedded directly in YAML

```yaml
enrichments:
  - id: "currency-lookup"                    # Unique identifier for this enrichment
    type: "lookup-enrichment"                # Type: lookup-enrichment for lookup operations
    lookup-config:                           # Configuration for the lookup operation
      lookup-key: "#currency"                # SpEL expression: extract 'currency' field from input data
      lookup-dataset:                        # Dataset configuration
        type: "inline"                       # Dataset type: inline (embedded data)
        key-field: "code"                    # Field in dataset to match against lookup-key value
        data:                                # Inline dataset array
          - code: "USD"                      # First record: code field (matches key-field)
            name: "US Dollar"                # Additional field: currency name
            symbol: "$"                      # Additional field: currency symbol
          - code: "EUR"                      # Second record: code field
            name: "Euro"                     # Additional field: currency name
            symbol: "€"                      # Additional field: currency symbol
    field-mappings:                          # Map lookup results to output fields
      - source-field: "name"                 # Source: 'name' field from matched record
        target-field: "currencyName"         # Target: copy to 'currencyName' in output
```

### 2. Database Datasets

**Use Case:** Large, dynamic data from database queries

```yaml
enrichments:
  - id: "customer-lookup"                              # Unique identifier for this enrichment
    type: "lookup-enrichment"                          # Type: lookup-enrichment for lookup operations
    condition: "#customerId != null"                   # Condition: only execute if customerId is not null
    lookup-config:                                     # Configuration for the lookup operation
      lookup-key: "#customerId"                        # SpEL expression: extract 'customerId' field from input data
      lookup-dataset:                                  # Dataset configuration
        type: "database"                               # Dataset type: database (SQL query)
        data-source-ref: "customer-database"           # Reference to data source defined in data-sources section
        query: "SELECT customer_name, tier, region 
          FROM customers WHERE customer_id = :customerId" # SQL query with named parameter :customerId
        parameters:                                    # Query parameters array
          - field: "customerId"                        # Parameter name: matches :customerId in query
            type: "string"                             # Parameter type: string
        cache-enabled: true                            # Enable caching for this dataset
        cache-ttl-seconds: 300                         # Cache TTL: 5 minutes (300 seconds)
    field-mappings:                                    # Map query results to output fields
      - source-field: "CUSTOMER_NAME"                  # Source: CUSTOMER_NAME column from query result
        target-field: "customerName"                   # Target: copy to 'customerName' in output
      - source-field: "TIER"                           # Source: TIER column from query result
        target-field: "customerTier"                   # Target: copy to 'customerTier' in output
```

### 3. File-System Datasets

**Use Case:** Reference data stored in JSON or XML files

```yaml
enrichments:
  - id: "product-lookup"                            # Unique identifier for this enrichment
    type: "lookup-enrichment"                       # Type: lookup-enrichment for lookup operations
    condition: "#productId != null"                 # Condition: only execute if productId is not null
    lookup-config:                                  # Configuration for the lookup operation
      lookup-key: "#productId"                      # SpEL expression: extract 'productId' field from input data
      lookup-dataset:                               # Dataset configuration
        type: "file-system"                         # Dataset type: file-system (JSON/XML file)
        key-field: "id"                             # Field in file data to match against lookup-key value
        file-path: "demo-data/json/products.json"   # Path to JSON file (relative to project root)
    field-mappings:                                 # Map file data to output fields
      - source-field: "name"                        # Source: 'name' field from matched record in file
        target-field: "productName"                 # Target: copy to 'productName' in output
      - source-field: "price"                       # Source: 'price' field from matched record in file
        target-field: "productPrice"                # Target: copy to 'productPrice' in output
```

### 4. REST API Datasets

**Use Case:** Real-time data from external REST APIs

```yaml
enrichments:
  - id: "currency-rate-lookup"                    # Unique identifier for this enrichment
    type: "lookup-enrichment"                     # Type: lookup-enrichment for lookup operations
    condition: "#currencyCode != null"            # Condition: only execute if currencyCode is not null
    lookup-config:                                # Configuration for the lookup operation
      lookup-key: "#currencyCode"                 # SpEL expression: extract 'currencyCode' field from input data
      lookup-dataset:                             # Dataset configuration
        type: "rest-api"                          # Dataset type: rest-api (external REST API call)
        data-source-ref: "currency-api-server"    # Reference to REST API data source (defined below)
        operation-ref: "currency-lookup"          # Reference to specific endpoint operation
    field-mappings:                               # Map API response to output fields
      - source-field: "name"                      # Source: 'name' field from API response JSON
        target-field: "currencyName"              # Target: copy to 'currencyName' in output
      - source-field: "rate"                      # Source: 'rate' field from API response JSON
        target-field: "exchangeRate"              # Target: copy to 'exchangeRate' in output
```

**REST API Data Source Configuration:**

```yaml
data-sources:
  - name: "currency-api-server"              # Data source name (referenced by data-source-ref above)
    type: "rest-api"                         # Data source type: rest-api
    enabled: true                            # Enable this data source
    connection:                              # Connection configuration
      base-url: "http://api.example.com"     # Base URL for all API calls
      timeout: 5000                          # Request timeout in milliseconds (5 seconds)
      max-retries: 3                         # Maximum number of retry attempts on failure
    endpoints:                               # Named endpoint definitions
      currency-lookup: "/api/currency/{key}" # Endpoint path with {key} placeholder for lookup-key value
    cache:                                   # Cache configuration for this data source
      enabled: true                          # Enable caching for API responses
      ttl-seconds: 300                       # Cache TTL: 5 minutes (300 seconds)
```

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

## External Data Sources

APEX supports separating data source configuration from enrichment logic using the `data-source-ref` pattern. This promotes reusability and cleaner configuration.

### Data Source Configuration

Define reusable data sources in the `data-sources` section:

```yaml
data-sources:
  - name: "customer-database"
    type: "database"
    source-type: "h2"
    connection:
      database: "./target/h2-demo/apex_demo"
      username: "sa"
      password: ""

  - name: "currency-api"
    type: "rest-api"
    enabled: true
    connection:
      base-url: "http://api.example.com"
      timeout: 5000
      max-retries: 3
    endpoints:
      currency-lookup: "/api/currency/{key}"
    cache:
      enabled: true
      ttl-seconds: 300
```

### Using data-source-ref

Reference external data sources in your enrichments:

```yaml
enrichments:
  - id: "customer-profile-lookup"
    type: "lookup-enrichment"
    condition: "#customerId != null"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "database"
        data-source-ref: "customer-database"  # ← References data source by name
        query: "SELECT customer_name, tier, region FROM customers WHERE customer_id = :customerId"
        parameters:
          - field: "customerId"
            type: "string"
    field-mappings:
      - source-field: "CUSTOMER_NAME"
        target-field: "customerName"
      - source-field: "TIER"
        target-field: "customerTier"
```

### Using query-ref

For even cleaner configuration, define named queries:

```yaml
data-sources:
  - name: "customer-database"
    type: "database"
    source-type: "h2"
    connection:
      database: "./target/h2-demo/apex_demo"
      username: "sa"
      password: ""
    queries:
      getCustomerProfile: "SELECT customer_name, tier, region FROM customers WHERE customer_id = :customerId"
      getCustomerOrders: "SELECT order_id, amount, status FROM orders WHERE customer_id = :customerId"

enrichments:
  - id: "customer-profile-lookup"
    type: "lookup-enrichment"
    condition: "#customerId != null"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "database"
        data-source-ref: "customer-database"
        query-ref: "getCustomerProfile"  # ← References named query
        parameters:
          - field: "customerId"
            type: "string"
    field-mappings:
      - source-field: "CUSTOMER_NAME"
        target-field: "customerName"
```

### Benefits of External Data Sources

1. **Reusability**: Define once, use in multiple enrichments
2. **Maintainability**: Update connection details in one place
3. **Separation of Concerns**: Keep data source configuration separate from business logic
4. **Named Queries**: Centralize SQL queries for easier management
5. **Environment Configuration**: Easier to override for different environments

---

## Handling Lookup Failures

APEX provides several patterns for handling lookup failures and checking for non-existence in lookups.

### Using result-field to Track Lookup Success

The `result-field` property stores a boolean indicating whether the lookup succeeded:

```yaml
enrichments:
  - id: "counterparty-lookup"                        # First enrichment: attempt lookup
    type: "lookup-enrichment"                        # Type: lookup-enrichment
    result-field: "counterpartyFound"                # Store lookup success/failure in this field
    condition: "#counterparty != null"               # Only attempt if counterparty field exists
    lookup-config:                                   # Lookup configuration
      lookup-key: "#counterparty"                    # Extract counterparty ID from input
      lookup-dataset:                                # Dataset to search
        type: "inline"                               # Inline dataset type
        key-field: "counterpartyId"                  # Field to match against
        data:                                        # Dataset records
          - counterpartyId: "BANK_A"                 # Record 1
            rating: "AAA"                            # Rating field
          - counterpartyId: "BANK_B"                 # Record 2
            rating: "AA"                             # Rating field
    field-mappings:                                  # Map matched data to output
      - source-field: "rating"                       # Source field from matched record
        target-field: "counterpartyRating"           # Target field in output

  - id: "set-default-rating"                         # Second enrichment: handle failure
    type: "field-enrichment"                         # Type: field-enrichment for setting values
    condition: "#counterpartyFound == false"         # Only execute if lookup FAILED
    field-mappings:                                  # Set default values
      - source-field: "counterpartyRating"           # Target the same field
        target-field: "counterpartyRating"           # Set default value
        expression: "'UNRATED'"                      # Default value when lookup fails
```

**How it works:**
1. First enrichment attempts lookup and stores result in `counterpartyFound` (true/false)
2. Second enrichment only runs if `counterpartyFound == false`
3. Sets a default value when lookup fails

### Checking for Non-Existence (Negative Lookup)

Use `result-field` to verify a value is NOT in a lookup dataset:

```yaml
enrichments:
  - id: "check-blacklist"                            # Check if customer is blacklisted
    type: "lookup-enrichment"                        # Type: lookup-enrichment
    result-field: "isBlacklisted"                    # Store lookup result (true if found)
    condition: "#customerId != null"                 # Only check if customerId exists
    lookup-config:                                   # Lookup configuration
      lookup-key: "#customerId"                      # Extract customer ID
      lookup-dataset:                                # Blacklist dataset
        type: "database"                             # Database lookup
        data-source-ref: "compliance-database"       # Reference to compliance DB
        query: "SELECT customer_id FROM blacklist WHERE customer_id = :customerId"  # Query blacklist
        parameters:                                  # Query parameters
          - field: "customerId"                      # Parameter: customerId
            type: "string"                           # Type: string
    field-mappings: []                               # No field mappings needed (just checking existence)

  - id: "reject-blacklisted"                         # Reject if found in blacklist
    type: "field-enrichment"                         # Type: field-enrichment
    condition: "#isBlacklisted == true"              # Only execute if customer IS blacklisted
    field-mappings:                                  # Set rejection fields
      - source-field: "status"                       # Set status field
        target-field: "status"                       # Target: status
        expression: "'REJECTED'"                     # Value: REJECTED
      - source-field: "rejectionReason"              # Set rejection reason
        target-field: "rejectionReason"              # Target: rejectionReason
        expression: "'Customer is blacklisted'"      # Reason message

  - id: "approve-clean-customer"                     # Approve if NOT in blacklist
    type: "field-enrichment"                         # Type: field-enrichment
    condition: "#isBlacklisted == false"             # Only execute if customer is NOT blacklisted
    field-mappings:                                  # Set approval fields
      - source-field: "status"                       # Set status field
        target-field: "status"                       # Target: status
        expression: "'APPROVED'"                     # Value: APPROVED
```

**How it works:**
1. First enrichment checks if customer exists in blacklist
2. `isBlacklisted` is `true` if found, `false` if not found
3. Subsequent enrichments branch based on the result

### Validation Before Lookup

Prevent lookup failures by validating input data first:

```yaml
enrichments:
  - id: "validate-required-fields"                   # Validation enrichment
    type: "calculation-enrichment"                   # Type: calculation-enrichment
    condition: "true"                                # Always run validation
    calculation-config:                              # Calculation configuration
      expression: "(#customerId != null && #currencyCode != null) ? 'VALID' : 'MISSING_REQUIRED_FIELDS'"  # Check required fields
      result-field: "validationResult"               # Store validation result
    field-mappings:                                  # Map result to output
      - source-field: "validationResult"             # Source: validation result
        target-field: "validationStatus"             # Target: validation status

  - id: "customer-lookup"                            # Lookup enrichment
    type: "lookup-enrichment"                        # Type: lookup-enrichment
    condition: "#validationStatus == 'VALID'"        # Only lookup if validation passed
    lookup-config:                                   # Lookup configuration
      lookup-key: "#customerId"                      # Extract customer ID
      lookup-dataset:                                # Dataset configuration
        type: "database"                             # Database lookup
        data-source-ref: "customer-database"         # Reference to customer DB
        query: "SELECT * FROM customers WHERE customer_id = :customerId"  # Query
        parameters:                                  # Parameters
          - field: "customerId"                      # Parameter: customerId
    field-mappings:                                  # Field mappings
      - source-field: "CUSTOMER_NAME"                # Map customer name
        target-field: "customerName"                 # Target field
```

### Multiple Fallback Levels

Chain multiple enrichments for sophisticated fallback logic:

```yaml
enrichments:
  - id: "primary-lookup"                             # Try primary data source
    type: "lookup-enrichment"                        # Type: lookup-enrichment
    result-field: "primaryFound"                     # Track primary lookup result
    condition: "#productId != null"                  # Only if productId exists
    lookup-config:                                   # Primary lookup config
      lookup-key: "#productId"                       # Extract product ID
      lookup-dataset:                                # Primary dataset
        type: "database"                             # Database type
        data-source-ref: "primary-product-db"        # Primary database
        query: "SELECT * FROM products WHERE product_id = :productId"  # Query
        parameters:                                  # Parameters
          - field: "productId"                       # Parameter: productId
    field-mappings:                                  # Field mappings
      - source-field: "PRODUCT_NAME"                 # Map product name
        target-field: "productName"                  # Target field

  - id: "secondary-lookup"                           # Try secondary data source if primary fails
    type: "lookup-enrichment"                        # Type: lookup-enrichment
    result-field: "secondaryFound"                   # Track secondary lookup result
    condition: "#primaryFound == false && #productId != null"  # Only if primary failed
    lookup-config:                                   # Secondary lookup config
      lookup-key: "#productId"                       # Extract product ID
      lookup-dataset:                                # Secondary dataset
        type: "rest-api"                             # REST API type
        data-source-ref: "backup-product-api"        # Backup API
        operation-ref: "product-lookup"              # API operation
    field-mappings:                                  # Field mappings
      - source-field: "name"                         # Map product name from API
        target-field: "productName"                  # Target field

  - id: "default-fallback"                           # Set default if both fail
    type: "field-enrichment"                         # Type: field-enrichment
    condition: "#primaryFound == false && #secondaryFound == false"  # Only if both failed
    field-mappings:                                  # Set defaults
      - source-field: "productName"                  # Set product name
        target-field: "productName"                  # Target field
        expression: "'UNKNOWN_PRODUCT'"              # Default value
      - source-field: "dataQuality"                  # Set data quality flag
        target-field: "dataQuality"                  # Target field
        expression: "'INCOMPLETE'"                   # Quality indicator
```

### Error Handling Configuration

Configure error handling behavior at the data source level:

```yaml
data-sources:
  - name: "external-api"                             # Data source name
    type: "rest-api"                                 # Type: REST API
    connection:                                      # Connection config
      base-url: "http://api.example.com"             # API base URL
      timeout: 5000                                  # Timeout: 5 seconds
      max-retries: 3                                 # Retry up to 3 times
      retry-delay-ms: 1000                           # Wait 1 second between retries
    error-handling:                                  # Error handling configuration
      on-error: "continue"                           # Continue processing on error (don't fail)
      fallback-value: null                           # Return null on failure
      log-errors: true                               # Log errors for monitoring
```

---

## Caching

APEX provides sophisticated two-level caching for lookup operations to optimize performance.

### Cache Configuration

Enable caching at the dataset level:

```yaml
enrichments:
  - id: "customer-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "database"
        data-source-ref: "customer-database"
        query: "SELECT * FROM customers WHERE customer_id = :customerId"
        parameters:
          - field: "customerId"
        cache-enabled: true        # ← Enable caching
        cache-ttl-seconds: 300     # ← Cache for 5 minutes
```

### Two-Level Caching Architecture

APEX uses two cache levels for optimal performance:

#### 1. Dataset Cache
- **Purpose**: Caches DatasetLookupService instances
- **TTL**: 2 hours (default)
- **Size**: 1000 entries (default)
- **Benefit**: Prevents duplicate dataset loading for identical configurations

#### 2. Lookup Result Cache
- **Purpose**: Caches individual lookup results
- **TTL**: 5 minutes (default)
- **Size**: 10,000 entries (default)
- **Benefit**: Avoids repeated lookups for the same key

### Content-Based Deduplication

APEX automatically deduplicates datasets using content-based signatures:

- **Inline datasets**: MD5 hash of data content + key field
- **File-based datasets**: File path + key field
- **Database datasets**: Connection + query + parameters + key field
- **REST API datasets**: Connection + endpoint + operation + key field

**Example**: If two enrichments use identical inline datasets, they share one cached DatasetLookupService instance.

### Cache Configuration in Data Sources

Configure caching at the data source level:

```yaml
data-sources:
  - name: "currency-api"
    type: "rest-api"
    connection:
      base-url: "http://api.example.com"
    cache:
      enabled: true
      ttl-seconds: 300
      max-size: 100
```

### Performance Benefits

| Scenario | Without Cache | With Cache | Improvement |
|----------|--------------|------------|-------------|
| Database lookup (1st call) | 50ms | 50ms | - |
| Database lookup (2nd call) | 50ms | <1ms | 50x faster |
| REST API lookup (1st call) | 200ms | 200ms | - |
| REST API lookup (2nd call) | 200ms | <1ms | 200x faster |

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

