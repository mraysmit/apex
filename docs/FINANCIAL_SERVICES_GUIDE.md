# SpEL Rules Engine - Financial Services Guide

## Overview

This guide provides comprehensive documentation for using the SpEL Rules Engine in financial services environments, with specific focus on post-trade settlement, regulatory compliance, and OTC derivatives validation.

## Financial Services Use Cases

### Post-Trade Settlement Validation

The Rules Engine excels at validating complex financial transactions during post-trade processing:

```yaml
metadata:
  name: "Post-Trade Settlement Rules"
  domain: "Financial Services"
  purpose: "Post-trade settlement validation"

rules:
  - id: "settlement-date-validation"
    name: "Settlement Date Validation"
    condition: "#settlementDate != null && #settlementDate.isAfter(#tradeDate)"
    message: "Settlement date must be after trade date"
    severity: "ERROR"
    
  - id: "counterparty-validation"
    name: "Counterparty Validation"
    condition: "#counterpartyLEI != null && #counterpartyLEI.length() == 20"
    message: "Valid LEI required for counterparty"
    severity: "ERROR"
    
  - id: "notional-amount-validation"
    name: "Notional Amount Validation"
    condition: "#notionalAmount > 0 && #notionalAmount <= 100000000"
    message: "Notional amount must be positive and within limits"
    severity: "ERROR"
```

### OTC Commodity Total Return Swaps

Specialized validation for OTC derivatives:

```yaml
rules:
  - id: "commodity-swap-validation"
    name: "OTC Commodity Swap Validation"
    condition: |
      #instrumentType == 'COMMODITY_TRS' && 
      #underlyingCommodity != null && 
      #returnType in {'TOTAL_RETURN', 'PRICE_RETURN'} &&
      #paymentFrequency in {'MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL'}
    message: "Valid commodity TRS structure required"
    severity: "ERROR"
    
  - id: "commodity-reference-validation"
    name: "Commodity Reference Validation"
    condition: "#commodityReferencePrice != null && #commodityReferencePrice > 0"
    message: "Valid commodity reference price required"
    severity: "ERROR"
    depends-on: ["commodity-enrichment"]

enrichments:
  - id: "commodity-enrichment"
    type: "lookup-enrichment"
    condition: "['underlyingCommodity'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/commodities.yaml"
        key-field: "code"
        cache-enabled: true
    field-mappings:
      - source-field: "name"
        target-field: "commodityName"
      - source-field: "sector"
        target-field: "commoditySector"
      - source-field: "unit"
        target-field: "commodityUnit"
```

## Types of Enrichment for Financial Services

### 1. Reference Data Enrichment

#### Legal Entity Identifier (LEI) Enrichment
```yaml
enrichments:
  - id: "lei-enrichment"
    type: "lookup-enrichment"
    condition: "['counterpartyLEI'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/lei-registry.yaml"
        key-field: "lei"
        cache-enabled: true
        cache-ttl-seconds: 86400  # 24 hours
    field-mappings:
      - source-field: "legalName"
        target-field: "counterpartyName"
      - source-field: "jurisdiction"
        target-field: "counterpartyJurisdiction"
      - source-field: "status"
        target-field: "leiStatus"
```

#### ISIN/CUSIP/SEDOL Enrichment
```yaml
enrichments:
  - id: "security-identifier-enrichment"
    type: "lookup-enrichment"
    condition: "['isin'] != null || ['cusip'] != null || ['sedol'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/security-identifiers.yaml"
        key-field: "primaryId"
        cache-enabled: true
    field-mappings:
      - source-field: "securityName"
        target-field: "instrumentName"
      - source-field: "issuer"
        target-field: "issuerName"
      - source-field: "maturityDate"
        target-field: "maturityDate"
```

#### Market Identifier Codes (MIC)
```yaml
enrichments:
  - id: "mic-enrichment"
    type: "lookup-enrichment"
    condition: "['marketCode'] != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "mic"
        data:
          - mic: "XNYS"
            name: "New York Stock Exchange"
            country: "US"
            timezone: "America/New_York"
          - mic: "XLON"
            name: "London Stock Exchange"
            country: "GB"
            timezone: "Europe/London"
          - mic: "XTKS"
            name: "Tokyo Stock Exchange"
            country: "JP"
            timezone: "Asia/Tokyo"
    field-mappings:
      - source-field: "name"
        target-field: "marketName"
      - source-field: "country"
        target-field: "marketCountry"
      - source-field: "timezone"
        target-field: "marketTimezone"
```

### 2. Counterparty Enrichment

#### Credit Rating Information
```yaml
enrichments:
  - id: "credit-rating-enrichment"
    type: "lookup-enrichment"
    condition: "['counterpartyLEI'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/credit-ratings.yaml"
        key-field: "lei"
        cache-enabled: true
    field-mappings:
      - source-field: "moodysRating"
        target-field: "moodysRating"
      - source-field: "spRating"
        target-field: "spRating"
      - source-field: "fitchRating"
        target-field: "fitchRating"
      - source-field: "riskTier"
        target-field: "counterpartyRiskTier"
```

#### Counterparty Classification
```yaml
enrichments:
  - id: "counterparty-classification"
    type: "lookup-enrichment"
    condition: "['counterpartyLEI'] != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "lei"
        data:
          - lei: "LEI123456789012345678"
            type: "INVESTMENT_BANK"
            tier: "TIER_1"
            nettingAgreement: true
          - lei: "LEI987654321098765432"
            type: "HEDGE_FUND"
            tier: "TIER_2"
            nettingAgreement: false
    field-mappings:
      - source-field: "type"
        target-field: "counterpartyType"
      - source-field: "tier"
        target-field: "counterpartyTier"
      - source-field: "nettingAgreement"
        target-field: "hasNettingAgreement"
```

### 3. Regulatory Enrichment

#### Regulatory Reporting Flags
```yaml
enrichments:
  - id: "regulatory-flags-enrichment"
    type: "lookup-enrichment"
    condition: "['instrumentType'] != null && ['notionalAmount'] != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "instrumentType"
        data:
          - instrumentType: "INTEREST_RATE_SWAP"
            mifidReporting: true
            emirReporting: true
            doddFrankReporting: true
            clearingMandatory: true
          - instrumentType: "COMMODITY_TRS"
            mifidReporting: true
            emirReporting: false
            doddFrankReporting: true
            clearingMandatory: false
          - instrumentType: "EQUITY_SWAP"
            mifidReporting: true
            emirReporting: true
            doddFrankReporting: false
            clearingMandatory: false
    field-mappings:
      - source-field: "mifidReporting"
        target-field: "requiresMiFIDReporting"
      - source-field: "emirReporting"
        target-field: "requiresEMIRReporting"
      - source-field: "doddFrankReporting"
        target-field: "requiresDoddFrankReporting"
      - source-field: "clearingMandatory"
        target-field: "clearingMandatory"
```

#### Transaction Reporting Fields
```yaml
rules:
  - id: "mifid-reporting-validation"
    name: "MiFID II Reporting Validation"
    condition: |
      #requiresMiFIDReporting == true implies (
        #uti != null && 
        #executionTimestamp != null && 
        #instrumentClassification != null
      )
    message: "MiFID II reporting requires UTI, execution timestamp, and instrument classification"
    severity: "ERROR"
    depends-on: ["regulatory-flags-enrichment"]
    
  - id: "emir-reporting-validation"
    name: "EMIR Reporting Validation"
    condition: |
      #requiresEMIRReporting == true implies (
        #uti != null && 
        #upi != null && 
        #counterpartyLEI != null
      )
    message: "EMIR reporting requires UTI, UPI, and counterparty LEI"
    severity: "ERROR"
    depends-on: ["regulatory-flags-enrichment"]
```

### 4. Risk Enrichment

#### Value-at-Risk (VaR) Metrics
```yaml
enrichments:
  - id: "var-enrichment"
    type: "lookup-enrichment"
    condition: "['instrumentType'] != null && ['notionalAmount'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/var-parameters.yaml"
        key-field: "instrumentType"
        cache-enabled: true
    field-mappings:
      - source-field: "varMultiplier"
        target-field: "varMultiplier"
      - source-field: "volatility"
        target-field: "impliedVolatility"
      - source-field: "correlationFactor"
        target-field: "correlationFactor"

rules:
  - id: "var-calculation"
    name: "VaR Calculation"
    condition: "true"  # Always calculate
    action: |
      #calculatedVaR = #notionalAmount * #varMultiplier * #impliedVolatility * 
                       sqrt(#holdingPeriod) * #correlationFactor
    depends-on: ["var-enrichment"]
    
  - id: "var-limit-check"
    name: "VaR Limit Validation"
    condition: "#calculatedVaR <= #varLimit"
    message: "Trade exceeds VaR limit"
    severity: "WARNING"
    depends-on: ["var-calculation"]
```

#### Margin Requirement Enrichment
```yaml
enrichments:
  - id: "margin-enrichment"
    type: "lookup-enrichment"
    condition: "['instrumentType'] != null"
    lookup-config:
      lookup-dataset:
        type: "inline"
        key-field: "instrumentType"
        data:
          - instrumentType: "INTEREST_RATE_SWAP"
            initialMarginRate: 0.02
            variationMarginThreshold: 500000
            minimumTransferAmount: 100000
          - instrumentType: "COMMODITY_TRS"
            initialMarginRate: 0.15
            variationMarginThreshold: 250000
            minimumTransferAmount: 50000
    field-mappings:
      - source-field: "initialMarginRate"
        target-field: "initialMarginRate"
      - source-field: "variationMarginThreshold"
        target-field: "vmThreshold"
      - source-field: "minimumTransferAmount"
        target-field: "minTransferAmount"

rules:
  - id: "initial-margin-calculation"
    name: "Initial Margin Calculation"
    condition: "true"
    action: "#initialMargin = #notionalAmount * #initialMarginRate"
    depends-on: ["margin-enrichment"]
```

## Financial Services Templates

### Complete OTC Derivatives Validation

```yaml
metadata:
  name: "OTC Derivatives Validation Suite"
  version: "2.0.0"
  domain: "Financial Services"
  purpose: "Comprehensive OTC derivatives validation"

# Currency and market data enrichment
enrichments:
  - id: "currency-enrichment"
    type: "lookup-enrichment"
    condition: "['currency'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/currencies.yaml"
        key-field: "code"
    field-mappings:
      - source-field: "name"
        target-field: "currencyName"
      - source-field: "isActive"
        target-field: "currencyActive"

  - id: "counterparty-enrichment"
    type: "lookup-enrichment"
    condition: "['counterpartyLEI'] != null"
    lookup-config:
      lookup-dataset:
        type: "yaml-file"
        file-path: "datasets/counterparties.yaml"
        key-field: "lei"
    field-mappings:
      - source-field: "name"
        target-field: "counterpartyName"
      - source-field: "riskRating"
        target-field: "counterpartyRisk"

# Validation rules
rules:
  - id: "basic-trade-validation"
    name: "Basic Trade Validation"
    condition: |
      #tradeDate != null && 
      #notionalAmount > 0 && 
      #currency != null && 
      #counterpartyLEI != null
    message: "Basic trade information is required"
    severity: "ERROR"
    
  - id: "currency-active-check"
    name: "Currency Active Check"
    condition: "#currencyActive == true"
    message: "Currency must be active for trading"
    severity: "ERROR"
    depends-on: ["currency-enrichment"]
    
  - id: "counterparty-risk-check"
    name: "Counterparty Risk Check"
    condition: "#counterpartyRisk in {'A', 'B', 'C'}"
    message: "Counterparty risk rating must be acceptable"
    severity: "WARNING"
    depends-on: ["counterparty-enrichment"]
    
  - id: "notional-limit-check"
    name: "Notional Amount Limit"
    condition: "#notionalAmount <= 50000000"
    message: "Trade exceeds maximum notional limit"
    severity: "WARNING"
```

## Implementation Best Practices

### Dataset Organization
- **Separate datasets by domain**: currencies, counterparties, instruments
- **Use external files for reusable data**: avoid inline datasets for common reference data
- **Version control datasets**: track changes to reference data
- **Environment-specific datasets**: different data for dev/test/prod

### Performance Considerations
- **Enable caching for frequently accessed datasets**
- **Use appropriate cache TTL values** based on data volatility
- **Monitor cache hit ratios** and adjust cache sizes
- **Preload critical datasets** during application startup

### Regulatory Compliance
- **Maintain audit trails** for all rule changes
- **Document business rationale** for each rule
- **Version control configurations** for regulatory reporting
- **Test rule changes thoroughly** before production deployment

### Error Handling
- **Graceful degradation** when enrichment data is unavailable
- **Clear error messages** for business users
- **Comprehensive logging** for audit and debugging
- **Fallback strategies** for critical validations
