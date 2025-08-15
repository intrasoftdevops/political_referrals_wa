package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private WatiApiService watiApiService;
    
    @Value("${wati.notification.group.id:}")
    private String notificationGroupId;
    
    @Value("${wati.notification.phones:}")
    private String notificationPhones;
    
    @Value("${wati.notification.enabled:false}")
    private boolean notificationsEnabled;
    
    /**
     * Envía notificación de despliegue exitoso
     */
    public void sendDeploymentSuccess(String serviceName, String region, String imageTag, String commitSha) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled");
            return;
        }
        
        String message = buildSuccessMessage(serviceName, region, imageTag, commitSha);
        sendWhatsAppNotificationToGroup(message);
    }
    
    /**
     * Envía notificación de despliegue fallido
     */
    public void sendDeploymentFailure(String serviceName, String region, String commitSha, String errorDetails) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled");
            return;
        }
        
        String message = buildFailureMessage(serviceName, region, commitSha, errorDetails);
        sendWhatsAppNotificationToGroup(message);
    }
    
    /**
     * Construye mensaje de éxito
     */
    private String buildSuccessMessage(String serviceName, String region, String imageTag, String commitSha) {
        return String.format(
            "🚀 *DESPLIEGUE EXITOSO* 🚀\n\n" +
            "✅ *Servicio:* %s\n" +
            "🌍 *Región:* %s\n" +
            "🐳 *Imagen:* %s\n" +
            "🔗 *Commit:* %s\n\n" +
            "🎉 La aplicación está funcionando en producción!",
            serviceName, region, imageTag, commitSha.substring(0, 8)
        );
    }
    
    /**
     * Construye mensaje de fallo
     */
    private String buildFailureMessage(String serviceName, String region, String commitSha, String errorDetails) {
        return String.format(
            "❌ *DESPLIEGUE FALLIDO* ❌\n\n" +
            "🔴 *Servicio:* %s\n" +
            "🌍 *Región:* %s\n" +
            "🔗 *Commit:* %s\n\n" +
            "⚠️ *Error:* %s\n\n" +
            "🔍 Revisa los logs para más detalles",
            serviceName, region, commitSha.substring(0, 8), errorDetails
        );
    }
    
    /**
     * Envía notificación por WhatsApp a grupo o múltiples números
     */
    private void sendWhatsAppNotificationToGroup(String message) {
        try {
            // Prioridad 1: Intentar enviar a grupo
            if (notificationGroupId != null && !notificationGroupId.isEmpty()) {
                sendToWhatsAppGroup(message);
                return;
            }
            
            // Prioridad 2: Enviar a múltiples números específicos
            if (notificationPhones != null && !notificationPhones.isEmpty()) {
                sendToMultiplePhones(message);
                return;
            }
            
            logger.warn("No group ID or phones configured for notifications");
            
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía mensaje a un grupo de WhatsApp
     */
    private void sendToWhatsAppGroup(String message) {
        try {
            // Usar el servicio Wati para enviar al grupo
            watiApiService.sendMessageToGroup(notificationGroupId, message);
            logger.info("WhatsApp notification sent successfully to group: {}", notificationGroupId);
        } catch (Exception e) {
            logger.error("Failed to send to WhatsApp group: {}", e.getMessage(), e);
            // Fallback: intentar con números individuales
            logger.info("Falling back to individual phone notifications");
            sendToMultiplePhones(message);
        }
    }
    
    /**
     * Envía mensaje a múltiples números de teléfono
     */
    private void sendToMultiplePhones(String message) {
        try {
            String[] phones = notificationPhones.split(",");
            int successCount = 0;
            
            for (String phone : phones) {
                phone = phone.trim();
                if (!phone.isEmpty()) {
                    try {
                        watiApiService.sendMessage(phone, message);
                        successCount++;
                        logger.debug("Message sent to phone: {}", phone);
                    } catch (Exception e) {
                        logger.warn("Failed to send to phone {}: {}", phone, e.getMessage());
                    }
                }
            }
            
            logger.info("WhatsApp notifications sent to {}/{} phones successfully", successCount, phones.length);
            
        } catch (Exception e) {
            logger.error("Failed to send to multiple phones: {}", e.getMessage(), e);
        }
    }
} 