package dev.mars.apex.rest.config;

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


import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;

import jakarta.annotation.PostConstruct;

/**
 * Test configuration to resolve Spring Boot 3.4.8 compatibility issues.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration.class
})
public class IntegrationTestConfiguration {

    /**
     * Set system property to enable test-aware logging in core module.
     */
    @PostConstruct
    public void setupTestEnvironment() {
        System.setProperty("test.environment", "true");
    }
}
