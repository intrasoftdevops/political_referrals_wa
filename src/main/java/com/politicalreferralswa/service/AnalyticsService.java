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
 * Servicio para obtener métricas de analytics del usuario
 */
@Service
public class AnalyticsService {
    
    @Value("${analytics.endpoint.url:http://localhost:8001}")
    private String analyticsEndpointUrl;
    
    @Value("${analytics.jwt.secret:your-secret-key}")
    private String jwtSecret;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final JWTService jwtService;
    
    public AnalyticsService(WebClient.Builder webClientBuilder, JWTService jwtService) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.jwtService = jwtService;
    }
    
    /**
     * Obtiene las métricas del usuario desde el endpoint de analytics
     * 
     * @param userId ID del usuario (número de teléfono)
     * @param sessionId ID de sesión
     * @return Optional con los datos de analytics del usuario
     */
    public Optional<AnalyticsData> getUserStats(String userId, String sessionId) {
        try {
            System.out.println("AnalyticsService: Obteniendo métricas para usuario " + userId);
            
            // Generar token JWT para el usuario
            String jwtToken = jwtService.generateToken(userId);
            
            // Llamar al endpoint de analytics
            String response = webClient.get()
                    .uri(analyticsEndpointUrl + "/api/v1/analytics/user-stats")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .header("X-Session-ID", sessionId)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                JsonNode responseJson = objectMapper.readTree(response);
                
                AnalyticsData analyticsData = new AnalyticsData(
                    responseJson.get("user_id").asText(),
                    responseJson.get("name").asText(),
                    extractRankingData(responseJson.get("ranking")),
                    extractRegionData(responseJson.get("region")),
                    extractCityData(responseJson.get("city")),
                    extractReferralsData(responseJson.get("referrals"))
                );
                
                System.out.println("AnalyticsService: Métricas obtenidas exitosamente para " + userId);
                return Optional.of(analyticsData);
                
            } else {
                System.err.println("AnalyticsService: Respuesta vacía del endpoint de analytics");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            System.err.println("AnalyticsService: Error al obtener métricas: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    /**
     * Detecta si una pregunta es sobre métricas personales
     * 
     * @param message Mensaje del usuario
     * @return true si es una pregunta de métricas
     */
    public boolean isAnalyticsQuestion(String message) {
        String normalizedMessage = message.toLowerCase().trim();
        
        // Patrones para detectar preguntas de métricas
        String[] analyticsPatterns = {
            "cómo voy",
            "como voy",
            "cómo estoy",
            "como estoy",
            "mi ranking",
            "mi posición",
            "qué puesto tengo",
            "que puesto tengo",
            "cuántos referidos",
            "cuantos referidos",
            "mis referidos",
            "mi rendimiento",
            "mis estadísticas",
            "mis métricas",
            "mi progreso",
            "cómo voy hoy",
            "como voy hoy",
            "cómo voy en la semana",
            "como voy en la semana",
            "cómo voy en el mes",
            "como voy en el mes",
            "qué puesto tengo en",
            "que puesto tengo en",
            "cuántos referidos tengo",
            "cuantos referidos tengo",
            "cuántos referidos llevo",
            "cuantos referidos llevo",
            "cuántos referidos este mes",
            "cuantos referidos este mes"
        };
        
        for (String pattern : analyticsPatterns) {
            if (normalizedMessage.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    private RankingData extractRankingData(JsonNode rankingNode) {
        if (rankingNode == null) return null;
        
        return new RankingData(
            extractPeriodData(rankingNode.get("today")),
            extractPeriodData(rankingNode.get("week")),
            extractPeriodData(rankingNode.get("month"))
        );
    }
    
    private PeriodData extractPeriodData(JsonNode periodNode) {
        if (periodNode == null) return null;
        
        return new PeriodData(
            periodNode.get("position").asInt(),
            periodNode.get("points").asInt()
        );
    }
    
    private RegionData extractRegionData(JsonNode regionNode) {
        if (regionNode == null) return null;
        
        return new RegionData(
            regionNode.get("position").asInt(),
            regionNode.get("total_participants").asInt(),
            regionNode.get("percentile").asDouble()
        );
    }
    
    private CityData extractCityData(JsonNode cityNode) {
        if (cityNode == null) return null;
        
        return new CityData(
            cityNode.get("position").asInt(),
            cityNode.get("total_participants").asInt(),
            cityNode.get("percentile").asDouble()
        );
    }
    
    private ReferralsData extractReferralsData(JsonNode referralsNode) {
        if (referralsNode == null) return null;
        
        return new ReferralsData(
            referralsNode.get("total_invited").asInt(),
            referralsNode.get("active_volunteers").asInt(),
            referralsNode.get("referrals_this_month").asInt(),
            referralsNode.get("conversion_rate").asDouble(),
            referralsNode.get("referral_points").asInt()
        );
    }
    
    // Clases de datos para las métricas
    public static class AnalyticsData {
        private final String userId;
        private final String name;
        private final RankingData ranking;
        private final RegionData region;
        private final CityData city;
        private final ReferralsData referrals;
        
        public AnalyticsData(String userId, String name, RankingData ranking, 
                           RegionData region, CityData city, ReferralsData referrals) {
            this.userId = userId;
            this.name = name;
            this.ranking = ranking;
            this.region = region;
            this.city = city;
            this.referrals = referrals;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getName() { return name; }
        public RankingData getRanking() { return ranking; }
        public RegionData getRegion() { return region; }
        public CityData getCity() { return city; }
        public ReferralsData getReferrals() { return referrals; }
    }
    
    public static class RankingData {
        private final PeriodData today;
        private final PeriodData week;
        private final PeriodData month;
        
        public RankingData(PeriodData today, PeriodData week, PeriodData month) {
            this.today = today;
            this.week = week;
            this.month = month;
        }
        
        public PeriodData getToday() { return today; }
        public PeriodData getWeek() { return week; }
        public PeriodData getMonth() { return month; }
    }
    
    public static class PeriodData {
        private final int position;
        private final int points;
        
        public PeriodData(int position, int points) {
            this.position = position;
            this.points = points;
        }
        
        public int getPosition() { return position; }
        public int getPoints() { return points; }
    }
    
    public static class RegionData {
        private final int position;
        private final int totalParticipants;
        private final double percentile;
        
        public RegionData(int position, int totalParticipants, double percentile) {
            this.position = position;
            this.totalParticipants = totalParticipants;
            this.percentile = percentile;
        }
        
        public int getPosition() { return position; }
        public int getTotalParticipants() { return totalParticipants; }
        public double getPercentile() { return percentile; }
    }
    
    public static class CityData {
        private final int position;
        private final int totalParticipants;
        private final double percentile;
        
        public CityData(int position, int totalParticipants, double percentile) {
            this.position = position;
            this.totalParticipants = totalParticipants;
            this.percentile = percentile;
        }
        
        public int getPosition() { return position; }
        public int getTotalParticipants() { return totalParticipants; }
        public double getPercentile() { return percentile; }
    }
    
    public static class ReferralsData {
        private final int totalInvited;
        private final int activeVolunteers;
        private final int referralsThisMonth;
        private final double conversionRate;
        private final int referralPoints;
        
        public ReferralsData(int totalInvited, int activeVolunteers, int referralsThisMonth, 
                           double conversionRate, int referralPoints) {
            this.totalInvited = totalInvited;
            this.activeVolunteers = activeVolunteers;
            this.referralsThisMonth = referralsThisMonth;
            this.conversionRate = conversionRate;
            this.referralPoints = referralPoints;
        }
        
        public int getTotalInvited() { return totalInvited; }
        public int getActiveVolunteers() { return activeVolunteers; }
        public int getReferralsThisMonth() { return referralsThisMonth; }
        public double getConversionRate() { return conversionRate; }
        public int getReferralPoints() { return referralPoints; }
    }
} 