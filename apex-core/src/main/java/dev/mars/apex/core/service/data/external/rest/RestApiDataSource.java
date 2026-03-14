package dev.mars.apex.core.service.data.external.rest;

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


import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import dev.mars.apex.core.service.data.external.cache.EnhancedCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * REST API implementation of ExternalDataSource.
 * 
 * This class provides HTTP-based connectivity to REST APIs with support for
 * authentication, circuit breaker patterns, caching, and response mapping.
 * 
 * Features:
 * - Multiple authentication methods (Bearer, API Key, Basic, OAuth2)
 * - Circuit breaker for resilience
 * - Response caching with TTL
 * - Configurable timeouts and retries
 * - JSON response parsing
 * - Health monitoring
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-30
 * @version 1.0
 */
public class RestApiDataSource implements ExternalDataSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiDataSource.class);
    
    private final HttpClient httpClient;
    private DataSourceConfiguration configuration;
    private ConnectionStatus connectionStatus;
    private DataSourceMetrics metrics;
    private CircuitBreaker circuitBreaker;

    // Enhanced cache manager for API responses
    private EnhancedCacheManager cacheManager;
    
    /**
     * Constructor with HttpClient and configuration.
     * 
     * @param httpClient The HTTP client to use
     * @param configuration The data source configuration
     */
    public RestApiDataSource(HttpClient httpClient, DataSourceConfiguration configuration) {
        this.httpClient = httpClient;
        this.configuration = configuration;
        this.connectionStatus = ConnectionStatus.notInitialized();
        this.metrics = new DataSourceMetrics();
        this.cacheManager = new EnhancedCacheManager(configuration);

        // Initialize circuit breaker if configured
        if (configuration.getCircuitBreaker() != null && configuration.getCircuitBreaker().isEnabled()) {
            this.circuitBreaker = new CircuitBreaker(configuration.getCircuitBreaker());
        }
    }
    
    @Override
    public void initialize(DataSourceConfiguration config) throws DataSourceException {
        this.configuration = config;
        this.connectionStatus = ConnectionStatus.connecting();
        LOGGER.info("Initializing REST API data source '{}': baseUrl='{}', timeout={}ms, circuitBreakerEnabled={}",
            sourceName(), configuredBaseUrl(), configuredTimeout(), circuitBreaker != null);

        try {
            // Validate configuration first
            if (config.getConnection() == null || config.getConnection().getBaseUrl() == null) {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "REST API data source requires baseUrl configuration", null, config.getName(), "initialize", false);
            }

            // Initialize successfully without testing connection during initialization
            // Connection testing will be done on-demand when testConnection() is called
            this.connectionStatus = ConnectionStatus.connected("REST API data source initialized");
            LOGGER.info("REST API data source '{}' initialized successfully: baseUrl='{}'", sourceName(), configuredBaseUrl());
        } catch (DataSourceException e) {
            // Re-throw configuration errors
            throw e;
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
            LOGGER.warn("REST API data source '{}' initialized with warning: baseUrl='{}', error={}",
                sourceName(), configuredBaseUrl(), e.getMessage());
            LOGGER.debug("REST API initialization warning for '{}':", sourceName(), e);
        }
    }
    
    @Override
    public DataSourceType getSourceType() {
        return DataSourceType.REST_API;
    }
    
    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }
    
    @Override
    public DataSourceMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public boolean isHealthy() {
        return testConnection();
    }
    
    @Override
    public boolean testConnection() {
        try {
            String healthEndpoint = getHealthEndpoint();
            LOGGER.debug("Testing REST API connection for '{}': endpoint='{}'", sourceName(), healthEndpoint);
            HttpRequest request = buildHttpRequest(healthEndpoint, "GET", null);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOGGER.debug("REST API connection test completed for '{}': endpoint='{}', status={}",
                sourceName(), healthEndpoint, response.statusCode());
            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (java.net.http.HttpConnectTimeoutException e) {
            LOGGER.warn("REST API connection test timed out for '{}': baseUrl='{}', error={}", sourceName(), configuredBaseUrl(), e.getMessage());
            LOGGER.debug("REST API timeout stack trace for '{}':", sourceName(), e);
            return false;
        } catch (java.net.ConnectException e) {
            LOGGER.warn("REST API connection test failed to connect for '{}': baseUrl='{}', error={}", sourceName(), configuredBaseUrl(), e.getMessage());
            LOGGER.debug("REST API connection error stack trace for '{}':", sourceName(), e);
            return false;
        } catch (java.io.IOException e) {
            LOGGER.warn("REST API connection test failed with IO error for '{}': baseUrl='{}', error={}", sourceName(), configuredBaseUrl(), e.getMessage());
            LOGGER.debug("REST API IO error stack trace for '{}':", sourceName(), e);
            return false;
        } catch (InterruptedException e) {
            LOGGER.warn("REST API connection test was interrupted for '{}': baseUrl='{}', error={}", sourceName(), configuredBaseUrl(), e.getMessage());
            LOGGER.debug("REST API interrupted stack trace for '{}':", sourceName(), e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            LOGGER.warn("REST API connection test failed for '{}': baseUrl='{}', error={}", sourceName(), configuredBaseUrl(), e.getMessage());
            LOGGER.debug("REST API general error stack trace for '{}':", sourceName(), e);
            return false;
        }
    }
    
    @Override
    public String getName() {
        return configuration != null ? configuration.getName() : "rest-api-source";
    }
    
    @Override
    public String getDataType() {
        return configuration != null ? configuration.getSourceType() : "rest-api";
    }
    
    @Override
    public boolean supportsDataType(String dataType) {
        return "rest-api".equals(dataType) || 
               (configuration != null && configuration.getSourceType().equals(dataType));
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(String dataType, Object... parameters) {
        long startTime = System.currentTimeMillis();
        LOGGER.debug("REST API getData start for '{}': dataType='{}', parameters={}",
            sourceName(), dataType, Arrays.toString(parameters));
        
        try {
            // Check cache first if enabled
            if (cacheManager.isEnabled()) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                Object cached = cacheManager.get(cacheKey);
                if (cached != null) {
                    metrics.recordCacheHit();
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    LOGGER.debug("REST API cache hit for '{}': cacheKey='{}'", sourceName(), cacheKey);
                    return (T) cached;
                }
                metrics.recordCacheMiss();
                LOGGER.debug("REST API cache miss for '{}': cacheKey='{}'", sourceName(), cacheKey);
            }

            // Execute API call with circuit breaker if enabled
            Object result;
            if (circuitBreaker != null) {
                result = circuitBreaker.execute(() -> executeApiCall(dataType, parameters));
            } else {
                result = executeApiCall(dataType, parameters);
            }

            // Cache the result if caching is enabled
            if (cacheManager.isEnabled() && result != null) {
                String cacheKey = cacheManager.generateCacheKey(dataType, parameters);
                cacheManager.put(cacheKey, result);
                LOGGER.debug("Cached REST API response for '{}': cacheKey='{}'", sourceName(), cacheKey);
            }

            metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
            LOGGER.debug("REST API getData completed for '{}': dataType='{}', resultType='{}'",
                sourceName(), dataType, result != null ? result.getClass().getSimpleName() : "null");
            return (T) result;
            
        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("Failed to get data from REST API '{}': dataType='{}', parameters={}, error={}",
                sourceName(), dataType, Arrays.toString(parameters), e.getMessage());
            LOGGER.debug("REST API getData failure for '{}':", sourceName(), e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        // For REST APIs, the "query" is typically an endpoint path
        long startTime = System.currentTimeMillis();

        try {
            LOGGER.debug("Executing REST API query for '{}': query='{}', parameters={}", sourceName(), query, parameters);
            // First, resolve named query from configuration
            String actualQuery = resolveNamedQuery(query);
            LOGGER.debug("Resolved REST API query for '{}': query='{}', resolved='{}'", sourceName(), query, actualQuery);
            String endpoint = buildEndpoint(actualQuery, parameters);
            LOGGER.debug("Built REST API endpoint for '{}': endpoint='{}'", sourceName(), endpoint);
            HttpRequest request = buildHttpRequest(endpoint, "GET", null);

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                LOGGER.debug("REST API response received for '{}': endpoint='{}', status={}, bodyLength={}",
                    sourceName(), endpoint, response.statusCode(), response.body().length());
            } catch (Exception e) {
                LOGGER.error("HTTP request failed for REST API '{}': endpoint='{}', error={}", sourceName(), endpoint, e.getMessage());
                LOGGER.debug("HTTP request failure for REST API '{}':", sourceName(), e);
                throw e;
            }

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try {
                    List<T> result = parseResponseToList(response.body());
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    LOGGER.debug("REST API query completed for '{}': query='{}', endpoint='{}', results={}",
                        sourceName(), query, endpoint, result != null ? result.size() : 0);
                    return result;
                } catch (Exception e) {
                    LOGGER.error("Response parsing failed for REST API '{}': query='{}', endpoint='{}', error={}",
                        sourceName(), query, endpoint, e.getMessage());
                    LOGGER.debug("Response parsing failure for REST API '{}':", sourceName(), e);
                    throw e;
                }
            } else {
                LOGGER.error("REST API call failed for '{}': query='{}', endpoint='{}', status={}",
                    sourceName(), query, endpoint, response.statusCode());
                LOGGER.debug("Failed REST API response body for '{}': {}", sourceName(), response.body());
                metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
                throw new DataSourceException(DataSourceException.ErrorType.EXECUTION_ERROR,
                    "API call failed with status: " + response.statusCode(), null,
                    configuration.getName(), "query", true);
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("REST API call failed for '{}': query='{}', error={}", sourceName(), query, e.getMessage());
            LOGGER.debug("REST API query failure for '{}':", sourceName(), e);
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            throw DataSourceException.executionError("REST API call failed", e, "query");
        } catch (DataSourceException e) {
            // Re-throw DataSourceException but ensure metrics are recorded
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            throw e;
        }
    }
    
    @Override
    public <T> T queryForObject(String query, Map<String, Object> parameters) throws DataSourceException {
        LOGGER.debug("Executing REST API queryForObject for '{}': query='{}', parameters={}", sourceName(), query, parameters);
        try {
            List<T> results = query(query, parameters);
            LOGGER.debug("REST API queryForObject completed for '{}': query='{}', resultCount={}", sourceName(), query, results.size());
            return results.isEmpty() ? null : results.get(0);
        } catch (DataSourceException e) {
            // Already logged at ERROR in query() — just propagate
            throw e;
        } catch (Exception e) {
            LOGGER.error("REST API queryForObject failed for '{}': query='{}', error={}", sourceName(), query, e.getMessage());
            LOGGER.debug("REST API queryForObject failure for '{}':", sourceName(), e);
            throw e;
        }
    }
    
    @Override
    public <T> List<List<T>> batchQuery(List<String> queries) throws DataSourceException {
        List<List<T>> results = new ArrayList<>();
        
        for (String query : queries) {
            List<T> queryResult = query(query, Collections.emptyMap());
            results.add(queryResult);
        }
        
        return results;
    }
    
    @Override
    public void batchUpdate(List<String> updates) throws DataSourceException {
        // For REST APIs, updates are typically POST/PUT/PATCH requests
        for (String update : updates) {
            try {
                HttpRequest request = buildHttpRequest(update, "POST", null);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new DataSourceException(DataSourceException.ErrorType.EXECUTION_ERROR,
                        "API update failed with status: " + response.statusCode(), null,
                        configuration.getName(), "batchUpdate", true);
                }
                
            } catch (IOException | InterruptedException e) {
                throw DataSourceException.executionError("REST API update failed", e, "batchUpdate");
            }
        }
    }
    
    @Override
    public DataSourceConfiguration getConfiguration() {
        return configuration;
    }
    
    @Override
    public void refresh() throws DataSourceException {
        LOGGER.info("Refreshing REST API data source '{}': baseUrl='{}', circuitBreakerEnabled={}",
            sourceName(), configuredBaseUrl(), circuitBreaker != null);
        // Clear cache
        cacheManager.clear();

        // Reset circuit breaker if present
        if (circuitBreaker != null) {
            circuitBreaker.reset();
        }

        // Test connection
        if (!testConnection()) {
            throw DataSourceException.connectionError("REST API is not available", null);
        }

        LOGGER.info("REST API data source '{}' refreshed", getName());
    }
    
    @Override
    public void shutdown() {
        LOGGER.info("Shutting down REST API data source '{}': baseUrl='{}', circuitBreakerEnabled={}",
            sourceName(), configuredBaseUrl(), circuitBreaker != null);
        cacheManager.clear();
        if (circuitBreaker != null) {
            circuitBreaker.shutdown();
        }
        connectionStatus = ConnectionStatus.shutdown();
        LOGGER.info("REST API data source '{}' shut down", getName());
    }
    
    /**
     * Execute API call for the given data type and parameters.
     */
    private Object executeApiCall(String dataType, Object... parameters) throws Exception {
        String endpoint = buildEndpoint(dataType, parameters);
        LOGGER.debug("Executing REST API call for '{}': dataType='{}', endpoint='{}'", sourceName(), dataType, endpoint);
        HttpRequest request = buildHttpRequest(endpoint, "GET", null);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return parseResponse(response.body());
        } else {
            throw new DataSourceException(DataSourceException.ErrorType.EXECUTION_ERROR,
                "API call failed with status: " + response.statusCode(),
                null, configuration.getName(), "executeApiCall", true);
        }
    }
    
    /**
     * Build endpoint URL from data type and parameters.
     */
    private String buildEndpoint(String dataType, Object... parameters) {
        String template = getEndpointForDataType(dataType);
        if (template == null) {
            throw new IllegalArgumentException("No endpoint defined for data type: " + dataType);
        }
        
        // Replace path variables with parameter values
        String endpoint = template;
        String[] paramNames = configuration.getParameterNames();
        
        if (paramNames != null) {
            for (int i = 0; i < parameters.length && i < paramNames.length; i++) {
                String placeholder = "{" + paramNames[i] + "}";
                endpoint = endpoint.replace(placeholder, parameters[i].toString());
            }
        } else {
            // Use generic parameter replacement
            for (int i = 0; i < parameters.length; i++) {
                String placeholder = "{param" + (i + 1) + "}";
                endpoint = endpoint.replace(placeholder, parameters[i].toString());
            }
        }
        
        // Ensure it's a complete URL
        if (!endpoint.startsWith("http")) {
            String baseUrl = configuration.getConnection().getBaseUrl();
            if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
                endpoint = baseUrl + endpoint.substring(1);
            } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
                endpoint = baseUrl + "/" + endpoint;
            } else {
                endpoint = baseUrl + endpoint;
            }
        }
        
        return endpoint;
    }
    
    /**
     * Build endpoint URL from query and parameters.
     */
    private String buildEndpoint(String query, Map<String, Object> parameters) {
        LOGGER.debug("Building REST API endpoint for '{}': query='{}', parameters={}", sourceName(), query, parameters);
        String endpoint = query;

        // Replace named parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            endpoint = endpoint.replace(placeholder, entry.getValue().toString());
            LOGGER.debug("Applied REST API path parameter for '{}': placeholder='{}', value='{}'", sourceName(), placeholder, entry.getValue());
        }

        // Ensure it's a complete URL
        if (!endpoint.startsWith("http")) {
            String baseUrl = configuration.getConnection().getBaseUrl();
            LOGGER.debug("Joining REST API base URL for '{}': baseUrl='{}', endpoint='{}'", sourceName(), baseUrl, endpoint);
            if (baseUrl.endsWith("/") && endpoint.startsWith("/")) {
                endpoint = baseUrl + endpoint.substring(1);
            } else if (!baseUrl.endsWith("/") && !endpoint.startsWith("/")) {
                endpoint = baseUrl + "/" + endpoint;
            } else {
                endpoint = baseUrl + endpoint;
            }
        }

        LOGGER.debug("Built REST API endpoint for '{}': endpoint='{}'", sourceName(), endpoint);
        return endpoint;
    }

    /**
     * Resolve named query from configuration.
     * Checks both queries and endpoints maps for the query name.
     */
    private String resolveNamedQuery(String query) {
        LOGGER.debug("Resolving REST API named query for '{}': query='{}'", sourceName(), query);

        // First check queries map (for backward compatibility)
        if (configuration.getQueries() != null && configuration.getQueries().containsKey(query)) {
            String result = configuration.getQueries().get(query);
            LOGGER.debug("Resolved REST API named query from queries map for '{}': query='{}', resolved='{}'", sourceName(), query, result);
            return result;
        }

        // Then check endpoints map (for REST API endpoint names)
        if (configuration.getEndpoints() != null && configuration.getEndpoints().containsKey(query)) {
            String result = configuration.getEndpoints().get(query);
            LOGGER.debug("Resolved REST API named query from endpoints map for '{}': query='{}', resolved='{}'", sourceName(), query, result);
            return result;
        }

        LOGGER.debug("REST API named query not found for '{}': query='{}', using literal value", sourceName(), query);
        return query; // Return as-is if not found in named queries or endpoints
    }

    /**
     * Build HTTP request with authentication and headers.
     */
    private HttpRequest buildHttpRequest(String url, String method, String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(getTimeoutMillis()));
        
        // Set HTTP method
        switch (method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "POST":
                builder.POST(body != null ? 
                    HttpRequest.BodyPublishers.ofString(body) : 
                    HttpRequest.BodyPublishers.noBody());
                break;
            case "PUT":
                builder.PUT(body != null ? 
                    HttpRequest.BodyPublishers.ofString(body) : 
                    HttpRequest.BodyPublishers.noBody());
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        // Add authentication headers
        addAuthenticationHeaders(builder);
        
        // Add custom headers
        addCustomHeaders(builder);
        
        return builder.build();
    }
    
    /**
     * Add authentication headers to the request.
     */
    private void addAuthenticationHeaders(HttpRequest.Builder builder) {
        if (configuration.getAuthentication() == null) {
            return;
        }
        
        String authHeaderValue = configuration.getAuthentication().getAuthorizationHeaderValue();
        if (authHeaderValue != null) {
            String headerName = configuration.getAuthentication().getTokenHeader();
            if (headerName == null) {
                headerName = "Authorization";
            }
            builder.header(headerName, authHeaderValue);
        }
        
        // Handle API key authentication
        if ("api-key".equals(configuration.getAuthentication().getType())) {
            String apiKey = configuration.getAuthentication().getApiKey();
            String apiKeyHeader = configuration.getAuthentication().getApiKeyHeader();
            if (apiKey != null && apiKeyHeader != null) {
                builder.header(apiKeyHeader, apiKey);
            }
        }
    }
    
    /**
     * Add custom headers to the request.
     */
    private void addCustomHeaders(HttpRequest.Builder builder) {
        if (configuration.getConnection().getHeaders() != null) {
            configuration.getConnection().getHeaders().forEach(builder::header);
        }
        
        // Add default headers
        builder.header("Accept", "application/json");
        builder.header("User-Agent", "SpEL-Rules-Engine/1.0");
    }
    
    /**
     * Get the endpoint template for a specific data type.
     */
    private String getEndpointForDataType(String dataType) {
        if (configuration.getEndpoints().containsKey(dataType)) {
            return configuration.getEndpoints().get(dataType);
        }
        return configuration.getEndpoints().get("default");
    }
    
    /**
     * Get the health check endpoint.
     */
    private String getHealthEndpoint() {
        if (configuration.getHealthCheck() != null && configuration.getHealthCheck().getEndpoint() != null) {
            return buildEndpoint(configuration.getHealthCheck().getEndpoint(), Collections.emptyMap());
        }
        return configuration.getConnection().getBaseUrl() + "/health";
    }
    
    /**
     * Parse JSON response to a generic object.
     */
    private Object parseResponse(String responseBody) {
        // Simple JSON parsing - in a real implementation, you'd use Jackson or similar
        if (responseBody.trim().startsWith("{")) {
            return parseJsonObject(responseBody);
        } else if (responseBody.trim().startsWith("[")) {
            return parseJsonArray(responseBody);
        } else {
            return responseBody;
        }
    }
    
    /**
     * Parse JSON response to a list.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> parseResponseToList(String responseBody) {
        Object parsed = parseResponse(responseBody);
        LOGGER.debug("Parsed REST API response for '{}': parsedType='{}'", sourceName(), parsed != null ? parsed.getClass().getName() : "null");

        if (parsed instanceof List) {
            return (List<T>) parsed;
        } else {
            List<T> result = Collections.singletonList((T) parsed);
            LOGGER.debug("Wrapped non-list REST API response for '{}': resultSize={}", sourceName(), result.size());
            return result;
        }
    }
    
    /**
     * Simple JSON object parsing (placeholder implementation).
     */
    private Map<String, Object> parseJsonObject(String json) {
        // Use Jackson ObjectMapper for proper JSON parsing
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(json, Map.class);
            LOGGER.debug("Parsed JSON object response for '{}': keys={}", sourceName(), result.keySet());
            return result;
        } catch (Exception e) {
            LOGGER.error("JSON parsing failed for REST API '{}': error={}", sourceName(), e.getMessage());
            LOGGER.debug("JSON parsing failure for REST API '{}':", sourceName(), e);
            // Fallback to raw JSON if parsing fails
            Map<String, Object> result = new HashMap<>();
            result.put("raw", json);
            return result;
        }
    }
    
    /**
     * Simple JSON array parsing (placeholder implementation).
     */
    private List<Object> parseJsonArray(String json) {
        // This is a very basic implementation - use Jackson in production
        List<Object> result = new ArrayList<>();
        result.add(json);
        return result;
    }
    
    /**
     * Get timeout in milliseconds.
     */
    private long getTimeoutMillis() {
        if (configuration.getConnection().getTimeout() != null) {
            return configuration.getConnection().getTimeout();
        }
        return 30000L; // Default 30 seconds
    }

    private String sourceName() {
        return configuration != null && configuration.getName() != null ? configuration.getName() : "rest-api-source";
    }

    private String configuredBaseUrl() {
        return configuration != null && configuration.getConnection() != null ? configuration.getConnection().getBaseUrl() : null;
    }

    private Integer configuredTimeout() {
        return configuration != null && configuration.getConnection() != null ? configuration.getConnection().getTimeout() : null;
    }
}
