# APEX Demo Lookup Files - Simple Grid Format

## 📊 **YAML Files and Java Test Files Mapping Grid**

| # | **YAML File** | **Java Test File** | **Data Source** | **Status** | **Notes** |
|---|---------------|-------------------|-------------|------------|-----------|
| 1 | `advanced-caching-demo.yaml` | ❌ None | UNKNOWN | ORPHANED | No test file exists |
| 2 | `BarrierOptionNestedTest.yaml` | ✅ `BarrierOptionNestedTest.java` | INLINE | ACTIVE | ✅ RENAMED - Test loads from resources/lookup/ |
| 3 | `BasicRestApiLookupTest.yaml` | ✅ `BasicRestApiLookupTest.java` | REST_API | ACTIVE | ✅ RENAMED - Phase 2.1 implementation |
| 4 | `CompoundKeyLookupTest.yaml` | ✅ `CompoundKeyLookupTest.java` | INLINE | ACTIVE | ✅ COMPLETE - Compound key lookup with 5 test methods |
| 5 | `ComprehensiveLookupTest.yaml` | ✅ `ComprehensiveLookupTest.java` | H2_DB | ACTIVE | ✅ RENAMED - Consolidated test |
| 6 | `ConditionalExpressionLookupTest.yaml` | ✅ `ConditionalExpressionLookupTest.java` | INLINE | ACTIVE | ✅ COMPLETE - **FIXED YAML FIRST VIOLATION** |
| 7 | `CurrencyMarketMappingTest.yaml` | ✅ `CurrencyMarketMappingTest.java` | INLINE | ACTIVE | ✅ COMPLETE - Currency to market mapping with 5 test methods |
| 8 | `customer-profile-enrichment.yaml` | ❌ None | UNKNOWN | ORPHANED | No test file exists |
| 9 | `enhanced-rest-api-demo.yaml` | ✅ `EnhancedRestApiDemoTest.java` | REST_API | ACTIVE | Phase 2.2 implementation |
| 10 | `ExternalDataSourceWorkingDemoTest.yaml` | ✅ `ExternalDataSourceWorkingDemoTest.java` | YAML_FILE | ACTIVE | ✅ RENAMED - External data source demo |
| 11 | `FileSystemLookupDemoTest-json.yaml` | ✅ `FileSystemLookupDemoTest.java` | JSON_FILE | ACTIVE | ✅ RENAMED - JSON file processing |
| 12 | `FileSystemLookupDemoTest-xml.yaml` | ✅ `FileSystemLookupDemoTest.java` | XML_FILE | ACTIVE | ✅ RENAMED - XML file processing |
| 13 | `h2-custom-parameters-demo.yaml` | ❌ None | UNKNOWN | ORPHANED | Configuration demo only |
| 14 | `H2CustomParametersDemoTest.yaml` | ✅ `H2CustomParametersDemoTest.java` | H2_DB | ACTIVE | ✅ RENAMED - H2 parameters with enrichment |
| 15 | `mathematical-operations-lookup.yaml` | ❌ None | UNKNOWN | ORPHANED | No test file exists |
| 16 | `MultiParameterLookupTest.yaml` | ✅ `MultiParameterLookupTest.java` | H2_DB | ACTIVE | ✅ RENAMED - Multi-parameter database lookup |
| 17 | `NestedFieldLookupDemoTest.yaml` | ✅ `NestedFieldLookupDemoTest.java` | INLINE | ACTIVE | ✅ RENAMED - Nested field navigation |
| 18 | `PostgreSQLMultiParamLookupTest.yaml` | ✅ `PostgreSQLMultiParamLookupTest.java` | POSTGRESQL_DB | ACTIVE | ✅ RENAMED - Phase 1.2 implementation |
| 19 | `postgresql-simple-database-enrichment.yaml` | ❌ None | UNKNOWN | ORPHANED | No test file exists |
| 20 | `PostgreSQLSimpleLookupTest.yaml` | ✅ `PostgreSQLSimpleLookupTest.java` | POSTGRESQL_DB | ACTIVE | ✅ RENAMED - Phase 1.1 implementation |
| 21 | `settlement-instruction-enrichment.yaml` | ❌ None | UNKNOWN | ORPHANED | No test file exists |
| 22 | `SharedDatasourceDemoTest.yaml` | ✅ `SharedDatasourceDemoTest.java` | INLINE | ACTIVE | ✅ COMPLETE - **FIXED YAML FIRST VIOLATION** |
| 23 | `SimpleFieldLookupDemoTest.yaml` | ✅ `SimpleFieldLookupDemoTest.java` | INLINE | ACTIVE | ✅ RENAMED - Simple field lookup demo |

---

## 📊 **Java Test Files and YAML Files Mapping Grid**

| # | **Java Test File** | **YAML File(s) Used** | **Data Source** | **Status** | **Notes** |
|---|-------------------|----------------------|-------------|------------|-----------|
| 1 | `BarrierOptionNestedTest.java` | ✅ `barrier-option-nested-enrichment.yaml` | INLINE | ACTIVE | Loads from resources/lookup/ |
| 2 | `BasicRestApiLookupTest.java` | ✅ `basic-rest-api-lookup.yaml` | REST_API | ACTIVE | Phase 2.1 REST API |
| 3 | `CompoundKeyLookupDemoTest.java` | ✅ `compound-key-lookup.yaml` | INLINE | ACTIVE | References orphaned YAML |
| 4 | `ComprehensiveLookupTest.java` | ✅ `comprehensive-lookup-enrichment.yaml` | H2_DB | ACTIVE | Consolidated lookup test |
| 5 | `DatabaseConnectionTest.java` | ❌ None | NO_YAML | NO YAML | Direct JDBC testing |
| 6 | `EnhancedRestApiDemoTest.java` | ❌ None | NO_YAML | NO YAML | Uses JDK HTTP server directly |
| 7 | `ExternalDataSourceWorkingDemoTest.java` | ✅ `external-data-source-working-enrichment.yaml` | YAML_FILE | ACTIVE | External data source demo |
| 8 | `FileSystemLookupDemoTest.java` | ✅ `json-file-lookup.yaml` + `xml-file-lookup.yaml` | JSON_FILE + XML_FILE | ACTIVE | Uses both JSON and XML |
| 9 | `H2CustomParametersDemoTest.java` | ✅ `h2-custom-parameters-enrichment.yaml` | H2_DB | ACTIVE | H2 custom parameters |
| 10 | `MultiParameterLookupTest.java` | ✅ `multi-parameter-lookup.yaml` | H2_DB | ACTIVE | Multi-parameter lookup |
| 11 | `NestedFieldLookupDemoTest.java` | ✅ `nested-field-lookup.yaml` | INLINE | ACTIVE | Nested field navigation |
| 12 | `PostgreSQLMultiParamLookupTest.java` | ✅ `postgresql-multi-param-lookup.yaml` | POSTGRESQL_DB | ACTIVE | Phase 1.2 PostgreSQL |
| 13 | `PostgreSQLSimpleLookupTest.java` | ✅ `postgresql-simple-lookup.yaml` | POSTGRESQL_DB | ACTIVE | Phase 1.1 PostgreSQL |
| 14 | `SimpleFieldLookupDemoTest.java` | ✅ `simple-field-lookup.yaml` | INLINE | ACTIVE | Simple field lookup |

---

## 🗂️ **Data Source Pattern Coverage**

| **Data Source Type** | **Count** | **Test Files** | **Coverage Status** |
|---------------------|-----------|----------------|-------------------|
| **INLINE** | 7 | `BarrierOptionNestedTest`, `CompoundKeyLookupTest`, `ConditionalExpressionLookupTest`, `CurrencyMarketMappingTest`, `NestedFieldLookupDemoTest`, `SharedDatasourceDemoTest`, `SimpleFieldLookupDemoTest` | ✅ **EXCELLENT** |
| **H2_DB** | 3 | `ComprehensiveLookupTest`, `H2CustomParametersDemoTest`, `MultiParameterLookupTest` | ✅ **GOOD** |
| **POSTGRESQL_DB** | 2 | `PostgreSQLMultiParamLookupTest`, `PostgreSQLSimpleLookupTest` | ✅ **GOOD** |
| **REST_API** | 2 | `BasicRestApiLookupTest`, `EnhancedRestApiDemoTest` | ✅ **GOOD** |
| **JSON_FILE** | 1 | `FileSystemLookupDemoTest` | ⚠️ **LIMITED** |
| **XML_FILE** | 1 | `FileSystemLookupDemoTest` | ⚠️ **LIMITED** |
| **YAML_FILE** | 1 | `ExternalDataSourceWorkingDemoTest` | ⚠️ **LIMITED** |
| **NO_YAML** | 2 | `DatabaseConnectionTest`, `EnhancedRestApiDemoTest` | 🚨 **VIOLATIONS** |
| **UNKNOWN** | 5 | Orphaned YAML files | ❓ **NEEDS ANALYSIS** |

### **🎯 Pattern Coverage Analysis**
- ✅ **INLINE patterns**: Excellent coverage (7 tests)
- ✅ **Database patterns**: Good coverage (H2: 3, PostgreSQL: 2)
- ✅ **REST API patterns**: Good coverage (2 tests)
- ⚠️ **File-based patterns**: Limited coverage (JSON: 1, XML: 1, YAML: 1)
- 🚨 **YAML First violations**: 2 tests bypass YAML entirely
- ❓ **Missing patterns**: 5 orphaned files need analysis

---

## 📊 **Summary Statistics Grid**

| **Category** | **Count** | **Percentage** |
|--------------|-----------|----------------|
| **Total YAML Files** | 23 | 100% |
| **Total Java Test Files** | 15 | 100% |
| **YAML Files with Tests** | 18 | 78% |
| **Orphaned YAML Files** | 5 | 22% |
| **Java Tests without YAML** | 2 | 14% |

---

## 🚨 **YAML FIRST PRINCIPLE VIOLATIONS**

| **File** | **Violation Type** | **Issue** | **Correct Data Source** | **Fix Required** |
|----------|-------------------|-----------|------------------------|------------------|
| ~~`ConditionalExpressionLookupTest.java`~~ | ~~Business Logic in Java~~ | ~~Complex credit score logic, validation loops, custom test scenarios~~ | ~~INLINE~~ | ✅ **FIXED** - Removed Java business logic, kept conditional expressions in YAML |
| ~~`SharedDatasourceDemoTest.java`~~ | ~~Database Setup in Java~~ | ~~50+ lines of H2 database setup, SQL execution, connection management~~ | ~~INLINE~~ | ✅ **FIXED** - Removed Java database setup, used YAML inline configuration |
| `DatabaseConnectionTest.java` | No YAML Usage | Direct JDBC testing, bypasses APEX entirely | H2_DB or POSTGRESQL_DB | Create YAML configuration, use APEX services |
| `EnhancedRestApiDemoTest.java` | No YAML Usage | Uses JDK HTTP server directly, bypasses APEX | REST_API | Create YAML configuration, use APEX REST lookup |

### **🔧 Immediate Fix Priority**
1. ✅ **COMPLETE**: Fixed `ConditionalExpressionLookupTest` and `SharedDatasourceDemoTest` (YAML First violations resolved)
2. **MEDIUM**: Convert `DatabaseConnectionTest` to use YAML + APEX
3. **MEDIUM**: Convert `EnhancedRestApiDemoTest` to use YAML + APEX

---

## 🔴 **Orphaned YAML Files (No Java Test)**

| # | **Orphaned YAML File** | **Purpose** | **Priority** |
|---|------------------------|-------------|--------------|
| 1 | `advanced-caching-demo.yaml` | Advanced caching strategies | HIGH |
| ~~2~~ | ~~`compound-key-lookup.yaml`~~ | ~~Compound key operations~~ | ✅ **COMPLETE** |
| ~~3~~ | ~~`conditional-expression-lookup.yaml`~~ | ~~Conditional logic evaluation~~ | ✅ **COMPLETE** |
| ~~4~~ | ~~`currency-market-mapping.yaml`~~ | ~~Currency to market mapping~~ | ✅ **COMPLETE** |
| 5 | `customer-profile-enrichment.yaml` | Customer profile enrichment | LOW |
| 6 | `h2-custom-parameters-demo.yaml` | H2 configuration demo | LOW |
| 7 | `mathematical-operations-lookup.yaml` | Mathematical operations | HIGH |
| 8 | `postgresql-simple-database-enrichment.yaml` | PostgreSQL enrichment | MEDIUM |
| 9 | `settlement-instruction-enrichment.yaml` | Settlement processing | HIGH |
| ~~10~~ | ~~`shared-datasource-demo.yaml`~~ | ~~Shared data source demonstration~~ | ✅ **COMPLETE** |

---

## ✅ **Active YAML-Java Pairs**

| # | **YAML File** | **Java Test File** | **Phase** |
|---|---------------|-------------------|-----------|
| 1 | `barrier-option-nested-enrichment.yaml` | `BarrierOptionNestedTest.java` | Complete |
| 2 | `basic-rest-api-lookup.yaml` | `BasicRestApiLookupTest.java` | Phase 2.1 |
| 3 | `comprehensive-lookup-enrichment.yaml` | `ComprehensiveLookupTest.java` | Complete |
| 4 | `external-data-source-working-enrichment.yaml` | `ExternalDataSourceWorkingDemoTest.java` | Complete |
| 5 | `h2-custom-parameters-enrichment.yaml` | `H2CustomParametersDemoTest.java` | Complete |
| 6 | `json-file-lookup.yaml` | `FileSystemLookupDemoTest.java` | Complete |
| 7 | `multi-parameter-lookup.yaml` | `MultiParameterLookupTest.java` | Complete |
| 8 | `nested-field-lookup.yaml` | `NestedFieldLookupDemoTest.java` | Complete |
| 9 | `postgresql-multi-param-lookup.yaml` | `PostgreSQLMultiParamLookupTest.java` | Phase 1.2 |
| 10 | `postgresql-simple-lookup.yaml` | `PostgreSQLSimpleLookupTest.java` | Phase 1.1 |
| 11 | `simple-field-lookup.yaml` | `SimpleFieldLookupDemoTest.java` | Complete |
| 12 | `xml-file-lookup.yaml` | `FileSystemLookupDemoTest.java` | Complete |

---

## 🟡 **Special Cases**

| **File** | **Type** | **Status** | **Explanation** |
|----------|----------|------------|-----------------|
| `DatabaseConnectionTest.java` | Java Test | No YAML Needed | Direct JDBC connection testing |
| `EnhancedRestApiDemoTest.java` | Java Test | No YAML Needed | Uses JDK HTTP server directly |
| `CompoundKeyLookupDemoTest.java` | Java Test | References Orphaned YAML | Test exists but YAML is orphaned |
| `h2-custom-parameters-demo.yaml` | YAML | Configuration Only | May not need dedicated test |

---

## 📋 **Action Items Grid**

| **Priority** | **Action** | **File** | **Effort** |
|--------------|------------|----------|------------|
| HIGH | Create test | `mathematical-operations-lookup.yaml` | 3 days |
| HIGH | Create test | `advanced-caching-demo.yaml` | 3 days |
| HIGH | Create test | `conditional-expression-lookup.yaml` | 2 days |
| HIGH | Create test | `settlement-instruction-enrichment.yaml` | 3 days |
| MEDIUM | Create test | `compound-key-lookup.yaml` | 1 day |
| ✅ COMPLETE | ~~Create test~~ | ~~`currency-market-mapping.yaml`~~ | **DONE** |
| MEDIUM | Create test | `postgresql-simple-database-enrichment.yaml` | 2 days |
| LOW | Create test | `customer-profile-enrichment.yaml` | 1 day |
| LOW | Consider test | `h2-custom-parameters-demo.yaml` | 1 day |
