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

import java.util.HashSet;
import java.util.Set;

/**
 * Validates that all identifiable elements in YAML configuration have unique IDs/names.
 *
 * <p>Checks for duplicates across:
 * <ul>
 *   <li>Rule IDs</li>
 *   <li>Enrichment IDs</li>
 *   <li>Data source names</li>
 *   <li>Rule group IDs</li>
 *   <li>Rule chain IDs</li>
 *   <li>Category names</li>
 * </ul>
 *
 * <p>Extracted from {@code ConfigurationLoader} as part of the validation layer refactoring.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 */
public class DuplicateValidator {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateValidator.class);

    /**
     * Validate all elements in the configuration for duplicate identifiers.
     *
     * @param config the YAML rule configuration to validate
     * @throws ConfigurationException if any duplicate identifiers are found
     */
    public void validate(YamlRuleConfiguration config) throws ConfigurationException {
        validateDuplicateRuleIds(config);
        validateDuplicateEnrichmentIds(config);
        validateDuplicateDataSourceNames(config);
        validateDuplicateRuleGroupIds(config);
        validateDuplicateRuleChainIds(config);
        validateDuplicateCategoryNames(config);
    }

    /**
     * Validate for duplicate rule IDs.
     */
    private void validateDuplicateRuleIds(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRules() != null) {
            Set<String> seenIds = new HashSet<>();
            for (YamlRule rule : config.getRules()) {
                if (rule.getId() != null && !rule.getId().trim().isEmpty()) {
                    String ruleId = rule.getId().trim();
                    if (seenIds.contains(ruleId)) {
                        throw new ConfigurationException("Duplicate rule ID found: '" + ruleId +
                            "'. Rule IDs must be unique within the configuration.");
                    }
                    seenIds.add(ruleId);
                }
            }
        }
    }

    /**
     * Validate for duplicate enrichment IDs.
     */
    private void validateDuplicateEnrichmentIds(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getEnrichments() == null) {
            return;
        }

        Set<String> enrichmentIds = new HashSet<>();
        for (YamlEnrichment enrichment : config.getEnrichments()) {
            String id = enrichment.getId();
            if (id != null) {
                if (!enrichmentIds.add(id)) {
                    throw new ConfigurationException("Duplicate enrichment ID found: " + id);
                }
            }
        }
    }

    /**
     * Validate for duplicate data source names.
     */
    private void validateDuplicateDataSourceNames(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getDataSources() != null) {
            Set<String> seenNames = new HashSet<>();
            for (YamlDataSource dataSource : config.getDataSources()) {
                if (dataSource.getName() != null && !dataSource.getName().trim().isEmpty()) {
                    String name = dataSource.getName().trim();
                    if (seenNames.contains(name)) {
                        throw new ConfigurationException("Duplicate data source name found: '" + name +
                            "'. Data source names must be unique within the configuration.");
                    }
                    seenNames.add(name);
                }
            }
        }
    }

    /**
     * Validate for duplicate rule group IDs.
     */
    private void validateDuplicateRuleGroupIds(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleGroups() != null) {
            Set<String> seenIds = new HashSet<>();
            for (YamlRuleGroup group : config.getRuleGroups()) {
                if (group.getId() != null && !group.getId().trim().isEmpty()) {
                    String groupId = group.getId().trim();
                    if (seenIds.contains(groupId)) {
                        throw new ConfigurationException("Duplicate rule group ID found: '" + groupId +
                            "'. Rule group IDs must be unique within the configuration.");
                    }
                    seenIds.add(groupId);
                }
            }
        }
    }

    /**
     * Validate for duplicate rule chain IDs.
     */
    private void validateDuplicateRuleChainIds(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getRuleChains() != null) {
            Set<String> seenIds = new HashSet<>();
            for (YamlRuleChain ruleChain : config.getRuleChains()) {
                if (ruleChain.getId() != null && !ruleChain.getId().trim().isEmpty()) {
                    String chainId = ruleChain.getId().trim();
                    if (seenIds.contains(chainId)) {
                        throw new ConfigurationException("Duplicate rule chain ID found: '" + chainId +
                            "'. Rule chain IDs must be unique within the configuration.");
                    }
                    seenIds.add(chainId);
                }
            }
        }
    }

    /**
     * Validate for duplicate category names.
     */
    private void validateDuplicateCategoryNames(YamlRuleConfiguration config) throws ConfigurationException {
        if (config.getCategories() != null) {
            Set<String> seenNames = new HashSet<>();
            for (YamlCategory category : config.getCategories()) {
                if (category.getName() != null && !category.getName().trim().isEmpty()) {
                    String name = category.getName().trim();
                    if (seenNames.contains(name)) {
                        throw new ConfigurationException("Duplicate category name found: '" + name +
                            "'. Category names must be unique within the configuration.");
                    }
                    seenNames.add(name);
                }
            }
        }
    }
}
