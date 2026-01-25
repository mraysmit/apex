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

import dev.mars.apex.core.service.schema.SchemaMetadata;
import dev.mars.apex.core.service.schema.DataSourceContext;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import dev.mars.apex.core.service.schema.diff.json.model.*;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SchemaDiffReportBuilder}.
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class SchemaDiffReportBuilderTest {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffReportBuilderTest.class);

    private SchemaDiffReportBuilder builder;

    @BeforeEach
    void setUp() {
        logger.info("=== Setting up SchemaDiffReportBuilder test ===");
        builder = new SchemaDiffReportBuilder();
    }

    @Test
    void testBuildReport_Success() {
        logger.info("[TEST] testBuildReport_Success - Testing report builder with matching schemas");
        
        SchemaMetadata sourceSchema = new SchemaMetadata("users", "CSV");
        sourceSchema.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        sourceSchema.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        logger.info("  → Source schema: table={}, type={}, columns={}", "users", "CSV", 2);
        
        SchemaMetadata targetSchema = new SchemaMetadata("users", "PostgreSQL");
        targetSchema.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        targetSchema.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        logger.info("  → Target schema: table={}, type={}, columns={}", "users", "PostgreSQL", 2);
        
        SchemaComparisonResult result = new SchemaComparisonResult(sourceSchema, targetSchema);
        result.addMatchingColumn("id", "INTEGER");
        result.addMatchingColumn("name", "VARCHAR");
        logger.info("  → Comparison: 2 matching columns");
        
        SchemaDiffReport report = builder.buildReport(result, null, null);
        logger.info("  → Built report with 6 sections");
        
        assertNotNull(report);
        assertNotNull(report.getMetadata());
        assertNotNull(report.getSource());
        assertNotNull(report.getTarget());
        assertNotNull(report.getSummary());
        assertNotNull(report.getColumns());
        assertNotNull(report.getCompatibility());
        logger.info("  [OK] Report built successfully - all sections present");
    }
}
