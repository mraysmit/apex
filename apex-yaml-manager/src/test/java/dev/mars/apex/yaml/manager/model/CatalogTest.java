package dev.mars.apex.yaml.manager.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Catalog.
 */
public class CatalogTest {

    private Catalog catalog;

    @BeforeEach
    public void setUp() {
        catalog = new Catalog();
    }

    @Test
    public void testAddConfiguration() {
        ConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalog.addConfiguration(metadata);

        assertEquals(1, catalog.getTotalConfigurations());
        assertNotNull(catalog.getConfiguration("rule-1"));
    }

    @Test
    public void testRemoveConfiguration() {
        ConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalog.addConfiguration(metadata);
        catalog.removeConfiguration("rule-1");

        assertEquals(0, catalog.getTotalConfigurations());
        assertNull(catalog.getConfiguration("rule-1"));
    }

    @Test
    public void testGetConfiguration() {
        ConfigMetadata metadata = createMetadata("rule-1", "rule-config", "Rule 1");
        catalog.addConfiguration(metadata);

        ConfigMetadata retrieved = catalog.getConfiguration("rule-1");
        assertNotNull(retrieved);
        assertEquals("rule-1", retrieved.getId());
        assertEquals("Rule 1", retrieved.getName());
    }

    @Test
    public void testFindByTag() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addTag("compliance");
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addTag("compliance");
        ConfigMetadata metadata3 = createMetadata("rule-3", "rule-config", "Rule 3");
        metadata3.addTag("risk");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);
        catalog.addConfiguration(metadata3);

        List<ConfigMetadata> complianceRules = catalog.findByTag("compliance");
        assertEquals(2, complianceRules.size());

        List<ConfigMetadata> riskRules = catalog.findByTag("risk");
        assertEquals(1, riskRules.size());
    }

    @Test
    public void testFindByBusinessDomain() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setBusinessDomain("Trade Validation");
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setBusinessDomain("Data Enrichment");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<ConfigMetadata> validationRules = catalog.findByMetadataAttribute("business-domain", "Trade Validation");
        assertEquals(1, validationRules.size());

        List<ConfigMetadata> enrichmentRules = catalog.findByMetadataAttribute("business-domain", "Data Enrichment");
        assertEquals(1, enrichmentRules.size());
    }

    @Test
    public void testFindByType() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        ConfigMetadata metadata2 = createMetadata("enrichment-1", "enrichment", "Enrichment 1");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<ConfigMetadata> ruleConfigs = catalog.findByType("rule-config");
        assertEquals(1, ruleConfigs.size());

        List<ConfigMetadata> enrichments = catalog.findByType("enrichment");
        assertEquals(1, enrichments.size());
    }

    @Test
    public void testFindByAuthor() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setAuthor("alice");
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setAuthor("bob");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<ConfigMetadata> aliceRules = catalog.findByAuthor("alice");
        assertEquals(1, aliceRules.size());

        List<ConfigMetadata> bobRules = catalog.findByAuthor("bob");
        assertEquals(1, bobRules.size());
    }

    @Test
    public void testFindUnused() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setOrphaned(false);

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<ConfigMetadata> unused = catalog.findUnused();
        assertEquals(1, unused.size());
        assertEquals("rule-1", unused.get(0).getId());
    }

    @Test
    public void testFindCritical() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setCritical(true);
        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setCritical(false);

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        List<ConfigMetadata> critical = catalog.findCritical();
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

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);
        catalog.addConfiguration(metadata3);

        List<ConfigMetadata> goodHealth = catalog.findByHealthScore(80, 100);
        assertEquals(1, goodHealth.size());

        List<ConfigMetadata> fairHealth = catalog.findByHealthScore(40, 60);
        assertEquals(1, fairHealth.size());
    }

    @Test
    public void testStatistics() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setOrphaned(true);
        metadata1.setCritical(false);
        metadata1.setHealthScore(80);

        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
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
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.addTag("compliance");
        metadata1.addTag("validation");

        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.addTag("compliance");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllTags().size());
        assertTrue(catalog.getAllTags().contains("compliance"));
        assertTrue(catalog.getAllTags().contains("validation"));
    }

    @Test
    public void testGetAllBusinessDomains() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setBusinessDomain("Trade Validation");

        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setBusinessDomain("Data Enrichment");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllBusinessDomains().size());
        assertTrue(catalog.getAllBusinessDomains().contains("Trade Validation"));
        assertTrue(catalog.getAllBusinessDomains().contains("Data Enrichment"));
    }

    @Test
    public void testGetAllTypes() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        ConfigMetadata metadata2 = createMetadata("enrichment-1", "enrichment", "Enrichment 1");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllTypes().size());
        assertTrue(catalog.getAllTypes().contains("rule-config"));
        assertTrue(catalog.getAllTypes().contains("enrichment"));
    }

    @Test
    public void testGetAllAuthors() {
        ConfigMetadata metadata1 = createMetadata("rule-1", "rule-config", "Rule 1");
        metadata1.setAuthor("alice");

        ConfigMetadata metadata2 = createMetadata("rule-2", "rule-config", "Rule 2");
        metadata2.setAuthor("bob");

        catalog.addConfiguration(metadata1);
        catalog.addConfiguration(metadata2);

        assertEquals(2, catalog.getAllAuthors().size());
        assertTrue(catalog.getAllAuthors().contains("alice"));
        assertTrue(catalog.getAllAuthors().contains("bob"));
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

