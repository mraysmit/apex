# APEX Data Sources Examples

This directory contains the **simplest possible examples** of each data source type supported by APEX. Each example demonstrates a single data source type with minimal configuration and clear documentation.

## 📁 Directory Structure

```
datasources/
├── README.md                    # This file - overview and navigation
├── inline/                      # Inline data embedded in YAML
├── database/                    # Database connections (H2, PostgreSQL, etc.)
├── filesystem/                  # File-based data sources
│   ├── csv/                     # CSV files
│   ├── json/                    # JSON files
│   └── xml/                     # XML files
└── restapi/                     # REST API data sources
```

## 🎯 Quick Start Guide

### 1. Inline Data Sources
**Best for:** Small, static reference data (< 100 records)

```bash
# Run the inline example
mvn test -Dtest="SimpleInlineDataSourceTest" -pl apex-demo
```

**Example:** Currency lookup with 3 currencies embedded in YAML
- No external dependencies
- Perfect for prototyping
- Data never changes

### 2. Database Data Sources  
**Best for:** Large, dynamic data (> 100 records)

```bash
# Run the database example
mvn test -Dtest="SimpleDatabaseDataSourceTest" -pl apex-demo
```

**Example:** Customer lookup from H2 database
- Handles large datasets
- Real-time data updates
- Complex queries with joins

### 3. CSV File Data Sources
**Best for:** Spreadsheet exports, batch data

```bash
# Run the CSV example
mvn test -Dtest="SimpleCsvDataSourceTest" -pl apex-demo
```

**Example:** Product lookup from CSV file
- Easy to create and edit
- Works with Excel exports
- Good for periodic data updates

### 4. JSON File Data Sources
**Best for:** API data exports, configuration data

```bash
# Run the JSON example
mvn test -Dtest="SimpleJsonDataSourceTest" -pl apex-demo
```

**Example:** User lookup from JSON file
- Structured data format
- Nested object support
- Web-friendly format

### 5. XML File Data Sources
**Best for:** Legacy system exports, structured documents

```bash
# Run the XML example
mvn test -Dtest="SimpleXmlDataSourceTest" -pl apex-demo
```

**Example:** Department lookup from XML file
- Hierarchical data support
- Legacy system compatibility
- Schema validation support

### 6. REST API Data Sources
**Best for:** Real-time external data, microservices

```bash
# Run the REST API example (requires internet)
mvn test -Dtest="SimpleRestApiDataSourceTest" -pl apex-demo
```

**Example:** User lookup from public API
- Real-time data access
- External service integration
- Dynamic data updates

## 🔄 When to Use Each Type

| Data Source | Size | Update Frequency | Complexity | Best For |
|-------------|------|------------------|------------|----------|
| **Inline** | < 100 records | Never | Simple | Status codes, constants |
| **Database** | > 100 records | Real-time | Complex | Customer data, transactions |
| **CSV** | 100-10K records | Daily/Weekly | Simple | Spreadsheet exports |
| **JSON** | 100-10K records | Daily/Weekly | Medium | API exports, config |
| **XML** | 100-10K records | Daily/Weekly | Medium | Legacy systems |
| **REST API** | Any size | Real-time | Complex | External services |

## 🚀 Running All Examples

```bash
# Run all data source examples
mvn test -Dtest="*DataSourceTest" -pl apex-demo

# Run specific type
mvn test -Dtest="*InlineDataSourceTest" -pl apex-demo
mvn test -Dtest="*DatabaseDataSourceTest" -pl apex-demo
mvn test -Dtest="*CsvDataSourceTest" -pl apex-demo
mvn test -Dtest="*JsonDataSourceTest" -pl apex-demo
mvn test -Dtest="*XmlDataSourceTest" -pl apex-demo
mvn test -Dtest="*RestApiDataSourceTest" -pl apex-demo
```

## 📚 More Complex Examples

For more advanced examples, see:
- `../lookup/` - Complex lookup patterns and expressions
- `../database/` - Advanced database configurations
- `../etl/` - ETL pipelines with multiple data sources
- `../enrichment/` - Complex enrichment scenarios

## 🔧 Key Concepts

### Data Source Configuration
All data sources follow the same pattern:
1. **Connection** - How to connect to the data
2. **Query/Path** - How to find specific records
3. **Field Mapping** - How to map source fields to target fields

### Lookup Configuration
Every lookup enrichment has:
- **lookup-key** - Expression to extract the search value
- **lookup-dataset** - Configuration for the data source
- **field-mappings** - How to map results to output fields

### Error Handling
All examples demonstrate:
- Missing lookup key (condition not met)
- No match found (graceful degradation)
- Data source errors (connection failures)

## 💡 Tips for Success

1. **Start Simple** - Begin with inline data, then move to files/databases
2. **Test Incrementally** - Verify each data source type works before combining
3. **Use Appropriate Types** - Match data source to your use case
4. **Handle Errors** - Always test missing data and connection failures
5. **Monitor Performance** - Database and API sources can be slower than files

## 🔗 Related Documentation

- [APEX Rules Engine User Guide](../../../../../../../../../docs/APEX_RULES_ENGINE_USER_GUIDE.md)
- [External Data Sources Guide](../../../../../../../../../docs/EXTERNAL_DATA_SOURCES_GUIDE.md)
- [YAML Configuration Reference](../../../../../../../../../docs/APEX_YAML_REFERENCE.md)
