package dev.mars.apex.rest.config;

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
