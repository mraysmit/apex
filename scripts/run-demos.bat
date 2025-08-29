@echo off
echo Running APEX Demo Suite...
echo.

echo === Running Bootstrap Demos ===
echo.

echo 1. Running Custody Auto-Repair Bootstrap Demo...
java -cp "apex-demo/target/apex-demo-1.0-SNAPSHOT-jar-with-dependencies.jar" dev.mars.apex.demo.enrichment.CustodyAutoRepairBootstrap
echo.

echo 2. Running Commodity Swap Validation Bootstrap Demo...
java -cp "apex-demo/target/apex-demo-1.0-SNAPSHOT-jar-with-dependencies.jar" dev.mars.apex.demo.validation.CommoditySwapValidationBootstrap
echo.

echo === Running Main Demo Runner ===
echo.

echo 3. Running All Demos via DemoRunner...
java -cp "apex-demo/target/apex-demo-1.0-SNAPSHOT-jar-with-dependencies.jar" dev.mars.apex.demo.runners.DemoRunner all
echo.

echo === Demo Execution Completed ===
pause
