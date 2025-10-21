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
        logger.debug("=== ANALYZING YAML CONTENT ===");
        logger.debug("File path: {}", filePath);

        YamlContentSummary summary = new YamlContentSummary(filePath);

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("YAML file not found: {}", filePath);
                return summary;
            }

            logger.debug("File exists, size: {} bytes", file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                Map<String, Object> data = yaml.load(fis);
                if (data == null) {
                    logger.warn("YAML data is null for file: {}", filePath);
                    return summary;
                }

                logger.debug("YAML loaded successfully, top-level keys: {}", data.keySet());

                // Extract metadata
                extractMetadata(summary, data);
                logger.debug("Metadata extracted: id={}, type={}", summary.getId(), summary.getFileType());

                // Analyze content
                analyzeContent(summary, data);
                logger.debug("Content analyzed: rules={}, enrichments={}, groups={}",
                    summary.getRuleCount(), summary.getEnrichmentCount(), summary.getRuleGroupCount());

                // Determine file type
                determineFileType(summary, data);
                logger.debug("Final file type determined: {}", summary.getFileType());
            }
        } catch (IOException e) {
            logger.error("Error reading YAML file: {}", filePath, e);
        }

        logger.debug("=== CONTENT ANALYSIS COMPLETE ===");
        logger.debug("Summary: {}", summary);
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
        // Count direct rules (rules section)
        if (data.containsKey("rules")) {
            List<Map<String, Object>> rules = (List<Map<String, Object>>) data.get("rules");
            if (rules != null) {
                summary.setRuleCount(rules.size());
            }
        }

        // Count rule groups and rules within them
        if (data.containsKey("rule-groups")) {
            List<Map<String, Object>> ruleGroups = (List<Map<String, Object>>) data.get("rule-groups");
            if (ruleGroups != null) {
                summary.setRuleGroupCount(ruleGroups.size());

                // Count rules within groups (if no direct rules counted)
                if (summary.getRuleCount() == 0) {
                    int totalRules = 0;
                    for (Map<String, Object> group : ruleGroups) {
                        // Support both "rules" and "rule-ids" patterns
                        if (group.containsKey("rules")) {
                            List<String> rules = (List<String>) group.get("rules");
                            if (rules != null) {
                                totalRules += rules.size();
                            }
                        } else if (group.containsKey("rule-ids")) {
                            List<String> ruleIds = (List<String>) group.get("rule-ids");
                            if (ruleIds != null) {
                                totalRules += ruleIds.size();
                            }
                        }
                    }
                    summary.setRuleCount(totalRules);
                }
            }
        }

        // Count enrichments (plural - list)
        if (data.containsKey("enrichments")) {
            List<Map<String, Object>> enrichments = (List<Map<String, Object>>) data.get("enrichments");
            if (enrichments != null) {
                summary.setEnrichmentCount(enrichments.size());
            }
        }

        // Count enrichment (singular - single object with steps)
        if (data.containsKey("enrichment")) {
            Map<String, Object> enrichment = (Map<String, Object>) data.get("enrichment");
            if (enrichment != null) {
                summary.setEnrichmentCount(1);
                // Count steps within enrichment
                if (enrichment.containsKey("steps")) {
                    List<Map<String, Object>> steps = (List<Map<String, Object>>) enrichment.get("steps");
                    if (steps != null) {
                        summary.addContentCount("enrichment-steps", steps.size());
                    }
                }
            }
        }

        // Count config files (rule-configurations, config-files)
        int configCount = 0;
        if (data.containsKey("rule-configurations")) {
            List<String> configFiles = (List<String>) data.get("rule-configurations");
            if (configFiles != null) {
                configCount += configFiles.size();
            }
        }
        if (data.containsKey("config-files")) {
            List<String> configFiles = (List<String>) data.get("config-files");
            if (configFiles != null) {
                configCount += configFiles.size();
            }
        }
        summary.setConfigFileCount(configCount);

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
        // Check metadata type first (most reliable)
        if (data.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) data.get("metadata");
            if (metadata != null && metadata.containsKey("type")) {
                String metadataType = (String) metadata.get("type");
                if (metadataType != null) {
                    summary.setFileType(metadataType);
                    return;
                }
            }
        }

        // Fallback to content-based detection
        // Prioritize rule-groups over direct rules for backward compatibility
        if (data.containsKey("rule-groups")) {
            summary.setFileType("rules");
        } else if (data.containsKey("rules")) {
            summary.setFileType("rule-config");
        } else if (data.containsKey("enrichments")) {
            summary.setFileType("enrichments");
        } else if (data.containsKey("enrichment")) {
            summary.setFileType("enrichment");
        } else if (data.containsKey("scenarios")) {
            summary.setFileType("scenario-registry");
        } else if (data.containsKey("config") || data.containsKey("data-sources")) {
            summary.setFileType("config");
        } else {
            summary.setFileType("unknown");
        }
    }
}

