#!/bin/bash
set -e

# Check if Gradle wrapper exists
if [ ! -f "gradlew" ]; then
    echo "Error: gradlew not found. Please ensure Gradle wrapper is initialized."
    exit 1
fi

# Make gradlew executable
chmod +x gradlew

# Build the project using Gradle
./gradlew build shadowJar

# Check if build was successful
if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Build completed successfully!"
echo "Artifacts are located in:"
echo "- JavaIM/build/libs/JavaIM-1.0-SNAPSHOT.jar (runnable shaded JAR)"
echo "- JavaIM/build/libs/JavaIM-1.0-SNAPSHOT-plain.jar (plain JAR)"
