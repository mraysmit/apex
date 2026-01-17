# APEX Dual Format Examples

This directory contains demonstration files showcasing APEX 2.2's **dual format support** for `queries`, `operations`, and `endpoints` in data source configurations.

## 📁 Files in This Directory

### 1. `array-format-showcase.yaml`
**Purpose**: Comprehensive demonstration of array format with financial services patterns

**Highlights:**
- Real-world trading platform queries and operations
- Rich metadata: descriptions, tags, performance hints
- Enterprise patterns: caching, circuit breakers, connection pooling
- Financial domain examples: trades, positions, counterparties, FX rates

**Use this file to:**
- Learn array format syntax and best practices
- Understand when metadata adds value
- See enterprise-grade configuration patterns
- Copy patterns for your own projects

### 2. `migration-comparison.yaml`
**Purpose**: Side-by-side comparison of map format vs. array format

**Highlights:**
- Legacy map format examples (still fully supported)
- Modern array format examples (new in APEX 2.2)
- Mixed format examples (both in same file)
- Migration strategy guidance
- When to use each format

**Use this file to:**
- Understand format differences
- Plan migration strategy
- Learn mixed format approach
- Make informed format decisions

## 🎯 Quick Start

### Running the Examples

These are demonstration YAML files showing syntax patterns. To use them:

**Option 1: Interactive Playground**
```bash
cd apex-playground
mvn spring-boot:run
# Open http://localhost:8081/playground
# Load example files to see syntax highlighting and validation
```

**Option 2: Compiler Validation**
```bash
cd apex-compiler
mvn exec:java -Dexec.args="../apex-demo/src/test/resources/examples/array-format-showcase.yaml"
```

**Option 3: Integration Tests**
Copy patterns into your test configurations and reference them in test classes extending `ColoredTestOutputExtension`.

## 🔧 Format Decision Guide

### Use **Map Format** (Legacy - Concise)
```yaml
queries:
  getCustomer: "SELECT * FROM customers WHERE id = :id"
```

✅ **When:**
- Small projects (< 10 queries)
- Simple, self-explanatory queries
- Internal tools with minimal documentation needs
- Rapid prototyping

### Use **Array Format** (New - Metadata-Rich)
```yaml
queries:
  - name: "getCustomer"
    value: "SELECT * FROM customers WHERE id = :id"
    description: "Retrieve customer profile by unique ID"
    tags: ["customer-management", "primary-lookup"]
    owner: "customer-team"
```

✅ **When:**
- Enterprise projects with multiple teams
- Complex queries requiring documentation
- Compliance/audit requirements
- Shared configurations across departments
- APIs with SLA tracking needs

### Use **Mixed Format** (Best of Both Worlds)
```yaml
queries:
  # Simple query - map format
  simpleCount: "SELECT COUNT(*) FROM users"
  
  # Complex query - array format
  - name: "complexReport"
    value: "SELECT dept, COUNT(*) FROM employees GROUP BY dept"
    description: "Department staffing analysis"
```

✅ **When:**
- Gradual migration from map to array format
- Mix of simple and complex queries
- Want flexibility to choose per query

## 📊 Array Format Field Reference

### Required Fields
| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Unique identifier (becomes map key) |
| `value` | String | Query/operation/endpoint value |

### Common Optional Fields
| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `description` | String | Human-readable purpose | "Retrieve customer by ID" |
| `tags` | Array | Classification tags | ["customer", "lookup"] |
| `owner` | String | Team/person responsible | "customer-team" |
| `performance` | String | Performance characteristic | "indexed", "slow" |
| `cacheable` | Boolean | Can results be cached? | true |
| `cache-ttl` | String | Cache time-to-live | "300s" |
| `transaction` | String | Transaction requirement | "required" |
| `audit` | Boolean | Enable audit logging | true |
| `method` | String | HTTP method (endpoints) | "POST", "GET" |
| `timeout` | String | Timeout duration | "5000ms" |
| `retry-policy` | String | Retry strategy | "exponential-backoff" |
| `sla` | String | Service-level agreement | "99.9%" |

**Custom Fields**: Add any metadata your team needs - the array format is extensible!

## 🚨 Common Errors & Solutions

### Error: Duplicate Key
```yaml
queries:
  - name: "getCustomer"
    value: "SELECT * FROM customers WHERE id = :id"
  - name: "getCustomer"  # ❌ DUPLICATE!
    value: "SELECT * FROM customers WHERE email = :email"
```

**Solution**: Use unique names for each entry
```yaml
queries:
  - name: "getCustomerById"
    value: "SELECT * FROM customers WHERE id = :id"
  - name: "getCustomerByEmail"
    value: "SELECT * FROM customers WHERE email = :email"
```

### Error: Missing Required Field
```yaml
queries:
  - value: "SELECT * FROM customers"  # ❌ Missing 'name'!
```

**Solution**: Always include both `name` and `value`
```yaml
queries:
  - name: "getAllCustomers"
    value: "SELECT * FROM customers"
```

### Error: Invalid Format
```yaml
queries:
  - "just-a-string"  # ❌ Not an object!
```

**Solution**: Use object format with `name` and `value`
```yaml
queries:
  - name: "myQuery"
    value: "SELECT * FROM table"
```

## 🎓 Best Practices

### 1. Consistent Naming Conventions
```yaml
queries:
  - name: "getCustomerById"      # ✅ Verb + Noun + Qualifier
  - name: "listActiveOrders"     # ✅ Clear action
  - name: "validateCounterparty" # ✅ Descriptive
```

### 2. Meaningful Descriptions
```yaml
queries:
  - name: "getTradeByID"
    value: "SELECT * FROM trades WHERE trade_id = :id"
    description: "Retrieve single trade by unique identifier (primary key lookup)"  # ✅ Explains purpose and approach
```

### 3. Useful Tags
```yaml
queries:
  - name: "getActivePositions"
    value: "SELECT * FROM positions WHERE status = 'ACTIVE'"
    tags: ["position-management", "portfolio", "read-only"]  # ✅ Enables filtering and discovery
```

### 4. Document Performance Characteristics
```yaml
queries:
  - name: "complexAggregation"
    value: "SELECT dept, COUNT(*) FROM employees GROUP BY dept"
    description: "Department staffing report"
    performance: "indexed-on-dept"  # ✅ Helps with troubleshooting
    cacheable: true
    cache-ttl: "600s"
```

## 🔗 Related Resources

### Documentation
- **[APEX_YAML_REFERENCE.md](../../docs/APEX_YAML_REFERENCE.md)** - Section 2.3: Dual Format Support
- **[DUAL_FORMAT_IMPLEMENTATION_SUMMARY.md](../../docs-design/design/DUAL_FORMAT_IMPLEMENTATION_SUMMARY.md)** - Technical details
- **[DUAL_FORMAT_FEATURE_COMPLETE.md](../../docs-design/design/DUAL_FORMAT_FEATURE_COMPLETE.md)** - Feature completion report

### Test Examples
- **DualFormatDeserializationTest.java** - Unit tests with validation examples
- **ExternalDataSourceIntegrationTest.java** - Integration test patterns

### Demo Projects
- **apex-demo** module - 16+ comprehensive demos showing real-world usage

## ❓ FAQ

**Q: Do I need to migrate existing configurations?**  
A: No! Map format is fully supported and remains valid. Only migrate if array format adds value for your use case.

**Q: Can I mix both formats in the same file?**  
A: Yes! APEX seamlessly handles mixed formats. Use what works best for each query.

**Q: What's the performance impact?**  
A: Negligible (<1ms overhead during YAML deserialization). Zero runtime impact.

**Q: Can I add custom metadata fields?**  
A: Yes! Array format is extensible. Add any fields your team needs for documentation/tooling.

**Q: How do I validate array format configurations?**  
A: Use apex-compiler or load in apex-playground for syntax validation and error checking.

**Q: Are there IDE tools for autocomplete?**  
A: Not yet. Future enhancement may include JSON Schema for IDE support.

---

**APEX Version**: 2.2+  
**Feature Status**: ✅ Production Ready  
**Last Updated**: January 17, 2026
