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


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application for the APEX Playground.
 * 
 * Interactive web-based playground for APEX Rules Engine with 4-panel JSFiddle-style interface
 * for processing source data files with YAML rules configurations.
 *
 * This class is part of the APEX - A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-23
 * @version 1.0
 */
@SpringBootApplication(exclude = {
    org.springframework.boot.actuate.autoconfigure.metrics.task.TaskExecutorMetricsAutoConfiguration.class
})
@OpenAPIDefinition(
    info = @Info(
        title = "APEX Playground API",
        version = "1.0.0",
        description = """
            Interactive web-based playground for APEX Rules Engine with 4-panel JSFiddle-style interface.
            
            ## Features
            
            - **4-Panel Interface**: Source Data, YAML Rules, Validation Results, Enrichment Results
            - **Real-time YAML Validation**: Syntax checking as you type
            - **Multiple Data Formats**: Support for JSON, XML, CSV input data
            - **Interactive Processing**: Immediate feedback on rule execution
            - **Performance Metrics**: Execution timing and performance data
            - **Save/Load Configurations**: Persist and share playground configurations
            
            ## API Endpoints
            
            The playground provides REST API endpoints for programmatic access to all functionality.
            
            ## Getting Started
            
            1. Open the playground interface at /playground
            2. Input your source data in the top-left panel
            3. Define your YAML rules in the top-right panel
            4. Click "Process" to see validation and enrichment results
            5. Iterate and refine your rules in real-time
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
        @Server(url = "http://localhost:8081", description = "Development Server"),
        @Server(url = "https://playground.apex.dev", description = "Production Server")
    }
)
public class PlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlaygroundApplication.class, args);
    }
}
