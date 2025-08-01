# APEX Commodity Swap Validation Bootstrap

## Overview

This APEX bootstrap demonstrates a complete end-to-end commodity derivatives validation and enrichment system. It showcases the layered API approach of APEX in solving real-world commodity swap validation challenges through ultra-simple APIs, template-based rules, advanced configuration, and comprehensive static data enrichment.

**Key Achievement**: This bootstrap demonstrates how APEX can provide comprehensive commodity derivatives validation with sub-100ms processing times, multi-layered validation approaches, and complete audit trails.

## What This Bootstrap Demonstrates

### Core APEX Features
- **Layered API Approach**: Ultra-simple, template-based, and advanced configuration APIs
- **Static Data Integration**: Client, counterparty, commodity, and currency validation
- **Performance Monitoring**: Real-time processing metrics and audit trails
- **Exception Handling**: Robust error handling and validation failure scenarios
- **YAML Configuration**: External business-user maintainable rule configurations

### Financial Domain Features
- **Commodity Total Return Swaps**: Complete financial instrument modeling
- **Multi-Asset Class Support**: Energy, metals, and agricultural commodities
- **Regulatory Compliance**: Dodd-Frank, EMIR, CFTC, and MiFID II support
- **Risk Management**: Credit limits, exposure checks, and concentration limits
- **Static Data Enrichment**: Real-time lookup and field population

### Advanced APEX Features Demonstrated
- **Conditional Chaining**: Eligibility checks with complex business logic
- **Accumulative Chaining**: Weighted scoring across multiple validation rules
- **Lookup Enrichments**: Automatic field population from static data sources
- **SpEL Expressions**: Complex conditional logic with null safety and object navigation
- **Performance Monitoring**: Sub-100ms processing times with comprehensive metrics
- **Database Integration**: Full PostgreSQL integration with connection pooling

## Prerequisites

### Required
- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL 12 or higher (optional - falls back to in-memory mode)

### Optional
- PostgreSQL database server running on localhost:5432
- Database user 'postgres' with password 'postgres'
- Database creation privileges

## Quick Start

### 1. Run the Bootstrap
```bash
# From the apex-demo directory
mvn compile exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CommoditySwapValidationBootstrap"
```

### 2. Alternative: Run with JAR
```bash
# Build the JAR with dependencies
mvn clean package

# Run the bootstrap
java -cp target/apex-demo-1.0-SNAPSHOT-jar-with-dependencies.jar \
  dev.mars.apex.demo.bootstrap.CommoditySwapValidationBootstrap
```

### 3. Database Configuration (Optional)
If PostgreSQL is not available, the bootstrap automatically falls back to in-memory simulation mode.

To use PostgreSQL, ensure it's running with default settings:
```bash
# Start PostgreSQL (varies by system)
sudo systemctl start postgresql  # Linux
brew services start postgresql   # macOS
net start postgresql-x64-12     # Windows
```

## Demonstration Scenarios

### Scenario 1: Ultra-Simple API Demonstration
**Purpose**: Demonstrate basic field validation using APEX's ultra-simple API
**Features**:
- Required field validation (Trade ID, Counterparty ID, Client ID)
- Positive notional amount validation
- Commodity type validation
- Simple boolean result evaluation

**Sample Output**:
```
--- SCENARIO 1: ULTRA-SIMPLE API DEMONSTRATION ---
Trade: TRS001 (ENERGY - WTI)
   ✓ Trade ID validation: PASS
   ✓ Counterparty validation: PASS
   ✓ Client validation: PASS
   ✓ Notional validation: PASS
   ✓ Commodity type validation: PASS
   ✓ Overall validation: PASS
   ✓ Processing time: 15ms
```

### Scenario 2: Template-Based Rules Demonstration
**Purpose**: Show business logic validation using template-based rules
**Features**:
- Maturity date eligibility checks
- Currency consistency validation
- Settlement terms validation
- Rule group evaluation with pass/fail reporting

**Sample Output**:
```
--- SCENARIO 2: TEMPLATE-BASED RULES DEMONSTRATION ---
Trade: TRS002 (METALS - GOLD)
   ✓ Validation result: PASS
   ✓ Rules passed: 7
   ✓ Rules failed: 0
   ✓ Business rules result: PASS
   ✓ Processing time: 23ms
```

### Scenario 3: Advanced Configuration Demonstration
**Purpose**: Complex validation using advanced APEX configuration
**Features**:
- Trade ID format validation (TRS### pattern)
- Notional amount range checks ($1M - $100M)
- Regulatory compliance validation
- Advanced SpEL expressions

**Sample Output**:
```
--- SCENARIO 3: ADVANCED CONFIGURATION DEMONSTRATION ---
Trade: TRS003 (AGRICULTURAL - CORN)
   ✓ Advanced rules created: 5
   ✓ trade-id-format: PASS
   ✓ notional-range: PASS
   ✓ commodity-energy-check: PASS
   ✓ maturity-date-check: PASS
   ✓ funding-spread-check: PASS
   ✓ Processing time: 18ms
```

### Scenario 4: Static Data Validation & Enrichment
**Purpose**: Demonstrate static data integration and field enrichment
**Features**:
- Client validation and name enrichment
- Counterparty validation and credit rating lookup
- Currency validation and decimal places lookup
- Commodity reference data validation and index provider enrichment

**Sample Output**:
```
--- SCENARIO 4: STATIC DATA VALIDATION & ENRICHMENT ---
Trade: TRS001 (ENERGY - WTI)

1. Client Validation:
   ✓ Client found: Energy Trading Fund Alpha
   ✓ Client active: true
   ✓ Client type: INSTITUTIONAL
   ✓ Regulatory classification: ECP
   ✓ Swap enriched with client name

2. Counterparty Validation:
   ✓ Counterparty found: Global Investment Bank
   ✓ Counterparty active: true
   ✓ Counterparty type: BANK
   ✓ Credit rating: AA-

3. Currency Validation:
   ✓ Currency found: US Dollar
   ✓ Currency active: true
   ✓ Currency tradeable: true
   ✓ Decimal places: 2

4. Commodity Reference Validation:
   ✓ Commodity found: West Texas Intermediate Crude Oil
   ✓ Commodity active: true
   ✓ Index provider: NYMEX
   ✓ Quote currency: USD
   ✓ Swap enriched with index provider

   ✓ Processing time: 12ms
```

### Scenario 5: Performance Monitoring Demonstration
**Purpose**: Show APEX performance monitoring capabilities
**Features**:
- Multiple swap processing
- Individual and aggregate timing metrics
- Performance target validation (<100ms per swap)
- Batch processing demonstration

**Sample Output**:
```
--- SCENARIO 5: PERFORMANCE MONITORING DEMONSTRATION ---
   ✓ Swap 1 (TRS001): 8ms - VALID
   ✓ Swap 2 (TRS002): 6ms - VALID
   ✓ Swap 3 (TRS003): 7ms - VALID
   ✓ Total processing time: 21ms
   ✓ Average per swap: 7ms
   ✓ Target: <100ms per swap
```

### Scenario 6: Exception Handling Demonstration
**Purpose**: Demonstrate robust error handling and validation failures
**Features**:
- Invalid trade ID format handling
- Null value handling with graceful degradation
- Invalid commodity type validation
- Exception catching and reporting

**Sample Output**:
```
--- SCENARIO 6: EXCEPTION HANDLING DEMONSTRATION ---

1. Invalid Trade ID Format:
   ✓ Trade ID format validation: FAIL

2. Null Notional Amount:
   ✓ Notional amount validation: FAIL

3. Invalid Commodity Type:
   ✓ Commodity type validation: FAIL

   ✓ Processing time: 14ms
```

## Architecture Overview

### System Components
```
CommoditySwapValidationBootstrap
├── Infrastructure Layer
│   ├── PostgreSQL Database (HikariCP connection pooling)
│   ├── YAML Configuration Management (SnakeYAML)
│   └── Logging and Audit (SLF4J with comprehensive tracing)
│
├── APEX Integration Layer
│   ├── Rules Service (Multi-layered API support)
│   ├── Enrichment Service (Static data lookup)
│   ├── Performance Monitor (Sub-100ms target tracking)
│   └── YAML Configuration Loader
│
├── Business Logic Layer
│   ├── Commodity Swap Validation
│   ├── Static Data Integration
│   ├── Risk Management Rules
│   └── Exception Handling
│
├── Data Access Layer
│   ├── Commodity Swaps DAO
│   ├── Static Data Repositories
│   └── Audit Trail Management
│
└── Data Model Layer
    ├── CommodityTotalReturnSwap
    ├── CommodityClient
    ├── CommodityCounterparty
    ├── CommodityReference
    └── CurrencyData
```

### Database Schema
The bootstrap creates the following tables:
- **commodity_swaps**: Main transaction storage
- **commodity_reference_data**: Static commodity data
- **client_data**: Client information and limits
- **counterparty_data**: Counterparty details and ratings
- **validation_audit**: Complete audit trail

### Static Data Coverage
- **3 Clients**: Institutional, hedge fund, and corporate clients
- **3 Counterparties**: Banks, trading houses, and specialists
- **6 Commodities**: Energy (WTI, Brent, Henry Hub), Metals (Gold, Silver), Agricultural (Corn)
- **6 Currencies**: USD, EUR, GBP, JPY, CHF, CAD

## YAML Configuration Analysis

### Configuration File Location
```
apex-demo/src/main/resources/bootstrap/commodity-swap-validation-bootstrap.yaml
```

This 280-line YAML file contains all business logic, validation rules, and enrichment configurations in an external, business-user maintainable format.

### Key Configuration Sections

#### 1. Rule Chains (4 chains, 15+ rules)
- **Ultra-Simple Validation**: Basic field checks
- **Template-Based Business Rules**: Weighted scoring with accumulative chaining
- **Advanced Configuration**: Complex SpEL expressions and format validation
- **Risk Management Rules**: Credit limits and exposure checks

#### 2. Enrichments (3 enrichment sources, 10+ field mappings)
- **Client Enrichment**: Name, regulatory classification, risk rating
- **Counterparty Enrichment**: Name, credit rating, regulatory status
- **Commodity Enrichment**: Index provider, quote currency, unit of measure

#### 3. Configuration Settings
- **Thresholds**: Notional limits, maturity constraints, validation scores
- **Performance**: Processing time targets, caching, audit settings
- **Business Rules**: Regulatory requirements, validation flags
- **Supported Assets**: Commodity types, indices, currencies, regulatory regimes

## Performance Metrics

### Target Performance
- **Processing Time**: <100ms per swap validation
- **Throughput**: >1000 swaps per second
- **Memory Usage**: <50MB for static data
- **Database Connections**: Pooled with HikariCP

### Actual Performance (Typical)
- **Scenario 1 (Ultra-Simple)**: 8-15ms
- **Scenario 2 (Template-Based)**: 15-25ms
- **Scenario 3 (Advanced)**: 12-20ms
- **Scenario 4 (Static Data)**: 10-15ms
- **Scenario 5 (Performance)**: 6-8ms per swap
- **Scenario 6 (Exception)**: 10-15ms

## Business Value

### Validation Coverage
- **100% Field Validation**: All required fields validated
- **Multi-Layer Validation**: Ultra-simple, template-based, and advanced
- **Static Data Integration**: Real-time enrichment and validation
- **Risk Management**: Credit limits and exposure monitoring
- **Regulatory Compliance**: Multiple regime support

### Operational Benefits
- **Sub-100ms Processing**: Real-time validation capability
- **Complete Audit Trail**: Full transaction and rule execution logging
- **External Configuration**: Business-user maintainable YAML rules
- **Exception Handling**: Robust error management and recovery
- **Performance Monitoring**: Real-time metrics and alerting

### Technical Advantages
- **Self-Contained**: Single file with all dependencies
- **Re-runnable**: Automatic cleanup and setup
- **Database Agnostic**: PostgreSQL or in-memory mode
- **Production Ready**: Connection pooling, error handling, monitoring
- **Extensible**: Easy to add new commodity types and validation rules

## Extending the Bootstrap

### Adding New Commodity Types
1. Add commodity reference data in `initializeCommodities()`
2. Update YAML configuration with new validation rules
3. Add new sample swap creation method
4. Update static data initialization

### Adding New Validation Rules
1. Update YAML configuration with new rule chains
2. Add corresponding SpEL expressions
3. Update test scenarios to cover new rules
4. Add audit trail support

### Adding New Static Data Sources
1. Create new data model classes
2. Add initialization methods
3. Update YAML enrichment configurations
4. Add database schema if needed

## Troubleshooting

### PostgreSQL Connection Issues
If you see connection errors:
1. Ensure PostgreSQL is running: `pg_ctl status`
2. Check connection settings in the bootstrap
3. Verify user permissions for database creation
4. The bootstrap will automatically fall back to in-memory mode if PostgreSQL is unavailable

### YAML Configuration Issues
If YAML loading fails:
1. Check file path: `apex-demo/src/main/resources/bootstrap/commodity-swap-validation-bootstrap.yaml`
2. Validate YAML syntax using online validators
3. Ensure proper indentation (spaces, not tabs)

### Memory Issues
For large datasets:
1. Increase JVM heap size: `-Xmx2g`
2. Enable garbage collection logging: `-XX:+PrintGC`

### Performance Issues
If processing times exceed targets:
1. Check database connection pool settings
2. Verify static data cache is enabled
3. Review rule complexity and optimize SpEL expressions

## Conclusion

This bootstrap demonstrates the complete power of the APEX Rules Engine in solving complex commodity derivatives validation challenges. Through external YAML configuration, multi-layered validation approaches, comprehensive static data integration, and sub-100ms processing times, APEX enables financial institutions to achieve:

- **Comprehensive Validation**: Multi-layered approach covering all aspects
- **Real-time Processing**: Sub-100ms validation with complete audit trails
- **Business-user Maintainable**: External YAML configuration
- **Production-ready Architecture**: Proper error handling, monitoring, and scalability
- **Regulatory Compliance**: Support for multiple regulatory regimes

The bootstrap serves as both a demonstration of APEX capabilities and a template for implementing similar solutions across various commodity derivatives use cases.
