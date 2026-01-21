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
package dev.mars.apex.core.service.schema.diff.json.generators;

import dev.mars.apex.core.service.schema.diff.json.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates Markdown reports from JSON schema diff data.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class JsonBasedMarkdownReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JsonBasedMarkdownReportGenerator.class);

    /**
     * Generate Markdown report from SchemaDiffReport object.
     *
     * @param report SchemaDiffReport data
     * @param outputPath Path to output Markdown file
     * @return Absolute path to generated Markdown file
     */
    public String generateFromReport(SchemaDiffReport report, String outputPath) throws IOException {
        logger.info("[SchemaDiff.MD] Generating Markdown report: {}", outputPath);
        
        StringBuilder md = new StringBuilder();
        
        // Header
        md.append("# Schema Diff Report\n\n");
        md.append("**Generated:** ").append(report.getMetadata().getGeneratedAt()).append("\n\n");
        md.append("**APEX Version:** ").append(report.getMetadata().getApexVersion())
          .append(" | **Report Version:** ").append(report.getMetadata().getReportVersion()).append("\n\n");
        md.append("**Source:** ").append(report.getSource().getName())
          .append(" (").append(report.getSource().getType()).append(") → ")
          .append("**Target:** ").append(report.getTarget().getName())
          .append(" (").append(report.getTarget().getType()).append(")\n\n");
        
        // Summary
        md.append("## 📈 Comparison Summary\n\n");
        ComparisonSummary.Statistics stats = report.getSummary().getStatistics();
        md.append("| Metric | Count |\n");
        md.append("|--------|-------|\n");
        md.append("| Matching | ").append(stats.getMatching()).append(" |\n");
        md.append("| ➕ Added | ").append(stats.getAdded()).append(" |\n");
        md.append("| ➖ Removed | ").append(stats.getRemoved()).append(" |\n");
        md.append("| 🔄 Changed | ").append(stats.getChanged()).append(" |\n\n");
        
        // Compatibility
        if (report.getCompatibility().isCompatible()) {
            md.append("> ✓ **Compatible Migration:** Target schema is backward compatible with source schema.\n\n");
        } else {
            md.append("> ⚠️ **Incompatible Migration:** Breaking changes detected that may cause data loss or runtime errors.\n\n");
        }
        
        // Matching columns
        if (report.getColumns().getMatching() != null && !report.getColumns().getMatching().isEmpty()) {
            md.append("## Matching Columns (").append(stats.getMatching()).append(")\n\n");
            md.append("| Column Name | Data Type | Nullable |\n");
            md.append("|-------------|-----------|----------|\n");
            for (ColumnDiff col : report.getColumns().getMatching()) {
                Boolean nullable = col.getSource() != null ? col.getSource().getNullable() : null;
                md.append("| `").append(col.getColumnName()).append("` | ")
                  .append(col.getSource().getDataType()).append(" | ")
                  .append(Boolean.TRUE.equals(nullable) ? "Yes" : "No").append(" |\n");
            }
            md.append("\n");
        }
        
        // Added columns
        if (report.getColumns().getAdded() != null && !report.getColumns().getAdded().isEmpty()) {
            md.append("## ➕ Added Columns (").append(stats.getAdded()).append(")\n\n");
            md.append("| Column Name | Data Type | Nullable |\n");
            md.append("|-------------|-----------|----------|\n");
            for (ColumnDiff col : report.getColumns().getAdded()) {
                Boolean nullable = col.getTarget() != null ? col.getTarget().getNullable() : null;
                md.append("| `").append(col.getColumnName()).append("` | ")
                  .append(col.getTarget().getDataType()).append(" | ")
                  .append(Boolean.TRUE.equals(nullable) ? "Yes" : "No").append(" |\n");
            }
            md.append("\n");
        }
        
        // Removed columns
        if (report.getColumns().getRemoved() != null && !report.getColumns().getRemoved().isEmpty()) {
            md.append("## ➖ Removed Columns (").append(stats.getRemoved()).append(")\n\n");
            md.append("| Column Name | Data Type |\n");
            md.append("|-------------|-----------|\n");
            for (ColumnDiff col : report.getColumns().getRemoved()) {
                md.append("| `").append(col.getColumnName()).append("` | ")
                  .append(col.getSource().getDataType()).append(" |\n");
            }
            md.append("\n");
        }
        
        // Changed columns
        if (report.getColumns().getChanged() != null && !report.getColumns().getChanged().isEmpty()) {
            md.append("## 🔄 Changed Columns (").append(stats.getChanged()).append(")\n\n");
            md.append("| Column Name | Source | Target |\n");
            md.append("|-------------|--------|--------|\n");
            for (ColumnDiff col : report.getColumns().getChanged()) {
                md.append("| `").append(col.getColumnName()).append("` | ")
                  .append(col.getSource().getDataType()).append(" | ")
                  .append(col.getTarget().getDataType()).append(" |\n");
            }
            md.append("\n");
        }
        
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, md.toString());
        
        logger.info("[SchemaDiff.MD] Markdown report generated successfully: {}", path.toAbsolutePath());
        return path.toAbsolutePath().toString();
    }

    /**
     * Generate Markdown report from JSON file.
     *
     * @param jsonPath Path to JSON schema diff file
     * @param outputPath Path to output Markdown file
     * @return Absolute path to generated Markdown file
     */
    public String generateFromJsonFile(String jsonPath, String outputPath) throws IOException {
        logger.info("[SchemaDiff.MD] Loading JSON from: {}", jsonPath);
        
        dev.mars.apex.core.service.schema.diff.json.SchemaDiffJsonSerializer serializer = 
            new dev.mars.apex.core.service.schema.diff.json.SchemaDiffJsonSerializer();
        
        SchemaDiffReport report = serializer.fromJsonFile(jsonPath);
        return generateFromReport(report, outputPath);
    }
}
