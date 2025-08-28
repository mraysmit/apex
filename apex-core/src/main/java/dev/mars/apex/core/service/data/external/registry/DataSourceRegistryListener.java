package dev.mars.apex.core.service.data.external.registry;

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
 * Listener interface for data source registry events.
 * 
 * Implementations of this interface can be registered with the DataSourceRegistry
 * to receive notifications about data source lifecycle events and health changes.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public interface DataSourceRegistryListener {
    
    /**
     * Called when a data source registry event occurs.
     * 
     * @param event The registry event
     */
    void onDataSourceEvent(DataSourceRegistryEvent event);
    
    /**
     * Called when a data source is registered.
     * 
     * Default implementation delegates to onDataSourceEvent.
     * 
     * @param event The registration event
     */
    default void onDataSourceRegistered(DataSourceRegistryEvent event) {
        onDataSourceEvent(event);
    }
    
    /**
     * Called when a data source is unregistered.
     * 
     * Default implementation delegates to onDataSourceEvent.
     * 
     * @param event The unregistration event
     */
    default void onDataSourceUnregistered(DataSourceRegistryEvent event) {
        onDataSourceEvent(event);
    }
    
    /**
     * Called when a data source health is restored.
     * 
     * Default implementation delegates to onDataSourceEvent.
     * 
     * @param event The health restored event
     */
    default void onDataSourceHealthRestored(DataSourceRegistryEvent event) {
        onDataSourceEvent(event);
    }
    
    /**
     * Called when a data source health is lost.
     * 
     * Default implementation delegates to onDataSourceEvent.
     * 
     * @param event The health lost event
     */
    default void onDataSourceHealthLost(DataSourceRegistryEvent event) {
        onDataSourceEvent(event);
    }
}
