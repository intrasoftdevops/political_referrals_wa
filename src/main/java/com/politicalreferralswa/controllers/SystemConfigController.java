package com.politicalreferralswa.controllers;

import com.politicalreferralswa.service.SystemConfigService;
import com.politicalreferralswa.annotation.RequiresApiKey;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api/system")
@Tag(
    name = "🎛️ System Configuration", 
    description = "API para configurar el sistema globalmente. Permite controlar en tiempo real el estado de la IA del sistema, habilitando o deshabilitando la atención automática para usuarios COMPLETED. **Requiere autenticación por API Key.**"
)
@SecurityRequirement(name = "ApiKeyAuth")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * Endpoint para obtener el estado actual de la IA del sistema
     */
    @GetMapping("/ai/status")
    @CrossOrigin(origins = "*") // Permitir CORS para este endpoint
    @Operation(
        summary = "🔍 Obtener Estado de la IA",
        description = "Obtiene el estado actual de la IA del sistema. Permite verificar si la IA está habilitada o deshabilitada para todos los usuarios COMPLETED."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "✅ Estado obtenido exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "Respuesta con el estado actual de la IA",
                    example = """
                    {
                      "aiEnabled": true,
                      "status": "HABILITADA",
                      "message": "La IA está habilitada y los usuarios COMPLETED interactúan con ella"
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> getAIStatus() {
        // Sincronizar estado desde BD antes de responder
        systemConfigService.refreshAIStateFromDatabase();
        
        Map<String, Object> response = new HashMap<>();
        response.put("aiEnabled", systemConfigService.isAIEnabled());
        response.put("status", systemConfigService.getAIStatus());
        response.put("message", systemConfigService.isAIEnabled() 
            ? "La IA está habilitada y los usuarios COMPLETED interactúan con ella" 
            : "La IA está deshabilitada y los usuarios COMPLETED serán atendidos por agentes humanos");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para habilitar la IA del sistema
     */
    @PostMapping("/ai/enable")
    @CrossOrigin(origins = "*") // Permitir CORS para este endpoint
    @RequiresApiKey("Habilitar IA del sistema")
    @Operation(
        summary = "🟢 Habilitar IA",
        description = "Habilita la IA del sistema para que todos los usuarios COMPLETED interactúen con ella. Los usuarios recibirán respuestas automáticas de la IA en lugar de ser atendidos por agentes humanos. **Requiere API Key válida.**"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "✅ IA habilitada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "Respuesta cuando la IA se habilita exitosamente",
                    example = """
                    {
                      "aiEnabled": true,
                      "status": "HABILITADA",
                      "message": "La IA del sistema ha sido habilitada. Todos los usuarios COMPLETED ahora interactuarán con la IA."
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> enableAI() {
        // Sincronizar estado desde BD antes de cambiar
        systemConfigService.refreshAIStateFromDatabase();
        
        systemConfigService.enableAI();
        
        Map<String, Object> response = new HashMap<>();
        response.put("aiEnabled", true);
        response.put("status", "HABILITADA");
        response.put("message", "La IA del sistema ha sido habilitada. Todos los usuarios COMPLETED ahora interactuarán con la IA.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para deshabilitar la IA del sistema
     */
    @PostMapping("/ai/disable")
    @CrossOrigin(origins = "*") // Permitir CORS para este endpoint
    @RequiresApiKey("Deshabilitar IA del sistema")
    @Operation(
        summary = "🔴 Deshabilitar IA",
        description = "Deshabilita la IA del sistema para que los usuarios COMPLETED sean atendidos por agentes humanos a través de WATI. Útil para mantenimiento, cambio de turnos o cuando se requiere intervención humana. **Requiere API Key válida.**"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "✅ IA deshabilitada exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "Respuesta cuando la IA se deshabilita exitosamente",
                    example = """
                    {
                      "aiEnabled": false,
                      "status": "DESHABILITADA",
                      "message": "La IA del sistema ha sido deshabilitada. Los usuarios COMPLETED ahora serán atendidos por agentes humanos a través de WATI."
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> disableAI() {
        // Sincronizar estado desde BD antes de cambiar
        systemConfigService.refreshAIStateFromDatabase();
        
        systemConfigService.disableAI();
        
        Map<String, Object> response = new HashMap<>();
        response.put("aiEnabled", false);
        response.put("status", "DESHABILITADA");
        response.put("message", "La IA del sistema ha sido deshabilitada. Los usuarios COMPLETED ahora serán atendidos por agentes humanos a través de WATI.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para cambiar el estado de la IA del sistema
     */
    @PostMapping("/ai/toggle")
    @CrossOrigin(origins = "*") // Permitir CORS para este endpoint
    @RequiresApiKey("Cambiar estado de la IA del sistema")
    @Operation(
        summary = "🔄 Cambiar Estado de la IA",
        description = "Cambia el estado actual de la IA del sistema. Si está habilitada la deshabilita, si está deshabilitada la habilita. Útil para cambios rápidos de estado sin especificar el valor final. **Requiere API Key válida.**"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "✅ Estado cambiado exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    description = "Respuesta cuando se cambia el estado de la IA",
                    example = """
                    {
                      "aiEnabled": false,
                      "status": "DESHABILITADA",
                      "message": "La IA del sistema ha sido deshabilitada. Los usuarios COMPLETED ahora serán atendidos por agentes humanos a través de WATI."
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<Map<String, Object>> toggleAI() {
        // Sincronizar estado desde BD antes de cambiar
        systemConfigService.refreshAIStateFromDatabase();
        
        boolean currentStatus = systemConfigService.isAIEnabled();
        systemConfigService.setAIEnabled(!currentStatus);
        
        Map<String, Object> response = new HashMap<>();
        response.put("aiEnabled", !currentStatus);
        response.put("status", systemConfigService.getAIStatus());
        response.put("message", !currentStatus 
            ? "La IA del sistema ha sido habilitada. Todos los usuarios COMPLETED ahora interactuarán con la IA."
            : "La IA del sistema ha sido deshabilitada. Los usuarios COMPLETED ahora serán atendidos por agentes humanos a través de WATI.");
        
        return ResponseEntity.ok(response);
    }


}
