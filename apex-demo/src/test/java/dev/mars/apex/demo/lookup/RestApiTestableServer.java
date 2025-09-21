/*
 * Copyright (c) 2025 APEX Rules Engine Contributors
 * Licensed under the Apache License, Version 2.0
 * Author: APEX Demo Team
 */
package dev.mars.apex.demo.lookup;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * REST API Testable Server
 *
 * This class provides a standalone JDK HTTP server that can be reused
 * across multiple test classes for REST API testing. It supports:
 * - Currency rate lookups
 * - Currency conversions
 * - Customer data lookups
 * - Health checks
 * - Proper error handling
 *
 * Usage:
 * <pre>
 * RestApiTestableServer server = new RestApiTestableServer();
 * server.start();
 * String baseUrl = server.getBaseUrl();
 * // ... use the server
 * server.stop();
 * </pre>
 *
 * @author APEX Demo Team
 * @since 2025-09-20
 * @version 2.0.0 (Renamed with RestApi prefix for consistency)
 */
public class RestApiTestableServer {

    private static final Logger logger = LoggerFactory.getLogger(RestApiTestableServer.class);

    private HttpServer httpServer;
    private int serverPort;
    private String baseUrl;
    private boolean isRunning = false;
    private final int responseDelaySeconds;

    /**
     * Default constructor with no response delay.
     */
    public RestApiTestableServer() {
        this(0);
    }

    /**
     * Constructor with configurable response delay.
     *
     * @param responseDelaySeconds Number of seconds to delay before returning responses (default: 0)
     */
    public RestApiTestableServer(int responseDelaySeconds) {
        this.responseDelaySeconds = Math.max(0, responseDelaySeconds);
    }

    /**
     * Get the configured response delay in seconds.
     */
    public int getResponseDelaySeconds() {
        return responseDelaySeconds;
    }

    /**
     * Start the HTTP server on an available port.
     */
    public void start() throws IOException {
        if (isRunning) {
            logger.warn("Server is already running on port {}", serverPort);
            return;
        }

        logger.info("🌐 Starting Test REST API Server...");

        // Create HTTP server on available port
        httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        serverPort = httpServer.getAddress().getPort();
        baseUrl = "http://localhost:" + serverPort;

        // Setup all REST API endpoints
        setupCurrencyRateEndpoint();
        setupCurrencyConversionEndpoint();
        setupCustomerEndpoints();
        setupHealthCheckEndpoint();

        // Start the server
        httpServer.start();
        isRunning = true;

        logger.info(" Test REST API Server started successfully:");
        logger.info("  Base URL: {}", baseUrl);
        logger.info("  Currency Rate Endpoint: /api/currency/{currencyCode}");
        logger.info("  Currency Conversion Endpoint: /api/convert");
        logger.info("  Customer Endpoints: /api/customers, /api/customers/{customerId}");
        logger.info("  Health Check Endpoint: /api/health");
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        if (!isRunning) {
            logger.warn("Server is not running");
            return;
        }

        if (httpServer != null) {
            logger.info("🛑 Stopping Test REST API Server...");
            httpServer.stop(0);
            isRunning = false;
            logger.info(" Test REST API Server stopped successfully");
        }
    }

    /**
     * Get the base URL of the server.
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get the server port.
     */
    public int getPort() {
        return serverPort;
    }

    /**
     * Check if the server is running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Apply the configured response delay if greater than 0.
     */
    private void applyResponseDelay() {
        if (responseDelaySeconds > 0) {
            try {
                logger.debug("⏱️ Applying response delay of {} seconds", responseDelaySeconds);
                Thread.sleep(responseDelaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Response delay interrupted", e);
            }
        }
    }

    private void setupCurrencyRateEndpoint() {
        httpServer.createContext("/api/currency", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.debug("📡 Handling currency rate request: {}", exchange.getRequestURI());

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

                // Apply configured response delay
                applyResponseDelay();

                String jsonResponse = createCurrencyRateResponse(currencyCode);
                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    private void setupCurrencyConversionEndpoint() {
        httpServer.createContext("/api/convert", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.debug("📡 Handling currency conversion request: {}", exchange.getRequestURI());

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

                    // Apply configured response delay
                    applyResponseDelay();

                    String jsonResponse = createCurrencyConversionResponse(from, to, amount);
                    sendJsonResponse(exchange, 200, jsonResponse);
                } catch (NumberFormatException e) {
                    sendErrorResponse(exchange, 400, "Invalid amount format");
                }
            }
        });
    }

    private void setupCustomerEndpoints() {
        // Setup customer endpoints - both all customers and individual customer lookup
        httpServer.createContext("/api/customers", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.debug("📡 Handling customer request: {}", exchange.getRequestURI());

                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendErrorResponse(exchange, 405, "Method Not Allowed");
                    return;
                }

                String path = exchange.getRequestURI().getPath();

                // Apply configured response delay
                applyResponseDelay();

                // Check if this is a specific customer lookup: /api/customers/{customerId}
                if (path.startsWith("/api/customers/")) {
                    String customerId = extractCustomerIdFromPath(path);
                    if (customerId != null && !customerId.isEmpty()) {
                        String jsonResponse = createCustomerResponse(customerId);
                        sendJsonResponse(exchange, 200, jsonResponse);
                    } else {
                        sendErrorResponse(exchange, 400, "Invalid customer ID format");
                    }
                } else if (path.equals("/api/customers")) {
                    // Return all customers
                    String jsonResponse = createAllCustomersResponse();
                    sendJsonResponse(exchange, 200, jsonResponse);
                } else {
                    sendErrorResponse(exchange, 404, "Customer endpoint not found");
                }
            }
        });
    }

    private void setupHealthCheckEndpoint() {
        httpServer.createContext("/api/health", new HttpHandler() {
            public void handle(HttpExchange exchange) throws IOException {
                logger.debug("📡 Handling health check request");

                // Apply configured response delay
                applyResponseDelay();

                String jsonResponse = """
                    {
                        "status": "UP",
                        "timestamp": "%s",
                        "service": "Test REST API Server",
                        "version": "1.0.0",
                        "baseUrl": "%s",
                        "responseDelaySeconds": %d
                    }
                    """.formatted(java.time.Instant.now().toString(), baseUrl, responseDelaySeconds);

                sendJsonResponse(exchange, 200, jsonResponse);
            }
        });
    }

    // Helper methods for path extraction
    private String extractCurrencyCodeFromPath(String path) {
        // Extract currency code from path like "/api/currency/USD"
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : null;
    }

    private String extractCustomerIdFromPath(String path) {
        // Extract customer ID from path like "/api/customers/CUST1"
        String[] parts = path.split("/");
        return parts.length >= 4 ? parts[3] : null;
    }

    // Helper methods for query parameter parsing
    private Map<String, String> parseQueryParameters(String query) {
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

    // Response creation methods
    private String createCurrencyRateResponse(String currencyCode) {
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
                    "symbol": "€",
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

    private String createCurrencyConversionResponse(String from, String to, double amount) {
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

    private double getCurrencyRate(String currencyCode) {
        return switch (currencyCode.toUpperCase()) {
            case "USD" -> 1.0;
            case "EUR" -> 0.85;
            case "GBP" -> 0.73;
            case "JPY" -> 110.0;
            default -> 1.0;
        };
    }

    private String createCustomerResponse(String customerId) {
        // Create realistic customer data with 5 attributes including 5-character customer ID
        return switch (customerId.toUpperCase()) {
            case "CUST1" -> """
                {
                    "customerId": "CUST1",
                    "customerName": "Acme Corporation",
                    "customerType": "CORPORATE",
                    "creditRating": "AAA",
                    "registrationDate": "2020-01-15",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "CUST2" -> """
                {
                    "customerId": "CUST2",
                    "customerName": "Global Trading Ltd",
                    "customerType": "INSTITUTIONAL",
                    "creditRating": "AA+",
                    "registrationDate": "2019-03-22",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "CUST3" -> """
                {
                    "customerId": "CUST3",
                    "customerName": "Investment Partners Inc",
                    "customerType": "HEDGE_FUND",
                    "creditRating": "A",
                    "registrationDate": "2021-07-10",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "CUST4" -> """
                {
                    "customerId": "CUST4",
                    "customerName": "Retail Bank Group",
                    "customerType": "BANK",
                    "creditRating": "AA",
                    "registrationDate": "2018-11-05",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            case "CUST5" -> """
                {
                    "customerId": "CUST5",
                    "customerName": "Pension Fund Alliance",
                    "customerType": "PENSION_FUND",
                    "creditRating": "AAA",
                    "registrationDate": "2017-09-18",
                    "lastUpdated": "%s"
                }
                """.formatted(java.time.Instant.now().toString());
            default -> """
                {
                    "customerId": "%s",
                    "customerName": "Unknown Customer",
                    "customerType": "UNKNOWN",
                    "creditRating": "NR",
                    "registrationDate": "1900-01-01",
                    "lastUpdated": "%s"
                }
                """.formatted(customerId, java.time.Instant.now().toString());
        };
    }

    private String createAllCustomersResponse() {
        // Return all customers as JSON array
        return """
            {
                "customers": [
                    {
                        "customerId": "CUST1",
                        "customerName": "Acme Corporation",
                        "customerType": "CORPORATE",
                        "creditRating": "AAA",
                        "registrationDate": "2020-01-15"
                    },
                    {
                        "customerId": "CUST2",
                        "customerName": "Global Trading Ltd",
                        "customerType": "INSTITUTIONAL",
                        "creditRating": "AA+",
                        "registrationDate": "2019-03-22"
                    },
                    {
                        "customerId": "CUST3",
                        "customerName": "Investment Partners Inc",
                        "customerType": "HEDGE_FUND",
                        "creditRating": "A",
                        "registrationDate": "2021-07-10"
                    },
                    {
                        "customerId": "CUST4",
                        "customerName": "Retail Bank Group",
                        "customerType": "BANK",
                        "creditRating": "AA",
                        "registrationDate": "2018-11-05"
                    },
                    {
                        "customerId": "CUST5",
                        "customerName": "Pension Fund Alliance",
                        "customerType": "PENSION_FUND",
                        "creditRating": "AAA",
                        "registrationDate": "2017-09-18"
                    }
                ],
                "totalCount": 5,
                "lastUpdated": "%s"
            }
            """.formatted(java.time.Instant.now().toString());
    }

    // HTTP response helper methods
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] responseBytes = jsonResponse.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        logger.debug("📤 Sent JSON response: {} bytes, status: {}", responseBytes.length, statusCode);
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
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
