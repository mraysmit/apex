package dev.mars.apex.core.config;

import dev.mars.apex.core.api.RuleSet;
import dev.mars.apex.core.constants.ErrorHandlingConstants;
import dev.mars.apex.core.constants.SeverityConstants;
import dev.mars.apex.core.engine.config.RulesEngineConfiguration;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import dev.mars.apex.core.engine.model.Rule;
import dev.mars.apex.core.engine.model.RuleGroup;
import dev.mars.apex.core.engine.model.metadata.RuleMetadata;
import dev.mars.apex.core.service.enrichment.EnrichmentGroupFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    
    private static final Logger logger = LoggerFactory.getLogger(YamlRuleFactory.class);

    // Thread-safe caches for concurrent access
    private final Map<String, Category> categoryCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Cache for YAML categories to enable metadata inheritance (thread-safe)
    private final Map<String, YamlCategory> yamlCategoryCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Create a RulesEngineConfiguration from YAML configuration using the new generic architecture.
     * This method leverages the GenericRuleSet for enhanced validation and metadata support.
     *
     * @param yamlConfig The YAML configuration
     * @return A configured RulesEngineConfiguration
     */
    public RulesEngineConfiguration createRulesEngineConfiguration(YamlRuleConfiguration yamlConfig) throws YamlConfigurationException {
        logger.info("Creating RulesEngineConfiguration from YAML configuration");

        RulesEngineConfiguration config = new RulesEngineConfiguration();

        // Process categories first to populate cache
        if (yamlConfig.getCategories() != null) {
            for (YamlCategory yamlCategory : yamlConfig.getCategories()) {
                if (yamlCategory.getEnabled() == null || yamlCategory.getEnabled()) {
                    Category category = createCategory(yamlCategory);
                    categoryCache.put(category.getName(), category);
                    // Also cache the YAML category for metadata inheritance
                    yamlCategoryCache.put(yamlCategory.getName(), yamlCategory);
                    logger.debug("Cached category '" + yamlCategory.getName() +
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
                            logger.warn("Failed to create rule '" + yamlRule.getId() +
                                          "': " + ruleException.getMessage());
                        }
                    }

                    logger.info("Created " + categoryRules.size() + " rules for category '" + categoryName +
                               "' using enhanced metadata support");

                } catch (Exception e) {
                    logger.warn("Failed to create rules for category '" + categoryName +
                                  "': " + e.getMessage());
                }
            }
        }

        // Process rule groups using two-phase approach (like enrichment groups)
        if (yamlConfig.getRuleGroups() != null) {
            logger.info("Processing " + yamlConfig.getRuleGroups().size() + " rule groups using two-phase approach");

            // Create all rule groups and register them (no cross-references yet)
            Map<String, RuleGroup> ruleGroupsById = new HashMap<>();
            for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
                logger.info("Phase 1 - Creating rule group: " + yamlGroup.getId() + ", enabled: " + yamlGroup.getEnabled());
                if (yamlGroup.getEnabled() == null || yamlGroup.getEnabled()) {
                    try {
                        logger.info("Creating rule group: " + yamlGroup.getId());
                        RuleGroup group = createRuleGroupWithoutReferences(yamlGroup, config);
                        config.registerRuleGroup(group);
                        ruleGroupsById.put(group.getId(), group);
                        logger.info("Successfully registered rule group: " + yamlGroup.getId());
                    } catch (YamlConfigurationException e) {
                        // Re-throw configuration exceptions to fail fast
                        logger.error("YamlConfigurationException for rule group " + yamlGroup.getId() + ": " + e.getMessage());
                        logger.debug("Full stack trace for rule group configuration exception:", e);
                        throw e;
                    } catch (Exception e) {
                        logger.warn("Failed to create rule group '" + yamlGroup.getId() +
                                      "': " + e.getMessage());
                    }
                } else {
                    logger.info("Skipping disabled rule group: " + yamlGroup.getId());
                }
            }

            //Process rule group references now that all rule groups are created and registered
            logger.info("Phase 2 - Processing rule group references with global registry");
            processRuleGroupReferencesWithGlobalRegistry(yamlConfig, config, ruleGroupsById);
        }

        // Create and register enrichment groups
        if (yamlConfig.getEnrichmentGroups() != null && !yamlConfig.getEnrichmentGroups().isEmpty()) {
            logger.info("Creating enrichment groups from YAML configuration");
            List<EnrichmentGroup> enrichmentGroups = EnrichmentGroupFactory.buildEnrichmentGroups(yamlConfig);

            for (EnrichmentGroup group : enrichmentGroups) {
                config.registerEnrichmentGroup(group);
                logger.info("Registered enrichment group: " + group.getId());
            }
        }

        logger.info("Successfully created RulesEngineConfiguration with " +
                   config.getAllRules().size() + " rules, " +
                   config.getAllRuleGroups().size() + " rule groups, and " +
                   config.getAllEnrichmentGroups().size() + " enrichment groups");

        return config;
    }
    
    /**
     * Create a GenericRuleSet from YAML configuration for a specific category.

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
        logger.debug("Creating GenericRuleSet for category: " + categoryName + " with " + yamlRules.size() + " rules");

        // Validate category name using the same validation as the generic API
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be null or empty");
        }

        RuleSet.GenericRuleSet ruleSet = RuleSet.category(categoryName);

        // Apply common metadata from the first rule or category configuration
        if (!yamlRules.isEmpty()) {
            YamlRule firstRule = yamlRules.get(0);

            // Apply enterprise metadata if available
            if (firstRule.getCreatedBy() != null && !firstRule.getCreatedBy().trim().isEmpty()) {
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
                    ruleSet.withEffectiveDate(parseDate(firstRule.getEffectiveDate()));
                } catch (DateTimeParseException e) {
                    logger.error("Invalid effective date format for rule " + firstRule.getId() +
                                  ": " + firstRule.getEffectiveDate());
                }
            }
            if (firstRule.getExpirationDate() != null) {
                try {
                    ruleSet.withExpirationDate(parseDate(firstRule.getExpirationDate()));
                } catch (DateTimeParseException e) {
                    logger.error("Invalid expiration date format for rule " + firstRule.getId() +
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

                logger.debug("Added rule '" + name + "' with severity '" + severity + "' to GenericRuleSet for category: " + categoryName);

            } catch (Exception e) {
                logger.warn("Failed to add rule '" + yamlRule.getName() +
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
                logger.debug("Applied custom properties to rule '" + yamlRule.getName() + "'");
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

        logger.debug("Creating category: " + name + " with priority: " + priority);

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
        logger.debug("Creating rule with metadata: " + yamlRule.getId() + " (" + yamlRule.getName() + ")");

        // Determine category
        String categoryName = yamlRule.getCategory() != null ? yamlRule.getCategory() : "default";

        // Look up category metadata from cache
        Category category = categoryCache.get(categoryName);
        YamlCategory yamlCategory = null;
        if (category != null) {
            // Find the corresponding YamlCategory for metadata inheritance
            yamlCategory = findYamlCategoryByName(categoryName);
            logger.debug("Found category '" + categoryName + "' for rule '" + yamlRule.getId() +
                       "'. YamlCategory found: " + (yamlCategory != null) +
                       (yamlCategory != null ? ", businessOwner: " + yamlCategory.getBusinessOwner() : ""));
        } else {
            logger.debug("No category found for '" + categoryName + "' in cache. Available categories: " +
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
        if (createdBy != null && !createdBy.trim().isEmpty()) {
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
                tempRuleSet.withEffectiveDate(parseDate(yamlRule.getEffectiveDate()));
            } catch (DateTimeParseException e) {
                logger.error("Invalid effective date format for rule " + yamlRule.getId() +
                              ": " + yamlRule.getEffectiveDate());
            }
        }
        if (yamlRule.getExpirationDate() != null) {
            try {
                tempRuleSet.withExpirationDate(parseDate(yamlRule.getExpirationDate()));
            } catch (DateTimeParseException e) {
                logger.error("Invalid expiration date format for rule " + yamlRule.getId() +
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
                initialMetadataBuilder.effectiveDate(parseDate(effectiveDate));
            } catch (Exception e) {
                logger.error("Invalid effective date format for rule " + yamlRule.getId() + ": " + effectiveDate);
            }
        }

        // Handle expiration date inheritance
        String expirationDate = yamlRule.getExpirationDate();
        if (expirationDate == null && yamlCategory != null) {
            expirationDate = yamlCategory.getExpirationDate();
        }
        if (expirationDate != null) {
            try {
                initialMetadataBuilder.expirationDate(parseDate(expirationDate));
            } catch (Exception e) {
                logger.error("Invalid expiration date format for rule " + yamlRule.getId() + ": " + expirationDate);
            }
        }

        RuleMetadata metadata = initialMetadataBuilder.build();

        // Create category set
        Set<Category> categories = new HashSet<>();
        categories.add(new Category(categoryName, yamlRule.getPriority() != null ? yamlRule.getPriority() : 100));

        // Extract severity from YAML rule, default to ERROR if not specified
        String severity = yamlRule.getSeverity() != null ? yamlRule.getSeverity() : SeverityConstants.DEFAULT_SEVERITY;

        // Extract error/success codes, field mappings, and result field from YAML rule
        String successCode = yamlRule.getSuccessCode();
        String errorCode = yamlRule.getErrorCode();
        List<String> mapToField = yamlRule.getMapToField();
        String resultField = yamlRule.getResultField();
        String noMatchMessage = yamlRule.getNoMatchMessage();

        boolean enabled = yamlRule.getEnabled() == null || yamlRule.getEnabled();

        Rule createdRule = new Rule(ruleId, categories, name, condition, message, description,
                                   yamlRule.getPriority() != null ? yamlRule.getPriority() : 100,
                                   severity, metadata, yamlRule.getDefaultValue(), successCode, errorCode, mapToField, resultField, noMatchMessage, enabled);

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
     * Create a RuleGroup from YAML rule group configuration without processing rule-group-references.
     * This is used in Phase 1 of the two-phase rule group creation process.
     *
     * @param yamlGroup The YAML rule group configuration
     * @param config The rules engine configuration (to lookup existing rules)
     * @return A RuleGroup object
     */
    public RuleGroup createRuleGroupWithoutReferences(YamlRuleGroup yamlGroup, RulesEngineConfiguration config) throws YamlConfigurationException {
        String id = yamlGroup.getId();
        String name = yamlGroup.getName() != null ? yamlGroup.getName() : id;
        String description = yamlGroup.getDescription();
        int priority = yamlGroup.getPriority() != null ? yamlGroup.getPriority() : 100;
        boolean stopOnFirstFailure = yamlGroup.getStopOnFirstFailure() != null ? yamlGroup.getStopOnFirstFailure() : false;
        boolean parallelExecution = yamlGroup.getParallelExecution() != null ? yamlGroup.getParallelExecution() : false;
        boolean debugMode = yamlGroup.getDebugMode() != null ? yamlGroup.getDebugMode() : Boolean.parseBoolean(System.getProperty("apex.rulegroup.debug", "false"));

        logger.debug("Creating rule group: " + id + " (" + name + ") with stopOnFirstFailure=" + stopOnFirstFailure +
                   ", parallelExecution=" + parallelExecution + ", debugMode=" + debugMode);

        // Determine category
        String categoryName = yamlGroup.getCategory() != null ? yamlGroup.getCategory() : "default";
        getOrCreateCategory(categoryName, priority); // Ensure category exists in cache

        // Look up category metadata from cache for inheritance
        YamlCategory yamlCategory = findYamlCategoryByName(categoryName);
        logger.debug("Found category '" + categoryName + "' for rule group '" + yamlGroup.getId() +
                   "'. YamlCategory found: " + (yamlCategory != null) +
                   (yamlCategory != null ? ", businessOwner: " + yamlCategory.getBusinessOwner() : ""));

        // Determine operator from YAML configuration
        boolean isAndOperator = true; // Default to AND logic for rule groups
        if (yamlGroup.getOperator() != null) {
            String operator = yamlGroup.getOperator().toUpperCase();
            if ("OR".equals(operator)) {
                isAndOperator = false;
            } else if (!"AND".equals(operator)) {
                logger.warn("Invalid operator '" + yamlGroup.getOperator() + "' for rule group '" + id + "'. Using AND as default.");
            }
        }

        // Parse error-handling strategy from YAML configuration
        String errorHandling = yamlGroup.getErrorHandling() != null ? yamlGroup.getErrorHandling() : ErrorHandlingConstants.DEFAULT_STRATEGY;
        if (!ErrorHandlingConstants.isValidStrategy(errorHandling)) {
            logger.warn("Invalid error-handling '" + errorHandling + "' for rule group '" + id + "'. Using " + ErrorHandlingConstants.DEFAULT_STRATEGY + " as default.");
            errorHandling = ErrorHandlingConstants.DEFAULT_STRATEGY;
        }

        RuleGroup group = new RuleGroup(id, categoryName, name, description, priority,
                                       isAndOperator, stopOnFirstFailure, parallelExecution, debugMode, errorHandling);

        // Apply enterprise metadata with category inheritance
        // Rule group metadata takes precedence, but inherit from category if not specified
        String createdBy = yamlGroup.getCreatedBy();
        if (createdBy == null && yamlCategory != null) {
            createdBy = yamlCategory.getCreatedBy();
        }
        if (createdBy != null) {
            group.setCreatedBy(createdBy);
        }

        String businessDomain = yamlGroup.getBusinessDomain();
        if (businessDomain == null && yamlCategory != null) {
            businessDomain = yamlCategory.getBusinessDomain();
        }
        if (businessDomain != null) {
            group.setBusinessDomain(businessDomain);
        }

        String businessOwner = yamlGroup.getBusinessOwner();
        if (businessOwner == null && yamlCategory != null) {
            businessOwner = yamlCategory.getBusinessOwner();
        }
        if (businessOwner != null) {
            group.setBusinessOwner(businessOwner);
        }

        // Handle effective date inheritance
        String effectiveDate = yamlGroup.getEffectiveDate();
        if (effectiveDate == null && yamlCategory != null) {
            effectiveDate = yamlCategory.getEffectiveDate();
        }
        if (effectiveDate != null) {
            group.setEffectiveDate(effectiveDate);
        }

        // Handle expiration date inheritance
        String expirationDate = yamlGroup.getExpirationDate();
        if (expirationDate == null && yamlCategory != null) {
            expirationDate = yamlCategory.getExpirationDate();
        }
        if (expirationDate != null) {
            group.setExpirationDate(expirationDate);
        }

        logger.debug("Applied metadata inheritance to rule group '" + id + "': " +
                   "createdBy=" + group.getCreatedBy() + ", " +
                   "businessDomain=" + group.getBusinessDomain() + ", " +
                   "businessOwner=" + group.getBusinessOwner());

        // Add rules to the group (but NOT rule-group-references - that's Phase 2)
        logger.info("About to add rules to group: " + yamlGroup.getId());
        addRulesToGroupWithoutGroupReferences(yamlGroup, group, config);
        logger.info("Finished adding rules to group: " + yamlGroup.getId());

        return group;
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

        logger.debug("Creating rule group: " + id + " (" + name + ") with stopOnFirstFailure=" + stopOnFirstFailure +
                   ", parallelExecution=" + parallelExecution + ", debugMode=" + debugMode);

        // Determine category
        String categoryName = yamlGroup.getCategory() != null ? yamlGroup.getCategory() : "default";
        getOrCreateCategory(categoryName, priority); // Ensure category exists in cache

        // Look up category metadata from cache for inheritance
        YamlCategory yamlCategory = findYamlCategoryByName(categoryName);
        logger.debug("Found category '" + categoryName + "' for rule group '" + yamlGroup.getId() +
                   "'. YamlCategory found: " + (yamlCategory != null) +
                   (yamlCategory != null ? ", businessOwner: " + yamlCategory.getBusinessOwner() : ""));

        // Determine operator from YAML configuration
        boolean isAndOperator = true; // Default to AND logic for rule groups
        if (yamlGroup.getOperator() != null) {
            String operator = yamlGroup.getOperator().toUpperCase();
            if ("OR".equals(operator)) {
                isAndOperator = false;
            } else if (!"AND".equals(operator)) {
                logger.warn("Invalid operator '" + yamlGroup.getOperator() + "' for rule group '" + id + "'. Using AND as default.");
            }
        }

        // Parse error-handling strategy from YAML configuration
        String errorHandling = yamlGroup.getErrorHandling() != null ? yamlGroup.getErrorHandling() : ErrorHandlingConstants.DEFAULT_STRATEGY;
        if (!ErrorHandlingConstants.isValidStrategy(errorHandling)) {
            logger.warn("Invalid error-handling '" + errorHandling + "' for rule group '" + id + "'. Using " + ErrorHandlingConstants.DEFAULT_STRATEGY + " as default.");
            errorHandling = ErrorHandlingConstants.DEFAULT_STRATEGY;
        }

        RuleGroup group = new RuleGroup(id, categoryName, name, description, priority,
                                       isAndOperator, stopOnFirstFailure, parallelExecution, debugMode, errorHandling);

        // Apply enterprise metadata with category inheritance
        // Rule group metadata takes precedence, but inherit from category if not specified
        String createdBy = yamlGroup.getCreatedBy();
        if (createdBy == null && yamlCategory != null) {
            createdBy = yamlCategory.getCreatedBy();
        }
        if (createdBy != null) {
            group.setCreatedBy(createdBy);
        }

        String businessDomain = yamlGroup.getBusinessDomain();
        if (businessDomain == null && yamlCategory != null) {
            businessDomain = yamlCategory.getBusinessDomain();
        }
        if (businessDomain != null) {
            group.setBusinessDomain(businessDomain);
        }

        String businessOwner = yamlGroup.getBusinessOwner();
        if (businessOwner == null && yamlCategory != null) {
            businessOwner = yamlCategory.getBusinessOwner();
        }
        if (businessOwner != null) {
            group.setBusinessOwner(businessOwner);
        }

        String sourceSystem = yamlGroup.getSourceSystem();
        if (sourceSystem != null) {
            group.setSourceSystem(sourceSystem);
        }

        String effectiveDate = yamlGroup.getEffectiveDate();
        if (effectiveDate == null && yamlCategory != null) {
            effectiveDate = yamlCategory.getEffectiveDate();
        }
        if (effectiveDate != null) {
            group.setEffectiveDate(effectiveDate);
        }

        String expirationDate = yamlGroup.getExpirationDate();
        if (expirationDate == null && yamlCategory != null) {
            expirationDate = yamlCategory.getExpirationDate();
        }
        if (expirationDate != null) {
            group.setExpirationDate(expirationDate);
        }

        logger.debug("Applied metadata inheritance to rule group '" + id + "': " +
                   "createdBy=" + group.getCreatedBy() + ", " +
                   "businessDomain=" + group.getBusinessDomain() + ", " +
                   "businessOwner=" + group.getBusinessOwner());

        // Add rules to the group
        logger.info("About to add rules to group: " + yamlGroup.getId());
        addRulesToGroup(yamlGroup, group, config);
        logger.info("Finished adding rules to group: " + yamlGroup.getId());

        return group;
    }

    /**
     * Add rules to a rule group based on the YAML configuration, excluding rule-group-references.
     * This is used in Phase 1 of the two-phase rule group creation process.
     */
    private void addRulesToGroupWithoutGroupReferences(YamlRuleGroup yamlGroup, RuleGroup group, RulesEngineConfiguration config) throws YamlConfigurationException {
        // Add rules by ID (simple list)
        if (yamlGroup.getRuleIds() != null) {
            logger.info("Processing " + yamlGroup.getRuleIds().size() + " rule IDs for group: " + yamlGroup.getId());
            int sequence = 1;
            for (String ruleId : yamlGroup.getRuleIds()) {
                logger.info("Processing rule ID: " + ruleId);
                Rule rule = config.getRuleById(ruleId);
                if (rule != null) {
                    group.addRule(rule, sequence++);
                    logger.debug("Added rule " + ruleId + " to group " + group.getId() + " with sequence " + sequence);
                } else {
                    logger.warn("Rule not found for ID: " + ruleId + " in group: " + group.getId());
                }
            }
        }

        // Add rules by reference (with more detailed configuration)
        if (yamlGroup.getRuleReferences() != null) {
            logger.info("Processing " + yamlGroup.getRuleReferences().size() + " rule references for group: " + yamlGroup.getId());
            for (YamlRuleGroup.RuleReference ref : yamlGroup.getRuleReferences()) {
                logger.info("Processing rule reference: " + ref.getRuleId() + ", enabled: " + ref.getEnabled() + ", override-priority: " + ref.getOverridePriority());
                if (ref.getEnabled() == null || ref.getEnabled()) {
                    Rule originalRule = config.getRuleById(ref.getRuleId());
                    if (originalRule != null) {
                        int sequence = ref.getSequence() != null ? ref.getSequence() : 1;

                        // Handle priority override
                        Rule ruleToAdd = originalRule;
                        if (ref.getOverridePriority() != null) {
                            validatePriorityOverride(ref.getOverridePriority(), ref.getRuleId());
                            ruleToAdd = createRuleWithOverriddenPriority(originalRule, ref.getOverridePriority(), yamlGroup.getId());
                            logger.debug("Applied priority override " + ref.getOverridePriority() + " to rule " + ref.getRuleId() + " in group " + yamlGroup.getId());
                        }

                        group.addRule(ruleToAdd, sequence);
                        logger.debug("Added rule " + ref.getRuleId() + " to group " + group.getId() + " with sequence " + sequence);
                    } else {
                        logger.warn("Rule not found for ID: " + ref.getRuleId() + " in group: " + group.getId());
                    }
                } else {
                    logger.info("Skipping disabled rule: " + ref.getRuleId());
                }
            }
        }

        // Note: Rule group references are NOT processed here - that's Phase 2
        logger.info("Phase 1 complete for group: " + yamlGroup.getId() + " (rule-group-references will be processed in Phase 2)");
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
                    logger.debug("Added rule " + ruleId + " to group " + group.getId());
                }
            }
        }
        
        // Add rules by reference (with more detailed configuration)
        if (yamlGroup.getRuleReferences() != null) {
            logger.info("Processing " + yamlGroup.getRuleReferences().size() + " rule references for group: " + yamlGroup.getId());
            for (YamlRuleGroup.RuleReference ref : yamlGroup.getRuleReferences()) {
                logger.info("Processing rule reference: " + ref.getRuleId() + ", enabled: " + ref.getEnabled() + ", override-priority: " + ref.getOverridePriority());
                if (ref.getEnabled() == null || ref.getEnabled()) {
                    Rule originalRule = config.getRuleById(ref.getRuleId());
                    if (originalRule != null) {
                        int sequence = ref.getSequence() != null ? ref.getSequence() : 1;

                        // Handle priority override
                        Rule ruleToAdd = originalRule;
                        if (ref.getOverridePriority() != null) {
                            validatePriorityOverride(ref.getOverridePriority(), ref.getRuleId());
                            ruleToAdd = createRuleWithOverriddenPriority(originalRule, ref.getOverridePriority(), yamlGroup.getId());
                            logger.debug("Applied priority override " + ref.getOverridePriority() + " to rule " + ref.getRuleId() + " in group " + yamlGroup.getId());
                        }

                        group.addRule(ruleToAdd, sequence);
                        logger.debug("Added rule " + ref.getRuleId() + " to group " + group.getId() + " with sequence " + sequence);
                    } else {
                        logger.warn("Rule not found for ID: " + ref.getRuleId() + " in group: " + group.getId());
                    }
                } else {
                    logger.info("Skipping disabled rule: " + ref.getRuleId());
                }
            }
        }

        // Note: Rule group references are processed in a separate phase after all rule groups are created
        // This is handled by processRuleGroupReferences() method
    }

    /**
     * Process rule group references using a global registry (like enrichment groups).
     * This enables cross-file rule-group references by using a combined registry of all rule groups.
     */
    private void processRuleGroupReferencesWithGlobalRegistry(YamlRuleConfiguration yamlConfig, RulesEngineConfiguration config, Map<String, RuleGroup> globalRuleGroupsById) throws YamlConfigurationException {
        if (yamlConfig.getRuleGroups() == null) {
            return;
        }

        logger.info("Processing rule group references with global registry containing " + globalRuleGroupsById.size() + " groups");

        // Build map of YAML groups for lookups
        Map<String, YamlRuleGroup> yamlGroupsById = new HashMap<>();
        for (YamlRuleGroup yg : yamlConfig.getRuleGroups()) {
            if (yg.getId() != null) {
                yamlGroupsById.put(yg.getId(), yg);
            }
        }

        Set<String> resolvedGroups = new HashSet<>();
        Set<String> resolvingGroups = new HashSet<>();

        for (String groupId : globalRuleGroupsById.keySet()) {
            resolveRuleGroupReferences(groupId, globalRuleGroupsById, yamlGroupsById, resolvedGroups, resolvingGroups);
        }
    }

    private void resolveRuleGroupReferences(String groupId,
                                          Map<String, RuleGroup> globalRuleGroupsById,
                                          Map<String, YamlRuleGroup> yamlGroupsById,
                                          Set<String> resolvedGroups,
                                          Set<String> resolvingGroups) throws YamlConfigurationException {
        if (resolvedGroups.contains(groupId)) {
            return;
        }

        if (resolvingGroups.contains(groupId)) {
            throw new YamlConfigurationException("Circular dependency detected in rule groups: " + resolvingGroups + " -> " + groupId);
        }

        resolvingGroups.add(groupId);

        YamlRuleGroup yamlGroup = yamlGroupsById.get(groupId);
        if (yamlGroup != null && yamlGroup.getRuleGroupReferences() != null) {
            RuleGroup targetGroup = globalRuleGroupsById.get(groupId);
            
            // Calculate starting sequence number (after existing rules)
            int nextSequence = targetGroup.getRules().size() + 1;

            for (String refId : yamlGroup.getRuleGroupReferences()) {
                // Recursively resolve the referenced group first
                resolveRuleGroupReferences(refId, globalRuleGroupsById, yamlGroupsById, resolvedGroups, resolvingGroups);

                RuleGroup referencedGroup = globalRuleGroupsById.get(refId);
                if (referencedGroup != null) {
                    // Add all rules from the referenced group to the target group
                    for (Rule rule : referencedGroup.getRules()) {
                        targetGroup.addRule(rule, nextSequence++);
                        logger.debug("Added rule " + rule.getId() + " from group " + refId + " to group " + targetGroup.getId());
                    }
                    logger.info("Successfully added " + referencedGroup.getRules().size() + " rules from group " + refId + " to group " + targetGroup.getId());
                } else {
                    String errorMsg = "Referenced rule group not found in global registry: " + refId + " in group: " + groupId;
                    logger.error(errorMsg);
                    throw new YamlConfigurationException(errorMsg);
                }
            }
        }

        resolvingGroups.remove(groupId);
        resolvedGroups.add(groupId);
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
            logger.debug("Creating new category: " + name + " with priority: " + defaultPriority);
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
    public List<Rule> createRules(YamlRuleConfiguration yamlConfig) {
        List<Rule> rules = new ArrayList<>();

        // Process categories first to populate cache for metadata inheritance
        if (yamlConfig.getCategories() != null) {
            for (YamlCategory yamlCategory : yamlConfig.getCategories()) {
                if (yamlCategory.getEnabled() == null || yamlCategory.getEnabled()) {
                    Category category = createCategory(yamlCategory);
                    categoryCache.put(category.getName(), category);
                    // Also cache the YAML category for metadata inheritance
                    yamlCategoryCache.put(yamlCategory.getName(), yamlCategory);
                    logger.debug("Cached category '" + yamlCategory.getName() +
                               "' with businessOwner: " + yamlCategory.getBusinessOwner() +
                               ", businessDomain: " + yamlCategory.getBusinessDomain());
                }
            }
        }

        if (yamlConfig.getRules() != null) {
            for (YamlRule yamlRule : yamlConfig.getRules()) {
                if (yamlRule.getEnabled() == null || yamlRule.getEnabled()) {
                    rules.add(createRuleWithMetadata(yamlRule));
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
     * Create a lookup index of rules keyed by their ID.
     * 
     * <p>This is an optimisation method for the item-level processing path in
     * {@link dev.mars.apex.core.engine.config.execution.SequentialProcessor}. Instead of
     * iterating the YAML rule list and calling {@code createRuleWithMetadata} for
     * every individual item lookup, callers build the index once and perform
     * O(1) lookups thereafter.</p>
     *
     * @param yamlConfig The YAML configuration containing rules
     * @return Map of rule ID → Rule; empty map if no rules are defined
     */
    public Map<String, Rule> createRuleIndex(YamlRuleConfiguration yamlConfig) {
        List<Rule> rules = createRules(yamlConfig);
        Map<String, Rule> index = new LinkedHashMap<>(rules.size());
        for (Rule rule : rules) {
            if (rule.getId() != null) {
                index.put(rule.getId(), rule);
            } else if (rule.getName() != null) {
                index.put(rule.getName(), rule);
            }
        }
        return index;
    }

    /**
     * Create a lookup index of rule groups keyed by their ID.
     *
     * <p>Counterpart to {@link #createRuleIndex(YamlRuleConfiguration)} for rule groups.
     * Builds all rules first (needed for group membership resolution), then all groups,
     * and returns an O(1) lookup map.</p>
     *
     * @param yamlConfig The YAML configuration containing rule groups
     * @param config The rules engine configuration (for rule resolution within groups)
     * @return Map of group ID → RuleGroup; empty map if no groups are defined
     * @throws YamlConfigurationException if group creation fails
     */
    public Map<String, RuleGroup> createRuleGroupIndex(YamlRuleConfiguration yamlConfig,
                                                        RulesEngineConfiguration config) throws YamlConfigurationException {
        List<RuleGroup> groups = createRuleGroups(yamlConfig, config);
        Map<String, RuleGroup> index = new LinkedHashMap<>(groups.size());
        for (RuleGroup group : groups) {
            if (group.getId() != null) {
                index.put(group.getId(), group);
            }
        }
        return index;
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
     * Parse a date string that can be either ISO-8601 instant format or simple date format.
     *
     * @param dateString The date string to parse
     * @return Instant representation of the date
     * @throws DateTimeParseException if the date cannot be parsed
     */
    private Instant parseDate(String dateString) throws DateTimeParseException {
        try {
            // First try to parse as ISO-8601 instant (e.g., "2024-01-01T00:00:00Z")
            return Instant.parse(dateString);
        } catch (DateTimeParseException e) {
            try {
                // If that fails, try to parse as simple date and convert to instant at start of day UTC
                LocalDate localDate = LocalDate.parse(dateString);
                return localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException e2) {
                // If both fail, throw the original exception
                throw e;
            }
        }
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
            logger.warn("Very high priority override (" + priority + ") for rule: " + ruleId +
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
            originalRule.getSeverity(),
            originalRule.getMetadata(),
            originalRule.getDefaultValue(),
            originalRule.getSuccessCode(),
            originalRule.getErrorCode(),
            originalRule.getMapToField(),
            originalRule.getResultField(),
            originalRule.getNoMatchMessage(),
            originalRule.isEnabled()
        );

        return newRule;
    }

    /**
     * Create an enrichment with metadata inheritance from categories.
     * This method applies the same category inheritance pattern used for rules.
     *
     * @param yamlEnrichment The YAML enrichment configuration
     * @return An Enrichment object with inherited metadata
     */
    public dev.mars.apex.core.engine.model.Enrichment createEnrichmentWithMetadata(dev.mars.apex.core.config.YamlEnrichment yamlEnrichment) {
        if (yamlEnrichment == null) {
            throw new IllegalArgumentException("YamlEnrichment cannot be null");
        }

        String id = yamlEnrichment.getId() != null ? yamlEnrichment.getId() : generateFallbackEnrichmentId();
        String name = yamlEnrichment.getName() != null ? yamlEnrichment.getName() : id;
        String description = yamlEnrichment.getDescription() != null ? yamlEnrichment.getDescription() : "";
        String type = yamlEnrichment.getType() != null ? yamlEnrichment.getType() : "field-enrichment";
        int priority = yamlEnrichment.getPriority() != null ? yamlEnrichment.getPriority() : 100;

        // Determine category
        String categoryName = yamlEnrichment.getCategory() != null ? yamlEnrichment.getCategory() : "default";

        // Look up category metadata from cache for inheritance
        YamlCategory yamlCategory = findYamlCategoryByName(categoryName);
        logger.debug("Found category '" + categoryName + "' for enrichment '" + yamlEnrichment.getId() +
                   "'. YamlCategory found: " + (yamlCategory != null) +
                   (yamlCategory != null ? ", businessOwner: " + yamlCategory.getBusinessOwner() : ""));

        // Create enrichment with category
        dev.mars.apex.core.engine.model.Enrichment enrichment = new dev.mars.apex.core.engine.model.Enrichment(id, categoryName, name, description, type, priority);

        // Apply enterprise metadata with category inheritance
        // Enrichment metadata takes precedence, but inherit from category if not specified
        String createdBy = yamlEnrichment.getCreatedBy();
        if (createdBy == null && yamlCategory != null) {
            createdBy = yamlCategory.getCreatedBy();
        }
        if (createdBy != null) {
            enrichment.setCreatedBy(createdBy);
        }

        String businessDomain = yamlEnrichment.getBusinessDomain();
        if (businessDomain == null && yamlCategory != null) {
            businessDomain = yamlCategory.getBusinessDomain();
        }
        if (businessDomain != null) {
            enrichment.setBusinessDomain(businessDomain);
        }

        String businessOwner = yamlEnrichment.getBusinessOwner();
        if (businessOwner == null && yamlCategory != null) {
            businessOwner = yamlCategory.getBusinessOwner();
        }
        if (businessOwner != null) {
            enrichment.setBusinessOwner(businessOwner);
        }

        String sourceSystem = yamlEnrichment.getSourceSystem();
        if (sourceSystem != null) {
            enrichment.setSourceSystem(sourceSystem);
        }

        String effectiveDate = yamlEnrichment.getEffectiveDate();
        if (effectiveDate == null && yamlCategory != null) {
            effectiveDate = yamlCategory.getEffectiveDate();
        }
        if (effectiveDate != null) {
            enrichment.setEffectiveDate(effectiveDate);
        }

        String expirationDate = yamlEnrichment.getExpirationDate();
        if (expirationDate == null && yamlCategory != null) {
            expirationDate = yamlCategory.getExpirationDate();
        }
        if (expirationDate != null) {
            enrichment.setExpirationDate(expirationDate);
        }

        logger.debug("Applied metadata inheritance to enrichment '" + id + "': " +
                   "createdBy=" + enrichment.getCreatedBy() + ", " +
                   "businessDomain=" + enrichment.getBusinessDomain() + ", " +
                   "businessOwner=" + enrichment.getBusinessOwner());

        return enrichment;
    }

    /**
     * Create a list of enrichments from YAML configuration with metadata inheritance.
     *
     * @param yamlConfig The YAML configuration
     * @return List of Enrichment objects with inherited metadata
     */
    public List<dev.mars.apex.core.engine.model.Enrichment> createEnrichments(YamlRuleConfiguration yamlConfig) {
        List<dev.mars.apex.core.engine.model.Enrichment> enrichments = new ArrayList<>();

        // Process categories first to populate cache for metadata inheritance
        if (yamlConfig.getCategories() != null) {
            for (YamlCategory yamlCategory : yamlConfig.getCategories()) {
                if (yamlCategory.getEnabled() == null || yamlCategory.getEnabled()) {
                    Category category = createCategory(yamlCategory);
                    categoryCache.put(yamlCategory.getName(), category);
                    yamlCategoryCache.put(yamlCategory.getName(), yamlCategory);
                }
            }
        }

        if (yamlConfig.getEnrichments() != null) {
            for (dev.mars.apex.core.config.YamlEnrichment yamlEnrichment : yamlConfig.getEnrichments()) {
                if (yamlEnrichment.getEnabled() == null || yamlEnrichment.getEnabled()) {
                    enrichments.add(createEnrichmentWithMetadata(yamlEnrichment));
                }
            }
        }

        return enrichments;
    }

    /**
     * Generate a fallback enrichment ID when none is specified in YAML.
     *
     * @return A generated enrichment ID
     */
    private String generateFallbackEnrichmentId() {
        return "E" + UUID.randomUUID().toString().substring(0, 8);
    }
}
