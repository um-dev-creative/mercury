---
name: PMD
description: Tool for static analysis enforcement in Mercury — runs automatically at mvn clean test phase
type: terminal
command-prefix: mvn
used-by: [developer, code-reviewer, devops-engineer]
---

## Purpose

PMD performs static analysis on all Mercury Java source files. It runs automatically at the Maven `test` phase and fails the build on any violation defined in `ruleset.xml`. It does not need to be invoked separately.

## Available Commands

### Run PMD via Maven test phase
```bash
mvn clean test
```
PMD runs before Surefire. If violations exist, the build fails with `BUILD FAILURE` and lists the violations.

### Run PMD only (skip tests)
```bash
mvn pmd:check
```
Runs PMD analysis and checks for violations without executing tests.

### Generate PMD HTML report
```bash
mvn pmd:pmd
```
Generates `target/site/pmd.html` and `target/pmd.xml`.

## Active Rules (from ruleset.xml)

Key rules that most commonly cause build failures:

| Category | Rule | Description |
|---|---|---|
| codestyle | `AtLeastOneConstructor` | Every class must have an explicit constructor |
| bestpractices | `UnusedPrivateField` | Private fields that are never read |
| bestpractices | `UnusedPrivateMethod` | Private methods that are never called |
| bestpractices | `UnusedLocalVariable` | Local variables that are assigned but never used |
| bestpractices | `UnusedFormalParameter` | Method parameters that are never used |
| bestpractices | `PreserveStackTrace` | Exception cause must be passed to new exception |
| bestpractices | `UseCollectionIsEmpty` | Use `isEmpty()` instead of `size() == 0` |
| bestpractices | `AvoidUsingHardCodedIP` | No hardcoded IP addresses |
| codestyle | `ConfusingTernary` | Avoid negated conditions in ternary expressions |
| bestpractices | `ConstantsInInterface` | Interfaces should not contain constants |
| bestpractices | `OneDeclarationPerLine` | One variable declaration per line |

## Output Locations

- PMD violations in build log: `target/pmd.xml`
- HTML report: `target/site/pmd.html` (after `mvn pmd:pmd`)

## Notes

- PMD runs at the `test` phase — it executes before JUnit tests
- Zero tolerance policy: any violation fails the build
- Ruleset file: `ruleset.xml` at project root — do not modify without review
- The `GlobalExceptionHandler` uses an explicit no-arg constructor solely to satisfy `AtLeastOneConstructor`
