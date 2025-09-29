package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.config.yaml.YamlEnrichment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichmentGroupModelTest {

    @Test
    @DisplayName("Adds enrichments by sequence and returns them in order")
    void testOrderingBySequence() {
        EnrichmentGroup group = new EnrichmentGroup("eg-1");

        YamlEnrichment e2 = new YamlEnrichment();
        e2.setId("E2");
        e2.setPriority(20);

        YamlEnrichment e1 = new YamlEnrichment();
        e1.setId("E1");
        e1.setPriority(10);

        group.addEnrichment(2, e2);
        group.addEnrichment(1, e1);

        List<YamlEnrichment> ordered = group.getEnrichmentsInOrder();
        assertEquals(2, ordered.size());
        assertEquals("E1", ordered.get(0).getId(), "Sequence 1 should come first");
        assertEquals("E2", ordered.get(1).getId(), "Sequence 2 should come second");
    }

    @Test
    @DisplayName("getEnrichmentsBySequence is unmodifiable and not a live view")
    void testUnmodifiableMap() {
        EnrichmentGroup group = new EnrichmentGroup("eg-2");

        YamlEnrichment e1 = new YamlEnrichment();
        e1.setId("E1");
        group.addEnrichment(1, e1);

        Map<Integer, YamlEnrichment> snapshot = group.getEnrichmentsBySequence();
        assertEquals(1, snapshot.size());

        // Attempt to modify should throw
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put(2, new YamlEnrichment()));
    }
}

