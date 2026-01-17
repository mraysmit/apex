/*
 * Copyright (c) 2024 Michael Rayment Smith
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
package dev.mars.apex.sync.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mars.apex.core.loader.YamlConfigurationLoader;
import dev.mars.apex.core.model.config.YamlConfiguration;
import dev.mars.apex.core.service.YamlRulesEngineService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for schema-diff JSON report generation in the pipeline.
 * Tests end-to-end functionality from YAML config to JSON output file.
 */
@ExtendWith(ColoredTestOutputExtension.class)
class SchemaDiffJsonIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testSchemaDiffPipeline_GeneratesJsonReport() throws Exception {
        // Load configuration with json-report-output parameter
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlConfiguration config = loader.loadFromClasspath("pipelines/schema-diff-json-test.yaml");
        
        // Execute pipeline
        YamlRulesEngineService service = new YamlRulesEngineService(config);
        service.processDataWithPipelines();
        
        // Verify JSON report was created
        File jsonReport = new File("target/reports/schema-diff-json-test.json");
        assertTrue(jsonReport.exists(), "JSON report file should exist");
        assertTrue(jsonReport.length() > 0, "JSON report should not be empty");
        
        // Verify JSON structure
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonReport);
        
        // Verify required top-level fields
        assertTrue(rootNode.has("$schema"), "Should have $schema field");
        assertTrue(rootNode.has("metadata"), "Should have metadata");
        assertTrue(rootNode.has("source"), "Should have source");
        assertTrue(rootNode.has("target"), "Should have target");
        assertTrue(rootNode.has("comparison"), "Should have comparison");
        
        // Verify metadata structure
        JsonNode metadata = rootNode.get("metadata");
        assertEquals("1.0", metadata.get("reportVersion").asText());
        assertEquals("APEX Schema Diff", metadata.get("generatedBy").asText());
        assertTrue(metadata.has("generatedAt"));
        
        // Verify data source information
        JsonNode source = rootNode.get("source");
        assertTrue(source.has("name"));
        assertTrue(source.has("type"));
        assertTrue(source.has("connection"));
        assertTrue(source.has("table"));
        
        // Verify comparison results
        JsonNode comparison = rootNode.get("comparison");
        assertTrue(comparison.has("summary"));
        assertTrue(comparison.has("columns"));
        assertTrue(comparison.has("compatibility"));
        assertTrue(comparison.has("recommendations"));
        
        // Verify summary statistics
        JsonNode summary = comparison.get("summary");
        assertTrue(summary.has("totalColumns"));
        assertTrue(summary.has("statistics"));
        
        JsonNode totalColumns = summary.get("totalColumns");
        assertTrue(totalColumns.has("source"));
        assertTrue(totalColumns.has("target"));
        assertTrue(totalColumns.has("matched"));
        assertTrue(totalColumns.has("modified"));
        assertTrue(totalColumns.has("common"));
    }

    @Test
    void testSchemaDiffPipeline_GeneratesBothReports() throws Exception {
        // Load configuration that generates both HTML and JSON
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlConfiguration config = loader.loadFromClasspath("pipelines/schema-diff-dual-output-test.yaml");
        
        // Execute pipeline
        YamlRulesEngineService service = new YamlRulesEngineService(config);
        service.processDataWithPipelines();
        
        // Verify both reports exist
        File htmlReport = new File("target/reports/schema-diff-dual-test.html");
        File jsonReport = new File("target/reports/schema-diff-dual-test.json");
        
        assertTrue(htmlReport.exists(), "HTML report should exist");
        assertTrue(jsonReport.exists(), "JSON report should exist");
        
        assertTrue(htmlReport.length() > 0, "HTML report should not be empty");
        assertTrue(jsonReport.length() > 0, "JSON report should not be empty");
    }

    @Test
    void testJsonReport_ValidatesAgainstSchema() throws Exception {
        // Load and execute pipeline
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlConfiguration config = loader.loadFromClasspath("pipelines/schema-diff-json-test.yaml");
        
        YamlRulesEngineService service = new YamlRulesEngineService(config);
        service.processDataWithPipelines();
        
        // Load JSON report
        File jsonReport = new File("target/reports/schema-diff-json-test.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonReport);
        
        // Verify schema reference
        String schemaUrl = rootNode.get("$schema").asText();
        assertEquals("https://apex-rules-engine.dev/schemas/schema-diff-v1.0.json", schemaUrl);
        
        // Validate structure matches schema expectations
        assertValidJsonStructure(rootNode);
    }

    @Test
    void testJsonReport_CompatibilityAnalysis() throws Exception {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlConfiguration config = loader.loadFromClasspath("pipelines/schema-diff-json-test.yaml");
        
        YamlRulesEngineService service = new YamlRulesEngineService(config);
        service.processDataWithPipelines();
        
        File jsonReport = new File("target/reports/schema-diff-json-test.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonReport);
        
        JsonNode compatibility = rootNode.get("comparison").get("compatibility");
        
        assertTrue(compatibility.has("compatible"));
        assertTrue(compatibility.has("breakingChanges"));
        assertTrue(compatibility.has("safeChanges"));
        
        // Verify breaking changes structure if any exist
        JsonNode breakingChanges = compatibility.get("breakingChanges");
        if (breakingChanges.size() > 0) {
            JsonNode firstChange = breakingChanges.get(0);
            assertTrue(firstChange.has("columnName"));
            assertTrue(firstChange.has("changeType"));
            assertTrue(firstChange.has("impact"));
            assertTrue(firstChange.has("description"));
        }
    }

    @Test
    void testJsonReport_Recommendations() throws Exception {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlConfiguration config = loader.loadFromClasspath("pipelines/schema-diff-json-test.yaml");
        
        YamlRulesEngineService service = new YamlRulesEngineService(config);
        service.processDataWithPipelines();
        
        File jsonReport = new File("target/reports/schema-diff-json-test.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonReport);
        
        JsonNode recommendations = rootNode.get("comparison").get("recommendations");
        
        assertTrue(recommendations.isArray());
        assertTrue(recommendations.size() > 0, "Should have at least one recommendation");
        
        JsonNode firstRec = recommendations.get(0);
        assertTrue(firstRec.has("priority"));
        assertTrue(firstRec.has("message"));
        assertTrue(firstRec.has("actions"));
        
        String priority = firstRec.get("priority").asText();
        assertTrue(priority.matches("HIGH|MEDIUM|LOW"), 
                   "Priority should be HIGH, MEDIUM, or LOW");
    }

    private void assertValidJsonStructure(JsonNode rootNode) {
        // Validate top-level structure
        assertNotNull(rootNode.get("$schema"));
        assertNotNull(rootNode.get("metadata"));
        assertNotNull(rootNode.get("source"));
        assertNotNull(rootNode.get("target"));
        assertNotNull(rootNode.get("comparison"));
        
        // Validate metadata
        JsonNode metadata = rootNode.get("metadata");
        assertNotNull(metadata.get("reportVersion"));
        assertNotNull(metadata.get("generatedAt"));
        assertNotNull(metadata.get("generatedBy"));
        
        // Validate data sources have required fields
        validateDataSource(rootNode.get("source"));
        validateDataSource(rootNode.get("target"));
        
        // Validate comparison
        JsonNode comparison = rootNode.get("comparison");
        assertNotNull(comparison.get("summary"));
        assertNotNull(comparison.get("columns"));
        assertNotNull(comparison.get("compatibility"));
        assertNotNull(comparison.get("recommendations"));
    }

    private void validateDataSource(JsonNode dataSource) {
        assertNotNull(dataSource.get("name"));
        assertNotNull(dataSource.get("type"));
        assertNotNull(dataSource.get("connection"));
        assertNotNull(dataSource.get("table"));
        
        JsonNode table = dataSource.get("table");
        assertNotNull(table.get("name"));
        assertTrue(table.has("rowCount"));
    }
}
