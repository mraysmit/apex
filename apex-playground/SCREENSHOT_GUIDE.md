# APEX Playground Screenshot Guide

This guide explains how to take screenshots of the APEX Playground using Selenium WebDriver.

## Overview

The screenshot functionality provides several ways to capture images of the APEX Playground:

1. **Automated Test Screenshots** - Screenshots taken during UI tests
2. **Standalone Screenshot Utility** - Independent screenshot capture tool
3. **Custom Screenshot Scenarios** - Programmatic screenshot capture with custom data

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Chrome browser installed (for headless screenshots)
- APEX Playground application running

## Quick Start

### 1. Start the APEX Playground

```bash
# Start the playground application
mvn spring-boot:run -pl apex-playground
```

The playground will be available at `http://localhost:8080/playground`

### 2. Take Screenshots Using the Script

**Windows:**
```cmd
scripts\take-playground-screenshots.bat
```

**Linux/macOS:**
```bash
chmod +x scripts/take-playground-screenshots.sh
./scripts/take-playground-screenshots.sh
```

**Custom URL and directory:**
```bash
./scripts/take-playground-screenshots.sh http://localhost:8080 my-screenshots
```

## Screenshot Types

### 1. Basic Playground Screenshot
- Empty playground interface
- Shows all 4 panels (Source Data, YAML Rules, Validation Results, Enrichment Results)
- Desktop resolution (1920x1080)

### 2. Playground with Sample Data
- Pre-loaded with realistic JSON data and YAML rules
- Shows processed results in the bottom panels
- Demonstrates the complete workflow

### 3. Responsive Screenshots
- **Mobile View**: 375x667 (iPhone 6/7/8 size)
- **Tablet View**: 768x1024 (iPad size)
- **Desktop View**: 1920x1080 (Full desktop)

### 4. Error Handling Screenshot
- Shows invalid YAML syntax
- Demonstrates error validation and status indicators

## Using the Test Framework

### Run Screenshot Tests

```bash
# Run all screenshot tests
mvn test -pl apex-playground -Dtest=PlaygroundScreenshotTest

# Run specific screenshot test
mvn test -pl apex-playground -Dtest=PlaygroundScreenshotTest#testBasicPlaygroundScreenshot
```

### Available Test Methods

- `testBasicPlaygroundScreenshot()` - Basic empty playground
- `testScreenshotWithSampleData()` - Playground with processed data
- `testMobileResponsiveScreenshot()` - Mobile viewport
- `testTabletResponsiveScreenshot()` - Tablet viewport
- `testMultipleBrowserScreenshots()` - Cross-browser screenshots
- `testElementSpecificScreenshots()` - Individual UI elements
- `testWorkflowScreenshots()` - Step-by-step workflow

## Programmatic Usage

### Using PlaygroundScreenshotUtil

```java
import dev.mars.apex.playground.ui.PlaygroundScreenshotUtil;

// Create screenshot utility
PlaygroundScreenshotUtil util = new PlaygroundScreenshotUtil("http://localhost:8080");

// Take basic screenshot
String path = util.takeScreenshot(
    PlaygroundScreenshotUtil.Browser.CHROME,
    PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
    "my_scenario"
);

// Take screenshot with custom data
PlaygroundScreenshotUtil.PlaygroundData data = 
    new PlaygroundScreenshotUtil.PlaygroundData(jsonData, yamlRules, true);

String path = util.takeScreenshot(
    PlaygroundScreenshotUtil.Browser.CHROME,
    PlaygroundScreenshotUtil.ScreenshotType.DESKTOP_VIEW,
    "custom_data",
    data,
    true // headless
);

// Clean up
util.close();
```

### Browser Support

- **Chrome** - Primary browser, best compatibility
- **Firefox** - Cross-browser testing
- **Edge** - Windows compatibility testing

### Screenshot Types

```java
public enum ScreenshotType {
    FULL_PAGE,           // Full page screenshot
    VIEWPORT_ONLY,       // Only visible viewport
    ELEMENT_SPECIFIC,    // Specific UI element
    MOBILE_VIEW,         // Mobile responsive view (375x667)
    TABLET_VIEW,         // Tablet responsive view (768x1024)
    DESKTOP_VIEW         // Desktop view (1920x1080)
}
```

## Output

Screenshots are saved with descriptive filenames:

```
playground_chrome_desktop_view_basic_playground_20240123_143022.png
playground_chrome_mobile_view_responsive_20240123_143025.png
playground_firefox_desktop_view_cross_browser_test_20240123_143030.png
```

**Filename Format:**
`playground_{browser}_{type}_{scenario}_{timestamp}.png`

## Default Locations

- **Test Screenshots**: `apex-playground/target/screenshots/`
- **Standalone Screenshots**: `screenshots/` (configurable)

## Troubleshooting

### Common Issues

1. **Chrome not found**
   - Install Chrome browser
   - WebDriverManager will automatically download ChromeDriver

2. **Playground not accessible**
   - Ensure the playground is running: `mvn spring-boot:run -pl apex-playground`
   - Check the URL is correct (default: http://localhost:8080)

3. **Permission errors**
   - Ensure write permissions to screenshot directory
   - Try running with elevated permissions if needed

4. **Headless mode issues**
   - Set `headless=false` for debugging
   - Check browser console for JavaScript errors

### Debug Mode

To run screenshots in non-headless mode for debugging:

```java
// In test code, set headless to false
util.takeScreenshot(browser, type, scenario, data, false);
```

## Integration with CI/CD

The screenshot functionality is designed to work in CI/CD environments:

- Uses headless browsers by default
- Automatic WebDriver management
- No GUI dependencies
- Configurable output directories

### Example CI Configuration

```yaml
# GitHub Actions example
- name: Take Screenshots
  run: |
    mvn spring-boot:run -pl apex-playground &
    sleep 30  # Wait for startup
    ./scripts/take-playground-screenshots.sh
    
- name: Upload Screenshots
  uses: actions/upload-artifact@v3
  with:
    name: playground-screenshots
    path: screenshots/
```

## Advanced Usage

### Custom Screenshot Scenarios

Create custom screenshot scenarios by extending the utility classes or creating new test methods with specific data combinations.

### Element-Specific Screenshots

Capture individual UI components:

```java
// Screenshot specific elements
String[] elements = {"sourceDataEditor", "yamlRulesEditor", "processBtn"};
for (String elementId : elements) {
    util.takeScreenshot(browser, ELEMENT_SPECIFIC, elementId);
}
```

### Workflow Documentation

Use the screenshot functionality to create step-by-step documentation of playground workflows.

## Support

For issues or questions about the screenshot functionality:

1. Check the console output for error messages
2. Verify all prerequisites are met
3. Test with a simple scenario first
4. Check browser compatibility
