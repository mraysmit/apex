# APEX YAML Dependency Structure - How It Actually Works

## Understanding APEX Dependencies

APEX YAML files establish dependencies through **file-based references** using specific keywords. The `YamlDependencyAnalyzer` scans these keywords and recursively follows file paths to build a complete dependency graph.

## File Reference Keywords

APEX recognizes these keywords for file references:

| Keyword | Purpose | Example |
|---------|---------|---------|
| `rule-configurations` | References rule configuration files | `rule-configurations: [01-base-validation-rules.yaml]` |
| `enrichment-refs` | References enrichment configuration files | `enrichment-refs: [03-enrichment-rules.yaml]` |
| `config-files` | References general configuration files | `config-files: [shared-config.yaml]` |
| `rule-chains` | References rule chain files | `rule-chains: [chain-1.yaml]` |
| `include` / `import` | Includes/imports other YAML files | `include: other-file.yaml` |

## Sample Dependency Chain

### Scenario Registry (Root)
```yaml
metadata:
  id: scenario-registry
  type: scenario-registry

rule-configurations:
  - 06-trade-processing-scenario.yaml
```

**Dependency:** scenario-registry → 06-trade-processing-scenario.yaml

---

### Trade Processing Scenario
```yaml
metadata:
  id: trade-processing-scenario
  type: scenario

rule-configurations:
  - 02-validation-groups.yaml

enrichment-refs:
  - 03-enrichment-rules.yaml
  - 05-database-lookups.yaml
```

**Dependencies:**
- 06-trade-processing-scenario → 02-validation-groups.yaml
- 06-trade-processing-scenario → 03-enrichment-rules.yaml
- 06-trade-processing-scenario → 05-database-lookups.yaml

---

### Validation Groups
```yaml
metadata:
  id: validation-groups
  type: rule-config

rule-configurations:
  - 01-base-validation-rules.yaml

rule-groups:
  - id: mandatory-trade-validation
    rule-ids:
      - trade-amount-validation
      - counterparty-validation
```

**Dependency:** 02-validation-groups → 01-base-validation-rules.yaml

**Note:** The `rule-ids` reference rules **within** the same file or imported files, not separate files.

---

### Base Validation Rules (Leaf Node)
```yaml
metadata:
  id: base-validation-rules
  type: rule-config

rules:
  - id: trade-amount-validation
    condition: "#tradeAmount > 0"
  - id: counterparty-validation
    condition: "#counterparty != null"
```

**Dependencies:** None (leaf node)

---

## Complete Dependency Tree

```
scenario-registry.yaml
└── rule-configurations: [06-trade-processing-scenario.yaml]
    └── 06-trade-processing-scenario.yaml
        ├── rule-configurations: [02-validation-groups.yaml]
        │   └── 02-validation-groups.yaml
        │       └── rule-configurations: [01-base-validation-rules.yaml]
        │           └── 01-base-validation-rules.yaml (LEAF)
        │
        └── enrichment-refs: [03-enrichment-rules.yaml, 05-database-lookups.yaml]
            ├── 03-enrichment-rules.yaml (LEAF - has inline data)
            └── 05-database-lookups.yaml (LEAF - has database connections)
```

## Key Insights

### 1. File Paths vs Configuration IDs

- **File Paths** (in keywords): Used for dependency resolution
  ```yaml
  rule-configurations:
    - 02-validation-groups.yaml  # This is a FILE PATH
  ```

- **Configuration IDs** (in rules/enrichments): Used within a file
  ```yaml
  rule-groups:
    - id: my-group
      rule-ids:
        - trade-amount-validation  # This is a RULE ID within the file
  ```

### 2. How the Analyzer Works

1. Start with root file (e.g., scenario-registry.yaml)
2. Scan for keywords: `rule-configurations`, `enrichment-refs`, `config-files`, etc.
3. Extract file paths from these keywords
4. For each file path, recursively repeat steps 2-3
5. Build complete graph of all dependencies

### 3. Leaf Nodes

Files with no outgoing file references are leaf nodes:
- `01-base-validation-rules.yaml` - Only contains rules, no file references
- `03-enrichment-rules.yaml` - Only contains enrichments and inline data
- `05-database-lookups.yaml` - Only contains enrichments and database connections

### 4. Circular Dependencies

The analyzer detects circular references:
```yaml
# File A
rule-configurations:
  - file-b.yaml

# File B
rule-configurations:
  - file-a.yaml  # Creates a cycle!
```

## Testing the Dependency Tree

### Load Sample Files
1. Open Dependency Tree Viewer UI
2. Click "Load Folder"
3. Navigate to `apex-yaml-manager/src/test/resources/apex-yaml-samples`
4. Click "Scan" to find all YAML files
5. Select all files and click "Load Selected"

### Expected Results
- **Total Files:** 13 YAML files
- **Root:** scenario-registry.yaml
- **Max Depth:** 3 levels (registry → scenario → groups → rules)
- **Circular Dependencies:** None detected

### Verify Dependencies
Click on each node to see:
- **Direct Dependencies:** Files this file references
- **All Dependencies:** Transitive closure (all files reachable)
- **Dependents:** Files that reference this file
- **Health Score:** Dependency health metrics

## Real-World Example

In production APEX systems:

```
scenario-registry.yaml (root)
├── otc-option-scenario.yaml
│   ├── otc-validation-rules.yaml
│   │   ├── base-validation-rules.yaml
│   │   └── otc-specific-rules.yaml
│   ├── otc-enrichment.yaml
│   │   └── market-data-enrichment.yaml
│   └── otc-compliance.yaml
│       └── regulatory-rules.yaml
│
└── equity-scenario.yaml
    ├── equity-validation-rules.yaml
    ├── equity-enrichment.yaml
    └── equity-compliance.yaml
```

Each scenario can reference different rule sets, enrichments, and compliance configurations, creating a flexible, reusable configuration system.

