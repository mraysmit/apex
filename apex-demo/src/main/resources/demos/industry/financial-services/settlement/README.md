# Comprehensive Financial Settlement Enrichment Demo

This demo showcases comprehensive post-trade settlement enrichment using the APEX Rules Engine with real-world financial examples from the "Types of Enrichment Relevant for Financial Services Post-Trade Settlement" guide.

## Overview

The demo implements all major enrichment categories for trade settlement processing, demonstrating how financial institutions can use the APEX Rules Engine to enrich trade data with reference data, regulatory information, risk metrics, settlement instructions, and fee calculations.

## Demo Components

### 📁 Configuration Files
- **`comprehensive-settlement-enrichment.yaml`** - Complete YAML configuration with all enrichment rules

### 📁 Java Classes
- **`ComprehensiveFinancialSettlementDemo.java`** - Main demo class with enrichment processing
- **`FinancialSettlementDemoRunner.java`** - Spring Boot runner and standalone demo
- **`ComprehensiveFinancialSettlementDemoTest.java`** - Comprehensive test suite

### 📁 Model Classes
- **`TradeConfirmation.java`** - Root trade confirmation model
- **`Header.java`** - Message header with routing information
- **`Trade.java`** - Core trade information
- **`TradeHeader.java`** - Trade identification and execution details
- **`Security.java`** - Financial instrument details
- **`Counterparty.java`** - Trading counterparty information
- **`PartyTradeIdentifier.java`** - Trade ID and party reference

## Enrichment Categories

### 🏛️ Reference Data Enrichment
- **LEI Codes** - Legal Entity Identifier lookup and validation
- **ISIN Validation** - International Securities Identification Number format validation
- **Security Master** - CUSIP, SEDOL, security name, asset class enrichment
- **MIC Codes** - Market Identifier Codes for trading venues
- **BIC Codes** - Bank Identifier Codes for settlement routing
- **SSI** - Standard Settlement Instructions with custodian details

### 🏦 Counterparty Enrichment
- **Credit Ratings** - Moody's, S&P, Fitch ratings lookup
- **Entity Classification** - Business model and regulatory status
- **Client Relationships** - Tier assignments and relationship status
- **Legal Agreements** - ISDA Master Agreement and CSA status
- **Clearing House** - Default fund contributions and membership status

### 📊 Regulatory Enrichment
- **UTI Generation** - Unique Transaction Identifier creation
- **Jurisdiction Flags** - MiFID II, EMIR, Dodd-Frank applicability
- **MiFID II Fields** - Transaction reference numbers and reporting flags
- **EMIR Fields** - Clearing obligations and thresholds
- **Legal Documentation** - CSA thresholds and minimum transfer amounts

### ⚠️ Risk Enrichment
- **VaR Calculations** - 1-day and 10-day Value-at-Risk
- **Volatility Metrics** - Implied and historical volatility lookup
- **Stress Testing** - Market crash and volatility shock scenarios
- **Risk Classification** - Risk level assignment and action requirements
- **Greeks** - Beta, duration, and other risk sensitivities

### 🏛️ Settlement Enrichment
- **Settlement Cycles** - T+1, T+2 cycle determination by market
- **Settlement Dates** - Business day adjusted settlement date calculation
- **Priority Assignment** - High/Medium/Normal priority based on trade value
- **Settlement Methods** - DVP, CREST, DTC method assignment

### 💰 Fee Calculation Enrichment
- **Broker Commissions** - Tiered commission rates with min/max caps
- **Exchange Fees** - Market-specific exchange fees
- **Clearing Fees** - Central counterparty clearing fees
- **Regulatory Fees** - SEC, TAF, FINRA fees for US trades
- **Total Calculations** - Net settlement amount calculations

## Trade Examples

### 1. UK Equity Trade - Royal Dutch Shell PLC
```yaml
Security: GB00B03MLX29 (Royal Dutch Shell PLC)
Counterparty: Deutsche Bank AG
Trading Venue: XLON (London Stock Exchange)
Trade Value: £27,505,000 (10,000 shares @ £2,750.50)
Settlement: T+2, CREST, Priority: MEDIUM
```

### 2. US Government Bond Trade - 10Y Treasury Note
```yaml
Security: US912828XG93 (US Treasury Note 10Y)
Counterparty: JPMorgan Chase
Trading Venue: BONDDESK
Trade Value: $98,750,000 (1,000,000 bonds @ $98.75)
Settlement: T+1, DTC, Priority: HIGH
```

### 3. German Equity Trade - SAP SE
```yaml
Security: DE0007164600 (SAP SE)
Counterparty: Barclays Bank PLC
Trading Venue: XPAR (Euronext Paris)
Trade Value: €603,750 (5,000 shares @ €120.75)
Settlement: T+2, EUROCLEAR, Priority: NORMAL
```

### 4. High-Value US Equity Trade - Apple Inc
```yaml
Security: US0378331005 (Apple Inc)
Counterparty: Goldman Sachs
Trading Venue: XNAS (NASDAQ)
Trade Value: $95,000,000 (500,000 shares @ $190.00)
Settlement: T+1, DTC, Priority: HIGH
Special: Triggers stress testing and high-priority processing
```

## Industry Standards

### 📋 Standards Compliance
- **ISO 20022** - Financial messaging standards
- **FpML** - Financial Products Markup Language
- **ISO 17442** - Legal Entity Identifier (LEI) codes
- **ISO 6166** - International Securities Identification Number (ISIN)
- **ISO 10383** - Market Identifier Codes (MIC)
- **ISO 9362** - Business Identifier Codes (BIC/SWIFT)

### 📋 Regulatory Frameworks
- **EMIR** - European Market Infrastructure Regulation
- **MiFID II** - Markets in Financial Instruments Directive
- **Dodd-Frank** - US derivatives regulation
- **ISDA** - International Swaps and Derivatives Association standards

## Running the Demo

### Option 1: Spring Boot Runner
```bash
# Run the complete interactive demo
java -jar apex-demo.jar dev.mars.apex.demo.financial.FinancialSettlementDemoRunner
```

### Option 2: Standalone Demo
```bash
# Run the standalone demo (shows configuration overview)
java dev.mars.apex.demo.financial.StandaloneFinancialSettlementDemo
```

### Option 3: Test Suite
```bash
# Run the comprehensive test suite
mvn test -Dtest=ComprehensiveFinancialSettlementDemoTest
```

## Expected Output

The demo processes each trade example and displays:

1. **Validation Results** - Rule validation status and error messages
2. **Reference Data Enrichment** - LEI, ISIN, MIC, BIC, SSI lookups
3. **Counterparty Enrichment** - Credit ratings, classifications, relationships
4. **Regulatory Enrichment** - UTI generation, jurisdiction flags, reporting requirements
5. **Risk Enrichment** - VaR calculations, volatility metrics, stress test results
6. **Settlement Enrichment** - Settlement dates, priorities, methods
7. **Fee Calculations** - Commission rates, fees, net settlement amounts
8. **Rule Execution Summary** - Success/failure counts and performance metrics

## Configuration Highlights

### APEX YAML Syntax Features Demonstrated
- **Proper Conditions**: `#data.field != null` syntax
- **Nested Field Access**: `#trade.security.instrumentId`
- **Ternary Operators**: `#condition ? 'value1' : 'value2'`
- **Mathematical Functions**: `T(java.lang.Math).max()`, `T(java.lang.Math).sqrt()`
- **Date/Time Functions**: `T(java.time.LocalDate).now()`
- **Complex Calculations**: Multi-step risk and fee calculations
- **Lookup Configurations**: Proper `lookup-config` with `field-mappings`
- **Calculation Enrichments**: Complex financial formulas

### Real-World Data
All examples use actual:
- **ISIN codes** for real securities (Shell, Apple, SAP, US Treasury)
- **LEI codes** for major financial institutions
- **MIC codes** for actual trading venues
- **BIC codes** for settlement routing
- **Market conventions** for settlement cycles and fees

## Integration Points

This demo can be extended to integrate with:
- **Market Data Feeds** - Real-time pricing and volatility data
- **Reference Data Vendors** - Bloomberg, Refinitiv, S&P
- **Settlement Systems** - CREST, DTC, Euroclear, Clearstream
- **Risk Management Systems** - VaR engines, stress testing platforms
- **Regulatory Reporting** - EMIR, MiFID II, Dodd-Frank reporting systems

## Next Steps

1. **Extend with Real Data Sources** - Replace inline datasets with external APIs
2. **Add More Asset Classes** - Derivatives, commodities, structured products
3. **Implement Real-Time Processing** - Stream processing for live trades
4. **Add Monitoring** - Metrics, alerts, and performance monitoring
5. **Regulatory Reporting** - Generate actual regulatory reports
6. **Integration Testing** - End-to-end testing with real settlement systems
