package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de auditoría local para reemplazar la funcionalidad del servicio externo
 */
@Service
@Slf4j
public class LocalAuditService {
    
    private final Firestore firestore;
    
    @Autowired
    public LocalAuditService(Firestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Registra un acceso exitoso a analytics
     */
    public void logAnalyticsAccess(String userId, String sessionId, String ipAddress, 
                                  String userAgent, boolean cacheHit, int responseTimeMs, 
                                  String dataAccessed) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
            auditData.put("userId", userId);
            auditData.put("sessionId", sessionId);
            auditData.put("ipAddress", ipAddress);
            auditData.put("userAgent", userAgent);
            auditData.put("operation", "analytics_access");
            auditData.put("cacheHit", cacheHit);
            auditData.put("responseTimeMs", responseTimeMs);
            auditData.put("dataAccessed", dataAccessed != null ? dataAccessed : "null");
            auditData.put("status", "success");
            
            // Guardar en Firestore de forma asíncrona
            CompletableFuture.runAsync(() -> {
                try {
                    DocumentReference docRef = firestore.collection("audit_logs").document();
                    docRef.set(auditData);
                    log.debug("Auditoría de acceso a analytics registrada para usuario: {}", userId);
                } catch (Exception e) {
                    log.error("Error guardando auditoría de acceso para usuario: {}", userId, e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error en servicio de auditoría para usuario: {}", userId, e);
        }
    }
    
    /**
     * Registra un error en analytics
     */
    public void logAnalyticsError(String userId, String sessionId, String ipAddress, 
                                 String userAgent, String errorMessage, String errorType) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
            auditData.put("userId", userId);
            auditData.put("sessionId", sessionId);
            auditData.put("ipAddress", ipAddress);
            auditData.put("userAgent", userAgent);
            auditData.put("operation", "analytics_error");
            auditData.put("errorMessage", errorMessage);
            auditData.put("errorType", errorType);
            auditData.put("status", "error");
            
            // Guardar en Firestore de forma asíncrona
            CompletableFuture.runAsync(() -> {
                try {
                    DocumentReference docRef = firestore.collection("audit_logs").document();
                    docRef.set(auditData);
                    log.debug("Auditoría de error en analytics registrada para usuario: {}", userId);
                } catch (Exception e) {
                    log.error("Error guardando auditoría de error para usuario: {}", userId, e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error en servicio de auditoría para usuario: {}", userId, e);
        }
    }
    
    /**
     * Registra una autenticación exitosa
     */
    public void logAuthSuccess(String userId, String sessionId, String ipAddress, String userAgent) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
            auditData.put("userId", userId);
            auditData.put("sessionId", sessionId);
            auditData.put("ipAddress", ipAddress);
            auditData.put("userAgent", userAgent);
            auditData.put("operation", "auth_success");
            auditData.put("status", "success");
            
            // Guardar en Firestore de forma asíncrona
            CompletableFuture.runAsync(() -> {
                try {
                    DocumentReference docRef = firestore.collection("audit_logs").document();
                    docRef.set(auditData);
                    log.debug("Auditoría de autenticación exitosa registrada para usuario: {}", userId);
                } catch (Exception e) {
                    log.error("Error guardando auditoría de autenticación para usuario: {}", userId, e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error en servicio de auditoría para usuario: {}", userId, e);
        }
    }
    
    /**
     * Registra acceso a datos de usuario
     */
    public void logUserDataAccess(String userId, String sessionId, String ipAddress, 
                                 String userAgent, String dataType, String operation) {
        try {
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
            auditData.put("userId", userId);
            auditData.put("sessionId", sessionId);
            auditData.put("ipAddress", ipAddress);
            auditData.put("userAgent", userAgent);
            auditData.put("operation", "user_data_access");
            auditData.put("dataType", dataType);
            auditData.put("accessOperation", operation);
            auditData.put("status", "success");
            
            // Guardar en Firestore de forma asíncrona
            CompletableFuture.runAsync(() -> {
                try {
                    DocumentReference docRef = firestore.collection("audit_logs").document();
                    docRef.set(auditData);
                    log.debug("Auditoría de acceso a datos de usuario registrada para usuario: {}", userId);
                } catch (Exception e) {
                    log.error("Error guardando auditoría de acceso a datos para usuario: {}", userId, e);
                }
            });
            
        } catch (Exception e) {
            log.error("Error en servicio de auditoría para usuario: {}", userId, e);
        }
    }
    
    /**
     * Verifica si el servicio de auditoría está habilitado
     */
    public boolean isEnabled() {
        return true; // Siempre habilitado en el servicio local
    }
}
