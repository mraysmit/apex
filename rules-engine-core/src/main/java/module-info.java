module com.rulesengine.core {
    // Existing dependencies (may vary for your project)
    requires java.base;

    requires java.logging;
    requires spring.expression;

    exports com.rulesengine.core.service.validation;
    exports com.rulesengine.core.service.common;
    exports com.rulesengine.core.service.lookup;
    exports com.rulesengine.core.engine.model;
    exports com.rulesengine.core.engine.config;

}