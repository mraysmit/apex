package dev.mars.apex.core.service.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Generates HTML reports for database schema metadata.
 * Creates detailed reports showing tables, columns, data types, keys, and other schema information.
 *
 * @author APEX Team
 * @since 2.1.0
 */
public class SchemaHtmlReportGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaHtmlReportGenerator.class);

    /**
     * Generate an HTML report for a single table schema.
     *
     * @param schema the schema metadata
     * @param outputPath the output file path
     * @throws IOException if file writing fails
     */
    public void generateReport(SchemaMetadata schema, String outputPath) throws IOException {
        generateReport(schema, null, outputPath);
    }

    /**
     * Generate an HTML report for a single table schema with data source context.
     *
     * @param schema the schema metadata
     * @param dataSourceContext the data source context (host, database, etc.)
     * @param outputPath the output file path
     * @throws IOException if file writing fails
     */
    public void generateReport(SchemaMetadata schema, DataSourceContext dataSourceContext, String outputPath) throws IOException {
        LOGGER.info("[SchemaReport] Generating HTML report for table: {}", schema.getSourceName());
        
        StringBuilder html = new StringBuilder();
        html.append(generateHtmlHeader("Database Schema Report - " + schema.getSourceName()));
        
        // Add data source context section if available
        if (dataSourceContext != null) {
            html.append(generateDataSourceSection(dataSourceContext));
        }
        
        html.append(generateTableSection(schema.getSourceName(), schema));
        html.append(generateHtmlFooter());
        
        writeToFile(html.toString(), outputPath);
        LOGGER.info("[SchemaReport] Report generated: {}", outputPath);
    }

    /**
     * Generate an HTML report for multiple table schemas.
     *
     * @param tableSchemas map of table names to schema metadata
     * @param outputPath the output file path
     * @throws IOException if file writing fails
     */
    public void generateReport(Map<String, SchemaMetadata> tableSchemas, String outputPath) throws IOException {
        generateReport(tableSchemas, null, outputPath);
    }

    /**
     * Generate an HTML report for multiple table schemas with data source context.
     *
     * @param tableSchemas map of table names to schema metadata
     * @param dataSourceContext the data source context (host, database, etc.)
     * @param outputPath the output file path
     * @throws IOException if file writing fails
     */
    public void generateReport(Map<String, SchemaMetadata> tableSchemas, DataSourceContext dataSourceContext, String outputPath) throws IOException {
        LOGGER.info("[SchemaReport] Generating HTML report for {} tables", tableSchemas.size());
        
        StringBuilder html = new StringBuilder();
        html.append(generateHtmlHeader("Database Schema Report - " + tableSchemas.size() + " Tables"));
        
        // Add data source context section if available
        if (dataSourceContext != null) {
            html.append(generateDataSourceSection(dataSourceContext));
        }
        
        // Generate table of contents
        html.append(generateTableOfContents(tableSchemas));
        
        // Generate section for each table
        tableSchemas.forEach((tableName, schema) -> {
            html.append(generateTableSection(tableName, schema));
        });
        
        html.append(generateHtmlFooter());
        
        writeToFile(html.toString(), outputPath);
        LOGGER.info("[SchemaReport] Report generated: {}", outputPath);
    }

    private String generateHtmlHeader(String title) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>""" + title + """
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
                    .metadata {
                        background-color: #f8f9fa;
                        padding: 15px;
                        border-radius: 5px;
                        margin-bottom: 20px;
                        font-size: 14px;
                        color: #666;
                    }
                    .data-source-info {
                        margin-bottom: 30px;
                    }
                    .data-source-info h3 {
                        color: #34495e;
                        margin-top: 0;
                        margin-bottom: 0;
                        padding: 10px;
                        background-color: #ecf0f1;
                        border-left: 4px solid #3498db;
                    }
                    .ds-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                        background-color: white;
                    }
                    .ds-table th {
                        background-color: #3498db;
                        color: white;
                        padding: 12px;
                        text-align: left;
                        font-weight: 600;
                        width: 200px;
                    }
                    .ds-table td {
                        padding: 10px 12px;
                        border-bottom: 1px solid #ddd;
                    }
                    .ds-table tr:hover {
                        background-color: #f8f9fa;
                    }
                    .toc {
                        background-color: #f8f9fa;
                        padding: 20px;
                        border-radius: 5px;
                        margin-bottom: 30px;
                    }
                    .toc h3 {
                        margin-top: 0;
                        color: #2c3e50;
                    }
                    .toc ul {
                        list-style-type: none;
                        padding-left: 0;
                    }
                    .toc li {
                        padding: 8px 0;
                        border-bottom: 1px solid #ddd;
                    }
                    .toc li:last-child {
                        border-bottom: none;
                    }
                    .toc a {
                        color: #3498db;
                        text-decoration: none;
                        font-weight: 500;
                    }
                    .toc a:hover {
                        text-decoration: underline;
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
                    .summary {
                        background-color: #e8f4f8;
                        padding: 15px;
                        border-radius: 5px;
                        margin: 20px 0;
                        border-left: 4px solid #3498db;
                    }
                    .summary strong {
                        color: #2c3e50;
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
                    <h1>""" + title + """
            </h1>
                    <div class="metadata">
                        <strong>Generated:</strong> """ + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + """
                        <br/>
                        <strong>Tool:</strong> APEX Rules Engine v2.1 - Schema Reader
                    </div>
            """;
    }

    private String generateTableOfContents(Map<String, SchemaMetadata> tableSchemas) {
        StringBuilder toc = new StringBuilder();
        toc.append("<div class=\"toc\">\n");
        toc.append("<h3>Table of Contents</h3>\n");
        toc.append("<ul>\n");
        
        tableSchemas.forEach((tableName, schema) -> {
            int columnCount = schema.getColumns().size();
            toc.append(String.format("<li><a href=\"#table-%s\">%s</a> <span style=\"color: #95a5a6;\">(%d columns)</span></li>\n", 
                                    tableName.toLowerCase(), tableName, columnCount));
        });
        
        toc.append("</ul>\n");
        toc.append("</div>\n");
        
        return toc.toString();
    }

    /**
     * Generate the data source information section.
     *
     * @param context the data source context
     * @return HTML string for the data source section
     */
    private String generateDataSourceSection(DataSourceContext context) {
        StringBuilder section = new StringBuilder();
        
        section.append("<div class=\"data-source-info\">\n");
        section.append("<h3>Data Source Information</h3>\n");
        
        section.append("<table class=\"ds-table\">\n");
        section.append("<tbody>\n");
        
        // Common fields
        if (context.getDataSourceName() != null) {
            appendTableRow(section, "Data Source Name", context.getDataSourceName());
        }
        
        // Type
        if (context.getDataSourceType() != null) {
            String typeLabel = context.isDatabase() ? "Database" : 
                              context.isFile() ? "File" : context.getDataSourceType();
            appendTableRow(section, "Source Type", typeLabel);
        }
        
        // Database-specific fields
        if (context.isDatabase()) {
            if (context.getDatabaseType() != null) {
                appendTableRow(section, "Database Type", context.getDatabaseType().toUpperCase());
            }
            if (context.getHost() != null) {
                appendTableRow(section, "Host", context.getHost());
            }
            if (context.getPort() != null) {
                appendTableRow(section, "Port", context.getPort().toString());
            }
            if (context.getDatabaseName() != null) {
                appendTableRow(section, "Database", context.getDatabaseName());
            }
            if (context.getSchemaName() != null) {
                appendTableRow(section, "Schema", context.getSchemaName());
            }
            if (context.getUsername() != null) {
                appendTableRow(section, "Username", context.getUsername());
            }
            if (context.getJdbcUrl() != null) {
                // Mask password in JDBC URL
                String maskedUrl = context.getJdbcUrl().replaceAll("password=[^&;]*", "password=****");
                appendTableRow(section, "JDBC URL", maskedUrl);
            }
        }
        
        // File-specific fields
        if (context.isFile()) {
            if (context.getFileName() != null) {
                appendTableRow(section, "File Name", context.getFileName());
            }
            if (context.getFileDirectory() != null) {
                appendTableRow(section, "Directory", context.getFileDirectory());
            }
            if (context.getFilePath() != null) {
                appendTableRow(section, "Full Path", context.getFilePath());
            }
        }
        
        // Filter parameters
        if (context.getSchemaFilter() != null) {
            appendTableRow(section, "Schema Filter", context.getSchemaFilter());
        }
        if (context.getTablePattern() != null) {
            appendTableRow(section, "Table Pattern", context.getTablePattern());
        }
        if (context.getExcludeTables() != null && !context.getExcludeTables().isEmpty()) {
            appendTableRow(section, "Excluded Tables", String.join(", ", context.getExcludeTables()));
        }
        
        // Additional properties
        if (context.getAdditionalProperties() != null && !context.getAdditionalProperties().isEmpty()) {
            context.getAdditionalProperties().forEach((key, value) -> {
                appendTableRow(section, formatPropertyName(key), value);
            });
        }
        
        section.append("</tbody>\n");
        section.append("</table>\n");
        section.append("</div>\n");
        
        return section.toString();
    }

    /**
     * Append a table row to the HTML builder.
     */
    private void appendTableRow(StringBuilder builder, String label, String value) {
        builder.append("<tr>\n");
        builder.append(String.format("<th>%s</th>\n", escapeHtml(label)));
        builder.append(String.format("<td>%s</td>\n", escapeHtml(value)));
        builder.append("</tr>\n");
    }

    /**
     * Append a single data source item to the HTML builder (legacy method kept for compatibility).
     */
    private void appendDataSourceItem(StringBuilder builder, String label, String value) {
        appendTableRow(builder, label, value);
    }

    /**
     * Check if context has any filter parameters.
     */
    private boolean hasFilterParameters(DataSourceContext context) {
        return context.getSchemaFilter() != null ||
               context.getTablePattern() != null ||
               (context.getExcludeTables() != null && !context.getExcludeTables().isEmpty());
    }

    /**
     * Format property name from camelCase or snake_case to Title Case.
     */
    private String formatPropertyName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        // Convert camelCase to space-separated, then capitalize
        String spaced = name.replaceAll("([A-Z])", " $1")
                           .replaceAll("[_-]", " ")
                           .trim();
        String[] words = spaced.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Escape HTML special characters.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private String generateTableSection(String tableName, SchemaMetadata schema) {
        StringBuilder section = new StringBuilder();
        
        section.append(String.format("<h2 id=\"table-%s\">%s</h2>\n", tableName.toLowerCase(), tableName));
        
        // Summary
        int pkCount = (int) schema.getColumns().stream().filter(SchemaMetadata.ColumnDefinition::isPrimaryKey).count();
        int nullableCount = (int) schema.getColumns().stream().filter(SchemaMetadata.ColumnDefinition::isNullable).count();
        
        section.append("<div class=\"summary\">\n");
        section.append(String.format("<strong>Total Columns:</strong> %d | ", schema.getColumns().size()));
        section.append(String.format("<strong>Primary Keys:</strong> %d | ", pkCount));
        section.append(String.format("<strong>Nullable:</strong> %d | ", nullableCount));
        section.append(String.format("<strong>Not Nullable:</strong> %d\n", schema.getColumns().size() - nullableCount));
        section.append("</div>\n");
        
        // Columns table
        section.append("<table>\n");
        section.append("<thead>\n");
        section.append("<tr>\n");
        section.append("<th>Column Name</th>\n");
        section.append("<th>Data Type</th>\n");
        section.append("<th>Nullable</th>\n");
        section.append("<th>Max Length</th>\n");
        section.append("<th>Precision</th>\n");
        section.append("<th>Scale</th>\n");
        section.append("<th>Attributes</th>\n");
        section.append("</tr>\n");
        section.append("</thead>\n");
        section.append("<tbody>\n");
        
        schema.getColumns().forEach(column -> {
            section.append("<tr>\n");
            
            // Column name (highlight if PK)
            String columnName = column.getName();
            if (column.isPrimaryKey()) {
                columnName = "<span class=\"primary-key\">" + columnName + "</span>";
            }
            section.append(String.format("<td>%s</td>\n", columnName));
            
            // Data type
            section.append(String.format("<td>%s</td>\n", column.getDataType()));
            
            // Nullable
            String nullableText = column.isNullable() ? 
                "<span class=\"nullable\">Yes</span>" : 
                "<span class=\"not-nullable\">No</span>";
            section.append(String.format("<td>%s</td>\n", nullableText));
            
            // Max length
            String maxLength = column.getMaxLength() != null ? column.getMaxLength().toString() : "-";
            section.append(String.format("<td>%s</td>\n", maxLength));
            
            // Precision
            String precision = column.getPrecision() != null ? column.getPrecision().toString() : "-";
            section.append(String.format("<td>%s</td>\n", precision));
            
            // Scale
            String scale = column.getScale() != null ? column.getScale().toString() : "-";
            section.append(String.format("<td>%s</td>\n", scale));
            
            // Attributes (badges)
            StringBuilder attributes = new StringBuilder();
            if (column.isPrimaryKey()) {
                attributes.append("<span class=\"badge badge-pk\">PRIMARY KEY</span>");
            }
            if (column.isNullable()) {
                attributes.append("<span class=\"badge badge-nullable\">NULLABLE</span>");
            }
            section.append(String.format("<td>%s</td>\n", attributes.toString()));
            
            section.append("</tr>\n");
        });
        
        section.append("</tbody>\n");
        section.append("</table>\n");
        
        return section.toString();
    }

    private String generateHtmlFooter() {
        return """
                    <div class="footer">
                        Generated by APEX Rules Engine - Schema Reader Service
                    </div>
                </div>
            </body>
            </html>
            """;
    }

    private void writeToFile(String content, String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        
        // Create parent directories if they don't exist
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        
        Files.writeString(path, content);
    }
}
