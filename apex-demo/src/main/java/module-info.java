module dev.mars.apex.demo {
    // Core dependencies
    requires java.base;
    requires java.logging;

    // APEX dependencies
    requires dev.mars.apex.core;
    requires spring.expression;
    requires spring.context;

    // Logging dependencies
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires jul.to.slf4j;
    requires jdk.compiler;

    // Export packages for Spring Expression Language access
    exports dev.mars.apex.demo.model to spring.expression;

    // Export main demo packages for external access
    exports dev.mars.apex.demo;
    exports dev.mars.apex.demo.core;
    exports dev.mars.apex.demo.rulesets;
    exports dev.mars.apex.demo.data to spring.expression;
    exports dev.mars.apex.demo.examples;
    exports dev.mars.apex.demo.advanced;
}
