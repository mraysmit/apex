# APEX Sequencing Design Flaw Demonstration

This folder contains **concrete proof** that APEX has a fundamental design flaw in its YAML processing architecture.

## The Design Flaw

**Problem**: APEX completely ignores the natural order of YAML sections, violating basic configuration system principles and breaking developer expectations.

**Impact**: Developers cannot implement basic sequential business logic patterns because APEX ignores their explicit intent expressed through YAML structure.

## Demonstration Files

### 1. `enrich-then-validate.yaml`
**Developer Intent**: Calculate risk score FIRST, then validate it
```yaml
# Natural order: enrichments -> rules
enrichments:
  - id: "calculate-risk-score"
    # Calculate risk based on amount
    
rules:
  - id: "validate-risk-threshold"
    condition: "#riskScore < 0.8"  # Depends on enrichment!
```

**APEX Reality**: Processes rules FIRST, enrichments SECOND
**Result**: Rule fails because `#riskScore` doesn't exist yet

### 2. `validate-then-enrich.yaml`
**Developer Intent**: Validate data FIRST, then enrich only valid records
```yaml
# Natural order: rules -> enrichments
rules:
  - id: "validate-customer-id"
    # Validate customer ID exists
    
enrichments:
  - id: "expensive-customer-lookup"
    condition: "#ruleResults.get('validate-customer-id').passed"
    # Only lookup valid customers
```

**APEX Reality**: May process enrichments FIRST (depending on processor)
**Result**: Expensive lookups run on invalid data

### 3. `SequencingFlawDemoTest.java`
**Purpose**: Proves the design flaw with concrete test cases
- Demonstrates broken "enrich-then-validate" pattern
- Shows inefficient "validate-then-enrich" behavior
- Provides logging to show exactly what goes wrong

## How to Run the Demonstration

```bash
cd apex-demo
mvn test -Dtest=SequencingFlawDemoTest
```

## Expected Results

The tests will demonstrate:

1. **Broken Business Logic**: Rules that depend on enriched fields fail because enrichments haven't run yet
2. **Performance Issues**: Expensive enrichments run on invalid data because validation hasn't occurred
3. **Unpredictable Behavior**: Different processors use different hardcoded orders, making behavior inconsistent

## The Fix

The solution is documented in `docs/design/apex-yaml-order.md`:

1. **Respect natural YAML order** as the default processing sequence
2. **Process sections top-to-bottom** as they appear in the file
3. **Preserve backward compatibility** with metadata flags
4. **Make processing order visible** and predictable from YAML structure

## Industry Comparison

**Every other major configuration system respects document order**:
- Docker Compose: Services start in file order
- Kubernetes: Resources apply in file order
- Ansible: Tasks execute in file order
- GitHub Actions: Steps run in file order

**APEX is the exception** - and it's wrong.
