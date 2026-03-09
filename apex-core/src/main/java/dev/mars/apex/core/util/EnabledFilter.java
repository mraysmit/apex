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
package dev.mars.apex.core.util;

import dev.mars.apex.core.config.component.ComponentConfiguration;
import dev.mars.apex.core.config.model.YamlCategory;
import dev.mars.apex.core.config.model.YamlDataSourceRef;
import dev.mars.apex.core.config.model.YamlEnrichment;
import dev.mars.apex.core.config.model.YamlEnrichmentGroup;
import dev.mars.apex.core.config.model.YamlEnrichmentRef;
import dev.mars.apex.core.config.model.YamlRule;
import dev.mars.apex.core.config.model.YamlRuleChain;
import dev.mars.apex.core.config.model.YamlRuleGroup;
import dev.mars.apex.core.config.model.YamlRuleRef;
import dev.mars.apex.core.config.model.YamlTransformation;
import dev.mars.apex.core.service.scenario.ScenarioConfiguration;
import dev.mars.apex.core.service.scenario.ScenarioStage;
import dev.mars.apex.engine.model.Rule;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised utility for determining whether APEX configuration items are enabled.
 *
 * <p>refactoring: replaces 11+ scattered enabled checks across 6 classes
 * with a single consistent location. Two conventions are unified:</p>
 * <ul>
 *   <li><b>Domain objects</b> ({@link Rule}): {@code isEnabled()} returning {@code boolean}, default {@code true}</li>
 *   <li><b>YAML config objects</b> ({@link YamlRule}, {@link YamlEnrichment}, etc.):
 *       {@code getEnabled()} returning nullable {@code Boolean} where {@code null} means enabled</li>
 * </ul>
 *
 * <p>All methods treat {@code null} as <em>enabled</em>.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-02-08
 */
public final class EnabledFilter {

    private EnabledFilter() {
        // utility class
    }

    // =========================================================================
    // Domain model checks (boolean primitive, default true)
    // =========================================================================

    /**
     * @return {@code true} if the rule is enabled (or rule is null)
     */
    public static boolean isEnabled(Rule rule) {
        return rule == null || rule.isEnabled();
    }

    // =========================================================================
    // YAML config checks (nullable Boolean, null = enabled)
    // =========================================================================

    /**
     * @return {@code true} if the YAML rule is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlRule yamlRule) {
        if (yamlRule == null) return true;
        return yamlRule.getEnabled() == null || yamlRule.getEnabled();
    }

    /**
     * @return {@code true} if the YAML enrichment is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlEnrichment enrichment) {
        if (enrichment == null) return true;
        return enrichment.getEnabled() == null || enrichment.getEnabled();
    }

    /**
     * @return {@code true} if the YAML transformation is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlTransformation transformation) {
        if (transformation == null) return true;
        return transformation.getEnabled() == null || transformation.getEnabled();
    }

    /**
     * @return {@code true} if the YAML rule group is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlRuleGroup ruleGroup) {
        if (ruleGroup == null) return true;
        return ruleGroup.getEnabled() == null || ruleGroup.getEnabled();
    }

    /**
     * @return {@code true} if the YAML rule chain is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlRuleChain ruleChain) {
        if (ruleChain == null) return true;
        return ruleChain.getEnabled() == null || ruleChain.getEnabled();
    }

    /**
     * @return {@code true} if the YAML category is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlCategory category) {
        if (category == null) return true;
        return category.getEnabled() == null || category.getEnabled();
    }

    /**
     * @return {@code true} if the rule reference is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlRuleGroup.RuleReference ref) {
        if (ref == null) return true;
        return ref.getEnabled() == null || ref.getEnabled();
    }

    /**
     * @return {@code true} if the enrichment reference is enabled (null → enabled)
     */
    public static boolean isEnabled(YamlEnrichmentGroup.EnrichmentReference ref) {
        if (ref == null) return true;
        return ref.getEnabled() == null || ref.getEnabled();
    }

    // =========================================================================
    // External reference checks (boolean via isEnabled(), default true)
    // =========================================================================

    /**
     * @return {@code true} if the data-source reference is enabled
     */
    public static boolean isEnabled(YamlDataSourceRef ref) {
        return ref == null || ref.isEnabled();
    }

    /**
     * @return {@code true} if the rule reference is enabled
     */
    public static boolean isEnabled(YamlRuleRef ref) {
        return ref == null || ref.isEnabled();
    }

    /**
     * @return {@code true} if the enrichment reference is enabled
     */
    public static boolean isEnabled(YamlEnrichmentRef ref) {
        return ref == null || ref.isEnabled();
    }

    // =========================================================================
    // Scenario checks (boolean primitive, default true)
    // =========================================================================

    /**
     * @return {@code true} if the scenario configuration is enabled
     */
    public static boolean isEnabled(ScenarioConfiguration scenario) {
        return scenario == null || scenario.isEnabled();
    }

    /**
     * @return {@code true} if the scenario stage is enabled
     */
    public static boolean isEnabled(ScenarioStage stage) {
        return stage == null || stage.isEnabled();
    }

    // =========================================================================
    // Component checks (metadata-based, null metadata = enabled)
    // =========================================================================

    /**
     * @return {@code true} if the component is enabled (null metadata → enabled)
     */
    public static boolean isEnabled(ComponentConfiguration component) {
        if (component == null) return true;
        return component.getMetadata() == null || component.getMetadata().isEnabled();
    }

    // =========================================================================
    // List filtering helpers
    // =========================================================================

    /**
     * Filter a list of rules, returning only enabled ones.
     */
    public static List<Rule> filterRules(List<Rule> rules) {
        if (rules == null) return List.of();
        return rules.stream()
                .filter(EnabledFilter::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Filter a list of YAML rules, returning only enabled ones.
     */
    public static List<YamlRule> filterYamlRules(List<YamlRule> rules) {
        if (rules == null) return List.of();
        return rules.stream()
                .filter(EnabledFilter::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Filter a list of YAML enrichments, returning only enabled ones.
     */
    public static List<YamlEnrichment> filterEnrichments(List<YamlEnrichment> enrichments) {
        if (enrichments == null) return List.of();
        return enrichments.stream()
                .filter(EnabledFilter::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Filter a list of YAML transformations, returning only enabled ones.
     */
    public static List<YamlTransformation> filterTransformations(List<YamlTransformation> transformations) {
        if (transformations == null) return List.of();
        return transformations.stream()
                .filter(EnabledFilter::isEnabled)
                .collect(Collectors.toList());
    }
}
