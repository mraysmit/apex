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