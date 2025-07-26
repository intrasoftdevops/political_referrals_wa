package com.politicalreferralswa.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirebaseConfig {

    // --- ¡¡¡MUY IMPORTANTE!!! ---
    // --- 1. CONFIGURA LA RUTA O EL NOMBRE DEL ARCHIVO JSON DE CREDENCIALES ---
    // Si lo pones en src/main/resources, solo necesitas el nombre del archivo.
    // Si lo pones en otra ruta, necesitarás la ruta absoluta (ej. "/Users/tu_usuario/Desktop/my-key.json").
    private static final String SERVICE_ACCOUNT_KEY_FILE_NAME = "political-referrals-wa-key.json"; // Asegúrate que este sea el nombre de tu archivo JSON en src/main/resources

    // --- 2. CONFIGURA EL ID DE TU PROYECTO DE GOOGLE CLOUD ---
    private static final String PROJECT_ID = "intreasoft-daniel"; // <--- ¡¡¡ACTUALIZA ESTO!!!

   
}