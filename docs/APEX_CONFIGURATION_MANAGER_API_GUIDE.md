# APEX Configuration Manager - Complete REST API Guide

**Version**: 2.0
**Last Updated**: 2025-11-09

## Overview

This comprehensive guide provides **copy-paste examples** for every REST API endpoint in the APEX Configuration Manager. All examples use the **Graph-100 dataset** located in `apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/` which contains 100+ interconnected YAML files designed to test complex dependency scenarios.

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Service Options

You have two options for starting APEX services:

#### Option 1: APEX REST API Only (Core Rules Engine)
**Port**: 8080 | **Purpose**: Main APEX Rules Engine API

```bash
# Start from apex-rest-api directory
cd apex-rest-api
mvn spring-boot:run

# OR start from root directory
mvn spring-boot:run -pl apex-rest-api
```

**Verify APEX REST API:**
```bash
curl http://localhost:8080/actuator/health
```

**Access Points:**
- REST API: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

#### Option 2: APEX Configuration Manager (YAML Configuration Management)
**Port**: 8082 | **Purpose**: YAML Configuration Management & Dependency Analysis

```bash
# Start from apex-yaml-manager directory
cd apex-yaml-manager
mvn spring-boot:run

# OR start from root directory
mvn spring-boot:run -pl apex-yaml-manager
```

**Verify Configuration Manager:**
```bash
curl http://localhost:8082/yaml-manager/api/health
```

**Access Points:**
- REST API: `http://localhost:8082/yaml-manager/api`
- Swagger: `http://localhost:8082/yaml-manager/swagger-ui.html`
- Tree Viewer: `http://localhost:8082/yaml-manager/d3-tree-viewer.html`

### Service Comparison

| Service | Port | Purpose | Context Path | Documentation Focus |
|---------|------|---------|--------------|-------------------|
| **apex-rest-api** | 8080 | Core APEX Rules Engine API | `/` | Rules processing, evaluation, transformations |
| **apex-yaml-manager** | 8082 | APEX Configuration Manager | `/yaml-manager` | **This document** - Dependency analysis, health checks, catalog management |

> **Note**: This document focuses on the **APEX Configuration Manager** endpoints. For core rules engine API documentation, see the apex-rest-api Swagger documentation.

---

## 🏥 Health Controller

**Base Path**: `/api/health`

### GET /api/health
**Purpose**: Basic health check to verify service is running

**Why Use This?**
- **Monitoring & Alerting**: Integrate with monitoring tools (Prometheus, Nagios, Datadog) to track service availability
- **Load Balancer Health Checks**: Configure load balancers to verify service instances are responsive
- **CI/CD Pipeline Validation**: Verify service started successfully after deployment
- **Quick Status Check**: Fast, lightweight endpoint for manual verification during troubleshooting
- **Kubernetes Liveness Probe**: Use as a liveness probe to detect when the service needs to be restarted

**When to Use**: Use this endpoint when you need a simple UP/DOWN status without detailed information. Perfect for automated health checks that run every few seconds.

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/health"
```

**Response**:
```json
{
  "status": "UP",
  "service": "apex-yaml-manager",
  "version": "1.0.0",
  "timestamp": 1698765432000
}
```

**Response Fields Explained**:
- **`status`**: Service health status. `UP` = service is running and accepting requests, `DOWN` = service is not responding
- **`service`**: Service identifier. Always returns `"apex-yaml-manager"` to confirm you're connected to the correct service
- **`version`**: Semantic version of the deployed service (e.g., `"1.0.0"`). Use this to verify compatibility with your client
- **`timestamp`**: Unix epoch timestamp (milliseconds) when the health check was performed. Use this to detect stale responses or caching issues

---

### GET /api/health/status
**Purpose**: Detailed system status with capabilities

**Why Use This?**
- **Feature Discovery**: Identify which capabilities are available in the deployed version
- **Version Compatibility**: Verify the service version supports required features before making API calls
- **Operational Dashboard**: Display service capabilities and status in admin/operations dashboards
- **Integration Planning**: Determine which features to integrate with based on available capabilities
- **Troubleshooting**: Verify all expected capabilities are enabled when debugging integration issues
- **Documentation Validation**: Confirm the running service matches the documented feature set

**When to Use**: Use this endpoint during initial integration, version upgrades, or when you need to verify specific capabilities are available. This is more detailed than the basic health check and provides actionable information about what the service can do.

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/health/status"
```

**Response**:
```json
{
  "status": "OPERATIONAL",
  "service": "apex-yaml-manager",
  "version": "1.0.0",
  "capabilities": [
    "dependency-analysis",
    "catalog-discovery",
    "health-checks",
    "refactoring-tools",
    "visualization"
  ],
  "timestamp": 1698765432000
}
```

**Response Fields Explained**:
- **`status`**: Operational status. `OPERATIONAL` = all features working, `DEGRADED` = some features unavailable, `DOWN` = service not responding
- **`service`**: Service identifier confirming this is the APEX YAML Manager service
- **`version`**: Semantic version number. Check this before using version-specific features
- **`capabilities`**: Array of feature flags indicating which capabilities are enabled in this deployment:
  - `"dependency-analysis"` - Dependency analysis and metrics calculation available
  - `"catalog-discovery"` - Catalog scanning and discovery features available
  - `"health-checks"` - Health monitoring endpoints available
  - `"refactoring-tools"` - Refactoring and code quality tools available
  - `"visualization"` - D3.js visualization generation available
- **`timestamp`**: Unix epoch timestamp (milliseconds) when the status was checked

---

## Dependency Analysis Controller

**Base Path**: `/api/dependencies`

### POST /api/dependencies/analyze
**Purpose**: Analyze YAML file dependencies and calculate comprehensive metrics

**Why Use This?**
- **Impact Analysis**: Before modifying a YAML file, understand how many other files depend on it and will be affected
- **Complexity Assessment**: Evaluate the complexity of your configuration structure to identify refactoring opportunities
- **Circular Dependency Detection**: Identify circular dependencies that could cause infinite loops or processing issues
- **Orphaned File Discovery**: Find YAML files that are defined but never referenced, indicating dead code
- **Critical File Identification**: Discover which files are most heavily used and require extra care during changes
- **Refactoring Planning**: Use metrics to prioritize which configurations need simplification or restructuring
- **Documentation**: Generate dependency reports for technical documentation and architecture reviews
- **Risk Assessment**: Understand the blast radius of changes before making modifications to production configurations

**When to Use**:
- Before making changes to any YAML file to understand downstream impact
- During code reviews to assess the complexity of new configurations
- When planning refactoring efforts to identify high-risk areas
- As part of CI/CD pipelines to detect configuration quality issues
- When onboarding new team members to help them understand configuration structure

**Using Graph-100 Root File**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/dependencies/analyze" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml"
```

**Using a Simple Scenario**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/dependencies/analyze" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/10-scenario-a.yaml"
```

**Response**:
```json
{
  "status": "success",
  "rootFile": "00-scenario-registry.yaml",
  "totalFiles": 42,
  "maxDepth": 9,
  "metrics": {
    "totalFiles": 42,
    "maxDepth": 9,
    "averageDepth": 3.2,
    "totalDependencies": 156,
    "totalDependents": 89,
    "complexityScore": 75,
    "complexityLevel": "HIGH",
    "circularDependencies": ["40-cycle-1.yaml", "41-cycle-2.yaml"],
    "orphanedFiles": ["98-invalid.yaml"],
    "criticalFiles": ["25-groups-common.yaml", "35-rules-common.yaml"],
    "warningCount": 3,
    "errorCount": 1
  },
  "timestamp": 1698765432000
}
```

**Response Fields Explained**:
- **`status`**: Analysis result status. `"success"` = analysis completed, `"error"` = analysis failed
- **`rootFile`**: Name of the root YAML file that was analyzed (entry point)
- **`totalFiles`**: Total number of YAML files discovered in the dependency tree (including root)
- **`maxDepth`**: Maximum depth of the dependency tree (how many levels deep the dependencies go)
- **`metrics`**: Comprehensive dependency metrics object:
  - **`totalFiles`**: Same as top-level `totalFiles` (redundant for consistency)
  - **`maxDepth`**: Same as top-level `maxDepth` (redundant for consistency)
  - **`averageDepth`**: Average depth of all files in the tree. Lower = flatter structure, higher = deeper nesting
  - **`totalDependencies`**: Total number of dependency relationships (file A depends on file B)
  - **`totalDependents`**: Total number of dependent relationships (file B is depended on by file A)
  - **`complexityScore`**: Calculated complexity score (0-100). Higher = more complex. Based on depth, dependencies, and structure
  - **`complexityLevel`**: Human-readable complexity level: `"LOW"` (0-33), `"MEDIUM"` (34-66), `"HIGH"` (67-100)
  - **`circularDependencies`**: Array of file names involved in circular dependency cycles. Empty array = no cycles (good!)
  - **`orphanedFiles`**: Array of files that exist but are not referenced by any other file. May indicate unused files
  - **`criticalFiles`**: Array of files that are heavily depended upon. Changes to these files have high impact
  - **`warningCount`**: Number of warnings detected (e.g., orphaned files, high complexity)
  - **`errorCount`**: Number of errors detected (e.g., circular dependencies, missing files)
- **`timestamp`**: Unix epoch timestamp (milliseconds) when the analysis was performed

---

### GET /api/dependencies/tree
**Purpose**: Generate D3.js-compatible hierarchical dependency tree

**Why Use This?**
- **Visual Dependency Mapping**: Create interactive visualizations showing how YAML files relate to each other
- **Architecture Documentation**: Generate visual diagrams for technical documentation and presentations
- **Onboarding**: Help new team members understand the configuration structure through visual exploration
- **Debugging**: Trace dependency chains visually to understand why a particular file is being loaded
- **Refactoring Planning**: Visualize the current structure before planning reorganization efforts
- **D3.js Integration**: Direct integration with D3.js tree visualization libraries for custom dashboards
- **Hierarchy Analysis**: Understand the depth and breadth of your configuration hierarchy
- **Health Visualization**: Color-code nodes by health score to quickly identify problematic areas

**When to Use**:
- When building custom visualization dashboards or tools
- During architecture reviews to present configuration structure
- When debugging complex dependency chains that are hard to understand from text
- To generate documentation diagrams automatically
- When you need programmatic access to the dependency tree structure

**Generate Tree for Graph-100 Dataset**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml"
```

**Generate Tree for Single Scenario**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/10-scenario-a.yaml"
```

**Response**:
```json
{
  "status": "success",
  "rootFile": "00-scenario-registry.yaml",
  "totalFiles": 42,
  "maxDepth": 9,
  "tree": {
    "name": "00-scenario-registry.yaml",
    "id": "00-scenario-registry.yaml",
    "path": "C:/Users/.../00-scenario-registry.yaml",
    "type": "scenario-registry",
    "depth": 0,
    "height": 9,
    "childCount": 4,
    "healthScore": 85,
    "circular": false,
    "dependencies": ["10-scenario-a.yaml", "11-scenario-b.yaml"],
    "contentSummary": {
      "filePath": "00-scenario-registry.yaml",
      "fileType": "scenario-registry",
      "ruleCount": 0,
      "ruleGroupCount": 0,
      "enrichmentCount": 0,
      "configFileCount": 4,
      "referenceCount": 8
    },
    "children": [
      {
        "name": "10-scenario-a.yaml",
        "type": "scenario",
        "depth": 1,
        "healthScore": 78,
        "children": [...]
      }
    ]
  }
}
```

**Response Fields Explained**:
- **`status`**: Tree generation status. `"success"` = tree generated successfully
- **`rootFile`**: Name of the root file used as the tree starting point
- **`totalFiles`**: Total number of files in the entire dependency tree
- **`maxDepth`**: Maximum depth of the tree (longest path from root to leaf)
- **`tree`**: Hierarchical tree structure (D3.js compatible) with nested nodes:
  - **`name`**: File name (e.g., `"00-scenario-registry.yaml"`)
  - **`id`**: Unique identifier for the node (typically same as name)
  - **`path`**: Full file system path to the YAML file
  - **`type`**: YAML file type (e.g., `"scenario-registry"`, `"scenario"`, `"rule-config"`, `"enrichment"`)
  - **`depth`**: Depth level in the tree (0 = root, 1 = direct child, etc.)
  - **`height`**: Height of this subtree (maximum depth from this node to any leaf)
  - **`childCount`**: Number of direct children (files this file directly depends on)
  - **`healthScore`**: Health score (0-100) for this file. Higher = better quality
  - **`circular`**: Boolean indicating if this file is part of a circular dependency cycle
  - **`dependencies`**: Array of file names this file directly depends on
  - **`contentSummary`**: Summary of what's inside this YAML file:
    - **`filePath`**: File name
    - **`fileType`**: Type of YAML configuration
    - **`ruleCount`**: Number of rules defined in this file
    - **`ruleGroupCount`**: Number of rule groups defined
    - **`enrichmentCount`**: Number of enrichments defined
    - **`configFileCount`**: Number of configuration files referenced
    - **`referenceCount`**: Total number of references to other files
  - **`children`**: Array of child nodes (recursive structure). Each child has the same structure as the parent

**How to Use the Tree**:
- Use D3.js tree layout to visualize the hierarchy
- Color nodes by `healthScore` to highlight problem areas
- Highlight nodes where `circular: true` to show dependency cycles
- Use `depth` to control tree layout and indentation
- Filter by `type` to show only specific configuration types

---

### GET /api/dependencies/content
**Purpose**: Retrieve file content and metadata for a specific YAML file

**Why Use This?**
- **File Inspection**: View the complete content of a YAML file without accessing the file system directly
- **Metadata Extraction**: Get structured metadata (author, dates, type) along with raw content
- **Dependency Context**: See both what a file depends on and what depends on it in a single call
- **Content Analysis**: Retrieve content for parsing, validation, or transformation in external tools
- **Audit Trail**: Access file content with metadata for compliance and audit purposes
- **Remote Access**: Retrieve file content from a centralized service without direct file system access
- **Health Assessment**: Get health score and issues for a specific file
- **Integration**: Fetch YAML content for processing in external systems or custom tools

**When to Use**:
- When you need to inspect a specific file's content and metadata together
- Building custom editors or viewers that need both content and context
- Implementing audit or compliance tools that need to track file content
- When debugging issues with a specific YAML file
- For remote access to YAML content without file system permissions

**Get Content for Scenario Registry**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/dependencies/content?filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml"
```

**Get Content for Rules File**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/dependencies/content?filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/30-rules-a.yaml"
```

**Response**:
```json
{
  "status": "success",
  "data": {
    "name": "30-rules-a.yaml",
    "path": "C:/Users/.../30-rules-a.yaml",
    "type": "rule-config",
    "healthScore": 92,
    "dependencies": [],
    "dependents": ["20-groups-a.yaml"],
    "contentSummary": {
      "filePath": "30-rules-a.yaml",
      "fileType": "rules",
      "ruleCount": 2,
      "ruleGroupCount": 0,
      "enrichmentCount": 0,
      "configFileCount": 0,
      "referenceCount": 0,
      "rawContent": "metadata:\n  id: graph100-rules-a\n  name: Graph100 Rules A\n..."
    },
    "author": "apex.demo@company.com",
    "created": "2025-10-19",
    "lastModified": "2025-10-19"
  }
}
```

**Response Fields Explained**:
- **`status`**: Content retrieval status. `"success"` = file found and content retrieved
- **`data`**: File content and metadata object:
  - **`name`**: File name (e.g., `"30-rules-a.yaml"`)
  - **`path`**: Full file system path to the YAML file
  - **`type`**: YAML file type (e.g., `"rule-config"`, `"enrichment"`, `"scenario"`)
  - **`healthScore`**: Health score (0-100) for this file. Higher = better quality
  - **`dependencies`**: Array of file names this file directly depends on (files it references)
  - **`dependents`**: Array of file names that depend on this file (files that reference it)
  - **`contentSummary`**: Detailed content analysis:
    - **`filePath`**: File name
    - **`fileType`**: Type of YAML configuration
    - **`ruleCount`**: Number of rules defined in this file
    - **`ruleGroupCount`**: Number of rule groups defined
    - **`enrichmentCount`**: Number of enrichments defined
    - **`configFileCount`**: Number of configuration files referenced
    - **`referenceCount`**: Total number of references to other files
    - **`rawContent`**: Complete YAML file content as a string. Use this to display or parse the file
  - **`author`**: Email or identifier of the person who created this file (from metadata)
  - **`created`**: ISO 8601 date when the file was created (from metadata)
  - **`lastModified`**: ISO 8601 date when the file was last modified (from metadata)

---

---

## 📚 Catalog Controller

**Base Path**: `/api/catalog`

### POST /api/catalog/scan
**Purpose**: Scan directory and build comprehensive catalog index

**Why Use This?**
- **Catalog Initialization**: Build a searchable index of all YAML configurations in a directory tree
- **Bulk Analysis**: Process hundreds of YAML files in a single operation for comprehensive analysis
- **Configuration Discovery**: Automatically discover all YAML files without manual registration
- **Validation at Scale**: Identify syntax errors and validation issues across entire configuration repositories
- **Performance Optimization**: Index files once, then use fast catalog queries instead of repeated file system scans
- **CI/CD Integration**: Scan configuration directories as part of build pipelines to validate all files
- **Environment Setup**: Initialize the catalog when deploying to new environments
- **Incremental Updates**: Re-scan directories after configuration changes to update the catalog

**When to Use**:
- On service startup to initialize the catalog
- After deploying new or updated YAML configurations
- As part of CI/CD pipelines to validate configuration repositories
- When switching between different configuration directories
- Before running catalog queries to ensure the index is up-to-date
- When you need to process all configurations in a directory tree at once

**Scan Graph-100 Directory**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/catalog/scan" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "directory=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
```

**Response**:
```json
{
  "status": "success",
  "message": "Directory scan completed successfully",
  "scannedFiles": 100,
  "validFiles": 99,
  "invalidFiles": 1,
  "processingTimeMs": 2847,
  "catalogSize": 99,
  "errors": [
    {
      "file": "98-invalid.yaml",
      "error": "Invalid YAML syntax: unterminated string"
    }
  ]
}
```

**Response Fields Explained**:
- **`status`**: Scan operation status. `"success"` = scan completed (even if some files had errors), `"error"` = scan failed completely
- **`message`**: Human-readable description of the scan result
- **`scannedFiles`**: Total number of YAML files found and scanned in the directory tree
- **`validFiles`**: Number of files that were successfully parsed and added to the catalog
- **`invalidFiles`**: Number of files that had parsing or validation errors
- **`processingTimeMs`**: Time taken to scan and process all files (in milliseconds). Use this to monitor performance
- **`catalogSize`**: Total number of configurations now in the catalog (should equal `validFiles` after a fresh scan)
- **`errors`**: Array of error objects for files that failed validation:
  - **`file`**: Name of the file that had an error
  - **`error`**: Description of what went wrong (e.g., syntax error, missing required fields)

**What This Means**:
- If `invalidFiles > 0`, check the `errors` array to see which files need fixing
- `processingTimeMs` helps you understand scan performance for large directories
- `catalogSize` tells you how many configurations are now available for querying
- A successful scan with errors means the catalog was updated with valid files, but some files were skipped

---

### GET /api/catalog/configurations
**Purpose**: Get all configurations with optional field filtering

**Why Use This?**
- **Configuration Inventory**: Get a complete list of all YAML configurations in the catalog
- **Dashboard Display**: Populate admin dashboards with configuration summaries
- **Bulk Operations**: Retrieve all configurations for batch processing or analysis
- **Field Filtering**: Request only specific fields (metadata, dependencies, health) to reduce payload size
- **Configuration Management**: Build configuration management UIs that display all available configs
- **Reporting**: Generate reports showing all configurations with their key attributes
- **Search Foundation**: Get the full dataset for client-side filtering and searching
- **Monitoring**: Track the total number and health of configurations over time

**When to Use**:
- Building configuration management dashboards or UIs
- Generating inventory reports for documentation or compliance
- When you need to process or analyze all configurations together
- For client-side filtering when you want to avoid multiple API calls
- When implementing custom search or filtering logic in your application
- To get an overview of all configurations before drilling into specific ones

**Get All Configurations (Full Details)**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/configurations"
```

**Get Configurations (Metadata Only)**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/configurations?fields=metadata"
```

**Response**:
```json
{
  "total": 99,
  "configurations": [
    {
      "id": "graph100-registry",
      "path": "C:/Users/.../00-scenario-registry.yaml",
      "type": "scenario-registry",
      "name": "Graph-100 Scenario Registry (Seed)",
      "description": "Registry for graph-100 dataset (initial seed)",
      "author": "apex.demo@company.com",
      "version": "0.1.0",
      "businessDomain": "Trade Processing",
      "owner": "platform.team@company.com",
      "created": "2025-10-19T00:00:00",
      "lastModified": "2025-10-19T00:00:00",
      "tags": ["registry", "master", "orchestration"],
      "dependencies": ["10-scenario-a.yaml", "11-scenario-b.yaml"],
      "dependents": [],
      "healthScore": 85,
      "usageCount": 0
    },
    {
      "id": "graph100-scenario-a",
      "path": "C:/Users/.../10-scenario-a.yaml",
      "type": "scenario",
      "name": "Graph100 Scenario A",
      "businessDomain": "Trade Processing",
      "healthScore": 78,
      "tags": ["scenario", "workflow", "orchestration"]
    }
  ]
}
```

---

### GET /api/catalog/metadata/{id}
**Purpose**: Get detailed metadata for specific configuration

**Why Use This?**
- **Configuration Details**: Get complete metadata for a single configuration without retrieving all configs
- **Dependency Inspection**: See both file-level dependencies and ID-level references in one call
- **Health Diagnostics**: View health score and specific issues for troubleshooting
- **Ownership Tracking**: Identify who owns and maintains a specific configuration
- **Usage Analysis**: Understand how frequently a configuration is referenced by others
- **Version Management**: Track version information and modification dates
- **Audit Trail**: Access creation and modification metadata for compliance
- **Quick Lookup**: Fast retrieval of metadata by ID without scanning the full catalog

**When to Use**:
- When displaying details for a specific configuration in a UI
- Before modifying a configuration to understand its current state and dependencies
- When investigating health issues with a particular file
- For audit or compliance reporting on specific configurations
- When you need detailed information about one config without the overhead of retrieving all configs
- To check if a configuration exists and get its current metadata

**Get Metadata for Scenario Registry**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/metadata/graph100-registry"
```

**Get Metadata for Rules Configuration**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/metadata/graph100-rules-a"
```

**Response**:
```json
{
  "id": "graph100-rules-a",
  "path": "C:/Users/.../30-rules-a.yaml",
  "type": "rule-config",
  "name": "Graph100 Rules A",
  "description": "Simple rules A",
  "author": "apex.demo@company.com",
  "version": "0.1.0",
  "businessDomain": "Trade Validation",
  "owner": "validation.team@company.com",
  "tags": ["rules", "validation", "business-logic"],
  "dependencies": [],
  "dependents": ["20-groups-a.yaml"],
  "referencedIds": ["a-rule-1", "a-rule-2"],
  "referencingIds": [],
  "usageCount": 1,
  "healthScore": 92,
  "healthIssues": [],
  "created": "2025-10-19T00:00:00",
  "lastModified": "2025-10-19T00:00:00"
}
```

---

### GET /api/catalog/statistics
**Purpose**: Get comprehensive catalog statistics

**Why Use This?**
- **Catalog Overview**: Get high-level metrics about your entire configuration catalog
- **Health Monitoring**: Track average health score and identify problematic configurations
- **Taxonomy Discovery**: Find all available tags, business domains, owners, and types for filtering
- **Dashboard Metrics**: Display key statistics in monitoring dashboards
- **Trend Analysis**: Track changes in catalog size, health, and complexity over time
- **Quality Assessment**: Identify orphaned files and critical configurations that need attention
- **Governance Reporting**: Generate reports on configuration ownership and organization
- **Filter Options**: Discover available values for tags, domains, and owners to build dynamic filters

**When to Use**:
- Building dashboard summary views that show catalog health at a glance
- Before implementing filters to discover available filter values
- For periodic health checks and quality assessments
- When generating executive reports on configuration management
- To track catalog growth and complexity trends over time
- When you need to understand the overall state of your configuration repository

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/statistics"
```

**Response**:
```json
{
  "totalConfigurations": 99,
  "orphanedCount": 5,
  "criticalCount": 8,
  "averageHealthScore": 82.3,
  "allTags": [
    "registry", "scenario", "rule-group", "rules", "enrichment",
    "config", "dataset", "pipeline", "validation", "orchestration"
  ],
  "allBusinessDomains": [
    "Trade Processing", "Trade Validation", "Data Enrichment",
    "Infrastructure", "Risk Management"
  ],
  "allOwners": [
    "platform.team@company.com", "trading.team@company.com",
    "validation.team@company.com", "data.team@company.com"
  ],
  "allTypes": [
    "scenario-registry", "scenario", "rule-config", "enrichment",
    "external-data-config", "dataset", "pipeline"
  ]
}
```

---

### GET /api/catalog/discovery/search
**Purpose**: Full-text search across all configuration descriptions and metadata

**Why Use This?**
- **Quick Discovery**: Find configurations by searching names, descriptions, and metadata
- **Relevance Ranking**: Results are scored by relevance to help you find the most relevant configs first
- **Flexible Search**: Search across multiple fields without knowing exact IDs or file names
- **User-Friendly**: Enable users to find configurations using natural language terms
- **Documentation Search**: Find configurations related to specific business concepts or features
- **Troubleshooting**: Quickly locate configurations related to a problem area
- **Knowledge Discovery**: Explore configurations related to specific business domains or features
- **Search UI**: Build search interfaces that let users find configurations by keywords

**When to Use**:
- Building search functionality in configuration management UIs
- When you know what you're looking for conceptually but not the exact ID
- For exploratory analysis to find configurations related to a topic
- When troubleshooting issues and need to find all related configurations
- To discover configurations in unfamiliar codebases
- For documentation and knowledge management tools

**Search for "trade" Related Configurations**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/search?query=trade"
```

**Search for "validation" with Metadata Fields Only**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/search?query=validation&fields=metadata"
```

**Response**:
```json
{
  "query": "trade",
  "count": 12,
  "results": [
    {
      "id": "graph100-registry",
      "name": "Graph-100 Scenario Registry (Seed)",
      "businessDomain": "Trade Processing",
      "relevanceScore": 0.95
    },
    {
      "id": "graph100-scenario-a",
      "name": "Graph100 Scenario A",
      "businessDomain": "Trade Processing",
      "relevanceScore": 0.87
    }
  ]
}
```

---

### GET /api/catalog/discovery/tags
**Purpose**: Find configurations by specific tags

**Why Use This?**
- **Tag-Based Organization**: Find all configurations with specific classification tags
- **Multi-Tag Filtering**: Combine multiple tags to narrow down results (AND logic)
- **Category Discovery**: Explore configurations by functional categories (validation, enrichment, etc.)
- **Consistent Classification**: Leverage tagging conventions to find related configurations
- **Bulk Operations**: Identify groups of configurations for batch processing or updates
- **Documentation**: Generate documentation organized by tags
- **Team Organization**: Find configurations owned by specific teams or functional areas
- **Feature Grouping**: Locate all configurations related to a specific feature or capability

**When to Use**:
- Building tag-based navigation in configuration management UIs
- When you need to find all configurations in a specific category
- For bulk operations on tagged configuration groups
- To generate reports organized by tags
- When implementing tag-based access control or workflows
- For discovering configurations that follow tagging conventions

**Find All Configurations with "validation" Tag**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/tags?tags=validation"
```

**Find Configurations with Multiple Tags**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/tags?tags=rules,validation"
```

**Response**:
```json
{
  "tags": ["validation"],
  "count": 15,
  "results": [
    {
      "id": "graph100-groups-a",
      "name": "Graph100 Groups A",
      "type": "rule-config",
      "tags": ["rule-group", "validation", "business-rules"]
    },
    {
      "id": "graph100-rules-a",
      "name": "Graph100 Rules A",
      "type": "rule-config",
      "tags": ["rules", "validation", "business-logic"]
    }
  ]
}
```

---

### GET /api/catalog/discovery/type
**Purpose**: Find configurations by document type

**Why Use This?**
- **Type-Based Filtering**: Find all configurations of a specific type (rules, enrichments, scenarios, etc.)
- **Architecture Analysis**: Understand the distribution of configuration types in your system
- **Type-Specific Operations**: Perform operations on all configurations of a particular type
- **Migration Planning**: Identify all configurations of a type that need to be migrated or updated
- **Documentation Generation**: Create type-specific documentation (all rules, all enrichments, etc.)
- **Validation**: Ensure type-specific validation rules are applied correctly
- **Inventory Management**: Track how many configurations exist for each type
- **Refactoring**: Find all configurations of a type that need refactoring

**When to Use**:
- Building type-specific views in configuration management UIs
- When you need to process all configurations of a specific type
- For type-based validation or migration operations
- To generate type-specific reports or documentation
- When analyzing the architecture and composition of your configuration repository
- For implementing type-specific workflows or access controls

**Find All Rule Configurations**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/type?type=rule-config"
```

**Find All Enrichment Configurations**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/type?type=enrichment"
```

**Response**:
```json
{
  "type": "rule-config",
  "count": 25,
  "results": [
    {
      "id": "graph100-groups-a",
      "name": "Graph100 Groups A",
      "path": "C:/Users/.../20-groups-a.yaml"
    },
    {
      "id": "graph100-rules-a",
      "name": "Graph100 Rules A",
      "path": "C:/Users/.../30-rules-a.yaml"
    }
  ]
}
```

---

### GET /api/catalog/discovery/author
**Purpose**: Find configurations by author

**Why Use This?**
- **Ownership Tracking**: Find all configurations created by a specific author
- **Accountability**: Identify who is responsible for specific configurations
- **Knowledge Management**: Locate configurations created by subject matter experts
- **Audit Trail**: Track configuration authorship for compliance and governance
- **Team Workload**: Understand the distribution of configuration ownership across team members
- **Handoff Planning**: Identify configurations that need ownership transfer when team members leave
- **Code Review**: Find all configurations by an author for review or quality assessment
- **Expertise Location**: Discover who has expertise in specific configuration areas

**When to Use**:
- For audit and compliance reporting on configuration ownership
- When planning team transitions or knowledge transfer
- To find configurations that need review by or from a specific author
- For workload analysis and team capacity planning
- When implementing author-based access controls or workflows
- To locate subject matter experts for specific configurations

**Find All Configurations by Demo Author**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/author?author=apex.demo@company.com"
```

**Response**:
```json
{
  "author": "apex.demo@company.com",
  "count": 95,
  "results": [
    {
      "id": "graph100-registry",
      "name": "Graph-100 Scenario Registry (Seed)",
      "type": "scenario-registry"
    }
  ]
}
```

---

### GET /api/catalog/discovery/health
**Purpose**: Find configurations by health score range

**Why Use This?**
- **Quality Monitoring**: Identify configurations with low health scores that need attention
- **Prioritization**: Focus refactoring efforts on the lowest-scoring configurations first
- **Quality Gates**: Enforce minimum health score requirements in CI/CD pipelines
- **Health Dashboards**: Display configurations grouped by health score ranges
- **Proactive Maintenance**: Find configurations approaching unhealthy states before they cause issues
- **Success Tracking**: Monitor improvements in health scores over time
- **Risk Assessment**: Identify high-risk configurations with poor health scores
- **Best Practices**: Find high-scoring configurations to use as examples

**When to Use**:
- Building health monitoring dashboards that show configurations by health grade
- When prioritizing technical debt and refactoring work
- For quality gate enforcement in deployment pipelines
- To identify configurations that need immediate attention
- When generating health reports for management or stakeholders
- For proactive maintenance and continuous improvement initiatives

**Find Configurations with High Health Scores (80-100)**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/health?minScore=80&maxScore=100"
```

**Find Configurations with Low Health Scores (0-50)**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/catalog/discovery/health?minScore=0&maxScore=50"
```

**Response**:
```json
{
  "minScore": 80,
  "maxScore": 100,
  "count": 67,
  "results": [
    {
      "id": "graph100-rules-a",
      "name": "Graph100 Rules A",
      "healthScore": 92,
      "healthGrade": "A"
    },
    {
      "id": "graph100-enrich-a",
      "name": "Graph100 Enrichment A",
      "healthScore": 88,
      "healthGrade": "B+"
    }
  ]
}
```

---

## 🏷️ Category Management Controller

**Base Path**: `/api/categories`

### Overview

The Category Management Controller provides comprehensive REST API endpoints for managing and querying APEX Rule Categories across your YAML configuration catalog. Categories provide an enterprise governance framework for organizing business rules with metadata inheritance, lifecycle management, and execution control.

**Key Features**:
- List all categories defined across YAML files
- Get detailed category information including usage statistics
- Search categories by business domain or owner
- Track category definitions and usage across rules/enrichments
- Monitor category lifecycle (active/expired)
- Generate category governance reports

### GET /api/categories
**Purpose**: Get all categories defined across all YAML files in the catalog

**Why Use This?**
- **Category Inventory**: Get a complete list of all rule categories across your configuration repository
- **Governance Overview**: View all categories with their business ownership and domain assignments
- **Metadata Inheritance**: Understand which categories provide metadata inheritance to rules and enrichments
- **Lifecycle Management**: See effective and expiration dates for all categories
- **Execution Control**: Review priority, parallel execution, and failure handling settings across categories
- **Usage Tracking**: Identify which categories are actively used vs. defined but unused
- **Dashboard Display**: Populate category management dashboards with complete category information
- **Audit Reporting**: Generate governance reports showing all categories and their ownership

**When to Use**:
- Building category management UIs that display all available categories
- For governance audits and compliance reporting
- When you need to understand the complete category taxonomy
- To identify unused or orphaned categories
- For generating category documentation
- When planning category consolidation or reorganization

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/categories"
```

**Response**:
```json
{
  "total": 5,
  "categories": [
    {
      "name": "customer-validation",
      "displayName": "Customer Validation Rules",
      "description": "Customer data validation rules",
      "priority": 10,
      "enabled": true,
      "businessDomain": "Customer Management",
      "businessOwner": "Customer Operations Team",
      "createdBy": "John Smith",
      "effectiveDate": "2025-01-01",
      "expirationDate": "2025-12-31",
      "stopOnFirstFailure": false,
      "parallelExecution": true,
      "definedInFiles": [
        "/path/to/customer-rules.yaml"
      ],
      "usedByRules": [],
      "usedByRuleGroups": [],
      "usedByEnrichments": [],
      "usedByEnrichmentGroups": [],
      "totalUsageCount": 0,
      "tags": ["validation", "customer"],
      "metadata": {}
    }
  ],
  "timestamp": "2025-11-09T10:30:00Z"
}
```

**Response Fields Explained**:
- **`total`**: Total number of categories found across all YAML files in the catalog
- **`categories`**: Array of category objects, each containing:
  - **`name`**: Unique category identifier (kebab-case, e.g., `"customer-validation"`)
  - **`displayName`**: Human-readable category name for UI display
  - **`description`**: Detailed description of the category's purpose and scope
  - **`priority`**: Execution priority (1-100). Higher priority categories execute first
  - **`enabled`**: Boolean indicating if the category is currently active
  - **`businessDomain`**: Business domain this category belongs to (e.g., `"Customer Management"`)
  - **`businessOwner`**: Team or individual responsible for this category (governance)
  - **`createdBy`**: Person who created this category
  - **`effectiveDate`**: ISO 8601 date when this category becomes active
  - **`expirationDate`**: ISO 8601 date when this category expires (null = no expiration)
  - **`stopOnFirstFailure`**: Boolean. If true, stop processing when first rule in this category fails
  - **`parallelExecution`**: Boolean. If true, rules in this category can execute in parallel
  - **`definedInFiles`**: Array of file paths where this category is defined
  - **`usedByRules`**: Array of rule IDs that use this category
  - **`usedByRuleGroups`**: Array of rule group IDs that use this category
  - **`usedByEnrichments`**: Array of enrichment IDs that use this category
  - **`usedByEnrichmentGroups`**: Array of enrichment group IDs that use this category
  - **`totalUsageCount`**: Total number of rules/enrichments using this category
  - **`tags`**: Array of tags for classification and search
  - **`metadata`**: Additional custom metadata key-value pairs
- **`timestamp`**: ISO 8601 timestamp when this response was generated

**What This Means**:
- Categories with `totalUsageCount: 0` are defined but not used (candidates for cleanup)
- `effectiveDate` and `expirationDate` control category lifecycle (temporal governance)
- `businessDomain` and `businessOwner` provide accountability and organization
- `priority` controls execution order when multiple categories are involved
- `stopOnFirstFailure` and `parallelExecution` control runtime behavior

---

### GET /api/categories/{categoryName}
**Purpose**: Get detailed information about a specific category

**Why Use This?**
- **Category Details**: Get complete information about a single category without retrieving all categories
- **Usage Analysis**: See exactly which rules, rule groups, enrichments, and enrichment groups use this category
- **Definition Tracking**: Identify which YAML files define this category
- **Governance Verification**: Verify business ownership, domain, and lifecycle dates for a specific category
- **Execution Settings**: Review priority, parallel execution, and failure handling configuration
- **Impact Assessment**: Understand the scope of impact before modifying or deprecating a category
- **Documentation**: Generate detailed documentation for specific categories
- **Troubleshooting**: Investigate category-related issues by examining complete category configuration

**When to Use**:
- When displaying category details in a UI
- Before modifying a category to understand its current usage
- For impact analysis when planning category changes
- To verify category configuration and governance metadata
- When troubleshooting issues related to a specific category
- For generating category-specific documentation

```bash
# Get customer-validation category details
curl -X GET "http://localhost:8082/yaml-manager/api/categories/customer-validation"
```

**Response**:
```json
{
  "category": {
    "name": "customer-validation",
    "displayName": "Customer Validation Rules",
    "description": "Customer data validation rules",
    "priority": 10,
    "enabled": true,
    "businessDomain": "Customer Management",
    "businessOwner": "Customer Operations Team",
    "createdBy": "John Smith",
    "effectiveDate": "2025-01-01",
    "expirationDate": "2025-12-31",
    "stopOnFirstFailure": false,
    "parallelExecution": true,
    "definedInFiles": [
      "/path/to/customer-rules.yaml"
    ],
    "totalUsageCount": 15,
    "tags": ["validation", "customer"]
  },
  "timestamp": "2025-11-09T10:30:00Z"
}
```

**Response Fields Explained**:
- **`category`**: Single category object with the same structure as in the `/api/categories` response (see above for field descriptions)
- **`timestamp`**: ISO 8601 timestamp when this response was generated

**What This Means**:
- This endpoint returns detailed information for a single category
- Use `totalUsageCount` to understand how widely this category is used
- Check `definedInFiles` to find where to modify the category definition
- Review `effectiveDate` and `expirationDate` to understand the category's lifecycle status
- If the category doesn't exist, you'll receive a 404 error (see below)

**Error Response (404)**:
```json
{
  "error": "Category not found: invalid-category",
  "timestamp": "2025-11-09T10:30:00Z"
}
```

**Error Response Explained**:
- **`error`**: Error message indicating the category name was not found in the catalog
- **`timestamp`**: When the error occurred

---

### GET /api/categories/search/business-domain/{businessDomain}
**Purpose**: Search categories by business domain

**Why Use This?**
- **Domain Organization**: Find all categories within a specific business domain
- **Domain Governance**: Review category ownership and organization within business domains
- **Cross-Domain Analysis**: Compare categories across different business domains
- **Domain-Specific Reporting**: Generate reports for specific business domains
- **Access Control**: Implement domain-based access control for category management
- **Team Coordination**: Help domain teams find and manage their categories
- **Architecture Review**: Understand how categories are distributed across business domains
- **Consolidation Planning**: Identify opportunities to consolidate categories within domains

**When to Use**:
- Building domain-specific views in category management UIs
- For domain-based governance and compliance reporting
- When implementing domain-based access controls
- To help business domain teams find their categories
- For architecture reviews focused on specific domains
- When planning category consolidation within domains

```bash
# Find all categories in Customer Management domain
curl -X GET "http://localhost:8082/yaml-manager/api/categories/search/business-domain/Customer%20Management"
```

**Response**:
```json
{
  "businessDomain": "Customer Management",
  "count": 3,
  "categories": [
    {
      "name": "customer-validation",
      "businessDomain": "Customer Management",
      "businessOwner": "Customer Operations Team"
    },
    {
      "name": "customer-enrichment",
      "businessDomain": "Customer Management",
      "businessOwner": "Customer Data Team"
    }
  ],
  "timestamp": "2025-11-09T10:30:00Z"
}
```

---

### GET /api/categories/search/business-owner/{businessOwner}
**Purpose**: Search categories by business owner

**Why Use This?**
- **Ownership Management**: Find all categories owned by a specific team or individual
- **Accountability Tracking**: Identify who is responsible for category governance
- **Team Workload**: Understand the distribution of category ownership across teams
- **Access Control**: Implement owner-based access control for category management
- **Handoff Planning**: Identify categories that need ownership transfer during team changes
- **Governance Reporting**: Generate reports showing category ownership distribution
- **Contact Discovery**: Find the right team to contact about specific categories
- **Capacity Planning**: Assess team capacity based on number of categories owned

**When to Use**:
- Building owner-specific views in category management UIs
- For governance audits focused on ownership and accountability
- When implementing owner-based access controls or workflows
- To help teams find and manage their categories
- For workload analysis and capacity planning
- When planning ownership transfers or team reorganizations

```bash
# Find all categories owned by Customer Operations Team
curl -X GET "http://localhost:8082/yaml-manager/api/categories/search/business-owner/Customer%20Operations%20Team"
```

**Response**:
```json
{
  "businessOwner": "Customer Operations Team",
  "count": 2,
  "categories": [
    {
      "name": "customer-validation",
      "businessDomain": "Customer Management",
      "businessOwner": "Customer Operations Team"
    }
  ],
  "timestamp": "2025-11-09T10:30:00Z"
}
```

---

### GET /api/categories/{categoryName}/definitions
**Purpose**: Get all YAML files that define a specific category

**Why Use This?**
- **Definition Tracking**: Find all YAML files where a category is defined
- **Duplicate Detection**: Identify if a category is defined in multiple files (potential issue)
- **Source Location**: Locate the source file to modify category configuration
- **Impact Analysis**: Understand which files need to be updated when changing a category
- **Consistency Verification**: Ensure category definitions are consistent across files
- **Refactoring**: Identify files that need updates during category consolidation
- **Documentation**: Generate documentation showing where categories are defined
- **Troubleshooting**: Debug category-related issues by finding definition sources

**When to Use**:
- Before modifying a category to find where it's defined
- When investigating duplicate or inconsistent category definitions
- For refactoring efforts that involve category consolidation
- To generate documentation showing category sources
- When troubleshooting category configuration issues
- For impact analysis before making category changes

```bash
# Find where customer-validation category is defined
curl -X GET "http://localhost:8082/yaml-manager/api/categories/customer-validation/definitions"
```

**Response**:
```json
{
  "categoryName": "customer-validation",
  "definedInFiles": [
    "/path/to/customer-rules.yaml",
    "/path/to/customer-categories.yaml"
  ],
  "count": 2,
  "timestamp": "2025-11-09T10:30:00Z"
}
```

---

### GET /api/categories/{categoryName}/usage
**Purpose**: Get usage statistics for a specific category

**Why Use This?**
- **Usage Analysis**: See exactly which rules, rule groups, enrichments, and enrichment groups use this category
- **Impact Assessment**: Understand the scope of impact before modifying or deprecating a category
- **Adoption Tracking**: Monitor how widely a category is being used across configurations
- **Unused Category Detection**: Identify categories with zero usage that may be candidates for removal
- **Dependency Mapping**: Map category dependencies to understand relationships
- **Refactoring Planning**: Assess the effort required to refactor or consolidate categories
- **Documentation**: Generate usage reports for category documentation
- **Governance**: Track category adoption and usage patterns for governance reporting

**When to Use**:
- Before modifying or deprecating a category to understand impact
- For adoption tracking and usage analysis
- When identifying unused categories for cleanup
- To generate category usage reports for stakeholders
- For refactoring planning and effort estimation
- When investigating why a category exists or is needed

```bash
# Get usage statistics for customer-validation category
curl -X GET "http://localhost:8082/yaml-manager/api/categories/customer-validation/usage"
```

**Response**:
```json
{
  "categoryName": "customer-validation",
  "totalUsageCount": 15,
  "usedByRules": [
    "validate-customer-email",
    "validate-customer-phone"
  ],
  "usedByRuleGroups": [
    "customer-validation-group"
  ],
  "usedByEnrichments": [],
  "usedByEnrichmentGroups": [],
  "timestamp": "2025-11-09T10:30:00Z"
}
```

**Response Fields Explained**:
- **`categoryName`**: Name of the category being analyzed
- **`totalUsageCount`**: Total number of rules, rule groups, enrichments, and enrichment groups using this category
- **`usedByRules`**: Array of rule IDs that reference this category. These rules inherit metadata from the category
- **`usedByRuleGroups`**: Array of rule group IDs that reference this category
- **`usedByEnrichments`**: Array of enrichment IDs that reference this category
- **`usedByEnrichmentGroups`**: Array of enrichment group IDs that reference this category
- **`timestamp`**: ISO 8601 timestamp when this usage analysis was performed

**What This Means**:
- If `totalUsageCount: 0`, the category is defined but not used (candidate for removal)
- The arrays show exactly which components depend on this category
- Use this information to assess the impact of modifying or deprecating the category
- Empty arrays indicate the category is not used by that component type

---

### GET /api/categories/statistics
**Purpose**: Get category statistics across the entire catalog

**Why Use This?**
- **Governance Metrics**: Get high-level metrics on category usage and organization
- **Health Monitoring**: Track active vs. expired categories over time
- **Usage Analysis**: Identify most-used categories and average usage patterns
- **Taxonomy Overview**: Understand the breadth of business domains and owners
- **Dashboard Metrics**: Display key category statistics in governance dashboards
- **Trend Tracking**: Monitor changes in category count, usage, and lifecycle over time
- **Capacity Planning**: Understand category growth and usage trends
- **Quality Assessment**: Identify categories with low or no usage that may need review

**When to Use**:
- Building category governance dashboards
- For periodic governance reviews and audits
- When generating executive reports on category management
- To track category lifecycle and usage trends
- For capacity planning and taxonomy management
- When assessing the overall health of your category taxonomy

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/categories/statistics"
```

**Response**:
```json
{
  "totalCategories": 12,
  "totalBusinessDomains": 5,
  "totalBusinessOwners": 8,
  "activeCategories": 10,
  "expiredCategories": 2,
  "averageUsageCount": 7.5,
  "mostUsedCategory": "customer-validation",
  "mostUsedCategoryCount": 25,
  "timestamp": "2025-11-09T10:30:00Z"
}
```

**Response Fields Explained**:
- **`totalCategories`**: Total number of categories defined across all YAML files in the catalog
- **`totalBusinessDomains`**: Number of distinct business domains represented in categories. Shows the breadth of your taxonomy
- **`totalBusinessOwners`**: Number of distinct business owners/teams responsible for categories. Shows ownership distribution
- **`activeCategories`**: Number of categories that are currently active (not expired based on `expirationDate`)
- **`expiredCategories`**: Number of categories that have passed their expiration date. These should be reviewed for removal
- **`averageUsageCount`**: Average number of rules/enrichments using each category. Higher = better adoption
- **`mostUsedCategory`**: Name of the category with the highest usage count. This is your most critical category
- **`mostUsedCategoryCount`**: Number of rules/enrichments using the most-used category. Shows the impact of your most critical category
- **`timestamp`**: ISO 8601 timestamp when these statistics were calculated

**What This Means**:
- **High `expiredCategories`**: Time to clean up old categories
- **Low `averageUsageCount`**: Many categories are defined but not widely used (potential consolidation opportunity)
- **`mostUsedCategory`**: This category is critical - changes to it have high impact
- **High `totalBusinessDomains`**: Good domain coverage, but may indicate complexity
- **Low `totalBusinessOwners`**: Ownership may be concentrated (potential risk)

**Governance Insights**:
- Track these metrics over time to monitor category taxonomy health
- Compare `activeCategories` vs `expiredCategories` to assess lifecycle management
- Use `averageUsageCount` to identify underutilized categories
- Monitor `mostUsedCategory` for critical dependencies

---

### GET /api/categories/business-domains
**Purpose**: Get all distinct business domains from categories

**Why Use This?**
- **Domain Discovery**: Find all business domains that have categories defined
- **Filter Options**: Populate domain filter dropdowns in UIs
- **Taxonomy Overview**: Understand the business domain taxonomy across categories
- **Governance Planning**: Identify domains that need category governance attention
- **Organization Analysis**: Understand how categories are organized by business domain
- **Documentation**: Generate domain-based category documentation
- **Access Control**: Build domain-based access control lists
- **Reporting**: Create domain-based governance reports

**When to Use**:
- Building domain filter dropdowns in category management UIs
- For taxonomy discovery and organization analysis
- When generating domain-based reports or documentation
- To understand the scope of business domains in your system
- For implementing domain-based access controls

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/categories/business-domains"
```

**Response**:
```json
{
  "businessDomains": [
    "Customer Management",
    "Order Processing",
    "Payment Processing",
    "Fraud Detection",
    "Compliance"
  ],
  "count": 5,
  "timestamp": "2025-11-09T10:30:00Z"
}
```

---

### GET /api/categories/business-owners
**Purpose**: Get all distinct business owners from categories

**Why Use This?**
- **Owner Discovery**: Find all teams/individuals who own categories
- **Filter Options**: Populate owner filter dropdowns in UIs
- **Accountability Mapping**: Understand the distribution of category ownership
- **Contact Directory**: Build a directory of category owners for governance
- **Workload Analysis**: Assess how category ownership is distributed across teams
- **Access Control**: Build owner-based access control lists
- **Reporting**: Create ownership-based governance reports
- **Handoff Planning**: Identify all owners for transition planning

**When to Use**:
- Building owner filter dropdowns in category management UIs
- For ownership discovery and accountability mapping
- When generating ownership-based reports or documentation
- To understand who owns categories in your system
- For implementing owner-based access controls
- When planning team transitions or reorganizations

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/categories/business-owners"
```

**Response**:
```json
{
  "businessOwners": [
    "Customer Operations Team",
    "Order Management Team",
    "Payment Team",
    "Risk Management Team",
    "Compliance Team"
  ],
  "count": 5,
  "timestamp": "2025-11-09T10:30:00Z"
}
```

---

### GET /api/categories/active
**Purpose**: Get categories that are currently active (not expired)

**Why Use This?**
- **Active Category Inventory**: Get only categories that are currently in effect
- **Lifecycle Management**: Focus on categories that are actively being used
- **Effective Date Filtering**: Exclude expired or future categories from analysis
- **Production View**: See only categories that should be active in production
- **Governance Focus**: Prioritize governance efforts on active categories
- **Documentation**: Generate documentation for currently active categories only
- **Validation**: Ensure only active categories are being used in configurations
- **Cleanup Planning**: Identify active categories before planning deprecation

**When to Use**:
- Building production-focused views that show only active categories
- For lifecycle management and governance of current categories
- When validating that configurations use only active categories
- To generate documentation for currently effective categories
- For operational dashboards that focus on active categories
- When planning category deprecation or lifecycle changes

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/categories/active"
```

**Response**:
```json
{
  "count": 10,
  "categories": [
    {
      "name": "customer-validation",
      "effectiveDate": "2025-01-01",
      "expirationDate": "2025-12-31",
      "enabled": true
    }
  ],
  "timestamp": "2025-11-09T10:30:00Z"
}
```

---

### GET /api/categories/expired
**Purpose**: Get categories that are currently expired

**Why Use This?**
- **Expired Category Identification**: Find categories that have passed their expiration date
- **Cleanup Planning**: Identify categories that can be removed or archived
- **Legacy Detection**: Discover legacy categories that should no longer be used
- **Compliance Verification**: Ensure expired categories are not being used in active configurations
- **Lifecycle Management**: Track categories through their complete lifecycle including expiration
- **Deprecation Tracking**: Monitor categories that have been deprecated and expired
- **Audit Trail**: Maintain records of expired categories for compliance and audit purposes
- **Migration Planning**: Identify expired categories that need to be migrated to new versions

**When to Use**:
- For cleanup and maintenance activities to remove expired categories
- When auditing configurations to ensure expired categories are not in use
- For lifecycle management and governance reporting
- To generate reports on deprecated and expired categories
- When planning category migrations or updates
- For compliance audits that require tracking expired governance metadata

```bash
curl -X GET "http://localhost:8082/yaml-manager/api/categories/expired"
```

**Response**:
```json
{
  "count": 2,
  "categories": [
    {
      "name": "legacy-validation",
      "effectiveDate": "2024-01-01",
      "expirationDate": "2024-12-31",
      "enabled": false
    }
  ],
  "timestamp": "2025-11-09T10:30:00Z"
}
```

### Category Management Use Cases

#### Use Case 1: Category Governance Audit
```bash
# Get all categories and their ownership
curl -X GET "http://localhost:8082/yaml-manager/api/categories" | jq '.categories[] | {name, businessDomain, businessOwner}'

# Get statistics
curl -X GET "http://localhost:8082/yaml-manager/api/categories/statistics"
```

#### Use Case 2: Find Expired Categories
```bash
# Get all expired categories that need review
curl -X GET "http://localhost:8082/yaml-manager/api/categories/expired"
```

#### Use Case 3: Domain-Based Category Management
```bash
# Get all business domains
curl -X GET "http://localhost:8082/yaml-manager/api/categories/business-domains"

# Get categories for specific domain
curl -X GET "http://localhost:8082/yaml-manager/api/categories/search/business-domain/Customer%20Management"
```

#### Use Case 4: Category Usage Analysis
```bash
# Get usage statistics for a category
curl -X GET "http://localhost:8082/yaml-manager/api/categories/customer-validation/usage"

# Find where category is defined
curl -X GET "http://localhost:8082/yaml-manager/api/categories/customer-validation/definitions"
```

---

## Validation Controller

**Base Path**: `/api/validation`

### POST /api/validation/structure
**Purpose**: Validate YAML syntax and required metadata fields

**Validate Valid YAML File**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/validation/structure" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/30-rules-a.yaml"
```

**Validate Invalid YAML File**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/validation/structure" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/98-invalid.yaml"
```

**Response (Valid)**:
```json
{
  "isValid": true,
  "filePath": "30-rules-a.yaml",
  "validationType": "STRUCTURE",
  "issues": [],
  "score": 100,
  "message": "YAML structure is valid"
}
```

**Response (Invalid)**:
```json
{
  "isValid": false,
  "filePath": "98-invalid.yaml",
  "validationType": "STRUCTURE",
  "issues": [
    {
      "type": "ERROR",
      "category": "SYNTAX",
      "message": "Invalid YAML syntax: unterminated string at line 12",
      "line": 12,
      "column": 20,
      "severity": "HIGH"
    }
  ],
  "score": 0,
  "message": "YAML structure validation failed"
}
```

### POST /api/validation/references
**Purpose**: Validate file references and dependencies exist

**Validate File with Valid References**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/validation/references" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/20-groups-a.yaml&baseDir=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
```

**Response**:
```json
{
  "isValid": true,
  "filePath": "20-groups-a.yaml",
  "validationType": "REFERENCES",
  "issues": [],
  "score": 100,
  "referencesChecked": 6,
  "validReferences": 6,
  "invalidReferences": 0,
  "message": "All file references are valid"
}
```

### POST /api/validation/consistency
**Purpose**: Validate naming conventions and unique IDs

**Validate Consistency**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/validation/consistency" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/30-rules-a.yaml"
```

**Response**:
```json
{
  "isValid": true,
  "filePath": "30-rules-a.yaml",
  "validationType": "CONSISTENCY",
  "issues": [],
  "score": 95,
  "checksPerformed": [
    "unique-ids", "naming-conventions", "metadata-completeness"
  ],
  "message": "Configuration is consistent"
}
```

### POST /api/validation/all
**Purpose**: Perform comprehensive validation (structure + references + consistency)

**Comprehensive Validation**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/validation/all" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/10-scenario-a.yaml&baseDir=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
```

**Response**:
```json
{
  "isValid": true,
  "filePath": "10-scenario-a.yaml",
  "validationType": "COMPREHENSIVE",
  "overallScore": 87,
  "structureScore": 100,
  "referenceScore": 85,
  "consistencyScore": 78,
  "issues": [
    {
      "type": "WARNING",
      "category": "CONSISTENCY",
      "message": "Missing recommended field: business-priority",
      "severity": "LOW"
    }
  ],
  "message": "Validation completed with minor warnings"
}
```

### GET /api/validation/is-valid
**Purpose**: Quick validation status check

**Quick Validation Check**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/validation/is-valid?filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/30-rules-a.yaml"
```

**Response**:
```json
{
  "isValid": true,
  "filePath": "30-rules-a.yaml",
  "quickCheck": true,
  "score": 92
}
```

---

## 🏥 Health Check Controller

**Base Path**: `/api/health-checks`

### POST /api/health-checks/check
**Purpose**: Perform comprehensive health check with scoring

**Health Check for Valid Configuration**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/health-checks/check" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/30-rules-a.yaml&baseDir=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
```

**Health Check for Complex Configuration**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/health-checks/check" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml&baseDir=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
```

**Response**:
```json
{
  "filePath": "30-rules-a.yaml",
  "overallScore": 92,
  "grade": "A-",
  "structuralScore": 100,
  "referenceScore": 95,
  "consistencyScore": 88,
  "performanceScore": 90,
  "complianceScore": 85,
  "metadataScore": 95,
  "trend": "STABLE",
  "lastChecked": "2025-10-28T10:30:00Z",
  "recommendations": [
    "Consider adding business-priority metadata field",
    "Add more descriptive rule names"
  ]
}
```

### POST /api/health-checks/report
**Purpose**: Generate detailed health report with issues and recommendations

**Generate Health Report**:
```bash
curl -X POST "http://localhost:8082/yaml-manager/api/health-checks/report" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/20-groups-a.yaml&baseDir=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
```

**Response**:
```json
{
  "overallScore": 78,
  "structuralHealth": 85,
  "referenceHealth": 72,
  "consistencyHealth": 80,
  "performanceHealth": 75,
  "complianceHealth": 70,
  "issues": [
    {
      "type": "WARNING",
      "category": "REFERENCE",
      "message": "Referenced file '34-rules-e.yaml' has lower health score",
      "severity": "MEDIUM",
      "recommendation": "Review and improve referenced file health",
      "estimatedEffortHours": 2
    },
    {
      "type": "INFO",
      "category": "COMPLIANCE",
      "message": "Missing optional field: business-priority",
      "severity": "LOW",
      "recommendation": "Add business-priority for better categorization",
      "estimatedEffortHours": 0.5
    }
  ],
  "recommendations": [
    "Improve referenced file health scores",
    "Add missing optional metadata fields",
    "Consider splitting complex rule groups"
  ],
  "generatedAt": "2025-10-28T10:30:00Z",
  "estimatedEffortHours": 2.5,
  "filesAnalyzed": 1,
  "filesWithIssues": 1,
  "healthLevel": "GOOD"
}
```

### GET /api/health-checks/score
**Purpose**: Get health score only (lightweight operation)

**Get Health Score**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/health-checks/score?filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/60-enrich-a.yaml"
```

**Response**:
```json
{
  "filePath": "60-enrich-a.yaml",
  "overallScore": 88,
  "grade": "B+",
  "trend": "IMPROVING"
}
```

### GET /api/health-checks/is-healthy
**Purpose**: Quick boolean health check

**Quick Health Check**:
```bash
curl -X GET "http://localhost:8082/yaml-manager/api/health-checks/is-healthy?filePath=C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/70-config-db.yaml&threshold=70"
```

**Response**:
```json
{
  "filePath": "70-config-db.yaml",
  "isHealthy": true,
  "score": 82,
  "threshold": 70,
  "status": "HEALTHY"
}
```

---

## � Advanced Use Cases & Integration Examples

### Batch Analysis of Multiple Files
**Purpose**: Analyze health and dependencies for all files in Graph-100 dataset

```bash
#!/bin/bash
# Batch analysis script for Graph-100 dataset
BASE_PATH="C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
API_BASE="http://localhost:8082/yaml-manager/api"

echo "=== Batch Analysis of Graph-100 Dataset ==="

# 1. Scan the entire directory
echo "1. Scanning directory..."
curl -X POST "${API_BASE}/catalog/scan" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "directory=${BASE_PATH}"

# 2. Get catalog statistics
echo -e "\n2. Getting catalog statistics..."
curl -X GET "${API_BASE}/catalog/statistics"

# 3. Find files with low health scores
echo -e "\n3. Finding files with health issues..."
curl -X GET "${API_BASE}/catalog/discovery/health?minScore=0&maxScore=70"

# 4. Analyze dependency tree from root
echo -e "\n4. Analyzing complete dependency tree..."
curl -X POST "${API_BASE}/dependencies/analyze" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "filePath=${BASE_PATH}/00-scenario-registry.yaml"
```

### CI/CD Pipeline Integration
**Purpose**: Validate YAML configurations in automated pipeline

```bash
#!/bin/bash
# CI/CD Validation Script
set -e

YAML_MANAGER_URL="http://localhost:8082/yaml-manager/api"
CONFIG_DIR="C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100"
MIN_HEALTH_SCORE=75
EXIT_CODE=0

echo "=== YAML Configuration Validation Pipeline ==="

# Function to validate single file
validate_file() {
    local file_path="$1"
    local file_name=$(basename "$file_path")

    echo "Validating: $file_name"

    # 1. Structure validation
    response=$(curl -s -X POST "${YAML_MANAGER_URL}/validation/structure" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "filePath=$file_path")

    is_valid=$(echo "$response" | jq -r '.isValid')
    if [ "$is_valid" != "true" ]; then
        echo "STRUCTURE VALIDATION FAILED: $file_name"
        echo "$response" | jq '.issues'
        EXIT_CODE=1
        return
    fi

    # 2. Health check
    health_response=$(curl -s -X POST "${YAML_MANAGER_URL}/health-checks/check" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "filePath=$file_path&baseDir=$CONFIG_DIR")

    health_score=$(echo "$health_response" | jq -r '.overallScore')
    if [ "$health_score" -lt "$MIN_HEALTH_SCORE" ]; then
        echo "⚠️  HEALTH SCORE BELOW THRESHOLD: $file_name (Score: $health_score)"
        EXIT_CODE=1
    else
        echo "PASSED: $file_name (Score: $health_score)"
    fi
}

# Validate key configuration files
validate_file "${CONFIG_DIR}/00-scenario-registry.yaml"
validate_file "${CONFIG_DIR}/10-scenario-a.yaml"
validate_file "${CONFIG_DIR}/20-groups-a.yaml"
validate_file "${CONFIG_DIR}/30-rules-a.yaml"

# Check for circular dependencies
echo -e "\nChecking for circular dependencies..."
circular_response=$(curl -s -X GET "${YAML_MANAGER_URL}/dependencies/circular-dependencies")
circular_count=$(echo "$circular_response" | jq '.circularDependencies | length')

if [ "$circular_count" -gt 0 ]; then
    echo "CIRCULAR DEPENDENCIES DETECTED:"
    echo "$circular_response" | jq '.circularDependencies'
    EXIT_CODE=1
else
    echo "No circular dependencies found"
fi

echo -e "\n=== Validation Complete ==="
exit $EXIT_CODE
```

### JavaScript Frontend Integration
**Purpose**: Build interactive dashboard using the REST API

```javascript
// YAML Manager Dashboard Integration
class YamlManagerDashboard {
    constructor(apiBaseUrl = 'http://localhost:8082/yaml-manager/api') {
        this.apiBase = apiBaseUrl;
        this.graphPath = 'C:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100';
    }

    // Load and display dependency tree
    async loadDependencyTree(rootFile = '00-scenario-registry.yaml') {
        try {
            const response = await fetch(`${this.apiBase}/dependencies/tree?rootFile=${this.graphPath}/${rootFile}`);
            const data = await response.json();

            if (data.status === 'success') {
                this.renderTree(data.tree);
                this.updateMetrics(data.totalFiles, data.maxDepth);
                return data;
            }
        } catch (error) {
            console.error('Failed to load dependency tree:', error);
        }
    }

    // Get catalog statistics and display dashboard
    async loadDashboard() {
        try {
            // 1. Scan directory first
            await fetch(`${this.apiBase}/catalog/scan`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `directory=${this.graphPath}`
            });

            // 2. Get statistics
            const statsResponse = await fetch(`${this.apiBase}/catalog/statistics`);
            const stats = await statsResponse.json();

            // 3. Get health distribution
            const healthyResponse = await fetch(`${this.apiBase}/catalog/discovery/health?minScore=80&maxScore=100`);
            const healthyFiles = await healthyResponse.json();

            const unhealthyResponse = await fetch(`${this.apiBase}/catalog/discovery/health?minScore=0&maxScore=60`);
            const unhealthyFiles = await unhealthyResponse.json();

            // Update dashboard
            this.updateDashboard({
                totalFiles: stats.totalConfigurations,
                averageHealth: stats.averageHealthScore,
                healthyCount: healthyFiles.count,
                unhealthyCount: unhealthyFiles.count,
                orphanedCount: stats.orphanedCount,
                criticalCount: stats.criticalCount
            });

        } catch (error) {
            console.error('Failed to load dashboard:', error);
        }
    }

    // Validate specific file and show results
    async validateFile(filePath) {
        try {
            const response = await fetch(`${this.apiBase}/validation/all`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `filePath=${filePath}&baseDir=${this.graphPath}`
            });

            const validation = await response.json();
            this.displayValidationResults(validation);
            return validation;

        } catch (error) {
            console.error('Validation failed:', error);
        }
    }

    // Search configurations
    async searchConfigurations(query) {
        try {
            const response = await fetch(`${this.apiBase}/catalog/discovery/search?query=${encodeURIComponent(query)}`);
            const results = await response.json();
            this.displaySearchResults(results);
            return results;
        } catch (error) {
            console.error('Search failed:', error);
        }
    }

    // Helper methods for UI updates
    renderTree(treeData) {
        // D3.js tree rendering logic
        console.log('Rendering tree with', treeData);
    }

    updateMetrics(totalFiles, maxDepth) {
        document.getElementById('total-files').textContent = totalFiles;
        document.getElementById('max-depth').textContent = maxDepth;
    }

    updateDashboard(stats) {
        document.getElementById('total-configurations').textContent = stats.totalFiles;
        document.getElementById('average-health').textContent = stats.averageHealth.toFixed(1);
        document.getElementById('healthy-files').textContent = stats.healthyCount;
        document.getElementById('unhealthy-files').textContent = stats.unhealthyCount;
    }

    displayValidationResults(validation) {
        const resultsDiv = document.getElementById('validation-results');
        resultsDiv.innerHTML = `
            <h3>Validation Results</h3>
            <p>Overall Score: ${validation.overallScore}/100</p>
            <p>Status: ${validation.isValid ? 'Valid' : 'Invalid'}</p>
            <div class="issues">
                ${validation.issues.map(issue => `
                    <div class="issue ${issue.type.toLowerCase()}">
                        <strong>${issue.type}:</strong> ${issue.message}
                    </div>
                `).join('')}
            </div>
        `;
    }

    displaySearchResults(results) {
        const resultsDiv = document.getElementById('search-results');
        resultsDiv.innerHTML = `
            <h3>Search Results (${results.count})</h3>
            <div class="results-list">
                ${results.results.map(result => `
                    <div class="result-item">
                        <h4>${result.name}</h4>
                        <p>Type: ${result.type}</p>
                        <p>Business Domain: ${result.businessDomain}</p>
                        <p>Health Score: ${result.healthScore || 'N/A'}</p>
                    </div>
                `).join('')}
            </div>
        `;
    }
}

// Usage example
const dashboard = new YamlManagerDashboard();

// Initialize dashboard
dashboard.loadDashboard();

// Load dependency tree
dashboard.loadDependencyTree('00-scenario-registry.yaml');

// Example search
dashboard.searchConfigurations('trade processing');

// Example validation
dashboard.validateFile(`${dashboard.graphPath}/30-rules-a.yaml`);
```

---

## Troubleshooting Guide

### Common Issues & Solutions

#### Service Unavailable (500 Error)
```bash
# Check if service is running
curl http://localhost:8082/yaml-manager/api/health

# Expected response:
# {"status":"UP","service":"apex-yaml-manager","version":"1.0.0"}
```

**Solution**: Ensure YAML Manager is started with `mvn spring-boot:run -pl apex-yaml-manager`

#### File Path Issues (400 Error)
```bash
# Incorrect (Windows paths need encoding)
curl "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=C:\configs\file.yaml"

# Correct (URL encoded)
curl "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=C%3A%2FUsers%2Fmraysmit%2Fdev%2Fidea-projects%2Fapex-rules-engine%2Fapex-yaml-manager%2Fsrc%2Ftest%2Fresources%2Fapex-yaml-samples%2Fgraph-100%2F00-scenario-registry.yaml"
```

**Solution**: Always URL-encode file paths, especially on Windows systems.

#### CORS Errors in Browser
```javascript
// Error: CORS policy blocks request
fetch('http://localhost:8082/yaml-manager/api/catalog/configurations')
  .catch(error => console.error('CORS Error:', error));
```

**Solution**: CORS is configured for localhost. Ensure you're accessing from `http://localhost:*` or `file://` origins.

#### Large Dataset Timeouts
```bash
# For large datasets, increase timeout
curl -X POST "http://localhost:8082/yaml-manager/api/catalog/scan" \
  --max-time 300 \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "directory=/path/to/large/dataset"
```

**Solution**: Use `--max-time` parameter for curl or increase application timeout settings.

### Performance Tips

#### Optimize Batch Operations
```bash
# Good: Scan once, then query multiple times
curl -X POST "${API_BASE}/catalog/scan" -d "directory=${BASE_PATH}"
curl -X GET "${API_BASE}/catalog/statistics"
curl -X GET "${API_BASE}/catalog/discovery/health?minScore=0&maxScore=70"

# Avoid: Multiple scans
curl -X POST "${API_BASE}/catalog/scan" -d "directory=${BASE_PATH}"
curl -X POST "${API_BASE}/catalog/scan" -d "directory=${BASE_PATH}"  # Redundant
```

#### Use Appropriate Endpoints
```bash
# For quick health check
curl "${API_BASE}/health-checks/score?filePath=${FILE}"

# Avoid for simple checks
curl -X POST "${API_BASE}/health-checks/report" -d "filePath=${FILE}"  # Too heavy
```

---

## Quick Reference

### Essential Endpoints Summary

| Operation | Method | Endpoint | Purpose |
|-----------|--------|----------|---------|
| **System Health** | GET | `/api/health` | Check service status |
| **Scan Directory** | POST | `/api/catalog/scan` | Index YAML files |
| **Dependency Tree** | GET | `/api/dependencies/tree` | Generate tree visualization |
| **Validate File** | POST | `/api/validation/all` | Comprehensive validation |
| **Health Check** | POST | `/api/health-checks/check` | File health scoring |
| **Search Configs** | GET | `/api/catalog/discovery/search` | Find configurations |
| **List Categories** | GET | `/api/categories` | Get all categories |
| **Category Details** | GET | `/api/categories/{name}` | Get category information |
| **Category Statistics** | GET | `/api/categories/statistics` | Category governance metrics |

### Graph-100 Sample Files

| File | Type | Purpose | Dependencies |
|------|------|---------|--------------|
| `00-scenario-registry.yaml` | scenario-registry | Root file | 4 scenarios |
| `10-scenario-a.yaml` | scenario | Complex scenario | groups, cycles, configs |
| `20-groups-a.yaml` | rule-config | Rule groups | rules, enrichments |
| `30-rules-a.yaml` | rule-config | Business rules | None |
| `60-enrich-a.yaml` | enrichment | Data enrichment | None |
| `70-config-db.yaml` | external-data-config | Database config | None |
| `98-invalid.yaml` | rule-config | Invalid YAML | None (for testing) |

### Response Status Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | Success | Operation completed successfully |
| 400 | Bad Request | Invalid file path, missing parameters |
| 404 | Not Found | File doesn't exist |
| 500 | Server Error | Service unavailable, internal error |

### Health Score Grades

| Score | Grade | Meaning |
|-------|-------|---------|
| 90-100 | A | Excellent health |
| 80-89 | B | Good health |
| 70-79 | C | Acceptable health |
| 60-69 | D | Poor health |
| 0-59 | F | Critical issues |

---

## 🔗 Additional Resources

- **Interactive Tree Viewer**: http://localhost:8082/yaml-manager/d3-tree-viewer.html
- **Swagger Documentation**: http://localhost:8082/yaml-manager/swagger-ui.html
- **OpenAPI Specification**: http://localhost:8082/yaml-manager/api-docs
- **Graph-100 Dataset**: `apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/`
- **Source Code**: `apex-yaml-manager/src/main/java/dev/mars/apex/yaml/manager/controller/`

---

*This comprehensive guide provides copy-paste examples for every REST API endpoint in the APEX YAML Manager. All examples use real files from the Graph-100 dataset for practical, working demonstrations.*

### 🔧 **Configuration & Deployment**

#### **Application Properties**
```properties
# Server Configuration
server.port=8082
server.servlet.context-path=/yaml-manager

# YAML Manager Settings
yaml-manager.max-file-size=10485760
yaml-manager.scan-timeout-seconds=300
yaml-manager.cache-enabled=true
yaml-manager.cache-ttl-minutes=60
```

#### **Docker & Container Support**
- **TestContainers Integration**: Database testing
- **PostgreSQL Support**: Enterprise database integration
- **H2 Database**: Development and testing

### 📈 **Performance Optimizations**

#### **Caching Strategy**
- **Service-Level Caching**: Dependency analysis results
- **TTL-Based Expiration**: Configurable cache timeouts
- **Memory Management**: Efficient large dataset handling

#### **Async Processing**
- **Non-blocking Operations**: Improved response times
- **Background Scanning**: Directory indexing
- **Batch Processing**: Multiple file analysis

## Migration Guide

### From Previous Version
1. **Update Dependencies**: Ensure latest `apex-core` version
2. **Configuration**: Update `application.properties` with new settings
3. **API Clients**: Update to use new endpoint structure
4. **Testing**: Run comprehensive test suite

### New Features Available
- Replace manual dependency tracking with automated analysis
- Integrate health monitoring into CI/CD pipelines
- Use catalog search for configuration discovery
- Implement validation gates in deployment processes

## Future Roadmap

### Phase 5: Refactoring Tools (PLANNED)
- **Dependency Simplification**: Automated refactoring suggestions
- **Configuration Consolidation**: Merge similar configurations
- **Performance Optimization**: Identify and resolve bottlenecks
- **Safe Refactoring**: Validation-backed transformations

### Advanced Features (PLANNED)
- **Real-time Monitoring**: Live health dashboards
- **Automated Remediation**: Self-healing configurations
- **ML-based Insights**: Predictive analysis and recommendations
- **Enterprise Integration**: LDAP, SSO, audit logging

## Support & Documentation

- **OpenAPI/Swagger**: `http://localhost:8082/yaml-manager/swagger-ui.html`
- **API Documentation**: `http://localhost:8082/yaml-manager/api-docs`
- **Interactive UI**: `http://localhost:8082/yaml-manager/d3-tree-viewer.html`
- **Health Monitoring**: `http://localhost:8082/yaml-manager/api/health`

## Technical Implementation Details

### Error Handling & Response Patterns

#### **Standardized Error Responses**
All endpoints follow consistent error response patterns:

```json
{
  "status": "error",
  "message": "Validation service not available",
  "timestamp": 1698765432000,
  "path": "/api/validation/structure"
}
```

#### **HTTP Status Codes**
- **200 OK**: Successful operation
- **400 Bad Request**: Invalid parameters or malformed request
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Service unavailable or internal error

#### **Service Availability Checks**
All controllers implement graceful degradation when services are unavailable:

```java
if (validationService == null) {
    return ResponseEntity.status(500).body("Validation service not available");
}
```

### Security & Authentication

#### **CORS Configuration**
Enhanced cross-origin resource sharing for development and production:

```java
@CrossOrigin(origins = {
    "http://localhost:*",
    "file://*",
    "https://your-production-domain.com"
})
```

#### **Input Validation**
- **Parameter Validation**: Required field checking
- **Path Traversal Protection**: Secure file path handling
- **URL Decoding**: Proper handling of encoded file paths
- **Size Limits**: Configurable file size restrictions

### Performance Metrics & Monitoring

#### **Response Time Optimization**
- **Average Response Time**: < 200ms for simple operations
- **Large Dataset Handling**: Optimized for 100+ file analysis
- **Memory Usage**: Efficient caching and garbage collection
- **Concurrent Requests**: Thread-safe service implementations

#### **Monitoring Endpoints**
```bash
# System health with detailed metrics
GET /api/health/status

# Performance statistics
GET /api/catalog/statistics

# Dependency complexity metrics
GET /api/dependencies/metrics
```

### API Versioning Strategy

#### **Current Version: v1**
- **URL Path Versioning**: `/api/v1/` (future-ready)
- **Header Versioning**: `Accept: application/vnd.apex.v1+json`
- **Backward Compatibility**: 12-month support policy
- **Migration Guides**: Provided for major version changes

#### **Version Information**
```json
{
  "api": {
    "version": "v1",
    "majorVersion": 1,
    "minorVersion": 0,
    "patchVersion": 0,
    "status": "stable",
    "releaseDate": "2025-10-28"
  }
}
```

## Integration Examples

### JavaScript/Frontend Integration

#### **Dependency Tree Loading**
```javascript
async function loadDependencyTree(rootFile) {
    const response = await fetch(`/api/dependencies/tree?rootFile=${encodeURIComponent(rootFile)}`);
    const data = await response.json();

    if (data.status === 'success') {
        renderTree(data.tree);
        updateMetrics(data.totalFiles, data.maxDepth);
    }
}
```

#### **Health Check Integration**
```javascript
async function checkFileHealth(filePath) {
    const response = await fetch('/api/health-checks/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `filePath=${encodeURIComponent(filePath)}`
    });

    const healthScore = await response.json();
    updateHealthIndicator(healthScore.overallScore, healthScore.grade);
}
```

### Java/Spring Integration

#### **Service Layer Usage**
```java
@Service
public class ConfigurationManagementService {

    @Autowired
    private DependencyAnalysisService dependencyService;

    @Autowired
    private HealthCheckService healthService;

    public ConfigurationReport analyzeConfiguration(String filePath) {
        // Analyze dependencies
        EnhancedYamlDependencyGraph graph = dependencyService.analyzeDependencies(filePath);
        DependencyMetrics metrics = dependencyService.calculateMetrics(graph);

        // Check health
        HealthScore health = healthService.performHealthCheck(filePath);

        return new ConfigurationReport(metrics, health);
    }
}
```

### CI/CD Pipeline Integration

#### **Jenkins Pipeline Example**
```groovy
pipeline {
    stages {
        stage('YAML Validation') {
            steps {
                script {
                    def response = sh(
                        script: "curl -X POST '${YAML_MANAGER_URL}/api/validation/all?filePath=${CONFIG_FILE}'",
                        returnStdout: true
                    )

                    def validation = readJSON text: response
                    if (!validation.isValid) {
                        error("YAML validation failed: ${validation.issues}")
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def healthResponse = sh(
                        script: "curl '${YAML_MANAGER_URL}/api/health-checks/score?filePath=${CONFIG_FILE}'",
                        returnStdout: true
                    )

                    def health = readJSON text: healthResponse
                    if (health.overallScore < 70) {
                        unstable("Configuration health score below threshold: ${health.overallScore}")
                    }
                }
            }
        }
    }
}
```

#### **GitHub Actions Example**
```yaml
name: YAML Configuration Validation
on: [push, pull_request]

jobs:
  validate-configs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Start YAML Manager
        run: |
          docker run -d -p 8082:8082 apex-yaml-manager:latest
          sleep 30

      - name: Validate Configurations
        run: |
          for file in configs/*.yaml; do
            response=$(curl -s -X POST "http://localhost:8082/api/validation/all?filePath=$file")
            if ! echo "$response" | jq -e '.isValid'; then
              echo "Validation failed for $file"
              exit 1
            fi
          done

      - name: Generate Health Report
        run: |
          curl -X POST "http://localhost:8082/api/health-checks/report?filePath=configs/" \
            -o health-report.json
```

## Troubleshooting Guide

### Common Issues & Solutions

#### **Service Unavailable Errors**
```bash
# Check service status
curl http://localhost:8082/api/health

# Verify service dependencies
curl http://localhost:8082/api/health/status
```

**Solution**: Ensure all required services are properly initialized and dependencies are available.

#### **File Path Issues**
```bash
# Incorrect (may fail with special characters)
curl "http://localhost:8082/api/dependencies/tree?rootFile=C:\configs\file.yaml"

# Correct (URL encoded)
curl "http://localhost:8082/api/dependencies/tree?rootFile=C%3A%5Cconfigs%5Cfile.yaml"
```

**Solution**: Always URL-encode file paths, especially on Windows systems.

#### **CORS Errors in Browser**
```javascript
// Error: CORS policy blocks request
fetch('http://localhost:8082/api/catalog/configurations')

// Solution: Ensure CORS is properly configured
// Check application.properties for cors.allowed-origins
```

#### **Large Dataset Timeouts**
```properties
# Increase timeout for large datasets
yaml-manager.scan-timeout-seconds=600
yaml-manager.max-file-size=52428800
```

### Performance Tuning

#### **Memory Optimization**
```properties
# JVM settings for large datasets
-Xmx4g -Xms2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

#### **Cache Configuration**
```properties
# Optimize caching for your use case
yaml-manager.cache-enabled=true
yaml-manager.cache-ttl-minutes=30
yaml-manager.cache-max-entries=1000
```

---

*This document reflects the current state of the APEX YAML Manager REST API as of October 2025. For the latest updates and detailed API specifications, refer to the OpenAPI documentation.*
