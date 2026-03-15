# APEX Authoring Abstraction Research and Proposal

## Status
Date: 2026-03-07  
Author: Mark A Ray-Smith  
Purpose: Document research findings and concrete proposals for a non-technical authoring system above APEX YAML.

Last Updated: 2026-03-07 (terminology refresh + concrete examples added in section 6.1)

## 0. Direct Answer: What The Abstraction Is
Abstraction name: `Authoring Intent Contract`.

Where it exists:
- In the authoring UI model (forms/tables/workflow steps), not in runtime YAML files.
- In this document: section `11.1` (intent entities) and section `6.1` (analyst-facing examples).

What it replaces for analysts:
- Replaces authoring of SpEL expressions.
- Replaces authoring of technical IDs and cross-references.
- Replaces authoring of data-source and query wiring.

What stays unchanged:
- APEX YAML as runtime contract.
- Engine execution semantics.

Compiler boundary:
- Input: business intent fields selected by analyst.
- Output: deterministic APEX YAML with IDs, refs, conditions, and policy defaults.

If this is a real abstraction, these must be true:
- Analyst can define a rule without seeing SpEL.
- Analyst can define enrichment mapping without `data-source-ref` or `query-ref`.
- Analyst can define routing flow without file paths or component YAML references.
- Reviewer can approve from business diff first, technical diff second.

If any of the above is false, this proposal failed.

## 1. Problem Statement
Business analysts rejected the current Blockly-style approach as too complex and not intuitive. This indicates the current authoring model exposes implementation structures (YAML sections and wiring) instead of business intent.

APEX YAML remains necessary for complete runtime capability, but it is too technical as the primary authoring surface for non-technical users.

## 2. Research Scope and Method
Research performed in this repository included:
- Full corpus inventory of `apex-demo/src/test/resources` YAML files.
- Category-level structural scans across `basic`, `business`, `categories`, `codes`, `conditional`, `config`, `database`, `datasources`, `enrichment`, `enrichmentgroups`, `errorhandling`, `etl`, `logging`, `lookup`, `metrics`, `rulegroups`, `scenario`, `sequencing`, `severity`, `transformation`.
- Deep reads of representative files from each major capability area.

Note: This draft is based on broad corpus scanning and representative deep reads. A line-by-line annotation of every YAML file is a possible follow-up phase.

## 3. Evidence Summary (Representative Files)
Core decision/rules patterns:
- `apex-demo/src/test/resources/dev/mars/apex/demo/basic/BasicYamlRuleGroupProcessingATest.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/rulegroups/CrossFileCompositeRuleGroups.yaml`

Lookup/enrichment patterns:
- `apex-demo/src/test/resources/dev/mars/apex/demo/lookup/SimpleFieldLookupDemoTest.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/lookup/ComprehensiveLookupTest.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/lookup/advanced-caching-demo.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/enrichment/comprehensivefinancialsettlementdemo.yaml`

Scenario/component orchestration:
- `apex-demo/src/test/resources/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/scenario/ComponentScenarioTest-simple-component-scenario.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/scenario/multi-stage-component.yaml`

Pipeline/ETL orchestration:
- `apex-demo/src/test/resources/dev/mars/apex/demo/etl/PipelineEtlTest.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/etl/PipelineExecutionKeywordTest_Sequential.yaml`

Error handling/recovery:
- `apex-demo/src/test/resources/dev/mars/apex/demo/errorhandling/SimpleErrorHandlingTest.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/errorhandling/SimpleFailurePolicyContinueTest-scenario.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/metrics/SimpleErrorRecoveryDemo.yaml`

Governance, metadata, and codes:
- `apex-demo/src/test/resources/dev/mars/apex/demo/categories/MetadataInheritanceTest.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/codes/TradeValidationCodesDemo.yaml`

Cross-section ordering/complexity:
- `apex-demo/src/test/resources/dev/mars/apex/demo/sequencing/TestALL_ComprehensiveSectionsTest.yaml`
- `apex-demo/src/test/resources/dev/mars/apex/demo/sequencing/AllSectionTypesSequentialTest.yaml`

## 4. Key Findings
1. APEX is a composition platform, not just a rule editor.
- Real use cases combine rules, enrichments, references, scenarios, components, pipelines, and failure policies.

2. Technical coupling is unavoidable in raw YAML.
- Business intent and infrastructure details are frequently adjacent (connections, queries, retries, cache, stage policies).

3. Cross-file orchestration is central to production behavior.
- Refs and registries (`rule-refs`, `enrichment-refs`, `data-source-refs`, scenario registry, components) are first-class and difficult for non-technical users.

4. Runtime behavior depends on policy semantics, not just rule text.
- Severity, failure policy, and recovery strategy materially change outcomes.

5. Existing Blockly-style modeling is the wrong abstraction for analyst workflows.
- It optimizes syntax assembly, not business intent communication, review, or governance.

## 5. Proposal: Multi-View Authoring Architecture
Introduce a multi-view authoring model where YAML is generated from higher-level intent.

### View A: Business Flow View (Analyst-owned)
- Model stages, business milestones, routing intent, and outcomes.
- Output target: scenario and component structures.

### View B: Decision View (Analyst-owned with guardrails)
- Model validations, decision criteria, severity intent, and grouped checks.
- Output target: rules and rule groups.

### View C: Data Contract View (Shared ownership)
- Model data needed, enrichment goals, and mapping outcomes.
- Output target: enrichments, lookup configuration, field mappings.

### View D: Integration View (Platform-owned)
- Model and maintain data source connectivity, queries/endpoints, cache, retries, placeholders.
- Output target: data source configs and environment-specific technical YAML.

### View E: Policy View (Platform/governance-owned)
- Apply org defaults for naming, metadata, failure policies, error recovery, and code conventions.
- Output target: injected defaults and policy checks during generation.

## 6. Role Model
- Business Analyst: Views A and B, selected operations in View C.
- Domain Engineer: View C plus advanced behavior.
- Platform Engineer: Views D and E.
- Reviewer/Auditor: Read-only business and technical diffs plus impact reports.

Ownership is represented as lanes, not runtime stack levels: analyst lane, domain lane, platform lane, and governance lane.

## 6.1 Concrete Examples (Intent -> Generated YAML)

This section defines abstraction as: what analysts can express without writing APEX syntax.

Analyst abstraction rules
- Analysts never author SpEL expressions.
- Analysts never author `data-source-ref`, `query-ref`, file paths, or component file references.
- Analysts never pick technical IDs directly.
- Analysts work with business labels, dropdown operators, and guided choices.
- Platform services compile those choices into valid APEX YAML.

Example 1: Eligibility checks (Decision)
- Analyst-facing input (form style):
  - Decision set name: `OTC Eligibility`
  - Check 1: Field `Notional`, operator `between`, values `1,000,000` and `500,000,000`, severity `Error`
  - Check 2: Field `Counterparty Status`, operator `equals`, value `Active`, severity `Error`
  - Group logic: `All checks must pass`
  - Execution preference: `Stop after first failed check`
- Platform-generated APEX YAML (output example):
```yaml
rules:
  - id: "trade-notional-range"
    condition: "#input.notional >= 1000000 and #input.notional <= 500000000"
    message: "Notional must be between 1M and 500M"
    severity: "ERROR"
  - id: "counterparty-active"
    condition: "#input.counterpartyStatus == 'ACTIVE'"
    message: "Counterparty must be ACTIVE"
    severity: "ERROR"
rule-groups:
  - id: "otc-eligibility"
    operator: "AND"
    stop-on-first-failure: true
    rule-ids: ["trade-notional-range", "counterparty-active"]
```

Example 2: Settlement enrichment (Data Contract + Integration)
- Analyst-facing input (table style):
  - Business capability: `Populate Settlement Instruction`
  - Source dataset (business name): `Customer Profile`
  - Mapping row: `Customer Tier` -> `Settlement Instruction`
  - Apply when: `Product Type is IRS or FX Option`
  - Missing data behavior: `Leave existing value`
- Where `lookup-config` comes from (source + derivation):
  - Source A, Analyst intent: business dataset name (`Customer Profile`) and mapping row (`Customer Tier` -> `Settlement Instruction`).
  - Source B, Platform integration catalog: curated binding for `Customer Profile`.
  - Source C, Field dictionary: canonical source field names and target field names.
- Integration catalog entry used by compiler (platform-owned, not analyst-authored):
  - `dataset-label`: `Customer Profile`
  - `data-source-ref`: `customer-database`
  - `query-ref`: `getCustomerTier`
  - `source-field-aliases`: `Customer Tier` -> `tier`
  - `approved-targets`: includes `settlementInstruction`
- Deterministic generation steps:
  1. Resolve dataset label `Customer Profile` to catalog binding (`customer-database`, `getCustomerTier`).
  2. Normalize mapping labels using field dictionary (`Customer Tier` -> `tier`; `Settlement Instruction` -> `settlementInstruction`).
  3. Build `lookup-config.lookup-dataset` from resolved refs.
  4. Build `lookup-config.field-mappings` from normalized field names.
  5. Apply policy defaults for missing data behavior and conditional execution.
- Guardrails:
  - If dataset label has no catalog binding, publish is blocked.
  - If mapping label does not resolve to an approved canonical field, publish is blocked.
  - Analyst never enters `data-source-ref` or `query-ref` directly.
- Platform-generated APEX YAML (output example):
```yaml
data-source-refs:
  - name: "customer-database"
    source: "data-sources/customer-database.yaml"
enrichments:
  - id: "settlement-instruction-enrichment"
    type: "lookup-enrichment"
    lookup-config:
      lookup-dataset:
        data-source-ref: "customer-database"
        query-ref: "getCustomerTier"
      field-mappings:
        - source-field: "tier"
          target-field: "settlementInstruction"
```

Example 3: High-value routing (Business Flow)
- Analyst-facing input (workflow style):
  - Workflow name: `FX High Value Routing`
  - Routing rule: `If notional is greater than 10M then High Value else Standard`
  - Stage A: `Run enhanced compliance` for `High Value`
  - Stage B: `Run final eligibility`
- Platform-generated APEX YAML (output example):
```yaml
metadata:
  type: "scenario-registry"
scenario-registry:
  scenarios:
    - id: "fx-high-value-routing"
      classification:
        expression: "#input.notional > 10000000 ? 'HIGH_VALUE' : 'STANDARD'"
      processing-stages:
        - stage-id: "high-value-checks"
          condition: "#classification == 'HIGH_VALUE'"
          component-refs:
            - file: "components/enhanced-compliance.yaml"
        - stage-id: "final-eligibility"
          rule-group-refs: ["otc-eligibility"]
```

Example 4: Severity policy defaults (Policy)
- Analyst/governance-facing input (policy matrix style):
  - `Warning` behavior: `Continue with defaults`
  - `Critical` behavior: `Fail fast`
  - Recovery default: `Continue with default` unless overridden
- Platform-generated APEX YAML (output example):
```yaml
error-recovery:
  enabled: true
  default-strategy: "CONTINUE_WITH_DEFAULT"
  severity-policies:
    WARNING:
      recovery-enabled: true
      strategy: "CONTINUE_WITH_DEFAULT"
    CRITICAL:
      recovery-enabled: false
      strategy: "FAIL_FAST"
```

Example 5: Audit package review (Reviewer)
- Reviewer-facing input (review checklist style):
  - Review business change summary
  - Review generated technical diff
  - Review impact report of affected scenarios/components
  - Approve or reject publish action
- Output artifacts:
  - Deterministic YAML diff
  - Validation and reference integrity report
  - Change set metadata for approval and audit trail

## 6.2 Low-Fidelity Input Form Mockups

Purpose: Start with the basics only: connection, dataset, lookup mapping, enrichment.

Basics-first scope for MVP
- In scope: `Database Connection -> Dataset -> Lookup Mapping -> Enrichment`.
- Out of scope here: workflow, policy, scenario, publish governance.

### Mockup A: Database Connection (Platform or advanced analyst)
```
+--------------------------------------------------------------------------+
| Add Data Connection                                                       |
+--------------------------------------------------------------------------+
| Connection Name*               [ Customer DB Prod                      ] |
| Database Type*                 [ PostgreSQL v ]                          |
| Host*                          [ db-prod.company.net                   ] |
| Port*                          [ 5432 ]                                  |
| Database*                      [ customer_master ]                       |
| Username*                      [ customer_reader ]                       |
| Password*                      [ *************** ]                       |
| SSL Mode*                      [ require v ]                             |
|                                                                          |
| [ Test Connection ] [ Save Connection ]                                  |
+--------------------------------------------------------------------------+
```

### Mockup B: Dataset Definition (Business-friendly)
```
+--------------------------------------------------------------------------+
| Create Dataset                                                            |
+--------------------------------------------------------------------------+
| Dataset Label*                 [ Customer Profile                      ] |
| Uses Connection*               [ Customer DB Prod v ]                    |
| Table or View*                 [ customer_profile_vw v ]                 |
| Key Field*                     [ customerId v ]                          |
| Status*                        [ Active v ]                              |
|                                                                          |
| Field Dictionary (business label -> physical field)                       |
| +----+-------------------------+--------------------------+-------------+ |
| | #  | Business Field          | Physical Field           | Type        | |
| +----+-------------------------+--------------------------+-------------+ |
| | 1  | Customer Tier           | tier                     | text        | |
| | 2  | Customer ID             | customer_id              | text        | |
| | 3  | Region                  | region_code              | text        | |
| +----+-------------------------+--------------------------+-------------+ |
|                                                                          |
| [ Validate Dataset ] [ Save Dataset ]                                    |
+--------------------------------------------------------------------------+
```

### Mockup C: Lookup Mapping (Dataset to use-case)
```
+--------------------------------------------------------------------------+
| Create Lookup Mapping                                                     |
+--------------------------------------------------------------------------+
| Mapping Name*                  [ Get Customer Tier                     ] |
| Lookup Mode*                   [ External Query Ref v ]                  |
|                               (Embedded SQL | External Query Ref)        |
| Dataset*                       [ Customer Profile v ]                    |
| Match Input Field*             [ customerId v ]                          |
| Returns*                       [ Customer Tier v ]                       |
| No Match Behavior*             [ Return empty v ]                        |
|                                                                          |
| Generated Binding Preview (read-only)                                     |
| Data Source Ref                [ customer-database ]                     |
| Query Ref                      [ getCustomerTier ]                       |
| Connection Name (embedded mode)[ demo-database ]                         |
|                                                                          |
| [ Validate Mapping ] [ Save Mapping ]                                    |
+--------------------------------------------------------------------------+
```

### Mockup D: Enrichment Definition (Business action)
```
+--------------------------------------------------------------------------+
| Create Enrichment                                                         |
+--------------------------------------------------------------------------+
| Enrichment Name*               [ Populate Settlement Instruction       ] |
| Lookup Mapping*                [ Get Customer Tier v ]                  |
| Apply When                     [ Product Type in IRS, FX Option       ] |
| Missing Data Behavior*         [ Leave existing value v ]               |
|                                                                          |
| Output Mapping                                                            |
| +----+-------------------------+-------------------------------+--------+ |
| | #  | Lookup Return Field     | Target Output Field           | Mode   | |
| +----+-------------------------+-------------------------------+--------+ |
| | 1  | Customer Tier           | settlementInstruction         | set    | |
| +----+-------------------------+-------------------------------+--------+ |
|                                                                          |
| [ Validate Enrichment ] [ Save ] [ Generate YAML Preview ]              |
+--------------------------------------------------------------------------+
```

### Basic Flow Trace (what gets generated from what)
1. Connection form creates a named technical connection asset.
2. Dataset form binds business labels to canonical fields.
3. Lookup mapping form creates one of two valid APEX patterns:
   - Embedded mode: `lookup-dataset.connection-name` + inline `query` + `parameters`.
   - External ref mode: `lookup-dataset.data-source-ref` + `query-ref`.
4. Enrichment form consumes that lookup mapping and emits lookup wiring + field mapping.

Mockup Notes
- Fields marked `*` are required.
- Technical refs are generated from saved connection/dataset/mapping assets.
- Analysts see business labels first; technical names are read-only previews.

## 6.3 Validation Against Real Example Files (Basics Only)

This section validates the basics flow against concrete `apex-demo` examples only.

Pattern A: Embedded database connection + inline query
- File: `apex-demo/src/test/resources/dev/mars/apex/demo/database/H2SimpleDatabaseConnectivityTest.yaml`
- Confirmed fields in example:
  - `data-sources` with `connection` (`database`, `username`, `password`)
  - `lookup-config.lookup-dataset.connection-name`
  - inline `query` and `parameters`
  - `field-mappings`

Pattern B: Same as A with simplified datasource demo
- File: `apex-demo/src/test/resources/dev/mars/apex/demo/datasources/database/SimpleDatabaseDataSourceTest.yaml`
- Confirmed fields in example:
  - `data-sources` database definition
  - `lookup-dataset.connection-name`
  - inline SQL query and parameter mapping
  - enrichment `field-mappings`

Pattern C: External data source references + query-ref
- File: `apex-demo/src/test/resources/dev/mars/apex/demo/conditional/conditional-mapping-design-v2.yaml`
- Confirmed fields in example:
  - `data-source-refs` with named source references
  - `lookup-dataset.data-source-ref`
  - `lookup-dataset.query-ref`
  - enrichment `field-mappings`

Pattern D: External query-ref pattern with mixed query formats
- File: `apex-demo/src/test/resources/dual-format-demo.yaml`
- Confirmed fields in example:
  - `lookup-config` with `lookup-dataset`
  - `lookup-dataset.data-source-ref`
  - `lookup-dataset.query-ref`
  - enrichment output fields (`output-field`) tied to query-ref lookups

Conclusion from validation
- The mockups in section 6.2 are valid only if they support both lookup modes above.
- `Lookup Mode` is required in authoring UX to avoid guessing and to match real APEX patterns.
- The proposal should treat `connection-name` and `data-source-ref/query-ref` as first-class, not interchangeable internals.

## 6.4 Better Form Design (Recommended)

Yes, there is a better design than static forms: a guided builder with strict progressive disclosure.

Detailed screen mockups are documented in:
- `docs-design/tasks/APEX_BASICS_SCREEN_MOCKUPS.md`

Recommended UX model
1. Step 1: Select or create connection
- Primary action: choose existing approved connection.
- Secondary action: create connection (advanced permission only).
- Immediate feedback: `Test Connection` and status badge (`Pass/Fail`).

2. Step 2: Define dataset
- Pick table/view from discovered metadata, not free text by default.
- Build business field dictionary by selecting discovered fields and assigning labels.
- Auto-suggest key field candidates.

3. Step 3: Build lookup mapping
- Explicit mode switch:
  - `Embedded query` mode (connection-name + SQL template + parameter mapper).
  - `External query-ref` mode (data-source-ref + query-ref picker).
- Side panel shows generated binding preview read-only.

4. Step 4: Define enrichment
- Choose lookup mapping asset.
- Map returned fields to target output fields using a matrix with autocomplete.
- Configure missing-data behavior with controlled options.

Design improvements over current mockups
- Replace long single-page forms with wizard steps to reduce cognitive load.
- Add inline validation per step instead of only end-of-form validation.
- Use dropdowns/autocomplete from known schema and query catalogs.
- Show technical wiring as preview only, never primary input.
- Save reusable assets at each step (`connection`, `dataset`, `lookup mapping`, `enrichment`).

Minimum validation gates per step
- Step 1: Connection test must pass.
- Step 2: Dataset must resolve and key field must be valid.
- Step 3: Lookup mode must be selected; required refs/parameters must resolve.
- Step 4: At least one valid output mapping; target fields must exist.

Why this is better for basics
- Matches how users think: connect data -> define dataset -> map lookup -> enrich output.
- Prevents technical mistakes early.
- Produces deterministic APEX artifacts from validated assets.

## 7. UX Direction (Post-Blockly)
Primary principle: YAML is an implementation artifact, not the default editor.

Recommended UI primitives:
- Task-first starts: "Validate", "Enrich", "Route", "Process Pipeline".
- Sentence/form builders for rules and conditions.
- Table/grid editing for mappings and stage lists.
- Guided templates for common financial patterns.
- Always-on preflight: structural validation, reference validation, impact analysis.
- Optional advanced YAML panel with drift warnings.

## 8. Compiler/Generator Concept
Create an Intent-to-YAML compiler that:
- Produces one or more YAML files per package.
- Separates business files and infrastructure files by design.
- Preserves stable IDs and supports deterministic regeneration.
- Emits validation diagnostics and change impact metadata.

## 9. Implementation Plan (Phased)
Phase 1: Foundations
- Define intent schema for rules, enrichments, scenarios, and components.
- Implement deterministic YAML generation for those domains.
- Integrate validation and health APIs from `apex-yaml-manager`.

Phase 2: Analyst Authoring UX
- Build form/table-based UI for Views A-C.
- Add template catalog and guided workflows.
- Add preflight/impact and publish workflow.

Phase 3: Pipeline and Advanced Orchestration
- Extend intent schema for ETL/pipeline and advanced sequencing cases.
- Add policy packs for retries, error handling, and governance metadata.

Phase 4: Round-trip and Governance
- Support advanced-mode YAML edits with drift detection.
- Add versioned change sets, approvals, and audit reporting.

## 10. Risks and Mitigations
Risk: Abstraction hides critical behavior.
- Mitigation: Dual-view (business summary + generated YAML + runtime preview).

Risk: Generator cannot cover edge-case YAML patterns.
- Mitigation: Escape hatches with explicit advanced mode and policy checks.

Risk: Fragmented ownership.
- Mitigation: Enforce role boundaries by ownership lane and explicit handoff workflow.

## 11. Immediate Next Steps
1. Draft minimal intent schema for rule + enrichment + scenario + component. (Completed on 2026-03-07; see section 11.1)
2. Select 5 representative demo configs and define expected generated output parity. (Completed on 2026-03-07; see section 11.2)
3. Build a proof-of-concept generator for those 5 cases and validate with the examples in section 6.1.
4. Validate with business analysts using scenario walkthroughs, not YAML walkthroughs.

## 11.1 Minimal Intent Schema Draft (Step 1 Completed)
Scope: Minimum analyst-safe authoring contract for Rule, Enrichment, Scenario, and Component.

Entity: RuleIntent
- Purpose: Capture a single business validation or decision check.
- Required fields:
	- `id`: Stable unique identifier (human-readable, deterministic).
	- `name`: Short business label.
	- `businessCondition`: Business-readable condition statement.
	- `severityIntent`: One of `CRITICAL`, `ERROR`, `WARNING`, `INFO`.
- Optional fields:
	- `message`: Business-facing failure or decision message.
	- `codeRef`: Business or technical code reference.
	- `tags`: Classification tags (domain, regulation, product).
	- `effectiveFrom` and `effectiveTo`: Optional validity window.
- Validation constraints:
	- `id` must be globally unique within package scope.
	- `businessCondition` must reference only known input fields.
	- `severityIntent` must align with policy defaults (View E) or declare override reason.

Entity: EnrichmentIntent
- Purpose: Describe business data derivation or lookup-driven field population.
- Required fields:
	- `id`: Stable unique identifier.
	- `name`: Business label.
	- `targetFields`: One or more output fields to populate.
	- `sourceType`: One of `lookup`, `derived`, `constant`.
- Optional fields:
	- `sourceRef`: Logical source reference (resolved by integration ownership lane).
	- `mappingRules`: Source-to-target mapping statements.
	- `whenCondition`: Optional condition controlling execution.
	- `defaultBehavior`: Behavior when source data is missing.
- Validation constraints:
	- Each `targetField` must map to an allowed schema field.
	- `sourceRef` must resolve to an approved integration asset before publish.
	- Conditional enrichments must not create circular dependencies.

Entity: ScenarioIntent
- Purpose: Define stage-based business flow and routing behavior.
- Required fields:
	- `id`: Stable scenario identifier.
	- `name`: Business process name.
	- `classificationInput`: Input attributes used for routing.
	- `stages`: Ordered list of stage definitions.
- Stage minimum fields:
	- `stageId`: Unique per scenario.
	- `stageType`: One of `classify`, `validate`, `enrich`, `decide`, `finalize`.
	- `entryCriteria`: Condition for entering stage.
	- `actions`: References to RuleIntent, EnrichmentIntent, or ComponentIntent.
- Validation constraints:
	- Stage order must be acyclic and deterministic.
	- All referenced actions must exist and be in-scope.
	- At least one terminal stage must produce an outcome.

Entity: ComponentIntent
- Purpose: Reusable bundle of actions for composition across scenarios.
- Required fields:
	- `id`: Stable component identifier.
	- `name`: Business capability label.
	- `actionRefs`: One or more references to rules, enrichments, or subcomponents.
- Optional fields:
	- `executionOrderHints`: Preferred ordering when explicit order is required.
	- `failurePolicyIntent`: High-level policy (e.g., terminate, continue-with-warnings).
	- `applicability`: Domain or product scoping metadata.
- Validation constraints:
	- Subcomponent nesting depth must remain within platform limits.
	- Duplicate or conflicting action references are not allowed.
	- Failure policy intent must be compatible with scenario-level policy.

Cross-Entity Constraints
- Reference integrity: every reference resolves to exactly one existing intent entity.
- Deterministic identity: IDs must remain stable across regenerate operations.
- Ownership boundaries:
	- Analyst lane can create/edit RuleIntent, ScenarioIntent, and approved EnrichmentIntent fields.
	- Platform lane owns integration binding details and policy enforcement.
- Explainability requirement: each generated artifact must map back to source intent IDs.

Acceptance Criteria for Step 1
- The four intent entities above are sufficient to model the representative examples in section 6.1 at business level.
- The contract is minimal (no pipeline-specific constructs yet).
- Validation constraints are explicit enough for preflight checks before generation.

Out of Scope for Step 1
- ETL/pipeline intent schema.
- Environment secret management schema.
- Round-trip edit conflict handling and merge strategies.

## 11.2 Representative Demo Set and Output Parity Criteria (Step 2 Completed)
Purpose: Define the first five reference scenarios used to validate intent-to-YAML generation quality.

Selection Criteria
- Covers core authoring views (Business Flow, Decision, Data Contract, Integration, Policy).
- Includes cross-file references and orchestration behavior.
- Includes at least one policy-heavy case (severity and recovery semantics).
- Keeps scope to high-signal demos already used in section 6.1.

Reference Set of 5 Demo Configs
1. `apex-demo/src/test/resources/dev/mars/apex/demo/basic/BasicYamlRuleGroupProcessingATest.yaml`
2. `apex-demo/src/test/resources/dev/mars/apex/demo/lookup/SimpleFieldLookupDemoTest.yaml`
3. `apex-demo/src/test/resources/dev/mars/apex/demo/enrichment/comprehensivefinancialsettlementdemo.yaml`
4. `apex-demo/src/test/resources/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml`
5. `apex-demo/src/test/resources/dev/mars/apex/demo/errorhandling/SimpleErrorHandlingTest.yaml`

Parity Definition
- Structural parity: Generated artifacts include the same conceptual sections and references required by the target behavior.
- Behavioral parity: For equivalent test inputs, outcome class is equivalent (for example, pass/fail route, enrichment applied/not applied, fail-fast/continue behavior).
- Policy parity: Severity and failure/recovery intent produce equivalent execution decisions.
- Traceability parity: Every generated artifact section is traceable back to source intent IDs.

Per-Demo Expected Parity

Demo 1: Basic rule-group validation
- Source: `apex-demo/src/test/resources/dev/mars/apex/demo/basic/BasicYamlRuleGroupProcessingATest.yaml`
- Expected parity:
	- Decision intent maps to rules and a rule-group with equivalent operator semantics.
	- Group evaluation behavior (including short-circuit where applicable) is preserved.
	- Message and severity intent remain consistent at decision outcome level.

Demo 2: Simple lookup enrichment
- Source: `apex-demo/src/test/resources/dev/mars/apex/demo/lookup/SimpleFieldLookupDemoTest.yaml`
- Expected parity:
	- Data Contract intent maps to enrichment target fields and mapping logic.
	- Integration intent resolves to valid source references for lookup execution.
	- Enrichment trigger conditions and populated output fields are equivalent.

Demo 3: Comprehensive settlement enrichment
- Source: `apex-demo/src/test/resources/dev/mars/apex/demo/enrichment/comprehensivefinancialsettlementdemo.yaml`
- Expected parity:
	- Multiple enrichment intents compose deterministically with stable ordering.
	- Field-level mapping outcomes are equivalent for representative settlement inputs.
	- Cross-section dependencies between validation and enrichment remain consistent.

Demo 4: Scenario and component orchestration
- Source: `apex-demo/src/test/resources/dev/mars/apex/demo/scenario/ComponentScenarioTest-registry.yaml`
- Expected parity:
	- Business Flow intent maps to equivalent stage progression and scenario routing.
	- Component references resolve to the intended action bundles without ambiguity.
	- Stage-level execution results remain equivalent (successful path and guarded path).

Demo 5: Error handling and recovery semantics
- Source: `apex-demo/src/test/resources/dev/mars/apex/demo/errorhandling/SimpleErrorHandlingTest.yaml`
- Expected parity:
	- Policy intent produces equivalent severity handling behavior.
	- Fail-fast versus continue behavior matches expected execution paths.
	- Recovery-related decisions are consistent with policy defaults and overrides.

Parity Review Checklist (for all 5)
- Reference integrity checks pass with no unresolved refs.
- Deterministic regeneration produces stable identifiers and ordering.
- Impact report identifies changed intents and affected generated artifacts.
- Reviewer can map each generated construct back to business intent without reading raw YAML first.

Exit Criteria for Step 2
- All five demos have documented parity expectations.
- Parity is defined in structural, behavioral, policy, and traceability terms.
- The set is sufficient to begin Step 3 proof-of-concept evaluation.

## 12. Decisions Needed
- Confirm role boundaries for Views A-E and ownership lanes.
- Confirm whether pipeline/ETL is in MVP or phase 2.
- Confirm advanced-mode editing policy (allowed roles and guardrails).
- Confirm canonical file layout for generated business vs infrastructure YAML.
