/*
 * Copyright (c) 2024 Michael Rayment Smith
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
package dev.mars.apex.core.service.schema.diff.json;

import dev.mars.apex.core.service.schema.DataSourceContext;
import dev.mars.apex.core.service.schema.SchemaMetadata;
import dev.mars.apex.core.service.schema.diff.SchemaComparisonResult;
import dev.mars.apex.core.service.schema.diff.json.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds SchemaDiffReport from SchemaComparisonResult and DataSourceContext.
 * Converts domain model to JSON representation.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class SchemaDiffReportBuilder {

    /**
     * Build a complete schema diff report from comparison result and contexts.
     *
     * @param result the schema comparison result
     * @param sourceContext the source data source context (may be null)
     * @param targetContext the target data source context (may be null)
     * @return the schema diff report
     */
    public SchemaDiffReport buildReport(SchemaComparisonResult result,
                                        DataSourceContext sourceContext,
                                        DataSourceContext targetContext) {
        
        ReportMetadata metadata = new ReportMetadata();
        metadata.setGeneratedAt(Instant.now().toString());
        metadata.setApexVersion("2.1.0");
        metadata.setReportVersion("1.0");
        
        DataSourceInfo source = buildDataSourceInfo(result.getSourceSchema(), sourceContext);
        DataSourceInfo target = buildDataSourceInfo(result.getTargetSchema(), targetContext);
        
        ComparisonSummary summary = buildSummary(result);
        ColumnComparison columns = buildColumnComparison(result);
        CompatibilityAnalysis compatibility = buildCompatibilityAnalysis(result);
        List<Recommendation> recommendations = buildRecommendations(result);
        
        return SchemaDiffReport.builder()
            .metadata(metadata)
            .source(source)
            .target(target)
            .summary(summary)
            .columns(columns)
            .compatibility(compatibility)
            .recommendations(recommendations)
            .build();
    }

    private DataSourceInfo buildDataSourceInfo(SchemaMetadata schema, DataSourceContext context) {
        String name = (context != null) ? context.getDataSourceName() : "Unknown";
        String type = (context != null) ? context.getDataSourceType() : "Unknown";
        
        DataSourceInfo info = new DataSourceInfo();
        info.setName(name);
        info.setType(type);
        
        DataSourceInfo.ConnectionInfo connection = new DataSourceInfo.ConnectionInfo();
        if (context != null && context.isFile()) {
            connection.setFilePath(context.getFilePath());
        } else if (context != null && context.isDatabase()) {
            connection.setHost(context.getHost());
            connection.setPort(context.getPort());
            connection.setDatabase(context.getDatabaseName());
            connection.setSchema(context.getSchemaName());
        }
        info.setConnection(connection);
        
        DataSourceInfo.TableMetadata tableMetadata = new DataSourceInfo.TableMetadata();
        tableMetadata.setTableName(schema.getSourceName());
        tableMetadata.setColumns(schema.getColumns().size());
        info.setTableMetadata(tableMetadata);
        
        return info;
    }

    private ComparisonSummary buildSummary(SchemaComparisonResult result) {
        int sourceTotal = result.getSourceSchema().getColumns().size();
        int targetTotal = result.getTargetSchema().getColumns().size();
        
        ComparisonSummary summary = new ComparisonSummary();
        ComparisonSummary.TotalColumns totalColumns = new ComparisonSummary.TotalColumns();
        totalColumns.setSource(sourceTotal);
        totalColumns.setTarget(targetTotal);
        summary.setTotalColumns(totalColumns);
        
        // Build statistics from the comparison result
        ComparisonSummary.Statistics statistics = new ComparisonSummary.Statistics();
        statistics.setMatching(result.getMatchingColumns().size());
        statistics.setAdded(result.getAddedColumns().size());
        statistics.setRemoved(result.getRemovedColumns().size());
        statistics.setChanged(result.getChangedColumns().size());
        statistics.setBreaking(result.getBreakingChanges().size());
        summary.setStatistics(statistics);
        
        summary.setCompatible(result.isCompatible());
        summary.setMigrationRisk(result.isCompatible() ? "LOW" : "HIGH");
        
        return summary;
    }

    private ColumnComparison buildColumnComparison(SchemaComparisonResult result) {
        ColumnComparison comparison = new ColumnComparison();
        
        // Process source columns to find matching and removed columns
        for (SchemaMetadata.ColumnDefinition sourceCol : result.getSourceSchema().getColumns()) {
            ColumnDiff.ColumnInfo sourceInfo = buildColumnInfo(sourceCol);
            
            // Try to find matching column in target
            SchemaMetadata.ColumnDefinition targetCol = findColumnByName(
                result.getTargetSchema(),
                sourceCol.getName()
            );
            
            ColumnDiff.ColumnInfo targetInfo = (targetCol != null) ? buildColumnInfo(targetCol) : null;
            
            String status = (targetCol != null) ? "MATCHED" : "MISSING_IN_TARGET";
            List<ColumnDiff.PropertyDifference> differences = new ArrayList<>();
            
            ColumnDiff diff = new ColumnDiff(
                sourceCol.getName(),
                status,
                sourceInfo,
                targetInfo,
                differences
            );
            
            if ("MATCHED".equals(status)) {
                comparison.getMatching().add(diff);
            } else {
                comparison.getRemoved().add(diff);
            }
        }
        
        // Process target columns to find added columns (exist in target but not in source)
        for (SchemaMetadata.ColumnDefinition targetCol : result.getTargetSchema().getColumns()) {
            // Check if this column exists in source
            SchemaMetadata.ColumnDefinition sourceCol = findColumnByName(
                result.getSourceSchema(),
                targetCol.getName()
            );
            
            // If not found in source, it's an added column
            if (sourceCol == null) {
                ColumnDiff.ColumnInfo targetInfo = buildColumnInfo(targetCol);
                
                ColumnDiff diff = new ColumnDiff(
                    targetCol.getName(),
                    "ADDED",
                    null,  // no source info for added columns
                    targetInfo,
                    new ArrayList<>()
                );
                
                comparison.getAdded().add(diff);
            }
        }
        
        return comparison;
    }

    private List<ColumnDiff> buildColumnDiffs(SchemaComparisonResult result) {
        List<ColumnDiff> diffs = new ArrayList<>();
        
        // For now, create diffs for all columns in source
        for (SchemaMetadata.ColumnDefinition col : result.getSourceSchema().getColumns()) {
            ColumnDiff.ColumnInfo sourceInfo = buildColumnInfo(col);
            
            // Try to find matching column in target
            SchemaMetadata.ColumnDefinition targetCol = findColumnByName(
                result.getTargetSchema(),
                col.getName()
            );
            
            ColumnDiff.ColumnInfo targetInfo = (targetCol != null) ? buildColumnInfo(targetCol) : null;
            
            String status = (targetCol != null) ? "MATCHED" : "MISSING_IN_TARGET";
            List<ColumnDiff.PropertyDifference> differences = new ArrayList<>();
            
            ColumnDiff diff = new ColumnDiff(
                col.getName(),
                status,
                sourceInfo,
                targetInfo,
                differences
            );
            
            diffs.add(diff);
        }
        
        return diffs;
    }

    private SchemaMetadata.ColumnDefinition findColumnByName(SchemaMetadata schema, String columnName) {
        return schema.getColumns().stream()
            .filter(col -> col.getName().equalsIgnoreCase(columnName))
            .findFirst()
            .orElse(null);
    }

    private ColumnDiff.ColumnInfo buildColumnInfo(SchemaMetadata.ColumnDefinition column) {
        ColumnDiff.ColumnInfo info = new ColumnDiff.ColumnInfo();
        info.setDataType(column.getDataType());
        info.setSize(column.getMaxLength());
        info.setNullable(column.isNullable());
        info.setPrimaryKey(column.isPrimaryKey());
        return info;
    }

    private CompatibilityAnalysis buildCompatibilityAnalysis(SchemaComparisonResult result) {
        CompatibilityAnalysis analysis = new CompatibilityAnalysis();
        analysis.setCompatible(result.isCompatible());
        analysis.setBreakingChanges(new ArrayList<>());
        analysis.setSafeChanges(new ArrayList<>());
        return analysis;
    }

    private List<Recommendation> buildRecommendations(SchemaComparisonResult result) {
        List<Recommendation> recommendations = new ArrayList<>();
        Recommendation rec = new Recommendation();
        if (result.isCompatible()) {
            rec.setPriority("LOW");
            rec.setDescription("Schema is fully compatible");
        } else {
            rec.setPriority("HIGH");
            rec.setDescription("Schema has breaking changes that require attention");
        }
        recommendations.add(rec);
        return recommendations;
    }
}
