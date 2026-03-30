package dev.mars.apex.core.config.component;

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

import dev.mars.apex.core.config.exception.ConfigurationException;
import dev.mars.apex.core.test.extension.ColoredTestOutputExtension;
import dev.mars.apex.core.test.extension.TestClassLoggingExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that ComponentLoader enforces the MAX_NESTING_DEPTH=5 contract.
 *
 * <p>Uses a 6-level chain of component YAML files:
 * depth-test-component-level1 → level2 → level3 → level4 → level5 → level6.
 * Resolving level1 must throw ConfigurationException when it reaches level6 at depth 6.
 *
 * <p>Also verifies that the existing 2-level chain (nested-component-level1 → level2)
 * resolves successfully, confirming depths within limits are accepted.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.4.0
 */
@DisplayName("Component Nesting Depth Enforcement Tests")
@ExtendWith({ColoredTestOutputExtension.class, TestClassLoggingExtension.class})
class ComponentNestingDepthEnforcementTest {

    private static final Logger logger = LoggerFactory.getLogger(ComponentNestingDepthEnforcementTest.class);
    private ComponentLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ComponentLoader();
    }

    @Test
    @DisplayName("Should reject component nesting exceeding depth 5")
    void shouldRejectNestingExceedingMaxDepth() throws Exception {
        String componentPath = "scenario/depth-test-component-level1.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);

        ConfigurationException exception = assertThrows(
            ConfigurationException.class,
            () -> loader.resolveAllReferences(component, componentPath),
            "Should throw ConfigurationException for nesting depth 6 (exceeds MAX_NESTING_DEPTH=5)"
        );

        assertTrue(exception.getMessage().contains("nesting depth"),
            "Exception message should mention nesting depth. Actual: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("depth-test-level6"),
            "Exception message should identify the offending component. Actual: " + exception.getMessage());
        logger.info("Correctly rejected depth 6 nesting: {}", exception.getMessage());
    }

    @Test
    @DisplayName("Should accept component nesting at depth 2 (within limits)")
    void shouldAcceptNestingWithinLimits() throws Exception {
        String componentPath = "scenario/nested-component-level1.yaml";
        ComponentConfiguration component = loader.loadComponent(componentPath);

        assertDoesNotThrow(
            () -> loader.resolveAllReferences(component, componentPath),
            "Should not throw for nesting depth 2 (within MAX_NESTING_DEPTH=5)"
        );

        var resolvedRefs = loader.resolveAllReferences(component, componentPath);
        assertFalse(resolvedRefs.isEmpty(), "Should resolve at least one file reference");
        logger.info("Correctly accepted depth 2 nesting with {} resolved files", resolvedRefs.size());
    }
}
