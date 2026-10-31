# Shared Skill: Release Process

**Used by:** `devops-engineer`

## Mercury Release Workflow

### Version Strategy
Mercury follows **Semantic Versioning**: `MAJOR.MINOR.PATCH`
- `MAJOR` — breaking API change or major architecture change
- `MINOR` — new feature, backward-compatible
- `PATCH` — bug fix, non-breaking

Tag format: `v<MAJOR>.<MINOR>.<PATCH>` (e.g., `v1.4.0`)

### Pre-Release Gate (must all pass)
```
1. mvn -B -V -e clean verify          — full build + PMD + JaCoCo gates
2. JaCoCo line coverage ≥ 70%         — checked by verify phase
3. JaCoCo branch coverage ≥ 50%       — checked by verify phase
4. PMD violations = 0                  — checked by verify phase
5. security-reviewer APPROVE           — no BLOCK findings
6. code-reviewer APPROVE               — no REQUEST_CHANGES
7. SonarCloud quality gate = PASSED    — from ci.yml workflow
```

### Release Steps
```bash
# Step 1: Ensure main branch is clean
git checkout main
git pull origin main

# Step 2: Update version in pom.xml
mvn versions:set -DnewVersion=<MAJOR>.<MINOR>.<PATCH>
mvn versions:commit

# Step 3: Run full verify
mvn -B -V -e clean verify

# Step 4: Update CHANGELOG
# Add release entry at top of CHANGELOG file

# Step 5: Commit version bump
git add pom.xml CHANGELOG
git commit -m "chore(release): bump version to <MAJOR>.<MINOR>.<PATCH>"

# Step 6: Tag release
git tag -a v<MAJOR>.<MINOR>.<PATCH> -m "Release v<MAJOR>.<MINOR>.<PATCH>"

# Step 7: Push
git push origin main
git push origin v<MAJOR>.<MINOR>.<PATCH>

# Step 8: Create GitHub release
gh release create v<MAJOR>.<MINOR>.<PATCH> \
  --title "Mercury v<MAJOR>.<MINOR>.<PATCH>" \
  --notes "See CHANGELOG for details" \
  --latest
```

### Docker Release
```bash
# Build and tag Docker image to match release
docker build -t mercury:<MAJOR>.<MINOR>.<PATCH> -t mercury:latest .

# Verify image starts
docker run --rm mercury:<MAJOR>.<MINOR>.<PATCH> --version || true
```

### CHANGELOG Format
```markdown
# CHANGELOG

## [v1.4.0] - 2025-03-15
### Added
- Campaign progress tracking endpoint
- Email channel retry mechanism

### Fixed
- Verification code expiry calculation

### Changed
- Upgraded Spring Boot to 3.5.8

## [v1.3.0] - 2025-02-01
...
```

### Branch Strategy
```
main        — production-ready code; release tags applied here
develop     — integration branch (if used)
feature/*   — feature branches off main; PR → main
hotfix/*    — hotfix branches off main; PR → main; fast-track release
```

### Post-Release
- Verify GitHub Actions CI passes on tagged commit
- Verify SonarCloud quality gate passes
- Update `dependabot.yml` if new ecosystems were added
- Notify team via relevant channel

## Checklist
- [ ] All pre-release gates pass
- [ ] `pom.xml` version updated via `mvn versions:set`
- [ ] `CHANGELOG` updated with release notes
- [ ] Version commit message: `chore(release): bump version to <version>`
- [ ] Git tag `v<MAJOR>.<MINOR>.<PATCH>` created and pushed
- [ ] GitHub release created via `gh release create`
- [ ] Docker image built and tagged (if applicable)
- [ ] CI passes on tagged commit
- [ ] SonarCloud quality gate = PASSED on release commit
