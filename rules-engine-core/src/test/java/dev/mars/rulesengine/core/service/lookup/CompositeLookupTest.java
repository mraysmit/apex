package dev.mars.rulesengine.core.service.lookup;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.transform.GenericTransformer;
import dev.mars.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

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
 * Test class for CompositeLookup.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Test class for CompositeLookup.
 */
public class CompositeLookupTest {
    private CompositeLookup compositeLookup;
    private MockValidator mockValidator;
    private MockTransformer mockTransformer1;
    private MockTransformer mockTransformer2;

    @BeforeEach
    public void setUp() {
        compositeLookup = new CompositeLookup("TestCompositeLookup");
        mockValidator = new MockValidator("TestValidator");
        mockTransformer1 = new MockTransformer("TestTransformer1");
        mockTransformer2 = new MockTransformer("TestTransformer2");
    }

    @Test
    public void testConstructor() {
        assertEquals("TestCompositeLookup", compositeLookup.getName());
    }

    @Test
    public void testGetName() {
        assertEquals("TestCompositeLookup", compositeLookup.getName());
    }

    @Test
    public void testValidateWithNoValidator() {
        // Test validate with no validation set
        assertTrue(compositeLookup.validate("testValue"));
        assertTrue(compositeLookup.validate(null));
        assertTrue(compositeLookup.validate(123));
    }

    @Test
    public void testValidateWithValidator() {
        // Configure the mock validation
        mockValidator.setValidationResult(true);

        // Set the validation
        compositeLookup.withValidator(mockValidator);

        // Test validate with validation set
        assertTrue(compositeLookup.validate("testValue"));

        // Verify the validation was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());

        // Configure the mock validation to return false
        mockValidator.setValidationResult(false);

        // Test validate with validation set
        assertFalse(compositeLookup.validate("testValue"));

        // Verify the validation was called with the correct value
        assertEquals("testValue", mockValidator.getLastValidatedValue());
    }

    @Test
    public void testTransformWithNoTransformer() {
        // Test transform with no transformer set
        assertEquals("testValue", compositeLookup.transform("testValue"));
        assertNull(compositeLookup.transform(null));
        assertEquals(123, compositeLookup.transform(123));
    }

    @Test
    public void testTransformWithTransformer() {
        // Configure the mock transformer
        mockTransformer1.setTransformationResult("transformedValue");

        // Set the transformer
        compositeLookup.withTransformer(mockTransformer1);

        // Test transform with transformer set
        assertEquals("transformedValue", compositeLookup.transform("testValue"));

        // Verify the transformer was called with the correct value
        assertEquals("testValue", mockTransformer1.getLastTransformedValue());

        // Configure the mock transformer to return a different value
        mockTransformer1.setTransformationResult(789);

        // Test transform with transformer set
        assertEquals(789, compositeLookup.transform("testValue"));

        // Verify the transformer was called with the correct value
        assertEquals("testValue", mockTransformer1.getLastTransformedValue());
    }

    @Test
    public void testEnrichDeprecatedMethod() {
        // Configure the mock transformer
        mockTransformer1.setTransformationResult("transformedValue");

        // Set the transformer
        compositeLookup.withTransformer(mockTransformer1);

        // Test enrich (deprecated method) with transformer set
        assertEquals("transformedValue", compositeLookup.enrich("testValue"));

        // Verify the transformer was called with the correct value
        assertEquals("testValue", mockTransformer1.getLastTransformedValue());
    }

    @Test
    public void testFluentInterface() {
        // Test the fluent interface
        CompositeLookup result = compositeLookup
            .withValidator(mockValidator)
            .withTransformer(mockTransformer1);

        // Verify the result is the same instance
        assertSame(compositeLookup, result);

        // Verify the components were set
        mockValidator.setValidationResult(true);
        assertTrue(compositeLookup.validate("testValue"));
        assertEquals("testValue", mockValidator.getLastValidatedValue());

        mockTransformer1.setTransformationResult("transformedValue");
        assertEquals("transformedValue", compositeLookup.transform("testValue"));
        assertEquals("testValue", mockTransformer1.getLastTransformedValue());
    }

    @Test
    public void testIntegrationOfValidateAndTransform() {
        // Configure the mock components
        mockValidator.setValidationResult(true);
        mockTransformer1.setTransformationResult("transformedValue");

        // Set the components
        compositeLookup
            .withValidator(mockValidator)
            .withTransformer(mockTransformer1);

        // Test the full pipeline: validate -> transform

        // For a valid value
        assertTrue(compositeLookup.validate("testValue"));
        assertEquals("testValue", mockValidator.getLastValidatedValue());

        Object transformed = compositeLookup.transform("testValue");
        assertEquals("transformedValue", transformed);
        assertEquals("testValue", mockTransformer1.getLastTransformedValue());
    }

    /**
     * Mock implementation of Validator for testing.
     */
    private static class MockValidator implements Validator<Object> {
        private final String name;
        private boolean validationResult;
        private Object lastValidatedValue;
        private RuleResult customRuleResult;

        public MockValidator(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean validate(Object value) {
            lastValidatedValue = value;
            return validationResult;
        }

        @Override
        public RuleResult validateWithResult(Object value) {
            lastValidatedValue = value;

            // If a custom RuleResult has been set, return it
            if (customRuleResult != null) {
                return customRuleResult;
            }

            // Otherwise, create a RuleResult based on the validation result
            if (validationResult) {
                return RuleResult.match(getName(), "Validation successful for " + getName());
            } else {
                return RuleResult.noMatch();
            }
        }

        @Override
        public Class<Object> getType() {
            return Object.class;
        }

        public void setValidationResult(boolean validationResult) {
            this.validationResult = validationResult;
        }

        public void setCustomRuleResult(RuleResult customRuleResult) {
            this.customRuleResult = customRuleResult;
        }

        public Object getLastValidatedValue() {
            return lastValidatedValue;
        }
    }

    /**
     * Mock implementation of GenericTransformer for testing.
     */
    private static class MockTransformer extends GenericTransformer<Object> {
        private Object transformationResult;
        private Object lastTransformedValue;

        public MockTransformer(String name) {
            super(name, Object.class, new RulesEngine(new RulesEngineConfiguration()), new ArrayList<>());
        }

        @Override
        public Object transform(Object value) {
            lastTransformedValue = value;
            return transformationResult;
        }

        public void setTransformationResult(Object transformationResult) {
            this.transformationResult = transformationResult;
        }

        public Object getLastTransformedValue() {
            return lastTransformedValue;
        }
    }
}
