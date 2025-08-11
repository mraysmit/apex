package dev.mars.apex.core.service.data.external.manager;

/**
 * Listener interface for data source manager events.
 * 
 * Implementations of this interface can be registered with the DataSourceManager
 * to receive notifications about manager lifecycle events and data source changes.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0.0
 * @version 1.0
 */
public interface DataSourceManagerListener {
    
    /**
     * Called when a data source manager event occurs.
     * 
     * @param event The manager event
     */
    void onManagerEvent(DataSourceManagerEvent event);
    
    /**
     * Called when the manager is initialized.
     * 
     * Default implementation delegates to onManagerEvent.
     * 
     * @param event The initialization event
     */
    default void onManagerInitialized(DataSourceManagerEvent event) {
        onManagerEvent(event);
    }
    
    /**
     * Called when the manager is shut down.
     * 
     * Default implementation delegates to onManagerEvent.
     * 
     * @param event The shutdown event
     */
    default void onManagerShutdown(DataSourceManagerEvent event) {
        onManagerEvent(event);
    }
    
    /**
     * Called when a data source is added.
     * 
     * Default implementation delegates to onManagerEvent.
     * 
     * @param event The data source added event
     */
    default void onDataSourceAdded(DataSourceManagerEvent event) {
        onManagerEvent(event);
    }
    
    /**
     * Called when a data source is removed.
     * 
     * Default implementation delegates to onManagerEvent.
     * 
     * @param event The data source removed event
     */
    default void onDataSourceRemoved(DataSourceManagerEvent event) {
        onManagerEvent(event);
    }
    
    /**
     * Called when a data source health is restored.
     * 
     * Default implementation delegates to onManagerEvent.
     * 
     * @param event The health restored event
     */
    default void onHealthRestored(DataSourceManagerEvent event) {
        onManagerEvent(event);
    }
    
    /**
     * Called when a data source health is lost.
     * 
     * Default implementation delegates to onManagerEvent.
     * 
     * @param event The health lost event
     */
    default void onHealthLost(DataSourceManagerEvent event) {
        onManagerEvent(event);
    }
    
    /**
     * Called when a refresh operation is completed.
     * 
     * Default implementation delegates to onManagerEvent.
     * 
     * @param event The refresh completed event
     */
    default void onRefreshCompleted(DataSourceManagerEvent event) {
        onManagerEvent(event);
    }
}
