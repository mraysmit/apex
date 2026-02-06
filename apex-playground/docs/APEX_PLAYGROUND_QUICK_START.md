# APEX Playground Quick Start

## Prerequisites
- Java 21+
- Maven 3.9+

## Start the Playground

```bash
cd apex-playground
mvn spring-boot:run
```

Wait for the log message: `APEX Playground is ready!`

## Access the UI

| URL | Description |
|-----|-------------|
| http://localhost:8081/playground | 4-panel rules playground |
| http://localhost:8081/playground/apex_editor_main.html | Visual rule editor (Blockly) |
| http://localhost:8081/swagger-ui.html | API documentation |
| http://localhost:8081/actuator/health | Health check |

## Using the 4-Panel Playground

1. **Top-Left** — Paste or type your YAML rules configuration
2. **Top-Right** — Enter source data (JSON, XML, or CSV) and select the format
3. Click **Process** to evaluate rules against your data
4. **Bottom-Left** — View validation results (rules passed/failed)
5. **Bottom-Right** — View enrichment results and execution trace

**Tip:** Click **Load Example** to browse 18 categories of pre-built configurations (basic, enrichment, lookup, scenario, etc.)

## No External Services Required

The playground runs standalone — no databases or other services are needed to start. If your YAML rules reference external data sources, connect to them at runtime via the **Data Sources** panel in the UI (supports PostgreSQL, MySQL, Oracle, SQL Server, H2).

## Key API Endpoints

```
POST /playground/api/process    — Evaluate data against YAML rules
POST /playground/api/validate   — Validate YAML syntax and structure
GET  /playground/api/examples   — List available example configurations
```

## Stop

Press `Ctrl+C` in the terminal running Maven.
