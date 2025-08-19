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

    @Value("${spring.cloud.gcp.credentials.location:}")
    private String credentialsLocation;

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
                    throw new RuntimeException("No se encontró el archivo de credenciales local");
                }
            } catch (Exception localEx) {
                System.out.println("WARN: No se pudieron cargar credenciales locales: " + localEx.getMessage());
                
                // Si no hay archivo local, intentar con la configuración especificada
                if (credentialsLocation != null && !credentialsLocation.trim().isEmpty()) {
                    if (credentialsLocation.startsWith("classpath:")) {
                        // Si es un classpath, cargar desde resources
                        String resourcePath = credentialsLocation.substring("classpath:".length());
                        var inputStream = getClass().getResourceAsStream("/" + resourcePath);
                        if (inputStream != null) {
                            credentials = GoogleCredentials.fromStream(inputStream);
                            System.out.println("INFO: Credenciales cargadas desde classpath");
                        } else {
                            throw new RuntimeException("No se pudo encontrar el archivo de credenciales: " + credentialsLocation);
                        }
                    } else {
                        // Si es una ruta de archivo, cargar directamente
                        credentials = GoogleCredentials.fromStream(new java.io.FileInputStream(credentialsLocation));
                        System.out.println("INFO: Credenciales cargadas desde archivo: " + credentialsLocation);
                    }
                } else {
                    // En entornos sin archivo de credenciales, usar credenciales por defecto
                    try {
                        credentials = GoogleCredentials.getApplicationDefault();
                        System.out.println("INFO: Usando credenciales por defecto de Google Cloud");
                    } catch (IOException e) {
                        throw new RuntimeException("No se pudieron obtener credenciales de Firebase. Para desarrollo local, asegúrate de tener political-referrals-wa-key.json en src/main/resources/", e);
                    }
                }
            }
        }

        try {
            // Configurar Firestore directamente con timeouts aumentados
            FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setCredentials(credentials)
                .setProjectId("intreasoft-daniel")
                .build();
            
            Firestore firestore = firestoreOptions.getService();
            System.out.println("INFO: Conectado exitosamente a Firebase Firestore en el proyecto: intreasoft-daniel (conexión directa)");
            return firestore;
            
        } catch (Exception e) {
            System.out.println("WARN: Fallo conexión directa, intentando con FirebaseApp: " + e.getMessage());
            
            // Fallback al método original
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("intreasoft-daniel")
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