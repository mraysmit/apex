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

package dev.mars.apex.demo.categories;

import dev.mars.apex.demo.DemoTestBase;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.config.RulesEngine;
import dev.mars.apex.core.engine.model.RuleResult;
import dev.mars.apex.core.engine.model.Category;
import dev.mars.apex.core.engine.model.Rule;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic demonstration of APEX Rule Categories functionality.
 * Tests category definition, rule assignment, and basic metadata inheritance.
 */
public class BasicCategoryTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(BasicCategoryTest.class);

    @Test
    public void testBasicCategoryDefinition() {
        logger.info("=== Testing Basic Category Definition ===");

        // Load YAML configuration with categories
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/BasicCategoryTest.yaml");

        // Verify categories are loaded
        assertNotNull(config.getCategories(), "Categories should be defined");
        assertEquals(2, config.getCategories().size(), "Should have 2 categories");

        // Verify rules are loaded
        assertNotNull(config.getRules(), "Rules should be defined");
        assertEquals(3, config.getRules().size(), "Should have 3 rules");

        logger.info("** Basic category definition test passed");
    }

    @Test
    public void testCategoryRuleAssignment() {
        logger.info("=== Testing Category Rule Assignment ===");

        // Load configuration and create rules engine
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/BasicCategoryTest.yaml");
        RulesEngine engine = createRulesEngine();

        // Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("age", 25);
        testData.put("income", 50000);
        testData.put("creditScore", 750);

        // Execute rules
        RuleResult result = testEvaluation(config, testData);

        // Verify execution was successful
        assertTrue(result.isSuccess(), "Rule execution should be successful");
        logger.info("Rule execution result: {}", result.getMessage());

        logger.info("** Category rule assignment test passed");
    }

    @Test
    public void testCategoryMetadataInheritance() {
        logger.info("=== Testing Category Metadata Inheritance ===");

        // Load configuration
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/BasicCategoryTest.yaml");

        // Verify category metadata is defined
        config.getCategories().forEach(category -> {
            assertNotNull(category.getName(), "Category name should be defined");
            assertNotNull(category.getBusinessDomain(), "Business domain should be defined");
            assertNotNull(category.getBusinessOwner(), "Business owner should be defined");
            assertNotNull(category.getCreatedBy(), "Created by should be defined");
            
            logger.info("Category '{}' has business-domain: '{}', business-owner: '{}', created-by: '{}'",
                category.getName(), category.getBusinessDomain(), category.getBusinessOwner(), category.getCreatedBy());
        });

        // Verify rules have category assignments
        config.getRules().forEach(rule -> {
            assertNotNull(rule.getCategory(), "Rule should have category assignment");
            logger.info("Rule '{}' assigned to category: '{}'", rule.getId(), rule.getCategory());
        });

        logger.info("** Category metadata inheritance test passed");
    }

    @Test
    public void testCategoryPriorityOrdering() {
        logger.info("=== Testing Category Priority Ordering ===");

        // Load configuration
        YamlRuleConfiguration config = loadAndValidateYaml("dev/mars/apex/demo/categories/BasicCategoryTest.yaml");

        // Verify categories have different priorities
        var categories = config.getCategories();
        assertTrue(categories.stream().anyMatch(c -> c.getPriority() != null && c.getPriority() == 10),
            "Should have high priority category");
        assertTrue(categories.stream().anyMatch(c -> c.getPriority() != null && c.getPriority() == 50),
            "Should have medium priority category");

        logger.info("** Category priority ordering test passed");
    }
}
