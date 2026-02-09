package dev.mars.apex.core.util;

import dev.mars.apex.core.config.yaml.sequential.ProcessingItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Report containing the processing sequence analysis of a YAML configuration file.
 *
 * <p>This report shows:
 * <ul>
 *   <li>Configuration sections - sections processed at load/initialization time</li>
 *   <li>Original sequence - items as they appear in the YAML document</li>
 *   <li>Planned sequence - items after groups-only logic filtering</li>
 *   <li>Filtered items - items removed by groups-only logic (execute via groups only)</li>
 * </ul>
 *
 * <p>The report provides insight into how apex-core will process a YAML file,
 * showing both configuration/initialization phase and execution phase.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-08
 */
public class ProcessingSequenceReport {

    private final String yamlFilePath;
    private final List<String> configurationSections;
    private final List<ProcessingItem> originalSequence;
    private final List<ProcessingItem> plannedSequence;
    private final List<ProcessingItem> filteredItems;
    
    /**
     * Creates a new processing sequence report with configuration sections.
     *
     * @param yamlFilePath The path to the YAML file analyzed
     * @param configurationSections List of configuration section names in document order
     * @param originalSequence The original item order from the YAML document
     * @param plannedSequence The planned execution order after filtering
     * @param filteredItems Items removed by groups-only logic
     */
    public ProcessingSequenceReport(String yamlFilePath,
                                   List<String> configurationSections,
                                   List<ProcessingItem> originalSequence,
                                   List<ProcessingItem> plannedSequence,
                                   List<ProcessingItem> filteredItems) {
        this.yamlFilePath = yamlFilePath;
        this.configurationSections = Collections.unmodifiableList(new ArrayList<>(configurationSections));
        this.originalSequence = Collections.unmodifiableList(new ArrayList<>(originalSequence));
        this.plannedSequence = Collections.unmodifiableList(new ArrayList<>(plannedSequence));
        this.filteredItems = Collections.unmodifiableList(new ArrayList<>(filteredItems));
    }
    
    /**
     * Gets the path to the YAML file that was analyzed.
     * 
     * @return The YAML file path
     */
    public String getYamlFilePath() {
        return yamlFilePath;
    }
    
    /**
     * Gets the original sequence of items as they appear in the YAML document.
     * This is the order before any filtering is applied.
     * 
     * @return Immutable list of processing items in document order
     */
    public List<ProcessingItem> getOriginalSequence() {
        return originalSequence;
    }
    
    /**
     * Gets the planned execution sequence after groups-only logic filtering.
     * This is the order in which items will actually execute.
     * 
     * @return Immutable list of processing items in execution order
     */
    public List<ProcessingItem> getPlannedSequence() {
        return plannedSequence;
    }
    
    /**
     * Gets the items that were filtered out by groups-only logic.
     * These items are definitions only and will execute via their groups.
     *
     * @return Immutable list of filtered processing items
     */
    public List<ProcessingItem> getFilteredItems() {
        return filteredItems;
    }

    /**
     * Gets the configuration sections that are processed at load/initialization time.
     * These sections are not part of the execution sequence but are processed once
     * when the YAML file is loaded.
     *
     * @return Immutable list of configuration section names in document order
     */
    public List<String> getConfigurationSections() {
        return configurationSections;
    }
    
    /**
     * Gets a formatted human-readable report of the processing sequence.
     * 
     * @return Formatted report string
     */
    public String getFormattedReport() {
        StringBuilder sb = new StringBuilder();

        // Extract just the filename from the path for better readability
        String fileName = yamlFilePath;
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        } else if (fileName.contains("\\")) {
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
        }

        sb.append("\n");
        sb.append("=".repeat(100)).append("\n");
        sb.append("YAML PROCESSING SEQUENCE ANALYSIS\n");
        sb.append("=".repeat(100)).append("\n");
        sb.append(String.format("FILE: %s\n", fileName));
        sb.append(String.format("PATH: %s\n", yamlFilePath));
        sb.append("=".repeat(100)).append("\n");
        sb.append("\n");

        // Configuration/Initialization Phase
        if (!configurationSections.isEmpty()) {
            sb.append("PHASE 1: CONFIGURATION/INITIALIZATION (").append(configurationSections.size()).append(" sections - processed at load time)\n");
            sb.append("-".repeat(100)).append("\n");
            for (int i = 0; i < configurationSections.size(); i++) {
                String section = configurationSections.get(i);
                sb.append(String.format("  %3d. %-28s [%s]\n",
                    i + 1,
                    section,
                    getConfigurationSectionDescription(section)));
            }
            sb.append("\n");
        }

        // Original sequence
        sb.append("PHASE 2: EXECUTION SEQUENCE (").append(originalSequence.size()).append(" items in YAML document order)\n");
        sb.append("-".repeat(100)).append("\n");
        if (originalSequence.isEmpty()) {
            sb.append("  (empty)\n");
        } else {
            for (int i = 0; i < originalSequence.size(); i++) {
                ProcessingItem item = originalSequence.get(i);
                sb.append(formatItem(i + 1, item, false));
            }
        }
        sb.append("\n");

        // Filtered items
        if (!filteredItems.isEmpty()) {
            sb.append("FILTERED ITEMS (").append(filteredItems.size()).append(" items - definitions only, execute via groups)\n");
            sb.append("-".repeat(100)).append("\n");
            for (ProcessingItem item : filteredItems) {
                sb.append(formatItem(0, item, true));
            }
            sb.append("\n");
        }

        // Planned sequence
        sb.append("PLANNED EXECUTION SEQUENCE (").append(plannedSequence.size()).append(" items after filtering)\n");
        sb.append("-".repeat(100)).append("\n");
        if (plannedSequence.isEmpty()) {
            sb.append("  (empty)\n");
        } else {
            for (int i = 0; i < plannedSequence.size(); i++) {
                ProcessingItem item = plannedSequence.get(i);
                sb.append(formatItem(i + 1, item, false));
            }
        }
        sb.append("\n");

        // Summary
        sb.append("SUMMARY\n");
        sb.append("-".repeat(100)).append("\n");
        sb.append(String.format("  Total items in YAML:     %d\n", originalSequence.size()));
        sb.append(String.format("  Filtered (groups-only):  %d\n", filteredItems.size()));
        sb.append(String.format("  Final execution order:   %d\n", plannedSequence.size()));
        sb.append("=".repeat(100)).append("\n");
        
        return sb.toString();
    }

    /**
     * Format a single processing item for display.
     *
     * @param index The sequence number (1-based), or 0 for filtered items
     * @param item The processing item to format
     * @param isFiltered Whether this is a filtered item
     * @return Formatted string for the item
     */
    private String formatItem(int index, ProcessingItem item, boolean isFiltered) {
        StringBuilder sb = new StringBuilder();

        // Build the description based on what metadata is available
        String description = buildItemDescription(item);

        if (isFiltered) {
            // Filtered items use >>> prefix
            sb.append(String.format("  >>> %-25s -> %-20s %s (FILTERED - definition only)\n",
                item.getSectionType(),
                item.getItemId(),
                description));
        } else {
            // Regular items use numbered format
            sb.append(String.format("  %3d. %-25s -> %-20s %s\n",
                index,
                item.getSectionType(),
                item.getItemId(),
                description));
        }

        return sb.toString();
    }

    /**
     * Build a human-readable description of the item based on available metadata.
     *
     * @param item The processing item
     * @return Description string
     */
    private String buildItemDescription(ProcessingItem item) {
        // For groups, show the name if available
        if (item.isEnrichmentGroup() || item.isRuleGroup()) {
            if (item.getItemName() != null && !item.getItemName().isEmpty()) {
                return "[" + item.getItemName() + "]";
            }
            return "[group]";
        }

        // For enrichments and rules, show the type
        if (item.isEnrichment() || item.isRule() || item.isTransformation()) {
            if (item.getItemType() != null && !item.getItemType().isEmpty()) {
                return "[" + item.getItemType() + "]";
            }
        }

        // Default: no description
        return "";
    }

    /**
     * Get a human-readable description of what a configuration section does.
     *
     * @param sectionName The configuration section name
     * @return Description of the section's purpose
     */
    private String getConfigurationSectionDescription(String sectionName) {
        return switch (sectionName) {
            case "metadata" -> "File metadata and processing mode";
            case "categories" -> "Category definitions for metadata inheritance";
            case "data-sources" -> "Data source connections (databases, APIs, files)";
            case "data-source-refs" -> "External data source references";
            case "data-sinks" -> "Data output destinations";
            case "error-recovery" -> "Error handling and recovery policies";
            default -> "Configuration section";
        };
    }

    @Override
    public String toString() {
        return getFormattedReport();
    }
}

