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
public class NotificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Endpoint para notificar despliegue exitoso
     */
    @PostMapping("/deployment/success")
    public ResponseEntity<String> notifyDeploymentSuccess(@RequestBody Map<String, String> payload) {
        try {
            String serviceName = payload.get("serviceName");
            String region = payload.get("region");
            String imageTag = payload.get("imageTag");
            String commitSha = payload.get("commitSha");
            String targetPhone = payload.get("targetPhone");
            String groupId = payload.get("groupId");
            
            logger.info("Received deployment success notification for service: {}", serviceName);
            
            // Usar configuración del payload o fallback a configuración local
            if (targetPhone != null && !targetPhone.isEmpty()) {
                notificationService.sendDeploymentSuccessToTarget(serviceName, region, imageTag, commitSha, targetPhone, groupId);
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
     * Endpoint para notificar despliegue fallido
     */
    @PostMapping("/deployment/failure")
    public ResponseEntity<String> notifyDeploymentFailure(@RequestBody Map<String, String> payload) {
        try {
            String serviceName = payload.get("serviceName");
            String region = payload.get("region");
            String commitSha = payload.get("commitSha");
            String errorDetails = payload.get("errorDetails");
            String targetPhone = payload.get("targetPhone");
            String groupId = payload.get("groupId");
            
            logger.info("Received deployment failure notification for service: {}", serviceName);
            
            // Usar configuración del payload o fallback a configuración local
            if (targetPhone != null && !targetPhone.isEmpty()) {
                notificationService.sendDeploymentFailureToTarget(serviceName, region, commitSha, errorDetails, targetPhone, groupId);
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
} 