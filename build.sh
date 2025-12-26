#!/bin/bash

echo "Building DevOps Panic Ribbon..."
echo

# Create build directory
mkdir -p build/classes

# Compile Java source
echo "Compiling Java source..."
javac -d build/classes -sourcepath src/main/java src/main/java/com/devops/panicribbon/Main.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Create JAR file
echo "Creating JAR file..."
jar cvfe panic-ribbon.jar com.devops.panicribbon.Main -C build/classes .

if [ $? -ne 0 ]; then
    echo "JAR creation failed!"
    exit 1
fi

echo
echo "Build successful! JAR file created: panic-ribbon.jar"
echo "Run with: java -jar panic-ribbon.jar"

