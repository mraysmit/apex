package dev.mars.apex.rest.config;

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
