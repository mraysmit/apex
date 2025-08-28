package dev.mars.apex.playground;

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


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic integration test for APEX Playground application.
 * 
 * Verifies that the Spring Boot application context loads successfully
 * and all components are properly configured.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.main.web-application-type=servlet",
    "server.port=0"  // Use random port for testing
})
class PlaygroundApplicationTest {

    /**
     * Test that the Spring Boot application context loads successfully.
     */
    @Test
    void contextLoads() {
        // This test will pass if the application context loads without errors
        // It verifies that all beans are properly configured and dependencies are resolved
    }
}
