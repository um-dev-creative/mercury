README - Build Instructions and Validation
=========================================

Generated: 2026-02-14

Purpose
-------
This file provides a concise, copy-paste friendly set of commands and guidance to run the project's unit tests, generate JaCoCo coverage, and validate Sonar analysis locally. It also includes tips to avoid known local tooling issues (PMD/ASM version compatibility with newer JDKs).

Quick test run (focused)
------------------------
To run a focused batch of unit tests (service and mapper tests used during development and validation):

```pwsh
# Run a focused test set using Surefire (avoids PMD lifecycle) 
mvn -Dtest="com.prx.mercury.api.v1.service.EmailServiceImplTest,com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest,com.prx.mercury.api.v1.to.VerificationCodeRequestTest,com.prx.mercury.mapper.VerificationCodeMapperTest,com.prx.mercury.mapper.TemplateMapperTest,com.prx.mercury.mapper.MessageRecordMapperTest" surefire:test -DtrimStackTrace=false
```

Run a single test class
-----------------------
```pwsh
mvn -Dtest=com.prx.mercury.api.v1.service.VerificationCodeServiceImplTest surefire:test
```

Run all tests (recommended on CI or using Java 21)
-------------------------------------------------
If your Java SDK is compatible with the project's tooling (Java 21 as specified in `pom.xml`) you can run the full test lifecycle and report generation:

```pwsh
# Run unit tests and generate JaCoCo coverage (profile may require configuration in pom.xml)
mvn -Pcoverage clean test
```

Generate dependency tree
------------------------
Useful to triage transitive vulnerabilities and determine exact versions:

```pwsh
mvn dependency:tree -DoutputFile=dependency-tree.txt -DoutputType=text
```

Sonar local verification
------------------------
1. Start local SonarQube (docker):

```pwsh
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts
```

2. Run tests with JaCoCo profile:

```pwsh
mvn -Pcoverage clean test
```

3. Run sonar-scanner against the local server (sonar-scanner must be installed):

```pwsh
sonar-scanner -Dsonar.projectKey=mercury_local -Dsonar.host.url=http://localhost:9000 -Dsonar.login=admin -Dsonar.password=admin
```

Workarounds for local tool incompatibilities
-------------------------------------------
- PMD / ASM may fail on very new JDK versions (error: "Unsupported class file major version 69"). Workarounds:
  - Use Java 21 for local full `mvn test` runs (recommended). The project `pom.xml` declares Java 21.
  - Alternatively run the focused test set via Surefire plugin directly (see Quick test run above).
  - Upgrade PMD/ASM plugin versions in `pom.xml` to versions compatible with the local JDK.

Collecting reports
------------------
- Surefire test reports are saved under `target/surefire-reports` after test runs.
- JaCoCo exec/xml/html reports are placed under `target/site/jacoco` when the jacoco plugin runs.

If you want me to:
- Run the full test suite and generate the JaCoCo report using Java 21 in this environment, I can attempt to switch the JDK for the run (if available) or advise on how to run it locally.
- Generate a dependency CVE report and propose fixes for high severity items.
- Create a GitHub Actions CI workflow that runs tests, JaCoCo, and Sonar on each PR.


---
End of README_BUILD.md

