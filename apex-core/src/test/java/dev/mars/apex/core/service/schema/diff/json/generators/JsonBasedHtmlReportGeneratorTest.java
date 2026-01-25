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

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonBasedHtmlReportGenerator}.
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class JsonBasedHtmlReportGeneratorTest {

    private static final Logger logger = LoggerFactory.getLogger(JsonBasedHtmlReportGeneratorTest.class);

    private JsonBasedHtmlReportGenerator generator;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up JsonBasedHtmlReportGenerator test ===");
        generator = new JsonBasedHtmlReportGenerator();
    }

    @Test
    void testGenerateFromReport() throws IOException {
        logger.info("[TEST] testGenerateFromReport - Testing HTML generation from JSON report");
        SchemaDiffReport report = createTestReport();
        logger.info("  → Report: source={}, target={}", report.getSource().getName(), report.getTarget().getName());
        
        Path outputFile = tempDir.resolve("test-report.html");
        logger.info("  → Target HTML: {}", outputFile);
        
        String path = generator.generateFromReport(report, outputFile.toString());
        logger.info("  → Generated HTML at: {}", path);
        
        assertNotNull(path);
        assertTrue(Files.exists(Path.of(path)));
        
        String html = Files.readString(Path.of(path));
        logger.info("  → HTML size: {} characters", html.length());
        
        assertTrue(html.contains("Schema Diff Report"));
        assertTrue(html.contains("Matching"));
        assertTrue(html.contains("test-source"));
        logger.info("  [OK] HTML generation successful - contains expected content");
    }

    @Test
    void testGeneratedHtmlContainsStats() throws IOException {
        logger.info("[TEST] testGeneratedHtmlContainsStats - Testing statistics rendering in HTML");
        SchemaDiffReport report = createTestReport();
        report.getSummary().getStatistics().setMatching(10);
        report.getSummary().getStatistics().setAdded(5);
        logger.info("  → Stats: matching=10, added=5");
        
        Path outputFile = tempDir.resolve("stats-report.html");
        String path = generator.generateFromReport(report, outputFile.toString());
        logger.info("  → Generated HTML: {}", path);
        
        String html = Files.readString(Path.of(path));
        assertTrue(html.contains("10")); // matching count
        assertTrue(html.contains("5"));  // added count
        logger.info("  [OK] Statistics correctly rendered in HTML");
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
