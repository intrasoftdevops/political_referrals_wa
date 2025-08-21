package com.politicalreferralswa.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SystemConfigService {
    
    // Variable global que controla si la IA está habilitada para todo el sistema
    // Por defecto la IA está HABILITADA al iniciar
    private final AtomicBoolean aiEnabled = new AtomicBoolean(true);
    
    /**
     * Verifica si la IA está habilitada globalmente en el sistema
     * @return true si la IA está habilitada, false si está deshabilitada
     */
    public boolean isAIEnabled() {
        return aiEnabled.get();
    }
    
    /**
     * Habilita la IA globalmente en el sistema
     */
    public void enableAI() {
        aiEnabled.set(true);
        System.out.println("SystemConfigService: IA del sistema HABILITADA");
    }
    
    /**
     * Deshabilita la IA globalmente en el sistema
     */
    public void disableAI() {
        aiEnabled.set(false);
        System.out.println("SystemConfigService: IA del sistema DESHABILITADA - Los usuarios serán atendidos por agentes humanos");
    }
    
    /**
     * Cambia el estado de la IA del sistema
     * @param enabled true para habilitar, false para deshabilitar
     */
    public void setAIEnabled(boolean enabled) {
        aiEnabled.set(enabled);
        System.out.println("SystemConfigService: IA del sistema " + (enabled ? "HABILITADA" : "DESHABILITADA"));
    }
    
    /**
     * Obtiene el estado actual de la IA como string para logging
     * @return "HABILITADA" o "DESHABILITADA"
     */
    public String getAIStatus() {
        return aiEnabled.get() ? "HABILITADA" : "DESHABILITADA";
    }
}
