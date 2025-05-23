
# Parameter Extraction Utility for Rules Engine

To address the requirement of scanning rule conditions for parameters and matching them to the facts map, I've designed a utility function that extracts parameters from rule conditions and ensures they're properly mapped to facts.

## Solution Overview

The solution involves creating a utility class with methods to:
1. Extract parameters from rule conditions
2. Validate that all required parameters exist in the facts map
3. Optionally add missing parameters with default values

## Implementation

```java
package com.rulesengine.core.util;

import com.rulesengine.core.engine.model.Rule;
import com.rulesengine.core.engine.model.RuleGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Utility class for extracting and managing rule parameters.
 * This class provides functionality to scan rule conditions for parameters
 * and match them against a facts map.
 */
public class RuleParameterExtractor {
    private static final Logger LOGGER = Logger.getLogger(RuleParameterExtractor.class.getName());
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("#([a-zA-Z0-9_]+)(?![a-zA-Z0-9_])");

    /**
     * Extract all parameters from a rule condition.
     * Parameters are identified by the # symbol followed by a valid identifier.
     *
     * @param condition The rule condition to scan
     * @return A set of parameter names (without the # symbol)
     */
    public static Set<String> extractParameters(String condition) {
        Set<String> parameters = new HashSet<>();
        if (condition == null || condition.isEmpty()) {
            return parameters;
        }

        Matcher matcher = PARAMETER_PATTERN.matcher(condition);
        while (matcher.find()) {
            parameters.add(matcher.group(1));
        }
        
        return parameters;
    }

    /**
     * Extract all parameters from a rule.
     *
     * @param rule The rule to extract parameters from
     * @return A set of parameter names
     */
    public static Set<String> extractParameters(Rule rule) {
        if (rule == null) {
            return new HashSet<>();
        }
        return extractParameters(rule.getCondition());
    }

    /**
     * Extract all parameters from a rule group.
     *
     * @param ruleGroup The rule group to extract parameters from
     * @return A set of parameter names
     */
    public static Set<String> extractParameters(RuleGroup ruleGroup) {
        Set<String> parameters = new HashSet<>();
        if (ruleGroup == null) {
            return parameters;
        }

        for (Rule rule : ruleGroup.getRules()) {
            parameters.addAll(extractParameters(rule));
        }
        
        return parameters;
    }

    /**
     * Validate that all required parameters exist in the facts map.
     *
     * @param rule The rule to validate
     * @param facts The facts map to check against
     * @return A set of missing parameter names, or an empty set if all parameters are present
     */
    public static Set<String> validateParameters(Rule rule, Map<String, Object> facts) {
        Set<String> parameters = extractParameters(rule);
        return getMissingParameters(parameters, facts);
    }

    /**
     * Validate that all required parameters exist in the facts map.
     *
     * @param ruleGroup The rule group to validate
     * @param facts The facts map to check against
     * @return A set of missing parameter names, or an empty set if all parameters are present
     */
    public static Set<String> validateParameters(RuleGroup ruleGroup, Map<String, Object> facts) {
        Set<String> parameters = extractParameters(ruleGroup);
        return getMissingParameters(parameters, facts);
    }

    /**
     * Get the set of parameters that are missing from the facts map.
     *
     * @param parameters The set of required parameters
     * @param facts The facts map to check against
     * @return A set of missing parameter names
     */
    private static Set<String> getMissingParameters(Set<String> parameters, Map<String, Object> facts) {
        Set<String> missingParameters = new HashSet<>();
        
        if (facts == null) {
            return parameters; // All parameters are missing if facts is null
        }
        
        for (String parameter : parameters) {
            if (!facts.containsKey(parameter)) {
                missingParameters.add(parameter);
            }
        }
        
        return missingParameters;
    }

    /**
     * Ensure that all required parameters exist in the facts map.
     * If a parameter is missing, it will be added with a null value.
     *
     * @param rule The rule to ensure parameters for
     * @param facts The facts map to update
     * @return The updated facts map
     */
    public static Map<String, Object> ensureParameters(Rule rule, Map<String, Object> facts) {
        Set<String> parameters = extractParameters(rule);
        return ensureParameters(parameters, facts);
    }

    /**
     * Ensure that all required parameters exist in the facts map.
     * If a parameter is missing, it will be added with a null value.
     *
     * @param ruleGroup The rule group to ensure parameters for
     * @param facts The facts map to update
     * @return The updated facts map
     */
    public static Map<String, Object> ensureParameters(RuleGroup ruleGroup, Map<String, Object> facts) {
        Set<String> parameters = extractParameters(ruleGroup);
        return ensureParameters(parameters, facts);
    }

    /**
     * Ensure that all required parameters exist in the facts map.
     * If a parameter is missing, it will be added with a null value.
     *
     * @param parameters The set of required parameters
     * @param facts The facts map to update
     * @return The updated facts map
     */
    private static Map<String, Object> ensureParameters(Set<String> parameters, Map<String, Object> facts) {
        Map<String, Object> updatedFacts = facts != null ? new HashMap<>(facts) : new HashMap<>();
        
        for (String parameter : parameters) {
            if (!updatedFacts.containsKey(parameter)) {
                LOGGER.warning("Adding missing parameter to facts map: " + parameter);
                updatedFacts.put(parameter, null);
            }
        }
        
        return updatedFacts;
    }
}
```

## Integration with RulesEngine

To integrate this utility with the RulesEngine, we can modify the `executeRule` method to automatically check for required parameters:

```java
public RuleResult executeRule(Rule rule, Map<String, Object> facts) {
    if (rule == null) {
        LOGGER.info("No rule provided for execution");
        return RuleResult.noRules();
    }

    LOGGER.info("Executing rule: " + rule.getName());
    LOGGER.fine("Facts provided: " + (facts != null ? facts.keySet() : "none"));

    // Check for missing parameters
    Set<String> missingParameters = RuleParameterExtractor.validateParameters(rule, facts);
    if (!missingParameters.isEmpty()) {
        LOGGER.warning("Missing parameters for rule '" + rule.getName() + "': " + missingParameters);
        return RuleResult.error(rule.getName(), "Missing parameters: " + missingParameters);
    }

    StandardEvaluationContext context = createContext(facts);

    // Evaluate the rule
    LOGGER.fine("Evaluating rule: " + rule.getName());
    try {
        Expression exp = parser.parseExpression(rule.getCondition());
        Boolean result = exp.getValue(context, Boolean.class);
        LOGGER.fine("Rule '" + rule.getName() + "' evaluated to: " + result);

        if (result != null && result) {
            LOGGER.info("Rule matched: " + rule.getName());
            return RuleResult.match(rule.getName(), rule.getMessage());
        }
    } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error evaluating rule '" + rule.getName() + "': " + e.getMessage(), e);
        return RuleResult.error(rule.getName(), "Error evaluating rule: " + e.getMessage());
    }

    LOGGER.info("Rule did not match: " + rule.getName());
    return RuleResult.noMatch();
}
```

## Usage Example

Here's how the utility can be used in the DynamicProductValidatorDemo:

```java
// Extract parameters from a rule condition
String condition = "#product != null && #product.price >= #minPrice && #product.price <= #maxPrice";
Set<String> parameters = RuleParameterExtractor.extractParameters(condition);
System.out.println("Parameters: " + parameters); // Output: [product, minPrice, maxPrice]

// Create a facts map
Map<String, Object> facts = new HashMap<>();
facts.put("product", new Product("Test Product", 150.0, "Test"));
facts.put("minPrice", 100.0);
// maxPrice is missing

// Validate parameters
Set<String> missingParams = RuleParameterExtractor.validateParameters(rule, facts);
System.out.println("Missing parameters: " + missingParams); // Output: [maxPrice]

// Ensure all parameters exist
facts = RuleParameterExtractor.ensureParameters(rule, facts);
// facts now contains "maxPrice" with a null value
```

## Benefits

1. **Automatic Parameter Detection**: No need to manually specify which parameters a rule needs.
2. **Early Validation**: Detect missing parameters before rule evaluation, preventing runtime errors.
3. **Improved Error Messages**: Provide clear error messages about which parameters are missing.
4. **Flexibility**: Works with any rule condition without requiring changes to the Rule class.
5. **Extensibility**: Can be extended to provide default values for missing parameters.

This solution maintains a clear separation between the core rules engine and the parameter extraction functionality, following SOLID principles. It also provides a flexible way to handle rule parameters without requiring changes to existing code.