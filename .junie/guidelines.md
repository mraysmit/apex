Follow SOLID principles with clear separation of concerns and dependency injection.

All business logic implemented in this project should use only the RulesEngine, Rules and RuleGroup. This is including validation, enrichment, lookup, transform, etc. as implemented in the rules-engine-demo.

The validator should be using the RulesEngine functionality to perform validation. Please remove the hard-coded validation and replace with the correct rules engine functionality. 

All Validators will use Rules and RuleGroups to define the validation and enrichment and all other services such as enrichment should do the same. 

All services should exclusively be using the RulesEngine, RuleConfigurationService and all other features of the rules-engine-code module to define business logic associated with their functions.  

Also RuleResult should only be used as the return type and there should be a simple evaluation that returns only a boolean for simplicity. Tests should follow these patterns exclusively.

Maintain a clear separation between the the modules rules-engine-core and rules-engine-demo. 

The rules-engine-core should only contain the core functionality of the rules engine and the rules-engine-demo should contain the demo application and any additional functionality that is needed to support the demo.

Use clearly segregated classes with no static methods. All classes should be instantiated and injected into the classes that need them.

Ensure that the design and style of the exsting code is followed.

Demo or Examples classes as used in this project in many places should not follow the same design as the rest of the code. They should be simple and easy to understand and should not follow the same design patterns as the rest of the code.

They should not have public method and only provide a simple example of how to use the class. They should not be used as a reference for how to implement the class in production code.

Demo or example classes can have a companion Config class to provide configuration or test data into the demo. 

Use as exmaples, the classes in rules-engine-demo such as the ProductTransformerDemo and ProductTransformerConfig and CustomerTransformerDemo and CustomerTransformerConfig.