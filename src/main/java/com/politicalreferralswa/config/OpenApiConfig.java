package com.politicalreferralswa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("🎛️ Political Referrals WA - API de Control del Sistema")
                        .description("API para controlar el sistema de IA del chatbot político colombiano. **Los endpoints de control requieren autenticación por API Key.**")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Intrasoft Development Team")
                                .email("devops@intrasoft.com")))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key requerida para acceder a los endpoints de control del sistema")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));
        
        // Configurar servidores según el perfil activo
        if ("local".equals(activeProfile)) {
            // Para desarrollo local
            openAPI.addServersItem(new Server()
                    .url("http://localhost:8080")
                    .description("Servidor local de desarrollo"));
        } else if ("dev".equals(activeProfile)) {
            // Para desarrollo desplegado
            openAPI.addServersItem(new Server()
                    .url("https://political-referrals-wa-dev-331919709696.us-central1.run.app")
                    .description("Servidor de desarrollo en Google Cloud Run"));
        } else if ("prod".equals(activeProfile)) {
            // Para producción
            openAPI.addServersItem(new Server()
                    .url("https://political-referrals-wa-prod-331919709696.us-central1.run.app")
                    .description("Servidor de producción en Google Cloud Run"));
        } else {
            // Fallback por defecto
            openAPI.addServersItem(new Server()
                    .url("https://political-referrals-wa-dev-331919709696.us-central1.run.app")
                    .description("Servidor por defecto (desarrollo)"));
        }
        
        return openAPI;
    }
} 