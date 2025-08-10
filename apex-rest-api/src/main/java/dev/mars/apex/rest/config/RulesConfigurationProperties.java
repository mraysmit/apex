package dev.mars.apex.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the APEX Rules Engine.
 * 
 * This class binds to the 'rules' prefix in application configuration files
 * and provides type-safe access to rules engine configuration properties.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-10
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "rules")
public class RulesConfigurationProperties {
    
    private Config config = new Config();
    private Performance performance = new Performance();
    private Error error = new Error();
    private Cache cache = new Cache();
    
    // Getters and setters
    public Config getConfig() {
        return config;
    }
    
    public void setConfig(Config config) {
        this.config = config;
    }
    
    public Performance getPerformance() {
        return performance;
    }
    
    public void setPerformance(Performance performance) {
        this.performance = performance;
    }
    
    public Error getError() {
        return error;
    }
    
    public void setError(Error error) {
        this.error = error;
    }
    
    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }


    
    /**
     * Configuration section for rules config settings.
     */
    public static class Config {
        private String path;
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
    }
    
    /**
     * Configuration section for performance settings.
     */
    public static class Performance {
        private Monitoring monitoring = new Monitoring();
        
        public Monitoring getMonitoring() {
            return monitoring;
        }
        
        public void setMonitoring(Monitoring monitoring) {
            this.monitoring = monitoring;
        }
        
        public static class Monitoring {
            private boolean enabled = true;
            
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }
    
    /**
     * Configuration section for error handling settings.
     */
    public static class Error {
        private Recovery recovery = new Recovery();
        
        public Recovery getRecovery() {
            return recovery;
        }
        
        public void setRecovery(Recovery recovery) {
            this.recovery = recovery;
        }
        
        public static class Recovery {
            private boolean enabled = true;
            
            public boolean isEnabled() {
                return enabled;
            }
            
            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }
    
    /**
     * Configuration section for cache settings.
     */
    public static class Cache {
        private boolean enabled = true;
        private int ttlSeconds = 3600;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getTtlSeconds() {
            return ttlSeconds;
        }
        
        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }


}
