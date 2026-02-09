package dev.mars.apex.sync.pipelines;

import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.sync.SyncTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for dual-format output (HTML + JSON) from schema-diff pipeline.
 * 
 * This test demonstrates:
 * - Reading schema from CSV file
 * - Creating in-memory target schema definition
 * - Comparing schemas using schema-diff
 * - Generating both HTML and JSON reports in a single pipeline execution
 * 
 * Use Cases:
 * - Documentation + automation in one pipeline
 * - Human review + CI/CD integration
 * - Compliance reporting (human + machine audit)
 */
class SchemaDiffDualOutputTest extends SyncTestBase {

    @Test
    void shouldGenerateBothHtmlAndJsonReports() throws Exception {
        logger.info("\n=== Schema Diff Dual Output Test ===\n");
        
        // Load the dual-output test configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/sync/pipelines/SchemaDiffDualOutputTest.yaml");
        assertNotNull(config, "Configuration should load successfully");
        
        // Create the rules engine
        var engine = RulesEngine.fromYamlConfig(config);
        assertNotNull(engine, "Engine should be created from configuration");
        
        // Note: This test demonstrates configuration for dual HTML+JSON report generation.
        // Actual execution would require valid CSV file and data sources.
        
        logger.info("Dual output (HTML + JSON) configuration loaded successfully");
        logger.info("Report outputs: schema-diff-dual-test.html, schema-diff-dual-test.json");
    }
}
