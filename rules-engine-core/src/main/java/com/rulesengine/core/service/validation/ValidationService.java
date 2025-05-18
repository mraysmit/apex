package com.rulesengine.core.service.validation;

import com.rulesengine.core.service.lookup.LookupServiceRegistry;

/**
 * Service for validation operations.
 */
public class ValidationService {
    private final LookupServiceRegistry registry;
    
    public ValidationService(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    public boolean validate(String validatorName, Object value) {
        Validator validator = registry.getService(validatorName, Validator.class);
        return validator != null && validator.validate(value);
    }
}