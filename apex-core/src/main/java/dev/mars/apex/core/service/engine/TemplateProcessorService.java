package dev.mars.apex.core.service.engine;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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

/**
 * Service for processing templates with SpEL expressions.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Service for processing templates with SpEL expressions.
 * This class handles replacing placeholders in templates with evaluated expressions.
 * Supports plain text, XML, and JSON formats.
 */
public class TemplateProcessorService {
    private static final Logger LOGGER = Logger.getLogger(TemplateProcessorService.class.getName());
    private final ExpressionEvaluatorService evaluatorService;

    public TemplateProcessorService(ExpressionEvaluatorService evaluatorService) {
        LOGGER.info("Initializing TemplateProcessorService");
        this.evaluatorService = evaluatorService;
        LOGGER.fine("Using evaluator service: " + evaluatorService.getClass().getSimpleName());
    }

    /**
     * Processes a template by replacing all placeholders with evaluated expressions.
     * Placeholders are in the format #{expression}.
     * 
     * @param template The template to process
     * @param context The evaluation context
     * @return The processed template
     */
    public String processTemplate(String template, EvaluationContext context) {
        LOGGER.info("Processing template with " + (template != null ? template.length() : 0) + " characters");
        LOGGER.fine("Template type: plain text");

        StringBuilder result = new StringBuilder();
        int pos = 0;
        int expressionsProcessed = 0;

        while (pos < template.length()) {
            int startExpr = template.indexOf("#{", pos);
            if (startExpr == -1) {
                result.append(template.substring(pos));
                break;
            }

            result.append(template.substring(pos, startExpr));
            int endExpr = template.indexOf("}", startExpr);
            if (endExpr == -1) {
                // Unclosed expression, append the rest as is
                LOGGER.warning("Unclosed expression found at position " + startExpr + ", treating as plain text");
                result.append(template.substring(startExpr));
                break;
            }

            // Extract and evaluate the expression
            String expr = template.substring(startExpr + 2, endExpr);
            LOGGER.fine("Processing expression: " + expr);
            try {
                Expression expression = evaluatorService.getParser().parseExpression(expr);
                Object value = expression.getValue(context);
                result.append(value != null ? value.toString() : "");
                expressionsProcessed++;
                LOGGER.finest("Expression '" + expr + "' evaluated to: " + value);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating expression '" + expr + "': " + e.getMessage(), e);
                // Keep the original expression on error
                result.append("#{").append(expr).append("}");
            }

            pos = endExpr + 1;
        }

        LOGGER.info("Template processing completed, " + expressionsProcessed + " expressions processed");
        return result.toString();
    }

    /**
     * Processes an XML template by replacing all placeholders with evaluated expressions.
     * Placeholders are in the format #{expression}.
     * This method properly escapes XML special characters in the evaluated expressions.
     * 
     * @param xmlTemplate The XML template to process
     * @param context The evaluation context
     * @return The processed XML template
     */
    public String processXmlTemplate(String xmlTemplate, EvaluationContext context) {
        LOGGER.info("Processing XML template with " + (xmlTemplate != null ? xmlTemplate.length() : 0) + " characters");
        LOGGER.fine("Template type: XML");

        StringBuilder result = new StringBuilder();
        int pos = 0;
        int expressionsProcessed = 0;

        while (pos < xmlTemplate.length()) {
            int startExpr = xmlTemplate.indexOf("#{", pos);
            if (startExpr == -1) {
                result.append(xmlTemplate.substring(pos));
                break;
            }

            result.append(xmlTemplate.substring(pos, startExpr));
            int endExpr = xmlTemplate.indexOf("}", startExpr);
            if (endExpr == -1) {
                // Unclosed expression, append the rest as is
                LOGGER.warning("Unclosed expression found at position " + startExpr + " in XML template, treating as plain text");
                result.append(xmlTemplate.substring(startExpr));
                break;
            }

            // Extract and evaluate the expression
            String expr = xmlTemplate.substring(startExpr + 2, endExpr);
            LOGGER.fine("Processing XML expression: " + expr);
            try {
                Expression expression = evaluatorService.getParser().parseExpression(expr);
                Object value = expression.getValue(context);
                String escapedValue = value != null ? escapeXml(value.toString()) : "";
                result.append(escapedValue);
                expressionsProcessed++;
                LOGGER.finest("XML expression '" + expr + "' evaluated to: " + value + " (escaped: " + escapedValue + ")");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating XML expression '" + expr + "': " + e.getMessage(), e);
                // Keep the original expression on error
                result.append("#{").append(expr).append("}");
            }

            pos = endExpr + 1;
        }

        LOGGER.info("XML template processing completed, " + expressionsProcessed + " expressions processed");
        return result.toString();
    }

    /**
     * Processes a JSON template by replacing all placeholders with evaluated expressions.
     * Placeholders are in the format #{expression}.
     * This method properly escapes JSON special characters in the evaluated expressions.
     * 
     * @param jsonTemplate The JSON template to process
     * @param context The evaluation context
     * @return The processed JSON template
     */
    public String processJsonTemplate(String jsonTemplate, EvaluationContext context) {
        LOGGER.info("Processing JSON template with " + (jsonTemplate != null ? jsonTemplate.length() : 0) + " characters");
        LOGGER.fine("Template type: JSON");

        StringBuilder result = new StringBuilder();
        int pos = 0;
        int expressionsProcessed = 0;

        while (pos < jsonTemplate.length()) {
            int startExpr = jsonTemplate.indexOf("#{", pos);
            if (startExpr == -1) {
                result.append(jsonTemplate.substring(pos));
                break;
            }

            result.append(jsonTemplate.substring(pos, startExpr));
            int endExpr = jsonTemplate.indexOf("}", startExpr);
            if (endExpr == -1) {
                // Unclosed expression, append the rest as is
                LOGGER.warning("Unclosed expression found at position " + startExpr + " in JSON template, treating as plain text");
                result.append(jsonTemplate.substring(startExpr));
                break;
            }

            // Extract and evaluate the expression
            String expr = jsonTemplate.substring(startExpr + 2, endExpr);
            LOGGER.fine("Processing JSON expression: " + expr);
            try {
                Expression expression = evaluatorService.getParser().parseExpression(expr);
                Object value = expression.getValue(context);
                String escapedValue = value != null ? escapeJson(value.toString()) : "";
                result.append(escapedValue);
                expressionsProcessed++;
                LOGGER.finest("JSON expression '" + expr + "' evaluated to: " + value + " (escaped: " + escapedValue + ")");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error evaluating JSON expression '" + expr + "': " + e.getMessage(), e);
                // Keep the original expression on error
                result.append("#{").append(expr).append("}");
            }

            pos = endExpr + 1;
        }

        LOGGER.info("JSON template processing completed, " + expressionsProcessed + " expressions processed");
        return result.toString();
    }

    /**
     * Escapes XML special characters in a string.
     * 
     * @param str The string to escape
     * @return The escaped string
     */
    private String escapeXml(String str) {
        LOGGER.finest("Escaping XML string: " + (str != null ? str.length() + " chars" : "null"));

        if (str == null) {
            LOGGER.finest("Input string is null, returning empty string");
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int specialCharsEscaped = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    specialCharsEscaped++;
                    break;
                case '>':
                    sb.append("&gt;");
                    specialCharsEscaped++;
                    break;
                case '&':
                    sb.append("&amp;");
                    specialCharsEscaped++;
                    break;
                case '"':
                    sb.append("&quot;");
                    specialCharsEscaped++;
                    break;
                case '\'':
                    sb.append("&apos;");
                    specialCharsEscaped++;
                    break;
                default:
                    sb.append(c);
            }
        }

        if (specialCharsEscaped > 0) {
            LOGGER.finest("Escaped " + specialCharsEscaped + " special XML characters");
        }

        return sb.toString();
    }

    /**
     * Escapes JSON special characters in a string.
     * 
     * @param str The string to escape
     * @return The escaped string
     */
    private String escapeJson(String str) {
        LOGGER.finest("Escaping JSON string: " + (str != null ? str.length() + " chars" : "null"));

        if (str == null) {
            LOGGER.finest("Input string is null, returning empty string");
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int specialCharsEscaped = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    specialCharsEscaped++;
                    break;
                case '\\':
                    sb.append("\\\\");
                    specialCharsEscaped++;
                    break;
                case '/':
                    sb.append("\\/");
                    specialCharsEscaped++;
                    break;
                case '\b':
                    sb.append("\\b");
                    specialCharsEscaped++;
                    break;
                case '\f':
                    sb.append("\\f");
                    specialCharsEscaped++;
                    break;
                case '\n':
                    sb.append("\\n");
                    specialCharsEscaped++;
                    break;
                case '\r':
                    sb.append("\\r");
                    specialCharsEscaped++;
                    break;
                case '\t':
                    sb.append("\\t");
                    specialCharsEscaped++;
                    break;
                default:
                    sb.append(c);
            }
        }

        if (specialCharsEscaped > 0) {
            LOGGER.finest("Escaped " + specialCharsEscaped + " special JSON characters");
        }

        return sb.toString();
    }
}
