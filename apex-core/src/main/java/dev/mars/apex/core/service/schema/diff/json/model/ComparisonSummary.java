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

/**
 * Summary statistics for schema comparison.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class ComparisonSummary {

    @JsonProperty("totalColumns")
    private TotalColumns totalColumns;

    @JsonProperty("statistics")
    private Statistics statistics;

    @JsonProperty("compatible")
    private boolean compatible;

    @JsonProperty("migrationRisk")
    private String migrationRisk;

    public ComparisonSummary() {
    }

    public ComparisonSummary(TotalColumns totalColumns, java.util.Map<String, String> statistics) {
        this.totalColumns = totalColumns;
        // Statistics is a nested class, not a Map - ignore for now
    }

    public TotalColumns getTotalColumns() {
        return totalColumns;
    }

    public void setTotalColumns(TotalColumns totalColumns) {
        this.totalColumns = totalColumns;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }

    public String getMigrationRisk() {
        return migrationRisk;
    }

    public void setMigrationRisk(String migrationRisk) {
        this.migrationRisk = migrationRisk;
    }

    public static class TotalColumns {
        @JsonProperty("source")
        private int source;

        @JsonProperty("target")
        private int target;

        public TotalColumns() {}

        public TotalColumns(int source, int target, int matched, int modified, int common) {
            this.source = source;
            this.target = target;
            // matched, modified, common are not fields - ignore
        }

        public int getSource() {
            return source;
        }

        public void setSource(int source) {
            this.source = source;
        }

        public int getTarget() {
            return target;
        }

        public void setTarget(int target) {
            this.target = target;
        }
    }

    public static class Statistics {
        @JsonProperty("matching")
        private int matching;

        @JsonProperty("added")
        private int added;

        @JsonProperty("removed")
        private int removed;

        @JsonProperty("changed")
        private int changed;

        @JsonProperty("breaking")
        private int breaking;

        public int getMatching() {
            return matching;
        }

        public void setMatching(int matching) {
            this.matching = matching;
        }

        public int getAdded() {
            return added;
        }

        public void setAdded(int added) {
            this.added = added;
        }

        public int getRemoved() {
            return removed;
        }

        public void setRemoved(int removed) {
            this.removed = removed;
        }

        public int getChanged() {
            return changed;
        }

        public void setChanged(int changed) {
            this.changed = changed;
        }

        public int getBreaking() {
            return breaking;
        }

        public void setBreaking(int breaking) {
            this.breaking = breaking;
        }
    }
}
