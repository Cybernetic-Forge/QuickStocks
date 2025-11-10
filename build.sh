#!/bin/bash
# QuickStocks Build Script
# Demonstrates building with and without obfuscation

set -e  # Exit on error

echo "=================================================="
echo "QuickStocks Build Script"
echo "=================================================="
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed"
    echo "Please install Maven 3.9+ to continue"
    exit 1
fi

# Check Maven version
echo "✓ Maven found: $(mvn --version | head -1)"
echo ""

# Function to build
build_project() {
    local build_type=$1
    local obfuscate_flag=$2
    
    echo "=================================================="
    echo "Building: $build_type"
    echo "=================================================="
    
    if [ -z "$obfuscate_flag" ]; then
        echo ""
        echo "⚠️  WARNING: Building without obfuscation!"
        echo "⚠️  Code will be easily decompilable."
        echo "⚠️  For production, use: ./build.sh prod"
        echo ""
        mvn clean package
    else
        echo ""
        echo "🔒 Building with ProGuard obfuscation enabled"
        echo "🔒 Code will be protected against decompilation"
        echo ""
        mvn clean package -Dobfuscate.enabled=true
    fi
    
    if [ $? -eq 0 ]; then
        echo ""
        echo "✓ Build successful!"
        if [ -f "target/QuickStocks-1.0.0-SNAPSHOT.jar" ]; then
            local size=$(du -h target/QuickStocks-1.0.0-SNAPSHOT.jar | cut -f1)
            echo "✓ JAR created: target/QuickStocks-1.0.0-SNAPSHOT.jar ($size)"
        fi
    else
        echo "❌ Build failed!"
        exit 1
    fi
    echo ""
}

# Parse command line arguments
case "${1:-dev}" in
    dev|development)
        echo "Building DEVELOPMENT version (no obfuscation)"
        echo ""
        build_project "Development Build" ""
        ;;
    
    prod|production|release)
        echo "Building PRODUCTION version (with obfuscation)"
        echo ""
        build_project "Production Build" "true"
        ;;
    
    both)
        echo "Building BOTH versions for comparison"
        echo ""
        
        # Build development version
        build_project "Development Build" ""
        
        # Save the dev JAR
        if [ -f "target/QuickStocks-1.0.0-SNAPSHOT.jar" ]; then
            cp target/QuickStocks-1.0.0-SNAPSHOT.jar target/QuickStocks-1.0.0-SNAPSHOT-dev.jar
            echo "✓ Saved as: target/QuickStocks-1.0.0-SNAPSHOT-dev.jar"
            echo ""
        fi
        
        # Build production version
        build_project "Production Build" "true"
        
        # Save the prod JAR
        if [ -f "target/QuickStocks-1.0.0-SNAPSHOT.jar" ]; then
            cp target/QuickStocks-1.0.0-SNAPSHOT.jar target/QuickStocks-1.0.0-SNAPSHOT-prod.jar
            echo "✓ Saved as: target/QuickStocks-1.0.0-SNAPSHOT-prod.jar"
            echo ""
        fi
        
        # Compare sizes
        echo "=================================================="
        echo "Build Comparison"
        echo "=================================================="
        if [ -f "target/QuickStocks-1.0.0-SNAPSHOT-dev.jar" ] && [ -f "target/QuickStocks-1.0.0-SNAPSHOT-prod.jar" ]; then
            echo "Development (no obfuscation):"
            ls -lh target/QuickStocks-1.0.0-SNAPSHOT-dev.jar | awk '{print "  Size: " $5}'
            echo ""
            echo "Production (with obfuscation):"
            ls -lh target/QuickStocks-1.0.0-SNAPSHOT-prod.jar | awk '{print "  Size: " $5}'
        fi
        ;;
    
    help|--help|-h)
        echo "Usage: $0 [dev|prod|both]"
        echo ""
        echo "Options:"
        echo "  dev, development  - Build without obfuscation (default)"
        echo "  prod, production  - Build with obfuscation"
        echo "  release           - Alias for production"
        echo "  both              - Build both versions and compare"
        echo "  help              - Show this help message"
        echo ""
        echo "Examples:"
        echo "  $0                  # Development build"
        echo "  $0 dev              # Development build"
        echo "  $0 prod             # Production build"
        echo "  $0 both             # Build both for comparison"
        exit 0
        ;;
    
    *)
        echo "❌ Unknown option: $1"
        echo "Run '$0 help' for usage information"
        exit 1
        ;;
esac

echo "=================================================="
echo "Build Complete!"
echo "=================================================="
