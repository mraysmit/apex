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
package dev.mars.apex.core.service.schema.diff.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.mars.apex.core.service.schema.diff.json.model.SchemaDiffReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serializes and deserializes schema diff reports to/from JSON format.
 * This is the canonical serialization mechanism for schema comparison results.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2.1.0
 */
public class SchemaDiffJsonSerializer {

    private static final Logger logger = LoggerFactory.getLogger(SchemaDiffJsonSerializer.class);
    private static final String REPORT_DIR = "target/reports";
    
    private final ObjectMapper objectMapper;

    public SchemaDiffJsonSerializer() {
        this.objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Serialize comparison result to JSON file.
     *
     * @param report the schema diff report
     * @param outputPath the output file path (relative or absolute)
     * @return the absolute path to the generated JSON file
     * @throws IOException if file writing fails
     */
    public String toJsonFile(SchemaDiffReport report, String outputPath) throws IOException {
        Path resolvedPath = resolveReportPath(outputPath);
        Files.createDirectories(resolvedPath.getParent());

        logger.info("[SchemaDiff.JSON] Generating JSON report: {}", resolvedPath);

        String json = toJsonString(report);
        Files.writeString(resolvedPath, json);

        logger.info("[SchemaDiff.JSON] JSON report generated successfully: {}", resolvedPath);
        return resolvedPath.toAbsolutePath().toString();
    }

    /**
     * Serialize comparison result to JSON string.
     *
     * @param report the schema diff report
     * @return JSON string representation
     * @throws JsonProcessingException if serialization fails
     */
    public String toJsonString(SchemaDiffReport report) throws JsonProcessingException {
        return objectMapper.writeValueAsString(report);
    }

    /**
     * Deserialize JSON file to strongly-typed report object.
     *
     * @param jsonPath the path to the JSON file
     * @return the deserialized schema diff report
     * @throws IOException if file reading or parsing fails
     */
    public SchemaDiffReport fromJsonFile(String jsonPath) throws IOException {
        logger.info("[SchemaDiff.JSON] Loading JSON report from: {}", jsonPath);
        String json = Files.readString(Paths.get(jsonPath));
        return fromJsonString(json);
    }

    /**
     * Deserialize JSON string to strongly-typed report object.
     *
     * @param json the JSON string
     * @return the deserialized schema diff report
     * @throws JsonProcessingException if parsing fails
     */
    public SchemaDiffReport fromJsonString(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, SchemaDiffReport.class);
    }

    /**
     * Resolve report path following same logic as HTML reports.
     * If path has no parent directory, place in default reports directory.
     */
    private Path resolveReportPath(String reportPath) {
        Path path = Paths.get(reportPath);
        if (path.getParent() == null) {
            return Paths.get(REPORT_DIR, reportPath);
        }
        return path;
    }
}
