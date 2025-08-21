package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.politicalreferralswa.model.SystemConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.Optional;

@Service
public class FirestoreConfigService {
    
    private final Firestore firestore;
    private static final String COLLECTION_NAME = "system_configuration";
    
    @Autowired
    public FirestoreConfigService(Firestore firestore) {
        this.firestore = firestore;
    }
    
    /**
     * Guarda o actualiza una configuración en Firestore
     * @param config La configuración a guardar
     * @return true si se guardó exitosamente, false en caso contrario
     */
    public boolean saveConfiguration(SystemConfiguration config) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME)
                .document(config.getConfigKey());
            
            WriteResult result = docRef.set(config).get();
            return result != null;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error al guardar configuración en Firestore: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Busca una configuración por su clave
     * @param configKey La clave de la configuración
     * @return Optional con la configuración encontrada
     */
    public Optional<SystemConfiguration> findConfigurationByKey(String configKey) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME)
                .document(configKey);
            
            DocumentSnapshot document = docRef.get().get();
            
            if (document.exists()) {
                SystemConfiguration config = document.toObject(SystemConfiguration.class);
                return Optional.ofNullable(config);
            } else {
                return Optional.empty();
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error al buscar configuración en Firestore: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Obtiene el valor de una configuración por su clave
     * @param configKey La clave de la configuración
     * @return Optional con el valor de la configuración
     */
    public Optional<String> findValueByConfigKey(String configKey) {
        Optional<SystemConfiguration> config = findConfigurationByKey(configKey);
        return config.map(SystemConfiguration::getConfigValue);
    }
    
    /**
     * Verifica si existe una configuración con la clave especificada
     * @param configKey La clave de la configuración
     * @return true si existe, false si no
     */
    public boolean existsByConfigKey(String configKey) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME)
                .document(configKey);
            
            DocumentSnapshot document = docRef.get().get();
            return document.exists();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error al verificar existencia en Firestore: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Elimina una configuración por su clave
     * @param configKey La clave de la configuración a eliminar
     * @return true si se eliminó exitosamente, false en caso contrario
     */
    public boolean deleteConfiguration(String configKey) {
        try {
            DocumentReference docRef = firestore.collection(COLLECTION_NAME)
                .document(configKey);
            
            WriteResult result = docRef.delete().get();
            return result != null;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error al eliminar configuración en Firestore: " + e.getMessage());
            return false;
        }
    }
}
