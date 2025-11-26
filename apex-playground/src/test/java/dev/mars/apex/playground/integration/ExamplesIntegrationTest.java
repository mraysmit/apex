package dev.mars.apex.playground.integration;

import dev.mars.apex.playground.model.PlaygroundRequest;
import dev.mars.apex.playground.model.PlaygroundResponse;
import dev.mars.apex.playground.service.DataProcessingService;
import dev.mars.apex.playground.service.PlaygroundService;
import dev.mars.apex.playground.service.YamlValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that verifies all examples in the apex-playground/examples directory
 * can be executed successfully by the PlaygroundService.
 */
public class ExamplesIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ExamplesIntegrationTest.class);
    private PlaygroundService playgroundService;

    @BeforeEach
    void setUp() {
        DataProcessingService dataProcessingService = new DataProcessingService();
        YamlValidationService yamlValidationService = new YamlValidationService();
        playgroundService = new PlaygroundService(dataProcessingService, yamlValidationService);
    }

    @Test
    @DisplayName("Run all playground examples")
    void runAllExamples() throws IOException {
        // Locate the examples directory
        // Assuming the test runs from the module root (apex-playground)
        Path examplesDir = Paths.get("examples");
        if (!Files.exists(examplesDir)) {
            // Try going up one level if running from IDE might be different
            examplesDir = Paths.get("apex-playground/examples");
        }
        
        assertTrue(Files.exists(examplesDir), "Examples directory not found at " + examplesDir.toAbsolutePath());
        logger.info("Running examples from: {}", examplesDir.toAbsolutePath());

        // Find all YAML files
        List<Path> yamlFiles;
        try (Stream<Path> walk = Files.walk(examplesDir)) {
            yamlFiles = walk
                    .filter(p -> !Files.isDirectory(p))
                    .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .collect(Collectors.toList());
        }

        assertFalse(yamlFiles.isEmpty(), "No YAML example files found");

        int successCount = 0;
        int failureCount = 0;

        for (Path yamlFile : yamlFiles) {
            logger.info("Processing example: {}", yamlFile.getFileName());
            
            // Find corresponding JSON data file
            Optional<Path> jsonFile = findJsonDataFile(yamlFile);
            
            if (jsonFile.isPresent()) {
                try {
                    runExample(yamlFile, jsonFile.get());
                    successCount++;
                } catch (AssertionError | Exception e) {
                    logger.error("Example failed: {}", yamlFile.getFileName(), e);
                    failureCount++;
                }
            } else {
                logger.warn("No corresponding JSON data file found for {}, skipping execution.", yamlFile.getFileName());
            }
        }

        logger.info("Examples execution completed. Success: {}, Failures: {}", successCount, failureCount);
        assertEquals(0, failureCount, "Some examples failed to execute. See logs for details.");
    }

    private Optional<Path> findJsonDataFile(Path yamlFile) {
        String yamlFilename = yamlFile.getFileName().toString();
        String baseName = yamlFilename.substring(0, yamlFilename.lastIndexOf('.'));
        Path dir = yamlFile.getParent();

        // Strategy 1: Exact match (e.g., example.yaml -> example.json)
        Path exactMatch = dir.resolve(baseName + ".json");
        if (Files.exists(exactMatch)) {
            return Optional.of(exactMatch);
        }

        // Strategy 2: Suffix match (e.g., example.yaml -> example-data.json)
        Path suffixMatch = dir.resolve(baseName + "-data.json");
        if (Files.exists(suffixMatch)) {
            return Optional.of(suffixMatch);
        }

        // Strategy 3: If there is only one JSON file in the directory, use it
        try (Stream<Path> list = Files.list(dir)) {
            List<Path> jsonFiles = list
                    .filter(p -> p.toString().endsWith(".json"))
                    .collect(Collectors.toList());
            
            if (jsonFiles.size() == 1) {
                return Optional.of(jsonFiles.get(0));
            }
        } catch (IOException e) {
            logger.warn("Error listing files in directory: {}", dir, e);
        }

        return Optional.empty();
    }

    private void runExample(Path yamlFile, Path jsonFile) throws IOException {
        String yamlContent = Files.readString(yamlFile);
        String jsonContent = Files.readString(jsonFile);

        PlaygroundRequest request = new PlaygroundRequest();
        request.setYamlRules(yamlContent);
        request.setSourceData(jsonContent);
        request.setDataFormat("JSON");

        PlaygroundResponse response = playgroundService.processData(request);

        if (!response.isSuccess()) {
            fail("Example failed: " + yamlFile.getFileName() + ". Errors: " + response.getErrors());
        }
        
        assertTrue(response.isSuccess(), "Response should be successful");
        logger.info("  -> Success");
    }
}
