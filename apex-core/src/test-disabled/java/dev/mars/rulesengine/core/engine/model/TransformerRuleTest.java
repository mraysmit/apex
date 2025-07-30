package dev.mars.apex.core.engine.model;

import dev.mars.apex.core.service.transform.FieldTransformerAction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

/**
 * Test class for TransformerRule.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for TransformerRule.
 * This class tests the functionality of the TransformerRule class.
 */
public class TransformerRuleTest {

    /**
     * Test creating a transformer rule with all properties.
     */
    @Test
    public void testCreateTransformerRuleWithAllProperties() {
        // Create a rule
        Rule rule = new Rule("TestRule", "#value > 10", "Value is greater than 10");
        
        // Create positive actions
        List<FieldTransformerAction<String>> positiveActions = new ArrayList<>();
        
        // Create negative actions
        List<FieldTransformerAction<String>> negativeActions = new ArrayList<>();
        
        // Create additional facts
        Map<String, Object> additionalFacts = new HashMap<>();
        additionalFacts.put("multiplier", 2);
        additionalFacts.put("divider", 2);
        
        // Create a transformer rule with all properties
        TransformerRule<String> transformerRule = new TransformerRule<>(rule, positiveActions, negativeActions, additionalFacts);
        
        // Verify transformer rule properties
        assertEquals(rule, transformerRule.getRule(), "Rule should match");
        assertEquals(positiveActions, transformerRule.getPositiveActions(), "Positive actions should match");
        assertEquals(negativeActions, transformerRule.getNegativeActions(), "Negative actions should match");
        assertEquals(additionalFacts, transformerRule.getAdditionalFacts(), "Additional facts should match");
    }
    
    /**
     * Test creating a transformer rule without additional facts.
     */
    @Test
    public void testCreateTransformerRuleWithoutAdditionalFacts() {
        // Create a rule
        Rule rule = new Rule("TestRule", "#value > 10", "Value is greater than 10");
        
        // Create positive actions
        List<FieldTransformerAction<String>> positiveActions = new ArrayList<>();
        
        // Create negative actions
        List<FieldTransformerAction<String>> negativeActions = new ArrayList<>();
        
        // Create a transformer rule without additional facts
        TransformerRule<String> transformerRule = new TransformerRule<>(
            rule, positiveActions, negativeActions);
        
        // Verify transformer rule properties
        assertEquals(rule, transformerRule.getRule(), "Rule should match");
        assertEquals(positiveActions, transformerRule.getPositiveActions(), "Positive actions should match");
        assertEquals(negativeActions, transformerRule.getNegativeActions(), "Negative actions should match");
        assertNotNull(transformerRule.getAdditionalFacts(), "Additional facts should not be null");
        assertTrue(transformerRule.getAdditionalFacts().isEmpty(), "Additional facts should be empty");
    }
    
    /**
     * Test creating a transformer rule with null actions.
     */
    @Test
    public void testCreateTransformerRuleWithNullActions() {
        // Create a rule
        Rule rule = new Rule("TestRule", "#value > 10", "Value is greater than 10");
        
        // Create a transformer rule with null actions
        TransformerRule<String> transformerRule = new TransformerRule<>(
            rule, null, null, null);
        
        // Verify transformer rule properties
        assertEquals(rule, transformerRule.getRule(), "Rule should match");
        assertNotNull(transformerRule.getPositiveActions(), "Positive actions should not be null");
        assertTrue(transformerRule.getPositiveActions().isEmpty(), "Positive actions should be empty");
        assertNotNull(transformerRule.getNegativeActions(), "Negative actions should not be null");
        assertTrue(transformerRule.getNegativeActions().isEmpty(), "Negative actions should be empty");
        assertNotNull(transformerRule.getAdditionalFacts(), "Additional facts should not be null");
        assertTrue(transformerRule.getAdditionalFacts().isEmpty(), "Additional facts should be empty");
    }
}
