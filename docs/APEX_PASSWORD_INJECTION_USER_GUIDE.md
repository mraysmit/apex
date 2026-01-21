# APEX Password Injection User Guide

**Version:** 2.1
**Date:** 2026-01-17
**Author:** Mark Andrew Ray-Smith Cityline Ltd

## Overview

The APEX Rules Engine provides powerful externalized configuration management through **password injection**, enabling secure credential management across different deployment environments. This guide demonstrates how to use property placeholders in YAML configurations to inject passwords, database credentials, API keys, and other sensitive values at runtime.

### Key Benefits

- **Security**: Keep credentials out of source code and configuration files
- **Flexibility**: Different values for development, staging, and production environments
- **Standard Compliance**: Compatible with Docker, Kubernetes, CI/CD pipelines, and cloud platforms
- **Zero Code Changes**: Same YAML configuration works across all environments

---

## Quick Start

### Basic Usage

**1. Define placeholders in your YAML configuration:**

```yaml
database:
  url: "jdbc:h2:file:./testdb"
  username: "sa"
  password: "$(DB_PASSWORD)"  # Will be injected at runtime
```

**2. Set the value via environment variable or system property:**

```bash
# Environment variable (recommended for production)
export DB_PASSWORD=my_secure_password

# Or system property (for development/testing)
java -DDB_PASSWORD=my_secure_password -jar myapp.jar
```

**3. APEX automatically resolves the placeholder when loading configuration**

The password is injected securely without being logged or exposed.

---

## Syntax Reference

APEX supports two placeholder syntaxes with optional default values:

### Syntax Patterns

| Pattern | Description | Example |
|---------|-------------|---------|
| `$(PROPERTY)` | Parentheses syntax | `$(DB_PASSWORD)` |
| `${PROPERTY}` | Curly braces syntax | `${DB_PASSWORD}` |
| `$(PROPERTY:default)` | Parentheses with default | `$(DB_PASSWORD:devpass)` |
| `${PROPERTY:default}` | Curly braces with default | `${DB_PASSWORD:devpass}` |

### Examples

```yaml
database:
  # Simple placeholder (no default)
  password: "$(DATABASE_PASSWORD)"
  
  # With default value (used if property not set)
  username: "$(DATABASE_USER:myapp_user)"
  
  # Mixed syntax in same file
  url: "${DATABASE_URL}"
  pool-size: "$(DB_POOL_SIZE:10)"
  
  # Complex defaults with special characters
  connection-string: "$(CONN_STR:Server=localhost;Database=mydb;Trusted_Connection=True)"
```

---

## Resolution Priority

When APEX resolves a placeholder, it checks sources in this order:

1. **System Properties** (highest priority)
   - Set via `-D` flag: `java -DDB_PASSWORD=secret`
   - Set in code: `System.setProperty("DB_PASSWORD", "secret")`

2. **Environment Variables** (medium priority)
   - Set in shell: `export DB_PASSWORD=secret`
   - Set in Docker: `-e DB_PASSWORD=secret`
   - Set in Kubernetes: via ConfigMap or Secret

3. **Default Values** (fallback)
   - Specified in YAML: `$(DB_PASSWORD:default_value)`

### Priority Example

```yaml
database:
  password: "$(DB_PASSWORD:development_password)"
```

**Resolution behavior:**
- If `System.setProperty("DB_PASSWORD", "system_value")` → Uses `"system_value"`
- Else if `export DB_PASSWORD=env_value` → Uses `"env_value"`
- Else → Uses `"development_password"` (default)

---

## Real-World Deployment Scenarios

### Docker Deployment

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  apex-app:
    image: mycompany/apex-app:latest
    environment:
      - DATABASE_PASSWORD=prod_secret_123
      - REDIS_PASSWORD=cache_secret_456
      - API_KEY=external_api_key_789
```

**Application YAML:**
```yaml
database:
  password: "$(DATABASE_PASSWORD)"

redis:
  password: "$(REDIS_PASSWORD)"

api:
  key: "$(API_KEY)"
```

**Run command:**
```bash
docker-compose up
```

---

### Kubernetes Deployment

**1. Create a Kubernetes Secret:**

```bash
kubectl create secret generic db-credentials \
  --from-literal=password=my_secure_production_password
```

**2. Reference in Pod configuration:**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: apex-app
spec:
  containers:
  - name: app
    image: mycompany/apex-app:latest
    env:
    - name: DATABASE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: db-credentials
          key: password
    - name: DATABASE_USER
      value: "production_user"
```

**3. Application YAML (unchanged):**

```yaml
database:
  username: "$(DATABASE_USER)"
  password: "$(DATABASE_PASSWORD)"
```

---

### CI/CD Pipeline Integration

#### GitLab CI

**.gitlab-ci.yml:**
```yaml
test:
  stage: test
  variables:
    DATABASE_PASSWORD: $CI_DATABASE_PASSWORD
    API_KEY: $CI_API_KEY
  script:
    - mvn test
```

#### GitHub Actions

**.github/workflows/test.yml:**
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    env:
      DATABASE_PASSWORD: ${{ secrets.DATABASE_PASSWORD }}
      API_KEY: ${{ secrets.API_KEY }}
    steps:
      - uses: actions/checkout@v2
      - name: Run tests
        run: mvn test
```

#### Jenkins

**Jenkinsfile:**
```groovy
pipeline {
    environment {
        DATABASE_PASSWORD = credentials('database-password-id')
        API_KEY = credentials('api-key-id')
    }
    stages {
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
    }
}
```

---

### Cloud Provider Patterns

#### AWS (with Secrets Manager)

```yaml
# Application fetches from AWS Secrets Manager and sets environment
database:
  password: "$(AWS_RDS_PASSWORD)"
  
# Before running application:
export AWS_RDS_PASSWORD=$(aws secretsmanager get-secret-value \
  --secret-id prod/database/password \
  --query SecretString \
  --output text)
```

#### Azure (with Key Vault)

```yaml
database:
  password: "$(AZURE_SQL_PASSWORD)"

# Before running application:
export AZURE_SQL_PASSWORD=$(az keyvault secret show \
  --name database-password \
  --vault-name my-keyvault \
  --query value \
  --output tsv)
```

#### Google Cloud (with Secret Manager)

```yaml
database:
  password: "$(GCP_DB_PASSWORD)"

# Before running application:
export GCP_DB_PASSWORD=$(gcloud secrets versions access latest \
  --secret="database-password")
```

---

## Complete Configuration Examples

### Multi-Environment Database Configuration

```yaml
database:
  # Connection details with defaults for development
  url: "$(DATABASE_URL:jdbc:postgresql://localhost:5432/myapp_dev)"
  username: "$(DATABASE_USER:dev_user)"
  password: "$(DATABASE_PASSWORD:dev_password)"
  
  # Connection pool settings
  pool:
    min-size: "$(DB_POOL_MIN:5)"
    max-size: "$(DB_POOL_MAX:20)"
    timeout: "$(DB_POOL_TIMEOUT:30000)"
  
  # SSL configuration
  ssl:
    enabled: "$(DB_SSL_ENABLED:false)"
    cert-path: "$(DB_SSL_CERT:/etc/ssl/certs/db-cert.pem)"
```

**Environment-specific values:**

```bash
# Development (uses defaults)
# No environment variables needed

# Staging
export DATABASE_URL="jdbc:postgresql://staging-db:5432/myapp_staging"
export DATABASE_USER="staging_user"
export DATABASE_PASSWORD="staging_secure_password"
export DB_SSL_ENABLED="true"

# Production
export DATABASE_URL="jdbc:postgresql://prod-db:5432/myapp_prod"
export DATABASE_USER="prod_user"
export DATABASE_PASSWORD="prod_ultra_secure_password"
export DB_POOL_MIN="10"
export DB_POOL_MAX="100"
export DB_SSL_ENABLED="true"
export DB_SSL_CERT="/etc/ssl/certs/prod-db-cert.pem"
```

---

### External Data Source Configuration

```yaml
# External data source reference with injected credentials
data-source-refs:
  - name: "customer-database"
    source: "data-sources/customer-database.yaml"
    enabled: true

# data-sources/customer-database.yaml
metadata:
  type: "external-data-config"

database:
  driver: "org.postgresql.Driver"
  url: "$(CUSTOMER_DB_URL)"
  username: "$(CUSTOMER_DB_USER)"
  password: "$(CUSTOMER_DB_PASSWORD)"
  
queries:
  getCustomer:
    sql: "SELECT * FROM customers WHERE customer_id = :customerId"
```

---

### Multi-Service Application

```yaml
# Complete application with multiple external services
services:
  # Primary database
  main-database:
    url: "$(MAIN_DB_URL)"
    username: "$(MAIN_DB_USER)"
    password: "$(MAIN_DB_PASSWORD)"
  
  # Cache layer
  redis:
    host: "$(REDIS_HOST:localhost)"
    port: "$(REDIS_PORT:6379)"
    password: "$(REDIS_PASSWORD)"
  
  # Message queue
  rabbitmq:
    host: "$(RABBITMQ_HOST:localhost)"
    username: "$(RABBITMQ_USER:guest)"
    password: "$(RABBITMQ_PASSWORD:guest)"
  
  # External APIs
  payment-gateway:
    url: "$(PAYMENT_API_URL)"
    api-key: "$(PAYMENT_API_KEY)"
    secret: "$(PAYMENT_API_SECRET)"
  
  # Monitoring
  datadog:
    api-key: "$(DATADOG_API_KEY)"
    app-key: "$(DATADOG_APP_KEY)"
```

---

## Security Features

### Automatic Password Masking

APEX automatically masks sensitive values in logs to prevent credential leakage:

**Masked keywords:**
- `password`, `passwd`, `pwd`
- `secret`, `token`, `key`
- `credential`, `auth`

**Example log output:**

```
[INFO] Resolved property: DATABASE_USER = myapp_user
[INFO] Resolved property: DATABASE_PASSWORD = ********
[INFO] Resolved property: API_KEY = ********
[INFO] Resolved property: DB_POOL_SIZE = 20
```

### Best Practices

1. **Never commit credentials to source control**
   ```yaml
   # Good
   password: "$(DB_PASSWORD)"
   
   # Bad
   password: "my_actual_password"
   ```

2. **Use specific property names**
   ```yaml
   # Good - Clear purpose
   customer-db-password: "$(CUSTOMER_DB_PASSWORD)"
   payment-api-key: "$(PAYMENT_API_KEY)"
   
   # Bad - Ambiguous
   password1: "$(PASSWORD1)"
   key: "$(KEY)"
   ```

3. **Provide safe defaults for development only**
   ```yaml
   # Good - Obvious development default
   password: "$(DB_PASSWORD:dev_password_change_in_prod)"
   
   # Bad - No default for production secret
   password: "$(PROD_DB_PASSWORD)"
   ```

4. **Use environment variables in production**
   ```bash
   # Good - Externalized configuration
   export DATABASE_PASSWORD=$(vault read -field=password secret/database)
   
   # Bad - Hardcoded in startup script
   java -DDATABASE_PASSWORD=hardcoded_secret -jar app.jar
   ```

---

## Common Use Cases

### Local Development

```yaml
# Convenient defaults for local development
database:
  url: "$(DB_URL:jdbc:h2:file:./dev-db)"
  username: "$(DB_USER:sa)"
  password: "$(DB_PASSWORD:)"  # Empty password for H2
```

Run application without setting any environment variables.

---

### Testing

```java
@Test
public void testWithInjectedPassword() {
    // Set test-specific password
    System.setProperty("DB_PASSWORD", "test_password_123");
    
    // Load configuration (password will be injected)
    YamlConfigurationLoader loader = new YamlConfigurationLoader();
    YamlRulesConfig config = loader.loadFromFile("test-config.yaml");
    
    // Use configuration with injected password
    EnrichmentService service = new EnrichmentService(config);
    // ... test logic
}
```

---

### Production Deployment

```bash
#!/bin/bash
# production-startup.sh

# Fetch secrets from vault
export DATABASE_PASSWORD=$(vault kv get -field=password secret/prod/database)
export API_KEY=$(vault kv get -field=key secret/prod/external-api)
export REDIS_PASSWORD=$(vault kv get -field=password secret/prod/redis)

# Start application (credentials injected automatically)
java -jar apex-application.jar
```

---

## Troubleshooting

### Property Not Resolving

**Problem:** Placeholder remains as literal text `$(DB_PASSWORD)` in configuration

**Solutions:**
1. Check property name spelling (case-sensitive)
2. Verify environment variable is set: `echo $DB_PASSWORD`
3. Verify system property is set before configuration loads
4. Check for typos in placeholder syntax

---

### Using Wrong Syntax

**Problem:** YAML parser error or unexpected behavior

**Correct syntax:**
```yaml
# Quotes required for placeholders
password: "$(DB_PASSWORD)"

# Without quotes may cause YAML parsing issues
password: $(DB_PASSWORD)
```

---

### Default Value Not Working

**Problem:** Expected default value not being used

**Check:**
```yaml
# Correct - colon separator
password: "$(DB_PASSWORD:default_value)"

# Wrong - equals sign
password: "$(DB_PASSWORD=default_value)"
```

---

### Password Visible in Logs

**Problem:** Sensitive values appearing in log files

**Solution:** APEX automatically masks values containing these keywords:
- password, passwd, pwd
- secret, token, key
- credential, auth

If your property doesn't contain these keywords, rename it:
```yaml
# Will be masked
api-secret-key: "$(API_SECRET_KEY)"

# Might not be masked
api-value: "$(API_VALUE)"
```

---

## Advanced Topics

### HashiCorp Vault Integration

APEX supports integration with HashiCorp Vault for enterprise-grade secrets management. The workflow involves retrieving secrets from Vault and injecting them via system properties.

**Vault Integration Pattern:**

```java
// 1. Retrieve secrets from Vault using HTTP API or Vault Java client
String vaultUrl = "https://vault.example.com:8200";
String vaultToken = System.getenv("VAULT_TOKEN");

// Example using Vault HTTP API
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(vaultUrl + "/v1/secret/data/database"))
    .header("X-Vault-Token", vaultToken)
    .GET()
    .build();

HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());

// Parse JSON response and extract secrets
Map<String, String> secrets = parseVaultResponse(response.body());

// 2. Set retrieved secrets as system properties for APEX injection
System.setProperty("DB_USERNAME", secrets.get("username"));
System.setProperty("DB_PASSWORD", secrets.get("password"));

// 3. Use normal password injection in YAML
database:
  username: "$(DB_USERNAME)"
  password: "$(DB_PASSWORD)"
```

**Production Vault Configuration:**

```yaml
# Application YAML with Vault-injected credentials
database:
  url: "$(VAULT_DB_URL)"
  username: "$(VAULT_DB_USERNAME)"
  password: "$(VAULT_DB_PASSWORD)"

redis:
  password: "$(VAULT_REDIS_PASSWORD)"

api-keys:
  payment-gateway: "$(VAULT_PAYMENT_API_KEY)"
```

**Startup script with Vault integration:**

```bash
#!/bin/bash
# Fetch secrets from Vault and start application

export VAULT_ADDR="https://vault.company.com:8200"
export VAULT_TOKEN=$(cat /var/run/secrets/vault-token)

# Retrieve database credentials
export VAULT_DB_URL=$(vault kv get -field=url secret/prod/database)
export VAULT_DB_USERNAME=$(vault kv get -field=username secret/prod/database)
export VAULT_DB_PASSWORD=$(vault kv get -field=password secret/prod/database)

# Retrieve API keys
export VAULT_PAYMENT_API_KEY=$(vault kv get -field=api-key secret/prod/payment)

# Start application with Vault-injected credentials
java -jar apex-application.jar
```

**Benefits of Vault Integration:**
- Centralized secrets management
- Audit trail and access control
- Secret rotation without application restart
- Dynamic secrets generation
- Fine-grained access policies
- Encryption at rest and in transit

---

### REST API Authentication

Password injection works seamlessly with REST API data sources for injecting API keys, tokens, and OAuth credentials.

**API Key Authentication:**

```yaml
data-sources:
  - name: "weather-api"
    type: "rest-api"
    enabled: true
    
    connection:
      baseUrl: "https://api.openweathermap.org/data/2.5"
      timeout: 15000
    
    # API Key injected from environment
    authentication:
      type: "api-key"
      apiKey: "${WEATHER_API_KEY}"
      keyParameter: "appid"
      keyLocation: "query"  # or "header"
    
    endpoints:
      getCurrentWeather: "/weather?q={city}&units={units}"
```

**Bearer Token Authentication:**

```yaml
data-sources:
  - name: "internal-api"
    type: "rest-api"
    enabled: true
    
    connection:
      baseUrl: "https://api.internal.example.com/v1"
      defaultHeaders:
        "Content-Type": "application/json"
    
    # Bearer token injected from environment
    authentication:
      type: "bearer"
      token: "${API_BEARER_TOKEN}"
      tokenPrefix: "Bearer"
    
    endpoints:
      getUser: "/users/{userId}"
```

**OAuth2 Authentication:**

```yaml
data-sources:
  - name: "oauth-api"
    type: "rest-api"
    enabled: true
    
    connection:
      baseUrl: "https://api.service.com/v2"
    
    # OAuth2 credentials injected from environment
    authentication:
      type: "oauth2"
      clientId: "${OAUTH_CLIENT_ID}"
      clientSecret: "${OAUTH_CLIENT_SECRET}"
      tokenUrl: "https://api.service.com/oauth/token"
      scope: "read:data write:data"
      grantType: "client_credentials"
    
    endpoints:
      getData: "/data/{dataId}"
```

**Environment variables for REST API authentication:**

```bash
# API Key
export WEATHER_API_KEY="your-api-key-here"

# Bearer Token
export API_BEARER_TOKEN="your-bearer-token-here"

# OAuth2 Credentials
export OAUTH_CLIENT_ID="your-client-id"
export OAUTH_CLIENT_SECRET="your-client-secret"
```

---

### Complex Default Values

YAML allows complex defaults including special characters:

```yaml
# Connection strings with multiple parameters
database:
  url: "$(DB_URL:jdbc:postgresql://localhost:5432/mydb?ssl=true&sslmode=require)"
  
# JSON in defaults
api:
  config: '$(API_CONFIG:{"timeout":30,"retries":3})'
  
# File paths
ssl:
  cert: "$(SSL_CERT_PATH:/etc/ssl/certs/default-cert.pem)"

# Complex connection strings
connection-string: "$(CONN_STR:Server=localhost;Database=mydb;Trusted_Connection=True)"
```

---

### Mixed Syntax Scenarios

Use both syntaxes in the same file:

```yaml
database:
  url: "${DATABASE_URL}"           # Curly braces
  username: "$(DATABASE_USER)"     # Parentheses
  password: "${DATABASE_PASSWORD}" # Curly braces
  timeout: "$(DB_TIMEOUT:30)"      # Parentheses with default
```

Both patterns work identically; choose based on team preference or existing conventions.

---

### Runtime Property Updates

For dynamic environments (not recommended for passwords):

```java
// Update property at runtime
System.setProperty("FEATURE_FLAG", "enabled");

// Reload configuration to pick up new values
YamlConfigurationLoader loader = new YamlConfigurationLoader();
config = loader.loadFromFile("config.yaml");
```

**Note:** For passwords, set properties before loading configuration, not during runtime.

---

## Integration with APEX Components

### Data Source References

```yaml
data-source-refs:
  - name: "production-db"
    source: "data-sources/prod-db.yaml"
    enabled: true

# data-sources/prod-db.yaml
database:
  driver: "com.mysql.cj.jdbc.Driver"
  url: "$(MYSQL_URL)"
  username: "$(MYSQL_USER)"
  password: "$(MYSQL_PASSWORD)"
```

---

### Enrichment Configurations

```yaml
enrichments:
  - id: "customer-lookup"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        data-source-ref: "customer-database"  # Uses injected credentials
        query-ref: "getCustomer"
```

---

### Rule Configurations

```yaml
rules:
  - id: "validate-api-access"
    condition: "apiKey != null && apiKey.length() > 0"
    message: "API key validation"
    
# Configuration with injected API key
external-services:
  api-key: "$(EXTERNAL_API_KEY)"
```

---

## Summary

### Key Takeaways

**Use placeholders** in YAML: `$(PROPERTY)` or `${PROPERTY}`  
**Provide defaults** for development: `$(PROPERTY:default)`  
**Set via environment** in production: `export PROPERTY=value`  
**Automatic masking** keeps logs secure  
**Works everywhere**: Docker, Kubernetes, CI/CD, cloud platforms, HashiCorp Vault  
**REST API support**: Inject API keys, tokens, OAuth credentials  
**Zero code changes** across environments  
**Testcontainers support**: Real database and Vault testing  

### Resolution Priority

1. System Properties (highest)
2. Environment Variables
3. Default Values (fallback)

### Recommended Workflow

1. **Development**: Use defaults in YAML for convenience
2. **Testing**: Set via `System.setProperty()` in test code or use Testcontainers
3. **Production**: Set via environment variables or secrets management (Vault, AWS Secrets Manager, etc.)
4. **CI/CD**: Inject via pipeline variables

### Supported Authentication Methods

- **Database passwords**: PostgreSQL, MySQL, H2, SQL Server, Oracle
- **API Keys**: REST API authentication
- **Bearer Tokens**: HTTP header authentication
- **OAuth2**: Client credentials flow
- **HashiCorp Vault**: Enterprise secrets management
- **Cloud providers**: AWS, Azure, GCP secrets integration

---

## Implementation Details

### Core Resolution Engine

Password injection is implemented in two key classes:

**YamlConfigurationLoader.resolveProperties():**
- Resolves placeholders during YAML file loading
- Supports both `${}` and `$()` syntax patterns
- Handles default values with `:` separator
- Automatic masking of sensitive values in logs

**YamlDataSource.resolveProperties():**
- Resolves placeholders in external data source configurations
- Same syntax support and resolution priority
- Used for database credentials, API keys, connection strings

**Resolution Algorithm:**

```java
private String resolveProperties(String value) {
    // Skip if no placeholders
    if (value == null || (!value.contains("${") && !value.contains("$("))) {
        return value;
    }
    
    // Resolve ${VAR} and ${VAR:default} patterns
    Pattern curlyPattern = Pattern.compile("\\$\\{([^}]+)\\}");
    Matcher matcher = curlyPattern.matcher(value);
    // ... replacement logic
    
    // Resolve $(VAR) and $(VAR:default) patterns  
    Pattern parenPattern = Pattern.compile("\\$\\(([^)]+)\\)");
    matcher = parenPattern.matcher(value);
    // ... replacement logic
    
    return resolvedValue;
}

private String resolveSingleProperty(String placeholder) {
    // Parse VAR:default syntax
    String[] parts = placeholder.split(":", 2);
    String key = parts[0].trim();
    String defaultValue = parts.length > 1 ? parts[1].trim() : null;
    
    // Resolution order
    String value = System.getProperty(key);        // 1. System Properties
    if (value == null) {
        value = System.getenv(key);                // 2. Environment Variables
    }
    if (value == null && defaultValue != null) {
        value = defaultValue;                       // 3. Default Values
    }
    
    return value;
}
```

### Security Implementation

**Automatic masking keywords:**
- `password`, `passwd`, `pwd`
- `secret`, `token`, `key`
- `credential`, `auth`

**Log output examples:**
```
[INFO] Resolved property: DATABASE_USER = myapp_user
[INFO] Resolved property: DATABASE_PASSWORD = ********
[INFO] Resolved property: API_SECRET_KEY = ********
[INFO] Resolved property: DB_POOL_SIZE = 20
```

---

## Additional Resources

- **APEX Configuration Guide**: See `APEX_CONFIGURATION_MANAGER_API_GUIDE.md`
- **External Data Sources**: See `APEX_LOOKUP_CONFIGURATION_GUIDE.md`
- **Database Setup**: See `APEX_DATABASE_GOTCHAS.md`
- **YAML Reference**: See `APEX_YAML_REFERENCE.md`

---

## Testing Your Configuration

To verify password injection is working:

```bash
# Run APEX demo tests
cd apex-demo
mvn test -Dtest="SimplePasswordInjectionTest"

# Or run all password injection tests
mvn test -Dtest="*PasswordInjection*"
```

Check logs for masked password output:
```
[INFO] Resolved property: DATABASE_PASSWORD = ********
```

---

### Testing with Testcontainers

APEX includes comprehensive tests using Testcontainers for real database and secrets management validation:

**PostgreSQL with Testcontainers:**

```java
@Testcontainers
class PostgreSQLPasswordInjectionTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
    
    @BeforeEach
    void setUp() {
        // Set system properties from container
        System.setProperty("DB_URL", postgres.getJdbcUrl());
        System.setProperty("DB_USERNAME", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
    }
    
    @Test
    void testPasswordInjection() {
        // YAML configuration uses injected values
        String yaml = """
            database:
              url: "$(DB_URL)"
              username: "$(DB_USERNAME)"
              password: "$(DB_PASSWORD)"
            """;
        
        // Load and verify
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        YamlRuleConfiguration config = loader.fromYamlString(yaml);
        
        // Test database operations with injected credentials
        // ...
    }
}
```

**HashiCorp Vault with Testcontainers:**

```java
@Testcontainers
class VaultPasswordInjectionTest {
    
    @Container
    static VaultContainer<?> vault = 
        new VaultContainer<>("hashicorp/vault:1.13")
            .withVaultToken("myroot");
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("vaultuser")
            .withPassword("vaultsecret");
    
    @BeforeEach
    void setUp() throws Exception {
        // Store secrets in Vault
        String vaultUrl = vault.getHttpHostAddress();
        storeSecretInVault(vaultUrl, "myroot", 
            postgres.getUsername(), postgres.getPassword());
        
        // Retrieve secrets from Vault
        Map<String, String> secrets = 
            retrieveSecretsFromVault(vaultUrl, "myroot");
        
        // Set system properties from Vault
        System.setProperty("VAULT_DB_USERNAME", secrets.get("username"));
        System.setProperty("VAULT_DB_PASSWORD", secrets.get("password"));
    }
    
    @Test
    void testVaultIntegration() {
        // YAML uses Vault-injected credentials
        String yaml = """
            database:
              username: "$(VAULT_DB_USERNAME)"
              password: "$(VAULT_DB_PASSWORD)"
            """;
        
        // Test complete Vault → APEX → Database workflow
        // ...
    }
    
    private void storeSecretInVault(String vaultUrl, String token, 
                                     String username, String password) {
        // Store secrets using Vault HTTP API
        HttpClient client = HttpClient.newHttpClient();
        String payload = String.format("""
            {
              "data": {
                "username": "%s",
                "password": "%s"
              }
            }
            """, username, password);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(vaultUrl + "/v1/secret/data/database"))
            .header("X-Vault-Token", token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    private Map<String, String> retrieveSecretsFromVault(
            String vaultUrl, String token) throws Exception {
        // Retrieve secrets using Vault HTTP API
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(vaultUrl + "/v1/secret/data/database"))
            .header("X-Vault-Token", token)
            .GET()
            .build();
        
        HttpResponse<String> response = 
            client.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Parse JSON and extract secrets
        return parseVaultResponse(response.body());
    }
}
```

**Running Testcontainers tests:**

```bash
# Requires Docker to be running
docker ps

# Run PostgreSQL Testcontainers tests
mvn test -Dtest=PostgreSQLPasswordInjectionTest

# Run Vault Testcontainers tests  
mvn test -Dtest=VaultPasswordInjectionTest
```

**Benefits of Testcontainers approach:**
- Tests against real databases (PostgreSQL, MySQL, etc.)
- Real HashiCorp Vault instance for secrets management
- No mocking - complete end-to-end validation
- Isolated test environment
- Reproducible tests across all environments
- Automatic cleanup after tests

---

**Version**: APEX Rules Engine 2.1  
**Last Updated**: January 2026  
**Status**: Production Ready ✅
