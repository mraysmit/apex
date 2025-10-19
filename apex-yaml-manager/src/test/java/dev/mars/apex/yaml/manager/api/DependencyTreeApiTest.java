package dev.mars.apex.yaml.manager.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DependencyTreeApiTest {

    @Autowired
    private MockMvc mockMvc;

    private String absRoot;

    @BeforeEach
    void setUp() {
        Path p = Paths.get("src/test/resources/apex-yaml-samples/scenario-registry.yaml");
        absRoot = p.toAbsolutePath().toString();
    }

    @Test
    void treeEndpointReturnsMultipleChildrenAndDepth() throws Exception {
        // 1) Analyze (populates currentGraph)
        mockMvc.perform(post("/api/dependencies/analyze")
                .param("filePath", absRoot)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.totalFiles", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.maxDepth", greaterThanOrEqualTo(2)));

        // 2) Tree (should use graph root key and produce children)
        mockMvc.perform(get("/api/dependencies/tree")
                .param("rootFile", absRoot))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.tree").exists())
                .andExpect(jsonPath("$.tree.children", not(empty())));

        // 3) Details endpoint for one child should resolve using tree key (path/name)
        // Fetch details for a known child key (relative filename)
        mockMvc.perform(get("/api/dependencies/{filePath}/details", "06-trade-processing-scenario.yaml"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.dependencies", notNullValue()));
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
        dev.mars.apex.core.config.yaml.YamlConfigurationLoader loader = new dev.mars.apex.core.config.yaml.YamlConfigurationLoader();
        java.util.Map<String, Object> map = loader.loadAsMap(abs);
        System.out.println("MAP KEYS=" + map.keySet());
        org.junit.jupiter.api.Assertions.assertTrue(map.containsKey("rule-configurations"), "YAML should contain 'rule-configurations' key");
    }
}
