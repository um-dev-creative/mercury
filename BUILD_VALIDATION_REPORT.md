BUILD VALIDATION REPORT
=======================

Generated: 2026-02-14

Summary
-------
This report summarizes the build / unit-test validation run performed locally in the repository root (C:\projects\mercury). The goal was to run a focused set of unit tests that exercise service and mapper logic and gather a concise, reproducible result set for CI validation.

Result (high level)
-------------------
- Command executed: (see Commands below)
- Total tests executed: 34
- Failures: 0
- Errors: 0
- Skipped: 0
- Build status: SUCCESS

Environment
-----------
- OS: Windows 11
- Maven: Apache Maven 3.9.3
- Java: 25.0.1 (runtime used for test execution)
- Maven home: C:\projects\ambient\maven

Commands run
------------
(Primary command used to run the focused test batch via Surefire):

mvn -Dtest="com.prx.mercury.api.v1.service.EmailServiceImplTest,com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest,com.prx.mercury.api.v1.to.VerificationCodeRequestTest,com.prx.mercury.mapper.VerificationCodeMapperTest,com.prx.mercury.mapper.TemplateMapperTest,com.prx.mercury.mapper.MessageRecordMapperTest" surefire:test -DtrimStackTrace=false

Notes:
- I invoked the Surefire plugin directly to avoid PMD/other lifecycle plugins that can fail under the current JDK/tooling combination.

Tests executed (per-class)
--------------------------
- com.prx.mercury.api.v1.service.EmailServiceImplTest — Tests run: 7 — Failures: 0
- com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest — Tests run: 12 — Failures: 0
- com.prx.mercury.api.v1.to.VerificationCodeRequestTest — Tests run: 5 — Failures: 0
- com.prx.mercury.mapper.MessageRecordMapperTest — Tests run: 5 — Failures: 0
- com.prx.mercury.mapper.TemplateMapperTest — Tests run: 2 — Failures: 0
- com.prx.mercury.mapper.VerificationCodeMapperTest — Tests run: 3 — Failures: 0

Sample logs / noteworthy output
------------------------------
- EmailServiceImplTest intentionally logs and throws when `templateDefined` is null; you may see a stack trace for that test in the run — this is expected as part of the test asserting the exception.
- During full lifecycle runs, PMD/ASM reported "Unsupported class file major version 69" due to the JDK used; running Surefire directly avoids that plugin execution.

Known issues and recommendations
--------------------------------
1) PMD / ASM compatibility with the JDK
   - Symptom: PMD parsing fails with "Unsupported class file major version 69" when running the full `mvn test` lifecycle under Java 25. This is a tooling compatibility issue between PMD/ASM and the JDK used to run tests.
   - Workarounds:
     - Run tests via Surefire directly (used in this report): use `sufire:test` goal or `-Dskip.pmd=true`/disable the PMD plugin locally.
     - Use a supported JDK (for example Java 21 as declared in `pom.xml`) for a full lifecycle `mvn test` run.
     - Upgrade PMD/plugin versions that support the JDK in your CI environment.

2) Dependency vulnerability warnings (IDE/scan)
   - The project `pom.xml` contains dependencies (direct or transitive) that are flagged by the IDE vulnerability scanner (examples: PostgreSQL driver 42.7.4, logback/log4j transitive entries, Apache POI 4.1.1). These warnings pre-existed the test runs and should be triaged separately.
   - Recommendation: run `mvn dependency:tree -DoutputFile=dependency-tree.txt` and use a CVE scanner (OWASP Dependency-Check, Snyk, Mend, or GitHub's Dependabot) to produce actionable upgrade/exclusion suggestions.

How to reproduce locally (quick)
--------------------------------
1. Ensure a compatible JDK is active (Java 21 recommended) OR run with the current Java but invoke Surefire directly.
2. From project root run:

mvn -Dtest="com.prx.mercury.api.v1.service.EmailServiceImplTest,com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest,com.prx.mercury.api.v1.to.VerificationCodeRequestTest,com.prx.mercury.mapper.VerificationCodeMapperTest,com.prx.mercury.mapper.TemplateMapperTest,com.prx.mercury.mapper.MessageRecordMapperTest" surefire:test

3. To run a single test class:

mvn -Dtest=com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest surefire:test

Generating coverage (JaCoCo)
---------------------------
- The project already configures JaCoCo in `pom.xml`. To run tests and generate JaCoCo reports (XML used by Sonar), run (example):

mvn -Pcoverage clean test

- If Sonar analysis is required locally, start a SonarQube instance and run the sonar-scanner pointing to the generated JaCoCo XML report (see project README for Sonar instructions).

Next actions I can take (pick any)
---------------------------------
- Run the full test suite inside a Java 21 environment and produce a project-wide test report and JaCoCo coverage.
- Generate a dependency tree and a short remediation plan for flagged CVEs.
- Create a CI pipeline example (GitHub Actions) that runs tests, coverage, and Sonar analysis.


---
End of BUILD_VALIDATION_REPORT

