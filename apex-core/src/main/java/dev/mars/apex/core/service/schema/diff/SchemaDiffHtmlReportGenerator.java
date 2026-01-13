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
 * Created: 2026-01-13
 */
package dev.mars.apex.core.service.schema.diff;

import dev.mars.apex.core.service.schema.DataSourceContext;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult.ColumnDifference;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult.DifferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates HTML reports for schema comparison results with detailed column information.
 * Follows the same format as SchemaHtmlReportGenerator for consistency.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1
 */
public class SchemaDiffHtmlReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffHtmlReportGenerator.class);
    
    private static final String REPORT_DIR = "target/reports";
    
    public String generateReport(SchemaComparisonResult result, String reportPath) throws IOException {
        return generateReport(result, null, null, reportPath);
    }
    
    public String generateReport(SchemaComparisonResult result, DataSourceContext sourceContext, 
                                DataSourceContext targetContext, String reportPath) throws IOException {
        Path outputPath = resolveReportPath(reportPath);
        Files.createDirectories(outputPath.getParent());
        
        logger.info("Generating schema diff HTML report: {}", outputPath);
        
        StringBuilder html = new StringBuilder();
        html.append(generateHtmlHeader(result));
        html.append(generateDataSourceSections(sourceContext, targetContext));
        html.append(generateSummarySection(result));
        html.append(generateSourceSchemaSection(result));
        html.append(generateTargetSchemaSection(result));
        html.append(generateDifferencesSection(result));
        html.append(generateHtmlFooter());
        
        writeToFile(html.toString(), outputPath.toString());
        
        logger.info("Schema diff report generated successfully: {}", outputPath);
        return outputPath.toAbsolutePath().toString();
    }
    
    private Path resolveReportPath(String reportPath) {
        Path path = Paths.get(reportPath);
        if (path.getParent() == null) {
            return Paths.get(REPORT_DIR, reportPath);
        }
        return path;
    }
    
    private void writeToFile(String content, String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content);
    }
    
    private String generateHtmlHeader(SchemaComparisonResult result) {
        String sourceTable = result.getSourceSchema().getSourceName();
        String targetTable = result.getTargetSchema().getSourceName();
        String title = "Schema Comparison: " + sourceTable + " → " + targetTable;
        
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>""" + escapeHtml(title) + """
            </title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background-color: #f5f5f5;
                    }
                    .container {
                        max-width: 1400px;
                        margin: 0 auto;
                        background-color: white;
                        padding: 30px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        border-radius: 8px;
                    }
                    h1 {
                        color: #2c3e50;
                        border-bottom: 3px solid #3498db;
                        padding-bottom: 10px;
                        margin-bottom: 30px;
                    }
                    h2 {
                        color: #34495e;
                        margin-top: 40px;
                        padding: 10px;
                        background-color: #ecf0f1;
                        border-left: 4px solid #3498db;
                    }
                    h3 {
                        color: #34495e;
                        margin-top: 20px;
                    }
                    .metadata {
                        background-color: #f8f9fa;
                        padding: 15px;
                        border-radius: 5px;
                        margin-bottom: 20px;
                        font-size: 14px;
                        color: #666;
                    }
                    .comparison-header {
                        text-align: center;
                        margin-bottom: 30px;
                        padding: 20px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        border-radius: 8px;
                    }
                    .schema-names {
                        font-size: 1.3em;
                        margin: 10px 0;
                    }
                    .schema-name {
                        padding: 5px 15px;
                        border-radius: 5px;
                        background-color: rgba(255,255,255,0.2);
                        display: inline-block;
                    }
                    .arrow {
                        margin: 0 15px;
                        font-size: 1.5em;
                    }
                    .summary {
                        background-color: #e8f4f8;
                        padding: 20px;
                        border-radius: 5px;
                        margin: 20px 0;
                        border-left: 4px solid #3498db;
                    }
                    .summary h3 {
                        margin-top: 0;
                    }
                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                        gap: 15px;
                        margin-top: 15px;
                    }
                    .stat-card {
                        padding: 15px;
                        text-align: center;
                        border-radius: 5px;
                        color: white;
                        font-weight: bold;
                    }
                    .stat-matching { background-color: #3498db; }
                    .stat-added { background-color: #27ae60; }
                    .stat-removed { background-color: #e74c3c; }
                    .stat-changed { background-color: #f39c12; }
                    .stat-breaking { background-color: #c0392b; }
                    .stat-value {
                        font-size: 2em;
                        display: block;
                    }
                    .stat-label {
                        font-size: 0.9em;
                        opacity: 0.9;
                    }
                    .schema-section {
                        margin-bottom: 40px;
                        padding: 20px;
                        background-color: #f8f9fa;
                        border-radius: 5px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 20px 0;
                        background-color: white;
                    }
                    th {
                        background-color: #3498db;
                        color: white;
                        padding: 12px;
                        text-align: left;
                        font-weight: 600;
                    }
                    td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #ddd;
                    }
                    tr:hover {
                        background-color: #f8f9fa;
                    }
                    .diff-added {
                        background-color: #d4edda !important;
                        border-left: 4px solid #27ae60;
                    }
                    .diff-removed {
                        background-color: #f8d7da !important;
                        border-left: 4px solid #e74c3c;
                    }
                    .diff-changed {
                        background-color: #fff3cd !important;
                        border-left: 4px solid #f39c12;
                    }
                    .diff-matching {
                        background-color: #d1ecf1 !important;
                    }
                    .primary-key {
                        color: #e74c3c;
                        font-weight: bold;
                    }
                    .nullable {
                        color: #95a5a6;
                        font-style: italic;
                    }
                    .not-nullable {
                        color: #27ae60;
                        font-weight: 500;
                    }
                    .badge {
                        display: inline-block;
                        padding: 3px 8px;
                        border-radius: 3px;
                        font-size: 11px;
                        font-weight: 600;
                        margin-left: 5px;
                    }
                    .badge-pk {
                        background-color: #e74c3c;
                        color: white;
                    }
                    .badge-nullable {
                        background-color: #95a5a6;
                        color: white;
                    }
                    .badge-added {
                        background-color: #27ae60;
                        color: white;
                    }
                    .badge-removed {
                        background-color: #e74c3c;
                        color: white;
                    }
                    .badge-changed {
                        background-color: #f39c12;
                        color: white;
                    }
                    .badge-breaking {
                        background-color: #c0392b;
                        color: white;
                    }
                    .alert {
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 5px;
                        border-left: 4px solid;
                    }
                    .alert-success {
                        background-color: #d4edda;
                        border-color: #27ae60;
                        color: #155724;
                    }
                    .alert-warning {
                        background-color: #fff3cd;
                        border-color: #f39c12;
                        color: #856404;
                    }
                    .alert-danger {
                        background-color: #f8d7da;
                        border-color: #e74c3c;
                        color: #721c24;
                    }
                    .footer {
                        margin-top: 40px;
                        padding-top: 20px;
                        border-top: 1px solid #ddd;
                        text-align: center;
                        color: #95a5a6;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>""" + escapeHtml(title) + """
            </h1>
                    <div class="metadata">
                        <strong>Generated:</strong> """ + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + """
                        <br/>
                        <strong>Tool:</strong> APEX Rules Engine v2.1 - Schema Diff Service
                    </div>
                    <div class="comparison-header">
                        <div class="schema-names">
                            <span class="schema-name">""" + escapeHtml(sourceTable) + """
            </span>
                            <span class="arrow">→</span>
                            <span class="schema-name">""" + escapeHtml(targetTable) + """
            </span>
                        </div>
                    </div>
            """;
    }
    
    private String generateDataSourceSections(DataSourceContext sourceContext, DataSourceContext targetContext) {
        if (sourceContext == null && targetContext == null) {
            return ""; // No data source information available
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<h2>Data Source Information</h2>\n");
        html.append("<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px;\">\n");
        
        // Source data source
        html.append("<div class=\"schema-section\">\n");
        html.append("<h3>Source Database</h3>\n");
        if (sourceContext != null) {
            html.append(generateDataSourceTable(sourceContext));
        } else {
            html.append("<p style=\"color: #95a5a6;\">No data source information available</p>\n");
        }
        html.append("</div>\n");
        
        // Target data source
        html.append("<div class=\"schema-section\">\n");
        html.append("<h3>Target Database</h3>\n");
        if (targetContext != null) {
            html.append(generateDataSourceTable(targetContext));
        } else {
            html.append("<p style=\"color: #95a5a6;\">No data source information available</p>\n");
        }
        html.append("</div>\n");
        
        html.append("</div>\n");
        
        return html.toString();
    }
    
    private String generateDataSourceTable(DataSourceContext context) {
        StringBuilder html = new StringBuilder();
        
        html.append("<table style=\"width: 100%; background-color: white;\">\n");
        html.append("<tbody>\n");
        
        if (context.getDataSourceName() != null) {
            appendTableRow(html, "Data Source", context.getDataSourceName());
        }
        
        if (context.getDatabaseType() != null) {
            appendTableRow(html, "Database Type", context.getDatabaseType().toUpperCase());
        }
        
        if (context.getHost() != null) {
            appendTableRow(html, "Host", context.getHost());
        }
        
        if (context.getPort() != null) {
            appendTableRow(html, "Port", context.getPort().toString());
        }
        
        if (context.getDatabaseName() != null) {
            appendTableRow(html, "Database", context.getDatabaseName());
        }
        
        if (context.getSchemaName() != null) {
            appendTableRow(html, "Schema", context.getSchemaName());
        }
        
        if (context.getUsername() != null) {
            appendTableRow(html, "Username", context.getUsername());
        }
        
        html.append("</tbody>\n");
        html.append("</table>\n");
        
        return html.toString();
    }
    
    private void appendTableRow(StringBuilder html, String label, String value) {
        html.append("<tr>\n");
        html.append("<th style=\"background-color: #3498db; color: white; padding: 8px; text-align: left; width: 140px;\">")
            .append(escapeHtml(label)).append("</th>\n");
        html.append("<td style=\"padding: 8px; border-bottom: 1px solid #ddd;\">")
            .append(escapeHtml(value)).append("</td>\n");
        html.append("</tr>\n");
    }
    
    private String generateSummarySection(SchemaComparisonResult result) {
        StringBuilder html = new StringBuilder();
        
        html.append("<div class=\"summary\">\n");
        html.append("<h3>Comparison Summary</h3>\n");
        html.append("<div class=\"stats-grid\">\n");
        
        html.append(generateStatCard("Matching", result.getMatchingColumns().size(), "stat-matching"));
        html.append(generateStatCard("Added", result.getAddedColumns().size(), "stat-added"));
        html.append(generateStatCard("Removed", result.getRemovedColumns().size(), "stat-removed"));
        html.append(generateStatCard("Changed", result.getChangedColumns().size(), "stat-changed"));
        
        if (!result.getBreakingChanges().isEmpty()) {
            html.append(generateStatCard("Breaking", result.getBreakingChanges().size(), "stat-breaking"));
        }
        
        html.append("</div>\n");
        
        // Compatibility status
        if (result.isCompatible()) {
            html.append("<div class=\"alert alert-success\" style=\"margin-top: 20px;\">\n");
            html.append("<strong>✓ Compatible Migration:</strong> Target schema is backward compatible with source schema.\n");
            html.append("</div>\n");
        } else {
            html.append("<div class=\"alert alert-danger\" style=\"margin-top: 20px;\">\n");
            html.append("<strong>⚠ Breaking Changes Detected:</strong> Target schema contains incompatible changes:\n");
            html.append("<ul style=\"margin-top: 10px;\">\n");
            for (String breaking : result.getBreakingChanges()) {
                html.append("<li>").append(escapeHtml(breaking)).append("</li>\n");
            }
            html.append("</ul>\n");
            html.append("</div>\n");
        }
        
        html.append("</div>\n");
        
        return html.toString();
    }
    
    private String generateStatCard(String label, int value, String cssClass) {
        return String.format("""
            <div class="stat-card %s">
                <span class="stat-value">%d</span>
                <span class="stat-label">%s</span>
            </div>
            """, cssClass, value, label);
    }
    
    private String generateSourceSchemaSection(SchemaComparisonResult result) {
        StringBuilder html = new StringBuilder();
        SchemaMetadata schema = result.getSourceSchema();
        
        html.append("<h2>Source Schema: ").append(escapeHtml(schema.getSourceName())).append("</h2>\n");
        html.append("<div class=\"schema-section\">\n");
        html.append(generateSchemaMetadata(schema, "source"));
        html.append(generateColumnTable(schema, result, true));
        html.append("</div>\n");
        
        return html.toString();
    }
    
    private String generateTargetSchemaSection(SchemaComparisonResult result) {
        StringBuilder html = new StringBuilder();
        SchemaMetadata schema = result.getTargetSchema();
        
        html.append("<h2>Target Schema: ").append(escapeHtml(schema.getSourceName())).append("</h2>\n");
        html.append("<div class=\"schema-section\">\n");
        html.append(generateSchemaMetadata(schema, "target"));
        html.append(generateColumnTable(schema, result, false));
        html.append("</div>\n");
        
        return html.toString();
    }
    
    private String generateSchemaMetadata(SchemaMetadata schema, String label) {
        int pkCount = (int) schema.getColumns().stream()
            .filter(SchemaMetadata.ColumnDefinition::isPrimaryKey).count();
        int nullableCount = (int) schema.getColumns().stream()
            .filter(SchemaMetadata.ColumnDefinition::isNullable).count();
        
        return String.format("""
            <div style="background-color: white; padding: 15px; border-radius: 5px; margin-bottom: 20px;">
                <strong>Total Columns:</strong> %d | 
                <strong>Primary Keys:</strong> %d | 
                <strong>Nullable:</strong> %d | 
                <strong>Not Nullable:</strong> %d
            </div>
            """, 
            schema.getColumns().size(), pkCount, nullableCount, 
            schema.getColumns().size() - nullableCount);
    }
    
    private String generateColumnTable(SchemaMetadata schema, SchemaComparisonResult result, boolean isSource) {
        StringBuilder html = new StringBuilder();
        
        html.append("<table>\n");
        html.append("<thead>\n");
        html.append("<tr>\n");
        html.append("<th>Column Name</th>\n");
        html.append("<th>Data Type</th>\n");
        html.append("<th>Nullable</th>\n");
        html.append("<th>Max Length</th>\n");
        html.append("<th>Precision</th>\n");
        html.append("<th>Scale</th>\n");
        html.append("<th>Status</th>\n");
        html.append("</tr>\n");
        html.append("</thead>\n");
        html.append("<tbody>\n");
        
        for (SchemaMetadata.ColumnDefinition column : schema.getColumns()) {
            String diffClass = getDiffClass(column.getName(), result, isSource);
            String statusBadge = getStatusBadge(column.getName(), result, isSource);
            
            html.append("<tr class=\"").append(diffClass).append("\">\n");
            
            // Column name (with PK indicator)
            String columnName = column.getName();
            if (column.isPrimaryKey()) {
                columnName = "<span class=\"primary-key\">" + escapeHtml(columnName) + 
                           "</span><span class=\"badge badge-pk\">PK</span>";
            } else {
                columnName = escapeHtml(columnName);
            }
            html.append("<td>").append(columnName).append("</td>\n");
            
            // Data type
            html.append("<td>").append(escapeHtml(column.getDataType())).append("</td>\n");
            
            // Nullable
            String nullableText = column.isNullable() ? 
                "<span class=\"nullable\">Yes</span>" : 
                "<span class=\"not-nullable\">No</span>";
            html.append("<td>").append(nullableText).append("</td>\n");
            
            // Max length
            String maxLength = column.getMaxLength() != null ? column.getMaxLength().toString() : "-";
            html.append("<td>").append(maxLength).append("</td>\n");
            
            // Precision
            String precision = column.getPrecision() != null ? column.getPrecision().toString() : "-";
            html.append("<td>").append(precision).append("</td>\n");
            
            // Scale
            String scale = column.getScale() != null ? column.getScale().toString() : "-";
            html.append("<td>").append(scale).append("</td>\n");
            
            // Status badge
            html.append("<td>").append(statusBadge).append("</td>\n");
            
            html.append("</tr>\n");
        }
        
        html.append("</tbody>\n");
        html.append("</table>\n");
        
        return html.toString();
    }
    
    private String getDiffClass(String columnName, SchemaComparisonResult result, boolean isSource) {
        // Check if column exists in different lists
        boolean isAdded = result.getAddedColumns().stream()
            .anyMatch(d -> d.getColumnName().equals(columnName));
        boolean isRemoved = result.getRemovedColumns().stream()
            .anyMatch(d -> d.getColumnName().equals(columnName));
        boolean isChanged = result.getChangedColumns().stream()
            .anyMatch(d -> d.getColumnName().equals(columnName));
        boolean isMatching = result.getMatchingColumns().stream()
            .anyMatch(d -> d.getColumnName().equals(columnName));
        
        if (isSource) {
            if (isRemoved) return "diff-removed";
            if (isChanged) return "diff-changed";
        } else {
            if (isAdded) return "diff-added";
            if (isChanged) return "diff-changed";
        }
        
        if (isMatching) return "diff-matching";
        return "";
    }
    
    private String getStatusBadge(String columnName, SchemaComparisonResult result, boolean isSource) {
        // Check status
        boolean isAdded = result.getAddedColumns().stream()
            .anyMatch(d -> d.getColumnName().equals(columnName));
        boolean isRemoved = result.getRemovedColumns().stream()
            .anyMatch(d -> d.getColumnName().equals(columnName));
        
        ColumnDifference changedCol = result.getChangedColumns().stream()
            .filter(d -> d.getColumnName().equals(columnName))
            .findFirst()
            .orElse(null);
        
        if (isSource) {
            if (isRemoved) {
                return "<span class=\"badge badge-removed\">REMOVED</span>";
            }
            if (changedCol != null) {
                if (changedCol.getDifferenceType() == DifferenceType.BREAKING_CHANGE) {
                    return "<span class=\"badge badge-breaking\">BREAKING CHANGE</span>";
                }
                return "<span class=\"badge badge-changed\">CHANGED</span>";
            }
        } else {
            if (isAdded) {
                return "<span class=\"badge badge-added\">ADDED</span>";
            }
            if (changedCol != null) {
                if (changedCol.getDifferenceType() == DifferenceType.BREAKING_CHANGE) {
                    return "<span class=\"badge badge-breaking\">BREAKING CHANGE</span>";
                }
                return "<span class=\"badge badge-changed\">CHANGED</span>";
            }
        }
        
        return "-";
    }
    
    private String generateDifferencesSection(SchemaComparisonResult result) {
        StringBuilder html = new StringBuilder();
        
        html.append("<h2>Detailed Differences</h2>\n");
        
        // Added columns
        if (!result.getAddedColumns().isEmpty()) {
            html.append("<h3 style=\"color: #27ae60;\">✓ Added Columns (").append(result.getAddedColumns().size()).append(")</h3>\n");
            html.append("<table>\n");
            html.append("<thead><tr><th>Column</th><th>Type</th><th>Impact</th></tr></thead>\n");
            html.append("<tbody>\n");
            for (ColumnDifference diff : result.getAddedColumns()) {
                html.append("<tr class=\"diff-added\">\n");
                html.append("<td><strong>").append(escapeHtml(diff.getColumnName())).append("</strong></td>\n");
                html.append("<td>").append(escapeHtml(diff.getTargetType())).append("</td>\n");
                html.append("<td>Non-breaking (new column)</td>\n");
                html.append("</tr>\n");
            }
            html.append("</tbody></table>\n");
        }
        
        // Removed columns
        if (!result.getRemovedColumns().isEmpty()) {
            html.append("<h3 style=\"color: #e74c3c;\">⚠ Removed Columns (").append(result.getRemovedColumns().size()).append(")</h3>\n");
            html.append("<div class=\"alert alert-warning\">Removing columns may cause data loss and application compatibility issues.</div>\n");
            html.append("<table>\n");
            html.append("<thead><tr><th>Column</th><th>Type</th><th>Impact</th></tr></thead>\n");
            html.append("<tbody>\n");
            for (ColumnDifference diff : result.getRemovedColumns()) {
                html.append("<tr class=\"diff-removed\">\n");
                html.append("<td><strong>").append(escapeHtml(diff.getColumnName())).append("</strong></td>\n");
                html.append("<td>").append(escapeHtml(diff.getSourceType())).append("</td>\n");
                html.append("<td><span class=\"badge badge-breaking\">BREAKING</span> Data loss risk</td>\n");
                html.append("</tr>\n");
            }
            html.append("</tbody></table>\n");
        }
        
        // Changed columns
        if (!result.getChangedColumns().isEmpty()) {
            html.append("<h3 style=\"color: #f39c12;\">⚡ Changed Columns (").append(result.getChangedColumns().size()).append(")</h3>\n");
            html.append("<table>\n");
            html.append("<thead><tr><th>Column</th><th>Source Type</th><th>Target Type</th><th>Impact</th></tr></thead>\n");
            html.append("<tbody>\n");
            for (ColumnDifference diff : result.getChangedColumns()) {
                boolean isBreaking = diff.getDifferenceType() == DifferenceType.BREAKING_CHANGE;
                String rowClass = isBreaking ? "diff-removed" : "diff-changed";
                
                html.append("<tr class=\"").append(rowClass).append("\">\n");
                html.append("<td><strong>").append(escapeHtml(diff.getColumnName())).append("</strong></td>\n");
                html.append("<td>").append(escapeHtml(diff.getSourceType())).append("</td>\n");
                html.append("<td>").append(escapeHtml(diff.getTargetType())).append("</td>\n");
                
                if (isBreaking) {
                    html.append("<td><span class=\"badge badge-breaking\">BREAKING</span> Type narrowing detected</td>\n");
                } else {
                    html.append("<td>Compatible type widening</td>\n");
                }
                
                html.append("</tr>\n");
            }
            html.append("</tbody></table>\n");
        }
        
        return html.toString();
    }
    
    private String generateHtmlFooter() {
        return """
                    <div class="footer">
                        Generated by APEX Rules Engine v2.1 - Schema Diff Service
                    </div>
                </div>
            </body>
            </html>
            """;
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
