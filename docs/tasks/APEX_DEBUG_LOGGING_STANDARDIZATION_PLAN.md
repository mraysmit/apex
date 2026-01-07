# Standardizing Debug Logging

The goal is to address the "non consistent debug logging" issue by standardizing logging practices in `RuleEngineService` and `RuleGroup`. Currently, there is a mix of SLF4J logging and `System.out/err` printing, which makes debugging and monitoring difficult.

## User Review Required
> [!IMPORTANT]
> This change will remove `System.out` and `System.err` output from `RuleEngineService` and `RuleGroup`. If there are specific test runners or scripts scraping stdout/stderr for these specific messages, they might break. The tests within the project will be checked, but external dependencies are unknown.

## Proposed Changes

### `apex-core`

#### [MODIFY] [RulesEngineLogger.java](file:///c:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/main/java/dev/mars/apex/core/util/RulesEngineLogger.java)
- Add `debug(String message, Throwable t)` method to explicitly handle exception logging with stack traces.

#### [MODIFY] [RuleEngineService.java](file:///c:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/main/java/dev/mars/apex/core/service/engine/RuleEngineService.java)
- Replace `private static final Logger logger` with `private final RulesEngineLogger logger`.
- Initialize `RulesEngineLogger` in the constructor or field.
- Replace all `System.out.println` and `System.err.println` calls with appropriate `logger` methods (e.g., `ruleEvaluationComplete`, `ruleEvaluationError`, `info`, `debug`).
- Use `RulesEngineLogger`'s specialized methods like `ruleEvaluationStart` where applicable.

#### [MODIFY] [RuleGroup.java](file:///c:/Users/mraysmit/dev/idea-projects/apex-rules-engine/apex-core/src/main/java/dev/mars/apex/core/engine/model/RuleGroup.java)
- Add `RulesEngineLogger` field.
- Replace `System.out.println("DEBUG: ...")` with `logger.debug(...)`.
- Replace `System.err.println(...)` with `logger.error(..., e)` ensuring the exception is passed for stack trace visibility.
- Ensure consistent log message formatting.

## Verification Plan

### Automated Tests
- Run `RuleEngineServiceTest` to ensure no regressions in logic.
- Run `ClasspathRuleGroupProcessingTest` and `ComprehensiveYamlValidationTest` as they likely use `RuleGroup`.

### Manual Verification
- Since I cannot easily run "manual" manual verification, I will rely on existing tests passing.
- I will verify that `System.out` usages are gone from the modified files by reading them after modification.
