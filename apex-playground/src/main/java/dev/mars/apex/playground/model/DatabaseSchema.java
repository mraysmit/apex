/*
 * Copyright 2025 Mark Raysmith
 * Licensed under the Apache License, Version 2.0
 */
package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Model representing database schema metadata.
 * Used to introspect database structure and load field paths into Blockly.
 */
public class DatabaseSchema {
    
    @JsonProperty("database")
    private String database;
    
    @JsonProperty("tables")
    private List<TableInfo> tables;
    
    /**
     * Information about a database table
     */
    public static class TableInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("schema")
        private String schema;
        
        @JsonProperty("columns")
        private List<ColumnInfo> columns;
        
        // Default constructor
        public TableInfo() {
        }
        
        // Getters and Setters
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getSchema() {
            return schema;
        }
        
        public void setSchema(String schema) {
            this.schema = schema;
        }
        
        public List<ColumnInfo> getColumns() {
            return columns;
        }
        
        public void setColumns(List<ColumnInfo> columns) {
            this.columns = columns;
        }
        
        @Override
        public String toString() {
            return "TableInfo{" +
                    "name='" + name + '\'' +
                    ", schema='" + schema + '\'' +
                    ", columns=" + columns +
                    '}';
        }
    }
    
    /**
     * Information about a table column
     */
    public static class ColumnInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("type")
        private String type;
        
        @JsonProperty("nullable")
        private boolean nullable;
        
        @JsonProperty("primaryKey")
        private boolean primaryKey;
        
        // Default constructor
        public ColumnInfo() {
        }
        
        // Getters and Setters
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
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
        
        @Override
        public String toString() {
            return "ColumnInfo{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", nullable=" + nullable +
                    ", primaryKey=" + primaryKey +
                    '}';
        }
    }
    
    // Default constructor
    public DatabaseSchema() {
    }
    
    // Getters and Setters
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public List<TableInfo> getTables() {
        return tables;
    }
    
    public void setTables(List<TableInfo> tables) {
        this.tables = tables;
    }
    
    @Override
    public String toString() {
        return "DatabaseSchema{" +
                "database='" + database + '\'' +
                ", tables=" + tables +
                '}';
    }
}
