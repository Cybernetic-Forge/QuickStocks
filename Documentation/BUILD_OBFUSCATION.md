# Build Obfuscation

QuickStocks supports optional code obfuscation during the build process to protect the plugin's source code from decompilation.

> **⚠️ IMPORTANT**: Obfuscation is **DISABLED by default**. You MUST explicitly enable it using `-Dobfuscate.enabled=true` when building, otherwise your code will NOT be obfuscated and can be easily decompiled. 
> 
> **For production releases, always use**: `mvn clean package -Dobfuscate.enabled=true`

## Overview

The obfuscation feature uses [ProGuard](https://www.guardsquare.com/proguard), a widely-used Java code optimizer and obfuscator. When enabled, ProGuard will:

- **Obfuscate** class names, method names, and field names (except those that need to be preserved for Bukkit/Paper API compatibility)
- **Optimize** the bytecode to improve performance
- **Shrink** the code by removing unused classes and methods

## Enabling Obfuscation

Obfuscation is **disabled by default** to keep development builds easy to debug. To enable it, you have two options:

### Option 1: Set Property in pom.xml

Edit the `pom.xml` file and change the `obfuscate.enabled` property from `false` to `true`:

```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <!-- Obfuscation control: set to true to enable ProGuard obfuscation -->
    <obfuscate.enabled>true</obfuscate.enabled>
</properties>
```

Then build normally:

```bash
mvn clean package
```

### Option 2: Pass Property via Command Line

You can enable obfuscation for a single build without modifying the pom.xml:

```bash
mvn clean package -Dobfuscate.enabled=true
```

## Build Output

- **Without obfuscation**: `target/QuickStocks-1.0.0-SNAPSHOT.jar`
- **With obfuscation**: `target/QuickStocks-1.0.0-SNAPSHOT.jar` (obfuscated version replaces the original)

The obfuscated JAR will have the same filename as the non-obfuscated version.

## Verifying Obfuscation Worked

After building with obfuscation enabled, you can verify it worked by checking for the ProGuard mapping file:

```bash
# Check if ProGuard ran - mapping file should exist
ls -lh target/proguard-mapping.txt

# The mapping file shows original → obfuscated name mappings
# If this file exists, obfuscation was applied
```

You can also use a Java decompiler to verify that class names are obfuscated. Classes like `PriceThresholdController` should be renamed to short names like `a`, `b`, `c`, etc.

**Common Issue**: If you can still see readable class names like `PriceThresholdController` in a decompiler, you likely built **without** the `-Dobfuscate.enabled=true` flag, so ProGuard was skipped entirely.

## What Gets Preserved

ProGuard is configured to preserve the following classes and members to ensure the plugin works correctly:

1. **Main Plugin Class**: `QuickStocksPlugin` - the plugin entry point
2. **Command Executors**: All classes implementing `CommandExecutor`
3. **Tab Completers**: All classes implementing `TabCompleter`
4. **Event Listeners**: All classes implementing `Listener`
5. **Configuration Serializable**: All classes implementing `ConfigurationSerializable`
6. **Public API**: All public classes in the `net.cyberneticforge.quickstocks.api` package
7. **Enums**: Enum values and valueOf methods
8. **Annotations**: All annotation information
9. **Line Numbers**: Source file and line number information (for stack traces)

## What Gets Obfuscated

Everything else will be obfuscated, including:

- Service classes
- Internal utility classes
- Database management classes
- Algorithm implementations
- Private methods and fields
- Non-public API classes

## Debugging Obfuscated Builds

Even when obfuscated, the plugin will still produce meaningful stack traces because:

- Line numbers are preserved (`-keepattributes SourceFile,LineNumberTable`)
- Class names for Bukkit-related classes are preserved
- Public API classes are preserved

However, internal implementation details will be harder to reverse-engineer from the compiled bytecode.

## Performance Impact

Obfuscation adds time to the build process:

- **Without obfuscation**: ~30-60 seconds (depending on your system)
- **With obfuscation**: +2-5 minutes (ProGuard analysis and optimization)

For development builds, it's recommended to keep obfuscation disabled. Enable it only for:

- Production releases
- Distribution builds
- Builds intended for public release

## Troubleshooting

### Build Fails with ProGuard Errors

If ProGuard reports warnings about missing classes, check that:

1. All dependencies are properly marked with `<scope>provided</scope>` in pom.xml
2. The Java version is Java 21 (ProGuard needs access to JDK modules)

### Plugin Doesn't Load After Obfuscation

If the plugin fails to load, ensure that:

1. All Bukkit-required classes are properly preserved (check the keep rules in pom.xml)
2. The plugin.yml file is not corrupted
3. Stack traces show which class is causing the issue

If you find a class that should be preserved but isn't, add a keep rule in the ProGuard configuration:

```xml
<option>-keep class your.package.YourClass {
    public *;
}</option>
```

## Security Considerations

**Obfuscation is not encryption**. While it makes reverse-engineering more difficult, determined attackers can still:

- Decompile the bytecode (though the code will be harder to read)
- Debug the running plugin
- Use reflection to access private members

For sensitive data like API keys or passwords, always:

- Store them in configuration files (not hardcoded)
- Use environment variables
- Implement proper access controls

## Advanced Configuration

The ProGuard configuration is located in the `pom.xml` file under the `proguard-maven-plugin` section. You can customize:

- **Optimization passes**: Default is 3 (`-optimizationpasses 3`)
- **Keep rules**: Add more `-keep` options to preserve additional classes
- **Obfuscation options**: Modify `-repackageclasses` to change package structure

See the [ProGuard documentation](https://www.guardsquare.com/manual/configuration) for more options.

## Recommended Workflow

1. **Development**: Build with `obfuscate.enabled=false` (default)
2. **Testing**: Build with `obfuscate.enabled=false` for easier debugging
3. **Pre-release**: Build with `obfuscate.enabled=true` and test thoroughly
4. **Release**: Always use `obfuscate.enabled=true` for public releases

## Example Commands

```bash
# Development build (no obfuscation)
mvn clean package

# Release build (with obfuscation via command line)
mvn clean package -Dobfuscate.enabled=true

# Release build (with obfuscation via pom.xml property)
# First, edit pom.xml and set obfuscate.enabled=true
mvn clean package

# View effective POM to see if profile is activated
mvn help:effective-pom -Dobfuscate.enabled=true
```

## References

- [ProGuard Official Website](https://www.guardsquare.com/proguard)
- [ProGuard Manual](https://www.guardsquare.com/manual/configuration)
- [Maven ProGuard Plugin](https://github.com/wvengen/proguard-maven-plugin)
