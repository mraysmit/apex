package dev.mars.apex.core.config.model.condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.mars.apex.core.config.model.YamlEnrichment;

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
 * Individual condition predicate for structured rule conditions.
 *
 * <p>This is a neutral abstraction extracted from the enrichment-specific
 * {@code YamlEnrichment.ConditionRule} inner class. It supports three
 * resolution types via the {@code type} field:</p>
 * <ul>
 *   <li>{@code "expression"} (default) — pure SpEL evaluation</li>
 *   <li>{@code "lookup"} — execute a lookup, stash result, then evaluate SpEL condition</li>
 *   <li>{@code "function"} — invoke an enrichment group, stash output, then evaluate SpEL condition</li>
 * </ul>
 *
 * <p>The {@code condition} SpEL string is always the final boolean gate regardless of type.</p>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.5
 */
public class SharedConditionRule {

    @JsonProperty("type")
    private String type; // "expression" (default), "lookup", "function"

    @JsonProperty("condition")
    private String condition; // SpEL expression → Boolean (always present)

    @JsonProperty("description")
    private String description;

    // For type: "lookup"
    @JsonProperty("lookup-config")
    private YamlEnrichment.LookupConfig lookupConfig;

    @JsonProperty("result-field")
    private String resultField; // Context field to stash lookup result

    // For type: "function"
    @JsonProperty("enrichment-group-ref")
    private String enrichmentGroupRef;

    @JsonProperty("input-parameters")
    private List<YamlEnrichment.FieldMapping> inputParameters;

    @JsonProperty("output-field")
    private String outputField; // Context field to stash function output

    public SharedConditionRule() {}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public YamlEnrichment.LookupConfig getLookupConfig() {
        return lookupConfig;
    }

    public void setLookupConfig(YamlEnrichment.LookupConfig lookupConfig) {
        this.lookupConfig = lookupConfig;
    }

    public String getResultField() {
        return resultField;
    }

    public void setResultField(String resultField) {
        this.resultField = resultField;
    }

    public String getEnrichmentGroupRef() {
        return enrichmentGroupRef;
    }

    public void setEnrichmentGroupRef(String enrichmentGroupRef) {
        this.enrichmentGroupRef = enrichmentGroupRef;
    }

    public List<YamlEnrichment.FieldMapping> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(List<YamlEnrichment.FieldMapping> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public String getOutputField() {
        return outputField;
    }

    public void setOutputField(String outputField) {
        this.outputField = outputField;
    }
}
