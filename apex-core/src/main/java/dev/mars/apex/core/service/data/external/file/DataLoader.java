package dev.mars.apex.core.service.data.external.file;

import dev.mars.apex.core.config.datasource.FileFormatConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for loading data from different file formats.
 * 
 * This interface defines the contract for data loaders that can parse
 * various file formats and convert them into structured data objects.
 * 
 * @author SpEL Rules Engine Team
 * @since 1.0.0
 * @version 1.0
 */
public interface DataLoader {
    
    /**
     * Load data from the specified file path.
     * 
     * @param filePath The path to the file to load
     * @param formatConfig The file format configuration
     * @return List of data objects loaded from the file
     * @throws IOException if file loading fails
     */
    List<Object> loadData(Path filePath, FileFormatConfig formatConfig) throws IOException;
    
    /**
     * Check if this loader supports the given file format.
     * 
     * @param formatConfig The file format configuration
     * @return true if this loader supports the format
     */
    boolean supportsFormat(FileFormatConfig formatConfig);
    
    /**
     * Get the supported file extensions for this loader.
     * 
     * @return Array of supported file extensions (without the dot)
     */
    String[] getSupportedExtensions();
}
