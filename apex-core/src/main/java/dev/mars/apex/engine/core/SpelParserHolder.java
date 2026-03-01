package dev.mars.apex.engine.core;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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
 * Shared singleton holder for the SpEL expression parser.
 *
 * <p>{@link SpelExpressionParser} is thread-safe and stateless, so a single shared
 * instance is sufficient for the entire APEX system. This holder eliminates redundant
 * parser allocations across 8+ classes that previously each created their own instance.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * Expression expr = SpelParserHolder.INSTANCE.parseExpression(spelString);
 * }</pre>
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2026-03-01
 * @version 1.0
 */
public final class SpelParserHolder {

    /**
     * The shared, thread-safe SpEL expression parser instance.
     */
    public static final ExpressionParser INSTANCE = new SpelExpressionParser();

    private SpelParserHolder() {
        // Utility class — no instantiation
    }
}
