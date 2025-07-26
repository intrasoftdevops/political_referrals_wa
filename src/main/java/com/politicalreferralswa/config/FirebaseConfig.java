package com.politicalreferralswa.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream; // Usaremos InputStream para getResourceAsStream

@Configuration
public class FirebaseConfig {

    // --- ¡¡¡MUY IMPORTANTE!!! ---
    // --- 1. CONFIGURA LA RUTA O EL NOMBRE DEL ARCHIVO JSON DE CREDENCIALES ---
    // Si lo pones en src/main/resources, solo necesitas el nombre del archivo.
    // Si lo pones en otra ruta, necesitarás la ruta absoluta (ej. "/Users/tu_usuario/Desktop/my-key.json").
    private static final String SERVICE_ACCOUNT_KEY_FILE_NAME = "political-referrals-wa-key.json"; // Asegúrate que este sea el nombre de tu archivo JSON en src/main/resources

    // --- 2. CONFIGURA EL ID DE TU PROYECTO DE GOOGLE CLOUD ---
    private static final String PROJECT_ID = "intreasoft-daniel"; // <--- ¡¡¡ACTUALIZA ESTO!!!

    @Bean
    public Firestore firestore() throws IOException {
        InputStream serviceAccount = null;
        try {
            // Intentar cargar desde src/main/resources
            serviceAccount = getClass().getClassLoader().getResourceAsStream(SERVICE_ACCOUNT_KEY_FILE_NAME);

            if (serviceAccount == null) {
                // Si no se encuentra en resources, intentar cargar desde una ruta absoluta (si la especificas aquí)
                // Esto es solo un fallback, la opción de resources es preferida.
                System.err.println("ADVERTENCIA: Archivo de credenciales no encontrado en classpath: " + SERVICE_ACCOUNT_KEY_FILE_NAME);
                // Si tienes una ruta absoluta configurada, la usarías aquí como fallback:
                // serviceAccount = new FileInputStream("/ruta/absoluta/a/tu/political-referrals-wa-key.json"); 
                // Si no tienes un fallback, lanzar un error.
                throw new IOException("Firebase service account key file not found. Ensure '" + SERVICE_ACCOUNT_KEY_FILE_NAME + "' is in src/main/resources/.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(PROJECT_ID)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) { 
                FirebaseApp.initializeApp(options);
            } else {
                System.out.println("INFO: FirebaseApp ya está inicializado. Reutilizando instancia.");
            }
            
            System.out.println("INFO: Conectado exitosamente a Firebase Firestore.");
            return FirestoreClient.getFirestore();
        } finally {
            if (serviceAccount != null) {
                serviceAccount.close(); 
            }
        }
    }
}