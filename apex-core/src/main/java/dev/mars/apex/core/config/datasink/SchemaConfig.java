package dev.mars.apex.core.config.datasink;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for schema management in data sinks.
 * 
 * This class defines how database schemas, table structures, and data
 * validation should be handled for data sink operations.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class SchemaConfig {
    
    /**
     * Enumeration of schema management strategies.
     */
    public enum SchemaStrategy {
        NONE("none", "No schema management"),
        VALIDATE_ONLY("validate-only", "Validate against existing schema"),
        CREATE_IF_MISSING("create-if-missing", "Create schema/table if it doesn't exist"),
        UPDATE_IF_CHANGED("update-if-changed", "Update schema if structure changes"),
        RECREATE("recreate", "Drop and recreate schema/table"),
        CUSTOM("custom", "Use custom schema management");
        
        private final String code;
        private final String description;
        
        SchemaStrategy(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static SchemaStrategy fromCode(String code) {
            if (code == null) {
                return VALIDATE_ONLY; // Default
            }
            
            for (SchemaStrategy strategy : values()) {
                if (strategy.code.equalsIgnoreCase(code)) {
                    return strategy;
                }
            }
            
            return VALIDATE_ONLY;
        }
    }
    
    private Boolean enabled = true;
    private String strategy = "validate-only";
    private String schemaName;
    private String tableName;
    private String catalogName;
    
    // Schema creation settings
    @JsonProperty("auto-create")
    private Boolean autoCreate = false;
    @JsonProperty("auto-update")
    private Boolean autoUpdate = false;
    @JsonProperty("drop-if-exists")
    private Boolean dropIfExists = false;
    @JsonProperty("init-script")
    private String initScript;
    @JsonProperty("init-scripts")
    private List<String> initScripts;
    
    // Column definitions
    private Map<String, ColumnDefinition> columns;
    private List<String> primaryKeys;
    private List<IndexDefinition> indexes;
    private List<ConstraintDefinition> constraints;
    
    // Data validation
    private Boolean validateData = true;
    private Boolean strictMode = false;
    private Boolean allowNulls = true;
    private Boolean truncateStrings = false;
    private Integer maxStringLength = 255;
    
    // Schema evolution
    private Boolean enableEvolution = false;
    private String evolutionStrategy = "additive"; // additive, breaking, custom
    private Boolean backupBeforeEvolution = true;
    private String backupTableSuffix = "_backup";
    
    // Custom schema management
    private String customSchemaManager;
    private Map<String, Object> customManagerProperties;
    
    /**
     * Column definition for schema management.
     */
    public static class ColumnDefinition {
        private String name;
        private String type;
        private Integer length;
        private Integer precision;
        private Integer scale;
        private Boolean nullable = true;
        private Object defaultValue;
        private Boolean primaryKey = false;
        private Boolean unique = false;
        private String comment;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Integer getLength() { return length; }
        public void setLength(Integer length) { this.length = length; }
        
        public Integer getPrecision() { return precision; }
        public void setPrecision(Integer precision) { this.precision = precision; }
        
        public Integer getScale() { return scale; }
        public void setScale(Integer scale) { this.scale = scale; }
        
        public Boolean getNullable() { return nullable; }
        public void setNullable(Boolean nullable) { this.nullable = nullable; }
        
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        
        public Boolean getPrimaryKey() { return primaryKey; }
        public void setPrimaryKey(Boolean primaryKey) { this.primaryKey = primaryKey; }
        
        public Boolean getUnique() { return unique; }
        public void setUnique(Boolean unique) { this.unique = unique; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        
        public ColumnDefinition copy() {
            ColumnDefinition copy = new ColumnDefinition();
            copy.name = this.name;
            copy.type = this.type;
            copy.length = this.length;
            copy.precision = this.precision;
            copy.scale = this.scale;
            copy.nullable = this.nullable;
            copy.defaultValue = this.defaultValue;
            copy.primaryKey = this.primaryKey;
            copy.unique = this.unique;
            copy.comment = this.comment;
            return copy;
        }
    }
    
    /**
     * Index definition for schema management.
     */
    public static class IndexDefinition {
        private String name;
        private List<String> columns;
        private Boolean unique = false;
        private String type = "BTREE";
        private String comment;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        
        public Boolean getUnique() { return unique; }
        public void setUnique(Boolean unique) { this.unique = unique; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        
        public IndexDefinition copy() {
            IndexDefinition copy = new IndexDefinition();
            copy.name = this.name;
            copy.columns = this.columns != null ? List.copyOf(this.columns) : null;
            copy.unique = this.unique;
            copy.type = this.type;
            copy.comment = this.comment;
            return copy;
        }
    }
    
    /**
     * Constraint definition for schema management.
     */
    public static class ConstraintDefinition {
        private String name;
        private String type; // PRIMARY_KEY, FOREIGN_KEY, UNIQUE, CHECK
        private List<String> columns;
        private String referencedTable;
        private List<String> referencedColumns;
        private String checkExpression;
        private String onDelete = "NO_ACTION";
        private String onUpdate = "NO_ACTION";
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        
        public String getReferencedTable() { return referencedTable; }
        public void setReferencedTable(String referencedTable) { this.referencedTable = referencedTable; }
        
        public List<String> getReferencedColumns() { return referencedColumns; }
        public void setReferencedColumns(List<String> referencedColumns) { this.referencedColumns = referencedColumns; }
        
        public String getCheckExpression() { return checkExpression; }
        public void setCheckExpression(String checkExpression) { this.checkExpression = checkExpression; }
        
        public String getOnDelete() { return onDelete; }
        public void setOnDelete(String onDelete) { this.onDelete = onDelete; }
        
        public String getOnUpdate() { return onUpdate; }
        public void setOnUpdate(String onUpdate) { this.onUpdate = onUpdate; }
        
        public ConstraintDefinition copy() {
            ConstraintDefinition copy = new ConstraintDefinition();
            copy.name = this.name;
            copy.type = this.type;
            copy.columns = this.columns != null ? List.copyOf(this.columns) : null;
            copy.referencedTable = this.referencedTable;
            copy.referencedColumns = this.referencedColumns != null ? List.copyOf(this.referencedColumns) : null;
            copy.checkExpression = this.checkExpression;
            copy.onDelete = this.onDelete;
            copy.onUpdate = this.onUpdate;
            return copy;
        }
    }
    
    /**
     * Default constructor.
     */
    public SchemaConfig() {
        this.columns = new HashMap<>();
        this.customManagerProperties = new HashMap<>();
    }
    
    // Getters and setters
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getStrategy() {
        return strategy;
    }
    
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    public SchemaStrategy getSchemaStrategy() {
        return SchemaStrategy.fromCode(strategy);
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getCatalogName() {
        return catalogName;
    }
    
    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }
    
    public Boolean getAutoCreate() {
        return autoCreate;
    }
    
    public void setAutoCreate(Boolean autoCreate) {
        this.autoCreate = autoCreate;
    }
    
    public Boolean getAutoUpdate() {
        return autoUpdate;
    }
    
    public void setAutoUpdate(Boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }
    
    public Boolean getDropIfExists() {
        return dropIfExists;
    }
    
    public void setDropIfExists(Boolean dropIfExists) {
        this.dropIfExists = dropIfExists;
    }
    
    public String getInitScript() {
        return initScript;
    }
    
    public void setInitScript(String initScript) {
        this.initScript = initScript;
    }
    
    public List<String> getInitScripts() {
        return initScripts;
    }
    
    public void setInitScripts(List<String> initScripts) {
        this.initScripts = initScripts;
    }
    
    public Map<String, ColumnDefinition> getColumns() {
        return columns;
    }
    
    public void setColumns(Map<String, ColumnDefinition> columns) {
        this.columns = columns != null ? columns : new HashMap<>();
    }
    
    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }
    
    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }
    
    public List<IndexDefinition> getIndexes() {
        return indexes;
    }
    
    public void setIndexes(List<IndexDefinition> indexes) {
        this.indexes = indexes;
    }
    
    public List<ConstraintDefinition> getConstraints() {
        return constraints;
    }
    
    public void setConstraints(List<ConstraintDefinition> constraints) {
        this.constraints = constraints;
    }
    
    public Boolean getValidateData() {
        return validateData;
    }
    
    public void setValidateData(Boolean validateData) {
        this.validateData = validateData;
    }
    
    public Boolean getStrictMode() {
        return strictMode;
    }
    
    public void setStrictMode(Boolean strictMode) {
        this.strictMode = strictMode;
    }
    
    public Boolean getAllowNulls() {
        return allowNulls;
    }
    
    public void setAllowNulls(Boolean allowNulls) {
        this.allowNulls = allowNulls;
    }
    
    public Boolean getTruncateStrings() {
        return truncateStrings;
    }
    
    public void setTruncateStrings(Boolean truncateStrings) {
        this.truncateStrings = truncateStrings;
    }
    
    public Integer getMaxStringLength() {
        return maxStringLength;
    }
    
    public void setMaxStringLength(Integer maxStringLength) {
        this.maxStringLength = maxStringLength;
    }
    
    /**
     * Add a column definition.
     * 
     * @param name The column name
     * @param definition The column definition
     */
    public void addColumn(String name, ColumnDefinition definition) {
        if (columns == null) {
            columns = new HashMap<>();
        }
        columns.put(name, definition);
    }
    
    /**
     * Get the full table name including schema and catalog.
     * 
     * @return The full table name
     */
    public String getFullTableName() {
        StringBuilder sb = new StringBuilder();
        
        if (catalogName != null && !catalogName.trim().isEmpty()) {
            sb.append(catalogName).append(".");
        }
        
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            sb.append(schemaName).append(".");
        }
        
        if (tableName != null && !tableName.trim().isEmpty()) {
            sb.append(tableName);
        }
        
        return sb.toString();
    }
    
    /**
     * Validate the schema configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (strategy == null || strategy.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema strategy is required");
        }
        
        SchemaStrategy schemaStrategy = getSchemaStrategy();
        if (schemaStrategy == null) {
            throw new IllegalArgumentException("Invalid schema strategy: " + strategy);
        }
        
        if (autoCreate && (tableName == null || tableName.trim().isEmpty())) {
            throw new IllegalArgumentException("Table name is required when auto-create is enabled");
        }
        
        if (maxStringLength != null && maxStringLength <= 0) {
            throw new IllegalArgumentException("Max string length must be positive");
        }
    }
    
    /**
     * Create a copy of this configuration.
     * 
     * @return A new SchemaConfig with the same settings
     */
    public SchemaConfig copy() {
        SchemaConfig copy = new SchemaConfig();
        copy.enabled = this.enabled;
        copy.strategy = this.strategy;
        copy.schemaName = this.schemaName;
        copy.tableName = this.tableName;
        copy.catalogName = this.catalogName;
        copy.autoCreate = this.autoCreate;
        copy.autoUpdate = this.autoUpdate;
        copy.dropIfExists = this.dropIfExists;
        copy.initScript = this.initScript;
        copy.initScripts = this.initScripts != null ? List.copyOf(this.initScripts) : null;
        
        // Deep copy columns
        copy.columns = new HashMap<>();
        if (this.columns != null) {
            for (Map.Entry<String, ColumnDefinition> entry : this.columns.entrySet()) {
                copy.columns.put(entry.getKey(), entry.getValue().copy());
            }
        }
        
        copy.primaryKeys = this.primaryKeys != null ? List.copyOf(this.primaryKeys) : null;
        
        // Deep copy indexes
        if (this.indexes != null) {
            copy.indexes = this.indexes.stream().map(IndexDefinition::copy).toList();
        }
        
        // Deep copy constraints
        if (this.constraints != null) {
            copy.constraints = this.constraints.stream().map(ConstraintDefinition::copy).toList();
        }
        
        copy.validateData = this.validateData;
        copy.strictMode = this.strictMode;
        copy.allowNulls = this.allowNulls;
        copy.truncateStrings = this.truncateStrings;
        copy.maxStringLength = this.maxStringLength;
        copy.enableEvolution = this.enableEvolution;
        copy.evolutionStrategy = this.evolutionStrategy;
        copy.backupBeforeEvolution = this.backupBeforeEvolution;
        copy.backupTableSuffix = this.backupTableSuffix;
        copy.customSchemaManager = this.customSchemaManager;
        copy.customManagerProperties = new HashMap<>(this.customManagerProperties);
        
        return copy;
    }
}
