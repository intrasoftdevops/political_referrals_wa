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

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
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
     * @param userMessage El mensaje del usuario
     * @param previousContext Contexto de conversación previa (opcional)
     * @return UserDataExtractionResult con los datos extraídos
     */
    public UserDataExtractionResult extractUserData(String userMessage, String previousContext) {
        long startTime = System.currentTimeMillis();
        try {
            System.out.println("GeminiService: Iniciando extracción de datos para mensaje: '" + userMessage + "'");
            String prompt = buildExtractionPrompt(userMessage, previousContext);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Map.of(
                "parts", Map.of("text", prompt)
            ));
            
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
                .block(Duration.ofSeconds(30));

            System.out.println("GeminiService: Respuesta recibida de Gemini: " + response);
            UserDataExtractionResult result = parseGeminiResponse(response);
            
            // Registrar métricas
            long responseTime = System.currentTimeMillis() - startTime;
            if (result.isSuccessful()) {
                metricsService.recordSuccessfulExtraction(result.getConfidence(), responseTime);
                // Registrar campos extraídos
                if (result.getName() != null) metricsService.recordFieldExtraction("name");
                if (result.getLastname() != null) metricsService.recordFieldExtraction("lastname");
                if (result.getCity() != null) metricsService.recordFieldExtraction("city");
                if (result.getState() != null) metricsService.recordFieldExtraction("state");
                if (result.getAcceptsTerms() != null) metricsService.recordFieldExtraction("acceptsTerms");
                if (result.getReferredByPhone() != null) metricsService.recordFieldExtraction("referredByPhone");
                if (result.getReferralCode() != null) metricsService.recordFieldExtraction("referralCode");
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

    private String buildExtractionPrompt(String userMessage, String previousContext) {
        return String.format("""
            Eres un extractor especializado en formularios políticos colombianos.

            CONTEXTO: Sistema de registro para campaña política en Colombia.
            MENSAJE: "%s"
            CONVERSACIÓN PREVIA: "%s"

            CAMPOS A EXTRAER:
            - name: Nombre (sin apellido, sin títulos)
            - lastname: Apellido(s) completo(s)
            - city: Ciudad colombiana específica  
            - state: Departamento/Estado colombiano
            - acceptsTerms: Si acepta términos explícitamente
            - referredByPhone: Número +57XXXXXXXXX
            - referralCode: Código alfanumérico de 8 dígitos

            CONOCIMIENTO COLOMBIANO:
            - Armenia: Quindío (principal), Antioquia
            - La Dorada: Caldas, Putumayo  
            - Sabaneta: Antioquia
            - Barbosa: Antioquia, Santander
            
            REGLAS DE AMBIGÜEDAD GEOGRÁFICA:
            - Si detectas una ciudad con múltiples ubicaciones (ej: Armenia, Barbosa), SIEMPRE marca ambigüedad en "city"
            - NO asumas automáticamente la ubicación principal
            - Para Armenia: "Hay varias Armenia en Colombia: Quindío, Antioquia, Bello. ¿Cuál es la tuya?"
            - Para La Dorada: "La Dorada existe en Caldas y Putumayo. ¿Cuál es?"
            - Para Barbosa: "Hay varias Barbosa en Colombia: Antioquia, Santander. ¿Cuál es la tuya?"
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

            RESPONDE EN JSON:
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
              "needsClarification": {
                "city": "mensaje específico|null",
                "other": "otra aclaración|null"
              },
              "confidence": 0.0-1.0
            }
            
            EJEMPLOS DE AMBIGÜEDAD:
            - Para "Soy de Armenia": {"city": "Armenia", "needsClarification": {"city": "Hay varias Armenia en Colombia: Quindío, Antioquia, Bello. ¿Cuál es la tuya?"}}
            - Para "Vivo en La Dorada": {"city": "La Dorada", "needsClarification": {"city": "La Dorada existe en Caldas y Putumayo. ¿Cuál es?"}}
            
            EJEMPLOS DE CORRECCIÓN:
            - Para "Me equivoqué, no soy de Medellín sino de Envigado": {"city": "Envigado", "correction": true, "previousValue": "Medellín"}
            - Para "Perdón, mi nombre es Carlos no Juan": {"name": "Carlos", "correction": true, "previousValue": "Juan"}
            - Para "Es Barbosa no Armenia": {"city": "Barbosa", "correction": true, "previousValue": "Armenia"}
            """, userMessage, previousContext != null ? previousContext : "");
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