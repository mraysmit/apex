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

### Layer 5: UI and Visualization
- Interactive dependency graph visualization
- Catalog browser with search
- Health dashboard with real-time metrics
- Impact analysis visualization
- REST API for programmatic access

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

**Health Check:**
- `http://localhost:8082/yaml-manager/api/health`

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

### Health
- `GET /api/health` - Health check
- `GET /api/health/status` - System status

### Dependency Analysis (planned)
- `POST /api/dependencies/analyze` - Analyze dependencies
- `GET /api/dependencies/{file}/impact` - Impact analysis
- `GET /api/dependencies/metrics` - Dependency metrics

### Catalog (Coming Soon)
- `GET /api/catalog/configurations` - List all configurations
- `GET /api/catalog/search` - Search configurations
- `GET /api/catalog/unused` - Find unused configurations
- `GET /api/catalog/critical` - Find critical configurations

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

### Refactoring (planned)
- `POST /api/refactoring/analyze` - Analyze refactoring options
- `POST /api/refactoring/execute` - Execute refactoring

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
│   │   ├── HealthController.java
│   │   ├── CatalogController.java (coming soon)
│   │   ├── DependencyAnalysisController.java (coming soon)
│   │   ├── HealthCheckController.java (coming soon)
│   │   └── RefactoringController.java (coming soon)
│   ├── service/
│   │   ├── CatalogService.java
│   │   ├── DependencyAnalysisService.java (coming soon)
│   │   ├── HealthCheckService.java (coming soon)
│   │   └── RefactoringService.java (coming soon)
│   ├── model/
│   │   ├── DependencyMetrics.java
│   │   ├── ImpactAnalysisResult.java
│   │   ├── YamlConfigMetadata.java
│   │   ├── YamlCatalog.java
│   │   ├── HealthReport.java
│   │   └── HealthIssue.java
│   └── util/ (coming soon)
├── src/main/resources/
│   └── application.properties
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
- Phase 4: UI and Visualization (NEXT - Phases Reordered)
- Phase 5: Refactoring and Optimization Tools (Planned)

**Note**: Phases 4 and 5 have been reordered. UI development is prioritized as the architecture is solid and all features are fully exposed via REST API (40+ endpoints). See ARCHITECTURE_ASSESSMENT.md for details.

## License

Apache License 2.0 - See LICENSE file for details

## Support

For issues, questions, or contributions, please contact the APEX team.

