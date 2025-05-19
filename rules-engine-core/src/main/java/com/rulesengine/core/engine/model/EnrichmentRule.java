package com.rulesengine.core.engine.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.rulesengine.core.service.transform.FieldEnrichmentAction;

public class EnrichmentRule<T> {
    private final Rule rule;
    private final List<FieldEnrichmentAction<T>> positiveActions;
    private final List<FieldEnrichmentAction<T>> negativeActions;
    private final Map<String, Object> additionalFacts;
    
    public EnrichmentRule(Rule rule, List<FieldEnrichmentAction<T>> positiveActions, 
                         List<FieldEnrichmentAction<T>> negativeActions, Map<String, Object> additionalFacts) {
        this.rule = rule;
        this.positiveActions = positiveActions != null ? new ArrayList<>(positiveActions) : new ArrayList<>();
        this.negativeActions = negativeActions != null ? new ArrayList<>(negativeActions) : new ArrayList<>();
        this.additionalFacts = additionalFacts != null ? new HashMap<>(additionalFacts) : new HashMap<>();
    }
    
    public EnrichmentRule(Rule rule, List<FieldEnrichmentAction<T>> positiveActions, 
                         List<FieldEnrichmentAction<T>> negativeActions) {
        this(rule, positiveActions, negativeActions, null);
    }
    
    public Rule getRule() {
        return rule;
    }
    
    public List<FieldEnrichmentAction<T>> getPositiveActions() {
        return positiveActions;
    }
    
    public List<FieldEnrichmentAction<T>> getNegativeActions() {
        return negativeActions;
    }
    
    public Map<String, Object> getAdditionalFacts() {
        return additionalFacts;
    }
}