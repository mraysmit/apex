package dev.mars.apex.core.service.data.external.config;

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
 * Observer pattern interface that enables components to receive notifications about
 * changes in the data source configuration. Listener interface for data source configuration events.
 * 
 * Implementations of this interface can be registered with the DataSourceConfigurationService
 * to receive notifications about configuration lifecycle events and health changes.
 *
 * Enables reactive programming patterns where components can respond to
 * configuration changes without tight coupling to the configuration service itself.
 *
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public interface DataSourceConfigurationListener {
    
    /**
     * Called when a data source configuration event occurs.
     * 
     * @param event The configuration event
     */
    void onConfigurationEvent(DataSourceConfigurationEvent event);
    
    /**
     * Called when the configuration service is initialized.
     * 
     * Default implementation delegates to onConfigurationEvent.
     * 
     * @param event The initialization event
     */
    default void onServiceInitialized(DataSourceConfigurationEvent event) {
        onConfigurationEvent(event);
    }
    
    /**
     * Called when a configuration is added.
     * 
     * Default implementation delegates to onConfigurationEvent.
     * 
     * @param event The configuration added event
     */
    default void onConfigurationAdded(DataSourceConfigurationEvent event) {
        onConfigurationEvent(event);
    }
    
    /**
     * Called when a configuration is removed.
     * 
     * Default implementation delegates to onConfigurationEvent.
     * 
     * @param event The configuration removed event
     */
    default void onConfigurationRemoved(DataSourceConfigurationEvent event) {
        onConfigurationEvent(event);
    }
    
    /**
     * Called when a configuration is updated.
     * 
     * Default implementation delegates to onConfigurationEvent.
     * 
     * @param event The configuration updated event
     */
    default void onConfigurationUpdated(DataSourceConfigurationEvent event) {
        onConfigurationEvent(event);
    }
    
    /**
     * Called when a data source health is restored.
     * 
     * Default implementation delegates to onConfigurationEvent.
     * 
     * @param event The health restored event
     */
    default void onHealthRestored(DataSourceConfigurationEvent event) {
        onConfigurationEvent(event);
    }
    
    /**
     * Called when a data source health is lost.
     * 
     * Default implementation delegates to onConfigurationEvent.
     * 
     * @param event The health lost event
     */
    default void onHealthLost(DataSourceConfigurationEvent event) {
        onConfigurationEvent(event);
    }
    
    /**
     * Called when configurations are reloaded.
     * 
     * Default implementation delegates to onConfigurationEvent.
     * 
     * @param event The reloaded event
     */
    default void onConfigurationsReloaded(DataSourceConfigurationEvent event) {
        onConfigurationEvent(event);
    }
}
