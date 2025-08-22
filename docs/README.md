# APEX Rules Engine REST API Documentation

Welcome to the comprehensive documentation for the APEX Rules Engine REST API. This documentation provides everything you need to integrate with and use the powerful rule-based processing capabilities of the APEX engine.

## Documentation Overview

### Core Documentation

#### [APEX REST API Guide](APEX_REST_API_GUIDE.md)
**The complete reference for all REST API endpoints**

This comprehensive guide covers:
- **Getting Started** - Setup and basic usage
- **All API Endpoints** - Detailed documentation for every endpoint
- **Request/Response Examples** - Real-world usage examples
- **Error Handling** - Complete error scenarios and responses
- **Best Practices** - Performance optimization and security guidelines
- **Complete Workflows** - End-to-end business process examples

**Perfect for:** Developers who need complete API reference and detailed examples

#### [Quick Reference Guide](APEX_REST_API_QUICK_REFERENCE.md)
**Fast lookup for developers who know the basics**

This quick reference includes:
- **Endpoint Summary** - All endpoints at a glance
- **Common Request Patterns** - Copy-paste ready examples
- **SpEL Expression Syntax** - Quick syntax reference
- **HTTP Status Codes** - Error code meanings
- **Performance Tips** - Quick optimization guidelines

**Perfect for:** Experienced developers who need quick lookup and examples

#### [API Changelog](APEX_REST_API_CHANGELOG.md)
**Complete history of API changes and new features**

This changelog documents:
- **Version 2.0.0** - Major REST API extension with 6 new controllers
- **New Features** - Detailed breakdown of all new capabilities
- **Breaking Changes** - Migration guidance (none for v2.0.0)
- **Bug Fixes** - Issues resolved in each version
- **Future Roadmap** - Planned enhancements

**Perfect for:** Teams tracking API evolution and planning upgrades

## Quick Start

### 1. Start the API Server
```bash
mvn spring-boot:run -pl apex-rest-api
```

### 2. Access Interactive Documentation
```
http://localhost:8080/swagger-ui.html
```

### 3. Test Your First API Call
```bash
curl -X POST http://localhost:8080/api/expressions/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "expression": "#amount * #rate + #fee",
    "context": {"amount": 1000, "rate": 0.05, "fee": 25}
  }'
```

## API Capabilities

### Rules Execution
Execute business rules individually or in batches
- **Single Rule Execution** - Test and execute individual rules
- **Batch Processing** - Apply multiple rules to the same facts
- **Named Rules Management** - Define and reuse named rules
- **Detailed Results** - Comprehensive execution information
- **Performance Metrics** - Track rule execution performance

### Expression Evaluation
Evaluate Spring Expression Language (SpEL) expressions
- **Syntax Validation** - Verify expressions before execution
- **Batch Evaluation** - Process multiple expressions efficiently
- **Function Discovery** - Explore available SpEL functions
- **Type-Safe Results** - Strongly typed expression results
- **Context-Aware Processing** - Rich context data for expression evaluation

### Data Transformation
Transform and normalize data using configurable rules
- **Dynamic Rules** - Apply transformation rules on-the-fly
- **Registered Transformers** - Use pre-configured transformation logic
- **Batch Processing** - Transform multiple objects efficiently
- **SpEL Integration** - Powerful expression-based transformations
- **Field Mapping** - Flexible source-to-target field transformations

### Object Enrichment
Enrich objects with additional data from external sources
- **YAML Configuration** - Flexible enrichment definitions
- **Multiple Sources** - Integrate data from various external systems
- **Batch Enrichment** - Process multiple objects simultaneously
- **Conditional Logic** - Apply enrichment based on conditions
- **Dataset Integration** - Inline and external dataset support

### Template Processing
Generate dynamic content using templates with SpEL expressions
- **Multi-Format Support** - JSON, XML, and text templates
- **Expression Evaluation** - Dynamic content generation
- **Batch Processing** - Process multiple templates together
- **Context-Aware** - Rich context data for template rendering
- **Variable Substitution** - Dynamic variable replacement

### Data Source Management
Manage and interact with external data sources
- **Source Discovery** - List and inspect available data sources
- **Connectivity Testing** - Verify data source health and performance
- **Standardized Lookups** - Consistent data retrieval interface
- **Performance Monitoring** - Track lookup times and success rates
- **Health Checks** - Real-time data source health monitoring

## Architecture Overview

### REST API Layer
- **6 Specialized Controllers** - Rules, Expressions, Transformations, Enrichments, Templates, Data Sources
- **Consistent Response Format** - Standardized JSON responses with comprehensive metadata
- **Comprehensive Validation** - Input validation using Jakarta Bean Validation
- **Error Handling** - Detailed error responses with troubleshooting info and context
- **Performance Monitoring** - Built-in response time tracking and metrics collection

### Service Layer
- **ExpressionEvaluationService** - High-level SpEL expression processing with context management
- **EnrichmentService** - Object enrichment with YAML dataset integration
- **TransformationService** - Data transformation with configurable rules
- **TemplateService** - Dynamic content generation with SpEL expressions
- **Enhanced DataServiceManager** - Improved data source management with health monitoring
- **Core Integration** - Seamless integration with APEX core services

### Integration Points
- **Swagger/OpenAPI** - Interactive API documentation with live testing capabilities
- **Spring Boot Actuator** - Health checks, metrics, and operational endpoints
- **Bootstrap Demos** - Complete end-to-end demonstration scenarios
- **Comprehensive Testing** - Unit, integration, performance, and demo tests

## Bootstrap Demonstrations

APEX includes comprehensive bootstrap demonstrations that showcase complete end-to-end scenarios with real-world data and infrastructure setup:

### Available Bootstrap Demos

#### 1. Custody Auto-Repair Bootstrap
**Complete custody settlement auto-repair demonstration for Asian markets**
- **Infrastructure**: PostgreSQL database with comprehensive settlement data
- **Scenarios**: 5 progressive scenarios from premium clients to exception handling
- **Features**: Weighted rule-based decision making, instruction enrichment, sub-100ms processing
- **Business Value**: Demonstrates 66% auto-repair success rate vs industry average 20-40%

#### 2. Commodity Swap Validation Bootstrap
**End-to-end commodity derivatives validation with static data enrichment**
- **Infrastructure**: 5 comprehensive database tables with realistic market data
- **Scenarios**: 6 learning scenarios from ultra-simple API to performance monitoring
- **Features**: Multi-layered validation, comprehensive enrichment, performance metrics
- **Coverage**: Energy (WTI, Brent), Metals (Gold, Silver), Agricultural (Corn) markets

#### 3. OTC Options Bootstrap Demo
**Comprehensive OTC Options processing with multiple data lookup methods**
- **Infrastructure**: PostgreSQL counterparty data, external YAML datasets, XML sample data
- **Features**: Three different data lookup approaches (inline, database, external files)
- **Coverage**: Major commodity classes (Natural Gas, Oil, Metals, Agricultural)
- **Integration**: Complete Spring Boot integration with realistic financial data

#### 4. Scenario-Based Processing Demo
**Automatic data type routing and scenario-specific processing**
- **Features**: Automatic data type detection, scenario-specific pipeline configuration
- **Coverage**: OTC Options, Commodity Swaps, Settlement Instructions
- **Architecture**: Centralized scenario registry with lightweight routing

### Running Bootstrap Demos

```bash
# Run all bootstrap demos
cd apex-demo
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CustodyAutoRepairBootstrap"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CommoditySwapValidationBootstrap"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.OtcOptionsBootstrapDemo"
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.ScenarioBasedProcessingDemo"

# Or use the provided scripts
./scripts/run-demos.bat    # Windows
./scripts/run-demos.sh     # Linux/Mac
```

## Real-World Use Cases

### Customer Onboarding Pipeline
```
Raw Data → Transform → Enrich → Validate → Generate Welcome Message
```
Perfect for processing new customer registrations with data normalization, profile enrichment, business rule validation, and personalized communication generation.

### Financial Transaction Processing
```
Transaction Data → Risk Assessment → Rule Validation → Alert Generation
```
Ideal for real-time transaction processing with risk scoring, compliance checking, and automated alert generation.

### Data Integration Workflows
```
External Data → Connectivity Test → Lookup → Transform → Enrich
```
Excellent for ETL processes that need to validate data sources, retrieve external data, and enrich internal datasets.

## Configuration and Deployment

### Development Setup
```yaml
# application-dev.yml
logging:
  level:
    dev.mars.apex: DEBUG

apex:
  rules:
    engines: [simple, advanced]
    performance:
      monitoring: true
```

### Production Configuration
```yaml
# application-prod.yml
server:
  port: 8080
  
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

apex:
  rules:
    performance:
      monitoring: true
    security:
      expression-validation: strict
```

## Testing Strategy

### Unit Tests
- **Controller Tests** - Isolated testing with mocked dependencies
- **Service Tests** - Business logic validation
- **DTO Tests** - Request/response object validation

### Integration Tests
- **End-to-End Workflows** - Complete API workflow testing
- **Error Scenarios** - Comprehensive error handling validation
- **Performance Tests** - Load and performance validation

### Demo Tests
- **Business Scenarios** - Real-world use case demonstrations
- **Workflow Examples** - Complete process implementations
- **Best Practice Demonstrations** - Optimal usage patterns

## Performance and Monitoring

### Key Metrics
- **Response Times** - Track API endpoint performance
- **Success Rates** - Monitor operation success/failure rates
- **Batch Efficiency** - Measure batch operation performance
- **Resource Usage** - Monitor memory and CPU utilization

### Monitoring Tools
- **Swagger UI** - Interactive testing and exploration
- **Actuator Endpoints** - Health checks and metrics
- **Application Logs** - Detailed operation logging
- **Performance Metrics** - Built-in response time tracking

## Security Considerations

### Input Validation
- **Request Validation** - Jakarta Bean Validation on all inputs
- **Expression Security** - SpEL expression validation and sanitization
- **Size Limits** - Configurable limits on request sizes and batch operations

### Production Security
- **HTTPS Enforcement** - Secure communication in production
- **Authentication** - OAuth 2.0 or JWT integration (planned)
- **Rate Limiting** - Protect against abuse (configurable)
- **Input Sanitization** - Prevent injection attacks

## Contributing and Support

### Getting Help
1. **Check Documentation** - Start with the comprehensive API guide
2. **Use Swagger UI** - Interactive testing and exploration
3. **Review Examples** - Study the demo tests and workflows
4. **Examine Source Code** - Controller implementations provide detailed behavior

### Contributing
1. **Follow Patterns** - Use existing controller patterns for consistency
2. **Add Tests** - Comprehensive test coverage for new features
3. **Update Documentation** - Keep documentation current with changes
4. **Performance Testing** - Validate performance impact of changes

## Roadmap

### Short Term (Next Release)
- **GraphQL Support** - Alternative query interface
- **WebSocket Endpoints** - Real-time rule execution
- **Advanced Caching** - Improved performance optimization

### Medium Term
- **Multi-Tenant Support** - Isolated rule execution environments
- **Machine Learning Integration** - AI-powered rule optimization
- **Advanced Security** - OAuth 2.0, JWT, and RBAC

### Long Term
- **Cloud-Native Features** - Kubernetes integration and scaling
- **Event-Driven Architecture** - Reactive rule processing
- **Advanced Analytics** - Rule performance and usage analytics

---

## Contact and Resources

- **Interactive Documentation**: `http://localhost:8080/swagger-ui.html`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Source Code**: Check the `apex-rest-api` module
- **Test Examples**: Review the `apex-demo` module

**Happy coding with the APEX Rules Engine REST API!**
