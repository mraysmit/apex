package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.config.datasource.ConnectionConfig;
import dev.mars.apex.core.service.data.external.ExternalDataSource;
import dev.mars.apex.core.service.data.external.factory.DataSourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Direct test of RestApiLookupService to isolate the issue.
 * Disabled: Requires external network connectivity to httpbin.org - flaky test
 */
@Disabled("Requires external network connectivity - flaky test")
class RestApiLookupServiceDirectTest {

    private RestApiLookupService lookupService;
    private ExternalDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        System.out.println("=== Setting up RestApiLookupService direct test ===");
        
        // Create data source configuration for httpbin.org
        DataSourceConfiguration config = new DataSourceConfiguration();
        config.setName("test-api");
        config.setType("rest-api");
        config.setSourceType("rest-api");
        config.setEnabled(true);
        
        ConnectionConfig connection = new ConnectionConfig();
        connection.setBaseUrl("https://httpbin.org");
        connection.setTimeout(10000);
        config.setConnection(connection);
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("test-get", "/get?param={param}");
        config.setEndpoints(endpoints);
        
        // Create data source
        DataSourceFactory factory = DataSourceFactory.getInstance();
        dataSource = factory.createDataSource(config);
        
        System.out.println("Created data source: " + dataSource.getClass().getName());
        
        // Create RestApiLookupService with correct constructor parameters
        String name = "test-lookup";
        String endpoint = "test-get";
        ArrayList<String> parameterFields = new ArrayList<>();
        parameterFields.add("param");
        Map<String, Object> defaultValues = new HashMap<>();

        lookupService = new RestApiLookupService(name, dataSource, endpoint, parameterFields, defaultValues);
        
        System.out.println("Created RestApiLookupService: " + lookupService.getClass().getName());
        System.out.println("✓ Setup completed successfully");
    }

    @Test
    void testDirectTransform() throws Exception {
        System.out.println("=== Testing direct transform call ===");
        
        String key = "test-value";
        System.out.println("Calling transform with key: " + key);
        
        // Call transform directly
        Object result = lookupService.transform(key);
        
        System.out.println("Transform returned: " + result);
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
        
        System.out.println("✓ Direct transform test passed!");
    }
}
