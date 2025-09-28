package dev.mars.apex.demo.etl;

import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import dev.mars.apex.core.engine.pipeline.DataPipelineEngine;
import dev.mars.apex.core.engine.pipeline.DataPipelineException;
import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PipelineEtlExecutionTestExtractInvalidSource.yaml
 * Tests invalid data source error handling
 */
@DisplayName("Invalid Source Extract Pipeline Test")
class PipelineEtlExecutionTestExtractInvalidSource extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineEtlExecutionTestExtractInvalidSource.class);
    
    private DataPipelineEngine pipelineEngine;
    private YamlConfigurationLoader yamlLoader;

    @BeforeEach
    public void setUp() {
        super.setUp();
        logger.info("Setting up Invalid Source Extract Pipeline Test...");
        pipelineEngine = new DataPipelineEngine();
        yamlLoader = new YamlConfigurationLoader();
        logger.info("✓ Invalid Source Extract Pipeline Test setup completed");
    }

    @Test
    @DisplayName("Should fail gracefully with invalid data source")
    void shouldFailGracefullyWithInvalidDataSource() throws Exception {
        logger.info("=== Testing Invalid Source Extract Pipeline ===");

        // Load the YAML configuration
        YamlRuleConfiguration config = yamlLoader.loadFromFile(
            "src/test/java/dev/mars/apex/demo/etl/PipelineEtlExecutionTestExtractInvalidSource.yaml");
        
        // Initialize pipeline
        pipelineEngine.initialize(config);
        
        // Execute pipeline and expect it to fail
        DataPipelineException exception = assertThrows(DataPipelineException.class, () -> {
            pipelineEngine.executePipeline("invalid-source-extract-pipeline");
        }, "Pipeline should throw DataPipelineException for invalid data source");

        // Validate the exception
        assertNotNull(exception, "Exception should not be null");
        assertTrue(exception.getMessage().contains("Required step failed"), 
                  "Exception should indicate step failure");

        logger.info("✓ Invalid source extract pipeline test completed successfully");
        logger.info("  - Pipeline correctly failed with exception: {}", exception.getMessage());
    }
}
