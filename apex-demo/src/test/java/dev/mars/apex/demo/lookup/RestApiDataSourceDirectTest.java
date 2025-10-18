package dev.mars.apex.demo.lookup;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.service.data.external.DataSourceException;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct test of RestApiDataSource to isolate the issue.
 */
public class RestApiDataSourceDirectTest {

    private ExternalDataSource dataSource;

    @BeforeEach
    void setUp() throws DataSourceException {
        System.out.println("=== Setting up RestApiDataSource direct test ===");

        // Create configuration
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-api");
        config.setType("rest-api");
        config.setSourceType("rest-api");
        config.setEnabled(true);

        // Set connection details
        ConnectionConfig connection = new ConnectionConfig();
        connection.setBaseUrl("https://httpbin.org");
        connection.setTimeout(10000);
        config.setConnection(connection);

        // Set endpoints
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("test-get", "/get?param={param}");
        config.setEndpoints(endpoints);

        // Create data source using factory
        DataSourceFactory factory = DataSourceFactory.getInstance();
        dataSource = factory.createDataSource(config);

        System.out.println("RestApiDataSource initialized successfully");
    }

    @Test
    void testDirectQueryForObject() throws DataSourceException {
        System.out.println("=== Testing direct queryForObject call ===");

        // Create parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param", "test-value");

        System.out.println("Calling queryForObject with query='test-get', parameters=" + parameters);

        // Call queryForObject directly
        Object result = dataSource.queryForObject("test-get", parameters);

        System.out.println("queryForObject returned: " + result);
        System.out.println("Result type: " + (result != null ? result.getClass().getName() : "null"));
        System.out.println("Is result instanceof Map? " + (result instanceof Map));

        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            System.out.println("Result map size: " + resultMap.size());
            System.out.println("Result map keys: " + resultMap.keySet());
            for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue() + " (" +
                                 (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null") + ")");
            }
        }

        // Basic assertions
        assertNotNull(result, "Result should not be null");
        assertTrue(result instanceof Map, "Result should be a Map");

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;

        System.out.println("Result map keys: " + resultMap.keySet());
        System.out.println("Result map: " + resultMap);

        System.out.println("✓ Direct queryForObject test passed!");
    }

    @Test
    void testSimpleJsonResponse() throws DataSourceException {
        System.out.println("=== Testing simple JSON response ===");

        // Test with a simple endpoint that returns known JSON
        // httpbin.org/json returns a simple JSON object

        // Update configuration for JSON endpoint
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("json-test-api");
        config.setType("rest-api");
        config.setSourceType("rest-api");
        config.setEnabled(true);

        ConnectionConfig connection = new ConnectionConfig();
        connection.setBaseUrl("https://httpbin.org");
        connection.setTimeout(10000);
        config.setConnection(connection);

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("json-test", "/json");
        config.setEndpoints(endpoints);

        // Create new data source
        DataSourceFactory factory = DataSourceFactory.getInstance();
        ExternalDataSource jsonDataSource = factory.createDataSource(config);

        System.out.println("Created JSON test data source");

        // Call with no parameters
        Object result = jsonDataSource.queryForObject("json-test", new HashMap<>());

        System.out.println("JSON test result: " + result);
        System.out.println("JSON test result type: " + (result != null ? result.getClass().getName() : "null"));
        System.out.println("Is JSON result instanceof Map? " + (result instanceof Map));

        if (result instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) result;
            System.out.println("JSON result map size: " + resultMap.size());
            System.out.println("JSON result map keys: " + resultMap.keySet());
        }

        // Basic assertions
        assertNotNull(result, "JSON result should not be null");
        assertTrue(result instanceof Map, "JSON result should be a Map");

        System.out.println("✓ Simple JSON response test passed!");
    }
}

