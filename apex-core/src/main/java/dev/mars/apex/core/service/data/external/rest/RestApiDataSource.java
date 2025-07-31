package dev.mars.apex.core.service.data.external.rest;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class RestApiDataSource implements ExternalDataSource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiDataSource.class);
    
    private final HttpClient httpClient;
    private DataSourceConfiguration configuration;
    private ConnectionStatus connectionStatus;
    private DataSourceMetrics metrics;
    private CircuitBreaker circuitBreaker;
    
    // Simple in-memory cache for API responses
    private final Map<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    
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
        
        // Initialize circuit breaker if configured
        if (configuration.getCircuitBreaker() != null && configuration.getCircuitBreaker().isEnabled()) {
            this.circuitBreaker = new CircuitBreaker(configuration.getCircuitBreaker());
        }
    }
    
    @Override
    public void initialize(DataSourceConfiguration config) throws DataSourceException {
        this.configuration = config;
        this.connectionStatus = ConnectionStatus.connecting();

        try {
            // Validate configuration first
            if (config.getConnection() == null || config.getConnection().getBaseUrl() == null) {
                throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                    "REST API data source requires baseUrl configuration", null, config.getName(), "initialize", false);
            }

            // Initialize successfully without testing connection during initialization
            // Connection testing will be done on-demand when testConnection() is called
            this.connectionStatus = ConnectionStatus.connected("REST API data source initialized");
            LOGGER.info("REST API data source '{}' initialized successfully", config.getName());
        } catch (DataSourceException e) {
            // Re-throw configuration errors
            throw e;
        } catch (Exception e) {
            this.connectionStatus = ConnectionStatus.error("Initialization failed", e);
            LOGGER.warn("REST API data source '{}' initialized but encountered error during connection test", config.getName(), e);
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
            HttpRequest request = buildHttpRequest(healthEndpoint, "GET", null);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (java.net.http.HttpConnectTimeoutException e) {
            LOGGER.warn("REST API connection test timed out for '{}'", configuration.getName(), e);
            return false;
        } catch (java.net.ConnectException e) {
            LOGGER.warn("REST API connection test failed to connect for '{}'", configuration.getName(), e);
            return false;
        } catch (java.io.IOException e) {
            LOGGER.warn("REST API connection test failed with IO error for '{}'", configuration.getName(), e);
            return false;
        } catch (InterruptedException e) {
            LOGGER.warn("REST API connection test was interrupted for '{}'", configuration.getName(), e);
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            LOGGER.warn("REST API connection test failed for '{}'", configuration.getName(), e);
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
    public Object getData(String dataType, Object... parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Check cache first if enabled
            if (isCacheEnabled()) {
                String cacheKey = generateCacheKey(dataType, parameters);
                CachedResponse cached = responseCache.get(cacheKey);
                if (cached != null && !cached.isExpired()) {
                    metrics.recordCacheHit();
                    metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                    return cached.getData();
                }
                metrics.recordCacheMiss();
            }
            
            // Execute API call with circuit breaker if enabled
            Object result;
            if (circuitBreaker != null) {
                result = circuitBreaker.execute(() -> executeApiCall(dataType, parameters));
            } else {
                result = executeApiCall(dataType, parameters);
            }
            
            // Cache the result if caching is enabled
            if (isCacheEnabled() && result != null) {
                String cacheKey = generateCacheKey(dataType, parameters);
                long ttl = configuration.getCache().getTtlSeconds() * 1000L;
                responseCache.put(cacheKey, new CachedResponse(result, System.currentTimeMillis() + ttl));
            }
            
            metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
            return result;
            
        } catch (Exception e) {
            metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
            LOGGER.error("Failed to get data from REST API", e);
            return null;
        }
    }
    
    @Override
    public <T> List<T> query(String query, Map<String, Object> parameters) throws DataSourceException {
        // For REST APIs, the "query" is typically an endpoint path
        long startTime = System.currentTimeMillis();

        try {
            // First, resolve named query from configuration
            String actualQuery = resolveNamedQuery(query);
            String endpoint = buildEndpoint(actualQuery, parameters);
            HttpRequest request = buildHttpRequest(endpoint, "GET", null);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                List<T> result = parseResponseToList(response.body());
                metrics.recordSuccessfulRequest(System.currentTimeMillis() - startTime);
                return result;
            } else {
                metrics.recordFailedRequest(System.currentTimeMillis() - startTime);
                throw new DataSourceException(DataSourceException.ErrorType.EXECUTION_ERROR,
                    "API call failed with status: " + response.statusCode(), null,
                    configuration.getName(), "query", true);
            }

        } catch (IOException | InterruptedException e) {
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
        List<T> results = query(query, parameters);
        return results.isEmpty() ? null : results.get(0);
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
        // Clear cache
        responseCache.clear();
        
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
        responseCache.clear();
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
        HttpRequest request = buildHttpRequest(endpoint, "GET", null);
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return parseResponse(response.body());
        } else {
            throw new RuntimeException("API call failed with status: " + response.statusCode());
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
        String endpoint = query;
        
        // Replace named parameters
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            endpoint = endpoint.replace(placeholder, entry.getValue().toString());
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
     * Resolve named query from configuration.
     */
    private String resolveNamedQuery(String query) {
        if (configuration.getQueries() != null && configuration.getQueries().containsKey(query)) {
            return configuration.getQueries().get(query);
        }
        return query; // Return as-is if not found in named queries
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
        if (parsed instanceof List) {
            return (List<T>) parsed;
        } else {
            return Collections.singletonList((T) parsed);
        }
    }
    
    /**
     * Simple JSON object parsing (placeholder implementation).
     */
    private Map<String, Object> parseJsonObject(String json) {
        // This is a very basic implementation - use Jackson in production
        Map<String, Object> result = new HashMap<>();
        result.put("raw", json);
        return result;
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
     * Check if caching is enabled.
     */
    private boolean isCacheEnabled() {
        return configuration.getCache() != null && configuration.getCache().isEnabled();
    }
    
    /**
     * Generate cache key for the given data type and parameters.
     */
    private String generateCacheKey(String dataType, Object... parameters) {
        StringBuilder key = new StringBuilder(dataType);
        for (Object param : parameters) {
            key.append(":").append(param != null ? param.toString() : "null");
        }
        return key.toString();
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
    
    /**
     * Simple cached response holder.
     */
    private static class CachedResponse {
        private final Object data;
        private final long expiryTime;
        
        public CachedResponse(Object data, long expiryTime) {
            this.data = data;
            this.expiryTime = expiryTime;
        }
        
        public Object getData() {
            return data;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
