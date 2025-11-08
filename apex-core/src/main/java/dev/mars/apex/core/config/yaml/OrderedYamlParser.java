package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Order-preserving YAML parser that maintains the natural sequence of YAML sections
 * as they appear in the document, fixing APEX's fundamental design flaw.
 * 
 * This parser replaces Jackson's @JsonProperty approach which loses document order
 * by parsing sections into separate fields regardless of their position in the YAML file.
 * 
 * Key Features:
 * - Preserves exact section order from YAML document
 * - Maintains full compatibility with existing YamlRuleConfiguration structure
 * - Supports all existing YAML features (property resolution, validation, etc.)
 * - Enables sequential processing that respects developer intent
 * 
 * @author APEX Sequential Processing Implementation
 * @since Phase 1 - Foundation
 */
public class OrderedYamlParser {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderedYamlParser.class);
    
    // Known YAML section names in APEX
    private static final Set<String> KNOWN_SECTIONS = Set.of(
        "metadata", "data-sources", "data-source-refs", "rule-refs", "enrichment-refs",
        "data-sinks", "categories", "rules", "rule-groups", "enrichments",
        "enrichment-groups", "transformations", "rule-chains", "pipeline", "error-recovery"
    );

    // Sections that support numbered suffixes (e.g., enrichments-1, enrichments-2)
    private static final Set<String> NUMBERED_SUFFIX_SECTIONS = Set.of(
        "enrichments", "rules", "enrichment-groups", "rule-groups",
        "transformations", "rule-chains", "enrichment-refs", "rule-refs"
    );
    
    private final ObjectMapper yamlMapper;
    private final Yaml snakeYaml;
    
    public OrderedYamlParser() {
        this.yamlMapper = createYamlMapper();
        this.snakeYaml = new Yaml();
    }
    
    /**
     * Parse YAML file preserving section order.
     * 
     * @param filePath Path to YAML file
     * @return OrderedYamlConfiguration with preserved section order
     * @throws YamlConfigurationException if parsing fails
     */
    public OrderedYamlConfiguration parseFile(String filePath) throws YamlConfigurationException {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                throw new YamlConfigurationException("Configuration file not found: " + filePath);
            }
            
            logger.info("Parsing YAML file with order preservation: " + filePath);
            
            String rawContent = Files.readString(path);
            return parseYamlString(rawContent, filePath);
            
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to read YAML file: " + filePath, e);
        }
    }
    
    /**
     * Parse YAML string preserving section order.
     * 
     * @param yamlContent YAML content as string
     * @return OrderedYamlConfiguration with preserved section order
     * @throws YamlConfigurationException if parsing fails
     */
    public OrderedYamlConfiguration parseYamlString(String yamlContent) throws YamlConfigurationException {
        return parseYamlString(yamlContent, "<string>");
    }
    
    /**
     * Parse YAML string preserving section order with source identification.
     * 
     * @param yamlContent YAML content as string
     * @param source Source identifier for error reporting
     * @return OrderedYamlConfiguration with preserved section order
     * @throws YamlConfigurationException if parsing fails
     */
    public OrderedYamlConfiguration parseYamlString(String yamlContent, String source) throws YamlConfigurationException {
        try {
            logger.debug("Parsing YAML content with order preservation from: " + source);

            // Step 1: Parse with SnakeYAML to get ordered structure
            Map<String, Object> orderedMap = snakeYaml.load(yamlContent);
            if (orderedMap == null) {
                throw new YamlConfigurationException("Empty or invalid YAML content in: " + source);
            }

            // Step 2: Extract section order from the ordered map
            List<String> sectionOrder = extractSectionOrder(orderedMap);
            logger.debug("Detected section order: " + sectionOrder);

            // Step 3: Extract item-level order from the ordered map
            List<ProcessingItem> itemOrder = extractItemOrder(orderedMap);
            logger.debug("Detected item order: " + itemOrder.size() + " items");

            // Step 4: Parse with Jackson for full object mapping
            YamlRuleConfiguration config = yamlMapper.readValue(yamlContent, YamlRuleConfiguration.class);

            // Step 4.5: Merge numbered suffix sections into base sections
            mergeNumberedSections(orderedMap, config);

            // Step 5: Create ordered configuration with both section and item order
            OrderedYamlConfiguration orderedConfig = new OrderedYamlConfiguration(config, sectionOrder, itemOrder);

            logger.info("Successfully parsed YAML with preserved order from: " + source +
                       " (sections: " + sectionOrder.size() + ", items: " + itemOrder.size() + ")");

            return orderedConfig;

        } catch (org.yaml.snakeyaml.scanner.ScannerException e) {
            throw new YamlConfigurationException("YAML syntax error in " + source + ": " + e.getMessage(), e);
        } catch (org.yaml.snakeyaml.parser.ParserException e) {
            throw new YamlConfigurationException("YAML parsing error in " + source + ": " + e.getMessage(), e);
        } catch (org.yaml.snakeyaml.constructor.ConstructorException e) {
            throw new YamlConfigurationException("YAML construction error in " + source + ": " + e.getMessage(), e);
        } catch (IOException e) {
            throw new YamlConfigurationException("Failed to parse YAML content from: " + source, e);
        }
    }
    
    /**
     * Normalize section name by removing numeric suffix.
     * Examples: "enrichments-1" -> "enrichments", "rules-2" -> "rules"
     *
     * @param sectionName Section name (possibly with numeric suffix)
     * @return Normalized section name without suffix
     */
    private String normalizeSectionName(String sectionName) {
        if (sectionName == null) {
            return null;
        }

        // Check if section name ends with "-<number>"
        if (sectionName.matches(".*-\\d+$")) {
            String baseName = sectionName.replaceAll("-\\d+$", "");
            // Only normalize if the base name is a known section that supports numbering
            if (NUMBERED_SUFFIX_SECTIONS.contains(baseName)) {
                logger.debug("Normalized section name: " + sectionName + " -> " + baseName);
                return baseName;
            }
        }

        return sectionName;
    }

    /**
     * Merge numbered suffix sections into base sections in document order.
     * For example, merge enrichments-1, enrichments-2, enrichments-3 into enrichments list.
     *
     * @param orderedMap The ordered YAML map from SnakeYAML
     * @param config The configuration object to update
     */
    private void mergeNumberedSections(Map<String, Object> orderedMap, YamlRuleConfiguration config) {
        // Track items to merge for each base section
        Map<String, List<Object>> itemsToMerge = new LinkedHashMap<>();

        // Iterate through YAML sections in document order
        for (String sectionName : orderedMap.keySet()) {
            String normalizedName = normalizeSectionName(sectionName);

            // Check if this is a numbered section
            if (!sectionName.equals(normalizedName) && NUMBERED_SUFFIX_SECTIONS.contains(normalizedName)) {
                Object sectionValue = orderedMap.get(sectionName);

                if (sectionValue instanceof List) {
                    // Add items to merge list for this base section
                    itemsToMerge.computeIfAbsent(normalizedName, k -> new ArrayList<>())
                               .addAll((List<?>) sectionValue);

                    logger.info("Found numbered section '" + sectionName + "' with " +
                               ((List<?>) sectionValue).size() + " items to merge into '" + normalizedName + "'");
                }
            }
        }

        // Now merge the collected items into the config object
        for (Map.Entry<String, List<Object>> entry : itemsToMerge.entrySet()) {
            String baseSectionName = entry.getKey();
            List<Object> itemsToAdd = entry.getValue();

            if (itemsToAdd.isEmpty()) {
                continue;
            }

            logger.info("Merging " + itemsToAdd.size() + " items into section '" + baseSectionName + "'");

            // Use Jackson to convert raw objects to typed objects and merge
            switch (baseSectionName) {
                case "enrichments":
                    mergeEnrichments(config, itemsToAdd);
                    break;
                case "rules":
                    mergeRules(config, itemsToAdd);
                    break;
                case "enrichment-groups":
                    mergeEnrichmentGroups(config, itemsToAdd);
                    break;
                case "rule-groups":
                    mergeRuleGroups(config, itemsToAdd);
                    break;
                case "transformations":
                    mergeTransformations(config, itemsToAdd);
                    break;
                case "rule-chains":
                    mergeRuleChains(config, itemsToAdd);
                    break;
                case "enrichment-refs":
                    mergeEnrichmentRefs(config, itemsToAdd);
                    break;
                case "rule-refs":
                    mergeRuleRefs(config, itemsToAdd);
                    break;
                default:
                    logger.warn("Unknown base section for merging: " + baseSectionName);
            }
        }
    }

    /**
     * Check if a section name is a known section (with or without numeric suffix).
     *
     * @param sectionName Section name to check
     * @return true if known section, false otherwise
     */
    private boolean isKnownSection(String sectionName) {
        String normalized = normalizeSectionName(sectionName);
        return KNOWN_SECTIONS.contains(normalized);
    }

    /**
     * Extract the order of sections as they appear in the YAML document.
     *
     * @param yamlMap Ordered map from SnakeYAML parsing
     * @return List of section names in document order
     */
    private List<String> extractSectionOrder(Map<String, Object> yamlMap) {
        List<String> sectionOrder = new ArrayList<>();

        // LinkedHashMap from SnakeYAML preserves insertion order
        for (String key : yamlMap.keySet()) {
            if (isKnownSection(key)) {
                sectionOrder.add(key);
                logger.debug("Found section in order: " + key);
            } else {
                logger.warn("Unknown YAML section encountered: " + key);
                // Still include unknown sections to preserve complete order
                sectionOrder.add(key);
            }
        }

        return sectionOrder;
    }

    /**
     * Extract item-level order from YAML document.
     * This captures the order of individual items (enrichments, rules, groups, etc.)
     * as they appear in the YAML document, enabling item-level sequential processing.
     *
     * <p>This method processes list sections (enrichments, rules, enrichment-groups, etc.)
     * and extracts the ID of each item to create a complete ordering of all processable
     * items in the document.
     *
     * <p>Note: Single-object sections like 'pipeline' are not included in item order
     * as they are processed at section-level only.
     *
     * @param yamlMap Ordered map from SnakeYAML parsing
     * @return List of processing items in document order
     */
    private List<ProcessingItem> extractItemOrder(Map<String, Object> yamlMap) {
        List<ProcessingItem> itemOrder = new ArrayList<>();

        // Sections that contain lists of items
        Set<String> LIST_SECTIONS = Set.of(
            "enrichments", "rules", "enrichment-groups", "rule-groups",
            "transformations", "rule-chains"
        );

        // Reference sections that need placeholders for later expansion
        Set<String> REFERENCE_SECTIONS = Set.of(
            "enrichment-refs", "rule-refs"
        );

        // Note: 'pipeline' is currently a single object (not a list) in YamlRuleConfiguration
        // and is processed at section-level, not item-level. If pipeline becomes a list in
        // the future (to support multiple pipelines per document), add it to LIST_SECTIONS.

        // LinkedHashMap from SnakeYAML preserves insertion order
        for (String sectionName : yamlMap.keySet()) {
            // Normalize section name to handle numbered suffixes (e.g., enrichments-1 -> enrichments)
            String normalizedSectionName = normalizeSectionName(sectionName);

            if (!KNOWN_SECTIONS.contains(normalizedSectionName)) {
                continue; // Skip unknown sections
            }

            Object sectionValue = yamlMap.get(sectionName);

            if (LIST_SECTIONS.contains(normalizedSectionName) && sectionValue instanceof List) {
                // Process list sections (enrichments, rules, etc.)
                // Store normalized section type in ProcessingItem for consistency
                List<?> items = (List<?>) sectionValue;
                for (Object item : items) {
                    if (item instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) item;
                        String itemId = (String) itemMap.get("id");
                        if (itemId != null) {
                            // Extract metadata for better reporting
                            String itemType = (String) itemMap.get("type");
                            String itemName = (String) itemMap.get("name");

                            // Use normalized section name so ProcessingItem always has base type
                            itemOrder.add(new ProcessingItem(normalizedSectionName, itemId, itemType, itemName));
                            logger.debug("Found item in order: " + sectionName + " (normalized: " +
                                       normalizedSectionName + ") -> " + itemId +
                                       (itemType != null ? " [" + itemType + "]" : ""));
                        } else {
                            logger.warn("Item in section '" + sectionName + "' has no ID");
                        }
                    }
                }
            } else if (REFERENCE_SECTIONS.contains(normalizedSectionName) && sectionValue instanceof List) {
                // Insert placeholder for reference sections
                // These will be expanded later by YamlConfigurationLoader after loading referenced files
                itemOrder.add(new ProcessingItem(normalizedSectionName, "*"));
                logger.debug("Added placeholder for reference section: " + sectionName);
            }
            // Single-object sections (like 'pipeline') are not included in item order
            // They are processed at section-level only
        }

        logger.info("Extracted " + itemOrder.size() + " items in document order");
        return itemOrder;
    }
    
    /**
     * Create and configure the YAML ObjectMapper.
     * Uses same configuration as YamlConfigurationLoader for compatibility.
     *
     * @return Configured ObjectMapper for YAML processing
     */
    private ObjectMapper createYamlMapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

        ObjectMapper mapper = new ObjectMapper(yamlFactory);

        // Configure mapper for better handling of missing properties
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        return mapper;
    }

    /**
     * Merge enrichments from numbered sections into the main enrichments list.
     */
    private void mergeEnrichments(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlEnrichment> existingEnrichments = config.getEnrichments();
        if (existingEnrichments == null) {
            existingEnrichments = new ArrayList<>();
            config.setEnrichments(existingEnrichments);
        }

        for (Object item : itemsToAdd) {
            YamlEnrichment enrichment = mapper.convertValue(item, YamlEnrichment.class);
            existingEnrichments.add(enrichment);
        }

        logger.info("Merged " + itemsToAdd.size() + " enrichments (total now: " + existingEnrichments.size() + ")");
    }

    /**
     * Merge rules from numbered sections into the main rules list.
     */
    private void mergeRules(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlRule> existingRules = config.getRules();
        if (existingRules == null) {
            existingRules = new ArrayList<>();
            config.setRules(existingRules);
        }

        for (Object item : itemsToAdd) {
            YamlRule rule = mapper.convertValue(item, YamlRule.class);
            existingRules.add(rule);
        }

        logger.info("Merged " + itemsToAdd.size() + " rules (total now: " + existingRules.size() + ")");
    }

    /**
     * Merge enrichment-groups from numbered sections into the main enrichment-groups list.
     */
    private void mergeEnrichmentGroups(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlEnrichmentGroup> existingGroups = config.getEnrichmentGroups();
        if (existingGroups == null) {
            existingGroups = new ArrayList<>();
            config.setEnrichmentGroups(existingGroups);
        }

        for (Object item : itemsToAdd) {
            YamlEnrichmentGroup group = mapper.convertValue(item, YamlEnrichmentGroup.class);
            existingGroups.add(group);
        }

        logger.info("Merged " + itemsToAdd.size() + " enrichment-groups (total now: " + existingGroups.size() + ")");
    }

    /**
     * Merge rule-groups from numbered sections into the main rule-groups list.
     */
    private void mergeRuleGroups(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlRuleGroup> existingGroups = config.getRuleGroups();
        if (existingGroups == null) {
            existingGroups = new ArrayList<>();
            config.setRuleGroups(existingGroups);
        }

        for (Object item : itemsToAdd) {
            YamlRuleGroup group = mapper.convertValue(item, YamlRuleGroup.class);
            existingGroups.add(group);
        }

        logger.info("Merged " + itemsToAdd.size() + " rule-groups (total now: " + existingGroups.size() + ")");
    }

    /**
     * Merge transformations from numbered sections into the main transformations list.
     */
    private void mergeTransformations(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlTransformation> existingTransformations = config.getTransformations();
        if (existingTransformations == null) {
            existingTransformations = new ArrayList<>();
            config.setTransformations(existingTransformations);
        }

        for (Object item : itemsToAdd) {
            YamlTransformation transformation = mapper.convertValue(item, YamlTransformation.class);
            existingTransformations.add(transformation);
        }

        logger.info("Merged " + itemsToAdd.size() + " transformations (total now: " + existingTransformations.size() + ")");
    }

    /**
     * Merge rule-chains from numbered sections into the main rule-chains list.
     */
    private void mergeRuleChains(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlRuleChain> existingChains = config.getRuleChains();
        if (existingChains == null) {
            existingChains = new ArrayList<>();
            config.setRuleChains(existingChains);
        }

        for (Object item : itemsToAdd) {
            YamlRuleChain chain = mapper.convertValue(item, YamlRuleChain.class);
            existingChains.add(chain);
        }

        logger.info("Merged " + itemsToAdd.size() + " rule-chains (total now: " + existingChains.size() + ")");
    }

    /**
     * Merge enrichment-refs from numbered sections into the main enrichment-refs list.
     */
    private void mergeEnrichmentRefs(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlEnrichmentRef> existingRefs = config.getEnrichmentRefs();
        if (existingRefs == null) {
            existingRefs = new ArrayList<>();
            config.setEnrichmentRefs(existingRefs);
        }

        for (Object item : itemsToAdd) {
            YamlEnrichmentRef ref = mapper.convertValue(item, YamlEnrichmentRef.class);
            existingRefs.add(ref);
        }

        logger.info("Merged " + itemsToAdd.size() + " enrichment-refs (total now: " + existingRefs.size() + ")");
    }

    /**
     * Merge rule-refs from numbered sections into the main rule-refs list.
     */
    private void mergeRuleRefs(YamlRuleConfiguration config, List<Object> itemsToAdd) {
        ObjectMapper mapper = createYamlMapper();
        List<YamlRuleRef> existingRefs = config.getRuleRefs();
        if (existingRefs == null) {
            existingRefs = new ArrayList<>();
            config.setRuleRefs(existingRefs);
        }

        for (Object item : itemsToAdd) {
            YamlRuleRef ref = mapper.convertValue(item, YamlRuleRef.class);
            existingRefs.add(ref);
        }

        logger.info("Merged " + itemsToAdd.size() + " rule-refs (total now: " + existingRefs.size() + ")");
    }
}
