package com.politicalreferralswa.controllers; // Corregir el paquete

import com.politicalreferralswa.service.ChatbotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map; // Para un ejemplo simple de JSON de entrada

@RestController // Indica que esta clase es un controlador REST
public class UserMessageController {

    private final ChatbotService chatbotService;

    // Inyección de dependencias del ChatbotService
    public UserMessageController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    /**
     * Endpoint para recibir mensajes de un usuario y enviar a la IA.
     * Suponemos que el JSON de entrada contiene "phoneNumber" y "message".
     *
     * Ejemplo de JSON de entrada:
     * {
     * "phoneNumber": "+573001234567",
     * "message": "¿Qué planes tienes para el fin de semana?"
     * }
     */
    @PostMapping("/api/message") // Define el endpoint POST
    public ResponseEntity<String> receiveUserMessage(@RequestBody Map<String, String> payload) {
        try {
            // 1. Obtener el número de teléfono del JSON de entrada
            String userPhoneNumber = payload.get("phoneNumber");
            // 2. Obtener el mensaje del usuario del JSON de entrada
            String userMessage = payload.get("message");

            // Validaciones básicas
            if (userPhoneNumber == null || userPhoneNumber.isEmpty()) {
                return new ResponseEntity<>("Error: 'phoneNumber' es requerido.", HttpStatus.BAD_REQUEST);
            }
            if (userMessage == null || userMessage.isEmpty()) {
                return new ResponseEntity<>("Error: 'message' es requerido.", HttpStatus.BAD_REQUEST);
            }

            System.out.println("Controlador: Recibido mensaje de " + userPhoneNumber + ": '" + userMessage + "'");

            // *** ESTA ES LA CLAVE DEL CAMBIO ***
            // Llamar al ChatbotService, pasando el número de teléfono como fromId
            String aiResponse = chatbotService.processIncomingMessage(userPhoneNumber, userMessage, "API");

            // Devolver la respuesta de la IA
            return new ResponseEntity<>(aiResponse, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Controlador: Error al procesar el mensaje del usuario: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Ocurrió un error interno al procesar tu solicitud.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}