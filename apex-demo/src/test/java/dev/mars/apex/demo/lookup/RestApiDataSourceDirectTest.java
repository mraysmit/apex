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
    private com.sun.net.httpserver.HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws DataSourceException {
        System.out.println("=== Setting up RestApiDataSource direct test ===");

        // Start a lightweight local HTTP server to avoid flaky external dependencies
        try {
            server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();

            // /get endpoint echoes query param as JSON
            server.createContext("/get", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String paramValue = "";
                if (query != null) {
                    for (String kv : query.split("&")) {
                        String[] parts = kv.split("=", 2);
                        if (parts.length == 2 && parts[0].equals("param")) {
                            paramValue = java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                String body = "{\"args\": {\"param\": \"" + paramValue + "\"}}";
                byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            // /json endpoint returns a static JSON document
            server.createContext("/json", exchange -> {
                String body = "{\"slideshow\": {\"title\": \"Sample Slide Show\"}}";
                byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            server.start();
            System.out.println("Local test HTTP server started on port " + port);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to start local HTTP server for tests", e);
        }

        // Create configuration
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-api");
        config.setType("rest-api");
        config.setSourceType("rest-api");
        config.setEnabled(true);

        // Set connection details
        ConnectionConfig connection = new ConnectionConfig();
        connection.setBaseUrl("http://127.0.0.1:" + port);
        connection.setTimeout(10000);
        config.setConnection(connection);

        // Set endpoints
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("test-get", "/get?param={param}");
        endpoints.put("json-test", "/json");
        config.setEndpoints(endpoints);

        // Create data source using factory
        DataSourceFactory factory = DataSourceFactory.getInstance();
        dataSource = factory.createDataSource(config);

        System.out.println("RestApiDataSource initialized successfully");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
            System.out.println("Local test HTTP server stopped");
        }
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

        // Test with a simple endpoint that returns known JSON via local test server

        // Update configuration for JSON endpoint
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("json-test-api");
        config.setType("rest-api");
        config.setSourceType("rest-api");
        config.setEnabled(true);

        ConnectionConfig connection = new ConnectionConfig();
        connection.setBaseUrl("http://127.0.0.1:" + port);
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

