package com.politicalreferralswa.controllers;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    
    /**
     * Endpoint de prueba simple para CI/CD
     */
    @GetMapping("/ping")
    public String ping() {
        logger.info("Ping endpoint called");
        return "pong";
    }
    
    /**
     * Endpoint de prueba para POST
     */
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> payload) {
        logger.info("Echo endpoint called with payload: {}", payload);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("status", "success");
        return payload;
    }
    
    /**
     * Endpoint de prueba para notificaciones
     */
    @PostMapping("/notification")
    public Map<String, Object> testNotification(@RequestBody Map<String, String> payload) {
        logger.info("Test notification endpoint called with payload: {}", payload);
        return Map.of(
            "status", "success",
            "message", "Test notification received",
            "payload", payload,
            "timestamp", System.currentTimeMillis()
        );
    }
} 