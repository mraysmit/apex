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

/**
 * Information about a data source in the schema comparison.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataSourceInfo {

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("connection")
    private ConnectionInfo connection;

    @JsonProperty("tableMetadata")
    private TableMetadata tableMetadata;

    public DataSourceInfo() {
    }

    public DataSourceInfo(String name, String type, ConnectionInfo connection, TableMetadata tableMetadata) {
        this.name = name;
        this.type = type;
        this.connection = connection;
        this.tableMetadata = tableMetadata;
    }

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

    public ConnectionInfo getConnection() {
        return connection;
    }

    public void setConnection(ConnectionInfo connection) {
        this.connection = connection;
    }

    public TableMetadata getTableMetadata() {
        return tableMetadata;
    }

    public void setTableMetadata(TableMetadata tableMetadata) {
        this.tableMetadata = tableMetadata;
    }

    /**
     * Convenience method for tests that expect getTable().
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public TableMetadata getTable() {
        return getTableMetadata();
    }

    /**
     * Connection information for database sources.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConnectionInfo {
        @JsonProperty("host")
        private String host;

        @JsonProperty("port")
        private Integer port;

        @JsonProperty("database")
        private String database;

        @JsonProperty("schema")
        private String schema;

        @JsonProperty("username")
        private String username;

        @JsonProperty("filePath")
        private String filePath;  // For CSV/file sources

        public ConnectionInfo() {}

        public ConnectionInfo(String jdbcUrl, String schema, String filePath) {
            this.filePath = filePath;
            this.schema = schema;
            // jdbcUrl parsing could be added if needed
        }

        public ConnectionInfo(String host, Integer port, String database) {
            this.host = host;
            this.port = port;
            this.database = database;
        }

        public String getJdbcUrl() {
            // Reconstruct JDBC URL from components if needed
            if (host != null && database != null) {
                return "jdbc:postgresql://" + host + ":" + (port != null ? port : 5432) + "/" + database;
            }
            return null;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

    /**
     * Metadata about the table being compared.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TableMetadata {
        @JsonProperty("tableName")
        private String tableName;

        @JsonProperty("columns")
        private Integer columns;

        @JsonProperty("primaryKeys")
        private Integer primaryKeys;

        public TableMetadata() {}

        public TableMetadata(String tableName, String schema, Integer rowCount) {
            this.tableName = tableName;
            // schema and rowCount not fields - ignore
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public Integer getColumns() {
            return columns;
        }

        public void setColumns(Integer columns) {
            this.columns = columns;
        }

        public Integer getPrimaryKeys() {
            return primaryKeys;
        }

        public void setPrimaryKeys(Integer primaryKeys) {
            this.primaryKeys = primaryKeys;
        }
    }
}
