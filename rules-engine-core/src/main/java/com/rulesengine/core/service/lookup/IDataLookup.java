package com.rulesengine.core.service.lookup;

import com.rulesengine.core.service.transform.Enricher;
import com.rulesengine.core.service.transform.Transformer;
import com.rulesengine.core.service.validation.Validator;

/**
 * Legacy interface that combines all capabilities.
 * Kept for backward compatibility.
 */
public interface IDataLookup extends Validator, Enricher, Transformer {
    // No additional methods needed as it inherits all from parent interfaces
}