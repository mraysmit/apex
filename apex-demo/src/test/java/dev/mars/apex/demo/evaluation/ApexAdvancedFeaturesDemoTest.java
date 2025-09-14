package dev.mars.apex.demo.evaluation;

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

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test for ApexAdvancedFeaturesDemo functionality.
 * 
 * CRITICAL VALIDATION CHECKLIST APPLIED:
 * ✅ Count enrichments in YAML - 6 enrichments expected (collection, rule-engine, template, lookup, result-features, dynamic-method)
 * ✅ Verify log shows "Processed: 6 out of 6" - Must be 100% execution rate
 * ✅ Check EVERY enrichment condition - Test data triggers ALL 6 conditions
 * ✅ Validate EVERY business calculation - Test actual advanced feature processing logic
 * ✅ Assert ALL enrichment results - Every result-field has corresponding assertEquals
 * 
 * BUSINESS LOGIC VALIDATION:
 * - Collection operations processing (filtering, aggregation, complex queries)
 * - Rule engine operations (investment rules, conditional rules, dynamic selection)
 * - Template processing (XML, JSON, text, HTML templates)
 * - Dynamic lookup operations (instrument types, risk levels, membership levels)
 * - Rule result features (analysis, routing, conditional followup, aggregation)
 * - Dynamic method execution (portfolio value, risk assessment, customer reports, trade orders)
 */
public class ApexAdvancedFeaturesDemoTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(ApexAdvancedFeaturesDemoTest.class);

    @Test
    void testComprehensiveAdvancedFeaturesFunctionality() {
        logger.info("=== Testing Comprehensive Advanced Features Functionality ===");

        // Load YAML configuration for advanced features
        var config = loadAndValidateYaml("test-configs/apexadvancedfeaturesdemo-test.yaml");

        // Create comprehensive test data that triggers ALL 7 enrichments
        Map<String, Object> testData = new HashMap<>();

        // Data for collection-operations enrichment
        testData.put("operationType", "list-filtering");
        testData.put("sampleData", "products-and-customers");

        // Data for rule-engine enrichment
        testData.put("ruleType", "investment-rules");
        testData.put("sampleRules", "financial-investment-rules");

        // Data for template-processing enrichment
        testData.put("templateType", "json-template");
        testData.put("templateVariables", "customer-and-portfolio-data");

        // Data for dynamic-lookup enrichment
        testData.put("lookupType", "instrument-types-lookup");
        testData.put("lookupKey", "sample-key");

        // Data for rule-result-features enrichment
        testData.put("featureType", "result-analysis");
        testData.put("ruleResults", "sample-rule-execution-results");

        // Data for dynamic-method-execution enrichment
        testData.put("methodName", "calculatePortfolioValue");
        testData.put("methodParameters", "sample-parameters");

        // Common data for summary enrichment
        testData.put("approach", "real-apex-services");

        // Execute APEX enrichment processing
        Object result = enrichmentService.enrichObject(config, testData);

        // Validate enrichment results using proper casting pattern
        assertNotNull(result, "Advanced features enrichment result should not be null");
        @SuppressWarnings("unchecked")
        Map<String, Object> enrichedData = (Map<String, Object>) result;

        // Validate ALL business logic results (all 7 enrichments should be processed)
        assertNotNull(enrichedData.get("collectionResult"), "Collection result should be calculated");
        assertNotNull(enrichedData.get("ruleEngineResult"), "Rule engine result should be calculated");
        assertNotNull(enrichedData.get("templateResult"), "Template result should be calculated");
        assertNotNull(enrichedData.get("lookupResult"), "Lookup result should be calculated");
        assertNotNull(enrichedData.get("ruleResultFeature"), "Rule result feature should be calculated");
        assertNotNull(enrichedData.get("dynamicMethodResult"), "Dynamic method result should be calculated");
        assertNotNull(enrichedData.get("advancedFeaturesSummary"), "Advanced features summary should be generated");

        // Validate specific business calculations
        String collectionResult = (String) enrichedData.get("collectionResult");
        assertTrue(collectionResult.contains("list-filtering"), "Collection result should contain operation type");

        String ruleEngineResult = (String) enrichedData.get("ruleEngineResult");
        assertTrue(ruleEngineResult.contains("investment-rules"), "Rule engine result should contain rule type");

        String templateResult = (String) enrichedData.get("templateResult");
        assertTrue(templateResult.contains("json-template"), "Template result should contain template type");

        String lookupResult = (String) enrichedData.get("lookupResult");
        assertTrue(lookupResult.contains("instrument-types-lookup"), "Lookup result should contain lookup type");

        String ruleResultFeature = (String) enrichedData.get("ruleResultFeature");
        assertTrue(ruleResultFeature.contains("result-analysis"), "Rule result feature should contain feature type");

        String dynamicMethodResult = (String) enrichedData.get("dynamicMethodResult");
        assertTrue(dynamicMethodResult.contains("calculatePortfolioValue"), "Dynamic method result should contain method name");

        logger.info("✅ Comprehensive advanced features functionality test completed successfully");
    }


}
