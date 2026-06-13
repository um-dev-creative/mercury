---
name: Git
description: Tool for version control and branch management in the Mercury repository
type: terminal
command-prefix: git
used-by: [orchestrator, developer, devops-engineer]
---

## Purpose

Git manages source control for Mercury. The main branch is `main`. Feature development happens on `feature/*` branches with PRs opened against `main` or `develop`.

## Available Commands

### Branch creation
```bash
git checkout -b feature/<issue-id>-<short-description>
# example: git checkout -b feature/ds-171-toggle-campaign
```

### Stage and commit
```bash
git add src/main/java/com/prx/mercury/api/v1/controller/CampaignController.java
git commit -m "feat: add PATCH /api/v1/campaigns/{id}/toggle endpoint"
```

### Commit message conventions
Mercury uses conventional commits:
- `feat:` — new feature
- `fix:` — bug fix
- `refactor:` — code restructure without behavior change
- `test:` — test additions/changes
- `chore:` — build, config, tooling changes
- `docs:` — documentation only

### Push and open PR
```bash
git push -u origin feature/<branch-name>
gh pr create --title "feat: ..." --body "..."
```

### Check current status
```bash
git status
git log --oneline -10
git diff HEAD
```

### Recent merge pattern
PRs are merged to `main`. Recent merges:
- `feat: add PATCH /api/v1/campaigns/{id}/toggle endpoint`
- `feat: add PUT /api/v1/campaigns/{id} endpoint to update campaign details`

## Output Locations

- Current branch: `git branch --show-current`
- Remote: `origin` at GitHub

## Notes

- Branch naming: `feature/<youtrack-id>-<hyphenated-description>`
- Never force-push to `main`
- PRs require build to pass (PMD + JaCoCo) before merge
