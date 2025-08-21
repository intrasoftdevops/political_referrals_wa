package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Servicio para sincronización de respaldo del estado de la IA desde Firestore
 * Solo se ejecuta cada 5 minutos como respaldo, ya que los endpoints verifican la BD en tiempo real
 */
@Service
public class AutoSyncService {
    
    private final SystemConfigService systemConfigService;
    
    @Autowired
    public AutoSyncService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }
    
    /**
     * Sincronización de respaldo cada 5 minutos para casos extremos
     * Solo se ejecuta si hay problemas de conectividad o para mantener consistencia
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void backupSyncAIState() {
        try {
            boolean synced = systemConfigService.refreshAIStateFromDatabase();
            if (synced) {
                System.out.println("AutoSyncService: Sincronización de respaldo completada - Estado de IA actualizado desde BD");
            }
        } catch (Exception e) {
            System.err.println("AutoSyncService: Error en sincronización de respaldo: " + e.getMessage());
        }
    }
}
