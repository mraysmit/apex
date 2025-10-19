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

import dev.mars.apex.yaml.manager.model.YamlCatalog;
import dev.mars.apex.yaml.manager.model.YamlConfigMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Service for managing YAML configuration catalog.
 *
 * Provides operations for:
 * - Adding and removing configurations from catalog
 * - Searching and discovering configurations
 * - Querying catalog statistics
 * - Managing configuration metadata
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@Service
public class CatalogService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogService.class);

    private final YamlCatalog catalog;

    public CatalogService() {
        this.catalog = new YamlCatalog();
        logger.info("CatalogService initialized");
    }

    /**
     * Add a configuration to the catalog.
     */
    public void addConfiguration(YamlConfigMetadata metadata) {
        logger.debug("Adding configuration to catalog: {}", metadata.getId());
        catalog.addConfiguration(metadata);
    }

    /**
     * Remove a configuration from the catalog.
     */
    public void removeConfiguration(String id) {
        logger.debug("Removing configuration from catalog: {}", id);
        catalog.removeConfiguration(id);
    }

    /**
     * Get a configuration by ID.
     */
    public YamlConfigMetadata getConfiguration(String id) {
        return catalog.getConfiguration(id);
    }

    /**
     * Get all configurations.
     */
    public Collection<YamlConfigMetadata> getAllConfigurations() {
        return catalog.getAllConfigurations();
    }

    /**
     * Find configurations by tag.
     */
    public List<YamlConfigMetadata> findByTag(String tag) {
        logger.debug("Finding configurations by tag: {}", tag);
        return catalog.findByTag(tag);
    }

    /**
     * Find configurations by category.
     */
    public List<YamlConfigMetadata> findByCategory(String category) {
        logger.debug("Finding configurations by category: {}", category);
        return catalog.findByCategory(category);
    }

    /**
     * Find configurations by type.
     */
    public List<YamlConfigMetadata> findByType(String type) {
        logger.debug("Finding configurations by type: {}", type);
        return catalog.findByType(type);
    }

    /**
     * Find configurations by author.
     */
    public List<YamlConfigMetadata> findByAuthor(String author) {
        logger.debug("Finding configurations by author: {}", author);
        return catalog.findByAuthor(author);
    }

    /**
     * Find unused configurations.
     */
    public List<YamlConfigMetadata> findUnused() {
        logger.debug("Finding unused configurations");
        return catalog.findUnused();
    }

    /**
     * Find critical configurations.
     */
    public List<YamlConfigMetadata> findCritical() {
        logger.debug("Finding critical configurations");
        return catalog.findCritical();
    }

    /**
     * Find configurations by health score range.
     */
    public List<YamlConfigMetadata> findByHealthScore(int minScore, int maxScore) {
        logger.debug("Finding configurations by health score: {} - {}", minScore, maxScore);
        return catalog.findByHealthScore(minScore, maxScore);
    }

    /**
     * Get catalog statistics.
     */
    public YamlCatalog getCatalog() {
        return catalog;
    }

    /**
     * Get total number of configurations.
     */
    public int getTotalConfigurations() {
        return catalog.getTotalConfigurations();
    }

    /**
     * Get number of orphaned configurations.
     */
    public int getOrphanedCount() {
        return catalog.getOrphanedCount();
    }

    /**
     * Get number of critical configurations.
     */
    public int getCriticalCount() {
        return catalog.getCriticalCount();
    }

    /**
     * Get average health score.
     */
    public double getAverageHealthScore() {
        return catalog.getAverageHealthScore();
    }
}

