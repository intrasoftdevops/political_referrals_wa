package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Servicio para analizar consultas de tribus usando el chatbot IA
 */
@Service
public class TribalAnalysisService {
    
    @Value("${CHATBOT_IA_URL:http://localhost:8000}")
    private String chatbotIaUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public TribalAnalysisService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Analiza una consulta para determinar si es sobre tribus y obtener respuesta IA
     * 
     * @param userMessage Mensaje del usuario
     * @param sessionId ID de sesión (teléfono del usuario)
     * @param userData Datos del usuario (opcional)
     * @return Optional con el resultado del análisis
     */
    public Optional<TribalAnalysisResult> analyzeTribalRequest(String userMessage, String sessionId, Map<String, Object> userData) {
        try {
            System.out.println("TribalAnalysisService: Analizando consulta de tribu para sesión " + sessionId);
            System.out.println("TribalAnalysisService: Mensaje: '" + userMessage + "'");
            
            // Construir el payload para el chatbot IA
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("query", userMessage);
            requestPayload.put("session_id", sessionId);
            requestPayload.put("user_data", userData != null ? userData : new HashMap<>());
            
            // Llamar al endpoint del chatbot IA
            String response = webClient.post()
                    .uri(chatbotIaUrl + "/tribal-analysis")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                
                boolean isTribalRequest = responseJson.get("is_tribal_request").asBoolean();
                String aiResponse = responseJson.get("ai_response").asText();
                String referralCode = responseJson.path("referral_code").asText("");
                String userName = responseJson.path("user_name").asText("");
                boolean shouldGenerateLink = responseJson.get("should_generate_link").asBoolean();
                
                TribalAnalysisResult result = new TribalAnalysisResult(
                    isTribalRequest,
                    aiResponse,
                    referralCode,
                    userName,
                    shouldGenerateLink
                );
                
                System.out.println("TribalAnalysisService: Análisis completado - Es tribu: " + isTribalRequest);
                return Optional.of(result);
                
            } else {
                System.err.println("TribalAnalysisService: Respuesta vacía del chatbot IA");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            System.err.println("TribalAnalysisService: Error al analizar consulta de tribu: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    /**
     * Resultado del análisis de tribus
     */
    public static class TribalAnalysisResult {
        private final boolean isTribalRequest;
        private final String aiResponse;
        private final String referralCode;
        private final String userName;
        private final boolean shouldGenerateLink;
        
        public TribalAnalysisResult(boolean isTribalRequest, String aiResponse, String referralCode, 
                                  String userName, boolean shouldGenerateLink) {
            this.isTribalRequest = isTribalRequest;
            this.aiResponse = aiResponse;
            this.referralCode = referralCode;
            this.userName = userName;
            this.shouldGenerateLink = shouldGenerateLink;
        }
        
        // Getters
        public boolean isTribalRequest() { return isTribalRequest; }
        public String getAiResponse() { return aiResponse; }
        public String getReferralCode() { return referralCode; }
        public String getUserName() { return userName; }
        public boolean shouldGenerateLink() { return shouldGenerateLink; }
    }
} 