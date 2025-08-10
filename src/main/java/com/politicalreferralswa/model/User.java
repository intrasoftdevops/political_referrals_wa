// src/main/java/com/politicalreferralswa/model/User.java
package com.politicalreferralswa.model;

import com.google.cloud.Timestamp;
import lombok.Data; // Importa Lombok
import lombok.NoArgsConstructor; // Puedes añadir este si no tienes constructor explícito
import lombok.AllArgsConstructor; // Puedes añadir este si tienes todos los campos en un constructor

@Data // Esta anotación de Lombok genera getters, setters, toString, equals y hashCode
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class User {
    private String id;
    private String phone_code;
    private String phone;
    private Timestamp created_at;
    private String chatbot_state;
    private boolean aceptaTerminos;
    private String name;
    private String lastname;
    private String city;
    private String state;
    private String referral_code;
    private Timestamp updated_at;
    private String referred_by_phone;
    private String referred_by_code;
    private String telegram_chat_id;

    // Si no usas Lombok, tendrías que tener los getters y setters escritos manualmente:
    /*
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    // ... y así para cada campo ...
    */
}