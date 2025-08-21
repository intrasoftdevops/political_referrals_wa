package com.politicalreferralswa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
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
    }
} 