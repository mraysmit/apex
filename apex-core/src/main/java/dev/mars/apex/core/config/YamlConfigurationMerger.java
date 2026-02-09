package dev.mars.apex.core.config;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for merging YAML rule configurations.
 * 
 * This class provides public static methods to merge multiple YAML configurations,
 * making the merge functionality available to both production code and tests.
 * 
 * @since 2025-11-02
 */
public class YamlConfigurationMerger {
    
    private static final Logger logger = LoggerFactory.getLogger(YamlConfigurationMerger.class);
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private YamlConfigurationMerger() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
    
    /**
     * Merge two YAML rule configurations.
     * 
     * This method merges all components from the source configuration into the target configuration.
     * The target configuration is modified in place.
     * 
     * Merge behavior:
     * - Metadata: Target metadata is preserved if it exists, otherwise source metadata is used
     * - All other components (data sources, rules, enrichments, etc.): Source components are appended to target
     * 
     * @param target The target configuration that will receive merged content (modified in place)
     * @param source The source configuration to merge from (not modified)
     */
    public static void merge(YamlRuleConfiguration target, YamlRuleConfiguration source) {
        if (target == null) {
            throw new IllegalArgumentException("Target configuration cannot be null");
        }
        if (source == null) {
            logger.debug("Source configuration is null, nothing to merge");
            return;
        }
        
        // Merge metadata (prefer target if both exist)
        if (target.getMetadata() == null && source.getMetadata() != null) {
            target.setMetadata(source.getMetadata());
        }

        // Merge data sources
        if (source.getDataSources() != null) {
            if (target.getDataSources() == null) {
                target.setDataSources(new ArrayList<>());
            }
            target.getDataSources().addAll(source.getDataSources());
        }

        // Merge data source references
        if (source.getDataSourceRefs() != null) {
            if (target.getDataSourceRefs() == null) {
                target.setDataSourceRefs(new ArrayList<>());
            }
            target.getDataSourceRefs().addAll(source.getDataSourceRefs());
        }

        // Merge rule references
        if (source.getRuleRefs() != null) {
            if (target.getRuleRefs() == null) {
                target.setRuleRefs(new ArrayList<>());
            }
            target.getRuleRefs().addAll(source.getRuleRefs());
        }

        // Merge data sinks
        if (source.getDataSinks() != null) {
            if (target.getDataSinks() == null) {
                target.setDataSinks(new ArrayList<>());
            }
            target.getDataSinks().addAll(source.getDataSinks());
        }

        // Merge categories
        if (source.getCategories() != null) {
            if (target.getCategories() == null) {
                target.setCategories(new ArrayList<>());
            }
            target.getCategories().addAll(source.getCategories());
        }

        // Merge rules
        if (source.getRules() != null) {
            if (target.getRules() == null) {
                target.setRules(new ArrayList<>());
            }
            target.getRules().addAll(source.getRules());
        }

        // Merge rule groups
        if (source.getRuleGroups() != null) {
            if (target.getRuleGroups() == null) {
                target.setRuleGroups(new ArrayList<>());
            }
            target.getRuleGroups().addAll(source.getRuleGroups());
        }

        // Merge enrichments
        if (source.getEnrichments() != null) {
            if (target.getEnrichments() == null) {
                target.setEnrichments(new ArrayList<>());
            }
            target.getEnrichments().addAll(source.getEnrichments());
        }

        // Merge enrichment groups
        if (source.getEnrichmentGroups() != null) {
            if (target.getEnrichmentGroups() == null) {
                target.setEnrichmentGroups(new ArrayList<>());
            }
            target.getEnrichmentGroups().addAll(source.getEnrichmentGroups());
        }

        // Merge rule chains
        if (source.getRuleChains() != null) {
            if (target.getRuleChains() == null) {
                target.setRuleChains(new ArrayList<>());
            }
            target.getRuleChains().addAll(source.getRuleChains());
        }

        logger.debug("Merged YAML configuration with " +
                   (source.getRules() != null ? source.getRules().size() : 0) + " rules, " +
                   (source.getRuleGroups() != null ? source.getRuleGroups().size() : 0) + " rule groups, and " +
                   (source.getEnrichments() != null ? source.getEnrichments().size() : 0) + " enrichments");
    }
}

