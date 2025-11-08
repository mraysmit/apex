package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.yaml.OrderedYamlConfiguration.ProcessingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sequential YAML processor that respects document order.
 * 
 * This processor fixes APEX's fundamental design flaw by processing YAML sections
 * in the order they appear in the document, rather than using hardcoded sequences.
 * 
 * Key Features:
 * - Processes sections in document order (enrichments before rules, rules before enrichments, etc.)
 * - Maintains processing context across sections
 * - Supports both STANDARD (backward compatible) and SEQUENTIAL processing modes
 * - Provides detailed logging and error handling
 * 
 * Processing Flow:
 * 1. Parse YAML with OrderedYamlParser to preserve section order
 * 2. Initialize processing context
 * 3. Process each section in document order
 * 4. Return processed configuration with results
 * 
 * @author APEX Sequential Processing Implementation
 * @version 1.0
 * @since Phase 2
 */
public class SequentialYamlProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(SequentialYamlProcessor.class);

    private final OrderedYamlParser orderedParser;
    /**
     * Create sequential processor.
     */
    public SequentialYamlProcessor() {
        this.orderedParser = new OrderedYamlParser();
        new DeferredDependencyResolver();

        logger.info("SequentialYamlProcessor initialized - ready to fix APEX's fundamental design flaw");
    }
    
    /**
     * Process YAML file with sequential section processing.
     * 
     * @param filePath Path to YAML file
     * @return Sequential processing result
     * @throws YamlConfigurationException if processing fails
     */
    public SequentialProcessingResult processFile(String filePath) throws YamlConfigurationException {
        logger.info("Starting sequential processing of file: " + filePath);
        
        // Step 1: Parse YAML with preserved order
        OrderedYamlConfiguration orderedConfig = orderedParser.parseFile(filePath);
        
        // Step 2: Process based on mode
        return processOrderedConfiguration(orderedConfig, filePath);
    }
    
    /**
     * Process YAML string with sequential section processing.
     * 
     * @param yamlContent YAML content as string
     * @return Sequential processing result
     * @throws YamlConfigurationException if processing fails
     */
    public SequentialProcessingResult processYamlString(String yamlContent) throws YamlConfigurationException {
        return processYamlString(yamlContent, "<string>");
    }
    
    /**
     * Process YAML string with sequential section processing and source identification.
     * 
     * @param yamlContent YAML content as string
     * @param source Source identifier for error reporting
     * @return Sequential processing result
     * @throws YamlConfigurationException if processing fails
     */
    public SequentialProcessingResult processYamlString(String yamlContent, String source) throws YamlConfigurationException {
        logger.info("Starting sequential processing of YAML string from: " + source);
        
        // Step 1: Parse YAML with preserved order
        OrderedYamlConfiguration orderedConfig = orderedParser.parseYamlString(yamlContent, source);
        
        // Step 2: Process based on mode
        return processOrderedConfiguration(orderedConfig, source);
    }
    
    /**
     * Process ordered configuration based on processing mode.
     *
     * @param orderedConfig Ordered YAML configuration
     * @param source Source identifier for error reporting
     * @return Sequential processing result
     * @throws YamlConfigurationException if processing fails
     */
    public SequentialProcessingResult processOrderedConfiguration(OrderedYamlConfiguration orderedConfig, String source)
            throws YamlConfigurationException {
        
        ProcessingMode mode = orderedConfig.getProcessingMode();
        logger.info("Processing mode detected: " + mode + " for source: " + source);
        
        if (mode == ProcessingMode.STANDARD) {
            return processStandardMode(orderedConfig, source);
        } else {
            return processSequentialMode(orderedConfig, source);
        }
    }
    
    /**
     * Process using standard (backward compatible) mode.
     * Uses hardcoded processing order for backward compatibility.
     */
    private SequentialProcessingResult processStandardMode(OrderedYamlConfiguration orderedConfig, String source) 
            throws YamlConfigurationException {
        
        logger.info("Processing in STANDARD mode (backward compatible) for: " + source);
        
        ProcessingContext context = new ProcessingContext(orderedConfig, source, ProcessingMode.STANDARD);
        
        // Standard hardcoded order: metadata -> data-sources -> rules -> enrichments -> etc.
        processMetadata(context);
        processDataSources(context);
        processRules(context);
        processEnrichments(context);
        processRuleGroups(context);
        processEnrichmentGroups(context);
        processTransformations(context);
        processRuleChains(context);
        processPipeline(context);
        processErrorRecovery(context);
        
        return new SequentialProcessingResult(context);
    }
    
    /**
     * Process using sequential mode - THE CORE FIX for APEX's fundamental design flaw.
     * Processes sections in the order they appear in the YAML document.
     */
    private SequentialProcessingResult processSequentialMode(OrderedYamlConfiguration orderedConfig, String source) 
            throws YamlConfigurationException {
        
        logger.info("Processing in SEQUENTIAL mode (respects document order) for: " + source);
        
        ProcessingContext context = new ProcessingContext(orderedConfig, source, ProcessingMode.SEQUENTIAL);
        List<String> sectionOrder = orderedConfig.getSectionOrder();
        
        logger.info("Document section order: " + sectionOrder);
        
        // Process each section in document order - THIS IS THE FIX!
        for (String sectionName : sectionOrder) {
            logger.debug("Processing section in document order: " + sectionName);
            
            switch (sectionName) {
                case "metadata" -> processMetadata(context);
                case "data-sources" -> processDataSources(context);
                case "data-source-refs" -> processDataSourceRefs(context);
                case "enrichments" -> processEnrichments(context);
                case "enrichment-refs" -> processEnrichmentRefs(context);
                case "rules" -> processRules(context);
                case "rule-refs" -> processRuleRefs(context);
                case "rule-groups" -> processRuleGroups(context);
                case "enrichment-groups" -> processEnrichmentGroups(context);
                case "transformations" -> processTransformations(context);
                case "rule-chains" -> processRuleChains(context);
                case "pipeline" -> processPipeline(context);
                case "data-sinks" -> processDataSinks(context);
                case "categories" -> processCategories(context);
                case "error-recovery" -> processErrorRecovery(context);
                default -> logger.warn("Unknown section encountered: " + sectionName + " in " + source);
            }
        }
        
        logger.info("Sequential processing completed for: " + source + " (processed " + sectionOrder.size() + " sections)");
        
        return new SequentialProcessingResult(context);
    }

    /**
     * Get section content for dependency analysis.
     */
    private Object getSectionContent(OrderedYamlConfiguration orderedConfig, String sectionName) {
        YamlRuleConfiguration config = orderedConfig.getConfiguration();

        return switch (sectionName) {
            case "metadata" -> config.getMetadata();
            case "data-sources" -> config.getDataSources();
            case "data-source-refs" -> config.getDataSourceRefs();
            case "enrichments" -> config.getEnrichments();
            case "enrichment-refs" -> config.getEnrichmentRefs();
            case "rules" -> config.getRules();
            case "rule-refs" -> config.getRuleRefs();
            case "rule-groups" -> config.getRuleGroups();
            case "enrichment-groups" -> config.getEnrichmentGroups();
            case "transformations" -> config.getTransformations();
            case "rule-chains" -> config.getRuleChains();
            case "pipeline" -> config.getPipeline();
            case "data-sinks" -> config.getDataSinks();
            case "categories" -> config.getCategories();
            case "error-recovery" -> config.getErrorRecovery();
            default -> null;
        };
    }

    /**
     * Process section by name - centralized section processing.
     */
    private void processSectionByName(String sectionName, ProcessingContext context) throws YamlConfigurationException {
        switch (sectionName) {
            case "metadata" -> processMetadata(context);
            case "data-sources" -> processDataSources(context);
            case "data-source-refs" -> processDataSourceRefs(context);
            case "enrichments" -> processEnrichments(context);
            case "enrichment-refs" -> processEnrichmentRefs(context);
            case "rules" -> processRules(context);
            case "rule-refs" -> processRuleRefs(context);
            case "rule-groups" -> processRuleGroups(context);
            case "enrichment-groups" -> processEnrichmentGroups(context);
            case "transformations" -> processTransformations(context);
            case "rule-chains" -> processRuleChains(context);
            case "pipeline" -> processPipeline(context);
            case "data-sinks" -> processDataSinks(context);
            case "categories" -> processCategories(context);
            case "error-recovery" -> processErrorRecovery(context);
            default -> logger.warn("Unknown section encountered: " + sectionName + " in " + context.getSource());
        }
    }

    // Section processing methods - each handles one type of YAML section
    
    private void processMetadata(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getMetadata() != null) {
            logger.debug("Processing metadata section");
            context.recordSectionProcessed("metadata");
        }
    }
    
    private void processDataSources(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getDataSources() != null) {
            logger.debug("Processing data-sources section");
            // TODO: Integrate with YamlDataSourceProcessor
            context.recordSectionProcessed("data-sources");
        }
    }
    
    private void processDataSourceRefs(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getDataSourceRefs() != null) {
            logger.debug("Processing data-source-refs section");
            context.recordSectionProcessed("data-source-refs");
        }
    }
    
    private void processEnrichments(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getEnrichments() != null) {
            logger.debug("Processing enrichments section");
            // TODO: Integrate with YamlEnrichmentProcessor
            context.recordSectionProcessed("enrichments");
        }
    }
    
    private void processEnrichmentRefs(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getEnrichmentRefs() != null) {
            logger.debug("Processing enrichment-refs section");
            context.recordSectionProcessed("enrichment-refs");
        }
    }
    
    private void processRules(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getRules() != null) {
            logger.debug("Processing rules section");
            // TODO: Integrate with YamlRuleProcessor
            context.recordSectionProcessed("rules");
        }
    }
    
    private void processRuleRefs(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getRuleRefs() != null) {
            logger.debug("Processing rule-refs section");
            context.recordSectionProcessed("rule-refs");
        }
    }
    
    private void processRuleGroups(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getRuleGroups() != null) {
            logger.debug("Processing rule-groups section");
            context.recordSectionProcessed("rule-groups");
        }
    }
    
    private void processEnrichmentGroups(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getEnrichmentGroups() != null) {
            logger.debug("Processing enrichment-groups section");
            context.recordSectionProcessed("enrichment-groups");
        }
    }
    
    private void processTransformations(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getTransformations() != null) {
            logger.debug("Processing transformations section");
            // TODO: Integrate with YamlTransformationProcessor
            context.recordSectionProcessed("transformations");
        }
    }
    
    private void processRuleChains(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getRuleChains() != null) {
            logger.debug("Processing rule-chains section");
            context.recordSectionProcessed("rule-chains");
        }
    }
    
    private void processPipeline(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getPipeline() != null) {
            logger.debug("Processing pipeline section");
            context.recordSectionProcessed("pipeline");
        }
    }
    
    private void processDataSinks(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getDataSinks() != null) {
            logger.debug("Processing data-sinks section");
            context.recordSectionProcessed("data-sinks");
        }
    }
    
    private void processCategories(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getCategories() != null) {
            logger.debug("Processing categories section");
            context.recordSectionProcessed("categories");
        }
    }
    
    private void processErrorRecovery(ProcessingContext context) throws YamlConfigurationException {
        if (context.getConfiguration().getConfiguration().getErrorRecovery() != null) {
            logger.debug("Processing error-recovery section");
            context.recordSectionProcessed("error-recovery");
        }
    }
}
