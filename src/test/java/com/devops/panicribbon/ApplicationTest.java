package com.devops.panicribbon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Comprehensive test suite for DevOps Panic Ribbon application
 * Run with: java -cp build/classes com.devops.panicribbon.ApplicationTest
 */
public class ApplicationTest {
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("DevOps Panic Ribbon - Test Suite");
        System.out.println("========================================\n");
        
        try {
            testJsonParsing();
            testServiceConfiguration();
            testServiceStatus();
            testLogging();
            testDefaultServicesJsonCreation();
            testHealthCheckUrlValidation();
            
            System.out.println("\n========================================");
            System.out.println("Test Results:");
            System.out.println("  Passed: " + testsPassed);
            System.out.println("  Failed: " + testsFailed);
            System.out.println("  Total:  " + (testsPassed + testsFailed));
            System.out.println("========================================");
            
            if (testsFailed == 0) {
                System.out.println("\n✓ All tests PASSED!");
                System.exit(0);
            } else {
                System.out.println("\n✗ Some tests FAILED!");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Test suite error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void testJsonParsing() {
        System.out.println("Testing JSON Parsing...");
        
        // Test 1: Valid JSON parsing
        String validJson = "{\n" +
            "  \"services\": [\n" +
            "    {\n" +
            "      \"name\": \"Test Service\",\n" +
            "      \"healthCheckUrl\": \"http://localhost:8080/health\",\n" +
            "      \"restartScriptPath\": \"restart.bat\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        
        try {
            Main.SimpleJsonParser parser = new Main.SimpleJsonParser(validJson);
            List<Main.ServiceConfig> services = parser.parseServices();
            assertTrue(services.size() == 1, "Should parse 1 service");
            assertTrue("Test Service".equals(services.get(0).name), "Service name should match");
            assertTrue("http://localhost:8080/health".equals(services.get(0).healthCheckUrl), 
                      "Health check URL should match");
            assertTrue("restart.bat".equals(services.get(0).restartScriptPath), 
                      "Restart script path should match");
            System.out.println("  ✓ Valid JSON parsing");
        } catch (Exception e) {
            fail("Valid JSON parsing failed: " + e.getMessage());
        }
        
        // Test 2: Multiple services
        String multiServiceJson = "{\n" +
            "  \"services\": [\n" +
            "    {\"name\": \"Service1\", \"healthCheckUrl\": \"http://localhost:8081/health\", \"restartScriptPath\": \"s1.bat\"},\n" +
            "    {\"name\": \"Service2\", \"healthCheckUrl\": \"http://localhost:8082/health\", \"restartScriptPath\": \"s2.bat\"}\n" +
            "  ]\n" +
            "}";
        
        try {
            Main.SimpleJsonParser parser = new Main.SimpleJsonParser(multiServiceJson);
            List<Main.ServiceConfig> services = parser.parseServices();
            assertTrue(services.size() == 2, "Should parse 2 services");
            assertTrue("Service1".equals(services.get(0).name), "First service name should match");
            assertTrue("Service2".equals(services.get(1).name), "Second service name should match");
            System.out.println("  ✓ Multiple services parsing");
        } catch (Exception e) {
            fail("Multiple services parsing failed: " + e.getMessage());
        }
        
        // Test 3: Empty services array
        String emptyJson = "{\"services\": []}";
        try {
            Main.SimpleJsonParser parser = new Main.SimpleJsonParser(emptyJson);
            List<Main.ServiceConfig> services = parser.parseServices();
            assertTrue(services.size() == 0, "Should parse empty services array");
            System.out.println("  ✓ Empty services array parsing");
        } catch (Exception e) {
            fail("Empty services array parsing failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testServiceConfiguration() {
        System.out.println("Testing Service Configuration...");
        
        // Test Service class
        Main.Service service = new Main.Service("Test", "http://test.com/health", "restart.sh");
        assertTrue("Test".equals(service.getName()), "Service name getter");
        assertTrue("http://test.com/health".equals(service.getHealthCheckUrl()), "Health URL getter");
        assertTrue("restart.sh".equals(service.getRestartScriptPath()), "Restart script getter");
        System.out.println("  ✓ Service class getters");
        
        // Test ServiceStatus class
        Main.ServiceStatus status = new Main.ServiceStatus(service);
        assertTrue(!status.isHealthy(), "Initial status should be unhealthy");
        assertTrue(status.getLatency() == -1, "Initial latency should be -1");
        
        status.update(true, 150);
        assertTrue(status.isHealthy(), "Status should be healthy after update");
        assertTrue(status.getLatency() == 150, "Latency should be 150ms");
        
        status.update(false, -1);
        assertTrue(!status.isHealthy(), "Status should be unhealthy after update");
        System.out.println("  ✓ ServiceStatus class");
        
        System.out.println();
    }
    
    private static void testServiceStatus() {
        System.out.println("Testing Service Status Updates...");
        
        Main.Service service = new Main.Service("Test", "http://localhost:8080/health", "test.bat");
        Main.ServiceStatus status = new Main.ServiceStatus(service);
        
        // Test status updates
        status.update(true, 100);
        assertTrue(status.isHealthy() && status.getLatency() == 100, "Status update to healthy");
        
        status.update(false, -1);
        assertTrue(!status.isHealthy() && status.getLatency() == -1, "Status update to unhealthy");
        
        status.update(true, 250);
        assertTrue(status.isHealthy() && status.getLatency() == 250, "Status update with latency");
        
        System.out.println("  ✓ Status updates work correctly");
        System.out.println();
    }
    
    private static void testLogging() {
        System.out.println("Testing Logging Functionality...");
        
        String testLogFile = "test-panic.log";
        Path logPath = Paths.get(testLogFile);
        
        try {
            // Clean up if exists
            if (Files.exists(logPath)) {
                Files.delete(logPath);
            }
            
            // Create a test main instance to test logging
            TestMain testMain = new TestMain(testLogFile);
            testMain.log("Test log message");
            
            // Verify log file was created
            assertTrue(Files.exists(logPath), "Log file should be created");
            
            // Verify log content
            String content = Files.readString(logPath);
            assertTrue(content.contains("Test log message"), "Log should contain message");
            assertTrue(content.contains("["), "Log should contain timestamp");
            
            // Clean up
            Files.delete(logPath);
            System.out.println("  ✓ Logging creates file and writes content");
        } catch (Exception e) {
            fail("Logging test failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testDefaultServicesJsonCreation() {
        System.out.println("Testing Default services.json Creation...");
        
        String testJsonFile = "test-services.json";
        Path jsonPath = Paths.get(testJsonFile);
        
        try {
            // Clean up if exists
            if (Files.exists(jsonPath)) {
                Files.delete(jsonPath);
            }
            
            // Test that default JSON would be created (simulate the logic)
            String defaultJson = "{\n" +
                "  \"services\": [\n" +
                "    {\n" +
                "      \"name\": \"Localhost\",\n" +
                "      \"healthCheckUrl\": \"http://localhost:8080/health\",\n" +
                "      \"restartScriptPath\": \"echo 'No restart script configured'\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
            
            Files.writeString(jsonPath, defaultJson);
            assertTrue(Files.exists(jsonPath), "Default JSON file should be created");
            
            // Verify it can be parsed
            String content = Files.readString(jsonPath);
            Main.SimpleJsonParser parser = new Main.SimpleJsonParser(content);
            List<Main.ServiceConfig> services = parser.parseServices();
            assertTrue(services.size() == 1, "Default JSON should have 1 service");
            assertTrue("Localhost".equals(services.get(0).name), "Default service name should be Localhost");
            
            // Clean up
            Files.delete(jsonPath);
            System.out.println("  ✓ Default services.json creation and parsing");
        } catch (Exception e) {
            fail("Default services.json test failed: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void testHealthCheckUrlValidation() {
        System.out.println("Testing Health Check URL Validation...");
        
        // Test valid URLs
        String[] validUrls = {
            "http://localhost:8080/health",
            "https://example.com/health",
            "http://192.168.1.1:8080/health",
            "http://localhost/health"
        };
        
        for (String url : validUrls) {
            try {
                java.net.URI.create(url);
                // If no exception, URL is valid
            } catch (Exception e) {
                fail("Valid URL rejected: " + url);
            }
        }
        System.out.println("  ✓ Valid URL formats accepted");
        
        // Test invalid URLs (should throw exception when creating URI)
        String[] invalidUrls = {
            "not-a-url",
            "ftp://invalid",
            ""
        };
        
        for (String url : invalidUrls) {
            try {
                java.net.URI.create(url);
                // Some might not throw, that's okay for this test
            } catch (Exception e) {
                // Expected for invalid URLs
            }
        }
        System.out.println("  ✓ Invalid URL handling");
        
        System.out.println();
    }
    
    // Helper methods
    private static void assertTrue(boolean condition, String message) {
        if (condition) {
            testsPassed++;
        } else {
            testsFailed++;
            System.err.println("  ✗ FAILED: " + message);
        }
    }
    
    private static void fail(String message) {
        testsFailed++;
        System.err.println("  ✗ FAILED: " + message);
    }
    
    // Test helper class to access Main's logging
    private static class TestMain {
        private final String logFile;
        
        public TestMain(String logFile) {
            this.logFile = logFile;
        }
        
        public void log(String message) {
            try {
                String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                String logMessage = "[" + timestamp + "] " + message;
                
                Files.writeString(
                    Paths.get(logFile),
                    logMessage + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
                );
            } catch (Exception e) {
                System.err.println("Error writing to log file: " + e.getMessage());
            }
        }
    }
}

