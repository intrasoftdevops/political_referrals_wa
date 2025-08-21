package com.politicalreferralswa.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.bp.Duration;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${firestore.project.id:intreasoft-daniel}")
    private String projectId;

    @Value("${FIRESTORE_DATABASE_ID:}")
    private String databaseId;

    @Bean
    public Firestore firestore() throws IOException {
        // Cargar credenciales
        GoogleCredentials credentials;
        
        // Verificar si estamos en Cloud Run o entorno de producción
        boolean isCloudRun = System.getenv("K_SERVICE") != null || 
                           System.getenv("PORT") != null ||
                           System.getProperty("spring.profiles.active", "").contains("prod");
        
        if (isCloudRun) {
            // En Cloud Run, usar credenciales por defecto directamente
            try {
                credentials = GoogleCredentials.getApplicationDefault();
                System.out.println("INFO: Usando credenciales por defecto de Google Cloud (Cloud Run)");
            } catch (IOException e) {
                throw new RuntimeException("No se pudieron obtener credenciales de Firebase en Cloud Run", e);
            }
        } else {
            // Solo en desarrollo local, intentar cargar desde archivo local
            try {
                var inputStream = getClass().getResourceAsStream("/political-referrals-wa-key.json");
                if (inputStream != null) {
                    credentials = GoogleCredentials.fromStream(inputStream);
                    System.out.println("INFO: Credenciales cargadas desde archivo local para desarrollo");
                } else {
                    // Si no hay archivo local, usar credenciales por defecto
                    credentials = GoogleCredentials.getApplicationDefault();
                    System.out.println("INFO: Usando credenciales por defecto de Google Cloud (desarrollo)");
                }
            } catch (Exception e) {
                System.out.println("WARN: No se pudieron cargar credenciales locales: " + e.getMessage());
                // Usar credenciales por defecto como fallback
                credentials = GoogleCredentials.getApplicationDefault();
                System.out.println("INFO: Usando credenciales por defecto de Google Cloud (fallback)");
            }
        }

        try {
            // Configurar Firestore directamente con timeouts aumentados
            FirestoreOptions.Builder firestoreOptionsBuilder = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId(projectId);
            
            // Solo agregar databaseId si está especificado, no está vacío y no es solo espacios
            if (databaseId != null && !databaseId.trim().isEmpty()) {
                firestoreOptionsBuilder.setDatabaseId(databaseId.trim());
                System.out.println("INFO: Usando base de datos específica con ID: '" + databaseId.trim() + "'");
            } else {
                System.out.println("INFO: No se especificó databaseId, usando base de datos por defecto del proyecto");
            }
            
            FirestoreOptions firestoreOptions = firestoreOptionsBuilder.build();
            Firestore firestore = firestoreOptions.getService();
            System.out.println("INFO: Conectado exitosamente a Firebase Firestore en el proyecto: " + projectId + " (conexión directa)");
            return firestore;
            
        } catch (Exception e) {
            System.out.println("WARN: Fallo conexión directa, intentando con FirebaseApp: " + e.getMessage());
            
            // Fallback al método original
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

            FirebaseApp firebaseApp;
            if (FirebaseApp.getApps().isEmpty()) { 
                firebaseApp = FirebaseApp.initializeApp(options);
            } else {
                firebaseApp = FirebaseApp.getInstance(); // Obtiene la instancia existente si ya fue inicializada.
            }
                
            System.out.println("INFO: Conectado exitosamente a Firebase Firestore en el proyecto: " + firebaseApp.getOptions().getProjectId() + " (fallback)");
            return FirestoreClient.getFirestore(firebaseApp);
        }
    }
}