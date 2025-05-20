
# Enrichers in the rulesengine.demo Module

This document provides an overview of all enrichers in the `rulesengine.demo` module. Enrichers are components that add additional information to domain objects based on business rules and reference data.

## Core Concepts

All enrichers in the demo module:
- Extend `AbstractEnricher<T>` from the core module
- Implement the `enrich(T value)` method to add information to the input object
- Are named services with a specific type they can enrich
- Follow SOLID principles with clear separation of concerns

## Enricher Classes

### 1. AccountingEnricher

**Purpose**: Enriches trade objects with accounting-related information.

**Type**: `Trade`

**Functionality**: Adds accounting data such as ledger entries, cost basis calculations, and tax implications to trades.

### 2. ComplianceEnricher

**Purpose**: Enriches trade objects with compliance-related information.

**Type**: `Trade`

**Functionality**: Adds AML/KYC status, restricted security flags, sanctions screening results, beneficial ownership data, and other compliance information to trades.

### 3. CorporateActionEnricher

**Purpose**: Enriches trade objects with corporate action data.

**Type**: `Trade`

**Functionality**: Adds ex-dates, record dates, payment dates, corporate action types, and other corporate action information to trades.

### 4. CounterpartyEnricher

**Purpose**: Enriches trade objects with counterparty information.

**Type**: `Trade`

**Functionality**: Adds credit ratings, counterparty types, relationship tiers, and other counterparty information to trades.

### 5. CustomerEnricher

**Purpose**: Enriches customer objects with additional information based on membership level and age.

**Type**: `Customer`

**Functionality**:
- Adds recommended product categories based on membership level
- Maintains membership discount rates
- Adds preferred categories to customer profiles

### 6. FeeEnricher

**Purpose**: Enriches trade objects with fee and commission data.

**Type**: `Trade`

**Functionality**:
- Calculates broker commissions based on product type (0.1% for equities, 0.05% for bonds, 0.2% for options)
- Adds clearing fees for derivatives
- Adds custody fees for securities
- Adds exchange fees for exchange-traded instruments
- Adds transaction taxes for specific venues (e.g., UK stamp duty for LSE trades)

### 7. MarketDataEnricher

**Purpose**: Enriches trade objects with market data.

**Type**: `Trade`

**Functionality**: Adds price quotes, bid-ask spreads, trading volumes, market depth, and other market data to trades.

### 8. OperationalEnricher

**Purpose**: Enriches trade objects with operational data.

**Type**: `Trade`

**Functionality**: Adds settlement instruction sequences, failure prediction scores, STP eligibility flags, and other operational data to trades.

### 9. PricingEnricher

**Purpose**: Enriches trade objects with pricing and valuation data.

**Type**: `Trade`

**Functionality**: Adds mark-to-market values, yield calculations, accrued interest, volatility metrics, and other pricing data to trades.

### 10. ProductEnricher

**Purpose**: Enriches product objects with additional information.

**Type**: `Product`

**Functionality**:
- Uses a `GenericEnricher` created by the `ProductEnricherFactory`
- Applies category-based discounts to product prices
- Adds category descriptions to product names
- Supports custom discount application

### 11. ReferenceDataEnricher

**Purpose**: Enriches trade objects with reference data.

**Type**: `Trade`

**Functionality**: Adds security identifiers, issuer information, maturity dates, coupon rates, and other reference data to trades.

### 12. RegulatoryEnricher

**Purpose**: Enriches trade objects with regulatory information.

**Type**: `Trade`

**Functionality**: Adds regulatory reporting flags, transaction reporting requirements, and other regulatory information to trades.

### 13. RiskEnricher

**Purpose**: Enriches trade objects with risk data.

**Type**: `Trade`

**Functionality**:
- Adds VaR metrics based on instrument type
- Calculates exposure based on product type
- Adds margin requirements for derivatives
- Specifies collateral eligibility for futures and options
- Includes stress test results for currency trades

### 14. SettlementEnricher

**Purpose**: Enriches trade objects with settlement data.

**Type**: `Trade`

**Functionality**: Adds settlement dates, settlement instructions, custodian details, and other settlement information to trades.

### 15. TradeEnricher

**Purpose**: Enriches trade objects with additional information based on value and category.

**Type**: `Trade`

**Functionality**:
- Uses the RulesEngine to apply enrichment rules
- Adds value and category descriptions to trades
- Provides both standard enrichment and rule-based enrichment with results

## Support Classes

### ProductEnricherFactory

**Purpose**: Factory for creating product enrichers using the GenericEnricher.

**Functionality**:
- Creates GenericEnricher instances with predefined rules and field mappings
- Supports creation of custom enrichers with specific rules and field mappings
- Provides a method to create enrichers that apply custom discounts

## Usage Pattern

Enrichers are typically used in the following way:

1. Create an instance of the enricher with a name and any required reference data
2. Call the `enrich` method with the object to be enriched
3. Use the enriched object with the additional information

Some enrichers like TradeEnricher also provide an `enrichWithResult` method that returns a `RuleResult` containing the enrichment outcome, which can be used for validation or further processing.

All enrichers in the demo module follow the same pattern and structure, making them consistent and easy to use throughout the application.