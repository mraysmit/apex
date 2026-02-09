package dev.mars.apex.core.config.yaml;

import dev.mars.apex.core.config.yaml.sequential.SectionRegistry;

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

import org.junit.jupiter.api.BeforeEach;

import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SectionRegistry performance optimization.
 *
 * @author APEX Performance Testing
 * @since 2.1
 */
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class SectionRegistryTest {

    private SectionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = SectionRegistry.getInstance();
        registry.clearCaches(); // Start fresh for each test
    }

    @Test
    @DisplayName("Should normalize numbered enrichment sections to base name")
    void testNormalizeNumberedEnrichments() {
        assertEquals("enrichments", registry.getNormalizedName("enrichments-1"));
        assertEquals("enrichments", registry.getNormalizedName("enrichments-2"));
        assertEquals("enrichments", registry.getNormalizedName("enrichments-5"));
        assertEquals("enrichments", registry.getNormalizedName("enrichments-10"));
    }

    @Test
    @DisplayName("Should normalize numbered rule sections to base name")
    void testNormalizeNumberedRules() {
        assertEquals("rules", registry.getNormalizedName("rules-1"));
        assertEquals("rules", registry.getNormalizedName("rules-10"));
        assertEquals("rules", registry.getNormalizedName("rules-99"));
    }

    @Test
    @DisplayName("Should normalize all supported numbered section types")
    void testNormalizeAllSectionTypes() {
        assertEquals("enrichment-groups", registry.getNormalizedName("enrichment-groups-1"));
        assertEquals("rule-groups", registry.getNormalizedName("rule-groups-2"));
        assertEquals("transformations", registry.getNormalizedName("transformations-3"));
        assertEquals("rule-chains", registry.getNormalizedName("rule-chains-4"));
        assertEquals("enrichment-refs", registry.getNormalizedName("enrichment-refs-5"));
        assertEquals("rule-refs", registry.getNormalizedName("rule-refs-6"));
    }

    @Test
    @DisplayName("Should return original name for base sections")
    void testBaseSections() {
        assertEquals("enrichments", registry.getNormalizedName("enrichments"));
        assertEquals("rules", registry.getNormalizedName("rules"));
        assertEquals("enrichment-groups", registry.getNormalizedName("enrichment-groups"));
        assertEquals("rule-groups", registry.getNormalizedName("rule-groups"));
    }

    @Test
    @DisplayName("Should return original name for non-numbered sections")
    void testNonNumberedSections() {
        assertEquals("metadata", registry.getNormalizedName("metadata"));
        assertEquals("pipeline", registry.getNormalizedName("pipeline"));
        assertEquals("error-recovery", registry.getNormalizedName("error-recovery"));
        assertEquals("data-sources", registry.getNormalizedName("data-sources"));
    }

    @Test
    @DisplayName("Should identify numbered section strategy")
    void testNumberedSectionStrategy() {
        assertEquals(SectionRegistry.MergeStrategy.NUMBERED_SECTION,
                    registry.getMergeStrategy("enrichments-1"));
        assertEquals(SectionRegistry.MergeStrategy.NUMBERED_SECTION,
                    registry.getMergeStrategy("rules-5"));
        assertEquals(SectionRegistry.MergeStrategy.NUMBERED_SECTION,
                    registry.getMergeStrategy("transformations-10"));
    }

    @Test
    @DisplayName("Should identify base section strategy")
    void testBaseSectionStrategy() {
        assertEquals(SectionRegistry.MergeStrategy.BASE_SECTION,
                    registry.getMergeStrategy("enrichments"));
        assertEquals(SectionRegistry.MergeStrategy.BASE_SECTION,
                    registry.getMergeStrategy("rules"));
        assertEquals(SectionRegistry.MergeStrategy.BASE_SECTION,
                    registry.getMergeStrategy("metadata"));
    }

    @Test
    @DisplayName("Should cache lookups for performance")
    void testCaching() {
        // First lookup - computes
        String result1 = registry.getNormalizedName("enrichments-100");

        // Second lookup - should return cached value
        String result2 = registry.getNormalizedName("enrichments-100");

        assertEquals(result1, result2);
        assertSame(result1, result2, "Should return same cached instance");
    }

    @Test
    @DisplayName("Should pre-warm cache with common patterns")
    void testPreWarming() {
        // Registry should pre-cache enrichments-1 through enrichments-10
        int initialSize = registry.getCacheSize();

        // These should be pre-cached (no computation needed)
        registry.getNormalizedName("enrichments-1");
        registry.getNormalizedName("enrichments-5");
        registry.getNormalizedName("enrichments-10");

        // Cache size should not grow (already pre-cached)
        assertEquals(initialSize, registry.getCacheSize(),
                    "Pre-warmed entries should not increase cache size");
    }

    @Test
    @DisplayName("Should handle null gracefully")
    void testNullHandling() {
        assertNull(registry.getNormalizedName(null));
        assertEquals(SectionRegistry.MergeStrategy.BASE_SECTION,
                    registry.getMergeStrategy(null));
    }

    @Test
    @DisplayName("Should handle invalid numbered patterns")
    void testInvalidNumberedPatterns() {
        // These look like numbered sections but aren't in BASE_SECTIONS
        assertEquals("unknown-section-1", registry.getNormalizedName("unknown-section-1"));
        assertEquals("custom-1", registry.getNormalizedName("custom-1"));

        assertEquals(SectionRegistry.MergeStrategy.BASE_SECTION,
                    registry.getMergeStrategy("unknown-section-1"));
    }

    @Test
    @DisplayName("Should handle edge case numbers")
    void testEdgeCaseNumbers() {
        assertEquals("enrichments", registry.getNormalizedName("enrichments-0"));
        assertEquals("enrichments", registry.getNormalizedName("enrichments-999"));
        assertEquals("enrichments", registry.getNormalizedName("enrichments-12345"));
    }

    @Test
    @DisplayName("Should be thread-safe singleton")
    void testSingleton() {
        SectionRegistry instance1 = SectionRegistry.getInstance();
        SectionRegistry instance2 = SectionRegistry.getInstance();

        assertSame(instance1, instance2, "Should return same singleton instance");
    }

    @Test
    @DisplayName("Should clear caches properly")
    void testCacheClear() {
        // Add some entries
        registry.getNormalizedName("enrichments-999");
        registry.getNormalizedName("rules-888");

        int sizeBefore = registry.getCacheSize();
        assertTrue(sizeBefore > 0, "Cache should have entries");

        // Clear
        registry.clearCaches();

        // Should be reset to pre-warmed state
        int sizeAfter = registry.getCacheSize();
        assertTrue(sizeAfter > 0, "Cache should be pre-warmed after clear");
    }
}

