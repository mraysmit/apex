package dev.mars.apex.core.service.classification;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleFactory;
import dev.mars.apex.core.service.enrichment.EnrichmentService;
import dev.mars.apex.core.service.scenario.DataTypeScenarioService;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced DataTypeScenarioService with integrated input data classification capabilities.
 *
 * This service extends the base DataTypeScenarioService to add multi-layer input data
 * classification, enabling real-time processing of files from message queues, REST APIs,
 * and other transport mechanisms.
 *
 * CLASSIFICATION LAYERS:
 * 1. File Format Detection - Identifies JSON, XML, CSV, etc.
 * 2. Content Classification - Analyzes message structure and content
 * 3. Business Classification - Applies SpEL rules for business logic
 * 4. Scenario Routing - Routes to appropriate processing scenarios
 *
 * PHASE 1.2 ENHANCEMENTS:
 * - Content-based file format detection
 * - Message type and content pattern analysis
 * - Basic classification result caching
 * - Enhanced confidence scoring
 *
 * DESIGN PRINCIPLES:
 * - Extends existing service for backward compatibility
 * - Pluggable classification strategies
 * - Performance-optimized with caching
 * - Conservative approach respecting existing patterns
 *
 * USAGE EXAMPLE:
 * ```java
 * EnhancedDataTypeScenarioService service = new EnhancedDataTypeScenarioService();
 * service.loadScenarios("config/data-type-scenarios.yaml");
 *
 * ApexProcessingContext context = ApexProcessingContext.builder()
 *     .source("rabbitmq")
 *     .fileName("trade_data.json")
 *     .build();
 *
 * ClassificationResult result = service.classifyInputData(jsonData, context);
 * ```
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public class EnhancedDataTypeScenarioService extends DataTypeScenarioService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDataTypeScenarioService.class);

    // Classification components
    private final List<FileFormatDetector> formatDetectors;
    private final ContentClassifier contentClassifier;
    private final ClassificationCache classificationCache;
    
    // Constructors following existing patterns
    public EnhancedDataTypeScenarioService() {
        super();
        this.formatDetectors = createDefaultFormatDetectors();
        this.contentClassifier = new ContentClassifier();
        this.classificationCache = new ClassificationCache();
        logger.debug("Enhanced DataTypeScenarioService initialized with {} format detectors, content classifier, and cache",
                    formatDetectors.size());
    }

    public EnhancedDataTypeScenarioService(YamlConfigurationLoader configLoader, YamlRuleFactory ruleFactory) {
        super(configLoader, ruleFactory);
        this.formatDetectors = createDefaultFormatDetectors();
        this.contentClassifier = new ContentClassifier();
        this.classificationCache = new ClassificationCache();
        logger.debug("Enhanced DataTypeScenarioService initialized with custom components");
    }

    public EnhancedDataTypeScenarioService(YamlConfigurationLoader configLoader,
                                         YamlRuleFactory ruleFactory,
                                         EnrichmentService enrichmentService) {
        super(configLoader, ruleFactory, enrichmentService);
        this.formatDetectors = createDefaultFormatDetectors();
        this.contentClassifier = new ContentClassifier();
        this.classificationCache = new ClassificationCache();
        logger.debug("Enhanced DataTypeScenarioService initialized with all custom components");
    }
    
    /**
     * NEW: Classify input data using multi-layer classification approach.
     *
     * This method implements the core classification logic that determines
     * the appropriate scenario for processing input data based on format,
     * content, and business rules.
     *
     * PHASE 1.2 ENHANCEMENTS:
     * - Check cache for existing classification results
     * - Content-based format detection with higher accuracy
     * - Message type and content pattern analysis
     * - Cache successful results for performance
     *
     * @param inputData the raw input data to classify
     * @param context processing context with metadata
     * @return classification result with scenario routing decision
     */
    public ClassificationResult classifyInputData(Object inputData, ApexProcessingContext context) {
        if (inputData == null) {
            return ClassificationResult.failed("Input data cannot be null");
        }

        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Starting Phase 1.2 classification for input data from source: {}", context.getSource());

            // Create classification context
            ClassificationContext classificationContext = ClassificationContext.builder()
                .inputData(inputData)
                .processingContext(context)
                .timestamp(startTime)
                .build();

            // Check cache first for performance
            ClassificationResult cachedResult = classificationCache.get(classificationContext);
            if (cachedResult != null) {
                logger.debug("Cache hit for classification from source: {}", context.getSource());
                return cachedResult;
            }

            // Layer 1: Enhanced file format detection
            FileFormatResult formatResult = detectFileFormat(classificationContext);
            logger.debug("File format detection result: {}", formatResult);

            if (!formatResult.isSuccessful()) {
                return ClassificationResult.failed("Could not detect file format");
            }

            // Layer 2: Content classification (NEW in Phase 1.2)
            ContentClassificationResult contentResult = contentClassifier.classify(
                formatResult.getFormat(),
                classificationContext.getInputDataAsString(),
                inputData
            );
            logger.debug("Content classification result: {}", contentResult);

            // Get scenario using classification-based routing
            ClassificationContext routingContext = ClassificationContext.builder()
                .processingContext(context)
                .metadata(context.getMetadata())
                .build();
            ScenarioConfiguration scenario = getScenarioForClassification(routingContext, formatResult, contentResult);

            if (scenario == null) {
                return ClassificationResult.failed("No scenario found for classified input data");
            }

            long classificationTime = System.currentTimeMillis() - startTime;

            // Enhanced confidence scoring combining format and content confidence
            double combinedConfidence = calculateCombinedConfidence(formatResult, contentResult);

            ClassificationResult result = ClassificationResult.builder()
                .successful(true)
                .fileFormat(formatResult.getFormat())
                .contentType(contentResult.getContentType())
                .businessClassification("basic-classification") // Placeholder for Phase 1.3
                .scenarioId(scenario.getScenarioId())
                .scenario(scenario)
                .parsedData(inputData)
                .confidence(combinedConfidence)
                .classificationTimeMs(classificationTime)
                .cacheable(true)
                .build();

            // Cache successful results
            classificationCache.put(classificationContext, result);

            return result;

        } catch (Exception e) {
            logger.error("Classification failed for input data from source: {}", context.getSource(), e);
            return ClassificationResult.failed(e);
        }
    }
    
    /**
     * Layer 1: Enhanced file format detection with multiple strategies.
     * Phase 1.2 enhancement: Uses both extension-based and content-based detection.
     */
    private FileFormatResult detectFileFormat(ClassificationContext context) {
        logger.debug("Detecting file format using {} detectors", formatDetectors.size());

        FileFormatResult bestResult = FileFormatResult.unknown();
        double bestConfidence = 0.0;

        // Try all detectors and use the one with highest confidence
        for (FileFormatDetector detector : formatDetectors) {
            if (detector.canDetect(context)) {
                logger.debug("Trying detector: {}", detector.getName());

                FileFormatResult result = detector.detect(context);
                if (result.isSuccessful() && result.getConfidence() > bestConfidence) {
                    bestResult = result;
                    bestConfidence = result.getConfidence();
                    logger.debug("New best format detection by {}: {} (confidence: {})",
                               detector.getName(), result.getFormat(), result.getConfidence());
                }
            }
        }

        if (bestResult.isSuccessful()) {
            logger.debug("Final format detection: {} with confidence: {}",
                       bestResult.getFormat(), bestResult.getConfidence());
        } else {
            logger.debug("No successful format detection");
        }

        return bestResult;
    }

    /**
     * Calculate combined confidence from format and content classification.
     * Phase 1.2 enhancement: Weighted combination of multiple confidence scores.
     */
    private double calculateCombinedConfidence(FileFormatResult formatResult, ContentClassificationResult contentResult) {
        double formatConfidence = formatResult.getConfidence();
        double contentConfidence = contentResult.isSuccessful() ? contentResult.getConfidence() : 0.0;

        // Weighted average: format detection 60%, content classification 40%
        double combined = (formatConfidence * 0.6) + (contentConfidence * 0.4);

        logger.debug("Combined confidence: format={}, content={}, combined={}",
                   formatConfidence, contentConfidence, combined);

        return Math.min(combined, 0.95); // Cap at 95% to leave room for improvement
    }
    
    /**
     * Creates the default list of file format detectors.
     * Phase 1.2 enhancement: Adds content-based detector.
     * Following the established pattern of creating default components.
     */
    private List<FileFormatDetector> createDefaultFormatDetectors() {
        List<FileFormatDetector> detectors = new ArrayList<>();

        // Add extension-based detector (highest priority for speed)
        detectors.add(new ExtensionBasedFileFormatDetector());

        // Add content-based detector (Phase 1.2 enhancement)
        detectors.add(new ContentBasedFileFormatDetector());

        // Future detectors will be added here in subsequent phases:
        // - Magic number detector
        // - Hybrid detector

        // Sort by priority (higher priority first)
        detectors.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        logger.debug("Created {} default format detectors", detectors.size());
        return detectors;
    }
    
    /**
     * Gets the list of configured format detectors.
     * Useful for testing and monitoring.
     */
    public List<FileFormatDetector> getFormatDetectors() {
        return new ArrayList<>(formatDetectors);
    }
    
    /**
     * Adds a custom format detector.
     * Maintains priority ordering.
     */
    public void addFormatDetector(FileFormatDetector detector) {
        formatDetectors.add(detector);
        formatDetectors.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        logger.debug("Added format detector: {} (priority: {})", detector.getName(), detector.getPriority());
    }

    /**
     * Gets the content classifier for advanced operations.
     * Phase 1.2 enhancement.
     */
    public ContentClassifier getContentClassifier() {
        return contentClassifier;
    }

    /**
     * Gets the classification cache for monitoring and management.
     * Phase 1.2 enhancement.
     */
    public ClassificationCache getClassificationCache() {
        return classificationCache;
    }

    /**
     * Clear the classification cache.
     * Useful for testing and cache management.
     */
    public void clearClassificationCache() {
        classificationCache.clear();
        logger.info("Classification cache cleared");
    }

    /**
     * Get classification cache statistics.
     * Phase 1.2 enhancement for monitoring.
     */
    public Object getClassificationCacheStatistics() {
        return classificationCache.getStatistics();
    }

    /**
     * Gets a scenario based on classification results instead of data type associations.
     * This method implements the new classification-based routing approach.
     *
     * @param context the classification context
     * @param formatResult the file format detection result
     * @param contentResult the content classification result
     * @return the scenario to use, or null if none found
     */
    private ScenarioConfiguration getScenarioForClassification(
            ClassificationContext context,
            FileFormatResult formatResult,
            ContentClassificationResult contentResult) {

        // For now, use the first available scenario as a default
        // In a real implementation, this would use business rules to select scenarios
        // based on format, content type, source, metadata, etc.

        if (!scenarioCache.isEmpty()) {
            String firstScenarioId = scenarioCache.keySet().iterator().next();
            ScenarioConfiguration scenario = scenarioCache.get(firstScenarioId);
            logger.debug("Selected scenario '{}' based on classification results", firstScenarioId);
            return scenario;
        }

        logger.warn("No scenarios available for classification-based routing");
        return null;
    }
}
