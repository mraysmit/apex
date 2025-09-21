/*
 * Copyright (c) 2024 Augment Code Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of Augment Code Inc.
 * ("Confidential Information"). You shall not disclose such Confidential Information
 * and shall use it only in accordance with the terms of the license agreement you
 * entered into with Augment Code Inc.
 *
 * Author: apex.demo.team@company.com
 * Created: 2024-12-24
 */

package dev.mars.apex.demo.lookup;

import dev.mars.apex.demo.infrastructure.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Required Field Validation Test")
class RequiredFieldValidationTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(RequiredFieldValidationTest.class);

    @Test
    @DisplayName("Test required field works when field exists")
    void testRequiredFieldExists() {
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RequiredFieldValidationTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("id", "1");

            Object result = testEnrichment(config, inputData);
            Map<String, Object> resultMap = (Map<String, Object>) result;

            assertEquals("Test1", resultMap.get("resultName"));
            logger.info("Required field test passed: {}", resultMap.get("resultName"));
        } catch (Exception e) {
            logger.error("Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test required field fails when field missing")
    void testRequiredFieldMissing() {
        try {
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/RequiredFieldValidationTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            Map<String, Object> inputData = new HashMap<>();
            inputData.put("id", "999"); // Non-existent ID

            Object result = testEnrichment(config, inputData);
            Map<String, Object> resultMap = (Map<String, Object>) result;

            // Should not have the field since lookup failed
            assertNull(resultMap.get("resultName"));
            logger.info("Required field test with missing data: {}", resultMap.get("resultName"));
        } catch (Exception e) {
            logger.error("Test failed", e);
            fail("Test failed: " + e.getMessage());
        }
    }
}
