Follow SOLID principles with clear separation of concerns and dependency injection.

All business logic implemented in this project should use only the RulesEngine, Rules and RuleGroup. This is including validation, enrichment, lookup, transform, etc. as implemented in the rules-engine-demo.

The validator should be using the RulesEngine functionality to perform validation. Please remove the hard-coded validation and replace with the correct rules engine functionality. 

All Validators will use Rules and RuleGroups to define the validation and enrichment and all other services such as enrichment should do the same. Some services are already exclusively using the RulesEngine to define business logic associated with their functions. Other services are using the RulesEngine. 

Also RuleResult should be used as the return type and there should be a simple evaluation that returns only a boolean for simplicity. Tests should follow these patterns exclusively.

Maintain a clear separation between the the modules rules-engine-core and rules-engine-demo. 

The rules-engine-core should only contain the core functionality of the rules engine and the rules-engine-demo should contain the demo application and any additional functionality that is needed to support the demo.

Use clearly segregated classes with no static methods. All classes should be instantiated and injected into the classes that need them.

Ensure that the design and style of the exsting code is followed.

