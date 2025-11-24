package dev.mars.apex.core.service.enrichment;

import dev.mars.apex.core.config.yaml.YamlCategory;
import dev.mars.apex.core.config.yaml.YamlEnrichment;
import dev.mars.apex.core.config.yaml.YamlEnrichmentGroup;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.constants.ErrorHandlingConstants;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.EnrichmentGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Factory to build runtime EnrichmentGroup models from YAML configuration.
 * Phase 3: mapping only (no execution). Group references/depends-on handled later.
 */
public class EnrichmentGroupFactory {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentGroupFactory.class);

    /**
     * Build enrichment groups from a rule configuration.
     * - Resolves enrichments by id
     * - Maps operator (AND/OR) into boolean andOperator
     * - Applies stop-on-first-failure and parallel-execution flags
     * - Populates enrichments by sequence based on enrichment-references, or order of enrichment-ids
     * - Applies category inheritance for enterprise metadata
     */
    public static List<EnrichmentGroup> buildEnrichmentGroups(YamlRuleConfiguration config) {
        if (config == null || config.getEnrichmentGroups() == null || config.getEnrichmentGroups().isEmpty()) {
            return Collections.emptyList();
        }

        // Build category caches for metadata inheritance
        Map<String, Category> categoryCache = new HashMap<>();
        Map<String, YamlCategory> yamlCategoryCache = new HashMap<>();
        buildCategoryCaches(config, categoryCache, yamlCategoryCache);

        Map<String, YamlEnrichment> enrichmentById = indexEnrichments(config.getEnrichments());
        List<EnrichmentGroup> groups = new ArrayList<>();
        Map<String, EnrichmentGroup> groupsById = new HashMap<>();

        for (YamlEnrichmentGroup yg : config.getEnrichmentGroups()) {
            if (yg == null || yg.getId() == null) continue;

            // Determine category for inheritance
            String categoryName = yg.getCategory() != null ? yg.getCategory() : "default";
            getOrCreateCategory(categoryName, yg.getPriority() != null ? yg.getPriority() : 100, categoryCache);

            // Look up category metadata from cache for inheritance
            YamlCategory yamlCategory = yamlCategoryCache.get(categoryName);
            logger.debug("Found category '" + categoryName + "' for enrichment group '" + yg.getId() +
                       "'. YamlCategory found: " + (yamlCategory != null) +
                       (yamlCategory != null ? ", businessOwner: " + yamlCategory.getBusinessOwner() : ""));

            // Parse error-handling strategy from YAML configuration
            String errorHandling = yg.getErrorHandling() != null ? yg.getErrorHandling() : ErrorHandlingConstants.DEFAULT_STRATEGY;
            if (!ErrorHandlingConstants.isValidStrategy(errorHandling)) {
                logger.warn("Invalid error-handling '" + errorHandling + "' for enrichment group '" + yg.getId() + "'. Using " + ErrorHandlingConstants.DEFAULT_STRATEGY + " as default.");
                errorHandling = ErrorHandlingConstants.DEFAULT_STRATEGY;
            }

            EnrichmentGroup g = new EnrichmentGroup(yg.getId())
                    .setName(yg.getName())
                    .setDescription(yg.getDescription())
                    .setAndOperator(mapOperatorToAnd(yg.getOperator()))
                    .setStopOnFirstFailure(Boolean.TRUE.equals(yg.getStopOnFirstFailure()))
                    .setParallelExecution(Boolean.TRUE.equals(yg.getParallelExecution()))
                    .setDebugMode(yg.getDebugMode() != null ? yg.getDebugMode() : Boolean.parseBoolean(System.getProperty("apex.enrichmentgroup.debug", "false")))
                    .setPriority(yg.getPriority())
                    .setErrorHandling(errorHandling);

            // Apply enterprise metadata with category inheritance
            // Enrichment group metadata takes precedence, but inherit from category if not specified
            applyMetadataInheritance(yg, yamlCategory, g);

            // 1) Add enrichment-ids first with auto-incrementing sequence starting at 1
            if (yg.getEnrichmentIds() != null && !yg.getEnrichmentIds().isEmpty()) {
                int sequence = 1;
                for (String id : yg.getEnrichmentIds()) {
                    if (id == null) continue;
                    YamlEnrichment e = enrichmentById.get(id);
                    if (e == null) {
                        logger.warn("Enrichment id not found: " + id + " in group " + yg.getId());
                        continue;
                    }
                    g.addEnrichment(sequence++, e);
                }
            }

            // 2) Then apply enrichment-references with explicit sequence (default 1), overriding positions if needed
            if (yg.getEnrichmentReferences() != null && !yg.getEnrichmentReferences().isEmpty()) {
                for (YamlEnrichmentGroup.EnrichmentReference ref : yg.getEnrichmentReferences()) {
                    if (ref == null || ref.getEnrichmentId() == null) continue;
                    if (Boolean.FALSE.equals(ref.getEnabled())) continue; // skip disabled refs
                    int sequence = ref.getSequence() != null ? ref.getSequence() : 1;
                    YamlEnrichment e = enrichmentById.get(ref.getEnrichmentId());
                    if (e == null) {
                        logger.warn("Enrichment reference not found: " + ref.getEnrichmentId() + " in group " + yg.getId());
                        continue;
                    }
                    // Note: override-priority is not applied here; priority is part of enrichment itself
                    g.addEnrichment(sequence, e);
                }
            }

            groups.add(g);
            groupsById.put(yg.getId(), g);
        }

        // Second phase: process enrichment-group-references recursively
        Map<String, YamlEnrichmentGroup> yamlGroupsById = new HashMap<>();
        for (YamlEnrichmentGroup yg : config.getEnrichmentGroups()) {
            if (yg != null && yg.getId() != null) {
                yamlGroupsById.put(yg.getId(), yg);
            }
        }

        Set<String> resolvedGroups = new HashSet<>();
        Set<String> resolvingGroups = new HashSet<>();

        for (String groupId : groupsById.keySet()) {
            resolveEnrichmentGroupReferences(groupId, groupsById, yamlGroupsById, resolvedGroups, resolvingGroups);
        }

        return groups;
    }

    private static void resolveEnrichmentGroupReferences(String groupId,
                                                       Map<String, EnrichmentGroup> groupsById,
                                                       Map<String, YamlEnrichmentGroup> yamlGroupsById,
                                                       Set<String> resolvedGroups,
                                                       Set<String> resolvingGroups) {
        if (resolvedGroups.contains(groupId)) {
            return;
        }

        if (resolvingGroups.contains(groupId)) {
            throw new RuntimeException("Circular dependency detected in enrichment groups: " + resolvingGroups + " -> " + groupId);
        }

        resolvingGroups.add(groupId);

        YamlEnrichmentGroup yamlGroup = yamlGroupsById.get(groupId);
        if (yamlGroup != null) {
            // Collect all references from plural and singular fields
            List<String> allReferences = new ArrayList<>();
            if (yamlGroup.getEnrichmentGroupReferences() != null) {
                allReferences.addAll(yamlGroup.getEnrichmentGroupReferences());
            }
            if (yamlGroup.getEnrichmentGroup() != null) {
                allReferences.add(yamlGroup.getEnrichmentGroup());
            }

            if (!allReferences.isEmpty()) {
                EnrichmentGroup targetGroup = groupsById.get(groupId);
                
                // Calculate starting sequence number (after existing enrichments)
                int nextSequence = targetGroup.getEnrichmentsBySequence().size() + 1;

                for (String refId : allReferences) {
                    // Recursively resolve the referenced group first
                    resolveEnrichmentGroupReferences(refId, groupsById, yamlGroupsById, resolvedGroups, resolvingGroups);

                    EnrichmentGroup referencedGroup = groupsById.get(refId);
                    if (referencedGroup != null) {
                        for (YamlEnrichment e : referencedGroup.getEnrichmentsInOrder()) {
                            targetGroup.addEnrichment(nextSequence++, e);
                        }
                    } else {
                        logger.warn("Referenced enrichment group not found: " + refId + " in group: " + groupId);
                    }
                }
            }
        }

        resolvingGroups.remove(groupId);
        resolvedGroups.add(groupId);
    }

    private static boolean mapOperatorToAnd(String operator) {
        if (operator == null) return true; // default AND
        String op = operator.trim().toUpperCase();
        if ("AND".equals(op)) return true;
        if ("OR".equals(op)) return false;
        logger.warn("Invalid operator '" + operator + "' for enrichment group. Using AND as default.");
        return true;
    }

    private static Map<String, YamlEnrichment> indexEnrichments(List<YamlEnrichment> enrichments) {
        Map<String, YamlEnrichment> map = new HashMap<>();
        if (enrichments != null) {
            for (YamlEnrichment e : enrichments) {
                if (e != null && e.getId() != null) {
                    map.put(e.getId(), e);
                }
            }
        }
        return map;
    }

    /**
     * Build category caches from YAML configuration for metadata inheritance.
     *
     * @param config The YAML configuration
     * @param categoryCache The category cache to populate
     * @param yamlCategoryCache The YAML category cache to populate
     */
    private static void buildCategoryCaches(YamlRuleConfiguration config,
                                          Map<String, Category> categoryCache,
                                          Map<String, YamlCategory> yamlCategoryCache) {
        if (config.getCategories() != null) {
            for (YamlCategory yamlCategory : config.getCategories()) {
                if (yamlCategory.getEnabled() == null || yamlCategory.getEnabled()) {
                    Category category = createCategory(yamlCategory);
                    categoryCache.put(category.getName(), category);
                    // Also cache the YAML category for metadata inheritance
                    yamlCategoryCache.put(yamlCategory.getName(), yamlCategory);
                    logger.debug("Cached category '" + yamlCategory.getName() +
                               "' with businessOwner: " + yamlCategory.getBusinessOwner() +
                               ", businessDomain: " + yamlCategory.getBusinessDomain());
                }
            }
        }
    }

    /**
     * Create a Category from YAML category configuration.
     *
     * @param yamlCategory The YAML category configuration
     * @return A Category object
     */
    private static Category createCategory(YamlCategory yamlCategory) {
        String name = yamlCategory.getName();
        int priority = yamlCategory.getPriority() != null ? yamlCategory.getPriority() : 100;

        logger.debug("Creating category: " + name + " with priority: " + priority);

        return new Category(name, priority);
    }

    /**
     * Get an existing category or create a new one.
     *
     * @param categoryName The category name
     * @param defaultPriority The default priority if creating a new category
     * @param categoryCache The category cache
     * @return The Category object
     */
    private static Category getOrCreateCategory(String categoryName, int defaultPriority, Map<String, Category> categoryCache) {
        return categoryCache.computeIfAbsent(categoryName, name -> {
            logger.debug("Creating new category: " + name + " with priority: " + defaultPriority);
            return new Category(name, defaultPriority);
        });
    }

    /**
     * Apply enterprise metadata inheritance to an enrichment group.
     *
     * @param yamlGroup The YAML enrichment group configuration
     * @param yamlCategory The YAML category for inheritance (may be null)
     * @param group The enrichment group to apply metadata to
     */
    private static void applyMetadataInheritance(YamlEnrichmentGroup yamlGroup, YamlCategory yamlCategory, EnrichmentGroup group) {
        String createdBy = yamlGroup.getCreatedBy();
        if (createdBy == null && yamlCategory != null) {
            createdBy = yamlCategory.getCreatedBy();
        }
        if (createdBy != null) {
            group.setCreatedBy(createdBy);
        }

        String businessDomain = yamlGroup.getBusinessDomain();
        if (businessDomain == null && yamlCategory != null) {
            businessDomain = yamlCategory.getBusinessDomain();
        }
        if (businessDomain != null) {
            group.setBusinessDomain(businessDomain);
        }

        String businessOwner = yamlGroup.getBusinessOwner();
        if (businessOwner == null && yamlCategory != null) {
            businessOwner = yamlCategory.getBusinessOwner();
        }
        if (businessOwner != null) {
            group.setBusinessOwner(businessOwner);
        }

        String sourceSystem = yamlGroup.getSourceSystem();
        if (sourceSystem != null) {
            group.setSourceSystem(sourceSystem);
        }

        String effectiveDate = yamlGroup.getEffectiveDate();
        if (effectiveDate == null && yamlCategory != null) {
            effectiveDate = yamlCategory.getEffectiveDate();
        }
        if (effectiveDate != null) {
            group.setEffectiveDate(effectiveDate);
        }

        String expirationDate = yamlGroup.getExpirationDate();
        if (expirationDate == null && yamlCategory != null) {
            expirationDate = yamlCategory.getExpirationDate();
        }
        if (expirationDate != null) {
            group.setExpirationDate(expirationDate);
        }

        logger.debug("Applied metadata inheritance to enrichment group '" + yamlGroup.getId() + "': " +
                   "createdBy=" + group.getCreatedBy() + ", " +
                   "businessDomain=" + group.getBusinessDomain() + ", " +
                   "businessOwner=" + group.getBusinessOwner());
    }
}

