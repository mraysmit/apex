package dev.mars.apex.core.util;

import dev.mars.apex.core.config.yaml.OrderedYamlConfiguration;
import dev.mars.apex.core.config.yaml.OrderedYamlParser;
import dev.mars.apex.core.config.yaml.ProcessingItem;
import dev.mars.apex.core.config.yaml.YamlEnrichmentGroup;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.YamlRuleGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility for analyzing the processing sequence of YAML configuration files.
 * 
 * <p>This analyzer uses apex-core's actual parsing and filtering logic to determine
 * the exact order in which items will be processed by the RulesEngine. It does NOT
 * execute the YAML - it only analyzes the planned processing sequence.
 * 
 * <p>The analyzer shows:
 * <ul>
 *   <li>Original sequence - items as they appear in the YAML document</li>
 *   <li>Planned sequence - items after groups-only logic filtering</li>
 *   <li>Filtered items - items removed by groups-only logic</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * YamlProcessingSequenceAnalyzer analyzer = new YamlProcessingSequenceAnalyzer();
 * ProcessingSequenceReport report = analyzer.analyze("path/to/config.yaml");
 * System.out.println(report.getFormattedReport());
 * </pre>
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class YamlProcessingSequenceAnalyzer {
    
    private static final Logger LOGGER = Logger.getLogger(YamlProcessingSequenceAnalyzer.class.getName());
    
    private final OrderedYamlParser parser;
    
    /**
     * Creates a new YAML processing sequence analyzer.
     */
    public YamlProcessingSequenceAnalyzer() {
        this.parser = new OrderedYamlParser();
    }
    
    /**
     * Analyzes the processing sequence of a YAML file.
     *
     * @param yamlFilePath Path to the YAML file to analyze
     * @return Processing sequence report
     * @throws IllegalArgumentException if the file does not exist or cannot be read
     * @throws RuntimeException if YAML parsing fails
     */
    public ProcessingSequenceReport analyze(String yamlFilePath) {
        LOGGER.info("Analyzing processing sequence for: " + yamlFilePath);

        // Validate file exists
        File file = new File(yamlFilePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("YAML file not found: " + yamlFilePath);
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException("Cannot read YAML file: " + yamlFilePath);
        }

        try {
            // Parse YAML using OrderedYamlParser
            OrderedYamlConfiguration orderedConfig = parser.parseFile(yamlFilePath);
            YamlRuleConfiguration config = orderedConfig.getConfiguration();

            // Extract configuration sections (processed at load time)
            List<String> configurationSections = extractConfigurationSections(orderedConfig);
            LOGGER.info("Found " + configurationSections.size() + " configuration sections");

            // Get original item order
            List<ProcessingItem> originalSequence = new ArrayList<>(orderedConfig.getItemOrder());
            LOGGER.info("Original sequence has " + originalSequence.size() + " items");

            // Apply groups-only logic to get planned sequence
            List<ProcessingItem> plannedSequence = applyGroupsOnlyLogic(config, originalSequence);
            LOGGER.info("Planned sequence has " + plannedSequence.size() + " items");

            // Calculate filtered items
            List<ProcessingItem> filteredItems = new ArrayList<>(originalSequence);
            filteredItems.removeAll(plannedSequence);
            LOGGER.info("Filtered " + filteredItems.size() + " items");

            return new ProcessingSequenceReport(yamlFilePath, configurationSections, originalSequence, plannedSequence, filteredItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze YAML file: " + yamlFilePath, e);
        }
    }
    
    /**
     * Analyzes the processing sequence of a YAML string.
     * 
     * @param yamlContent The YAML content to analyze
     * @return Processing sequence report
     */
    public ProcessingSequenceReport analyzeYamlString(String yamlContent) {
        return analyzeYamlString(yamlContent, "<inline-yaml>");
    }
    
    /**
     * Analyzes the processing sequence of a YAML string with a custom identifier.
     *
     * @param yamlContent The YAML content to analyze
     * @param identifier A descriptive identifier for the YAML content
     * @return Processing sequence report
     * @throws RuntimeException if YAML parsing fails
     */
    public ProcessingSequenceReport analyzeYamlString(String yamlContent, String identifier) {
        LOGGER.info("Analyzing processing sequence for: " + identifier);

        try {
            // Parse YAML using OrderedYamlParser
            OrderedYamlConfiguration orderedConfig = parser.parseYamlString(yamlContent, identifier);
            YamlRuleConfiguration config = orderedConfig.getConfiguration();

            // Extract configuration sections (processed at load time)
            List<String> configurationSections = extractConfigurationSections(orderedConfig);
            LOGGER.info("Found " + configurationSections.size() + " configuration sections");

            // Get original item order
            List<ProcessingItem> originalSequence = new ArrayList<>(orderedConfig.getItemOrder());
            LOGGER.info("Original sequence has " + originalSequence.size() + " items");

            // Apply groups-only logic to get planned sequence
            List<ProcessingItem> plannedSequence = applyGroupsOnlyLogic(config, originalSequence);
            LOGGER.info("Planned sequence has " + plannedSequence.size() + " items");

            // Calculate filtered items
            List<ProcessingItem> filteredItems = new ArrayList<>(originalSequence);
            filteredItems.removeAll(plannedSequence);
            LOGGER.info("Filtered " + filteredItems.size() + " items");

            return new ProcessingSequenceReport(identifier, configurationSections, originalSequence, plannedSequence, filteredItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze YAML content: " + identifier, e);
        }
    }

    /**
     * Extract configuration sections that are processed at load/initialization time.
     * These sections are not part of the execution sequence but are processed once
     * when the YAML file is loaded.
     *
     * @param orderedConfig The ordered YAML configuration
     * @return List of configuration section names in document order
     */
    private List<String> extractConfigurationSections(OrderedYamlConfiguration orderedConfig) {
        List<String> configSections = new ArrayList<>();

        // Configuration sections that are processed at load time (not in itemOrder)
        Set<String> CONFIG_SECTIONS = Set.of(
            "metadata", "categories", "data-sources", "data-source-refs",
            "data-sinks", "error-recovery"
        );

        // Get all sections in document order and filter for configuration sections
        for (String section : orderedConfig.getSectionOrder()) {
            if (CONFIG_SECTIONS.contains(section) && orderedConfig.isSectionPopulated(section)) {
                configSections.add(section);
            }
        }

        return configSections;
    }

    /**
     * Apply groups-only logic to filter itemOrder.
     * This is a simplified version of YamlConfigurationLoader.applyGroupsOnlyLogic()
     * that only performs filtering without modifying the configuration.
     * 
     * <p>When enrichments/rules and enrichment-groups/rule-groups coexist in the SAME file:
     * <ul>
     *   <li>Enrichments/rules referenced by groups are definitions only (do NOT execute at their definition position)</li>
     *   <li>Enrichments/rules NOT referenced by groups execute directly at their definition position</li>
     *   <li>Enrichment-groups/rule-groups execute at their position in document order</li>
     * </ul>
     * 
     * @param config The YAML configuration
     * @param originalOrder The original item order
     * @return Filtered item order (planned execution sequence)
     */
    private List<ProcessingItem> applyGroupsOnlyLogic(YamlRuleConfiguration config, List<ProcessingItem> originalOrder) {
        LOGGER.fine("Applying groups-only logic");
        
        if (originalOrder == null || originalOrder.isEmpty()) {
            LOGGER.fine("No items to filter");
            return new ArrayList<>();
        }
        
        // Collect enrichment IDs referenced by enrichment-groups
        Set<String> referencedEnrichmentIds = new HashSet<>();
        if (config.getEnrichmentGroups() != null && !config.getEnrichmentGroups().isEmpty()) {
            for (YamlEnrichmentGroup group : config.getEnrichmentGroups()) {
                if (group.getEnrichmentIds() != null) {
                    referencedEnrichmentIds.addAll(group.getEnrichmentIds());
                }
            }
            LOGGER.fine("Found " + referencedEnrichmentIds.size() + " enrichment IDs referenced by groups");
        }
        
        // Collect rule IDs referenced by rule-groups
        Set<String> referencedRuleIds = new HashSet<>();
        if (config.getRuleGroups() != null && !config.getRuleGroups().isEmpty()) {
            for (YamlRuleGroup group : config.getRuleGroups()) {
                if (group.getRuleIds() != null) {
                    referencedRuleIds.addAll(group.getRuleIds());
                }
            }
            LOGGER.fine("Found " + referencedRuleIds.size() + " rule IDs referenced by groups");
        }
        
        // If no groups exist, no filtering needed
        if (referencedEnrichmentIds.isEmpty() && referencedRuleIds.isEmpty()) {
            LOGGER.fine("No groups found - all items execute at definition position");
            return new ArrayList<>(originalOrder);
        }
        
        // Filter items
        List<ProcessingItem> filteredOrder = new ArrayList<>();
        int enrichmentsFiltered = 0;
        int rulesFiltered = 0;
        
        for (ProcessingItem item : originalOrder) {
            boolean shouldRemove = false;
            
            if ("enrichments".equals(item.getSectionType()) &&
                referencedEnrichmentIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via enrichment-group
                enrichmentsFiltered++;
                LOGGER.fine("Filtering enrichment '" + item.getItemId() + "' (referenced by group)");
            } else if ("rules".equals(item.getSectionType()) &&
                       referencedRuleIds.contains(item.getItemId())) {
                shouldRemove = true;  // Skip - will execute via rule-group
                rulesFiltered++;
                LOGGER.fine("Filtering rule '" + item.getItemId() + "' (referenced by group)");
            }
            
            if (!shouldRemove) {
                filteredOrder.add(item);
            }
        }
        
        LOGGER.fine("Filtered " + enrichmentsFiltered + " enrichments and " + rulesFiltered + " rules");
        LOGGER.fine("Original: " + originalOrder.size() + " items, Filtered: " + filteredOrder.size() + " items");
        
        return filteredOrder;
    }
}

