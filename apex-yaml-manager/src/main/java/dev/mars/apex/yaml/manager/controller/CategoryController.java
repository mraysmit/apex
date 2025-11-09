package dev.mars.apex.yaml.manager.controller;

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
import dev.mars.apex.yaml.manager.service.CategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API controller for category management operations.
 *
 * Provides endpoints for:
 * - Listing all categories across the catalog
 * - Getting detailed information about specific categories
 * - Searching configurations by category
 * - Category statistics and governance reports
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-11-09
 * @version 1.0
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    /**
     * Get all categories defined across all YAML files in the catalog.
     *
     * @return Map containing all categories with their metadata
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories() {
        logger.debug("Getting all categories");
        
        Map<String, CategorySummary> categories = categoryService.getAllCategories();
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", categories.size());
        response.put("categories", categories.values());
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed information about a specific category.
     *
     * @param categoryName Name of the category
     * @return Category details including usage information
     */
    @GetMapping("/{categoryName}")
    public ResponseEntity<Map<String, Object>> getCategory(@PathVariable String categoryName) {
        logger.debug("Getting category: {}", categoryName);
        
        CategorySummary category = categoryService.getCategory(categoryName);
        
        if (category == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Category not found: " + categoryName);
            errorResponse.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(404).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("category", category);
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Search categories by business domain.
     *
     * @param businessDomain Business domain to search for
     * @return List of categories in the specified business domain
     */
    @GetMapping("/search/business-domain/{businessDomain}")
    public ResponseEntity<Map<String, Object>> searchByBusinessDomain(@PathVariable String businessDomain) {
        logger.debug("Searching categories by business domain: {}", businessDomain);
        
        List<CategorySummary> categories = categoryService.findByBusinessDomain(businessDomain);
        
        Map<String, Object> response = new HashMap<>();
        response.put("businessDomain", businessDomain);
        response.put("count", categories.size());
        response.put("categories", categories);
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Search categories by business owner.
     *
     * @param businessOwner Business owner to search for
     * @return List of categories owned by the specified owner
     */
    @GetMapping("/search/business-owner/{businessOwner}")
    public ResponseEntity<Map<String, Object>> searchByBusinessOwner(@PathVariable String businessOwner) {
        logger.debug("Searching categories by business owner: {}", businessOwner);
        
        List<CategorySummary> categories = categoryService.findByBusinessOwner(businessOwner);
        
        Map<String, Object> response = new HashMap<>();
        response.put("businessOwner", businessOwner);
        response.put("count", categories.size());
        response.put("categories", categories);
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all configurations that define a specific category.
     *
     * @param categoryName Name of the category
     * @return List of file paths that define this category
     */
    @GetMapping("/{categoryName}/definitions")
    public ResponseEntity<Map<String, Object>> getCategoryDefinitions(@PathVariable String categoryName) {
        logger.debug("Getting definitions for category: {}", categoryName);
        
        CategorySummary category = categoryService.getCategory(categoryName);
        
        if (category == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Category not found: " + categoryName);
            errorResponse.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(404).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("categoryName", categoryName);
        response.put("definedInFiles", category.getDefinedInFiles());
        response.put("count", category.getDefinedInFiles().size());
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get usage statistics for a specific category.
     *
     * @param categoryName Name of the category
     * @return Usage statistics including rules, rule groups, enrichments that use this category
     */
    @GetMapping("/{categoryName}/usage")
    public ResponseEntity<Map<String, Object>> getCategoryUsage(@PathVariable String categoryName) {
        logger.debug("Getting usage for category: {}", categoryName);
        
        CategorySummary category = categoryService.getCategory(categoryName);
        
        if (category == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Category not found: " + categoryName);
            errorResponse.put("timestamp", java.time.Instant.now());
            return ResponseEntity.status(404).body(errorResponse);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("categoryName", categoryName);
        response.put("totalUsageCount", category.getTotalUsageCount());
        response.put("usedByRules", category.getUsedByRules());
        response.put("usedByRuleGroups", category.getUsedByRuleGroups());
        response.put("usedByEnrichments", category.getUsedByEnrichments());
        response.put("usedByEnrichmentGroups", category.getUsedByEnrichmentGroups());
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get category statistics across the entire catalog.
     *
     * @return Statistics including total categories, business domains, owners, etc.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getCategoryStatistics() {
        logger.debug("Getting category statistics");
        
        Map<String, Object> stats = categoryService.getCategoryStatistics();
        stats.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all distinct business domains from categories.
     *
     * @return Set of all business domains
     */
    @GetMapping("/business-domains")
    public ResponseEntity<Map<String, Object>> getAllBusinessDomains() {
        logger.debug("Getting all business domains from categories");
        
        Set<String> domains = categoryService.getAllBusinessDomains();
        
        Map<String, Object> response = new HashMap<>();
        response.put("businessDomains", domains);
        response.put("count", domains.size());
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get all distinct business owners from categories.
     *
     * @return Set of all business owners
     */
    @GetMapping("/business-owners")
    public ResponseEntity<Map<String, Object>> getAllBusinessOwners() {
        logger.debug("Getting all business owners from categories");
        
        Set<String> owners = categoryService.getAllBusinessOwners();
        
        Map<String, Object> response = new HashMap<>();
        response.put("businessOwners", owners);
        response.put("count", owners.size());
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get categories that are currently active (not expired).
     *
     * @return List of active categories
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveCategories() {
        logger.debug("Getting active categories");
        
        List<CategorySummary> categories = categoryService.getActiveCategories();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", categories.size());
        response.put("categories", categories);
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get categories that are currently expired.
     *
     * @return List of expired categories
     */
    @GetMapping("/expired")
    public ResponseEntity<Map<String, Object>> getExpiredCategories() {
        logger.debug("Getting expired categories");
        
        List<CategorySummary> categories = categoryService.getExpiredCategories();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", categories.size());
        response.put("categories", categories);
        response.put("timestamp", java.time.Instant.now());
        
        return ResponseEntity.ok(response);
    }
}

