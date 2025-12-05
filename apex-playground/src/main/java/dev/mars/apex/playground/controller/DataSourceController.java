/*
 * Copyright 2025 Mark Raysmith
 * Licensed under the Apache License, Version 2.0
 */
package dev.mars.apex.playground.controller;

import dev.mars.apex.playground.model.DataSourceConnection;
import dev.mars.apex.playground.model.DatabaseSchema;
import dev.mars.apex.playground.model.QueryRequest;
import dev.mars.apex.playground.model.QueryResult;
import dev.mars.apex.playground.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for managing database connections and executing queries.
 * Provides endpoints for the Data Sources accordion section in the Visual Rule Editor.
 */
@RestController
@RequestMapping("/playground/api/datasources")
@Tag(name = "Data Sources API", description = "Manage database connections and execute queries")
public class DataSourceController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceController.class);
    
    private final DataSourceService dataSourceService;
    
    @Autowired
    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }
    
    /**
     * Get all database connections.
     */
    @GetMapping("/connections")
    @Operation(
        summary = "Get all database connections",
        description = "Retrieve a list of all configured database connections"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of connections retrieved successfully",
        content = @Content(schema = @Schema(implementation = DataSourceConnection.class))
    )
    public ResponseEntity<List<DataSourceConnection>> getAllConnections() {
        logger.debug("Getting all connections");
        List<DataSourceConnection> connections = dataSourceService.getAllConnections();
        return ResponseEntity.ok(connections);
    }
    
    /**
     * Get a specific connection by ID.
     */
    @GetMapping("/connections/{connectionId}")
    @Operation(
        summary = "Get a specific connection",
        description = "Retrieve details of a specific database connection by ID"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Connection retrieved successfully"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Connection not found"
    )
    public ResponseEntity<DataSourceConnection> getConnection(
            @Parameter(description = "Connection ID") @PathVariable String connectionId) {
        
        logger.debug("Getting connection: {}", connectionId);
        DataSourceConnection connection = dataSourceService.getConnection(connectionId);
        
        if (connection == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(connection);
    }
    
    /**
     * Create a new database connection.
     */
    @PostMapping("/connections")
    @Operation(
        summary = "Create a new database connection",
        description = "Create and test a new database connection with connection pooling"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Connection created successfully"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Failed to create connection"
    )
    public ResponseEntity<DataSourceConnection> createConnection(
            @RequestBody DataSourceConnection connection) {
        
        logger.info("Creating connection: {}", connection.getName());
        
        try {
            DataSourceConnection created = dataSourceService.createConnection(connection);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Failed to create connection: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Test a connection configuration without creating it.
     */
    @PostMapping("/test")
    @Operation(
        summary = "Test database connection",
        description = "Test a database connection configuration without persisting it"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Connection test completed"
    )
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestBody DataSourceConnection connection) {
        
        logger.info("Testing connection: {}", connection.getName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = dataSourceService.testConnection(connection);
            response.put("success", success);
            
            if (success) {
                response.put("message", "Connection successful");
                logger.info("Connection test successful for: {}", connection.getName());
            } else {
                response.put("message", "Connection failed");
                logger.warn("Connection test failed for: {}", connection.getName());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Connection test error for {}: {}", connection.getName(), e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Update an existing connection.
     */
    @PutMapping("/connections/{connectionId}")
    @Operation(
        summary = "Update a database connection",
        description = "Update an existing database connection configuration"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Connection updated successfully"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Connection not found"
    )
    public ResponseEntity<DataSourceConnection> updateConnection(
            @Parameter(description = "Connection ID") @PathVariable String connectionId,
            @RequestBody DataSourceConnection connection) {
        
        logger.info("Updating connection: {}", connectionId);
        
        try {
            DataSourceConnection updated = dataSourceService.updateConnection(connectionId, connection);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Failed to update connection {}: {}", connectionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a database connection.
     */
    @DeleteMapping("/connections/{connectionId}")
    @Operation(
        summary = "Delete a database connection",
        description = "Delete a database connection and close its connection pool"
    )
    @ApiResponse(
        responseCode = "204",
        description = "Connection deleted successfully"
    )
    public ResponseEntity<Void> deleteConnection(
            @Parameter(description = "Connection ID") @PathVariable String connectionId) {
        
        logger.info("Deleting connection: {}", connectionId);
        dataSourceService.deleteConnection(connectionId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Execute a SQL query.
     */
    @PostMapping("/connections/{connectionId}/query")
    @Operation(
        summary = "Execute SQL query",
        description = "Execute a SQL query on the specified database connection"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Query executed successfully"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Query execution failed"
    )
    public ResponseEntity<QueryResult> executeQuery(
            @Parameter(description = "Connection ID") @PathVariable String connectionId,
            @RequestBody QueryRequest request) {
        
        logger.info("Executing query on connection {}: {}", connectionId, 
            request.getSql().substring(0, Math.min(50, request.getSql().length())));
        
        try {
            QueryResult result = dataSourceService.executeQuery(connectionId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Query execution failed on connection {}: {}", connectionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get database schema metadata.
     */
    @GetMapping("/connections/{connectionId}/schema")
    @Operation(
        summary = "Get database schema",
        description = "Retrieve database schema metadata including tables and columns"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Schema retrieved successfully"
    )
    @ApiResponse(
        responseCode = "500",
        description = "Schema introspection failed"
    )
    public ResponseEntity<DatabaseSchema> getSchema(
            @Parameter(description = "Connection ID") @PathVariable String connectionId) {
        
        logger.info("Getting schema for connection: {}", connectionId);
        
        try {
            DatabaseSchema schema = dataSourceService.getSchema(connectionId);
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            logger.error("Schema introspection failed for connection {}: {}", connectionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
