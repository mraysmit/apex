package dev.mars.apex.yaml.manager.service;

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

import dev.mars.apex.yaml.manager.model.YamlContentSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service for analyzing YAML file contents and generating summaries.
 */
@Service
public class YamlContentAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(YamlContentAnalyzer.class);
    private final Yaml yaml = new Yaml();

    /**
     * Analyze a YAML file and generate a content summary.
     */
    public YamlContentSummary analyzYamlContent(String filePath) {
        YamlContentSummary summary = new YamlContentSummary(filePath);

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("YAML file not found: {}", filePath);
                return summary;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                Map<String, Object> data = yaml.load(fis);
                if (data == null) {
                    return summary;
                }

                // Extract metadata
                extractMetadata(summary, data);

                // Analyze content
                analyzeContent(summary, data);

                // Determine file type
                determineFileType(summary, data);
            }
        } catch (IOException e) {
            logger.error("Error reading YAML file: {}", filePath, e);
        }

        return summary;
    }

    /**
     * Extract metadata from YAML.
     */
    private void extractMetadata(YamlContentSummary summary, Map<String, Object> data) {
        if (data.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            if (metadata != null) {
                summary.setId((String) metadata.get("id"));
                summary.setName((String) metadata.get("name"));
                summary.setDescription((String) metadata.get("description"));
                summary.setVersion((String) metadata.get("version"));
            }
        }
    }

    /**
     * Analyze content and count items.
     */
    private void analyzeContent(YamlContentSummary summary, Map<String, Object> data) {
        // Count rule groups
        if (data.containsKey("rule-groups")) {
            List<Map<String, Object>> ruleGroups = (List<Map<String, Object>>) data.get("rule-groups");
            if (ruleGroups != null) {
                summary.setRuleGroupCount(ruleGroups.size());
                
                // Count rules within groups
                int totalRules = 0;
                for (Map<String, Object> group : ruleGroups) {
                    if (group.containsKey("rules")) {
                        List<String> rules = (List<String>) group.get("rules");
                        if (rules != null) {
                            totalRules += rules.size();
                        }
                    }
                }
                summary.setRuleCount(totalRules);
            }
        }

        // Count enrichments
        if (data.containsKey("enrichments")) {
            List<Map<String, Object>> enrichments = (List<Map<String, Object>>) data.get("enrichments");
            if (enrichments != null) {
                summary.setEnrichmentCount(enrichments.size());
            }
        }

        // Count config files
        if (data.containsKey("config-files")) {
            List<String> configFiles = (List<String>) data.get("config-files");
            if (configFiles != null) {
                summary.setConfigFileCount(configFiles.size());
            }
        }

        // Count references
        int refCount = 0;
        if (data.containsKey("enrichment-refs")) {
            List<String> refs = (List<String>) data.get("enrichment-refs");
            if (refs != null) {
                refCount += refs.size();
            }
        }
        if (data.containsKey("rule-refs")) {
            List<String> refs = (List<String>) data.get("rule-refs");
            if (refs != null) {
                refCount += refs.size();
            }
        }
        summary.setReferenceCount(refCount);
    }

    /**
     * Determine the type of YAML file based on content.
     */
    private void determineFileType(YamlContentSummary summary, Map<String, Object> data) {
        if (data.containsKey("rule-groups")) {
            summary.setFileType("rules");
        } else if (data.containsKey("enrichments")) {
            summary.setFileType("enrichments");
        } else if (data.containsKey("scenarios")) {
            summary.setFileType("scenario");
        } else if (data.containsKey("config")) {
            summary.setFileType("config");
        } else {
            summary.setFileType("unknown");
        }
    }
}

