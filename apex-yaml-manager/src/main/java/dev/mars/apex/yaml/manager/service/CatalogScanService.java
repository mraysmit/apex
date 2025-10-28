package dev.mars.apex.yaml.manager.service;

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

import dev.mars.apex.yaml.manager.model.YamlConfigMetadata;
import dev.mars.apex.yaml.manager.model.YamlContentSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Service for scanning directories and indexing YAML files into the catalog.
 *
 * Provides operations for:
 * - Scanning directories recursively for YAML files
 * - Analyzing YAML file content and extracting metadata
 * - Indexing files into the catalog
 * - Tracking scan results and errors
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-28
 * @version 1.0
 */
@Service
public class CatalogScanService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogScanService.class);

    @Autowired
    private YamlContentAnalyzer contentAnalyzer;

    @Autowired
    private CatalogService catalogService;

    /**
     * Scan a directory and index all YAML files into the catalog.
     *
     * @param directoryPath Path to directory to scan
     * @return Map containing scan results (filesScanned, filesIndexed, errors, duration)
     */
    public Map<String, Object> scanDirectory(String directoryPath) {
        logger.info("Starting catalog scan for directory: {}", directoryPath);
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        File directory = new File(directoryPath);
        
        // Validate directory
        if (!directory.exists()) {
            String error = "Directory does not exist: " + directoryPath;
            logger.error(error);
            result.put("success", false);
            result.put("error", error);
            return result;
        }
        
        if (!directory.isDirectory()) {
            String error = "Path is not a directory: " + directoryPath;
            logger.error(error);
            result.put("success", false);
            result.put("error", error);
            return result;
        }
        
        // Scan directory
        int filesScanned = 0;
        int filesIndexed = scanAndIndexDirectory(directory, errors);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Build result
        result.put("success", true);
        result.put("directoryPath", directoryPath);
        result.put("filesScanned", filesScanned);
        result.put("filesIndexed", filesIndexed);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        result.put("durationMs", duration);
        
        logger.info("Catalog scan complete: {} files indexed, {} errors, {} ms",
            filesIndexed, errors.size(), duration);
        
        return result;
    }

    /**
     * Recursively scan directory and index all YAML files.
     *
     * @param directory Directory to scan
     * @param errors List to collect error messages
     * @return Number of files successfully indexed
     */
    private int scanAndIndexDirectory(File directory, List<String> errors) {
        int count = 0;
        
        if (!directory.exists() || !directory.isDirectory()) {
            return count;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return count;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Recursively scan subdirectories
                count += scanAndIndexDirectory(file, errors);
            } else if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
                // Index YAML file
                if (indexYamlFile(file, errors)) {
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * Index a single YAML file into the catalog.
     *
     * @param file YAML file to index
     * @param errors List to collect error messages
     * @return true if file was successfully indexed, false otherwise
     */
    private boolean indexYamlFile(File file, List<String> errors) {
        try {
            String absolutePath = file.getAbsolutePath();
            logger.debug("Indexing YAML file: {}", absolutePath);
            
            // Analyze content
            YamlContentSummary summary = contentAnalyzer.analyzYamlContent(absolutePath);

            // Create metadata from summary
            YamlConfigMetadata metadata = createMetadataFromSummary(summary, absolutePath);

            // Add to catalog
            catalogService.addConfiguration(metadata);

            return true;
        } catch (MarkedYAMLException e) {
            // Parser validation error - capture detailed location information
            Mark problemMark = e.getProblemMark();
            Mark contextMark = e.getContextMark();

            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(String.format("YAML validation error in '%s'", file.getName()));

            if (problemMark != null) {
                errorMsg.append(String.format(" at line %d, column %d",
                    problemMark.getLine() + 1, problemMark.getColumn() + 1));
            }

            errorMsg.append(String.format(": %s", e.getProblem()));

            if (contextMark != null && e.getContext() != null) {
                errorMsg.append(String.format(" (context: %s at line %d)",
                    e.getContext(), contextMark.getLine() + 1));
            }

            String error = errorMsg.toString();
            logger.info(error);
            errors.add(error);
            return false;
        } catch (Exception e) {
            // Other errors (IO, etc.)
            String error = String.format("Error processing file '%s': %s", file.getName(), e.getMessage());
            logger.info(error);
            errors.add(error);
            return false;
        }
    }

    /**
     * Create YamlConfigMetadata from YamlContentSummary.
     *
     * @param summary Content summary from analyzer
     * @param filePath Absolute file path
     * @return YamlConfigMetadata object
     */
    private YamlConfigMetadata createMetadataFromSummary(YamlContentSummary summary, String filePath) {
        YamlConfigMetadata metadata = new YamlConfigMetadata();

        metadata.setId(summary.getId() != null ? summary.getId() : extractIdFromPath(filePath));
        metadata.setPath(filePath);
        metadata.setName(summary.getName());
        metadata.setDescription(summary.getDescription());
        metadata.setType(summary.getFileType());
        metadata.setVersion(summary.getVersion());
        metadata.setAuthor(summary.getAuthor() != null ? summary.getAuthor() : "unknown");

        // Set tags
        if (summary.getTags() != null && !summary.getTags().isEmpty()) {
            metadata.setTags(new HashSet<>(summary.getTags()));
        }

        // Set business domain and owner
        metadata.setBusinessDomain(summary.getBusinessDomain());
        metadata.setOwner(summary.getOwner());

        // Set dependencies
        if (summary.getDependencies() != null && !summary.getDependencies().isEmpty()) {
            metadata.setDependencies(new HashSet<>(summary.getDependencies()));
        }

        // Set timestamps
        if (summary.getCreatedDate() != null) {
            try {
                metadata.setCreated(LocalDateTime.parse(summary.getCreatedDate() + "T00:00:00"));
            } catch (Exception e) {
                logger.debug("Could not parse created date: {}", summary.getCreatedDate());
            }
        }
        if (summary.getLastModifiedDate() != null) {
            try {
                metadata.setLastModified(LocalDateTime.parse(summary.getLastModifiedDate() + "T00:00:00"));
            } catch (Exception e) {
                logger.debug("Could not parse last modified date: {}", summary.getLastModifiedDate());
            }
        }

        // Set health score (default to 75 if not available)
        metadata.setHealthScore(75);

        return metadata;
    }

    /**
     * Extract ID from file path if not present in metadata.
     *
     * @param filePath File path
     * @return Extracted ID
     */
    private String extractIdFromPath(String filePath) {
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        // Remove .yaml or .yml extension
        return fileName.replaceAll("\\.(yaml|yml)$", "");
    }
}

