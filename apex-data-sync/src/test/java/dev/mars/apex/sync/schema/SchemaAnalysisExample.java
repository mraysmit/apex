package dev.mars.apex.sync.schema;

import dev.mars.apex.core.engine.core.RulesEngine;
import dev.mars.apex.sync.SyncTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example demonstrating database schema analysis with automatic report generation.
 * 
 * This test shows how to:
 * - Analyze single table schemas with HTML report generation
 * - Enumerate all tables in a database with schema filtering
 * - Configure custom report output paths
 * - Use external data-source references
 */
class SchemaAnalysisExample extends SyncTestBase {

    @Test
    void shouldDemonstrateSchemaAnalysisCapabilities() throws Exception {
        logger.info("\n=== Schema Analysis Example ===\n");
        
        // Load the example configuration
        var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/sync/schema/SchemaAnalysisExample.yaml");
        assertNotNull(config, "Configuration should load successfully");
        
        // Create the rules engine
        var engine = RulesEngine.fromYamlConfig(config);
        
        // Note: This example demonstrates configuration only.
        // Actual execution would require a valid database connection.
        
        logger.info("Schema analysis example configuration loaded successfully");
    }
}
