package com.politicalreferralswa.model;

import com.google.cloud.Timestamp;
import lombok.Data;          // Asegúrate de que esta importación esté
import lombok.NoArgsConstructor; // Asegúrate de que esta importación esté (opcional pero bueno)
import lombok.AllArgsConstructor; // Asegúrate de que esta importación esté (opcional pero bueno)

@Data // <--- ¡ESTA ANOTACIÓN ES FUNDAMENTAL!
@NoArgsConstructor // <--- ESTA TAMBIÉN (para el constructor vacío)
@AllArgsConstructor // <--- Y ESTA (para el constructor con todos los campos)
public class User {
    // ... tus campos ...
    private String id;
    private String phone_code;
    private String phone;
    private Timestamp created_at;
    private String chatbot_state;
    private boolean aceptaTerminos; 
    private String name;
    private String city;
    private String referral_code;
    private Timestamp updated_at;
}