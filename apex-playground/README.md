# APEX Playground

Interactive web-based playground for APEX Rules Engine with 4-panel JSFiddle-style interface for processing source data files with YAML rules configurations.

## Overview

The APEX Playground provides an intuitive web interface for experimenting with and testing APEX rules engine capabilities. It features a 4-panel layout inspired by JSFiddle that allows users to:

- Input source data in various formats (JSON, XML, CSV)
- Define YAML rules configurations with real-time syntax validation
- Process data and see immediate validation results
- View enrichment results and performance metrics
- Save and load configurations for reuse

## Features

### 4-Panel Interface
- **Top-Left Panel**: Source Data Input (JSON/XML/CSV)
- **Top-Right Panel**: YAML Rules Configuration with syntax highlighting
- **Bottom-Left Panel**: Processing Results & Validation Output
- **Bottom-Right Panel**: Enrichment Results & Performance Metrics

### Key Capabilities
- ✅ Real-time YAML syntax validation
- ✅ Interactive data processing with immediate feedback
- ✅ Support for multiple data formats (JSON, XML, CSV)
- ✅ Rules validation and enrichment results display
- ✅ Performance metrics and execution timing
- ✅ Save/load configurations
- ✅ Example templates and presets
- ✅ Responsive web design
- ✅ REST API for programmatic access

## Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher
- APEX Core modules (automatically included)

### Running the Playground

1. **Build the project:**
   ```bash
   mvn clean package
   ```

2. **Run the application:**
   ```bash
   java -jar target/apex-playground-1.0-SNAPSHOT.jar
   ```
   
   Or with Maven:
   ```bash
   mvn spring-boot:run
   ```

3. **Access the playground:**
   - **Main Interface**: http://localhost:8081/playground
   - **API Documentation**: http://localhost:8081/swagger-ui.html
   - **Health Check**: http://localhost:8081/actuator/health

## Usage

### Basic Workflow

1. **Input Data**: Paste or upload your source data in the top-left panel
2. **Configure Rules**: Write or load YAML rules in the top-right panel
3. **Validate**: Click "Validate" to check YAML syntax (or see real-time validation)
4. **Process**: Click "Process" to execute rules against your data
5. **Review Results**: See validation results (bottom-left) and enrichment results (bottom-right)
6. **Iterate**: Modify rules or data and reprocess instantly

### Example Configuration

**Source Data (JSON):**
```json
{
  "name": "John Doe",
  "age": 30,
  "email": "john.doe@example.com",
  "amount": 1500.00,
  "currency": "USD"
}
```

**YAML Rules:**
```yaml
metadata:
  name: "Sample Validation Rules"
  version: "1.0.0"
  description: "Example validation rules for playground"

rules:
  - id: "age-check"
    name: "Age Validation"
    condition: "#age >= 18"
    message: "Age must be 18 or older"
    
  - id: "email-check"
    name: "Email Validation"
    condition: "#email != null && #email.contains('@')"
    message: "Valid email address required"
```

## API Endpoints

The playground provides REST API endpoints for programmatic access:

- `GET /playground/api/health` - Health check
- `POST /playground/api/process` - Process data with YAML rules
- `POST /playground/api/validate` - Validate YAML configuration
- `GET /playground/api/examples` - Get example templates

## Configuration

The playground can be configured via `application.yml`:

```yaml
apex:
  playground:
    max-file-size: 10485760      # 10MB
    max-rules-per-config: 100
    processing-timeout: 30000     # 30 seconds
    examples-enabled: true
    metrics-enabled: true
```

## Development Status

### Phase 1: ✅ Complete
- [x] Module setup and basic structure
- [x] Spring Boot application configuration
- [x] Basic web interface with 4-panel layout
- [x] REST API endpoints (placeholder implementations)
- [x] Project structure and build configuration

### Phase 2: 🚧 Coming Next
- [ ] Backend service implementations
- [ ] YAML validation integration with APEX core
- [ ] Data processing with rules engine
- [ ] Real-time validation feedback

### Phase 3: 📋 Planned
- [ ] Enhanced frontend with CodeMirror integration
- [ ] File upload/download functionality
- [ ] Example templates library
- [ ] Advanced error handling

### Phase 4: 📋 Planned
- [ ] Performance metrics and monitoring
- [ ] Save/load configurations
- [ ] Advanced features and optimizations

### Phase 5: 📋 Planned
- [ ] Comprehensive testing
- [ ] Documentation and tutorials
- [ ] Example scenarios

## Architecture

The playground is built as a Spring Boot web application that integrates with the existing APEX core modules:

```
apex-playground/
├── src/main/java/dev/mars/apex/playground/
│   ├── PlaygroundApplication.java          # Spring Boot main class
│   ├── controller/                         # Web and API controllers
│   ├── service/                           # Business logic services
│   ├── config/                            # Configuration classes
│   └── model/                             # Data models (Phase 2)
├── src/main/resources/
│   ├── static/                            # CSS, JS, images
│   ├── templates/                         # Thymeleaf templates
│   └── application.yml                    # Configuration
└── src/test/java/                         # Tests
```

## Contributing

This module is part of the APEX Rules Engine project. Please follow the existing code style and patterns established in the other APEX modules.

## License

Apache 2.0 License - see the main project LICENSE file for details.
