module dev.mars.rulesengine.demo {
    // Existing dependencies
    requires java.base;
    requires java.logging;

    // Add missing dependencies
    requires dev.mars.rulesengine.core;
    requires spring.expression;
}