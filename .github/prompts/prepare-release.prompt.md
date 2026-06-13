---
name: Prepare Release
description: >
  Cut a Mercury release: version bump, full build verification, Docker tag,
  git tag, and GitHub release draft.
mode: agent
agent: devops-engineer
tools: [read_file, replace_string_in_file, run_in_terminal, file_search]
---

## Input Variables

- `${version}` — new version string (e.g., `1.3.0`)
- `${releaseBranch}` — branch to release from (e.g., `main` or `release/1.3.0`)

## Steps

1. **Verify the branch is clean and passing**:
   ```bash
   git status
   mvn -B -V -e clean verify
   ```
   Must be `BUILD SUCCESS` before proceeding.

2. **Bump version in `pom.xml`**:
   - Change `<version>...</version>` under `<groupId>com.prx</groupId><artifactId>mercury</artifactId>` to `${version}`

3. **Build the release artifact**:
   ```bash
   mvn -U clean package -DskipTests
   ```
   Confirm `target/mercury-${version}.jar` exists.

4. **Build and tag Docker image**:
   ```bash
   docker build -t prx/mercury:${version} -t prx/mercury:latest .
   ```

5. **Run full security audit** (invoke `security-reviewer` with `security-audit.prompt.md`, scope=`pre-release`).

6. **Create annotated git tag**:
   ```bash
   git tag -a v${version} -m "Release ${version}"
   git push origin v${version}
   ```

7. **Draft GitHub release**:
   - Title: `v${version}`
   - Body: changelog entries since last tag
   - Attach: `target/mercury-${version}.jar`

## Constraints

- Do not tag if `mvn verify` fails
- Do not push the tag before security audit passes
- `VAULT_TOKEN` and real secrets must not appear in release notes or artifact config

## Output

```
Release Preparation — Mercury v${version}

Step 1 — Build verify: PASS / FAIL
Step 2 — pom.xml bumped: ${oldVersion} → ${version}
Step 3 — JAR built: target/mercury-${version}.jar ✓
Step 4 — Docker tagged: prx/mercury:${version} ✓
Step 5 — Security audit: PASS / FAIL
Step 6 — Git tag: v${version} pushed ✓
Step 7 — GitHub release: DRAFT / PUBLISHED

Overall: READY / BLOCKED (reason)
```
