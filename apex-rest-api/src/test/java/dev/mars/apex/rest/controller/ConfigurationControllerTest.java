package dev.mars.apex.rest.controller;

import dev.mars.apex.core.api.RulesService;
import dev.mars.apex.core.config.yaml.YamlConfigurationLoader;
import dev.mars.apex.core.config.yaml.YamlConfigurationException;
import dev.mars.apex.core.config.yaml.YamlRuleConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigurationController.
 * Tests controller logic using Spring Boot Test with MockBean.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Configuration Controller Unit Tests")
public class ConfigurationControllerTest {

    @Autowired
    private ConfigurationController configurationController;

    @MockBean
    private RulesService rulesService;

    @MockBean
    private YamlConfigurationLoader configurationLoader;

    @Nested
    @DisplayName("Configuration Info Tests")
    class ConfigurationInfoTests {

        @Test
        @DisplayName("Should return configuration info")
        void shouldReturnConfigurationInfo() {
            // When - default configuration is loaded at startup
            ResponseEntity<Map<String, Object>> response = configurationController.getConfigurationInfo();

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("hasConfiguration"));
            assertTrue(response.getBody().containsKey("timestamp"));
            // The application loads a default configuration at startup, so hasConfiguration should be true
            assertEquals(true, response.getBody().get("hasConfiguration"));
            assertTrue(response.getBody().containsKey("statistics"));
        }

        // Note: getConfigurationStatistics() method doesn't exist in the actual controller
        // The getConfigurationInfo() method provides similar functionality
    }

    @Nested
    @DisplayName("Configuration Loading Tests")
    class ConfigurationLoadingTests {

        @Test
        @DisplayName("Should load valid YAML configuration")
        void shouldLoadValidYamlConfiguration() throws YamlConfigurationException {
            // Given
            String validYaml = """
                name: Test Configuration
                rules:
                  - name: test-rule
                    condition: "#age >= 18"
                    message: "Must be adult"
                enrichments: []
                """;

            // Create a mock YamlRuleConfiguration with proper metadata
            YamlRuleConfiguration mockConfig = mock(YamlRuleConfiguration.class);
            YamlRuleConfiguration.ConfigurationMetadata mockMetadata = mock(YamlRuleConfiguration.ConfigurationMetadata.class);
            when(mockMetadata.getName()).thenReturn("Test Configuration");
            when(mockMetadata.getVersion()).thenReturn("1.0");
            when(mockConfig.getMetadata()).thenReturn(mockMetadata);
            when(mockConfig.getRules()).thenReturn(java.util.List.of());
            when(mockConfig.getEnrichments()).thenReturn(java.util.List.of());

            // Mock the configuration loader to return our mock config
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class))).thenReturn(mockConfig);

            // When
            ResponseEntity<Map<String, Object>> response = configurationController.loadConfiguration(validYaml);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Configuration loaded successfully", response.getBody().get("message"));
            assertEquals("Test Configuration", response.getBody().get("configurationName"));

            // Verify service calls
            verify(configurationLoader).loadFromStream(any(java.io.InputStream.class));
        }

        @Test
        @DisplayName("Should handle empty YAML configuration")
        void shouldHandleEmptyYamlConfiguration() throws YamlConfigurationException {
            // Given
            String emptyYaml = """
                name: Empty Configuration
                rules: []
                enrichments: []
                """;

            // Create a mock YamlRuleConfiguration for empty config
            YamlRuleConfiguration mockConfig = mock(YamlRuleConfiguration.class);
            YamlRuleConfiguration.ConfigurationMetadata mockMetadata = mock(YamlRuleConfiguration.ConfigurationMetadata.class);
            when(mockMetadata.getName()).thenReturn("Empty Configuration");
            when(mockConfig.getMetadata()).thenReturn(mockMetadata);
            when(mockConfig.getRules()).thenReturn(java.util.List.of());
            when(mockConfig.getEnrichments()).thenReturn(java.util.List.of());

            // Mock the configuration loader
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class))).thenReturn(mockConfig);

            // When
            ResponseEntity<Map<String, Object>> response = configurationController.loadConfiguration(emptyYaml);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Configuration loaded successfully", response.getBody().get("message"));
            assertEquals("Empty Configuration", response.getBody().get("configurationName"));

            // Verify service calls
            verify(configurationLoader).loadFromStream(any(java.io.InputStream.class));
        }

        @Test
        @DisplayName("Should handle null configuration")
        void shouldHandleNullConfiguration() {
            // When & Then - Controller throws NullPointerException for null input
            assertThrows(NullPointerException.class, () -> {
                configurationController.loadConfiguration(null);
            });
        }

        @Test
        @DisplayName("Should handle empty string configuration")
        void shouldHandleEmptyStringConfiguration() throws YamlConfigurationException {
            // Given
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class)))
                .thenThrow(new YamlConfigurationException("Invalid YAML format"));

            // When & Then
            assertThrows(YamlConfigurationException.class, () -> {
                configurationController.loadConfiguration("");
            });
        }
    }

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {

        @Test
        @DisplayName("Should validate valid YAML configuration")
        void shouldValidateValidYamlConfiguration() throws YamlConfigurationException {
            // Given
            String validYaml = """
                name: Validation Test
                rules:
                  - name: age-rule
                    condition: "#age >= 21"
                    message: "Must be 21 or older"
                enrichments: []
                """;

            // Create a mock YamlRuleConfiguration for validation
            YamlRuleConfiguration mockConfig = mock(YamlRuleConfiguration.class);
            YamlRuleConfiguration.ConfigurationMetadata mockMetadata = mock(YamlRuleConfiguration.ConfigurationMetadata.class);
            when(mockMetadata.getName()).thenReturn("Validation Test");
            when(mockConfig.getMetadata()).thenReturn(mockMetadata);
            when(mockConfig.getRules()).thenReturn(java.util.List.of());
            when(mockConfig.getEnrichments()).thenReturn(java.util.List.of());

            // Mock the configuration loader
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class))).thenReturn(mockConfig);

            // When
            ResponseEntity<Map<String, Object>> response = configurationController.validateConfiguration(validYaml);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(true, response.getBody().get("valid"));
            assertEquals("Configuration is valid", response.getBody().get("message"));

            // Verify service calls
            verify(configurationLoader).loadFromStream(any(java.io.InputStream.class));
        }

        @Test
        @DisplayName("Should handle null validation input")
        void shouldHandleNullValidationInput() {
            // When & Then - Controller throws NullPointerException for null input
            assertThrows(NullPointerException.class, () -> {
                configurationController.validateConfiguration(null);
            });
        }
    }

    @Nested
    @DisplayName("File Upload Tests")
    class FileUploadTests {

        @Test
        @DisplayName("Should upload valid YAML file")
        void shouldUploadValidYamlFile() throws Exception {
            // Given
            String yamlContent = """
                name: Upload Test
                rules:
                  - name: upload-rule
                    condition: "#status == 'active'"
                    message: "Status is active"
                enrichments: []
                """;
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-config.yaml",
                "application/x-yaml",
                yamlContent.getBytes()
            );

            // Create a mock YamlRuleConfiguration for upload
            YamlRuleConfiguration mockConfig = mock(YamlRuleConfiguration.class);
            YamlRuleConfiguration.ConfigurationMetadata mockMetadata = mock(YamlRuleConfiguration.ConfigurationMetadata.class);
            when(mockMetadata.getName()).thenReturn("Upload Test Configuration");
            when(mockConfig.getMetadata()).thenReturn(mockMetadata);
            when(mockConfig.getRules()).thenReturn(java.util.List.of());
            when(mockConfig.getEnrichments()).thenReturn(java.util.List.of());

            // Mock the configuration loader
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class))).thenReturn(mockConfig);

            // When
            ResponseEntity<Map<String, Object>> response = configurationController.uploadConfiguration(file);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Configuration file uploaded and loaded successfully", response.getBody().get("message"));
            assertEquals("test-config.yaml", response.getBody().get("fileName"));

            // Verify service calls
            verify(configurationLoader).loadFromStream(any(java.io.InputStream.class));
        }

        @Test
        @DisplayName("Should handle empty file")
        void shouldHandleEmptyFile() throws Exception {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.yaml",
                "application/x-yaml",
                new byte[0]
            );

            // When
            ResponseEntity<Map<String, Object>> response = configurationController.uploadConfiguration(emptyFile);

            // Then - Controller returns HTTP 400 for empty files
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("No file provided", response.getBody().get("error"));
        }

        @Test
        @DisplayName("Should handle null file")
        void shouldHandleNullFile() {
            // When & Then
            assertThrows(Exception.class, () -> configurationController.uploadConfiguration(null));
        }

        @Test
        @DisplayName("Should handle file with wrong extension")
        void shouldHandleFileWithWrongExtension() {
            // Given
            MockMultipartFile txtFile = new MockMultipartFile(
                "file", 
                "config.txt", 
                "text/plain", 
                "some content".getBytes()
            );

            // When & Then
            // The controller should still try to process it as YAML
            // The actual validation happens in the service layer
            assertDoesNotThrow(() -> {
                ResponseEntity<Map<String, Object>> response = configurationController.uploadConfiguration(txtFile);
                assertNotNull(response);
            });
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle invalid YAML syntax")
        void shouldHandleInvalidYamlSyntax() throws YamlConfigurationException {
            // Given
            String invalidYaml = """
                name: Invalid Configuration
                rules:
                  - name: test-rule
                    condition: "#age >= 18
                    message: "Missing quote
                enrichments: []
                """;

            // Mock the configuration loader to throw exception for invalid YAML
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class)))
                .thenThrow(new YamlConfigurationException("Invalid YAML syntax"));

            // When & Then
            assertThrows(YamlConfigurationException.class, () -> {
                configurationController.loadConfiguration(invalidYaml);
            });
        }

        @Test
        @DisplayName("Should handle large configuration file")
        void shouldHandleLargeConfigurationFile() throws Exception {
            // Given
            StringBuilder largeYaml = new StringBuilder();
            largeYaml.append("name: Large Configuration\n");
            largeYaml.append("rules:\n");
            for (int i = 0; i < 1000; i++) {
                largeYaml.append("  - name: rule").append(i).append("\n");
                largeYaml.append("    condition: \"#value > ").append(i).append("\"\n");
                largeYaml.append("    message: \"Rule ").append(i).append("\"\n");
            }
            largeYaml.append("enrichments: []\n");

            MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large-config.yaml", "application/x-yaml", largeYaml.toString().getBytes()
            );

            // Create a mock YamlRuleConfiguration for large config
            YamlRuleConfiguration mockConfig = mock(YamlRuleConfiguration.class);
            YamlRuleConfiguration.ConfigurationMetadata mockMetadata = mock(YamlRuleConfiguration.ConfigurationMetadata.class);
            when(mockMetadata.getName()).thenReturn("Large Configuration");
            when(mockConfig.getMetadata()).thenReturn(mockMetadata);
            when(mockConfig.getRules()).thenReturn(java.util.List.of());
            when(mockConfig.getEnrichments()).thenReturn(java.util.List.of());

            // Mock the configuration loader
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class))).thenReturn(mockConfig);

            // When
            ResponseEntity<Map<String, Object>> response = configurationController.uploadConfiguration(largeFile);

            // Then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Configuration file uploaded and loaded successfully", response.getBody().get("message"));
            assertEquals("large-config.yaml", response.getBody().get("fileName"));
        }

        @Test
        @DisplayName("Should handle non-YAML file upload")
        void shouldHandleNonYamlFileUpload() throws Exception {
            // Given
            String textContent = "This is not a YAML file";
            MockMultipartFile txtFile = new MockMultipartFile(
                "file", "config.txt", "text/plain", textContent.getBytes()
            );

            // When
            ResponseEntity<Map<String, Object>> response = configurationController.uploadConfiguration(txtFile);

            // Then - Controller returns HTTP 400 for non-YAML files
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("File must be a YAML file (.yaml or .yml)", response.getBody().get("error"));
        }

        @Test
        @DisplayName("Should handle configuration with missing required fields")
        void shouldHandleConfigurationWithMissingRequiredFields() throws YamlConfigurationException {
            // Given
            String incompleteYaml = """
                rules:
                  - condition: "#age >= 18"
                    message: "Must be adult"
                enrichments: []
                """;

            // Mock the configuration loader to throw exception for missing fields
            when(configurationLoader.loadFromStream(any(java.io.InputStream.class)))
                .thenThrow(new YamlConfigurationException("Missing required field: name"));

            // When & Then
            assertThrows(YamlConfigurationException.class, () -> {
                configurationController.validateConfiguration(incompleteYaml);
            });
        }
    }
}
