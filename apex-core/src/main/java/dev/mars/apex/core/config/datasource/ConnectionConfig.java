package dev.mars.apex.core.config.datasource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for data source connections.
 * 
 * This class contains connection-specific settings such as host, port,
 * database name, URLs, timeouts, and connection pooling parameters.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class ConnectionConfig {
    
    // Database connection properties
    private String host;
    private Integer port;
    private String database;
    private String schema;
    private String username;
    private String password;
    private boolean sslEnabled = false;
    private String trustStore;
    private String trustStorePassword;
    
    // HTTP/REST API connection properties
    private String baseUrl;
    private Integer timeout = 30000; // 30 seconds default
    private Integer retryAttempts = 3;
    private Integer retryDelay = 1000; // 1 second default
    private Map<String, String> headers;
    
    // Message queue connection properties
    private String bootstrapServers;
    private String securityProtocol;
    private String saslMechanism;
    
    // File system connection properties
    private String basePath;
    private String filePattern;
    private Integer pollingInterval;
    private String encoding = "UTF-8";
    
    // Connection pooling configuration
    private ConnectionPoolConfig connectionPool;
    
    // Custom connection properties
    private Map<String, Object> customProperties;
    
    /**
     * Default constructor.
     */
    public ConnectionConfig() {
        this.headers = new HashMap<>();
        this.customProperties = new HashMap<>();
    }
    
    // Database connection properties
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getSchema() {
        return schema;
    }
    
    public void setSchema(String schema) {
        this.schema = schema;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isSslEnabled() {
        return sslEnabled;
    }
    
    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }
    
    public String getTrustStore() {
        return trustStore;
    }
    
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }
    
    public String getTrustStorePassword() {
        return trustStorePassword;
    }
    
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
    
    // HTTP/REST API connection properties
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public Integer getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    public Integer getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(Integer retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    public Integer getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(Integer retryDelay) {
        this.retryDelay = retryDelay;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers != null ? headers : new HashMap<>();
    }
    
    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }
    
    // Message queue connection properties
    
    public String getBootstrapServers() {
        return bootstrapServers;
    }
    
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }
    
    public String getSecurityProtocol() {
        return securityProtocol;
    }
    
    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }
    
    public String getSaslMechanism() {
        return saslMechanism;
    }
    
    public void setSaslMechanism(String saslMechanism) {
        this.saslMechanism = saslMechanism;
    }
    
    // File system connection properties
    
    public String getBasePath() {
        return basePath;
    }
    
    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
    
    public String getFilePattern() {
        return filePattern;
    }
    
    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }
    
    public Integer getPollingInterval() {
        return pollingInterval;
    }
    
    public void setPollingInterval(Integer pollingInterval) {
        this.pollingInterval = pollingInterval;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    // Connection pooling
    
    public ConnectionPoolConfig getConnectionPool() {
        return connectionPool;
    }
    
    public void setConnectionPool(ConnectionPoolConfig connectionPool) {
        this.connectionPool = connectionPool;
    }
    
    // Custom properties
    
    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }
    
    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties != null ? customProperties : new HashMap<>();
    }
    
    public Object getCustomProperty(String key) {
        return customProperties.get(key);
    }
    
    public void setCustomProperty(String key, Object value) {
        customProperties.put(key, value);
    }
    
    // Validation
    
    /**
     * Validate the connection configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        // Validation logic can be added here based on specific requirements
        if (timeout != null && timeout <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        
        if (retryAttempts != null && retryAttempts < 0) {
            throw new IllegalArgumentException("Retry attempts cannot be negative");
        }
        
        if (retryDelay != null && retryDelay < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative");
        }
        
        if (connectionPool != null) {
            connectionPool.validate();
        }
    }
    
    /**
     * Create a copy of this connection configuration.
     * 
     * @return A new ConnectionConfig with the same settings
     */
    public ConnectionConfig copy() {
        ConnectionConfig copy = new ConnectionConfig();
        
        // Database properties
        copy.host = this.host;
        copy.port = this.port;
        copy.database = this.database;
        copy.schema = this.schema;
        copy.username = this.username;
        copy.password = this.password;
        copy.sslEnabled = this.sslEnabled;
        copy.trustStore = this.trustStore;
        copy.trustStorePassword = this.trustStorePassword;
        
        // HTTP properties
        copy.baseUrl = this.baseUrl;
        copy.timeout = this.timeout;
        copy.retryAttempts = this.retryAttempts;
        copy.retryDelay = this.retryDelay;
        copy.headers = new HashMap<>(this.headers);
        
        // Message queue properties
        copy.bootstrapServers = this.bootstrapServers;
        copy.securityProtocol = this.securityProtocol;
        copy.saslMechanism = this.saslMechanism;
        
        // File system properties
        copy.basePath = this.basePath;
        copy.filePattern = this.filePattern;
        copy.pollingInterval = this.pollingInterval;
        copy.encoding = this.encoding;
        
        // Connection pool
        copy.connectionPool = this.connectionPool != null ? this.connectionPool.copy() : null;
        
        // Custom properties
        copy.customProperties = new HashMap<>(this.customProperties);
        
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionConfig that = (ConnectionConfig) o;
        return sslEnabled == that.sslEnabled &&
               Objects.equals(host, that.host) &&
               Objects.equals(port, that.port) &&
               Objects.equals(database, that.database) &&
               Objects.equals(username, that.username) &&
               Objects.equals(baseUrl, that.baseUrl) &&
               Objects.equals(timeout, that.timeout);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(host, port, database, username, baseUrl, timeout, sslEnabled);
    }
    
    @Override
    public String toString() {
        return "ConnectionConfig{" +
               "host='" + host + '\'' +
               ", port=" + port +
               ", database='" + database + '\'' +
               ", baseUrl='" + baseUrl + '\'' +
               ", timeout=" + timeout +
               ", sslEnabled=" + sslEnabled +
               '}';
    }
}
