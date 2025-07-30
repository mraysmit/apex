package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.service.common.NamedService;
import dev.mars.apex.core.service.validation.Validator;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Legacy interface that combines validation and transformation capabilities.
 *
 * This interface is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
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
