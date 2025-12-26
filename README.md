# DevOps Panic Ribbon

A production-grade, ultra-lightweight Java AWT desktop application for monitoring microservice health. The application displays a 12-pixel-wide vertical ribbon on the right edge of your screen, with each segment representing a service's health status.

## Features

- **Minimalist UI**: 12px wide vertical ribbon, always-on-top, 80% opacity
- **Real-time Health Monitoring**: Polls service health endpoints every 10 seconds
- **Visual Status Indicators**: Green segments for healthy services (HTTP 200), Red for unhealthy
- **Interactive Controls**:
  - **Hover**: Shows service name and latency in a tooltip
  - **Left-Click on Red Segment**: Executes the restart script for that service
  - **Right-Click**: Context menu with "Refresh Now", "View Logs", and "Exit"
- **Low Resource Usage**: Optimized for < 15MB RAM usage
- **Java 21 Virtual Threads**: Non-blocking health checks using virtual threads
- **Logging**: All actions logged to `panic.log` with timestamps

## Requirements

- **Java 21** or higher (for virtual threads support)
- No external dependencies (uses only standard Java libraries)

## Configuration

Edit `services.json` in the root directory to configure your services:

```json
{
  "services": [
    {
      "name": "Service Name",
      "healthCheckUrl": "http://localhost:8080/health",
      "restartScriptPath": "restart-service.bat"
    }
  ]
}
```

### Configuration Fields

- **name**: Display name for the service (shown in tooltip)
- **healthCheckUrl**: HTTP endpoint to check for service health (expects HTTP 200 for healthy)
- **restartScriptPath**: Path to the script/batch file to execute when clicking a red segment
  - Windows: Use `.bat` or `.cmd` files
  - Linux/Mac: Use `.sh` scripts (ensure they have execute permissions)

## Compilation

### Option 1: Manual Compilation (Recommended for Ultra-Lightweight)

1. **Compile the Java source**:
   ```bash
   javac -d build/classes src/main/java/com/devops/panicribbon/Main.java
   ```

2. **Create a JAR file**:
   ```bash
   jar cvfe panic-ribbon.jar com.devops.panicribbon.Main -C build/classes .
   ```

### Option 2: Using Maven

If you prefer using Maven, create a `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.devops</groupId>
    <artifactId>panic-ribbon</artifactId>
    <version>1.0.0</version>
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.devops.panicribbon.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Then compile and package:
```bash
mvn clean compile package
```

The JAR will be created in `target/panic-ribbon-1.0.0.jar`.

## Running the Application

1. Ensure `services.json` is in the same directory as the JAR file
2. Run the application:
   ```bash
   java -jar panic-ribbon.jar
   ```

Or if running from compiled classes:
```bash
java -cp build/classes com.devops.panicribbon.Main
```

## Usage

1. **Monitor Services**: The ribbon automatically appears on the right edge of your primary monitor
2. **Check Status**: Hover over any segment to see the service name and current latency
3. **Restart Service**: Left-click on a red (unhealthy) segment to execute its restart script
4. **Context Menu**: Right-click any segment for additional options:
   - **Refresh Now**: Immediately check the service health
   - **View Logs**: Logs the action (extend this to open log viewer)
   - **Exit**: Close the application

## Logging

All actions are logged to `panic.log` in the application directory with timestamps:
- Service health check results
- Restart script executions
- User interactions
- Errors and exceptions

## Technical Details

- **UI Framework**: Java AWT (no Swing/JavaFX)
- **HTTP Client**: `java.net.http.HttpClient` (Java 11+)
- **Concurrency**: Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`)
- **Window Properties**:
  - Borderless and undecorated
  - Always-on-top
  - Focusable: false (won't steal focus from your IDE)
  - 80% opacity
  - 12 pixels wide, full screen height

## Troubleshooting

### Window doesn't appear
- Check that Java 21+ is installed: `java -version`
- Ensure no other application is blocking the right edge of the screen
- Check `panic.log` for error messages

### Health checks always show red
- Verify the `healthCheckUrl` endpoints are accessible
- Check network connectivity
- Ensure services are running and responding with HTTP 200

### Restart scripts don't execute
- Verify script paths in `services.json` are correct
- Ensure scripts have proper permissions (Linux/Mac: `chmod +x script.sh`)
- Check `panic.log` for execution errors

### Default services.json created
- If `services.json` is missing, the application creates a default one with a "Localhost" service
- Edit the file and restart the application

## License

This project is provided as-is for DevOps monitoring purposes.

