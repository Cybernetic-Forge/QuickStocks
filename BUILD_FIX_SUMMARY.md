# Build Issue Fix - Summary

## Problem Statement
The project was experiencing build failures in the CI/CD pipeline with the following error:
```
error reading /home/runner/.m2/repository/net/kyori/adventure-text-serializer-gson/4.24.0/adventure-text-serializer-gson-4.24.0.jar; 
zip END header not found
```

This error appeared for multiple Adventure dependencies and caused compilation failures across the entire project.

## Root Cause Analysis

### Primary Issue: Corrupted Maven Cache
The "zip END header not found" error indicates that JAR files in the Maven local repository (`.m2/repository`) were incomplete or corrupted. This typically occurs when:
1. Network interruptions occur during dependency downloads
2. Disk space issues prevent complete file writes
3. Concurrent Maven processes corrupt the cache
4. CI/CD runner issues cause partial downloads

### Secondary Issue: Version Inconsistency
The project was at version 0.0.4, but the request was to revert to version 0.0.3, likely because the 0.0.4 release encountered the build issue.

## Solution Implemented

### 1. Version Revert (0.0.4 → 0.0.3)
**Files Modified:**
- `pom.xml` - Maven project version
- `src/main/resources/plugin.yml` - Minecraft plugin version

**Rationale:** Reverting to the last known good version (0.0.3) ensures consistency with the previous successful release.

### 2. CI/CD Workflow Enhancement
**File Modified:** `.github/workflows/release.yml`

**Changes Added:**
```yaml
- name: Clean corrupted Maven dependencies
  run: |
    echo "Cleaning potentially corrupted Adventure dependencies..."
    rm -rf ~/.m2/repository/net/kyori/adventure-text-serializer-gson || true
    rm -rf ~/.m2/repository/net/kyori/adventure-text-serializer-legacy || true
    rm -rf ~/.m2/repository/net/kyori/adventure-text-logger-slf4j || true
    echo "Cleaning Maven cache for Paper API..."
    rm -rf ~/.m2/repository/io/papermc/paper || true
    echo "Running Maven dependency resolution with forced updates..."
    mvn -B -ntp dependency:purge-local-repository -DreResolve=true -DactTransitively=false || true

- name: Build project
  run: mvn -B -ntp clean package
```

**Key Features:**
1. **Proactive Cache Cleanup**: Removes specific corrupted dependency directories before build
2. **Graceful Failure Handling**: Uses `|| true` to prevent cleanup failures from stopping the workflow
3. **Dependency Purge**: Uses Maven's `dependency:purge-local-repository` to force re-resolution
4. **Clean Build**: Changed from `mvn package` to `mvn clean package` for thorough cleanup

### 3. Affected Dependencies
The fix specifically targets these commonly corrupted dependencies:
- `net.kyori:adventure-text-serializer-gson:4.24.0`
- `net.kyori:adventure-text-serializer-legacy:4.24.0`
- `net.kyori:adventure-text-logger-slf4j:4.24.0`
- `io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT`

## Testing & Validation

### Local Testing Limitations
Due to sandboxed environment network restrictions, full local testing was not possible. The Maven build requires access to:
- `repo.papermc.io` - Paper API repository
- `jitpack.io` - JitPack repository
- `repo.minebench.de` - ChestShop repository
- `repo.codemc.org` - CodeMC repository
- `maven.enginehub.org` - WorldGuard repository

### CI/CD Validation
The fix will be validated when:
1. A PR is merged to `dev` or `main` branch
2. The GitHub Actions workflow runs
3. The cleanup steps execute before the build
4. Maven successfully downloads fresh dependencies
5. The build completes and creates a release

## Expected Outcomes

### Successful Build Indicators
✅ Maven dependencies download successfully  
✅ Compilation completes without errors  
✅ Tests pass (if applicable)  
✅ JAR artifact is created in `target/` directory  
✅ GitHub release is created with the JAR file  

### Failure Recovery
If the build still fails:
1. Check GitHub Actions logs for the specific failure point
2. Verify network connectivity to Maven repositories
3. Check for disk space issues on the runner
4. Consider adding retry logic for dependency downloads
5. May need to investigate specific dependency version conflicts

## Best Practices Going Forward

### Prevention Strategies
1. **Maven Caching**: Use GitHub Actions cache for Maven dependencies with appropriate cache keys
2. **Retry Logic**: Consider adding Maven retry flags: `-Dmaven.wagon.http.retryHandler.count=3`
3. **Dependency Versions**: Pin specific versions for transitive dependencies to avoid resolution conflicts
4. **Health Checks**: Add a pre-build step to verify Maven repository integrity

### Monitoring
Monitor for these warning signs in future builds:
- Incomplete JAR file errors
- Checksum verification failures
- Timeout errors during dependency downloads
- Unexplained compilation failures after dependency updates

## Additional Notes

### Maven Offline Mode
For environments with persistent network issues, consider:
```bash
mvn -o clean package  # Offline mode
```
However, this requires all dependencies to be pre-cached.

### Alternative Solutions Not Implemented
1. **Dependency Mirroring**: Set up a private Maven mirror/proxy (e.g., Nexus, Artifactory)
2. **Docker Build**: Use a Docker container with pre-cached dependencies
3. **Dependency Vendoring**: Include critical JARs directly in the repository (not recommended)

## References
- Maven Documentation: https://maven.apache.org/ref/current/
- GitHub Actions Caching: https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows
- Maven Dependency Plugin: https://maven.apache.org/plugins/maven-dependency-plugin/

---

**Fix Applied:** 2025-11-17  
**Version Reverted:** 0.0.4 → 0.0.3  
**Status:** Ready for CI/CD validation
