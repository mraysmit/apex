# APEX Core Test Coverage Plan

## Current State
- **Overall Coverage:** 53.7% (15,654 / 29,143 lines)
- **Target:** 80% (23,314 lines needed)
- **Gap:** 7,660 additional lines to cover

---

## Priority Matrix

### 🔴 PHASE 1: Zero Coverage Classes (Quick Wins)
*Target: Cover 1,500+ lines with basic tests*

| Class | Package | Lines Missed | Effort | Impact |
|-------|---------|--------------|--------|--------|
| `SchemaDiffHtmlReportGenerator` | service.schema.diff | 263 | Medium | High |
| `RetryConfig` | config.datasink | 193 | Low | High |
| `PerformanceAnalyzer` | service.monitoring | 179 | Medium | Medium |
| `TemplateProcessorService` | service.engine | 169 | Medium | Medium |
| `DataPipelineEngine` | engine.pipeline | 167 | Medium | High |
| `MessageQueueDataSource` | service.data.external.messagequeue | 133 | Medium | Low |
| `RulesEngineBuilder` | engine.config | 132 | Low | High |
| `CircuitBreaker` | service.data.external.rest | 117 | Low | Medium |
| `EnhancedDataTypeScenarioService` | service.classification | 114 | Medium | Medium |
| `ContentClassifier` | service.classification | 107 | Medium | Medium |
| `MockDataProvider` | util | 106 | Low | Low |
| `ErrorContextService` | service.error | 99 | Low | Medium |
| `ClassificationCache` | service.classification | 92 | Low | Medium |
| `YamlDependencyService` | service.yaml | 88 | Medium | Medium |
| `SimpleRulesEngine` | api | 84 | Low | High |
| `ContentBasedFileFormatDetector` | service.classification | 84 | Low | Medium |

**Phase 1 Total: ~2,127 lines (potential +7.3% coverage)**

---

### 🟠 PHASE 2: Low Coverage Critical Classes (<30%)
*Target: Improve coverage by 2,000+ lines*

| Class | Package | Covered | Missed | Current % | Target % |
|-------|---------|---------|--------|-----------|----------|
| `RuleChainExecutor` | engine.config.execution | 6 | 149 | 3.9% | 70% |
| `AccumulativeChainingExecutor` | engine.executor | 19 | 273 | 6.5% | 70% |
| `ComplexWorkflowExecutor` | engine.executor | 14 | 198 | 6.6% | 70% |
| `FileSystemDataSink` | service.data.external.file | 19 | 239 | 7.4% | 70% |
| `DeferredDependencyResolver` | config.yaml | 10 | 118 | 7.8% | 70% |
| `ResponseMappingConfig` | config.datasource | 19 | 129 | 12.8% | 70% |
| `ResultBasedRoutingExecutor` | engine.executor | 18 | 115 | 13.5% | 70% |
| `DatasetLookupServiceFactory` | service.lookup | 46 | 287 | 13.8% | 70% |
| `ErrorHandlingConfig` | config.datasink | 24 | 130 | 15.6% | 70% |
| `BatchConfig` | config.datasink | 27 | 139 | 16.3% | 70% |
| `OutputFormatConfig` | config.datasink | 28 | 96 | 22.6% | 70% |
| `RulesEngineConfiguration` | engine.config | 41 | 131 | 23.8% | 70% |
| `DataSinkMetrics` | service.data.external | 34 | 105 | 24.5% | 70% |
| `SequentialYamlProcessor` | config.yaml | 56 | 154 | 26.7% | 70% |
| `DatabaseDataSink` | service.data.external.database | 115 | 272 | 29.7% | 70% |
| `SchemaReaderService` | service.schema | 84 | 183 | 31.5% | 70% |

**Phase 2 Total: ~2,718 lines missed (potential +6% coverage if brought to 70%)**

---

### 🟡 PHASE 3: Medium Coverage High-Volume Classes (30-60%)
*Target: Polish coverage on large classes*

| Class | Package | Covered | Missed | Current % | Target % |
|-------|---------|---------|--------|-----------|----------|
| `SchemaConfig` | config.datasink | 49 | 85 | 36.6% | 80% |
| `OrderedYamlParser` | config.yaml | 73 | 122 | 37.4% | 80% |
| `RestApiDataSource` | service.data.external.rest | 146 | 146 | 50.0% | 80% |
| `FileSystemDataSource` | service.data.external.file | 190 | 183 | 50.9% | 80% |
| `DataSourceManager` | service.data.external.manager | 110 | 105 | 51.2% | 80% |
| `DataSourceRegistry` | service.data.external.registry | 111 | 87 | 56.1% | 80% |
| `PipelineExecutor` | engine.pipeline | 389 | 283 | 57.9% | 80% |

**Phase 3 Total: ~1,011 lines missed (potential +2% coverage if brought to 80%)**

---

### 🟢 PHASE 4: Near-Threshold Classes (60-79%)
*Target: Push over 80% threshold*

| Class | Package | Covered | Missed | Current % |
|-------|---------|---------|--------|-----------|
| `UnifiedRuleEvaluator` | service.engine | 192 | 127 | 60.2% |
| `RuleGroup` | engine.model | 240 | 147 | 62.0% |
| `YamlTransformationProcessor` | service.transformation | 141 | 86 | 62.1% |
| `YamlEnrichmentProcessor` | service.enrichment | 558 | 331 | 62.8% |
| `YamlRuleFactory` | config.yaml | 392 | 201 | 66.1% |
| `SequentialProcessor` | engine.config.execution | 289 | 98 | 74.7% |
| `YamlConfigurationLoader` | config.yaml | 1059 | 308 | 77.5% |

**Phase 4 Total: ~1,298 lines missed**

---

## Implementation Strategy

### Week 1: Phase 1 Quick Wins (0% classes)
```
Day 1-2: Config classes (RetryConfig, BatchConfig, ErrorHandlingConfig, OutputFormatConfig)
Day 3-4: Builder classes (RulesEngineBuilder, SimpleRulesEngine) 
Day 5:   Utility classes (MockDataProvider)
```

**Test Files to Create:**
```java
// config.datasink package
RetryConfigTest.java           // Test retry strategies, delays, max attempts
BatchConfigTest.java           // Test batch modes, sizes, flush policies
ErrorHandlingConfigTest.java   // Test error policies, recovery options

// engine.config package  
RulesEngineBuilderTest.java    // Test builder patterns, configurations

// api package
SimpleRulesEngineTest.java     // Test simple evaluation API
```

### Week 2: Phase 1 Services + Phase 2 Start
```
Day 1-2: Classification services (ContentClassifier, ClassificationCache, FileFormatDetector)
Day 3-4: Error/Monitoring services (ErrorContextService, PerformanceAnalyzer)
Day 5:   Start executor tests (RuleChainExecutor, AccumulativeChainingExecutor)
```

**Test Files to Create:**
```java
// service.classification package
ContentClassifierTest.java
ClassificationCacheTest.java
ContentBasedFileFormatDetectorTest.java
EnhancedDataTypeScenarioServiceTest.java

// service.error package
ErrorContextServiceTest.java

// service.monitoring package
PerformanceAnalyzerTest.java
```

### Week 3: Phase 2 Executors and Data Sinks
```
Day 1-2: Executors (ComplexWorkflowExecutor, ResultBasedRoutingExecutor)
Day 3-4: Data sinks (FileSystemDataSink, DatabaseDataSink)
Day 5:   Lookup services (DatasetLookupServiceFactory)
```

**Test Files to Create:**
```java
// engine.executor package
AccumulativeChainingExecutorTest.java
ComplexWorkflowExecutorTest.java
ResultBasedRoutingExecutorTest.java

// service.data.external.file package
FileSystemDataSinkTest.java

// service.data.external.database package
DatabaseDataSinkTest.java

// service.lookup package
DatasetLookupServiceFactoryTest.java
```

### Week 4: Phase 2-3 Completion
```
Day 1-2: Pipeline and schema services
Day 3-4: REST and config classes
Day 5:   Phase 4 polish
```

---

## Test Pattern Templates

### Config Class Test Template
```java
@ExtendWith(ColoredTestOutputExtension.class)
class RetryConfigTest {
    
    @Test
    void shouldCreateWithDefaults() {
        RetryConfig config = new RetryConfig();
        assertNotNull(config.getStrategy());
        assertTrue(config.getMaxAttempts() > 0);
    }
    
    @Test
    void shouldValidateConfiguration() {
        RetryConfig config = RetryConfig.builder()
            .maxAttempts(3)
            .initialDelay(Duration.ofMillis(100))
            .strategy(RetryStrategy.EXPONENTIAL_BACKOFF)
            .build();
        assertTrue(config.isValid());
    }
    
    @Test
    void shouldRejectInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () ->
            RetryConfig.builder().maxAttempts(-1).build()
        );
    }
}
```

### Service Class Test Template
```java
@ExtendWith(ColoredTestOutputExtension.class)
class ErrorContextServiceTest {
    
    private ErrorContextService service;
    
    @BeforeEach
    void setUp() {
        service = new ErrorContextService();
    }
    
    @Test
    void shouldCreateErrorContext() {
        ErrorContext context = service.createContext(
            ErrorType.VALIDATION_ERROR,
            "Test error message"
        );
        assertNotNull(context);
        assertEquals(ErrorType.VALIDATION_ERROR, context.getType());
    }
    
    @Test
    void shouldAnalyzeExpression() {
        ExpressionAnalysis analysis = service.analyzeExpression(
            "#data['field'] > 100"
        );
        assertNotNull(analysis);
        assertFalse(analysis.getVariables().isEmpty());
    }
}
```

### Executor Test Template
```java
@ExtendWith(ColoredTestOutputExtension.class)
class AccumulativeChainingExecutorTest extends DemoTestBase {
    
    private AccumulativeChainingExecutor executor;
    
    @BeforeEach
    void setUp() {
        executor = new AccumulativeChainingExecutor(serviceRegistry, expressionEvaluator);
    }
    
    @Test
    void shouldExecuteChainSequentially() {
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "executor/AccumulativeChainingExecutorTest.yaml"
        );
        Map<String, Object> data = Map.of("amount", 1000, "type", "TRADE");
        
        ExecutionResult result = executor.execute(config, data);
        
        assertTrue(result.isSuccess());
        // Verify accumulation
    }
}
```

---

## Coverage Milestones

| Milestone | Target % | Lines Needed | Estimated Completion |
|-----------|----------|--------------|---------------------|
| Phase 1 Complete | 61% | +2,100 | Week 1 |
| Phase 2 Complete | 70% | +2,600 | Week 2-3 |
| Phase 3 Complete | 76% | +1,700 | Week 3-4 |
| Phase 4 Complete | **80%** | +1,200 | Week 4 |

---

## Exclusion Candidates

Consider adding JaCoCo exclusions for:
- Generated code (if any)
- Deprecated classes scheduled for removal
- Integration-only code that requires external services

```xml
<configuration>
    <excludes>
        <exclude>**/generated/**</exclude>
        <exclude>**/MockDataProvider.class</exclude>
    </excludes>
</configuration>
```

---

## Quick Commands

```bash
# Run tests with coverage
mvn clean test -pl apex-core

# Generate report only
mvn jacoco:report -pl apex-core

# Check coverage threshold
mvn jacoco:check -pl apex-core

# Run specific test class
mvn test -pl apex-core -Dtest=RetryConfigTest
```

---

## Success Criteria

1. ✅ Overall line coverage ≥ 80%
2. ✅ No package below 50% coverage
3. ✅ All critical paths tested (RulesEngine, executors, enrichment)
4. ✅ Error handling paths covered
5. ✅ Edge cases documented and tested
