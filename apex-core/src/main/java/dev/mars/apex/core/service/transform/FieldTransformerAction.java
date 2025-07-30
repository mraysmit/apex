package dev.mars.apex.core.service.transform;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

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
 * Implementation of FieldTransformerAction functionality.
 *
 * This class is part of the PeeGeeQ message queue system, providing
 * production-ready PostgreSQL-based message queuing capabilities.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class FieldTransformerAction<T> {
    private final String fieldName;
    private final Function<T, Object> fieldValueExtractor;
    private final BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer;
    private final BiConsumer<T, Object> fieldValueSetter;
    
    public FieldTransformerAction(String fieldName, Function<T, Object> fieldValueExtractor,
                                  BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer,
                                  BiConsumer<T, Object> fieldValueSetter) {
        this.fieldName = fieldName;
        this.fieldValueExtractor = fieldValueExtractor;
        this.fieldValueTransformer = fieldValueTransformer;
        this.fieldValueSetter = fieldValueSetter;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public Function<T, Object> getFieldValueExtractor() {
        return fieldValueExtractor;
    }
    
    public BiFunction<Object, Map<String, Object>, Object> getFieldValueTransformer() {
        return fieldValueTransformer;
    }
    
    public BiConsumer<T, Object> getFieldValueSetter() {
        return fieldValueSetter;
    }
}
