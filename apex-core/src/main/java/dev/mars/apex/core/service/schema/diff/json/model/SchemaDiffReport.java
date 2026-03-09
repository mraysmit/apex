/*
 * Copyright 2026 Mark Andrew Ray-Smith Cityline Ltd
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
 *
 * Created: 2026-01-17
 */
package dev.mars.apex.core.service.schema.diff.json.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON representation of a schema diff report.
 * This is the canonical serialization format for schema comparison results.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-18
 */
@JsonPropertyOrder({"$schema", "metadata", "source", "target", "summary", "columns", "compatibility", "recommendations"})
public class SchemaDiffReport {

    @JsonProperty("$schema")
    private String schema = "https://apex.mars.dev/schemas/schema-diff/v1.0.json";

    @JsonProperty("metadata")
    private ReportMetadata metadata;

    @JsonProperty("source")
    private DataSourceInfo source;

    @JsonProperty("target")
    private DataSourceInfo target;

    @JsonProperty("summary")
    private ComparisonSummary summary;

    @JsonProperty("columns")
    private ColumnComparison columns;

    @JsonProperty("compatibility")
    private CompatibilityAnalysis compatibility;

    @JsonProperty("recommendations")
    private List<Recommendation> recommendations = new ArrayList<>();

    // Default constructor for Jackson
    public SchemaDiffReport() {
    }

    // Getters and Setters
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public ReportMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ReportMetadata metadata) {
        this.metadata = metadata;
    }

    public DataSourceInfo getSource() {
        return source;
    }

    public void setSource(DataSourceInfo source) {
        this.source = source;
    }

    public DataSourceInfo getTarget() {
        return target;
    }

    public void setTarget(DataSourceInfo target) {
        this.target = target;
    }

    public ComparisonSummary getSummary() {
        return summary;
    }

    public void setSummary(ComparisonSummary summary) {
        this.summary = summary;
    }

    public ColumnComparison getColumns() {
        return columns;
    }

    public void setColumns(ColumnComparison columns) {
        this.columns = columns;
    }

    public CompatibilityAnalysis getCompatibility() {
        return compatibility;
    }

    public void setCompatibility(CompatibilityAnalysis compatibility) {
        this.compatibility = compatibility;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    /**
     * Convenience method for tests that expect getComparison() to return column comparisons.
     */
    @JsonIgnore
    public ColumnComparison getComparison() {
        return getColumns();
    }

    /**
     * Builder for SchemaDiffReport.
     */
    public static class Builder {
        private final SchemaDiffReport report = new SchemaDiffReport();

        public Builder metadata(ReportMetadata metadata) {
            report.metadata = metadata;
            return this;
        }

        public Builder source(DataSourceInfo source) {
            report.source = source;
            return this;
        }

        public Builder target(DataSourceInfo target) {
            report.target = target;
            return this;
        }

        public Builder summary(ComparisonSummary summary) {
            report.summary = summary;
            return this;
        }

        public Builder columns(ColumnComparison columns) {
            report.columns = columns;
            return this;
        }

        public Builder comparison(ColumnComparison comparison) {
            report.columns = comparison;
            return this;
        }

        public Builder compatibility(CompatibilityAnalysis compatibility) {
            report.compatibility = compatibility;
            return this;
        }

        public Builder addRecommendation(Recommendation recommendation) {
            report.recommendations.add(recommendation);
            return this;
        }

        public Builder recommendations(List<Recommendation> recommendations) {
            report.recommendations = new ArrayList<>(recommendations);
            return this;
        }

        public SchemaDiffReport build() {
            return report;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
