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

/**
 * Metadata about the schema diff report.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-18
 */
public class ReportMetadata {

    @JsonProperty("generatedAt")
    private String generatedAt;

    @JsonProperty("apexVersion")
    private String apexVersion = "2.1.0";

    @JsonProperty("reportVersion")
    private String reportVersion = "1.0";

    @JsonProperty("comparisonType")
    private String comparisonType;

    public ReportMetadata() {
    }

    public ReportMetadata(String reportVersion, java.time.Instant generatedAt, String generatedBy) {
        this.reportVersion = reportVersion;
        this.generatedAt = generatedAt.toString();
        // generatedBy is not a field in this class, just ignore it
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getApexVersion() {
        return apexVersion;
    }

    public void setApexVersion(String apexVersion) {
        this.apexVersion = apexVersion;
    }

    public String getReportVersion() {
        return reportVersion;
    }

    public void setReportVersion(String reportVersion) {
        this.reportVersion = reportVersion;
    }

    @JsonIgnore
    public String getGeneratedBy() {
        return "APEX Schema Diff Engine";
    }

    public String getComparisonType() {
        return comparisonType;
    }

    public void setComparisonType(String comparisonType) {
        this.comparisonType = comparisonType;
    }
}
