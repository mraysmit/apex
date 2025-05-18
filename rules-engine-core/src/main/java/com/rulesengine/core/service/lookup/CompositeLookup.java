package com.rulesengine.core.service.lookup;

import com.rulesengine.core.service.transform.Enricher;
import com.rulesengine.core.service.transform.Transformer;
import com.rulesengine.core.service.validation.Validator;

/**
 * Implementation that combines validation, enrichment, and transformation.
 */
public class CompositeLookup implements IDataLookup {
    private String name;
    private Validator validator;
    private Enricher enricher;
    private Transformer transformer;
    
    public CompositeLookup(String name) {
        this.name = name;
    }
    
    public CompositeLookup withValidator(Validator validator) {
        this.validator = validator;
        return this;
    }
    
    public CompositeLookup withEnricher(Enricher enricher) {
        this.enricher = enricher;
        return this;
    }
    
    public CompositeLookup withTransformer(Transformer transformer) {
        this.transformer = transformer;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean validate(Object value) {
        return validator != null ? validator.validate(value) : true;
    }
    
    @Override
    public Object enrich(Object value) {
        return enricher != null ? enricher.enrich(value) : value;
    }
    
    @Override
    public Object transform(Object value) {
        return transformer != null ? transformer.transform(value) : value;
    }
}