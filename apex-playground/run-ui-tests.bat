@echo off
echo Running APEX Playground UI Tests...
echo.
echo These tests require Chrome browser and will run in headless mode.
echo Make sure the application is not already running on port 8081.
echo.

mvn test -Dtest="**/ui/**" -pl apex-playground

echo.
echo UI Tests completed.
pause
