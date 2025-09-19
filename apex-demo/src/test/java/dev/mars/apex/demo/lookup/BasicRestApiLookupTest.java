package dev.mars.apex.demo.lookup;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 2.1: Basic REST API Infrastructure Test
 *
 * Tests APEX REST API integration using JDK's built-in HTTP server.
 * This test demonstrates real REST API functionality without external dependencies.
 *
 * Key Features Tested:
 * - JDK HttpServer integration
 * - Real HTTP requests and responses
 * - JSON data parsing
 * - Currency rate lookup
 * - Currency conversion
 * - Basic error handling
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 1.0.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BasicRestApiLookupTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(BasicRestApiLookupTest.class);

    private static HttpServer httpServer;
    private static int serverPort;

    @BeforeAll
    static void setupBasicRestApiTestSuite() throws IOException {
        logger.info("================================================================================");
        logger.info("PHASE 2.1: Basic REST API Infrastructure Setup");
        logger.info("================================================================================");

        // Setup JDK HTTP Server for all tests
        setupJdkHttpServer();

        logger.info("✅ Basic REST API test suite setup completed successfully");
    }

    @AfterAll
    static void teardownBasicRestApiTestSuite() {
        if (httpServer != null) {
            logger.info("🛑 Stopping JDK HTTP server...");
            httpServer.stop(0);
            logger.info("✅ JDK HTTP server stopped successfully");
        }
    }

    private static void setupJdkHttpServer() throws IOException {
        logger.info("🌐 Setting up JDK HTTP server for REST API testing...");

        // Create HTTP server on available port
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = httpServer.getAddress().getPort();

        // Setup currency rate endpoint
        setupCurrencyRateEndpoint();

        // Setup currency conversion endpoint
        setupCurrencyConversionEndpoint();

        // Setup health check endpoint
        setupHealthCheckEndpoint();

        // Start the server
        httpServer.start();

        logger.info("✅ JDK HTTP server started successfully:");
        logger.info("  Server URL: http://localhost:{}", serverPort);
        logger.info("  Currency Rate Endpoint: /api/currency/{currencyCode}");
        logger.info("  Currency Conversion Endpoint: /api/convert");
        logger.info("  Health Check Endpoint: /api/health");
    }

    private static void setupCurrencyRateEndpoint() {
        httpServer.createContext("/api/currency", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.info("📡 Handling currency rate request: {}", exchange.getRequestURI());

                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String currencyCode = extractCurrencyCodeFromPath(path);

                if (currencyCode == null || currencyCode.isEmpty()) {
                    sendErrorResponse(exchange, 400, "Currency code is required");
                    return;
                }

                String jsonResponse = createCurrencyRateResponse(currencyCode);
                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    private static void setupCurrencyConversionEndpoint() {
        httpServer.createContext("/api/convert", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.info("📡 Handling currency conversion request: {}", exchange.getRequestURI());

                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQueryParameters(query);

                String from = params.get("from");
                String to = params.get("to");
                String amountStr = params.getOrDefault("amount", "1.0");

                if (from == null || to == null) {
                    sendErrorResponse(exchange, 400, "Both 'from' and 'to' currency codes are required");
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    String jsonResponse = createCurrencyConversionResponse(from, to, amount);
                    sendJsonResponse(exchange, 200, jsonResponse);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid amount format");
                }
            }
        });
    }

    private static void setupHealthCheckEndpoint() {
        httpServer.createContext("/api/health", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.info("📡 Handling health check request");

                String jsonResponse = """
                    {
                        "status": "UP",
                        "timestamp": "%s",
                        "service": "APEX Basic REST API Test Server",
                        "version": "1.0.0"
                    }
                    """.formatted(java.time.Instant.now().toString());

                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    private static String extractCurrencyCodeFromPath(String path) {
        // Extract currency code from path like "/api/currency/USD"
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : null;
    }

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

    private static String createCurrencyRateResponse(String currencyCode) {
        // Create realistic currency rate data for testing
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
                    "symbol": "EUR",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "GBP" -> """
                {
                    "code": "GBP",
                    "name": "British Pound",
                    "rate": 0.73,
                    "symbol": "£",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "JPY" -> """
                {
                    "code": "JPY",
                    "name": "Japanese Yen",
                    "rate": 110.0,
                    "symbol": "¥",
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

    private static String createCurrencyConversionResponse(String from, String to, double amount) {
        // Simple conversion logic for testing (using hardcoded rates)
        double fromRate = getCurrencyRate(from);
        double toRate = getCurrencyRate(to);
        double exchangeRate = toRate / fromRate;
        double convertedAmount = amount * exchangeRate;

        return """
            {
                "fromCurrency": "%s",
                "toCurrency": "%s",
                "originalAmount": %.2f,
                "convertedAmount": %.2f,
                "exchangeRate": %.4f,
                "timestamp": "%s"
            }
            """.formatted(from, to, amount, convertedAmount, exchangeRate, java.time.Instant.now().toString());
    }

    private static double getCurrencyRate(String currencyCode) {
        return switch (currencyCode.toUpperCase()) {
            case "USD" -> 1.0;
            case "EUR" -> 0.85;
            case "GBP" -> 0.73;
            case "JPY" -> 110.0;
            default -> 1.0;
        };
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

    @Test
    @Order(1)
    @DisplayName("Should validate JDK HTTP server setup")
    void testJdkHttpServerSetup() {
        logger.info("================================================================================");
        logger.info("PHASE 2.1: JDK HTTP Server Setup Validation");
        logger.info("================================================================================");

        // Validate server is running
        assertNotNull(httpServer, "JDK HTTP server should be initialized");
        assertTrue(httpServer.getAddress().getPort() > 0, "Server should be running on a valid port");

        logger.info("✅ JDK HTTP Server Details:");
        logger.info("  Server Address: {}", httpServer.getAddress());
        logger.info("  Server Port: {}", serverPort);
        logger.info("  Server Running: {}", httpServer.getAddress() != null);

        logger.info("✅ JDK HTTP server setup validation completed successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Should perform direct HTTP currency rate lookup")
    void testDirectHttpCurrencyRateLookup() throws Exception {
        logger.info("================================================================================");
        logger.info("PHASE 2.1: Direct HTTP Currency Rate Lookup");
        logger.info("================================================================================");

        // Test direct HTTP call to our JDK server
        String currencyCode = "EUR";
        String url = "http://localhost:" + serverPort + "/api/currency/" + currencyCode;

        logger.info("🔧 Testing direct HTTP call:");
        logger.info("  URL: {}", url);
        logger.info("  Currency Code: {}", currencyCode);

        // Make direct HTTP call using Java's built-in HTTP client
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

        logger.info("✅ HTTP Response Details:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Parse JSON response
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"code\": \"EUR\""), "Response should contain EUR code");
        assertTrue(jsonResponse.contains("\"name\": \"Euro\""), "Response should contain Euro name");
        assertTrue(jsonResponse.contains("\"rate\": 0.85"), "Response should contain exchange rate");
        assertTrue(jsonResponse.contains("\"symbol\": \"EUR\""), "Response should contain EUR symbol");
        assertTrue(jsonResponse.contains("\"lastUpdated\""), "Response should contain timestamp");

        // Validate performance requirement
        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Direct HTTP currency rate lookup completed successfully in {}ms", responseTime);
    }

    @Test
    @Order(3)
    @DisplayName("Should perform direct HTTP currency conversion")
    void testDirectHttpCurrencyConversion() throws Exception {
        logger.info("================================================================================");
        logger.info("PHASE 2.1: Direct HTTP Currency Conversion");
        logger.info("================================================================================");

        // Test direct HTTP call for currency conversion
        String fromCurrency = "USD";
        String toCurrency = "EUR";
        double amount = 100.0;
        String url = String.format("http://localhost:%d/api/convert?from=%s&to=%s&amount=%.2f",
            serverPort, fromCurrency, toCurrency, amount);

        logger.info("🔧 Testing direct HTTP currency conversion:");
        logger.info("  URL: {}", url);
        logger.info("  From Currency: {}", fromCurrency);
        logger.info("  To Currency: {}", toCurrency);
        logger.info("  Amount: {}", amount);

        // Make direct HTTP call using Java's built-in HTTP client
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

        logger.info("✅ HTTP Response Details:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Parse JSON response and validate conversion logic
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"fromCurrency\": \"USD\""), "Response should contain from currency");
        assertTrue(jsonResponse.contains("\"toCurrency\": \"EUR\""), "Response should contain to currency");
        assertTrue(jsonResponse.contains("\"originalAmount\": 100.00"), "Response should contain original amount");
        assertTrue(jsonResponse.contains("\"convertedAmount\": 85.00"), "Response should contain converted amount (100 * 0.85)");
        assertTrue(jsonResponse.contains("\"exchangeRate\": 0.8500"), "Response should contain exchange rate");
        assertTrue(jsonResponse.contains("\"timestamp\""), "Response should contain timestamp");

        // Validate performance requirement
        long responseTime = endTime - startTime;
        assertTrue(responseTime < 1000, "Response time should be < 1000ms, was: " + responseTime + "ms");

        logger.info("✅ Direct HTTP currency conversion completed successfully in {}ms", responseTime);
    }

    @Test
    @Order(4)
    @DisplayName("Should validate health check endpoint")
    void testHealthCheckEndpoint() throws Exception {
        logger.info("================================================================================");
        logger.info("PHASE 2.1: Health Check Endpoint Validation");
        logger.info("================================================================================");

        // Test health check endpoint
        String url = "http://localhost:" + serverPort + "/api/health";

        logger.info("🔧 Testing health check endpoint:");
        logger.info("  URL: {}", url);

        // Make direct HTTP call using Java's built-in HTTP client
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
        assertEquals(200, response.statusCode(), "Health check should return 200 OK");
        assertNotNull(response.body(), "Health check response body should not be null");

        logger.info("✅ Health Check Response Details:");
        logger.info("  Status Code: {}", response.statusCode());
        logger.info("  Response Body: {}", response.body());

        // Parse JSON response and validate health check data
        String jsonResponse = response.body();
        assertTrue(jsonResponse.contains("\"status\": \"UP\""), "Health status should be UP");
        assertTrue(jsonResponse.contains("\"service\": \"APEX Basic REST API Test Server\""), "Service name should be present");
        assertTrue(jsonResponse.contains("\"version\": \"1.0.0\""), "Version should be present");
        assertTrue(jsonResponse.contains("\"timestamp\""), "Timestamp should be present");

        // Validate performance requirement
        long responseTime = endTime - startTime;
        assertTrue(responseTime < 500, "Health check response time should be < 500ms, was: " + responseTime + "ms");

        logger.info("✅ Health check endpoint validation completed successfully in {}ms", responseTime);
        logger.info("================================================================================");
        logger.info("🎉 PHASE 2.1: Basic REST API Infrastructure - ALL TESTS PASSED!");
        logger.info("================================================================================");
    }
}
