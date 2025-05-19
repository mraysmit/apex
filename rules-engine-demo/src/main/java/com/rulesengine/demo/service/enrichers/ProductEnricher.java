package com.rulesengine.demo.service.enrichers;

import com.rulesengine.core.engine.RulesEngine;
import com.rulesengine.core.engine.model.RuleResult;
import com.rulesengine.core.service.transform.AbstractEnricher;
import com.rulesengine.core.service.transform.GenericEnricher;
import com.rulesengine.demo.model.Product;

public class ProductEnricher extends AbstractEnricher<Product> {
    private final GenericEnricher<Product> genericEnricher;
    private final ProductEnricherFactory factory;
    
    public ProductEnricher(String name, ProductEnricherFactory factory) {
        super(name, Product.class);
        this.factory = factory;
        this.genericEnricher = factory.createProductEnricher(name);
    }
    
    @Override
    public Product enrich(Product product) {
        return genericEnricher.enrich(product);
    }
    
    public Product enrich(Product product, Double customDiscount) {
        if (product == null) {
            return null;
        }
        
        GenericEnricher<Product> enricherWithCustomDiscount = 
            factory.createProductEnricherWithCustomDiscount(getName(), customDiscount);
        
        return enricherWithCustomDiscount.enrich(product);
    }
    
    public RuleResult enrichWithResult(Product product) {
        return genericEnricher.enrichWithResult(product);
    }
}