# APEX Keyword Implementation Analysis

## Purpose
This document tracks which APEX keywords are **functionally implemented** vs just defined in @JsonProperty annotations.

## Methodology
A keyword is considered "functionally implemented" if:
1. It has a @JsonProperty annotation (defined)
2. It has getter/setter methods (accessible)
3. **It is actually READ and USED in processing logic** (implemented)

## Analysis Status

### VERIFIED IMPLEMENTED
These keywords are confirmed to be functionally implemented in apex-core processing logic:

**Error Handling & Recovery:**
- error-recovery (Used in UnifiedRuleEvaluator.java)
- error-handling (Used in UnifiedRuleEvaluator.java)
- failure-policy (Used in ScenarioStageExecutor.java)
- severity-policies (Used in ErrorRecoveryConfig.java, UnifiedRuleEvaluator.java)
- retry (Used in RetryConfig.java, multiple implementations)
- retry-count (Used in SeverityRecoveryPolicy.java)
- retry-delay (Used in SeverityRecoveryPolicy.java)
- retry-delay-ms (Used in RetryConfig.java)
- max-retries (Used in SeverityRecoveryPolicy.java, RetryConfig.java)
- max-attempts (Used in RetryConfig.java, PipelineStep.java)
- backoff-multiplier (Used in RetryConfig.java line 398)
- delay-ms (Used in PipelineStep.java, RetryConfig.java)
- max-delay-ms (Used in RetryConfig.java line 418)
- timeout-ms (Used in multiple data source implementations)
- alert-on-failure (Used in HealthCheckConfig.java line 175-185, PipelineConfiguration.java)
- recovery-enabled (Used in ErrorRecoveryConfig.java, UnifiedRuleEvaluator.java)
- log-recovery-attempts (Used in ErrorRecoveryConfig.java line 348, UnifiedRuleEvaluator.java)
- default-strategy (Used in ErrorRecoveryConfig.java, ErrorRecoveryService.java line 95)
- strategy (Used in SeverityRecoveryPolicy.java, ErrorRecoveryService.java)
- fallback-value (Used in error recovery logic)

**Monitoring & Metrics:**
- collect-metrics (Used in PipelineConfiguration.java line 174, DataSourceManager.java line 587)
- metrics-enabled (Used in ErrorRecoveryConfig.java line 437, UnifiedRuleEvaluator.java)
- monitoring (Used in RulePerformanceMonitor.java, multiple implementations)

**Circuit Breaker:**
- circuit-breaker (Used in RestApiDataSource.java line 80-82, 194-196)

**Health Check:**
- health-check (Used in DatabaseHealthIndicator.java, RestApiDataSource.java line 562)

**Data Sources:**
- batch (Used in RestApiDataSource.java line 302, multiple implementations)
- validate-result (Used in rule evaluation logic)
- custom-validators (Used in validation framework)
- schema (Used in data source configurations)
- pattern (Used in AccumulativeChainingExecutor.java line 39)
- default-values (Used in field mapping logic)
- sink (Used in PipelineExecutor.java line 697, data sink implementations)
- output-format (Used in data sink configurations)
- document-position (Used in OrderedYamlParser.java for document order processing)
- key-patterns (Used in cache key generation)

### NOT FUNCTIONALLY IMPLEMENTED IN CORE ENGINE
These keywords are defined and stored, but do NOT affect rule/enrichment execution behavior:

**Metadata Fields (Audit/Governance only - not used in execution logic):**
- author (Stored in metadata, not checked during execution)
- business-domain (Stored in metadata, used for categorization only, not execution)
- business-owner (Stored in metadata, used for search/filtering only, not execution)
- created (Stored in metadata, not checked during execution)
- created-by (Stored in metadata, not checked during execution)
- last-modified (Stored in metadata, not checked during execution)
- owner (Stored in metadata, not checked during execution)
- display-name (Stored in metadata, not used in execution)
- documentation-url (Stored in metadata, not used in execution)
- criticality (Stored in metadata, not checked during execution)

**Date Fields (NOT checked before rule execution):**
- effective-date (Stored but NOT checked in RulesEngine.executeRule() - rules execute regardless of date)
- expiration-date (Stored but NOT checked in RulesEngine.executeRule() - rules execute regardless of date)

**Note**: These fields ARE used in:
- apex-yaml-manager for filtering/searching categories
- Metadata storage and retrieval
- Documentation and governance
- But they do NOT affect whether a rule/enrichment is executed in apex-core

### ⚠️ NEEDS DEEPER VERIFICATION
These keywords need investigation to confirm they affect execution behavior:

- custom-properties (May be stored but not processed)
- custom-validators (May be defined but not invoked)
- auto-create (May be defined but not implemented)
- auto-update (May be defined but not implemented)
- drop-if-exists (May be defined but not implemented)

## Next Steps
1. Systematically review each of the 176 keywords
2. Check if keyword is READ in processing logic (not just stored)
3. Verify keyword affects system behavior
4. Update APEX_YAML_REFERENCE.md to remove/mark unimplemented keywords

