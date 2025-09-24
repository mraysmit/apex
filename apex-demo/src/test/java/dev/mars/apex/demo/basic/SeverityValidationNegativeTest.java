/*
 * Copyright 2024 APEX Demo Team
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

package dev.mars.apex.demo.basic;

import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.demo.ColoredTestOutputExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test for SeverityValidationNegativeTest.yaml
 * Tests that invalid severity values are properly handled.
 */
@ExtendWith(ColoredTestOutputExtension.class)
class SeverityValidationNegativeTest {

    @Test
    @DisplayName("Test negative severity validation")
    void testNegativeSeverityValidation() {
        // Load YAML configuration with invalid severity values
        YamlConfigurationLoader loader = new YamlConfigurationLoader();

        // This should throw an exception due to invalid severity values
        YamlConfigurationException exception = assertThrows(YamlConfigurationException.class, () -> {
            loader.loadFromFile("src/test/java/dev/mars/apex/demo/basic/SeverityValidationNegativeTest.yaml");
        });

        // Validate the exception message contains severity validation error
        assertTrue(exception.getMessage().contains("invalid severity"));
        assertTrue(exception.getMessage().contains("CRITICAL"));
    }
}
