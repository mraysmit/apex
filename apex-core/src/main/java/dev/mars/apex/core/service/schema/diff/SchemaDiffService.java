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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for comparing two SchemaMetadata objects.
 * Implements set-based comparison for schema columns.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class SchemaDiffService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaDiffService.class);

    /**
     * Compare two schemas and return detailed comparison result.
     *
     * @param sourceSchema the source schema
     * @param targetSchema the target schema
     * @param options comparison options (type mappings, case sensitivity, etc.)
     * @return detailed comparison result
     */
    public SchemaComparisonResult compareSchemas(SchemaMetadata sourceSchema, 
                                                SchemaMetadata targetSchema,
                                                ComparisonOptions options) {
        LOGGER.info("Comparing schemas: {} → {}", 
                   sourceSchema.getSourceName(), targetSchema.getSourceName());

        SchemaComparisonResult result = new SchemaComparisonResult(sourceSchema, targetSchema);

        // Build column maps for efficient lookup
        Map<String, SchemaMetadata.ColumnDefinition> sourceColumns = buildColumnMap(sourceSchema, options);
        Map<String, SchemaMetadata.ColumnDefinition> targetColumns = buildColumnMap(targetSchema, options);

        // Set-based comparison
        Set<String> sourceColumnNames = sourceColumns.keySet();
        Set<String> targetColumnNames = targetColumns.keySet();

        // Find added columns (in target, not in source)
        for (String columnName : targetColumnNames) {
            if (!sourceColumnNames.contains(columnName)) {
                SchemaMetadata.ColumnDefinition targetCol = targetColumns.get(columnName);
                result.addAddedColumn(targetCol.getName(), targetCol.getDataType());
                LOGGER.debug("Added column: {} ({})", targetCol.getName(), targetCol.getDataType());
            }
        }

        // Find removed columns (in source, not in target)
        for (String columnName : sourceColumnNames) {
            if (!targetColumnNames.contains(columnName)) {
                SchemaMetadata.ColumnDefinition sourceCol = sourceColumns.get(columnName);
                result.addRemovedColumn(sourceCol.getName(), sourceCol.getDataType());
                LOGGER.debug("Removed column: {} ({})", sourceCol.getName(), sourceCol.getDataType());
            }
        }

        // Find matching and changed columns (in both)
        for (String columnName : sourceColumnNames) {
            if (targetColumnNames.contains(columnName)) {
                SchemaMetadata.ColumnDefinition sourceCol = sourceColumns.get(columnName);
                SchemaMetadata.ColumnDefinition targetCol = targetColumns.get(columnName);

                if (areTypesCompatible(sourceCol.getDataType(), targetCol.getDataType(), options)) {
                    result.addMatchingColumn(columnName, sourceCol.getDataType());
                    LOGGER.debug("Matching column: {} ({})", columnName, sourceCol.getDataType());
                } else {
                    boolean breaking = isBreakingChange(sourceCol.getDataType(), targetCol.getDataType(), options);
                    result.addChangedColumn(columnName, sourceCol.getDataType(), targetCol.getDataType(), breaking);
                    LOGGER.debug("Changed column: {} ({} → {}) [breaking={}]", 
                               columnName, sourceCol.getDataType(), targetCol.getDataType(), breaking);
                }
            }
        }

        LOGGER.info("Schema comparison complete: {} added, {} removed, {} matching, {} changed, {} breaking", 
                   result.getAddedColumns().size(),
                   result.getRemovedColumns().size(),
                   result.getMatchingColumns().size(),
                   result.getChangedColumns().size(),
                   result.getBreakingChanges().size());

        return result;
    }

    /**
     * Build map of column name → column definition.
     * Handles case-insensitive name comparison if configured.
     */
    private Map<String, SchemaMetadata.ColumnDefinition> buildColumnMap(SchemaMetadata schema, 
                                                                       ComparisonOptions options) {
        Map<String, SchemaMetadata.ColumnDefinition> columnMap = new LinkedHashMap<>();
        for (SchemaMetadata.ColumnDefinition column : schema.getColumns()) {
            String key = options.isCaseInsensitiveNames() ? 
                column.getName().toLowerCase() : column.getName();
            columnMap.put(key, column);
        }
        return columnMap;
    }

    /**
     * Check if two data types are compatible (same or widening conversion).
     */
    private boolean areTypesCompatible(String sourceType, String targetType, ComparisonOptions options) {
        // Exact match
        if (sourceType.equalsIgnoreCase(targetType)) {
            return true;
        }

        // Check type mappings (e.g., VARCHAR → TEXT, INT → BIGINT)
        if (options.getTypeMappings() != null) {
            String mappedType = options.getTypeMappings().get(sourceType.toUpperCase());
            if (mappedType != null && mappedType.equalsIgnoreCase(targetType)) {
                return true;
            }
        }

        // Inferred type tolerance for CSV (all CSV types infer to VARCHAR/TEXT)
        if (options.isInferredTypeTolerance() && 
            isTextType(sourceType) && isTextType(targetType)) {
            return true;
        }

        return false;
    }

    /**
     * Check if a type change is breaking (type narrowing or incompatible).
     */
    private boolean isBreakingChange(String sourceType, String targetType, ComparisonOptions options) {
        // For now, any type change not covered by areTypesCompatible is considered breaking
        // Future: implement detailed type conversion rules
        return true;
    }

    private boolean isTextType(String type) {
        String upperType = type.toUpperCase();
        return upperType.contains("VARCHAR") || upperType.contains("TEXT") || 
               upperType.contains("STRING") || upperType.contains("CHAR");
    }
}
