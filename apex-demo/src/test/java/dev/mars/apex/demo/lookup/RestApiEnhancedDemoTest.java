/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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
 */
package dev.mars.apex.demo.lookup;

import com.sun.net.httpserver.HttpServer;
import dev.mars.apex.demo.infrastructure.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Enhanced Demo Test - YAML First Approach
 *
 * DEMONSTRATES:
 * - Enhanced REST API lookup functionality using APEX enrichments
 * - Currency rates, market data, and metrics lookup through REST API endpoints
 * - YAML-driven REST API connectivity and JSON processing
 *
 * BUSINESS LOGIC VALIDATION:
 * - REST API lookup enrichments with multiple endpoints
 * - Advanced JSON processing and field mapping through YAML
 * - YAML-driven REST API connectivity validation
 *
 * YAML FIRST PRINCIPLE:
 * - ALL business logic is in YAML enrichments
 * - Java test only sets up minimal HTTP server, loads YAML and calls APEX
 * - NO direct HTTP client calls or JSON processing logic
 * - Simple server setup and basic assertions only
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 2.0.0 (Renamed with RestApi prefix for consistency)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RestApiEnhancedDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RestApiEnhancedDemoTest.class);

    private static HttpServer httpServer;
    private static String baseUrl;
    private static int serverPort;
    private static final AtomicInteger requestCounter = new AtomicInteger(0);

    @BeforeAll
    static void setupEnhancedRestApiTestSuite() throws Exception {
        logger.info("================================================================================");
        logger.info("ENHANCED REST API DEMO TEST SUITE - YAML FIRST APPROACH");
        logger.info("================================================================================");

        logger.info("🔧 Setting up enhanced HTTP server for REST API testing...");

        // Create HTTP server on available port
        httpServer = HttpServer.create(new java.net.InetSocketAddress(0), 0);
        serverPort = httpServer.getAddress().getPort();
        baseUrl = "http://localhost:" + serverPort;

        // Setup enhanced REST API endpoints
        setupCurrencyRateEndpoint();
        setupMarketDataEndpoint();
        setupMetricsEndpoint();
        setupHealthCheckEndpoint();
        setupBatchEndpoint();

        // Start the server
        httpServer.start();

        logger.info("✅ Enhanced HTTP server started successfully:");
        logger.info("  Base URL: {}", baseUrl);
        logger.info("  Currency Rate Endpoint: /api/currency/{currencyCode}");
        logger.info("  Market Data Endpoint: /api/market/{symbol}");
        logger.info("  Metrics Endpoint: /api/metrics");
        logger.info("  Health Check Endpoint: /api/health");
        logger.info("  Batch Processing Endpoint: /api/batch");

        logger.info("✅ Enhanced REST API test suite setup completed successfully");
    }

    @AfterAll
    static void teardownEnhancedRestApiTestSuite() {
        if (httpServer != null) {
            logger.info("🛑 Stopping enhanced HTTP server...");
            httpServer.stop(0);
            logger.info("✅ Enhanced HTTP server stopped successfully");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Enhanced Currency Rate Lookup - YAML First")
    void testEnhancedCurrencyRateLookup() throws Exception {
        logger.info("=== Enhanced Currency Rate Lookup Test ===");

        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/lookup/RestApiEnhancedDemoTest.yaml";
        String tempYamlPath = updateYamlWithServerPort(yamlPath);

        var config = yamlLoader.loadFromFile(tempYamlPath);

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("currencyCode", "USD");
        testData.put("requestId", "REQ-001");

        logger.info("Input data: {}", testData);

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("US Dollar", enrichedData.get("currencyName"), "Currency name should be enriched");
        assertEquals("$", enrichedData.get("currencySymbol"), "Currency symbol should be enriched");
        assertEquals(1.0, enrichedData.get("exchangeRate"), "Exchange rate should be enriched");

        logger.info("✅ Enhanced currency rate lookup completed successfully");

        // Cleanup
        Files.deleteIfExists(Paths.get(tempYamlPath));
    }

    @Test
    @Order(2)
    @DisplayName("Enhanced Market Data Lookup - YAML First")
    void testEnhancedMarketDataLookup() throws Exception {
        logger.info("=== Enhanced Market Data Lookup Test ===");

        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/lookup/RestApiEnhancedDemoTest.yaml";
        String tempYamlPath = updateYamlWithServerPort(yamlPath);

        var config = yamlLoader.loadFromFile(tempYamlPath);

        // Test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("symbol", "AAPL");
        testData.put("requestId", "REQ-002");

        logger.info("Input data: {}", testData);

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("Apple Inc.", enrichedData.get("companyName"), "Company name should be enriched");
        assertNotNull(enrichedData.get("currentPrice"), "Current price should be enriched");
        assertNotNull(enrichedData.get("marketCap"), "Market cap should be enriched");

        logger.info("✅ Enhanced market data lookup completed successfully");

        // Cleanup
        Files.deleteIfExists(Paths.get(tempYamlPath));
    }

    @Test
    @Order(3)
    @DisplayName("Enhanced Metrics Collection - YAML First")
    void testEnhancedMetricsCollection() throws Exception {
        logger.info("=== Enhanced Metrics Collection Test ===");

        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/lookup/RestApiEnhancedDemoTest.yaml";
        String tempYamlPath = updateYamlWithServerPort(yamlPath);

        var config = yamlLoader.loadFromFile(tempYamlPath);

        // Test data - must match condition: #metricsRequest != null && #metricsRequest == true
        Map<String, Object> testData = new HashMap<>();
        testData.put("metricsRequest", true);
        testData.put("requestId", "REQ-003");

        logger.info("Input data: {}", testData);

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        assertNotNull(enrichedData, "Enriched data should not be null");
        assertNotNull(enrichedData.get("totalRequests"), "Total requests should be enriched");
        assertNotNull(enrichedData.get("averageResponseTime"), "Average response time should be enriched");
        assertNotNull(enrichedData.get("uptime"), "Uptime should be enriched");

        logger.info("✅ Enhanced metrics collection completed successfully");

        // Cleanup
        Files.deleteIfExists(Paths.get(tempYamlPath));
    }

    @Test
    @Order(4)
    @DisplayName("Enhanced Health Check - YAML First")
    void testEnhancedHealthCheck() throws Exception {
        logger.info("=== Enhanced Health Check Test ===");

        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/lookup/RestApiEnhancedDemoTest.yaml";
        String tempYamlPath = updateYamlWithServerPort(yamlPath);

        var config = yamlLoader.loadFromFile(tempYamlPath);

        // Test data - must match condition: #healthCheck != null && #healthCheck == true
        Map<String, Object> testData = new HashMap<>();
        testData.put("healthCheck", true);
        testData.put("requestId", "REQ-004");

        logger.info("Input data: {}", testData);

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals("UP", enrichedData.get("status"), "Health status should be UP");
        assertNotNull(enrichedData.get("timestamp"), "Timestamp should be enriched");
        assertEquals("Enhanced REST API Server", enrichedData.get("service"), "Service name should be enriched");

        logger.info("✅ Enhanced health check completed successfully");

        // Cleanup
        Files.deleteIfExists(Paths.get(tempYamlPath));
    }

    @Test
    @Order(5)
    @DisplayName("Enhanced Batch Processing - YAML First")
    void testEnhancedBatchProcessing() throws Exception {
        logger.info("=== Enhanced Batch Processing Test ===");

        // Load YAML configuration
        String yamlPath = "src/test/java/dev/mars/apex/demo/lookup/RestApiEnhancedDemoTest.yaml";
        String tempYamlPath = updateYamlWithServerPort(yamlPath);

        var config = yamlLoader.loadFromFile(tempYamlPath);

        // Test data - must match condition: #batchRequest != null && #batchRequest == true
        Map<String, Object> testData = new HashMap<>();
        testData.put("batchRequest", true);
        testData.put("batchSize", 5);
        testData.put("requestId", "REQ-005");

        logger.info("Input data: {}", testData);

        // Process with APEX
        Object result = enrichmentService.enrichObject(config, testData);

        // Assertions
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        assertNotNull(enrichedData, "Enriched data should not be null");
        assertEquals(5, enrichedData.get("processedCount"), "Processed count should match batch size");
        assertNotNull(enrichedData.get("processingTime"), "Processing time should be enriched");
        assertEquals("SUCCESS", enrichedData.get("batchStatus"), "Batch status should be SUCCESS");

        logger.info("✅ Enhanced batch processing completed successfully");

        // Cleanup
        Files.deleteIfExists(Paths.get(tempYamlPath));
    }

    // Helper methods for HTTP server setup
    private static void setupCurrencyRateEndpoint() {
        httpServer.createContext("/api/currency", exchange -> {
            requestCounter.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            String currencyCode = extractCurrencyCodeFromPath(path);

            String jsonResponse = createCurrencyRateResponse(currencyCode);
            sendJsonResponse(exchange, jsonResponse);
        });
    }

    private static void setupMarketDataEndpoint() {
        httpServer.createContext("/api/market", exchange -> {
            requestCounter.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            String symbol = extractSymbolFromPath(path);

            String jsonResponse = createMarketDataResponse(symbol);
            sendJsonResponse(exchange, jsonResponse);
        });
    }

    private static void setupMetricsEndpoint() {
        httpServer.createContext("/api/metrics", exchange -> {
            String jsonResponse = createMetricsResponse();
            sendJsonResponse(exchange, jsonResponse);
        });
    }

    private static void setupHealthCheckEndpoint() {
        httpServer.createContext("/api/health", exchange -> {
            String jsonResponse = createHealthCheckResponse();
            sendJsonResponse(exchange, jsonResponse);
        });
    }

    private static void setupBatchEndpoint() {
        httpServer.createContext("/api/batch", exchange -> {
            String jsonResponse = createBatchResponse();
            sendJsonResponse(exchange, jsonResponse);
        });
    }

    // Helper methods for path extraction
    private static String extractCurrencyCodeFromPath(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "USD";
    }

    private static String extractSymbolFromPath(String path) {
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : "AAPL";
    }

    // Response creation methods
    private static String createCurrencyRateResponse(String currencyCode) {
        return switch (currencyCode.toUpperCase()) {
            case "USD" -> """
                {
                    "code": "USD",
                    "name": "US Dollar",
                    "rate": 1.0,
                    "symbol": "$",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "EUR" -> """
                {
                    "code": "EUR",
                    "name": "Euro",
                    "rate": 0.85,
                    "symbol": "€",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            default -> """
                {
                    "code": "%s",
                    "name": "Unknown Currency",
                    "rate": 1.0,
                    "symbol": "?",
                    "lastUpdated": "%s"
                }
                """.formatted(currencyCode, java.time.Instant.now().toString());
        };
    }

    private static String createMarketDataResponse(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "AAPL" -> """
                {
                    "symbol": "AAPL",
                    "companyName": "Apple Inc.",
                    "currentPrice": 150.25,
                    "marketCap": "2.5T",
                    "volume": 50000000,
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "GOOGL" -> """
                {
                    "symbol": "GOOGL",
                    "companyName": "Alphabet Inc.",
                    "currentPrice": 2750.80,
                    "marketCap": "1.8T",
                    "volume": 25000000,
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            default -> """
                {
                    "symbol": "%s",
                    "companyName": "Unknown Company",
                    "currentPrice": 100.0,
                    "marketCap": "1B",
                    "volume": 1000000,
                    "lastUpdated": "%s"
                }
                """.formatted(symbol, java.time.Instant.now().toString());
        };
    }

    private static String createMetricsResponse() {
        return """
            {
                "totalRequests": %d,
                "averageResponseTime": 125.5,
                "uptime": "2h 15m 30s",
                "memoryUsage": "45.2MB",
                "cpuUsage": "12.8%%",
                "timestamp": "%s"
            }
            """.formatted(requestCounter.get(), java.time.Instant.now().toString());
    }

    private static String createHealthCheckResponse() {
        return """
            {
                "status": "UP",
                "timestamp": "%s",
                "service": "Enhanced REST API Server",
                "version": "2.0.0",
                "baseUrl": "%s"
            }
            """.formatted(java.time.Instant.now().toString(), baseUrl);
    }

    private static String createBatchResponse() {
        return """
            {
                "batchId": "BATCH-%d",
                "processedCount": 5,
                "processingTime": "250ms",
                "batchStatus": "SUCCESS",
                "timestamp": "%s"
            }
            """.formatted(System.currentTimeMillis() % 10000, java.time.Instant.now().toString());
    }

    // HTTP response helper methods
    private static void sendJsonResponse(com.sun.net.httpserver.HttpExchange exchange, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes();
        exchange.sendResponseHeaders(200, responseBytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    /**
     * Helper method to update YAML configuration with dynamic server port.
     * This allows APEX to connect to the test HTTP server.
     */
    private String updateYamlWithServerPort(String yamlFilePath) throws IOException {
        String yamlContent = Files.readString(Paths.get(yamlFilePath));

        // Replace placeholder port with actual server port
        yamlContent = yamlContent.replace("${PORT}", String.valueOf(serverPort));

        // Write to temporary file
        String tempYamlPath = yamlFilePath.replace(".yaml", "_temp_" + serverPort + ".yaml");
        Files.writeString(Paths.get(tempYamlPath), yamlContent);

        return tempYamlPath;
    }
}
