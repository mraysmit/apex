# APEX Enrichment Groups Implementation Plan

## Overview
Add enrichment grouping capabilities to APEX, mirroring the sophisticated rule group architecture with logical operators, dependencies, and execution control.

### Adjustments to align with Rule Group conventions
- YAML syntax alignment:
  - Use `enrichment-refs` entries with `name`, `source`, and `enabled` (mirrors `rule-refs`).
  - Keep `enrichment-ids` and `enrichment-references` with fields: `enrichment-id`, `sequence`, `enabled`, and optional `override-priority`.
  - For tests, embed YAML strings or place YAML next to the test class rather than under a separate resources folder.
- Stop-on-first semantics: retain `stop-on-first-failure` and document behavior clearly:
  - AND: stop on first failure
  - OR: stop on first success
- Parallel execution: when `parallel-execution` is true, short-circuiting is disabled; evaluate all enrichments (mirror RuleGroup behavior). Default to false.
- Dependencies vs references: start with `enrichment-group-references` (flattening in a second pass) and stage `depends-on` (topological order + cycle validation) to a later sub-phase.
- Severity model: reuse existing severity/aggregation approach to keep reporting consistent.
- Integration points: place `YamlEnrichmentGroup` and `YamlEnrichmentFactory` under `dev.mars.apex.core.config.yaml`; wire execution via `YamlEnrichmentProcessor`/`EnrichmentService` to avoid duplicate loaders.
- Validation: enforce unique group IDs, existence of referenced enrichment IDs, allowed operators {AND, OR}, unique sequences, and cycle checks for group references/depends-on.
- Testing: follow existing rule group test patterns; see Phase 5 guidelines for exact cases.

## Phase 1: Core Data Model Enhancement
*Duration: 3-4 days*

### 1.1 Create EnrichmentGroup Class

````java path=apex-core/src/main/java/dev/mars/apex/core/engine/model/EnrichmentGroup.java mode=EDIT
/**
 * Configuration class representing a group of enrichments with logical operators.
 * 
 * Mirrors RuleGroup functionality for enrichment orchestration with AND/OR logic,
 * conditional execution, and dependency management.
 */
public class EnrichmentGroup {
    private String id;
    private String name;
    private String description;
    private String operator; // "AND", "OR"
    private boolean stopOnFirstFailure;
    private boolean parallelExecution;
    private boolean debugMode;
    private int priority;
    private boolean enabled;
    private String condition; // SpEL condition for group execution
    private List<String> dependsOn;
    private Map<Integer, List<Enrichment>> enrichmentsBySequence;
    private Map<String, Object> metadata;
    
    // Execution methods
    public EnrichmentGroupResult evaluate(StandardEvaluationContext context);
    public EnrichmentGroupResult evaluateSequential(StandardEvaluationContext context);
    public EnrichmentGroupResult evaluateParallel(StandardEvaluationContext context);
}
````

### 1.2 Create EnrichmentGroupResult Class

````java path=apex-core/src/main/java/dev/mars/apex/core/engine/model/EnrichmentGroupResult.java mode=EDIT
/**
 * Result of enrichment group evaluation with individual enrichment results.
 */
public class EnrichmentGroupResult {
    private String groupId;
    private boolean success;
    private String message;
    private List<EnrichmentResult> enrichmentResults;
    private long executionTimeMs;
    private Map<String, Object> groupMetadata;
    private EnrichmentSeverity aggregatedSeverity;
    
    // Result aggregation methods
    public void addEnrichmentResult(EnrichmentResult result);
    public EnrichmentSeverity calculateAggregatedSeverity();
}
````

### 1.3 Create YAML Configuration Classes

````java path=apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichmentGroup.java mode=EDIT
/**
 * YAML configuration class for enrichment groups.
 */
public class YamlEnrichmentGroup {
    private String id;
    private String name;
    private String description;
    private String operator = "AND";
    private boolean stopOnFirstFailure = true;
    private boolean parallelExecution = false;
    private boolean debugMode = false;
    private int priority = 0;
    private boolean enabled = true;
    private String condition;
    private List<String> dependsOn;
    private List<String> enrichmentIds;
    private List<YamlEnrichmentReference> enrichmentReferences;
    private List<String> enrichmentGroupReferences;
    private Map<String, Object> metadata;
}
````

## Phase 2: YAML Configuration Enhancement
*Duration: 2-3 days*

### 2.1 Extend YAML Schema

````yaml path=apex-demo/src/test/resources/apex/enrichments/customer-enrichment-groups.yaml mode=EDIT
enrichment-refs:
  - name: "customer-field-enrichments"
    source: "customer-field-enrichments.yaml"
    enabled: true
  - name: "market-data-enrichments"
    source: "market-data-enrichments.yaml"
    enabled: true
  - name: "calculation-enrichments"
    source: "calculation-enrichments.yaml"
    enabled: true

enrichment-groups:
  # AND group - all enrichments must succeed
  - id: "customer-data-enrichment"
    name: "Customer Data Enrichment Group"
    description: "Complete customer data enrichment pipeline"
    operator: "AND"
    stop-on-first-failure: true
    parallel-execution: false
    priority: 10
    enabled: true
    condition: "#data.customerType == 'PREMIUM'"
    enrichment-ids:
      - "customer-name-enrichment"
      - "customer-address-enrichment"
      - "customer-contact-enrichment"
    metadata:
      owner: "Data Team"
      sla-ms: 2000

  # OR group - any enrichment can succeed
  - id: "market-data-fallback"
    name: "Market Data Fallback Group"
    description: "Try multiple market data sources"
    operator: "OR"
    stop-on-first-failure: false
    parallel-execution: true
    enrichment-references:
      - enrichment-id: "bloomberg-lookup"
        sequence: 1
        enabled: true
      - enrichment-id: "reuters-lookup"
        sequence: 2
        enabled: true
      - enrichment-id: "internal-pricing"
        sequence: 3
        enabled: true

  # Hierarchical group references
  - id: "complete-enrichment-pipeline"
    name: "Complete Enrichment Pipeline"
    operator: "AND"
    enrichment-group-references:
      - "customer-data-enrichment"
      - "market-data-fallback"
    enrichment-ids:
      - "final-validation-enrichment"
    # Stage depends-on in a later sub-phase; shown here for illustration
    depends-on: ["customer-data-enrichment"]
````

### 2.2 Update YamlMetadataValidator

````java path=apex-core/src/main/java/dev/mars/apex/core/util/YamlMetadataValidator.java mode=EDIT
private void validateEnrichmentGroups(Map<String, Object> yamlData, ValidationResult result) {
    Object groupsObj = yamlData.get("enrichment-groups");
    if (groupsObj == null) return;

    if (!(groupsObj instanceof List)) {
        result.addError("enrichment-groups must be a list");
        return;
    }

    List<?> groups = (List<?>) groupsObj;
    Set<String> groupIds = new HashSet<>();
    Map<String, Set<String>> groupDependencies = new HashMap<>();

    for (Object groupObj : groups) {
        validateEnrichmentGroupStructure(groupObj, groupIds, groupDependencies, result);
    }

    // Check for circular dependencies
    validateEnrichmentGroupDependencies(groupDependencies, result);
}
````

## Phase 3: Processing Engine Enhancement
*Duration: 4-5 days*

### 3.1 Create EnrichmentGroupExecutor

Note: Mirror RuleGroup semantics — when `parallel-execution` is true, short-circuiting is disabled and all enrichments are evaluated.

````java path=apex-core/src/main/java/dev/mars/apex/core/service/enrichment/EnrichmentGroupExecutor.java mode=EDIT
/**
 * Executor for enrichment groups with dependency resolution and failure policies.
 */
@Service
public class EnrichmentGroupExecutor {
    
    private final EnrichmentService enrichmentService;
    private final ExpressionEvaluatorService expressionEvaluator;
    
    public List<EnrichmentGroupResult> executeEnrichmentGroups(
            List<EnrichmentGroup> groups, 
            Object data, 
            StandardEvaluationContext context) {
        
        List<EnrichmentGroupResult> results = new ArrayList<>();
        Map<String, EnrichmentGroupResult> completedGroups = new HashMap<>();
        
        // Sort by priority and dependencies
        List<EnrichmentGroup> sortedGroups = resolveDependencyOrder(groups);
        
        for (EnrichmentGroup group : sortedGroups) {
            if (!shouldExecuteGroup(group, data, context, completedGroups)) {
                continue;
            }
            
            EnrichmentGroupResult result = executeGroup(group, data, context);
            results.add(result);
            completedGroups.put(group.getId(), result);
            
            if (!result.isSuccess() && shouldTerminateOnFailure(group)) {
                break;
            }
        }
        
        return results;
    }
}
````

### 3.2 Enhance EnrichmentService

````java path=apex-core/src/main/java/dev/mars/apex/core/service/enrichment/EnrichmentService.java mode=EDIT
// Add group processing capability
public List<EnrichmentGroupResult> processEnrichmentGroups(
        List<EnrichmentGroup> groups, 
        Object data, 
        StandardEvaluationContext context) {
    
    return enrichmentGroupExecutor.executeEnrichmentGroups(groups, data, context);
}

// Enhanced individual enrichment processing for group context
public EnrichmentResult processEnrichmentInGroup(
        Enrichment enrichment, 
        Object data, 
        StandardEvaluationContext context,
        EnrichmentGroup parentGroup) {
    
    // Add group context to evaluation
    context.setVariable("parentGroup", parentGroup);
    return processEnrichment(enrichment, data, context);
}
````

## Phase 4: Factory and Configuration Integration
*Duration: 2-3 days*

### 4.1 Enhance YamlEnrichmentFactory

````java path=apex-core/src/main/java/dev/mars/apex/core/config/yaml/YamlEnrichmentFactory.java mode=EDIT
public List<EnrichmentGroup> createEnrichmentGroups(Map<String, Object> yamlData) {
    List<EnrichmentGroup> groups = new ArrayList<>();
    
    // Load enrichment groups
    Object groupsObj = yamlData.get("enrichment-groups");
    if (groupsObj instanceof List) {
        List<Map<String, Object>> groupConfigs = (List<Map<String, Object>>) groupsObj;
        
        for (Map<String, Object> groupConfig : groupConfigs) {
            EnrichmentGroup group = createEnrichmentGroup(groupConfig);
            groups.add(group);
        }
        
        // Second pass: resolve group references
        resolveEnrichmentGroupReferences(groups, yamlData);
    }
    
    return groups;
}

private void addEnrichmentsToGroup(EnrichmentGroup group, Map<String, Object> groupConfig) {
    // Handle enrichment-ids
    List<String> enrichmentIds = (List<String>) groupConfig.get("enrichment-ids");
    if (enrichmentIds != null) {
        for (String enrichmentId : enrichmentIds) {
            Enrichment enrichment = findEnrichmentById(enrichmentId);
            if (enrichment != null) {
                group.addEnrichment(enrichment);
            }
        }
    }
    
    // Handle enrichment-references with sequence control
    List<Map<String, Object>> enrichmentRefs = 
        (List<Map<String, Object>>) groupConfig.get("enrichment-references");
    if (enrichmentRefs != null) {
        for (Map<String, Object> ref : enrichmentRefs) {
            addEnrichmentReference(group, ref);
        }
    }
}
````

## Phase 5: Comprehensive Testing

Testing guidelines:
- Prefer embedded YAML strings in tests (or colocate YAML next to the Java test class) for clarity and maintenance.
- Suppress DEBUG logging in tests for readable output.
- Cover: AND/OR semantics, stop-on-first behavior, parallel-execution true/false, enrichment-refs cross-file resolution, enrichment-group-references flattening/order, and depends-on DAG with cycle errors (when enabled).
- Reuse patterns from existing rule group tests (e.g., BasicYamlRuleGroupProcessingATest, SimpleBasicYamlRuleGroupProcessingTest).
*Duration: 3-4 days*

### 5.1 Unit Tests

````java path=apex-core/src/test/java/dev/mars/apex/core/engine/model/EnrichmentGroupTest.java mode=EDIT
@Test
void testAndGroupAllEnrichmentsSucceed() {
    EnrichmentGroup group = createTestAndGroup();
    StandardEvaluationContext context = new StandardEvaluationContext();
    
    EnrichmentGroupResult result = group.evaluate(context);
    
    assertTrue(result.isSuccess());
    assertEquals(3, result.getEnrichmentResults().size());
    assertTrue(result.getEnrichmentResults().stream().allMatch(EnrichmentResult::isSuccess));
}

@Test
void testOrGroupAnyEnrichmentSucceeds() {
    EnrichmentGroup group = createTestOrGroup();
    StandardEvaluationContext context = new StandardEvaluationContext();
    
    EnrichmentGroupResult result = group.evaluate(context);
    
    assertTrue(result.isSuccess());
    assertTrue(result.getEnrichmentResults().stream().anyMatch(EnrichmentResult::isSuccess));
}
````

### 5.2 Integration Tests

````java path=apex-demo/src/test/java/dev/mars/apex/demo/enrichment/EnrichmentGroupIntegrationTest.java mode=EDIT
@Test
void testComplexEnrichmentGroupWorkflow() {
    // Load configuration with enrichment groups
    String yamlContent = loadTestYaml("complex-enrichment-groups.yaml");
    
    // Process test data through enrichment groups
    TestCustomer customer = new TestCustomer();
    List<EnrichmentGroupResult> results = enrichmentService.processEnrichmentGroups(
        enrichmentGroups, customer, context);
    
    // Verify group execution order and results
    assertEquals(3, results.size());
    assertTrue(results.get(0).isSuccess()); // validation group
    assertTrue(results.get(1).isSuccess()); // enrichment group
    assertTrue(results.get(2).isSuccess()); // compliance group
}
````

## Phase 6: Documentation and Migration
*Duration: 2-3 days*

### 6.1 Create Migration Guide

````markdown path=docs/ENRICHMENT_GROUP_MIGRATION_GUIDE.md mode=EDIT
# APEX Enrichment Groups Migration Guide

## Overview
Migrate from flat enrichment arrays to grouped enrichment processing with logical operators and dependency management.

## Before (Flat Structure)
```yaml
enrichments:
  - id: "customer-name"
    type: "field-enrichment"
  - id: "customer-address"  
    type: "field-enrichment"
  - id: "market-data"
    type: "lookup-enrichment"
```

## After (Grouped Structure)
```yaml
enrichment-groups:
  - id: "customer-data"
    operator: "AND"
    enrichment-ids: ["customer-name", "customer-address"]
  
  - id: "market-data-fallback"
    operator: "OR"
    enrichment-ids: ["bloomberg-data", "reuters-data", "internal-data"]
```
````

## Implementation Timeline

**Total Estimated Duration: 16-22 days**

- **Phase 1**: Core Data Model (3-4 days)
- **Phase 2**: YAML Configuration (2-3 days)  
- **Phase 3**: Processing Engine (4-5 days)
- **Phase 4**: Factory Integration (2-3 days)
- **Phase 5**: Testing (3-4 days)
- **Phase 6**: Documentation (2-3 days)

## Benefits

1. **Logical Grouping**: AND/OR operators for enrichment orchestration
2. **Dependency Management**: Control enrichment execution order
3. **Failure Policies**: Configurable error handling per group
4. **Performance**: Parallel execution and short-circuiting
5. **Backward Compatibility**: Existing enrichments continue to work
6. **Consistency**: Mirrors proven rule group architecture

This enhancement brings enrichments to feature parity with rule groups while maintaining APEX's architectural consistency.
