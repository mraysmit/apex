package com.rulesengine.core.service.lookup;

import com.rulesengine.core.service.common.NamedService;
import com.rulesengine.core.service.validation.Validator;

/**
 * Legacy interface that combines validation and transformation capabilities.
 * Kept for backward compatibility.
 */
public interface IDataLookup extends Validator, NamedService {
    /**
     * Transform a value of type Object.
     * 
     * @param value The value to transform
     * @return The transformed value
     */
    Object transform(Object value);

    /**
     * Enrich a value of type Object.
     * 
     * @param value The value to enrich
     * @return The enriched value
     * @deprecated Use {@link #transform(Object)} instead.
     */
    @Deprecated
    default Object enrich(Object value) {
        return transform(value);
    }

    /**
     * Get the type of objects this lookup can handle.
     * 
     * @return The class of objects this lookup can handle
     */
    @Override
    default Class<Object> getType() {
        return Object.class;
    }
}
