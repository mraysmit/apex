package dev.mars.apex.core.service.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents schema metadata for a data source.
 * Contains information about columns, types, and constraints.
 *
 * @author APEX Team
 * @since 2.1.0
 */
public class SchemaMetadata {

    private String sourceName;
    private String sourceType; // "database", "csv", etc.
    private List<ColumnDefinition> columns = new ArrayList<>();

    public SchemaMetadata() {
    }

    public SchemaMetadata(String sourceName, String sourceType) {
        this.sourceName = sourceName;
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public List<ColumnDefinition> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnDefinition> columns) {
        this.columns = columns;
    }

    public void addColumn(ColumnDefinition column) {
        this.columns.add(column);
    }

    /**
     * Represents a single column in the schema.
     */
    public static class ColumnDefinition {
        private String name;
        private String dataType;
        private boolean nullable = true;
        private boolean primaryKey = false;
        private Integer maxLength;
        private Integer precision;
        private Integer scale;

        public ColumnDefinition() {
        }

        public ColumnDefinition(String name, String dataType) {
            this.name = name;
            this.dataType = dataType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        public void setPrimaryKey(boolean primaryKey) {
            this.primaryKey = primaryKey;
        }

        public Integer getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColumnDefinition that = (ColumnDefinition) o;
            return Objects.equals(name, that.name) &&
                   Objects.equals(dataType, that.dataType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, dataType);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" ").append(dataType);
            if (maxLength != null) {
                sb.append("(").append(maxLength).append(")");
            } else if (precision != null) {
                sb.append("(").append(precision);
                if (scale != null) {
                    sb.append(",").append(scale);
                }
                sb.append(")");
            }
            if (primaryKey) {
                sb.append(" PRIMARY KEY");
            }
            if (!nullable) {
                sb.append(" NOT NULL");
            }
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return "SchemaMetadata{" +
               "sourceName='" + sourceName + '\'' +
               ", sourceType='" + sourceType + '\'' +
               ", columns=" + columns.size() +
               '}';
    }
}
