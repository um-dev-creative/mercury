# Tool: Git

**Purpose:** Version control operations for Mercury feature branches, commits, and releases.

## Branch Strategy

### Branch Types
```
main        — production-ready; release tags applied here
feature/*   — feature branches (off main)
hotfix/*    — hotfix branches (off main, fast-track release)
```

### Create Feature Branch
```bash
git checkout main
git pull origin main
git checkout -b feature/<ticket-or-description>
# Example:
git checkout -b feature/campaign-progress-endpoint
```

### Create Hotfix Branch
```bash
git checkout main
git pull origin main
git checkout -b hotfix/<description>
```

## Commit Conventions

### Format
```
<type>(<scope>): <short summary>

Types: feat | fix | chore | refactor | test | docs | ci | perf
Scope: optional, e.g. campaign | kafka | auth | db | scheduler

Examples:
  feat(campaign): add GET /api/v1/campaigns/{id}/progress endpoint
  fix(auth): throw ForbiddenException instead of returning null
  chore(release): bump version to 1.4.0
  test(campaign): add unit tests for CampaignServiceImpl
  ci: update JaCoCo branch coverage threshold to 50%
  db: add V20250315001__add_campaign_status_column.sql migration
```

### Atomic Commits
- One logical change per commit
- Commit after each Mercury layer is complete (persistence, service, controller, tests)

## Common Operations

### Stage and Commit
```bash
git add src/main/java/com/umdc/mercury/api/v1/service/CampaignServiceImpl.java
git commit -m "feat(campaign): add createCampaign async service method"
```

### Stage All Changes
```bash
git add .
git status   # always verify before committing
git commit -m "feat(campaign): implement campaign creation flow"
```

### Push Feature Branch
```bash
git push origin feature/campaign-progress-endpoint
```

### Update Branch with Latest Main
```bash
git fetch origin
git rebase origin/main
# or
git merge origin/main
```

## Release Tagging
```bash
# Create annotated release tag
git tag -a v1.4.0 -m "Release v1.4.0"

# Push tag
git push origin v1.4.0

# List tags
git --no-pager tag -l "v*" --sort=-v:refname | head -10
```

## Viewing History
```bash
# Compact log
git --no-pager log --oneline -20

# Changes in a file
git --no-pager log --oneline -- src/main/java/com/umdc/mercury/api/v1/service/CampaignServiceImpl.java

# Diff of current changes
git --no-pager diff
```

## Key Files
- `.gitignore` — files excluded from version control
- `CHANGELOG` — release notes (update before each release)
- `pom.xml` — version bumped via `mvn versions:set` before tagging
