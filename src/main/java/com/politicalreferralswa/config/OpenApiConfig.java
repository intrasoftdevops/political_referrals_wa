package com.politicalreferralswa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Political Referrals WA API")
                        .version("1.0.0")
                        .description("""
                            API REST para el chatbot político colombiano con extracción inteligente de datos usando Gemini AI.
                            
                            ## Características Principales
                            - **Extracción automática** de datos en conversación natural
                            - **Soporte para WhatsApp** (Wati API) y **Telegram**
                            - **Integración con Gemini AI** para procesamiento inteligente
                            - **Sistema de métricas** para monitoreo de rendimiento
                            
                            ## Endpoints Disponibles
                            - `/api/wati-webhook` - Webhook para mensajes de WhatsApp
                            - `/telegram_webhook` - Webhook para mensajes de Telegram
                            - `/api/message` - API directa para envío de mensajes
                            - `/api/metrics/gemini` - Métricas del sistema de extracción
                            """)
                        .contact(new Contact()
                                .name("Political Referrals WA Team")
                                .email("support@politicalreferrals.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("/") // URL Relativa
                                .description("Default Server URL")
                ))
                .tags(List.of(
                        new Tag().name("WhatsApp").description("Endpoints relacionados con WhatsApp a través de Wati API"),
                        new Tag().name("Telegram").description("Endpoints relacionados con Telegram Bot API"),
                        new Tag().name("Messages").description("API directa para envío de mensajes"),
                        new Tag().name("Metrics").description("Métricas y estadísticas del sistema"),
                        new Tag().name("Admin").description("Operaciones de administración de usuarios") // Añadimos el tag para Admin
                ));
    }
} 