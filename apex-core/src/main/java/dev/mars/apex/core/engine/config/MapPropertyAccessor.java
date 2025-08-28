package dev.mars.apex.core.engine.config;

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


import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import java.util.Map;

/**
 * A custom PropertyAccessor that allows Map entries to be accessed as properties in SpEL expressions.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class MapPropertyAccessor implements PropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] { Map.class };
    }

    @Override
    public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            return map.containsKey(name);
        }
        return false;
    }

    @Override
    public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            Object value = map.get(name);
            return new TypedValue(value);
        }
        throw new AccessException("Cannot read property '" + name + "' from non-Map object");
    }

    @Override
    public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
        return target instanceof Map;
    }

    @Override
    public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
        if (target instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) target;
            map.put(name, newValue);
        } else {
            throw new AccessException("Cannot write property '" + name + "' to non-Map object");
        }
    }
}
