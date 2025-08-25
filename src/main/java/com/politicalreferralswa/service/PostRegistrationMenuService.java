package com.politicalreferralswa.service;

import com.google.cloud.Timestamp;
import com.politicalreferralswa.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para manejar el menú post-registro y las sesiones de DQBot
 */
@Service
public class PostRegistrationMenuService {

    private final WatiApiService watiApiService;
    private final AnalyticsService analyticsService;
    private final WebClient webClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Configuración del timeout de DQBot (30 minutos)
    private static final long DQBOT_TIMEOUT_MINUTES = 30;
    
    // URL del chatbotIA (configurable)
    private final String chatbotIAUrl;

    @Autowired
    public PostRegistrationMenuService(WatiApiService watiApiService, 
                                     AnalyticsService analyticsService,
                                     WebClient.Builder webClientBuilder,
                                     @org.springframework.beans.factory.annotation.Value("${CHATBOT_IA_URL:http://localhost:8000}") String chatbotIAUrl) {
        this.watiApiService = watiApiService;
        this.analyticsService = analyticsService;
        this.webClient = webClientBuilder.build();
        this.chatbotIAUrl = chatbotIAUrl;
        
        System.out.println("PostRegistrationMenuService: ========================================");
        System.out.println("PostRegistrationMenuService: Inicializado con chatbotIA URL: " + this.chatbotIAUrl);
        System.out.println("PostRegistrationMenuService: WebClient configurado: " + (this.webClient != null ? "OK" : "ERROR"));
        System.out.println("PostRegistrationMenuService: WatiApiService configurado: " + (this.watiApiService != null ? "OK" : "ERROR"));
        System.out.println("PostRegistrationMenuService: AnalyticsService configurado: " + (this.analyticsService != null ? "OK" : "ERROR"));
        System.out.println("PostRegistrationMenuService: Scheduler configurado: " + (this.scheduler != null ? "OK" : "ERROR"));
        System.out.println("PostRegistrationMenuService: Servicio inicializado correctamente");
        System.out.println("PostRegistrationMenuService: ========================================");
    }

    /**
     * Verifica si un usuario debe ver el menú post-registro
     */
    public boolean shouldShowPostRegistrationMenu(User user) {
        return user != null && "COMPLETED".equals(user.getChatbot_state());
    }

    /**
     * Muestra el menú post-registro con 4 botones
     */
    public void showPostRegistrationMenu(String phoneNumber) {
        System.out.println("PostRegistrationMenuService: Mostrando menú principal para usuario: " + phoneNumber);
        
        String menuMessage = """
            🎯 *Menú de Opciones*
            
            ¡Hola! Ya completaste tu registro. ¿En qué puedo ayudarte?
            
            Selecciona una opción:""";
        
        System.out.println("PostRegistrationMenuService: Enviando menú principal con botones: 🤖 DQBot, 🧑‍💼 Agente, 📋 Otras opciones");
        
        watiApiService.sendInteractiveButtonMessageSync(
            phoneNumber, 
            menuMessage,
            "🤖 DQBot", 
            "🧑‍💼 Agente",
            "📋 Otras opciones"
        );
        
        System.out.println("PostRegistrationMenuService: Menú principal enviado exitosamente");
    }

    /**
     * Maneja la selección de botones del menú post-registro
     */
    public void handleMenuSelection(String phoneNumber, String buttonText, User user) {
        System.out.println("PostRegistrationMenuService: Botón seleccionado: '" + buttonText + "' para usuario: " + phoneNumber);
        
        switch (buttonText) {
            case "🤖 DQBot":
                System.out.println("PostRegistrationMenuService: Activando DQBot...");
                handleDQBotRequest(phoneNumber, user);
                break;
            case "🧑‍💼 Agente":
                System.out.println("PostRegistrationMenuService: Conectando con agente...");
                handleHumanAgentRequest(phoneNumber, user);
                break;
            case "📋 Otras opciones":
                System.out.println("PostRegistrationMenuService: Mostrando submenú...");
                showSubMenu(phoneNumber, user);
                break;
            case "✅ ¿Cómo voy?":
                System.out.println("PostRegistrationMenuService: Mostrando analytics...");
                handleAnalyticsRequest(phoneNumber, user);
                break;
            case "📣 Compartir link":
                System.out.println("PostRegistrationMenuService: Generando link de referidos...");
                handleShareLinkRequest(phoneNumber, user);
                break;
            case "↩️ Volver":
                System.out.println("PostRegistrationMenuService: Volviendo al menú principal...");
                showPostRegistrationMenu(phoneNumber);
                break;
            default:
                System.out.println("PostRegistrationMenuService: Botón no reconocido: '" + buttonText + "'. Verificando si es solicitud de menú...");
                
                // Verificar si es una solicitud de menú
                if (isMenuRequest(buttonText)) {
                    System.out.println("PostRegistrationMenuService: Es solicitud de menú, mostrando menú principal...");
                    showPostRegistrationMenu(phoneNumber);
                } else {
                    System.out.println("PostRegistrationMenuService: No es solicitud de menú, enviando mensaje de error...");
                    // Respuesta por defecto si no se reconoce el botón
                    watiApiService.sendWhatsAppMessageSync(phoneNumber, 
                        "Por favor, selecciona una de las opciones del menú.");
                }
        }
    }

    /**
     * Maneja la solicitud de analytics "¿Cómo voy?"
     */
    private void handleAnalyticsRequest(String phoneNumber, User user) {
        try {
            // Obtener métricas del usuario
            var analyticsData = analyticsService.getUserStats(phoneNumber, phoneNumber);
            
            if (analyticsData.isPresent()) {
                var data = analyticsData.get();
                String response = buildAnalyticsResponse(data);
                watiApiService.sendWhatsAppMessageSync(phoneNumber, response);
            } else {
                watiApiService.sendWhatsAppMessageSync(phoneNumber, 
                    "Lo siento, no pude obtener tus métricas en este momento. Por favor, intenta más tarde.");
            }
        } catch (Exception e) {
            System.err.println("Error al obtener analytics: " + e.getMessage());
            watiApiService.sendWhatsAppMessageSync(phoneNumber, 
                "Hubo un error al obtener tus métricas. Por favor, intenta más tarde.");
        }
    }

    /**
     * Construye la respuesta de analytics
     */
    private String buildAnalyticsResponse(AnalyticsService.AnalyticsData data) {
        StringBuilder response = new StringBuilder();
        response.append("📊 *Tus Métricas*\n\n");
        
        if (data.getCity() != null) {
            response.append("🏙️ *En tu ciudad:*\n");
            response.append("Posición: #").append(data.getCity().getPosition())
                   .append(" de ").append(data.getCity().getTotalParticipants()).append(" participantes\n\n");
        }
        
        if (data.getRegion() != null) {
            response.append("🗺️ *En Colombia:*\n");
            response.append("Posición: #").append(data.getRegion().getPosition())
                   .append(" de ").append(data.getRegion().getTotalParticipants()).append(" participantes\n\n");
        }
        
        if (data.getRanking() != null) {
            response.append("📈 *Rendimiento:*\n");
            if (data.getRanking().getToday() != null) {
                response.append("• Hoy: #").append(data.getRanking().getToday().getPosition())
                       .append(" con ").append(data.getRanking().getToday().getPoints()).append(" puntos\n");
            }
            if (data.getRanking().getWeek() != null) {
                response.append("• Esta semana: #").append(data.getRanking().getWeek().getPosition()).append("\n");
            }
            if (data.getRanking().getMonth() != null) {
                response.append("• Este mes: #").append(data.getRanking().getMonth().getPosition()).append("\n");
            }
            response.append("\n");
        }
        
        if (data.getReferrals() != null) {
            response.append("👥 *Referidos:*\n");
            response.append("• Total invitados: ").append(data.getReferrals().getTotalInvited()).append(" personas\n");
            response.append("• Voluntarios activos: ").append(data.getReferrals().getActiveVolunteers()).append(" personas\n");
            response.append("• Referidos este mes: ").append(data.getReferrals().getReferralsThisMonth()).append("\n");
            response.append("• Tasa de conversión: ").append(String.format("%.1f", data.getReferrals().getConversionRate())).append("%\n\n");
        }
        
        response.append("💪 *Próximos pasos:*\n");
        response.append("• Mantén el momentum\n");
        response.append("• Fortalece tu red local\n");
        response.append("• Busca nuevos referidos\n\n");
        
        response.append("¡Estás construyendo un movimiento increíble! 🚀");
        
        return response.toString();
    }

    /**
     * Muestra el submenú con opciones adicionales
     */
    private void showSubMenu(String phoneNumber, User user) {
        System.out.println("PostRegistrationMenuService: Mostrando submenú para usuario: " + phoneNumber);
        
        String subMenuMessage = """
            📋 *Otras Opciones*
            
            ¿Qué te gustaría hacer?
            
            Selecciona una opción:""";
        
        System.out.println("PostRegistrationMenuService: Enviando submenú con botones: ✅ ¿Cómo voy?, 📣 Compartir link, ↩️ Volver");
        
        watiApiService.sendInteractiveButtonMessageSync(
            phoneNumber, 
            subMenuMessage,
            "✅ ¿Cómo voy?", 
            "📣 Compartir link",
            "↩️ Volver"
        );
        
        System.out.println("PostRegistrationMenuService: Submenú enviado exitosamente");
    }

    /**
     * Maneja la solicitud de compartir link de referidos
     */
    private void handleShareLinkRequest(String phoneNumber, User user) {
        if (user.getReferral_code() != null && !user.getReferral_code().isEmpty()) {
            String shareMessage = buildShareLinkMessage(user);
            watiApiService.sendWhatsAppMessageSync(phoneNumber, shareMessage);
        } else {
            watiApiService.sendWhatsAppMessageSync(phoneNumber, 
                "Lo siento, no tienes un código de referido disponible en este momento.");
        }
    }

    /**
     * Construye el mensaje para compartir el link de referidos
     */
    private String buildShareLinkMessage(User user) {
        String displayName = user.getName();
        if (user.getLastname() != null && !user.getLastname().trim().isEmpty()) {
            displayName += " " + user.getLastname();
        }
        
        // Construir el mensaje con mejor formato para evitar problemas de espacios
        String whatsappMessage = String.format(
            "Amigos, soy %s y quiero invitarte a unirte a la campaña de Daniel Quintero a la Presidencia:\n\n%s",
            displayName,
            buildWhatsAppInviteLink(user)
        );
        
        System.out.println("PostRegistrationMenuService: Mensaje completo para compartir: '" + whatsappMessage + "'");
        
        return whatsappMessage;
    }
    
    /**
     * Construye el link de invitación de WhatsApp
     */
    private String buildWhatsAppInviteLink(User user) {
        try {
            String referralCode = user.getReferral_code();
            if (referralCode == null || referralCode.isEmpty()) {
                return "Error: No se pudo generar el link de referidos";
            }
            
            // Construir el mensaje de WhatsApp con formato más limpio
            String whatsappText = String.format("Hola, vengo referido por %s", referralCode);
            System.out.println("PostRegistrationMenuService: Texto original para WhatsApp: '" + whatsappText + "'");
            
            // Codificar el texto de manera más robusta
            String encodedText = URLEncoder.encode(whatsappText, StandardCharsets.UTF_8.toString())
                .replace("+", "%20"); // Asegurar que los espacios se codifiquen como %20 en lugar de +
            System.out.println("PostRegistrationMenuService: Texto codificado: '" + encodedText + "'");
            
            // Usar el número de WhatsApp según el ambiente (dev/prod)
            String whatsappNumber = getWhatsAppInviteNumber();
            
            String finalLink = String.format("https://wa.me/%s?text=%s", whatsappNumber, encodedText);
            System.out.println("PostRegistrationMenuService: Link final generado: '" + finalLink + "'");
            
            return finalLink;
            
        } catch (UnsupportedEncodingException e) {
            System.err.println("ERROR: No se pudo codificar el link de WhatsApp: " + e.getMessage());
            return "Error al generar el link de referidos";
        }
    }
    
    /**
     * Obtiene el número de WhatsApp según el ambiente
     */
    private String getWhatsAppInviteNumber() {
        // Obtener el perfil activo del sistema
        String activeProfile = System.getProperty("spring.profiles.active");
        if (activeProfile == null) {
            activeProfile = "local"; // Por defecto
        }
        
        if ("prod".equals(activeProfile)) {
            return "573019700355"; // Número de producción
        } else {
            return "573224029924"; // Número de desarrollo
        }
    }

    /**
     * Maneja la solicitud de hablar con un agente humano
     */
    private void handleHumanAgentRequest(String phoneNumber, User user) {
        String message = """
            🧑‍💼 *Conectando con agente humano*
            
            Tu solicitud ha sido enviada a nuestro equipo de agentes humanos.
            
            Un agente se pondrá en contacto contigo lo antes posible.
            
            💡 *Tip:* Escribe "Menú" para volver al menú principal en cualquier momento.
            
            ¡Gracias por tu paciencia! 🙏
            """;
        
        watiApiService.sendWhatsAppMessageSync(phoneNumber, message);
        
        // Aquí podrías implementar la lógica para notificar a los agentes humanos
        // Por ejemplo, enviar a un canal de WATI específico para agentes
    }

    /**
     * Maneja la solicitud de activar DQBot
     */
    private void handleDQBotRequest(String phoneNumber, User user) {
        // Activar DQBot para este usuario
        user.setQbot_active(true);
        user.setQbot_session_start(Timestamp.now());
        user.setLast_interaction(Timestamp.now());
        
        String welcomeMessage = """
            🤖 *DQBot Activado*
            
            ¡Hola! Soy DQBot, tu asistente de IA para la campaña de Daniel Quintero.
            
            Puedo ayudarte con:
            • Preguntas sobre la campaña
            • Información sobre políticas
            • Dudas sobre el proceso electoral
            • Y mucho más...
            
            *Escribe tu pregunta y te responderé de inmediato!*
            
            💡 *Tip:* Escribe "Menú" para volver al menú principal en cualquier momento.
            
            ⏰ *Nota:* Mi sesión se desactivará automáticamente después de 30 minutos de inactividad.
            """;
        
        watiApiService.sendWhatsAppMessageSync(phoneNumber, welcomeMessage);
        
        // Programar el timeout de DQBot
        scheduleDQBotTimeout(phoneNumber, user);
    }

    /**
     * Programa el timeout de DQBot para desactivarlo después de 30 minutos
     */
    private void scheduleDQBotTimeout(String phoneNumber, User user) {
        scheduler.schedule(() -> {
            try {
                // Verificar si el usuario ha tenido interacción reciente
                if (user.getLast_interaction() != null) {
                    long minutesSinceLastInteraction = Duration.between(
                        user.getLast_interaction().toDate().toInstant(), 
                        Instant.now()
                    ).toMinutes();
                    
                    if (minutesSinceLastInteraction >= DQBOT_TIMEOUT_MINUTES) {
                        // Desactivar DQBot
                        user.setQbot_active(false);
                        user.setQbot_session_start(null);
                        
                        String timeoutMessage = """
                            ⏰ *Sesión de DQBot Expirada*
                            
                            Han pasado 30 minutos sin actividad, por lo que he desactivado mi sesión.
                            
                            Si necesitas ayuda nuevamente, escribe cualquier mensaje y te mostraré el menú de opciones.
                            
                            ¡Hasta pronto! 👋
                            """;
                        
                        watiApiService.sendWhatsAppMessageSync(phoneNumber, timeoutMessage);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error en timeout de DQBot: " + e.getMessage());
            }
        }, DQBOT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Procesa un mensaje del usuario cuando DQBot está activo
     */
    public void processDQBotMessage(String phoneNumber, String message, User user) {
        // Verificar si el usuario quiere ver el menú
        if (isMenuRequest(message)) {
            deactivateDQBot(user);
            String menuMessage = """
                🎯 *Volviendo al Menú Principal*
                
                He cerrado tu sesión de DQBot y te muestro las opciones disponibles.
                """;
            watiApiService.sendWhatsAppMessageSync(phoneNumber, menuMessage);
            showPostRegistrationMenu(phoneNumber);
            return;
        }
        
        // Actualizar timestamp de última interacción
        user.setLast_interaction(Timestamp.now());
        
        try {
            // Enviar consulta al chatbotIA
            String aiResponse = queryChatbotIA(message, phoneNumber);
            
            // Agregar firma "Te respondió DQBot" al final de la respuesta
            String formattedResponse = aiResponse + "\n\n*Te respondió DQBot*";
            
            System.out.println("PostRegistrationMenuService: Enviando respuesta de DQBot con formato: '" + formattedResponse + "'");
            
            watiApiService.sendWhatsAppMessageSync(phoneNumber, formattedResponse);
            
            // Reprogramar el timeout
            scheduleDQBotTimeout(phoneNumber, user);
            
        } catch (Exception e) {
            System.err.println("Error al procesar mensaje de DQBot: " + e.getMessage());
            watiApiService.sendWhatsAppMessageSync(phoneNumber, 
                "Lo siento, tuve un problema al procesar tu mensaje. Por favor, intenta de nuevo.");
        }
    }

    /**
     * Consulta al chatbotIA para obtener respuestas de IA
     */
    private String queryChatbotIA(String message, String sessionId) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", message);
            requestBody.put("session_id", sessionId);
            
            System.out.println("PostRegistrationMenuService: Consultando chatbotIA con mensaje: '" + message + "' y sessionId: '" + sessionId + "'");
            
            String response = webClient.post()
                .uri(chatbotIAUrl + "/chat")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            System.out.println("PostRegistrationMenuService: Respuesta raw de chatbotIA: '" + response + "'");
            
            // Parsear la respuesta del chatbotIA de manera más robusta
            if (response != null && response.contains("response")) {
                try {
                    // Intentar extraer la respuesta del JSON de manera más robusta
                    int startIndex = response.indexOf("\"response\":\"") + 12;
                    int endIndex = response.indexOf("\"", startIndex);
                    if (startIndex > 11 && endIndex > startIndex) {
                        String extractedResponse = response.substring(startIndex, endIndex);
                        System.out.println("PostRegistrationMenuService: Respuesta extraída de chatbotIA: '" + extractedResponse + "'");
                        return extractedResponse;
                    }
                } catch (Exception parseException) {
                    System.err.println("Error al parsear respuesta de chatbotIA: " + parseException.getMessage());
                }
            }
            
            System.out.println("PostRegistrationMenuService: No se pudo extraer respuesta válida, usando mensaje por defecto");
            return "Lo siento, no pude procesar tu consulta en este momento. Por favor, intenta de nuevo.";
            
        } catch (Exception e) {
            System.err.println("Error al consultar chatbotIA: " + e.getMessage());
            return "Lo siento, tuve un problema al conectar con la IA. Por favor, intenta más tarde.";
        }
    }

    /**
     * Verifica si DQBot está activo para un usuario
     */
    public boolean isDQBotActive(User user) {
        return user != null && user.isQbot_active();
    }

    /**
     * Desactiva DQBot para un usuario
     */
    public void deactivateDQBot(User user) {
        if (user != null) {
            user.setQbot_active(false);
            user.setQbot_session_start(null);
        }
    }
    
    /**
     * Verifica si el usuario quiere ver el menú
     */
    private boolean isMenuRequest(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String normalizedMessage = message.trim().toLowerCase();
        System.out.println("PostRegistrationMenuService: Verificando si es solicitud de menú. Mensaje original: '" + message + "', Normalizado: '" + normalizedMessage + "'");
        
        // Detectar variantes de "menu" (todas en minúsculas para comparación)
        boolean isMenu = normalizedMessage.equals("menu") ||
                        normalizedMessage.equals("menú") ||
                        normalizedMessage.equals("opciones") ||
                        normalizedMessage.equals("ayuda") ||
                        normalizedMessage.equals("volver") ||
                        normalizedMessage.equals("inicio") ||
                        normalizedMessage.equals("principal") ||
                        normalizedMessage.equals("home") ||
                        normalizedMessage.equals("start") ||
                        normalizedMessage.equals("menu principal") ||
                        normalizedMessage.equals("menú principal") ||
                        normalizedMessage.equals("ver menu") ||
                        normalizedMessage.equals("ver menú") ||
                        normalizedMessage.equals("mostrar menu") ||
                        normalizedMessage.equals("mostrar menú") ||
                        normalizedMessage.equals("quiero menu") ||
                        normalizedMessage.equals("quiero menú") ||
                        normalizedMessage.equals("dame menu") ||
                        normalizedMessage.equals("dame menú");
        
        System.out.println("PostRegistrationMenuService: ¿Es solicitud de menú? " + isMenu);
        return isMenu;
    }
}
