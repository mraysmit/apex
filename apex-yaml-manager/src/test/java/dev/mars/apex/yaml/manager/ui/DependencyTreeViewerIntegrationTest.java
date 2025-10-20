package dev.mars.apex.yaml.manager.ui;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Dependency Tree Viewer UI endpoint.
 *
 * Tests the HTML template rendering and page structure without requiring
 * a browser or Selenium WebDriver.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@WebMvcTest(YamlManagerUIController.class)
public class DependencyTreeViewerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testTreeViewerEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(status().isOk())
                .andExpect(view().name("dependency-tree-viewer"));
    }

    @Test
    public void testTreeViewerPageContainsTitle() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(model().attributeExists("title"));
    }

    @Test
    public void testTreeViewerPageHasVersion() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(model().attributeExists("version"));
    }

    @Test
    public void testTreeViewerPageHasApiBaseUrl() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(model().attributeExists("apiBaseUrl"));
    }

    @Test
    public void testTreeViewerPageTitleAttribute() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(model().attribute("title", "Dependency Tree Viewer - APEX YAML Manager"));
    }

    @Test
    public void testTreeViewerPageVersionAttribute() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(model().attribute("version", "1.0.0"));
    }

    @Test
    public void testTreeViewerPageApiBaseUrlAttribute() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(model().attribute("apiBaseUrl", "/yaml-manager/api"));
    }

    @Test
    public void testTreeViewerReturnsCorrectViewName() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(view().name("dependency-tree-viewer"));
    }

    @Test
    public void testTreeViewerEndpointExists() throws Exception {
        mockMvc.perform(get("/ui/tree-viewer"))
                .andExpect(status().isOk());
    }
}

