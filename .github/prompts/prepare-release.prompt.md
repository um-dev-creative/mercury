---
name: prepare-release
description: Execute the full Mercury release process from version bump to GitHub release
mode: agent
agent: devops-engineer
tools:
  - run_in_terminal
  - read_file
  - insert_edit_into_file
  - replace_string_in_file
  - get_errors
---
# Prepare Release

## Input Variables
- `${releaseVersion}` — new version number, e.g., `1.4.0`
- `${releaseSummary}` — one-line description of this release
- `${releaseNotes}` — detailed changelog entries (Added / Fixed / Changed)

## Pre-Release Gate (ALL must pass before proceeding)

### Step 1: Verify Main Branch is Clean
```bash
git checkout main && git pull origin main
git --no-pager status
git --no-pager log --oneline -5
```
No uncommitted changes allowed.

### Step 2: Run Full Verify
```bash
mvn -B -V -e clean verify
```
Must produce `BUILD SUCCESS`. PMD violations = 0. JaCoCo line ≥ 70% / branch ≥ 50%.

### Step 3: SonarCloud Gate
Confirm SonarCloud quality gate PASSED on latest `main` commit:
```bash
gh run list --workflow ci.yml --branch main --limit 3
```
Latest run must show success.

## Release Steps

### Step 4: Bump Version in pom.xml
```bash
mvn versions:set -DnewVersion=${releaseVersion}
mvn versions:commit
```
Verify `pom.xml` now shows `<version>${releaseVersion}</version>`.

### Step 5: Update CHANGELOG
Prepend to `CHANGELOG` file:
```markdown
## [v${releaseVersion}] - $(date +%Y-%m-%d)
${releaseNotes}
```

### Step 6: Commit Version Bump
```bash
git add pom.xml CHANGELOG
git commit -m "chore(release): bump version to ${releaseVersion}"
```

### Step 7: Final Build Verification
```bash
mvn -B -V -e clean verify
```
Must pass with new version. Do not proceed if this fails.

### Step 8: Create and Push Release Tag
```bash
git tag -a v${releaseVersion} -m "Release v${releaseVersion}: ${releaseSummary}"
git push origin main
git push origin v${releaseVersion}
```

### Step 9: Create GitHub Release
```bash
gh release create v${releaseVersion} \
  --title "Mercury v${releaseVersion}" \
  --notes "${releaseNotes}" \
  --latest
```

### Step 10: Build Docker Image (if applicable)
```bash
docker build -t mercury:${releaseVersion} -t mercury:latest .
# Verify image starts
docker run --rm mercury:${releaseVersion} --help 2>/dev/null || echo "Image built OK"
```

### Step 11: Verify CI on Tag
```bash
gh run list --workflow ci.yml --limit 5
# Wait for tagged commit CI run to complete successfully
```

## Post-Release

### Step 12: Bump to Next Development Version
```bash
# e.g., 1.4.0 → 1.4.1-SNAPSHOT
mvn versions:set -DnewVersion=${nextDevVersion}-SNAPSHOT
mvn versions:commit
git add pom.xml
git commit -m "chore: begin development on ${nextDevVersion}-SNAPSHOT"
git push origin main
```

## Constraints
- NEVER tag a release if `mvn clean verify` fails
- NEVER tag a release if SonarCloud quality gate is RED
- NEVER skip the `CHANGELOG` update
- NEVER use a non-semantic-versioning tag format
- NEVER force-push to `main`

## Output Format
1. Pre-release gate results (all checks pass/fail)
2. `pom.xml` version change confirmed
3. `CHANGELOG` entry shown
4. Git tag created: `v${releaseVersion}`
5. GitHub release URL
6. Docker image tag (if built)
7. CI run status on tag
8. Next development version set
