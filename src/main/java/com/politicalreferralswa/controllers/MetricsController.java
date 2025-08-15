package com.politicalreferralswa.controllers;

import com.politicalreferralswa.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics", description = "Métricas y estadísticas del sistema")
public class MetricsController {

    private final MetricsService metricsService;
    
    @Autowired
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/gemini")
    @Operation(
        summary = "Obtener métricas del sistema",
        description = "Endpoint para consultar estadísticas de precisión, velocidad y confianza del sistema de extracción inteligente. Proporciona métricas detalladas sobre el rendimiento del sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Métricas obtenidas exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "totalExtractions": 150,
                      "successfulExtractions": 142,
                      "precision": 0.946,
                      "averageResponseTimeMs": 1250.5,
                      "averageConfidence": 0.87,
                      "fieldExtractions": {
                        "name": 120,
                        "city": 95,
                        "acceptsTerms": 80,
                        "lastname": 85,
                        "state": 90
                      }
                    }
                    """
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
                    value = "\"Error al obtener métricas\""
                )
            )
        )
    })
    public Map<String, Object> getGeminiMetrics() {
        return metricsService.getMetrics();
    }
} 