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

import dev.mars.apex.yaml.manager.model.CategorySummary;
import dev.mars.apex.yaml.manager.model.ConfigMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing category information across the YAML catalog.
 *
 * Provides operations for:
 * - Extracting category definitions from YAML files
 * - Tracking category usage across rules, enrichments, and groups
 * - Searching and filtering categories
 * - Category governance and lifecycle management
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-09
 * @version 1.0
 */
@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CatalogService catalogService;

    /**
     * Get all categories defined across all YAML files in the catalog.
     *
     * @return Map of category name to CategorySummary
     */
    public Map<String, CategorySummary> getAllCategories() {
        logger.debug("Building category index from catalog");
        
        Map<String, CategorySummary> categoryIndex = new HashMap<>();
        
        // Iterate through all configurations in the catalog
        for (ConfigMetadata config : catalogService.getAllConfigurations()) {
            if (config.getCategories() != null && !config.getCategories().isEmpty()) {
                // Parse the YAML file to extract full category details
                extractCategoriesFromFile(config.getPath(), categoryIndex);
            }
        }
        
        logger.debug("Found {} categories across catalog", categoryIndex.size());
        return categoryIndex;
    }

    /**
     * Get a specific category by name.
     *
     * @param categoryName Name of the category
     * @return CategorySummary or null if not found
     */
    public CategorySummary getCategory(String categoryName) {
        Map<String, CategorySummary> allCategories = getAllCategories();
        return allCategories.get(categoryName);
    }

    /**
     * Find categories by business domain.
     *
     * @param businessDomain Business domain to search for
     * @return List of categories in the specified domain
     */
    public List<CategorySummary> findByBusinessDomain(String businessDomain) {
        return getAllCategories().values().stream()
                .filter(cat -> businessDomain.equals(cat.getBusinessDomain()))
                .collect(Collectors.toList());
    }

    /**
     * Find categories by business owner.
     *
     * @param businessOwner Business owner to search for
     * @return List of categories owned by the specified owner
     */
    public List<CategorySummary> findByBusinessOwner(String businessOwner) {
        return getAllCategories().values().stream()
                .filter(cat -> businessOwner.equals(cat.getBusinessOwner()))
                .collect(Collectors.toList());
    }

    /**
     * Get all distinct business domains from categories.
     *
     * @return Set of business domains
     */
    public Set<String> getAllBusinessDomains() {
        return getAllCategories().values().stream()
                .map(CategorySummary::getBusinessDomain)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Get all distinct business owners from categories.
     *
     * @return Set of business owners
     */
    public Set<String> getAllBusinessOwners() {
        return getAllCategories().values().stream()
                .map(CategorySummary::getBusinessOwner)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Get categories that are currently active (not expired).
     *
     * @return List of active categories
     */
    public List<CategorySummary> getActiveCategories() {
        LocalDate today = LocalDate.now();
        return getAllCategories().values().stream()
                .filter(cat -> isActive(cat, today))
                .collect(Collectors.toList());
    }

    /**
     * Get categories that are currently expired.
     *
     * @return List of expired categories
     */
    public List<CategorySummary> getExpiredCategories() {
        LocalDate today = LocalDate.now();
        return getAllCategories().values().stream()
                .filter(cat -> isExpired(cat, today))
                .collect(Collectors.toList());
    }

    /**
     * Get category statistics.
     *
     * @return Map containing various statistics
     */
    public Map<String, Object> getCategoryStatistics() {
        Map<String, CategorySummary> allCategories = getAllCategories();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCategories", allCategories.size());
        stats.put("totalBusinessDomains", getAllBusinessDomains().size());
        stats.put("totalBusinessOwners", getAllBusinessOwners().size());
        stats.put("activeCategories", getActiveCategories().size());
        stats.put("expiredCategories", getExpiredCategories().size());
        
        // Calculate average usage
        double avgUsage = allCategories.values().stream()
                .mapToInt(CategorySummary::getTotalUsageCount)
                .average()
                .orElse(0.0);
        stats.put("averageUsageCount", avgUsage);
        
        // Find most used category
        Optional<CategorySummary> mostUsed = allCategories.values().stream()
                .max(Comparator.comparingInt(CategorySummary::getTotalUsageCount));
        mostUsed.ifPresent(cat -> {
            stats.put("mostUsedCategory", cat.getName());
            stats.put("mostUsedCategoryCount", cat.getTotalUsageCount());
        });
        
        return stats;
    }

    /**
     * Extract category information from a YAML file.
     *
     * @param filePath Path to YAML file
     * @param categoryIndex Map to populate with category information
     */
    @SuppressWarnings("unchecked")
    private void extractCategoriesFromFile(String filePath, Map<String, CategorySummary> categoryIndex) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(new FileInputStream(filePath));
            
            if (data != null && data.containsKey("categories")) {
                List<Map<String, Object>> categories = (List<Map<String, Object>>) data.get("categories");
                
                if (categories != null) {
                    for (Map<String, Object> categoryData : categories) {
                        String categoryName = (String) categoryData.get("name");
                        
                        if (categoryName != null) {
                            CategorySummary category = categoryIndex.computeIfAbsent(
                                    categoryName, k -> new CategorySummary(categoryName));
                            
                            // Populate category fields
                            populateCategoryFromData(category, categoryData);
                            
                            // Track which file defines this category
                            category.addDefinedInFile(filePath);
                            
                            // TODO: Track usage by scanning rules/enrichments that reference this category
                            // This would require parsing all rules/enrichments in the file
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to extract categories from file: {}", filePath);
            logger.debug("Full exception details:", e);
        } catch (Exception e) {
            logger.warn("Error parsing categories from file: {}", filePath);
            logger.debug("Full exception details:", e);
        }
    }

    /**
     * Populate CategorySummary from YAML data.
     *
     * @param category CategorySummary to populate
     * @param data YAML data map
     */
    @SuppressWarnings("unchecked")
    private void populateCategoryFromData(CategorySummary category, Map<String, Object> data) {
        category.setDisplayName((String) data.get("display-name"));
        category.setDescription((String) data.get("description"));
        category.setPriority((Integer) data.get("priority"));
        category.setEnabled((Boolean) data.get("enabled"));
        category.setExecutionOrder((Integer) data.get("execution-order"));
        category.setStopOnFirstFailure((Boolean) data.get("stop-on-first-failure"));
        category.setParallelExecution((Boolean) data.get("parallel-execution"));
        category.setBusinessDomain((String) data.get("business-domain"));
        category.setBusinessOwner((String) data.get("business-owner"));
        category.setCreatedBy((String) data.get("created-by"));
        category.setEffectiveDate((String) data.get("effective-date"));
        category.setExpirationDate((String) data.get("expiration-date"));
        category.setParentCategory((String) data.get("parent-category"));
        
        if (data.containsKey("tags")) {
            Object tagsObj = data.get("tags");
            if (tagsObj instanceof List) {
                category.setTags((List<String>) tagsObj);
            }
        }
        
        if (data.containsKey("metadata")) {
            Object metadataObj = data.get("metadata");
            if (metadataObj instanceof Map) {
                category.setMetadata((Map<String, Object>) metadataObj);
            }
        }
    }

    /**
     * Check if a category is currently active.
     *
     * @param category Category to check
     * @param today Today's date
     * @return true if active, false otherwise
     */
    private boolean isActive(CategorySummary category, LocalDate today) {
        // If no dates specified, consider it active
        if (category.getEffectiveDate() == null && category.getExpirationDate() == null) {
            return true;
        }
        
        try {
            if (category.getEffectiveDate() != null) {
                LocalDate effectiveDate = parseDate(category.getEffectiveDate());
                if (today.isBefore(effectiveDate)) {
                    return false;
                }
            }
            
            if (category.getExpirationDate() != null) {
                LocalDate expirationDate = parseDate(category.getExpirationDate());
                if (today.isAfter(expirationDate)) {
                    return false;
                }
            }
            
            return true;
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse dates for category: {}", category.getName());
            return true; // Default to active if dates can't be parsed
        }
    }

    /**
     * Check if a category is expired.
     *
     * @param category Category to check
     * @param today Today's date
     * @return true if expired, false otherwise
     */
    private boolean isExpired(CategorySummary category, LocalDate today) {
        if (category.getExpirationDate() == null) {
            return false;
        }
        
        try {
            LocalDate expirationDate = parseDate(category.getExpirationDate());
            return today.isAfter(expirationDate);
        } catch (DateTimeParseException e) {
            logger.warn("Failed to parse expiration date for category: {}", category.getName());
            return false;
        }
    }

    /**
     * Parse a date string in various formats.
     *
     * @param dateStr Date string
     * @return LocalDate
     */
    private LocalDate parseDate(String dateStr) {
        // Try ISO date format first (2025-01-01)
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            // Try ISO date-time format (2025-01-01T00:00:00Z)
            if (dateStr.contains("T")) {
                return LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            }
            throw e;
        }
    }
}

