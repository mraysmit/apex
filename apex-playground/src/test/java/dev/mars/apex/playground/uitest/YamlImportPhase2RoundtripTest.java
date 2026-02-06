package dev.mars.apex.playground.uitest;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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
 */

import dev.mars.apex.playground.ui.BaseYamlImportSeleniumTest;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2 Roundtrip Integrity Tests.
 *
 * <p>Validates the import of 4 previously-unsupported sections/patterns:</p>
 * <ol>
 *   <li><b>Error recovery</b> — {@code error-recovery:} section with severity policies</li>
 *   <li><b>Categories</b> — {@code categories:} section with category blocks</li>
 *   <li><b>Data sinks</b> — {@code data-sinks:} section with all 4 sink types
 *       (database, file, REST, queue)</li>
 *   <li><b>Query-ref</b> — {@code query-ref:} fallback in lookup datasets when
 *       {@code query:} is absent</li>
 * </ol>
 *
 * @author APEX Test Suite
 * @since 2025-12-19
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class YamlImportPhase2RoundtripTest extends BaseYamlImportSeleniumTest {

    // ========================================================================
    // ERROR RECOVERY SECTION (Gap #5)
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("Phase2-RT-1: Error recovery section creates blocks")
    void testErrorRecoveryImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/error-recovery-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        verifyBlockExists("apex_error_recovery", 1, "Should have 1 Error Recovery block");
    }

    @Test
    @Order(2)
    @DisplayName("Phase2-RT-2: Error recovery severity policies are imported")
    void testErrorRecoverySeverityPolicies() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/error-recovery-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // 4 severity policies: CRITICAL, ERROR, WARNING, INFO
        verifyBlockExists("apex_severity_policy", 4,
                "Should have 4 severity policy blocks (CRITICAL, ERROR, WARNING, INFO)");
    }

    @Test
    @Order(3)
    @DisplayName("Phase2-RT-3: Error recovery field values are correct")
    void testErrorRecoveryFieldValues() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/error-recovery-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String enabled = getBlockFieldValue("apex_error_recovery", "ENABLED");
        assertEquals("TRUE", enabled, "Error recovery should be enabled");

        String defaultStrategy = getBlockFieldValue("apex_error_recovery", "DEFAULT_STRATEGY");
        assertEquals("CONTINUE_WITH_DEFAULT", defaultStrategy,
                "Default strategy should be CONTINUE_WITH_DEFAULT");

        String logRecovery = getBlockFieldValue("apex_error_recovery", "LOG_RECOVERY");
        assertEquals("TRUE", logRecovery, "Log recovery should be true");
    }

    @Test
    @Order(4)
    @DisplayName("Phase2-RT-4: Error recovery survives roundtrip")
    void testErrorRecoveryRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/error-recovery-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exported.contains("error-recovery:"),
                "Exported YAML must contain 'error-recovery:' section");
        assertTrue(exported.contains("severity-policies:"),
                "Exported YAML must contain 'severity-policies:'");
        assertTrue(exported.contains("FAIL_FAST") || exported.contains("fail-fast"),
                "Exported YAML must contain FAIL_FAST strategy");
        assertTrue(exported.contains("CONTINUE_WITH_DEFAULT") || exported.contains("continue-with-default"),
                "Exported YAML must contain CONTINUE_WITH_DEFAULT strategy");
    }

    // ========================================================================
    // CATEGORIES SECTION (Gap #6)
    // ========================================================================

    @Test
    @Order(5)
    @DisplayName("Phase2-RT-5: Categories section creates blocks")
    void testCategoriesImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/categories-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        verifyBlockExists("apex_section_categories", 1, "Should have 1 Categories section block");
        verifyBlockExists("apex_category", 3, "Should have 3 category blocks");
    }

    @Test
    @Order(6)
    @DisplayName("Phase2-RT-6: Category field values are correct")
    void testCategoryFieldValues() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/categories-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // Verify first category block has correct values
        String name = getBlockFieldValue("apex_category", "NAME");
        assertEquals("validation", name, "First category name should be 'validation'");

        String description = getBlockFieldValue("apex_category", "DESCRIPTION");
        assertEquals("Data validation checks", description,
                "First category description should match");

        String priority = getBlockFieldValue("apex_category", "PRIORITY");
        assertEquals("10", priority, "First category priority should be '10'");
    }

    @Test
    @Order(7)
    @DisplayName("Phase2-RT-7: Categories survive roundtrip")
    void testCategoriesRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/categories-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exported.contains("categories:"),
                "Exported YAML must contain 'categories:' section");
        assertTrue(exported.contains("validation"),
                "Exported YAML must contain category name 'validation'");
        assertTrue(exported.contains("compliance"),
                "Exported YAML must contain category name 'compliance'");
    }

    // ========================================================================
    // DATA SINKS SECTION (Gap #7)
    // ========================================================================

    @Test
    @Order(8)
    @DisplayName("Phase2-RT-8: Data sinks section creates blocks for all 4 types")
    void testDataSinksImportCreatesBlocks() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/data-sinks-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        verifyBlockExists("apex_section_data_sinks", 1, "Should have 1 Data Sinks section block");
        verifyBlockExists("apex_data_sink_database", 1, "Should have 1 database sink block");
        verifyBlockExists("apex_data_sink_file", 1, "Should have 1 file sink block");
        verifyBlockExists("apex_data_sink_rest", 1, "Should have 1 REST sink block");
        verifyBlockExists("apex_data_sink_queue", 1, "Should have 1 queue sink block");
    }

    @Test
    @Order(9)
    @DisplayName("Phase2-RT-9: Database sink field values are correct")
    void testDatabaseSinkFieldValues() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/data-sinks-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String name = getBlockFieldValue("apex_data_sink_database", "NAME");
        assertEquals("db-output", name, "Database sink name should be 'db-output'");

        String tableName = getBlockFieldValue("apex_data_sink_database", "TABLE_NAME");
        assertEquals("processed_results", tableName, "Table name should be 'processed_results'");

        String operation = getBlockFieldValue("apex_data_sink_database", "OPERATION");
        assertEquals("INSERT", operation, "Operation should be 'INSERT'");
    }

    @Test
    @Order(10)
    @DisplayName("Phase2-RT-10: File sink field values are correct")
    void testFileSinkFieldValues() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/data-sinks-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String name = getBlockFieldValue("apex_data_sink_file", "NAME");
        assertEquals("file-output", name, "File sink name should be 'file-output'");

        String basePath = getBlockFieldValue("apex_data_sink_file", "BASE_PATH");
        assertEquals("./output/results", basePath, "Base path should match");

        String format = getBlockFieldValue("apex_data_sink_file", "FORMAT");
        assertEquals("json", format, "Format should be 'json'");
    }

    @Test
    @Order(11)
    @DisplayName("Phase2-RT-11: REST sink field values are correct")
    void testRestSinkFieldValues() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/data-sinks-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String name = getBlockFieldValue("apex_data_sink_rest", "NAME");
        assertEquals("api-output", name, "REST sink name should be 'api-output'");

        String baseUrl = getBlockFieldValue("apex_data_sink_rest", "BASE_URL");
        assertEquals("https://api.example.com", baseUrl, "Base URL should match");

        String method = getBlockFieldValue("apex_data_sink_rest", "METHOD");
        assertEquals("POST", method, "Method should be 'POST'");
    }

    @Test
    @Order(12)
    @DisplayName("Phase2-RT-12: Queue sink field values are correct")
    void testQueueSinkFieldValues() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/data-sinks-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String name = getBlockFieldValue("apex_data_sink_queue", "NAME");
        assertEquals("queue-output", name, "Queue sink name should be 'queue-output'");

        String queueType = getBlockFieldValue("apex_data_sink_queue", "QUEUE_TYPE");
        assertEquals("kafka", queueType, "Queue type should be 'kafka'");

        String queueName = getBlockFieldValue("apex_data_sink_queue", "QUEUE_NAME");
        assertEquals("processed-events", queueName, "Queue name should be 'processed-events'");
    }

    @Test
    @Order(13)
    @DisplayName("Phase2-RT-13: Data sinks survive roundtrip")
    void testDataSinksRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/data-sinks-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");
        assertTrue(exported.contains("data-sinks:"),
                "Exported YAML must contain 'data-sinks:' section");
    }

    // ========================================================================
    // QUERY-REF PATTERN (Gap #13)
    // ========================================================================

    @Test
    @Order(14)
    @DisplayName("Phase2-RT-14: Query-ref imported into QUERY field on database dataset")
    void testQueryRefImported() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/query-ref-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        // The dataset block should exist with the query-ref value in the QUERY field
        String queryValue = getBlockFieldValue("apex_lookup_dataset_database", "QUERY");
        assertEquals("getActiveCustomer", queryValue,
                "QUERY field should contain the query-ref value 'getActiveCustomer' as fallback");
    }

    @Test
    @Order(15)
    @DisplayName("Phase2-RT-15: Query-ref import also preserves data-source-ref")
    void testQueryRefDataSourceRef() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/query-ref-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        String dsRef = getBlockFieldValue("apex_lookup_dataset_database", "DS_REF");
        assertEquals("customer-database", dsRef,
                "DS_REF field should contain 'customer-database'");
    }

    // ========================================================================
    // COMBINED PHASE 2 (All Fixes Together)
    // ========================================================================

    @Test
    @Order(16)
    @DisplayName("Phase2-RT-16: Combined Phase 2 import creates all expected blocks")
    void testPhase2CombinedImport() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/phase2-combined-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
        verifyBlockExists("apex_error_recovery", 1, "Should have 1 Error Recovery block");
        verifyBlockExists("apex_severity_policy", 2, "Should have 2 severity policy blocks");
        verifyBlockExists("apex_section_categories", 1, "Should have 1 Categories section block");
        verifyBlockExists("apex_category", 1, "Should have 1 category block");
        verifyBlockExists("apex_section_data_sinks", 1, "Should have 1 Data Sinks section block");
        verifyBlockExists("apex_data_sink_database", 1, "Should have 1 database sink block");
    }

    @Test
    @Order(17)
    @DisplayName("Phase2-RT-17: Combined Phase 2 roundtrip preserves all sections")
    void testPhase2CombinedRoundtrip() throws IOException {
        String yaml = loadYamlFile("examples/roundtrip/phase2-combined-roundtrip-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);
        waitForBlocksToRender();
        String exported = exportYamlContent();

        assertNotNull(exported, "Exported YAML should not be null");
        assertFalse(exported.isEmpty(), "Exported YAML should not be empty");

        // All Phase 2 sections survive roundtrip
        assertTrue(exported.contains("error-recovery:"),
                "Combined: error-recovery must survive roundtrip");
        assertTrue(exported.contains("categories:"),
                "Combined: categories must survive roundtrip");
        assertTrue(exported.contains("data-sinks:"),
                "Combined: data-sinks must survive roundtrip");

        // Phase 1 sections still work
        assertTrue(exported.contains("rules:"),
                "Combined: rules must survive roundtrip");
        assertTrue(exported.contains("enrichments:"),
                "Combined: enrichments must survive roundtrip");

        // Phase 1 metadata still works
        assertTrue(exported.contains("created-by:"),
                "Combined: created-by metadata must survive roundtrip");
    }

    // ========================================================================
    // REGRESSION — Existing error-recovery example still works
    // ========================================================================

    @Test
    @Order(18)
    @DisplayName("Phase2-RT-18: Existing error-recovery-test.yaml imports correctly")
    void testExistingErrorRecoveryExample() throws IOException {
        String yaml = loadYamlFile("examples/validation/error-recovery-test.yaml");
        driver.get(baseUrl + "/playground/apex_editor_main.html");
        waitForBlocklyWorkspaceToLoad();

        importYamlContent(yaml);

        int blockCount = getBlockCount();
        assertTrue(blockCount >= 2,
                "Existing error-recovery example should import blocks, found: " + blockCount);
        verifyBlockExists("apex_rule_config", 1, "Should have 1 Rule Configuration block");
    }
}
