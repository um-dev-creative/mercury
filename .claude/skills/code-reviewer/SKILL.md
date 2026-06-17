---
agent: code-reviewer
version: 1.0
---

## 1. Project-Specific Patterns

PMD runs automatically at `mvn clean test` phase via `pom.xml`. Violations fail the build. The active ruleset is `ruleset.xml` at the project root.

Key PMD rules to check manually in review:
- `AtLeastOneConstructor` — every class needs an explicit constructor
- `UnusedPrivateField`, `UnusedPrivateMethod`, `UnusedLocalVariable`, `UnusedFormalParameter`
- `PreserveStackTrace` — `throw new RuntimeException(e.getMessage())` is a violation; use `throw new RuntimeException(msg, e)`
- `UseCollectionIsEmpty` — `list.size() == 0` is a violation; use `list.isEmpty()`
- `ConfusingTernary` — negated conditions in ternary operators
- `AvoidUsingHardCodedIP`
- `OneDeclarationPerLine`

## 2. Naming Conventions

Check that new files follow:
- `*Api` for OpenAPI interface files in `controller` package
- `*Controller` implements `*Api`
- `*ServiceImpl` implements `*Service`
- `*Mapper` in `mapper` package with `@Mapper(componentModel = "spring")`
- `*Entity` for JPA entities, `*Document` for MongoDB documents
- `*Repository` for Spring Data interfaces
- `*TO`, `*Request`, `*Response` for transfer objects in `api/v1/to`

## 3. Error Handling

Review that:
- New exception types are registered in `GlobalExceptionHandler` with `@ExceptionHandler`
- `CompletionException` unwrapping is handled for async service failures
- Logger uses `logger.warn()` for client errors (4xx), `logger.error()` for server errors (5xx)
- Stack trace is preserved in `logger.error(msg, ex)` calls — not just `logger.error(ex.getMessage())`

## 4. Key Files

- `ruleset.xml` — full PMD ruleset
- `../../../src/main/java/com/umdc/api/v1/controller/GlobalExceptionHandler.java` — exception mapping
- `../../../src/main/java/com/umdc/api/v1/controller/CampaignApi.java` — reference for correct OpenAPI placement
- `../../../src/main/java/com/umdc/api/v1/service/CampaignServiceImpl.java` — reference service implementation

## 5. Constraints

- Zero tolerance for OpenAPI annotations on `*Controller` classes — they belong on `*Api` interfaces
- Zero tolerance for hardcoded `fixedRate` in `@Scheduled` — must use `fixedRateString = "${prx.scheduler.*}"`
- `MapStruct` mappers must not be instantiated with `new` — they are Spring-managed beans
- All loggers: `private static final Logger logger = LoggerFactory.getLogger(ClassName.class)`

## 6. Checklist

- [ ] PMD: `mvn clean test` passes without violations
- [ ] No `@Operation` / `@ApiResponse` on `*Controller` classes
- [ ] No hardcoded scheduler rates
- [ ] No `new XxxMapper()` instantiation
- [ ] Logger is SLF4J `LoggerFactory.getLogger()`
- [ ] New exceptions registered in `GlobalExceptionHandler`
- [ ] `PreserveStackTrace` — cause passed to new exception constructors
- [ ] `AtLeastOneConstructor` — all new classes have explicit constructor
