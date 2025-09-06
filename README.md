![APEX System Logo](docs/APEX%20System%20logo.png)

# APEX Rules Engine

[![Java](https://img.shields.io/badge/Java-23-orange.svg)](https://openjdk.java.net/projects/jdk/23/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Compatible-blue.svg)](https://www.postgresql.org/)
[![Documentation](https://img.shields.io/badge/Docs-Complete-blue.svg)](docs/)
[![Demos](https://img.shields.io/badge/Demos-16%2B-orange.svg)](apex-demo/)
[![Playground](https://img.shields.io/badge/Playground-Interactive-purple.svg)](http://localhost:8081/playground)
[![Financial Services](https://img.shields.io/badge/Financial%20Services-Ready-gold.svg)](docs/APEX_FINANCIAL_SERVICES_GUIDE.md)
[![API Docs](https://img.shields.io/badge/API-Swagger-green.svg)](http://localhost:8080/swagger-ui.html)

**Version:** 2.1
**Date:** 2025-08-28
**Author:** Mark Andrew Ray-Smith Cityline Ltd

A powerful expression processor for Java applications with comprehensive data source integration, **external data-source reference system**, scenario-based configuration management, and enterprise-grade YAML validation.

## Quick Start

### Interactive Playground (Recommended)
```bash
cd apex-playground
mvn spring-boot:run
# Access at http://localhost:8081/playground
```

### REST API
```bash
cd apex-rest-api
mvn spring-boot:run
# Access Swagger UI at http://localhost:8080/swagger-ui.html
```

### Run Demonstrations
```bash
cd apex-demo
# External Data-Source Reference Demos (APEX 2.1)
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.examples.SimplePostgreSQLLookupDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.examples.PostgreSQLLookupDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.lookup.ExternalDataSourceWorkingDemo"

# Pipeline Orchestration Demo (NEW - ETL workflows)
mvn exec:java@csv-to-h2-etl

# Bootstrap demos (complete end-to-end scenarios)
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.enrichment.OtcOptionsBootstrapDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CommoditySwapBootstrapDemo"

# Lookup pattern examples
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.lookups.SimpleFieldLookupDemo"

# File System Lookup Demo (NEW - file-system dataset type)
mvn exec:java@file-system-lookup
```

## Key Features

### APEX 2.1 - External Data-Source Reference System
- **Clean Architecture**: Separation of infrastructure and business logic configurations
- **Configuration Caching**: Automatic caching of external configurations for performance
- **Reusable Components**: Share data-source configurations across multiple rule sets
- **Enterprise Scalability**: Environment-specific infrastructure with shared business logic
- **Enhanced H2 Support**: Custom H2 parameters directly in YAML configuration for performance tuning and debugging

### Core Features
- **Interactive Playground**: 4-panel web interface for real-time rule development and testing
- **Pipeline Orchestration**: YAML-driven ETL workflows with dependency management and error handling
- **Data Sink Architecture**: Comprehensive output capabilities to databases, files, and audit systems
- **Scenario-Based Configuration**: Centralized management and routing of data processing pipelines
- **External Data Integration**: Connect to databases, REST APIs, file systems, and caches
- **Advanced H2 Database Support**: Custom parameters for performance tuning, debugging, and compatibility modes
- **YAML Dataset Enrichment**: Embed reference data directly in configuration files
- **Financial Services Ready**: OTC derivatives validation, regulatory compliance, risk assessment
- **Enterprise Features**: Connection pooling, health monitoring, caching, circuit breakers
- **100% Test Coverage**: Comprehensive testing with cross-browser UI support

## External Data-Source Reference System

APEX 2.1 introduces a revolutionary **external data-source reference system** that enables clean architecture and enterprise-grade configuration management.

### Clean Separation of Concerns

**Traditional Approach (Mixed Configuration):**
```yaml
# Everything mixed together - infrastructure + business logic
metadata:
  name: "Legacy Configuration"

data-sources:  # Infrastructure mixed with business logic
  - name: "customer-database"
    type: "database"
    connection:
      url: "jdbc:postgresql://localhost:5432/customers"
      username: "user"
      password: "pass"

enrichments:  # Business logic
  - id: "customer-lookup"
    type: "lookup-enrichment"
    # ... business logic configuration
```

**Modern Approach (Clean Architecture):**
```yaml
# Business Logic Configuration (Clean and Focused)
metadata:
  name: "Modern Configuration"
  version: "2.1.0"

# External data-source references (infrastructure configuration - reusable)
data-source-refs:
  - name: "customer-database"
    source: "data-sources/customer-database.yaml"  # External infrastructure file
    enabled: true

# Business logic enrichments (lean and focused)
enrichments:
  - id: "customer-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        data-source-ref: "customer-database"  # References external data-source
        query-ref: "getActiveCustomer"        # Named query from external config
```

```yaml
# External Infrastructure Configuration (Reusable)
# File: data-sources/customer-database.yaml
metadata:
  name: "Customer Database Configuration"
  type: "external-data-config"

connection:
  type: "database"
  driver: "postgresql"
  url: "jdbc:postgresql://localhost:5432/customers"
  username: "${DB_USERNAME}"
  password: "${DB_PASSWORD}"
  pool:
    initial-size: 5
    max-size: 20

queries:
  getActiveCustomer:
    sql: "SELECT * FROM customers WHERE customer_id = :customerId AND status = 'ACTIVE'"
    parameters:
      - name: "customerId"
        type: "string"
        required: true
```

### Key Benefits

- **Clean Architecture**: Infrastructure and business logic cleanly separated
- **Reusable Components**: External data-source configurations shared across multiple rule sets
- **Performance**: Configuration caching and connection pooling
- **Environment Management**: Different infrastructure configurations for dev/test/prod
- **Enterprise Scalability**: Production-ready configuration management

## Pipeline Orchestration

APEX 2.1 introduces powerful **Pipeline Orchestration** capabilities that enable complete ETL (Extract, Transform, Load) workflows defined entirely in YAML configuration.

### YAML-Driven ETL Workflows

**Traditional Approach (Hardcoded Java):**
```java
// Complex orchestration code
dataSource.extract("getAllCustomers", "customer-csv-input");
database.insert("customer-h2-database", "insertCustomer", customerData);
auditLog.write("audit-log-file", "writeAuditRecord", auditData);
```

**Modern Approach (YAML-Driven Pipeline):**
```java
// Simple APEX pipeline execution
pipelineEngine.executePipeline("customer-etl-pipeline");
```

```yaml
# Complete ETL workflow in YAML
pipeline:
  name: "customer-etl-pipeline"
  steps:
    - name: "extract-customers"
      type: "extract"
      source: "customer-csv-input"
      operation: "getAllCustomers"

    - name: "load-to-database"
      type: "load"
      sink: "customer-h2-database"
      operation: "insertCustomer"
      depends-on: ["extract-customers"]

    - name: "audit-logging"
      type: "audit"
      sink: "audit-log-file"
      operation: "writeAuditRecord"
      depends-on: ["load-to-database"]
      optional: true
```

### Key Pipeline Features

- **Dependency Management**: Automatic step dependency resolution and validation
- **Error Handling**: Configurable error handling strategies with optional steps
- **Data Flow**: Automatic data passing between pipeline steps
- **Monitoring**: Built-in step timing and execution tracking
- **Data Sinks**: Database, file system, and audit logging outputs
- **Batch Processing**: Efficient bulk operations with configurable batch sizes

### Pipeline Demo

```bash
# Run the complete ETL pipeline demonstration
mvn exec:java@csv-to-h2-etl -pl apex-demo
```

This demo processes customer data from CSV to H2 database with full audit logging in approximately 23ms, demonstrating production-ready performance.

## Architecture

```mermaid
graph TB
    subgraph "Business Layer"
        A[Business Rules]
        B[YAML Configuration]
        C[External Data-Source References]
    end

    subgraph "Rules Engine Core"
        D[Expression Evaluator]
        E[Rule Engine Service]
        F[Context Manager]
        G[DataSource Resolver]
    end

    subgraph "External Data-Source Layer"
        H[External Database Configs]
        I[External API Configs]
        J[External File Configs]
        K[Configuration Cache]
    end

    subgraph "Data Layer"
        L[YAML Datasets]
        M[External APIs]
        N[Database Lookups]
        O[File Systems]
    end

    subgraph "Integration Layer"
        P[Spring Boot]
        Q[REST APIs]
        R[Microservices]
    end

    A --> D
    B --> E
    C --> G
    G --> H
    G --> I
    G --> J
    G --> K
    H --> N
    I --> M
    J --> O
    D --> F
    E --> L
    E --> M
    E --> N
    F --> P
    F --> Q
    F --> R
```

## Project Structure

- **apex-core**: Core rules engine with **external data-source reference system** and **pipeline orchestration engine**
- **apex-demo**: 16+ comprehensive demonstrations including **pipeline orchestration** and **external data-source reference examples**
- **apex-playground**: Interactive web-based development environment
- **apex-rest-api**: Complete REST API with OpenAPI/Swagger documentation
- **docs**: Comprehensive documentation and guides including **pipeline orchestration guide** and **external data-source reference guide**

## Learning Paths

### Quick Start (30 minutes)
1. **APEX Playground** (15 minutes) - Interactive experimentation
2. **Pipeline Orchestration Demo** (5 minutes) - ETL workflow demonstration
3. **Simple PostgreSQL Lookup Demo** (5 minutes) - External data-source references
4. **OTC Options Bootstrap Demo** (5 minutes) - Complete workflow

### Developer Path (3-4 hours)
1. **Pipeline Orchestration Demo** (30 minutes) - YAML-driven ETL workflows
2. **External Data-Source Reference Demos** (45 minutes) - Modern clean architecture
3. **All Lookup Pattern Examples** (60 minutes) - Master data enrichment
4. **All Bootstrap Demonstrations** (120 minutes) - Complete financial workflows
5. **Advanced Feature Demos** (60-90 minutes) - Technical deep dive

### Pipeline Orchestration Path (1 hour)
1. **CSV to H2 ETL Demo** (20 minutes) - Complete ETL pipeline demonstration
2. **Pipeline Configuration Review** (20 minutes) - YAML pipeline syntax and features
3. **Data Sink Architecture** (20 minutes) - Database and file output capabilities

### External Data-Source Reference Path (1-2 hours)
1. **SimplePostgreSQLLookupDemo** (20 minutes) - Basic external references
2. **PostgreSQLLookupDemo** (30 minutes) - Advanced multi-table lookups
3. **ExternalDataSourceWorkingDemo** (30 minutes) - Production-ready patterns
4. **Documentation Review** (30 minutes) - APEX YAML Reference and Data Management Guide

### Production Implementation (4-6 hours)
1. **Complete Demo Ecosystem** (180 minutes) - All 16 demonstrations
2. **Documentation Deep Dive** (120-180 minutes) - All 7 guides
3. **Custom Implementation** (varies) - Build your own configurations

## Documentation

### Essential Guides
- **[APEX Playground](http://localhost:8081/playground)** - Interactive development environment
- **[Rules Engine User Guide](docs/APEX_RULES_ENGINE_USER_GUIDE.md)** - Complete user documentation with data management
- **[Pipeline Orchestration Guide](docs/APEX_DATA_PIPELINE_ORCHESTRATION_GUIDE.md)** - YAML-driven ETL workflows
- **[Technical Reference](docs/APEX_TECHNICAL_REFERENCE.md)** - Architecture and implementation
- **[Financial Services Guide](docs/APEX_FINANCIAL_SERVICES_GUIDE.md)** - Domain-specific patterns
- **[Bootstrap Demos Guide](docs/APEX_BOOTSTRAP_DEMOS_GUIDE.md)** - 16 comprehensive demonstrations
- **[REST API Guide](docs/APEX_REST_API_GUIDE.md)** - Complete HTTP API reference

### Quick Reference
- **Configuration Questions**: [Rules Engine User Guide](docs/APEX_RULES_ENGINE_USER_GUIDE.md)
- **Implementation Questions**: [Technical Reference](docs/APEX_TECHNICAL_REFERENCE.md)
- **Financial Services Questions**: [Financial Services Guide](docs/APEX_FINANCIAL_SERVICES_GUIDE.md)

## Use Cases

### Perfect For
- **ETL Workflows**: Complete data pipeline orchestration with dependency management
- **Data Processing**: Multi-step data transformation and validation workflows
- **Currency Reference Data**: ISO currency codes with metadata
- **Regulatory Compliance**: MiFID II, EMIR, Dodd-Frank reporting
- **OTC Derivatives Validation**: Multi-tier validation framework
- **Trade Settlement**: Post-trade processing and auto-repair workflows
- **Risk Assessment**: Credit, market, and operational risk scoring

### Data Integration
- **ETL Pipelines**: Use **Pipeline Orchestration** for complete data processing workflows
- **Data Sinks**: Output to databases, files, and audit systems with **Data Sink Architecture**
- **Static Reference Data** (< 100 records): Use YAML Datasets
- **Transactional Data**: Use **External Database References** (PostgreSQL, MySQL, Oracle)
- **Real-time Data**: Use **External API References** with caching
- **Batch Data**: Use **External File References** (CSV, JSON, XML)
- **Clean Architecture**: Use **External Data-Source References** for separation of concerns

## Requirements

- Java 21+
- Maven 3.6+
- PostgreSQL (for database demos)

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

## Getting Help

### Pipeline Orchestration
1. **[APEX Data Pipeline Orchestration Guide](docs/APEX_DATA_PIPELINE_ORCHESTRATION_GUIDE.md)** - Complete pipeline orchestration documentation
2. **CSV to H2 ETL Demo** - `mvn exec:java@csv-to-h2-etl -pl apex-demo`
3. **[APEX YAML Reference Guide](docs/APEX_YAML_REFERENCE.md)** - Pipeline configuration syntax

### External Data-Source Reference System
1. **[APEX YAML Reference Guide](docs/APEX_YAML_REFERENCE.md)** - Complete external data-source reference syntax
2. **[Rules Engine User Guide](docs/APEX_RULES_ENGINE_USER_GUIDE.md)** - Section: External Data Source Integration
3. **External Data-Source Reference Demos** - SimplePostgreSQLLookupDemo, PostgreSQLLookupDemo, ExternalDataSourceWorkingDemo

### General Documentation
1. Start with the **[APEX Playground](http://localhost:8081/playground)** for hands-on experience
2. Review the **[Bootstrap Demos Guide](docs/APEX_BOOTSTRAP_DEMOS_GUIDE.md)** for practical examples
3. Check the **[Rules Engine User Guide](docs/APEX_RULES_ENGINE_USER_GUIDE.md)** for comprehensive documentation
4. Explore the **16+ demonstrations** in the `apex-demo` module

---

**Version:** 2.1 | **Author:** Mark Andrew Ray-Smith Cityline Ltd | **Date:** 2025-08-28
