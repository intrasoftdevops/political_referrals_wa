package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono; // Importación necesaria aunque .block() se use
import com.fasterxml.jackson.databind.ObjectMapper; // Para parsear la respuesta JSON de la IA
import com.fasterxml.jackson.databind.JsonNode; // Para parsear la respuesta JSON de la IA
import org.springframework.web.util.UriComponentsBuilder; // Para construir la URL

import java.net.URI; // Importación necesaria para java.net.URI
import java.util.HashMap;
import java.util.Map;

@Service
public class AIBotService {

    @Value("${ai.bot.endpoint}")
    private String aiBotEndpoint; // La URL base de tu bot de IA de FastAPI (del application.properties)

    private final WebClient webClient;
    private final ObjectMapper objectMapper; // Para parsear la respuesta JSON de la IA

    // Constructor: Inyecta WebClient.Builder y ObjectMapper.
    // WebClient se construye sin una base URL para tener control absoluto sobre la URI en cada llamada.
    public AIBotService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build(); // Construimos WebClient SIN una base URL
        this.objectMapper = objectMapper; // Inicializar ObjectMapper
    }

    /**
     * Envía una consulta al bot de IA de FastAPI y recupera la respuesta.
     * Construye la URL completa y envía el cuerpo JSON esperado por FastAPI.
     *
     * @param sessionId Un ID único para la sesión (usaremos el número de teléfono/chat ID del usuario).
     * @param userQuery La pregunta del usuario.
     * @return La respuesta del bot de IA (String) o un mensaje de error si la conexión falla.
     */
    public String getAIResponse(String sessionId, String userQuery) {
        System.out.println("AIBotService: Enviando consulta al bot de IA para sesión " + sessionId + ": '" + userQuery + "'");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // El bot de IA espera JSON

        // Construir el cuerpo de la petición JSON para el bot de IA
        // Formato esperado: {"query": "user_message", "session_id": "user_phone_number"}
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("query", userQuery);
        requestBody.put("session_id", sessionId);

        try {
            // Construir la URL COMPLETA Y ABSOLUTA para la llamada al bot de IA
            // FastAPI endpoint: POST https://chatbotia-331919709696.us-east1.run.app/chat
            URI fullApiUri = UriComponentsBuilder.fromUriString(aiBotEndpoint) // Usa la URL base de FastAPI desde properties
                                                .path("/chat")               // Añade la ruta del endpoint de chat
                                                .build()
                                                .toUri(); // Obtener el objeto URI java.net.URI

            System.out.println("AIBotService: URL de AI Bot construida: " + fullApiUri.toString()); // Log la URL final para verificación

            // Realizar la llamada POST al bot de IA
            // Se usa .block() para convertir el Mono reactivo a un String síncrono,
            // lo que es apropiado si ChatbotService no es reactivo.
            String jsonResponse = webClient.post()
                .uri(fullApiUri) // <<--- ¡PASAMOS EL OBJETO URI ABSOLUTO DIRECTAMENTE!
                .headers(h -> h.addAll(headers))
                .bodyValue(requestBody) // Enviar el cuerpo JSON con la consulta y sesión
                .retrieve()
                .bodyToMono(String.class) // Esperar la respuesta como String JSON
                .block(); // Bloquear para obtener el resultado de forma síncrona

            // Parsear la respuesta JSON para extraer el mensaje del bot
            // La respuesta de tu bot de IA es: {"response": {"response": "bot_answer"}}
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            String aiBotMessage = rootNode.path("response").path("response").asText();

            System.out.println("AIBotService: Respuesta recibida del bot de IA: '" + aiBotMessage + "'");
            return aiBotMessage;

        } catch (Exception e) {
            // Capturar cualquier excepción (conexión, red, parsing JSON, etc.)
            System.err.println("AIBotService: ERROR al obtener respuesta del bot de IA: " + e.getMessage());
            e.printStackTrace(); // Imprimir el stack trace para depuración
            return "Lo siento, tuve un problema al conectar con la inteligencia artificial. Por favor, intenta de nuevo más tarde.";
        }
    }
}