package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.lookup.LookupServiceRegistry;

/**
 * Service for transformation operations.
 */
public class TransformationService {
    private final LookupServiceRegistry registry;
    
    public TransformationService(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    public Object transform(String transformerName, Object value) {
        Transformer transformer = registry.getService(transformerName, Transformer.class);
        return transformer != null ? transformer.transform(value) : value;
    }
}