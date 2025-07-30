package dev.mars.apex.core.engine.config;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

import java.util.Map;

/**
 * A custom PropertyAccessor that allows Map entries to be accessed as properties in SpEL expressions.
 * This enables expressions like "age > 18" instead of requiring "['age'] > 18".
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
