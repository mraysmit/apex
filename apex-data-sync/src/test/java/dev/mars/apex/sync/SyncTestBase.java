/*
 * Copyright 2026 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created: 2026-01-14
 */

package dev.mars.apex.sync;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.service.data.external.database.JdbcTemplateFactory;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.engine.ExpressionEvaluatorService;
import dev.mars.apex.core.service.lookup.LookupServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Base test class for all apex-data-sync tests.
 * Provides common APEX service setup, test isolation, and validation utilities.
 * 
 * <p>This class ensures consistent test patterns across the apex-data-sync module
 * and provides automatic cleanup of resources (cache, database connections, H2 files)
 * to guarantee test isolation.</p>
 * 
 * <p><strong>Standard Usage:</strong></p>
 * <pre>{@code
 * @ExtendWith(ColoredTestOutputExtension.class)
 * public class MyDataSyncTest extends SyncTestBase {
 *     
 *     @Test
 *     void testDataSync() {
 *         // APEX services (yamlLoader, serviceRegistry, etc.) are already initialized
 *         RulesEngine engine = RulesEngine.fromFile("config.yaml");
 *         RuleResult result = engine.evaluate(new HashMap<>());
 *         // Cleanup happens automatically in @AfterEach
 *     }
 * }
 * }</pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@ExtendWith(ColoredTestOutputExtension.class)
public abstract class SyncTestBase {

    protected static final Logger logger = LoggerFactory.getLogger(SyncTestBase.class);

    // Real APEX services for testing
    protected YamlConfigurationLoader yamlLoader;
    protected LookupServiceRegistry serviceRegistry;
    protected ExpressionEvaluatorService expressionEvaluator;
    protected RulesEngineConfiguration rulesEngineConfiguration;

    /**
     * Initialize APEX services before each test.
     * Ensures clean state by clearing caches and resetting statistics.
     */
    @BeforeEach
    public void setUp() {
        logger.info("Setting up APEX services for apex-data-sync testing...");

        // Clear cache and reset statistics to ensure test isolation
        // This prevents cache state from previous tests from affecting current test
        ApexCacheManager cacheManager = ApexCacheManager.getInstance();
        cacheManager.clearAll();

        // Reset statistics for all cache scopes to ensure clean state
        cacheManager.getAllStatistics().values().forEach(stats -> stats.reset());
        logger.info("Cache cleared and statistics reset for test isolation");

        // Clear DataSourceFactory cache to ensure fresh data source connections
        try {
            DataSourceFactory.getInstance().clearCache();
            logger.info("DataSourceFactory cache cleared for test isolation");
        } catch (Exception e) {
            logger.warn("Error clearing DataSourceFactory cache", e);
        }

        // Clear JdbcTemplateFactory cache to ensure fresh JDBC connections
        try {
            JdbcTemplateFactory.clearCache();
            logger.info("JdbcTemplateFactory cache cleared for test isolation");
        } catch (Exception e) {
            logger.warn("Error clearing JdbcTemplateFactory cache", e);
        }

        // Initialize real APEX services
        this.yamlLoader = new YamlConfigurationLoader();
        this.serviceRegistry = new LookupServiceRegistry();
        this.expressionEvaluator = new ExpressionEvaluatorService();
        this.rulesEngineConfiguration = new RulesEngineConfiguration();

        logger.info("✓ APEX services initialized successfully");
    }

    /**
     * Clean up resources after each test.
     * Ensures test isolation by clearing JDBC connections, shutting down H2,
     * and removing database files.
     */
    @AfterEach
    public void tearDown() {
        logger.info("Cleaning up APEX services after test...");

        // Clear JDBC DataSource cache to ensure database connections are properly closed
        try {
            JdbcTemplateFactory.clearCache();
            logger.info("JDBC DataSource cache cleared for test isolation");
        } catch (Exception e) {
            logger.warn("Error clearing JDBC DataSource cache", e);
        }

        // Clear DataSourceFactory cache to ensure fresh data source connections
        try {
            DataSourceFactory.getInstance().clearCache();
            logger.info("DataSourceFactory cache cleared for test isolation");
        } catch (Exception e) {
            logger.warn("Error clearing DataSourceFactory cache", e);
        }

        // Shutdown H2 database to release locks and close connections
        try {
            // Execute H2 SHUTDOWN command to properly close the database
            java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:h2:./target/h2-demo/schema_test;DB_CLOSE_DELAY=-1", "sa", "");
            java.sql.Statement stmt = conn.createStatement();
            stmt.execute("SHUTDOWN");
            stmt.close();
            conn.close();
            logger.info("H2 database shutdown completed for test isolation");
        } catch (Exception e) {
            logger.debug("H2 database shutdown (expected if not connected): {}", e.getMessage());
        }

        // Clean up H2 database files to prevent persistence between tests
        cleanupH2DatabaseFiles();

        // Reset the cache manager singleton to ensure complete isolation between tests
        // This is more thorough than just clearing cache entries and statistics
        // Note: resetInstance() internally calls shutdown() before nullifying the instance
        ApexCacheManager.resetInstance();
        logger.info("Cache manager singleton reset for test isolation");

        logger.info("✓ APEX services cleanup completed");
    }

    /**
     * Clean up H2 database files from various test databases.
     */
    private void cleanupH2DatabaseFiles() {
        try {
            // Clean up common H2 database file patterns
            String[] dbPaths = {
                "./target/h2-demo/schema_test",
                "./target/h2-demo/multi_table_test",
                "./target/h2-demo/mssql_test",
                "./target/h2-demo/postgres_test"
            };

            for (String dbPath : dbPaths) {
                File dbFile = new File(dbPath + ".mv.db");
                File dbTraceFile = new File(dbPath + ".trace.db");
                
                if (dbFile.exists() && dbFile.delete()) {
                    logger.debug("Cleaned up H2 database file: {}", dbFile.getName());
                }
                if (dbTraceFile.exists() && dbTraceFile.delete()) {
                    logger.debug("Cleaned up H2 trace file: {}", dbTraceFile.getName());
                }
            }
        } catch (Exception e) {
            logger.warn("Error cleaning up H2 database files", e);
        }
    }

    /**
     * Validate that a pipeline executed all expected steps.
     * 
     * @param actualSteps Number of steps executed
     * @param expectedSteps Expected number of steps
     * @param context Description of what's being validated
     */
    protected void validateExecutionRate(int actualSteps, int expectedSteps, String context) {
        if (actualSteps != expectedSteps) {
            logger.error("EXECUTION RATE FAILURE: {} - Expected {} steps, but executed {} steps",
                context, expectedSteps, actualSteps);
            throw new AssertionError(String.format(
                "Execution rate failure for %s: Expected %d steps, executed %d steps (%.1f%%)",
                context, expectedSteps, actualSteps, (actualSteps * 100.0 / expectedSteps)));
        }
        logger.info("✓ 100%% execution rate verified: {} - Processed {} out of {} steps",
            context, actualSteps, expectedSteps);
    }
}
