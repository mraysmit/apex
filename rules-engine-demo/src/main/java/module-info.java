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
    requires jdk.compiler;

    // Export packages for Spring Expression Language access
    exports dev.mars.rulesengine.demo.model to spring.expression;

    // Export main demo packages for external access
    exports dev.mars.rulesengine.demo;
    exports dev.mars.rulesengine.demo.rulesets;
    exports dev.mars.rulesengine.demo.data to spring.expression;
    exports dev.mars.rulesengine.demo.examples;
    exports dev.mars.rulesengine.demo.core;
    exports dev.mars.rulesengine.demo.advanced;
}