package dev.mars.apex.core.config.datasource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for authentication settings.
 * 
 * This class contains authentication-related configuration for various
 * authentication methods including basic auth, bearer tokens, OAuth2,
 * API keys, and certificate-based authentication.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public class AuthenticationConfig {
    
    /**
     * Enumeration of supported authentication types.
     */
    public enum AuthenticationType {
        NONE("none", "No authentication"),
        BASIC("basic", "Basic authentication (username/password)"),
        BEARER_TOKEN("bearer-token", "Bearer token authentication"),
        API_KEY("api-key", "API key authentication"),
        OAUTH2("oauth2", "OAuth 2.0 authentication"),
        CERTIFICATE("certificate", "Certificate-based authentication"),
        CUSTOM("custom", "Custom authentication implementation");
        
        private final String code;
        private final String description;
        
        AuthenticationType(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
        
        public static AuthenticationType fromCode(String code) {
            if (code == null) {
                return NONE;
            }
            
            for (AuthenticationType type : values()) {
                if (type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
            
            return NONE;
        }
    }
    
    private String type = "none";
    private String username;
    private String password;
    private String token;
    private String apiKey;
    private String apiKeyHeader = "X-API-Key";
    private String tokenHeader = "Authorization";
    private String tokenPrefix = "Bearer ";
    
    // OAuth2 configuration
    private String clientId;
    private String clientSecret;
    private String tokenUrl;
    private String scope;
    private String grantType = "client_credentials";
    
    // Certificate configuration
    private String certificatePath;
    private String certificatePassword;
    private String keyStorePath;
    private String keyStorePassword;
    private String keyStoreType = "JKS";
    private String trustStorePath;
    private String trustStorePassword;
    private String trustStoreType = "JKS";
    
    // Custom authentication
    private String customImplementation;
    private Map<String, Object> customProperties;
    
    // Token refresh configuration
    private Boolean autoRefresh = true;
    private Long refreshThresholdSeconds = 300L; // 5 minutes before expiry
    private Integer maxRefreshAttempts = 3;
    
    /**
     * Default constructor.
     */
    public AuthenticationConfig() {
        this.customProperties = new HashMap<>();
    }
    
    /**
     * Constructor with authentication type.
     * 
     * @param type The authentication type
     */
    public AuthenticationConfig(String type) {
        this();
        this.type = type;
    }
    
    // Basic configuration
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public AuthenticationType getAuthenticationType() {
        return AuthenticationType.fromCode(type);
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
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getApiKeyHeader() {
        return apiKeyHeader;
    }
    
    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }
    
    public String getTokenHeader() {
        return tokenHeader;
    }
    
    public void setTokenHeader(String tokenHeader) {
        this.tokenHeader = tokenHeader;
    }
    
    public String getTokenPrefix() {
        return tokenPrefix;
    }
    
    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }
    
    // OAuth2 configuration
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getTokenUrl() {
        return tokenUrl;
    }
    
    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getGrantType() {
        return grantType;
    }
    
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
    
    // Certificate configuration
    
    public String getCertificatePath() {
        return certificatePath;
    }
    
    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }
    
    public String getCertificatePassword() {
        return certificatePassword;
    }
    
    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }
    
    public String getKeyStorePath() {
        return keyStorePath;
    }
    
    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }
    
    public String getKeyStorePassword() {
        return keyStorePassword;
    }
    
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }
    
    public String getKeyStoreType() {
        return keyStoreType;
    }
    
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }
    
    public String getTrustStorePath() {
        return trustStorePath;
    }
    
    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }
    
    public String getTrustStorePassword() {
        return trustStorePassword;
    }
    
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
    
    public String getTrustStoreType() {
        return trustStoreType;
    }
    
    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }
    
    // Custom authentication
    
    public String getCustomImplementation() {
        return customImplementation;
    }
    
    public void setCustomImplementation(String customImplementation) {
        this.customImplementation = customImplementation;
    }
    
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
    
    // Token refresh configuration
    
    public Boolean getAutoRefresh() {
        return autoRefresh;
    }
    
    public void setAutoRefresh(Boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }
    
    public boolean isAutoRefreshEnabled() {
        return autoRefresh != null && autoRefresh;
    }
    
    public Long getRefreshThresholdSeconds() {
        return refreshThresholdSeconds;
    }
    
    public void setRefreshThresholdSeconds(Long refreshThresholdSeconds) {
        this.refreshThresholdSeconds = refreshThresholdSeconds;
    }
    
    public Integer getMaxRefreshAttempts() {
        return maxRefreshAttempts;
    }
    
    public void setMaxRefreshAttempts(Integer maxRefreshAttempts) {
        this.maxRefreshAttempts = maxRefreshAttempts;
    }
    
    // Utility methods
    
    /**
     * Check if authentication is enabled.
     * 
     * @return true if authentication is enabled
     */
    public boolean isAuthenticationEnabled() {
        return getAuthenticationType() != AuthenticationType.NONE;
    }
    
    /**
     * Get the formatted authorization header value.
     * 
     * @return Authorization header value, or null if not applicable
     */
    public String getAuthorizationHeaderValue() {
        AuthenticationType authType = getAuthenticationType();
        
        switch (authType) {
            case BEARER_TOKEN:
                return token != null ? tokenPrefix + token : null;
            case API_KEY:
                return apiKey; // API key goes in its own header
            case BASIC:
                if (username != null && password != null) {
                    String credentials = username + ":" + password;
                    return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
                }
                return null;
            default:
                return null;
        }
    }
    
    // Validation
    
    /**
     * Validate the authentication configuration.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        AuthenticationType authType = getAuthenticationType();
        
        switch (authType) {
            case BASIC:
                if (username == null || username.trim().isEmpty()) {
                    throw new IllegalArgumentException("Username is required for basic authentication");
                }
                if (password == null || password.trim().isEmpty()) {
                    throw new IllegalArgumentException("Password is required for basic authentication");
                }
                break;
                
            case BEARER_TOKEN:
                if (token == null || token.trim().isEmpty()) {
                    throw new IllegalArgumentException("Token is required for bearer token authentication");
                }
                break;
                
            case API_KEY:
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new IllegalArgumentException("API key is required for API key authentication");
                }
                if (apiKeyHeader == null || apiKeyHeader.trim().isEmpty()) {
                    throw new IllegalArgumentException("API key header is required for API key authentication");
                }
                break;
                
            case OAUTH2:
                if (clientId == null || clientId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Client ID is required for OAuth2 authentication");
                }
                if (clientSecret == null || clientSecret.trim().isEmpty()) {
                    throw new IllegalArgumentException("Client secret is required for OAuth2 authentication");
                }
                if (tokenUrl == null || tokenUrl.trim().isEmpty()) {
                    throw new IllegalArgumentException("Token URL is required for OAuth2 authentication");
                }
                break;
                
            case CERTIFICATE:
                if (certificatePath == null || certificatePath.trim().isEmpty()) {
                    throw new IllegalArgumentException("Certificate path is required for certificate authentication");
                }
                break;
                
            case CUSTOM:
                if (customImplementation == null || customImplementation.trim().isEmpty()) {
                    throw new IllegalArgumentException("Custom implementation class is required for custom authentication");
                }
                break;
                
            case NONE:
                // No validation needed
                break;
        }
        
        if (refreshThresholdSeconds != null && refreshThresholdSeconds <= 0) {
            throw new IllegalArgumentException("Refresh threshold must be positive");
        }
        
        if (maxRefreshAttempts != null && maxRefreshAttempts <= 0) {
            throw new IllegalArgumentException("Max refresh attempts must be positive");
        }
    }
    
    /**
     * Create a copy of this authentication configuration.
     * Note: Sensitive information like passwords and tokens are also copied.
     * 
     * @return A new AuthenticationConfig with the same settings
     */
    public AuthenticationConfig copy() {
        AuthenticationConfig copy = new AuthenticationConfig();
        copy.type = this.type;
        copy.username = this.username;
        copy.password = this.password;
        copy.token = this.token;
        copy.apiKey = this.apiKey;
        copy.apiKeyHeader = this.apiKeyHeader;
        copy.tokenHeader = this.tokenHeader;
        copy.tokenPrefix = this.tokenPrefix;
        copy.clientId = this.clientId;
        copy.clientSecret = this.clientSecret;
        copy.tokenUrl = this.tokenUrl;
        copy.scope = this.scope;
        copy.grantType = this.grantType;
        copy.certificatePath = this.certificatePath;
        copy.certificatePassword = this.certificatePassword;
        copy.keyStorePath = this.keyStorePath;
        copy.keyStorePassword = this.keyStorePassword;
        copy.keyStoreType = this.keyStoreType;
        copy.trustStorePath = this.trustStorePath;
        copy.trustStorePassword = this.trustStorePassword;
        copy.trustStoreType = this.trustStoreType;
        copy.customImplementation = this.customImplementation;
        copy.customProperties = new HashMap<>(this.customProperties);
        copy.autoRefresh = this.autoRefresh;
        copy.refreshThresholdSeconds = this.refreshThresholdSeconds;
        copy.maxRefreshAttempts = this.maxRefreshAttempts;
        return copy;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationConfig that = (AuthenticationConfig) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(username, that.username) &&
               Objects.equals(apiKeyHeader, that.apiKeyHeader) &&
               Objects.equals(tokenHeader, that.tokenHeader);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, username, apiKeyHeader, tokenHeader);
    }
    
    @Override
    public String toString() {
        return "AuthenticationConfig{" +
               "type='" + type + '\'' +
               ", username='" + username + '\'' +
               ", apiKeyHeader='" + apiKeyHeader + '\'' +
               ", tokenHeader='" + tokenHeader + '\'' +
               ", autoRefresh=" + autoRefresh +
               '}';
    }
}
