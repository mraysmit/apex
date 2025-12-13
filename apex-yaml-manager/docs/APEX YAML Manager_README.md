# APEX YAML Manager

**Enterprise YAML Configuration Management System for APEX Rules Engine**

Version: 1.0.0 | Port: 8082 | Context: `/yaml-manager`

---

## 📋 Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Features](#features)
- [API Endpoints](#api-endpoints)
- [Web UI](#web-ui)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Development](#development)
- [Integration](#integration-with-other-modules)
- [Troubleshooting](#troubleshooting)

---

## Overview

The APEX YAML Manager provides comprehensive tools for managing the complexity of APEX YAML configurations at enterprise scale. It enables developers, architects, and operations teams to understand, analyze, and optimize their configuration ecosystems.

**Key Capabilities:**
- 🔍 **Dependency Analysis** - Visualize and understand configuration relationships
- 📚 **Catalog Discovery** - Search and discover configurations across your system
- ✅ **Health Checks** - Assess configuration quality and identify issues
- 🔧 **Validation** - Comprehensive structural and reference validation
- 📊 **Visualization** - Interactive D3.js dependency tree viewer
- 🌐 **REST API** - Full programmatic access to all features

## Features

### Layer 1: Enhanced Dependency Analysis
- Bidirectional dependency graphs (forward and reverse edges)
- Impact analysis (what breaks if I change this?)
- Comprehensive metrics (complexity, depth, criticality)
- Circular dependency detection
- Orphaned file identification

### Layer 2: Catalog and Discovery System
- Centralized metadata index of all configurations
- Full-text search across descriptions, tags, authors
- Domain-based and use-case based discovery
- Relationship queries (find all files using X)
- Unused configuration detection
- Critical configuration identification

### Layer 3: Validation and Health Checks ✅ COMPLETE
- ✅ Structural validation (syntax, required fields)
- ✅ Reference validation (all references exist)
- ✅ Consistency checks (unique IDs, naming conventions)
- ✅ Performance checks (detect overly deep chains)
- ✅ Compliance checks (required metadata)
- ✅ Health scoring (0-100 scale)
- ✅ Automated issue detection with recommendations
- ✅ REST API endpoints for all validation operations
- ✅ 28 comprehensive test cases

### Layer 4: Refactoring and Optimization Tools
- Dependency simplification suggestions
- Consolidation analysis (files that could be merged)
- Splitting analysis (files that should be split)
- Reusability suggestions (common patterns)
- Safe refactoring with validation
- Performance optimization suggestions

### Layer 5: UI and Visualization ✅ COMPLETE
- ✅ Interactive D3.js dependency tree visualization
- ✅ Node expansion/collapse with click interaction
- ✅ Pan and zoom controls with toolbar
- ✅ YAML content viewer with syntax highlighting
- ✅ File metadata display panel
- ✅ APEX keyword colorization
- ✅ Resizable split-panel layout
- ✅ REST API for programmatic access
- Catalog browser with search (planned)
- Health dashboard with real-time metrics (planned)
- Impact analysis visualization (planned)

---

## 🚀 Quick Start

### Prerequisites

Before starting, ensure you have:
- ✅ **Java 21+** installed (`java -version`)
- ✅ **Maven 3.8+** installed (`mvn -version`)
- ✅ **APEX Core module** built successfully

### Step 1: Build the Module

From the repository root directory:

```bash
# Build the entire project (recommended)
mvn clean install

# Or build just apex-yaml-manager
mvn clean install -pl apex-yaml-manager
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 15.234 s
```

### Step 2: Start the Service

**Option A: Using Maven (Development)**
```bash
mvn spring-boot:run -pl apex-yaml-manager
```

**Option B: Using Java JAR (Production)**
```bash
cd apex-yaml-manager/target
java -jar apex-yaml-manager-1.0-SNAPSHOT.jar
```

**Expected Output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.0)

2025-11-06 20:15:30 - Started YamlManagerApplication in 3.456 seconds
2025-11-06 20:15:30 - Tomcat started on port(s): 8082 (http)
```

### Step 3: Verify Service is Running

**Check Health Endpoint:**
```bash
curl http://localhost:8082/yaml-manager/api/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "service": "apex-yaml-manager",
  "version": "1.0.0",
  "timestamp": "2025-11-06T20:15:35.123Z"
}
```

### Step 4: Access the Web UI

Open your browser and navigate to:

**🌳 Dependency Tree Viewer:**
```
http://localhost:8082/yaml-manager/d3-tree-viewer.html
```

**📖 API Documentation (Swagger):**
```
http://localhost:8082/yaml-manager/swagger-ui.html
```

### Step 5: Stop the Service

**If running with Maven:**
- Press `Ctrl+C` in the terminal

**If running as JAR:**
- Press `Ctrl+C` in the terminal
- Or find and kill the process:
  ```bash
  # Windows
  netstat -ano | findstr :8082
  taskkill /PID <process_id> /F

  # Linux/Mac
  lsof -i :8082
  kill -9 <process_id>
  ```

---

## 🌐 Service URLs

Once the service is running, access these URLs:

### Web UI
| Feature | URL | Description |
|---------|-----|-------------|
| **Dependency Tree Viewer** | `http://localhost:8082/yaml-manager/d3-tree-viewer.html` | Interactive D3.js visualization |

### API Documentation
| Feature | URL | Description |
|---------|-----|-------------|
| **Swagger UI** | `http://localhost:8082/yaml-manager/swagger-ui.html` | Interactive API documentation |
| **OpenAPI JSON** | `http://localhost:8082/yaml-manager/api-docs` | OpenAPI 3.0 specification |

### Health & Monitoring
| Feature | URL | Description |
|---------|-----|-------------|
| **System Health** | `http://localhost:8082/yaml-manager/api/health` | Service health check |
| **System Status** | `http://localhost:8082/yaml-manager/api/health/status` | Detailed status with capabilities |
| **Actuator Health** | `http://localhost:8082/yaml-manager/actuator/health` | Spring Boot actuator health |
| **Actuator Metrics** | `http://localhost:8082/yaml-manager/actuator/metrics` | Application metrics |

---

## 🌳 Web UI

### Dependency Tree Viewer

The interactive D3.js dependency tree viewer provides visual exploration of YAML configuration dependencies.

**Features:**
- 🔍 **Interactive Navigation** - Click nodes to expand/collapse
- 🎨 **Visual Indicators** - Color-coded nodes by type and status
- 📏 **Pan & Zoom** - Navigate large dependency trees
- 📄 **Content Viewer** - View YAML content with syntax highlighting
- 📊 **Metadata Panel** - Display file metadata and statistics
- 🎯 **APEX Keywords** - Highlighted domain-specific keywords

**Default Configuration:**

The viewer loads a demo dataset on startup:
- **Location**: `apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/`
- **Root File**: `00-scenario-registry.yaml`
- **Description**: Test dataset with 100+ YAML files and deep dependency chains

**Changing the Default Dataset:**

1. Edit `apex-yaml-manager/src/main/resources/static/js/d3-tree-viewer.js`
2. Locate line 49:
   ```javascript
   const rootFile = "C:/Users/markr/dev/java/corejava/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
   ```
3. Replace with the absolute path to your root YAML file
4. Restart the service

**⚠️ Important Notes:**
- File path must be absolute
- File must be accessible to the Spring Boot application
- All referenced files must be in the same directory or subdirectories

**Using the Viewer:**

1. **Load a Tree**: Enter root file path and click "Load Tree"
2. **Expand Nodes**: Click on collapsed nodes (▶) to expand
3. **Collapse Nodes**: Click on expanded nodes (▼) to collapse
4. **View Content**: Click on a node to view YAML content in right panel
5. **Pan**: Click and drag the background
6. **Zoom**: Use mouse wheel or zoom controls
7. **Reset View**: Click "Reset Zoom" button

---

## 📡 API Endpoints

All endpoints are prefixed with `/yaml-manager/api`

### Health & Status ✅ COMPLETE

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `GET` | `/health` | Service health check | ✅ |
| `GET` | `/health/status` | Detailed system status | ✅ |

**Example:**
```bash
curl http://localhost:8082/yaml-manager/api/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "apex-yaml-manager",
  "version": "1.0.0",
  "timestamp": "2025-11-06T20:15:35.123Z"
}
```

### Dependency Analysis ✅ COMPLETE

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/dependencies/analyze` | Analyze file dependencies | ✅ |
| `GET` | `/dependencies/tree` | Get dependency tree | ✅ |
| `GET` | `/dependencies/content` | Get file content & metadata | ✅ |

**Example - Analyze Dependencies:**
```bash
curl -X POST http://localhost:8082/yaml-manager/api/dependencies/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/your/config.yaml",
    "includeMetrics": true,
    "maxDepth": 10
  }'
```

**Example - Get Dependency Tree:**
```bash
curl "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=/path/to/root.yaml"
```

### Catalog Management ✅ COMPLETE

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `GET` | `/catalog/scan` | Scan directory and build catalog | ✅ |
| `GET` | `/catalog/metadata` | Get file metadata | ✅ |
| `GET` | `/catalog/summary` | Get catalog statistics | ✅ |

**Example - Scan Directory:**
```bash
curl "http://localhost:8082/yaml-manager/api/catalog/scan?directory=/path/to/configs"
```

**Example - Get Metadata:**
```bash
curl "http://localhost:8082/yaml-manager/api/catalog/metadata?filePath=/path/to/config.yaml"
```

### Health Checks ✅ COMPLETE

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/health-checks/check` | Perform health check | ✅ |
| `POST` | `/health-checks/report` | Generate health report | ✅ |
| `GET` | `/health-checks/score` | Get health score | ✅ |
| `GET` | `/health-checks/is-healthy` | Check if file is healthy | ✅ |

**Example - Health Check:**
```bash
curl -X POST http://localhost:8082/yaml-manager/api/health-checks/check \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/config.yaml",
    "checkTypes": ["STRUCTURE", "REFERENCES", "CONSISTENCY"]
  }'
```

**Example - Get Health Score:**
```bash
curl "http://localhost:8082/yaml-manager/api/health-checks/score?filePath=/path/to/config.yaml"
```

### Validation ✅ COMPLETE

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/validation/structure` | Validate YAML structure | ✅ |
| `POST` | `/validation/references` | Validate file references | ✅ |
| `POST` | `/validation/consistency` | Validate consistency rules | ✅ |
| `POST` | `/validation/all` | Perform all validations | ✅ |
| `GET` | `/validation/is-valid` | Check if file is valid | ✅ |

**Example - Validate Structure:**
```bash
curl -X POST http://localhost:8082/yaml-manager/api/validation/structure \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/config.yaml"
  }'
```

**Example - Validate All:**
```bash
curl -X POST http://localhost:8082/yaml-manager/api/validation/all \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/config.yaml",
    "strict": true
  }'
```

### Refactoring ⏳ PLANNED

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/refactoring/analyze` | Analyze refactoring options | ⏳ |
| `POST` | `/refactoring/simplify` | Simplify dependencies | ⏳ |
| `POST` | `/refactoring/consolidate` | Consolidate configurations | ⏳ |
| `POST` | `/refactoring/split` | Split large configurations | ⏳ |
| `POST` | `/refactoring/execute` | Execute refactoring | ⏳ |

## Architecture

### Model Classes
- `DependencyMetrics` - Metrics for dependency analysis
- `ImpactAnalysisResult` - Result of impact analysis
- `YamlConfigMetadata` - Metadata for a configuration
- `YamlCatalog` - Central catalog of configurations
- `HealthReport` - Comprehensive health report
- `HealthIssue` - Individual health issue

### Service Classes
- `CatalogService` - Catalog management ✅
- `DependencyAnalysisService` - Dependency analysis ✅
- `YamlContentAnalyzer` - YAML content extraction ✅
- `ValidationService` - Validation rules engine ✅
- `HealthCheckService` - Health checks ✅
- `RefactoringService` - Refactoring tools (planned)

### Controller Classes
- `HealthController` - Health and status endpoints ✅
- `CatalogController` - Catalog endpoints ✅
- `DependencyAnalysisController` - Dependency analysis endpoints ✅
- `HealthCheckController` - Health check endpoints ✅
- `ValidationController` - Validation endpoints ✅
- `RefactoringController` - Refactoring endpoints (planned)

## Configuration

Edit `src/main/resources/application.properties` to customize:

```properties
# Server port
server.port=8082

# Context path
server.servlet.context-path=/yaml-manager

# Logging level
logging.level.dev.mars.apex=DEBUG

# YAML Manager specific settings
yaml-manager.max-file-size=10485760
yaml-manager.scan-timeout-seconds=300
yaml-manager.cache-enabled=true
yaml-manager.cache-ttl-minutes=60
```

## Development

### Project Structure

```
apex-yaml-manager/
├── src/main/java/dev/mars/apex/yaml/manager/
│   ├── YamlManagerApplication.java
│   ├── controller/
│   │   ├── HealthController.java ✅
│   │   ├── CatalogController.java ✅
│   │   ├── DependencyAnalysisController.java ✅
│   │   ├── HealthCheckController.java ✅
│   │   ├── ValidationController.java ✅
│   │   └── RefactoringController.java (planned)
│   ├── service/
│   │   ├── CatalogService.java ✅
│   │   ├── DependencyAnalysisService.java ✅
│   │   ├── YamlContentAnalyzer.java ✅
│   │   ├── HealthCheckService.java ✅
│   │   ├── ValidationService.java ✅
│   │   └── RefactoringService.java (planned)
│   ├── model/
│   │   ├── DependencyMetrics.java ✅
│   │   ├── ImpactAnalysisResult.java ✅
│   │   ├── YamlConfigMetadata.java ✅
│   │   ├── YamlCatalog.java ✅
│   │   ├── YamlContentSummary.java ✅
│   │   ├── HealthReport.java ✅
│   │   ├── HealthScore.java ✅
│   │   ├── HealthIssue.java ✅
│   │   ├── ValidationResult.java ✅
│   │   └── ValidationIssue.java ✅
│   └── util/
│       └── YamlUtils.java ✅
├── src/main/resources/
│   ├── application.properties
│   └── static/
│       ├── d3-tree-viewer.html ✅
│       ├── css/
│       │   └── d3-tree-viewer.css ✅
│       └── js/
│           └── d3-tree-viewer.js ✅
└── pom.xml
```

### Running Tests

```bash
mvn test
```

## Integration with Other Modules

- **apex-core**: Uses YAML dependency analyzer and core utilities
- **apex-playground**: Can integrate YAML management features into playground UI
- **apex-rest-api**: Can expose YAML manager endpoints through REST API

## Implementation Roadmap

- ✅ Phase 1: Enhanced Dependency Analysis (COMPLETE)
- ✅ Phase 2: Catalog and Discovery System (COMPLETE)
- ✅ Phase 3: Validation and Health Checks (COMPLETE)
- ✅ Phase 4: UI and Visualization - Core Features (COMPLETE)
  - ✅ D3.js dependency tree viewer
  - ✅ Interactive node expansion/collapse
  - ✅ YAML content display with syntax highlighting
  - ✅ File metadata panel
  - ✅ Pan/zoom controls
  - Advanced catalog browser (planned)
  - Health dashboard (planned)
- Phase 5: Refactoring and Optimization Tools (NEXT)

## License

Apache License 2.0 - See LICENSE file for details

---

## 🔧 Troubleshooting

### Common Issues & Solutions

#### Issue 1: Service Won't Start - Port Already in Use

**Symptom:**
```
***************************
APPLICATION FAILED TO START
***************************

Description:
Web server failed to start. Port 8082 was already in use.
```

**Solution:**
```bash
# Option A: Find and kill the process using port 8082
# Windows:
netstat -ano | findstr :8082
taskkill /PID <process_id> /F

# Linux/Mac:
lsof -i :8082
kill -9 <process_id>

# Option B: Change the port in application.properties
# Edit: apex-yaml-manager/src/main/resources/application.properties
server.port=8083
```

#### Issue 2: Service Unavailable (500 Error)

**Symptom:**
```bash
curl http://localhost:8082/yaml-manager/api/health
# Returns: 500 Internal Server Error
```

**Solution:**
```bash
# 1. Check if service is actually running
curl http://localhost:8082/yaml-manager/actuator/health

# 2. Check logs for errors
tail -f apex-yaml-manager/logs/application.log

# 3. Rebuild and restart
mvn clean install -pl apex-yaml-manager
mvn spring-boot:run -pl apex-yaml-manager
```

#### Issue 3: Dependency Tree Not Loading

**Symptom:**
- Tree viewer shows "Loading..." indefinitely
- Console shows 404 or 500 errors

**Solution:**
```bash
# 1. Verify the root file path is correct and absolute
# 2. Check file permissions (file must be readable)
# 3. Verify all referenced files exist
# 4. Check browser console for JavaScript errors (F12)
# 5. Test the API directly:
curl "http://localhost:8082/yaml-manager/api/dependencies/tree?rootFile=/absolute/path/to/root.yaml"
```

#### Issue 4: CORS Errors in Browser

**Symptom:**
```
Access to XMLHttpRequest blocked by CORS policy
```

**Solution:**
- The service should be accessed via `http://localhost:8082/yaml-manager/`
- Do not open HTML files directly from filesystem (`file://`)
- If needed, configure CORS in `application.properties`:
  ```properties
  spring.web.cors.allowed-origins=http://localhost:3000
  spring.web.cors.allowed-methods=GET,POST,PUT,DELETE
  ```

#### Issue 5: Build Failures

**Symptom:**
```
[ERROR] Failed to execute goal on project apex-yaml-manager
```

**Solution:**
```bash
# 1. Clean and rebuild from root
cd apex-rules-engine
mvn clean install

# 2. If apex-core dependency issues:
mvn clean install -pl apex-core
mvn clean install -pl apex-yaml-manager

# 3. Clear Maven cache if needed
rm -rf ~/.m2/repository/com/apex
mvn clean install
```

#### Issue 6: Out of Memory Errors

**Symptom:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
```bash
# Increase heap size when running
export MAVEN_OPTS="-Xmx2g"
mvn spring-boot:run -pl apex-yaml-manager

# Or when running JAR:
java -Xmx2g -jar apex-yaml-manager/target/apex-yaml-manager-1.0-SNAPSHOT.jar
```

### Getting Help

**Check Logs:**
```bash
# Application logs
tail -f apex-yaml-manager/logs/application.log

# Maven output
mvn spring-boot:run -pl apex-yaml-manager -X  # Debug mode
```

**Verify Configuration:**
```bash
# Check application.properties
cat apex-yaml-manager/src/main/resources/application.properties

# Check active profile
curl http://localhost:8082/yaml-manager/actuator/env
```

**Test Endpoints:**
```bash
# Health check
curl http://localhost:8082/yaml-manager/api/health

# System status
curl http://localhost:8082/yaml-manager/api/health/status

# Actuator health
curl http://localhost:8082/yaml-manager/actuator/health
```

---

## 📞 Support

For issues, questions, or contributions:

- **Documentation**: Review this README and other docs in `apex-yaml-manager/docs/`
- **API Reference**: Access Swagger UI at `http://localhost:8082/yaml-manager/swagger-ui.html`
- **Examples**: Check test resources in `apex-yaml-manager/src/test/resources/`
- **Contact**: APEX Team at apexsupport@mars.dev

---

## 📄 License

Apache License 2.0 - See LICENSE file for details

---

**Last Updated**: 2025-11-06
**Version**: 1.0.0
**Status**: Production Ready ✅

