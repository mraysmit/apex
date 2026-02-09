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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Result of comparing two schema metadata objects.
 * Contains lists of added, removed, matching, and changed columns.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-13
 */
public class SchemaComparisonResult {

    private final SchemaMetadata sourceSchema;
    private final SchemaMetadata targetSchema;
    
    private final List<ColumnDifference> addedColumns = new ArrayList<>();
    private final List<ColumnDifference> removedColumns = new ArrayList<>();
    private final List<ColumnDifference> matchingColumns = new ArrayList<>();
    private final List<ColumnDifference> changedColumns = new ArrayList<>();
    
    private final List<String> breakingChanges = new ArrayList<>();
    private boolean compatible = true;

    public SchemaComparisonResult(SchemaMetadata sourceSchema, SchemaMetadata targetSchema) {
        this.sourceSchema = sourceSchema;
        this.targetSchema = targetSchema;
    }

    public void addAddedColumn(String columnName, String targetType) {
        addedColumns.add(new ColumnDifference(columnName, null, targetType, DifferenceType.ADDED));
    }

    public void addRemovedColumn(String columnName, String sourceType) {
        removedColumns.add(new ColumnDifference(columnName, sourceType, null, DifferenceType.REMOVED));
        addBreakingChange("Removed column: " + columnName + " (" + sourceType + ")");
    }

    public void addMatchingColumn(String columnName, String type) {
        matchingColumns.add(new ColumnDifference(columnName, type, type, DifferenceType.MATCHING));
    }

    public void addChangedColumn(String columnName, String sourceType, String targetType, boolean breaking) {
        changedColumns.add(new ColumnDifference(columnName, sourceType, targetType, 
            breaking ? DifferenceType.BREAKING_CHANGE : DifferenceType.CHANGED));
        if (breaking) {
            addBreakingChange("Type narrowing: " + columnName + " (" + sourceType + " → " + targetType + ")");
        }
    }

    public void addBreakingChange(String description) {
        breakingChanges.add(description);
        compatible = false;
    }

    // Getters
    public SchemaMetadata getSourceSchema() {
        return sourceSchema;
    }

    public SchemaMetadata getTargetSchema() {
        return targetSchema;
    }

    public List<ColumnDifference> getAddedColumns() {
        return addedColumns;
    }

    public List<ColumnDifference> getRemovedColumns() {
        return removedColumns;
    }

    public List<ColumnDifference> getMatchingColumns() {
        return matchingColumns;
    }

    public List<ColumnDifference> getChangedColumns() {
        return changedColumns;
    }

    public List<String> getBreakingChanges() {
        return breakingChanges;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public boolean hasChanges() {
        return !addedColumns.isEmpty() || !removedColumns.isEmpty() || !changedColumns.isEmpty();
    }

    /**
     * Represents a single column difference between source and target schemas.
     */
    public static class ColumnDifference {
        private final String columnName;
        private final String sourceType;
        private final String targetType;
        private final DifferenceType differenceType;

        public ColumnDifference(String columnName, String sourceType, String targetType, 
                               DifferenceType differenceType) {
            this.columnName = columnName;
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.differenceType = differenceType;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getSourceType() {
            return sourceType;
        }

        public String getTargetType() {
            return targetType;
        }

        public DifferenceType getDifferenceType() {
            return differenceType;
        }
    }

    /**
     * Type of schema difference.
     */
    public enum DifferenceType {
        ADDED,
        REMOVED,
        MATCHING,
        CHANGED,
        BREAKING_CHANGE
    }
}
