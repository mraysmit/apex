# APEX REST API

A comprehensive REST API for APEX (Advanced Processing Engine for eXpressions) with YAML Dataset Enrichment functionality.

## Overview

This module provides a complete REST API interface for APEX, enabling rule evaluation, validation, configuration management, and system monitoring through HTTP endpoints. The API is built with Spring Boot and includes OpenAPI/Swagger documentation.

## Features

- **Rule Evaluation**: Simple rule checking and complex validation scenarios
- **Expression Evaluation**: SpEL expression processing with context management
- **Data Transformation**: Transform and normalize data using configurable rules
- **Object Enrichment**: Enrich objects with data from external sources and datasets
- **Template Processing**: Generate dynamic content using templates with SpEL expressions
- **Data Source Management**: Manage and interact with external data sources
- **Configuration Management**: Load and manage YAML configurations via API
- **Performance Monitoring**: System health checks and performance metrics
- **OpenAPI Documentation**: Interactive API documentation with Swagger UI
- **Spring Boot Actuator**: Production-ready monitoring and management endpoints
- **Comprehensive Error Handling**: Detailed error responses and validation
- **Named Rules Management**: Define and reuse named rules
- **Bootstrap Integration**: Complete integration with APEX bootstrap demonstrations

## Quick Start

### 1. Build and Run

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/apex-rest-api-1.0-SNAPSHOT.jar

# Or run with Maven
mvn spring-boot:run
```

### 2. Access the API

- **API Base URL**: http://localhost:8080/api/
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Documentation**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/actuator/health

### 3. Simple Rule Check

```bash
curl -X POST http://localhost:8080/api/rules/check \
  -H "Content-Type: application/json" \
  -d '{
    "condition": "#age >= 18",
    "data": {"age": 25},
    "ruleName": "age-check",
    "message": "User is an adult"
  }'
```

Response:
```json
{
  "success": true,
  "matched": true,
  "ruleName": "age-check",
  "message": "User is an adult",
  "timestamp": "2024-07-27T10:30:00Z",
  "evaluationId": "uuid-here"
}
```

## API Endpoints

### Rule Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/rules/check` | Evaluate a single rule condition |
| POST | `/api/rules/validate` | Validate data against multiple rules |
| POST | `/api/rules/define/{name}` | Define a named rule for reuse |
| POST | `/api/rules/test/{name}` | Test a previously defined rule |
| GET | `/api/rules/defined` | Get all defined rules |

### Expression Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/expressions/evaluate` | Evaluate SpEL expressions with context |
| POST | `/api/expressions/validate` | Validate SpEL expression syntax |
| POST | `/api/expressions/batch` | Evaluate multiple expressions |
| GET | `/api/expressions/functions` | Get available SpEL functions |

### Transformation Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/transformations/apply` | Apply transformation rules to data |
| POST | `/api/transformations/register` | Register a new transformer |
| POST | `/api/transformations/batch` | Transform multiple objects |
| GET | `/api/transformations/registered` | Get registered transformers |

### Enrichment Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/enrichments/enrich` | Enrich objects with external data |
| POST | `/api/enrichments/batch` | Enrich multiple objects |
| POST | `/api/enrichments/configure` | Configure enrichment rules |
| GET | `/api/enrichments/sources` | Get available enrichment sources |

### Template Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/templates/process` | Process templates with SpEL expressions |
| POST | `/api/templates/batch` | Process multiple templates |
| POST | `/api/templates/validate` | Validate template syntax |
| GET | `/api/templates/variables` | Get available template variables |

### Data Source Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/datasources/list` | List available data sources |
| GET | `/api/datasources/{name}/health` | Check data source health |
| POST | `/api/datasources/{name}/query` | Execute query on data source |
| GET | `/api/datasources/{name}/stats` | Get data source statistics |

### Configuration Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/config/info` | Get current configuration information |
| POST | `/api/config/load` | Load YAML configuration from content |
| POST | `/api/config/upload` | Upload and load YAML configuration file |
| POST | `/api/config/validate` | Validate YAML configuration |

### Monitoring & Health

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/monitoring/health` | Health check endpoint |
| GET | `/api/monitoring/stats` | System statistics |
| GET | `/api/monitoring/performance` | Performance metrics |
| GET | `/api/monitoring/ready` | Readiness check |

### Actuator Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Spring Boot health check |

## Bootstrap Demo Integration

The REST API is fully integrated with APEX bootstrap demonstrations, providing real-world examples of API usage:

### Available Bootstrap Demos

#### 1. Custody Auto-Repair Bootstrap
Demonstrates custody settlement auto-repair using the REST API:
```bash
# Start the API server
mvn spring-boot:run

# Run the bootstrap demo (uses REST API internally)
cd ../apex-demo
mvn exec:java -Dexec.mainClass="dev.mars.apex.demo.bootstrap.CustodyAutoRepairBootstrap"
```

#### 2. Commodity Swap Validation Bootstrap
Shows progressive API usage from simple to advanced:
```bash
# Example API calls demonstrated in the bootstrap
curl -X POST http://localhost:8080/api/rules/check \
  -H "Content-Type: application/json" \
  -d '{"condition": "#notionalAmount > 1000000", "data": {"notionalAmount": 5000000}}'

curl -X POST http://localhost:8080/api/enrichments/enrich \
  -H "Content-Type: application/json" \
  -d '{"data": {"currency": "USD"}, "enrichmentConfig": "currency-enrichment"}'
```

#### 3. OTC Options Bootstrap Demo
Demonstrates multiple data source integration via API:
```bash
# Data source health check
curl -X GET http://localhost:8080/api/datasources/counterparty-db/health

# Query external data source
curl -X POST http://localhost:8080/api/datasources/counterparty-db/query \
  -H "Content-Type: application/json" \
  -d '{"query": "getCounterpartyById", "parameters": {"id": "GOLDMAN_SACHS"}}'
```

#### 4. Scenario-Based Processing Demo
Shows automatic routing and processing via API:
```bash
# Process different data types automatically
curl -X POST http://localhost:8080/api/rules/validate \
  -H "Content-Type: application/json" \
  -d '{"data": {"dataType": "OTC_OPTION", "underlying": "Natural Gas"}, "scenario": "auto-detect"}'
```

### Learning Path with REST API

1. **Start the API Server**: `mvn spring-boot:run`
2. **Explore Swagger UI**: http://localhost:8080/swagger-ui.html
3. **Run Bootstrap Demos**: Each demo shows different API usage patterns
4. **Try Interactive Examples**: Use Swagger UI to test endpoints
5. **Review Demo Source Code**: See how demos integrate with the API
| GET | `/actuator/info` | Application information |
| GET | `/actuator/metrics` | Application metrics |

## Usage Examples

### 1. Complex Validation

```bash
curl -X POST http://localhost:8080/api/rules/validate \
  -H "Content-Type: application/json" \
  -d '{
    "data": {"age": 16, "email": null, "balance": 1500},
    "validationRules": [
      {
        "name": "age-check",
        "condition": "#data.age >= 18",
        "message": "Must be at least 18 years old",
        "severity": "ERROR"
      },
      {
        "name": "email-check",
        "condition": "#data.email != null",
        "message": "Email address is required",
        "severity": "ERROR"
      }
    ],
    "includeDetails": true
  }'
```

### 2. Define and Test Named Rules

```bash
# Define a rule
curl -X POST http://localhost:8080/api/rules/define/positive-balance \
  -H "Content-Type: application/json" \
  -d '{
    "condition": "#balance > 0",
    "message": "Balance must be positive"
  }'

# Test the rule
curl -X POST http://localhost:8080/api/rules/test/positive-balance \
  -H "Content-Type: application/json" \
  -d '{"balance": 1000}'
```

### 3. Load Configuration

```bash
curl -X POST http://localhost:8080/api/config/load \
  -H "Content-Type: application/x-yaml" \
  -d '
metadata:
  name: "My Rules"
  version: "1.0.0"

rules:
  - id: "sample-rule"
    name: "Sample Rule"
    condition: "#value > 100"
    message: "Value exceeds threshold"
'
```

## Configuration

### Application Properties

Key configuration properties in `application.yml`:

```yaml
# Server configuration
server:
  port: 8080

# Rules Engine configuration
rules:
  config:
    default-path: classpath:rules/default-rules.yaml
  performance:
    monitoring:
      enabled: true
  error:
    recovery:
      enabled: true

# OpenAPI configuration
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

### Environment Profiles

- **Development**: `spring.profiles.active=dev`
- **Production**: `spring.profiles.active=prod`
- **Testing**: `spring.profiles.active=test`

## Error Handling

The API provides comprehensive error handling with detailed error responses:

```json
{
  "success": false,
  "error": "Rule evaluation failed",
  "errorDetails": "Invalid SpEL expression: #invalid.syntax",
  "timestamp": "2024-07-27T10:30:00Z",
  "evaluationId": "uuid-here"
}
```

## Security Considerations

### Current Implementation
- No authentication/authorization (suitable for internal/trusted networks)
- Input validation on all endpoints
- Error message sanitization

### Production Recommendations
- Add Spring Security for authentication/authorization
- Implement API key or JWT token authentication
- Add rate limiting
- Use HTTPS in production
- Implement audit logging

## Monitoring and Observability

### Health Checks
- Basic functionality testing
- Memory usage monitoring
- System resource checks

### Metrics
- Rule evaluation performance
- API request/response times
- System resource utilization
- Error rates and patterns

### Logging
- Structured logging with correlation IDs
- Configurable log levels
- Separate log files for different components

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=RulesControllerTest

# Run with specific profile
mvn test -Dspring.profiles.active=test
```

### Building

```bash
# Build JAR
mvn clean package

# Build Docker image (if Dockerfile exists)
docker build -t rules-engine-rest-api .
```

### IDE Setup

1. Import as Maven project
2. Set Java 17 as project SDK
3. Enable annotation processing
4. Configure code style (if applicable)

## Deployment

### Standalone JAR

```bash
java -jar rules-engine-rest-api-1.0-SNAPSHOT.jar
```

### Docker

```bash
docker run -p 8080:8080 rules-engine-rest-api
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rules-engine-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: rules-engine-api
  template:
    metadata:
      labels:
        app: rules-engine-api
    spec:
      containers:
      - name: api
        image: rules-engine-rest-api:latest
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /api/monitoring/health
            port: 8080
        readinessProbe:
          httpGet:
            path: /api/monitoring/ready
            port: 8080
```

## Contributing

1. Follow the existing code style and patterns
2. Add tests for new functionality
3. Update documentation for API changes
4. Ensure all tests pass before submitting

## Support

- Check the Swagger UI for interactive API documentation
- Review the logs for detailed error information
- Use the health check endpoints for system status
- Refer to the main Rules Engine documentation for core concepts
