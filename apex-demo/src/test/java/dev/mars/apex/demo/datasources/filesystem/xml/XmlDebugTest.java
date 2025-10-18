package dev.mars.apex.demo.datasources.filesystem.xml;

import dev.mars.apex.demo.DemoTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XML Debug Test
 * 
 * This test is specifically designed to debug the XML parsing issue.
 * It will help us understand why the XML file is loading 0 records.
 */
@DisplayName("XML Debug Test")
public class XmlDebugTest extends DemoTestBase {

    private static final Logger logger = LoggerFactory.getLogger(XmlDebugTest.class);

    @Test
    @DisplayName("Debug XML file loading with detailed logging")
    void debugXmlFileLoading() {
        logger.info("=".repeat(80));
        logger.info("XML DEBUG TEST - Investigating XML parsing issue");
        logger.info("=".repeat(80));

        try {
            // Test with the working XML configuration from FileSystemLookupDemoTest-xml.yaml
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/lookup/FileSystemLookupDemoTest-xml.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            logger.info("✓ Successfully loaded XML configuration");
            logger.info("Configuration ID: {}", config.getMetadata().getId());
            logger.info("Configuration Name: {}", config.getMetadata().getName());

            // Create test data with product ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("productId", "PROD001");
            testData.put("transactionId", "TXN123");

            logger.info("Input test data: {}", testData);

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("Enrichment result: {}", enrichedData);

            // Check if enrichment worked
            if (enrichedData.containsKey("productName")) {
                logger.info("✓ XML enrichment SUCCESSFUL!");
                logger.info("  Product Name: {}", enrichedData.get("productName"));
                logger.info("  Product Price: {}", enrichedData.get("productPrice"));
                logger.info("  Product Category: {}", enrichedData.get("productCategory"));
                logger.info("  Product Available: {}", enrichedData.get("productAvailable"));
            } else {
                logger.error("✗ XML enrichment FAILED - no product data found");
                logger.error("Available keys in result: {}", enrichedData.keySet());
            }

        } catch (Exception e) {
            logger.error("XML debug test failed: " + e.getMessage(), e);
            fail("XML debug test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Debug with our SimpleXmlDataSourceTest configuration")
    void debugSimpleXmlConfiguration() {
        logger.info("=".repeat(80));
        logger.info("XML DEBUG TEST - Testing SimpleXmlDataSourceTest configuration");
        logger.info("=".repeat(80));

        try {
            // Test with our SimpleXmlDataSourceTest configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/xml/SimpleXmlDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            logger.info("✓ Successfully loaded SimpleXmlDataSourceTest configuration");

            // Create test data with product ID
            Map<String, Object> testData = new HashMap<>();
            testData.put("productId", "PROD001");
            testData.put("employeeId", "E123");

            logger.info("Input test data: {}", testData);

            // Process with APEX
            Object result = enrichmentService.enrichObject(config, testData);
            assertNotNull(result, "Enrichment result should not be null");

            @SuppressWarnings("unchecked")
            Map<String, Object> enrichedData = (Map<String, Object>) result;

            logger.info("Enrichment result: {}", enrichedData);

            // Check if enrichment worked
            if (enrichedData.containsKey("productName")) {
                logger.info("✓ SimpleXmlDataSourceTest configuration WORKS!");
                assertEquals("US Treasury Bond", enrichedData.get("productName"));
                assertEquals(1200.0, enrichedData.get("productPrice"));
                assertEquals("FixedIncome", enrichedData.get("productCategory"));
            } else {
                logger.error("✗ SimpleXmlDataSourceTest configuration FAILED");
                logger.error("Available keys in result: {}", enrichedData.keySet());
            }

        } catch (Exception e) {
            logger.error("SimpleXmlDataSourceTest debug failed: " + e.getMessage(), e);
            fail("SimpleXmlDataSourceTest debug failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test XML regex pattern manually")
    void testXmlRegexPattern() {
        logger.info("=".repeat(80));
        logger.info("XML REGEX PATTERN TEST");
        logger.info("=".repeat(80));

        try {
            // Read the XML file content directly
            String xmlContent = java.nio.file.Files.readString(
                java.nio.file.Paths.get("demo-data/xml/products.xml"),
                java.nio.charset.StandardCharsets.UTF_8
            );

            logger.info("XML file content:");
            logger.info(xmlContent);

            // Test the regex pattern used by XmlDataLoader
            String recordElement = "product";
            java.util.regex.Pattern recordPattern = java.util.regex.Pattern.compile(
                "<" + recordElement + "(?:\\s[^>]*)?>([\\s\\S]*?)</" + recordElement + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );

            logger.info("Using regex pattern: {}", recordPattern.pattern());

            java.util.regex.Matcher recordMatcher = recordPattern.matcher(xmlContent);

            int matchCount = 0;
            while (recordMatcher.find()) {
                matchCount++;
                String recordXml = recordMatcher.group(0);
                logger.info("Match {}: {}", matchCount, recordXml);
            }

            logger.info("Total matches found: {}", matchCount);

            if (matchCount == 0) {
                logger.error("✗ No matches found with the regex pattern!");
                logger.error("This explains why XML parsing is failing.");
            } else {
                logger.info("✓ Regex pattern works correctly, found {} matches", matchCount);
            }

        } catch (Exception e) {
            logger.error("XML regex test failed: " + e.getMessage(), e);
            fail("XML regex test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Debug XML configuration parsing")
    void debugXmlConfigurationParsing() {
        logger.info("=".repeat(80));
        logger.info("XML CONFIGURATION PARSING DEBUG");
        logger.info("=".repeat(80));

        try {
            // Load the YAML configuration
            var config = yamlLoader.loadFromFile("src/test/java/dev/mars/apex/demo/datasources/filesystem/xml/SimpleXmlDataSourceTest.yaml");
            assertNotNull(config, "YAML configuration should not be null");

            // Get the enrichment configuration
            var enrichments = config.getEnrichments();
            assertNotNull(enrichments, "Enrichments should not be null");
            assertFalse(enrichments.isEmpty(), "Enrichments should not be empty");

            var enrichment = enrichments.get(0);
            logger.info("Enrichment ID: {}", enrichment.getId());
            logger.info("Enrichment Type: {}", enrichment.getType());

            // Get the lookup configuration
            var lookupConfig = enrichment.getLookupConfig();
            assertNotNull(lookupConfig, "Lookup config should not be null");

            var lookupDataset = lookupConfig.getLookupDataset();
            assertNotNull(lookupDataset, "Lookup dataset should not be null");

            logger.info("Dataset Type: {}", lookupDataset.getType());
            logger.info("Key Field: {}", lookupDataset.getKeyField());
            logger.info("File Path: {}", lookupDataset.getFilePath());

            // Check if the lookup dataset has additional properties
            logger.info("Lookup dataset class: {}", lookupDataset.getClass().getName());

            // For now, just check that the configuration is being parsed
            // The format-config issue will be addressed separately
            logger.info("✓ Configuration parsing appears to be working correctly.");
            logger.info("The format-config field needs to be added to the LookupDataset class.");

        } catch (Exception e) {
            logger.error("XML configuration parsing debug failed: " + e.getMessage(), e);
            fail("XML configuration parsing debug failed: " + e.getMessage());
        }
    }
}
