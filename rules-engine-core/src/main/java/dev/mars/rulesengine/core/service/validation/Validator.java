package dev.mars.rulesengine.core.service.validation;

import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.common.NamedService;

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
 * Interface for validation services.
 *
 * This interface is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Interface for validation services.
 * @param <T> The type of object this validation can validate
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
     * Validate a value of type T and return a detailed result.
     * 
     * @param value The value to validate
     * @return A RuleResult containing the validation outcome
     */
    default RuleResult validateWithResult(T value) {
        boolean isValid = validate(value);
        if (isValid) {
            return RuleResult.match(getName(), "Validation successful for " + getName());
        } else {
            return RuleResult.noMatch();
        }
    }

    /**
     * Get the type of objects this validation can validate.
     * 
     * @return The class of objects this validation can validate
     */
    Class<T> getType();
}
