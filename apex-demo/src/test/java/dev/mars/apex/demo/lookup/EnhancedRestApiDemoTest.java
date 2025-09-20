package dev.mars.apex.demo.lookup;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enhanced REST API Demo Test - YAML First Approach
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
 * @version 2.0.0 - Converted to YAML First approach
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnhancedRestApiDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedRestApiDemoTest.class);

    private static HttpServer httpServer;
    private static int serverPort;
    private static AtomicInteger requestCounter = new AtomicInteger(0);
    private static AtomicInteger errorCounter = new AtomicInteger(0);

    /**
     * Setup minimal HTTP server for REST API testing.
     * This is infrastructure setup, not business logic - business logic is in YAML.
     */
    @BeforeAll
    static void setupEnhancedRestApiTestSuite() throws IOException {
        logger.info("Setting up HTTP server for enhanced REST API demo...");

        // Setup enhanced JDK HTTP Server with multiple endpoints (infrastructure only)
        setupEnhancedHttpServer();

        logger.info("✓ Enhanced REST API test suite setup completed successfully");
    }

    @AfterAll
    static void teardownEnhancedRestApiTestSuite() {
        if (httpServer != null) {
            logger.info("Stopping enhanced HTTP server...");
            httpServer.stop(0);
            logger.info("✓ Enhanced HTTP server stopped successfully");
        }
    }

    private static void setupEnhancedHttpServer() throws IOException {
        logger.info("Setting up enhanced JDK HTTP server for REST API testing...");

        // Create HTTP server on available port
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = httpServer.getAddress().getPort();

        // Setup multiple enhanced endpoints (infrastructure only)
        setupCurrencyRatesEndpoint();
        setupMarketDataEndpoint();
        setupErrorSimulationEndpoint();
        setupMetricsEndpoint();
        setupBatchProcessingEndpoint();

        // Start the server
        httpServer.start();

        logger.info("✓ Enhanced JDK HTTP server started successfully:");
        logger.info("  Server URL: http://localhost:{}", serverPort);
        logger.info("  Currency Rates Endpoint: /api/v2/currency/rates");
        logger.info("  Market Data Endpoint: /api/v2/market/data");
        logger.info("  Error Simulation Endpoint: /api/v2/test/errors");
        logger.info("  Metrics Endpoint: /api/v2/metrics");
        logger.info("  Batch Processing Endpoint: /api/v2/batch/process");
    }

    private static void setupCurrencyRatesEndpoint() {
        httpServer.createContext("/api/v2/currency/rates", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                requestCounter.incrementAndGet();
                logger.info("📡 Handling enhanced currency rates request: {}", exchange.getRequestURI());

                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                String jsonResponse = createEnhancedCurrencyRatesResponse();
                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    private static void setupMarketDataEndpoint() {
        httpServer.createContext("/api/v2/market/data", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                requestCounter.incrementAndGet();
                logger.info("📡 Handling market data request: {}", exchange.getRequestURI());

                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParameters(query);
                String symbol = params.getOrDefault("symbol", "DEFAULT");

                String jsonResponse = createMarketDataResponse(symbol);
                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    private static void setupErrorSimulationEndpoint() {
        httpServer.createContext("/api/v2/test/errors", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                requestCounter.incrementAndGet();
                logger.info("📡 Handling error simulation request: {}", exchange.getRequestURI());

                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParameters(query);
                String errorType = params.getOrDefault("type", "none");

                switch (errorType) {
                    case "timeout":
                        // Simulate timeout by delaying response
                        try {
                            Thread.sleep(6000); // 6 second delay
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        sendJsonResponse(exchange, 200, "{\"message\": \"Delayed response\"}");
                        break;
                    case "server_error":
                        errorCounter.incrementAndGet();
                        sendErrorResponse(exchange, 500, "Internal Server Error");
                        break;
                    case "not_found":
                        errorCounter.incrementAndGet();
                        sendErrorResponse(exchange, 404, "Resource Not Found");
                        break;
                    case "rate_limit":
                        errorCounter.incrementAndGet();
                        sendErrorResponse(exchange, 429, "Too Many Requests");
                        break;
                    default:
                        sendJsonResponse(exchange, 200, "{\"message\": \"No error simulation\"}");
                }
            }
        });
    }

    private static void setupMetricsEndpoint() {
        httpServer.createContext("/api/v2/metrics", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.info("📡 Handling metrics request: {}", exchange.getRequestURI());

                String jsonResponse = createMetricsResponse();
                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    private static void setupBatchProcessingEndpoint() {
        httpServer.createContext("/api/v2/batch/process", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                requestCounter.incrementAndGet();
                logger.info("📡 Handling batch processing request: {}", exchange.getRequestURI());

                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                // Simulate batch processing
                String jsonResponse = createBatchProcessingResponse();
                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    @Test
    @Order(1)
    @DisplayName("Should test enhanced currency rates lookup functionality")
    void testEnhancedCurrencyRatesLookup() {
        logger.info("=== Testing Enhanced Currency Rates Lookup Functionality ===");

        // Load YAML configuration for enhanced REST API demo
        try {
            // Update YAML configuration with dynamic server port
            String tempYamlPath = updateYamlWithServerPort("src/test/java/dev/mars/apex/demo/lookup/EnhancedRestApiDemoTest.yaml");
            var config = yamlLoader.loadFromFile(tempYamlPath);
            assertNotNull(config, "YAML configuration should not be null");

            // Test data - currency rates lookup
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("lookupType", "currency_rates");

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, inputData);

            // Validate enrichment results
            assertNotNull(result, "Enhanced currency rates lookup result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate YAML-driven REST API lookup results
            assertNotNull(enrichedData.get("currencySource"), "Currency source should be retrieved from REST API");
            assertNotNull(enrichedData.get("apiVersion"), "API version should be retrieved from REST API");
            assertNotNull(enrichedData.get("totalRates"), "Total rates should be retrieved from REST API");
            assertNotNull(enrichedData.get("currencyRates"), "Currency rates should be retrieved from REST API");

            // Validate specific REST API lookup results
            assertEquals("APEX Enhanced Currency API", enrichedData.get("currencySource"), "Should retrieve correct currency source");
            assertEquals("2.0", enrichedData.get("apiVersion"), "Should retrieve correct API version");
            assertEquals(4, ((Number) enrichedData.get("totalRates")).intValue(), "Should retrieve correct total rates count");

            logger.info("✓ Enhanced currency rates lookup functionality test completed successfully");

        } catch (Exception e) {
            logger.error("Enhanced currency rates lookup test failed: " + e.getMessage(), e);
            fail("Enhanced currency rates lookup test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should test market data lookup functionality")
    void testMarketDataLookup() {
        logger.info("=== Testing Market Data Lookup Functionality ===");

        // Load YAML configuration for market data lookup
        try {
            // Update YAML configuration with dynamic server port
            String tempYamlPath = updateYamlWithServerPort("src/test/java/dev/mars/apex/demo/lookup/EnhancedRestApiDemoTest.yaml");
            var config = yamlLoader.loadFromFile(tempYamlPath);
            assertNotNull(config, "YAML configuration should not be null");

            // Test data - market data lookup for EURUSD
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("lookupType", "market_data");
            inputData.put("symbol", "EURUSD");

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, inputData);

            // Validate enrichment results
            assertNotNull(result, "Market data lookup result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // DEBUG: Print the actual enriched data
            System.out.println("DEBUG: Enriched data keys: " + enrichedData.keySet());
            System.out.println("DEBUG: Enriched data: " + enrichedData);

            // Validate YAML-driven REST API lookup results
            assertNotNull(enrichedData.get("marketSymbol"), "Market symbol should be retrieved from REST API");
            assertNotNull(enrichedData.get("marketName"), "Market name should be retrieved from REST API");
            assertNotNull(enrichedData.get("bidPrice"), "Bid price should be retrieved from REST API");
            assertNotNull(enrichedData.get("askPrice"), "Ask price should be retrieved from REST API");
            assertNotNull(enrichedData.get("tradingVolume"), "Trading volume should be retrieved from REST API");
            assertNotNull(enrichedData.get("changePercent"), "Change percent should be retrieved from REST API");

            // Validate specific REST API lookup results for EURUSD
            assertEquals("EURUSD", enrichedData.get("marketSymbol"), "Should retrieve correct market symbol");
            assertEquals("Euro/US Dollar", enrichedData.get("marketName"), "Should retrieve correct market name");
            assertEquals(1.0850, ((Number) enrichedData.get("bidPrice")).doubleValue(), 0.0001, "Should retrieve correct bid price");
            assertEquals(1.0852, ((Number) enrichedData.get("askPrice")).doubleValue(), 0.0001, "Should retrieve correct ask price");
            assertEquals(1250000, ((Number) enrichedData.get("tradingVolume")).intValue(), "Should retrieve correct trading volume");
            assertEquals(0.14, ((Number) enrichedData.get("changePercent")).doubleValue(), 0.01, "Should retrieve correct change percent");

            logger.info("✓ Market data lookup functionality test completed successfully");

        } catch (Exception e) {
            logger.error("Market data lookup test failed: " + e.getMessage(), e);
            fail("Market data lookup test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should test metrics lookup functionality")
    void testMetricsLookup() {
        logger.info("=== Testing Metrics Lookup Functionality ===");

        // Load YAML configuration for metrics lookup
        try {
            // Update YAML configuration with dynamic server port
            String tempYamlPath = updateYamlWithServerPort("src/test/java/dev/mars/apex/demo/lookup/EnhancedRestApiDemoTest.yaml");
            var config = yamlLoader.loadFromFile(tempYamlPath);
            assertNotNull(config, "YAML configuration should not be null");

            // Test data - metrics lookup
            Map<String, Object> inputData = new HashMap<>();
            inputData.put("lookupType", "metrics");

            // Execute APEX enrichment processing - ALL logic in YAML
            Object result = enrichmentService.enrichObject(config, inputData);

            // Validate enrichment results
            assertNotNull(result, "Metrics lookup result should not be null");
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            // Validate YAML-driven REST API lookup results
            assertNotNull(enrichedData.get("totalRequests"), "Total requests should be retrieved from REST API");
            assertNotNull(enrichedData.get("errorCount"), "Error count should be retrieved from REST API");
            assertNotNull(enrichedData.get("successRate"), "Success rate should be retrieved from REST API");
            assertNotNull(enrichedData.get("uptimeSeconds"), "Uptime seconds should be retrieved from REST API");
            assertNotNull(enrichedData.get("endpointMetrics"), "Endpoint metrics should be retrieved from REST API");

            // Validate specific REST API lookup results for metrics
            assertTrue(((Number) enrichedData.get("totalRequests")).intValue() >= 0, "Total requests should be non-negative");
            assertTrue(((Number) enrichedData.get("errorCount")).intValue() >= 0, "Error count should be non-negative");
            assertTrue(((Number) enrichedData.get("successRate")).doubleValue() >= 0.0, "Success rate should be non-negative");
            assertTrue(((Number) enrichedData.get("uptimeSeconds")).longValue() >= 0, "Uptime should be non-negative");

            logger.info("✓ Metrics lookup functionality test completed successfully");

        } catch (Exception e) {
            logger.error("Metrics lookup test failed: " + e.getMessage(), e);
            fail("Metrics lookup test failed: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should complete enhanced REST API demo validation")
    void testEnhancedRestApiDemoCompletion() {
        logger.info("=== Enhanced REST API Demo - COMPLETION VALIDATION ===");

        // Validate all enhanced features are working through APEX
        assertTrue(requestCounter.get() > 0, "Request counter should show activity from APEX REST API lookups");

        logger.info("📊 Enhanced REST API Demo Statistics:");
        logger.info("  Total Requests Processed: {}", requestCounter.get());
        logger.info("  Error Requests Processed: {}", errorCounter.get());
        logger.info("  Success Rate: {}%", requestCounter.get() > 0 ?
            (double)(requestCounter.get() - errorCounter.get()) / requestCounter.get() * 100 : 100.0);

        // Validate server is still running
        assertNotNull(httpServer, "Enhanced HTTP server should still be running");
        assertTrue(serverPort > 0, "Server port should be assigned");

        logger.info("✓ Enhanced REST API demo validation completed successfully");
        logger.info("🎉 Enhanced REST API Demo - ALL YAML FIRST TESTS PASSED!");
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

    // Utility methods
    private static Map<String, String> parseQueryParameters(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        Map<String, String> params = new java.util.HashMap<>();
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private static String createEnhancedCurrencyRatesResponse() {
        return """
            {
                "metadata": {
                    "source": "APEX Enhanced Currency API",
                    "version": "2.0",
                    "timestamp": "%s",
                    "total_rates": 4
                },
                "rates": [
                    {
                        "code": "USD",
                        "name": "US Dollar",
                        "rate": 1.0000,
                        "symbol": "USD",
                        "trend": "stable"
                    },
                    {
                        "code": "EUR",
                        "name": "Euro",
                        "rate": 0.8500,
                        "symbol": "EUR",
                        "trend": "up"
                    },
                    {
                        "code": "GBP",
                        "name": "British Pound",
                        "rate": 0.7300,
                        "symbol": "GBP",
                        "trend": "down"
                    },
                    {
                        "code": "JPY",
                        "name": "Japanese Yen",
                        "rate": 110.0000,
                        "symbol": "JPY",
                        "trend": "stable"
                    }
                ]
            }
            """.formatted(java.time.Instant.now().toString());
    }

    private static String createMarketDataResponse(String symbol) {
        // Simulate different market data based on symbol
        return switch (symbol.toUpperCase()) {
            case "EURUSD" -> """
                {
                    "symbol": "EURUSD",
                    "name": "Euro/US Dollar",
                    "bid": 1.0850,
                    "ask": 1.0852,
                    "spread": 0.0002,
                    "volume": 1250000,
                    "change": 0.0015,
                    "change_percent": 0.14,
                    "timestamp": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "GBPUSD" -> """
                {
                    "symbol": "GBPUSD",
                    "name": "British Pound/US Dollar",
                    "bid": 1.2650,
                    "ask": 1.2653,
                    "spread": 0.0003,
                    "volume": 980000,
                    "change": -0.0025,
                    "change_percent": -0.20,
                    "timestamp": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            default -> """
                {
                    "symbol": "%s",
                    "name": "Unknown Symbol",
                    "bid": 1.0000,
                    "ask": 1.0001,
                    "spread": 0.0001,
                    "volume": 0,
                    "change": 0.0000,
                    "change_percent": 0.00,
                    "timestamp": "%s"
                }
                """.formatted(symbol, java.time.Instant.now().toString());
        };
    }

    private static String createMetricsResponse() {
        return """
            {
                "server_metrics": {
                    "total_requests": %d,
                    "error_count": %d,
                    "success_rate": %.2f,
                    "uptime_seconds": %d,
                    "memory_usage": "45MB",
                    "cpu_usage": "12%%"
                },
                "endpoint_metrics": {
                    "/api/v2/currency/rates": {
                        "requests": %d,
                        "avg_response_time": "25ms"
                    },
                    "/api/v2/market/data": {
                        "requests": %d,
                        "avg_response_time": "18ms"
                    }
                },
                "timestamp": "%s"
            }
            """.formatted(
                requestCounter.get(),
                errorCounter.get(),
                requestCounter.get() > 0 ? (double)(requestCounter.get() - errorCounter.get()) / requestCounter.get() * 100 : 100.0,
                System.currentTimeMillis() / 1000,
                requestCounter.get() / 2,
                requestCounter.get() / 2,
                java.time.Instant.now().toString()
            );
    }

    private static String createBatchProcessingResponse() {
        return """
            {
                "batch_id": "batch_%d",
                "status": "completed",
                "processed_items": 150,
                "failed_items": 2,
                "success_rate": 98.67,
                "processing_time_ms": 1250,
                "results": [
                    {
                        "item_id": "item_001",
                        "status": "success",
                        "result": "processed"
                    },
                    {
                        "item_id": "item_002",
                        "status": "success",
                        "result": "processed"
                    }
                ],
                "timestamp": "%s"
            }
            """.formatted(System.currentTimeMillis(), java.time.Instant.now().toString());
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        logger.info("📤 Sent JSON response: {} bytes, status: {}", responseBytes.length, statusCode);
    }

    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        String errorResponse = """
            {
                "error": "%s",
                "status": %d,
                "timestamp": "%s"
            }
            """.formatted(message, statusCode, java.time.Instant.now().toString());

        sendJsonResponse(exchange, statusCode, errorResponse);
    }
}
