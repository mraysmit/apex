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
 * Created: 2026-01-13
 */
package dev.mars.apex.core.service.schema.diff;

import dev.mars.apex.core.service.schema.SchemaMetadata;
import org.junit.jupiter.api.Test;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SchemaDiffService.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@DisplayName("Schema Diff Service Tests")
class SchemaDiffServiceTest {

    private final SchemaDiffService diffService = new SchemaDiffService();

    @Test
    @DisplayName("Should detect added columns")
    void testAddedColumns() {
        // Arrange
        SchemaMetadata source = new SchemaMetadata("source", "csv");
        source.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));

        SchemaMetadata target = new SchemaMetadata("target", "database");
        target.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        target.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        target.addColumn(new SchemaMetadata.ColumnDefinition("email", "VARCHAR")); // Added

        // Act
        SchemaComparisonResult result = diffService.compareSchemas(source, target, ComparisonOptions.defaults());

        // Assert
        assertEquals(1, result.getAddedColumns().size());
        assertEquals("email", result.getAddedColumns().get(0).getColumnName());
        assertEquals(2, result.getMatchingColumns().size());
        assertEquals(0, result.getRemovedColumns().size());
        assertTrue(result.isCompatible()); // Adding columns is compatible
    }

    @Test
    @DisplayName("Should detect removed columns as breaking change")
    void testRemovedColumns() {
        // Arrange
        SchemaMetadata source = new SchemaMetadata("source", "csv");
        source.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("email", "VARCHAR"));

        SchemaMetadata target = new SchemaMetadata("target", "database");
        target.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        target.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        // email removed

        // Act
        SchemaComparisonResult result = diffService.compareSchemas(source, target, ComparisonOptions.defaults());

        // Assert
        assertEquals(0, result.getAddedColumns().size());
        assertEquals(2, result.getMatchingColumns().size());
        assertEquals(1, result.getRemovedColumns().size());
        assertEquals("email", result.getRemovedColumns().get(0).getColumnName());
        assertFalse(result.isCompatible()); // Removing columns is breaking
        assertTrue(result.getBreakingChanges().size() > 0);
    }

    @Test
    @DisplayName("Should detect type changes")
    void testTypeChanges() {
        // Arrange
        SchemaMetadata source = new SchemaMetadata("source", "csv");
        source.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("amount", "INTEGER"));

        SchemaMetadata target = new SchemaMetadata("target", "database");
        target.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        target.addColumn(new SchemaMetadata.ColumnDefinition("amount", "DECIMAL")); // Type changed

        // Act
        SchemaComparisonResult result = diffService.compareSchemas(source, target, ComparisonOptions.defaults());

        // Assert
        assertEquals(1, result.getMatchingColumns().size());
        assertEquals(1, result.getChangedColumns().size());
        assertEquals("amount", result.getChangedColumns().get(0).getColumnName());
        assertFalse(result.isCompatible()); // Type changes are breaking by default
    }

    @Test
    @DisplayName("Should handle case-insensitive column names")
    void testCaseInsensitiveNames() {
        // Arrange
        SchemaMetadata source = new SchemaMetadata("source", "csv");
        source.addColumn(new SchemaMetadata.ColumnDefinition("CustomerID", "INTEGER"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("CustomerName", "VARCHAR"));

        SchemaMetadata target = new SchemaMetadata("target", "database");
        target.addColumn(new SchemaMetadata.ColumnDefinition("customerid", "INTEGER")); // Different case
        target.addColumn(new SchemaMetadata.ColumnDefinition("customername", "VARCHAR")); // Different case

        ComparisonOptions options = ComparisonOptions.defaults();
        options.setCaseInsensitiveNames(true);

        // Act
        SchemaComparisonResult result = diffService.compareSchemas(source, target, options);

        // Assert
        assertEquals(2, result.getMatchingColumns().size());
        assertEquals(0, result.getAddedColumns().size());
        assertEquals(0, result.getRemovedColumns().size());
        assertTrue(result.isCompatible());
    }

    @Test
    @DisplayName("Should match identical schemas")
    void testIdenticalSchemas() {
        // Arrange
        SchemaMetadata source = new SchemaMetadata("source", "csv");
        source.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("active", "BOOLEAN"));

        SchemaMetadata target = new SchemaMetadata("target", "database");
        target.addColumn(new SchemaMetadata.ColumnDefinition("id", "INTEGER"));
        target.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        target.addColumn(new SchemaMetadata.ColumnDefinition("active", "BOOLEAN"));

        // Act
        SchemaComparisonResult result = diffService.compareSchemas(source, target, ComparisonOptions.defaults());

        // Assert
        assertEquals(3, result.getMatchingColumns().size());
        assertEquals(0, result.getAddedColumns().size());
        assertEquals(0, result.getRemovedColumns().size());
        assertEquals(0, result.getChangedColumns().size());
        assertTrue(result.isCompatible());
        assertFalse(result.hasChanges());
    }

    @Test
    @DisplayName("Should apply type mappings")
    void testTypeMappings() {
        // Arrange
        SchemaMetadata source = new SchemaMetadata("source-sqlserver", "database");
        source.addColumn(new SchemaMetadata.ColumnDefinition("name", "NVARCHAR"));
        source.addColumn(new SchemaMetadata.ColumnDefinition("amount", "NUMBER"));

        SchemaMetadata target = new SchemaMetadata("target-postgres", "database");
        target.addColumn(new SchemaMetadata.ColumnDefinition("name", "VARCHAR"));
        target.addColumn(new SchemaMetadata.ColumnDefinition("amount", "NUMERIC"));

        ComparisonOptions options = ComparisonOptions.defaults();
        // Defaults already include NVARCHAR→VARCHAR and NUMBER→NUMERIC

        // Act
        SchemaComparisonResult result = diffService.compareSchemas(source, target, options);

        // Assert
        assertEquals(2, result.getMatchingColumns().size());
        assertEquals(0, result.getChangedColumns().size());
        assertTrue(result.isCompatible());
    }
}
