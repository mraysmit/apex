package dev.mars.apex.yaml.manager.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Graph100ValidationIssuesTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    private Path datasetFolder;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port + "/yaml-manager/api/dependencies";
        URL resource = this.getClass().getClassLoader()
                .getResource("apex-yaml-samples/graph-100/00-scenario-registry.yaml");
        assertNotNull(resource, "graph-100 registry must exist on classpath");
        Path registryPath = Paths.get(resource.toURI());
        datasetFolder = registryPath.getParent();
    }

    @Test
    @DisplayName("graph-100: validate-tree returns OK with stats and depth for expanded dataset")
    void validateTree_returnsOkWithStats() {
        File registry = datasetFolder.resolve("00-scenario-registry.yaml").toFile();
        assertTrue(registry.exists(), "registry file must exist");

        ResponseEntity<Map> validation = restTemplate.postForEntity(
                baseUrl + "/validate-tree?rootFile=" + registry.getAbsolutePath(),
                null,
                Map.class
        );
        assertEquals(HttpStatus.OK, validation.getStatusCode());
        assertEquals("success", validation.getBody().get("status"));

        Map validationMap = (Map) validation.getBody().get("validation");
        assertNotNull(validationMap);
        Map issues = (Map) validationMap.get("issues");
        assertNotNull(issues);
        Map stats = (Map) validationMap.get("stats");
        assertNotNull(stats);
        Number nodeCount = (Number) stats.get("nodeCount");
        Number maxDepthObserved = (Number) stats.get("maxDepthObserved");
        assertNotNull(nodeCount);
        assertNotNull(maxDepthObserved);
        assertTrue(nodeCount.intValue() >= 25, "expected nodeCount >= 25 after expansion");
        assertTrue(maxDepthObserved.intValue() >= 7, "expected maxDepthObserved >= 7 due to chain files");
    }

    @Test
    @DisplayName("graph-100: validate-tree reports invalid YAML as HIGH issue")
    void validateTree_reportsInvalidYamlAsHigh() {
        File registry = datasetFolder.resolve("00-scenario-registry.yaml").toFile();
        assertTrue(registry.exists(), "registry file must exist");

        ResponseEntity<Map> validation = restTemplate.postForEntity(
                baseUrl + "/validate-tree?rootFile=" + registry.getAbsolutePath(),
                null,
                Map.class
        );
        assertEquals(HttpStatus.OK, validation.getStatusCode());
        assertEquals("success", validation.getBody().get("status"));

        Map validationMap = (Map) validation.getBody().get("validation");
        assertNotNull(validationMap);
        Map issues = (Map) validationMap.get("issues");
        assertNotNull(issues);
        List high = (List) issues.get("HIGH");
        assertNotNull(high);
        assertTrue(!high.isEmpty(), "expected at least one HIGH issue");
        boolean mentionsInvalid = high.stream().anyMatch(o -> String.valueOf(o).contains("98-invalid.yaml"));
        assertTrue(mentionsInvalid, "HIGH issues should mention 98-invalid.yaml");
    }


    @Test
    @DisplayName("graph-100: invalid YAML message starts with expected prefix")
    void validateTree_invalidYamlMessageFormat() {
        File registry = datasetFolder.resolve("00-scenario-registry.yaml").toFile();
        assertTrue(registry.exists(), "registry file must exist");

        ResponseEntity<Map> validation = restTemplate.postForEntity(
                baseUrl + "/validate-tree?rootFile=" + registry.getAbsolutePath(),
                null,
                Map.class
        );
        assertEquals(HttpStatus.OK, validation.getStatusCode());
        assertEquals("success", validation.getBody().get("status"));

        Map validationMap = (Map) validation.getBody().get("validation");
        assertNotNull(validationMap);
        Map issues = (Map) validationMap.get("issues");
        assertNotNull(issues);
        List high = (List) issues.get("HIGH");
        assertNotNull(high);
        boolean hasFormatted = high.stream()
                .anyMatch(o -> {
                    String msg = String.valueOf(o);
                    return msg.startsWith("Invalid YAML file: ") && msg.contains("98-invalid.yaml");
                });
        assertTrue(hasFormatted, "Expected a HIGH issue starting with 'Invalid YAML file: ' and mentioning 98-invalid.yaml");
    }

}

