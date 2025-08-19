package com.politicalreferralswa.controllers;

import com.politicalreferralswa.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode; 

@RestController
public class WebhookController {

    private final ChatbotService chatbotService;

    @Value("${WEBHOOK_VERIFY_TOKEN}") 
    private String webhookVerifyToken;

    @Autowired
    public WebhookController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
        @RequestParam("hub.mode") String mode,
        @RequestParam("hub.verify_token") String token,
        @RequestParam("hub.challenge") String challenge) {

        if (mode.equals("subscribe") && token.equals(this.webhookVerifyToken)) {
            System.out.println("WebhookController: Webhook verificado por Meta.");
            return new ResponseEntity<>(challenge, HttpStatus.OK);
        } else {
            System.err.println("WebhookController: Falló la verificación del webhook. Token esperado: " + this.webhookVerifyToken + ", Token recibido: " + token);
            return new ResponseEntity<>("Verification failed", HttpStatus.FORBIDDEN);
        }
    }

    // --- ¡¡¡ESTE ES EL ENDPOINT CLAVE PARA RECIBIR MENSAJES REALES DE WHATSAPP!!! ---
    // Está diseñado para el payload COMPLETO y ANIDADO que Meta envía para webhooks reales.
    @PostMapping("/webhook")
    public String receiveMessage(@RequestBody JsonNode webhookData) {
        System.out.println("\n--- WHATSAPP WEBHOOK RECIBIDO (POST) ---");
        System.out.println("Payload completo (RAW): " + webhookData.toString()); // Aquí verás el payload real de Meta

        try {
            JsonNode entryNode = null;
            JsonNode changesNode = null;
            JsonNode valueNode = null;
            String field = null;

            // --- ESTE ES EL INTENTO PRINCIPAL: Parsear como payload completo y anidado de Meta ---
            // Verifica si tiene la estructura "object" -> "entry" -> "changes" -> "value"
            if (webhookData.has("object") && webhookData.path("object").asText().equals("whatsapp_business_account") &&
                webhookData.has("entry") && webhookData.path("entry").isArray() && webhookData.path("entry").get(0) != null) {
                
                entryNode = webhookData.path("entry").get(0);
                if (entryNode.has("changes") && entryNode.path("changes").isArray() && entryNode.path("changes").get(0) != null) {
                    changesNode = entryNode.path("changes").get(0);
                    valueNode = changesNode.path("value");
                    field = changesNode.path("field").asText();
                    System.out.println("WebhookController: Payload detectado como formato ANIDADO estándar de Meta (Real WhatsApp Message).");
                } else {
                    // Si 'entry' existe pero 'changes' no o es vacío, puede ser un webhook de otro tipo (ej. de suscripción)
                    System.out.println("WebhookController: Payload anidado detectado, pero no contiene 'changes' con datos de mensaje. Field: " + (entryNode.has("field") ? entryNode.path("field").asText() : "N/A"));
                    return "EVENT_RECEIVED_NO_MESSAGE_CHANGES"; 
                }
            } 
            // --- ESTE ES EL FALLBACK: Si no es el formato anidado, intentar como formato simplificado (para tus pruebas) ---
            else if (webhookData.has("field") && webhookData.has("value")) {
                field = webhookData.path("field").asText();
                valueNode = webhookData.path("value");
                System.out.println("WebhookController: Payload detectado como formato SIMPLIFICADO (para pruebas).");
            } else {
                // Si no coincide con ninguna de las estructuras conocidas
                System.out.println("WebhookController: Payload de webhook no reconocido: no es anidado estándar ni simplificado.");
                return "EVENT_NOT_RECOGNIZED";
            }

            // --- Procesamiento del evento basado en 'field' y 'valueNode' ---
            if (field != null) { 
                if (field.equals("messages") && valueNode.path("messages").isArray() && valueNode.path("messages").get(0) != null) {
                    // Procesar mensajes entrantes de usuarios
                    JsonNode messageNode = valueNode.path("messages").get(0);
                    String fromPhoneNumber = messageNode.path("from").asText(); 
                    String messageType = messageNode.path("type").asText(); 

                    if (messageType.equals("text")) {
                        String messageText = messageNode.path("text").path("body").asText(); 

                        System.out.println("WebhookController: Mensaje de WhatsApp. De: " + fromPhoneNumber + ", Contenido: '" + messageText + "'");
                        
                        String botResponse = chatbotService.processIncomingMessage(fromPhoneNumber, messageText, "WHATSAPP");
                        
                        System.out.println("WebhookController: Respuesta del bot procesada: '" + botResponse + "'");
                        
                    } else {
                        System.out.println("WebhookController: Recibido mensaje no textual (tipo: " + messageType + "). Ignorando.");
                    }
                } else if (field.equals("statuses") && valueNode.path("statuses").isArray() && valueNode.path("statuses").get(0) != null) {
                    // Manejar eventos de estado de mensaje (entrega, lectura)
                    JsonNode statusNode = valueNode.path("statuses").get(0);
                    String status = statusNode.path("status").asText();
                    String recipientId = statusNode.path("recipient_id").asText();
                    System.out.println("WebhookController: Evento de estado de mensaje. Para: " + recipientId + ", Estado: " + status);
                } else {
                    System.out.println("WebhookController: Payload de webhook reconocido, pero el 'field' no es 'messages' ni 'statuses' o el contenido es inesperado (Field: " + field + ").");
                }
            } else {
                System.out.println("WebhookController: 'field' no pudo ser determinado en el payload.");
            }

            // Siempre retorna 200 OK para que Meta no reintente el webhook
            return "EVENT_RECEIVED"; 

        } catch (Exception e) {
            System.err.println("WebhookController: ERROR CRÍTICO al procesar el payload del webhook de WhatsApp: " + e.getMessage());
            e.printStackTrace(); 
            return "ERROR_PROCESSING_EVENT"; 
        }
    }
}