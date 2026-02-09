package dev.mars.apex.yaml.manager.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DependencyTreeApiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String absRoot;

    @BeforeEach
    void setUp() {
        Path p = Paths.get("src/test/resources/apex-yaml-samples/scenario-registry.yaml");
        absRoot = p.toAbsolutePath().toString();
    }

    @Test
    void treeEndpointReturnsMultipleChildrenAndDepth() throws Exception {
        // 1) Analyze (populates currentGraph)
        String analyzeUrl = UriComponentsBuilder.fromPath("/api/dependencies/analyze")
                .queryParam("filePath", absRoot)
                .toUriString();

        ResponseEntity<Map> analyzeResponse = restTemplate.postForEntity(analyzeUrl, null, Map.class);

        assertEquals(HttpStatus.OK, analyzeResponse.getStatusCode());
        assertNotNull(analyzeResponse.getBody());
        assertEquals("success", analyzeResponse.getBody().get("status"));

        Object totalFilesObj = analyzeResponse.getBody().get("totalFiles");
        int totalFiles = totalFilesObj instanceof Number ? ((Number) totalFilesObj).intValue() : Integer.parseInt(totalFilesObj.toString());
        int maxDepth = ((Number) analyzeResponse.getBody().get("maxDepth")).intValue();

        // Verify we have at least the root file
        assertTrue(totalFiles >= 1, "Expected totalFiles >= 1 but was " + totalFiles);
        assertTrue(maxDepth >= 0, "Expected maxDepth >= 0 but was " + maxDepth);

        // 2) Tree (should use graph root key and produce children)
        String treeUrl = UriComponentsBuilder.fromPath("/api/dependencies/tree")
                .queryParam("rootFile", absRoot)
                .toUriString();

        ResponseEntity<Map> treeResponse = restTemplate.getForEntity(treeUrl, Map.class);

        assertEquals(HttpStatus.OK, treeResponse.getStatusCode());
        assertNotNull(treeResponse.getBody());
        assertEquals("success", treeResponse.getBody().get("status"));
        assertNotNull(treeResponse.getBody().get("tree"));
        Map<String, Object> tree = (Map<String, Object>) treeResponse.getBody().get("tree");
        assertNotNull(tree.get("children"));
        assertFalse(((java.util.List<?>) tree.get("children")).isEmpty());

        // 3) Details endpoint for one child should resolve using tree key (path/name)
        ResponseEntity<Map> detailsResponse = restTemplate.getForEntity(
                "/api/dependencies/06-trade-processing-scenario.yaml/details",
                Map.class);

        assertEquals(HttpStatus.OK, detailsResponse.getStatusCode());
        assertNotNull(detailsResponse.getBody());
        assertEquals("success", detailsResponse.getBody().get("status"));
        Map<String, Object> data = (Map<String, Object>) detailsResponse.getBody().get("data");
        assertNotNull(data.get("dependencies"));
    }
    @Test
    void coreAnalyzerSeesDependencies() throws Exception {
        String base = Paths.get("src/test/resources/apex-yaml-samples").toAbsolutePath().toString();
        dev.mars.apex.core.util.YamlDependencyAnalyzer analyzer = new dev.mars.apex.core.util.YamlDependencyAnalyzer(base);
        dev.mars.apex.core.util.YamlDependencyGraph graph = analyzer.analyzeYamlDependencies("scenario-registry.yaml");
        System.out.println("CORE ANALYZER totalFiles=" + graph.getTotalFiles() + ", maxDepth=" + graph.getMaxDepth());
        System.out.println("CORE ANALYZER files=" + graph.getAllReferencedFiles());
        org.junit.jupiter.api.Assertions.assertTrue(graph.getTotalFiles() >= 2, "Expected at least root + 1 dependency");
    }
    @Test
    void loaderSeesRuleConfigurationsKey() throws Exception {
        String abs = Paths.get("src/test/resources/apex-yaml-samples/scenario-registry.yaml").toAbsolutePath().toString();
        dev.mars.apex.core.config.loader.YamlConfigurationLoader loader = new dev.mars.apex.core.config.loader.YamlConfigurationLoader();
        java.util.Map<String, Object> map = loader.loadAsMap(abs);
        System.out.println("MAP KEYS=" + map.keySet());
        org.junit.jupiter.api.Assertions.assertTrue(map.containsKey("rule-configurations"), "YAML should contain 'rule-configurations' key");
    }
}
