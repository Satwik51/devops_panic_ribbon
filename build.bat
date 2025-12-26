@echo off
echo Building DevOps Panic Ribbon...
echo.

REM Create build directory
if not exist "build\classes" mkdir build\classes

REM Compile Java source
echo Compiling Java source...
javac -d build\classes -sourcepath src\main\java src\main\java\com\devops\panicribbon\Main.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b 1
)

REM Create JAR file
echo Creating JAR file...
jar cvfe panic-ribbon.jar com.devops.panicribbon.Main -C build\classes .

if %ERRORLEVEL% NEQ 0 (
    echo JAR creation failed!
    exit /b 1
)

echo.
echo Build successful! JAR file created: panic-ribbon.jar
echo Run with: java -jar panic-ribbon.jar
pause

