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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import dev.mars.apex.core.service.schema.diff.json.model.SchemaDiffReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates HTML reports from JSON schema diff data using Handlebars templates.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-01-18
 */
public class JsonBasedHtmlReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(JsonBasedHtmlReportGenerator.class);
    private final Handlebars handlebars;
    private Template compiledTemplate;

    public JsonBasedHtmlReportGenerator() {
        ClassPathTemplateLoader loader = new ClassPathTemplateLoader("/templates/schema-diff", ".hbs");
        this.handlebars = new Handlebars(loader);
        try {
            this.compiledTemplate = handlebars.compile("main");
            logger.info("[SchemaDiff.HTML] Handlebars template compiled successfully");
        } catch (IOException e) {
            logger.error("[SchemaDiff.HTML] Failed to compile Handlebars template: {}", e.getMessage());
            logger.debug("Stack trace for Handlebars template compilation failure:", e);
            throw new java.io.UncheckedIOException("Failed to compile Handlebars template", e);
        }
    }

    /**
     * Generate HTML report from SchemaDiffReport object.
     *
     * @param report SchemaDiffReport data
     * @param outputPath Path to output HTML file
     * @return Absolute path to generated HTML file
     */
    public String generateFromReport(SchemaDiffReport report, String outputPath) throws IOException {
        logger.info("[SchemaDiff.HTML] Generating HTML report: {}", outputPath);
        
        String html = compiledTemplate.apply(report);
        
        Path path = Paths.get(outputPath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, html);
        
        logger.info("[SchemaDiff.HTML] HTML report generated successfully: {}", path.toAbsolutePath());
        return path.toAbsolutePath().toString();
    }

    /**
     * Generate HTML report from JSON file.
     *
     * @param jsonPath Path to JSON schema diff file
     * @param outputPath Path to output HTML file
     * @return Absolute path to generated HTML file
     */
    public String generateFromJsonFile(String jsonPath, String outputPath) throws IOException {
        logger.info("[SchemaDiff.HTML] Loading JSON from: {}", jsonPath);
        
        // Use existing serializer to read JSON
        dev.mars.apex.core.service.schema.diff.json.SchemaDiffJsonSerializer serializer = 
            new dev.mars.apex.core.service.schema.diff.json.SchemaDiffJsonSerializer();
        
        SchemaDiffReport report = serializer.fromJsonFile(jsonPath);
        return generateFromReport(report, outputPath);
    }
}
