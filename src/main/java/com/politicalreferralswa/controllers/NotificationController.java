package com.politicalreferralswa.controllers;

import com.politicalreferralswa.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*") // Permitir CORS para CI/CD
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Endpoint público para CI/CD - No requiere autenticación
     */
    @PostMapping("/deployment/success")
    public ResponseEntity<String> notifyDeploymentSuccess(@RequestBody Map<String, String> payload) {
        try {
            String serviceName = payload.get("serviceName");
            String region = payload.get("region");
            String imageTag = payload.get("imageTag");
            String commitSha = payload.get("commitSha");
            String commitTitle = payload.get("commitTitle");
            String targetPhones = payload.get("targetPhones");
            String groupId = payload.get("groupId");
            
            logger.info("Received deployment success notification for service: {}", serviceName);
            logger.info("Payload: targetPhones={}, groupId={}, commitTitle={}", targetPhones, groupId, commitTitle);
            
            // Usar configuración del payload o fallback a configuración local
            if ((targetPhones != null && !targetPhones.isEmpty()) || (groupId != null && !groupId.isEmpty())) {
                notificationService.sendDeploymentSuccessToTarget(serviceName, region, imageTag, commitSha, targetPhones, groupId);
            } else {
                notificationService.sendDeploymentSuccess(serviceName, region, imageTag, commitSha);
            }
            
            return ResponseEntity.ok("Notification sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send deployment success notification: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notification: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint público para CI/CD - No requiere autenticación
     */
    @PostMapping("/deployment/failure")
    public ResponseEntity<String> notifyDeploymentFailure(@RequestBody Map<String, String> payload) {
        try {
            String serviceName = payload.get("serviceName");
            String region = payload.get("region");
            String commitSha = payload.get("commitSha");
            String errorDetails = payload.get("errorDetails");
            String targetPhones = payload.get("targetPhones");
            String groupId = payload.get("groupId");
            
            logger.info("Received deployment failure notification for service: {}", serviceName);
            logger.info("Payload: targetPhones={}, groupId={}", targetPhones, groupId);
            
            // Usar configuración del payload o fallback a configuración local
            if ((targetPhones != null && !targetPhones.isEmpty()) || (groupId != null && !groupId.isEmpty())) {
                notificationService.sendDeploymentFailureToTarget(serviceName, region, commitSha, errorDetails, targetPhones, groupId);
            } else {
                notificationService.sendDeploymentFailure(serviceName, region, commitSha, errorDetails);
            }
            
            return ResponseEntity.ok("Notification sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send deployment failure notification: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to send notification: " + e.getMessage());
        }
    }
    
    /**
     * Endpoint de health check para notificaciones
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Notification service is healthy");
    }
    
    /**
     * Endpoint público de prueba para CI/CD
     */
    @PostMapping("/test")
    public ResponseEntity<String> testNotification(@RequestBody Map<String, String> payload) {
        logger.info("Received test notification: {}", payload);
        return ResponseEntity.ok("Test notification received successfully");
    }
} 