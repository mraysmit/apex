package dev.mars.apex.core.config.validation;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.config.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Validates cross-component references within a YAML configuration, ensuring
 * referential integrity across rules, rule groups, rule chains, enrichments,
 * and data sources. Also detects circular dependencies in sequential-dependency chains.
 *
 * <p>Extracted from {@code ConfigurationLoader} as part of the validation layer refactoring.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 */
public class CrossReferenceValidator {

    private static final Logger logger = LoggerFactory.getLogger(CrossReferenceValidator.class);

    /**
     * Validate cross-component references: rule group → rule, rule chain → rule, circular deps.
     *
     * @param config the YAML rule configuration to validate
     * @throws ConfigurationException if any cross-reference is invalid
     */
    public void validateCrossComponentReferences(YamlRuleConfiguration config) throws ConfigurationException {
        // Build reference maps for validation
        Set<String> ruleIds = buildRuleIdSet(config);
        // Validate rule group references
        validateRuleGroupReferences(config, ruleIds);

        // Validate rule chain references
        validateRuleChainReferences(config, ruleIds);

        // Validate circular dependencies in rule chains
        validateCircularDependencies(config);
    }

    /**
     * Validate enrichment references to data sources and other components.
     *
     * @param config the YAML rule configuration to validate
     * @throws ConfigurationException if any enrichment reference is invalid
     */
    public void validateEnrichmentReferences(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getEnrichments() == null || config.getEnrichments().isEmpty()) {
            return;
        }

        // Build reference maps for validation
        Set<String> dataSourceNames = buildDataSourceNameSet(config);
        for (YamlEnrichment enrichment : config.getEnrichments()) {
            String enrichmentId = enrichment.getId();

            // Validate lookup service references
            if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupService() != null) {
                String serviceName = enrichment.getLookupConfig().getLookupService();
                if (!dataSourceNames.contains(serviceName)) {
                    throw new ConfigurationException("Enrichment '" + enrichmentId + "' references unknown lookup service: " + serviceName);
                }
            }

            // Validate file path references for file-based datasets
            if (enrichment.getLookupConfig() != null && enrichment.getLookupConfig().getLookupDataset() != null) {
                YamlEnrichment.LookupDataset dataset = enrichment.getLookupConfig().getLookupDataset();
                if (dataset.getFilePath() != null && !dataset.getFilePath().trim().isEmpty()) {
                    validateFilePathReference(dataset.getFilePath(), enrichmentId);
                }
            }

            // Validate target type references if specified
            if (enrichment.getTargetType() != null && !enrichment.getTargetType().trim().isEmpty()) {
                validateTargetTypeReference(enrichment.getTargetType(), enrichmentId);
            }
        }

        logger.debug("Enrichment reference validation completed successfully");
    }

    /**
     * Validate file path references in enrichments.
     */
    private void validateFilePathReference(String filePath, String enrichmentId) throws ConfigurationException {
        // Check for absolute vs relative paths
        if (filePath.startsWith("/") || filePath.matches("^[A-Za-z]:.*")) {
            logger.warn("Enrichment '" + enrichmentId + "' uses absolute file path: " + filePath + ". Consider using relative paths for portability");
        }

        // Check for potentially problematic path patterns
        if (filePath.contains("..")) {
            logger.warn("Enrichment '" + enrichmentId + "' uses parent directory references in file path: " + filePath + ". This may cause security or portability issues");
        }

        // Check for common file path issues
        if (filePath.contains("\\")) {
            logger.info("Enrichment '" + enrichmentId + "' uses backslashes in file path: " + filePath + ". Consider using forward slashes for cross-platform compatibility");
        }
    }

    /**
     * Validate target type references in enrichments.
     */
    private void validateTargetTypeReference(String targetType, String enrichmentId) throws ConfigurationException {
        // Check for valid Java class name format
        if (!targetType.matches("^[a-zA-Z_$][a-zA-Z\\d_$]*(?:\\.[a-zA-Z_$][a-zA-Z\\d_$]*)*$")) {
            throw new ConfigurationException("Invalid target type format '" + targetType + "' for enrichment: " + enrichmentId + ". Must be a valid Java class name");
        }

        // Warn about common issues
        if (targetType.contains("..")) {
            throw new ConfigurationException("Invalid target type '" + targetType + "' for enrichment: " + enrichmentId + ". Contains consecutive dots");
        }

        if (targetType.startsWith(".") || targetType.endsWith(".")) {
            throw new ConfigurationException("Invalid target type '" + targetType + "' for enrichment: " + enrichmentId + ". Cannot start or end with dot");
        }
    }

    /**
     * Build a set of all rule IDs in the configuration.
     */
    private Set<String> buildRuleIdSet(YamlRuleConfiguration config) {
        Set<String> ruleIds = new HashSet<>();
        if (config.getRules() != null) {
            for (YamlRule rule : config.getRules()) {
                if (rule.getId() != null) {
                    ruleIds.add(rule.getId());
                }
            }
        }
        return ruleIds;
    }

    /**
     * Build a set of all data source names in the configuration.
     */
    private Set<String> buildDataSourceNameSet(YamlRuleConfiguration config) {
        Set<String> dataSourceNames = new HashSet<>();
        if (config.getDataSources() != null) {
            for (YamlDataSource dataSource : config.getDataSources()) {
                if (dataSource.getName() != null) {
                    dataSourceNames.add(dataSource.getName());
                }
            }
        }
        return dataSourceNames;
    }

    /**
     * Validate rule references in rule groups.
     */
    private void validateRuleGroupReferences(YamlRuleConfiguration config, Set<String> ruleIds) throws ConfigurationException {
        if (config.getRuleGroups() != null) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                if (group.getRuleIds() != null) {
                    for (String ruleId : group.getRuleIds()) {
                        if (ruleId != null && !ruleId.trim().isEmpty() && !ruleIds.contains(ruleId)) {
                            throw new ConfigurationException("Rule reference not found: Rule '" + ruleId +
                                "' referenced in rule group '" + group.getId() + "' does not exist");
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate rule references in rule chains.
     */
    private void validateRuleChainReferences(YamlRuleConfiguration config, Set<String> ruleIds) throws ConfigurationException {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                Map<String, Object> chainConfig = ruleChain.getConfiguration();
                if (chainConfig != null) {
                    validateRuleReferencesInChainConfig(chainConfig, ruleIds, ruleChain.getId());
                }
            }
        }
    }

    /**
     * Recursively validate rule references in rule chain configuration.
     */
    @SuppressWarnings("unchecked")
    private void validateRuleReferencesInChainConfig(Map<String, Object> config, Set<String> ruleIds, String chainId) throws ConfigurationException {
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Check for rule-id references
            if ("rule-id".equals(key) && value instanceof String) {
                String ruleId = (String) value;
                if (!ruleId.trim().isEmpty() && !ruleIds.contains(ruleId)) {
                    throw new ConfigurationException("Rule reference not found: Rule '" + ruleId +
                        "' referenced in rule chain '" + chainId + "' does not exist");
                }
            }

            // Recursively check nested maps and lists
            if (value instanceof Map) {
                validateRuleReferencesInChainConfig((Map<String, Object>) value, ruleIds, chainId);
            } else if (value instanceof List) {
                List<Object> list = (List<Object>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        validateRuleReferencesInChainConfig((Map<String, Object>) item, ruleIds, chainId);
                    }
                }
            }
        }
    }

    /**
     * Validate for circular dependencies in rule chains.
     */
    private void validateCircularDependencies(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleChains() != null) {
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                if ("sequential-dependency".equals(ruleChain.getPattern())) {
                    validateSequentialDependencyCircularDependencies(ruleChain);
                }
            }
        }
    }

    /**
     * Validate circular dependencies in sequential-dependency rule chains.
     */
    @SuppressWarnings("unchecked")
    private void validateSequentialDependencyCircularDependencies(YamlRuleChain ruleChain) throws ConfigurationException {
        Map<String, Object> config = ruleChain.getConfiguration();
        if (config == null || !config.containsKey("stages")) {
            return; // Already validated in pattern validation
        }

        Object stagesObj = config.get("stages");
        if (!(stagesObj instanceof List)) {
            return; // Already validated in pattern validation
        }

        List<Map<String, Object>> stages = (List<Map<String, Object>>) stagesObj;
        Map<String, Set<String>> dependencies = new HashMap<>();

        // Build dependency graph
        for (Map<String, Object> stage : stages) {
            String stageId = getStageId(stage);
            if (stageId != null) {
                Set<String> stageDeps = new HashSet<>();
                Object dependsOnObj = stage.get("depends-on");
                if (dependsOnObj instanceof List) {
                    List<String> dependsOn = (List<String>) dependsOnObj;
                    stageDeps.addAll(dependsOn);
                }
                dependencies.put(stageId, stageDeps);
            }
        }

        // Check for circular dependencies using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String stageId : dependencies.keySet()) {
            if (hasCircularDependency(stageId, dependencies, visited, recursionStack)) {
                throw new ConfigurationException("Circular dependency detected in rule chain '" +
                    ruleChain.getId() + "' involving stage: " + stageId);
            }
        }
    }

    /**
     * Get stage ID from stage configuration.
     */
    private String getStageId(Map<String, Object> stage) {
        Object stageObj = stage.get("stage");
        if (stageObj != null) {
            return stageObj.toString();
        }
        Object ruleIdObj = stage.get("rule-id");
        if (ruleIdObj != null) {
            return ruleIdObj.toString();
        }
        return null;
    }

    /**
     * Check for circular dependencies using DFS.
     */
    private boolean hasCircularDependency(String stageId, Map<String, Set<String>> dependencies,
                                         Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(stageId)) {
            return true; // Circular dependency found
        }
        if (visited.contains(stageId)) {
            return false; // Already processed
        }

        visited.add(stageId);
        recursionStack.add(stageId);

        Set<String> deps = dependencies.get(stageId);
        if (deps != null) {
            for (String dep : deps) {
                if (hasCircularDependency(dep, dependencies, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(stageId);
        return false;
    }
}
