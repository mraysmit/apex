package dev.mars.apex.core.service.lookup;

import dev.mars.apex.core.service.NamedService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
 * Registry for lookup services.
 *
* This class is part of the APEX A powerful expression processor for Java applications.
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-07-27
 * @version 1.0
 */
public class LookupServiceRegistry {
    private final Map<String, NamedService> services = new ConcurrentHashMap<>();
    
    public void registerService(NamedService service) {
        NamedService validatedService = Objects.requireNonNull(service, "service");
        services.put(Objects.requireNonNull(validatedService.getName(), "service name"), validatedService);
    }
    
    public <T extends NamedService> T getService(String name, Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (name == null) {
            return null;
        }
        NamedService service = services.get(name);
        if (service != null && type.isInstance(service)) {
            return type.cast(service);
        }
        return null;
    }

    /**
     * Get the names of all registered services of a specific type.
     *
     * @param type The type of services to get names for
     * @return Array of service names
     */
    public <T extends NamedService> String[] getServiceNames(Class<T> type) {
        if (type == null) {
            return new String[0];
        }
        return services.entrySet().stream()
            .filter(entry -> type.isInstance(entry.getValue()))
            .map(Map.Entry::getKey)
            .sorted()
            .toArray(String[]::new);
    }

    public Map<String, NamedService> getRegisteredServices() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(services));
    }
}
