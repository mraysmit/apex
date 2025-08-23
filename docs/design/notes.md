
I'll analyze the DataSourceFactoryTest class and the broader codebase for concurrent access concerns, then propose a remediation approach without making any changes.

Now let me examine the DataSourceFactory implementation to understand the concurrent access patterns:

Now let me examine other related classes to understand the broader concurrent access patterns:

Now let me examine the test patterns and concurrent access concerns in more detail:

Based on my analysis of the DataSourceFactoryTest class and the broader codebase, I can identify several concurrent access concerns and propose a comprehensive remediation approach. Here's my assessment:

## Concurrent Access Concerns Identified

### 1. **DataSourceFactory Singleton Pattern Issues**
- **Double-checked locking implementation**: While the current implementation uses `volatile` and synchronized blocks, it's correct but could benefit from additional concurrent access testing
- **Shared cache maps**: `jdbcDataSourceCache`, `httpClientCache`, and `customProviders` use `ConcurrentHashMap` which is good, but cache operations aren't atomic across multiple maps

### 2. **Cache Management Thread Safety**
- **InMemoryCacheManager**: Uses `ConcurrentHashMap` but has race conditions in:
    - `evictLRU()` method during size checking and eviction
    - `evictExpired()` iterator operations
    - Cache statistics updates during concurrent access

### 3. **DataSource Implementation Concerns**
- **DatabaseDataSource**: `resultCache` and `preparedQueries` use `ConcurrentHashMap` but cache key generation and TTL checking aren't atomic
- **FileSystemDataSource**: File operations and cache clearing aren't synchronized
- **RestApiDataSource**: Response caching has similar atomicity issues

### 4. **Metrics Collection Race Conditions**
- **DataSourceMetrics**: While using `AtomicLong` and `AtomicReference`, the min/max response time updates have potential race conditions in the compare-and-set loops

### 5. **Test-Specific Concurrency Issues**
- **Singleton thread safety test**: Only tests 10 threads, insufficient for stress testing
- **Cache clearing in tearDown**: Not synchronized with ongoing operations
- **Custom provider registration**: No concurrent registration/unregistration testing

## Proposed Remediation Approach

### Phase 1: Enhanced Concurrent Testing
```java
// Add comprehensive concurrent access tests
@Test
@DisplayName("Should handle concurrent data source creation safely")
void testConcurrentDataSourceCreation() throws InterruptedException {
    final int threadCount = 50;
    final int operationsPerThread = 100;
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completionLatch = new CountDownLatch(threadCount);
    final ConcurrentLinkedQueue<Exception> exceptions = new ConcurrentLinkedQueue<>();
    final AtomicInteger successCount = new AtomicInteger(0);

    // Create worker threads that perform concurrent operations
    for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        Thread worker = new Thread(() -> {
            try {
                startLatch.await(); // Synchronize start
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        DataSourceConfiguration config = createCacheConfig(
                            "concurrent-test-" + threadId + "-" + j, "memory");
                        ExternalDataSource ds = factory.createDataSource(config);
                        ds.shutdown();
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        exceptions.offer(e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exceptions.offer(e);
            } finally {
                completionLatch.countDown();
            }
        });
        worker.start();
    }

    startLatch.countDown(); // Start all threads
    assertTrue(completionLatch.await(30, TimeUnit.SECONDS), 
        "All threads should complete within timeout");
    
    assertTrue(exceptions.isEmpty(), 
        "No exceptions should occur: " + exceptions.stream()
            .map(Exception::getMessage).collect(Collectors.joining(", ")));
    assertEquals(threadCount * operationsPerThread, successCount.get());
}
```

### Phase 2: Cache Manager Improvements
```java
// Enhanced InMemoryCacheManager with better synchronization
public class InMemoryCacheManager implements CacheManager {
    private final ConcurrentHashMap<String, CacheEntry> cache;
    private final ReadWriteLock evictionLock = new ReentrantReadWriteLock();
    private final AtomicInteger currentSize = new AtomicInteger(0);
    
    @Override
    public void put(String key, Object value, long ttlSeconds) {
        evictionLock.readLock().lock();
        try {
            // Check size atomically and evict if needed
            while (currentSize.get() >= maxSize) {
                evictionLock.readLock().unlock();
                evictionLock.writeLock().lock();
                try {
                    if (currentSize.get() >= maxSize) {
                        evictLRUInternal();
                    }
                    evictionLock.readLock().lock();
                } finally {
                    evictionLock.writeLock().unlock();
                }
            }
            
            CacheEntry entry = new CacheEntry(value, calculateExpiry(ttlSeconds));
            CacheEntry previous = cache.put(key, entry);
            if (previous == null) {
                currentSize.incrementAndGet();
            }
        } finally {
            evictionLock.readLock().unlock();
        }
    }
}
```

### Phase 3: Factory-Level Synchronization
```java
// Enhanced DataSourceFactory with better concurrent access control
public class DataSourceFactory {
    private final ReadWriteLock factoryLock = new ReentrantReadWriteLock();
    private final ConcurrentHashMap<String, CompletableFuture<DataSource>> pendingCreations = new ConcurrentHashMap<>();
    
    public ExternalDataSource createDataSource(DataSourceConfiguration configuration) throws DataSourceException {
        String cacheKey = generateCacheKey(configuration);
        
        // Prevent duplicate concurrent creations
        CompletableFuture<DataSource> future = pendingCreations.computeIfAbsent(cacheKey, 
            k -> CompletableFuture.supplyAsync(() -> {
                try {
                    return createDataSourceInternal(configuration);
                } catch (DataSourceException e) {
                    throw new RuntimeException(e);
                } finally {
                    pendingCreations.remove(k);
                }
            }));
        
        try {
            return (ExternalDataSource) future.get();
        } catch (Exception e) {
            throw new DataSourceException(ErrorType.CONFIGURATION_ERROR, 
                "Failed to create data source", e);
        }
    }
}
```

### Phase 4: Metrics Thread Safety Improvements
```java
// Enhanced DataSourceMetrics with lock-free algorithms
public class DataSourceMetrics {
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxResponseTime = new AtomicLong(0);
    
    public void recordResponseTime(long responseTimeMs) {
        // Use more efficient lock-free min/max updates
        updateMin(minResponseTime, responseTimeMs);
        updateMax(maxResponseTime, responseTimeMs);
    }
    
    private void updateMin(AtomicLong current, long newValue) {
        long currentValue = current.get();
        while (newValue < currentValue) {
            if (current.compareAndSet(currentValue, newValue)) {
                break;
            }
            currentValue = current.get();
        }
    }
}
```

### Phase 5: Test Infrastructure Enhancements
```java
// Add stress testing utilities
@TestMethodOrder(OrderAnnotation.class)
class DataSourceFactoryStressTest {
    
    @Test
    @Order(1)
    @DisplayName("Stress test: High-frequency singleton access")
    void stressTestSingletonAccess() throws InterruptedException {
        final int threadCount = 100;
        final int accessesPerThread = 1000;
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final Set<DataSourceFactory> instances = ConcurrentHashMap.newKeySet();
        
        List<CompletableFuture<Void>> futures = IntStream.range(0, threadCount)
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                try {
                    barrier.await(); // Synchronize all threads
                    for (int j = 0; j < accessesPerThread; j++) {
                        instances.add(DataSourceFactory.getInstance());
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }))
            .collect(Collectors.toList());
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .get(30, TimeUnit.SECONDS);
        
        assertEquals(1, instances.size(), "Should have exactly one singleton instance");
    }
    
    @Test
    @Order(2)
    @DisplayName("Stress test: Concurrent cache operations")
    void stressTestCacheOperations() {
        // Implementation for concurrent cache stress testing
    }
}
```

### Phase 6: Documentation and Monitoring
```java
// Add comprehensive concurrent access documentation
/**
 * Thread Safety Analysis for DataSourceFactory
 * 
 * SAFE OPERATIONS:
 * - getInstance() - Double-checked locking with volatile
 * - registerProvider() - ConcurrentHashMap operations
 * - getSupportedTypes() - Read-only operations on concurrent collections
 * 
 * POTENTIAL RACE CONDITIONS:
 * - createDataSource() with identical configurations
 * - clearCache() during active data source creation
 * - shutdown() during ongoing operations
 * 
 * MITIGATION STRATEGIES:
 * - Use CompletableFuture for deduplicating concurrent creations
 * - Implement graceful shutdown with operation completion waiting
 * - Add monitoring for concurrent access patterns
 */
```

## Implementation Priority

1. **High Priority**: Fix cache manager race conditions and add concurrent creation deduplication
2. **Medium Priority**: Enhance test coverage with stress testing and concurrent scenarios
3. **Low Priority**: Add monitoring and documentation improvements

## Testing Strategy

1. **Unit Tests**: Add concurrent access tests for each component
2. **Integration Tests**: Test full factory lifecycle under concurrent load
3. **Stress Tests**: Long-running tests with high thread counts
4. **Property-Based Tests**: Use tools like jqwik for concurrent property testing

This remediation approach addresses the identified concurrent access concerns while maintaining backward compatibility and improving overall system reliability under concurrent load.




## Current Test Coverage Analysis

I've identified several areas where test coverage for data services and data lookup functionality can be improved:

### **Existing Test Coverage (Good)**
- ✅ **Basic DataSource implementations** - CustomDataSourceTest (32 tests)
- ✅ **LookupServiceRegistry** - LookupServiceRegistryTest (comprehensive)
- ✅ **YamlDataSource configuration** - YamlDataSourceTest (extensive Map-based config tests)
- ✅ **External data source integration** - ExternalDataSourceIntegrationTest (8 tests)
- ✅ **Demo data services** - DataServiceManagerTest, MockDataSourceTest

### **Test Coverage Gaps (Need Improvement)**
- ❌ **YAML-to-runtime data lookup integration** - Limited end-to-end testing
- ❌ **Multi-data source lookup scenarios** - No comprehensive failover testing
- ❌ **Data enrichment workflows** - Missing complex lookup chain testing
- ❌ **Performance under load** - No performance benchmarks for lookup operations
- ❌ **Error handling in lookup chains** - Limited error scenario coverage

## Proposed Test Implementation Plan

### **Phase 1: YAML Data Lookup Integration Tests**
Create tests that validate YAML configurations translate correctly to working data lookup functionality:
- Database query execution from YAML configs
- File-based lookup operations (CSV, JSON)
- Cache-based lookup with TTL and eviction
- REST API lookup with authentication
- Parameter binding and query execution

### **Phase 2: Data Service Manager Lookup Tests**
Build comprehensive tests for DataServiceManager lookup operations:
- Multi-source lookup with prioritization
- Failover scenarios when primary sources fail
- Data source health monitoring during lookups
- Concurrent lookup operations
- Memory and resource management

### **Phase 3: End-to-End Scenario Tests**
Develop real-world scenario tests using the YAML examples:
- User enrichment workflows (cache → database → API)
- Session management with cache fallback
- Configuration-driven data processing
- Multi-step data transformation pipelines

### **Phase 4: Performance and Error Handling**
Create robust tests for production scenarios:
- Load testing with concurrent lookups
- Memory usage under high volume
- Network failure simulation
- Invalid configuration handling
- Data source unavailability scenarios

## Key Test Areas to Focus On

1. **YAML Configuration Validation**
    - Verify all YAML data source types can be loaded and executed
    - Test parameter binding and query execution
    - Validate connection pooling and caching configurations

2. **Data Lookup Chain Testing**
    - Test complex lookup scenarios from the mixed-example.yaml
    - Validate cache-first, database-fallback patterns
    - Test data enrichment from multiple sources

3. **Integration with Lookup Services**
    - Test LookupServiceRegistry with YAML-configured services
    - Validate lookup service discovery and execution
    - Test lookup service chaining and composition

4. **Error Handling and Resilience**
    - Test circuit breaker patterns
    - Validate retry logic and timeout handling
    - Test graceful degradation when sources are unavailable

Would you like me to proceed with implementing these tests? I recommend starting with **Phase 1 (YAML Data Lookup Integration Tests)** as it will provide the foundation for validating that your YAML configurations work correctly with the actual data lookup functionality.


5. API Layer - Partial Coverage
RulesService - No dedicated test file (instance-based service)

