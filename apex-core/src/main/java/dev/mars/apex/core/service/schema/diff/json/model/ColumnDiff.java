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
 * Created: 2026-01-17
 */
package dev.mars.apex.core.service.schema.diff.json.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailed information about a column difference.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnDiff {

    @JsonProperty("columnName")
    private String columnName;

    @JsonProperty("status")
    private String status;

    @JsonProperty("source")
    private ColumnInfo source;

    @JsonProperty("target")
    private ColumnInfo target;

    @JsonProperty("differences")
    private List<PropertyDifference> differences = new ArrayList<>();

    @JsonProperty("breakingChange")
    private boolean breakingChange;

    @JsonProperty("migrationAction")
    private String migrationAction;

    public ColumnDiff() {
    }

    public ColumnDiff(String columnName, String status, ColumnInfo source, ColumnInfo target, List<PropertyDifference> differences) {
        this.columnName = columnName;
        this.status = status;
        this.source = source;
        this.target = target;
        this.differences = differences;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ColumnInfo getSource() {
        return source;
    }

    public void setSource(ColumnInfo source) {
        this.source = source;
    }

    public ColumnInfo getTarget() {
        return target;
    }

    public void setTarget(ColumnInfo target) {
        this.target = target;
    }

    public List<PropertyDifference> getDifferences() {
        return differences;
    }

    public void setDifferences(List<PropertyDifference> differences) {
        this.differences = differences;
    }

    public boolean isBreakingChange() {
        return breakingChange;
    }

    public void setBreakingChange(boolean breakingChange) {
        this.breakingChange = breakingChange;
    }

    public String getMigrationAction() {
        return migrationAction;
    }

    public void setMigrationAction(String migrationAction) {
        this.migrationAction = migrationAction;
    }

    /**
     * Column metadata information.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ColumnInfo {
        @JsonProperty("dataType")
        private String dataType;

        @JsonProperty("size")
        private Integer size;

        @JsonProperty("precision")
        private Integer precision;

        @JsonProperty("scale")
        private Integer scale;

        @JsonProperty("nullable")
        private Boolean nullable;

        @JsonProperty("primaryKey")
        private Boolean primaryKey;

        @JsonProperty("autoIncrement")
        private Boolean autoIncrement;

        @JsonProperty("defaultValue")
        private String defaultValue;

        public ColumnInfo() {}

        public ColumnInfo(String dataType, Integer size, Boolean nullable, String defaultValue) {
            this.dataType = dataType;
            this.size = size;
            this.nullable = nullable;
            this.defaultValue = defaultValue;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public Integer getSize() {
            return size;
        }

        public void setSize(Integer size) {
            this.size = size;
        }

        public Integer getPrecision() {
            return precision;
        }

        public void setPrecision(Integer precision) {
            this.precision = precision;
        }

        public Integer getScale() {
            return scale;
        }

        public void setScale(Integer scale) {
            this.scale = scale;
        }

        public Boolean getNullable() {
            return nullable;
        }

        public void setNullable(Boolean nullable) {
            this.nullable = nullable;
        }

        public Boolean getPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(Boolean primaryKey) {
            this.primaryKey = primaryKey;
        }

        public Boolean getAutoIncrement() {
            return autoIncrement;
        }

        public void setAutoIncrement(Boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    /**
     * Represents a single property difference between source and target.
     */
    public static class PropertyDifference {
        @JsonProperty("property")
        private String property;

        @JsonProperty("sourceValue")
        private Object sourceValue;

        @JsonProperty("targetValue")
        private Object targetValue;

        @JsonProperty("changeType")
        private String changeType;

        @JsonProperty("breaking")
        private boolean breaking;

        @JsonProperty("description")
        private String description;

        public PropertyDifference() {}

        public PropertyDifference(String property, Object sourceValue, Object targetValue, boolean breaking) {
            this.property = property;
            this.sourceValue = sourceValue;
            this.targetValue = targetValue;
            this.breaking = breaking;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public Object getSourceValue() {
            return sourceValue;
        }

        public void setSourceValue(Object sourceValue) {
            this.sourceValue = sourceValue;
        }

        public Object getTargetValue() {
            return targetValue;
        }

        public void setTargetValue(Object targetValue) {
            this.targetValue = targetValue;
        }

        public String getChangeType() {
            return changeType;
        }

        public void setChangeType(String changeType) {
            this.changeType = changeType;
        }

        public boolean isBreaking() {
            return breaking;
        }

        public void setBreaking(boolean breaking) {
            this.breaking = breaking;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
