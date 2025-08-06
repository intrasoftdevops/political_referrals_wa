package com.politicalreferralswa.controllers; // Corregir el paquete

import com.politicalreferralswa.service.ChatbotService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map; // Para un ejemplo simple de JSON de entrada

@RestController // Indica que esta clase es un controlador REST
@Tag(name = "Messages", description = "API directa para envío de mensajes")
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
    @Operation(
        summary = "Enviar mensaje directo",
        description = "Endpoint para enviar mensajes y recibir respuestas del chatbot sin usar WhatsApp o Telegram. Útil para testing y integraciones directas."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Mensaje procesado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    value = "\"¡Hola! Soy el bot de Reset a la Política. ¿En qué puedo ayudarte?\""
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos requeridos faltantes",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    value = "\"Error: 'phoneNumber' es requerido.\""
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
                    value = "\"Ocurrió un error interno al procesar tu solicitud.\""
                )
            )
        )
    })
    public ResponseEntity<String> receiveUserMessage(
        @RequestBody @Schema(
            description = "Datos del mensaje",
            example = "{\"phoneNumber\": \"+573001234567\", \"message\": \"Hola! Soy Miguel de Barranquilla\"}"
        ) Map<String, String> payload
    ) {
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