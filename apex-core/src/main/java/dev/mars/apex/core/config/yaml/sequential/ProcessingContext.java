package dev.mars.apex.core.config.yaml.sequential;

import dev.mars.apex.core.config.yaml.sequential.OrderedYamlConfiguration.ProcessingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Processing context for sequential YAML processing.
 * 
 * Maintains state and tracking information during sequential processing of YAML sections.
 * This context enables proper error handling, progress tracking, and debugging of the
 * sequential processing flow.
 * 
 * Key Features:
 * - Section processing tracking
 * - Error collection and reporting
 * - Processing statistics
 * - State management across sections
 * - Debugging and logging support
 * 
 * @author APEX Sequential Processing Implementation
 * @since 2025-10-29
 * @version 1.0
 */
public class ProcessingContext {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessingContext.class);
    
    private final OrderedYamlConfiguration configuration;
    private final String source;
    private final ProcessingMode mode;
    private final LocalDateTime startTime;
    
    // Processing tracking
    private final List<String> processedSections;
    private final Map<String, LocalDateTime> sectionTimestamps;
    private final List<ProcessingError> errors;
    private final Map<String, Object> processingState;
    
    // Statistics
    private int sectionsProcessed;
    private int errorsEncountered;
    
    /**
     * Create processing context for sequential YAML processing.
     * 
     * @param configuration Ordered YAML configuration
     * @param source Source identifier
     * @param mode Processing mode (STANDARD or SEQUENTIAL)
     */
    public ProcessingContext(OrderedYamlConfiguration configuration, String source, ProcessingMode mode) {
        this.configuration = configuration;
        this.source = source;
        this.mode = mode;
        this.startTime = LocalDateTime.now();
        
        this.processedSections = new ArrayList<>();
        this.sectionTimestamps = new HashMap<>();
        this.errors = new ArrayList<>();
        this.processingState = new HashMap<>();
        
        this.sectionsProcessed = 0;
        this.errorsEncountered = 0;
        
        logger.debug("ProcessingContext created for: " + source + " (mode: " + mode + ")");
    }
    
    /**
     * Record that a section has been successfully processed.
     * 
     * @param sectionName Name of the processed section
     */
    public void recordSectionProcessed(String sectionName) {
        processedSections.add(sectionName);
        sectionTimestamps.put(sectionName, LocalDateTime.now());
        sectionsProcessed++;
        
        logger.debug("Section processed: " + sectionName + " (total: " + sectionsProcessed + ")");
    }
    
    /**
     * Record a processing error.
     * 
     * @param sectionName Section where error occurred
     * @param error Error description
     * @param exception Optional exception
     */
    public void recordError(String sectionName, String error, Throwable exception) {
        ProcessingError processingError = new ProcessingError(sectionName, error, exception, LocalDateTime.now());
        errors.add(processingError);
        errorsEncountered++;
        
        logger.warn("Processing error in section '" + sectionName + "': " + error);
        if (exception != null) {
            logger.warn("Exception details: " + exception.getMessage());
        }
    }
    
    /**
     * Record a processing error without exception.
     * 
     * @param sectionName Section where error occurred
     * @param error Error description
     */
    public void recordError(String sectionName, String error) {
        recordError(sectionName, error, null);
    }
    
    /**
     * Store processing state for cross-section communication.
     * 
     * @param key State key
     * @param value State value
     */
    public void setState(String key, Object value) {
        processingState.put(key, value);
        logger.debug("Processing state set: " + key + " = " + value);
    }
    
    /**
     * Retrieve processing state.
     * 
     * @param key State key
     * @return State value or null if not found
     */
    public Object getState(String key) {
        return processingState.get(key);
    }
    
    /**
     * Retrieve processing state with type casting.
     * 
     * @param key State key
     * @param type Expected type
     * @return Typed state value or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T> T getState(String key, Class<T> type) {
        Object value = processingState.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Check if processing has errors.
     * 
     * @return true if errors were encountered
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Get processing summary for logging and debugging.
     * 
     * @return Processing summary string
     */
    public String getProcessingSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Processing Summary for: ").append(source).append("\n");
        summary.append("Mode: ").append(mode).append("\n");
        summary.append("Sections processed: ").append(sectionsProcessed).append("\n");
        summary.append("Errors encountered: ").append(errorsEncountered).append("\n");
        summary.append("Processing order: ").append(processedSections).append("\n");
        
        if (hasErrors()) {
            summary.append("Errors:\n");
            for (ProcessingError error : errors) {
                summary.append("  - ").append(error.getSectionName()).append(": ").append(error.getError()).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    // Getters
    
    public OrderedYamlConfiguration getConfiguration() {
        return configuration;
    }
    
    public String getSource() {
        return source;
    }
    
    public ProcessingMode getMode() {
        return mode;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public List<String> getProcessedSections() {
        return Collections.unmodifiableList(processedSections);
    }
    
    public Map<String, LocalDateTime> getSectionTimestamps() {
        return Collections.unmodifiableMap(sectionTimestamps);
    }
    
    public List<ProcessingError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    public int getSectionsProcessed() {
        return sectionsProcessed;
    }
    
    public int getErrorsEncountered() {
        return errorsEncountered;
    }
    
    /**
     * Processing error record.
     */
    public static class ProcessingError {
        private final String sectionName;
        private final String error;
        private final Throwable exception;
        private final LocalDateTime timestamp;
        
        public ProcessingError(String sectionName, String error, Throwable exception, LocalDateTime timestamp) {
            this.sectionName = sectionName;
            this.error = error;
            this.exception = exception;
            this.timestamp = timestamp;
        }
        
        public String getSectionName() {
            return sectionName;
        }
        
        public String getError() {
            return error;
        }
        
        public Throwable getException() {
            return exception;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "ProcessingError{" +
                   "section='" + sectionName + '\'' +
                   ", error='" + error + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
}
