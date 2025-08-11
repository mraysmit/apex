package dev.mars.apex.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
 * Spring Boot application for the Rules Engine REST API.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration.class
})
@OpenAPIDefinition(
    info = @Info(
        title = "APEX REST API",
        version = "1.0.0",
        description = """
            Comprehensive REST API for APEX (Advanced Processing Engine for eXpressions) with YAML Dataset Enrichment.

            ## API Versioning

            This API follows semantic versioning (SemVer) with URL path versioning:
            - Current version: v1 (implicit in /api/ paths)
            - Future versions: /api/v2/, /api/v3/, etc.

            ## Version Support

            - **v1**: Current stable version
            - Backward compatibility maintained for minimum 12 months after new major version release
            - Deprecation warnings provided via HTTP headers

            ## Error Handling

            All errors follow RFC 7807 Problem Details format with correlation IDs for request tracking.

            ## Rate Limiting

            Standard rate limits apply. Contact support for enterprise limits.
            """,
        contact = @Contact(
            name = "APEX Team",
            email = "apexsupport@mars.dev",
            url = "https://github.com/apex/apex"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development Server (v1)"),
        @Server(url = "https://api.apex.dev", description = "Production Server (v1)"),
        @Server(url = "https://api.apex.dev/v2", description = "Production Server (v2 - Coming Soon)")
    }
)
public class ApexRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApexRestApiApplication.class, args);
    }
}
