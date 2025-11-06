package dev.mars.apex.core.config.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

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
    
    private static final Logger LOGGER = Logger.getLogger(OrderedYamlParser.class.getName());
    
    // Known YAML section names in APEX
    private static final Set<String> KNOWN_SECTIONS = Set.of(
        "metadata", "data-sources", "data-source-refs", "rule-refs", "enrichment-refs",
        "data-sinks", "categories", "rules", "rule-groups", "enrichments", 
        "enrichment-groups", "transformations", "rule-chains", "pipeline", "error-recovery"
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
            
            LOGGER.info("Parsing YAML file with order preservation: " + filePath);
            
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
            LOGGER.fine("Parsing YAML content with order preservation from: " + source);

            // Step 1: Parse with SnakeYAML to get ordered structure
            Map<String, Object> orderedMap = snakeYaml.load(yamlContent);
            if (orderedMap == null) {
                throw new YamlConfigurationException("Empty or invalid YAML content in: " + source);
            }

            // Step 2: Extract section order from the ordered map
            List<String> sectionOrder = extractSectionOrder(orderedMap);
            LOGGER.fine("Detected section order: " + sectionOrder);

            // Step 3: Extract item-level order from the ordered map
            List<ProcessingItem> itemOrder = extractItemOrder(orderedMap);
            LOGGER.fine("Detected item order: " + itemOrder.size() + " items");

            // Step 4: Parse with Jackson for full object mapping
            YamlRuleConfiguration config = yamlMapper.readValue(yamlContent, YamlRuleConfiguration.class);

            // Step 5: Create ordered configuration with both section and item order
            OrderedYamlConfiguration orderedConfig = new OrderedYamlConfiguration(config, sectionOrder, itemOrder);

            LOGGER.info("Successfully parsed YAML with preserved order from: " + source +
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
     * Extract the order of sections as they appear in the YAML document.
     *
     * @param yamlMap Ordered map from SnakeYAML parsing
     * @return List of section names in document order
     */
    private List<String> extractSectionOrder(Map<String, Object> yamlMap) {
        List<String> sectionOrder = new ArrayList<>();

        // LinkedHashMap from SnakeYAML preserves insertion order
        for (String key : yamlMap.keySet()) {
            if (KNOWN_SECTIONS.contains(key)) {
                sectionOrder.add(key);
                LOGGER.fine("Found section in order: " + key);
            } else {
                LOGGER.warning("Unknown YAML section encountered: " + key);
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
            if (!KNOWN_SECTIONS.contains(sectionName)) {
                continue; // Skip unknown sections
            }

            Object sectionValue = yamlMap.get(sectionName);

            if (LIST_SECTIONS.contains(sectionName) && sectionValue instanceof List) {
                // Process list sections (enrichments, rules, etc.)
                List<?> items = (List<?>) sectionValue;
                for (Object item : items) {
                    if (item instanceof Map) {
                        Map<?, ?> itemMap = (Map<?, ?>) item;
                        String itemId = (String) itemMap.get("id");
                        if (itemId != null) {
                            itemOrder.add(new ProcessingItem(sectionName, itemId));
                            LOGGER.fine("Found item in order: " + sectionName + " -> " + itemId);
                        } else {
                            LOGGER.warning("Item in section '" + sectionName + "' has no ID");
                        }
                    }
                }
            } else if (REFERENCE_SECTIONS.contains(sectionName) && sectionValue instanceof List) {
                // Insert placeholder for reference sections
                // These will be expanded later by YamlConfigurationLoader after loading referenced files
                itemOrder.add(new ProcessingItem(sectionName, "*"));
                LOGGER.fine("Added placeholder for reference section: " + sectionName);
            }
            // Single-object sections (like 'pipeline') are not included in item order
            // They are processed at section-level only
        }

        LOGGER.info("Extracted " + itemOrder.size() + " items in document order");
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
}
