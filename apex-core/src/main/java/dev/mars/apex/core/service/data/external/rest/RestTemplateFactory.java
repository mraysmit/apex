package dev.mars.apex.core.service.data.external.rest;

import dev.mars.apex.core.config.datasource.DataSourceConfiguration;
import dev.mars.apex.core.service.data.external.DataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory class for creating HTTP clients for REST API data sources.
 * 
 * This factory creates and manages HttpClient instances with appropriate
 * configurations for timeouts, authentication, and connection pooling.
 * 
 * Features:
 * - HttpClient creation and caching
 * - Timeout configuration
 * - Connection pooling
 * - SSL/TLS configuration
 * - Proxy support
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class RestTemplateFactory {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateFactory.class);
    
    // Cache of created HTTP clients to avoid recreating them
    private static final ConcurrentMap<String, HttpClient> HTTP_CLIENT_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Create an HttpClient from the given configuration.
     * 
     * @param config The data source configuration
     * @return Configured HttpClient
     * @throws DataSourceException if HttpClient creation fails
     */
    public static HttpClient createHttpClient(DataSourceConfiguration config) throws DataSourceException {
        String cacheKey = generateCacheKey(config);
        
        // Return cached HttpClient if available
        HttpClient cachedClient = HTTP_CLIENT_CACHE.get(cacheKey);
        if (cachedClient != null) {
            LOGGER.debug("Returning cached HttpClient for '{}'", config.getName());
            return cachedClient;
        }
        
        try {
            HttpClient httpClient = createNewHttpClient(config);
            
            // Cache the HttpClient
            HTTP_CLIENT_CACHE.put(cacheKey, httpClient);
            
            LOGGER.info("Created and cached HttpClient for '{}'", config.getName());
            
            return httpClient;
            
        } catch (Exception e) {
            throw new DataSourceException(DataSourceException.ErrorType.CONFIGURATION_ERROR,
                "Failed to create HttpClient for '" + config.getName() + "'", e,
                config.getName(), "createHttpClient", false);
        }
    }
    
    /**
     * Create a new HttpClient instance.
     */
    private static HttpClient createNewHttpClient(DataSourceConfiguration config) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        
        // Configure timeouts
        configureTimeouts(builder, config);
        
        // Configure connection settings
        configureConnection(builder, config);
        
        // Configure SSL/TLS
        configureSsl(builder, config);
        
        // Configure proxy if needed
        configureProxy(builder, config);
        
        return builder.build();
    }
    
    /**
     * Configure timeout settings.
     */
    private static void configureTimeouts(HttpClient.Builder builder, DataSourceConfiguration config) {
        if (config.getConnection() != null && config.getConnection().getTimeout() != null) {
            Duration timeout = Duration.ofMillis(config.getConnection().getTimeout());
            builder.connectTimeout(timeout);
            LOGGER.debug("Set HTTP client timeout to {}ms for '{}'", 
                config.getConnection().getTimeout(), config.getName());
        } else {
            // Default timeout
            builder.connectTimeout(Duration.ofSeconds(30));
        }
    }
    
    /**
     * Configure connection settings.
     */
    private static void configureConnection(HttpClient.Builder builder, DataSourceConfiguration config) {
        // Set HTTP version preference
        builder.version(HttpClient.Version.HTTP_2);
        
        // Configure redirect policy
        builder.followRedirects(HttpClient.Redirect.NORMAL);
        
        LOGGER.debug("Configured HTTP client connection settings for '{}'", config.getName());
    }
    
    /**
     * Configure SSL/TLS settings.
     */
    private static void configureSsl(HttpClient.Builder builder, DataSourceConfiguration config) {
        if (config.getConnection() != null && config.getConnection().isSslEnabled()) {
            try {
                // In a production environment, you would configure custom SSL context
                // For now, we'll use the default SSL context
                LOGGER.debug("SSL enabled for HTTP client '{}'", config.getName());
                
                // Custom SSL configuration would go here
                // SSLContext sslContext = createCustomSslContext(config);
                // builder.sslContext(sslContext);
                
            } catch (Exception e) {
                LOGGER.warn("Failed to configure SSL for HTTP client '{}': {}", 
                    config.getName(), e.getMessage());
            }
        }
    }
    
    /**
     * Configure proxy settings.
     */
    private static void configureProxy(HttpClient.Builder builder, DataSourceConfiguration config) {
        // Proxy configuration would be added here if needed
        // This could be extended to support proxy settings from configuration
        
        // Example:
        // if (config.getConnection().getProxyHost() != null) {
        //     ProxySelector proxySelector = ProxySelector.of(
        //         new InetSocketAddress(config.getConnection().getProxyHost(), 
        //                              config.getConnection().getProxyPort()));
        //     builder.proxy(proxySelector);
        // }
        
        LOGGER.debug("Proxy configuration checked for '{}'", config.getName());
    }
    
    /**
     * Create a simple HttpClient with default settings.
     * 
     * @return Default HttpClient
     */
    public static HttpClient createDefaultHttpClient() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    /**
     * Create an HttpClient with custom timeout.
     * 
     * @param timeoutMillis Timeout in milliseconds
     * @return HttpClient with custom timeout
     */
    public static HttpClient createHttpClientWithTimeout(long timeoutMillis) {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(timeoutMillis))
            .build();
    }
    
    /**
     * Test the HttpClient by making a simple request.
     * 
     * @param httpClient The HttpClient to test
     * @param config The data source configuration
     * @return true if the test passes
     */
    public static boolean testHttpClient(HttpClient httpClient, DataSourceConfiguration config) {
        try {
            String baseUrl = config.getConnection().getBaseUrl();
            if (baseUrl == null) {
                LOGGER.warn("No base URL configured for HTTP client test");
                return false;
            }
            
            // Create a simple HEAD request to test connectivity
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl))
                .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(10))
                .build();
            
            java.net.http.HttpResponse<Void> response = httpClient.send(request, 
                java.net.http.HttpResponse.BodyHandlers.discarding());
            
            boolean success = response.statusCode() < 500; // Accept any non-server-error response
            
            if (success) {
                LOGGER.debug("HTTP client test successful for '{}' - Status: {}", 
                    config.getName(), response.statusCode());
            } else {
                LOGGER.warn("HTTP client test failed for '{}' - Status: {}", 
                    config.getName(), response.statusCode());
            }
            
            return success;
            
        } catch (Exception e) {
            LOGGER.warn("HTTP client test failed for '{}': {}", config.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate cache key for HttpClient.
     */
    private static String generateCacheKey(DataSourceConfiguration config) {
        StringBuilder key = new StringBuilder();
        key.append(config.getName()).append(":");
        
        if (config.getConnection() != null) {
            key.append(config.getConnection().getBaseUrl()).append(":");
            key.append(config.getConnection().getTimeout()).append(":");
            key.append(config.getConnection().isSslEnabled());
        }
        
        return key.toString();
    }
    
    /**
     * Remove HttpClient from cache.
     * 
     * @param config The data source configuration
     */
    public static void removeFromCache(DataSourceConfiguration config) {
        String cacheKey = generateCacheKey(config);
        HttpClient removed = HTTP_CLIENT_CACHE.remove(cacheKey);
        if (removed != null) {
            LOGGER.info("Removed HttpClient from cache for '{}'", config.getName());
        }
    }
    
    /**
     * Clear all cached HttpClients.
     */
    public static void clearCache() {
        int size = HTTP_CLIENT_CACHE.size();
        HTTP_CLIENT_CACHE.clear();
        LOGGER.info("Cleared {} HttpClients from cache", size);
    }
    
    /**
     * Get the number of cached HttpClients.
     * 
     * @return Number of cached HttpClients
     */
    public static int getCacheSize() {
        return HTTP_CLIENT_CACHE.size();
    }
    
    /**
     * Check if an HttpClient is cached for the given configuration.
     * 
     * @param config The data source configuration
     * @return true if HttpClient is cached
     */
    public static boolean isCached(DataSourceConfiguration config) {
        String cacheKey = generateCacheKey(config);
        return HTTP_CLIENT_CACHE.containsKey(cacheKey);
    }
    
    /**
     * Create an HttpClient builder with common settings.
     * This can be used as a starting point for custom configurations.
     * 
     * @return HttpClient.Builder with common settings
     */
    public static HttpClient.Builder createBaseBuilder() {
        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30));
    }
    
    /**
     * Shutdown and cleanup resources.
     * This method should be called when the application is shutting down.
     */
    public static void shutdown() {
        clearCache();
        LOGGER.info("RestTemplateFactory shut down");
    }
}
