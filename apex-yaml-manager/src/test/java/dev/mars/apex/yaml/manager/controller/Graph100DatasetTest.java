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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Graph100DatasetTest {

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
        assertNotNull(resource, "graph-100 seed registry must exist on classpath");
        // Resolve folder from registry file path
        Path registryPath = Paths.get(resource.toURI());
        datasetFolder = registryPath.getParent();
    }

    @Test
    @DisplayName("graph-100: scan-folder returns files >= seed minimum")
    void scanFolder_seedMinimum() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/scan-folder?folderPath=" + datasetFolder.toAbsolutePath(),
                null,
                Map.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body = response.getBody();
        assertNotNull(body);
        assertEquals("success", body.get("status"));
        Number totalFiles = (Number) body.get("totalFiles");
        assertNotNull(totalFiles);
        assertTrue(totalFiles.intValue() >= 6, "expected at least 6 seed files");
    }

    @Test
    @DisplayName("graph-100: tree and validate-tree succeed for registry root")
    void treeAndValidate_seedRegistry() {
        File registry = datasetFolder.resolve("00-scenario-registry.yaml").toFile();
        assertTrue(registry.exists(), "seed registry file must exist");

        ResponseEntity<Map> tree = restTemplate.getForEntity(
                baseUrl + "/tree?rootFile=" + registry.getAbsolutePath(),
                Map.class
        );
        assertEquals(HttpStatus.OK, tree.getStatusCode());
        assertNotNull(tree.getBody());
        assertEquals("success", tree.getBody().get("status"));
        assertNotNull(tree.getBody().get("tree"));

        ResponseEntity<Map> validation = restTemplate.postForEntity(
                baseUrl + "/validate-tree?rootFile=" + registry.getAbsolutePath(),
                null,
                Map.class
        );
        assertEquals(HttpStatus.OK, validation.getStatusCode());
        assertNotNull(validation.getBody());
        assertEquals("success", validation.getBody().get("status"));
        Map validationObj = (Map) validation.getBody().get("validation");
        assertNotNull(validationObj);
        // Since dataset contains an intentional missing reference, validation should flag it
        assertEquals("error", validationObj.get("status"));
        Map issues = (Map) validationObj.get("issues");
        assertNotNull(issues);
        java.util.List critical = (java.util.List) issues.get("CRITICAL");
        assertNotNull(critical);
        assertTrue(!critical.isEmpty(), "expected at least one CRITICAL issue for missing file reference");
        boolean mentionsMissingFile = critical.stream().anyMatch(o -> String.valueOf(o).contains("99-missing.yaml"));
        assertTrue(mentionsMissingFile, "CRITICAL issues should mention 99-missing.yaml");
    }
}

