package dev.mars.apex.yaml.manager.service;

import dev.mars.apex.yaml.manager.model.YamlCatalog;
import dev.mars.apex.yaml.manager.model.YamlConfigMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CatalogService.
 */
public class CatalogServiceTest {

    private CatalogService catalogService;

    @BeforeEach
    public void setUp() {
        catalogService = new CatalogService();
    }

    @Test
    public void testAddConfiguration() {
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        assertEquals(1, catalogService.getTotalConfigurations());
        assertNotNull(catalogService.getConfiguration("rule-1"));
    }

    @Test
    public void testRemoveConfiguration() {
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);
        catalogService.removeConfiguration("rule-1");

        assertEquals(0, catalogService.getTotalConfigurations());
        assertNull(catalogService.getConfiguration("rule-1"));
    }

    @Test
    public void testGetConfiguration() {
        YamlConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        YamlConfigMetadata retrieved = catalogService.getConfiguration("rule-1");
        assertNotNull(retrieved);
        assertEquals("rule-1", retrieved.getId());
        assertEquals("Rule 1", retrieved.getName());
    }

    @Test
    public void testGetAllConfigurations() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        Collection<YamlConfigMetadata> all = catalogService.getAllConfigurations();
        assertEquals(2, all.size());
    }

    @Test
    public void testFindByTag() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addTag("compliance");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addTag("compliance");
        YamlConfigMetadata metadata3 = createMetadata("rule-3", "rule-config", "Rule 3");
        metadata3.addTag("risk");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);
        catalogService.addConfiguration(metadata3);

        List<YamlConfigMetadata> complianceRules = catalogService.findByTag("compliance");
        assertEquals(2, complianceRules.size());

        List<YamlConfigMetadata> riskRules = catalogService.findByTag("risk");
        assertEquals(1, riskRules.size());
    }

    @Test
    public void testFindByCategory() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addCategory("validation");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addCategory("enrichment");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<YamlConfigMetadata> validationRules = catalogService.findByCategory("validation");
        assertEquals(1, validationRules.size());

        List<YamlConfigMetadata> enrichmentRules = catalogService.findByCategory("enrichment");
        assertEquals(1, enrichmentRules.size());
    }

    @Test
    public void testFindByType() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        YamlConfigMetadata metadata2 = createMetadata("enrichment-1", "enrichment", "Enrichment 1");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<YamlConfigMetadata> ruleConfigs = catalogService.findByType("rule-config");
        assertEquals(1, ruleConfigs.size());

        List<YamlConfigMetadata> enrichments = catalogService.findByType("enrichment");
        assertEquals(1, enrichments.size());
    }

    @Test
    public void testFindByAuthor() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setAuthor("alice");
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setAuthor("bob");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<YamlConfigMetadata> aliceRules = catalogService.findByAuthor("alice");
        assertEquals(1, aliceRules.size());

        List<YamlConfigMetadata> bobRules = catalogService.findByAuthor("bob");
        assertEquals(1, bobRules.size());
    }

    @Test
    public void testFindUnused() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setOrphaned(false);

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<YamlConfigMetadata> unused = catalogService.findUnused();
        assertEquals(1, unused.size());
        assertEquals("rule-1", unused.get(0).getId());
    }

    @Test
    public void testFindCritical() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setCritical(true);
        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setCritical(false);

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<YamlConfigMetadata> critical = catalogService.findCritical();
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

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);
        catalogService.addConfiguration(metadata3);

        List<YamlConfigMetadata> goodHealth = catalogService.findByHealthScore(80, 100);
        assertEquals(1, goodHealth.size());

        List<YamlConfigMetadata> fairHealth = catalogService.findByHealthScore(40, 60);
        assertEquals(1, fairHealth.size());
    }

    @Test
    public void testGetCatalogStatistics() {
        YamlConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        metadata1.setCritical(false);
        metadata1.setHealthScore(80);

        YamlConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setOrphaned(false);
        metadata2.setCritical(true);
        metadata2.setHealthScore(60);

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        assertEquals(2, catalogService.getTotalConfigurations());
        assertEquals(1, catalogService.getOrphanedCount());
        assertEquals(1, catalogService.getCriticalCount());
        assertEquals(70.0, catalogService.getAverageHealthScore());
    }

    @Test
    public void testGetCatalog() {
        YamlCatalog catalog = catalogService.getCatalog();
        assertNotNull(catalog);
        assertEquals(0, catalog.getTotalConfigurations());
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

