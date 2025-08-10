# APEX Rules Engine REST API - Quick Reference

**Version:** 1.0
**Date:** 2025-08-02
**Author:** Mark Andrew Ray-Smith Cityline Ltd

## Base URL
```
http://localhost:8080/api
```

## Endpoints Overview

### 🔄 Transformation API (`/api/transformations`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/transformers` | Get registered transformers |
| POST | `/{transformerName}` | Transform data with registered transformer |
| POST | `/dynamic` | Transform with dynamic rules |
| POST | `/{transformerName}/detailed` | Transform with detailed result |

### 🔍 Enrichment API (`/api/enrichment`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/configurations` | Get predefined configurations |
| POST | `/enrich` | Enrich object with YAML config |
| POST | `/batch` | Batch enrichment |
| POST | `/predefined/{configName}` | Enrich with predefined config |

### 📄 Template Processing API (`/api/templates`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/json` | Process JSON template |
| POST | `/xml` | Process XML template |
| POST | `/text` | Process text template |
| POST | `/batch` | Process multiple templates |

### 🗄️ Data Source API (`/api/datasources`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get all data sources |
| GET | `/{name}` | Get specific data source |
| POST | `/{name}/test` | Test data source |
| POST | `/{name}/lookup` | Perform lookup |

### 🧮 Expression API (`/api/expressions`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/evaluate` | Evaluate expression |
| POST | `/evaluate/detailed` | Evaluate with detailed result |
| POST | `/batch` | Batch expression evaluation |
| POST | `/validate` | Validate expression syntax |
| GET | `/functions` | Get available functions |

### ⚡ Rules API (`/api/rules`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/execute` | Execute single rule |
| POST | `/batch` | Execute batch rules |

## Common Request Patterns

### Transform Data
```json
POST /api/transformations/dynamic
{
  "data": { "firstName": "john", "email": "JOHN@EXAMPLE.COM" },
  "transformerRules": [
    {
      "name": "normalize-name",
      "condition": "#firstName != null",
      "transformation": "#firstName.substring(0,1).toUpperCase() + #firstName.substring(1).toLowerCase()",
      "targetField": "firstName"
    }
  ]
}
```

### Enrich Object
```json
POST /api/enrichment/enrich
{
  "targetObject": { "customerId": "CUST001" },
  "yamlConfiguration": "metadata:\n  name: \"Customer Enrichment\"\nenrichments:\n  - name: \"lookup\"\n    condition: \"#customerId != null\""
}
```

### Process Template
```json
POST /api/templates/json
{
  "template": "{\"id\": \"#{#customerId}\", \"name\": \"#{#customerName}\"}",
  "context": { "customerId": "CUST001", "customerName": "John Doe" }
}
```

### Evaluate Expression
```json
POST /api/expressions/evaluate
{
  "expression": "#amount * #rate + #fee",
  "context": { "amount": 1000, "rate": 0.05, "fee": 25 }
}
```

### Execute Rule
```json
POST /api/rules/execute
{
  "rule": {
    "name": "high-value",
    "condition": "#amount > 1000",
    "message": "High value transaction"
  },
  "facts": { "amount": 1500 }
}
```

## Response Format

### Success Response
```json
{
  "success": true,
  "data": { /* response data */ },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Error Response
```json
{
  "success": false,
  "error": "Error category",
  "message": "Detailed error description",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## HTTP Status Codes
- `200 OK` - Success
- `400 Bad Request` - Invalid request
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## SpEL Expression Syntax

### Variables
- Use `#` prefix: `#amount`, `#customer.name`

### Operators
- Arithmetic: `+`, `-`, `*`, `/`, `%`
- Comparison: `==`, `!=`, `<`, `>`, `<=`, `>=`
- Logical: `&&`, `||`, `!`
- Ternary: `condition ? value1 : value2`

### Common Functions
- String: `#text.length()`, `#text.toLowerCase()`, `#text.substring(0,5)`
- Math: `T(java.lang.Math).abs(#value)`, `T(java.lang.Math).max(#a, #b)`
- Date: `T(java.time.LocalDate).now()`, `T(java.time.Instant).now()`

### Collections
- Filter: `#items.?[price > 100]`
- Project: `#items.![name]`
- First: `#items.^[price > 100]`
- Last: `#items.$[price > 100]`

## Batch Operations

### Batch Rules
```json
{
  "rules": [
    { "name": "rule1", "condition": "#amount > 1000", "message": "High value" },
    { "name": "rule2", "condition": "#tier == 'GOLD'", "message": "Gold customer" }
  ],
  "facts": { "amount": 1500, "tier": "GOLD" }
}
```

### Batch Expressions
```json
{
  "expressions": [
    { "name": "calc1", "expression": "#amount * #rate" },
    { "name": "calc2", "expression": "#amount > 1000" }
  ],
  "context": { "amount": 1500, "rate": 0.05 }
}
```

### Batch Templates
```json
{
  "templates": [
    { "name": "json-template", "type": "JSON", "template": "{\"id\": \"#{#id}\"}" },
    { "name": "xml-template", "type": "XML", "template": "<id>#{#id}</id>" }
  ],
  "context": { "id": "CUST001" }
}
```

## Common Workflows

### 1. Data Processing Pipeline
```
Transform → Enrich → Apply Rules → Generate Templates
```

### 2. Risk Assessment
```
Evaluate Risk Expressions → Apply Risk Rules → Generate Alerts
```

### 3. Customer Onboarding
```
Normalize Data → Enrich Profile → Validate Rules → Create Welcome Message
```

## Performance Tips

1. **Use Batch Operations** for multiple items
2. **Cache Results** for frequently used data
3. **Validate Expressions** before evaluation
4. **Limit Batch Sizes** to < 100 items
5. **Monitor Response Times** using timestamps

## Error Handling Best Practices

1. **Check `success` field** in responses
2. **Implement retry logic** for transient failures
3. **Validate input** before sending requests
4. **Handle partial failures** in batch operations
5. **Log errors** with request context

## Testing Endpoints

### Using curl
```bash
# Test expression evaluation
curl -X POST http://localhost:8080/api/expressions/evaluate \
  -H "Content-Type: application/json" \
  -d '{"expression": "#amount > 1000", "context": {"amount": 1500}}'

# Test rule execution
curl -X POST http://localhost:8080/api/rules/execute \
  -H "Content-Type: application/json" \
  -d '{"rule": {"name": "test", "condition": "#value > 0", "message": "Positive"}, "facts": {"value": 10}}'
```

### Using Swagger UI
Visit: `http://localhost:8080/swagger-ui.html`

## Configuration

### Application Properties
```yaml
# Enable debug logging
logging:
  level:
    dev.mars.apex: DEBUG

# Configure timeouts
timeouts:
  rule-execution: 5000
  expression-evaluation: 2000

# Set batch limits
limits:
  max-batch-size: 100
```

## Monitoring

### Key Metrics to Monitor
- Response times per endpoint
- Success/failure rates
- Expression evaluation performance
- Data source lookup times
- Memory usage during batch operations

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

## Support

- **Documentation**: `/swagger-ui.html`
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Source Code**: Check controller implementations for detailed behavior
