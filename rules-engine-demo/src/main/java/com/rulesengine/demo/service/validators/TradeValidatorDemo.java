/**
 * A validator for Trade objects.
 * This validator checks if a trade meets certain criteria.
 */
package com.rulesengine.demo.service.validators;

import com.rulesengine.core.service.validation.Validator;
import com.rulesengine.demo.model.Trade;

import java.util.Arrays;
import java.util.List;

public class TradeValidatorDemo implements Validator<Trade> {
    private final String name;
    private final List<String> allowedValues;
    private final List<String> allowedCategories;

    /**
     * Create a new TradeValidatorDemo with the specified criteria.
     *
     * @param name The name of the validator
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public TradeValidatorDemo(String name, List<String> allowedValues, List<String> allowedCategories) {
        this.name = name;
        this.allowedValues = allowedValues;
        this.allowedCategories = allowedCategories;
    }

    /**
     * Create a new TradeValidatorDemo with the specified criteria.
     *
     * @param name The name of the validator
     * @param allowedValues The allowed values for a valid trade
     * @param allowedCategories The allowed categories for a valid trade
     */
    public TradeValidatorDemo(String name, String[] allowedValues, String[] allowedCategories) {
        this.name = name;
        this.allowedValues = Arrays.asList(allowedValues);
        this.allowedCategories = Arrays.asList(allowedCategories);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean validate(Trade trade) {
        if (trade == null) {
            return false;
        }

        // Check value if allowed values are specified
        if (!allowedValues.isEmpty() && !allowedValues.contains(trade.getValue())) {
            return false;
        }

        // Check category if allowed categories are specified
        if (!allowedCategories.isEmpty() && !allowedCategories.contains(trade.getCategory())) {
            return false;
        }

        return true;
    }

    @Override
    public Class<Trade> getType() {
        return Trade.class;
    }
}
