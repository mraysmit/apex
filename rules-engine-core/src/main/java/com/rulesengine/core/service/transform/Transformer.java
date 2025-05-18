package com.rulesengine.core.service.transform;

import com.rulesengine.core.service.NamedService;

/**
 * Interface for transformation services.
 */
public interface Transformer extends NamedService {
    Object transform(Object value);
}