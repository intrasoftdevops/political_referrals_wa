package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value; // Para leer valores de application.properties
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // Para hacer llamadas HTTP

import java.util.HashMap;
import java.util.Map;

@Service // Marca esta clase como un Bean de servicio de Spring
public class WhatsAppCloudApiService {

    // Inyecta el ID del número de teléfono desde application.properties
    @Value("${whatsapp.api.phone-number-id}") 
    private String phoneNumberId;

    // Inyecta el Token de Acceso desde application.properties
    @Value("${whatsapp.api.access-token}") 
    private String accessToken;

    private final RestTemplate restTemplate; // Cliente HTTP de Spring

    // Constructor para inicializar RestTemplate
    public WhatsAppCloudApiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Envía un mensaje de texto a un número de WhatsApp usando la API de WhatsApp Business Cloud.
     * @param toPhoneNumber El número de teléfono del destinatario (ej. "573001234567").
     * @param messageText El texto del mensaje a enviar.
     */
    public void sendWhatsAppMessage(String toPhoneNumber, String messageText) {
        // Construye la URL del endpoint de Meta para enviar mensajes
        // v22.0 es la versión de la API que usaste en Postman.
        String url = String.format("https://graph.facebook.com/v22.0/%s/messages", phoneNumberId);

        // Configura las cabeceras de la petición
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // El cuerpo es JSON
        headers.setBearerAuth(accessToken); // Autenticación con Bearer Token

        // Construye el cuerpo del mensaje en formato JSON
        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("messaging_product", "whatsapp"); // Siempre "whatsapp"
        messageBody.put("to", toPhoneNumber); // Número del destinatario
        messageBody.put("type", "text"); // Tipo de mensaje (texto, plantilla, etc.)
        
        Map<String, String> textContent = new HashMap<>();
        textContent.put("body", messageText); // Contenido del texto
        messageBody.put("text", textContent);

        // Crea la entidad de la petición (cuerpo + cabeceras)
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(messageBody, headers);

        try {
            // Realiza la petición POST a la API de Meta
            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            System.out.println("WhatsAppCloudApiService: Mensaje enviado exitosamente a " + toPhoneNumber);
        } catch (Exception e) {
            System.err.println("WhatsAppCloudApiService: ERROR al enviar mensaje a WhatsApp: " + e.getMessage());
            e.printStackTrace(); // Imprime la traza completa del error para depurar
        }
    }
}