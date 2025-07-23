package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.politicalreferralswa.model.User; 
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;
import com.google.cloud.Timestamp;

import com.politicalreferralswa.service.WhatsAppCloudApiService; 
import java.net.URLEncoder; // Para codificar la URL
import java.nio.charset.StandardCharsets; // Para especificar la codificación
import java.io.UnsupportedEncodingException; // Para manejar la excepción de codificación

@Service
public class ChatbotService {

    private final Firestore firestore;
    private final WhatsAppCloudApiService whatsAppCloudApiService; 

    public ChatbotService(Firestore firestore, WhatsAppCloudApiService whatsAppCloudApiService) { 
        this.firestore = firestore;
        this.whatsAppCloudApiService = whatsAppCloudApiService; 
    }

    public String processIncomingMessage(String fromPhoneNumber, String messageText) {
        System.out.println("ChatbotService: Procesando mensaje entrante de " + fromPhoneNumber + ": '" + messageText + "'");

        User user = getUserByPhoneNumber(fromPhoneNumber).orElse(null); 
        String responseMessage = ""; 

        if (user == null) {
            System.out.println("ChatbotService: Nuevo usuario detectado: " + fromPhoneNumber);
            user = new User();
            user.setId(UUID.randomUUID().toString()); 
            user.setPhone_code("+57"); 
            user.setPhone(fromPhoneNumber); 
            user.setCreated_at(Timestamp.now()); 
            user.setChatbot_state("INIT"); 
            user.setAceptaTerminos(false); 

            saveUser(user); 

            responseMessage = """
                    ¡Hola!
                    Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia. Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra politica de tratamiento de datos, plasmadas aqui https://danielquinterocalle.com/privacidad. Si continuas está conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                    Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                    """;
            
            user.setChatbot_state("WAITING_TERMS_ACCEPTANCE");
            saveUser(user);
        } else {
            System.out.println("ChatbotService: Usuario existente. Estado actual: " + user.getChatbot_state());
            responseMessage = handleExistingUserMessage(user, messageText); 
        }

        whatsAppCloudApiService.sendWhatsAppMessage(fromPhoneNumber, responseMessage); 

        return responseMessage; 
    }

    private String handleExistingUserMessage(User user, String messageText) {
        String currentChatbotState = user.getChatbot_state(); 
        String nextChatbotState = currentChatbotState;
        String responseMessage = "";

        switch (currentChatbotState) {
            case "WAITING_TERMS_ACCEPTANCE":
                if (messageText.equalsIgnoreCase("Sí") || messageText.equalsIgnoreCase("Si")) {
                    user.setAceptaTerminos(true); 
                    responseMessage = "¿Cuál es tu nombre?";
                    nextChatbotState = "WAITING_NAME";
                } else {
                    responseMessage = "Para continuar, debes aceptar los términos. ¿Aceptas? (Sí/No)";
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                }
                break;
            case "WAITING_NAME":
                user.setName(messageText); 
                responseMessage = "¿En qué ciudad vives?";
                nextChatbotState = "WAITING_CITY";
                break;
            case "WAITING_CITY":
                user.setCity(messageText); 
                responseMessage = "Confirmamos tus datos: " + user.getName() + ", de " + user.getCity() + ". ¿Es correcto? (Sí/No)"; 
                nextChatbotState = "CONFIRM_DATA";
                break;
            case "CONFIRM_DATA":
                if (messageText.equalsIgnoreCase("Sí") || messageText.equalsIgnoreCase("Si")) {
                    String referralCode = generateUniqueReferralCode();
                    user.setReferral_code(referralCode); 

                    String dynamicInviteLink; 
                    try {
                        // El mensaje pre-llenado codificado para la URL
                        String encodedPreFilledMessage = URLEncoder.encode("Hola, vengo referido por:", StandardCharsets.UTF_8.toString()); 
                        // El código de referido también codificado
                        String encodedReferralCode = URLEncoder.encode(referralCode, StandardCharsets.UTF_8.toString()); 
                        
                        // Construcción del enlace wa.me dinámico con el código de referido
                        // Asumo 573150841309 es el número de tu bot de WhatsApp.
                        dynamicInviteLink = "https://wa.me/573150841309?text=" + encodedPreFilledMessage + encodedReferralCode; 
                    } catch (UnsupportedEncodingException e) {
                        System.err.println("ERROR: No se pudo codificar el código de referido. Causa: " + e.getMessage());
                        e.printStackTrace();
                        // Enlace de fallback si la codificación falla
                        dynamicInviteLink = "https://wa.me/573150841309?text=Error%20al%20generar%20referido"; 
                    }

                    // --- Generación del mensaje de respuesta largo con String.format y Text Block ---
                    responseMessage = String.format(
                        """
                        %s Gracias por unirte a la ola de cambio que estamos construyendo para Colombia.
                        Tu decisión de registrarte en la plataforma es un paso fundamental para construir el país que soñamos: un país más justo, equitativo y próspero para todos los colombianos.
                        Sé que muchos comparten la misma visión de un futuro mejor, y por eso quiero invitarte a que compartas este proyecto con tus amigos, familiares y conocidos. Juntos podemos lograr una transformación real y profunda.
                        
                        Aquí tienes tu enlace de referido: %s
                        
                        Comparte este enlace y ayúdanos a llegar a más personas.
                        Cada persona que se registre a través de tu enlace nos acerca a la meta de construir una Colombia innovadora, incluyente y con oportunidades para todos.
                        Recuerda, el cambio empieza con cada uno de nosotros. ¡Gracias por ser parte de este movimiento!
                        
                        Con esperanza y convicción,
                        Daniel Quintero Calle.
                        Candidato Presidencial 2026.
                        #ColombiaEnMarcha #UnidosPorElCambio #FuturoParaTodos
                        """, 
                        user.getName(), // Primer %s para el nombre del usuario
                        dynamicInviteLink // Segundo %s para el enlace de referido dinámico
                    );
                    // ----------------------------------------------------------------------------------

                    nextChatbotState = "COMPLETED";
                } else {
                    responseMessage = "Por favor, vuelve a escribir tu nombre completo.";
                    nextChatbotState = "WAITING_NAME";
                }
                break;
            case "COMPLETED":
                // Mensaje para el estado COMPLETED (sin cambios, usa el código de referido del usuario)
                responseMessage = "¡Ya estás registrado! Puedes compartir tu código: " + user.getReferral_code() + ". Escribe 'Hola' para ver tus opciones."; 
                break;
            default:
                responseMessage = "Lo siento, no entendí. Escribe 'Hola' para empezar.";
                nextChatbotState = "INIT";
        }

        user.setChatbot_state(nextChatbotState); 
        user.setUpdated_at(Timestamp.now()); 
        saveUser(user); 

        return responseMessage; 
    }

    // --- Métodos Auxiliares (sin cambios) ---
    private Optional<User> getUserByPhoneNumber(String phoneNumber) {
        try {
            return Optional.ofNullable(firestore.collection("users").document(phoneNumber).get().get().toObject(User.class));
        } catch (Exception e) {
            System.err.println("ERROR al buscar usuario en Firestore: " + e.getMessage());
            return Optional.empty();
        }
    }

    private void saveUser(User user) {
        try {
            firestore.collection("users").document(user.getPhone()).set(user).get();
        } catch (Exception e) {
            System.err.println("ERROR al guardar usuario en Firestore: " + e.getMessage());
        }
    }

    private String generateUniqueReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public void processReferralCodeInMessage(String incomingReferralCode, String newUserId) {
        System.out.println("DEBUG: Lógica de processReferralCodeInMessage (con mock). Código: " + incomingReferralCode + ", Nuevo Usuario ID: " + newUserId);
    }
}