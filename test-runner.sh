#!/bin/bash

echo "========================================"
echo "Running DevOps Panic Ribbon Tests"
echo "========================================"
echo

# Compile test class
echo "Compiling test class..."
javac -d build/classes -sourcepath "src/main/java:src/test/java" -cp build/classes src/test/java/com/devops/panicribbon/ApplicationTest.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo
echo "Running tests..."
echo
java -cp build/classes com.devops.panicribbon.ApplicationTest

