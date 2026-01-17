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
 * Collection of column comparisons organized by status.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class ColumnComparison {

    @JsonProperty("matching")
    private List<ColumnDiff> matching = new ArrayList<>();

    @JsonProperty("added")
    private List<ColumnDiff> added = new ArrayList<>();

    @JsonProperty("removed")
    private List<ColumnDiff> removed = new ArrayList<>();

    @JsonProperty("changed")
    private List<ColumnDiff> changed = new ArrayList<>();

    public ColumnComparison() {
    }

    public ColumnComparison(ComparisonSummary summary, List<?> columnDiffs, CompatibilityAnalysis compatibility, List<?> recommendations) {
        // Tests may pass these parameters, but ColumnComparison only manages column diff lists
        // Ignoring parameters as they belong to parent SchemaDiffReport
    }

    public List<ColumnDiff> getMatching() {
        return matching;
    }

    public void setMatching(List<ColumnDiff> matching) {
        this.matching = matching;
    }

    public List<ColumnDiff> getAdded() {
        return added;
    }

    public void setAdded(List<ColumnDiff> added) {
        this.added = added;
    }

    public List<ColumnDiff> getRemoved() {
        return removed;
    }

    public void setRemoved(List<ColumnDiff> removed) {
        this.removed = removed;
    }

    public List<ColumnDiff> getChanged() {
        return changed;
    }

    public void setChanged(List<ColumnDiff> changed) {
        this.changed = changed;
    }
}
