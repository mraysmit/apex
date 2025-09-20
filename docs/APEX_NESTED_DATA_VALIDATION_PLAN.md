# 🎯 APEX Nested Data Structure Validation - Implementation Guide

## Overview

This implementation validates that **APEX can handle complex nested XML data structures** through its built-in engines. All processing logic is defined in APEX YAML configurations - no custom Java processing code.

## Purpose & Scope

**Primary Goal**: Prove APEX functionality with nested data structures
**Validation Focus**: APEX enrichment, validation, and lookup engines
**Data Model**: Barrier Options with 3+ levels of XML nesting
**Success Criteria**: 100% processing through APEX YAML configurations

## Implementation Architecture

### Data Flow
```
XML Source Data → APEX YAML Config → APEX Engine Processing → Validation Results
```

### APEX Engines Being Validated
1. **APEX Enrichment Engine** - Nested field processing and calculations
2. **APEX Validation Engine** - Nested business rule validation  
3. **APEX Lookup Engine** - Nested data extraction and mapping

## Detailed Implementation Plan

### Phase 1: XML Data Model Creation

#### File: `barrier-option-simple.xml`
**Location**: `apex-demo/src/test/resources/infrastructure/bootstrap/datasets/`
**Purpose**: Source data for APEX nested processing validation

```xml
<?xml version="1.0" encoding="UTF-8"?>
<BarrierOption>
    <!-- Level 0: Root elements -->
    <tradeDate>2025-09-19</tradeDate>
    <buyerParty>GOLDMAN_SACHS</buyerParty>
    <sellerParty>JP_MORGAN</sellerParty>
    <optionType>Call</optionType>
    
    <!-- Level 1: Nested underlying asset -->
    <underlyingAsset>
        <commodity>Gold</commodity>
        <unit>Troy Ounce</unit>
        <exchange>COMEX</exchange>
        <!-- Level 2: Market data nesting -->
        <marketData>
            <currentPrice currency="USD">2100.00</currentPrice>
            <volatility>0.18</volatility>
        </marketData>
    </underlyingAsset>
    
    <!-- Level 1: Nested pricing terms -->
    <pricingTerms>
        <strikePrice currency="USD">2150.00</strikePrice>
        <notionalQuantity>100</notionalQuantity>
        <premium currency="USD">15000.00</premium>
    </pricingTerms>
    
    <!-- Level 1: Nested barrier terms -->
    <barrierTerms>
        <barrierType>Knock-Out</barrierType>
        <barrierLevel currency="USD">2300.00</barrierLevel>
        <barrierDirection>Up-and-Out</barrierDirection>
        <monitoringFrequency>Continuous</monitoringFrequency>
        <!-- Level 2: Knockout conditions -->
        <knockoutConditions>
            <triggerEvent>Price Touch</triggerEvent>
            <!-- Level 3: Observation period -->
            <observationPeriod>
                <startDate>2025-09-19</startDate>
                <endDate>2025-12-19</endDate>
            </observationPeriod>
            <!-- Level 3: Rebate terms -->
            <rebateTerms>
                <rebateAmount currency="USD">5000.00</rebateAmount>
                <rebatePaymentDate>2025-12-21</rebatePaymentDate>
            </rebateTerms>
        </knockoutConditions>
    </barrierTerms>
    
    <!-- Level 0: Simple settlement -->
    <expiryDate>2025-12-19</expiryDate>
    <settlementType>Cash</settlementType>
</BarrierOption>
```

**Nested Structure Testing Levels**:
- **Level 1**: `underlyingAsset.commodity`
- **Level 2**: `underlyingAsset.marketData.currentPrice`
- **Level 3**: `barrierTerms.knockoutConditions.observationPeriod.startDate`
- **Level 3**: `barrierTerms.knockoutConditions.rebateTerms.rebateAmount`

### Phase 2: APEX Enrichment Configuration

#### File: `barrier-option-nested-enrichment.yaml`
**Location**: `apex-demo/src/test/resources/lookup/`
**Purpose**: Validate APEX enrichment engine nested processing capabilities

```yaml
# APEX Nested Data Processing Validation Configuration
# Validates APEX enrichment engine can handle complex nested XML structures

metadata:
  id: "barrier-option-nested-enrichment"
  name: "APEX Nested Data Processing Validation"
  version: "1.0.0"
  description: "Validates APEX enrichment engine nested data processing capabilities"
  business-domain: "Financial Derivatives - Barrier Options"
  author: "apex.validation.team@company.com"
  created-date: "2025-09-19"
  tags: ["apex-validation", "nested-data", "barrier-options", "enrichment"]

# APEX Enrichment Rules - All processing logic defined here
enrichments:
  # VALIDATES: APEX can navigate 2-level nested structures
  - id: "apex-level2-navigation"
    name: "APEX Level 2 Nested Field Navigation"
    description: "Validates APEX can extract underlyingAsset.marketData.currentPrice"
    condition: "#underlyingAsset?.marketData?.currentPrice != null"
    result-field: "apexExtractedMarketPrice"
    expression: "#underlyingAsset.marketData.currentPrice"
    priority: 1
    categories: ["nested-navigation", "level-2"]
    
  # VALIDATES: APEX can perform cross-nested-field business calculations
  - id: "apex-cross-nested-calculation"
    name: "APEX Cross-Nested Business Calculation"
    description: "Validates APEX can calculate barrier spread across nested structures"
    condition: "#barrierTerms?.barrierLevel != null && #pricingTerms?.strikePrice != null"
    result-field: "apexBarrierSpread"
    expression: "#barrierTerms.barrierLevel - #pricingTerms.strikePrice"
    priority: 2
    categories: ["business-calculation", "cross-nested"]
    
  # VALIDATES: APEX can process 3-level deeply nested conditional logic
  - id: "apex-level3-conditional"
    name: "APEX Level 3 Nested Conditional Processing"
    description: "Validates APEX can process barrierTerms.knockoutConditions.rebateTerms logic"
    condition: "#barrierTerms?.knockoutConditions?.rebateTerms?.rebateAmount != null && #pricingTerms?.premium != null"
    result-field: "apexRebatePercentage"
    expression: "(#barrierTerms.knockoutConditions.rebateTerms.rebateAmount / #pricingTerms.premium) * 100"
    priority: 3
    categories: ["level-3-nested", "conditional-logic", "percentage-calculation"]
    
  # VALIDATES: APEX can handle complex nested date calculations with SpEL
  - id: "apex-nested-date-calculation"
    name: "APEX Nested Date Calculation with SpEL"
    description: "Validates APEX can calculate days between nested observation period dates"
    condition: "#barrierTerms?.knockoutConditions?.observationPeriod?.startDate != null && #barrierTerms?.knockoutConditions?.observationPeriod?.endDate != null"
    result-field: "apexObservationPeriodDays"
    expression: "T(java.time.temporal.ChronoUnit).DAYS.between(T(java.time.LocalDate).parse(#barrierTerms.knockoutConditions.observationPeriod.startDate), T(java.time.LocalDate).parse(#barrierTerms.knockoutConditions.observationPeriod.endDate))"
    priority: 4
    categories: ["date-calculation", "spel-integration", "level-3-nested"]

# Processing Configuration
processing-config:
  parallel-processing: false
  error-handling: "fail-fast"
  logging-level: "INFO"
  performance-monitoring: true

# Expected Results for Validation
expected-results:
  total-enrichments: 4
  processing-success-rate: "100%"
  nested-levels-tested: 3
  business-calculations: 2
```

### Phase 3: APEX Validation Configuration

#### File: `barrier-option-nested-validation.yaml`
**Location**: `apex-demo/src/test/resources/lookup/`
**Purpose**: Validate APEX validation engine nested rule processing

```yaml
# APEX Nested Validation Rules Configuration
# Validates APEX validation engine can enforce business rules on nested structures

metadata:
  id: "barrier-option-nested-validation"
  name: "APEX Nested Validation Rules"
  version: "1.0.0"
  description: "Validates APEX validation engine nested business rule enforcement"

# APEX Validation Rules - All validation logic defined here
rules:
  # VALIDATES: APEX can validate nested field relationships
  - id: "nested-barrier-strike-validation"
    name: "Nested Barrier vs Strike Price Validation"
    description: "Validates barrier level is above strike price for up-and-out options"
    condition: "#barrierTerms?.barrierLevel != null && #pricingTerms?.strikePrice != null && #barrierTerms?.barrierDirection == 'Up-and-Out'"
    validation-expression: "#barrierTerms.barrierLevel > #pricingTerms.strikePrice"
    message: "Barrier level must be above strike price for up-and-out options"
    severity: "ERROR"
    categories: ["nested-validation", "business-rule"]
    
  # VALIDATES: APEX can validate 3-level nested date consistency
  - id: "nested-date-consistency-validation"
    name: "Nested Date Consistency Validation"
    description: "Validates observation period dates are consistent with expiry"
    condition: "#barrierTerms?.knockoutConditions?.observationPeriod?.endDate != null && #expiryDate != null"
    validation-expression: "T(java.time.LocalDate).parse(#barrierTerms.knockoutConditions.observationPeriod.endDate).equals(T(java.time.LocalDate).parse(#expiryDate))"
    message: "Observation period end date must match option expiry date"
    severity: "ERROR"
    categories: ["date-validation", "level-3-nested"]
    
  # VALIDATES: APEX can validate nested rebate business logic
  - id: "nested-rebate-validation"
    name: "Nested Rebate Amount Validation"
    description: "Validates rebate amount is reasonable percentage of premium"
    condition: "#barrierTerms?.knockoutConditions?.rebateTerms?.rebateAmount != null && #pricingTerms?.premium != null"
    validation-expression: "(#barrierTerms.knockoutConditions.rebateTerms.rebateAmount / #pricingTerms.premium) <= 0.5"
    message: "Rebate amount cannot exceed 50% of premium"
    severity: "WARNING"
    categories: ["rebate-validation", "business-logic"]
```

## Implementation Success Criteria

### ✅ APEX Functionality Validation
1. **APEX Nested Navigation**: Successfully access 3+ level nested XML fields
2. **APEX Mathematical Processing**: Accurate calculations on nested values
3. **APEX Conditional Logic**: Complex nested condition evaluation
4. **APEX Date Processing**: SpEL-based nested date calculations
5. **APEX Validation Engine**: Nested business rule enforcement
6. **100% Processing Rate**: Log shows "Processed: 4 out of 4"

### ✅ Business Logic Validation
1. **Financial Calculations**: Barrier spread, rebate percentage calculations
2. **Date Calculations**: Observation period day calculations
3. **Cross-Field Validation**: Barrier level vs strike price validation
4. **Nested Rule Enforcement**: Multi-level business rule validation

### ✅ Technical Implementation
1. **Zero Custom Processing**: All logic in APEX YAML configurations
2. **Real APEX Services**: Use `enrichmentService.enrichObject()` exclusively
3. **Comprehensive Assertions**: Every enrichment result validated
4. **Incremental Testing**: Test after each YAML addition

## Incremental Development Steps

### Step 1: Basic XML and Level 2 Nesting
- Create `barrier-option-simple.xml`
- Create enrichment YAML with 2 enrichments (Level 1 and Level 2 nesting)
- Test and validate 2 out of 2 processing

### Step 2: Add Level 3 Nesting
- Add Level 3 nested enrichment to YAML
- Test and validate 3 out of 3 processing

### Step 3: Add Mathematical Calculations
- Add percentage and date calculation enrichments
- Test and validate 4 out of 4 processing

### Step 4: Add Validation Rules
- Create validation YAML configuration
- Add validation test methods
- Validate nested business rule enforcement

## Expected Outcomes

This implementation will **prove that APEX can handle complex nested data structures** through its built-in engines, demonstrating:

1. **APEX Robustness**: Handle 3+ levels of XML nesting
2. **APEX Flexibility**: Process various data types (strings, numbers, dates)
3. **APEX Business Logic**: Support complex financial calculations
4. **APEX Integration**: Seamless SpEL expression processing
5. **APEX Validation**: Comprehensive nested rule enforcement

**Result**: Confidence that APEX can process real-world complex financial instruments with deeply nested data structures.
