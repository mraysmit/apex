package dev.mars.apex.core.service.data.external.registry;

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
