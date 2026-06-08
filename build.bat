@echo off
setlocal enabledelayedexpansion

REM Check if Gradle wrapper exists
if not exist "gradlew.bat" (
    echo Error: gradlew.bat not found. Please ensure Gradle wrapper is initialized.
    exit /b 1
)

REM Build the project using Gradle
call gradlew.bat build shadowJar

REM Check if build was successful
if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo Build completed successfully!
echo Artifacts are located in:
echo - JavaIM\build\libs\JavaIM-1.0-SNAPSHOT.jar (runnable shaded JAR)
echo - JavaIM\build\libs\JavaIM-1.0-SNAPSHOT-plain.jar (plain JAR)
