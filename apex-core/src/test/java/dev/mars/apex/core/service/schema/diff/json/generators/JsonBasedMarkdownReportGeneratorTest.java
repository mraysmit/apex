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
package dev.mars.apex.core.service.schema.diff.json.generators;

import dev.mars.apex.core.service.schema.diff.json.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonBasedMarkdownReportGenerator}.
 */
class JsonBasedMarkdownReportGeneratorTest {

    private static final Logger logger = LoggerFactory.getLogger(JsonBasedMarkdownReportGeneratorTest.class);

    private JsonBasedMarkdownReportGenerator generator;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up JsonBasedMarkdownReportGenerator test ===");
        generator = new JsonBasedMarkdownReportGenerator();
    }

    @Test
    void testGenerateFromReport() throws IOException {
        logger.info("[TEST] testGenerateFromReport - Testing Markdown generation from JSON report");
        SchemaDiffReport report = createTestReport();
        logger.info("  → Report: source={}, target={}", report.getSource().getName(), report.getTarget().getName());
        
        Path outputFile = tempDir.resolve("test-report.md");
        logger.info("  → Target Markdown: {}", outputFile);
        
        String path = generator.generateFromReport(report, outputFile.toString());
        logger.info("  → Generated Markdown at: {}", path);
        
        assertNotNull(path);
        assertTrue(Files.exists(Path.of(path)));
        
        String markdown = Files.readString(Path.of(path));
        logger.info("  → Markdown size: {} characters", markdown.length());
        
        assertTrue(markdown.contains("# 📊 Schema Diff Report"));
        assertTrue(markdown.contains("## 📈 Comparison Summary"));
        assertTrue(markdown.contains("test-source"));
        logger.info("  ✓ Markdown generation successful - contains expected sections");
    }

    @Test
    void testGeneratedMarkdownContainsStats() throws IOException {
        logger.info("[TEST] testGeneratedMarkdownContainsStats - Testing statistics table in Markdown");
        SchemaDiffReport report = createTestReport();
        report.getSummary().getStatistics().setMatching(10);
        report.getSummary().getStatistics().setAdded(5);
        logger.info("  → Stats: matching=10, added=5");
        
        Path outputFile = tempDir.resolve("stats-report.md");
        String path = generator.generateFromReport(report, outputFile.toString());
        logger.info("  → Generated Markdown: {}", path);
        
        String markdown = Files.readString(Path.of(path));
        assertTrue(markdown.contains("| ✅ Matching | 10 |"));
        assertTrue(markdown.contains("| ➕ Added | 5 |"));
        logger.info("  ✓ Statistics correctly formatted in Markdown table");
    }

    @Test
    void testCompatibleMigrationMessage() throws IOException {
        logger.info("[TEST] testCompatibleMigrationMessage - Testing compatibility message rendering");
        SchemaDiffReport report = createTestReport();
        report.getCompatibility().setCompatible(true);
        logger.info("  → Compatibility: true (backward compatible)");
        
        Path outputFile = tempDir.resolve("compatible-report.md");
        String path = generator.generateFromReport(report, outputFile.toString());
        logger.info("  → Generated Markdown: {}", path);
        
        String markdown = Files.readString(Path.of(path));
        assertTrue(markdown.contains("✓ **Compatible Migration:**"));
        logger.info("  ✓ Compatible migration message correctly rendered");
    }

    private SchemaDiffReport createTestReport() {
        ReportMetadata metadata = new ReportMetadata();
        metadata.setReportVersion("1.0");
        metadata.setGeneratedAt(Instant.now().toString());
        metadata.setApexVersion("2.1.0");
        metadata.setComparisonType("schema-diff");
        
        DataSourceInfo source = new DataSourceInfo();
        source.setName("test-source");
        source.setType("CSV");
        
        DataSourceInfo target = new DataSourceInfo();
        target.setName("test-target");
        target.setType("PostgreSQL");
        
        ComparisonSummary summary = new ComparisonSummary();
        summary.setTotalColumns(new ComparisonSummary.TotalColumns());
        summary.setStatistics(new ComparisonSummary.Statistics());
        summary.getStatistics().setMatching(2);
        summary.getStatistics().setAdded(0);
        summary.getStatistics().setRemoved(0);
        summary.getStatistics().setChanged(0);
        
        ColumnComparison columns = new ColumnComparison();
        CompatibilityAnalysis compatibility = new CompatibilityAnalysis();
        compatibility.setCompatible(true);
        
        return SchemaDiffReport.builder()
            .metadata(metadata)
            .source(source)
            .target(target)
            .summary(summary)
            .columns(columns)
            .compatibility(compatibility)
            .build();
    }
}
