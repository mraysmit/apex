# APEX ETL Pipeline Comprehensive Guide

## Overview

The APEX Rules Engine provides a powerful ETL (Extract, Transform, Load) pipeline system that supports YAML-driven configuration for data processing workflows. This comprehensive guide covers pipeline configuration, error handling, operation resolution, practical demonstrations, and best practices.

## Table of Contents

1. [Pipeline Configuration Structure](#pipeline-configuration-structure)
2. [Data Source Types](#data-source-types)
   - [File System Data Sources](#file-system-data-sources)
   - [Database Data Sources](#database-data-sources)
3. [Error Handling Configuration](#error-handling-configuration)
4. [Operation Resolution System](#operation-resolution-system)
5. [Step Types and Dependencies](#step-types-and-dependencies)
6. [Test Data and Domain Model](#test-data-and-domain-model)
   - [OTC Options Trade Domain Model](#otc-options-trade-domain-model)
   - [Test Data Files](#test-data-files)
   - [Financial Terminology](#financial-terminology)
7. [Practical Examples](#practical-examples)
   - [JSON Data Extraction](#json-data-extraction)
   - [XML Data Extraction](#xml-data-extraction)
   - [Advanced Database Extraction](#advanced-database-extraction)
8. [Practical Demo: CSV to H2 Database Pipeline](#practical-demo-csv-to-h2-database-pipeline)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)
11. [Example Configurations](#example-configurations)

## Pipeline Configuration Structure

### Basic Pipeline YAML Structure

```yaml
metadata:                                    # Configuration metadata and identification
  id: "pipeline-id"                        # Unique identifier for this configuration
  name: "Pipeline Name"                    # Human-readable name displayed in logs and UI
  version: "1.0.0"                        # Version number for tracking configuration changes
  type: "pipeline-config"                 # Configuration type: defines a data pipeline

pipeline:                                   # Main pipeline definition
  name: "pipeline-name"                    # Pipeline execution name
  description: "Pipeline description"      # Brief description of pipeline purpose
  
  execution:                              # Execution behavior settings for the entire pipeline
    mode: "sequential"                    # Execute pipeline steps one after another in order (vs "parallel")
    error-handling: "stop-on-error"      # Stop entire pipeline if any step fails (vs "continue-on-error")
    max-retries: 3                        # Retry failed steps up to 3 times before giving up (0 = no retries)
    retry-delay-ms: 1000                  # Wait time between retry attempts in milliseconds

  steps:                                  # List of pipeline steps to execute
    - name: "step-name"                   # Unique name for this step
      type: "extract"                     # Step type: extract, transform, load, audit
      source: "data-source-name"          # References data-sources section
      operation: "operation-name"         # Operation to execute on the source/sink
      description: "Step description"     # Human-readable description
      depends-on: ["previous-step"]       # List of steps that must complete first
      # optional: true                    # CRITICAL: Overrides pipeline-level error-handling!

data-sources:                             # Input data source definitions
  - name: "source-name"                   # Unique identifier for this data source
    type: "file-system"                   # Data source type: file-system, database, api, etc.
    enabled: true                         # Whether this data source is active
    connection:                           # Connection parameters specific to source type
      base-path: "./data"                 # File system: directory path
      file-pattern: "*.csv"               # File system: filename pattern
      encoding: "UTF-8"                   # File system: character encoding
    operations:                           # Named operations available on this source
      getAllData: "SELECT * FROM csv"     # SQL-like query for file sources

## Data Source Types

APEX ETL pipelines support multiple data source types for extracting data. Each source type has specific configuration requirements and capabilities.

### File System Data Sources

File system data sources read data from files in various formats: CSV, JSON, XML, and plain text.

#### Common File System Configuration

```yaml
data-sources:
  - name: "file-source-name"
    type: "file-system"
    enabled: true
    description: "Description of the data source"

    connection:
      base-path: "./data"                 # Directory containing the files
      file-pattern: "*.csv"               # File name pattern (supports wildcards)
      encoding: "UTF-8"                   # Character encoding (default: UTF-8)

    file-format:
      type: "csv"                         # File format: csv, json, xml, text
      # Format-specific options below

    operations:
      operationName: "query or path"      # Named operations for this source
```

#### CSV File Format

```yaml
file-format:
  type: "csv"
  delimiter: ","                          # Column separator (default: comma)
  has-header: true                        # First row contains column names
  quote-char: "\""                        # Quote character for text fields
  escape-char: "\\"                       # Escape character
  skip-lines: 0                           # Number of lines to skip at start

operations:
  getAllRecords: "SELECT * FROM csv"
  getActiveOnly: "SELECT * FROM csv WHERE status = 'ACTIVE'"
```

**Example CSV Data:**
```csv
id,name,email,status,created_date
1,John Smith,john@example.com,ACTIVE,2025-01-15
2,Jane Doe,jane@example.com,INACTIVE,2025-02-20
```

#### JSON File Format

JSON data sources support JSONPath queries for flexible data extraction from complex nested structures.

```yaml
file-format:
  type: "json"
  root-path: "$"                          # JSONPath to root element (default: "$")
  flatten-arrays: false                   # Whether to flatten nested arrays

operations:
  getAllTrades: "$[*]"                    # Extract all array elements
  getActiveTrades: "$[?(@.status == 'ACTIVE')]"  # Filter with JSONPath predicate
  getCallOptions: "$[?(@.optionType == 'Call')]"  # Option type filter
  getHighValueTrades: "$[?(@.notionalAmount > 50000000)]"  # Numeric comparison
```

**Example JSON Data:**
```json
[
  {
    "id": "PROD-001",
    "name": "Gaming Laptop",
    "price": 1299.99,
    "category": "Electronics",
    "active": true,
    "stock": 45,
    "specifications": {
      "cpu": "Intel i7",
      "ram": "16GB",
      "storage": "512GB SSD",
      "gpu": "NVIDIA RTX 3060"
    },
    "tags": ["gaming", "laptop", "high-performance"]
  }
]
```

**JSONPath Query Examples:**
- `$[*]` - All trades
- `$[0]` - First trade
- `$[?(@.status == 'ACTIVE')]` - Active trades only
- `$[?(@.notionalAmount > 50000000)]` - Trades over $50M
- `$[?(@.optionType == 'Call')]` - Call options only
- `$[*].underlyingAsset` - All underlying assets
- `$[*].tags[*]` - All tags from all trades

**Accessing Nested Data:**
When extracted, nested objects and arrays are preserved in the data structure:
```java
Map<String, Object> trade = (Map<String, Object>) extractedData.get(0);
Map<String, Object> underlying = (Map<String, Object>) trade.get("underlyingAsset");
String commodity = (String) underlying.get("commodity");  // "Natural Gas"

List<String> tags = (List<String>) trade.get("tags");
String firstTag = tags.get(0);  // "energy"
```

#### XML File Format

XML data sources parse XML documents and extract elements with support for attributes and nested structures.

```yaml
file-format:
  type: "xml"
  record-element: "trade"                 # XML element representing a record
  include-attributes: true                # Include XML attributes in output
  namespace-aware: false                  # Handle XML namespaces

operations:
  getAllTrades: "SELECT * FROM xml"
  getConfirmedTrades: "SELECT * FROM xml WHERE status='CONFIRMED'"
  getPendingTrades: "SELECT * FROM xml WHERE status='PENDING_CONFIRMATION'"
```

**Example XML Data:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<trades>
    <trade id="OTC-2025-001" status="CONFIRMED">
        <tradeDate>2025-10-15</tradeDate>
        <counterparties>
            <buyer>
                <partyId>GOLDMAN_SACHS</partyId>
                <legalName>Goldman Sachs International</legalName>
                <lei>W22LROWP2IHZNBB6K528</lei>
            </buyer>
        </counterparties>
        <optionDetails>
            <optionType>Call</optionType>
            <underlyingAsset>
                <commodity>Natural Gas</commodity>
                <unit>MMBtu</unit>
                <ticker>NG</ticker>
            </underlyingAsset>
            <strikePrice currency="USD">3.50</strikePrice>
        </optionDetails>
        <riskMetrics>
            <delta>0.65</delta>
            <gamma>0.12</gamma>
        </riskMetrics>
    </trade>
</trades>
```

**Accessing XML Data:**
XML attributes are prefixed with `@` in the extracted data:
```java
Map<String, Object> trade = (Map<String, Object>) extractedData.get(0);

// Attributes (prefixed with @)
String tradeId = (String) trade.get("@id");        // "OTC-2025-001"
String status = (String) trade.get("@status");     // "CONFIRMED"

// Nested elements
Map<String, Object> counterparties = (Map<String, Object>) trade.get("counterparties");
Map<String, Object> buyer = (Map<String, Object>) counterparties.get("buyer");
String partyId = (String) buyer.get("partyId");    // "GOLDMAN_SACHS"
String lei = (String) buyer.get("lei");            // "W22LROWP2IHZNBB6K528"

// Deeply nested elements
Map<String, Object> optionDetails = (Map<String, Object>) trade.get("optionDetails");
Map<String, Object> underlyingAsset = (Map<String, Object>) optionDetails.get("underlyingAsset");
String commodity = (String) underlyingAsset.get("commodity");  // "Natural Gas"
```

### Database Data Sources

Database data sources connect to relational databases using JDBC and execute SQL queries.

#### Basic Database Configuration

```yaml
data-sources:
  - name: "database-source"
    type: "database"
    source-type: "h2"                     # Database type: h2, postgresql, mysql, etc.
    enabled: true
    description: "Database connection for OTC trade data"

    connection:
      database: "./target/database/mydb"  # H2: file path (without .mv.db extension)
      # OR for other databases:
      # url: "jdbc:postgresql://localhost:5432/mydb"
      username: "sa"
      password: ""

    queries:
      getAllTrades: "SELECT * FROM otc_trades"
      getActiveTrades: "SELECT * FROM otc_trades WHERE status = 'ACTIVE'"
```

**Important Configuration Notes:**
- For H2 databases, use `database:` field with file path (APEX adds JDBC URL prefix)
- For other databases, use `url:` field with full JDBC URL
- Always include `source-type:` field to specify database type
- Use `queries:` section (not `operations:`) for SQL statements

#### Advanced Database Queries

Database sources support complex SQL including JOINs, aggregations, and subqueries.

```yaml
data-sources:
  - name: "advanced-db-source"
    type: "database"
    source-type: "h2"
    enabled: true

    connection:
      database: "./target/database/analytics_db"
      username: "sa"
      password: ""

    queries:
      # Complex query with JOIN and aggregations
      getCounterpartyExposure: |
        SELECT
          cp.id as counterparty_id,
          cp.party_id,
          cp.legal_name,
          cp.lei,
          cp.status as counterparty_status,
          COUNT(t.id) as total_trades,
          COALESCE(SUM(t.notional_amount), 0) as total_notional,
          COALESCE(AVG(t.notional_amount), 0) as avg_trade_size,
          MAX(t.trade_date) as last_trade_date
        FROM counterparties cp
        LEFT JOIN otc_trades t ON (cp.party_id = t.buyer_party_id OR cp.party_id = t.seller_party_id)
        GROUP BY cp.id, cp.party_id, cp.legal_name, cp.lei, cp.status
        ORDER BY total_notional DESC

      # Query with multiple JOINs
      getTradeDetails: |
        SELECT
          t.trade_id,
          t.trade_date,
          t.notional_amount,
          buyer.legal_name as buyer_name,
          buyer.lei as buyer_lei,
          seller.legal_name as seller_name,
          seller.lei as seller_lei,
          tp.commodity,
          tp.ticker,
          tp.quantity,
          tp.delta
        FROM otc_trades t
        INNER JOIN counterparties buyer ON t.buyer_party_id = buyer.party_id
        INNER JOIN counterparties seller ON t.seller_party_id = seller.party_id
        LEFT JOIN trade_positions tp ON t.trade_id = tp.trade_id
        ORDER BY t.trade_date DESC

      # Aggregation with HAVING clause
      getTopCounterparties: |
        SELECT
          cp.id,
          cp.legal_name,
          COUNT(t.id) as trade_count,
          SUM(t.notional_amount) as total_notional
        FROM counterparties cp
        LEFT JOIN otc_trades t ON (cp.party_id = t.buyer_party_id OR cp.party_id = t.seller_party_id)
        WHERE cp.status = 'ACTIVE'
        GROUP BY cp.id, cp.legal_name
        HAVING COUNT(t.id) > 5
        ORDER BY total_notional DESC
        LIMIT 10
```

**Supported SQL Features:**
- ✅ SELECT with multiple columns
- ✅ WHERE clauses with complex conditions
- ✅ INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL OUTER JOIN
- ✅ GROUP BY with aggregation functions (COUNT, SUM, AVG, MIN, MAX)
- ✅ HAVING clauses for filtered aggregations
- ✅ ORDER BY with multiple columns and ASC/DESC
- ✅ LIMIT and OFFSET for pagination
- ✅ COALESCE and other SQL functions
- ✅ Subqueries in SELECT, FROM, and WHERE clauses
- ✅ UNION and UNION ALL
- ✅ Common Table Expressions (CTEs) with WITH clause

data-sinks:                               # Output data sink definitions
  - name: "sink-name"                     # Unique identifier for this data sink
    type: "database"                      # Data sink type: database, file-system, api, etc.
    enabled: true                         # Whether this data sink is active
    connection:                           # Connection parameters specific to sink type
      url: "jdbc:h2:./database"          # Database: JDBC connection URL
      driver: "org.h2.Driver"             # Database: JDBC driver class
    operations:                           # Named operations available on this sink
      insertRecord: "INSERT INTO table..."  # SQL statement with parameter binding
```

## Error Handling Configuration

### Pipeline-Level Error Handling

The `execution.error-handling` setting controls how the pipeline responds to step failures:

- **`"stop-on-error"`** (Recommended): Pipeline stops immediately when any step fails
- **`"continue-on-error"`**: Pipeline continues executing remaining steps even if some fail

### Step-Level Error Handling Override

**⚠️ CRITICAL CONFIGURATION PRECEDENCE:**

```yaml
pipeline:
  execution:
    error-handling: "stop-on-error"      # Pipeline-level setting

  steps:
    - name: "critical-step"
      type: "load"
      # optional: true                    # ❌ OVERRIDES pipeline-level error-handling!
      # If uncommented, this step's failure will NOT stop the pipeline
```

**Key Insights:**
- Step-level `optional: true` **overrides** pipeline-level `error-handling: "stop-on-error"`
- Use `optional: true` only for truly optional steps (logging, notifications, etc.)
- For critical steps, omit `optional` to respect pipeline-level error handling

### Retry Configuration

```yaml
execution:
  max-retries: 3                          # Number of retry attempts (0 = no retries)
  retry-delay-ms: 1000                    # Wait time between retries in milliseconds
```

## Operation Resolution System

### Data Source Operations

File system data sources support SQL-like queries:

```yaml
data-sources:
  - name: "csv-input"
    type: "file-system"
    operations:
      getAllRecords: "SELECT * FROM csv"           # Read all columns
      getActiveOnly: "SELECT * FROM csv WHERE status = 'ACTIVE'"  # Filtered query
```

### Data Sink Operations

#### Database Sinks

```yaml
data-sinks:
  - name: "database-output"
    type: "database"
    operations:
      insertTrade: |                      # Multi-line SQL with parameter binding
        INSERT INTO otc_trades (trade_id, buyer_party_id, seller_party_id, trade_date, notional_amount, created_at)
        VALUES (:column_1, :column_2, :column_3, :column_4, :column_5, CURRENT_TIMESTAMP)
```

#### File System Sinks

File system sinks support operation mapping from configured names to built-in operations:

```yaml
data-sinks:
  - name: "audit-log"
    type: "file-system"
    operations:
      writeAuditRecord: "write"           # Maps custom name to built-in "write" operation
      appendLog: "append"                 # Maps custom name to built-in "append" operation
```

**Built-in File System Operations:**
- `write`: Write data to file (overwrites existing content)
- `append`: Append data to existing file
- `overwrite`: Explicitly overwrite file content
- `rotate`: Rotate log files (create new file, archive old)
- `archive`: Archive current file and create new one

### Operation Resolution Process

1. **Direct Match**: Check if operation name directly matches a built-in operation
2. **Configuration Mapping**: Look up operation name in the `operations` configuration
3. **Validation**: Verify that the resolved operation is supported by the sink type
4. **Execution**: Execute the resolved operation with provided data

## Step Types and Dependencies

### Step Types

- **`extract`**: Read data from a data source
- **`transform`**: Process/modify data (requires custom transformation logic)
- **`load`**: Write data to a data sink
- **`audit`**: Write audit/logging records for compliance

### Step Dependencies

```yaml
steps:
  - name: "extract-data"
    type: "extract"
    # No dependencies - runs first

  - name: "load-data"
    type: "load"
    depends-on: ["extract-data"]          # Waits for extract-data to complete

  - name: "audit-logging"
    type: "audit"
    depends-on: ["load-data"]             # Waits for load-data to complete
```

## Test Data and Domain Model

The APEX ETL demo uses realistic **OTC (Over-The-Counter) options trade processing** data to demonstrate middle office trade processing workflows. This section describes the domain model, test data structure, and financial terminology used throughout the examples.

### OTC Options Trade Domain Model

The ETL examples use a comprehensive financial domain model representing OTC derivatives trading:

#### Trade Entity

Core trade information:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `tradeId` | String | Unique trade identifier | "OTC-2025-001" |
| `tradeDate` | Date | Trade execution date | "2025-10-15" |
| `expiryDate` | Date | Option expiry date | "2025-12-28" |
| `optionType` | String | Call or Put option | "Call", "Put" |
| `status` | String | Trade lifecycle status | "ACTIVE", "CONFIRMED", "PENDING_CONFIRMATION", "SETTLED", "EXPIRED" |
| `notionalAmount` | Number | Total trade value (USD) | 75000000 |
| `strikePrice` | Number | Exercise price | 3.50 |
| `premium` | Number | Option premium paid | 450000 |
| `settlementType` | String | Settlement method | "Cash", "Physical" |
| `currency` | String | Currency code | "USD", "EUR" |

#### Counterparties

Trading parties involved in the transaction:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `buyerParty` | String | Buyer identifier | "GOLDMAN_SACHS" |
| `sellerParty` | String | Seller identifier | "JP_MORGAN" |
| `legalName` | String | Full legal entity name | "Goldman Sachs International" |
| `lei` | String | Legal Entity Identifier (20-char) | "W22LROWP2IHZNBB6K528" |

#### Underlying Asset

The commodity or asset underlying the option:

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `commodity` | String | Asset name | "Natural Gas", "Brent Crude Oil", "Gold" |
| `unit` | String | Unit of measurement | "MMBtu", "Barrel", "Troy Ounce" |
| `ticker` | String | Market ticker symbol | "NG", "BZ", "GC" |
| `quantity` | Number | Quantity of underlying | 10000 |

#### Risk Metrics (Greeks)

Option risk sensitivities used for risk management:

| Field | Type | Description | Typical Range |
|-------|------|-------------|---------------|
| `delta` | Number | Price sensitivity to underlying | -1.0 to 1.0 |
| `gamma` | Number | Rate of change of delta | 0.0 to 0.5 |
| `vega` | Number | Sensitivity to volatility | 0 to 500000 |
| `theta` | Number | Time decay (daily P&L) | -10000 to 0 |

### Test Data Files

The ETL demo includes comprehensive test data files demonstrating various data formats:

#### JSON Test Data: otc-options.json

**Location**: `apex-demo/demo-data/json/otc-options.json`
**Records**: 6 OTC option trades
**Format**: JSON array with nested objects

**Commodities Covered**:
- Natural Gas (NG) - Energy
- Brent Crude Oil (BZ) - Energy
- Gold (GC) - Precious Metals
- Silver (SI) - Precious Metals
- Copper (HG) - Industrial Metals
- Wheat (ZW) - Agriculture

**Data Structure Features**:
- ✅ Nested objects (`underlyingAsset`)
- ✅ Arrays (`tags` for categorization)
- ✅ Multiple data types (strings, numbers, dates)
- ✅ Various trade statuses (ACTIVE, EXPIRED)
- ✅ Both Call and Put options
- ✅ Cash and Physical settlement types

**Sample Record**:
```json
{
  "tradeId": "OTC-2025-001",
  "tradeDate": "2025-10-15",
  "buyerParty": "GOLDMAN_SACHS",
  "sellerParty": "JP_MORGAN",
  "optionType": "Call",
  "status": "ACTIVE",
  "notionalAmount": 75000000,
  "underlyingAsset": {
    "commodity": "Natural Gas",
    "unit": "MMBtu",
    "ticker": "NG"
  },
  "strikePrice": 3.50,
  "currency": "USD",
  "expiryDate": "2025-12-28",
  "settlementType": "Cash",
  "premium": 450000,
  "tags": ["energy", "commodity", "otc-derivative"]
}
```

#### XML Test Data: otc-trades.xml

**Location**: `apex-demo/demo-data/xml/otc-trades.xml`
**Records**: 4 OTC trades
**Format**: XML with attributes and nested elements (FpML-style)

**Data Structure Features**:
- ✅ XML attributes (`id`, `status`, `currency`)
- ✅ Multi-level nesting (counterparties → buyer/seller → lei)
- ✅ Complex hierarchies (optionDetails → underlyingAsset)
- ✅ Risk metrics (delta, gamma, vega, theta)
- ✅ LEI codes for regulatory compliance
- ✅ Various trade statuses (CONFIRMED, PENDING_CONFIRMATION, SETTLED)

**Sample Record**:
```xml
<trade id="OTC-2025-001" status="CONFIRMED">
    <tradeDate>2025-10-15</tradeDate>
    <counterparties>
        <buyer>
            <partyId>GOLDMAN_SACHS</partyId>
            <legalName>Goldman Sachs International</legalName>
            <lei>W22LROWP2IHZNBB6K528</lei>
        </buyer>
        <seller>
            <partyId>JP_MORGAN</partyId>
            <legalName>J.P. Morgan Securities LLC</legalName>
            <lei>8I5DZWZKVSZI1NUHU748</lei>
        </seller>
    </counterparties>
    <optionDetails>
        <optionType>Call</optionType>
        <underlyingAsset>
            <commodity>Natural Gas</commodity>
            <unit>MMBtu</unit>
            <ticker>NG</ticker>
        </underlyingAsset>
        <strikePrice currency="USD">3.50</strikePrice>
        <notionalQuantity>10000</notionalQuantity>
    </optionDetails>
    <riskMetrics>
        <delta>0.65</delta>
        <gamma>0.12</gamma>
        <vega>125000</vega>
        <theta>-5000</theta>
    </riskMetrics>
</trade>
```

### Financial Terminology

Understanding these terms is essential for working with the OTC options examples:

#### Trading Terms

- **OTC (Over-The-Counter)**: Trades executed directly between two parties, not on a centralized exchange. Allows for customized contract terms.
- **Call Option**: Gives the buyer the right (but not obligation) to buy the underlying asset at the strike price before expiry.
- **Put Option**: Gives the buyer the right (but not obligation) to sell the underlying asset at the strike price before expiry.
- **Strike Price**: The predetermined price at which the option can be exercised.
- **Premium**: The upfront payment made by the option buyer to the seller for the right to exercise.
- **Notional Amount**: The total value of the trade, calculated as quantity × price.
- **Expiry Date**: The date when the option expires and can no longer be exercised.

#### Settlement Terms

- **Cash Settlement**: Financial settlement where the difference between strike and market price is paid in cash.
- **Physical Settlement**: Actual delivery of the underlying commodity occurs upon exercise.

#### Regulatory Terms

- **LEI (Legal Entity Identifier)**: A 20-character alphanumeric code that uniquely identifies legal entities participating in financial transactions. Required for regulatory reporting (EMIR, Dodd-Frank).
- **Counterparty**: The other party in a financial transaction (buyer or seller).

#### Risk Management Terms (Greeks)

- **Delta (Δ)**: Measures how much the option price changes for a $1 change in the underlying asset price. Range: -1 to +1 (Call options: 0 to 1, Put options: -1 to 0).
- **Gamma (Γ)**: Measures the rate of change of delta. Higher gamma means delta changes more rapidly.
- **Vega (ν)**: Measures sensitivity to volatility. Shows how much the option price changes for a 1% change in implied volatility.
- **Theta (Θ)**: Measures time decay. Shows how much value the option loses each day as it approaches expiry (usually negative).

#### Middle Office Functions

The test data supports these middle office workflows:

1. **Trade Capture**: Extracting trade data from various sources (JSON feeds, XML confirmations)
2. **Confirmation Matching**: Validating counterparty details and trade terms against confirmations
3. **Risk Management**: Processing risk metrics (Greeks) for portfolio risk analysis
4. **Settlement Processing**: Handling cash vs physical settlement workflows
5. **Regulatory Reporting**: Extracting LEI codes and trade details for compliance reporting
6. **Lifecycle Management**: Tracking trades through statuses (PENDING → CONFIRMED → SETTLED)

### ETL Patterns Demonstrated

The test data and examples demonstrate comprehensive ETL patterns applicable to financial data processing:

#### Data Extraction Patterns

**1. JSON with JSONPath Filtering**
- Extract all trades: `$[*]`
- Filter by status: `$[?(@.status == 'ACTIVE')]`
- Filter by option type: `$[?(@.optionType == 'Call')]`
- Filter by notional amount: `$[?(@.notionalAmount > 50000000)]`
- Extract specific fields: `$[*].underlyingAsset`

**2. XML with Attributes and Elements**
- Extract trade IDs from attributes: `@id`
- Extract trade status from attributes: `@status`
- Navigate nested structures: `counterparties → buyer → lei`
- Access deeply nested data: `optionDetails → underlyingAsset → commodity`
- Extract risk metrics: `riskMetrics → delta/gamma/vega/theta`

**3. Database Queries with Complex JOINs**
- Counterparty exposure analysis with LEFT JOIN
- Trade details with INNER JOIN on multiple tables
- Aggregations with GROUP BY (COUNT, SUM, AVG, MAX)
- Conditional aggregation with CASE statements
- Risk aggregation across positions

**4. Nested Data Access**
- JSON nested objects: `trade.underlyingAsset.commodity`
- XML multi-level nesting: `trade.counterparties.buyer.lei`
- Array processing: `trade.tags[*]`
- Type preservation: Numbers, strings, dates, booleans

#### Data Transformation Patterns

**1. Enrichment**
- Calculate net delta: `SUM(delta * quantity)`
- Calculate total exposure: `SUM(notional_amount)`
- Derive buy-side vs sell-side notional
- Add calculated risk metrics

**2. Filtering and Selection**
- Filter by trade status (ACTIVE, CONFIRMED, SETTLED)
- Filter by commodity type (Energy, Precious Metals, Agriculture)
- Filter by settlement method (Cash, Physical)
- Filter by option type (Call, Put)
- Filter by notional threshold

**3. Aggregation**
- Counterparty exposure summaries
- Commodity risk aggregation
- Trade volume analysis
- Average trade size calculations
- Risk metrics rollup (total vega, net delta)

**4. Normalization**
- Convert between JSON and XML representations
- Standardize date formats
- Normalize party identifiers
- Map status codes

#### Data Loading Patterns

**1. Batch Operations**
- Bulk insert of trade records
- Batch size configuration (50 records per batch)
- Transaction-per-batch mode
- Performance metrics collection

**2. Upsert Operations**
- MERGE INTO for database sinks
- Update existing trades or insert new ones
- Key-based conflict resolution
- Timestamp tracking (created_at, updated_at)

**3. Error Handling**
- Dead letter queues for failed records
- Retry mechanisms with exponential backoff
- Circuit breaker for connection failures
- Error logging and audit trails

**4. Audit and Compliance**
- Audit log for all data movements
- Track processing timestamps
- Maintain data lineage
- Support regulatory reporting requirements

#### Real-World Use Cases

The patterns support these financial industry scenarios:

**Trade Lifecycle Management**:
```
JSON Feed → Extract → Validate → Enrich → Load to Database → Audit
```

**Confirmation Matching**:
```
XML Confirmation → Extract → Match with Internal Trade → Update Status → Notify
```

**Risk Aggregation**:
```
Database Extract → Calculate Net Delta → Aggregate by Commodity → Load to Risk System
```

**Regulatory Reporting**:
```
Extract Trades with LEI → Filter by Date Range → Transform to Regulatory Format → Submit
```

**Counterparty Exposure**:
```
Database Query (JOINs) → Aggregate Notional → Calculate Limits → Alert on Breach
```

## Practical Examples

This section provides complete, working examples of ETL pipelines using different data source types.

### JSON Data Extraction

This example demonstrates extracting OTC options trade data from a JSON file with nested structures.

#### Use Case
Extract OTC (Over-The-Counter) options trade data from a JSON file for middle office trade processing, including trade details, counterparties, option specifications, and underlying asset information.

#### Complete YAML Configuration

```yaml
metadata:
  id: "json-otc-extract-pipeline"
  name: "JSON OTC Options Extract Test Pipeline"
  type: "pipeline-config"
  version: "1.0.0"
  description: "Extract OTC options trade data from JSON file for middle office processing"

pipeline:
  name: "json-otc-extract-pipeline"
  description: "Extract OTC options trades from JSON for confirmation and settlement"

  execution:
    mode: "sequential"
    error-handling: "stop-on-error"
    max-retries: 0
    retry-delay-ms: 0

  steps:
    - name: "extract-otc-options"
      type: "extract"
      source: "json-otc-options-source"
      operation: "getAllTrades"
      description: "Extract all OTC option trade records from JSON file"

data-sources:
  - name: "json-otc-options-source"
    type: "file-system"
    enabled: true
    description: "JSON file containing OTC options trade data from trading systems"

    connection:
      base-path: "./demo-data/json"
      file-pattern: "otc-options.json"
      encoding: "UTF-8"

    file-format:
      type: "json"
      root-path: "$"                      # Start at root of JSON document
      flatten-arrays: false               # Preserve array structures

    operations:
      getAllTrades: "$[*]"                # Extract all trades
      getActiveTrades: "$[?(@.status == 'ACTIVE')]"  # Only active trades
      getCallOptions: "$[?(@.optionType == 'Call')]"  # Call options only
      getPutOptions: "$[?(@.optionType == 'Put')]"    # Put options only
      getHighValueTrades: "$[?(@.notionalAmount > 50000000)]"  # Trades > $50M
```

#### Sample JSON Data (otc-options.json)

```json
[
  {
    "tradeId": "OTC-2025-001",
    "tradeDate": "2025-10-15",
    "buyerParty": "GOLDMAN_SACHS",
    "sellerParty": "JP_MORGAN",
    "optionType": "Call",
    "status": "ACTIVE",
    "notionalAmount": 75000000,
    "underlyingAsset": {
      "commodity": "Natural Gas",
      "unit": "MMBtu",
      "ticker": "NG"
    },
    "strikePrice": 3.50,
    "currency": "USD",
    "expiryDate": "2025-12-28",
    "settlementType": "Cash",
    "premium": 450000,
    "tags": ["energy", "commodity", "otc-derivative"]
  },
  {
    "tradeId": "OTC-2025-002",
    "tradeDate": "2025-10-16",
    "buyerParty": "MORGAN_STANLEY",
    "sellerParty": "CITIGROUP",
    "optionType": "Put",
    "status": "ACTIVE",
    "notionalAmount": 120000000,
    "underlyingAsset": {
      "commodity": "Brent Crude Oil",
      "unit": "Barrel",
      "ticker": "BZ"
    },
    "strikePrice": 85.00,
    "currency": "USD",
    "expiryDate": "2025-11-30",
    "settlementType": "Physical",
    "premium": 850000,
    "tags": ["energy", "commodity", "otc-derivative"]
  }
]
```

#### Java Code to Execute Pipeline

```java
@Test
void shouldExtractDataFromJsonFile() throws Exception {
    // Load pipeline configuration
    String configPath = "PipelineEtlExecutionTestExtractJson.yaml";
    YamlRuleConfiguration config = yamlLoader.loadFromFile(configPath);

    // Initialize and execute pipeline
    pipelineEngine.initialize(config);
    YamlPipelineExecutionResult result = pipelineEngine.executePipeline("json-otc-extract-pipeline");

    // Verify extraction results
    assertTrue(result.isSuccess(), "Pipeline should execute successfully");

    var extractResult = result.getStepResults().get(0);
    assertEquals("extract-otc-options", extractResult.getStepName());

    // Access extracted data
    @SuppressWarnings("unchecked")
    List<Object> trades = (List<Object>) extractResult.getData();
    assertEquals(6, trades.size(), "Should extract 6 OTC option trades");

    // Access first trade
    @SuppressWarnings("unchecked")
    Map<String, Object> firstTrade = (Map<String, Object>) trades.get(0);
    assertEquals("OTC-2025-001", firstTrade.get("tradeId"));
    assertEquals("GOLDMAN_SACHS", firstTrade.get("buyerParty"));
    assertEquals("JP_MORGAN", firstTrade.get("sellerParty"));
    assertEquals("Call", firstTrade.get("optionType"));

    // Access nested underlying asset
    @SuppressWarnings("unchecked")
    Map<String, Object> underlying = (Map<String, Object>) firstTrade.get("underlyingAsset");
    assertNotNull(underlying, "Trade should have underlyingAsset");
    assertEquals("Natural Gas", underlying.get("commodity"));
    assertEquals("MMBtu", underlying.get("unit"));
    assertEquals("NG", underlying.get("ticker"));

    // Access tags array
    @SuppressWarnings("unchecked")
    List<String> tags = (List<String>) firstTrade.get("tags");
    assertNotNull(tags, "Trade should have tags");
    assertTrue(tags.contains("otc-derivative"));
}
```

#### Expected Output

```
Pipeline execution started: json-otc-extract-pipeline
Executing step: extract-otc-options (extract)
  Source: json-otc-options-source
  Operation: getAllTrades
  Reading JSON file: ./demo-data/json/otc-options.json
  Extracted 6 OTC option trades
Step completed: extract-otc-options (SUCCESS)
Pipeline execution completed: SUCCESS
Total steps: 1, Successful: 1, Failed: 0
```

#### Key Features Demonstrated

✅ **JSONPath Queries**: Flexible data extraction with predicates for filtering trades
✅ **Nested Objects**: UnderlyingAsset object preserved in structure
✅ **Arrays**: Tags array maintained as list for categorization
✅ **Type Preservation**: Numbers (notionalAmount, strikePrice), strings, correctly typed
✅ **Multiple Operations**: Different queries on same data source (by status, optionType, notionalAmount)
✅ **Financial Data**: Realistic OTC options trade data for middle office processing

### XML Data Extraction

This example demonstrates extracting OTC trade data from an XML file with complex nested structures, attributes, and counterparty information.

#### Use Case
Extract OTC trade confirmation data from an XML file (FpML-style) containing trade details, counterparty information, option specifications, and risk metrics for middle office confirmation matching and settlement processing.

#### Complete YAML Configuration

```yaml
metadata:
  id: "xml-otc-extract-pipeline"
  name: "XML OTC Trades Extract Test Pipeline"
  type: "pipeline-config"
  version: "1.0.0"
  description: "Extract OTC trade confirmation data from XML file"

pipeline:
  name: "xml-otc-extract-pipeline"
  description: "Extract OTC trade confirmations from XML for matching and settlement"

  execution:
    mode: "sequential"
    error-handling: "stop-on-error"
    max-retries: 0
    retry-delay-ms: 0

  steps:
    - name: "extract-otc-trades"
      type: "extract"
      source: "xml-otc-trades-source"
      operation: "getAllTrades"
      description: "Extract all OTC trade records from XML confirmation file"

data-sources:
  - name: "xml-otc-trades-source"
    type: "file-system"
    enabled: true
    description: "XML file containing OTC trade confirmation data (FpML-style)"

    connection:
      base-path: "./demo-data/xml"
      file-pattern: "otc-trades.xml"
      encoding: "UTF-8"

    file-format:
      type: "xml"
      record-element: "trade"             # Each <trade> element is a record
      include-attributes: true            # Include XML attributes (id, status)

    operations:
      getAllTrades: "SELECT * FROM xml"
      getConfirmedTrades: "SELECT * FROM xml WHERE status='CONFIRMED'"
      getPendingTrades: "SELECT * FROM xml WHERE status='PENDING_CONFIRMATION'"
      getSettledTrades: "SELECT * FROM xml WHERE status='SETTLED'"
```

#### Sample XML Data (otc-trades.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<trades>
    <trade id="OTC-2025-001" status="CONFIRMED">
        <tradeDate>2025-10-15</tradeDate>
        <counterparties>
            <buyer>
                <partyId>GOLDMAN_SACHS</partyId>
                <legalName>Goldman Sachs International</legalName>
                <lei>W22LROWP2IHZNBB6K528</lei>
            </buyer>
            <seller>
                <partyId>JP_MORGAN</partyId>
                <legalName>J.P. Morgan Securities LLC</legalName>
                <lei>8I5DZWZKVSZI1NUHU748</lei>
            </seller>
        </counterparties>
        <optionDetails>
            <optionType>Call</optionType>
            <underlyingAsset>
                <commodity>Natural Gas</commodity>
                <unit>MMBtu</unit>
                <ticker>NG</ticker>
            </underlyingAsset>
            <strikePrice currency="USD">3.50</strikePrice>
            <notionalQuantity>10000</notionalQuantity>
        </optionDetails>
        <expiryDate>2025-12-28</expiryDate>
        <settlementType>Cash</settlementType>
        <premium currency="USD">450000</premium>
        <riskMetrics>
            <delta>0.65</delta>
            <gamma>0.12</gamma>
            <vega>125000</vega>
            <theta>-5000</theta>
        </riskMetrics>
    </trade>
</trades>
```

#### Java Code to Execute Pipeline

```java
@Test
void shouldExtractDataFromXmlFile() throws Exception {
    // Load pipeline configuration
    String configPath = "PipelineEtlExecutionTestExtractXml.yaml";
    YamlRuleConfiguration config = yamlLoader.loadFromFile(configPath);

    // Initialize and execute pipeline
    pipelineEngine.initialize(config);
    YamlPipelineExecutionResult result = pipelineEngine.executePipeline("xml-otc-extract-pipeline");

    // Verify extraction results
    assertTrue(result.isSuccess(), "Pipeline should execute successfully");

    var extractResult = result.getStepResults().get(0);
    assertEquals("extract-otc-trades", extractResult.getStepName());

    // Access extracted data
    @SuppressWarnings("unchecked")
    List<Object> trades = (List<Object>) extractResult.getData();
    assertEquals(4, trades.size(), "Should extract 4 OTC trades");

    // Access first trade
    @SuppressWarnings("unchecked")
    Map<String, Object> firstTrade = (Map<String, Object>) trades.get(0);

    // XML attributes are prefixed with @
    assertEquals("OTC-2025-001", firstTrade.get("@id"));
    assertEquals("CONFIRMED", firstTrade.get("@status"));

    // Access nested counterparties object
    @SuppressWarnings("unchecked")
    Map<String, Object> counterparties = (Map<String, Object>) firstTrade.get("counterparties");
    @SuppressWarnings("unchecked")
    Map<String, Object> buyer = (Map<String, Object>) counterparties.get("buyer");
    assertEquals("GOLDMAN_SACHS", buyer.get("partyId"));
    assertEquals("W22LROWP2IHZNBB6K528", buyer.get("lei"));

    // Access deeply nested option details and underlying asset
    @SuppressWarnings("unchecked")
    Map<String, Object> optionDetails = (Map<String, Object>) firstTrade.get("optionDetails");
    @SuppressWarnings("unchecked")
    Map<String, Object> underlyingAsset = (Map<String, Object>) optionDetails.get("underlyingAsset");
    assertEquals("Natural Gas", underlyingAsset.get("commodity"));
    assertEquals("NG", underlyingAsset.get("ticker"));

    // Access risk metrics
    @SuppressWarnings("unchecked")
    Map<String, Object> riskMetrics = (Map<String, Object>) firstTrade.get("riskMetrics");
    assertEquals("0.65", riskMetrics.get("delta"));
}
```

#### Expected Output

```
Pipeline execution started: xml-otc-extract-pipeline
Executing step: extract-otc-trades (extract)
  Source: xml-otc-trades-source
  Operation: getAllTrades
  Reading XML file: ./demo-data/xml/otc-trades.xml
  Parsing XML with record element: trade
  Extracted 4 OTC trades
Step completed: extract-otc-trades (SUCCESS)
Pipeline execution completed: SUCCESS
Total steps: 1, Successful: 1, Failed: 0
```

#### Key Features Demonstrated

✅ **XML Attributes**: Attributes prefixed with `@` in data structure (trade id, status)
✅ **Nested Elements**: Multi-level nesting (trade → counterparties → buyer → lei)
✅ **Record Element**: Specify which XML element represents a record (trade)
✅ **Complex Structures**: Counterparties, option details, underlying asset, and risk metrics
✅ **Filtering**: Query operations for different trade statuses (CONFIRMED, PENDING, SETTLED)
✅ **Financial Data**: LEI codes, risk Greeks, and OTC trade confirmation data

### Advanced Database Extraction

This example demonstrates complex database queries with JOINs, aggregations, and analytical functions for OTC trade processing.

#### Use Case
Extract OTC trade analytics from a relational database with multiple tables, calculating counterparty exposure, trade volumes, and risk aggregations for middle office reporting and risk management.

#### Database Schema

```sql
-- Counterparties table
CREATE TABLE counterparties (
  id INTEGER PRIMARY KEY,
  party_id VARCHAR(50) UNIQUE NOT NULL,
  legal_name VARCHAR(255) NOT NULL,
  lei VARCHAR(20) UNIQUE,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- OTC Trades table
CREATE TABLE otc_trades (
  id INTEGER PRIMARY KEY,
  trade_id VARCHAR(50) UNIQUE NOT NULL,
  buyer_party_id VARCHAR(50) NOT NULL,
  seller_party_id VARCHAR(50) NOT NULL,
  trade_date DATE NOT NULL,
  expiry_date DATE NOT NULL,
  option_type VARCHAR(10) NOT NULL,  -- 'Call' or 'Put'
  status VARCHAR(50) DEFAULT 'PENDING',
  notional_amount DECIMAL(18, 2) NOT NULL,
  strike_price DECIMAL(18, 4) NOT NULL,
  premium DECIMAL(18, 2) NOT NULL,
  settlement_type VARCHAR(20) NOT NULL,  -- 'Cash' or 'Physical'
  FOREIGN KEY (buyer_party_id) REFERENCES counterparties(party_id),
  FOREIGN KEY (seller_party_id) REFERENCES counterparties(party_id)
);

-- Trade positions table (for risk aggregation)
CREATE TABLE trade_positions (
  id INTEGER PRIMARY KEY,
  trade_id VARCHAR(50) NOT NULL,
  commodity VARCHAR(100) NOT NULL,
  unit VARCHAR(50) NOT NULL,
  ticker VARCHAR(10) NOT NULL,
  quantity DECIMAL(18, 4) NOT NULL,
  delta DECIMAL(10, 6),
  gamma DECIMAL(10, 6),
  vega DECIMAL(18, 2),
  theta DECIMAL(18, 2),
  FOREIGN KEY (trade_id) REFERENCES otc_trades(trade_id)
);
```

#### Complete YAML Configuration

```yaml
metadata:
  id: "advanced-otc-database-extract-pipeline"
  name: "Advanced OTC Database Extract Pipeline"
  type: "pipeline-config"
  version: "1.0.0"
  description: "Extract OTC trade analytics with complex queries for risk and exposure reporting"

pipeline:
  name: "advanced-otc-database-extract-pipeline"
  description: "Extract counterparty exposure and trade analytics"

  execution:
    mode: "sequential"
    error-handling: "stop-on-error"
    max-retries: 0
    retry-delay-ms: 0

  steps:
    - name: "extract-counterparty-exposure"
      type: "extract"
      source: "advanced-otc-db-source"
      operation: "getCounterpartyExposure"
      description: "Extract counterparty exposure summary with aggregations"

data-sources:
  - name: "advanced-otc-db-source"
    type: "database"
    source-type: "h2"
    enabled: true
    description: "H2 database with OTC trade and counterparty data"

    connection:
      database: "./target/test/etl/database/advanced_otc_test_db"
      username: "sa"
      password: ""

    queries:
      # Complex query with JOIN and aggregations for counterparty exposure
      getCounterpartyExposure: |
        SELECT
          cp.id as counterparty_id,
          cp.party_id,
          cp.legal_name,
          cp.lei,
          cp.status as counterparty_status,
          COUNT(DISTINCT t.id) as total_trades,
          COALESCE(SUM(CASE WHEN t.buyer_party_id = cp.party_id THEN t.notional_amount ELSE 0 END), 0) as buy_side_notional,
          COALESCE(SUM(CASE WHEN t.seller_party_id = cp.party_id THEN t.notional_amount ELSE 0 END), 0) as sell_side_notional,
          COALESCE(SUM(t.notional_amount), 0) as total_notional,
          COALESCE(AVG(t.notional_amount), 0) as avg_trade_size,
          MAX(t.trade_date) as last_trade_date
        FROM counterparties cp
        LEFT JOIN otc_trades t ON (cp.party_id = t.buyer_party_id OR cp.party_id = t.seller_party_id)
        GROUP BY cp.id, cp.party_id, cp.legal_name, cp.lei, cp.status
        ORDER BY total_notional DESC

      # Query for trade details with counterparty and position information
      getTradeDetails: |
        SELECT
          t.trade_id,
          t.trade_date,
          t.expiry_date,
          t.option_type,
          t.status,
          t.notional_amount,
          t.strike_price,
          t.premium,
          t.settlement_type,
          buyer.legal_name as buyer_name,
          buyer.lei as buyer_lei,
          seller.legal_name as seller_name,
          seller.lei as seller_lei,
          tp.commodity,
          tp.ticker,
          tp.quantity,
          tp.delta,
          tp.gamma,
          tp.vega,
          tp.theta
        FROM otc_trades t
        INNER JOIN counterparties buyer ON t.buyer_party_id = buyer.party_id
        INNER JOIN counterparties seller ON t.seller_party_id = seller.party_id
        LEFT JOIN trade_positions tp ON t.trade_id = tp.trade_id
        ORDER BY t.trade_date DESC

      # Commodity exposure aggregation
      getCommodityExposure: |
        SELECT
          tp.commodity,
          tp.ticker,
          COUNT(DISTINCT t.id) as trade_count,
          SUM(tp.quantity) as total_quantity,
          SUM(t.notional_amount) as total_notional,
          AVG(t.strike_price) as avg_strike_price,
          SUM(tp.delta * tp.quantity) as net_delta,
          SUM(tp.vega) as total_vega
        FROM trade_positions tp
        INNER JOIN otc_trades t ON tp.trade_id = t.trade_id
        WHERE t.status IN ('ACTIVE', 'CONFIRMED')
        GROUP BY tp.commodity, tp.ticker
        ORDER BY total_notional DESC
```

#### Java Code to Execute Pipeline

```java
@Test
void shouldExtractCounterpartyExposure() throws Exception {
    // Setup database with test data
    setupAdvancedOtcDatabase();

    // Load pipeline configuration
    String configPath = "PipelineEtlExecutionTestExtractDatabaseAdvanced.yaml";
    YamlRuleConfiguration config = yamlLoader.loadFromFile(configPath);

    // Initialize and execute pipeline
    pipelineEngine.initialize(config);
    YamlPipelineExecutionResult result = pipelineEngine.executePipeline("advanced-otc-database-extract-pipeline");

    // Verify extraction results
    assertTrue(result.isSuccess(), "Pipeline should execute successfully");

    var extractResult = result.getStepResults().get(0);
    assertEquals("extract-counterparty-exposure", extractResult.getStepName());

    // Access extracted data
    @SuppressWarnings("unchecked")
    List<Object> exposureSummaries = (List<Object>) extractResult.getData();
    assertTrue(exposureSummaries.size() > 0, "Should extract counterparty exposure summaries");

    // Verify aggregated data
    @SuppressWarnings("unchecked")
    Map<String, Object> firstCounterparty = (Map<String, Object>) exposureSummaries.get(0);

    assertNotNull(firstCounterparty.get("counterparty_id"));
    assertNotNull(firstCounterparty.get("party_id"));
    assertNotNull(firstCounterparty.get("legal_name"));
    assertNotNull(firstCounterparty.get("lei"));
    assertNotNull(firstCounterparty.get("total_trades"));
    assertNotNull(firstCounterparty.get("total_notional"));
    assertNotNull(firstCounterparty.get("buy_side_notional"));
    assertNotNull(firstCounterparty.get("sell_side_notional"));

    // Verify aggregation calculations
    int totalTrades = ((Number) firstCounterparty.get("total_trades")).intValue();
    double totalNotional = ((Number) firstCounterparty.get("total_notional")).doubleValue();
    double avgTradeSize = ((Number) firstCounterparty.get("avg_trade_size")).doubleValue();

    assertTrue(totalTrades >= 0, "Total trades should be non-negative");
    assertTrue(totalNotional >= 0, "Total notional should be non-negative");

    if (totalTrades > 0) {
        assertTrue(avgTradeSize > 0, "Average trade size should be positive");
    }
}

private void setupAdvancedOtcDatabase() {
    String jdbcUrl = "jdbc:h2:./target/test/etl/database/advanced_otc_test_db";

    try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
        Statement statement = connection.createStatement();

        // Create tables
        statement.execute("CREATE TABLE IF NOT EXISTS counterparties (...)");
        statement.execute("CREATE TABLE IF NOT EXISTS otc_trades (...)");
        statement.execute("CREATE TABLE IF NOT EXISTS trade_positions (...)");

        // Insert test data
        statement.execute("INSERT INTO counterparties VALUES (1, 'GOLDMAN_SACHS', 'Goldman Sachs International', 'W22LROWP2IHZNBB6K528', ...)");
        statement.execute("INSERT INTO otc_trades VALUES (1, 'OTC-2025-001', 'GOLDMAN_SACHS', 'JP_MORGAN', '2025-10-15', ...)");
        statement.execute("INSERT INTO trade_positions VALUES (1, 'OTC-2025-001', 'Natural Gas', 'MMBtu', 'NG', 10000, 0.65, ...)");
    }
}
```

#### Expected Output

```
Pipeline execution started: advanced-otc-database-extract-pipeline
Executing step: extract-counterparty-exposure (extract)
  Source: advanced-otc-db-source
  Operation: getCounterpartyExposure
  Connecting to database: ./target/test/etl/database/advanced_otc_test_db
  Executing query with JOIN and aggregations
  Extracted 3 counterparty exposure summaries
Step completed: extract-counterparty-exposure (SUCCESS)
Pipeline execution completed: SUCCESS
Total steps: 1, Successful: 1, Failed: 0

Sample extracted data:
{
  "counterparty_id": 1,
  "party_id": "GOLDMAN_SACHS",
  "legal_name": "Goldman Sachs International",
  "lei": "W22LROWP2IHZNBB6K528",
  "counterparty_status": "ACTIVE",
  "total_trades": 4,
  "buy_side_notional": 195000000.00,
  "sell_side_notional": 85000000.00,
  "total_notional": 280000000.00,
  "avg_trade_size": 70000000.00,
  "last_trade_date": "2025-10-20"
}
```

#### Key Features Demonstrated

✅ **Complex JOINs**: LEFT JOIN to include counterparties without trades, INNER JOIN for trade details
✅ **Aggregation Functions**: COUNT, SUM, AVG, MAX for exposure calculations
✅ **GROUP BY**: Grouping by multiple columns (counterparty, commodity)
✅ **COALESCE**: Handle NULL values in aggregations for zero-trade counterparties
✅ **CASE Statements**: Conditional aggregation for buy-side vs sell-side notional
✅ **Multiple Queries**: Different analytical queries (exposure, trade details, commodity risk)
✅ **ORDER BY**: Sort results by calculated columns (total_notional DESC)
✅ **Calculated Columns**: Derive new columns from aggregations (net_delta, total_vega)
✅ **Financial Analytics**: Counterparty exposure, commodity risk, and trade volume analysis

## Practical Demo: CSV to H2 Database Pipeline

This demo showcases APEX's new data sink capabilities by implementing a complete ETL (Extract, Transform, Load) pipeline that processes CSV customer data and writes it to an H2 database.

### Demo Overview

The demo demonstrates:

- **YAML Configuration**: Complete pipeline configuration using APEX YAML syntax
- **Data Sinks**: Database output using the new DataSink framework
- **Error Handling**: Comprehensive error handling and retry mechanisms
- **Batch Processing**: Efficient bulk operations with configurable batch sizes
- **Schema Management**: Automatic database table creation and management
- **Monitoring**: Metrics collection and performance monitoring

### Architecture

```
CSV Data → APEX Processing → H2 Database
    ↓           ↓              ↓
Sample     Enrichment/     customer_database
Records    Validation      + audit_log.json
```

### Demo Components

#### 1. CsvToH2PipelineDemo.java
Main demo class that orchestrates the ETL process:
- Sets up demo environment and sample data
- Loads YAML configuration
- Creates and initializes data sinks
- Processes sample customer records
- Demonstrates both individual and batch operations
- Collects and displays metrics

#### 2. csv-to-h2-pipeline.yaml
Complete APEX configuration demonstrating:
- **Data Sink Configuration**: H2 database with connection pooling
- **SQL Operations**: INSERT, UPDATE, UPSERT operations
- **Schema Management**: Auto-creation of customer table with indexes
- **Error Handling**: Retry strategies and dead letter handling
- **Batch Processing**: Optimized bulk operations
- **Health Checks**: Connection monitoring and circuit breaker
- **Audit Logging**: Secondary file-based data sink for audit trail

#### 3. Sample Data
The demo creates sample customer records with:
- Customer ID, Name, Email
- Registration dates and status
- Processing timestamps

### Configuration Highlights

#### Database Connection
```yaml
connection:
  database: "./target/demo/etl/output/customer_database"
  username: "sa"
  password: ""
  mode: "PostgreSQL"
  connection-pool:
    max-size: 10
    min-size: 2
    connection-timeout: 30000
```

#### SQL Operations
```yaml
operations:
  insertCustomer: |
    INSERT INTO customers (customer_id, customer_name, email, status, processed_at, created_at)
    VALUES (:id, :customerName, :email, :status, :processedAt, CURRENT_TIMESTAMP)

  upsertCustomer: |
    MERGE INTO customers (customer_id, customer_name, email, status, processed_at)
    KEY (customer_id) VALUES (:id, :customerName, :email, :status, :processedAt)
```

#### Error Handling
```yaml
error-handling:
  strategy: "log-and-continue"
  max-retries: 3
  retry-delay: 1000
  dead-letter-enabled: true
  dead-letter-table: "failed_customer_records"
```

#### Batch Processing
```yaml
batch:
  enabled: true
  batch-size: 50
  timeout-ms: 10000
  transaction-mode: "per-batch"
  enable-metrics: true
```

### Running the Demo

#### Prerequisites
- Java 21+
- Maven 3.6+
- APEX Core library

#### Execution
```bash
# From the apex-demo directory
mvn compile exec:java -Dexec.mainClass="dev.mars.apex.demo.etl.CsvToH2PipelineDemo"
```

#### Expected Output
```
=== APEX ETL Demo: CSV to H2 Database Pipeline ===
Setting up demo environment...
✓ Created sample CSV file: ./target/demo/etl/data/customers.csv
✓ Demo environment setup complete
Loading YAML configuration...
✓ Configuration loaded: CSV to H2 ETL Pipeline Demo
✓ Data sinks configured: 2
Creating data sink...
✓ Data sink created and initialized: customer-h2-database
✓ Sink type: Database
✓ Connection status: CONNECTED
Processing sample data...
Processing 5 customer records individually...
Processing additional records in batch...
✓ Batch processed 5 records successfully
✓ Sample data processing complete
Verifying ETL results...
=== ETL Processing Metrics ===
Total writes: 6
Successful writes: 6
Failed writes: 0
Total batches: 1
Successful batches: 1
Total records written: 10
Write success rate: 100.00%
Average write time: 15.50ms
Data sink health status: HEALTHY
✓ ETL process completed successfully with 10 records processed
=== Demo completed successfully ===
```

### Generated Artifacts

After running the demo, you'll find:

#### Database Files
- `./target/demo/etl/output/customer_database.mv.db` - H2 database file
- `./target/demo/etl/output/customer_database.trace.db` - H2 trace file (if enabled)

#### Audit Files
- `./target/demo/etl/output/audit/customer_audit_*.json` - Audit log files

#### Sample Data
- `./target/demo/etl/data/customers.csv` - Generated sample CSV data

### Database Schema

The demo creates a `customers` table with the following structure:

```sql
CREATE TABLE customers (
  customer_id INTEGER PRIMARY KEY,
  customer_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE,
  status VARCHAR(50) DEFAULT 'ACTIVE',
  processed_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Key Features Demonstrated

#### 1. YAML Configuration Integration
- Complete pipeline configuration in YAML
- APEX syntax compliance with kebab-case properties
- Reuse of existing configuration patterns

#### 2. Data Sink Framework
- Database data sink with H2 support
- File system data sink for audit logging
- Factory pattern for sink creation

#### 3. Error Handling & Resilience
- Retry mechanisms with exponential backoff
- Dead letter handling for failed records
- Circuit breaker for connection failures

#### 4. Performance Optimization
- Connection pooling for database efficiency
- Batch processing for high throughput
- Memory management and monitoring

#### 5. Monitoring & Observability
- Comprehensive metrics collection
- Health checks and status monitoring
- Performance timing and success rates

### Extending the Demo

This demo can be extended to showcase additional APEX features:

1. **Data Sources**: Add CSV file input using APEX data sources
2. **Rule Processing**: Add business rules for data validation
3. **Enrichments**: Implement data transformation rules
4. **Multiple Outputs**: Route data to different sinks based on conditions
5. **Real-time Processing**: Add file watching for continuous processing

## Best Practices

### Error Handling
1. Use `error-handling: "stop-on-error"` for production pipelines
2. Only use `optional: true` for non-critical steps (logging, notifications)
3. Configure appropriate retry settings for transient failures
4. Monitor pipeline execution logs for error patterns

### Configuration Organization
1. Use descriptive names for steps, sources, and sinks
2. Add comprehensive comments explaining configuration choices
3. Group related operations logically
4. Version your pipeline configurations

### Performance Optimization
1. Use `mode: "parallel"` for independent steps when possible
2. Configure appropriate buffer sizes for large datasets
3. Consider database connection pooling for high-throughput scenarios
4. Monitor step execution times and optimize bottlenecks

### Data Integrity
1. Handle data type conversions explicitly
2. Validate required fields before processing
3. Implement proper error handling for constraint violations
4. Use transactions for database operations when needed

## Troubleshooting

### Common Issues

#### Pipeline Execution Issues

1. **Pipeline continues despite failures**
   - Check for `optional: true` on critical steps
   - Verify `error-handling: "stop-on-error"` is set
   - Review step-level optional settings that override pipeline-level error handling

2. **Operation not supported errors**
   - Verify operation names match configuration
   - Check that sink type supports the operation
   - Review operation resolution logs
   - For databases, ensure operation is in `queries:` section, not `operations:`

3. **Step dependency errors**
   - Check for circular dependencies in `depends-on` declarations
   - Verify all referenced step names exist
   - Ensure dependency graph is acyclic

#### Data Source Issues

4. **JSON extraction errors**
   - **Invalid JSONPath**: Verify JSONPath syntax (use `$[*]` for arrays, `$[?(@.field == value)]` for filters)
   - **Empty results**: Check `root-path` setting matches JSON structure
   - **Nested data missing**: Ensure `flatten-arrays: false` to preserve structure
   - **Type errors**: JSON numbers may be parsed as Double or Integer

   **Example Fix:**
   ```yaml
   file-format:
     type: "json"
     root-path: "$"              # Start at document root
     flatten-arrays: false       # Preserve nested arrays
   operations:
     getAllItems: "$[*]"         # Correct: extract all array elements
     # NOT: "SELECT * FROM json" # Wrong for JSON
   ```

5. **XML extraction errors**
   - **No data extracted**: Verify `record-element` matches XML element name (case-sensitive)
   - **Attributes missing**: Set `include-attributes: true` to extract XML attributes
   - **Nested elements missing**: Check XML structure matches expected hierarchy
   - **Attribute access**: Remember attributes are prefixed with `@` in extracted data

   **Example Fix:**
   ```yaml
   file-format:
     type: "xml"
     record-element: "trade"     # Must match XML element name exactly
     include-attributes: true    # Required to extract id, status, etc.
   ```

   **Java Access:**
   ```java
   String tradeId = (String) trade.get("@id");        // Attribute
   String buyerName = (String) trade.get("buyerParty");  // Element
   ```

6. **Database extraction errors**
   - **Missing source-type**: Always include `source-type: "h2"` (or postgresql, mysql, etc.)
   - **Wrong configuration fields**: Use `database:` for H2 file path, not `url:`
   - **Wrong operations section**: Use `queries:` for SQL, not `operations:`
   - **Connection failures**: Check database file path and permissions
   - **SQL syntax errors**: Verify SQL is compatible with database type (H2, PostgreSQL, etc.)

   **Example Fix:**
   ```yaml
   data-sources:
     - name: "db-source"
       type: "database"
       source-type: "h2"         # REQUIRED: specify database type
       connection:
         database: "./target/db/mydb"  # H2: use 'database' field
         # NOT: url: "jdbc:h2:..."     # Wrong for H2
       queries:                   # Use 'queries' for SQL
         getAllTrades: "SELECT * FROM otc_trades"
         # NOT: operations:       # Wrong section name
   ```

7. **CSV extraction errors**
   - **Wrong delimiter**: Check `delimiter` setting matches CSV file
   - **Header issues**: Verify `has-header: true` if first row contains column names
   - **Encoding problems**: Set correct `encoding` (UTF-8, ISO-8859-1, etc.)
   - **Quote character issues**: Adjust `quote-char` if using non-standard quotes

#### Data Sink Issues

8. **Data integrity violations**
   - Check for duplicate primary keys
   - Validate data types match target schema
   - Review constraint definitions
   - Use UPSERT operations for idempotent loads

9. **Database Connection Errors**
   - Ensure target directory exists and is writable
   - Check H2 database file permissions
   - Verify JDBC driver is on classpath
   - Check connection pool settings for high-load scenarios

#### Configuration Issues

10. **Configuration Loading Errors**
    - Verify YAML syntax and indentation (use 2 spaces, not tabs)
    - Check classpath for configuration file
    - Validate metadata section is complete
    - Ensure all referenced data sources/sinks are defined

11. **Memory Issues**
    - Adjust batch sizes in configuration
    - Monitor memory usage with JVM flags
    - Use streaming for large datasets
    - Consider pagination for database queries

### Debugging Tips

#### Enable Detailed Logging

Add JVM argument for DEBUG level logging:
```bash
-Dlogging.level.dev.mars.apex=DEBUG
```

This provides detailed information about:
- Pipeline initialization and configuration loading
- Step execution with timing information
- Data source connections and queries
- Operation resolution and mapping
- Error stack traces and context

#### Validate Configuration

Before running pipeline, validate YAML syntax:
```bash
# Use yamllint or online YAML validator
yamllint pipeline-config.yaml
```

#### Test Data Sources Independently

Create simple test to verify data source connectivity:
```java
@Test
void testDataSourceConnection() {
    ApexConfiguration config = configLoader.loadConfiguration("config.yaml");
    DataSourceConfiguration dsConfig = config.getDataSources().get(0);
    ExternalDataSource dataSource = DataSourceFactory.createDataSource(dsConfig);
    dataSource.initialize(dsConfig);

    // Test query
    Object result = dataSource.query("test-operation", Map.of());
    assertNotNull(result, "Data source should return data");
}
```

#### Use Small Test Datasets

Start with minimal data to validate pipeline logic:
- JSON: 2-3 records with simple structure
- XML: 1-2 elements with minimal nesting
- Database: 5-10 rows in test tables
- CSV: 3-5 rows with all column types

#### Verify Extracted Data Structure

Print extracted data to understand structure:
```java
@Test
void inspectExtractedData() {
    PipelineContext context = executor.execute();
    StepResult result = context.getStepResult("extract-step");

    // Print data structure
    System.out.println("Extracted data type: " + result.getData().getClass());
    System.out.println("Extracted data: " + result.getData());

    // For JSON/XML, inspect first record
    if (result.getData() instanceof List) {
        List<?> list = (List<?>) result.getData();
        if (!list.isEmpty()) {
            System.out.println("First record type: " + list.get(0).getClass());
            System.out.println("First record: " + list.get(0));
        }
    }
}
```

#### Check File Paths

Verify file paths are correct relative to working directory:
```java
@Test
void verifyFilePaths() {
    Path basePath = Paths.get("./demo-data/json");
    assertTrue(Files.exists(basePath), "Base path should exist");

    Path filePath = basePath.resolve("otc-options.json");
    assertTrue(Files.exists(filePath), "Data file should exist");
}
```

### Performance Optimization Tips

1. **Use Parallel Execution** for independent steps
   ```yaml
   execution:
     mode: "parallel"  # Steps without dependencies run concurrently
   ```

2. **Batch Database Operations** for high throughput
   ```yaml
   batch:
     enabled: true
     batch-size: 100
   ```

3. **Connection Pooling** for database sources
   ```yaml
   connection:
     connection-pool:
       max-size: 10
       min-size: 2
   ```

4. **Limit Result Sets** with SQL LIMIT clause
   ```sql
   SELECT * FROM large_table LIMIT 1000
   ```

5. **Use Indexes** on database columns used in WHERE clauses

6. **Stream Large Files** instead of loading entirely into memory

## Example Configurations

See the following example files in the `apex-demo/src/test/java/dev/mars/apex/demo/etl/` directory:

### Basic Examples
- `SimplePipelineTest.yaml`: Basic pipeline for learning
- `PipelineEtlTest.yaml`: Core ETL processing with CSV

### Data Source Examples
- `PipelineEtlExecutionTestExtractCsv.yaml`: CSV file extraction
- `PipelineEtlExecutionTestExtractJson.yaml`: JSON file extraction with JSONPath queries
- `PipelineEtlExecutionTestExtractXml.yaml`: XML file extraction with nested elements
- `PipelineEtlExecutionTestExtractDatabase.yaml`: Basic database extraction
- `PipelineEtlExecutionTestExtractDatabaseAdvanced.yaml`: Advanced database queries with JOINs and aggregations

### Data Sink Examples
- `PipelineEtlExecutionTestLoadFilesystem.yaml`: File system output
- `PipelineEtlExecutionTestLoadDatabase.yaml`: Database loading
- `CsvToH2PipelineTest.yaml`: Production-ready CSV to database pipeline (OTC trade data)

### Transform Examples
- `PipelineTransformStepTest.yaml`: Field addition, calculation, validation, aggregation

### Advanced Features
- `PipelineExecutionKeywordTest.yaml`: Error handling, retry mechanisms, execution modes
- `PipelineStepDependencyTest.yaml`: Step dependencies and circular dependency detection

Each example includes comprehensive inline comments explaining configuration options and best practices.

## Summary of ETL Capabilities

### Supported Data Sources
| Type | Format | Features | Example |
|------|--------|----------|---------|
| **File System** | CSV | Delimiters, headers, quotes | Trade data import |
| **File System** | JSON | JSONPath queries, nested objects | OTC options trades |
| **File System** | XML | Attributes, nested elements | Trade confirmations (FpML-style) |
| **File System** | Text | Line-based, custom parsing | Log file analysis |
| **Database** | SQL | JOINs, aggregations, CTEs | Counterparty exposure analytics |
| **REST API** | JSON/XML | HTTP methods, authentication | External data integration |
| **Cache** | In-memory | Fast access, temporary data | Session data |
| **Message Queue** | Various | Async processing, pub/sub | Event streaming |

### Supported Data Sinks
| Type | Features | Use Cases |
|------|----------|-----------|
| **File System** | Write, append, rotate | Audit logs, reports, exports |
| **Database** | INSERT, UPDATE, UPSERT, batch | Data warehousing, persistence |
| **REST API** | POST, PUT, PATCH | External system integration |
| **Message Queue** | Publish, routing | Event-driven architectures |

### Supported Transformations
| Type | Description | Example |
|------|-------------|---------|
| **Field Addition** | Add new fields with default values | Add timestamp, status fields |
| **Calculation** | Compute derived fields | Calculate totals, percentages |
| **Validation** | Validate data against rules | Check required fields, formats |
| **Filter** | Remove records based on criteria | Filter inactive records |
| **Aggregation** | Group and summarize data | Sum by category, count by status |

### Pipeline Features
| Feature | Description | Configuration |
|---------|-------------|---------------|
| **Sequential Execution** | Steps run one after another | `mode: "sequential"` |
| **Parallel Execution** | Independent steps run concurrently | `mode: "parallel"` |
| **Error Handling** | Stop or continue on errors | `error-handling: "stop-on-error"` |
| **Retry Mechanism** | Automatic retry with delays | `max-retries: 3, retry-delay-ms: 1000` |
| **Step Dependencies** | Control execution order | `depends-on: ["step1", "step2"]` |
| **Optional Steps** | Steps that can fail without stopping pipeline | `optional: true` |
| **Circular Dependency Detection** | Prevent infinite loops | Automatic validation |

### Test Coverage
The APEX ETL framework includes comprehensive test coverage:

- **52 Total Tests** - All passing ✅
- **Extract Tests**: 12 tests (CSV, JSON, XML, Database basic/advanced, empty, invalid)
- **Load Tests**: 8 tests (FileSystem, Database, Batch, Invalid records)
- **Transform Tests**: 5 tests (Field addition, calculation, validation, aggregation, errors)
- **Dependency Tests**: 5 tests (Order, failure propagation, optional, circular detection)
- **Execution Tests**: 10 tests (Sequential, parallel, error handling, retry)
- **Config Tests**: 4 tests (Validation, performance)
- **Core Tests**: 4 tests (End-to-end pipelines)
- **Initialization Tests**: 4 tests (Service setup)

## Related Documentation

- [APEX YAML Reference](../../docs/APEX_YAML_REFERENCE.md)
- [Data Sink Framework Design](../../docs/APEX_DATA_PIPELINE_OUTPUT_DESIGN.md)
- [APEX Core Documentation](../../apex-core/README.md)
