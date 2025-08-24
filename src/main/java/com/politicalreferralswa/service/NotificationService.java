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

    @Autowired
    private LocalAnalyticsService localAnalyticsService;
    
    @Value("${WATI_NOTIFICATION_GROUP_ID:}")
    private String notificationGroupId;
    
    @Value("${WATI_NOTIFICATION_PHONES:}")
    private String notificationPhones;
    
    @Value("${WATI_NOTIFICATION_ENABLED:false}")
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
     * Envía notificación de despliegue exitoso a un objetivo específico
     */
    public void sendDeploymentSuccessToTarget(String serviceName, String region, String imageTag, String commitSha, String targetPhones, String groupId) {
        String message = buildSuccessMessage(serviceName, region, imageTag, commitSha);
        sendWhatsAppNotificationToTarget(message, targetPhones, groupId);
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
     * Envía notificación de despliegue fallido a un objetivo específico
     */
    public void sendDeploymentFailureToTarget(String serviceName, String region, String commitSha, String errorDetails, String targetPhones, String groupId) {
        String message = buildFailureMessage(serviceName, region, commitSha, errorDetails);
        sendWhatsAppNotificationToTarget(message, targetPhones, groupId);
    }
    
    /**
     * Construye mensaje de éxito
     */
    private String buildSuccessMessage(String serviceName, String region, String imageTag, String commitSha) {
        // Manejar commit SHA de forma segura
        String shortCommit = commitSha != null && commitSha.length() >= 8 ? 
            commitSha.substring(0, 8) : commitSha;
            
        return String.format(
            "🚀 *DESPLIEGUE EXITOSO* 🚀\n\n" +
            "✅ *Servicio:* %s\n" +
            "🌍 *Región:* %s\n" +
            "🐳 *Imagen:* %s\n" +
            "🔗 *Commit:* %s\n\n" +
            "🎉 La aplicación está funcionando en producción!",
            serviceName, region, imageTag, shortCommit
        );
    }
    
    /**
     * Construye mensaje de fallo
     */
    private String buildFailureMessage(String serviceName, String region, String commitSha, String errorDetails) {
        // Manejar commit SHA de forma segura
        String shortCommit = commitSha != null && commitSha.length() >= 8 ? 
            commitSha.substring(0, 8) : commitSha;
            
        return String.format(
            "❌ *DESPLIEGUE FALLIDO* ❌\n\n" +
            "🔴 *Servicio:* %s\n" +
            "🌍 *Región:* %s\n" +
            "🔗 *Commit:* %s\n\n" +
            "⚠️ *Error:* %s\n\n" +
            "🔍 Revisa los logs para más detalles",
            serviceName, region, shortCommit, errorDetails
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
                String cleanPhone = phone.trim();
                if (!cleanPhone.isEmpty()) {
                    try {
                        watiApiService.sendMessage(cleanPhone, message);
                        successCount++;
                        logger.debug("Message sent to phone: {}", cleanPhone);
                    } catch (Exception e) {
                        logger.warn("Failed to send to phone {}: {}", cleanPhone, e.getMessage());
                    }
                }
            }
            
            logger.info("WhatsApp notifications sent to {}/{} phones successfully", successCount, phones.length);
            
        } catch (Exception e) {
            logger.error("Failed to send to multiple phones: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación a un objetivo específico (prioridad: grupo > teléfono)
     */
    private void sendWhatsAppNotificationToTarget(String message, String targetPhones, String groupId) {
        try {
            // Prioridad 1: Intentar enviar a grupo si se especifica
            if (groupId != null && !groupId.isEmpty()) {
                watiApiService.sendMessageToGroup(groupId, message);
                logger.info("WhatsApp notification sent to group: {}", groupId);
                return;
            }
            
            // Prioridad 2: Enviar a múltiples números de teléfono
            if (targetPhones != null && !targetPhones.isEmpty()) {
                logger.info("Attempting to send WhatsApp notification to phones: {}", targetPhones);
                
                // Dividir los números de teléfono por comas
                String[] phones = targetPhones.split(",");
                int successCount = 0;
                
                for (String phone : phones) {
                    String cleanPhone = phone.trim();
                    if (!cleanPhone.isEmpty()) {
                        try {
                            // Intentar envío con manejo de errores SSL
                            try {
                                watiApiService.sendNotificationMessage(cleanPhone, message);
                                successCount++;
                                logger.info("WhatsApp notification sent successfully to phone: {}", cleanPhone);
                            } catch (Exception sslError) {
                                logger.warn("SSL error occurred for phone {}, attempting retry: {}", cleanPhone, sslError.getMessage());
                                
                                // Esperar un poco y reintentar
                                Thread.sleep(2000);
                                watiApiService.sendNotificationMessage(cleanPhone, message);
                                successCount++;
                                logger.info("WhatsApp notification sent successfully on retry to phone: {}", cleanPhone);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to send to phone {}: {}", cleanPhone, e.getMessage());
                        }
                    }
                }
                
                logger.info("WhatsApp notifications sent to {}/{} phones successfully", successCount, phones.length);
                return;
            }
            
            logger.warn("No group ID or phones specified for target notification");
            
        } catch (Exception e) {
            logger.error("Failed to send target WhatsApp notification: {}", e.getMessage(), e);
            
            // Log adicional para debugging SSL
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage());
            }
        }
    }
    
    /**
     * Envía notificación de nuevo referido registrado
     */
    public void sendReferralNotification(String referrerPhone, String newUserFirstName, int totalReferrals) {
        if (!notificationsEnabled) {
            logger.info("Notifications disabled, skipping referral notification");
            return;
        }
        
        try {
            // Obtener el ranking de la ciudad del referente
            Integer cityRank = null;
            try {
                LocalAnalyticsService.UserStatsResponse stats = localAnalyticsService.getUserStats(referrerPhone);
                if (stats != null && stats.getCity() != null) {
                    cityRank = stats.getCity().getPosition();
                }
            } catch (Exception e) {
                logger.error("Failed to get city rank for referrer {}: {}", referrerPhone, e.getMessage());
                // No detener la notificación si falla la obtención del ranking
            }

            String message = buildReferralNotificationMessage(newUserFirstName, totalReferrals, cityRank);
            
            // Enviar notificación al referente
            watiApiService.sendWhatsAppMessageSync(referrerPhone, message);
            logger.info("Referral notification sent successfully to: {}", referrerPhone);
            
        } catch (Exception e) {
            logger.error("Failed to send referral notification to {}: {}", referrerPhone, e.getMessage(), e);
        }
    }
    
    /**
     * Construye mensaje de notificación de referido
     */
    private String buildReferralNotificationMessage(String newUserFirstName, int totalReferrals, Integer cityRank) {
        String rankMessage = "";
        if (cityRank != null && cityRank > 0) {
            rankMessage = String.format("\n\nActualmente ocupas el *puesto #%d* en tu ciudad. ¡Sigue así!", cityRank);
        }

        return String.format(
            "%s acaba de registrarse en la plataforma. 🎉\n\n" +
            "Actualmente ya cuentas con %d voluntarios referidos.%s",
            newUserFirstName, totalReferrals, rankMessage
        );
    }
} 