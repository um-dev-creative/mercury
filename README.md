# Mercury
## Qodana badges
[![Qodana](https://github.com/um-dev-creative/mercury/actions/workflows/qodana_code_quality.yml/badge.svg?branch=main)](https://github.com/um-dev-creative/mercury/actions/workflows/qodana_code_quality.yml)
## Sonar Cloud badges
[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=prx-dev_mercury)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)

[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=coverage)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=prx-dev_mercury&metric=bugs)](https://sonarcloud.io/summary/new_code?id=prx-dev_mercury)

## Technologies
[![Java](https://img.shields.io/badge/Java-21-blue?logo=java&style=flat-square)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen?logo=spring&style=flat-square)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8-red?logo=apachemaven&style=flat-square)](https://maven.apache.org/)
[![Docker base image](https://img.shields.io/badge/amazoncorretto-21--alpine3.20-blue?logo=docker&style=flat-square)](https://hub.docker.com/_/amazoncorretto)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-42.7.4-blue?logo=postgresql&style=flat-square)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-detected-brightgreen?logo=mongodb&style=flat-square)](https://www.mongodb.com/)
[![Kafka](https://img.shields.io/badge/Kafka-detected-orange?logo=apachekafka&style=flat-square)](https://kafka.apache.org/)
[![MapStruct](https://img.shields.io/badge/MapStruct-1.5.5.Final-blue?logo=mapstruct&style=flat-square)](https://mapstruct.org/)
[![Telegram Bots](https://img.shields.io/badge/TelegramBots-9.3.0-blue?logo=telegram&style=flat-square)](https://core.telegram.org/)
[![JUnit](https://img.shields.io/badge/JUnit-5.11.3-red?logo=junit&style=flat-square)](https://junit.org/)
[![Mockito](https://img.shields.io/badge/Mockito-5.14.2-red?logo=mockito&style=flat-square)](https://site.mockito.org/)
[![SonarCloud](https://img.shields.io/badge/SonarCloud-detected-4E9BCF?logo=sonarcloud&style=flat-square)](https://sonarcloud.io/)

One-line summary: Messaging backend and utility service for PRX (Mercury).

Overview
--------
Mercury is a messaging backend and utility service used by PRX to process and route messages, integrate with external services (email, Telegram), and expose REST APIs for clients. It is implemented as a Spring Boot application and includes Kafka integration, JPA (PostgreSQL), and optional MongoDB support for some persistence needs.

Requiriments
------------
(Note: heading intentionally matches the requested spelling. See the correct heading below.)

Requirements
------------
Minimum requirements to build and run Mercury locally:
- Java 21 (JDK) or a compatible runtime (Amazon Corretto 21 recommended)
- Maven 3.6+ (3.8+ recommended)
- Docker (optional; required to run the provided Dockerfile locally)
- PostgreSQL (if you need to run integration tests or use the JPA-backed features)

Quick build
-----------
This project uses Maven. From the repository root the basic build steps are:

```pwsh
# Clean and build the project (skip tests for a fast build)
mvn -U clean package -DskipTests

# Run unit tests and generate reports (recommended before pushing changes)
mvn clean test

# Build the executable JAR (artifact name: target/mercury.jar)
mvn -U clean package
```

Build and run with Docker (optional):

```pwsh
# Build the Docker image (uses the repository Dockerfile)
docker build -t prx/mercury:latest .

# Run the container exposing port 8118
docker run --rm -p 8118:8118 prx/mercury:latest
```

Notes on runtime configuration
- The application exposes port 8118 by default (see `Dockerfile` CMD and `EXPOSE`).
- The Docker image copies several certificate files and a keystore; ensure `src/main/resources/` contains the expected `.crt` and `.jks` files if you plan to build the container as-is.
- The service consults environment variables such as `VAULT_ENABLED` (see `Dockerfile` CMD) and may use Spring Cloud Config or Vault depending on your environment.

Known issues and workarounds
---------------------------
1. Missing certificates or keystore when building the Docker image
   - Symptom: Docker build fails because cert files or keystore are not found in `src/main/resources/`.
   - Workaround: Place the expected cert and keystore files (`*.crt`, `keystore.jks`) in `src/main/resources/` before building, or comment-out the COPY/KEYTOOL sections in the Dockerfile for local testing.

2. Transitive dependency vulnerability warnings reported by IDE / scanning tools
   - Symptom: IDE or scanning tool warns about transitive vulnerabilities (log4j/logback, commons, PostgreSQL driver, etc.). These come from direct or transitive dependencies declared in `pom.xml`.
   - Workaround: Generate a dependency tree and pin/override versions for vulnerable transitive dependencies in your `pom.xml` using `<dependencyManagement>` or explicit `<exclusions>`. Example to produce a tree:

```pwsh
mvn dependency:tree -DoutputFile=dependency-tree.txt -DoutputType=text
```

3. Sonar coverage thresholds may fail on local runs
   - Symptom: Local Sonar/SonarCloud checks indicate low coverage or fail Quality Gate due to branch/line thresholds enforced by CI.
   - Workaround: Run tests with the coverage profile and ensure JaCoCo reports are generated before running Sonar local analysis (see next section).

Continuous Integration
----------------------
This repository includes SonarCloud badges and properties suggesting Sonar integration. There is no repository-local CI config guaranteed in this tree (GitLab CI/GitHub Actions may be used by the upstream project). Typical CI steps you can adopt:
- Checkout code
- Run `mvn -Pcoverage -DskipTests=false clean test` to generate coverage reports (JaCoCo)
- Run static analysis, linters, and `mvn verify`
- Publish code coverage and send results to SonarCloud using the Sonar scanner with a project token

Example pipeline snippet (GitLab CI / GitHub Actions style, conceptual):

1. Build & test
```pwsh
mvn -Pcoverage clean test
```
2. Package
```pwsh
mvn -DskipTests=false package
```
3. Run Sonar analysis (requires SONAR_TOKEN)
```pwsh
sonar-scanner -Dsonar.projectKey=prx-dev_mercury -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$env:SONAR_TOKEN
```

## How to verify Sonar coverage locally

A short checklist and exact PowerShell commands to generate the JaCoCo XML report and run Sonar locally.

1) Generate the JaCoCo XML report (runs tests and produces XML/HTML reports):

```powershell
mvn -U -DskipITs clean verify
```

2) Confirm the JaCoCo XML report exists at the path configured in `pom.xml`:

```powershell
Test-Path .\target\site\jacoco\jacoco.xml
```

3) Run Sonar analysis. Choose one of the options below and provide your Sonar token as needed.

- Using Maven (recommended if running on the same machine that built the project):

```powershell
mvn sonar:sonar -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=<SONAR_TOKEN>
```

- Using SonarScanner CLI:

```powershell
sonar-scanner -Dsonar.projectKey=prx-dev_mercury -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=<SONAR_TOKEN>
```

Notes:
- `pom.xml` already sets `sonar.coverage.jacoco.xmlReportPaths` to `target/site/jacoco/jacoco.xml` and includes `src/main/java` in `sonar.inclusions`/`sonar.coverage.inclusions` so Sonar should pick up coverage for production sources.
- If your CI produces JaCoCo in a different location or runs tests in a different job, ensure the JaCoCo XML is available to the Sonar job (upload/download artifact between jobs or run Sonar in the same job).

## Documentation

- CHANGELOG.md — project changelog and migration notes
- BUILD_VALIDATION_REPORT.md — results of the upgrade validation and recommended fixes
- README-BUILD.md — build/run instructions and CI guidance


```pwsh
mvn org.springdoc:springdoc-openapi-maven-plugin:generate -DskipTests
```

- JavaDoc: Generate JavaDoc with Maven:

```pwsh
mvn javadoc:javadoc
```

- Project site and reports: The repository contains `htmlReport/` and `target/site/` artifacts (if you run site generation). To generate the Maven site:

```pwsh
mvn site
```

- Developer knowledge base: https://prx.myjetbrains.com/articles/PRX-A-77/Mercury (also referenced in existing README content)

Tech stack and versions
-----------------------
Below is an alphabetically sorted list of detected technologies, the extracted version (or a conservative fallback), and the source file where that information was found.

| Technology |       Version | Source |
|---|--------------:|---|
| Amazon Corretto (Docker base image) | 21-alpine3.20 | Dockerfile |
| Apache POI (poi-ooxml) |         4.1.1 | pom.xml |
| Docker (project contains a Dockerfile) |      detected | Dockerfile |
| Google Gson |        2.10.1 | pom.xml |
| JaCoCo (Maven plugin) |        0.8.12 | pom.xml |
| Java (language / runtime) |            21 | pom.xml |
| JUnit Jupiter |        5.11.3 | pom.xml |
| MapStruct |   1.5.5.Final | pom.xml |
| Maven (build tool) |           3.8 | pom.xml |
| Maven Javadoc Plugin |         3.6.3 | pom.xml |
| Maven Surefire Plugin |         3.2.5 | pom.xml |
| MongoDB (Spring Data integration present) |      detected | pom.xml |
| Mockito |        5.14.2 | pom.xml |
| org.json (json) |      20250517 | pom.xml |
| PostgreSQL JDBC driver |        42.7.4 | pom.xml |
| SonarCloud (project properties present) |      detected | pom.xml |
| Spring Boot |         3.5.8 | pom.xml |
| Spring Cloud |      2025.0.1 | pom.xml |
| Spring Core |         6.2.1 | pom.xml |
| Spring Data JPA (dependency present) |      detected | pom.xml |
| Spring Kafka (dependency present) |      detected | pom.xml |
| Spring Web (spring-boot-starter-web) |      detected | pom.xml |
| Springdoc OpenAPI (springdoc) |         2.6.0 | pom.xml |
| Telegram Bots (org.telegram) |         9.3.0 | pom.xml |

Note: "detected" means the technology is present but an explicit version was not found in the scanned files.

Files scanned
-------------
- pom.xml (project metadata, properties, dependencies, plugin versions)
- Dockerfile (base image tag and runtime container instructions)

License
-------
The repository does not contain a LICENSE file. Add one if you want to specify terms for reuse.

More
----
For development notes, CI, and architecture links see the original README content and the repository files.
