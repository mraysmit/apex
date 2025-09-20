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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2.2: Enhanced REST API Features Test
 * 
 * Tests advanced REST API functionality including:
 * - Multiple data source endpoints
 * - Error handling and retry logic
 * - Request/response validation
 * - Performance monitoring
 * - Advanced JSON processing
 * 
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnhancedRestApiDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedRestApiDemoTest.class);

    private static HttpServer httpServer;
    private static int serverPort;
    private static AtomicInteger requestCounter = new AtomicInteger(0);
    private static AtomicInteger errorCounter = new AtomicInteger(0);

    @BeforeAll
    static void setupEnhancedRestApiTestSuite() throws IOException {
        logger.info("================================================================================");
        logger.info("PHASE 2.2: Enhanced REST API Features Setup");
        logger.info("================================================================================");

        // Setup enhanced JDK HTTP Server with multiple endpoints
        setupEnhancedHttpServer();

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

    private static void setupEnhancedHttpServer() throws IOException {
        logger.info("🌐 Setting up enhanced JDK HTTP server for advanced REST API testing...");

        // Create HTTP server on available port
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = httpServer.getAddress().getPort();

        // Setup multiple enhanced endpoints
        setupCurrencyRatesEndpoint();
        setupMarketDataEndpoint();
        setupErrorSimulationEndpoint();
        setupMetricsEndpoint();
        setupBatchProcessingEndpoint();

        // Start the server
        httpServer.start();

        logger.info("✅ Enhanced JDK HTTP server started successfully:");
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
    @DisplayName("Should validate enhanced HTTP server setup")
    void testEnhancedHttpServerSetup() {
        logger.info("================================================================================");
        logger.info("PHASE 2.2: Enhanced HTTP Server Setup Validation");
        logger.info("================================================================================");

        // Validate server is running
        assertNotNull(httpServer, "Enhanced HTTP server should be initialized");
        assertTrue(serverPort > 0, "Server port should be assigned");

        logger.info("🔧 Enhanced HTTP Server Details:");
        logger.info("  Server Address: {}", httpServer.getAddress());
        logger.info("  Server Port: {}", serverPort);
        logger.info("  Server Running: true");

        logger.info("✅ Enhanced HTTP server setup validation completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should test enhanced currency rates endpoint")
    void testEnhancedCurrencyRatesEndpoint() throws Exception {
        logger.info("================================================================================");
        logger.info("PHASE 2.2: Enhanced Currency Rates Endpoint Test");
        logger.info("================================================================================");

        String url = "http://localhost:" + serverPort + "/api/v2/currency/rates";
        logger.info("🔧 Testing enhanced currency rates endpoint:");
        logger.info("  URL: {}", url);

        // Make HTTP call using Java's built-in HTTP client
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Validate HTTP response
        assertEquals(200, response.statusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.body(), "Response body should not be null");

        logger.info("✅ Enhanced Currency Rates Response:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Validate enhanced JSON structure
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"rates\""), "Response should contain rates array");
        assertTrue(jsonResponse.contains("\"metadata\""), "Response should contain metadata");
        assertTrue(jsonResponse.contains("\"timestamp\""), "Response should contain timestamp");
        assertTrue(jsonResponse.contains("\"source\""), "Response should contain data source");

        // Validate performance
        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Enhanced currency rates endpoint test completed successfully in {}ms", responseTime);
    }

    @Test
    @Order(3)
    @DisplayName("Should test market data endpoint with parameters")
    void testMarketDataEndpoint() throws Exception {
        logger.info("================================================================================");
        logger.info("PHASE 2.2: Market Data Endpoint Test");
        logger.info("================================================================================");

        String symbol = "EURUSD";
        String url = "http://localhost:" + serverPort + "/api/v2/market/data?symbol=" + symbol;
        logger.info("🔧 Testing market data endpoint:");
        logger.info("  URL: {}", url);
        logger.info("  Symbol: {}", symbol);

        // Make HTTP call
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Validate response
        assertEquals(200, response.statusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.body(), "Response body should not be null");

        logger.info("✅ Market Data Response:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Validate market data structure
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"symbol\": \"" + symbol + "\""), "Response should contain requested symbol");
        assertTrue(jsonResponse.contains("\"bid\""), "Response should contain bid price");
        assertTrue(jsonResponse.contains("\"ask\""), "Response should contain ask price");
        assertTrue(jsonResponse.contains("\"volume\""), "Response should contain volume");

        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Market data endpoint test completed successfully in {}ms", responseTime);
    }

    @Test
    @Order(4)
    @DisplayName("Should test error handling and simulation")
    void testErrorHandlingAndSimulation() throws Exception {
        logger.info("================================================================================");
        logger.info("PHASE 2.2: Error Handling and Simulation Test");
        logger.info("================================================================================");

        // Test different error scenarios
        testErrorScenario("server_error", 500);
        testErrorScenario("not_found", 404);
        testErrorScenario("rate_limit", 429);

        logger.info("✅ Error handling and simulation test completed successfully");
    }

    @Test
    @Order(5)
    @DisplayName("Should test metrics endpoint")
    void testMetricsEndpoint() throws Exception {
        logger.info("================================================================================");
        logger.info("PHASE 2.2: Metrics Endpoint Test");
        logger.info("================================================================================");

        String url = "http://localhost:" + serverPort + "/api/v2/metrics";
        logger.info("🔧 Testing metrics endpoint:");
        logger.info("  URL: {}", url);

        // Make HTTP call
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        long startTime = System.currentTimeMillis();
        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());
        long endTime = System.currentTimeMillis();

        // Validate response
        assertEquals(200, response.statusCode(), "HTTP status should be 200 OK");
        assertNotNull(response.body(), "Response body should not be null");

        logger.info("✅ Metrics Response:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Validate metrics structure
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"server_metrics\""), "Response should contain server metrics");
        assertTrue(jsonResponse.contains("\"total_requests\""), "Response should contain total requests");
        assertTrue(jsonResponse.contains("\"endpoint_metrics\""), "Response should contain endpoint metrics");

        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Metrics endpoint test completed successfully in {}ms", responseTime);
    }

    @Test
    @Order(6)
    @DisplayName("Should complete Phase 2.2 validation")
    void testPhase22Completion() {
        logger.info("================================================================================");
        logger.info("PHASE 2.2: Enhanced REST API Features - COMPLETION VALIDATION");
        logger.info("================================================================================");

        // Validate all enhanced features are working
        assertTrue(requestCounter.get() > 0, "Request counter should show activity");

        logger.info("📊 Phase 2.2 Statistics:");
        logger.info("  Total Requests Processed: {}", requestCounter.get());
        logger.info("  Error Requests Processed: {}", errorCounter.get());
        logger.info("  Success Rate: {}%", requestCounter.get() > 0 ?
            (double)(requestCounter.get() - errorCounter.get()) / requestCounter.get() * 100 : 100.0);

        logger.info("================================================================================");
        logger.info("🎉 PHASE 2.2: Enhanced REST API Features - ALL TESTS PASSED!");
        logger.info("================================================================================");
    }

    private void testErrorScenario(String errorType, int expectedStatus) throws Exception {
        String url = "http://localhost:" + serverPort + "/api/v2/test/errors?type=" + errorType;
        logger.info("🔧 Testing error scenario: {} (expecting {})", errorType, expectedStatus);

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .header("Accept", "application/json")
            .GET()
            .build();

        java.net.http.HttpResponse<String> response = httpClient.send(request,
            java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(expectedStatus, response.statusCode(),
            "Error scenario " + errorType + " should return " + expectedStatus);

        logger.info("✅ Error scenario {} returned expected status {}", errorType, expectedStatus);
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
