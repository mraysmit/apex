/*
 * Copyright 2025 Mark Raysmith
 * Licensed under the Apache License, Version 2.0
 */
package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

/**
 * Model representing a database connection configuration.
 * Used by the Data Sources accordion section in the Visual Rule Editor.
 */
public class DataSourceConnection {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("type")
    private DatabaseType type;
    
    @JsonProperty("host")
    private String host;
    
    @JsonProperty("port")
    private int port;
    
    @JsonProperty("database")
    private String database;

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("username")
    private String username;
    
    @JsonProperty("password")
    private String password; // Encrypted in production
    
    @JsonProperty("properties")
    private Map<String, String> properties;
    
    @JsonProperty("connected")
    private boolean connected;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("lastUsed")
    private Instant lastUsed;
    
    /**
     * Supported database types
     */
    public enum DatabaseType {
        POSTGRESQL, MYSQL, ORACLE, SQLSERVER, H2
    }
    
    // Default constructor
    public DataSourceConnection() {
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public DatabaseType getType() {
        return type;
    }
    
    public void setType(DatabaseType type) {
        this.type = type;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    @Override
    public String toString() {
        return "DataSourceConnection{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", schema='" + schema + '\'' +
                ", username='" + username + '\'' +
                ", connected=" + connected +
                ", createdAt=" + createdAt +
                ", lastUsed=" + lastUsed +
                '}';
    }
}
