package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.service.transform.GenericTransformer;
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
 * Implementation that combines validation and transformation.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class CompositeLookup implements IDataLookup {
    private String name;
    private Validator<Object> validator;
    private GenericTransformer<Object> transformer;

    public CompositeLookup(String name) {
        this.name = name;
    }

    public CompositeLookup withValidator(Validator<Object> validator) {
        this.validator = validator;
        return this;
    }

    public CompositeLookup withTransformer(GenericTransformer<Object> transformer) {
        this.transformer = transformer;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Object value) {
        return validator != null ? validator.validate(value) : true;
    }

    /**
     * @deprecated Use {@link #transform(Object)} instead.
     */
    @Deprecated
    @Override
    public Object enrich(Object value) {
        // Delegate to transform method
        return transform(value);
    }

    @Override
    public Object transform(Object value) {
        return transformer != null ? transformer.transform(value) : value;
    }

    @Override
    public Class<Object> getType() {
        return Object.class;
    }
}
