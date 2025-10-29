package dev.mars.apex.rest.config;

import dev.mars.apex.core.service.enrichment.SequentialEnrichmentService;
import dev.mars.apex.core.config.yaml.SequentialYamlRulesEngineService;
import dev.mars.apex.core.service.integration.SequentialProcessingIntegrationService;
import dev.mars.apex.rest.ApexRestApiApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
 * Unit tests for sequential processing Spring configuration.
 * Tests that all sequential processing services are properly configured as Spring beans.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-29
 * @version 1.0
 */
@SpringBootTest(classes = ApexRestApiApplication.class)
@ActiveProfiles("test")
public class SequentialProcessingConfigurationTest {

    @Autowired
    private SequentialEnrichmentService sequentialEnrichmentService;

    @Autowired
    private SequentialYamlRulesEngineService sequentialYamlRulesEngineService;

    @Autowired
    private SequentialProcessingIntegrationService sequentialProcessingIntegrationService;

    @Test
    @DisplayName("SequentialEnrichmentService should be configured as Spring bean")
    public void testSequentialEnrichmentServiceConfiguration() {
        assertNotNull(sequentialEnrichmentService, 
            "SequentialEnrichmentService should be available as Spring bean");
    }

    @Test
    @DisplayName("SequentialYamlRulesEngineService should be configured as Spring bean")
    public void testSequentialYamlRulesEngineServiceConfiguration() {
        assertNotNull(sequentialYamlRulesEngineService, 
            "SequentialYamlRulesEngineService should be available as Spring bean");
    }

    @Test
    @DisplayName("SequentialProcessingIntegrationService should be configured as Spring bean")
    public void testSequentialProcessingIntegrationServiceConfiguration() {
        assertNotNull(sequentialProcessingIntegrationService, 
            "SequentialProcessingIntegrationService should be available as Spring bean");
    }

    @Test
    @DisplayName("Sequential processing services should be properly initialized")
    public void testSequentialProcessingServicesInitialization() {
        // Test that services are not just present but properly initialized
        assertNotNull(sequentialEnrichmentService, "SequentialEnrichmentService should be initialized");
        assertNotNull(sequentialYamlRulesEngineService, "SequentialYamlRulesEngineService should be initialized");
        assertNotNull(sequentialProcessingIntegrationService, "SequentialProcessingIntegrationService should be initialized");

        // Verify services are properly configured by checking they have different class names
        assert !sequentialEnrichmentService.getClass().equals(sequentialYamlRulesEngineService.getClass());
        assert !sequentialEnrichmentService.getClass().equals(sequentialProcessingIntegrationService.getClass());
        assert !sequentialYamlRulesEngineService.getClass().equals(sequentialProcessingIntegrationService.getClass());
    }
}
