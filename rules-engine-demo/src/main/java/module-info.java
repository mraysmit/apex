module dev.mars.rulesengine.demo {
    // Core dependencies
    requires java.base;
    requires java.logging;

    // Rules engine dependencies
    requires dev.mars.rulesengine.core;
    requires spring.expression;
    requires spring.context;

    // Logging dependencies
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires jul.to.slf4j;

    // Export packages for Spring Expression Language access
    exports dev.mars.rulesengine.demo.model to spring.expression;
    exports dev.mars.rulesengine.demo.examples to spring.expression;
    exports dev.mars.rulesengine.demo.framework to spring.expression;

    // Legacy exports (to be removed in future versions)
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