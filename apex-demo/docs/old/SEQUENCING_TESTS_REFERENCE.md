# APEX Sequencing Tests Reference

**Document Version:** 1.0  
**Last Updated:** November 8, 2025  
**Location:** `apex-demo/src/test/java/dev/mars/apex/demo/sequencing/`  
**Total Tests:** 47 test classes

---

## Overview

This document provides a comprehensive reference for all test classes in the sequencing folder, organized alphabetically. Each test includes a brief description and three key bullet points highlighting its purpose and functionality.

---

## Test Classes (Alphabetical)

### AllProcessorsTest
**Purpose:** Comprehensive test demonstrating APEX's design flaw where different processors produce different results from the same YAML file.

- Tests ALL APEX processors (YamlEnrichmentProcessor, RulesEngineConfiguration, SimpleRulesEngine) with identical YAML
- Proves that different processors produce inconsistent execution orders
- Demonstrates the fundamental design flaw that sequential processing fixes

---

### AllSectionTypesSequentialTest
**Purpose:** Ultimate complexity test with ALL 8 section types in a single YAML file.

- Tests the most complex scenario: enrichment-refs, enrichments, enrichment-groups, rule-refs, rules, rule-groups (both inline and external)
- Verifies that all section types load correctly and process in exact document order
- Validates that dependencies are resolved correctly across all section types

---

### AMinimalSequentialProcessingTest
**Purpose:** Minimal demonstration of sequential processing where rules depend on enrichment results.

- Demonstrates the core fix for APEX's design flaw: respecting YAML document order when processing-mode is "sequential"
- Uses minimal configuration to clearly show enrichment-then-validation pattern
- Follows established patterns from other sequencing tests for consistency

---

### AnalyzerGapDetectionTest
**Purpose:** Critical test exposing gaps in the YamlProcessingSequenceAnalyzer.

- Tests YAML sections NOT currently handled by the analyzer (transformations, data-sources, data-sinks, pipeline, categories, error-recovery)
- Verifies that the analyzer shows MULTIPLE items in the SAME section (not just section types)
- Ensures the analyzer lists individual items in their exact YAML file order

---

### BothRefsEnrichmentFirstTest
**Purpose:** Critical test where BOTH enrichment-refs AND rule-refs appear in the same file (enrichment-refs first).

- Verifies that enrichment-refs expands BEFORE inline enrichments
- Verifies that rule-refs expands BEFORE inline rules
- Proves that all enrichments execute BEFORE all rules when enrichment-refs comes first

---

### BothRefsRuleFirstTest
**Purpose:** Critical test where BOTH rule-refs AND enrichment-refs appear in the same file (rule-refs first).

- Verifies that rule-refs expands BEFORE enrichment-refs
- Proves that rules execute BEFORE enrichments (unusual but valid pattern)
- Tests refs-only scenario (no inline items) to prove placeholder expansion works

---

### ChainedDependenciesWithinSectionsTest
**Purpose:** Tests chained dependencies within sections (E1→E2, R1→R2).

- Verifies that enrichments within the enrichments section execute in document order
- Verifies that rules within the rules section execute in document order
- Proves that items WITHIN a section respect document order

---

### ComprehensiveValidationTest
**Purpose:** Comprehensive sequential processing validation using RulesEngine with actual business logic.

- Tests end-to-end functionality with real data (not just YAML syntax)
- Validates complex dependencies are resolved and executed properly
- Follows APEX testing principles: use RulesEngine.evaluate(), test business logic, validate enriched data values

---

### DeferredDependencyResolverTest
**Purpose:** Phase 3 tests for deferred dependency resolution system.

- Tests forward reference resolution (rules referencing enrichments defined later)
- Tests complex dependency chains and circular dependency detection
- Tests unresolvable dependency handling and deferred processing queue management

---

### EnrichmentGroupRefsSequentialOrderTest
**Purpose:** Priority 2 test for enrichment-refs with groups placeholder expansion.

- Verifies enrichment-refs placeholder is inserted at correct position during YAML parsing
- Verifies placeholder is expanded to actual enrichment groups AFTER reference processing
- Proves referenced enrichment groups execute at the correct position in document order

---

### EnrichmentRefsBeforeInlineTest
**Purpose:** Critical test where enrichment-refs appears BEFORE any inline enrichments.

- Proves that reference placeholders can appear at ANY position in the document
- Verifies that referenced enrichments execute BEFORE inline enrichments
- Validates that all enrichments execute BEFORE rules

---

### EnrichmentRefsSequentialOrderTest
**Purpose:** Critical test verifying the CORE FIX for sequential processing with enrichment-refs.

- Verifies enrichment-refs placeholder is inserted at correct position during YAML parsing
- Proves that referenced enrichments execute at the correct position (not appended to end)
- Tests OTC Options trade processing scenario with market data and Greeks calculations

---

### ExecutionTracker
**Purpose:** Utility class to track execution order of enrichments and rules during testing.

- Provides static methods to record execution order: `ExecutionTracker.record(id)`
- Used in YAML expressions to definitively prove APEX respects document order
- Provides `getExecutionLog()` to retrieve actual execution sequence for assertions

---

### ItemLevelProcessingOtcOptionsTest
**Purpose:** Tests item-level processing with OTC options trade processing.

- Demonstrates NEW item-level processing capability where individual items within sections are processed in document order
- Tests cross-section dependencies (rules depending on specific enrichments)
- Uses real database lookups (not inline data) for realistic testing

---

### ItemOrderInvestigationTest
**Purpose:** Investigation test to examine itemOrder contents and identify reference expansion bugs.

- Inspects itemOrder structure to understand how references are expanded
- Identifies bugs where referenced items are appended to end instead of inserted at placeholder position
- Provides detailed logging of itemOrder contents for debugging

---

### LoggingSeverityFixTest
**Purpose:** Verifies that logging severity fixes are working correctly.

- Demonstrates that critical business logic failures are now logged at ERROR/SEVERE level
- Tests that enrichment failures with missing required fields log as ERROR (not WARNING)
- Validates that the logging severity flaw has been fixed

---

### LoggingSeverityFlawTest
**Purpose:** Demonstrates APEX's critical logging severity flaw.

- Shows that business logic failures are incorrectly logged as WARNING instead of ERROR/SEVERE
- Focuses on console output analysis to demonstrate the problem
- Provides evidence of the logging severity issue before the fix

---

### MixedEnrichmentGroupsAndItemsTest
**Purpose:** Priority 2 test for complex interleaving of enrichments and enrichment groups.

- Verifies enrichment-refs expands to BOTH individual enrichments AND enrichment groups
- Tests that inline enrichments execute AFTER enrichment-refs
- Proves that inline enrichment groups execute AFTER inline enrichments

---

### MixedRuleGroupsAndItemsTest
**Purpose:** Priority 2 test for complex interleaving of rules and rule groups.

- Verifies rule-refs expands to BOTH individual rules AND rule groups
- Tests that inline rules execute AFTER rule-refs
- Proves that inline rule groups execute AFTER inline rules

---

### OrderedYamlParserComplexTest
**Purpose:** Complex sequential processing tests using RulesEngine with multiple sections.

- Tests complex multi-section YAML files with actual business logic
- Validates enrich-then-validate and validate-then-enrich patterns with real enrichments and rules
- Follows APEX testing principles: use RulesEngine.evaluate(), test business logic, validate data values

---

### OrderedYamlParserTest
**Purpose:** Phase 1 tests validating that OrderedYamlParser correctly preserves YAML section order.

- Tests section order preservation from YAML documents
- Validates accurate parsing of all YAML content
- Tests compatibility with existing YamlRuleConfiguration structure and edge cases

---

### RuleGroupRefsSequentialOrderTest
**Purpose:** Priority 2 test for rule-refs with groups placeholder expansion.

- Verifies rule-refs placeholder is inserted at correct position during YAML parsing
- Verifies placeholder is expanded to actual rule groups AFTER reference processing
- Proves referenced rule groups execute at the correct position in document order

---

### RuleGroupsSequentialBasicTest
**Purpose:** Tests rule-groups executing in sequential (document order) mode.

- Validates that rule-groups are executed in the order they appear in the YAML document
- Tests enrichments → rules → rule-groups execution sequence
- Follows sequential processing implementation patterns

---

### RuleRefsBeforeInlineTest
**Purpose:** Critical test where rule-refs appears BEFORE any inline rules.

- Proves that reference placeholders can appear at ANY position in the document
- Verifies that referenced rules execute BEFORE inline rules
- Validates that all enrichments execute BEFORE all rules

---

### RuleRefsSequentialOrderTest
**Purpose:** Critical test verifying that rule-refs are expanded at the correct position in document order.

- Tests the CORE FIX: rule-refs placeholders are expanded at correct position (not at end of rules section)
- Validates that external rules are loaded and execute at the placeholder position
- Tests OTC options trade validation scenario

---

### RulesBeforeEnrichmentsTest
**Purpose:** Unusual order test where rules appear BEFORE enrichments in the YAML file.

- Tests the REVERSE of typical order (rules before enrichments) which is unusual but valid
- Verifies that rules execute first, then enrichments (following document order)
- Proves that sequential processing respects ANY document order, not just typical patterns

---

### SequentialProcessingIntegrationTest
**Purpose:** Phase 4 integration tests validating sequential processing system integration.

- Tests OrderedYamlParser and SequentialYamlProcessor integration with existing APEX services
- Validates processing mode detection and selection
- Tests backward compatibility with standard processing

---

### SequentialYamlProcessorTest
**Purpose:** Demonstrates that APEX's fundamental design flaw has been FIXED.

- Tests enrich-then-validate pattern (enrichments before rules)
- Tests validate-then-enrich pattern (rules before enrichments)
- Proves that sequential mode processes sections in YAML document order (not hardcoded sequence)

---

### Test1_EnrichmentRefsPositionTest
**Purpose:** Proves that enrichment-refs executes at EXACT reference position.

- Definitively proves that enrichment-refs executes at exact position in YAML (not appended to end)
- Verifies external enrichments execute in THEIR document order
- Tests that inline enrichments before/after are not affected by the reference

---

### Test2_EnrichmentGroupsOnlyTest
**Purpose:** Proves that when external file has BOTH enrichments and enrichment-groups, ONLY enrichment-groups execute.

- Tests the "groups-only" logic: when groups exist, individual enrichments become definitions only
- Verifies that enrichments are executed via groups, not directly from enrichments section
- Proves no double execution occurs

---

### Test3_MinimalEnrichmentGroupTest
**Purpose:** Minimal test to isolate and verify enrichment-groups can execute enrichments properly.

- Minimal configuration to debug why enrichments in groups might be skipped
- Tests basic enrichment-group with lookup enrichment
- Validates that enrichment-groups work correctly in simplest case

---

### Test4_StandaloneEnrichmentsTest
**Purpose:** Critical test proving that enrichments NOT referenced by groups execute directly.

- Definitively proves the groups-only logic: grouped enrichments are definitions, standalone enrichments execute directly
- Tests mixed scenario with both standalone and grouped enrichments
- Verifies that standalone enrichments execute at their definition position

---

### Test4B_AllStandaloneTest
**Purpose:** Proves that when NO groups exist, ALL enrichments execute at their definition positions.

- Tests scenario with 4 standalone enrichments and NO enrichment-groups section
- Verifies exact execution count (4 items) and exact execution order
- Proves no double execution and correct position verification

---

### Test4C_AllGroupedTest
**Purpose:** Proves that when ALL enrichments are in groups, NONE execute at definition position.

- Tests scenario with 4 grouped enrichments (all in groups) and 2 enrichment-groups
- Verifies that all enrichments execute via groups (not at definition position)
- Proves exact execution order and no double execution

---

### Test4D_EmptyGroupTest
**Purpose:** Proves that empty groups don't break the system.

- Tests edge case with 1 empty enrichment-group
- Verifies that empty groups are handled gracefully without errors
- Validates that non-empty groups still work correctly

---

### Test4E_MissingReferenceTest
**Purpose:** Proves that groups referencing non-existent enrichments are properly validated.

- Tests edge case where enrichment-group references non-existent enrichment ID
- Verifies that APEX rejects invalid configurations with clear error messages
- Validates that missing reference validation works correctly

---

### Test4F_ComplexInterleavingTest
**Purpose:** Proves that complex patterns of standalone and grouped items work correctly.

- Tests complex interleaving: 3 standalone enrichments at positions 1, 3, 5 and 3 grouped enrichments at positions 2, 4, 6
- Verifies exact execution order with 6 items total
- Proves that complex patterns maintain document order

---

### Test5_NumberedSuffixesBasicTest
**Purpose:** Tests that numbered suffixes (enrichments-1, enrichments-2, enrichments-3) work correctly.

- Verifies that numbered suffixes are recognized by the parser
- Tests that numbered sections are merged into a single enrichments section
- Validates that items execute in document order across all numbered sections

---

### Test6A_NumberedSuffixesWithGroupsTest
**Purpose:** Tests that groups-only logic works correctly when enrichments are defined in numbered sections.

- Verifies that enrichments in numbered sections are merged correctly
- Tests that groups-only logic filters grouped enrichments from their definition positions
- Validates that grouped enrichments execute via the group, standalone enrichments execute at definition position

---

### Test6B_ComplexNumberedWithGroupsTest
**Purpose:** Tests complex interleaving of numbered sections and multiple groups.

- Tests complex document order: enrichments-1, enrichment-groups-1, enrichments-2, enrichment-groups-2, enrichments-3
- Verifies that groups execute at their document position (not at end)
- Validates that standalone enrichments execute at their definition position across multiple numbered sections

---

### Test7A_RuleGroupsBasicTest
**Purpose:** Tests that rule-groups work correctly with groups-only logic.

- Verifies that rules NOT referenced by groups execute at their definition position
- Tests that rules referenced by groups execute via the group (not at definition position)
- Validates no double execution occurs

---

### Test7B_NumberedSuffixesWithRuleGroupsTest
**Purpose:** Tests that rule-groups work correctly with numbered suffixes.

- Tests document order: rules-1, rule-groups-1, rules-2
- Verifies that grouped rules execute via the group at the group's position
- Validates that standalone rules execute at their definition position

---

### Test8_TransformationsBasicTest
**Purpose:** Tests that transformations execute in exact document order.

- Tests transformations section with 3 transformation items
- Verifies that transformations execute in document order: transform-1, transform-2, transform-3
- NOTE: Transformations feature is not yet implemented (expected failure)

---

### TestALL_ComprehensiveSectionsTest
**Purpose:** Test for comprehensive YAML file containing ALL APEX section keywords.

- Validates that the analyzer correctly handles ALL 15 APEX section keywords
- Tests multiple items in each list section (enrichments, enrichment-groups, rules, rule-groups, transformations, rule-chains)
- Verifies both itemOrder sections and non-itemOrder sections are handled correctly

---

### TestEdge3_DuplicateIDsAcrossNumberedSectionsTest
**Purpose:** Edge case test for duplicate IDs across numbered sections.

- Tests that duplicate enrichment IDs across numbered sections (enrichments-1, enrichments-2) are detected
- Verifies that YamlConfigurationException is thrown with clear error message
- Validates that duplicate ID validation runs AFTER numbered sections are merged

---

### TestEdge4_IDCollisionInlineVsExternalTest
**Purpose:** Edge case test for ID collision between inline and external enrichments.

- Tests that duplicate IDs between inline enrichments and external enrichments are detected
- Verifies that YamlConfigurationException is thrown with clear error message
- Validates that duplicate ID validation runs AFTER external files are merged

---

### TestEdge5_ForwardReferenceToExternalTest
**Purpose:** Edge case test for forward reference to external enrichments.

- Tests that enrichment-groups can reference enrichments from external files loaded later
- Verifies that reference resolution happens before group validation
- Tests scenario where enrichment-groups comes BEFORE enrichment-refs

---

### TestEdge6_MultipleRefsWithNumberedSuffixesTest
**Purpose:** Edge case test for multiple reference sections with numbered suffixes.

- Tests that multiple enrichment-refs sections (enrichment-refs, enrichment-refs-1) are handled correctly
- Verifies that multiple refs sections are merged properly
- Validates that placeholder expansion handles multiple refs sections

---

### TestEdge7_EmptyNumberedSectionsTest
**Purpose:** Edge case test for empty numbered sections.

- Tests that empty numbered sections mixed with populated ones are handled correctly
- Verifies that empty sections are skipped during merge without causing errors
- Validates that document order is maintained despite empty sections

---

### UseCase1EnrichmentFirstTest
**Purpose:** Use case test for enrichment-first processing pattern.

- Tests common business scenario: enrich customer data FIRST (lookup customer tier), then validate using enriched data
- Demonstrates pattern where validation rules depend on enriched data
- Validates that sequential processing enables this common use case

---

### UseCase2ValidationFirstTest
**Purpose:** Use case test for validation-first processing pattern.

- Tests performance optimization scenario: fast validation of input data FIRST, expensive enrichment SECOND
- Demonstrates pattern where invalid data is rejected quickly without expensive lookups
- Validates that sequential processing enables performance optimization

---

### UseCase3MixedProcessingTest
**Purpose:** Use case test for complex multi-step processing with alternating enrichment and validation phases.

- Tests complex workflow: load reference data → basic enrichment → validation → advanced enrichment
- Demonstrates full power of sequential processing with multiple interleaved phases
- Validates that sequential processing enables complex business logic patterns

---

### YamlSectionOrderProofTest
**Purpose:** Proof test demonstrating that YAML section order is actually followed during execution.

- Provides definitive proof that execution order matches YAML document order (not hardcoded sequence)
- Test 1: enrichments → rules (enrichments execute first)
- Test 2: rules → enrichments (rules execute first)

---

## Summary Statistics

- **Total Test Classes:** 47
- **Core Sequential Processing Tests:** 15
- **Reference Expansion Tests:** 8
- **Groups-Only Logic Tests:** 10
- **Numbered Suffixes Tests:** 4
- **Edge Case Tests:** 5
- **Use Case Tests:** 3
- **Utility Classes:** 1 (ExecutionTracker)
- **Investigation/Proof Tests:** 3

---

## Test Categories

### Core Sequential Processing
- AMinimalSequentialProcessingTest
- SequentialYamlProcessorTest
- SequentialProcessingIntegrationTest
- YamlSectionOrderProofTest
- OrderedYamlParserTest
- OrderedYamlParserComplexTest

### Reference Expansion
- EnrichmentRefsSequentialOrderTest
- RuleRefsSequentialOrderTest
- EnrichmentGroupRefsSequentialOrderTest
- RuleGroupRefsSequentialOrderTest
- Test1_EnrichmentRefsPositionTest
- EnrichmentRefsBeforeInlineTest
- RuleRefsBeforeInlineTest
- BothRefsEnrichmentFirstTest
- BothRefsRuleFirstTest

### Groups-Only Logic
- Test2_EnrichmentGroupsOnlyTest
- Test3_MinimalEnrichmentGroupTest
- Test4_StandaloneEnrichmentsTest
- Test4B_AllStandaloneTest
- Test4C_AllGroupedTest
- Test4D_EmptyGroupTest
- Test4E_MissingReferenceTest
- Test4F_ComplexInterleavingTest
- Test7A_RuleGroupsBasicTest
- MixedEnrichmentGroupsAndItemsTest
- MixedRuleGroupsAndItemsTest

### Numbered Suffixes
- Test5_NumberedSuffixesBasicTest
- Test6A_NumberedSuffixesWithGroupsTest
- Test6B_ComplexNumberedWithGroupsTest
- Test7B_NumberedSuffixesWithRuleGroupsTest

### Edge Cases
- TestEdge3_DuplicateIDsAcrossNumberedSectionsTest
- TestEdge4_IDCollisionInlineVsExternalTest
- TestEdge5_ForwardReferenceToExternalTest
- TestEdge6_MultipleRefsWithNumberedSuffixesTest
- TestEdge7_EmptyNumberedSectionsTest

### Use Cases
- UseCase1EnrichmentFirstTest
- UseCase2ValidationFirstTest
- UseCase3MixedProcessingTest

### Comprehensive/Integration
- AllSectionTypesSequentialTest
- AllProcessorsTest
- ComprehensiveValidationTest
- TestALL_ComprehensiveSectionsTest
- ChainedDependenciesWithinSectionsTest
- RulesBeforeEnrichmentsTest

### Analysis/Investigation
- AnalyzerGapDetectionTest
- ItemOrderInvestigationTest
- LoggingSeverityFlawTest
- LoggingSeverityFixTest

---

**End of Document**

