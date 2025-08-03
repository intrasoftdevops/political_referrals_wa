package com.politicalreferralswa.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.threeten.bp.Duration;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore firestore() throws IOException {
        // En un entorno de Google Cloud (como Cloud Run) o con gcloud configurado localmente,
        // applicationDefault() automáticamente encuentra las credenciales necesarias.
        // Esto elimina la necesidad de manejar manualmente los archivos de claves JSON.
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

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