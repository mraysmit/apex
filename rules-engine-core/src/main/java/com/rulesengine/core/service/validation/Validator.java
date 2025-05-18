package com.rulesengine.core.service.validation;

import com.rulesengine.core.service.NamedService;

/**
 * Interface for validation services.
 */
public interface Validator extends NamedService {
    boolean validate(Object value);
}