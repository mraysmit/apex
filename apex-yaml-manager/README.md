# APEX YAML Manager

Enterprise YAML configuration management system for APEX Rules Engine.

## Overview

The APEX YAML Manager provides comprehensive tools for managing the complexity of APEX YAML configurations at enterprise scale. It enables developers, architects, and operations teams to understand, analyze, and optimize their configuration ecosystems.

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

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- APEX Core module

### Building

```bash
mvn clean install
```

### Running

Start the server:
```bash
mvn spring-boot:run -pl apex-yaml-manager
```

Stop the server:
- Press `Ctrl+C` in the terminal where the server is running

### URLs

Once the server is running, access the following URLs:

**Web UI:**
- Dependency Tree Visualization: `http://localhost:8082/yaml-manager/d3-tree-viewer.html`

**API Documentation:**
- Swagger UI: `http://localhost:8082/yaml-manager/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/yaml-manager/api-docs`

**Health Endpoints:**
- System Health: `http://localhost:8082/yaml-manager/api/health`
- System Status: `http://localhost:8082/yaml-manager/api/health/status`

### Dependency Tree Viewer Configuration

The D3 tree viewer is configured to load a default demo dataset on startup:

**Default Dataset:**
- **Location**: `apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/`
- **Root File**: `00-scenario-registry.yaml`
- **Description**: Test dataset with 100+ YAML files and deep dependency chains

**To Change the Default Dataset:**

Edit `apex-yaml-manager/src/main/resources/static/js/d3-tree-viewer.js` (line 49):

```javascript
const rootFile = "C:/Users/markr/dev/java/corejava/apex-rules-engine/apex-yaml-manager/src/test/resources/apex-yaml-samples/graph-100/00-scenario-registry.yaml";
```

Replace with the absolute path to your desired root YAML file. The tree viewer will automatically load all dependencies from that root file.

**Note**: The file path must be absolute and accessible to the Spring Boot application.

## API Endpoints

### Health ✅ COMPLETE
- `GET /api/health` - Health check (returns status, service name, version, timestamp)
- `GET /api/health/status` - System status (returns detailed status with capabilities)

### Dependency Analysis ✅ COMPLETE
- `POST /api/dependencies/analyze` - Analyze dependencies for a YAML file
- `GET /api/dependencies/tree` - Get dependency tree structure
- `GET /api/dependencies/content` - Get file content and metadata

### Catalog ✅ COMPLETE
- `GET /api/catalog/scan` - Scan directory and build catalog
- `GET /api/catalog/metadata` - Get metadata for specific file
- `GET /api/catalog/summary` - Get catalog summary statistics

### Health Checks ✅ COMPLETE
- `POST /api/health-checks/check` - Perform health check
- `POST /api/health-checks/report` - Generate health report
- `GET /api/health-checks/score` - Get health score
- `GET /api/health-checks/is-healthy` - Check if file is healthy

### Validation ✅ COMPLETE
- `POST /api/validation/structure` - Validate YAML structure
- `POST /api/validation/references` - Validate file references
- `POST /api/validation/consistency` - Validate consistency rules
- `POST /api/validation/all` - Perform all validations
- `GET /api/validation/is-valid` - Check if file is valid

### Refactoring (Planned)
- `POST /api/refactoring/analyze` - Analyze refactoring options
- `POST /api/refactoring/simplify` - Simplify dependencies
- `POST /api/refactoring/consolidate` - Consolidate configurations
- `POST /api/refactoring/split` - Split large configurations
- `POST /api/refactoring/execute` - Execute refactoring with validation

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

## Support

For issues, questions, or contributions, please contact the APEX team.

