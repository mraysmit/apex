package dev.mars.rulesengine.core.service.lookup;

import dev.mars.rulesengine.core.service.common.NamedService;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for lookup services.
 * Responsible only for registration and retrieval of services.
 */
public class LookupServiceRegistry {
    private final Map<String, NamedService> services = new HashMap<>();
    
    public void registerService(NamedService service) {
        services.put(service.getName(), service);
    }
    
    public <T extends NamedService> T getService(String name, Class<T> type) {
        NamedService service = services.get(name);
        if (service != null && type.isInstance(service)) {
            return type.cast(service);
        }
        return null;
    }
}