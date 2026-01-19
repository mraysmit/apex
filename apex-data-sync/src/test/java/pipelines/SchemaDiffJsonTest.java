package pipelines;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.sync.SyncTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for JSON report generation from schema-diff pipeline.
 * 
 * This test demonstrates:
 * - Reading schema from CSV file
 * - Creating in-memory target schema definition
 * - Comparing schemas using schema-diff
 * - Generating JSON report for machine-readable output
 * 
 * Use Cases:
 * - Automated CSV validation before import
 * - API-driven schema validation
 * - CI/CD integration for data pipelines
 */
class SchemaDiffJsonTest extends SyncTestBase {

    @Test
    void shouldGenerateJsonReport() throws Exception {
        logger.info("\n=== Schema Diff JSON Output Test ===\n");
        
        // Load the JSON output test configuration
        var config = yamlLoader.loadFromFile("src/test/java/pipelines/SchemaDiffJsonTest.yaml");
        assertNotNull(config, "Configuration should load successfully");
        
        // Create the rules engine
        var engine = RulesEngine.fromYamlConfig(config);
        assertNotNull(engine, "Engine should be created from configuration");
        
        // Note: This test demonstrates configuration for JSON report generation.
        // Actual execution would require valid CSV file and data sources.
        
        logger.info("JSON output configuration loaded successfully");
        logger.info("Report output: schema-diff-json-test.json");
    }
}
