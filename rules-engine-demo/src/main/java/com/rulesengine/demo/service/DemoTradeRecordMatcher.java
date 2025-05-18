package com.rulesengine.demo.service;

import com.rulesengine.core.service.lookup.RecordMatcher;
import com.rulesengine.demo.model.Trade;
import com.rulesengine.core.service.lookup.LookupServiceRegistry;
import com.rulesengine.core.service.validation.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of RecordMatcher for Trade objects.
 */
public class DemoTradeRecordMatcher implements RecordMatcher<Trade> {
    private final LookupServiceRegistry registry;
    
    public DemoTradeRecordMatcher(LookupServiceRegistry registry) {
        this.registry = registry;
    }
    
    @Override
    public List<Trade> findMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> matchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (hasMatch(trade, validatorNames)) {
                matchingTrades.add(trade);
            }
        }
        return matchingTrades;
    }
    
    @Override
    public List<Trade> findNonMatchingRecords(List<Trade> sourceTrades, List<String> validatorNames) {
        List<Trade> nonMatchingTrades = new ArrayList<>();
        for (Trade trade : sourceTrades) {
            if (!hasMatch(trade, validatorNames)) {
                nonMatchingTrades.add(trade);
            }
        }
        return nonMatchingTrades;
    }
    
    private boolean hasMatch(Trade trade, List<String> validatorNames) {
        for (String validatorName : validatorNames) {
            Validator validator = registry.getService(validatorName, Validator.class);
            if (validator != null && validator.validate(trade.getValue())) {
                return true;
            }
        }
        return false;
    }
}