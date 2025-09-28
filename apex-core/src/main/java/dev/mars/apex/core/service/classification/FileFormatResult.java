package dev.mars.apex.core.service.classification;

import java.util.Objects;

/**
 * Result of file format detection operations.
 * Contains the detected format and confidence level.
 * 
 * <p>This class represents the outcome of file format detection including:
 * <ul>
 *   <li>Detected format (JSON, XML, CSV, etc.)</li>
 *   <li>Confidence level (0.0 to 1.0)</li>
 *   <li>Detection method used</li>
 *   <li>Additional format-specific metadata</li>
 * </ul>
 * 
 * @since 1.0
 */
public final class FileFormatResult {
    
    /**
     * Common file format constants.
     */
    public static final String FORMAT_JSON = "JSON";
    public static final String FORMAT_XML = "XML";
    public static final String FORMAT_CSV = "CSV";
    public static final String FORMAT_TEXT = "TEXT";
    public static final String FORMAT_UNKNOWN = "UNKNOWN";
    
    /**
     * Detection method constants.
     */
    public static final String METHOD_EXTENSION = "EXTENSION";
    public static final String METHOD_CONTENT = "CONTENT";
    public static final String METHOD_HYBRID = "HYBRID";
    
    private final String format;
    private final double confidence;
    private final String detectionMethod;
    private final String details;
    
    private FileFormatResult(String format, double confidence, String detectionMethod, String details) {
        this.format = format != null ? format : FORMAT_UNKNOWN;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.detectionMethod = detectionMethod != null ? detectionMethod : METHOD_EXTENSION;
        this.details = details != null ? details : "";
    }
    
    /**
     * Creates a new FileFormatResult.
     * 
     * @param format detected format
     * @param confidence confidence level (0.0 to 1.0)
     * @return new FileFormatResult
     */
    public static FileFormatResult of(String format, double confidence) {
        return new FileFormatResult(format, confidence, METHOD_EXTENSION, "");
    }
    
    /**
     * Creates a new FileFormatResult with detection method.
     * 
     * @param format detected format
     * @param confidence confidence level (0.0 to 1.0)
     * @param detectionMethod detection method used
     * @return new FileFormatResult
     */
    public static FileFormatResult of(String format, double confidence, String detectionMethod) {
        return new FileFormatResult(format, confidence, detectionMethod, "");
    }
    
    /**
     * Creates a new FileFormatResult with full details.
     * 
     * @param format detected format
     * @param confidence confidence level (0.0 to 1.0)
     * @param detectionMethod detection method used
     * @param details additional details
     * @return new FileFormatResult
     */
    public static FileFormatResult of(String format, double confidence, String detectionMethod, String details) {
        return new FileFormatResult(format, confidence, detectionMethod, details);
    }
    
    /**
     * Creates an unknown format result.
     * 
     * @return FileFormatResult for unknown format
     */
    public static FileFormatResult unknown() {
        return new FileFormatResult(FORMAT_UNKNOWN, 0.0, METHOD_EXTENSION, "No format detected");
    }
    
    /**
     * Creates a JSON format result.
     * 
     * @param confidence confidence level
     * @return FileFormatResult for JSON
     */
    public static FileFormatResult json(double confidence) {
        return new FileFormatResult(FORMAT_JSON, confidence, METHOD_CONTENT, "Valid JSON structure detected");
    }
    
    /**
     * Creates an XML format result.
     * 
     * @param confidence confidence level
     * @return FileFormatResult for XML
     */
    public static FileFormatResult xml(double confidence) {
        return new FileFormatResult(FORMAT_XML, confidence, METHOD_CONTENT, "Valid XML structure detected");
    }
    
    /**
     * Creates a CSV format result.
     * 
     * @param confidence confidence level
     * @return FileFormatResult for CSV
     */
    public static FileFormatResult csv(double confidence) {
        return new FileFormatResult(FORMAT_CSV, confidence, METHOD_CONTENT, "CSV structure detected");
    }
    
    /**
     * Gets the detected format.
     * 
     * @return format string
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Gets the confidence level.
     * 
     * @return confidence (0.0 to 1.0)
     */
    public double getConfidence() {
        return confidence;
    }
    
    /**
     * Gets the detection method used.
     * 
     * @return detection method
     */
    public String getDetectionMethod() {
        return detectionMethod;
    }
    
    /**
     * Gets additional details about the detection.
     * 
     * @return details string
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * Checks if the format is known (not UNKNOWN).
     * 
     * @return true if format is known
     */
    public boolean isKnownFormat() {
        return !FORMAT_UNKNOWN.equals(format);
    }
    
    /**
     * Checks if confidence meets the given threshold.
     *
     * @param threshold confidence threshold
     * @return true if confidence >= threshold
     */
    public boolean meetsConfidenceThreshold(double threshold) {
        return confidence >= threshold;
    }

    /**
     * Checks if this result has high confidence (>= 0.8).
     *
     * @return true if confidence is high
     */
    public boolean isConfident() {
        return isKnownFormat() && confidence >= 0.8;
    }

    /**
     * Checks if the detection was successful (format is known).
     *
     * @return true if detection was successful
     */
    public boolean isSuccessful() {
        return isKnownFormat();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileFormatResult that = (FileFormatResult) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               Objects.equals(format, that.format) &&
               Objects.equals(detectionMethod, that.detectionMethod) &&
               Objects.equals(details, that.details);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(format, confidence, detectionMethod, details);
    }
    
    @Override
    public String toString() {
        return "FileFormatResult{" +
               "format='" + format + '\'' +
               ", confidence=" + confidence +
               ", detectionMethod='" + detectionMethod + '\'' +
               ", details='" + details + '\'' +
               '}';
    }
}
