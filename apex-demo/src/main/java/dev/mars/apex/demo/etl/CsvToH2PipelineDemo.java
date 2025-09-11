package dev.mars.apex.demo.etl;

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

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Demonstration of CSV to H2 Database ETL Pipeline using APEX Data Sinks.
 * 
 * This demo shows how to:
 * 1. Load YAML configuration with data sinks
 * 2. Create and initialize data sinks
 * 3. Process CSV data and write to H2 database
 * 4. Handle errors and batch operations
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public class CsvToH2PipelineDemo {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvToH2PipelineDemo.class);
    
    public static void main(String[] args) {
        LOGGER.info("=== APEX ETL Demo: CSV to H2 Database Pipeline ===");
        
        try {
            CsvToH2PipelineDemo demo = new CsvToH2PipelineDemo();
            demo.runDemo();
            
        } catch (Exception e) {
            LOGGER.error("Demo failed with error", e);
            System.exit(1);
        }
        
        LOGGER.info("=== Demo completed successfully ===");
    }
    
    public void runDemo() throws Exception {
        LOGGER.info("=== APEX ETL Demo: CSV to H2 Database Pipeline ===");

        // JDBC drivers are automatically loaded by apex-core JdbcTemplateFactory

        // Step 1: Create required CSV file
        createSampleCsvFile();

        // Step 2: Load YAML configuration
        YamlRuleConfiguration config = loadConfiguration();

        // Step 3: Call APEX core processing engine
        DataPipelineEngine pipelineEngine = new DataPipelineEngine();
        pipelineEngine.initialize(config);

        // Step 4: Execute YAML-defined pipeline (following APEX core principle)
        pipelineEngine.executePipeline("customer-etl-pipeline");

        // Step 5: Cleanup
        pipelineEngine.shutdown();

        LOGGER.info("=== Demo completed successfully ===");
    }
    

    
    /**
     * Create required CSV file for demo.
     */
    private void createSampleCsvFile() throws Exception {
        Path dataDir = Paths.get("./target/demo/etl/data");
        Files.createDirectories(dataDir);

        Path csvFile = dataDir.resolve("customers.csv");
        String csvContent = """
            customer_id,customer_name,email_address,registration_date,status
            1,John Smith,john.smith@email.com,2024-01-15,ACTIVE
            2,Jane Doe,jane.doe@email.com,2024-01-16,ACTIVE
            3,Bob Johnson,bob.johnson@email.com,2024-01-17,PENDING
            4,Alice Brown,alice.brown@email.com,2024-01-18,ACTIVE
            5,Charlie Wilson,charlie.wilson@email.com,2024-01-19,INACTIVE
            6,David Lee,david.lee@email.com,2024-01-20,ACTIVE
            7,Emma Davis,emma.davis@email.com,2024-01-21,ACTIVE
            8,Frank Miller,frank.miller@email.com,2024-01-22,PENDING
            9,Grace Taylor,grace.taylor@email.com,2024-01-23,ACTIVE
            10,Henry Anderson,henry.anderson@email.com,2024-01-24,ACTIVE
            """;

        Files.writeString(csvFile, csvContent);
        LOGGER.info("✓ Created sample CSV file: {}", csvFile);
    }



    /**
     * Load YAML configuration (APEX way).
     */
    private YamlRuleConfiguration loadConfiguration() throws Exception {
        YamlConfigurationLoader loader = new YamlConfigurationLoader();
        return loader.loadFromClasspath("etl/csv-to-h2-pipeline.yaml");
    }



}
