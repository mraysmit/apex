package dev.mars.apex.rest.controller;

import dev.mars.apex.core.service.data.DataServiceManager;
import dev.mars.apex.core.service.data.DataSource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;

/**
 * REST Controller for data source management operations.
 * Provides endpoints for managing external data sources used by the APEX rules engine.
 */
@RestController
@RequestMapping("/api/datasources")
@Tag(name = "Data Sources", description = "Data source management operations")
public class DataSourceController {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceController.class);

    @Autowired
    private DataServiceManager dataServiceManager;

    /**
     * Get all registered data sources.
     */
    @GetMapping
    @Operation(
        summary = "Get all registered data sources",
        description = "Returns a list of all registered data sources available for use in rules."
    )
    @ApiResponse(responseCode = "200", description = "Data sources retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAllDataSources() {
        logger.debug("Retrieving all registered data sources");

        try {
            // Get all registered data sources
            String[] dataSourceNames = dataServiceManager.getRegisteredDataSources();

            List<Map<String, Object>> dataSources = new ArrayList<>();
            for (String name : dataSourceNames) {
                DataSource dataSource = dataServiceManager.getDataSource(name);
                if (dataSource != null) {
                    Map<String, Object> dsInfo = new HashMap<>();
                    dsInfo.put("name", name);
                    dsInfo.put("type", dataSource.getClass().getSimpleName());
                    dsInfo.put("description", getDataSourceDescription(dataSource));
                    dsInfo.put("available", true);
                    dataSources.add(dsInfo);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("dataSources", dataSources);
            response.put("count", dataSources.size());
            response.put("timestamp", Instant.now());

            logger.debug("Retrieved {} data sources", dataSources.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving data sources: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve data sources");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a specific data source by name.
     */
    @GetMapping("/{name}")
    @Operation(
        summary = "Get data source by name",
        description = "Returns information about a specific data source."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Data source retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<Map<String, Object>> getDataSource(
            @Parameter(description = "Name of the data source", example = "customerLookup")
            @PathVariable @NotBlank String name) {

        logger.debug("Retrieving data source: {}", name);

        try {
            DataSource dataSource = dataServiceManager.getDataSource(name);
            if (dataSource == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Data source not found");
                errorResponse.put("name", name);
                errorResponse.put("timestamp", Instant.now());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("name", name);
            response.put("type", dataSource.getClass().getSimpleName());
            response.put("description", getDataSourceDescription(dataSource));
            response.put("available", true);
            response.put("timestamp", Instant.now());

            logger.debug("Retrieved data source: {}", name);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving data source '{}': {}", name, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve data source");
            errorResponse.put("name", name);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Test a data source connection.
     */
    @PostMapping("/{name}/test")
    @Operation(
        summary = "Test data source connection",
        description = "Tests the connection and functionality of a data source."
    )
    @ApiResponse(responseCode = "200", description = "Data source test completed")
    public ResponseEntity<Map<String, Object>> testDataSource(
            @Parameter(description = "Name of the data source to test")
            @PathVariable @NotBlank String name,

            @RequestBody(required = false)
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Test parameters (optional)",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Customer lookup test",
                        value = """
                        {
                          "testKey": "CUST001",
                          "expectedFields": ["customerName", "customerTier", "riskRating"]
                        }
                        """
                    )
                )
            )
            Map<String, Object> testParams) {

        logger.info("Testing data source: {}", name);

        try {
            DataSource dataSource = dataServiceManager.getDataSource(name);
            if (dataSource == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Data source not found");
                errorResponse.put("name", name);
                errorResponse.put("timestamp", Instant.now());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            // Perform test lookup
            String testKey = testParams != null ? (String) testParams.get("testKey") : "TEST_KEY";
            long startTime = System.currentTimeMillis();
            
            try {
                Object testResult = dataSource.lookup(testKey);
                long responseTime = System.currentTimeMillis() - startTime;

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("name", name);
                response.put("testKey", testKey);
                response.put("testResult", testResult);
                response.put("responseTimeMs", responseTime);
                response.put("available", true);
                response.put("timestamp", Instant.now());

                logger.info("Data source test successful: {} ({}ms)", name, responseTime);
                return ResponseEntity.ok(response);

            } catch (Exception testException) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("name", name);
                response.put("testKey", testKey);
                response.put("error", "Test lookup failed");
                response.put("message", testException.getMessage());
                response.put("responseTimeMs", responseTime);
                response.put("available", false);
                response.put("timestamp", Instant.now());

                logger.warn("Data source test failed: {} - {}", name, testException.getMessage());
                return ResponseEntity.ok(response); // Return 200 with failure details
            }

        } catch (Exception e) {
            logger.error("Error testing data source '{}': {}", name, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to test data source");
            errorResponse.put("name", name);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Perform a lookup using a data source.
     */
    @PostMapping("/{name}/lookup")
    @Operation(
        summary = "Perform data source lookup",
        description = "Performs a lookup operation using the specified data source."
    )
    @ApiResponse(responseCode = "200", description = "Lookup completed successfully")
    public ResponseEntity<Map<String, Object>> performLookup(
            @Parameter(description = "Name of the data source to use")
            @PathVariable @NotBlank String name,

            @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Lookup request",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Customer lookup",
                        value = """
                        {
                          "key": "CUST001"
                        }
                        """
                    )
                )
            )
            @Valid @NotNull LookupRequest request) {

        logger.info("Performing lookup on data source '{}' with key: {}", name, request.getKey());

        try {
            DataSource dataSource = dataServiceManager.getDataSource(name);
            if (dataSource == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Data source not found");
                errorResponse.put("name", name);
                errorResponse.put("timestamp", Instant.now());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }

            long startTime = System.currentTimeMillis();
            Object result = dataSource.lookup(request.getKey());
            long responseTime = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("dataSource", name);
            response.put("key", request.getKey());
            response.put("result", result);
            response.put("responseTimeMs", responseTime);
            response.put("timestamp", Instant.now());

            logger.info("Lookup completed successfully: {} ({}ms)", name, responseTime);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error performing lookup on '{}': {}", name, e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Lookup failed");
            errorResponse.put("dataSource", name);
            errorResponse.put("key", request.getKey());
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", Instant.now());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get a description for a data source (helper method).
     */
    private String getDataSourceDescription(DataSource dataSource) {
        String className = dataSource.getClass().getSimpleName();
        switch (className) {
            case "MockDataSource":
                return "Mock data source for testing and development";
            case "DatabaseDataSource":
                return "Database-backed data source";
            case "RestApiDataSource":
                return "REST API-backed data source";
            case "FileDataSource":
                return "File-based data source";
            default:
                return "Custom data source: " + className;
        }
    }

    // DTOs for request/response
    public static class LookupRequest {
        @NotBlank
        private String key;

        // Getters and setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }
}
