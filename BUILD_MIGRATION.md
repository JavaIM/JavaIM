# Maven to Gradle Migration Guide

## Overview
This document outlines the migration of JavaIM project from Apache Maven to Gradle.

## Changes Made

### 1. Build Configuration Files Created
- **settings.gradle**: Configures the multi-module project structure
  - Root module: JavaIM-Parent
  - Sub-modules: LibraryTransformers, JavaIM

- **build.gradle** (root): Defines global build configuration
  - Java version: 17
  - Common repositories (mavenCentral)
  - UTF-8 encoding

- **JavaIM/build.gradle**: Main application module
  - All dependencies from original pom.xml
  - Shadow JAR plugin configuration for creating shaded JARs
  - Source JAR generation
  - Publishing configuration

- **LibraryTransformers/build.gradle**: Transformer library module
  - Maven shade plugin dependency
  - Minimal configuration

### 2. Maven Files Removed
- D:\IdeaProjects\JavaIM\pom.xml
- D:\IdeaProjects\JavaIM\JavaIM\pom.xml
- D:\IdeaProjects\JavaIM\LibraryTransformers\pom.xml

### 3. Gradle Wrapper Setup
- Created gradlew (Unix/Linux script)
- Created gradlew.bat (Windows batch script)
- Created gradle/wrapper/gradle-wrapper.properties (configured for Gradle 8.5)
- Gradle wrapper JAR already existed

### 4. Build Scripts Updated
- **build.bat**: Updated to use gradlew.bat instead of mvn
- **build.sh**: Updated to use ./gradlew instead of mvn

### 5. Docker Configuration Updated
- **dockerinstall.sh**: Removed Maven installation, switched to Gradle wrapper
  - Gradle wrapper is automatically downloaded on first use

### 6. Project Configuration Added
- **gradle.properties**: Added Gradle configuration for:
  - JVM memory settings
  - Parallel builds
  - Build cache
  - File encoding

### 7. Git Configuration Updated
- **.gitignore**: Added Maven-related exclusions (target/ directory, etc.)

### 8. Maven Output Directories Cleaned
- Deleted D:\IdeaProjects\JavaIM\JavaIM\target
- Deleted D:\IdeaProjects\JavaIM\LibraryTransformers\target

## Build Output

### Standard Build
```bash
./gradlew build
```
Produces:
- JavaIM/build/libs/JavaIM-1.0-SNAPSHOT.jar (runnable shaded JAR)
- JavaIM/build/libs/JavaIM-1.0-SNAPSHOT-plain.jar (plain JAR)

### Shaded JAR Build
```bash
./gradlew shadowJar
```
Produces:
- JavaIM/build/libs/JavaIM-1.0-SNAPSHOT.jar (with all dependencies included)

### Full Build
```bash
./gradlew build shadowJar
```

## Dependencies Migrated

All Maven dependencies have been converted to Gradle format:

### Database
- org.xerial:sqlite-jdbc:3.41.2.2
- com.mysql:mysql-connector-j:8.0.32

### Apache Commons
- commons-io:commons-io:2.11.0
- commons-codec:commons-codec:1.15

### Logging
- org.apache.logging.log4j:log4j-api:2.20.0
- org.apache.logging.log4j:log4j-core:2.20.0

### Utilities & Libraries
- cn.hutool:hutool-crypto:5.8.18
- com.google.code.gson:gson:2.10.1
- org.jetbrains:annotations:24.0.1

### JavaFX (platform-specific)
- javafx-base 17.0.1 (linux, win)
- javafx-controls 17.0.1 (linux, win)
- javafx-fxml 17.0.1 (linux, win)
- javafx-graphics 17.0.1 (linux, win)

### UI & Graphics
- com.jfoenix:jfoenix:9.0.10
- de.jensd:fontawesomefx:8.9

### Build Extensions
- io.github.edwgiz:log4j-maven-shade-plugin-extensions:2.20.0

## Build Verification

Build test results:
```
BUILD SUCCESSFUL in 12s
8 actionable tasks: 8 executed
```

Output artifacts:
- JavaIM-1.0-SNAPSHOT.jar (runnable shaded JAR)
- JavaIM-1.0-SNAPSHOT-plain.jar (plain JAR)

## Usage

### Windows
```batch
.\gradlew.bat build
.\gradlew.bat shadowJar
```

### Unix/Linux/Mac
```bash
./gradlew build
./gradlew shadowJar
```

### IntelliJ IDEA
The project is fully compatible with IntelliJ IDEA's Gradle support. The IDE will automatically detect and import the Gradle project configuration.

## Advantages of Gradle Migration

1. **Performance**: Gradle's incremental builds and daemon process make builds faster
2. **Flexibility**: DSL (Domain Specific Language) provides more powerful configuration
3. **Caching**: Built-in build cache reduces build times for CI/CD pipelines
4. **Parallel Builds**: Automatically build multiple modules in parallel
5. **Wrapper**: Consistent build environment across all machines

## Notes

- The custom LibraryTransformers is still supported in Gradle
- All Maven Shade Plugin configuration has been converted to Gradle Shadow Plugin
- Project uses Java 17 with UTF-8 encoding as before
- Docker builds now use Gradle wrapper instead of Maven

