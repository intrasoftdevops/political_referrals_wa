package com.politicalreferralswa.controllers;

import com.politicalreferralswa.service.ChatbotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture; // Importación necesaria para procesamiento asíncrono

/**
 * Controlador REST para recibir los webhooks de mensajes entrantes de Wati.
 * La URL configurada en Wati debe coincidir con el valor de @RequestMapping.
 *
 * NOTA IMPORTANTE: Asegúrate de que este archivo se llame WatiWebhookController.java
 * en tu sistema de archivos, coincidiendo con el nombre de la clase pública.
 */
@RestController
@RequestMapping("/api/wati-webhook") // <<--- URL QUE DEBES CONFIGURAR EN WATI
public class WatiWebhookController {

    private final ChatbotService chatbotService;
    private final ObjectMapper objectMapper;

    @Autowired
    public WatiWebhookController(ChatbotService chatbotService, ObjectMapper objectMapper) {
        this.chatbotService = chatbotService;
        this.objectMapper = objectMapper;
    }

    /**
     * Este es el endpoint principal para recibir los mensajes POST de los webhooks de Wati.
     * Procesa el payload JSON recibido de Wati para extraer la información del mensaje
     * y la pasa al ChatbotService para su lógica de negocio.
     *
     * @param payload El cuerpo JSON de la solicitud POST como String.
     * @return ResponseEntity<String> con HttpStatus.OK (200) para confirmar la recepción a Wati
     * mientras el procesamiento real ocurre en segundo plano.
     */
    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String payload) {
        System.out.println("\n--- WATI WEBHOOK RECIBIDO (POST) ---");
        System.out.println("Payload completo (RAW): " + payload);

        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            String eventType = rootNode.path("eventType").asText();
            String messageType = rootNode.path("type").asText();

            // Solo procesamos mensajes de tipo "message"
            if ("message".equals(eventType)) {
                String fromPhoneNumber = rootNode.path("waId").asText(); // Número de WhatsApp del remitente
                String messageText = null;

                if ("text".equals(messageType)) {
                    messageText = rootNode.path("text").asText();
                } else if ("button".equals(messageType) && rootNode.has("buttonReply") && rootNode.path("buttonReply").has("body")) {
                    messageText = rootNode.path("buttonReply").path("body").asText();
                } else if ("interactiveButtonReply".equals(messageType) && rootNode.has("interactiveButtonReply") && rootNode.path("interactiveButtonReply").has("title")) {
                    messageText = rootNode.path("interactiveButtonReply").path("title").asText();
                } else {
                    System.out.println("WatiWebhookController: Recibido mensaje de tipo no soportado ('" + messageType + "') o sin contenido de texto relevante. Ignorando.");
                    return new ResponseEntity<>("Evento de mensaje no soportado/sin texto", HttpStatus.OK);
                }

                if (fromPhoneNumber != null && !fromPhoneNumber.isEmpty() && messageText != null && !messageText.isEmpty()) {
                    System.out.println("WatiWebhookController: Mensaje de Wati. De: " + fromPhoneNumber + ", Contenido: '" + messageText + "'");
                    
                    // ****** CAMBIO CLAVE AQUÍ: Procesar de forma asíncrona ******
                    final String finalFromPhoneNumber = fromPhoneNumber; // Necesario para usar en la lambda
                    final String finalMessageText = messageText;         // Necesario para usar en la lambda
                    
                    CompletableFuture.runAsync(() -> {
                        try {
                            // La lógica pesada se ejecuta en un hilo separado
                            String primaryResponse = chatbotService.processIncomingMessage(finalFromPhoneNumber, finalMessageText, "WHATSAPP");
                            System.out.println("WatiWebhookController: Mensaje procesado por ChatbotService (Async). Respuesta principal: " + primaryResponse);
                        } catch (Exception e) {
                            System.err.println("WatiWebhookController: ERROR al procesar el mensaje de Wati de forma asíncrona: " + e.getMessage());
                            e.printStackTrace();
                            // Considera aquí una forma de notificar al usuario si el error es grave,
                            // o simplemente loguéalo para tu monitoreo.
                        }
                    });

                    // Devolver inmediatamente OK para Wati, evitando reintentos.
                    System.out.println("WatiWebhookController: Mensaje de Wati recibido, procesamiento iniciado asíncronamente.");
                    return new ResponseEntity<>("Mensaje recibido y en procesamiento asíncrono", HttpStatus.OK);

                } else {
                    System.out.println("WatiWebhookController: Datos incompletos o inválidos en el webhook de Wati (teléfono o texto nulo/vacío) para el tipo de mensaje: " + messageType);
                    return new ResponseEntity<>("Datos de mensaje incompletos/inválidos", HttpStatus.BAD_REQUEST);
                }
            } else {
                System.out.println("WatiWebhookController: Webhook de Wati recibido, pero no es un mensaje entrante de usuario (eventType: '" + eventType + "'). Ignorando.");
                return new ResponseEntity<>("Evento no de mensaje, ignorado", HttpStatus.OK);
            }

        } catch (Exception e) {
            System.err.println("WatiWebhookController: ERROR CRÍTICO al procesar el payload del webhook de Wati: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>("Error interno del servidor", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}