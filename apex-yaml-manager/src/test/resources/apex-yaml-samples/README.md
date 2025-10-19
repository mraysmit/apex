# APEX YAML Sample Files - Dependency Tree Viewer Test Suite

This directory contains comprehensive APEX YAML configuration files demonstrating various configuration types and dependency patterns for testing the Dependency Tree Viewer.

## File Structure and Dependencies

### 1. Base Validation Rules (`01-base-validation-rules.yaml`)
**Type:** `rule-config`  
**Purpose:** Foundational validation rules for trade processing  
**Key Features:**
- 5 individual validation rules
- Trade amount, counterparty, settlement date, currency, and trade type validation
- Reusable rules referenced by validation groups

**Use Case:** Demonstrates basic rule definitions that can be reused across multiple rule groups.

---

### 2. Validation Groups (`02-validation-groups.yaml`)
**Type:** `rule-config`  
**Purpose:** Rule groups that combine base validation rules  
**Dependencies:** References rules from `01-base-validation-rules.yaml`  
**Key Features:**
- 3 rule groups with different operators (AND, OR)
- Demonstrates rule reuse across multiple groups
- Shows stop-on-first-failure patterns
- Priority-based execution

**Use Case:** Shows how individual rules are grouped for different validation scenarios (mandatory, settlement, comprehensive).

---

### 3. Enrichment Rules (`03-enrichment-rules.yaml`)
**Type:** `enrichment`  
**Purpose:** Data enrichment with inline datasets and lookups  
**Key Features:**
- Inline dataset for currency exchange rates
- Lookup enrichment using inline data
- Calculation enrichments for trade classification
- Field mappings for data transformation

**Use Case:** Demonstrates inline datasets, lookup enrichments, and calculated fields.

---

### 4. Enrichment Groups (`04-enrichment-groups.yaml`)
**Type:** `enrichment`  
**Purpose:** Grouped enrichment configurations  
**Dependencies:** References enrichments from `03-enrichment-rules.yaml`  
**Key Features:**
- 3 enrichment groups with different scopes
- Demonstrates enrichment reuse patterns
- Shows composition of enrichments

**Use Case:** Shows how enrichments are grouped for different processing stages.

---

### 5. Database Lookups (`05-database-lookups.yaml`)
**Type:** `enrichment`  
**Purpose:** Database connection configurations and lookup mappings  
**Key Features:**
- PostgreSQL database connections
- Connection pooling and timeout configuration
- Environment variable placeholders for credentials
- SQL query-based lookups with parameter mapping
- Multiple data sources for different domains

**Use Case:** Demonstrates database connections, parameterized queries, and credential management.

---

### 6. Trade Processing Scenario (`06-trade-processing-scenario.yaml`)
**Type:** `scenario`  
**Purpose:** End-to-end trade processing workflow  
**Dependencies:** References all previous configurations  
**Key Features:**
- Classification rules for trade routing
- Multi-stage processing pipeline
- Failure policies (terminate, continue)
- Execution order and dependencies
- Error handling strategies

**Use Case:** Shows complete end-to-end scenario with multiple processing stages.

---

### 7. Scenario Registry (`scenario-registry.yaml`)
**Type:** `scenario-registry`  
**Purpose:** Central registry of all scenarios and configurations  
**Dependencies:** References all scenario and configuration files  
**Key Features:**
- Scenario metadata and ownership
- Business domain classification
- Configuration file references
- Multiple scenario definitions

**Use Case:** Demonstrates scenario discovery and registration.

---

## Dependency Tree Visualization

The dependency tree shows how APEX YAML files reference each other through file-based dependencies:

```
scenario-registry.yaml (root)
└── rule-configurations: [06-trade-processing-scenario.yaml]
    └── 06-trade-processing-scenario.yaml
        ├── rule-configurations: [02-validation-groups.yaml]
        │   └── 02-validation-groups.yaml
        │       └── rule-configurations: [01-base-validation-rules.yaml]
        │           └── 01-base-validation-rules.yaml (5 rules)
        ├── enrichment-refs: [03-enrichment-rules.yaml, 05-database-lookups.yaml]
        │   ├── 03-enrichment-rules.yaml
        │   │   └── data-sources: [currency-rates (inline dataset)]
        │   └── 05-database-lookups.yaml
        │       └── data-sources: [trade-database, counterparty-database (PostgreSQL)]
        └── (processing-stages reference the same files)

Additional scenarios:
├── 02-validation-groups.yaml
│   └── rule-configurations: [01-base-validation-rules.yaml]
├── 04-enrichment-groups.yaml
│   └── enrichment-refs: [03-enrichment-rules.yaml]
└── 05-database-lookups.yaml
```

**Key Points:**
- Dependencies are established through `rule-configurations`, `enrichment-refs`, and `config-files` keys
- These keys contain **file paths** (not configuration IDs)
- The YamlDependencyAnalyzer recursively follows these file references
- Inline datasets and data-sources are leaf nodes (no further dependencies)

## Configuration Types Demonstrated

| Type | File | Features |
|------|------|----------|
| **rule-config** | 01, 02 | Individual rules, rule groups, reuse patterns |
| **enrichment** | 03, 04, 05 | Inline datasets, lookups, calculations, database connections |
| **scenario** | 06 | Multi-stage processing, classification, error handling |
| **scenario-registry** | scenario-registry | Central registry, metadata, ownership |

## Key APEX Features Showcased

- ✅ **Rule Reuse:** Rules referenced by multiple groups
- ✅ **Enrichment Reuse:** Enrichments grouped and reused
- ✅ **Inline Datasets:** Currency rates lookup table
- ✅ **Database Lookups:** PostgreSQL connections with parameterized queries
- ✅ **Nested Dependencies:** Multi-level configuration references
- ✅ **Environment Variables:** Placeholder resolution for credentials
- ✅ **Error Handling:** Failure policies and severity thresholds
- ✅ **Classification Rules:** SpEL-based trade routing
- ✅ **Processing Stages:** Ordered execution with dependencies

## Testing the Dependency Tree Viewer

### Load Sample YAML Files
1. Open the Dependency Tree Viewer UI
2. Click "Load Folder" button
3. Navigate to `apex-yaml-manager/src/test/resources/apex-yaml-samples`
4. Click "Scan" to find all YAML files
5. Select all files and click "Load Selected"

### Verify Tree Display
- Tree should show 13 YAML files
- Dependencies should be properly visualized
- Clicking nodes should show detailed configuration information
- Expand/collapse functionality should work

### Detail View Information

When clicking on nodes in the tree, the detail panel should display:

**For Rules:**
- Rule ID, name, and description
- Condition expression
- Severity and priority
- Message template

**For Enrichments:**
- Enrichment ID and type
- Condition and field mappings
- Data source references
- Calculation expressions

**For Lookups:**
- Database connection details
- Query parameters
- Field mappings
- Connection pooling configuration

**For Scenarios:**
- Processing stages and execution order
- Classification rules
- Failure policies
- Error handling strategies

**For Registries:**
- Scenario metadata
- Business domain and ownership
- Configuration file references
- Version information

## File Naming Convention

Files are numbered (01-06) to indicate dependency order and are connected through file-based references:

| File | Type | Dependencies | References |
|------|------|--------------|-----------|
| `01-base-validation-rules.yaml` | rule-config | None | (leaf node) |
| `02-validation-groups.yaml` | rule-config | 01 | `rule-configurations: [01-base-validation-rules.yaml]` |
| `03-enrichment-rules.yaml` | enrichment | None | (leaf node with inline data) |
| `04-enrichment-groups.yaml` | enrichment | 03 | `enrichment-refs: [03-enrichment-rules.yaml]` |
| `05-database-lookups.yaml` | enrichment | None | (leaf node with database connections) |
| `06-trade-processing-scenario.yaml` | scenario | 02, 03, 05 | `rule-configurations: [02-...]` + `enrichment-refs: [03-..., 05-...]` |
| `scenario-registry.yaml` | scenario-registry | 06 | `rule-configurations: [06-trade-processing-scenario.yaml]` |

**How Dependencies Work:**
- Each file declares dependencies using APEX keywords: `rule-configurations`, `enrichment-refs`, `config-files`
- These keywords contain **file paths** (not IDs)
- The YamlDependencyAnalyzer scans these keys and recursively follows file references
- This creates a complete dependency graph from root to leaf nodes

