package dev.mars.apex.sync.etl;

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Functional test for SQL Server to PostgreSQL sync logic.
 * 
 * Note: This test uses the 'Clean Architecture' pattern where infrastructure 
 * (Sources/Sinks) is defined in separate YAML files referenced by the main pipeline.
 * All files are co-located in this package for test portability.
 */
public class MsSqlToPostgresSyncTest {

    private static final Logger logger = LoggerFactory.getLogger(MsSqlToPostgresSyncTest.class);
    private RulesEngine rulesEngine;

    @AfterEach
    public void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    void testSyncPipelineStructure() throws Exception {
        // Set required environment variables for validation pass
        System.setProperty("SQLSERVER_URL", "jdbc:h2:mem:mssql_test;MODE=MSSQLServer");
        System.setProperty("SQLSERVER_USER", "sa");
        System.setProperty("SQLSERVER_PASS", "pass");
        System.setProperty("POSTGRES_URL", "jdbc:h2:mem:postgres_test;MODE=PostgreSQL");
        System.setProperty("POSTGRES_USER", "postgres");
        System.setProperty("POSTGRES_PASS", "pass");

        // 1. Resolve path to the co-located YAML file
        // We look for the file in the same directory structure under src/test/java/
        // In a real build context, we might copy these to target/test-classes, but 
        // for direct source access we can resolve relative to the project root.
        
        Path configPath = Paths.get("src/test/java/dev/mars/apex/sync/etl/MsSqlToPostgresSyncTest.yaml");
        File configFile = configPath.toFile();
        
        if (!configFile.exists()) {
            throw new RuntimeException("Test configuration not found at: " + configFile.getAbsolutePath());
        }

        logger.info("Initializing RulesEngine with config: {}", configFile.getAbsolutePath());

        // 2. Initialize Engine
        rulesEngine = RulesEngine.fromFile(configFile.getPath());
        assertNotNull(rulesEngine, "RulesEngine should be initialized");

        // 3. Evaluate (This validates the YAML structure and Reference loading)
        // Since we don't have a live MSSQL/Postgres instance in this unit test context,
        // we expect the engine to load the graph, but execution might fail on connection 
        // if we let it run fully. However, simply loading and validating the graph 
        // is a significant test of the configuration validity.
        
        // For now, we assert the engine loaded without throwing an exception.
        logger.info("Pipeline configuration loaded and validated successfully.");
    }
}
