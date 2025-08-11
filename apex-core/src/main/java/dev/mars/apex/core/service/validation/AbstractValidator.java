package dev.mars.apex.core.service.validation;

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
 * Abstract base class for validators.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
/**
 * Abstract base class for validators.
 * Provides common functionality for all validators.
 * 
 * @param <T> The type of object this validation can validate
 */
public abstract class AbstractValidator<T> implements Validator<T> {
    private final String name;
    private final Class<T> type;

    /**
     * Create a new AbstractValidator with the specified name and type.
     * 
     * @param name The name of the validation
     * @param type The class of objects this validation can validate
     */
    protected AbstractValidator(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the type of objects this validation can validate.
     * 
     * @return The class of objects this validation can validate
     */
    public Class<T> getType() {
        return type;
    }

}
