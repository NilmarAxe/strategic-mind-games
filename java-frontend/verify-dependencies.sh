#!/bin/bash

echo "=========================================="
echo "Maven Dependencies Verification"
echo "=========================================="

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "✗ Maven not found. Please install Maven."
    exit 1
fi

echo "✓ Maven found: $(mvn --version | head -n 1)"

# Check pom.xml exists
if [ ! -f "pom.xml" ]; then
    echo "✗ pom.xml not found in current directory"
    exit 1
fi

echo "✓ pom.xml found"

# Validate pom.xml
echo ""
echo "Validating pom.xml..."
mvn validate

if [ $? -eq 0 ]; then
    echo "✓ pom.xml is valid"
else
    echo "✗ pom.xml validation failed"
    exit 1
fi

# Download dependencies
echo ""
echo "Downloading dependencies..."
mvn dependency:resolve

if [ $? -eq 0 ]; then
    echo "✓ All dependencies resolved"
else
    echo "✗ Failed to resolve dependencies"
    exit 1
fi

# Display dependency tree
echo ""
echo "Dependency tree:"
mvn dependency:tree | head -n 30

# Check for dependency conflicts
echo ""
echo "Checking for conflicts..."
mvn dependency:analyze

echo ""
echo "=========================================="
echo "Dependency verification complete!"
echo "=========================================="