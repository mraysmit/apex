package dev.mars.apex.demo.support.util;

import java.util.Map;
import java.util.HashMap;

/**
 * ResourcePathResolver provided backward compatibility during the apex-demo
 * resource reorganization by mapping old resource paths to new organized paths.
 *
 * ⚠️ DEPRECATED: This class is no longer needed as of Phase 4 completion.
 * All Java classes now use the new organized paths directly.
 *
 * MIGRATION COMPLETED:
 * ✅ Phase 1: Created new structure alongside old structure
 * ✅ Phase 2: Used this resolver to map old paths to new paths
 * ✅ Phase 3: Updated Java classes to use new paths directly
 * ✅ Phase 4: Removed old structure - this resolver is now obsolete
 *
 * FINAL DIRECTORY ORGANIZATION:
 * demos/
 * ├── quickstart/          # 5-10 min introduction
 * ├── fundamentals/        # Core concepts (rules, enrichments, datasets)
 * ├── patterns/            # Implementation patterns (lookups, calculations, validations)
 * ├── industry/            # Real-world applications (financial-services)
 * ├── bootstrap/           # Bootstrap configurations
 * └── advanced/            # Advanced techniques (performance, integration, complex-scenarios)
 *
 * reference/
 * └── syntax-examples/     # YAML syntax reference materials
 *
 * This class is kept for historical reference and documentation purposes.
 * It shows the complete migration path from the old disorganized structure
 * to the new well-organized, educational structure.
 *
 * @author apex-demo reorganization team
 * @version 2.0 (DEPRECATED)
 * @since 2025-08-24
 * @deprecated As of Phase 4, all paths use the new structure directly.
 *             This class is kept for historical reference only.
 */
@Deprecated
public class ResourcePathResolver {
    
    /**
     * Static mapping of old resource paths to new organized paths.
     * This map contains all known resource path migrations.
     */
    private static final Map<String, String> PATH_MIGRATIONS = new HashMap<>();
    
    static {
        // Bootstrap files
        PATH_MIGRATIONS.put(
            "bootstrap/custody-auto-repair-bootstrap.yaml",
            "demos/bootstrap/custody-auto-repair/bootstrap-config.yaml"
        );
        PATH_MIGRATIONS.put(
            "bootstrap/otc-options-bootstrap.yaml",
            "demos/bootstrap/otc-options/bootstrap-config.yaml"
        );
        PATH_MIGRATIONS.put(
            "bootstrap/datasets/market-data.yaml",
            "demos/bootstrap/custody-auto-repair/datasets/market-data.yaml"
        );
        
        // Lookup examples
        PATH_MIGRATIONS.put(
            "examples/lookups/simple-field-lookup.yaml",
            "demos/patterns/lookups/simple-field-lookup.yaml"
        );
        PATH_MIGRATIONS.put(
            "examples/lookups/conditional-expression-lookup.yaml",
            "demos/patterns/lookups/conditional-expression-lookup.yaml"
        );
        PATH_MIGRATIONS.put(
            "examples/lookups/nested-field-lookup.yaml",
            "demos/patterns/lookups/nested-field-lookup.yaml"
        );
        PATH_MIGRATIONS.put(
            "examples/lookups/compound-key-lookup.yaml",
            "demos/patterns/lookups/compound-key-lookup.yaml"
        );
        PATH_MIGRATIONS.put(
            "examples/lookups/hierarchical-lookup.yaml",
            "demos/patterns/lookups/hierarchical-lookup.yaml"
        );
        
        // Financial settlement
        PATH_MIGRATIONS.put(
            "financial-settlement/comprehensive-settlement-enrichment.yaml",
            "demos/industry/financial-services/settlement/comprehensive-settlement-enrichment.yaml"
        );
        
        // Configuration files
        PATH_MIGRATIONS.put(
            "config/financial-validation-rules.yaml",
            "demos/fundamentals/rules/financial-validation-rules.yaml"
        );
        PATH_MIGRATIONS.put(
            "config/data-type-scenarios.yaml",
            "demos/fundamentals/datasets/data-type-scenarios.yaml"
        );
        
        // Demo rules
        PATH_MIGRATIONS.put(
            "demo-rules/custody-auto-repair-rules.yaml",
            "demos/industry/financial-services/custody/custody-auto-repair-rules.yaml"
        );
        PATH_MIGRATIONS.put(
            "demo-rules/quick-start.yaml",
            "demos/quickstart/quick-start.yaml"
        );
        
        // Demo configs
        PATH_MIGRATIONS.put(
            "demo-configs/comprehensive-lookup-demo.yaml",
            "demos/patterns/lookups/comprehensive-lookup-demo.yaml"
        );
        
        // Scenarios
        PATH_MIGRATIONS.put(
            "scenarios/otc-options-scenario.yaml",
            "demos/advanced/complex-scenarios/otc-options-scenario.yaml"
        );
        PATH_MIGRATIONS.put(
            "scenarios/commodity-swaps-scenario.yaml",
            "demos/advanced/complex-scenarios/commodity-swaps-scenario.yaml"
        );
        PATH_MIGRATIONS.put(
            "scenarios/settlement-auto-repair-scenario.yaml",
            "demos/advanced/complex-scenarios/settlement-auto-repair-scenario.yaml"
        );
        
        // YAML examples
        PATH_MIGRATIONS.put(
            "yaml-examples/file-processing-config.yaml",
            "reference/syntax-examples/file-processing-config.yaml"
        );
    }
    
    /**
     * Resolves an old resource path to its new organized location.
     * If no mapping exists, returns the original path unchanged.
     *
     * @param originalPath the original resource path
     * @return the new organized path, or original path if no mapping exists
     * @deprecated As of Phase 4, use new paths directly. This method is kept for historical reference.
     */
    @Deprecated
    public static String resolvePath(String originalPath) {
        if (originalPath == null) {
            return null;
        }
        
        return PATH_MIGRATIONS.getOrDefault(originalPath, originalPath);
    }
    
    /**
     * Checks if a path has a migration mapping.
     *
     * @param originalPath the original resource path
     * @return true if a migration mapping exists, false otherwise
     * @deprecated As of Phase 4, use new paths directly. This method is kept for historical reference.
     */
    @Deprecated
    public static boolean hasMigration(String originalPath) {
        return originalPath != null && PATH_MIGRATIONS.containsKey(originalPath);
    }
    
    /**
     * Gets all path migrations for debugging and validation purposes.
     *
     * @return unmodifiable map of all path migrations
     * @deprecated As of Phase 4, use new paths directly. This method is kept for historical reference.
     */
    @Deprecated
    public static Map<String, String> getAllMigrations() {
        return Map.copyOf(PATH_MIGRATIONS);
    }
    
    /**
     * Validates that all new paths exist in the classpath.
     * This method is intended for testing and validation.
     *
     * @return true if all new paths exist, false otherwise
     * @deprecated As of Phase 4, use new paths directly. This method is kept for historical reference.
     */
    @Deprecated
    public static boolean validateAllNewPathsExist() {
        ClassLoader classLoader = ResourcePathResolver.class.getClassLoader();
        
        for (String newPath : PATH_MIGRATIONS.values()) {
            if (classLoader.getResource(newPath) == null) {
                System.err.println("Missing resource: " + newPath);
                return false;
            }
        }
        
        return true;
    }
}
