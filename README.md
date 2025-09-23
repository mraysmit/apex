![APEX System Logo](docs/APEX%20System%20logo.png)

# APEX Rules Engine

[![Java](https://img.shields.io/badge/Java-23-orange.svg)](https://openjdk.java.net/projects/jdk/23/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Compatible-blue.svg)](https://www.postgresql.org/)
[![Documentation](https://img.shields.io/badge/Docs-Complete-blue.svg)](docs/)
[![Demos](https://img.shields.io/badge/Demos-16%2B-orange.svg)](apex-demo/)
[![Playground](https://img.shields.io/badge/Playground-Interactive-purple.svg)](http://localhost:8081/playground)
[![Financial Services](https://img.shields.io/badge/Financial%20Services-Ready-gold.svg)](docs/old/APEX_FINANCIAL_SERVICES_GUIDE.md)
[![API Docs](https://img.shields.io/badge/API-Swagger-green.svg)](http://localhost:8080/swagger-ui.html)

**Version:** 2.1
**Date:** 2025-08-28
**Author:** Mark Andrew Ray-Smith Cityline Ltd

A powerful expression processor for Java applications with comprehensive **data validation and enrichment capabilities**, **external data-source reference system**, scenario-based configuration management, and enterprise-grade YAML validation.

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

# Bootstrap demos (complete end-to-end scenarios)
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.enrichment.OtcOptionsBootstrapDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CommoditySwapBootstrapDemo"

# Lookup pattern examples
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.lookups.SimpleFieldLookupDemo"
```

## Key Features

### Core Capabilities - Data Validation and Enrichment
- **Data Validation**: Comprehensive rule-based validation with enterprise-grade error reporting
- **Data Enrichment**: Multi-source data enrichment with YAML datasets and external lookups
- **YAML Dataset Enrichment**: Embed reference data directly in configuration files
- **External Data Integration**: Connect to databases, REST APIs, file systems, and caches

### Interactive Playground - Development Environment
- **4-Panel Web Interface**: Real-time rule development and testing
- **Built-in Templates**: Financial services patterns and examples
- **Live Preview**: See validation and enrichment results instantly
- **Cross-Browser Support**: Comprehensive UI testing with 100% coverage

### APEX 2.1 - External Data-Source Reference System
- **Clean Architecture**: Separation of infrastructure and business logic configurations
- **Configuration Caching**: Automatic caching of external configurations for performance
- **Reusable Components**: Share data-source configurations across multiple rule sets
- **Enterprise Scalability**: Environment-specific infrastructure with shared business logic
- **Enhanced H2 Support**: Custom H2 parameters directly in YAML configuration for performance tuning and debugging

### Scenario-Based Configuration
- **Centralized Management**: Single registry manages all processing pipelines
- **Automatic Routing**: Data type detection and rule selection
- **Lightweight Configuration**: Clean separation of routing logic and business rules
- **Dependency Management**: Complete YAML dependency chain analysis

### Financial Services Ready
- **OTC Derivatives Validation**: Multi-tier validation framework
- **Regulatory Compliance**: MiFID II, EMIR, Dodd-Frank reporting
- **Trade Settlement**: Post-trade processing and auto-repair workflows
- **Risk Assessment**: Credit, market, and operational risk scoring

### Enterprise Features
- **Connection Pooling**: Production-ready database connection management
- **Health Monitoring**: Comprehensive system health checks
- **Caching**: Multi-level caching with circuit breakers
- **100% Test Coverage**: Comprehensive testing with cross-browser UI support

## External Data-Source Reference System

APEX 2.1 introduces a revolutionary **external data-source reference system** that enables clean architecture and enterprise-grade configuration management.

### Clean Separation of Concerns

**Traditional Approach (Mixed Configuration):**
```yaml
# Everything mixed together - infrastructure + business logic
metadata:
  name: "Legacy Configuration"
  version: "1.0.0"
  type: "rule-config"

data-sources:
  - name: "customer-database"
    type: "database"
    connection:
      url: "jdbc:postgresql://localhost:5432/customers"
      username: "user"
      password: "pass"
    queries:
      getCustomer:
        sql: "SELECT * FROM customers WHERE customer_id = :customerId"

enrichments:
  - id: "customer-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        type: "database"
        data-source: "customer-database"
        query: "getCustomer"
```

**Modern Approach (Clean Architecture):**
```yaml
# Business Logic Configuration (Clean and Focused)
metadata:
  id: "Modern Configuration"
  name: "Modern Configuration"
  version: "2.1.0"
  description: "Business logic using external data-source references"
  type: "rule-config"
  author: "business.rules.team@company.com"

# External data-source references (infrastructure configuration - reusable)
data-source-refs:
  - name: "customer-database"
    source: "data-sources/customer-database.yaml"
    enabled: true
    description: "Customer database for profile enrichment"

# Business logic enrichments (lean and focused)
enrichments:
  - id: "customer-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-key: "#customerId"
      lookup-dataset:
        type: "database"
        data-source-ref: "customer-database"
        query-ref: "getActiveCustomer"
```

```yaml
# External Infrastructure Configuration (Reusable)
# File: data-sources/customer-database.yaml
metadata:
  id: "Customer Database Configuration"
  name: "Customer Database Configuration"
  version: "1.0.0"
  description: "Database connections for customer data"
  type: "external-data-config"
  author: "data.team@company.com"
  tags: ["database", "production", "postgresql"]

dataSources:
  - name: "customer-database"
    type: "database"
    sourceType: "postgresql"
    enabled: true
    description: "Primary customer database"

    connection:
      host: "localhost"
      port: 5432
      database: "customers"
      username: "${DB_USERNAME}"
      password: "${DB_PASSWORD}"

    queries:
      getActiveCustomer: "SELECT * FROM customers WHERE customer_id = :customerId AND status = 'ACTIVE'"

    cache:
      enabled: true
      ttlSeconds: 300
      maxSize: 1000

configuration:
  defaultConnectionTimeout: 30000
  monitoring:
    enabled: true
    healthCheckLogging: true
```

### Key Benefits

- **Clean Architecture**: Infrastructure and business logic cleanly separated
- **Reusable Components**: External data-source configurations shared across multiple rule sets
- **Performance**: Configuration caching and connection pooling
- **Environment Management**: Different infrastructure configurations for dev/test/prod
- **Enterprise Scalability**: Production-ready configuration management

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

- **apex-core**: Core rules engine and **external data-source reference system**
- **apex-demo**: 16+ comprehensive demonstrations including **external data-source reference examples**
- **apex-playground**: Interactive web-based development environment
- **apex-rest-api**: Complete REST API with OpenAPI/Swagger documentation
- **docs**: Comprehensive documentation and guides including **external data-source reference guide**

## Learning Paths

### Quick Start (30 minutes)
1. **APEX Playground** (15 minutes) - Interactive experimentation
2. **Simple PostgreSQL Lookup Demo** (5 minutes) - External data-source references
3. **OTC Options Bootstrap Demo** (10 minutes) - Complete workflow

### Developer Path (3-4 hours)
1. **External Data-Source Reference Demos** (45 minutes) - Modern clean architecture
2. **All Lookup Pattern Examples** (60 minutes) - Master data enrichment
3. **All Bootstrap Demonstrations** (120 minutes) - Complete financial workflows
4. **Advanced Feature Demos** (60-90 minutes) - Technical deep dive

### External Data-Source Reference Path (1-2 hours)
1. **SimplePostgreSQLLookupDemo** (20 minutes) - Basic external references
2. **PostgreSQLLookupDemo** (30 minutes) - Advanced multi-table lookups
3. **ExternalDataSourceWorkingDemo** (30 minutes) - Production-ready patterns
4. **Documentation Review** (30 minutes) - APEX YAML Reference and Data Management Guide

### Production Implementation (4-6 hours)
1. **Complete Demo Ecosystem** (180 minutes) - All 16 demonstrations
2. **Documentation Deep Dive** (120-180 minutes) - All 6 guides
3. **Custom Implementation** (varies) - Build your own configurations

## Demo Categories

### Bootstrap Demonstrations - Complete End-to-End Scenarios
- **OTC Options Bootstrap Demo** - Multi-source data integration with PostgreSQL
- **Commodity Swap Bootstrap Demo** - Complex validation workflows
- **Financial Services Demos** - Regulatory compliance and risk assessment

### Lookup Pattern Examples
- **Simple PostgreSQL Lookup Demo** - Basic external data-source references
- **PostgreSQL Lookup Demo** - Advanced multi-table database lookups
- **External Data Source Working Demo** - Production-ready patterns
- **Simple Field Lookup Demo** - YAML dataset enrichment

### Advanced Feature Demonstrations
- **Dynamic Method Execution** - Runtime method invocation
- **Performance and Exception Handling** - Error handling and metrics
- **Data Service Management** - Infrastructure management
- **YAML Configuration Patterns** - Advanced configuration examples

## Documentation

### Essential Guides
- **[APEX Playground](http://localhost:8081/playground)** - Interactive development environment
- **[Rules Engine User Guide](docs/APEX_RULES_ENGINE_USER_GUIDE.md)** - Complete user documentation
- **[Technical Reference](docs/APEX_TECHNICAL_REFERENCE.md)** - Architecture and implementation
- **[Financial Services Guide](docs/old/APEX_FINANCIAL_SERVICES_GUIDE.md)** - Domain-specific patterns
- **[Bootstrap Demos Guide](docs/APEX_BOOTSTRAP_DEMOS_GUIDE.md)** - 16 comprehensive demonstrations
- **[REST API Guide](docs/APEX_REST_API_GUIDE.md)** - Complete HTTP API reference
- **[Data Management Guide](docs/APEX_DATA_MANAGEMENT_GUIDE.md)** - Data integration and management

### Quick Reference
- **Configuration Questions**: [Rules Engine User Guide](docs/APEX_RULES_ENGINE_USER_GUIDE.md)
- **Implementation Questions**: [Technical Reference](docs/APEX_TECHNICAL_REFERENCE.md)
- **Financial Services Questions**: [Financial Services Guide](docs/old/APEX_FINANCIAL_SERVICES_GUIDE.md)

## Use Cases

### Perfect For
- **Currency Reference Data**: ISO currency codes with metadata
- **Regulatory Compliance**: MiFID II, EMIR, Dodd-Frank reporting
- **OTC Derivatives Validation**: Multi-tier validation framework
- **Trade Settlement**: Post-trade processing and auto-repair workflows
- **Risk Assessment**: Credit, market, and operational risk scoring

### Data Integration
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

### External Data-Source Reference System
1. **[APEX YAML Reference Guide](docs/APEX_YAML_REFERENCE.md)** - Complete external data-source reference syntax
2. **[APEX Data Management Guide](docs/APEX_DATA_MANAGEMENT_GUIDE.md)** - Section 16: External Data Source Integration
3. **External Data-Source Reference Demos** - SimplePostgreSQLLookupDemo, PostgreSQLLookupDemo, ExternalDataSourceWorkingDemo

### General Documentation
1. Start with the **[APEX Playground](http://localhost:8081/playground)** for hands-on experience
2. Review the **[Bootstrap Demos Guide](docs/APEX_BOOTSTRAP_DEMOS_GUIDE.md)** for practical examples
3. Check the **[Rules Engine User Guide](docs/APEX_RULES_ENGINE_USER_GUIDE.md)** for comprehensive documentation
4. Explore the **16+ demonstrations** in the `apex-demo` module

---

**Version:** 2.1 | **Author:** Mark Andrew Ray-Smith Cityline Ltd | **Date:** 2025-08-28
