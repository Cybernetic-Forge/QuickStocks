# Pipeline Workflow Visualization

## Before Enhancement
```
┌─────────────────────────────────────────────────────────────────┐
│ GitHub Actions: Build & Release (Original)                      │
└─────────────────────────────────────────────────────────────────┘
    │
    ├─► 1. Checkout repository
    │
    ├─► 2. Set up Java
    │
    ├─► 3. Compute next semantic version (v0.0.1)
    │
    ├─► 4. Build project (creates QuickStocks-1.0.0-SNAPSHOT.jar)
    │                     ^^^^^^ PROBLEM: Wrong version!
    │
    └─► 5. Create GitHub Release (tag: v0.0.1)
         └─► Uploads: QuickStocks-1.0.0-SNAPSHOT.jar
                      ^^^^^^ PROBLEM: Mismatch between tag and artifact!
```

## After Enhancement
```
┌─────────────────────────────────────────────────────────────────┐
│ GitHub Actions: Build & Release (Enhanced)                      │
└─────────────────────────────────────────────────────────────────┘
    │
    ├─► 1. Checkout repository (with push token)
    │
    ├─► 2. Set up Java
    │
    ├─► 3. Compute next semantic version
    │    └─► Output: tag=v0.0.1, version=0.0.1
    │
    ├─► 4. Update pom.xml version
    │    └─► mvn versions:set -DnewVersion=0.0.1
    │         Changes: <version>1.0.0-SNAPSHOT</version>
    │                → <version>0.0.1</version>
    │
    ├─► 5. Update plugin.yml version
    │    └─► sed -i "s/^version: .*/version: 0.0.1/"
    │         Changes: version: 1.0.0-SNAPSHOT
    │                → version: 0.0.1
    │
    ├─► 6. Commit and push version changes
    │    └─► git commit -m "chore: bump version to 0.0.1"
    │         git push (to PR branch)
    │
    ├─► 7. Build project (creates QuickStocks-0.0.1.jar)
    │                     ✓ Correct version!
    │
    └─► 8. Create GitHub Release (tag: v0.0.1)
         └─► Uploads: QuickStocks-0.0.1.jar
                      ✓ Perfect match between tag and artifact!
```

## Semantic Versioning Logic

### PR to `dev` branch (Patch Bump)
```
Current: v0.0.0  →  Next: v0.0.1  →  Artifact: QuickStocks-0.0.1.jar
Current: v0.0.1  →  Next: v0.0.2  →  Artifact: QuickStocks-0.0.2.jar
Current: v1.2.3  →  Next: v1.2.4  →  Artifact: QuickStocks-1.2.4.jar
```

### PR to `main` branch (Minor Bump)
```
Current: v0.0.1  →  Next: v0.1.0  →  Artifact: QuickStocks-0.1.0.jar
Current: v0.1.5  →  Next: v0.2.0  →  Artifact: QuickStocks-0.2.0.jar
Current: v1.2.9  →  Next: v1.3.0  →  Artifact: QuickStocks-1.3.0.jar
```

## File Changes Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   pom.xml    │     │  plugin.yml  │     │   Git Repo   │
│              │     │              │     │              │
│ <version>    │     │ version:     │     │   Commit:    │
│  1.0.0-      │ ──► │  1.0.0-      │ ──► │   "chore:    │
│  SNAPSHOT    │     │  SNAPSHOT    │     │   bump       │
│ </version>   │     │              │     │   version"   │
│      ↓       │     │      ↓       │     │      ↓       │
│ <version>    │     │ version:     │     │   Pushed to  │
│  0.0.1       │     │  0.0.1       │     │   PR branch  │
│ </version>   │     │              │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                     │
       └────────────────────┴─────────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │  Maven Build    │
                   │                 │
                   │  Produces:      │
                   │  QuickStocks-   │
                   │  0.0.1.jar      │
                   └─────────────────┘
```

## Benefits Summary

✓ **Consistent Versioning**: Code version matches release version
✓ **Proper Artifact Names**: JAR files have semantic version numbers
✓ **Git History**: All version changes tracked in commits
✓ **Automation**: Zero manual intervention required
✓ **Traceability**: Clear audit trail of version increments
