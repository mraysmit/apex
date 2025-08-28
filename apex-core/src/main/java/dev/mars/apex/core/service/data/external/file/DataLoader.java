package dev.mars.apex.core.service.data.external.file;

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
 * @author Mark Andrew Ray-Smith Cityline Ltd
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
