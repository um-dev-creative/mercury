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

Run all tests (recommended on CI or using Java 25)
-------------------------------------------------
If your Java SDK is compatible with the project's tooling (Java 25 LTS as specified in `pom.xml`) you can run the full test lifecycle and report generation:

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
- Resolved as of the Java 25 migration (MER-4): the "Unsupported class file major version 69" error
  was caused by ASM 9.7, pinned for the Surefire Mockito-agent override, not supporting Java 25
  bytecode. `pom.xml` now pins `asm.version` to 9.10.1, which supports it — no workaround needed
  when building on Java 25 as declared in `pom.xml`.
- If you still see this error on an even newer local JDK than the project targets, the same fix
  applies: bump `asm.version` (and check `jacoco-maven-plugin`/`maven-pmd-plugin`) to a release that
  supports that JDK's class file version.

Collecting reports
------------------
- Surefire test reports are saved under `target/surefire-reports` after test runs.
- JaCoCo exec/xml/html reports are placed under `target/site/jacoco` when the jacoco plugin runs.

If you want me to:
- Run the full test suite and generate the JaCoCo report using Java 25 in this environment, I can attempt to switch the JDK for the run (if available) or advise on how to run it locally.
- Generate a dependency CVE report and propose fixes for high severity items.
- Create a GitHub Actions CI workflow that runs tests, JaCoCo, and Sonar on each PR.


---
End of README_BUILD.md

