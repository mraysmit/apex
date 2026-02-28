package dev.mars.apex.yaml.manager;

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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot application for the APEX YAML Manager.
 *
 * Enterprise YAML configuration management system for APEX with:
 * - Dependency analysis and impact assessment
 * - Configuration catalog and discovery
 * - Health checks and quality scoring
 * - Refactoring and optimization tools
 * - Interactive visualization and REST API
 *
 * This class is part of the APEX - A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration.class
})
@OpenAPIDefinition(
    info = @Info(
        title = "APEX YAML Manager API",
        version = "1.0.0",
        description = """
            Enterprise YAML configuration management system for APEX Rules Engine.
            
            ## Features
            
            - **Dependency Analysis**: Understand configuration relationships and impact
            - **Catalog Discovery**: Search and discover configurations across your system
            - **Health Checks**: Assess configuration quality and identify issues
            - **Refactoring Tools**: Safe configuration reorganization and optimization
            - **Impact Analysis**: Know what breaks before making changes
            - **Visualization**: Interactive dependency graphs and dashboards
            
            ## API Endpoints
            
            The YAML Manager provides REST API endpoints for all management operations.
            
            ## Getting Started
            
            1. Open the YAML Manager interface at /yaml-manager
            2. Upload or scan your YAML configuration folder
            3. Explore dependencies, catalog, and health metrics
            4. Use refactoring tools to optimize your configurations
            5. Export reports and visualizations
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
        @Server(url = "http://localhost:8082", description = "Development Server"),
        @Server(url = "https://yaml-manager.apex.dev", description = "Production Server")
    }
)
public class ManagerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ManagerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ManagerApplication.class, args);
    }

    @Bean
    public CommandLineRunner logApplicationUrls(
            @Value("${server.port:8080}") int port,
            @Value("${server.servlet.context-path:}") String contextPath) {
        return args -> {
            String baseUrl = "http://localhost:" + port + contextPath;
            
            logger.info("");
            logger.info("╔════════════════════════════════════════════════════════════════════════════╗");
            logger.info("║                    APEX YAML Manager - Ready                               ║");
            logger.info("╠════════════════════════════════════════════════════════════════════════════╣");
            logger.info("║  Main UI:           {}                              ", baseUrl);
            logger.info("║  D3 Visualization:  {}/d3-tree-viewer.html          ", baseUrl);
            logger.info("║  API Endpoints:     {}/api                          ", baseUrl);
            logger.info("║  Swagger UI:        {}/swagger-ui.html              ", baseUrl);
            logger.info("║  API Docs:          {}/api-docs                     ", baseUrl);
            logger.info("║  Health Check:      {}/actuator/health              ", baseUrl);
            logger.info("╚════════════════════════════════════════════════════════════════════════════╝");
            logger.info("");
        };
    }
}

