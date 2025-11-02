# APEX YAML Manager - Complete REST API Guide

## Overview

This comprehensive guide provides **copy-paste examples** for every REST API endpoint in the APEX YAML Manager. All examples use the **Graph-100 dataset** located in `apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/` which contains 100+ interconnected YAML files designed to test complex dependency scenarios.

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

#### Option 2: APEX YAML Manager (Configuration Management)
**Port**: 8082 | **Purpose**: YAML Configuration Management & Dependency Analysis

```bash
# Start from apex-yaml-manager directory
cd apex-yaml-manager
mvn spring-boot:run

# OR start from root directory
mvn spring-boot:run -pl apex-yaml-manager
```

**Verify YAML Manager:**
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
| **apex-yaml-manager** | 8082 | YAML Configuration Management | `/yaml-manager` | **This document** - Dependency analysis, health checks, catalog management |

> **Note**: This document focuses on the **APEX YAML Manager** endpoints. For core rules engine API documentation, see the apex-rest-api Swagger documentation.

---

## 🏥 Health Controller

**Base Path**: `/api/health`

### GET /api/health
**Purpose**: Basic health check to verify service is running

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

### GET /api/health/status
**Purpose**: Detailed system status with capabilities

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

---

## 🔍 Dependency Analysis Controller

**Base Path**: `/api/dependencies`

### POST /api/dependencies/analyze
**Purpose**: Analyze YAML file dependencies and calculate comprehensive metrics

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

### GET /api/dependencies/tree
**Purpose**: Generate D3.js-compatible hierarchical dependency tree

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

### GET /api/dependencies/content
**Purpose**: Retrieve file content and metadata for a specific YAML file

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

---

## 📚 Catalog Controller

**Base Path**: `/api/catalog`

### POST /api/catalog/scan
**Purpose**: Scan directory and build comprehensive catalog index

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

### GET /api/catalog/configurations
**Purpose**: Get all configurations with optional field filtering

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

### GET /api/catalog/metadata/{id}
**Purpose**: Get detailed metadata for specific configuration

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

### GET /api/catalog/statistics
**Purpose**: Get comprehensive catalog statistics

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

### GET /api/catalog/discovery/search
**Purpose**: Full-text search across all configuration descriptions and metadata

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

### GET /api/catalog/discovery/tags
**Purpose**: Find configurations by specific tags

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

### GET /api/catalog/discovery/type
**Purpose**: Find configurations by document type

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

### GET /api/catalog/discovery/author
**Purpose**: Find configurations by author

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

### GET /api/catalog/discovery/health
**Purpose**: Find configurations by health score range

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

## ✅ Validation Controller

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
        echo "❌ STRUCTURE VALIDATION FAILED: $file_name"
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
        echo "✅ PASSED: $file_name (Score: $health_score)"
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
    echo "❌ CIRCULAR DEPENDENCIES DETECTED:"
    echo "$circular_response" | jq '.circularDependencies'
    EXIT_CODE=1
else
    echo "✅ No circular dependencies found"
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
            <p>Status: ${validation.isValid ? '✅ Valid' : '❌ Invalid'}</p>
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

## 🚨 Troubleshooting Guide

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
# ❌ Incorrect (Windows paths need encoding)
curl "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=C:\configs\file.yaml"

# ✅ Correct (URL encoded)
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
# ✅ Good: Scan once, then query multiple times
curl -X POST "${API_BASE}/catalog/scan" -d "directory=${BASE_PATH}"
curl -X GET "${API_BASE}/catalog/statistics"
curl -X GET "${API_BASE}/catalog/discovery/health?minScore=0&maxScore=70"

# ❌ Avoid: Multiple scans
curl -X POST "${API_BASE}/catalog/scan" -d "directory=${BASE_PATH}"
curl -X POST "${API_BASE}/catalog/scan" -d "directory=${BASE_PATH}"  # Redundant
```

#### Use Appropriate Endpoints
```bash
# ✅ For quick health check
curl "${API_BASE}/health-checks/score?filePath=${FILE}"

# ❌ Avoid for simple checks
curl -X POST "${API_BASE}/health-checks/report" -d "filePath=${FILE}"  # Too heavy
```

---

## 📋 Quick Reference

### Essential Endpoints Summary

| Operation | Method | Endpoint | Purpose |
|-----------|--------|----------|---------|
| **System Health** | GET | `/api/health` | Check service status |
| **Scan Directory** | POST | `/api/catalog/scan` | Index YAML files |
| **Dependency Tree** | GET | `/api/dependencies/tree` | Generate tree visualization |
| **Validate File** | POST | `/api/validation/all` | Comprehensive validation |
| **Health Check** | POST | `/api/health-checks/check` | File health scoring |
| **Search Configs** | GET | `/api/catalog/discovery/search` | Find configurations |

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
