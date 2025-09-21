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
