package dev.mars.apex.core.engine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.mars.apex.core.service.transform.FieldTransformerAction;

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
 * Implementation of TransformerRule functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class TransformerRule<T> {
    private final Rule rule;
    private final List<FieldTransformerAction<T>> positiveActions;
    private final List<FieldTransformerAction<T>> negativeActions;
    private final Map<String, Object> additionalFacts;
    
    public TransformerRule(Rule rule, List<FieldTransformerAction<T>> positiveActions,
                           List<FieldTransformerAction<T>> negativeActions, Map<String, Object> additionalFacts) {
        this.rule = rule;
        this.positiveActions = positiveActions != null ? new ArrayList<>(positiveActions) : new ArrayList<>();
        this.negativeActions = negativeActions != null ? new ArrayList<>(negativeActions) : new ArrayList<>();
        this.additionalFacts = additionalFacts != null ? new HashMap<>(additionalFacts) : new HashMap<>();
    }
    
    public TransformerRule(Rule rule, List<FieldTransformerAction<T>> positiveActions,
                           List<FieldTransformerAction<T>> negativeActions) {
        this(rule, positiveActions, negativeActions, null);
    }
    
    public Rule getRule() {
        return rule;
    }
    
    public List<FieldTransformerAction<T>> getPositiveActions() {
        return positiveActions;
    }
    
    public List<FieldTransformerAction<T>> getNegativeActions() {
        return negativeActions;
    }
    
    public Map<String, Object> getAdditionalFacts() {
        return additionalFacts;
    }
}
