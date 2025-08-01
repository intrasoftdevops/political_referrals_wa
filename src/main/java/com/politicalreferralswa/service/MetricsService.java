package com.politicalreferralswa.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.time.Instant;
import java.util.Map;

@Service
public class MetricsService {
    
    private final AtomicLong totalExtractions = new AtomicLong(0);
    private final AtomicLong successfulExtractions = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicReference<Double> totalConfidence = new AtomicReference<>(0.0);
    private final Map<String, AtomicLong> fieldExtractions = new ConcurrentHashMap<>();
    
    /**
     * Registra una extracción exitosa
     */
    public void recordSuccessfulExtraction(double confidence, long responseTimeMs) {
        totalExtractions.incrementAndGet();
        successfulExtractions.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
        totalConfidence.updateAndGet(current -> current + confidence);
    }
    
    /**
     * Registra una extracción fallida
     */
    public void recordFailedExtraction(long responseTimeMs) {
        totalExtractions.incrementAndGet();
        totalResponseTime.addAndGet(responseTimeMs);
    }
    
    /**
     * Registra extracción de un campo específico
     */
    public void recordFieldExtraction(String fieldName) {
        fieldExtractions.computeIfAbsent(fieldName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    /**
     * Obtiene métricas actuales
     */
    public Map<String, Object> getMetrics() {
        long total = totalExtractions.get();
        long successful = successfulExtractions.get();
        long totalTime = totalResponseTime.get();
        double totalConf = totalConfidence.get();
        
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("totalExtractions", total);
        metrics.put("successfulExtractions", successful);
        metrics.put("precision", total > 0 ? (double) successful / total : 0.0);
        metrics.put("averageResponseTimeMs", total > 0 ? (double) totalTime / total : 0.0);
        metrics.put("averageConfidence", successful > 0 ? totalConf / successful : 0.0);
        
        // Métricas por campo
        Map<String, Long> fieldMetrics = new ConcurrentHashMap<>();
        fieldExtractions.forEach((field, count) -> fieldMetrics.put(field, count.get()));
        metrics.put("fieldExtractions", fieldMetrics);
        
        return metrics;
    }
    
    /**
     * Imprime métricas en consola
     */
    public void printMetrics() {
        Map<String, Object> metrics = getMetrics();
        System.out.println("\n=== MÉTRICAS DE GEMINI ===");
        System.out.println("Total extracciones: " + metrics.get("totalExtractions"));
        System.out.println("Extracciones exitosas: " + metrics.get("successfulExtractions"));
        System.out.println("Precisión: " + String.format("%.2f%%", (Double) metrics.get("precision") * 100));
        System.out.println("Tiempo promedio: " + String.format("%.2f ms", (Double) metrics.get("averageResponseTimeMs")));
        System.out.println("Confianza promedio: " + String.format("%.2f", (Double) metrics.get("averageConfidence")));
        
        @SuppressWarnings("unchecked")
        Map<String, Long> fieldMetrics = (Map<String, Long>) metrics.get("fieldExtractions");
        if (!fieldMetrics.isEmpty()) {
            System.out.println("Extracciones por campo:");
            fieldMetrics.forEach((field, count) -> 
                System.out.println("  " + field + ": " + count));
        }
        System.out.println("========================\n");
    }
} 