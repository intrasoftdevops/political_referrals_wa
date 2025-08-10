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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Tag(name = "WhatsApp", description = "Endpoints relacionados con WhatsApp a través de Wati API")
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
    @Operation(
        summary = "Recibir mensajes de WhatsApp",
        description = "Endpoint principal para procesar mensajes entrantes de WhatsApp a través de Wati API. Extrae automáticamente datos del usuario usando Gemini AI y responde de forma inteligente."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Mensaje recibido y procesado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    value = "\"Mensaje recibido y en procesamiento asíncrono\""
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos de mensaje incompletos o inválidos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    value = "\"Datos de mensaje incompletos/inválidos\""
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error interno del servidor",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    value = "\"Error interno del servidor\""
                )
            )
        )
    })
    public ResponseEntity<String> receiveMessage(
        @RequestBody @Schema(
            description = "Payload del webhook de Wati",
            example = """
            {
              "eventType": "message",
              "type": "text",
              "waId": "+573001234567",
              "senderName": "Miguel",
              "text": "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, acepto sus términos"
            }
            """
        ) String payload
    ) {
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
                    // Extraer el nombre del remitente si está disponible
                    String senderName = rootNode.path("senderName").asText(null);
                    System.out.println("WatiWebhookController: Mensaje de Wati. De: " + fromPhoneNumber + 
                                     (senderName != null ? " (Nombre: " + senderName + ")" : "") + 
                                     ", Contenido: '" + messageText + "'");
                    
                    // ****** CAMBIO CLAVE AQUÍ: Procesar de forma asíncrona ******
                    final String finalFromPhoneNumber = fromPhoneNumber; // Necesario para usar en la lambda
                    final String finalMessageText = messageText;         // Necesario para usar en la lambda
                    final String finalSenderName = senderName;           // Nombre del remitente
                    
                    CompletableFuture.runAsync(() -> {
                        try {
                            // La lógica pesada se ejecuta en un hilo separado
                            String primaryResponse = chatbotService.processIncomingMessage(finalFromPhoneNumber, finalMessageText, "WHATSAPP", finalSenderName);
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