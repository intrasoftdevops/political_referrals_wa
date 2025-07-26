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
     * @return ResponseEntity<Void> con HttpStatus.OK (200) para confirmar la recepción a Wati.
     */
    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody String payload) {
        System.out.println("\n--- WATI WEBHOOK RECIBIDO (POST) ---");
        System.out.println("Payload completo (RAW): " + payload);

        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            String eventType = rootNode.path("eventType").asText();
            String messageType = rootNode.path("type").asText();

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
                    return new ResponseEntity<>(HttpStatus.OK);
                }

                if (fromPhoneNumber != null && !fromPhoneNumber.isEmpty() && messageText != null && !messageText.isEmpty()) {
                    System.out.println("WatiWebhookController: Mensaje de Wati. De: " + fromPhoneNumber + ", Contenido: '" + messageText + "'");
                    // <<--- ¡¡¡LLAMADA AL CHATBOT SERVICE CON EL CANAL "WHATSAPP"!!! ---
                    chatbotService.processIncomingMessage(fromPhoneNumber, messageText, "WHATSAPP");
                    System.out.println("WatiWebhookController: Mensaje procesado por ChatbotService.");
                } else {
                    System.out.println("WatiWebhookController: Datos incompletos o inválidos en el webhook de Wati (teléfono o texto nulo/vacío) para el tipo de mensaje: " + messageType);
                }
            } else {
                System.out.println("WatiWebhookController: Webhook de Wati recibido, pero no es un mensaje entrante de usuario (eventType: '" + eventType + "'). Ignorando.");
            }

            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("WatiWebhookController: ERROR CRÍTICO al procesar el payload del webhook de Wati: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}