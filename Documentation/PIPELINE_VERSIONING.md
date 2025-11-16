# Pipeline Versioning Enhancement

## Overview
This document describes the automatic version management system implemented in the GitHub Actions pipelines for QuickStocks.

## Workflows

The system is split into two separate workflows:

### 1. Build CI Workflow (`build-ci.yml`)
- **Trigger**: Runs on every push or PR that modifies files in `src/`
- **Purpose**: Continuous integration - builds and tests the code
- **Actions**: 
  - Checks out code
  - Sets up Java 21
  - Builds project with Maven
  - Runs tests

### 2. Release Workflow (`release.yml`)
- **Trigger**: Runs when PRs to `dev` or `main` branches are **merged**
- **Purpose**: Creates versioned releases with proper artifact names
- **Actions**:
  - Computes semantic version
  - Updates `pom.xml` and `plugin.yml`
  - Commits version changes
  - Builds versioned artifact
  - Creates GitHub release with tag

## Purpose
The release workflow automatically updates the Maven version in `pom.xml` and the plugin version in `plugin.yml` to match the semantic version being released. This ensures that:
1. The built artifact is named `QuickStocks-X.X.X.jar` (e.g., `QuickStocks-0.0.1.jar`)
2. The version in the source code matches the released version
3. Version updates are committed to the target branch (dev or main)

## How It Works

### Semantic Versioning Rules
The release workflow calculates the next version based on the merged PR's target branch:
- **Merged PR to `dev`**: Patch version is incremented (e.g., `v0.0.0` → `v0.0.1`)
- **Merged PR to `main`**: Minor version is incremented, patch reset (e.g., `v0.0.1` → `v0.1.0`)

### Release Workflow Steps
1. **Compute Next Version**: Analyzes existing tags and target branch to determine next version
2. **Update pom.xml**: Uses Maven Versions Plugin to update the version
   ```bash
   mvn versions:set -DnewVersion=X.X.X -DgenerateBackupPoms=false
   ```
3. **Update plugin.yml**: Uses sed to replace the version line
   ```bash
   sed -i "s/^version: .*/version: X.X.X/" src/main/resources/plugin.yml
   ```
4. **Commit Changes**: Commits both files with message `chore: bump version to X.X.X`
5. **Push to Branch**: Pushes the commit back to the PR branch
6. **Build Project**: Builds with the updated version
7. **Create Release**: Creates a GitHub release with the versioned artifact

## Files Modified by Pipeline
- `pom.xml` - Maven project version
- `src/main/resources/plugin.yml` - Bukkit plugin version

## Version Format
- **Git Tag**: `vX.X.X` (e.g., `v0.0.1`, `v1.0.0`)
- **Maven/Plugin Version**: `X.X.X` (no 'v' prefix, no -SNAPSHOT suffix)
- **Artifact Name**: `QuickStocks-X.X.X.jar`

## Example Workflows

### Build CI Workflow
```
Developer commits changes to src/main/java/...
↓
Build CI workflow triggers automatically
↓
Maven builds and tests the code
↓
Developer receives build/test status
```

### Release Workflow

#### Scenario 1: PR merged to dev branch
```
Current tag: v0.0.0
PR merged to: dev
↓
Release workflow triggers
↓
Next version computed: v0.0.1
↓
pom.xml and plugin.yml updated
↓
Changes committed to dev branch
↓
Build creates: QuickStocks-0.0.1.jar
↓
GitHub release created with tag v0.0.1
```

#### Scenario 2: PR merged to main branch
```
Current tag: v0.0.1
PR merged to: main
↓
Release workflow triggers
↓
Next version computed: v0.1.0
↓
pom.xml and plugin.yml updated
↓
Changes committed to main branch
↓
Build creates: QuickStocks-0.1.0.jar
↓
GitHub release created with tag v0.1.0
```

## Authentication
The workflow uses `GITHUB_TOKEN` provided by GitHub Actions with the following permissions:
- `contents: write` - Required to push commits and create releases

## Git Configuration
Commits are made using the GitHub Actions bot identity:
- **User**: `github-actions[bot]`
- **Email**: `github-actions[bot]@users.noreply.github.com`

## Benefits
1. **Consistency**: Version in code always matches the release version
2. **Automation**: No manual version bumping required
3. **Traceability**: Version updates are tracked in git history
4. **Correct Artifacts**: JAR files have proper version numbers

## Troubleshooting

### Problem: Push fails with authentication error
**Solution**: Ensure `token: ${{ secrets.GITHUB_TOKEN }}` is set in the checkout step

### Problem: Maven versions:set fails
**Solution**: Check Maven Versions Plugin is available (it's part of standard Maven)

### Problem: Wrong version format
**Solution**: Verify the version computation logic in the "Compute next semantic version" step

## Development Notes
- The workflow only runs on pull requests to `dev` or `main` branches
- Each PR creates a new release with an incremented version
- The version is computed from existing git tags, not from pom.xml
- If no tags exist, starts from `v0.0.0`
