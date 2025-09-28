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

/**
 * Interface for detecting file formats from input data.
 * 
 * File format detectors implement various strategies for identifying the format
 * of input data, such as extension-based detection, content-based detection,
 * or magic number detection.
 * 
 * DESIGN PRINCIPLES:
 * - Strategy pattern for pluggable detection algorithms
 * - Confidence-based results for ambiguous cases
 * - Lightweight and fast detection for real-time processing
 * - Support for multiple detection strategies
 * 
 * IMPLEMENTATION STRATEGIES:
 * - Extension-based: Uses file extension patterns (*.json, *.xml, *.csv)
 * - Content-based: Analyzes content structure and patterns
 * - Magic number: Uses file signature bytes
 * - Hybrid: Combines multiple strategies for higher confidence
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 */
public interface FileFormatDetector {
    
    /**
     * Checks if this detector can analyze the given classification context.
     * 
     * @param context the classification context containing input data
     * @return true if this detector can process the context
     */
    boolean canDetect(ClassificationContext context);
    
    /**
     * Detects the file format from the classification context.
     * 
     * @param context the classification context containing input data
     * @return file format detection result with confidence score
     */
    FileFormatResult detect(ClassificationContext context);
    
    /**
     * Gets the priority of this detector (higher numbers = higher priority).
     * 
     * @return priority value for ordering detectors
     */
    int getPriority();
    
    /**
     * Gets the name of this detector for logging and debugging.
     * 
     * @return detector name
     */
    String getName();
}
