/*
 * Copyright 2025 Mark Raysmith
 * Licensed under the Apache License, Version 2.0
 */
package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for executing SQL queries.
 */
public class QueryRequest {
    
    @JsonProperty("sql")
    private String sql;
    
    @JsonProperty("limit")
    private int limit = 100;
    
    @JsonProperty("offset")
    private int offset = 0;
    
    // Default constructor
    public QueryRequest() {
    }
    
    public QueryRequest(String sql) {
        this.sql = sql;
    }
    
    public QueryRequest(String sql, int limit, int offset) {
        this.sql = sql;
        this.limit = limit;
        this.offset = offset;
    }
    
    // Getters and Setters
    
    public String getSql() {
        return sql;
    }
    
    public void setSql(String sql) {
        this.sql = sql;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    @Override
    public String toString() {
        return "QueryRequest{" +
                "sql='" + sql + '\'' +
                ", limit=" + limit +
                ", offset=" + offset +
                '}';
    }
}
