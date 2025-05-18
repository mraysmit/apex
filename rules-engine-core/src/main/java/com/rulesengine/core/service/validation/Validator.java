package com.rulesengine.core.service.validation;

import com.rulesengine.core.service.common.NamedService;

/**
 * Interface for validation services.
 * @param <T> The type of object this validator can validate
 */
public interface Validator<T> extends NamedService {
    /**
     * Validate a value of type T.
     * 
     * @param value The value to validate
     * @return True if the value is valid, false otherwise
     */
    boolean validate(T value);

    /**
     * Get the type of objects this validator can validate.
     * 
     * @return The class of objects this validator can validate
     */
    Class<T> getType();
}
