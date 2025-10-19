package dev.mars.apex.yaml.manager.service;

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

import dev.mars.apex.yaml.manager.model.YamlContentSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YamlContentAnalyzer.
 */
@DisplayName("YamlContentAnalyzer Tests")
class YamlContentAnalyzerTest {

    private YamlContentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new YamlContentAnalyzer();
    }

    @Test
    @DisplayName("Should analyze trade validation rules YAML")
    void testAnalyzeTradeValidationRules() {
        String filePath = "src/test/resources/apex-yaml-samples/trade-validation-rules.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        assertNotNull(summary);
        assertEquals("trade-validation-rules", summary.getId());
        assertEquals("Trade Validation Rules", summary.getName());
        assertEquals("rules", summary.getFileType());
        assertEquals(2, summary.getRuleGroupCount());
        assertEquals(5, summary.getRuleCount());
        assertEquals(1, summary.getReferenceCount());
        assertEquals(1, summary.getConfigFileCount());
    }

    @Test
    @DisplayName("Should analyze trade enrichment YAML")
    void testAnalyzeTradeEnrichment() {
        String filePath = "src/test/resources/apex-yaml-samples/trade-enrichment.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        assertNotNull(summary);
        assertEquals("trade-enrichment", summary.getId());
        assertEquals("Trade Enrichment", summary.getName());
        assertEquals("enrichments", summary.getFileType());
        assertEquals(3, summary.getEnrichmentCount());
    }

    @Test
    @DisplayName("Should handle non-existent file gracefully")
    void testNonExistentFile() {
        String filePath = "non-existent-file.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        assertNotNull(summary);
        assertEquals(filePath, summary.getFilePath());
        assertNull(summary.getId());
    }

    @Test
    @DisplayName("Should extract metadata correctly")
    void testMetadataExtraction() {
        String filePath = "src/test/resources/apex-yaml-samples/trade-validation-rules.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        assertNotNull(summary.getId());
        assertNotNull(summary.getName());
        assertNotNull(summary.getDescription());
        assertNotNull(summary.getVersion());
    }

    @Test
    @DisplayName("Should determine file type as rules")
    void testFileTypeRules() {
        String filePath = "src/test/resources/apex-yaml-samples/trade-validation-rules.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        assertEquals("rules", summary.getFileType());
    }

    @Test
    @DisplayName("Should determine file type as enrichments")
    void testFileTypeEnrichments() {
        String filePath = "src/test/resources/apex-yaml-samples/trade-enrichment.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        assertEquals("enrichments", summary.getFileType());
    }

    @Test
    @DisplayName("Should count all rules across rule groups")
    void testRuleCountAcrossGroups() {
        String filePath = "src/test/resources/apex-yaml-samples/trade-validation-rules.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        // basic-validation has 3 rules, enriched-validation has 2 rules
        assertEquals(5, summary.getRuleCount());
    }

    @Test
    @DisplayName("Should count enrichments correctly")
    void testEnrichmentCount() {
        String filePath = "src/test/resources/apex-yaml-samples/trade-enrichment.yaml";
        YamlContentSummary summary = analyzer.analyzYamlContent(filePath);

        // Should have 3 enrichments
        assertEquals(3, summary.getEnrichmentCount());
    }

    @Test
    @DisplayName("Should create summary with file path")
    void testSummaryCreation() {
        String filePath = "test.yaml";
        YamlContentSummary summary = new YamlContentSummary(filePath);

        assertEquals(filePath, summary.getFilePath());
        assertNotNull(summary.getContentCounts());
    }

    @Test
    @DisplayName("Should add content counts")
    void testAddContentCount() {
        YamlContentSummary summary = new YamlContentSummary("test.yaml");
        summary.addContentCount("custom-items", 5);

        assertEquals(5, summary.getContentCounts().get("custom-items"));
    }
}

