package dev.mars.apex.yaml.manager.service;

import dev.mars.apex.yaml.manager.model.Catalog;
import dev.mars.apex.yaml.manager.model.ConfigMetadata;
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
        ConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        assertEquals(1, catalogService.getTotalConfigurations());
        assertNotNull(catalogService.getConfiguration("rule-1"));
    }

    @Test
    public void testRemoveConfiguration() {
        ConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);
        catalogService.removeConfiguration("rule-1");

        assertEquals(0, catalogService.getTotalConfigurations());
        assertNull(catalogService.getConfiguration("rule-1"));
    }

    @Test
    public void testGetConfiguration() {
        ConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalogService.addConfiguration(metadata);

        ConfigMetadata retrieved = catalogService.getConfiguration("rule-1");
        assertNotNull(retrieved);
        assertEquals("rule-1", retrieved.getId());
        assertEquals("Rule 1", retrieved.getName());
    }

    @Test
    public void testGetAllConfigurations() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        Collection<ConfigMetadata> all = catalogService.getAllConfigurations();
        assertEquals(2, all.size());
    }

    @Test
    public void testFindByTag() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addTag("compliance");
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addTag("compliance");
        ConfigMetadata metadata3 = createMetadata("rule-3", "rule-config", "Rule 3");
        metadata3.addTag("risk");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);
        catalogService.addConfiguration(metadata3);

        List<ConfigMetadata> complianceRules = catalogService.findByTag("compliance");
        assertEquals(2, complianceRules.size());

        List<ConfigMetadata> riskRules = catalogService.findByTag("risk");
        assertEquals(1, riskRules.size());
    }

    @Test
    public void testFindByBusinessDomain() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setBusinessDomain("Trade Validation");
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setBusinessDomain("Data Enrichment");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<ConfigMetadata> validationRules = catalogService.findByMetadataAttribute("business-domain", "Trade Validation");
        assertEquals(1, validationRules.size());

        List<ConfigMetadata> enrichmentRules = catalogService.findByMetadataAttribute("business-domain", "Data Enrichment");
        assertEquals(1, enrichmentRules.size());
    }

    @Test
    public void testFindByType() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        ConfigMetadata metadata2 = createMetadata("enrichment-1", "enrichment", "Enrichment 1");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<ConfigMetadata> ruleConfigs = catalogService.findByType("rule-config");
        assertEquals(1, ruleConfigs.size());

        List<ConfigMetadata> enrichments = catalogService.findByType("enrichment");
        assertEquals(1, enrichments.size());
    }

    @Test
    public void testFindByAuthor() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setAuthor("alice");
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setAuthor("bob");

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<ConfigMetadata> aliceRules = catalogService.findByAuthor("alice");
        assertEquals(1, aliceRules.size());

        List<ConfigMetadata> bobRules = catalogService.findByAuthor("bob");
        assertEquals(1, bobRules.size());
    }

    @Test
    public void testFindUnused() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setOrphaned(false);

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<ConfigMetadata> unused = catalogService.findUnused();
        assertEquals(1, unused.size());
        assertEquals("rule-1", unused.get(0).getId());
    }

    @Test
    public void testFindCritical() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setCritical(true);
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setCritical(false);

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);

        List<ConfigMetadata> critical = catalogService.findCritical();
        assertEquals(1, critical.size());
        assertEquals("rule-1", critical.get(0).getId());
    }

    @Test
    public void testFindByHealthScore() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setHealthScore(85);
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setHealthScore(45);
        ConfigMetadata metadata3 = createMetadata("rule-3", "rule-config", "Rule 3");
        metadata3.setHealthScore(25);

        catalogService.addConfiguration(metadata1);
        catalogService.addConfiguration(metadata2);
        catalogService.addConfiguration(metadata3);

        List<ConfigMetadata> goodHealth = catalogService.findByHealthScore(80, 100);
        assertEquals(1, goodHealth.size());

        List<ConfigMetadata> fairHealth = catalogService.findByHealthScore(40, 60);
        assertEquals(1, fairHealth.size());
    }

    @Test
    public void testGetCatalogStatistics() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        metadata1.setCritical(false);
        metadata1.setHealthScore(80);

        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
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
        Catalog catalog = catalogService.getCatalog();
        assertNotNull(catalog);
        assertEquals(0, catalog.getTotalConfigurations());
    }

    private ConfigMetadata createMetadata(String id, String type, String name) {
        ConfigMetadata metadata = new ConfigMetadata();
        metadata.setId(id);
        metadata.setType(type);
        metadata.setName(name);
        metadata.setPath("/configs/" + id + ".yaml");
        metadata.setDescription("Test configuration for " + name);
        return metadata;
    }
}

