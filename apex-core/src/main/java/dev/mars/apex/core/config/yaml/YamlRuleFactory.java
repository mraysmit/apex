package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.api.RuleSet;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.metadata.RuleMetadata;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

/**
 * Factory service for converting YAML configuration objects into rules engine objects.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Factory service for converting YAML configuration objects into rules engine objects.
 * This class handles the transformation from YAML configuration to actual Rule, RuleGroup, and Category objects.
 */
public class YamlRuleFactory {
    
    private static final Logger LOGGER = Logger.getLogger(YamlRuleFactory.class.getName());

    private final Map<String, Category> categoryCache = new HashMap<>();

    // Cache for YAML categories to enable metadata inheritance
    private final Map<String, YamlCategory> yamlCategoryCache = new HashMap<>();
    
    /**
     * Create a RulesEngineConfiguration from YAML configuration using the new generic architecture.
     * This method leverages the GenericRuleSet for enhanced validation and metadata support.
     *
     * @param yamlConfig The YAML configuration
     * @return A configured RulesEngineConfiguration
     */
    public RulesEngineConfiguration createRulesEngineConfiguration(YamlRuleConfiguration yamlConfig) throws YamlConfigurationException {
        LOGGER.info("Creating RulesEngineConfiguration from YAML configuration using generic architecture");

        RulesEngineConfiguration config = new RulesEngineConfiguration();

        // Process categories first to populate cache
        if (yamlConfig.getCategories() != null) {
            for (YamlCategory yamlCategory : yamlConfig.getCategories()) {
                if (yamlCategory.getEnabled() == null || yamlCategory.getEnabled()) {
                    Category category = createCategory(yamlCategory);
                    categoryCache.put(category.getName(), category);
                    // Also cache the YAML category for metadata inheritance
                    yamlCategoryCache.put(yamlCategory.getName(), yamlCategory);
                    LOGGER.fine("Cached category '" + yamlCategory.getName() +
                               "' with businessOwner: " + yamlCategory.getBusinessOwner() +
                               ", businessDomain: " + yamlCategory.getBusinessDomain());
                }
            }
        }

        // Group rules by category for GenericRuleSet creation
        if (yamlConfig.getRules() != null) {
            Map<String, List<YamlRule>> rulesByCategory = yamlConfig.getRules().stream()
                .filter(rule -> rule.getEnabled() == null || rule.getEnabled())
                .collect(Collectors.groupingBy(rule ->
                    rule.getCategory() != null ? rule.getCategory() : "default"));

            // Create GenericRuleSet for each category
            for (Map.Entry<String, List<YamlRule>> entry : rulesByCategory.entrySet()) {
                String categoryName = entry.getKey();
                List<YamlRule> categoryRules = entry.getValue();

                try {
                    // Use individual rule creation for better metadata support
                    for (YamlRule yamlRule : categoryRules) {
                        try {
                            Rule rule = createRuleWithMetadata(yamlRule);
                            config.registerRule(rule);
                        } catch (Exception ruleException) {
                            LOGGER.warning("Failed to create rule '" + yamlRule.getId() +
                                          "': " + ruleException.getMessage());
                        }
                    }

                    LOGGER.info("Created " + categoryRules.size() + " rules for category '" + categoryName +
                               "' using enhanced metadata support");

                } catch (Exception e) {
                    LOGGER.warning("Failed to create rules for category '" + categoryName +
                                  "': " + e.getMessage());
                }
            }
        }

        // Process rule groups (legacy support)
        if (yamlConfig.getRuleGroups() != null) {
            LOGGER.info("Processing " + yamlConfig.getRuleGroups().size() + " rule groups");
            for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
                LOGGER.info("Processing rule group: " + yamlGroup.getId() + ", enabled: " + yamlGroup.getEnabled());
                if (yamlGroup.getEnabled() == null || yamlGroup.getEnabled()) {
                    try {
                        LOGGER.info("Creating rule group: " + yamlGroup.getId());
                        RuleGroup group = createRuleGroup(yamlGroup, config);
                        config.registerRuleGroup(group);
                        LOGGER.info("Successfully registered rule group: " + yamlGroup.getId());
                    } catch (YamlConfigurationException e) {
                        // Re-throw configuration exceptions to fail fast
                        LOGGER.severe("YamlConfigurationException for rule group " + yamlGroup.getId() + ": " + e.getMessage());
                        throw e;
                    } catch (Exception e) {
                        LOGGER.warning("Failed to create rule group '" + yamlGroup.getId() +
                                      "': " + e.getMessage());
                    }
                } else {
                    LOGGER.info("Skipping disabled rule group: " + yamlGroup.getId());
                }
            }

            // Second phase: Process rule group references now that all rule groups are created
            LOGGER.info("Processing rule group references in second phase");
            processRuleGroupReferences(yamlConfig, config);
        }

        LOGGER.info("Successfully created RulesEngineConfiguration with " +
                   config.getAllRules().size() + " rules and " +
                   config.getAllRuleGroups().size() + " rule groups");

        return config;
    }
    
    /**
     * Create a GenericRuleSet from YAML configuration for a specific category.
     * This method leverages the new generic architecture with full enterprise metadata support.
     *
     * This is a public API method intended for advanced users who need fine-grained control
     * over rule set creation for specific categories.
     *
     * @param categoryName The category name
     * @param yamlRules The list of YAML rules for this category
     * @return A configured GenericRuleSet
     */
    @SuppressWarnings("unused") // Public API method for advanced users
    public RuleSet.GenericRuleSet createGenericRuleSet(String categoryName, List<YamlRule> yamlRules) {
        LOGGER.fine("Creating GenericRuleSet for category: " + categoryName + " with " + yamlRules.size() + " rules");

        // Validate category name using the same validation as the generic API
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }

        RuleSet.GenericRuleSet ruleSet = RuleSet.category(categoryName);

        // Apply common metadata from the first rule or category configuration
        if (!yamlRules.isEmpty()) {
            YamlRule firstRule = yamlRules.get(0);

            // Apply enterprise metadata if available
            if (firstRule.getCreatedBy() != null) {
                ruleSet.withCreatedBy(firstRule.getCreatedBy());
            }
            if (firstRule.getBusinessDomain() != null) {
                ruleSet.withBusinessDomain(firstRule.getBusinessDomain());
            }
            if (firstRule.getBusinessOwner() != null) {
                ruleSet.withBusinessOwner(firstRule.getBusinessOwner());
            }
            if (firstRule.getSourceSystem() != null) {
                ruleSet.withSourceSystem(firstRule.getSourceSystem());
            }

            // Parse and apply dates
            if (firstRule.getEffectiveDate() != null) {
                try {
                    ruleSet.withEffectiveDate(Instant.parse(firstRule.getEffectiveDate()));
                } catch (DateTimeParseException e) {
                    LOGGER.warning("Invalid effective date format for rule " + firstRule.getId() +
                                  ": " + firstRule.getEffectiveDate());
                }
            }
            if (firstRule.getExpirationDate() != null) {
                try {
                    ruleSet.withExpirationDate(Instant.parse(firstRule.getExpirationDate()));
                } catch (DateTimeParseException e) {
                    LOGGER.warning("Invalid expiration date format for rule " + firstRule.getId() +
                                  ": " + firstRule.getExpirationDate());
                }
            }
        }

        // Add rules with validation and metadata
        for (YamlRule yamlRule : yamlRules) {
            try {
                String name = yamlRule.getName();
                String condition = yamlRule.getCondition();
                String message = yamlRule.getMessage() != null ? yamlRule.getMessage() :
                                "Rule " + name + " triggered";
                String description = yamlRule.getDescription() != null ? yamlRule.getDescription() :
                                   message;
                String severity = yamlRule.getSeverity() != null ? yamlRule.getSeverity() : SeverityConstants.DEFAULT_SEVERITY;

                ruleSet.customRuleWithSeverity(name, condition, message, description, severity);

                LOGGER.fine("Added rule '" + name + "' with severity '" + severity + "' to GenericRuleSet for category: " + categoryName);

            } catch (Exception e) {
                LOGGER.warning("Failed to add rule '" + yamlRule.getName() +
                              "' to GenericRuleSet: " + e.getMessage());
                throw new RuntimeException("Failed to create rule '" + yamlRule.getName() +
                                         "' in category '" + categoryName + "'", e);
            }
        }

        // Apply custom properties to individual rules after creation
        List<Rule> createdRules = ruleSet.getRules();
        for (int i = 0; i < yamlRules.size() && i < createdRules.size(); i++) {
            YamlRule yamlRule = yamlRules.get(i);
            Rule createdRule = createdRules.get(i);

            if (yamlRule.getCustomProperties() != null && !yamlRule.getCustomProperties().isEmpty()) {
                // Create new metadata with custom properties
                dev.mars.apex.core.engine.model.metadata.RuleMetadata.Builder metadataBuilder =
                    dev.mars.apex.core.engine.model.metadata.RuleMetadata.builder(createdRule.getMetadata());

                for (Map.Entry<String, Object> entry : yamlRule.getCustomProperties().entrySet()) {
                    metadataBuilder.customProperty(entry.getKey(), entry.getValue());
                }

                // Create new rule with updated metadata
                dev.mars.apex.core.engine.model.metadata.RuleMetadata updatedMetadata = metadataBuilder.build();
                Rule updatedRule = createdRule.withMetadata(updatedMetadata);

                // Replace the rule in the list (this is a limitation of the current design)
                // For now, we'll need to rebuild the rule set with updated rules
                LOGGER.fine("Applied custom properties to rule '" + yamlRule.getName() + "'");
            }
        }

        return ruleSet;
    }

    /**
     * Create a Category from YAML category configuration.
     *
     * @param yamlCategory The YAML category configuration
     * @return A Category object
     */
    public Category createCategory(YamlCategory yamlCategory) {
        String name = yamlCategory.getName();
        int priority = yamlCategory.getPriority() != null ? yamlCategory.getPriority() : 100;

        LOGGER.fine("Creating category: " + name + " with priority: " + priority);

        return new Category(name, priority);
    }
    
    /**
     * Create a Rule with comprehensive metadata from YAML rule configuration.
     * This method uses the new generic architecture for enhanced validation and metadata support.
     * Rules inherit metadata from their category if not explicitly specified.
     *
     * @param yamlRule The YAML rule configuration
     * @return A Rule object with full metadata
     */
    public Rule createRuleWithMetadata(YamlRule yamlRule) {
        LOGGER.fine("Creating rule with metadata: " + yamlRule.getId() + " (" + yamlRule.getName() + ")");

        // Determine category
        String categoryName = yamlRule.getCategory() != null ? yamlRule.getCategory() : "default";

        // Look up category metadata from cache
        Category category = categoryCache.get(categoryName);
        YamlCategory yamlCategory = null;
        if (category != null) {
            // Find the corresponding YamlCategory for metadata inheritance
            yamlCategory = findYamlCategoryByName(categoryName);
            LOGGER.fine("Found category '" + categoryName + "' for rule '" + yamlRule.getId() +
                       "'. YamlCategory found: " + (yamlCategory != null) +
                       (yamlCategory != null ? ", businessOwner: " + yamlCategory.getBusinessOwner() : ""));
        } else {
            LOGGER.fine("No category found for '" + categoryName + "' in cache. Available categories: " +
                       categoryCache.keySet());
        }

        // Create a temporary GenericRuleSet to leverage the new architecture
        RuleSet.GenericRuleSet tempRuleSet = RuleSet.category(categoryName);

        // Apply enterprise metadata with category inheritance
        // Rule metadata takes precedence, but inherit from category if not specified
        String createdBy = yamlRule.getCreatedBy();
        if (createdBy == null && yamlCategory != null) {
            createdBy = yamlCategory.getCreatedBy();
        }
        if (createdBy != null) {
            tempRuleSet.withCreatedBy(createdBy);
        }

        String businessDomain = yamlRule.getBusinessDomain();
        if (businessDomain == null && yamlCategory != null) {
            businessDomain = yamlCategory.getBusinessDomain();
        }
        if (businessDomain != null) {
            tempRuleSet.withBusinessDomain(businessDomain);
        }

        String businessOwner = yamlRule.getBusinessOwner();
        if (businessOwner == null && yamlCategory != null) {
            businessOwner = yamlCategory.getBusinessOwner();
        }
        if (businessOwner != null) {
            tempRuleSet.withBusinessOwner(businessOwner);
        }

        if (yamlRule.getSourceSystem() != null) {
            tempRuleSet.withSourceSystem(yamlRule.getSourceSystem());
        }

        // Parse and apply dates
        if (yamlRule.getEffectiveDate() != null) {
            try {
                tempRuleSet.withEffectiveDate(Instant.parse(yamlRule.getEffectiveDate()));
            } catch (DateTimeParseException e) {
                LOGGER.warning("Invalid effective date format for rule " + yamlRule.getId() +
                              ": " + yamlRule.getEffectiveDate());
            }
        }
        if (yamlRule.getExpirationDate() != null) {
            try {
                tempRuleSet.withExpirationDate(Instant.parse(yamlRule.getExpirationDate()));
            } catch (DateTimeParseException e) {
                LOGGER.warning("Invalid expiration date format for rule " + yamlRule.getId() +
                              ": " + yamlRule.getExpirationDate());
            }
        }

        // Create the rule with validation
        String name = yamlRule.getName();
        String condition = yamlRule.getCondition();
        String message = yamlRule.getMessage() != null ? yamlRule.getMessage() :
                        "Rule " + name + " triggered";
        String description = yamlRule.getDescription() != null ? yamlRule.getDescription() :
                           message;

        // Create the rule directly with the specified ID instead of using GenericRuleSet
        // which generates its own unique ID
        String ruleId = yamlRule.getId() != null ? yamlRule.getId() :
                       generateFallbackRuleId(categoryName, name);

        // Create rule with all the metadata we've collected
        RuleMetadata.Builder initialMetadataBuilder = RuleMetadata.builder()
            .createdByUser(createdBy != null ? createdBy : "system");

        if (businessDomain != null) {
            initialMetadataBuilder.businessDomain(businessDomain);
        }
        if (businessOwner != null) {
            initialMetadataBuilder.businessOwner(businessOwner);
        }
        if (yamlRule.getSourceSystem() != null) {
            initialMetadataBuilder.sourceSystem(yamlRule.getSourceSystem());
        }

        // Handle effective date inheritance
        String effectiveDate = yamlRule.getEffectiveDate();
        if (effectiveDate == null && yamlCategory != null) {
            effectiveDate = yamlCategory.getEffectiveDate();
        }
        if (effectiveDate != null) {
            try {
                initialMetadataBuilder.effectiveDate(Instant.parse(effectiveDate));
            } catch (Exception e) {
                LOGGER.warning("Invalid effective date format for rule " + yamlRule.getId() + ": " + effectiveDate);
            }
        }

        // Handle expiration date inheritance
        String expirationDate = yamlRule.getExpirationDate();
        if (expirationDate == null && yamlCategory != null) {
            expirationDate = yamlCategory.getExpirationDate();
        }
        if (expirationDate != null) {
            try {
                initialMetadataBuilder.expirationDate(Instant.parse(expirationDate));
            } catch (Exception e) {
                LOGGER.warning("Invalid expiration date format for rule " + yamlRule.getId() + ": " + expirationDate);
            }
        }

        RuleMetadata metadata = initialMetadataBuilder.build();

        // Create category set
        Set<Category> categories = new HashSet<>();
        categories.add(new Category(categoryName, yamlRule.getPriority() != null ? yamlRule.getPriority() : 100));

        // Extract severity from YAML rule, default to ERROR if not specified
        String severity = yamlRule.getSeverity() != null ? yamlRule.getSeverity() : SeverityConstants.DEFAULT_SEVERITY;

        // Extract error/success codes and field mappings from YAML rule
        String successCode = yamlRule.getSuccessCode();
        String errorCode = yamlRule.getErrorCode();
        Object mapToField = yamlRule.getMapToField();

        Rule createdRule = new Rule(ruleId, categories, name, condition, message, description,
                                   yamlRule.getPriority() != null ? yamlRule.getPriority() : 100,
                                   severity, metadata, yamlRule.getDefaultValue(), successCode, errorCode, mapToField);

        // Apply custom properties if available
        if (yamlRule.getCustomProperties() != null && !yamlRule.getCustomProperties().isEmpty()) {
            // Create new metadata with custom properties
            dev.mars.apex.core.engine.model.metadata.RuleMetadata.Builder metadataBuilder =
                dev.mars.apex.core.engine.model.metadata.RuleMetadata.builder(createdRule.getMetadata());

            for (Map.Entry<String, Object> entry : yamlRule.getCustomProperties().entrySet()) {
                metadataBuilder.customProperty(entry.getKey(), entry.getValue());
            }

            // Create new rule with updated metadata
            dev.mars.apex.core.engine.model.metadata.RuleMetadata updatedMetadata = metadataBuilder.build();
            createdRule = createdRule.withMetadata(updatedMetadata);
        }

        return createdRule;
    }

    /**
     * Create a Rule from YAML rule configuration (legacy method for backward compatibility).
     *
     * @param yamlRule The YAML rule configuration
     * @return A Rule object
     * @deprecated Use createRuleWithMetadata for enhanced features
     */
    @Deprecated
    public Rule createRule(YamlRule yamlRule) {
        LOGGER.fine("Creating rule (legacy): " + yamlRule.getId() + " (" + yamlRule.getName() + ")");

        // For backward compatibility, try to use the new method first
        try {
            return createRuleWithMetadata(yamlRule);
        } catch (Exception e) {
            LOGGER.warning("Failed to create rule with metadata, falling back to legacy creation: " + e.getMessage());

            // Fallback to legacy creation
            String id = yamlRule.getId();
            String name = yamlRule.getName();
            String condition = yamlRule.getCondition();
            String message = yamlRule.getMessage() != null ? yamlRule.getMessage() : "Rule " + name + " triggered";
            String description = yamlRule.getDescription() != null ? yamlRule.getDescription() : "";
            int priority = yamlRule.getPriority() != null ? yamlRule.getPriority() : 100;

            // Extract severity from YAML rule, default to ERROR if not specified
            String severity = yamlRule.getSeverity() != null ? yamlRule.getSeverity() : SeverityConstants.DEFAULT_SEVERITY;

            // Determine category
            Category category = null;
            if (yamlRule.getCategory() != null) {
                category = getOrCreateCategory(yamlRule.getCategory(), priority);
            } else if (yamlRule.getCategories() != null && !yamlRule.getCategories().isEmpty()) {
                // Use the first category if multiple are specified
                category = getOrCreateCategory(yamlRule.getCategories().get(0), priority);
            } else {
                // Default category
                category = getOrCreateCategory("default", priority);
            }

            return new Rule(id, category, name, condition, message, description, priority, severity);
        }
    }
    
    /**
     * Create a RuleGroup from YAML rule group configuration.
     *
     * @param yamlGroup The YAML rule group configuration
     * @param config The rules engine configuration (to lookup existing rules)
     * @return A RuleGroup object
     */
    public RuleGroup createRuleGroup(YamlRuleGroup yamlGroup, RulesEngineConfiguration config) throws YamlConfigurationException {

        String id = yamlGroup.getId();
        String name = yamlGroup.getName();
        String description = yamlGroup.getDescription() != null ? yamlGroup.getDescription() : "";
        int priority = yamlGroup.getPriority() != null ? yamlGroup.getPriority() : 100;
        boolean stopOnFirstFailure = yamlGroup.getStopOnFirstFailure() != null ? yamlGroup.getStopOnFirstFailure() : false;
        boolean parallelExecution = yamlGroup.getParallelExecution() != null ? yamlGroup.getParallelExecution() : false;

        // Debug mode can be enabled via YAML configuration or system property for troubleshooting
        boolean debugMode = yamlGroup.getDebugMode() != null ? yamlGroup.getDebugMode() :
                           Boolean.parseBoolean(System.getProperty("apex.rulegroup.debug", "false"));

        LOGGER.fine("Creating rule group: " + id + " (" + name + ") with stopOnFirstFailure=" + stopOnFirstFailure +
                   ", parallelExecution=" + parallelExecution + ", debugMode=" + debugMode);

        // Determine category
        String categoryName = yamlGroup.getCategory() != null ? yamlGroup.getCategory() : "default";
        getOrCreateCategory(categoryName, priority); // Ensure category exists in cache

        // Determine operator from YAML configuration
        boolean isAndOperator = true; // Default to AND logic for rule groups
        if (yamlGroup.getOperator() != null) {
            String operator = yamlGroup.getOperator().toUpperCase();
            if ("OR".equals(operator)) {
                isAndOperator = false;
            } else if (!"AND".equals(operator)) {
                LOGGER.warning("Invalid operator '" + yamlGroup.getOperator() + "' for rule group '" + id + "'. Using AND as default.");
            }
        }

        RuleGroup group = new RuleGroup(id, categoryName, name, description, priority,
                                       isAndOperator, stopOnFirstFailure, parallelExecution, debugMode);

        // Add rules to the group
        LOGGER.info("About to add rules to group: " + yamlGroup.getId());
        addRulesToGroup(yamlGroup, group, config);
        LOGGER.info("Finished adding rules to group: " + yamlGroup.getId());

        return group;
    }
    
    /**
     * Add rules to a rule group based on YAML configuration.
     */
    private void addRulesToGroup(YamlRuleGroup yamlGroup, RuleGroup group, RulesEngineConfiguration config) throws YamlConfigurationException {

        // Add rules by ID
        if (yamlGroup.getRuleIds() != null) {
            int sequence = 1;
            for (String ruleId : yamlGroup.getRuleIds()) {
                Rule rule = config.getRuleByIdWithLogging(ruleId);
                if (rule != null) {
                    group.addRule(rule, sequence++);
                    LOGGER.fine("Added rule " + ruleId + " to group " + group.getId());
                }
            }
        }
        
        // Add rules by reference (with more detailed configuration)
        if (yamlGroup.getRuleReferences() != null) {
            LOGGER.info("Processing " + yamlGroup.getRuleReferences().size() + " rule references for group: " + yamlGroup.getId());
            for (YamlRuleGroup.RuleReference ref : yamlGroup.getRuleReferences()) {
                LOGGER.info("Processing rule reference: " + ref.getRuleId() + ", enabled: " + ref.getEnabled() + ", override-priority: " + ref.getOverridePriority());
                if (ref.getEnabled() == null || ref.getEnabled()) {
                    Rule originalRule = config.getRuleById(ref.getRuleId());
                    if (originalRule != null) {
                        int sequence = ref.getSequence() != null ? ref.getSequence() : 1;

                        // Handle priority override
                        Rule ruleToAdd = originalRule;
                        if (ref.getOverridePriority() != null) {
                            validatePriorityOverride(ref.getOverridePriority(), ref.getRuleId());
                            ruleToAdd = createRuleWithOverriddenPriority(originalRule, ref.getOverridePriority(), yamlGroup.getId());
                            LOGGER.fine("Applied priority override " + ref.getOverridePriority() + " to rule " + ref.getRuleId() + " in group " + yamlGroup.getId());
                        }

                        group.addRule(ruleToAdd, sequence);
                        LOGGER.fine("Added rule " + ref.getRuleId() + " to group " + group.getId() + " with sequence " + sequence);
                    } else {
                        LOGGER.warning("Rule not found for ID: " + ref.getRuleId() + " in group: " + group.getId());
                    }
                } else {
                    LOGGER.info("Skipping disabled rule: " + ref.getRuleId());
                }
            }
        }

        // Note: Rule group references are processed in a separate phase after all rule groups are created
        // This is handled by processRuleGroupReferences() method
    }

    /**
     * Process rule group references in a second phase after all rule groups have been created.
     * This allows rule groups to reference other rule groups that might be defined later in the configuration.
     */
    private void processRuleGroupReferences(YamlRuleConfiguration yamlConfig, RulesEngineConfiguration config) throws YamlConfigurationException {
        if (yamlConfig.getRuleGroups() == null) {
            return;
        }

        for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
            if ((yamlGroup.getEnabled() == null || yamlGroup.getEnabled()) && yamlGroup.getRuleGroupReferences() != null) {
                LOGGER.info("Processing rule group references for group: " + yamlGroup.getId());

                RuleGroup targetGroup = config.getRuleGroupById(yamlGroup.getId());
                if (targetGroup == null) {
                    LOGGER.warning("Target rule group not found: " + yamlGroup.getId());
                    continue;
                }

                addRuleGroupReferencesToGroup(yamlGroup, targetGroup, config);
            }
        }
    }

    /**
     * Add rules from referenced rule groups to the target rule group.
     */
    private void addRuleGroupReferencesToGroup(YamlRuleGroup yamlGroup, RuleGroup targetGroup, RulesEngineConfiguration config) throws YamlConfigurationException {
        if (yamlGroup.getRuleGroupReferences() == null) {
            return;
        }

        LOGGER.info("Processing " + yamlGroup.getRuleGroupReferences().size() + " rule group references for group: " + yamlGroup.getId());

        // Calculate starting sequence number (after existing rules)
        int nextSequence = targetGroup.getRules().size() + 1;

        for (String referencedGroupId : yamlGroup.getRuleGroupReferences()) {
            LOGGER.info("Processing rule group reference: " + referencedGroupId);

            RuleGroup referencedGroup = config.getRuleGroupById(referencedGroupId);
            if (referencedGroup != null) {
                // Add all rules from the referenced group to the target group
                for (Rule rule : referencedGroup.getRules()) {
                    targetGroup.addRule(rule, nextSequence++);
                    LOGGER.fine("Added rule " + rule.getId() + " from group " + referencedGroupId + " to group " + targetGroup.getId());
                }
                LOGGER.info("Successfully added " + referencedGroup.getRules().size() + " rules from group " + referencedGroupId + " to group " + targetGroup.getId());
            } else {
                String errorMsg = "Referenced rule group not found: " + referencedGroupId + " in group: " + yamlGroup.getId();
                LOGGER.severe(errorMsg);
                throw new YamlConfigurationException(errorMsg);
            }
        }
    }
    
    /**
     * Get an existing category or create a new one.
     * 
     * @param categoryName The category name
     * @param defaultPriority The default priority if creating a new category
     * @return The Category object
     */
    private Category getOrCreateCategory(String categoryName, int defaultPriority) {
        return categoryCache.computeIfAbsent(categoryName, name -> {
            LOGGER.fine("Creating new category: " + name + " with priority: " + defaultPriority);
            return new Category(name, defaultPriority);
        });
    }
    
    /**
     * Create a list of rules from YAML configuration.
     *
     * This is a public API method intended for users who need to create rules
     * independently without a full RulesEngineConfiguration.
     *
     * @param yamlConfig The YAML configuration
     * @return List of Rule objects
     */
    @SuppressWarnings("unused") // Public API method for independent rule creation
    public List<Rule> createRules(YamlRuleConfiguration yamlConfig) {
        List<Rule> rules = new ArrayList<>();
        
        if (yamlConfig.getRules() != null) {
            for (YamlRule yamlRule : yamlConfig.getRules()) {
                if (yamlRule.getEnabled() == null || yamlRule.getEnabled()) {
                    rules.add(createRule(yamlRule));
                }
            }
        }
        
        return rules;
    }
    
    /**
     * Create a list of rule groups from YAML configuration.
     *
     * This is a public API method intended for users who need to create rule groups
     * independently without a full RulesEngineConfiguration.
     *
     * @param yamlConfig The YAML configuration
     * @param config The rules engine configuration (to lookup existing rules)
     * @return List of RuleGroup objects
     */
    @SuppressWarnings("unused") // Public API method for independent rule group creation
    public List<RuleGroup> createRuleGroups(YamlRuleConfiguration yamlConfig, RulesEngineConfiguration config) throws YamlConfigurationException {
        List<RuleGroup> groups = new ArrayList<>();
        
        if (yamlConfig.getRuleGroups() != null) {
            for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
                if (yamlGroup.getEnabled() == null || yamlGroup.getEnabled()) {
                    groups.add(createRuleGroup(yamlGroup, config));
                }
            }
        }
        
        return groups;
    }
    
    /**
     * Create a list of categories from YAML configuration.
     *
     * This is a public API method intended for users who need to create categories
     * independently without a full RulesEngineConfiguration.
     *
     * @param yamlConfig The YAML configuration
     * @return List of Category objects
     */
    @SuppressWarnings("unused") // Public API method for independent category creation
    public List<Category> createCategories(YamlRuleConfiguration yamlConfig) {
        List<Category> categories = new ArrayList<>();
        
        if (yamlConfig.getCategories() != null) {
            for (YamlCategory yamlCategory : yamlConfig.getCategories()) {
                if (yamlCategory.getEnabled() == null || yamlCategory.getEnabled()) {
                    categories.add(createCategory(yamlCategory));
                }
            }
        }
        
        return categories;
    }
    
    /**
     * Find a YamlCategory by name from the cache.
     *
     * @param categoryName The name of the category to find
     * @return The YamlCategory or null if not found
     */
    private YamlCategory findYamlCategoryByName(String categoryName) {
        return yamlCategoryCache.get(categoryName);
    }

    /**
     * Generate a fallback rule ID when none is specified in YAML.
     *
     * @param categoryName The category name
     * @param ruleName The rule name
     * @return A generated rule ID
     */
    private String generateFallbackRuleId(String categoryName, String ruleName) {
        String sanitizedName = ruleName.toLowerCase()
            .replaceAll("[^a-z0-9\\-_]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        String timestamp = String.valueOf(System.currentTimeMillis() % 100000);
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s-%s-%s-%s",
            categoryName.toLowerCase(),
            sanitizedName,
            timestamp,
            uuid);
    }

    /**
     * Clear the category cache.
     */
    public void clearCache() {
        categoryCache.clear();
        yamlCategoryCache.clear();
    }

    /**
     * Validate priority override value.
     *
     * @param priority The priority value to validate
     * @param ruleId The rule ID for error reporting
     * @throws YamlConfigurationException if priority is invalid
     */
    private void validatePriorityOverride(Integer priority, String ruleId) throws YamlConfigurationException {
        if (priority == null) return;

        if (priority < 1) {
            throw new YamlConfigurationException(
                "override-priority must be >= 1 for rule: " + ruleId + ", got: " + priority);
        }

        if (priority > 1000) {
            LOGGER.warning("Very high priority override (" + priority + ") for rule: " + ruleId +
                          ". Consider using priorities between 1-100.");
        }
    }

    /**
     * Create a copy of a rule with overridden priority for use in a specific rule group.
     * This preserves the original rule while allowing group-specific priority behavior.
     *
     * @param originalRule The original rule to copy
     * @param newPriority The new priority to apply
     * @param groupId The rule group ID for unique identification
     * @return A new rule instance with the overridden priority
     */
    private Rule createRuleWithOverriddenPriority(Rule originalRule, int newPriority, String groupId) {
        // Create new categories with overridden priority
        Set<Category> newCategories = originalRule.getCategories().stream()
            .map(cat -> new Category(cat.getName(), newPriority))
            .collect(Collectors.toSet());

        // Create unique ID for this group-specific rule instance
        String newRuleId = originalRule.getId() + "_group_" + groupId + "_priority_" + newPriority;

        // Create new rule with same properties but different priority and ID
        Rule newRule = new Rule(
            newRuleId,
            newCategories,
            originalRule.getName(),
            originalRule.getCondition(),
            originalRule.getMessage(),
            originalRule.getDescription(),
            newPriority,
            originalRule.getMetadata()
        );

        return newRule;
    }
}
