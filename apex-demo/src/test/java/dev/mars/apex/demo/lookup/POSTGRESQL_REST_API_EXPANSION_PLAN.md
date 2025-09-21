# PostgreSQL & REST API Testing Expansion Plan
## Comprehensive Coverage for APEX Lookup Functionality

### 📋 **Executive Summary**
This plan expands APEX lookup testing from 59% to 95%+ coverage by implementing PostgreSQL and REST API integration tests using **real services only** (no mocking). Each phase is incrementally testable with clear validation criteria.

---

## 🎯 **Phase 1: PostgreSQL Foundation (Weeks 1-2)**
**Goal**: Establish real PostgreSQL testing infrastructure and basic lookup functionality

### **Phase 1.1: PostgreSQL Simple Lookup (Week 1)**

#### **Deliverables**
- [ ] `PostgreSQLSimpleLookupTest.java`
- [ ] `postgresql-test-data.sql` initialization script
- [ ] Updated `postgresql-simple-lookup.yaml` configuration

#### **Implementation Steps**
1. **Setup PostgreSQL Testcontainers Infrastructure**
   ```java
   @Container
   static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(TestContainerImages.POSTGRES)
           .withDatabaseName("apex_test")
           .withUsername("apex_user")
           .withPassword("apex_pass")
           .withInitScript("postgresql-test-data.sql");
   ```

2. **Create SQL Initialization Script**
   ```sql
   -- postgresql-test-data.sql
   CREATE TABLE customers (
       customer_id VARCHAR(20) PRIMARY KEY,
       customer_name VARCHAR(100) NOT NULL,
       customer_type VARCHAR(20) NOT NULL,
       tier VARCHAR(20) NOT NULL,
       region VARCHAR(10) NOT NULL,
       status VARCHAR(20) NOT NULL,
       created_date DATE DEFAULT CURRENT_DATE
   );
   
   INSERT INTO customers VALUES 
   ('CUST000001', 'Acme Corporation', 'CORPORATE', 'PLATINUM', 'NA', 'ACTIVE', '2023-01-15'),
   ('CUST000002', 'Beta Industries', 'CORPORATE', 'GOLD', 'EU', 'ACTIVE', '2023-02-20');
   ```

3. **Implement Basic Test Class**
   ```java
   class PostgreSQLSimpleLookupTest extends DemoTestBase {
       
       @Test
       @DisplayName("Should perform simple PostgreSQL customer lookup")
       void testPostgreSQLSimpleLookup() {
           // Load YAML and update with real PostgreSQL connection
           // Execute APEX enrichment
           // Validate results
       }
   }
   ```

#### **Validation Criteria**
- [ ] PostgreSQL container starts successfully
- [ ] Database schema created and populated
- [ ] APEX connects to real PostgreSQL
- [ ] Simple lookup returns expected customer data
- [ ] Test passes consistently (3+ runs)

#### **Success Metrics**
- **Response Time**: < 100ms for simple lookup
- **Connection Pool**: Validates proper connection management
- **Data Accuracy**: 100% match with expected customer data

---

### **Phase 1.2: PostgreSQL Multi-Parameter Lookup (Week 2)**

#### **Deliverables**
- [ ] `PostgreSQLMultiParamLookupTest.java`
- [ ] `postgresql-trading-schema.sql` complex schema
- [ ] Updated `postgresql-multi-param-lookup.yaml`

#### **Implementation Steps**
1. **Create Complex Trading Schema**
   ```sql
   -- postgresql-trading-schema.sql
   CREATE TABLE settlement_instructions (
       instruction_id VARCHAR(20) PRIMARY KEY,
       counterparty_id VARCHAR(20) NOT NULL,
       instrument_type VARCHAR(20) NOT NULL,
       currency VARCHAR(3) NOT NULL,
       market VARCHAR(10) NOT NULL,
       min_amount DECIMAL(15,2),
       max_amount DECIMAL(15,2),
       priority INTEGER DEFAULT 1
   );
   
   CREATE TABLE counterparties (
       counterparty_id VARCHAR(20) PRIMARY KEY,
       counterparty_name VARCHAR(100) NOT NULL,
       credit_rating VARCHAR(10)
   );
   ```

2. **Implement Multi-Parameter Test**
   ```java
   @Test
   @DisplayName("Should perform complex PostgreSQL multi-parameter lookup")
   void testPostgreSQLMultiParameterLookup() {
       // Setup complex test data with multiple parameters
       // Execute JOIN queries through APEX
       // Validate complex query results
   }
   ```

#### **Validation Criteria**
- [ ] Complex schema with JOINs works correctly
- [ ] Multi-parameter binding functions properly
- [ ] PostgreSQL-specific features (arrays, JSON) tested
- [ ] Performance acceptable for complex queries

---

## 🌐 **Phase 2: JDK HTTP Server REST API (Weeks 3-4)**
**Goal**: Implement REST API testing using built-in JDK HTTP server (no external dependencies)

### **Phase 2.1: Basic REST API Infrastructure (Week 3)**

#### **Deliverables**
- [ ] `BasicRestApiLookupTest.java`
- [ ] `basic-rest-api-lookup.yaml` configuration
- [ ] JDK HttpServer utility classes

#### **Implementation Steps**
1. **Setup JDK HTTP Server Infrastructure**
   ```java
   class BasicRestApiLookupTest extends DemoTestBase {
       
       private HttpServer httpServer;
       private int serverPort;
       
       @BeforeEach
       void setupRealHttpServer() throws IOException {
           httpServer = HttpServer.create(new InetSocketAddress(0), 0);
           serverPort = httpServer.getAddress().getPort();
           setupCurrencyDataEndpoint();
           httpServer.start();
       }
       
       private void setupCurrencyDataEndpoint() {
           httpServer.createContext("/api/currency", exchange -> {
               // Return real JSON response
               String jsonResponse = createCurrencyResponse();
               exchange.getResponseHeaders().set("Content-Type", "application/json");
               exchange.sendResponseHeaders(200, jsonResponse.length());
               try (OutputStream os = exchange.getResponseBody()) {
                   os.write(jsonResponse.getBytes());
               }
           });
       }
   }
   ```

2. **Create Basic REST API Configuration**
   ```yaml
   # basic-rest-api-lookup.yaml
   data-sources:
     - name: "currency-api"
       type: "rest-api"
       enabled: true
       api-config:
         base-url: "http://localhost:${server.port}"
         timeout: 5000
   
   enrichments:
     - id: "currency-lookup"
       type: "lookup-enrichment"
       condition: "#currencyCode != null"
       lookup-config:
         lookup-dataset:
           type: "rest-api"
           connection-name: "currency-api"
           endpoint: "/api/currency"
   ```

#### **Validation Criteria**
- [ ] JDK HTTP server starts and responds correctly
- [ ] APEX connects to real HTTP server
- [ ] JSON responses parsed properly
- [ ] Basic authentication works
- [ ] Error handling for HTTP errors

---

### **Phase 2.2: Enhanced REST API Features (Week 4)**

#### **Deliverables**
- [ ] `EnhancedRestApiDemoTest.java`
- [ ] OAuth2 authentication server implementation
- [ ] Circuit breaker and rate limiting validation

#### **Implementation Steps**
1. **Implement OAuth2 Authentication Server**
   ```java
   private void setupOAuth2Endpoints() {
       authServer.createContext("/oauth/token", exchange -> {
           if ("POST".equals(exchange.getRequestMethod())) {
               // Validate client credentials
               String tokenResponse = createOAuth2TokenResponse();
               exchange.getResponseHeaders().set("Content-Type", "application/json");
               exchange.sendResponseHeaders(200, tokenResponse.length());
               try (OutputStream os = exchange.getResponseBody()) {
                   os.write(tokenResponse.getBytes());
               }
           }
       });
   }
   ```

2. **Test Enterprise Features**
   ```java
   @Test
   @DisplayName("Should handle OAuth2 authentication with real servers")
   void testEnterpriseRestApiFeatures() {
       // Test OAuth2 token acquisition
       // Test authenticated API calls
       // Validate circuit breaker functionality
       // Test rate limiting enforcement
   }
   ```

#### **Validation Criteria**
- [ ] OAuth2 token flow works end-to-end
- [ ] Circuit breaker opens/closes correctly
- [ ] Rate limiting enforced properly
- [ ] Encrypted caching functions
- [ ] API monitoring metrics collected

---

## 🚀 **Phase 3: Advanced Integration (Weeks 5-6)**
**Goal**: Implement remaining orphaned YAML files and advanced integration features that combine PostgreSQL and REST API capabilities with sophisticated data processing.

---

## 🚀 **Phase 3.1: Shared Data Sources & Mathematical Operations (Week 5)**

### **📋 Deliverables**
- [ ] `SharedDataSourceDemoTest.java` - Multi-enrichment connection sharing
- [ ] `MathematicalOperationsLookupTest.java` - Complex financial calculations
- [ ] `AdvancedCachingDemoTest.java` - Cache strategy validation
- [ ] `JsonFileLookupTest.java` - JSON file processing
- [ ] `XmlFileLookupTest.java` - XML file processing

### **🔧 Implementation Focus Areas**

#### **1. Shared Data Source Testing**
```yaml
# shared-datasource-demo.yaml - Test multiple enrichments sharing same PostgreSQL connection
data-sources:
  - name: "shared-postgres"
    type: "database"
    connection:
      url: "jdbc:postgresql://localhost:5432/apex_test"
      pool-size: 5
      max-idle: 2

enrichments:
  - id: "customer-lookup"
    data-source: "shared-postgres"
  - id: "account-lookup"
    data-source: "shared-postgres"
  - id: "transaction-lookup"
    data-source: "shared-postgres"
```

**Key Tests:**
- **Connection Pool Efficiency**: Validate that multiple enrichments reuse the same connection pool
- **Resource Utilization**: Measure memory and connection usage across shared operations
- **Transaction Isolation**: Ensure data consistency when multiple enrichments access same database
- **Performance Optimization**: Verify connection sharing improves performance vs individual connections

#### **2. Mathematical Operations Testing**
```yaml
# mathematical-operations-lookup.yaml - Complex financial calculations with SpEL
enrichments:
  - id: "compound-interest-calculation"
    lookup-config:
      transformations:
        - field: "futureValue"
          expression: "#principal * T(java.lang.Math).pow(1 + #rate, #years)"
        - field: "monthlyPayment"
          expression: "(#loanAmount * #monthlyRate) / (1 - T(java.lang.Math).pow(1 + #monthlyRate, -#months))"
        - field: "presentValue"
          expression: "#futureValue / T(java.lang.Math).pow(1 + #discountRate, #periods)"
        - field: "annualizedReturn"
          expression: "T(java.lang.Math).pow(#endValue / #startValue, 1.0 / #years) - 1"
```

**Key Tests:**
- **Compound Interest Calculations**: Accurate to 6 decimal places
- **Cross-Field Mathematical Operations**: Complex formulas using multiple input fields
- **Date Arithmetic with SpEL**: Time-based calculations and date manipulations
- **Precision Validation**: Financial-grade accuracy requirements
- **Edge Case Handling**: Division by zero, negative values, overflow scenarios

#### **3. Advanced Caching Mechanisms**
```yaml
# advanced-caching-demo.yaml - Multiple cache strategies testing
cache-config:
  strategies:
    - name: "lru-cache"
      type: "LRU"
      max-size: 1000
      ttl: 3600
    - name: "lfu-cache"
      type: "LFU"
      max-size: 500
      ttl: 7200
    - name: "fifo-cache"
      type: "FIFO"
      max-size: 750
      ttl: 1800

enrichments:
  - id: "cached-customer-lookup"
    cache-strategy: "lru-cache"
  - id: "cached-market-data"
    cache-strategy: "lfu-cache"
  - id: "cached-reference-data"
    cache-strategy: "fifo-cache"
```

**Key Tests:**
- **LRU (Least Recently Used)**: Validate proper eviction of old entries
- **LFU (Least Frequently Used)**: Test frequency-based cache management
- **FIFO (First In, First Out)**: Verify queue-based cache behavior
- **Memory Stability**: Ensure cache doesn't cause memory leaks
- **Performance Impact**: Measure cache hit/miss ratios and response times

#### **Implementation Steps**
1. **Shared Data Source Testing**
   ```java
   @Test
   @DisplayName("Should share real PostgreSQL connection across enrichments")
   void testSharedDataSourceFunctionality() {
       // Setup shared PostgreSQL connection with connection pooling
       // Test multiple enrichments using same connection simultaneously
       // Validate connection pool efficiency and resource utilization
       // Measure performance improvement vs individual connections
       // Verify transaction isolation between concurrent enrichments
   }
   ```

2. **Mathematical Operations Testing**
   ```java
   @Test
   @DisplayName("Should perform complex financial calculations with 6-decimal precision")
   void testMathematicalOperationsInLookups() {
       // Test compound interest calculations: FV = PV * (1 + r)^n
       // Test loan payment calculations: PMT = (PV * r) / (1 - (1 + r)^-n)
       // Test present value calculations: PV = FV / (1 + r)^n
       // Test annualized return calculations: ((End/Start)^(1/years)) - 1
       // Validate precision to 6 decimal places for all calculations
       // Test edge cases: zero rates, negative values, very large numbers
   }
   ```

3. **Advanced Caching Testing**
   ```java
   @Test
   @DisplayName("Should validate LRU, LFU, and FIFO cache strategies")
   void testAdvancedCachingStrategies() {
       // Test LRU cache: verify least recently used items are evicted first
       // Test LFU cache: verify least frequently used items are evicted first
       // Test FIFO cache: verify first-in items are evicted first
       // Measure cache hit/miss ratios under different access patterns
       // Validate memory usage remains stable under load
   }
   ```

#### **Validation Criteria**
- [ ] Connection pooling works efficiently with 30% reduction in connection overhead
- [ ] Mathematical calculations accurate to 6 decimal places for all financial operations
- [ ] Cache strategies (LRU, LFU, FIFO) function correctly with >80% hit ratio
- [ ] Memory usage remains stable under load with no memory leaks
- [ ] Shared connections improve performance by 25% vs individual connections

---

## 🛡️ **Phase 3.2: Error Handling & Resilience (Week 6)**

### **📋 Deliverables**
- [ ] `RestApiErrorHandlingTest.java` - Comprehensive HTTP error scenarios
- [ ] `DatabaseFailureRecoveryTest.java` - Database resilience testing
- [ ] `ConditionalExpressionLookupTest.java` - Complex conditional logic
- [ ] `SettlementInstructionEnrichmentTest.java` - Financial workflow testing

### **🔧 Implementation Focus Areas**

#### **1. REST API Error Scenarios**
```yaml
# Enhanced error handling configuration
rest-api-config:
  error-handling:
    retry-policy:
      max-attempts: 3
      backoff-strategy: "exponential"
      initial-delay: 1000
    fallback-values:
      default-currency: "USD"
      default-rate: 1.0
    timeout-config:
      connection-timeout: 5000
      read-timeout: 10000
```

**Comprehensive HTTP Error Testing:**
- **404 Not Found**: Missing resource handling with fallback values
- **500 Internal Server Error**: Server failure recovery with retry logic
- **429 Too Many Requests**: Rate limiting behavior with backoff
- **Network Timeouts**: Connection timeout handling with circuit breaker
- **Malformed JSON**: Invalid response parsing with error recovery
- **Authentication Failures**: 401/403 error handling with token refresh
- **Fallback Mechanisms**: Default value assignment when APIs fail

#### **2. Database Failure Recovery**
```yaml
# Database resilience configuration
database-config:
  resilience:
    connection-pool:
      max-pool-size: 10
      min-idle: 2
      connection-timeout: 30000
    retry-policy:
      max-attempts: 5
      retry-delay: 2000
    circuit-breaker:
      failure-threshold: 5
      recovery-timeout: 60000
```

**Database Resilience Testing:**
- **Connection Timeout Scenarios**: Network interruption handling
- **Database Unavailability**: Complete database failure recovery
- **Connection Pool Exhaustion**: Resource limit handling with queuing
- **Transaction Rollback**: Data consistency during failures
- **Retry Mechanisms**: Automatic retry with exponential backoff
- **Circuit Breaker Pattern**: Fail-fast when database is down

#### **3. Complex Conditional Logic**
```yaml
# conditional-expression-lookup.yaml - Advanced conditional expressions
enrichments:
  - id: "conditional-enrichment"
    condition: "#customerType == 'PREMIUM' and #accountBalance > 100000"
    lookup-config:
      conditional-mappings:
        - condition: "#region == 'NA' and #tier == 'PLATINUM'"
          value: "PRIORITY_PROCESSING"
        - condition: "#region == 'EU' and #riskScore < 0.3"
          value: "STANDARD_PROCESSING"
        - condition: "#accountAge > 365 and #transactionVolume > 1000000"
          value: "VIP_PROCESSING"
        - default: "REGULAR_PROCESSING"
```

#### **Implementation Steps**
1. **REST API Error Scenarios**
   ```java
   @Test
   @DisplayName("Should handle HTTP 4xx/5xx errors gracefully with fallback")
   void testRestApiErrorHandling() {
       // Test 404 Not Found responses with default value fallback
       // Test 500 Internal Server Error with retry mechanism
       // Test 429 Rate Limiting with exponential backoff
       // Test network timeouts with circuit breaker activation
       // Test malformed JSON responses with error recovery
       // Validate fallback mechanisms provide sensible defaults
   }
   ```

2. **Database Failure Recovery**
   ```java
   @Test
   @DisplayName("Should recover from database connection failures within 30 seconds")
   void testDatabaseFailureRecovery() {
       // Test connection timeout scenarios with retry logic
       // Test database unavailability with circuit breaker
       // Test connection pool exhaustion with queuing
       // Test transaction rollback with data consistency validation
       // Validate recovery time is under 30 seconds
   }
   ```

3. **Complex Conditional Logic**
   ```java
   @Test
   @DisplayName("Should evaluate complex conditional expressions correctly")
   void testConditionalExpressionLogic() {
       // Test nested AND/OR conditions with multiple fields
       // Test numeric comparisons with precision handling
       // Test string matching with case sensitivity
       // Test date comparisons with timezone handling
       // Validate default fallback when no conditions match
   }
   ```

#### **Validation Criteria**
- [ ] All error scenarios handled gracefully with appropriate fallbacks
- [ ] Fallback mechanisms activate correctly within 5 seconds
- [ ] No memory leaks during error conditions or recovery
- [ ] Recovery time < 30 seconds for all failure scenarios
- [ ] Circuit breaker prevents cascade failures
- [ ] Retry mechanisms use exponential backoff to avoid overwhelming services

---

## 📊 **Phase 3 Orphaned YAML Files to Address**

### **🎯 Priority 1: Core Integration Files**
1. **`shared-datasource-demo.yaml`** - Multi-enrichment connection sharing
   - Tests connection pool efficiency across multiple enrichments
   - Validates resource utilization and transaction isolation
   - Measures performance improvement vs individual connections

2. **`mathematical-operations-lookup.yaml`** - Complex financial calculations
   - Implements compound interest, loan payment, and present value calculations
   - Tests SpEL mathematical expressions with 6-decimal precision
   - Validates edge cases and error handling for mathematical operations

3. **`advanced-caching-demo.yaml`** - Cache strategy validation
   - Tests LRU, LFU, and FIFO cache strategies
   - Measures cache hit/miss ratios and memory usage
   - Validates cache eviction policies and TTL handling

4. **`conditional-expression-lookup.yaml`** - Complex conditional logic
   - Tests nested AND/OR conditions with multiple field comparisons
   - Validates numeric, string, and date comparisons
   - Tests default fallback when no conditions match

### **🎯 Priority 2: File Processing Files**
5. **`json-file-lookup.yaml`** - JSON file data source processing
   - Tests JSON file parsing and field extraction
   - Validates nested JSON structure navigation
   - Tests error handling for malformed JSON files

6. **`xml-file-lookup.yaml`** - XML file data source processing
   - Tests XML file parsing with XPath expressions
   - Validates XML namespace handling and attribute extraction
   - Tests error handling for malformed XML files

7. **`settlement-instruction-enrichment.yaml`** - Financial workflow testing
   - Tests complex financial settlement processing
   - Validates multi-step enrichment workflows
   - Tests transaction integrity and rollback scenarios

8. **`currency-market-mapping.yaml`** - Market data integration
   - Tests real-time market data integration
   - Validates currency conversion and rate calculations
   - Tests market data caching and refresh mechanisms

### **🎯 Priority 3: Enhanced Features**
9. **`customer-profile-enrichment.yaml`** - Customer data enrichment
   - Tests customer profile lookup and enrichment
   - Validates customer segmentation and risk scoring
   - Tests customer data privacy and masking

10. **`enhanced-rest-api-demo.yaml`** - Advanced REST API features
    - Tests OAuth2 authentication and token management
    - Validates rate limiting and circuit breaker patterns
    - Tests API versioning and backward compatibility

---

## 🎯 **Phase 3 Success Metrics**

### **📈 Performance Targets**
- **Shared Connection Efficiency**: 30% reduction in connection overhead compared to individual connections
- **Mathematical Accuracy**: 6 decimal place precision for all financial calculations
- **Cache Hit Ratio**: >80% for frequently accessed data with proper eviction policies
- **Error Recovery Time**: <30 seconds for all failure scenarios including database and API failures
- **Memory Usage**: Stable under load with no memory leaks during extended operations
- **Throughput**: Maintain >500 operations/second with shared resources

### **✅ Quality Gates**
- **Connection Pooling**: Efficient resource utilization across multiple enrichments
- **Mathematical Operations**: All financial calculations accurate and validated against known test cases
- **Cache Strategies**: LRU, LFU, FIFO all function correctly with measurable performance benefits
- **Error Handling**: Graceful degradation for all failure scenarios with appropriate fallbacks
- **File Processing**: JSON and XML files processed correctly with proper error handling
- **Memory Stability**: No memory leaks during extended operations or error conditions

### **🔧 Technical Validation**
- **Concurrent Access**: Multiple enrichments can safely share data sources without conflicts
- **Transaction Integrity**: Data consistency maintained during failures and rollbacks
- **Performance Optimization**: Shared resources improve overall system performance
- **Resilience**: System continues operating during partial failures with degraded functionality
- **Accuracy**: All mathematical operations meet financial-grade precision requirements
- **Scalability**: System handles increased load without performance degradation

---

## 🚀 **Phase 3 Implementation Strategy**

### **Week 5 Focus: Core Integration**
1. **Day 1-2**: Shared data source implementation and connection pooling testing
2. **Day 3-4**: Mathematical operations with complex SpEL expressions and precision validation
3. **Day 5**: Advanced caching mechanisms with LRU/LFU/FIFO strategies

### **Week 6 Focus: Resilience & Error Handling**
1. **Day 1-2**: REST API error handling and recovery mechanisms with circuit breakers
2. **Day 3-4**: Database failure recovery and resilience testing with retry logic
3. **Day 5**: File processing (JSON/XML) and conditional logic validation

### **Integration Points**
- **PostgreSQL + REST API**: Combined data sources in single enrichments with fallback mechanisms
- **Caching + Database**: Cached database lookups with fallback to live data
- **Error Handling + Performance**: Graceful degradation without performance impact
- **Mathematical + Conditional**: Complex calculations with conditional logic and validation

---

## 📊 **Phase 4: Performance & Load Testing (Weeks 7-8)**
**Goal**: Validate performance under load with real services

### **Phase 4.1: Concurrent Access Testing (Week 7)**

#### **Deliverables**
- [ ] `ConcurrentLookupTest.java`
- [ ] `MemoryUsageTest.java`
- [ ] Performance benchmarking results

#### **Implementation Steps**
1. **Concurrent Load Testing**
   ```java
   @Test
   @DisplayName("Should handle 1000+ concurrent lookups")
   void testConcurrentLookupAccess() {
       int threadCount = 50;
       int requestsPerThread = 20;
       
       ExecutorService executor = Executors.newFixedThreadPool(threadCount);
       CountDownLatch latch = new CountDownLatch(threadCount);
       
       // Execute concurrent real lookups
       // Measure throughput and response times
       // Validate data consistency
   }
   ```

#### **Validation Criteria**
- [ ] Throughput: 1000+ lookups/second
- [ ] Response time: 95th percentile < 200ms
- [ ] Zero data corruption under load
- [ ] Memory usage < 512MB under load

---

### **Phase 4.2: Integration & Documentation (Week 8)**

#### **Deliverables**
- [ ] End-to-end integration tests
- [ ] Performance benchmark report
- [ ] Comprehensive README.md
- [ ] Test coverage analysis

#### **Final Validation Criteria**
- [ ] All 22 YAML files have corresponding tests
- [ ] Test coverage: 95%+ (up from 59%)
- [ ] All tests pass consistently (10+ runs)
- [ ] Performance benchmarks met
- [ ] Zero orphaned YAML files

---

## 📈 **Success Metrics & KPIs**

### **Coverage Goals**
- **PostgreSQL Coverage**: 100% (3/3 YAML files)
- **REST API Coverage**: 100% (1 existing + 2 new)
- **Orphaned Files**: 0 (down from 9)
- **Overall Coverage**: 95%+ (up from 59%)

### **Performance Targets**
- **PostgreSQL Lookup**: < 50ms average
- **REST API Lookup**: < 100ms average
- **Concurrent Throughput**: 1000+ ops/sec
- **Memory Usage**: < 512MB under load

### **Quality Gates**
- [ ] Zero test failures across all phases
- [ ] No memory leaks detected
- [ ] All error scenarios covered
- [ ] Documentation complete and accurate

---

## 🔧 **Technical Dependencies**

### **Required Dependencies**
```xml
<!-- PostgreSQL Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>

<!-- Performance Testing -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>

<!-- JDK HTTP Server: Built-in, no additional dependencies -->
```

### **Infrastructure Requirements**
- **Docker**: For PostgreSQL Testcontainers
- **JDK 21+**: For built-in HTTP server
- **Maven**: For test execution and reporting

---

## 🎯 **Next Steps**

1. **Phase 1 Start**: Begin PostgreSQL simple lookup implementation
2. **Weekly Reviews**: Validate each phase completion
3. **Continuous Integration**: Ensure all tests pass in CI/CD
4. **Documentation**: Update as each phase completes

This plan achieves **comprehensive APEX lookup coverage** using only real services while maintaining incremental testability and clear validation criteria at each phase.
