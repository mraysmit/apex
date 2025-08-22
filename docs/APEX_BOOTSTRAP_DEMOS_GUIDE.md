# APEX Bootstrap Demonstrations Guide

**Version:** 1.0
**Date:** 2025-08-22
**Author:** Mark Andrew Ray-Smith Cityline Ltd

## Overview

APEX includes four comprehensive bootstrap demonstrations that showcase complete end-to-end scenarios with real-world financial data, infrastructure setup, and comprehensive processing pipelines. These demos are designed to provide practical, hands-on learning experiences that demonstrate APEX capabilities through authentic financial services use cases.

## Why Bootstrap Demos Matter

Bootstrap demonstrations provide several key advantages for learning and evaluating APEX:

### Complete Infrastructure Setup
- **Automatic Database Creation**: PostgreSQL tables with realistic schemas and constraints
- **Sample Data Generation**: Authentic financial data with proper relationships and constraints
- **Configuration Loading**: External YAML configurations with comprehensive validation
- **Environment Detection**: Automatic fallback to in-memory databases when PostgreSQL unavailable

### Real-World Scenarios
- **Authentic Financial Data**: Based on actual financial services use cases and market conventions
- **Production-Ready Patterns**: Proper error handling, audit trails, and performance monitoring
- **Regulatory Compliance**: Audit trails and compliance reporting suitable for financial services
- **Performance Benchmarking**: Sub-100ms processing targets with comprehensive metrics

### Progressive Learning
- **Structured Learning Path**: Each demo builds on concepts from previous ones
- **Multiple Complexity Levels**: From ultra-simple API usage to advanced configuration patterns
- **Comprehensive Documentation**: Detailed explanations of what each scenario demonstrates
- **Interactive Execution**: Real-time feedback and results analysis

### Self-Contained Execution
- **No External Dependencies**: Everything needed to run is included in the project
- **Automatic Setup**: Infrastructure and data creation handled automatically
- **Graceful Degradation**: Fallback mechanisms for different environments
- **Clean Execution**: Proper cleanup and resource management

## Available Bootstrap Demonstrations

### 1. Custody Auto-Repair Bootstrap
**File**: `CustodyAutoRepairBootstrap.java`
**Focus**: Weighted rule-based decision making for custody settlement auto-repair

#### What This Demo Demonstrates
- **Weighted Rule-Based Decision Making**: Sophisticated scoring algorithm across client, market, and instrument factors
- **Sub-100ms Processing**: Real-time performance with comprehensive metrics collection
- **Asian Markets Focus**: Authentic market conventions for Japan, Hong Kong, Singapore
- **Exception Handling**: Comprehensive handling of edge cases and business exceptions
- **Audit Trail**: Complete regulatory compliance with detailed processing logs

#### Key Business Scenarios
1. **Premium Client in Japan**: Full auto-repair with comprehensive enrichment
2. **Standard Client in Hong Kong**: Partial repair with selective enrichment
3. **Unknown Client in Singapore**: Market and instrument enrichment only
4. **High-Value Transaction Exception**: Automatic routing to manual review
5. **Client Opt-Out Exception**: Respect client preferences and skip auto-repair

#### Technical Architecture
- **PostgreSQL Database**: Comprehensive settlement tables with proper indexing
- **Client Profiles**: Regulatory classifications and risk ratings
- **Market Data**: Asian markets with authentic trading conventions
- **Performance Monitoring**: Real-time metrics with sub-100ms targets
- **Audit System**: Complete processing logs for regulatory compliance

#### Expected Results
- **66% Auto-Repair Success Rate**: Significantly above industry average (20-40%)
- **43% Reduction in Manual Intervention**: From 60% to 34% manual processing
- **Sub-100ms Processing Times**: Real-time performance with comprehensive audit trails
- **Business-User Maintainable**: External YAML configuration for business users

### 2. Commodity Swap Validation Bootstrap
**File**: `CommoditySwapValidationBootstrap.java`
**Focus**: Progressive API complexity and multi-layered validation

#### What This Demo Demonstrates
- **Progressive API Complexity**: Evolution from ultra-simple to advanced configuration
- **Multi-Layered Validation**: 4 distinct validation approaches with sophisticated business logic
- **Static Data Enrichment**: Comprehensive data enhancement from multiple sources
- **Performance Monitoring**: Sub-100ms processing with detailed metrics analysis
- **Realistic Market Data**: Energy, Metals, and Agricultural markets with authentic conventions

#### Six Learning Scenarios
1. **Ultra-Simple API**: Basic field validation with minimal configuration
2. **Template-Based Rules**: Business logic validation with weighted scoring
3. **Advanced Configuration**: Complex validation with pattern matching and conditional logic
4. **Static Data Validation & Enrichment**: Comprehensive data enrichment from repositories
5. **Performance Monitoring**: Metrics collection and performance analysis
6. **Exception Handling**: Error scenarios and recovery mechanisms

#### Technical Architecture
- **5 Comprehensive Database Tables**: Commodity swaps, reference data, client data, counterparty data, audit logs
- **Realistic Market Data**: Energy (WTI, Brent, Henry Hub), Metals (Gold, Silver), Agricultural (Corn)
- **Production Patterns**: Proper indexing, constraints, and audit trail structures
- **API Progression**: Demonstrates evolution from simple to sophisticated usage patterns

#### Expected Results
- **Progressive Learning**: Clear understanding of API evolution and complexity management
- **Performance Optimization**: Sub-100ms processing with comprehensive metrics
- **Data Integration**: Understanding of static data enrichment patterns
- **Business Logic**: Sophisticated validation with weighted scoring and conditional processing

### 3. OTC Options Bootstrap Demo
**File**: `OtcOptionsBootstrapDemo.java`
**Focus**: Multiple data integration methods and Spring Boot integration

#### What This Demo Demonstrates
- **Three Data Lookup Methods**: Comprehensive demonstration of different integration approaches
- **Spring Boot Integration**: Complete application with proper dependency injection
- **Major Commodity Coverage**: Natural Gas, Oil, Metals, Agricultural products
- **Realistic Financial Data**: Authentic OTC Options structures and market conventions
- **Complete Integration**: Full end-to-end processing with external data sources

#### Data Integration Methods
1. **Method 1: Inline Datasets**: Embedded reference data for small, static lookups
2. **Method 2: PostgreSQL Database**: Dynamic data retrieval with connection pooling
3. **Method 3: External YAML Files**: Shared reference data with file-based storage

#### Technical Architecture
- **PostgreSQL Counterparty Data**: Realistic counterparty reference information
- **External YAML Datasets**: Market and currency data in external files
- **XML Sample Data**: Authentic OTC Options covering major commodity classes
- **Spring Boot Application**: Complete integration with dependency injection
- **Multiple Data Sources**: Demonstration of when to use each approach

#### Expected Results
- **Data Integration Understanding**: Clear guidance on when to use each data lookup method
- **Spring Boot Proficiency**: Understanding of proper integration patterns
- **Financial Data Modeling**: Experience with realistic OTC Options structures
- **Performance Optimization**: Understanding of different data access performance characteristics

### 4. Scenario-Based Processing Demo
**File**: `ScenarioBasedProcessingDemo.java`
**Focus**: Automatic data type routing and centralized configuration management

#### What This Demo Demonstrates
- **Automatic Data Type Detection**: Intelligent routing based on data structure analysis
- **Centralized Registry**: Single configuration point for all scenario management
- **Flexible Routing**: Support for multiple scenarios per data type
- **Graceful Degradation**: Proper handling of edge cases and unknown data types
- **Configuration Management**: Centralized scenario registry with lightweight routing

#### Supported Data Types
- **OTC Options**: Derivatives-specific processing with options validation
- **Commodity Swaps**: Multi-layered validation with commodity-specific rules
- **Settlement Instructions**: Auto-repair logic with settlement-specific processing
- **Unknown Types**: Fallback processing with graceful error handling

#### Technical Architecture
- **Scenario Registry**: Central catalog of all available scenarios
- **Data Type Detection**: Automatic routing based on object structure
- **Lightweight Routing**: Scenario files contain only mappings and references
- **Fallback Mechanisms**: Graceful handling of unknown or unsupported data types

#### Expected Results
- **Configuration Management**: Understanding of centralized scenario management
- **Routing Logic**: Experience with automatic data type detection and routing
- **Scalability Patterns**: Understanding of how to manage complex configuration hierarchies
- **Error Handling**: Proper handling of edge cases and unknown data types

## Running Bootstrap Demonstrations

### Prerequisites
- Java 21 or higher
- Maven 3.6 or higher
- PostgreSQL (optional - demos will use in-memory database if unavailable)
- 4GB RAM minimum (8GB recommended for optimal performance)

### Quick Start
```bash
# Navigate to demo module
cd apex-demo

# Run all demos in sequence (recommended for learning)
./scripts/run-demos.sh     # Linux/Mac
./scripts/run-demos.bat    # Windows

# Or run individual demos
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.OtcOptionsBootstrapDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CommoditySwapValidationBootstrap"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CustodyAutoRepairBootstrap"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.ScenarioBasedProcessingDemo"
```

### Environment Configuration
```bash
# Run with specific profiles for different environments
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CustodyAutoRepairBootstrap" -Dspring.profiles.active=dev

# Enable debug logging for detailed analysis
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CommoditySwapValidationBootstrap" -Dlogging.level.dev.mars.apex=DEBUG

# Run with PostgreSQL connection (if available)
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.OtcOptionsBootstrapDemo" -Dspring.datasource.url=jdbc:postgresql://localhost:5432/apex_demo
```

## Recommended Learning Path

### For New Users (Total Time: 75-105 minutes)

#### Phase 1: Basic Data Integration (15-20 minutes)
**Start with OTC Options Bootstrap**
- Learn basic data integration patterns
- Understand different data source approaches
- See complete end-to-end processing
- Experience Spring Boot integration

#### Phase 2: API Progression (20-30 minutes)
**Progress to Commodity Swap Validation**
- Experience API evolution from simple to advanced
- Learn multi-layered validation techniques
- Understand performance monitoring
- See realistic market data integration

#### Phase 3: Real-World Business Logic (25-35 minutes)
**Explore Custody Auto-Repair**
- See sophisticated business logic in action
- Experience weighted rule-based decision making
- Understand exception handling and edge cases
- Learn about regulatory compliance and audit trails

#### Phase 4: Advanced Configuration (15-20 minutes)
**Finish with Scenario-Based Processing**
- Learn advanced routing and configuration management
- Understand centralized scenario management
- See how different data types are handled
- Experience graceful degradation patterns

### For Experienced Users (Total Time: 45-60 minutes)

#### Fast Track Learning Path
1. **Quick Review of OTC Options** (10 minutes) - Focus on data integration patterns
2. **Deep Dive into Commodity Swap Validation** (15-20 minutes) - API progression and validation layers
3. **Comprehensive Analysis of Custody Auto-Repair** (15-20 minutes) - Business logic and performance
4. **Configuration Management with Scenario-Based Processing** (5-10 minutes) - Advanced routing patterns

### For Architects and Technical Leaders (Total Time: 30-45 minutes)

#### Architecture-Focused Path
1. **Technical Architecture Review** - Focus on infrastructure setup and integration patterns
2. **Performance Analysis** - Deep dive into sub-100ms processing and metrics collection
3. **Configuration Management** - Understanding of YAML validation and scenario management
4. **Integration Patterns** - Spring Boot integration and data source management

## What to Expect During Execution

### Startup and Initialization
Each bootstrap demo begins with clear startup messages indicating:
- Demo name and purpose
- Infrastructure setup progress
- Configuration loading status
- Environment detection results
- Database connection status

### Infrastructure Setup
Automatic creation and setup of:
- Database tables with proper schemas and constraints
- Sample data generation with realistic relationships
- External file creation (YAML, XML) as needed
- Configuration validation and loading
- Performance monitoring initialization

### Scenario Execution
Progressive execution through multiple scenarios:
- Clear scenario identification and purpose
- Real-time processing with performance metrics
- Detailed logging of all processing steps
- Success/failure indicators for each operation
- Comprehensive results analysis

### Performance Metrics
Real-time performance monitoring including:
- Individual operation timing (sub-100ms targets)
- Aggregate processing times and throughput
- Success rates and error analysis
- Memory usage and resource utilization
- Comparative analysis across scenarios

### Results Analysis
Comprehensive analysis and reporting:
- Summary of all scenarios executed
- Performance metrics and benchmark comparisons
- Success rates and error analysis
- Key learnings and takeaways
- Recommendations for next steps

## Integration with REST API

All bootstrap demos integrate seamlessly with the APEX REST API, providing practical examples of:

### API Usage Patterns
- Rule evaluation via HTTP endpoints
- Configuration management through API calls
- Performance monitoring through actuator endpoints
- Interactive testing through Swagger UI

### Starting the REST API
```bash
# Start the API server
cd apex-rest-api
mvn spring-boot:run

# Access interactive documentation
open http://localhost:8080/swagger-ui.html

# Check health status
curl http://localhost:8080/actuator/health
```

### API Integration Examples
Each bootstrap demo shows how to:
- Submit rule evaluation requests via REST API
- Load and validate YAML configurations through API endpoints
- Monitor performance metrics through actuator endpoints
- Handle errors and exceptions in API responses

## Troubleshooting and Support

### Common Issues and Solutions

#### Database Connection Issues
**Problem**: PostgreSQL connection failures
**Solution**: Demos automatically fall back to in-memory H2 database
**Verification**: Check startup logs for database connection status

#### Memory Issues
**Problem**: OutOfMemoryError during execution
**Solution**: Increase JVM heap size: `-Xmx4g -Xms2g`
**Prevention**: Ensure minimum 4GB RAM available

#### Configuration Loading Issues
**Problem**: YAML configuration validation errors
**Solution**: Check file paths and YAML syntax
**Verification**: Enable debug logging for detailed error messages

#### Performance Issues
**Problem**: Processing times exceed 100ms targets
**Solution**: Check system resources and database performance
**Optimization**: Use connection pooling and enable caching

### Getting Help

#### Self-Service Resources
- **Comprehensive Documentation**: Detailed guides for each bootstrap demo
- **Source Code Analysis**: Well-commented code with clear explanations
- **Configuration Examples**: Working YAML configurations with annotations
- **Performance Benchmarks**: Expected performance metrics and targets

#### Community Support
- **GitHub Issues**: Report bugs and request features
- **Discussion Forums**: Community-driven support and knowledge sharing
- **Documentation Contributions**: Help improve documentation and examples

#### Professional Support
- **Implementation Consulting**: Expert guidance for production deployments
- **Performance Optimization**: Tuning and optimization services
- **Custom Development**: Tailored solutions for specific requirements
- **Training and Workshops**: Comprehensive training programs

## Next Steps

After completing the bootstrap demonstrations:

### Immediate Next Steps
1. **Explore REST API**: Use Swagger UI to test API endpoints interactively
2. **Review Source Code**: Analyze bootstrap implementation for patterns and best practices
3. **Customize Configurations**: Modify YAML configurations to understand impact
4. **Performance Testing**: Run demos with different data volumes and configurations

### Integration Planning
1. **Assess Requirements**: Determine which patterns apply to your use cases
2. **Architecture Design**: Plan integration with existing systems
3. **Data Integration**: Design data source integration strategy
4. **Performance Planning**: Establish performance targets and monitoring

### Production Readiness
1. **Security Review**: Implement appropriate security measures
2. **Monitoring Setup**: Configure production monitoring and alerting
3. **Deployment Planning**: Plan deployment strategy and rollback procedures
4. **Documentation**: Create operational documentation and runbooks

---

**Last Updated**: August 22, 2025
**Bootstrap Demos Version**: 1.0-SNAPSHOT
**APEX Version**: 1.0-SNAPSHOT

This comprehensive guide provides everything needed to understand, run, and learn from the APEX bootstrap demonstrations. The demos serve as both educational tools and practical templates for implementing similar solutions in production environments.
