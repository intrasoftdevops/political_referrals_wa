package com.politicalreferralswa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class GeminiService {

        @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;
    
    @Value("${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiApiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, MetricsService metricsService) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    /**
     * Extrae datos de usuario de un mensaje usando Gemini AI
     * 
     * @param userMessage     El mensaje del usuario
     * @param previousContext Contexto de conversación previa (opcional)
     * @param currentState    Estado actual del chatbot (opcional)
     * @return UserDataExtractionResult con los datos extraídos
     */
    public UserDataExtractionResult extractUserData(String userMessage, String previousContext, String currentState) {
        long startTime = System.currentTimeMillis();
        try {
            System.out.println("GeminiService: Iniciando extracción de datos para mensaje: '" + userMessage + "'");
            String prompt = buildExtractionPrompt(userMessage, previousContext, currentState);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Map.of(
                    "parts", Map.of("text", prompt)));

            // Configuración específica para extracción precisa
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.1);
            generationConfig.put("topK", 1);
            generationConfig.put("topP", 1.0);
            generationConfig.put("maxOutputTokens", 512);
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String fullUrl = geminiApiUrl + "?key=" + geminiApiKey;
            System.out.println("GeminiService: Enviando consulta a Gemini API: " + fullUrl);

            String response = webClient.post()
                    .uri(fullUrl)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("GeminiService: Respuesta recibida de Gemini: " + response);
            UserDataExtractionResult result = parseGeminiResponse(response);

            // Registrar métricas
            long responseTime = System.currentTimeMillis() - startTime;
            if (result.isSuccessful()) {
                metricsService.recordSuccessfulExtraction(result.getConfidence(), responseTime);
                // Registrar campos extraídos
                if (result.getName() != null)
                    metricsService.recordFieldExtraction("name");
                if (result.getLastname() != null)
                    metricsService.recordFieldExtraction("lastname");
                if (result.getCity() != null)
                    metricsService.recordFieldExtraction("city");
                if (result.getState() != null)
                    metricsService.recordFieldExtraction("state");
                if (result.getAcceptsTerms() != null)
                    metricsService.recordFieldExtraction("acceptsTerms");
                if (result.getReferredByPhone() != null)
                    metricsService.recordFieldExtraction("referredByPhone");
                if (result.getReferralCode() != null)
                    metricsService.recordFieldExtraction("referralCode");
            } else {
                metricsService.recordFailedExtraction(responseTime);
            }

            return result;

        } catch (Exception e) {
            System.err.println("Error al extraer datos con Gemini: " + e.getMessage());
            e.printStackTrace();
            long responseTime = System.currentTimeMillis() - startTime;
            metricsService.recordFailedExtraction(responseTime);
            return UserDataExtractionResult.empty();
        }
    }

    private String buildExtractionPrompt(String userMessage, String previousContext, String currentState) {
        String safeUserMessage = userMessage != null ? userMessage : "";
        String safePreviousContext = previousContext != null ? previousContext : "";
        String safeCurrentState = currentState != null ? currentState : "";
        
        return String.format("""
            Eres un asistente de extracción de información muy inteligente, amigable y con profundo conocimiento de Colombia.
            CONTEXTO: Sistema de registro para campaña política en Colombia.
            MENSAJE: "%s"
            CONVERSACIÓN PREVIA: "%s"
            ESTADO ACTUAL: "%s"

            CAMPOS A EXTRAER:
            - name: Nombre (sin apellido, sin títulos)
            - lastname: Apellido(s) completo(s)
            - city: Ciudad donde vive actualmente (residencia, no origen)
            - state: Departamento/Estado colombiano donde reside
            - city: Ciudad donde vive actualmente (residencia, no origen)
            - state: Departamento/Estado colombiano donde reside
            - acceptsTerms: Si acepta términos explícitamente
            - referredByPhone: Número +57XXXXXXXXX
            - referralCode: Código alfanumérico de 8 dígitos

            ANÁLISIS SEMÁNTICO DE NOMBRES:
            - Distingue inteligentemente entre nombres y apellidos.
            - Ejemplos:
              - "Juan Carlos" = name: "Juan Carlos" (nombre compuesto)
              - "María José Rodríguez" = name: "María José", lastname: "Rodríguez"
              - "Carlos Alberto Pérez González" = name: "Carlos Alberto", lastname: "Pérez González"
              - "Pablo" = name: "Pablo" (nombre simple)
              - "Dr. Juan" = name: "Juan" (ignora títulos)
              - "José María" = name: "José María" (ambos son nombres)

            INTERPRETACIÓN POR ESTADO ACTUAL:
            - Si el estado es "WAITING_LASTNAME": Cualquier texto se interpreta como APELLIDO
            - Si el estado es "WAITING_NAME": 
              * Si el mensaje es una CONFIRMACIÓN del nombre existente, marcar como confirmación
              * Si es un NUEVO nombre, extraerlo normalmente
            - Si el estado es "WAITING_CITY": Cualquier texto se interpreta como CIUDAD
            - Si el estado es null o vacío: Usar análisis semántico normal

            DETECCIÓN INTELIGENTE DE CONFIRMACIONES:
            - Analiza si el mensaje confirma el nombre existente o proporciona uno nuevo
            - Ejemplos de CONFIRMACIÓN: "Si, es correcto", "Sí, está bien", "Correcto", "Perfecto", "Es correcto", "Si, es", "Sí, es"
            - Ejemplos de NUEVO NOMBRE: "Me llamo Carlos", "Soy María", "Carlos", "María"
            - Campo "isConfirmation": true si confirma, false si es nuevo nombre

            DETECCIÓN DE CONTEXTO EMOCIONAL:
            - Si el usuario expresa que ya respondió, frustración o impaciencia, genera una respuesta empática en "emotionalContext".

            EJEMPLOS DE CONTEXTO EMOCIONAL:
            - "Soy Pablo, ya me lo habías preguntado" → emotionalContext: "Disculpa Pablo, tienes razón. Continuemos..."
            - "Ya te dije, en Medellín" → emotionalContext: "Perdón por preguntar de nuevo. Gracias por tu paciencia..."
            - "Otra vez el nombre?" → emotionalContext: "Mis disculpas, no quería repetir la pregunta..."

            CONOCIMIENTO COLOMBIANO:
            - Armenia: Quindío (principal), Antioquia
            - La Dorada: Caldas, Putumayo
            - Sabaneta: Antioquia
            - Barbosa: Antioquia, Santander

            INFERENCIA DE JERGA COLOMBIANA:
            - Usa conocimiento general de Colombia para interpretar jerga, apodos y expresiones locales.
            - Ejemplos comunes (no te limites a estos):
              - "rolo/rolos" = Bogotá (también "cachaco", "de la nevera", "capitalino")
              - "paisa/paisas" = Medellín/Antioquia
              - "costeño/costeños" = Costa Caribe (Barranquilla, Cartagena, etc.)
              - "caleño/caleños" = Cali, Valle del Cauca
              - "opita/opitas" = Tolima (especialmente Ibagué)
              - "santandereano" = Santander (Bucaramanga)

            INSTRUCCIONES DE RAZONAMIENTO:
            1. Si encuentras jerga NO listada arriba, usa tu conocimiento general de Colombia.
            2. Considera apodos de ciudades, gentilicios informales, expresiones regionales.
            3. Si es jerga que claramente identifica una ciudad/región, extráela con confianza alta.
            4. Solo pide aclaración si genuinamente no puedes inferir la ubicación.
            5. Recuerda que Colombia tiene mucha diversidad de expresiones regionales.

            REGLAS DE AMBIGÜEDAD GEOGRÁFICA:
            - Si detectas una ciudad con múltiples ubicaciones (ej: Armenia, Barbosa), marca ambigüedad en "city" y pide aclaración.
            - No asumas automáticamente la ubicación principal.
            - Ejemplos de aclaración:
              - Armenia: "Hay varias Armenia en Colombia: Quindío, Antioquia, Bello. ¿Cuál es la tuya?"
              - La Dorada: "La Dorada existe en Caldas y Putumayo. ¿Cuál es?"
              - Barbosa: "Hay varias Barbosa en Colombia: Antioquia, Santander. ¿Cuál es la tuya?"
            - Bogotá: Cundinamarca
            - Medellín: Antioquia
            - Cali: Valle del Cauca
            - Barranquilla: Atlántico
            - Cartagena: Bolívar
            - Bucaramanga: Santander
            - Pereira: Risaralda
            - Manizales: Caldas
            - Ibagué: Tolima
            - Villavicencio: Meta
            - Pasto: Nariño
            - Montería: Córdoba
            - Valledupar: Cesar
            - Popayán: Cauca
            - Tunja: Boyacá
            - Florencia: Caquetá
            - Mocoa: Putumayo
            - Leticia: Amazonas
            - Mitú: Vaupés
            - Inírida: Guainía
            - Puerto Carreño: Vichada
            - San José del Guaviare: Guaviare
            - Yopal: Casanare
            - Arauca: Arauca
            - Riohacha: La Guajira
            - Quibdó: Chocó
            - San Andrés: San Andrés y Providencia
            - Números móviles: +57 300/301/302/310/311/312/313/314/315/316/317/318/319/320/321/322/323/324/350/351

            RESPONDE SOLO EN JSON:
            {
              "name": "string|null",
              "lastname": "string|null",
              "city": "string|null",
              "state": "string|null",
              "acceptsTerms": "boolean|null",
              "referredByPhone": "string|null",
              "referralCode": "string|null",
              "correction": "boolean|null",
              "previousValue": "string|null",
              "isConfirmation": "boolean|null",
              "needsClarification": {
                "city": "mensaje específico|null",
                "other": "otra aclaración|null"
              },
              "emotionalContext": "mensaje empático|null",
              "confidence": 0.0-1.0
            }

            EJEMPLOS DE RAZONAMIENTO INTELIGENTE:

            NOMBRES Y CONTEXTO EMOCIONAL:
            - "Soy Pablo, ya me lo habías preguntado": {"name": "Pablo", "emotionalContext": "Disculpa Pablo, tienes razón. Continuemos...", "confidence": 0.95}
            - "Ya te dije, mi nombre es Carlos": {"name": "Carlos", "emotionalContext": "Perdón por preguntar de nuevo. Gracias por tu paciencia...", "confidence": 0.9}
            - "María José Rodríguez": {"name": "María José", "lastname": "Rodríguez", "confidence": 0.95}

            CIUDADES Y JERGA:
            - "Soy rolo": {"city": "Bogotá", "state": "Cundinamarca", "confidence": 0.9}
            - "Ya te dije, en Medellín": {"city": "Medellín", "state": "Antioquia", "emotionalContext": "Perdón por preguntar de nuevo...", "confidence": 0.9}
            - "Soy de la nevera": {"city": "Bogotá", "state": "Cundinamarca", "confidence": 0.85}
            - "Soy cachaco": {"city": "Bogotá", "state": "Cundinamarca", "confidence": 0.85}
            - "Soy paisa": {"city": "Medellín", "state": "Antioquia", "confidence": 0.9}
            - "Soy de la ciudad de la eterna primavera": {"city": "Medellín", "state": "Antioquia", "confidence": 0.8}
            - "Soy costeño": {"city": "Barranquilla", "state": "Atlántico", "confidence": 0.8}
            - "Soy caleño": {"city": "Cali", "state": "Valle del Cauca", "confidence": 0.9}
            - "Soy de la sucursal del cielo": {"city": "Cali", "state": "Valle del Cauca", "confidence": 0.8}
            - "Soy opita": {"city": "Ibagué", "state": "Tolima", "confidence": 0.85}
            - "Soy capitalino": {"city": "Bogotá", "state": "Cundinamarca", "confidence": 0.85}

            EJEMPLOS DE AMBIGÜEDAD:
            - "Soy de Armenia": {"city": "Armenia", "needsClarification": {"city": "Hay varias Armenia en Colombia: Quindío, Antioquia, Bello. ¿Cuál es la tuya?"}}
            - "Vivo en La Dorada": {"city": "La Dorada", "needsClarification": {"city": "La Dorada existe en Caldas y Putumayo. ¿Cuál es?"}}

            EJEMPLOS DE CORRECCIÓN:
            - "Me equivoqué, no soy de Medellín sino de Envigado": {"city": "Envigado", "correction": true, "previousValue": "Medellín"}
            - "Perdón, mi nombre es Carlos no Juan": {"name": "Carlos", "correction": true, "previousValue": "Juan"}
            - "Es Barbosa no Armenia": {"city": "Barbosa", "correction": true, "previousValue": "Armenia"}

            EJEMPLOS DE CONFIRMACIÓN DE NOMBRE:
            - "Si, es correcto": {"isConfirmation": true, "confidence": 0.95}
            - "Sí, está bien": {"isConfirmation": true, "confidence": 0.95}
            - "Correcto": {"isConfirmation": true, "confidence": 0.9}
            - "Perfecto": {"isConfirmation": true, "confidence": 0.85}
            - "Es correcto": {"isConfirmation": true, "confidence": 0.9}
            - "Si, es": {"isConfirmation": true, "confidence": 0.9}
            - "Sí, es": {"isConfirmation": true, "confidence": 0.9}
            - "Si, es correcto": {"isConfirmation": true, "confidence": 0.95}
            - "Sí, es correcto": {"isConfirmation": true, "confidence": 0.95}

            EJEMPLOS DE ACEPTACIÓN DE TÉRMINOS:
            - "Sí": {"acceptsTerms": true, "confidence": 0.95}
            - "Si": {"acceptsTerms": true, "confidence": 0.95}
            - "Yes": {"acceptsTerms": true, "confidence": 0.95}
            - "Acepto": {"acceptsTerms": true, "confidence": 0.9}
            - "Estoy de acuerdo": {"acceptsTerms": true, "confidence": 0.9}
            - "Claro": {"acceptsTerms": true, "confidence": 0.85}
            - "Por supuesto": {"acceptsTerms": true, "confidence": 0.85}
            - "Perfecto": {"acceptsTerms": true, "confidence": 0.8}
            - "No": {"acceptsTerms": false, "confidence": 0.95}
            - "No acepto": {"acceptsTerms": false, "confidence": 0.9}
            - "No estoy de acuerdo": {"acceptsTerms": false, "confidence": 0.9}
            """,
            safeUserMessage,
            safePreviousContext,
            safeCurrentState
        );
    }

    private UserDataExtractionResult parseGeminiResponse(String response) {
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

                    JsonNode extractedData = objectMapper.readTree(text);

                    return UserDataExtractionResult.fromJson(extractedData);
                }
            }

            return UserDataExtractionResult.empty();

        } catch (Exception e) {
            System.err.println("Error al parsear respuesta de Gemini: " + e.getMessage());
            return UserDataExtractionResult.empty();
        }
    }
}