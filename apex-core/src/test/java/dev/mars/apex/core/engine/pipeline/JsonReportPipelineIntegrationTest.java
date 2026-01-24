package dev.mars.apex.core.engine.pipeline;

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
 */

import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.service.schema.diff.json.SchemaDiffJsonSerializer;
import dev.mars.apex.core.service.schema.diff.json.model.SchemaDiffReport;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JSON/HTML/Markdown report generation through the APEX pipeline.
 * Tests the complete flow: YAML → RulesEngine → Pipeline → SchemaComparisonResult → JSON → HTML/Markdown
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("JSON Report Pipeline Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JsonReportPipelineIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(JsonReportPipelineIntegrationTest.class);
    private static final String TEST_YAML_BASE_PATH = "src/test/java/dev/mars/apex/core/engine/pipeline/";
    
    private RulesEngine rulesEngine;

    @AfterEach
    void tearDown() {
        if (rulesEngine != null) {
            rulesEngine.shutdown();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should generate JSON report from schema-diff pipeline")
    void shouldGenerateJsonReportFromPipeline() throws Exception {
        logger.info("\n=== Test: Generate JSON Report via Pipeline ===\n");

        // Load pipeline configuration with json-report-output parameter
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/JsonReportPipelineIntegrationTest_JsonOutput.yaml");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify pipeline success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully: " + result.getMessage());
        logger.info("[OK] Pipeline executed successfully");

        // Verify JSON file was created
        Path jsonPath = Paths.get("apex-data-sync/reports/schema-diff-json-test.json");
        assertTrue(Files.exists(jsonPath), "JSON report file should exist");
        logger.info("[OK] JSON report file created: {}", jsonPath.toAbsolutePath());

        // Verify JSON content is valid
        SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
        SchemaDiffReport report = serializer.fromJsonFile(jsonPath.toString());
        
        assertNotNull(report, "Should deserialize JSON report");
        assertNotNull(report.getMetadata(), "Report should have metadata");
        assertNotNull(report.getSource(), "Report should have source info");
        assertNotNull(report.getTarget(), "Report should have target info");
        assertNotNull(report.getSummary(), "Report should have summary");
        assertNotNull(report.getColumns(), "Report should have columns");
        
        logger.info("[OK] JSON report structure validated");
        logger.info("  → Source: {} ({})", report.getSource().getName(), report.getSource().getType());
        logger.info("  → Target: {} ({})", report.getTarget().getName(), report.getTarget().getType());
        logger.info("  → Stats: {} matching, {} added",
            report.getColumns().getMatching() != null ? report.getColumns().getMatching().size() : 0,
            report.getColumns().getAdded() != null ? report.getColumns().getAdded().size() : 0);
    }

    @Test
    @Order(2)
    @DisplayName("Should generate both HTML and JSON reports from pipeline")
    void shouldGenerateBothReportsFromPipeline() throws Exception {
        logger.info("\n=== Test: Generate Both HTML and JSON Reports ===\n");

        // Load pipeline with both report-output and json-report-output
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/JsonReportPipelineIntegrationTest_DualOutput.yaml");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        // Verify pipeline success
        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        logger.info("[OK] Pipeline executed successfully");

        // Verify JSON report
        Path jsonPath = Paths.get("apex-data-sync/reports/schema-diff-dual-test.json");
        assertTrue(Files.exists(jsonPath), "JSON report should exist");
        logger.info("[OK] JSON report created: {} ({} bytes)", 
            jsonPath.toAbsolutePath(), Files.size(jsonPath));

        // Verify HTML report
        Path htmlPath = Paths.get("apex-data-sync/reports/schema-diff-dual-test.html");
        assertTrue(Files.exists(htmlPath), "HTML report should exist");
        logger.info("[OK] HTML report created: {} ({} bytes)", 
            htmlPath.toAbsolutePath(), Files.size(htmlPath));

        // Validate JSON content
        SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
        SchemaDiffReport report = serializer.fromJsonFile(jsonPath.toString());
        
        assertNotNull(report.getMetadata().getReportVersion(), "Should have report version");
        assertNotNull(report.getMetadata().getApexVersion(), "Should have APEX version");
        logger.info("[OK] JSON report version: {}, APEX version: {}", 
            report.getMetadata().getReportVersion(), report.getMetadata().getApexVersion());

        // Validate HTML content (generated from JSON model using Handlebars template)
        String htmlContent = Files.readString(htmlPath);
        assertTrue(htmlContent.contains("Schema Diff Report"), "HTML should contain schema diff title");
        assertTrue(htmlContent.contains("Matching"), "HTML should contain stats");
        assertTrue(htmlContent.contains("APEX Version"), "HTML should contain APEX version from JSON model");
        logger.info("[OK] HTML report contains expected content (generated from JSON model)");
    }

    @Test
    @Order(3)
    @DisplayName("Should detect schema changes and reflect in JSON report")
    void shouldDetectChangesInJsonReport() throws Exception {
        logger.info("\n=== Test: Schema Changes Reflected in JSON ===\n");

        // Load pipeline comparing schemas with differences
        rulesEngine = RulesEngine.fromClasspath("dev/mars/apex/core/engine/config/JsonReportPipelineIntegrationTest_WithChanges.yaml");

        // Execute pipeline
        RuleResult result = rulesEngine.evaluate(new HashMap<>());

        assertTrue(result.isSuccess(), "Pipeline should execute successfully");
        logger.info("[OK] Pipeline executed successfully");

        // Load generated JSON report
        Path jsonPath = Paths.get("apex-data-sync/reports/schema-diff-changes-test.json");
        assertTrue(Files.exists(jsonPath), "JSON report should exist");

        SchemaDiffJsonSerializer serializer = new SchemaDiffJsonSerializer();
        SchemaDiffReport report = serializer.fromJsonFile(jsonPath.toString());

        // Verify statistics reflect actual changes from columns section
        int matchingCount = report.getColumns().getMatching() != null ? report.getColumns().getMatching().size() : 0;
        int addedCount = report.getColumns().getAdded() != null ? report.getColumns().getAdded().size() : 0;
        int removedCount = report.getColumns().getRemoved() != null ? report.getColumns().getRemoved().size() : 0;
        int changedCount = report.getColumns().getChanged() != null ? report.getColumns().getChanged().size() : 0;
        
        logger.info("  → Statistics: {} matching, {} added, {} removed, {} changed",
            matchingCount, addedCount, removedCount, changedCount);

        // Verify column counts differ between source and target
        assertNotNull(report.getSummary().getTotalColumns(), "Should have total columns info");
        int sourceColumns = report.getSummary().getTotalColumns().getSource();
        int targetColumns = report.getSummary().getTotalColumns().getTarget();
        assertNotEquals(sourceColumns, targetColumns, "Source and target should have different column counts");
        
        logger.info("  → Source columns: {}, Target columns: {}", sourceColumns, targetColumns);
        logger.info("[OK] JSON report correctly reflects schema differences");

        // Verify column details are present
        if (report.getColumns().getAdded() != null && !report.getColumns().getAdded().isEmpty()) {
            logger.info("  → Added columns: {}", 
                report.getColumns().getAdded().size());
            report.getColumns().getAdded().forEach(col ->
                logger.info("    - {} ({})", col.getColumnName(), col.getTarget().getDataType()));
        }
    }
}
