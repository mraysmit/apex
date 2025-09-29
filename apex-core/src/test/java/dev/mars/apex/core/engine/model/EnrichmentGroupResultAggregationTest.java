package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.constants.SeverityConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichmentGroupResultAggregationTest {

    @Test
    @DisplayName("Aggregated severity is ERROR if any child is ERROR")
    void testAggregateError() {
        Map<String, Object> data = new HashMap<>();
        RuleResult info = RuleResult.enrichmentSuccess(data, SeverityConstants.INFO);
        RuleResult warn = RuleResult.enrichmentSuccess(data, SeverityConstants.WARNING);
        RuleResult error = RuleResult.enrichmentFailure(List.of("fail"), data, SeverityConstants.ERROR);

        EnrichmentGroupResult result = EnrichmentGroupResult.of("eg-agg", true, "msg",
                List.of(info, warn, error), 5);

        assertEquals(SeverityConstants.ERROR, result.getAggregatedSeverity());
    }

    @Test
    @DisplayName("Aggregated severity is WARNING if no ERROR but any WARNING")
    void testAggregateWarning() {
        Map<String, Object> data = new HashMap<>();
        RuleResult info = RuleResult.enrichmentSuccess(data, SeverityConstants.INFO);
        RuleResult warn = RuleResult.enrichmentSuccess(data, SeverityConstants.WARNING);

        EnrichmentGroupResult result = EnrichmentGroupResult.of("eg-agg", true, "msg",
                List.of(info, warn), 5);

        assertEquals(SeverityConstants.WARNING, result.getAggregatedSeverity());
    }

    @Test
    @DisplayName("Aggregated severity defaults to INFO when only INFO or empty")
    void testAggregateInfoOrEmpty() {
        Map<String, Object> data = new HashMap<>();
        RuleResult info = RuleResult.enrichmentSuccess(data, SeverityConstants.INFO);

        EnrichmentGroupResult onlyInfo = EnrichmentGroupResult.of("eg-agg", true, "msg",
                List.of(info), 5);
        assertEquals(SeverityConstants.INFO, onlyInfo.getAggregatedSeverity());

        EnrichmentGroupResult empty = EnrichmentGroupResult.of("eg-agg", true, "msg",
                List.of(), 1);
        assertEquals(SeverityConstants.INFO, empty.getAggregatedSeverity());
    }
}

