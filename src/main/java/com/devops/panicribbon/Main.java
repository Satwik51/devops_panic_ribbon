package com.devops.panicribbon;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

// Simple JSON parser to avoid external dependencies

public class Main {
    private static final int RIBBON_WIDTH = 12;
    private static final int POLL_INTERVAL_SECONDS = 10;
    private static final float OPACITY = 0.8f;
    private static final String SERVICES_JSON = "services.json";
    private static final String LOG_FILE = "panic.log";
    
    private Frame frame;
    private List<Service> services = new ArrayList<>();
    private List<ServiceStatus> serviceStatuses = new ArrayList<>();
    private ScheduledExecutorService scheduler;
    private HttpClient httpClient;
    private GraphicsEnvironment ge;
    private GraphicsDevice gd;
    private Rectangle screenBounds;
    private int segmentHeight;
    private ServiceStatus hoveredService = null;
    private Frame tooltipFrame = null;
    
    public static void main(String[] args) {
        new Main().start();
    }
    
    public void start() {
        // Initialize graphics environment
        ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        gd = ge.getDefaultScreenDevice();
        screenBounds = gd.getDefaultConfiguration().getBounds();
        
        // Load services configuration
        loadServices();
        
        // Initialize HTTP client
        httpClient = HttpClient.newHttpClient();
        
        // Create and configure the main window
        createWindow();
        
        // Start health check polling
        startHealthChecks();
        
        // Keep the application running
        frame.setVisible(true);
    }
    
    private void loadServices() {
        Path servicesPath = Paths.get(SERVICES_JSON);
        
        if (!Files.exists(servicesPath)) {
            log("services.json not found, creating default configuration");
            createDefaultServicesJson();
        }
        
        try {
            String content = Files.readString(servicesPath);
            SimpleJsonParser parser = new SimpleJsonParser(content);
            List<ServiceConfig> serviceConfigs = parser.parseServices();
            
            services.clear();
            serviceStatuses.clear();
            
            for (ServiceConfig config : serviceConfigs) {
                Service service = new Service(
                    config.name,
                    config.healthCheckUrl,
                    config.restartScriptPath
                );
                services.add(service);
                serviceStatuses.add(new ServiceStatus(service));
            }
            
            log("Loaded " + services.size() + " service(s)");
        } catch (Exception e) {
            log("Error loading services.json: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (services.isEmpty()) {
            // Fallback: create a dummy service
            Service dummy = new Service("Localhost", "http://localhost:8080/health", "echo 'No restart script'");
            services.add(dummy);
            serviceStatuses.add(new ServiceStatus(dummy));
        }
        
        // Calculate segment height
        segmentHeight = screenBounds.height / services.size();
    }
    
    private void createDefaultServicesJson() {
        try {
            String defaultJson = "{\n" +
                "  \"services\": [\n" +
                "    {\n" +
                "      \"name\": \"Localhost\",\n" +
                "      \"healthCheckUrl\": \"http://localhost:8080/health\",\n" +
                "      \"restartScriptPath\": \"echo 'No restart script configured'\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
            
            Files.writeString(Paths.get(SERVICES_JSON), defaultJson);
            log("Created default services.json");
        } catch (Exception e) {
            log("Error creating default services.json: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createWindow() {
        frame = new Frame();
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setFocusable(false);
        frame.setResizable(false);
        frame.setSize(RIBBON_WIDTH, screenBounds.height);
        frame.setLocation(screenBounds.x + screenBounds.width - RIBBON_WIDTH, screenBounds.y);
        
        // Set opacity using Java 9+ API (Frame extends Window which has setOpacity)
        try {
            frame.setOpacity(OPACITY);
        } catch (Exception e) {
            // Fallback for older Java versions or if opacity not supported
            log("Could not set opacity: " + e.getMessage() + " (continuing without opacity)");
        }
        
        // Custom panel for drawing
        Panel panel = new Panel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                drawRibbon(g);
            }
        };
        
        panel.setBackground(Color.BLACK);
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hideTooltip();
            }
        });
        
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e);
            }
        });
        
        frame.add(panel);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
    }
    
    private void drawRibbon(Graphics g) {
        int y = 0;
        for (int i = 0; i < serviceStatuses.size(); i++) {
            ServiceStatus status = serviceStatuses.get(i);
            Color color = status.isHealthy() ? Color.GREEN : Color.RED;
            
            g.setColor(color);
            g.fillRect(0, y, RIBBON_WIDTH, segmentHeight);
            
            // Draw border between segments
            if (i < serviceStatuses.size() - 1) {
                g.setColor(Color.BLACK);
                g.drawLine(0, y + segmentHeight - 1, RIBBON_WIDTH, y + segmentHeight - 1);
            }
            
            y += segmentHeight;
        }
    }
    
    private void handleMouseMove(MouseEvent e) {
        int y = e.getY();
        int segmentIndex = y / segmentHeight;
        
        if (segmentIndex >= 0 && segmentIndex < serviceStatuses.size()) {
            ServiceStatus status = serviceStatuses.get(segmentIndex);
            hoveredService = status;
            showTooltip(e.getXOnScreen(), e.getYOnScreen(), status);
        } else {
            hideTooltip();
        }
    }
    
    private void showTooltip(int x, int y) {
        if (hoveredService == null) return;
        showTooltip(x, y, hoveredService);
    }
    
    private void showTooltip(int x, int y, ServiceStatus status) {
        hideTooltip();
        
        String text = status.getService().getName() + 
                     "\nLatency: " + (status.getLatency() >= 0 ? status.getLatency() + "ms" : "N/A");
        
        tooltipFrame = new Frame();
        tooltipFrame.setUndecorated(true);
        tooltipFrame.setAlwaysOnTop(true);
        tooltipFrame.setFocusable(false);
        
        Label label = new Label(text);
        label.setBackground(new Color(255, 255, 200));
        label.setForeground(Color.BLACK);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        
        tooltipFrame.add(label);
        tooltipFrame.pack();
        tooltipFrame.setLocation(x + 15, y - 10);
        tooltipFrame.setVisible(true);
    }
    
    private void hideTooltip() {
        if (tooltipFrame != null) {
            tooltipFrame.dispose();
            tooltipFrame = null;
        }
        hoveredService = null;
    }
    
    private void handleMouseClick(MouseEvent e) {
        int y = e.getY();
        int segmentIndex = y / segmentHeight;
        
        if (segmentIndex >= 0 && segmentIndex < serviceStatuses.size()) {
            ServiceStatus status = serviceStatuses.get(segmentIndex);
            
            if (e.getButton() == MouseEvent.BUTTON1) {
                // Left click - execute restart script if unhealthy
                if (!status.isHealthy()) {
                    executeRestartScript(status.getService());
                }
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                // Right click - show popup menu
                showPopupMenu(e.getXOnScreen(), e.getYOnScreen(), status);
            }
        }
    }
    
    private void showPopupMenu(int x, int y, ServiceStatus status) {
        PopupMenu popup = new PopupMenu();
        
        MenuItem refreshItem = new MenuItem("Refresh Now");
        refreshItem.addActionListener(ae -> {
            log("Manual refresh requested for: " + status.getService().getName());
            checkServiceHealth(status);
        });
        popup.add(refreshItem);
        
        MenuItem logsItem = new MenuItem("View Logs");
        logsItem.addActionListener(ae -> {
            log("View logs requested for: " + status.getService().getName());
            // In a real implementation, you might open a log viewer
            // For now, just log the action
        });
        popup.add(logsItem);
        
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(ae -> shutdown());
        popup.add(exitItem);
        
        frame.add(popup);
        popup.show(frame, x - frame.getX(), y - frame.getY());
    }
    
    private void executeRestartScript(Service service) {
        log("Executing restart script for: " + service.getName() + " (" + service.getRestartScriptPath() + ")");
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("win")) {
                // Windows
                pb = new ProcessBuilder("cmd.exe", "/c", service.getRestartScriptPath());
            } else {
                // Unix/Linux/Mac
                pb = new ProcessBuilder("sh", "-c", service.getRestartScriptPath());
            }
            
            pb.directory(new File(System.getProperty("user.dir")));
            Process process = pb.start();
            
            // Don't wait for the process to complete (fire and forget)
            Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
                try {
                    int exitCode = process.waitFor();
                    log("Restart script completed for " + service.getName() + " with exit code: " + exitCode);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log("Restart script interrupted for: " + service.getName());
                }
            });
            
        } catch (Exception e) {
            log("Error executing restart script for " + service.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startHealthChecks() {
        scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            for (ServiceStatus status : serviceStatuses) {
                Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
                    checkServiceHealth(status);
                });
            }
        }, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    
    private void checkServiceHealth(ServiceStatus status) {
        long startTime = System.currentTimeMillis();
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(status.getService().getHealthCheckUrl()))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;
            
            boolean healthy = response.statusCode() == 200;
            status.update(healthy, latency);
            
            log("Health check: " + status.getService().getName() + 
                " - " + (healthy ? "HEALTHY" : "UNHEALTHY") + 
                " (" + response.statusCode() + ") - " + latency + "ms");
            
        } catch (java.net.http.HttpTimeoutException e) {
            status.update(false, -1);
            log("Health check timeout: " + status.getService().getName());
        } catch (Exception e) {
            status.update(false, -1);
            log("Health check error: " + status.getService().getName() + " - " + e.getMessage());
        }
        
        // Repaint on AWT Event Dispatch Thread
        EventQueue.invokeLater(() -> {
            frame.repaint();
        });
    }
    
    private void shutdown() {
        log("Shutting down application");
        if (scheduler != null) {
            scheduler.shutdown();
        }
        hideTooltip();
        if (frame != null) {
            frame.dispose();
        }
        System.exit(0);
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String logMessage = "[" + timestamp + "] " + message;
        
        System.out.println(logMessage);
        
        try {
            Files.writeString(
                Paths.get(LOG_FILE),
                logMessage + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }
    
    // Inner classes (package-private for testing)
    static class ServiceConfig {
        String name;
        String healthCheckUrl;
        String restartScriptPath;
    }
    
    static class SimpleJsonParser {
        private String json;
        private int pos = 0;
        
        public SimpleJsonParser(String json) {
            this.json = json;
        }
        
        public List<ServiceConfig> parseServices() {
            List<ServiceConfig> services = new ArrayList<>();
            skipWhitespace();
            
            if (pos >= json.length() || json.charAt(pos) != '{') {
                throw new RuntimeException("Expected '{' at start of JSON");
            }
            pos++;
            
            skipWhitespace();
            if (!consume("\"services\"")) {
                throw new RuntimeException("Expected 'services' key");
            }
            skipWhitespace();
            if (pos >= json.length() || json.charAt(pos) != ':') {
                throw new RuntimeException("Expected ':' after 'services'");
            }
            pos++;
            skipWhitespace();
            
            if (pos >= json.length() || json.charAt(pos) != '[') {
                throw new RuntimeException("Expected '[' for services array");
            }
            pos++;
            skipWhitespace();
            
            while (pos < json.length() && json.charAt(pos) != ']') {
                if (json.charAt(pos) == '{') {
                    ServiceConfig config = parseServiceObject();
                    services.add(config);
                }
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                }
            }
            
            return services;
        }
        
        private ServiceConfig parseServiceObject() {
            ServiceConfig config = new ServiceConfig();
            pos++; // skip '{'
            skipWhitespace();
            
            while (pos < json.length() && json.charAt(pos) != '}') {
                String key = parseString();
                skipWhitespace();
                if (pos >= json.length() || json.charAt(pos) != ':') {
                    throw new RuntimeException("Expected ':' after key");
                }
                pos++;
                skipWhitespace();
                String value = parseString();
                
                switch (key) {
                    case "name":
                        config.name = value;
                        break;
                    case "healthCheckUrl":
                        config.healthCheckUrl = value;
                        break;
                    case "restartScriptPath":
                        config.restartScriptPath = value;
                        break;
                }
                
                skipWhitespace();
                if (pos < json.length() && json.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                }
            }
            if (pos < json.length()) {
                pos++; // skip '}'
            }
            skipWhitespace();
            
            return config;
        }
        
        private String parseString() {
            if (pos >= json.length() || json.charAt(pos) != '"') {
                throw new RuntimeException("Expected '\"' at start of string");
            }
            pos++;
            int start = pos;
            while (pos < json.length() && json.charAt(pos) != '"') {
                if (json.charAt(pos) == '\\' && pos + 1 < json.length()) {
                    pos++; // skip escaped character
                }
                pos++;
            }
            if (pos >= json.length()) {
                throw new RuntimeException("Unterminated string");
            }
            String value = json.substring(start, pos);
            pos++; // skip closing '"'
            skipWhitespace();
            return value;
        }
        
        private boolean consume(String str) {
            if (pos + str.length() <= json.length() && 
                json.substring(pos, pos + str.length()).equals(str)) {
                pos += str.length();
                return true;
            }
            return false;
        }
        
        private void skipWhitespace() {
            while (pos < json.length() && 
                   (json.charAt(pos) == ' ' || json.charAt(pos) == '\n' || 
                    json.charAt(pos) == '\r' || json.charAt(pos) == '\t')) {
                pos++;
            }
        }
    }
    
    static class Service {
        private final String name;
        private final String healthCheckUrl;
        private final String restartScriptPath;
        
        public Service(String name, String healthCheckUrl, String restartScriptPath) {
            this.name = name;
            this.healthCheckUrl = healthCheckUrl;
            this.restartScriptPath = restartScriptPath;
        }
        
        public String getName() { return name; }
        public String getHealthCheckUrl() { return healthCheckUrl; }
        public String getRestartScriptPath() { return restartScriptPath; }
    }
    
    static class ServiceStatus {
        private final Service service;
        private final AtomicReference<Boolean> healthy = new AtomicReference<>(false);
        private final AtomicReference<Long> latency = new AtomicReference<>(-1L);
        
        public ServiceStatus(Service service) {
            this.service = service;
        }
        
        public Service getService() { return service; }
        
        public boolean isHealthy() {
            return healthy.get();
        }
        
        public long getLatency() {
            return latency.get();
        }
        
        public void update(boolean healthy, long latency) {
            this.healthy.set(healthy);
            this.latency.set(latency);
        }
    }
}

