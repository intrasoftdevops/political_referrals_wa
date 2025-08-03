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

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsLocation;

    @Bean
    public Firestore firestore() throws IOException {
        // Cargar credenciales desde la ubicación configurada en application.properties
        GoogleCredentials credentials;
        
        if (credentialsLocation.startsWith("classpath:")) {
            // Si es un classpath, cargar desde resources
            String resourcePath = credentialsLocation.substring("classpath:".length());
            var inputStream = getClass().getResourceAsStream("/" + resourcePath);
            if (inputStream != null) {
                credentials = GoogleCredentials.fromStream(inputStream);
                System.out.println("INFO: Credenciales cargadas ");
            } else {
                throw new RuntimeException("No se pudo encontrar el archivo de credenciales: " + credentialsLocation);
            }
        } else {
            // Si es una ruta de archivo, cargar directamente
            credentials = GoogleCredentials.fromStream(new java.io.FileInputStream(credentialsLocation));
            System.out.println("INFO: Credenciales cargadas desde archivo: " + credentialsLocation);
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