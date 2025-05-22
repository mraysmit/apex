This project comprises 2 modules: rules-engine-core and rules-engine-demo.

rules-engine-core contains the core functionality of the rules engine and should not contain any demo or example code. It should only contain the core functionality of the rules engine.

rules-engine-demo contains the demo application and any additional functionality that is needed to support the demo. It should not contain any core functionality of the rules engine.

Maintain a clear separation between the modules rules-engine-core and rules-engine-demo.

Follow SOLID principles with clear separation of concerns and dependency injection.

All business logic implemented in this project should use only the RulesEngine, Rules and RuleGroup. This is including validation, enrichment, lookup, transform, etc. as implemented in the rules-engine-demo.

All Validators will use Rules and RuleGroups to define the validation and enrichment and all other services such as enrichment should do the same. 

All services should exclusively be using the RulesEngine, RuleConfigurationService and all other features of the rules-engine-code module to define business logic associated with their functions.  

Also RuleResult should only be used as the return type and there should be a simple evaluation that returns only a boolean for simplicity. Tests should follow these patterns exclusively.

In the rules-engine-code module use clearly segregated classes with no static methods. All classes should be instantiated and injected into the classes that need them.

In the rules-engine-demo module, use static methods and classes where appropriate to simplify the code and make it easier to understand. 

In the rules-engine-demo module try to keep the exmaples as self-contained as possible. This means that the examples should not depend on any external classes or libraries apart from the rules-engine-core module.

