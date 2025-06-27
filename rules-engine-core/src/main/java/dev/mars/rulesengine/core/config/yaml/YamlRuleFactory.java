package dev.mars.rulesengine.core.config.yaml;

import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Category;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Factory service for converting YAML configuration objects into rules engine objects.
 * This class handles the transformation from YAML configuration to actual Rule, RuleGroup, and Category objects.
 */
public class YamlRuleFactory {
    
    private static final Logger LOGGER = Logger.getLogger(YamlRuleFactory.class.getName());
    
    private final Map<String, Category> categoryCache = new HashMap<>();
    
    /**
     * Create a RulesEngineConfiguration from YAML configuration.
     * 
     * @param yamlConfig The YAML configuration
     * @return A configured RulesEngineConfiguration
     */
    public RulesEngineConfiguration createRulesEngineConfiguration(YamlRuleConfiguration yamlConfig) {
        LOGGER.info("Creating RulesEngineConfiguration from YAML configuration");
        
        RulesEngineConfiguration config = new RulesEngineConfiguration();
        
        // Process categories first
        if (yamlConfig.getCategories() != null) {
            for (YamlCategory yamlCategory : yamlConfig.getCategories()) {
                Category category = createCategory(yamlCategory);
                categoryCache.put(category.getName(), category);
            }
        }
        
        // Process individual rules
        if (yamlConfig.getRules() != null) {
            for (YamlRule yamlRule : yamlConfig.getRules()) {
                if (yamlRule.getEnabled() == null || yamlRule.getEnabled()) {
                    Rule rule = createRule(yamlRule);
                    config.registerRule(rule);
                }
            }
        }
        
        // Process rule groups
        if (yamlConfig.getRuleGroups() != null) {
            for (YamlRuleGroup yamlGroup : yamlConfig.getRuleGroups()) {
                if (yamlGroup.getEnabled() == null || yamlGroup.getEnabled()) {
                    RuleGroup group = createRuleGroup(yamlGroup, config);
                    config.registerRuleGroup(group);
                }
            }
        }
        
        LOGGER.info("Successfully created RulesEngineConfiguration with " + 
                   config.getAllRules().size() + " rules and " + 
                   config.getAllRuleGroups().size() + " rule groups");
        
        return config;
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
     * Create a Rule from YAML rule configuration.
     * 
     * @param yamlRule The YAML rule configuration
     * @return A Rule object
     */
    public Rule createRule(YamlRule yamlRule) {
        String id = yamlRule.getId();
        String name = yamlRule.getName();
        String condition = yamlRule.getCondition();
        String message = yamlRule.getMessage() != null ? yamlRule.getMessage() : "Rule " + name + " triggered";
        String description = yamlRule.getDescription() != null ? yamlRule.getDescription() : "";
        int priority = yamlRule.getPriority() != null ? yamlRule.getPriority() : 100;
        
        LOGGER.fine("Creating rule: " + id + " (" + name + ")");
        
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
        
        return new Rule(id, category, name, condition, message, description, priority);
    }
    
    /**
     * Create a RuleGroup from YAML rule group configuration.
     * 
     * @param yamlGroup The YAML rule group configuration
     * @param config The rules engine configuration (to lookup existing rules)
     * @return A RuleGroup object
     */
    public RuleGroup createRuleGroup(YamlRuleGroup yamlGroup, RulesEngineConfiguration config) {
        String id = yamlGroup.getId();
        String name = yamlGroup.getName();
        String description = yamlGroup.getDescription() != null ? yamlGroup.getDescription() : "";
        int priority = yamlGroup.getPriority() != null ? yamlGroup.getPriority() : 100;
        boolean stopOnFirstFailure = yamlGroup.getStopOnFirstFailure() != null ? yamlGroup.getStopOnFirstFailure() : false;
        
        LOGGER.fine("Creating rule group: " + id + " (" + name + ")");
        
        // Determine category
        String categoryName = yamlGroup.getCategory() != null ? yamlGroup.getCategory() : "default";
        Category category = getOrCreateCategory(categoryName, priority);
        
        RuleGroup group = new RuleGroup(id, categoryName, name, description, priority, stopOnFirstFailure);
        
        // Add rules to the group
        addRulesToGroup(yamlGroup, group, config);
        
        return group;
    }
    
    /**
     * Add rules to a rule group based on YAML configuration.
     */
    private void addRulesToGroup(YamlRuleGroup yamlGroup, RuleGroup group, RulesEngineConfiguration config) {
        // Add rules by ID
        if (yamlGroup.getRuleIds() != null) {
            int sequence = 1;
            for (String ruleId : yamlGroup.getRuleIds()) {
                Rule rule = config.getRuleById(ruleId);
                if (rule != null) {
                    group.addRule(rule, sequence++);
                    LOGGER.fine("Added rule " + ruleId + " to group " + group.getId());
                } else {
                    LOGGER.warning("Rule not found for ID: " + ruleId + " in group: " + group.getId());
                }
            }
        }
        
        // Add rules by reference (with more detailed configuration)
        if (yamlGroup.getRuleReferences() != null) {
            for (YamlRuleGroup.RuleReference ref : yamlGroup.getRuleReferences()) {
                if (ref.getEnabled() == null || ref.getEnabled()) {
                    Rule rule = config.getRuleById(ref.getRuleId());
                    if (rule != null) {
                        int sequence = ref.getSequence() != null ? ref.getSequence() : 1;
                        group.addRule(rule, sequence);
                        LOGGER.fine("Added rule " + ref.getRuleId() + " to group " + group.getId() + " with sequence " + sequence);
                    } else {
                        LOGGER.warning("Rule not found for ID: " + ref.getRuleId() + " in group: " + group.getId());
                    }
                }
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
     * @param yamlConfig The YAML configuration
     * @return List of Rule objects
     */
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
     * @param yamlConfig The YAML configuration
     * @param config The rules engine configuration (to lookup existing rules)
     * @return List of RuleGroup objects
     */
    public List<RuleGroup> createRuleGroups(YamlRuleConfiguration yamlConfig, RulesEngineConfiguration config) {
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
     * Clear the category cache.
     */
    public void clearCache() {
        categoryCache.clear();
    }
}
