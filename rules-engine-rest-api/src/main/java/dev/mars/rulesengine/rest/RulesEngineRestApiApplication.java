package dev.mars.rulesengine.rest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for the Rules Engine REST API.
 * 
 * This application provides a comprehensive REST API for the SpEL Rules Engine,
 * including rule evaluation, configuration management, performance monitoring,
 * and system health checks.
 * 
 * Features:
 * - Rule evaluation and validation endpoints
 * - YAML configuration management
 * - Performance monitoring and metrics
 * - OpenAPI/Swagger documentation
 * - Health checks and system information
 * 
 * Access points:
 * - API: http://localhost:8080/api/
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - Health: http://localhost:8080/actuator/health
 * - Metrics: http://localhost:8080/actuator/metrics
 */
@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "SpEL Rules Engine REST API",
        version = "1.0.0",
        description = "Comprehensive REST API for the SpEL Rules Engine with YAML Dataset Enrichment",
        contact = @Contact(
            name = "Rules Engine Team",
            email = "support@rulesengine.dev",
            url = "https://github.com/rulesengine/spel-rules-engine"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development Server"),
        @Server(url = "https://api.rulesengine.dev", description = "Production Server")
    }
)
public class RulesEngineRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RulesEngineRestApiApplication.class, args);
    }
}
