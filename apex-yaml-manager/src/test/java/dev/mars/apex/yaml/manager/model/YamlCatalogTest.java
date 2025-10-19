package dev.mars.apex.yaml.manager.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for YamlCatalog.
 */
public class YamlCatalogTest {

    private YamlCatalog catalog;

    @BeforeEach
    public void setUp() {
        catalog = new YamlCatalog();
    }

    @Test
    public void testAddConfiguration() {
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalog.addConfiguration(metadata);

        assertEquals(1, catalog.getTotalConfigurations());
        assertNotNull(catalog.getConfiguration("rule-1"));
    }

    @Test
    public void testRemoveConfiguration() {
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalog.addConfiguration(metadata);
        catalog.removeConfiguration("rule-1");

        assertEquals(0, catalog.getTotalConfigurations());
        assertNull(catalog.getConfiguration("rule-1"));
    }

    @Test
    public void testGetConfiguration() {
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalog.addConfiguration(metadata);

        YamlConfigMetadata retrieved = catalog.getConfiguration("rule-1");
        assertNotNull(retrieved);
        assertEquals("rule-1", retrieved.getId());
        assertEquals("Rule 1", retrieved.getName());
    }

    @Test
    public void testFindByTag() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addTag("compliance");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addTag("compliance");
        YamlConfigMetadata metadata3 = createMetadata("rule-3", "rule-config", "Rule 3");
        metadata3.addTag("risk");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);
        catalog.addConfiguration(metadata3);

        List<YamlConfigMetadata> complianceRules = catalog.findByTag("compliance");
        assertEquals(2, complianceRules.size());

        List<YamlConfigMetadata> riskRules = catalog.findByTag("risk");
        assertEquals(1, riskRules.size());
    }

    @Test
    public void testFindByCategory() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addCategory("validation");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addCategory("enrichment");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<YamlConfigMetadata> validationRules = catalog.findByCategory("validation");
        assertEquals(1, validationRules.size());

        List<YamlConfigMetadata> enrichmentRules = catalog.findByCategory("enrichment");
        assertEquals(1, enrichmentRules.size());
    }

    @Test
    public void testFindByType() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        YamlConfigMetadata metadata2 = createMetadata("enrichment-1", "enrichment", "Enrichment 1");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<YamlConfigMetadata> ruleConfigs = catalog.findByType("rule-config");
        assertEquals(1, ruleConfigs.size());

        List<YamlConfigMetadata> enrichments = catalog.findByType("enrichment");
        assertEquals(1, enrichments.size());
    }

    @Test
    public void testFindByAuthor() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setAuthor("alice");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setAuthor("bob");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<YamlConfigMetadata> aliceRules = catalog.findByAuthor("alice");
        assertEquals(1, aliceRules.size());

        List<YamlConfigMetadata> bobRules = catalog.findByAuthor("bob");
        assertEquals(1, bobRules.size());
    }

    @Test
    public void testFindUnused() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setOrphaned(false);

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<YamlConfigMetadata> unused = catalog.findUnused();
        assertEquals(1, unused.size());
        assertEquals("rule-1", unused.get(0).getId());
    }

    @Test
    public void testFindCritical() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setCritical(true);
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setCritical(false);

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<YamlConfigMetadata> critical = catalog.findCritical();
        assertEquals(1, critical.size());
        assertEquals("rule-1", critical.get(0).getId());
    }

    @Test
    public void testFindByHealthScore() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setHealthScore(85);
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setHealthScore(45);
        YamlConfigMetadata metadata3 = createMetadata("rule-3", "rule-config", "Rule 3");
        metadata3.setHealthScore(25);

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);
        catalog.addConfiguration(metadata3);

        List<YamlConfigMetadata> goodHealth = catalog.findByHealthScore(80, 100);
        assertEquals(1, goodHealth.size());

        List<YamlConfigMetadata> fairHealth = catalog.findByHealthScore(40, 60);
        assertEquals(1, fairHealth.size());
    }

    @Test
    public void testStatistics() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        metadata1.setCritical(false);
        metadata1.setHealthScore(80);

        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setOrphaned(false);
        metadata2.setCritical(true);
        metadata2.setHealthScore(60);

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getTotalConfigurations());
        assertEquals(1, catalog.getOrphanedCount());
        assertEquals(1, catalog.getCriticalCount());
        assertEquals(70.0, catalog.getAverageHealthScore());
    }

    @Test
    public void testGetAllTags() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addTag("compliance");
        metadata1.addTag("validation");

        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addTag("compliance");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllTags().size());
        assertTrue(catalog.getAllTags().contains("compliance"));
        assertTrue(catalog.getAllTags().contains("validation"));
    }

    @Test
    public void testGetAllCategories() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addCategory("validation");

        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addCategory("enrichment");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllCategories().size());
        assertTrue(catalog.getAllCategories().contains("validation"));
        assertTrue(catalog.getAllCategories().contains("enrichment"));
    }

    @Test
    public void testGetAllTypes() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        YamlConfigMetadata metadata2 = createMetadata("enrichment-1", "enrichment", "Enrichment 1");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllTypes().size());
        assertTrue(catalog.getAllTypes().contains("rule-config"));
        assertTrue(catalog.getAllTypes().contains("enrichment"));
    }

    @Test
    public void testGetAllAuthors() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setAuthor("alice");

        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setAuthor("bob");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllAuthors().size());
        assertTrue(catalog.getAllAuthors().contains("alice"));
        assertTrue(catalog.getAllAuthors().contains("bob"));
    }

    private YamlConfigMetadata createMetadata(String id, String type, String name) {
        YamlConfigMetadata metadata = new YamlConfigMetadata();
        metadata.setId(id);
        metadata.setType(type);
        metadata.setName(name);
        metadata.setPath("/configs/" + id + ".yaml");
        metadata.setDescription("Test configuration for " + name);
        return metadata;
    }
}

