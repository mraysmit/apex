#!/bin/bash

# SpEL Rules Engine REST API Startup Script
# This script builds and starts the REST API server

set -e

echo "🚀 Starting SpEL Rules Engine REST API..."
echo

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or later."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17 or later is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version: $(java -version 2>&1 | head -n 1)"
echo "✅ Maven version: $(mvn -version | head -n 1)"
echo

# Build the project
echo "🔨 Building the project..."
mvn clean package -DskipTests -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the error messages above."
    exit 1
fi

echo "✅ Build completed successfully"
echo

# Start the REST API
echo "🌐 Starting REST API server..."
echo "📍 Server will be available at: http://localhost:8080"
echo "📚 Swagger UI will be available at: http://localhost:8080/swagger-ui.html"
echo "🏥 Health check: http://localhost:8080/actuator/health"
echo
echo "Press Ctrl+C to stop the server"
echo

cd rules-engine-rest-api
java -jar target/rules-engine-rest-api-1.0-SNAPSHOT.jar
