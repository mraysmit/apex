#!/bin/bash
# Script to take screenshots of the APEX Playground using Selenium
# Usage: ./take-playground-screenshots.sh [base_url] [screenshot_dir]
# Example: ./take-playground-screenshots.sh http://localhost:8080 screenshots

set -e

# Set default values
BASE_URL=${1:-"http://localhost:8080"}
SCREENSHOT_DIR=${2:-"screenshots"}

echo "============================================================================"
echo "APEX Playground Screenshot Capture"
echo "============================================================================"
echo "Base URL: $BASE_URL"
echo "Screenshot Directory: $SCREENSHOT_DIR"
echo

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not available in PATH"
    echo "Please install Maven or add it to your PATH"
    exit 1
fi

# Check if the playground is running
echo "Checking if playground is accessible at $BASE_URL..."
if ! curl -s --head "$BASE_URL/playground" > /dev/null 2>&1; then
    echo "WARNING: Playground may not be running at $BASE_URL"
    echo "Please ensure the APEX Playground application is started"
    echo
    echo "To start the playground, run:"
    echo "  mvn spring-boot:run -pl apex-playground"
    echo
    read -p "Continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Exiting..."
        exit 1
    fi
fi

echo
echo "Starting screenshot capture..."
echo

# Run the screenshot utility
mvn exec:java -pl apex-playground \
    -Dexec.mainClass="dev.mars.apex.playground.util.PlaygroundScreenshotRunner" \
    -Dexec.args="$BASE_URL $SCREENSHOT_DIR" \
    -Dexec.cleanupDaemonThreads=false

if [ $? -eq 0 ]; then
    echo
    echo "============================================================================"
    echo "Screenshot capture completed successfully!"
    echo "============================================================================"
    echo "Screenshots saved to: $SCREENSHOT_DIR"
    echo
    echo "Generated screenshots:"
    ls -la "$SCREENSHOT_DIR"/playground_*.png 2>/dev/null || echo "No screenshots found"
    echo
    echo "You can view the screenshots with:"
    echo "  open $SCREENSHOT_DIR  # macOS"
    echo "  xdg-open $SCREENSHOT_DIR  # Linux"
else
    echo
    echo "============================================================================"
    echo "Screenshot capture failed!"
    echo "============================================================================"
    echo "Please check the error messages above and ensure:"
    echo "1. The APEX Playground is running at $BASE_URL"
    echo "2. Chrome browser is installed"
    echo "3. All dependencies are available"
fi

echo
