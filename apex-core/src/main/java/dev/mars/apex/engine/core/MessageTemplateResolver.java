package dev.mars.apex.engine.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves message template placeholders against SpEL evaluation contexts.
 *
 * <p>Supports two placeholder formats:</p>
 * <ul>
 *   <li>{@code {{#expression}}} - Handlebars-style (used in most YAML configs)</li>
 *   <li>{@code #{expression}} - SpEL template style (used by TemplateProcessorService)</li>
 * </ul>
 *
 * <p>The expression inside each placeholder is evaluated as a SpEL expression against
 * the provided context. If evaluation fails, the original placeholder is preserved.</p>
 *
 * <p>Extracted from {@link UnifiedRuleEvaluator} to isolate template resolution
 * from core rule evaluation logic.</p>
 *
 * @author Mark A Ray-Smith
 * @since 2026-02-28
 * @version 1.0
 */
public class MessageTemplateResolver {

    private static final Logger logger = LoggerFactory.getLogger(MessageTemplateResolver.class);

    /**
     * Pattern to match Handlebars-style placeholders in rule messages.
     * Matches {{#expression}} format used in YAML rule message templates.
     */
    private static final Pattern HANDLEBARS_PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(#[^}]+)\\}\\}");

    /**
     * Pattern to match SpEL template-style placeholders.
     * Matches #{expression} format used by TemplateProcessorService.
     */
    private static final Pattern HASH_PLACEHOLDER_PATTERN = Pattern.compile("#\\{([^}]+)\\}");

    private final ExpressionParser parser;

    /**
     * Create a new MessageTemplateResolver.
     *
     * @param parser The SpEL expression parser used to evaluate placeholder expressions
     */
    public MessageTemplateResolver(ExpressionParser parser) {
        this.parser = parser;
    }

    /**
     * Resolve message template placeholders against the SpEL evaluation context.
     *
     * @param message The message template to resolve
     * @param context The SpEL evaluation context containing variable bindings
     * @return The message with all resolvable placeholders replaced by their values
     */
    public String resolve(String message, EvaluationContext context) {
        if (message == null || context == null) {
            return message;
        }

        // Quick check: if no placeholders, return as-is
        if (!message.contains("{{#") && !message.contains("#{")) {
            return message;
        }

        String resolved = message;

        // Resolve {{#expression}} (Handlebars-style) placeholders
        if (resolved.contains("{{#")) {
            Matcher hbMatcher = HANDLEBARS_PLACEHOLDER_PATTERN.matcher(resolved);
            StringBuilder sb = new StringBuilder();
            while (hbMatcher.find()) {
                String spelExpr = hbMatcher.group(1); // e.g., "#age" or "#amount"
                try {
                    Expression expression = parser.parseExpression(spelExpr);
                    Object value = expression.getValue(context);
                    String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
                    hbMatcher.appendReplacement(sb, replacement);
                    logger.trace("Resolved message placeholder '{{{{{}}}}}' to '{}'", spelExpr, value);
                } catch (Exception e) {
                    // Preserve original placeholder on error
                    hbMatcher.appendReplacement(sb, Matcher.quoteReplacement(hbMatcher.group(0)));
                    logger.debug("Could not resolve message placeholder '{}': {}", spelExpr, e.getMessage());
                }
            }
            hbMatcher.appendTail(sb);
            resolved = sb.toString();
        }

        // Resolve #{expression} (SpEL template) placeholders
        if (resolved.contains("#{")) {
            Matcher spelMatcher = HASH_PLACEHOLDER_PATTERN.matcher(resolved);
            StringBuilder sb = new StringBuilder();
            while (spelMatcher.find()) {
                String spelExpr = spelMatcher.group(1); // e.g., "age" or "amount"
                try {
                    Expression expression = parser.parseExpression(spelExpr);
                    Object value = expression.getValue(context);
                    String replacement = value != null ? Matcher.quoteReplacement(value.toString()) : "";
                    spelMatcher.appendReplacement(sb, replacement);
                    logger.trace("Resolved message placeholder '#{{{}}}' to '{}'", spelExpr, value);
                } catch (Exception e) {
                    spelMatcher.appendReplacement(sb, Matcher.quoteReplacement(spelMatcher.group(0)));
                    logger.debug("Could not resolve message placeholder '{}': {}", spelExpr, e.getMessage());
                }
            }
            spelMatcher.appendTail(sb);
            resolved = sb.toString();
        }

        if (!resolved.equals(message)) {
            logger.debug("Resolved message template: '{}' -> '{}'", message, resolved);
        }

        return resolved;
    }
}
