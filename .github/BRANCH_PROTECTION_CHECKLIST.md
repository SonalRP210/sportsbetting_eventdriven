# Branch Protection Checklist

Use this checklist to configure `main` branch protection after splitting CI workflows.

## Recommended Branch Rules

- Require a pull request before merging
- Require approvals (suggestion: 1-2)
- Dismiss stale approvals on new commits
- Require conversation resolution before merging
- Require status checks to pass before merging
- Require branches to be up to date before merging
- Include administrators (recommended)

## Required Status Checks

From `CI PR` workflow:

- `Fast Tests`
- `SonarQube PR` (when Sonar is enabled)
- `Dependency Review`
- `Trivy Filesystem`

Optional additional checks from other workflows (if you choose to require them on PRs):

- `Full Verify`
- `SonarQube Main`

## Required Repository Configuration

- Repository variable: `SONAR_HOST_URL`
- Repository secret: `SONAR_TOKEN`

## Notes

- `SonarQube PR` is conditionally skipped when `SONAR_HOST_URL` is not set.
- `Trivy` uploads SARIF findings to the GitHub Security tab.
- If you later enforce signed commits or merge queue, add those controls here too.
