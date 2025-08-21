package com.politicalreferralswa.interceptor;

import com.politicalreferralswa.annotation.RequiresApiKey;
import com.politicalreferralswa.service.ApiKeyAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {
    
    private final ApiKeyAuthService apiKeyAuthService;
    
    @Autowired
    public ApiKeyInterceptor(ApiKeyAuthService apiKeyAuthService) {
        this.apiKeyAuthService = apiKeyAuthService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Solo interceptar métodos de controlador
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequiresApiKey requiresApiKey = handlerMethod.getMethodAnnotation(RequiresApiKey.class);
        
        // Log para debugging
        System.out.println("🔐 ApiKeyInterceptor: Endpoint: " + request.getRequestURI());
        System.out.println("🔐 ApiKeyInterceptor: Método: " + request.getMethod());
        System.out.println("🔐 ApiKeyInterceptor: Requiere API Key: " + (requiresApiKey != null));
        
        // Si no requiere API Key, continuar
        if (requiresApiKey == null || !requiresApiKey.required()) {
            System.out.println("🔐 ApiKeyInterceptor: Endpoint público, continuando...");
            return true;
        }
        
        // Obtener la API Key del header
        String apiKey = request.getHeader("X-API-Key");
        System.out.println("🔐 ApiKeyInterceptor: API Key recibida: " + (apiKey != null ? "SÍ" : "NO"));
        
        // Validar la API Key
        if (!apiKeyAuthService.isValidApiKey(apiKey)) {
            System.out.println("🔐 ApiKeyInterceptor: API Key inválida o faltante, denegando acceso");
            sendUnauthorizedResponse(response, requiresApiKey.value());
            return false;
        }
        
        // API Key válida, continuar
        System.out.println("🔐 ApiKeyInterceptor: API Key válida, permitiendo acceso");
        return true;
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String endpointDescription) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        
        String errorMessage = String.format("""
            {
              "error": "Unauthorized",
              "message": "API Key requerida para acceder a este endpoint",
              "endpoint": "%s",
              "status": 401,
              "timestamp": "%s"
            }
            """, 
            endpointDescription.isEmpty() ? "Endpoint protegido" : endpointDescription,
            java.time.LocalDateTime.now()
        );
        
        response.getWriter().write(errorMessage);
    }
}
