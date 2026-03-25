package dev.mars.apex.core.config.model.condition;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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

/**
 * Shared condition group with AND/OR logic for structured rule conditions.
 *
 * <p>This is a neutral abstraction extracted from the enrichment-specific
 * {@code YamlEnrichment.ConditionGroup} inner class. It can be used by
 * both rule evaluation and enrichment processing paths.</p>
 *
 * <p>The group evaluates its list of {@link SharedConditionRule} predicates
 * using short-circuit AND or OR semantics.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.5
 */
public class SharedConditionGroup {

    @JsonProperty("operator")
    private String operator; // "AND" or "OR"

    @JsonProperty("rules")
    private List<SharedConditionRule> rules;

    public SharedConditionGroup() {}

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<SharedConditionRule> getRules() {
        return rules;
    }

    public void setRules(List<SharedConditionRule> rules) {
        this.rules = rules;
    }
}
