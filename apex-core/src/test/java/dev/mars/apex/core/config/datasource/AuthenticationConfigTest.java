package dev.mars.apex.core.config.datasource;

import dev.mars.apex.core.config.datasource.AuthenticationConfig.AuthenticationType;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for AuthenticationConfig.
 * 
 * Tests cover:
 * - Constructor behavior and initialization
 * - Authentication type enum functionality and conversion
 * - Basic authentication properties (username, password)
 * - Token-based authentication (bearer tokens, API keys)
 * - OAuth2 configuration (client credentials, token URL, scope)
 * - Certificate-based authentication (keystore, truststore)
 * - Custom authentication implementation
 * - Token refresh configuration
 * - Custom properties management
 * - Utility methods (authorization header generation, authentication status)
 * - Validation logic for all authentication types
 * - Copy method deep cloning behavior
 * - Equals and hashCode contracts
 * - ToString representation (without exposing sensitive data)
 * - Edge cases and error handling
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
class AuthenticationConfigTest {

    private AuthenticationConfig config;

    @BeforeEach
    void setUp() {
        config = new AuthenticationConfig();
    }

    // ========================================
    // Constructor Tests
    // ========================================

    @Test
    @DisplayName("Should initialize with default constructor")
    void testDefaultConstructor() {
        AuthenticationConfig config = new AuthenticationConfig();
        
        // Basic properties should have default values
        assertEquals("none", config.getType());
        assertEquals(AuthenticationType.NONE, config.getAuthenticationType());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
        assertNull(config.getToken());
        assertNull(config.getApiKey());
        assertEquals("X-API-Key", config.getApiKeyHeader()); // Default
        assertEquals("Authorization", config.getTokenHeader()); // Default
        assertEquals("Bearer ", config.getTokenPrefix()); // Default
        
        // OAuth2 properties should be null or default
        assertNull(config.getClientId());
        assertNull(config.getClientSecret());
        assertNull(config.getTokenUrl());
        assertNull(config.getScope());
        assertEquals("client_credentials", config.getGrantType()); // Default
        
        // Certificate properties should be null or default
        assertNull(config.getCertificatePath());
        assertNull(config.getCertificatePassword());
        assertNull(config.getKeyStorePath());
        assertNull(config.getKeyStorePassword());
        assertEquals("JKS", config.getKeyStoreType()); // Default
        assertNull(config.getTrustStorePath());
        assertNull(config.getTrustStorePassword());
        assertEquals("JKS", config.getTrustStoreType()); // Default
        
        // Custom authentication should be null
        assertNull(config.getCustomImplementation());
        
        // Token refresh should have defaults
        assertTrue(config.getAutoRefresh()); // Default is true
        assertEquals(300L, config.getRefreshThresholdSeconds()); // Default is 5 minutes
        assertEquals(3, config.getMaxRefreshAttempts()); // Default is 3
        
        // Custom properties should be initialized but empty
        assertNotNull(config.getCustomProperties());
        assertTrue(config.getCustomProperties().isEmpty());
    }

    @Test
    @DisplayName("Should initialize with type constructor")
    void testTypeConstructor() {
        AuthenticationConfig config = new AuthenticationConfig("basic");
        
        assertEquals("basic", config.getType());
        assertEquals(AuthenticationType.BASIC, config.getAuthenticationType());
        
        // Custom properties should still be initialized
        assertNotNull(config.getCustomProperties());
        assertTrue(config.getCustomProperties().isEmpty());
    }

    @Test
    @DisplayName("Should handle null type in constructor")
    void testTypeConstructorWithNull() {
        AuthenticationConfig config = new AuthenticationConfig(null);
        
        assertNull(config.getType());
        assertEquals(AuthenticationType.NONE, config.getAuthenticationType()); // fromCode handles null
    }

    // ========================================
    // AuthenticationType Enum Tests
    // ========================================

    @Test
    @DisplayName("Should have correct authentication type enum values")
    void testAuthenticationTypeEnumValues() {
        AuthenticationType[] types = AuthenticationType.values();
        assertEquals(7, types.length);
        
        assertEquals(AuthenticationType.NONE, types[0]);
        assertEquals(AuthenticationType.BASIC, types[1]);
        assertEquals(AuthenticationType.BEARER_TOKEN, types[2]);
        assertEquals(AuthenticationType.API_KEY, types[3]);
        assertEquals(AuthenticationType.OAUTH2, types[4]);
        assertEquals(AuthenticationType.CERTIFICATE, types[5]);
        assertEquals(AuthenticationType.CUSTOM, types[6]);
    }

    @Test
    @DisplayName("Should have correct authentication type codes")
    void testAuthenticationTypeCodes() {
        assertEquals("none", AuthenticationType.NONE.getCode());
        assertEquals("basic", AuthenticationType.BASIC.getCode());
        assertEquals("bearer-token", AuthenticationType.BEARER_TOKEN.getCode());
        assertEquals("api-key", AuthenticationType.API_KEY.getCode());
        assertEquals("oauth2", AuthenticationType.OAUTH2.getCode());
        assertEquals("certificate", AuthenticationType.CERTIFICATE.getCode());
        assertEquals("custom", AuthenticationType.CUSTOM.getCode());
    }

    @Test
    @DisplayName("Should have correct authentication type descriptions")
    void testAuthenticationTypeDescriptions() {
        assertEquals("No authentication", AuthenticationType.NONE.getDescription());
        assertEquals("Basic authentication (username/password)", AuthenticationType.BASIC.getDescription());
        assertEquals("Bearer token authentication", AuthenticationType.BEARER_TOKEN.getDescription());
        assertEquals("API key authentication", AuthenticationType.API_KEY.getDescription());
        assertEquals("OAuth 2.0 authentication", AuthenticationType.OAUTH2.getDescription());
        assertEquals("Certificate-based authentication", AuthenticationType.CERTIFICATE.getDescription());
        assertEquals("Custom authentication implementation", AuthenticationType.CUSTOM.getDescription());
    }

    @Test
    @DisplayName("Should convert code to authentication type")
    void testFromCodeConversion() {
        assertEquals(AuthenticationType.NONE, AuthenticationType.fromCode("none"));
        assertEquals(AuthenticationType.BASIC, AuthenticationType.fromCode("basic"));
        assertEquals(AuthenticationType.BEARER_TOKEN, AuthenticationType.fromCode("bearer-token"));
        assertEquals(AuthenticationType.API_KEY, AuthenticationType.fromCode("api-key"));
        assertEquals(AuthenticationType.OAUTH2, AuthenticationType.fromCode("oauth2"));
        assertEquals(AuthenticationType.CERTIFICATE, AuthenticationType.fromCode("certificate"));
        assertEquals(AuthenticationType.CUSTOM, AuthenticationType.fromCode("custom"));
    }

    @Test
    @DisplayName("Should handle case insensitive code conversion")
    void testFromCodeCaseInsensitive() {
        assertEquals(AuthenticationType.BASIC, AuthenticationType.fromCode("BASIC"));
        assertEquals(AuthenticationType.BASIC, AuthenticationType.fromCode("Basic"));
        assertEquals(AuthenticationType.BEARER_TOKEN, AuthenticationType.fromCode("BEARER-TOKEN"));
        assertEquals(AuthenticationType.API_KEY, AuthenticationType.fromCode("API-KEY"));
        assertEquals(AuthenticationType.OAUTH2, AuthenticationType.fromCode("OAUTH2"));
    }

    @Test
    @DisplayName("Should return NONE for unknown or null codes")
    void testFromCodeUnknown() {
        assertEquals(AuthenticationType.NONE, AuthenticationType.fromCode("unknown"));
        assertEquals(AuthenticationType.NONE, AuthenticationType.fromCode("invalid"));
        assertEquals(AuthenticationType.NONE, AuthenticationType.fromCode(""));
        assertEquals(AuthenticationType.NONE, AuthenticationType.fromCode(null));
    }

    @Test
    @DisplayName("Should convert type string to AuthenticationType enum")
    void testGetAuthenticationType() {
        config.setType("basic");
        assertEquals(AuthenticationType.BASIC, config.getAuthenticationType());
        
        config.setType("bearer-token");
        assertEquals(AuthenticationType.BEARER_TOKEN, config.getAuthenticationType());
        
        config.setType("api-key");
        assertEquals(AuthenticationType.API_KEY, config.getAuthenticationType());
        
        config.setType("oauth2");
        assertEquals(AuthenticationType.OAUTH2, config.getAuthenticationType());
        
        config.setType("certificate");
        assertEquals(AuthenticationType.CERTIFICATE, config.getAuthenticationType());
        
        config.setType("custom");
        assertEquals(AuthenticationType.CUSTOM, config.getAuthenticationType());
        
        config.setType("none");
        assertEquals(AuthenticationType.NONE, config.getAuthenticationType());
    }

    // ========================================
    // Basic Authentication Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get basic authentication properties")
    void testBasicAuthenticationProperties() {
        config.setType("basic");
        config.setUsername("testuser");
        config.setPassword("testpass");
        
        assertEquals("basic", config.getType());
        assertEquals("testuser", config.getUsername());
        assertEquals("testpass", config.getPassword());
        assertEquals(AuthenticationType.BASIC, config.getAuthenticationType());
    }

    @Test
    @DisplayName("Should handle null basic authentication properties")
    void testNullBasicAuthenticationProperties() {
        config.setType(null);
        config.setUsername(null);
        config.setPassword(null);
        
        assertNull(config.getType());
        assertNull(config.getUsername());
        assertNull(config.getPassword());
    }

    // ========================================
    // Token Authentication Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get token authentication properties")
    void testTokenAuthenticationProperties() {
        config.setType("bearer-token");
        config.setToken("abc123token");
        config.setTokenHeader("Custom-Auth");
        config.setTokenPrefix("Token ");
        
        assertEquals("bearer-token", config.getType());
        assertEquals("abc123token", config.getToken());
        assertEquals("Custom-Auth", config.getTokenHeader());
        assertEquals("Token ", config.getTokenPrefix());
    }

    @Test
    @DisplayName("Should set and get API key authentication properties")
    void testApiKeyAuthenticationProperties() {
        config.setType("api-key");
        config.setApiKey("secret-api-key-123");
        config.setApiKeyHeader("X-Custom-API-Key");
        
        assertEquals("api-key", config.getType());
        assertEquals("secret-api-key-123", config.getApiKey());
        assertEquals("X-Custom-API-Key", config.getApiKeyHeader());
    }

    @Test
    @DisplayName("Should handle null token authentication properties")
    void testNullTokenAuthenticationProperties() {
        config.setToken(null);
        config.setApiKey(null);
        config.setTokenHeader(null);
        config.setTokenPrefix(null);
        config.setApiKeyHeader(null);

        assertNull(config.getToken());
        assertNull(config.getApiKey());
        assertNull(config.getTokenHeader());
        assertNull(config.getTokenPrefix());
        assertNull(config.getApiKeyHeader());
    }

    // ========================================
    // OAuth2 Authentication Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get OAuth2 authentication properties")
    void testOAuth2AuthenticationProperties() {
        config.setType("oauth2");
        config.setClientId("client123");
        config.setClientSecret("secret456");
        config.setTokenUrl("https://auth.example.com/token");
        config.setScope("read write");
        config.setGrantType("authorization_code");

        assertEquals("oauth2", config.getType());
        assertEquals("client123", config.getClientId());
        assertEquals("secret456", config.getClientSecret());
        assertEquals("https://auth.example.com/token", config.getTokenUrl());
        assertEquals("read write", config.getScope());
        assertEquals("authorization_code", config.getGrantType());
    }

    @Test
    @DisplayName("Should handle null OAuth2 authentication properties")
    void testNullOAuth2AuthenticationProperties() {
        config.setClientId(null);
        config.setClientSecret(null);
        config.setTokenUrl(null);
        config.setScope(null);
        config.setGrantType(null);

        assertNull(config.getClientId());
        assertNull(config.getClientSecret());
        assertNull(config.getTokenUrl());
        assertNull(config.getScope());
        assertNull(config.getGrantType());
    }

    // ========================================
    // Certificate Authentication Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get certificate authentication properties")
    void testCertificateAuthenticationProperties() {
        config.setType("certificate");
        config.setCertificatePath("/path/to/cert.pem");
        config.setCertificatePassword("certpass");
        config.setKeyStorePath("/path/to/keystore.jks");
        config.setKeyStorePassword("keystorepass");
        config.setKeyStoreType("PKCS12");
        config.setTrustStorePath("/path/to/truststore.jks");
        config.setTrustStorePassword("truststorepass");
        config.setTrustStoreType("PKCS12");

        assertEquals("certificate", config.getType());
        assertEquals("/path/to/cert.pem", config.getCertificatePath());
        assertEquals("certpass", config.getCertificatePassword());
        assertEquals("/path/to/keystore.jks", config.getKeyStorePath());
        assertEquals("keystorepass", config.getKeyStorePassword());
        assertEquals("PKCS12", config.getKeyStoreType());
        assertEquals("/path/to/truststore.jks", config.getTrustStorePath());
        assertEquals("truststorepass", config.getTrustStorePassword());
        assertEquals("PKCS12", config.getTrustStoreType());
    }

    @Test
    @DisplayName("Should handle null certificate authentication properties")
    void testNullCertificateAuthenticationProperties() {
        config.setCertificatePath(null);
        config.setCertificatePassword(null);
        config.setKeyStorePath(null);
        config.setKeyStorePassword(null);
        config.setKeyStoreType(null);
        config.setTrustStorePath(null);
        config.setTrustStorePassword(null);
        config.setTrustStoreType(null);

        assertNull(config.getCertificatePath());
        assertNull(config.getCertificatePassword());
        assertNull(config.getKeyStorePath());
        assertNull(config.getKeyStorePassword());
        assertNull(config.getKeyStoreType());
        assertNull(config.getTrustStorePath());
        assertNull(config.getTrustStorePassword());
        assertNull(config.getTrustStoreType());
    }

    // ========================================
    // Custom Authentication Property Tests
    // ========================================

    @Test
    @DisplayName("Should set and get custom authentication properties")
    void testCustomAuthenticationProperties() {
        config.setType("custom");
        config.setCustomImplementation("com.example.CustomAuth");

        Map<String, Object> customProps = new HashMap<>();
        customProps.put("endpoint", "https://custom.auth.com");
        customProps.put("timeout", 30000);
        customProps.put("retries", 3);

        config.setCustomProperties(customProps);

        assertEquals("custom", config.getType());
        assertEquals("com.example.CustomAuth", config.getCustomImplementation());
        assertEquals(customProps, config.getCustomProperties());
    }

    @Test
    @DisplayName("Should handle null custom authentication properties")
    void testNullCustomAuthenticationProperties() {
        config.setCustomImplementation(null);
        config.setCustomProperties(null);

        assertNull(config.getCustomImplementation());
        assertNotNull(config.getCustomProperties()); // Should return empty map, not null
        assertTrue(config.getCustomProperties().isEmpty());
    }

    @Test
    @DisplayName("Should set and get individual custom properties")
    void testIndividualCustomProperties() {
        config.setCustomProperty("key1", "value1");
        config.setCustomProperty("key2", 42);
        config.setCustomProperty("key3", true);

        assertEquals("value1", config.getCustomProperty("key1"));
        assertEquals(42, config.getCustomProperty("key2"));
        assertEquals(true, config.getCustomProperty("key3"));
        assertNull(config.getCustomProperty("nonexistent"));
    }

    @Test
    @DisplayName("Should handle null keys and values in custom properties")
    void testCustomPropertiesNullHandling() {
        config.setCustomProperty("nullValue", null);
        config.setCustomProperty(null, "nullKey");

        assertNull(config.getCustomProperty("nullValue"));
        assertEquals("nullKey", config.getCustomProperty(null));
    }

    // ========================================
    // Token Refresh Configuration Tests
    // ========================================

    @Test
    @DisplayName("Should set and get token refresh configuration")
    void testTokenRefreshConfiguration() {
        config.setAutoRefresh(false);
        config.setRefreshThresholdSeconds(600L);
        config.setMaxRefreshAttempts(5);

        assertFalse(config.getAutoRefresh());
        assertFalse(config.isAutoRefreshEnabled());
        assertEquals(600L, config.getRefreshThresholdSeconds());
        assertEquals(5, config.getMaxRefreshAttempts());
    }

    @Test
    @DisplayName("Should handle boolean convenience method for auto refresh")
    void testAutoRefreshConvenienceMethod() {
        config.setAutoRefresh(true);
        assertTrue(config.isAutoRefreshEnabled());

        config.setAutoRefresh(false);
        assertFalse(config.isAutoRefreshEnabled());

        config.setAutoRefresh(null);
        assertFalse(config.isAutoRefreshEnabled()); // null should be false
    }

    @Test
    @DisplayName("Should handle null token refresh configuration")
    void testNullTokenRefreshConfiguration() {
        config.setAutoRefresh(null);
        config.setRefreshThresholdSeconds(null);
        config.setMaxRefreshAttempts(null);

        assertNull(config.getAutoRefresh());
        assertNull(config.getRefreshThresholdSeconds());
        assertNull(config.getMaxRefreshAttempts());
    }
