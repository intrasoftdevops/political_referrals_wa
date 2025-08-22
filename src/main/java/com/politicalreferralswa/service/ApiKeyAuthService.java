package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyAuthService {
    
    @Value("${SYSTEM_API_KEY:default-secure-key}")
    private String validApiKey;
    
    /**
     * Valida si la API key proporcionada es válida
     * @param apiKey La API key a validar
     * @return true si la API key es válida, false en caso contrario
     */
    public boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        return validApiKey.equals(apiKey);
    }
    
    /**
     * Obtiene la API key válida (solo para logging/debug)
     * @return La API key válida
     */
    public String getValidApiKey() {
        return validApiKey;
    }
}
