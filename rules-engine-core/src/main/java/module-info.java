module com.rulesengine.core {
    // Existing dependencies (may vary for your project)
    requires java.base;

    requires java.logging;
    requires spring.expression;

    exports dev.mars.rulesengine.core.service.validation;
    exports dev.mars.rulesengine.core.service.common;
    exports dev.mars.rulesengine.core.service.lookup;
    exports dev.mars.rulesengine.core.engine.model;
    exports dev.mars.rulesengine.core.engine.config;

}