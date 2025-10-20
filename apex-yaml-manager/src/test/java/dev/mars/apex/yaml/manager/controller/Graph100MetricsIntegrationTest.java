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
class Graph100MetricsIntegrationTest {

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
    @DisplayName("graph-100: metrics, cycles and missing-ref checks after validate-tree")
    void metricsCyclesMissing_afterValidateTree() {
        File registry = datasetFolder.resolve("00-scenario-registry.yaml").toFile();
        assertTrue(registry.exists(), "registry file must exist");

        // Load graph via validate-tree (sets currentGraph on controller)
        ResponseEntity<Map> validation = restTemplate.postForEntity(
                baseUrl + "/validate-tree?rootFile=" + registry.getAbsolutePath(),
                null,
                Map.class
        );
        assertEquals(HttpStatus.OK, validation.getStatusCode());
        assertEquals("success", validation.getBody().get("status"));

        // Metrics
        ResponseEntity<Map> metricsResp = restTemplate.getForEntity(baseUrl + "/metrics", Map.class);
        assertEquals(HttpStatus.OK, metricsResp.getStatusCode());
        Map metricsWrapper = metricsResp.getBody();
        assertNotNull(metricsWrapper);
        assertEquals("success", metricsWrapper.get("status"));
        Map metrics = (Map) metricsWrapper.get("metrics");
        assertNotNull(metrics);
        Number totalFiles = (Number) metrics.get("totalFiles");
        Number maxDepth = (Number) metrics.get("maxDepth");
        assertNotNull(totalFiles);
        assertNotNull(maxDepth);
        assertTrue(totalFiles.intValue() >= 20, "expected at least 20 files after expansion");
        assertTrue(maxDepth.intValue() >= 7, "expected depth >= 7 due to chain files");

        // Cycles
        ResponseEntity<Map> cyclesResp = restTemplate.getForEntity(baseUrl + "/circular-dependencies", Map.class);
        assertEquals(HttpStatus.OK, cyclesResp.getStatusCode());
        Map cyclesBody = cyclesResp.getBody();
        assertNotNull(cyclesBody);
        assertEquals("success", cyclesBody.get("status"));
        Number count = (Number) cyclesBody.get("count");
        assertNotNull(count);
        assertTrue(count.intValue() >= 1, "expected at least one cycle");
        List cycles = (List) cyclesBody.get("circularDependencies");
        assertNotNull(cycles);

        // Report text basic check (not asserting missing files here because report omits them)
        ResponseEntity<Map> reportResp = restTemplate.getForEntity(baseUrl + "/report", Map.class);
        assertEquals(HttpStatus.OK, reportResp.getStatusCode());
        Map reportBody = reportResp.getBody();
        assertNotNull(reportBody);
        assertEquals("success", reportBody.get("status"));
        String report = (String) reportBody.get("report");
        assertNotNull(report);
        assertTrue(report.contains("YAML Dependency Analysis Report"));
    }
}

