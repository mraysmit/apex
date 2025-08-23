I'll read the lookups.md file in detail to understand the advanced lookup configurations and then present a plan for implementing self-contained examples.

Now I have a comprehensive understanding of the advanced lookup configurations described in the lookups.md file. Let me present a detailed plan for implementing self-contained examples in the apex-demo module for each YAML configuration use case.

# 📋 **Implementation Plan: Advanced Lookup Configuration Examples**

## 🎯 **Overview**

Based on the lookups.md file, I'll create comprehensive, self-contained examples for each advanced lookup pattern. Each example will include:
- Complete YAML configuration files
- Java demo classes with realistic data
- Model classes representing real-world scenarios
- Documentation explaining the use case

## 📊 **Planned Examples Structure**

### **1. Simple Field Lookup Example**
**File**: `apex-demo/src/main/resources/examples/lookups/simple-field-lookup.yaml`
**Demo**: `SimpleFieldLookupDemo.java`
**Use Case**: Basic currency code to currency details lookup

**Components**:
- **Model**: `CurrencyTransaction.java` (amount, currencyCode, description)
- **YAML Config**: Simple `lookup-key: "#currencyCode"` with inline currency dataset
- **Demo Data**: Transactions in USD, EUR, GBP, JPY
- **Enrichment**: Add currency name, symbol, decimal places

### **2. Compound String Concatenation Lookup**
**File**: `apex-demo/src/main/resources/examples/lookups/compound-key-lookup.yaml`
**Demo**: `CompoundKeyLookupDemo.java`
**Use Case**: Customer-region specific pricing lookup

**Components**:
- **Model**: `CustomerOrder.java` (customerId, region, productId, quantity)
- **YAML Config**: `lookup-key: "#customerId + '-' + #region"`
- **Demo Data**: Orders from different customers in different regions
- **Enrichment**: Add customer tier, regional discount, special pricing

### **3. Complex SpEL Expression Lookup**
**File**: `apex-demo/src/main/resources/examples/lookups/complex-spel-lookup.yaml`
**Demo**: `ComplexSpelLookupDemo.java`
**Use Case**: Trading pair lookup with currency normalization

**Components**:
- **Model**: `CurrencyTrade.java` (baseCurrency, quoteCurrency, amount, tradeDate)
- **YAML Config**: `lookup-key: "#baseCurrency.toUpperCase() + '/' + #quoteCurrency.toUpperCase()"`
- **Demo Data**: Trades with mixed case currency codes
- **Enrichment**: Add spread, minimum size, trading hours, market maker

### **4. Conditional Compound Key Lookup**
**File**: `apex-demo/src/main/resources/examples/lookups/conditional-lookup.yaml`
**Demo**: `ConditionalLookupDemo.java`
**Use Case**: Flexible counterparty lookup based on party type

**Components**:
- **Model**: `BusinessTransaction.java` (partyId, partyType, transactionType, amount)
- **YAML Config**: `lookup-key: "#partyType == 'CUSTOMER' ? 'CUST-' + #partyId : (#partyType == 'VENDOR' ? 'VEND-' + #partyId : 'UNKN-' + #partyId)"`
- **Demo Data**: Mixed transactions with customers, vendors, and unknown parties
- **Enrichment**: Add legal name, credit rating, payment terms, risk category

### **5. Hierarchical/Nested Field Lookup**
**File**: `apex-demo/src/main/resources/examples/lookups/hierarchical-lookup.yaml`
**Demo**: `HierarchicalLookupDemo.java`
**Use Case**: Trade settlement instruction lookup

**Components**:
- **Model**: `FinancialTrade.java` with nested `Instrument.java` and `Counterparty.java`
- **YAML Config**: `lookup-key: "#trade.instrument.symbol + ':' + #trade.counterparty.id + ':' + #settlementDate.toString()"`
- **Demo Data**: Complex trade objects with nested structures
- **Enrichment**: Add settlement instructions, custodian details, account numbers

### **6. Multi-Dimensional Product Lookup**
**File**: `apex-demo/src/main/resources/examples/lookups/multi-dimensional-lookup.yaml`
**Demo**: `MultiDimensionalLookupDemo.java`
**Use Case**: Product pricing based on multiple factors

**Components**:
- **Model**: `ProductOrder.java` with nested `Product.java` and `Customer.java`
- **YAML Config**: `lookup-key: "#product.category + '|' + #product.id + '|' + #customer.tier + '|' + #customer.region"`
- **Demo Data**: Orders with various product categories, customer tiers, and regions
- **Enrichment**: Add base price, discount rate, minimum quantity, pricing currency

### **7. Hash-Based Complex Key Lookup**
**File**: `apex-demo/src/main/resources/examples/lookups/hash-based-lookup.yaml`
**Demo**: `HashBasedLookupDemo.java`
**Use Case**: Portfolio risk metrics lookup using computed hash

**Components**:
- **Model**: `PortfolioRiskRequest.java` (portfolioId, strategy, region, asOfDate)
- **YAML Config**: `lookup-key: "T(java.lang.String).valueOf((#portfolioId + #strategy + #region + #asOfDate.toString()).hashCode())"`
- **Demo Data**: Portfolio risk requests with various combinations
- **Enrichment**: Add VaR 95%, expected shortfall, risk metrics

### **8. Database Lookup with Multiple Parameters**
**File**: `apex-demo/src/main/resources/examples/lookups/database-multi-param-lookup.yaml`
**Demo**: `DatabaseMultiParamLookupDemo.java`
**Use Case**: Portfolio position lookup with multiple query parameters

**Components**:
- **Model**: `PositionRequest.java` (portfolioId, instrumentId, asOfDate)
- **YAML Config**: Database lookup with multiple parameters in query
- **Demo Data**: Position requests for different portfolios and instruments
- **Mock Database**: In-memory H2 database with sample position data
- **Enrichment**: Add position quantity, market value, currency

## 🏗️ **Implementation Structure**

### **Directory Organization**:
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

### **Common Features for All Examples**:

1. **Self-Contained**: Each example runs independently with its own data
2. **Realistic Data**: Business-relevant scenarios with meaningful test data
3. **Comprehensive Logging**: Detailed output showing lookup process
4. **Error Handling**: Demonstrate handling of missing keys, null values
5. **Performance Metrics**: Show lookup timing and caching effects
6. **Documentation**: Inline comments explaining SpEL expressions and patterns

### **Shared Infrastructure**:

1. **Base Demo Class**: `AbstractLookupDemo.java` with common functionality
2. **Test Data Generators**: Utility classes to create realistic test data
3. **Result Formatters**: Pretty-print enriched results
4. **Performance Monitors**: Measure and report lookup performance

### **Advanced Features to Demonstrate**:

1. **Caching**: Show cache hits/misses and performance improvements
2. **Error Scenarios**: Missing keys, malformed data, lookup failures
3. **Conditional Logic**: Complex SpEL expressions with multiple conditions
4. **Data Transformation**: String manipulation, case conversion, formatting
5. **Null Handling**: Graceful handling of null values and missing fields
6. **Performance Optimization**: Efficient lookup strategies for large datasets

### **Integration with Existing Demo Structure**:

1. **Main Runner**: Add to `AllDemosRunner.java` with new lookup examples section
2. **Test Coverage**: Unit tests for each lookup pattern
3. **Documentation**: README files explaining each pattern and use case
4. **YAML Validation**: Ensure all examples pass the YAML validation tests

## 🎯 **Expected Outcomes**

After implementation, users will have:

1. **8 Complete Lookup Examples** covering all advanced patterns from lookups.md
2. **Realistic Business Scenarios** they can adapt to their own use cases
3. **Performance Benchmarks** showing the efficiency of different lookup strategies
4. **Best Practices Guide** through working examples and documentation
5. **Error Handling Patterns** for robust production implementations

Each example will be fully functional, well-documented, and demonstrate real-world usage patterns that developers can immediately apply to their own projects.

Would you like me to proceed with implementing these examples, starting with a specific pattern, or would you like me to modify any aspect of this plan?
