---
name: prepare-release
description: Prepares a Mercury release by verifying the build, updating the version, updating CHANGELOG, and creating a Git tag
mode: agent
agent: devops-engineer
tools: [Read, Edit, Bash]
---

## Input Variables

- `${releaseVersion}` — new version string (e.g., `0.1.0`)
- `${releaseNotes}` — summary of changes for CHANGELOG (or `AUTO` to generate from git log)

## Steps

1. **Verify the build is clean**:
   ```bash
   mvn -B -V -e clean verify
   ```
   If this fails, STOP and report the failure. Do not proceed with release.

2. **Verify there are no uncommitted changes**:
   ```bash
   git status
   ```
   All changes must be committed before tagging.

3. **Update `pom.xml` version**:
   - Change `<version>0.0.1</version>` to `<version>${releaseVersion}</version>`
   - File: `pom.xml`

4. **Update `CHANGELOG`**:
   - Add new entry at the top:
     ```
     ## [${releaseVersion}] - $(date +%Y-%m-%d)
     ${releaseNotes}
     ```
   - If `AUTO`, generate from: `git log <previous-tag>...HEAD --oneline`

5. **Commit the version bump**:
   ```bash
   git add pom.xml CHANGELOG
   git commit -m "chore: bump version to ${releaseVersion}"
   ```

6. **Create a Git tag**:
   ```bash
   git tag -a v${releaseVersion} -m "Release ${releaseVersion}"
   ```

7. **Build the release artifact**:
   ```bash
   mvn -U clean package -DskipTests
   ```
   Verify `target/mercury-${releaseVersion}.jar` exists.

8. **Build Docker image**:
   ```bash
   docker build -t mercury:${releaseVersion} -t mercury:latest .
   ```

9. **Report release checklist**:
   - `pom.xml` version updated
   - `CHANGELOG` updated
   - Git tag `v${releaseVersion}` created
   - JAR artifact `target/mercury-${releaseVersion}.jar` built
   - Docker image `mercury:${releaseVersion}` built

## Constraints

- Never release if `mvn clean verify` fails (PMD violations or JaCoCo below threshold)
- Never push directly to `main` — push via PR unless this is a hotfix tag
- New env vars introduced in this release must be in `environment_variables.md` before tagging
- `PRX_KAFKA_AUTO_STARTUP` must remain `false` as default in `bootstrap.yml`

## Output Format

Release checklist table:
| Step | Status | Details |
|---|---|---|
| `mvn clean verify` | PASS/FAIL | build log tail |
| Version bumped | YES/NO | old → new |
| CHANGELOG updated | YES/NO | entry preview |
| Git tag created | YES/NO | tag name |
| JAR built | YES/NO | file path + size |
| Docker image built | YES/NO | image tag |
