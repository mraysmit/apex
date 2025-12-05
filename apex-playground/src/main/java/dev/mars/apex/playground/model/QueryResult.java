/*
 * Copyright 2025 Mark Raysmith
 * Licensed under the Apache License, Version 2.0
 */
package dev.mars.apex.playground.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response model containing SQL query results.
 */
public class QueryResult {
    
    @JsonProperty("columns")
    private List<String> columns;
    
    @JsonProperty("rows")
    private List<List<Object>> rows;
    
    @JsonProperty("rowCount")
    private int rowCount;
    
    @JsonProperty("hasMore")
    private boolean hasMore;
    
    @JsonProperty("executionTimeMs")
    private long executionTimeMs;
    
    // Default constructor
    public QueryResult() {
    }
    
    // Getters and Setters
    
    public List<String> getColumns() {
        return columns;
    }
    
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    
    public List<List<Object>> getRows() {
        return rows;
    }
    
    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }
    
    public int getRowCount() {
        return rowCount;
    }
    
    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }
    
    public boolean isHasMore() {
        return hasMore;
    }
    
    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    @Override
    public String toString() {
        return "QueryResult{" +
                "columns=" + columns +
                ", rowCount=" + rowCount +
                ", hasMore=" + hasMore +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}
