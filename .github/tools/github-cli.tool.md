# Tool: GitHub CLI (`gh`)

**Purpose:** Create pull requests, releases, and manage GitHub resources for Mercury.

## Prerequisites
```bash
gh auth status    # verify authentication
gh auth login     # authenticate if needed
```

## Pull Request Operations

### Create Pull Request
```bash
gh pr create \
  --base main \
  --head feature/campaign-progress-endpoint \
  --title "feat(campaign): add campaign progress endpoint" \
  --body "$(cat .github/PULL_REQUEST_TEMPLATE.md)"
```

### Create PR with Inline Body
```bash
gh pr create \
  --base main \
  --head feature/campaign-progress-endpoint \
  --title "feat(campaign): add campaign progress endpoint" \
  --body "## Summary
Adds GET /api/v1/campaigns/{id}/progress endpoint.

## Changes
- CampaignProgressServiceImpl
- CampaignApi + CampaignController
- CampaignProgressResponse DTO record

## Checklist
- [x] PMD violations = 0
- [x] Line coverage ≥ 70%
- [x] Branch coverage ≥ 50%"
```

### View PR Status
```bash
gh pr view --web
gh pr checks
```

### List Open PRs
```bash
gh pr list --state open
```

### Merge PR (after approval)
```bash
gh pr merge --squash --delete-branch
```

## Release Operations

### Create Release
```bash
gh release create v1.4.0 \
  --title "Mercury v1.4.0" \
  --notes "See CHANGELOG for full details." \
  --latest
```

### Create Release from CHANGELOG
```bash
gh release create v1.4.0 \
  --title "Mercury v1.4.0" \
  --notes-file CHANGELOG \
  --latest
```

### List Releases
```bash
gh release list --limit 10
```

### View Release
```bash
gh release view v1.4.0
```

## Repository Info
```bash
gh repo view                  # view repo details
gh run list --limit 10        # list recent workflow runs
gh run view <run-id>          # view specific run details
```

## Key Workflows (for reference)
```
.github/workflows/ci.yml                — main CI (triggered on PR/push)
.github/workflows/build.yml             — SonarQube build
.github/workflows/qodana_code_quality.yml — Qodana analysis
```

## Notes
- Mercury repo: `um-dev-creative/mercury`
- Always use `--base main` for PRs (feature branches off main)
- PR title must follow commit convention: `<type>(<scope>): <summary>`
- PR template at `.github/PULL_REQUEST_TEMPLATE.md` — fill out checklist
