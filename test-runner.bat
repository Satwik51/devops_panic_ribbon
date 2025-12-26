@echo off
echo ========================================
echo Running DevOps Panic Ribbon Tests
echo ========================================
echo.

REM Compile test class
echo Compiling test class...
javac -d build\classes -sourcepath src\main\java;src\test\java -cp build\classes src\test\java\com\devops\panicribbon\ApplicationTest.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running tests...
echo.
java -cp build\classes com.devops.panicribbon.ApplicationTest

echo.
pause

