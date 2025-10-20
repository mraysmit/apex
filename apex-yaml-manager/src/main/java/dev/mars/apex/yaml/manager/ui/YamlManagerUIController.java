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

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * UI Controller for APEX YAML Manager web interface.
 *
 * Serves the main UI pages and provides configuration to the frontend.
 * Follows the apex-playground pattern with 4-panel interface.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-10-18
 * @version 1.0
 */
@Controller
@RequestMapping("/ui")
public class YamlManagerUIController {

    /**
     * Serve the main YAML Manager UI page.
     */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("title", "APEX YAML Manager");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("apiBaseUrl", "/yaml-manager/api");
        return "yaml-manager";
    }

    /**
     * Serve the dependency analysis page.
     */
    @GetMapping("/dependencies")
    public String dependencies(Model model) {
        model.addAttribute("title", "Dependency Analysis - APEX YAML Manager");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("apiBaseUrl", "/yaml-manager/api");
        return "dependencies";
    }

    /**
     * Serve the catalog browser page.
     */
    @GetMapping("/catalog")
    public String catalog(Model model) {
        model.addAttribute("title", "Catalog Browser - APEX YAML Manager");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("apiBaseUrl", "/yaml-manager/api");
        return "catalog";
    }

    /**
     * Serve the validation page.
     */
    @GetMapping("/validation")
    public String validation(Model model) {
        model.addAttribute("title", "Validation - APEX YAML Manager");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("apiBaseUrl", "/yaml-manager/api");
        return "validation";
    }

    /**
     * Serve the health check page.
     */
    @GetMapping("/health")
    public String health(Model model) {
        model.addAttribute("title", "Health Checks - APEX YAML Manager");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("apiBaseUrl", "/yaml-manager/api");
        return "health";
    }

    /**
     * Serve the dependency tree viewer page.
     */
    @GetMapping("/tree-viewer")
    public String treeViewer(Model model) {
        model.addAttribute("title", "Dependency Tree Viewer - APEX YAML Manager");
        model.addAttribute("version", "1.0.0");
        model.addAttribute("apiBaseUrl", "/yaml-manager/api");
        return "dependency-tree-viewer";
    }
}

