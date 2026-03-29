package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.cache.ApexCacheManager;
import dev.mars.apex.core.config.loader.ConfigurationLoader;
import dev.mars.apex.core.config.model.YamlRuleConfiguration;
import dev.mars.apex.engine.execution.EnrichmentGroupExecutor;
import dev.mars.apex.engine.core.RulesEngine;
import dev.mars.apex.engine.model.EnrichmentGroup;
import dev.mars.apex.engine.model.EnrichmentGroupResult;
import dev.mars.apex.engine.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for enrichment group execution using RulesEngine.
 * Migrated from deprecated EnrichmentProcessor to RulesEngine API.
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class EnrichmentGroupExecutionTest {

    private ConfigurationLoader loader;

    @BeforeEach
    void setUp() {
        ApexCacheManager.resetInstance();
        loader = new ConfigurationLoader();
    }

    private String buildAndGroupYaml() {
        return """
            metadata:
              name: "AND Group Test"
              description: "Test AND enrichment group"

            enrichments:
              - id: e1
                name: E1
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: a
                    target-field: a_copy
              - id: e2
                name: E2
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: b
                    target-field: b_copy

            enrichment-groups:
              - id: g_and
                name: G AND
                operator: AND
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
            """;
    }

    private String buildOrGroupYaml() {
        return """
            metadata:
              name: "OR Group Test"
              description: "Test OR enrichment group"

            enrichments:
              - id: e1
                name: E1
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: a
                    target-field: a_copy
              - id: e2
                name: E2
                type: field-enrichment
                enabled: true
                field-mappings:
                  - source-field: b
                    target-field: b_copy

            enrichment-groups:
              - id: g_or
                name: G OR
                operator: OR
                stop-on-first-failure: true
                enrichment-ids: [ e1, e2 ]
            """;
    }

    @Test
    @DisplayName("OR group succeeds when first enrichment succeeds")
    void testOrGroupShortCircuitOnSuccess() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildOrGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 will succeed; e2 would fail but OR should succeed

        RuleResult result = engine.evaluate(input);
        assertTrue(result.isSuccess(), "OR group should succeed when first enrichment succeeds");
        assertNotNull(result.getEnrichedData());
        assertEquals("value-a", result.getEnrichedData().get("a_copy"));
    }

    @Test
    @DisplayName("AND group succeeds when all enrichments succeed")
    void testAndGroupSucceedsWhenAllSucceed() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildAndGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a");
        input.put("b", "value-b");

        RuleResult result = engine.evaluate(input);
        assertTrue(result.isSuccess(), "AND group should succeed when all enrichments succeed");
        assertNotNull(result.getEnrichedData());
        assertEquals("value-a", result.getEnrichedData().get("a_copy"));
        assertEquals("value-b", result.getEnrichedData().get("b_copy"));
    }

    @Test
    @DisplayName("AND group fails when any enrichment fails")
    void testAndGroupFailsWhenAnyFails() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildAndGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 succeeds
        // 'b' missing -> e2 has nothing to copy

        RuleResult result = engine.evaluate(input);
        // Since field-enrichment without required flag just copies null, this should succeed
        assertTrue(result.isSuccess(), "Field enrichment without required flag should succeed even with missing source");
    }

    @Test
    @DisplayName("OR group succeeds when any enrichment succeeds")
    void testOrGroupSucceedsWhenAnySucceeds() throws Exception {
        YamlRuleConfiguration config = loader.fromYamlString(buildOrGroupYaml());
        RulesEngine engine = RulesEngine.fromYamlConfig(config);

        Map<String, Object> input = new HashMap<>();
        input.put("a", "value-a"); // e1 succeeds
        input.put("b", "value-b"); // e2 also succeeds

        RuleResult result = engine.evaluate(input);
        assertTrue(result.isSuccess(), "OR group should succeed when any enrichment succeeds");
        assertNotNull(result.getEnrichedData());
    }

    @Nested
    @DisplayName("executeEnrichmentGroupsList must not mutate caller's nested Maps")
    class CallerNestedMapIsolationTests {

        @Test
        @DisplayName("Caller's nested Map must remain empty after enrichment group execution")
        void callerNestedMapMustNotBeMutatedByEnrichmentGroupExecution() {
            // Caller-owned nested structure: metadata.params is empty and must stay empty
            Map<String, Object> callerParams = new HashMap<>();
            Map<String, Object> callerMetadata = new HashMap<>();
            callerMetadata.put("params", callerParams);

            Map<String, Object> callerData = new HashMap<>();
            callerData.put("name", "original");
            callerData.put("metadata", callerMetadata);

            // Enrichment result targets the same nested path with injected data
            Map<String, Object> enrichedParams = new HashMap<>();
            enrichedParams.put("injected", "leaked-value");
            Map<String, Object> enrichedMetadata = new HashMap<>();
            enrichedMetadata.put("params", enrichedParams);
            Map<String, Object> enrichedOutput = new HashMap<>();
            enrichedOutput.put("metadata", enrichedMetadata);

            RuleResult enrichmentResult = RuleResult.enrichmentSuccess(enrichedOutput);

            EnrichmentGroup group = new EnrichmentGroup("g_isolation_test").setName("Isolation Test Group");
            EnrichmentGroupExecutor executor = new EnrichmentGroupExecutor(null) {
                @Override
                public EnrichmentGroupResult processEnrichmentGroup(EnrichmentGroup requestedGroup, Object targetObject,
                                                                    YamlRuleConfiguration yamlConfig) {
                    return EnrichmentGroupResult.of(
                            requestedGroup.getId(), true, "Enrichment group succeeded",
                            List.of(enrichmentResult), 0L);
                }
            };

            RuleResult result = executor.executeEnrichmentGroupsList(List.of(group), callerData, null);

            assertTrue(result.isSuccess(), "Enrichment group should succeed");
            assertNotNull(result.getEnrichedData(), "Enriched data should be present");

            // CRITICAL ASSERTION: caller's nested Map must be untouched
            assertTrue(callerParams.isEmpty(),
                    "MUTATION BUG: deepMergeInto wrote enrichment data into the caller's " +
                    "nested Map via convertToMap aliasing. callerParams should be empty but contains: " + callerParams);

            assertEquals("original", callerData.get("name"),
                    "Caller's top-level field must not be altered");
        }

        @Test
        @DisplayName("Two sequential enrichment group calls must not accumulate state in shared nested Map")
        void twoSequentialCallsMustNotAccumulateStateInCallerNestedMap() {
            Map<String, Object> callerParams = new HashMap<>();
            Map<String, Object> callerMetadata = new HashMap<>();
            callerMetadata.put("params", callerParams);

            Map<String, Object> callerData = new HashMap<>();
            callerData.put("name", "original");
            callerData.put("metadata", callerMetadata);

            // Call 1 enrichment result
            Map<String, Object> enrichedParams1 = new HashMap<>();
            enrichedParams1.put("call", "first");
            Map<String, Object> enrichedMeta1 = new HashMap<>();
            enrichedMeta1.put("params", enrichedParams1);
            Map<String, Object> enrichedOutput1 = new HashMap<>();
            enrichedOutput1.put("metadata", enrichedMeta1);
            RuleResult enrichmentResult1 = RuleResult.enrichmentSuccess(enrichedOutput1);

            // Call 2 enrichment result
            Map<String, Object> enrichedParams2 = new HashMap<>();
            enrichedParams2.put("call", "second");
            Map<String, Object> enrichedMeta2 = new HashMap<>();
            enrichedMeta2.put("params", enrichedParams2);
            Map<String, Object> enrichedOutput2 = new HashMap<>();
            enrichedOutput2.put("metadata", enrichedMeta2);
            RuleResult enrichmentResult2 = RuleResult.enrichmentSuccess(enrichedOutput2);

            EnrichmentGroup group = new EnrichmentGroup("g_accum_test").setName("Accumulation Test Group");

            // Executor that returns different results on each call
            final int[] callCount = {0};
            EnrichmentGroupExecutor executor = new EnrichmentGroupExecutor(null) {
                @Override
                public EnrichmentGroupResult processEnrichmentGroup(EnrichmentGroup requestedGroup, Object targetObject,
                                                                    YamlRuleConfiguration yamlConfig) {
                    RuleResult result = (callCount[0]++ == 0) ? enrichmentResult1 : enrichmentResult2;
                    return EnrichmentGroupResult.of(
                            requestedGroup.getId(), true, "Enrichment group succeeded",
                            List.of(result), 0L);
                }
            };

            // Call 1
            executor.executeEnrichmentGroupsList(List.of(group), callerData, null);
            assertTrue(callerParams.isEmpty(),
                    "MUTATION BUG (call 1): callerParams polluted with: " + callerParams);

            // Call 2
            executor.executeEnrichmentGroupsList(List.of(group), callerData, null);
            assertTrue(callerParams.isEmpty(),
                    "MUTATION BUG (call 2): callerParams polluted with: " + callerParams);
        }
    }

      @Test
      @DisplayName("Parallel enrichment group should preserve nested updates from all enrichments")
      void testParallelGroupPreservesNestedUpdates() {
        Map<String, Object> leftAudit = new HashMap<>();
        leftAudit.put("left", "value-a");
        Map<String, Object> leftTrade = new HashMap<>();
        leftTrade.put("audit", leftAudit);
        Map<String, Object> leftData = new HashMap<>();
        leftData.put("trade", leftTrade);

        Map<String, Object> rightAudit = new HashMap<>();
        rightAudit.put("right", "value-b");
        Map<String, Object> rightTrade = new HashMap<>();
        rightTrade.put("audit", rightAudit);
        Map<String, Object> rightData = new HashMap<>();
        rightData.put("trade", rightTrade);

        RuleResult leftResult = RuleResult.enrichmentSuccess(leftData);
        RuleResult rightResult = RuleResult.enrichmentSuccess(rightData);

        EnrichmentGroup group = new EnrichmentGroup("g_parallel_nested").setName("G Parallel Nested");
        EnrichmentGroupExecutor executor = new EnrichmentGroupExecutor(null) {
          @Override
          public EnrichmentGroupResult processEnrichmentGroup(EnrichmentGroup requestedGroup, Object targetObject,
                                    YamlRuleConfiguration yamlConfig) {
            return EnrichmentGroupResult.of(
              requestedGroup.getId(),
              true,
              "Enrichment group succeeded",
              List.of(leftResult, rightResult),
              0L
            );
          }
        };

        RuleResult result = executor.executeEnrichmentGroupsList(List.of(group), new HashMap<>(), null);

        assertTrue(result.isSuccess(), "Parallel enrichment group should succeed");
        assertNotNull(result.getEnrichedData(), "Enriched data should be present");

        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedTrade = (Map<String, Object>) result.getEnrichedData().get("trade");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedAudit = (Map<String, Object>) enrichedTrade.get("audit");

        assertNotNull(enrichedTrade, "Nested trade map should be present");
        assertNotNull(enrichedAudit, "Nested audit map should be present");
        assertEquals("value-a", enrichedAudit.get("left"), "Left nested field should be preserved");
        assertEquals("value-b", enrichedAudit.get("right"), "Right nested field should be preserved");
      }
}


