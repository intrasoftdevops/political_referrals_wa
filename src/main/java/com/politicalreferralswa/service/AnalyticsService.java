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