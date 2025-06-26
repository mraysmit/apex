module dev.mars.rulesengine.demo {
    // Existing dependencies
    requires java.base;
    requires java.logging;

    // Add missing dependencies
    requires dev.mars.rulesengine.core;
    requires spring.expression;
    requires spring.context;

    // Logging dependencies
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires jul.to.slf4j;

    // Export existing model packages so Spring Expression Language can access them
    exports dev.mars.rulesengine.demo.model to spring.expression;
    exports dev.mars.rulesengine.demo.service.providers to spring.expression;
    exports dev.mars.rulesengine.demo.service.transformers to spring.expression;
    exports dev.mars.rulesengine.demo.service.validators to spring.expression;
    exports dev.mars.rulesengine.demo.integration to spring.expression;

    // Export new financial model packages
    exports dev.mars.rulesengine.demo.examples.financial.model to spring.expression;
    exports dev.mars.rulesengine.demo.datasets to spring.expression;

    // Export main demo packages for external access
    exports dev.mars.rulesengine.demo;
    exports dev.mars.rulesengine.demo.examples.financial;
    exports dev.mars.rulesengine.demo.rulesets;
    exports dev.mars.rulesengine.demo.showcase;
    exports dev.mars.rulesengine.demo.simplified;
}