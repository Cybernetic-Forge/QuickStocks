package com.example.quickstocks.infrastructure.web;

import com.example.quickstocks.infrastructure.db.Db;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Simple health check HTTP server for database connectivity monitoring.
 * Provides a /stocks/pingdb endpoint for op-only health checks.
 */
public class HealthCheckService {
    
    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getName());
    
    private final Db db;
    private HttpServer server;
    private final int port;
    
    public HealthCheckService(Db db, int port) {
        this.db = db;
        this.port = port;
    }
    
    /**
     * Starts the health check HTTP server.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/stocks/pingdb", new PingDbHandler());
        server.setExecutor(null); // Use default executor
        server.start();
        
        logger.info("Health check server started on port " + port);
        logger.info("Health check endpoint: http://localhost:" + port + "/stocks/pingdb");
    }
    
    /**
     * Stops the health check HTTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop(1);
            logger.info("Health check server stopped");
        }
    }
    
    /**
     * HTTP handler for the /stocks/pingdb endpoint.
     */
    private class PingDbHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            try {
                // Perform a quick database connectivity check
                Integer result = db.queryValue("SELECT 1");
                
                if (result != null && result == 1) {
                    // Database is accessible
                    String responseBody = "{\n  \"status\": \"healthy\",\n  \"database\": \"connected\",\n  \"message\": \"✅ Database connectivity OK\"\n}";
                    sendJsonResponse(exchange, 200, responseBody);
                } else {
                    // Unexpected result
                    String responseBody = "{\n  \"status\": \"warning\",\n  \"database\": \"unexpected_result\",\n  \"message\": \"⚠️ Database returned unexpected result\"\n}";
                    sendJsonResponse(exchange, 200, responseBody);
                }
                
            } catch (SQLException e) {
                // Database connection failed
                logger.warning("Database health check failed: " + e.getMessage());
                String responseBody = String.format("{\n  \"status\": \"unhealthy\",\n  \"database\": \"disconnected\",\n  \"message\": \"❌ Database connectivity failed\",\n  \"error\": \"%s\"\n}", e.getMessage().replace("\"", "\\\""));
                sendJsonResponse(exchange, 503, responseBody);
                
            } catch (Exception e) {
                // Other errors
                logger.warning("Health check error: " + e.getMessage());
                String responseBody = String.format("{\n  \"status\": \"error\",\n  \"database\": \"unknown\",\n  \"message\": \"❌ Health check error\",\n  \"error\": \"%s\"\n}", e.getMessage().replace("\"", "\\\""));
                sendJsonResponse(exchange, 500, responseBody);
            }
        }
        
        private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, statusCode, responseBody);
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
            byte[] response = responseBody.getBytes();
            exchange.sendResponseHeaders(statusCode, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}
