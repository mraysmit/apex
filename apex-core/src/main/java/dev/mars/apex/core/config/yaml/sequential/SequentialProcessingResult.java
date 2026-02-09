package dev.mars.apex.core.config.yaml.sequential;

import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.config.yaml.sequential.OrderedYamlConfiguration.ProcessingMode;
import dev.mars.apex.core.config.yaml.sequential.ProcessingContext.ProcessingError;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Result of sequential YAML processing.
 * 
 * Contains the processed configuration, processing statistics, and any errors
 * encountered during sequential processing. This result provides comprehensive
 * information about the processing outcome for debugging and validation.
 * 
 * Key Features:
 * - Processed configuration access
 * - Processing mode and order information
 * - Error reporting and statistics
 * - Performance metrics
 * - Success/failure determination
 * 
 * @author APEX Sequential Processing Implementation
 * @since 2025-10-29
 * @version 1.0
 */
public class SequentialProcessingResult {
    
    private final OrderedYamlConfiguration configuration;
    private final String source;
    private final ProcessingMode mode;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Duration processingDuration;
    
    // Processing results
    private final List<String> processedSections;
    private final Map<String, LocalDateTime> sectionTimestamps;
    private final List<ProcessingError> errors;
    private final int sectionsProcessed;
    private final int errorsEncountered;
    
    // Success determination
    private final boolean successful;
    
    /**
     * Create sequential processing result from processing context.
     * 
     * @param context Processing context with results
     */
    public SequentialProcessingResult(ProcessingContext context) {
        this.configuration = context.getConfiguration();
        this.source = context.getSource();
        this.mode = context.getMode();
        this.startTime = context.getStartTime();
        this.endTime = LocalDateTime.now();
        this.processingDuration = Duration.between(startTime, endTime);
        
        this.processedSections = context.getProcessedSections();
        this.sectionTimestamps = context.getSectionTimestamps();
        this.errors = context.getErrors();
        this.sectionsProcessed = context.getSectionsProcessed();
        this.errorsEncountered = context.getErrorsEncountered();
        
        // Determine success - no errors and at least one section processed
        this.successful = !context.hasErrors() && sectionsProcessed > 0;
    }
    
    /**
     * Check if processing was successful.
     * 
     * @return true if processing completed without errors
     */
    public boolean isSuccessful() {
        return successful;
    }
    
    /**
     * Check if processing encountered errors.
     * 
     * @return true if errors were encountered
     */
    public boolean hasErrors() {
        return errorsEncountered > 0;
    }
    
    /**
     * Get the processed configuration.
     * 
     * @return Ordered YAML configuration
     */
    public OrderedYamlConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * Get the underlying YamlRuleConfiguration.
     * 
     * @return YAML rule configuration
     */
    public YamlRuleConfiguration getYamlRuleConfiguration() {
        return configuration.getConfiguration();
    }
    
    /**
     * Get processing mode used.
     * 
     * @return Processing mode (STANDARD or SEQUENTIAL)
     */
    public ProcessingMode getProcessingMode() {
        return mode;
    }
    
    /**
     * Get section processing order.
     * 
     * @return List of section names in processing order
     */
    public List<String> getProcessedSections() {
        return processedSections;
    }
    
    /**
     * Get section processing timestamps.
     * 
     * @return Map of section names to processing timestamps
     */
    public Map<String, LocalDateTime> getSectionTimestamps() {
        return sectionTimestamps;
    }
    
    /**
     * Get processing errors.
     * 
     * @return List of processing errors
     */
    public List<ProcessingError> getErrors() {
        return errors;
    }
    
    /**
     * Get source identifier.
     * 
     * @return Source identifier
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Get processing start time.
     * 
     * @return Start time
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * Get processing end time.
     * 
     * @return End time
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    /**
     * Get processing duration.
     * 
     * @return Processing duration
     */
    public Duration getProcessingDuration() {
        return processingDuration;
    }
    
    /**
     * Get number of sections processed.
     * 
     * @return Sections processed count
     */
    public int getSectionsProcessed() {
        return sectionsProcessed;
    }
    
    /**
     * Get number of errors encountered.
     * 
     * @return Errors encountered count
     */
    public int getErrorsEncountered() {
        return errorsEncountered;
    }
    
    /**
     * Get processing summary for logging and debugging.
     * 
     * @return Processing summary string
     */
    public String getProcessingSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Sequential Processing Result for: ").append(source).append("\n");
        summary.append("Mode: ").append(mode).append("\n");
        summary.append("Success: ").append(successful).append("\n");
        summary.append("Duration: ").append(processingDuration.toMillis()).append("ms\n");
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
    
    /**
     * Get detailed processing report.
     * 
     * @return Detailed processing report
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== SEQUENTIAL PROCESSING DETAILED REPORT ===\n");
        report.append("Source: ").append(source).append("\n");
        report.append("Processing Mode: ").append(mode).append("\n");
        report.append("Start Time: ").append(startTime).append("\n");
        report.append("End Time: ").append(endTime).append("\n");
        report.append("Duration: ").append(processingDuration.toMillis()).append("ms\n");
        report.append("Success: ").append(successful).append("\n");
        report.append("\n");
        
        report.append("=== SECTION PROCESSING ORDER ===\n");
        if (mode == ProcessingMode.SEQUENTIAL) {
            report.append("Document Order (SEQUENTIAL mode - respects YAML order):\n");
        } else {
            report.append("Hardcoded Order (STANDARD mode - backward compatible):\n");
        }
        
        for (int i = 0; i < processedSections.size(); i++) {
            String section = processedSections.get(i);
            LocalDateTime timestamp = sectionTimestamps.get(section);
            report.append(String.format("%d. %s (processed at: %s)\n", i + 1, section, timestamp));
        }
        report.append("\n");
        
        if (hasErrors()) {
            report.append("=== PROCESSING ERRORS ===\n");
            for (ProcessingError error : errors) {
                report.append("Section: ").append(error.getSectionName()).append("\n");
                report.append("Error: ").append(error.getError()).append("\n");
                report.append("Timestamp: ").append(error.getTimestamp()).append("\n");
                if (error.getException() != null) {
                    report.append("Exception: ").append(error.getException().getMessage()).append("\n");
                }
                report.append("\n");
            }
        }
        
        report.append("=== CONFIGURATION SUMMARY ===\n");
        report.append("Total sections in document: ").append(configuration.getSectionOrder().size()).append("\n");
        report.append("Populated sections: ").append(configuration.getPopulatedSections().size()).append("\n");
        report.append("Document section order: ").append(configuration.getSectionOrder()).append("\n");
        report.append("Populated sections: ").append(configuration.getPopulatedSections()).append("\n");
        
        return report.toString();
    }
    
    @Override
    public String toString() {
        return "SequentialProcessingResult{" +
               "source='" + source + '\'' +
               ", mode=" + mode +
               ", successful=" + successful +
               ", sectionsProcessed=" + sectionsProcessed +
               ", errorsEncountered=" + errorsEncountered +
               ", duration=" + processingDuration.toMillis() + "ms" +
               '}';
    }
}
