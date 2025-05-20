package com.rulesengine.core.engine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.rulesengine.core.service.transform.FieldTransformerAction;

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