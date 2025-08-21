package com.politicalreferralswa.model;

import com.google.cloud.Timestamp;
import java.util.Date;

/**
 * Modelo para la configuración del sistema en Firestore
 * Usa tipos nativos de Firestore para evitar problemas de serialización
 */
public class SystemConfiguration {
    
    private String configKey;
    private String configValue;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // Constructor por defecto (requerido por Firestore)
    public SystemConfiguration() {
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }
    
    // Constructor con parámetros
    public SystemConfiguration(String configKey, String configValue, String description) {
        this();
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
    }
    
    // Getters y Setters
    public String getConfigKey() {
        return configKey;
    }
    
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }
    
    public String getConfigValue() {
        return configValue;
    }
    
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
        this.updatedAt = Timestamp.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
