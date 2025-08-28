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
 * Configuration properties for timeout settings.
 * 
 * This class binds to the 'timeouts' prefix in application configuration files
 * and provides type-safe access to timeout configuration properties.
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-10
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "timeouts")
public class TimeoutsConfigurationProperties {
    
    private int ruleExecution = 5000;
    private int templateProcessing = 3000;
    private int expressionEvaluation = 2000;
    
    public int getRuleExecution() {
        return ruleExecution;
    }
    
    public void setRuleExecution(int ruleExecution) {
        this.ruleExecution = ruleExecution;
    }
    
    public int getTemplateProcessing() {
        return templateProcessing;
    }
    
    public void setTemplateProcessing(int templateProcessing) {
        this.templateProcessing = templateProcessing;
    }
    
    public int getExpressionEvaluation() {
        return expressionEvaluation;
    }
    
    public void setExpressionEvaluation(int expressionEvaluation) {
        this.expressionEvaluation = expressionEvaluation;
    }
}
