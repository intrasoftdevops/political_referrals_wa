package com.politicalreferralswa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
public class NameValidationService {

        @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;
    
    @Value("${GEMINI_API_URL}")
    private String geminiApiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public NameValidationService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Valida si un nombre extraído de WhatsApp es un nombre válido y extrae nombre/apellido usando IA
     * @param fullName El nombre completo a validar
     * @return CompletableFuture<NameValidationResult> con el resultado de la validación
     */
    public CompletableFuture<NameValidationResult> validateName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return CompletableFuture.completedFuture(new NameValidationResult(false, "Nombre vacío", null, null, null));
        }

        String prompt = buildValidationPrompt(fullName.trim());
        
        return webClient.post()
                .uri(geminiApiUrl + "?key=" + geminiApiKey)
                .bodyValue(buildRequest(prompt))
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseValidationResponse)
                .toFuture();
    }

    private String buildValidationPrompt(String fullName) {
        return String.format("""
            Eres un validador y extractor de nombres especializado en identificar nombres reales de personas.
            
            TAREA: Valida si el siguiente texto es un nombre válido de persona y extrae nombre y apellido.
            
            NOMBRE COMPLETO A VALIDAR: "%s"
            
            CRITERIOS DE VALIDACIÓN:
            1. Debe ser un nombre real de persona (no apodos, números, símbolos, etc.)
            2. Debe tener entre 2 y 50 caracteres
            3. Puede contener espacios para nombres compuestos
            4. Debe contener solo letras, espacios y caracteres especiales comunes en nombres (á, é, í, ó, ú, ñ, etc.)
            5. No debe ser un nombre genérico como "Usuario", "WhatsApp", "Contacto", etc.
            6. No debe ser un número de teléfono o código
            7. No debe ser un emoji o símbolo
            
            REGLAS DE EXTRACCIÓN:
            1. Si es un solo nombre (ej: "Juan"), extrae solo el nombre
            2. Si tiene nombre y apellido (ej: "Juan Pérez"), extrae ambos
            3. Si tiene nombre compuesto (ej: "Juan Carlos"), el nombre compuesto va en "name"
            4. Si tiene apellidos compuestos (ej: "Juan Pérez González"), todos los apellidos van en "lastname"
            5. Si solo tiene un apellido, va en "lastname"
            
            EJEMPLOS DE EXTRACCIÓN:
            - "Juan" → name: "Juan", lastname: null
            - "Juan Pérez" → name: "Juan", lastname: "Pérez"
            - "María José Rodríguez" → name: "María José", lastname: "Rodríguez"
            - "Carlos Alberto Pérez González" → name: "Carlos Alberto", lastname: "Pérez González"
            - "Ana" → name: "Ana", lastname: null
            - "José María López" → name: "José María", lastname: "López"
            
            EJEMPLOS DE NOMBRES INVÁLIDOS:
            - "Usuario"
            - "WhatsApp"
            - "Contacto"
            - "123456"
            - "ABC123"
            - "👋"
            - "Hola"
            - "Test"
            - "Admin"
            - "User"
            - "Unknown"
            - "Sin nombre"
            - "No disponible"
            
            RESPONDE SOLO EN JSON:
            {
              "isValid": true/false,
              "reason": "explicación de por qué es válido o inválido",
              "name": "nombre extraído (sin apellido)",
              "lastname": "apellido extraído (null si no hay)",
              "suggestedName": "nombre sugerido si es inválido, null si es válido"
            }
            """, fullName);
    }

    private String buildRequest(String prompt) {
        return String.format("""
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "%s"
                    }
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.1,
                "topK": 1,
                "topP": 1,
                "maxOutputTokens": 200
              }
            }
            """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    private NameValidationResult parseValidationResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidates = rootNode.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    
                    // Limpiar el texto de markdown si está presente
                    text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                    
                    JsonNode validationData = objectMapper.readTree(text);
                    
                    boolean isValid = validationData.path("isValid").asBoolean();
                    String reason = validationData.path("reason").asText();
                    String extractedName = validationData.path("name").asText(null);
                    String extractedLastname = validationData.path("lastname").asText(null);
                    String suggestedName = validationData.path("suggestedName").asText(null);
                    
                    return new NameValidationResult(isValid, reason, extractedName, extractedLastname, suggestedName);
                }
            }
            
            return new NameValidationResult(false, "Error al procesar respuesta de IA", null, null, null);
            
        } catch (Exception e) {
            System.err.println("Error al parsear respuesta de validación: " + e.getMessage());
            return new NameValidationResult(false, "Error al validar nombre", null, null, null);
        }
    }

    public static class NameValidationResult {
        private final boolean isValid;
        private final String reason;
        private final String extractedName;
        private final String extractedLastname;
        private final String suggestedName;

        public NameValidationResult(boolean isValid, String reason, String extractedName, String extractedLastname, String suggestedName) {
            this.isValid = isValid;
            this.reason = reason;
            this.extractedName = extractedName;
            this.extractedLastname = extractedLastname;
            this.suggestedName = suggestedName;
        }

        public boolean isValid() { return isValid; }
        public String getReason() { return reason; }
        public String getExtractedName() { return extractedName; }
        public String getExtractedLastname() { return extractedLastname; }
        public String getSuggestedName() { return suggestedName; }
    }
} 