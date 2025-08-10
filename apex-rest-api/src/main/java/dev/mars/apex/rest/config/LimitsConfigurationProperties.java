package dev.mars.apex.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for limit settings.
 * 
 * This class binds to the 'limits' prefix in application configuration files
 * and provides type-safe access to limit configuration properties.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-10
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "limits")
public class LimitsConfigurationProperties {
    
    private int maxBatchSize = 100;
    private int maxExpressionLength = 1000;
    
    public int getMaxBatchSize() {
        return maxBatchSize;
    }
    
    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }
    
    public int getMaxExpressionLength() {
        return maxExpressionLength;
    }
    
    public void setMaxExpressionLength(int maxExpressionLength) {
        this.maxExpressionLength = maxExpressionLength;
    }
}
