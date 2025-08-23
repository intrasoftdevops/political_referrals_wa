package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * Servicio para obtener métricas de analytics del usuario
 */
@Service
public class AnalyticsService {
    
    private final LocalAnalyticsService localAnalyticsService;
    
    public AnalyticsService(LocalAnalyticsService localAnalyticsService) {
        this.localAnalyticsService = localAnalyticsService;
    }
    
    /**
     * Obtiene las métricas del usuario usando el servicio local
     * 
     * @param userId ID del usuario (número de teléfono)
     * @param sessionId ID de sesión
     * @return Optional con los datos de analytics del usuario
     */
    public Optional<AnalyticsData> getUserStats(String userId, String sessionId) {
        try {
            System.out.println("AnalyticsService: Obteniendo métricas para usuario " + userId);
            
            // Usar el servicio local en lugar del endpoint externo
            LocalAnalyticsService.UserStatsResponse localStats = localAnalyticsService.getUserStats(userId);
            
            if (localStats != null) {
                // Convertir la respuesta local al formato existente
                AnalyticsData analyticsData = new AnalyticsData(
                    localStats.getUserId(),
                    localStats.getName(),
                    convertRankingData(localStats.getRanking()),
                    convertRegionData(localStats.getRegion()),
                    convertCityData(localStats.getCity()),
                    convertReferralsData(localStats.getReferrals())
                );
                
                System.out.println("AnalyticsService: Métricas obtenidas exitosamente del servicio local para " + userId);
                return Optional.of(analyticsData);
                
            } else {
                System.err.println("AnalyticsService: Respuesta vacía del servicio local");
                return Optional.empty();
            }
            
        } catch (Exception e) {
            System.err.println("AnalyticsService: Error al obtener métricas del servicio local: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    

    
    /**
     * Métodos de conversión para compatibilidad con el formato existente
     */
    private RankingData convertRankingData(LocalAnalyticsService.Ranking localRanking) {
        if (localRanking == null) return null;
        
        return new RankingData(
            convertPeriodData(localRanking.getToday()),
            convertPeriodData(localRanking.getWeek()),
            convertPeriodData(localRanking.getMonth())
        );
    }
    
    private PeriodData convertPeriodData(LocalAnalyticsService.RankingPeriod localPeriod) {
        if (localPeriod == null) return null;
        
        return new PeriodData(
            localPeriod.getPosition(),
            localPeriod.getPoints()
        );
    }
    
    private RegionData convertRegionData(LocalAnalyticsService.Region localRegion) {
        if (localRegion == null) return null;
        
        return new RegionData(
            localRegion.getPosition(),
            localRegion.getTotalParticipants(),
            localRegion.getPercentile()
        );
    }
    
    private CityData convertCityData(LocalAnalyticsService.City localCity) {
        if (localCity == null) return null;
        
        return new CityData(
            localCity.getPosition(),
            localCity.getTotalParticipants(),
            localCity.getPercentile()
        );
    }
    
    private ReferralsData convertReferralsData(LocalAnalyticsService.Referrals localReferrals) {
        if (localReferrals == null) return null;
        
        return new ReferralsData(
            localReferrals.getTotalInvited(),
            localReferrals.getActiveVolunteers(),
            localReferrals.getReferralsThisMonth(),
            localReferrals.getConversionRate(),
            localReferrals.getReferralPoints()
        );
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
            // Consultas generales
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
            
            // Consultas por período
            "cómo voy hoy",
            "como voy hoy",
            "cómo estoy hoy",
            "como estoy hoy",
            "mi ranking hoy",
            "mi posición hoy",
            "qué puesto tengo hoy",
            "que puesto tengo hoy",
            "cómo voy en la semana",
            "como voy en la semana",
            "cómo estoy en la semana",
            "como estoy en la semana",
            "mi ranking esta semana",
            "mi posición esta semana",
            "cómo voy en el mes",
            "como voy en el mes",
            "cómo estoy en el mes",
            "como estoy en el mes",
            "mi ranking este mes",
            "mi posición este mes",
            
            // Consultas por ubicación
            "cómo voy en mi ciudad",
            "como voy en mi ciudad",
            "cómo estoy en mi ciudad",
            "como estoy en mi ciudad",
            "mi ranking en mi ciudad",
            "mi posición en mi ciudad",
            "cómo voy en mi departamento",
            "como voy en mi departamento",
            "cómo estoy en mi departamento",
            "como estoy en mi departamento",
            "mi ranking en mi departamento",
            "mi posición en mi departamento",
            "cómo voy en mi país",
            "como voy en mi país",
            "cómo estoy en mi país",
            "como estoy en mi país",
            "mi ranking en colombia",
            "mi posición en colombia",
            "cómo voy en colombia",
            "como voy en colombia",
            
            // Consultas sobre referidos
            "cuántos referidos tengo",
            "cuantos referidos tengo",
            "cuántos referidos llevo",
            "cuantos referidos llevo",
            "cuántos referidos este mes",
            "cuantos referidos este mes",
            "mis referidos este mes",
            "cuántas personas he invitado",
            "cuantas personas he invitado",
            "cuántos voluntarios tengo",
            "cuantos voluntarios tengo",
            "mi tasa de conversión",
            "mi conversión de referidos",
            "cuántos referidos llevo",
            "cuantos referidos llevo",
            "cuántos referidos tengo hasta ahora",
            "cuantos referidos tengo hasta ahora",
            "cuántas personas he convocado",
            "cuantas personas he convocado",
            "mi red de referidos",
            "mis invitados",
            "cuántos referidos he conseguido",
            "cuantos referidos he conseguido",
            "cuántos referidos he logrado",
            "cuantos referidos he logrado",
            "cuántos referidos he sumado",
            "cuantos referidos he sumado",
            
            // Variaciones adicionales
            "qué tal voy",
            "que tal voy",
            "cómo me va",
            "como me va",
            "cómo va mi campaña",
            "como va mi campaña",
            "mi estado en la campaña",
            "mi progreso en la campaña",
            "cómo voy en la campaña",
            "como voy en la campaña"
        };
        
        for (String pattern : analyticsPatterns) {
            if (normalizedMessage.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clasifica el tipo de consulta de analytics para personalizar la respuesta
     * 
     * @param message Mensaje del usuario
     * @return Tipo de consulta detectada
     */
    public AnalyticsQueryType classifyAnalyticsQuery(String message) {
        String normalizedMessage = message.toLowerCase().trim();
        
        // Consultas específicas por período (prioridad alta)
        if (normalizedMessage.contains("hoy") || normalizedMessage.contains("este día") || 
            normalizedMessage.contains("el día") || normalizedMessage.contains("actual")) {
            return AnalyticsQueryType.TODAY;
        }
        if (normalizedMessage.contains("semana") || normalizedMessage.contains("esta semana") || 
            normalizedMessage.contains("la semana") || normalizedMessage.contains("semanal")) {
            return AnalyticsQueryType.WEEK;
        }
        if (normalizedMessage.contains("mes") || normalizedMessage.contains("este mes") || 
            normalizedMessage.contains("el mes") || normalizedMessage.contains("mensual")) {
            return AnalyticsQueryType.MONTH;
        }
        
        // Consultas específicas por ubicación (prioridad alta)
        if (normalizedMessage.contains("ciudad") || normalizedMessage.contains("en mi ciudad") || 
            normalizedMessage.contains("local") || normalizedMessage.contains("aquí")) {
            return AnalyticsQueryType.CITY;
        }
        if (normalizedMessage.contains("departamento") || normalizedMessage.contains("en mi departamento") || 
            normalizedMessage.contains("región") || normalizedMessage.contains("en mi región") ||
            normalizedMessage.contains("regional")) {
            return AnalyticsQueryType.REGION;
        }
        if (normalizedMessage.contains("país") || normalizedMessage.contains("en mi país") || 
            normalizedMessage.contains("colombia") || normalizedMessage.contains("nacional") ||
            normalizedMessage.contains("todo el país")) {
            return AnalyticsQueryType.COUNTRY;
        }
        
        // Consultas sobre referidos específicos (prioridad alta)
        if (normalizedMessage.contains("referidos") || normalizedMessage.contains("invitados") ||
            normalizedMessage.contains("personas") || normalizedMessage.contains("voluntarios") ||
            normalizedMessage.contains("conversión") || normalizedMessage.contains("tasa") ||
            normalizedMessage.contains("cuántos") || normalizedMessage.contains("cuantos") ||
            normalizedMessage.contains("cuántas") || normalizedMessage.contains("cuantas") ||
            normalizedMessage.contains("llevo") || normalizedMessage.contains("tengo") ||
            normalizedMessage.contains("he invitado") || normalizedMessage.contains("he convocado")) {
            return AnalyticsQueryType.REFERRALS;
        }
        
        // Consulta general (prioridad baja)
        return AnalyticsQueryType.GENERAL;
    }
    
    /**
     * Enum para clasificar tipos de consultas de analytics
     */
    public enum AnalyticsQueryType {
        TODAY,      // Rendimiento de hoy
        WEEK,       // Rendimiento de la semana
        MONTH,      // Rendimiento del mes
        CITY,       // Rendimiento en la ciudad
        REGION,     // Rendimiento en el departamento/región
        COUNTRY,    // Rendimiento nacional
        REFERRALS,  // Consulta específica sobre referidos
        GENERAL     // Consulta general de rendimiento
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