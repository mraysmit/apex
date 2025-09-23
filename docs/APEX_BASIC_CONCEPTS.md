![APEX System Logo](APEX%20System%20logo.png)

**Version:** 1.0
**Date:** 2025-09-06
**Author:** MArk A Ray-Smith Cityline Ltd.

## APEX Core Processing Principles

**Primary Function:**
- APEX processes incoming datasets against YAML configuration files

**Two Main Processing Types:**

**1. Validation Processing:**
- Validates source dataset fields against reference data
- Uses REST API endpoints as validation data sources
- Performs simple validation checks (e.g., format, existence, business rules)

**2. Data Enrichment Processing:**
- Enriches incoming dataset using lookup keys
- Queries REST API endpoints for reference data
- Adds missing or supplementary data to the source dataset

**Test Requirements:**
- Create simple incoming datasets for testing
- Datasets don't need to be complex
- Use available TestableRestApiServer endpoints (currency, customer)

## Rule Severity Levels

Every rule in APEX has a severity level that indicates how critical a rule failure is. Understanding severity is fundamental to building effective business rules and handling validation results appropriately.

### **Severity Levels**

**ERROR** - Critical validation failure
- Must be addressed before processing can continue
- Typically blocks business transactions
- Used for mandatory field validation, format violations, business rule violations
- Example: Missing required customer ID, invalid currency code

**WARNING** - Important issue that should be reviewed
- Processing can continue but attention is needed
- Used for business policy violations, unusual conditions
- May require manual review or approval
- Example: Transaction outside business hours, high-value transaction

**INFO** - Informational message
- No action required, processing continues normally
- Used for logging, audit trails, informational notifications
- Provides context about processing decisions
- Example: Successful validation, data enrichment completed

### **Severity in YAML Configuration**

```yaml
rules:
  - id: "customer-id-required"
    name: "Customer ID Validation"
    condition: "#customerId != null && #customerId.trim().length() > 0"
    message: "Customer ID is required and cannot be empty"
    severity: "ERROR"  # Critical - blocks processing

  - id: "high-value-transaction"
    name: "High Value Transaction Check"
    condition: "#amount <= 10000"
    message: "Transaction amount {{#amount}} exceeds normal limits"
    severity: "WARNING"  # Important - requires review

  - id: "processing-complete"
    name: "Processing Status"
    condition: "true"
    message: "Transaction processing completed successfully"
    severity: "INFO"  # Informational - audit trail
```

### **Severity in Business Logic**

Use severity levels to implement appropriate business responses:

```java
// Process rule results based on severity
for (RuleResult result : results) {
    switch (result.getSeverity()) {
        case "ERROR":
            // Stop processing, return error to user
            throw new ValidationException(result.getMessage());

        case "WARNING":
            // Log warning, continue processing
            logger.warn("Business rule warning: {}", result.getMessage());
            warningMessages.add(result.getMessage());
            break;

        case "INFO":
            // Log information, continue processing
            logger.info("Processing info: {}", result.getMessage());
            break;
    }
}
```

### **Default Severity Behavior**

- If no severity is specified in YAML, rules default to **"INFO"**
- This ensures backward compatibility with existing configurations
- Best practice: Always explicitly specify severity for clarity

## Two Example Tests

**Test 1: Currency Code Validation**
````java path=apex-demo/src/test/java/dev/mars/apex/demo/lookup/CurrencyValidationExampleTest.java mode=EDIT
@Test
void testCurrencyCodeValidation() throws Exception {
    // Incoming dataset with currency code to validate
    Map<String, Object> incomingData = Map.of(
        "transactionId", "TXN-001",
        "currencyCode", "EUR",
        "amount", 1000.00
    );
    
    // YAML config validates currency exists via REST API
    String yamlConfig = updateYamlWithServerPort("currency-validation-test.yaml");
    var config = yamlLoader.loadFromFile(yamlConfig);
    
    // Process validation
    Object result = enrichmentService.enrichObject(config, incomingData);
    Map<String, Object> validatedData = (Map<String, Object>) result;
    
    // Assert validation passed (no errors)
    assertNull(validatedData.get("validationErrors"));
}
````

**Test 2: Customer Name Enrichment**
````java path=apex-demo/src/test/java/dev/mars/apex/demo/lookup/CustomerEnrichmentExampleTest.java mode=EDIT
@Test
void testCustomerNameEnrichment() throws Exception {
    // Incoming dataset with blank customer name
    Map<String, Object> incomingData = Map.of(
        "orderId", "ORD-001",
        "customerId", "CUST1",
        "customerName", "",  // Blank - needs enrichment
        "orderAmount", 2500.00
    );
    
    // YAML config enriches customer name using customerId lookup
    String yamlConfig = updateYamlWithServerPort("customer-enrichment-test.yaml");
    var config = yamlLoader.loadFromFile(yamlConfig);
    
    // Process enrichment
    Object result = enrichmentService.enrichObject(config, incomingData);
    Map<String, Object> enrichedData = (Map<String, Object>) result;
    
    // Assert customer name was enriched from REST API
    assertEquals("Acme Corporation", enrichedData.get("customerName"));
}
````

These examples demonstrate your two core APEX patterns: validation against REST API data sources and enrichment using REST API lookups with simple incoming datasets.
