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
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
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
     * Scan classpath for YAML files under the specified prefix.
     * 
     * <p>Uses Spring's PathMatchingResourcePatternResolver to find all YAML resources
     * matching the prefix pattern. This enables scanning JAR-packaged resources and
     * test resources on the classpath.</p>
     *
     * @param classpathPrefix The classpath prefix to scan (e.g., "config/", "META-INF/apex/")
     * @return Map containing scan results (resourcesScanned, resourcesIndexed, errors, duration)
     */
    public Map<String, Object> scanClasspath(String classpathPrefix) {
        return scanClasspath(classpathPrefix, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Scan classpath for YAML files under the specified prefix using a specific ClassLoader.
     * 
     * <p>Uses Spring's PathMatchingResourcePatternResolver to find all YAML resources
     * matching the prefix pattern. This enables scanning JAR-packaged resources and
     * test resources on the classpath.</p>
     *
     * @param classpathPrefix The classpath prefix to scan (e.g., "config/", "META-INF/apex/")
     * @param classLoader The ClassLoader to use for resource resolution
     * @return Map containing scan results (resourcesScanned, resourcesIndexed, errors, duration)
     */
    public Map<String, Object> scanClasspath(String classpathPrefix, ClassLoader classLoader) {
        logger.info("Starting classpath scan for prefix: {}", classpathPrefix);
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        List<String> errors = new ArrayList<>();
        
        // Validate prefix
        if (classpathPrefix == null || classpathPrefix.trim().isEmpty()) {
            String error = "Classpath prefix cannot be null or empty";
            logger.error(error);
            result.put("success", false);
            result.put("error", error);
            return result;
        }
        
        // Normalize prefix (ensure no leading slash, ensure trailing slash for directories)
        String normalizedPrefix = classpathPrefix.startsWith("/") 
            ? classpathPrefix.substring(1) 
            : classpathPrefix;
        if (!normalizedPrefix.isEmpty() && !normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix + "/";
        }
        
        int resourcesScanned = 0;
        int resourcesIndexed = 0;
        
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
            
            // Scan for both .yaml and .yml files
            String[] patterns = {
                "classpath*:" + normalizedPrefix + "**/*.yaml",
                "classpath*:" + normalizedPrefix + "**/*.yml"
            };
            
            for (String pattern : patterns) {
                logger.debug("Scanning classpath pattern: {}", pattern);
                
                try {
                    Resource[] resources = resolver.getResources(pattern);
                    resourcesScanned += resources.length;
                    
                    for (Resource resource : resources) {
                        if (resource.isReadable()) {
                            if (indexClasspathResource(resource, normalizedPrefix, errors)) {
                                resourcesIndexed++;
                            }
                        }
                    }
                } catch (IOException e) {
                    String error = "Error scanning pattern '" + pattern + "': " + e.getMessage();
                    logger.warn(error);
                    errors.add(error);
                }
            }
            
        } catch (Exception e) {
            String error = "Classpath scan failed: " + e.getMessage();
            logger.error(error, e);
            result.put("success", false);
            result.put("error", error);
            result.put("errors", errors);
            return result;
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Build result
        result.put("success", true);
        result.put("classpathPrefix", classpathPrefix);
        result.put("resourcesScanned", resourcesScanned);
        result.put("resourcesIndexed", resourcesIndexed);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        result.put("durationMs", duration);
        
        logger.info("Classpath scan complete: {} resources indexed, {} errors, {} ms",
            resourcesIndexed, errors.size(), duration);
        
        return result;
    }

    /**
     * Scan both filesystem directories and classpath prefixes for YAML files.
     * 
     * <p>This method combines filesystem and classpath scanning, providing a unified
     * way to discover all YAML configuration files regardless of their location.</p>
     *
     * @param filesystemPaths List of filesystem directory paths to scan
     * @param classpathPrefixes List of classpath prefixes to scan
     * @return Map containing combined scan results
     */
    public Map<String, Object> scanAll(List<String> filesystemPaths, List<String> classpathPrefixes) {
        logger.info("Starting combined scan: {} filesystem paths, {} classpath prefixes",
            filesystemPaths != null ? filesystemPaths.size() : 0,
            classpathPrefixes != null ? classpathPrefixes.size() : 0);
        
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        List<String> allErrors = new ArrayList<>();
        List<Map<String, Object>> filesystemResults = new ArrayList<>();
        List<Map<String, Object>> classpathResults = new ArrayList<>();
        
        int totalFilesIndexed = 0;
        int totalResourcesIndexed = 0;
        
        // Scan filesystem paths
        if (filesystemPaths != null) {
            for (String path : filesystemPaths) {
                Map<String, Object> scanResult = scanDirectory(path);
                filesystemResults.add(scanResult);
                
                if (Boolean.TRUE.equals(scanResult.get("success"))) {
                    totalFilesIndexed += ((Number) scanResult.getOrDefault("filesIndexed", 0)).intValue();
                }
                
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) scanResult.get("errors");
                if (errors != null) {
                    allErrors.addAll(errors);
                }
            }
        }
        
        // Scan classpath prefixes
        if (classpathPrefixes != null) {
            for (String prefix : classpathPrefixes) {
                Map<String, Object> scanResult = scanClasspath(prefix);
                classpathResults.add(scanResult);
                
                if (Boolean.TRUE.equals(scanResult.get("success"))) {
                    totalResourcesIndexed += ((Number) scanResult.getOrDefault("resourcesIndexed", 0)).intValue();
                }
                
                @SuppressWarnings("unchecked")
                List<String> errors = (List<String>) scanResult.get("errors");
                if (errors != null) {
                    allErrors.addAll(errors);
                }
            }
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Build combined result
        result.put("success", true);
        result.put("filesystemResults", filesystemResults);
        result.put("classpathResults", classpathResults);
        result.put("totalFilesIndexed", totalFilesIndexed);
        result.put("totalResourcesIndexed", totalResourcesIndexed);
        result.put("totalIndexed", totalFilesIndexed + totalResourcesIndexed);
        result.put("errorCount", allErrors.size());
        result.put("errors", allErrors);
        result.put("durationMs", duration);
        
        logger.info("Combined scan complete: {} files + {} classpath resources indexed, {} errors, {} ms",
            totalFilesIndexed, totalResourcesIndexed, allErrors.size(), duration);
        
        return result;
    }

    /**
     * Index a classpath resource into the catalog.
     *
     * @param resource The Spring Resource to index
     * @param classpathPrefix The prefix used for scanning (for metadata)
     * @param errors List to collect error messages
     * @return true if resource was successfully indexed, false otherwise
     */
    private boolean indexClasspathResource(Resource resource, String classpathPrefix, List<String> errors) {
        try {
            String resourcePath = getResourcePath(resource);
            logger.debug("Indexing classpath resource: {}", resourcePath);
            
            // Analyze content from input stream
            try (InputStream is = resource.getInputStream()) {
                YamlContentSummary summary = contentAnalyzer.analyzeYamlContent(is, resourcePath);
                
                // Create metadata from summary
                YamlConfigMetadata metadata = createMetadataFromSummary(summary, "classpath:" + resourcePath);
                
                // Mark as classpath resource
                metadata.setClasspathResource(true);
                metadata.setClasspathPrefix(classpathPrefix);
                
                // Add to catalog
                catalogService.addConfiguration(metadata);
                
                return true;
            }
        } catch (MarkedYAMLException e) {
            // Parser validation error - capture detailed location information
            Mark problemMark = e.getProblemMark();
            
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(String.format("YAML validation error in classpath resource '%s'", 
                getResourcePath(resource)));
            
            if (problemMark != null) {
                errorMsg.append(String.format(" at line %d, column %d",
                    problemMark.getLine() + 1, problemMark.getColumn() + 1));
            }
            
            errorMsg.append(String.format(": %s", e.getProblem()));
            
            String error = errorMsg.toString();
            logger.info(error);
            errors.add(error);
            return false;
        } catch (Exception e) {
            String error = String.format("Error processing classpath resource '%s': %s", 
                getResourcePath(resource), e.getMessage());
            logger.info(error);
            errors.add(error);
            return false;
        }
    }

    /**
     * Get the path string for a Spring Resource.
     *
     * @param resource The Spring Resource
     * @return The resource path or a descriptive string
     */
    private String getResourcePath(Resource resource) {
        try {
            URL url = resource.getURL();
            return url.toString();
        } catch (IOException e) {
            return resource.getDescription();
        }
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

        // Set categories
        if (summary.getCategories() != null && !summary.getCategories().isEmpty()) {
            metadata.setCategories(new HashSet<>(summary.getCategories()));
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

