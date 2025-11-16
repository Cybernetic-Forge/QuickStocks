# Pipeline Version Automation - Implementation Summary

## Issue Resolution
**Original Issue**: Pipeline enhancement to automatically commit version updates into Maven so the plugin gets versioned as "QuickStocks-X.X.X.jar" in dev/main branches.

**Status**: ✅ COMPLETED

## Changes Implemented

### 1. Split into Two Separate Workflows

#### a. Build CI Workflow (`.github/workflows/build-ci.yml`)
**Purpose**: Continuous integration - runs on every commit
**Trigger**: 
- Push or pull request events
- Only when files in `src/**` are modified

**Steps**:
1. Checkout repository
2. Set up Java 21
3. Build project with Maven
4. Run tests

**Benefits**:
- Fast feedback for developers
- Runs automatically on code changes
- No unnecessary builds for documentation changes

#### b. Release Workflow (`.github/workflows/release.yml`)
**Purpose**: Create versioned releases when PRs are merged
**Trigger**: 
- Pull request closed event (merged only)
- Target branches: `dev` or `main`

**Steps**:
1. Checkout base branch (dev or main)
2. Compute next semantic version
3. Update pom.xml using Maven Versions Plugin
4. Update plugin.yml using sed
5. Commit version changes to base branch
6. Build project
7. Create GitHub Release with tag

**Version Computation**:
- Modified to output both `tag` (with 'v' prefix) and `version` (without prefix)
- This allows using the version number directly in Maven and plugin.yml

**Update pom.xml Version**:
```bash
mvn -B -ntp versions:set -DnewVersion=$VERSION -DgenerateBackupPoms=false
```
- Uses Maven Versions Plugin to update the project version
- No backup POM files are created (cleaner git history)

**Update plugin.yml Version**:
```bash
sed -i "s/^version: .*/version: $VERSION/" src/main/resources/plugin.yml
```
- Uses sed to replace the version line in Bukkit's plugin descriptor
- Ensures the plugin version matches the Maven version

**Commit and Push Version Changes**:
```bash
git config user.name "github-actions[bot]"
git config user.email "github-actions[bot]@users.noreply.github.com"
git add pom.xml src/main/resources/plugin.yml
git commit -m "chore: bump version to $VERSION"
git push
```
- Commits version changes with a clear commit message
- Uses GitHub Actions bot identity
- Pushes changes to the base branch (dev or main)

**Enhanced Checkout**:
- Added `token: ${{ secrets.GITHUB_TOKEN }}` for push authentication
- Uses `ref: ${{ github.event.pull_request.base.ref }}` to checkout the target branch
- Only runs if PR was merged: `if: github.event.pull_request.merged == true`

### 2. Removed Old Workflow
- Deleted `.github/workflows/build.yml` (combined workflow)
- Replaced with two specialized workflows for better separation of concerns

### 2. Documentation

#### a. PIPELINE_VERSIONING.md
Comprehensive documentation including:
- Split workflow architecture (Build CI + Release)
- Purpose and overview of each workflow
- Semantic versioning rules (dev = patch, main = minor)
- Detailed step-by-step workflow explanation
- Example scenarios for both workflows
- Authentication and git configuration details
- Troubleshooting guide
- Development notes

#### b. PIPELINE_WORKFLOW_DIAGRAM.md
Visual documentation including:
- Current split architecture diagrams
- Build CI workflow visualization
- Release workflow visualization
- Before/after comparison
- Before/after workflow diagrams
- Semantic versioning logic examples
- File changes flow visualization
- Benefits summary

## Workflow Behavior

### Build CI Workflow
Triggered on every push or PR that modifies `src/**`:
```
Developer commits to src/main/java/...
↓
Build CI runs automatically
↓
Code is compiled and tested
↓
Developer receives build status
```

### Release Workflow

#### For Merged PRs to `dev` branch (Patch Release)
```
Current:  v0.0.0  →  Next:  v0.0.1  →  Artifact:  QuickStocks-0.0.1.jar
Current:  v1.2.3  →  Next:  v1.2.4  →  Artifact:  QuickStocks-1.2.4.jar
```

#### For Merged PRs to `main` branch (Minor Release)
```
Current:  v0.0.1  →  Next:  v0.1.0  →  Artifact:  QuickStocks-0.1.0.jar
Current:  v1.2.9  →  Next:  v1.3.0  →  Artifact:  QuickStocks-1.3.0.jar
```

## Benefits Achieved

1. ✅ **Automated Version Management**: No manual version bumping required
2. ✅ **Consistent Versioning**: Maven, plugin, and artifact versions always match
3. ✅ **Proper Artifact Names**: JAR files now have semantic version numbers (QuickStocks-X.X.X.jar)
4. ✅ **Git History**: All version changes tracked in commits
5. ✅ **Traceability**: Clear audit trail of version increments
6. ✅ **Zero Manual Intervention**: Fully automated process
7. ✅ **Separated Concerns**: Build CI runs independently from releases
8. ✅ **Efficient CI**: Only builds when source code changes
9. ✅ **Fast Feedback**: Developers get quick build status without release overhead

## Testing Performed

1. ✅ YAML syntax validated with yamllint
2. ✅ sed command tested and verified on plugin.yml
3. ✅ Simulation script created and executed successfully
4. ✅ Git operations verified
5. ✅ Documentation reviewed and completed

## Files Modified

1. `.github/workflows/build-ci.yml` - **NEW** Build CI workflow (27 lines)
2. `.github/workflows/release.yml` - **NEW** Release workflow (90 lines)
3. `.github/workflows/build.yml` - **DELETED** (replaced by split workflows)
4. `Documentation/PIPELINE_VERSIONING.md` - Updated for split architecture
5. `Documentation/PIPELINE_WORKFLOW_DIAGRAM.md` - Updated with new workflow diagrams
6. `Documentation/PIPELINE_VERSIONING_IMPLEMENTATION.md` - Updated implementation details

**Total Changes**: 117 lines added, 90 lines removed (split and improved)

## Next Steps

When this PR is merged, the repository will have:
1. **Build CI** that runs automatically on source code changes
2. **Release workflow** that triggers only when PRs are merged to dev/main
3. Automatic version computation and updates
4. Properly versioned artifacts in releases

## Example Usage

### Daily Development
```
Developer makes changes to src/main/java/...
↓
Commits and pushes
↓
Build CI runs automatically
↓
Maven compiles and tests
↓
Developer sees build status (pass/fail)
```

### Creating a Release
```
Developer merges PR to dev branch
↓
Release workflow triggers automatically
↓
Computes next patch version (e.g., v0.0.1)
↓
Updates pom.xml: <version>0.0.1</version>
↓
Updates plugin.yml: version: 0.0.1
↓
Commits: "chore: bump version to 0.0.1"
↓
Pushes commit to dev branch
↓
Builds: QuickStocks-0.0.1.jar
↓
Creates release with tag v0.0.1 containing QuickStocks-0.0.1.jar
```

## Implementation Notes

- Uses standard Maven Versions Plugin (no external dependencies)
- Uses sed for plugin.yml (available in all Unix/Linux environments)
- GitHub Actions bot identity used for commits (clean git history)
- GITHUB_TOKEN provides secure authentication (no secrets needed)
- Works with both dev and main branches
- Falls back to v0.0.0 if no tags exist

## Security Considerations

- Uses GitHub's built-in GITHUB_TOKEN (secure)
- No additional secrets or credentials required
- Bot commits are clearly identified in git history
- Only has write access within the workflow context

## Maintenance

The workflow is self-contained and requires no ongoing maintenance. It will:
- Continue to work as long as Maven Versions Plugin is available
- Automatically handle version increments based on branch
- Maintain consistent versioning across all artifacts

---

**Implementation Date**: 2025-11-16
**Implemented By**: GitHub Copilot
**Status**: Ready for Review and Merge
