package com.politicalreferralswa.service;

import com.politicalreferralswa.model.SystemConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SystemConfigService {
    
    private final FirestoreConfigService firestoreConfigService;
    private final AtomicBoolean aiEnabled = new AtomicBoolean(true);
    
    // Clave para la configuración de IA en Firestore
    private static final String AI_ENABLED_KEY = "ai_enabled";
    
    @Autowired
    public SystemConfigService(FirestoreConfigService firestoreConfigService) {
        this.firestoreConfigService = firestoreConfigService;
    }
    
    /**
     * Inicializa el estado de la IA desde Firestore al arrancar el servicio
     */
    @PostConstruct
    public void initializeAIState() {
        try {
            // Buscar el estado en Firestore
            Optional<String> aiState = firestoreConfigService.findValueByConfigKey(AI_ENABLED_KEY);
            
            if (aiState.isPresent()) {
                // Si existe en Firestore, usar ese valor
                boolean enabled = Boolean.parseBoolean(aiState.get());
                aiEnabled.set(enabled);
                System.out.println("SystemConfigService: Estado de IA cargado desde Firestore: " + (enabled ? "HABILITADA" : "DESHABILITADA"));
            } else {
                // Si no existe en Firestore, crear con valor por defecto (HABILITADA)
                SystemConfiguration config = new SystemConfiguration(
                    AI_ENABLED_KEY, 
                    "true", 
                    "Estado de la IA del sistema (true=habilitada, false=deshabilitada)"
                );
                firestoreConfigService.saveConfiguration(config);
                aiEnabled.set(true);
                System.out.println("SystemConfigService: Estado de IA inicializado en Firestore como HABILITADA");
            }
        } catch (Exception e) {
            System.err.println("SystemConfigService: Error al inicializar estado de IA desde Firestore: " + e.getMessage());
            // En caso de error, mantener el valor por defecto (HABILITADA)
            aiEnabled.set(true);
        }
    }
    
    /**
     * Verifica si la IA está habilitada globalmente en el sistema
     * @return true si la IA está habilitada, false si está deshabilitada
     */
    public boolean isAIEnabled() {
        return aiEnabled.get();
    }
    
    /**
     * Habilita la IA globalmente en el sistema y persiste en Firestore
     */
    public void enableAI() {
        aiEnabled.set(true);
        persistAIState(true);
        System.out.println("SystemConfigService: IA del sistema HABILITADA y persistida en Firestore");
    }
    
    /**
     * Deshabilita la IA globalmente en el sistema y persiste en Firestore
     */
    public void disableAI() {
        aiEnabled.set(false);
        persistAIState(false);
        System.out.println("SystemConfigService: IA del sistema DESHABILITADA y persistida en Firestore - Los usuarios serán atendidos por agentes humanos");
    }
    
    /**
     * Cambia el estado de la IA del sistema y persiste en Firestore
     * @param enabled true para habilitar, false para deshabilitar
     */
    public void setAIEnabled(boolean enabled) {
        aiEnabled.set(enabled);
        persistAIState(enabled);
        System.out.println("SystemConfigService: IA del sistema " + (enabled ? "HABILITADA" : "DESHABILITADA") + " y persistida en Firestore");
    }
    
    /**
     * Obtiene el estado actual de la IA como string para logging
     * @return "HABILITADA" o "DESHABILITADA"
     */
    public String getAIStatus() {
        return aiEnabled.get() ? "HABILITADA" : "DESHABILITADA";
    }
    
    /**
     * Lee el estado actual de la IA desde Firestore y actualiza la memoria local
     * Útil para sincronizar cambios manuales en la base de datos
     * @return true si se actualizó exitosamente, false en caso contrario
     */
    public boolean refreshAIStateFromDatabase() {
        try {
            Optional<String> aiState = firestoreConfigService.findValueByConfigKey(AI_ENABLED_KEY);
            
            if (aiState.isPresent()) {
                boolean enabled = Boolean.parseBoolean(aiState.get());
                boolean wasChanged = aiEnabled.get() != enabled;
                
                aiEnabled.set(enabled);
                
                if (wasChanged) {
                    System.out.println("SystemConfigService: Estado de IA sincronizado desde BD: " + (enabled ? "HABILITADA" : "DESHABILITADA"));
                } else {
                    System.out.println("SystemConfigService: Estado de IA ya está sincronizado: " + (enabled ? "HABILITADA" : "DESHABILITADA"));
                }
                
                return true;
            } else {
                System.out.println("SystemConfigService: Estado de IA no encontrado en BD, manteniendo estado actual: " + getAIStatus());
                return false;
            }
        } catch (Exception e) {
            System.err.println("SystemConfigService: Error al refrescar estado de IA desde BD: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene el estado actual de la IA desde Firestore (sin modificar memoria local)
     * @return Optional con el estado de la BD, o empty si no existe
     */
    public Optional<Boolean> getAIStateFromDatabase() {
        try {
            Optional<String> aiState = firestoreConfigService.findValueByConfigKey(AI_ENABLED_KEY);
            return aiState.map(Boolean::parseBoolean);
            } catch (Exception e) {
            System.err.println("SystemConfigService: Error al leer estado de IA desde BD: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Persiste el estado de la IA en Firestore
     * @param enabled el estado a persistir
     */
    private void persistAIState(boolean enabled) {
        try {
            Optional<SystemConfiguration> existingConfig = firestoreConfigService.findConfigurationByKey(AI_ENABLED_KEY);
            
            if (existingConfig.isPresent()) {
                // Actualizar configuración existente
                SystemConfiguration config = existingConfig.get();
                config.setConfigValue(String.valueOf(enabled));
                firestoreConfigService.saveConfiguration(config);
            } else {
                // Crear nueva configuración
                SystemConfiguration config = new SystemConfiguration(
                    AI_ENABLED_KEY, 
                    String.valueOf(enabled), 
                    "Estado de la IA del sistema (true=habilitada, false=deshabilitada)"
                );
                firestoreConfigService.saveConfiguration(config);
            }
        } catch (Exception e) {
            System.err.println("SystemConfigService: Error al persistir estado de IA en Firestore: " + e.getMessage());
            // No lanzar excepción para no interrumpir el flujo principal
        }
    }
}
