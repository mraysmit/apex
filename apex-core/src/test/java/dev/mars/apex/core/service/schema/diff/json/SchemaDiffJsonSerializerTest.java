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
package dev.mars.apex.core.service.schema.diff.json;

import dev.mars.apex.core.service.schema.diff.json.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SchemaDiffJsonSerializer}.
 */
class SchemaDiffJsonSerializerTest {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffJsonSerializerTest.class);

    private SchemaDiffJsonSerializer serializer;
    
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up SchemaDiffJsonSerializer test ===");
        serializer = new SchemaDiffJsonSerializer();
    }

    @Test
    void testToJsonString() throws IOException {
        logger.info("[TEST] testToJsonString - Testing JSON string serialization");
        SchemaDiffReport report = createMinimalReport();
        logger.info("  → Created report: source={}, target={}", report.getSource().getName(), report.getTarget().getName());
        
        String json = serializer.toJsonString(report);
        logger.info("  → Generated JSON length: {} characters", json.length());
        
        assertNotNull(json);
        assertTrue(json.contains("\"$schema\""));
        assertTrue(json.contains("\"metadata\""));
        logger.info("  ✓ JSON string serialization successful");
    }

    @Test
    void testRoundTrip() throws IOException {
        logger.info("[TEST] testRoundTrip - Testing serialize/deserialize round-trip");
        SchemaDiffReport original = createMinimalReport();
        logger.info("  → Original report version: {}", original.getMetadata().getReportVersion());
        
        String json = serializer.toJsonString(original);
        logger.info("  → Serialized to JSON: {} characters", json.length());
        
        SchemaDiffReport deserialized = serializer.fromJsonString(json);
        logger.info("  → Deserialized report version: {}", deserialized.getMetadata().getReportVersion());
        
        assertNotNull(deserialized);
        assertEquals(original.getMetadata().getReportVersion(), deserialized.getMetadata().getReportVersion());
        logger.info("  ✓ Round-trip successful - data integrity verified");
    }

    @Test
    void testToJsonFile() throws IOException {
        logger.info("[TEST] testToJsonFile - Testing JSON file generation");
        SchemaDiffReport report = createMinimalReport();
        File outputFile = tempDir.resolve("test-report.json").toFile();
        logger.info("  → Target file: {}", outputFile.getAbsolutePath());
        
        String path = serializer.toJsonFile(report, outputFile.getAbsolutePath());
        logger.info("  → File created at: {}", path);
        logger.info("  → File size: {} bytes", outputFile.length());
        
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
        logger.info("  ✓ JSON file generation successful");
    }

    private SchemaDiffReport createMinimalReport() {
        ReportMetadata metadata = new ReportMetadata();
        metadata.setReportVersion("1.0");
        metadata.setGeneratedAt(Instant.now().toString());
        metadata.setApexVersion("2.1.0");
        metadata.setComparisonType("schema-diff");
        
        DataSourceInfo source = new DataSourceInfo();
        source.setName("source");
        source.setType("CSV");
        
        DataSourceInfo target = new DataSourceInfo();
        target.setName("target");
        target.setType("PostgreSQL");
        
        ComparisonSummary summary = new ComparisonSummary();
        summary.setTotalColumns(new ComparisonSummary.TotalColumns());
        
        ColumnComparison columns = new ColumnComparison();
        CompatibilityAnalysis compatibility = new CompatibilityAnalysis();
        
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
