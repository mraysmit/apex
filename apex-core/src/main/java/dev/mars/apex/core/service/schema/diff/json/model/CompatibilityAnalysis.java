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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility analysis including breaking changes and safe changes.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-18
 */
public class CompatibilityAnalysis {

    @JsonProperty("compatible")
    private boolean compatible;

    @JsonProperty("overallRisk")
    private String overallRisk;

    @JsonProperty("breakingChanges")
    private List<BreakingChange> breakingChanges = new ArrayList<>();

    @JsonProperty("safeChanges")
    private List<SafeChange> safeChanges = new ArrayList<>();

    public CompatibilityAnalysis() {
    }

    public CompatibilityAnalysis(boolean compatible, List<BreakingChange> breakingChanges, List<SafeChange> safeChanges) {
        this.compatible = compatible;
        this.breakingChanges = breakingChanges;
        this.safeChanges = safeChanges;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }

    public String getOverallRisk() {
        return overallRisk;
    }

    public void setOverallRisk(String overallRisk) {
        this.overallRisk = overallRisk;
    }

    public List<BreakingChange> getBreakingChanges() {
        return breakingChanges;
    }

    public void setBreakingChanges(List<BreakingChange> breakingChanges) {
        this.breakingChanges = breakingChanges;
    }

    public List<SafeChange> getSafeChanges() {
        return safeChanges;
    }

    public void setSafeChanges(List<SafeChange> safeChanges) {
        this.safeChanges = safeChanges;
    }

    public static class BreakingChange {
        @JsonProperty("severity")
        private String severity;

        @JsonProperty("category")
        private String category;

        @JsonProperty("description")
        private String description;

        @JsonProperty("affectedColumn")
        private String affectedColumn;

        @JsonProperty("recommendation")
        private String recommendation;

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAffectedColumn() {
            return affectedColumn;
        }

        public void setAffectedColumn(String affectedColumn) {
            this.affectedColumn = affectedColumn;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public void setRecommendation(String recommendation) {
            this.recommendation = recommendation;
        }
    }

    public static class SafeChange {
        @JsonProperty("category")
        private String category;

        @JsonProperty("description")
        private String description;

        @JsonProperty("affectedColumn")
        private String affectedColumn;

        @JsonProperty("impact")
        private String impact;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAffectedColumn() {
            return affectedColumn;
        }

        public void setAffectedColumn(String affectedColumn) {
            this.affectedColumn = affectedColumn;
        }

        public String getImpact() {
            return impact;
        }

        public void setImpact(String impact) {
            this.impact = impact;
        }
    }
}
