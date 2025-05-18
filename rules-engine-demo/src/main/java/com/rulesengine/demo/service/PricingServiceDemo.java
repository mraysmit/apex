package com.rulesengine.demo.service;

/**
 * Service for calculating different pricing strategies for financial instruments.
 */
public class PricingServiceDemo {
    public double calculateStandardPrice(double basePrice) {
        return basePrice;
    }

    public double calculatePremiumPrice(double basePrice) {
        return basePrice * 1.2; // 20% premium
    }

    public double calculateSalePrice(double basePrice) {
        return basePrice * 0.8; // 20% discount
    }

    public double calculateClearancePrice(double basePrice) {
        return basePrice * 0.5; // 50% discount
    }
}