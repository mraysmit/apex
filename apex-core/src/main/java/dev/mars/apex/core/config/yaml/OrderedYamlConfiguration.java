package dev.mars.apex.core.config.yaml;

import java.util.*;
import java.util.logging.Logger;

/**
 * Wrapper for YamlRuleConfiguration that preserves the natural order of YAML sections
 * as they appear in the document, enabling sequential processing that respects developer intent.
 * 
 * This class fixes APEX's fundamental design flaw by maintaining section order information
 * that was lost during Jackson's @JsonProperty parsing process.
 * 
 * Key Features:
 * - Preserves exact section order from YAML document
 * - Provides access to sections in document order for sequential processing
 * - Maintains full compatibility with existing YamlRuleConfiguration
 * - Supports processing mode detection and selection
 * 
 * @author APEX Sequential Processing Implementation
 * @since Phase 1 - Foundation
 */
public class OrderedYamlConfiguration {
    
    private static final Logger LOGGER = Logger.getLogger(OrderedYamlConfiguration.class.getName());
    
    private final YamlRuleConfiguration configuration;
    private final List<String> sectionOrder;
    private final Map<String, Integer> sectionPositions;
    private final List<ProcessingItem> itemOrder;

    /**
     * Create an ordered YAML configuration (backward compatibility constructor).
     *
     * @param configuration The parsed YAML configuration
     * @param sectionOrder The order of sections as they appear in the YAML document
     * @deprecated Use {@link #OrderedYamlConfiguration(YamlRuleConfiguration, List, List)} instead
     */
    @Deprecated
    public OrderedYamlConfiguration(YamlRuleConfiguration configuration, List<String> sectionOrder) {
        this(configuration, sectionOrder, new ArrayList<>());
    }

    /**
     * Create an ordered YAML configuration with both section and item order.
     *
     * @param configuration The parsed YAML configuration
     * @param sectionOrder The order of sections as they appear in the YAML document
     * @param itemOrder The order of individual items as they appear in the YAML document
     */
    public OrderedYamlConfiguration(YamlRuleConfiguration configuration, List<String> sectionOrder, List<ProcessingItem> itemOrder) {
        this.configuration = configuration;
        this.sectionOrder = new ArrayList<>(sectionOrder);
        this.sectionPositions = createPositionMap(sectionOrder);
        this.itemOrder = new ArrayList<>(itemOrder);

        LOGGER.fine("Created OrderedYamlConfiguration with " + sectionOrder.size() + " sections and " + itemOrder.size() + " items");
    }
    
    /**
     * Get the underlying YamlRuleConfiguration.
     * 
     * @return The configuration object
     */
    public YamlRuleConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Get the order of sections as they appear in the YAML document.
     *
     * @return Immutable list of section names in document order
     */
    public List<String> getSectionOrder() {
        return Collections.unmodifiableList(sectionOrder);
    }

    /**
     * Get the order of individual items as they appear in the YAML document.
     * This enables item-level sequential processing where items from different
     * sections can be interleaved in document order.
     *
     * @return Immutable list of processing items in document order
     */
    public List<ProcessingItem> getItemOrder() {
        return Collections.unmodifiableList(itemOrder);
    }
    
    /**
     * Get the position of a section in the document (0-based).
     * 
     * @param sectionName The name of the section
     * @return The position of the section, or -1 if not found
     */
    public int getSectionPosition(String sectionName) {
        return sectionPositions.getOrDefault(sectionName, -1);
    }
    
    /**
     * Check if a section appears before another section in the document.
     * 
     * @param firstSection The first section name
     * @param secondSection The second section name
     * @return true if firstSection appears before secondSection
     */
    public boolean isSectionBefore(String firstSection, String secondSection) {
        int firstPos = getSectionPosition(firstSection);
        int secondPos = getSectionPosition(secondSection);
        
        // If either section is not found, return false
        if (firstPos == -1 || secondPos == -1) {
            return false;
        }
        
        return firstPos < secondPos;
    }
    
    /**
     * Get sections that contain actual content (not null or empty).
     * 
     * @return List of section names that have content, in document order
     */
    public List<String> getPopulatedSections() {
        List<String> populatedSections = new ArrayList<>();
        
        for (String section : sectionOrder) {
            if (isSectionPopulated(section)) {
                populatedSections.add(section);
            }
        }
        
        return populatedSections;
    }
    
    /**
     * Check if a section has content (not null or empty).
     * 
     * @param sectionName The name of the section to check
     * @return true if the section has content
     */
    public boolean isSectionPopulated(String sectionName) {
        switch (sectionName) {
            case "metadata":
                return configuration.getMetadata() != null;
            case "data-sources":
                return configuration.getDataSources() != null && !configuration.getDataSources().isEmpty();
            case "data-source-refs":
                return configuration.getDataSourceRefs() != null && !configuration.getDataSourceRefs().isEmpty();
            case "rule-refs":
                return configuration.getRuleRefs() != null && !configuration.getRuleRefs().isEmpty();
            case "enrichment-refs":
                return configuration.getEnrichmentRefs() != null && !configuration.getEnrichmentRefs().isEmpty();
            case "data-sinks":
                return configuration.getDataSinks() != null && !configuration.getDataSinks().isEmpty();
            case "categories":
                return configuration.getCategories() != null && !configuration.getCategories().isEmpty();
            case "rules":
                return configuration.getRules() != null && !configuration.getRules().isEmpty();
            case "rule-groups":
                return configuration.getRuleGroups() != null && !configuration.getRuleGroups().isEmpty();
            case "enrichments":
                return configuration.getEnrichments() != null && !configuration.getEnrichments().isEmpty();
            case "enrichment-groups":
                return configuration.getEnrichmentGroups() != null && !configuration.getEnrichmentGroups().isEmpty();
            case "transformations":
                return configuration.getTransformations() != null && !configuration.getTransformations().isEmpty();
            case "rule-chains":
                return configuration.getRuleChains() != null && !configuration.getRuleChains().isEmpty();
            case "pipeline":
                return configuration.getPipeline() != null;
            case "error-recovery":
                return configuration.getErrorRecovery() != null;
            default:
                LOGGER.warning("Unknown section for population check: " + sectionName);
                return false;
        }
    }
    
    /**
     * Determine the processing mode based on metadata or default to STANDARD.
     *
     * @return The processing mode (STANDARD or SEQUENTIAL)
     */
    public ProcessingMode getProcessingMode() {
        if (configuration.getMetadata() != null) {
            String processingMode = configuration.getMetadata().getProcessingMode();
            if ("sequential".equalsIgnoreCase(processingMode)) {
                return ProcessingMode.SEQUENTIAL;
            }
        }

        // Default to STANDARD for backward compatibility
        return ProcessingMode.STANDARD;
    }
    
    /**
     * Create a position map for fast section position lookups.
     * 
     * @param sectionOrder The ordered list of sections
     * @return Map of section name to position
     */
    private Map<String, Integer> createPositionMap(List<String> sectionOrder) {
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < sectionOrder.size(); i++) {
            positions.put(sectionOrder.get(i), i);
        }
        return positions;
    }
    
    /**
     * Processing mode enumeration.
     */
    public enum ProcessingMode {
        /**
         * Standard processing mode - uses hardcoded processing orders (current behavior).
         * Maintains backward compatibility.
         */
        STANDARD,
        
        /**
         * Sequential processing mode - processes sections in document order.
         * Respects developer intent expressed through YAML structure.
         */
        SEQUENTIAL
    }
    
    @Override
    public String toString() {
        return "OrderedYamlConfiguration{" +
                "sectionOrder=" + sectionOrder +
                ", populatedSections=" + getPopulatedSections() +
                ", itemOrder=" + itemOrder.size() + " items" +
                ", processingMode=" + getProcessingMode() +
                '}';
    }
}
