package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.politicalreferralswa.model.User; // Asegúrate de que User.java tiene campos: id (String UUID), phone (String), telegram_chat_id (String), Y AHORA referred_by_code (String)
import com.politicalreferralswa.service.UserDataExtractionResult;
import com.politicalreferralswa.service.GeminiService;
import com.politicalreferralswa.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.api.core.ApiFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service
public class ChatbotService {

    private final Firestore firestore;
    private final WatiApiService watiApiService;
    private final TelegramApiService telegramApiService;
    private final AIBotService aiBotService;
    private final UserDataExtractor userDataExtractor;
    private final GeminiService geminiService;
    private final NameValidationService nameValidationService;
    private final TribalAnalysisService tribalAnalysisService;
    private final AnalyticsService analyticsService;
    private final SystemConfigService systemConfigService;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private final PostRegistrationMenuService postRegistrationMenuService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Value("${WELCOME_VIDEO_URL}")
    private String welcomeVideoUrl;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private static final Pattern REFERRAL_MESSAGE_PATTERN = Pattern
            .compile("(?i).*(?:referido\\s+por\\s*:?\\s*([A-Za-z0-9]{8})|codigo\\s*:?\\s*([A-Za-z0-9]{8}))");
    private static final String TELEGRAM_BOT_USERNAME = "ResetPoliticaBot";
    private static final Pattern STRICT_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+\\d{10,15}$");

    // Nuevos mensajes de la campaña
    private static final String WELCOME_MESSAGE_BASE = "Hola. Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
    private static final String ADD_CONTACT_CTA = "Te pido que lo primero que hagas sea guardar este número con el nombre: Daniel Quintero Presidente.";
    
    private static final String PRIVACY_MESSAGE = """
        Responde (Sí/No) si aceptas nuestra política de privacidad:

        Respetamos la ley y cuidamos tu información, vamos a mantenerla de forma confidencial, esta es nuestra política de seguridad https://danielquinterocalle.com/privacidad.""";
    
    private static final String PRIVACY_MESSAGE_HEADER = "Política de Privacidad";
    private static final String PRIVACY_MESSAGE_BODY = """
        Responde si aceptas nuestra política de privacidad:

        Respetamos la ley y cuidamos tu información, vamos a mantenerla de forma confidencial, esta es nuestra política de seguridad https://danielquinterocalle.com/privacidad.""";

    // Patrones para detectar solicitudes de link de tribu
    private static final List<String> TRIBAL_LINK_PATTERNS = List.of(
        "mándame el link de mi tribu",
        "envíame el link de mi tribu",
        "¿me puedes mandar el enlace de mi tribu?",
        "pásame el link de la tribu",
        "¿dónde está el link de mi tribu?",
        "mandame el link d mi tribu",
        "mandame el link mi tribu",
        "pasame el link d mi tribu",
        "pasame link tribu",
        "mandame link tribu",
        "enlace tribu porfa",
        "link tribu ya",
        "dame el enlace de mi grupo",
        "pásame el link del grupo",
        "¿dónde está el grupo?",
        "¿cómo entro a la tribu?",
        "¿cuál es el link de ingreso a la tribu?",
        "parce, mándame el link de mi tribu",
        "oe, ¿tenés el enlace de la tribu?",
        "mijo, pásame el link del parche",
        "mija, pásame el link del parche",
        "necesito el link pa entrar a mi tribu",
        "¿dónde está el bendito link de la tribu?",
        "hágame el favor y me manda el link de la tribu",
        "¿y el enlace pa unirme?",
        "manda ese link pues",
        "quiero entrar a mi tribu",
        "cómo ingreso a mi tribu",
        "no encuentro el link de mi tribu",
        "perdí el link de la tribu",
        "ayúdame con el link de mi tribu",
        "me puedes enviar el link de mi grupo",
        "necesito volver a entrar a mi tribu",
        "como es que invito gente?",
        "dame el link",
        // Patrones adicionales más flexibles
        "pásame el link de mi tribu",
        "mandame el link de mi tribu",
        "envíame el link de mi tribu",
        "dame el link de mi tribu",
        "pásame el enlace de mi tribu",
        "mandame el enlace de mi tribu",
        "envíame el enlace de mi tribu",
        "dame el enlace de mi tribu",
        "link de mi tribu",
        "enlace de mi tribu",
        "link tribu",
        "enlace tribu",
        "link del grupo",
        "enlace del grupo",
        "link de la tribu",
        "enlace de la tribu",
        "¿dónde está el link de la tribu?",
        "¿dónde está el enlace de la tribu?",
        "¿dónde está el link del grupo?",
        "¿dónde está el enlace del grupo?",
        "¿cómo entro a mi tribu?",
        "¿cómo entro al grupo?",
        "¿cuál es el link de ingreso?",
        "¿cuál es el enlace de ingreso?",
        "perdí el link de mi tribu",
        "perdí el enlace de mi tribu",
        "perdí el link del grupo",
        "perdí el enlace del grupo",
        "ayúdame con el link de mi tribu",
        "ayúdame con el enlace de mi tribu",
        "ayúdame con el link del grupo",
        "ayúdame con el enlace del grupo",
        "necesito el link para entrar",
        "necesito el enlace para entrar",
        "necesito el link para unirme",
        "necesito el enlace para unirme",
        "como invito gente",
        "cómo invito gente",
        "como es que invito",
        "cómo es que invito",
        "¿dónde está el grupo?",
        "¿cómo entro a la tribu?",
        "¿cómo entro al grupo?",
        "cómo ingreso a mi tribu",
        "cómo ingreso al grupo",
        "mijo, pásame el link del parche",
        "mija, pásame el link del parche",
        "link del parche",
        "enlace del parche",
        "link de mi parche",
        "enlace de mi parche"
    );
    
    /**
     * Envía el mensaje de política de privacidad con botones interactivos SÍ/NO
     * @param phoneNumber Número de teléfono del usuario
     */
    private void sendPrivacyMessageWithButtons(String phoneNumber) {
        System.out.println("ChatbotService: Enviando mensaje de privacidad con botones interactivos");
        watiApiService.sendInteractiveButtonMessageSync(phoneNumber, PRIVACY_MESSAGE_BODY, "✅ SÍ", "❌ NO");
    }

    /**
     * Envía mensaje de confirmación de datos con botones interactivos
     */
    private void sendDataConfirmationMessage(String phoneNumber, User user) {
        System.out.println("ChatbotService: Enviando mensaje de confirmación de datos");
        
        // Construir el mensaje de confirmación
        String confirmationMessage = "Confírmame si tus datos están correctos:\n";
        confirmationMessage += "Nombre: " + user.getName();
        if (user.getLastname() != null && !user.getLastname().trim().isEmpty()) {
            confirmationMessage += " " + user.getLastname();
        }
        confirmationMessage += "\nCiudad: " + user.getCity();
        if (user.getState() != null && !user.getState().trim().isEmpty()) {
            confirmationMessage += "\nDepartamento: " + user.getState();
        }
        confirmationMessage += "\n\n(Sí/No)";
        
        watiApiService.sendInteractiveButtonMessageSync(phoneNumber, confirmationMessage, "✅ SÍ", "❌ NO");
    }

    // Número de WhatsApp según el ambiente
    private String getWhatsAppInviteNumber() {
        if ("prod".equals(activeProfile)) {
            return "573019700355"; // Número de producción
        } else {
            return "573224029924"; // Número de desarrollo
        }
    }

    // Patrones para detectar solicitudes de eliminación
    private static final List<String> DELETE_REQUEST_PATTERNS = List.of(
        "eliminarme 2026",
        "eliminar mi tribu 2026"
    );

    public ChatbotService(Firestore firestore, WatiApiService watiApiService,
                          TelegramApiService telegramApiService, AIBotService aiBotService,
                          UserDataExtractor userDataExtractor, GeminiService geminiService,
                          NameValidationService nameValidationService,
                          TribalAnalysisService tribalAnalysisService, AnalyticsService analyticsService,
                          SystemConfigService systemConfigService, RestTemplate restTemplate,
                          NotificationService notificationService,
                          PostRegistrationMenuService postRegistrationMenuService) {
        this.firestore = firestore;
        this.watiApiService = watiApiService;
        this.telegramApiService = telegramApiService;
        this.aiBotService = aiBotService;
        this.userDataExtractor = userDataExtractor;
        this.geminiService = geminiService;
        this.nameValidationService = nameValidationService;
        this.tribalAnalysisService = tribalAnalysisService;
        this.analyticsService = analyticsService;
        this.systemConfigService = systemConfigService;
        this.restTemplate = restTemplate;
        this.notificationService = notificationService;
        this.postRegistrationMenuService = postRegistrationMenuService;
    }

    /**
     * MÉTODO DE UTILIDAD PARA CREAR UN USUARIO REFERENTE DE PRUEBA Y USUARIOS REFERIDOS
     */
    public void createTestReferrerUser() {
        String testPhoneNumber = "+573100000001";
        String testReferralCode = "TESTCODE"; // Usar código fijo "TESTCODE" para pruebas

        Optional<User> existingUser = findUserByAnyIdentifier(testPhoneNumber, "WHATSAPP");
        if (existingUser.isPresent()) {
            System.out.println(
                    "DEBUG: Usuario referente de prueba '" + testPhoneNumber + "' ya existe. No se creará de nuevo.");
            return;
        }

        User testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setPhone_code("+57");
        testUser.setPhone(testPhoneNumber);
        testUser.setName("Referente de Prueba");
        testUser.setCity("Bogota");
        testUser.setChatbot_state("COMPLETED");
        testUser.setAceptaTerminos(true);
        testUser.setReferral_code(testReferralCode);
        testUser.setCreated_at(Timestamp.now());
        testUser.setUpdated_at(Timestamp.now());
        testUser.setReferred_by_phone(null);
        testUser.setReferred_by_code(null);

        try {
            saveUser(testUser);
            System.out.println("DEBUG: Usuario referente de prueba '" + testUser.getName() + "' con código '"
                    + testUser.getReferral_code() + "' creado exitosamente en Firestore.");
            
            // Crear usuarios de prueba referidos para comprobar la funcionalidad de eliminación de tribu
            createTestReferredUsers(testReferralCode);
            
        } catch (Exception e) {
            System.err.println("ERROR DEBUG: No se pudo crear el usuario de prueba en Firestore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MÉTODO DE UTILIDAD PARA CREAR USUARIOS REFERIDOS DE PRUEBA
     */
    private void createTestReferredUsers(String referralCode) {
        try {
            // Crear 3 usuarios de prueba referidos
            String[] testNames = {"Usuario Referido 1", "Usuario Referido 2", "Usuario Referido 3"};
            String[] testCities = {"Medellin", "Cali", "Barranquilla"};
            String[] testPhones = {"+573100000002", "+573100000003", "+573100000004"};
            
            for (int i = 0; i < testNames.length; i++) {
                String phoneNumber = testPhones[i];
                
                // Verificar si ya existe
                Optional<User> existingUser = findUserByAnyIdentifier(phoneNumber, "WHATSAPP");
                if (existingUser.isPresent()) {
                    System.out.println("DEBUG: Usuario referido de prueba '" + phoneNumber + "' ya existe. No se creará de nuevo.");
                    continue;
                }
                
                User referredUser = new User();
                referredUser.setId(UUID.randomUUID().toString());
                referredUser.setPhone_code("+57");
                referredUser.setPhone(phoneNumber);
                referredUser.setName(testNames[i]);
                referredUser.setCity(testCities[i]);
                referredUser.setChatbot_state("COMPLETED");
                referredUser.setAceptaTerminos(true);
                referredUser.setReferral_code(generateUniqueReferralCode());
                referredUser.setCreated_at(Timestamp.now());
                referredUser.setUpdated_at(Timestamp.now());
                referredUser.setReferred_by_phone("3100000001"); // Sin el +, teléfono del usuario principal
                referredUser.setReferred_by_code(referralCode); // Usar el código de referido del usuario principal
                
                saveUser(referredUser);
                System.out.println("DEBUG: Usuario referido de prueba '" + referredUser.getName() + "' creado exitosamente. Referido por código: " + referralCode + " y teléfono: 3100000001");
            }
            
            System.out.println("DEBUG: Usuarios referidos de prueba creados exitosamente para el código: " + referralCode);
            
        } catch (Exception e) {
            System.err.println("ERROR DEBUG: No se pudieron crear los usuarios referidos de prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa un mensaje entrante de un usuario.
     *
     * @param fromId      El ID del remitente (número de teléfono para WhatsApp, chat ID para Telegram).
     * @param messageText El texto del mensaje recibido.
     * @param channelType El tipo de canal.
     * @return String La respuesta principal del chatbot (el primer mensaje enviado).
     */
    public String processIncomingMessage(String fromId, String messageText, String channelType) {
        return processIncomingMessage(fromId, messageText, channelType, null);
    }

    public String processIncomingMessage(String fromId, String messageText, String channelType, String senderName) {
        System.out.println("ChatbotService: Procesando mensaje entrante de " + fromId + " (Canal: " + channelType
                + "): '" + messageText + "'");

        User user = findUserByAnyIdentifier(fromId, channelType).orElse(null);
        boolean isNewUser = (user == null);
        ChatResponse chatResponse = null;

        // LOGGING DETALLADO PARA DEBUG
        System.out.println("=== DEBUG WHATSAPP WELCOME MESSAGE ===");
        System.out.println("FromId: " + fromId);
        System.out.println("ChannelType: " + channelType);
        System.out.println("IsNewUser: " + isNewUser);
        if (user != null) {
            System.out.println("User ID: " + user.getId());
            System.out.println("User Phone: " + user.getPhone());
            System.out.println("User State: " + user.getChatbot_state());
            System.out.println("User Name: " + user.getName());
            System.out.println("User City: " + user.getCity());
        }
        System.out.println("=====================================");

        String normalizedPhoneForWhatsapp = "";
        if ("WHATSAPP".equalsIgnoreCase(channelType)) {
            String cleanedId = fromId.replaceAll("[^\\d+]", "");
            if (cleanedId.startsWith("+") && STRICT_PHONE_NUMBER_PATTERN.matcher(cleanedId).matches()) {
                normalizedPhoneForWhatsapp = cleanedId;
            } else if (cleanedId.matches("^\\d{10,15}$")) {
                normalizedPhoneForWhatsapp = "+" + cleanedId;
            }
            System.out.println("DEBUG: Phone normalized to: " + normalizedPhoneForWhatsapp);
        }

        if (isNewUser) {
            System.out.println("ChatbotService: Nuevo usuario detectado: " + fromId);
            user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setCreated_at(Timestamp.now());
            user.setAceptaTerminos(false);
            user.setReferred_by_phone(null); // Asegúrate de inicializarlo
            user.setReferred_by_code(null); // Asegúrate de inicializarlo

            // Validar y guardar el nombre del remitente si está disponible
            if (senderName != null && !senderName.trim().isEmpty()) {
                System.out.println("ChatbotService: Validando nombre de WhatsApp: " + senderName);
                
                try {
                    // Validar y extraer nombre/apellido con IA de forma síncrona
                    NameValidationService.NameValidationResult validationResult = 
                        nameValidationService.validateName(senderName.trim()).get();
                    
                    if (validationResult.isValid()) {
                        // Guardar nombre y apellido extraídos
                        if (validationResult.getExtractedName() != null) {
                            user.setName(validationResult.getExtractedName());
                        }
                        if (validationResult.getExtractedLastname() != null) {
                            user.setLastname(validationResult.getExtractedLastname());
                        }
                        
                        System.out.println("ChatbotService: ✅ Nombre de WhatsApp validado y guardado: " + 
                            validationResult.getExtractedName() + 
                            (validationResult.getExtractedLastname() != null ? " " + validationResult.getExtractedLastname() : ""));
                    } else {
                        System.out.println("ChatbotService: ❌ Nombre de WhatsApp inválido: " + senderName + " - Razón: " + validationResult.getReason());
                        // No guardar el nombre si es inválido
                    }
                } catch (Exception e) {
                    System.err.println("ChatbotService: Error al validar nombre: " + e.getMessage());
                    // En caso de error, no guardar el nombre por seguridad
                }
            }

            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                user.setPhone_code("+57");
                user.setPhone(normalizedPhoneForWhatsapp);

                System.out.println("DEBUG: Llamando handleNewUserIntro para nuevo usuario WhatsApp");
                chatResponse = handleNewUserIntro(user, messageText, senderName);
                user.setChatbot_state(chatResponse.getNextChatbotState());
                saveUser(user);

            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                user.setTelegram_chat_id(fromId);
                user.setChatbot_state("TELEGRAM_WAITING_PHONE_NUMBER");
                saveUser(user);
                chatResponse = new ChatResponse(
                        "¡Hola! 👋 Soy el bot de Reset a la Política. Para identificarte y continuar, por favor, envíame tu número de teléfono.",
                        "¡Hola! 👋 Soy el bot de Reset a la Política. Para identificarte y continuar, por favor, envíame tu número de teléfono.");
            } else {
                System.err.println("ChatbotService: Nuevo usuario de canal desconocido ('" + channelType
                        + "'). No se pudo inicializar.");
                return "Lo siento, no puedo procesar tu solicitud desde este canal.";
            }
        } else {
            System.out.println("ChatbotService: Usuario existente. Estado actual: " + user.getChatbot_state()
                    + ". ID del documento: " + (user.getPhone() != null ? user.getPhone().substring(1) : user.getId()));

            // VALIDACIÓN ADICIONAL: Verificar si el usuario ya está completado
            if ("COMPLETED".equals(user.getChatbot_state())) {
                System.out.println("DEBUG: Usuario ya completado, procesando con AI Bot");
                chatResponse = handleExistingUserMessage(user, messageText);
            } else {
                // Verificar si el usuario tiene datos básicos pero estado inconsistente
                boolean hasBasicData = (user.getName() != null && !user.getName().isEmpty()) ||
                                     (user.getCity() != null && !user.getCity().isEmpty()) ||
                                     (user.getReferral_code() != null && !user.getReferral_code().isEmpty());
                
                if (hasBasicData && (user.getChatbot_state() == null || "NEW_USER".equals(user.getChatbot_state()))) {
                    System.out.println("⚠️  WARNING: Usuario existente con datos pero estado inconsistente. Recuperando estado...");
                    
                    // IMPORTANTE: Si el usuario viene de un reseteo, limpiar TODOS los datos excepto referral_code
                    if (user.isReset_from_deletion()) {
                        System.out.println("DEBUG: Usuario viene de reseteo, limpiando datos y manteniendo solo referral_code");
                        // Limpiar TODOS los datos del usuario para forzar nuevo registro completo
                        user.setName(null);
                        user.setLastname(null);
                        user.setCity(null);
                        user.setState(null);
                        user.setAceptaTerminos(false);
                        user.setChatbot_state("NEW");
                        user.setReset_from_deletion(false); // Resetear el flag
                        saveUser(user);
                        System.out.println("DEBUG: Usuario reseteado - todos los datos limpiados para nuevo registro completo");
                    } else {
                        // Solo para usuarios que NO vienen de reseteo, intentar recuperar el estado
                        if (user.getName() != null && user.getCity() != null && user.isAceptaTerminos()) {
                            // Usuario parece estar completo, verificar si tiene código de referido
                            if (user.getReferral_code() == null || user.getReferral_code().isEmpty()) {
                                String referralCode = generateUniqueReferralCode();
                                user.setReferral_code(referralCode);
                                System.out.println("DEBUG: Generando código de referido faltante: " + referralCode);
                            }
                            user.setChatbot_state("COMPLETED");
                            saveUser(user);
                            System.out.println("DEBUG: Usuario recuperado como COMPLETED");
                        } else if (user.getName() != null && user.getCity() != null && !user.isAceptaTerminos()) {
                            // SIEMPRE validar política de privacidad antes de completar
                            user.setChatbot_state("WAITING_TERMS_ACCEPTANCE");
                            saveUser(user);
                            System.out.println("DEBUG: Usuario recuperado como WAITING_TERMS_ACCEPTANCE (validando política)");
                        } else if (user.getName() != null && (user.getCity() == null || user.getCity().isEmpty())) {
                            user.setChatbot_state("WAITING_CITY");
                            saveUser(user);
                            System.out.println("DEBUG: Usuario recuperado como WAITING_CITY");
                        } else {
                            user.setChatbot_state("WAITING_NAME");
                            saveUser(user);
                            System.out.println("DEBUG: Usuario recuperado como WAITING_NAME");
                        }
                    }
                }

                boolean userUpdated = false;
                if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                    if (user.getPhone() == null || !user.getPhone().equals(normalizedPhoneForWhatsapp)) {
                        user.setPhone(normalizedPhoneForWhatsapp);
                        user.setPhone_code("+57");
                        userUpdated = true;
                        System.out.println("DEBUG: Actualizando número de teléfono de usuario existente: "
                                + normalizedPhoneForWhatsapp);
                    }
                } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                    if (user.getTelegram_chat_id() == null || !user.getTelegram_chat_id().equals(fromId)) {
                        user.setTelegram_chat_id(fromId);
                        userUpdated = true;
                        System.out.println("DEBUG: Actualizando Telegram Chat ID de usuario existente: " + fromId);
                    }
                }

                if (userUpdated) {
                    saveUser(user);
                }

                System.out.println("DEBUG: Llamando handleExistingUserMessage para usuario existente");
                chatResponse = handleExistingUserMessage(user, messageText);
            }
        }

        if (chatResponse != null) {
            // LOGGING PARA IDENTIFICAR CUÁNDO SE ENVÍA EL MENSAJE DE BIENVENIDA
            String primaryMessage = chatResponse.getPrimaryMessage();
            if (primaryMessage != null && primaryMessage.contains("Soy el bot de Reset a la Política")) {
                System.out.println("⚠️  WARNING: Se está enviando mensaje de bienvenida!");
                System.out.println("   FromId: " + fromId);
                System.out.println("   IsNewUser: " + isNewUser);
                System.out.println("   UserState: " + (user != null ? user.getChatbot_state() : "null"));
                System.out.println("   NextState: " + chatResponse.getNextChatbotState());
                System.out.println("   Message: " + primaryMessage.substring(0, Math.min(100, primaryMessage.length())) + "...");
            }

            // RESPUESTA RÁPIDA OPTIMIZADA: Solo para usuarios COMPLETED y solo si es necesario
            if (user != null && "COMPLETED".equals(user.getChatbot_state()) && 
                "WHATSAPP".equalsIgnoreCase(channelType)) {
                // Enviar respuesta inmediata solo si el procesamiento será lento
                // Por ahora, no enviar mensaje intermedio para evitar confusión
                System.out.println("ChatbotService: Usuario COMPLETED detectado, procesando directamente");
            }

            // Enviar mensaje principal de forma síncrona para garantizar el orden
            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                System.out.println("ChatbotService: Enviando mensaje principal a " + fromId + " (Canal: " + channelType + ")");
                
                // Detectar si es un mensaje múltiple
                if (primaryMessage != null && primaryMessage.startsWith("MULTI:")) {
                    // Remover el prefijo "MULTI:" y dividir por "|"
                    String messagesContent = primaryMessage.substring(6); // Remover "MULTI:"
                    String[] messages = messagesContent.split("\\|");
                    
                    // Enviar cada mensaje con una pausa
                    for (int i = 0; i < messages.length; i++) {
                        if (!messages[i].trim().isEmpty()) {
                            sendWhatsAppMessageSync(fromId, messages[i].trim());
                            
                            // Pausa entre mensajes (excepto el último)
                            if (i < messages.length - 1) {
                                try {
                                    Thread.sleep(1500); // 1.5 segundos entre mensajes
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    // Mensaje normal
                    sendWhatsAppMessageSync(fromId, primaryMessage);
                }
            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                telegramApiService.sendTelegramMessage(fromId, chatResponse.getPrimaryMessage());
            } else {
                System.err.println("ChatbotService: Canal desconocido ('" + channelType
                        + "'). No se pudo enviar el mensaje principal.");
            }

            // Enviar mensajes secundarios de forma secuencial para garantizar el orden
            chatResponse.getSecondaryMessage().ifPresent(secondaryMsg -> {
                String[] messagesToSend = secondaryMsg.split("###SPLIT###");
                for (int i = 0; i < messagesToSend.length; i++) {
                    String msg = messagesToSend[i].trim();
                    if (!msg.isEmpty()) {
                        System.out.println("ChatbotService: Enviando mensaje secundario " + (i + 1) + "/" + messagesToSend.length + " a " + fromId + " (Canal: "
                                + channelType + "): '" + msg + "'");
                        if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                            // Verificar si es el mensaje de guardar contacto para enviar botones interactivos
                            if (msg.contains("Te pido que lo primero que hagas sea guardar este número")) {
                                // Enviar mensaje sin botones interactivos
                                System.out.println("ChatbotService: Enviando mensaje de guardar contacto sin botones interactivos");
                                sendWhatsAppMessageSync(fromId, msg);
                                
                                // Enviar inmediatamente el mensaje de confirmación de nombre (sin retraso)
                                System.out.println("ChatbotService: Enviando mensaje de confirmación de nombre inmediatamente");
                                sendWhatsAppMessageSync(fromId, "¿Me confirmas tu nombre para guardarte en mis contactos?");
                            } else {
                                // Enviar de forma síncrona para garantizar el orden
                                sendWhatsAppMessageSync(fromId, msg);
                            }
                        } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                            telegramApiService.sendTelegramMessage(fromId, msg);
                        } else {
                            System.err.println("ChatbotService: Canal desconocido ('" + channelType
                                    + "'). No se pudo enviar el mensaje secundario.");
                        }
                    }
                }
            });

            user.setChatbot_state(chatResponse.getNextChatbotState());
            user.setUpdated_at(Timestamp.now());
            saveUser(user);

            // Limpiar el prefijo MULTI: del mensaje de retorno para logs
            String returnMessage = chatResponse.getPrimaryMessage();
            if (returnMessage != null && returnMessage.startsWith("MULTI:")) {
                String[] messages = returnMessage.substring(6).split("\\|");
                returnMessage = messages[0].trim(); // Retornar solo el primer mensaje
            }
            return returnMessage;
        }
        return "ERROR: No se pudo generar una respuesta.";
    }

    /**
     * Maneja la lógica de inicio para nuevos usuarios con extracción inteligente de datos.
     * Usa Gemini AI para detectar automáticamente información del usuario.
     *
     * @param user        El objeto User del nuevo usuario.
     * @param messageText El primer mensaje enviado por el usuario.
     * @return ChatResponse con el mensaje y el siguiente estado.
     */
    private ChatResponse handleNewUserIntro(User user, String messageText) {
        return handleNewUserIntro(user, messageText, null);
    }

    private ChatResponse handleNewUserIntro(User user, String messageText, String senderName) {
        System.out.println("DEBUG handleNewUserIntro: Mensaje entrante recibido: '" + messageText + "'");
        System.out.println("DEBUG handleNewUserIntro: Usuario ID: " + user.getId());
        System.out.println("DEBUG handleNewUserIntro: Usuario Phone: " + user.getPhone());

        // Intentar extracción inteligente de datos primero
        UserDataExtractor.ExtractionResult extractionResult = userDataExtractor.extractAndUpdateUser(user, messageText, null);
        
        System.out.println("DEBUG handleNewUserIntro: Resultado de extracción - Success: " + extractionResult.isSuccess() + 
                          ", Message: '" + extractionResult.getMessage() + "', NextState: " + extractionResult.getNextState());
        
        // CORRECCIÓN CRÍTICA: Si la extracción inteligente detectó un código de referido pero no estableció referred_by_phone,
        // debemos buscarlo manualmente antes de continuar
        if (extractionResult.isSuccess() && user.getReferred_by_code() != null && user.getReferred_by_phone() == null) {
            System.out.println("DEBUG handleNewUserIntro: 🔍 CORRECCIÓN: Código de referido detectado pero sin referred_by_phone. Buscando usuario referente...");
            Optional<User> referrerUser = getUserByReferralCode(user.getReferred_by_code());
            if (referrerUser.isPresent()) {
                // Guardar el número del referente sin el símbolo "+"
                String referrerPhone = referrerUser.get().getPhone();
                if (referrerPhone != null && referrerPhone.startsWith("+")) {
                    referrerPhone = referrerPhone.substring(1);
                }
                user.setReferred_by_phone(referrerPhone);
                System.out.println("DEBUG handleNewUserIntro: ✅ CORRECCIÓN APLICADA: Establecido referred_by_phone: " + user.getReferred_by_phone());
            } else {
                System.out.println("DEBUG handleNewUserIntro: ⚠️ CORRECCIÓN FALLIDA: Código de referido no encontrado en base de datos");
            }
        }
        
        if (extractionResult.isSuccess()) {
            // Guardar usuario actualizado después de la extracción
            saveUser(user);
            
            if (extractionResult.needsClarification()) {
                // Si necesita aclaración, preguntar específicamente
                System.out.println("DEBUG handleNewUserIntro: Usando extracción inteligente - Necesita aclaración");
                return new ChatResponse(extractionResult.getMessage(), "WAITING_CLARIFICATION");
            } else if (extractionResult.isCompleted()) {
                // Si se completó la extracción, pero aún necesitamos validar política de privacidad
                System.out.println("DEBUG handleNewUserIntro: Usando extracción inteligente - Completado, pero validando política");
                
                // Enviar mensajes secuenciales: primero bienvenida, luego instrucción de contacto
                sendWhatsAppMessageSync(user.getPhone(), WELCOME_MESSAGE_BASE);
                sendWhatsAppMessageSync(user.getPhone(), ADD_CONTACT_CTA);
                
                // Programar el mensaje de privacidad con botones interactivos después de un retraso
                final String userPhone6 = user.getPhone();
                scheduler.schedule(() -> {
                    sendPrivacyMessageWithButtons(userPhone6);
                }, 10, TimeUnit.SECONDS);

                return new ChatResponse("", "WAITING_TERMS_ACCEPTANCE"); // No enviar mensaje primario aquí
            } else {
                // Si se extrajo parcialmente, verificar si hay código de referido para usar mensaje personalizado
                if (user.getReferred_by_code() != null && user.getReferred_by_phone() != null) {
                    System.out.println("DEBUG handleNewUserIntro: 🔍 Código de referido detectado por IA, usando mensaje personalizado");
                    
                    // Buscar usuario referente para obtener su nombre
                    Optional<User> referrerUser = getUserByReferralCode(user.getReferred_by_code());
                    String personalizedGreeting = "";
                    
                    if (referrerUser.isPresent()) {
                        String referrerName = referrerUser.get().getName();
                        if (referrerName != null && !referrerName.trim().isEmpty()) {
                            personalizedGreeting = "Te ha referido " + referrerName.trim() + ". ";
                        } else {
                            personalizedGreeting = "Te ha referido un amigo. ";
                        }
                    } else {
                        personalizedGreeting = "Te ha referido un amigo. ";
                    }
                    
                    if (user.getName() != null && !user.getName().trim().isEmpty()) {
                        personalizedGreeting += "¡Hola " + user.getName().trim() + "! ";
                    }
                    
                    // Usar mensaje personalizado en lugar del mensaje genérico de la IA
                    String finalMessage;
                    if (user.getName() != null && !user.getName().trim().isEmpty()) {
                        finalMessage = personalizedGreeting + "¿Me confirmas si tu nombre es el que aparece en WhatsApp " + user.getName().trim() + " o me dices cómo te llamas para guardarte en mis contactos?";
                    } else {
                        finalMessage = personalizedGreeting + "¿Me confirmas tu nombre para guardarte en mis contactos?";
                    }
                    System.out.println("⚠️  WARNING: Generando mensaje personalizado con código de referido detectado por IA");
                    
                    sendWhatsAppMessageSync(user.getPhone(), WELCOME_MESSAGE_BASE);
                    sendWhatsAppMessageSync(user.getPhone(), ADD_CONTACT_CTA);
                    scheduler.schedule(() -> sendWhatsAppMessageSync(user.getPhone(), finalMessage), 10, TimeUnit.SECONDS);

                    return new ChatResponse("", "WAITING_NAME");
                } else {
                    // Si no hay código de referido, usar el mensaje de extracción de la IA
                    System.out.println("DEBUG handleNewUserIntro: Usando extracción inteligente - Parcial, sin política de privacidad");
                    
                    // Preparar múltiples mensajes para extracción parcial
                    System.out.println("⚠️  WARNING: Generando mensaje de bienvenida en extracción inteligente parcial");
                    
                    sendWhatsAppMessageSync(user.getPhone(), WELCOME_MESSAGE_BASE);
                    sendWhatsAppMessageSync(user.getPhone(), ADD_CONTACT_CTA);
                    scheduler.schedule(() -> sendWhatsAppMessageSync(user.getPhone(), extractionResult.getMessage()), 10, TimeUnit.SECONDS);

                    return new ChatResponse("", extractionResult.getNextState());
                }
            }
        }
        
        // Verificar si la IA detectó un código de referido aunque la extracción general falló
        String detectedReferralCode = null;
        
        // Obtener el resultado de extracción de la IA para acceder a los campos extraídos
        UserDataExtractionResult aiExtraction = geminiService.extractUserData(messageText, "", null);
        if (aiExtraction.getReferralCode() != null && !aiExtraction.getReferralCode().trim().isEmpty()) {
            detectedReferralCode = aiExtraction.getReferralCode().trim();
            System.out.println("DEBUG handleNewUserIntro: 🔍 IA detectó código de referido: '" + detectedReferralCode + "'");
        }

        // Si la IA no detectó código, usar el método tradicional
        if (detectedReferralCode == null) {
            System.out.println("DEBUG handleNewUserIntro: IA no detectó código, usando método tradicional");
            Matcher matcher = REFERRAL_MESSAGE_PATTERN.matcher(messageText.trim());
            System.out.println("DEBUG handleNewUserIntro: Resultado de la coincidencia del patrón Regex: " + matcher.matches());
            
            if (matcher.matches()) {
                // El nuevo patrón tiene dos grupos: uno para "referido por" y otro para "codigo"
                detectedReferralCode = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                System.out.println("DEBUG handleNewUserIntro: Código de referido extraído por regex: '" + detectedReferralCode + "'");
            }
        }

        if (detectedReferralCode != null) {
            String incomingReferralCode = detectedReferralCode;
            System.out.println("DEBUG handleNewUserIntro: Código de referido a procesar: '" + incomingReferralCode + "'");

            System.out.println(
                    "ChatbotService: Primer mensaje contiene posible código de referido: " + incomingReferralCode);
            Optional<User> referrerUser = getUserByReferralCode(incomingReferralCode);

            if (referrerUser.isPresent()) {
                // MODIFICACIÓN CLAVE AQUÍ: Guardar el código de referido también
                // Guardar el número del referente SOLO con la parte local (sin código de país)
                String referrerPhone = referrerUser.get().getPhone();
                System.out.println("DEBUG handleNewUserIntro: 🔍 Número original del referente: " + referrerPhone);
                
                // Extraer solo la parte local del número (sin +57)
                if (referrerPhone != null) {
                    if (referrerPhone.startsWith("+57")) {
                        referrerPhone = referrerPhone.substring(3); // Quitar +57
                        System.out.println("DEBUG handleNewUserIntro: 🔍 Número después de quitar +57: " + referrerPhone);
                    } else if (referrerPhone.startsWith("57")) {
                        referrerPhone = referrerPhone.substring(2); // Quitar 57
                        System.out.println("DEBUG handleNewUserIntro: 🔍 Número después de quitar 57: " + referrerPhone);
                    } else if (referrerPhone.startsWith("+")) {
                        referrerPhone = referrerPhone.substring(1); // Quitar +
                        System.out.println("DEBUG handleNewUserIntro: 🔍 Número después de quitar +: " + referrerPhone);
                    } else {
                        System.out.println("DEBUG handleNewUserIntro: 🔍 Número sin procesar (no empieza con +57, 57 o +): " + referrerPhone);
                    }
                }
                
                System.out.println("DEBUG handleNewUserIntro: 🔍 Número final a guardar: " + referrerPhone);
                user.setReferred_by_phone(referrerPhone);
                user.setReferred_by_code(incomingReferralCode); // <-- AÑADIDO: Guardar el código de referido
                System.out.println("DEBUG handleNewUserIntro: Estableciendo referred_by_phone: '" + user.getReferred_by_phone() + "' y referred_by_code: '" + user.getReferred_by_code() + "'");


                // Personalizar saludo si tenemos el nombre de WhatsApp validado
                String personalizedGreeting = "";
                // Agregar mensaje de referido
                String referrerName = referrerUser.get().getName();
                if (referrerName != null && !referrerName.trim().isEmpty()) {
                    personalizedGreeting = "Te ha referido " + referrerName.trim() + ". ";
                } else {
                    personalizedGreeting = "Te ha referido un amigo. ";
                }
                
                if (user.getName() != null && !user.getName().trim().isEmpty()) {
                    personalizedGreeting += "¡Hola " + user.getName().trim() + "! ";
                }
                
                // Enviar mensaje de bienvenida personalizado primero, y mensaje de contacto como secundario
                String responseMessage = personalizedGreeting + "¡Hola! Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
                String nextChatbotState = "WAITING_NAME";
                
                // Crear ChatResponse con mensaje secundario usando el constructor correcto
                return new ChatResponse(responseMessage, nextChatbotState, 
                    Optional.of("Te pido que lo primero que hagas sea guardar este número con el nombre: Daniel Quintero Presidente."));
            } else {
                System.out.println(
                        "ChatbotService: Código de referido válido en formato, pero NO ENCONTRADO en el primer mensaje: "
                                + incomingReferralCode);
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida con código de referido inválido");
                
                // Enviar mensaje de bienvenida con aviso de código inválido primero, y mensaje de contacto como secundario
                String responseMessage = "Parece que el código de referido que me enviaste no es válido, pero no te preocupes, ¡podemos continuar!\n\n¡Hola! Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
                String nextChatbotState = "WAITING_NAME";
                
                // Crear ChatResponse con mensaje secundario usando el constructor correcto
                return new ChatResponse(responseMessage, nextChatbotState, 
                    Optional.of("Te pido que lo primero que hagas sea guardar este número con el nombre: Daniel Quintero Presidente."));
            }
        } else {
            System.out.println("DEBUG handleNewUserIntro: El mensaje no coincide con el patrón de referido.");

            System.out
                    .println("ChatbotService: Primer mensaje no contiene código de referido. Iniciando flujo general.");
            
            // Verificar si ya tenemos un nombre validado
            if (user.getName() != null && !user.getName().trim().isEmpty()) {
                System.out.println("⚠️  WARNING: Generando mensaje con nombre ya validado: " + user.getName());
                
                // Construir nombre completo para mostrar
                String fullName = user.getName();
                if (user.getLastname() != null && !user.getLastname().trim().isEmpty()) {
                    fullName += " " + user.getLastname();
                }
                
                // Si no tiene apellido, preguntarlo
                if (user.getLastname() == null || user.getLastname().trim().isEmpty()) {
                    // Enviar mensaje de bienvenida primero, y mensaje de contacto como secundario
                    String responseMessage = "Hola. Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
                    String nextChatbotState = "WAITING_NAME";
                    
                    // Crear ChatResponse con mensaje secundario usando el constructor correcto
                    return new ChatResponse(responseMessage, nextChatbotState, 
                        Optional.of("Te pido que lo primero que hagas sea guardar este número con el nombre: Daniel Quintero Presidente.\n\nResponde con uno de los botones de abajo cuando hayas guardado el contacto:"));
                } else {
                    // Si ya tiene nombre y apellido, preguntar ciudad
                    // Enviar mensaje de bienvenida primero, y mensaje de contacto como secundario
                    String responseMessage = "Hola. Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
                    String nextChatbotState = "WAITING_NAME";
                    
                    // Crear ChatResponse con mensaje secundario usando el constructor correcto
                    return new ChatResponse(responseMessage, nextChatbotState, 
                        Optional.of(ADD_CONTACT_CTA));
                }
            } else {
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida general (sin código de referido)");
                
                // Enviar mensaje de bienvenida primero, y mensaje de contacto como secundario
                String responseMessage = "Hola. Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
                String nextChatbotState = "WAITING_NAME";
                
                // Crear ChatResponse con mensaje secundario usando el constructor correcto
                return new ChatResponse(responseMessage, nextChatbotState, 
                    Optional.of(ADD_CONTACT_CTA));
            }
        }
    }


    private ChatResponse handleExistingUserMessage(User user, String messageText) {
        messageText = messageText.trim();

        String currentChatbotState = user.getChatbot_state();
        
        System.out.println("DEBUG handleExistingUserMessage: Estado actual: " + currentChatbotState);
        System.out.println("DEBUG handleExistingUserMessage: Usuario ID: " + user.getId());
        System.out.println("DEBUG handleExistingUserMessage: Usuario Phone: " + user.getPhone());
        
        // Si el estado es null, inicializar como nuevo usuario
        if (currentChatbotState == null) {
            currentChatbotState = "NEW_USER";
            user.setChatbot_state(currentChatbotState);
            System.out.println("⚠️  WARNING: Usuario existente con estado null, estableciendo como NEW_USER");
        }
        
        String responseMessage = "";
        String nextChatbotState = currentChatbotState;
        Optional<String> secondaryMessage = Optional.empty();

        Matcher matcher = REFERRAL_MESSAGE_PATTERN.matcher(messageText);

        switch (currentChatbotState) {
            case "WAITING_CLARIFICATION":
                // Procesar aclaración del usuario
                UserDataExtractor.ExtractionResult clarificationResult = userDataExtractor.extractAndUpdateUser(user, messageText, null);
                
                if (clarificationResult.isSuccess()) {
                    // Guardar usuario actualizado después de la aclaración
                    saveUser(user);
                    
                    if (clarificationResult.needsClarification()) {
                        // Si aún necesita aclaración
                        responseMessage = clarificationResult.getMessage();
                        nextChatbotState = "WAITING_CLARIFICATION";
                    } else if (clarificationResult.isCompleted()) {
                        // Si se completó con la aclaración
                        responseMessage = clarificationResult.getMessage();
                        nextChatbotState = "CONFIRM_DATA";
                    } else {
                        // Si se resolvió parcialmente
                        responseMessage = clarificationResult.getMessage();
                        nextChatbotState = clarificationResult.getNextState();
                    }
                } else {
                    // Si falló la extracción inteligente, intentar usar el mensaje directamente como ciudad
                    boolean hasName = user.getName() != null && !user.getName().isEmpty();
                    boolean hasCity = user.getCity() != null && !user.getCity().isEmpty();
                    
                    if (hasName && !hasCity) {
                        // Si tiene nombre pero no ciudad, procesar el mensaje para extraer solo la ciudad
                        String potentialCity = extractCityFromMessage(messageText.trim());
                        if (!potentialCity.isEmpty()) {
                            user.setCity(potentialCity);
                            saveUser(user);
                            
                            // Confirmar datos con la ciudad proporcionada
                            responseMessage = "Confirmamos tus datos: " + user.getName() + 
                                (user.getLastname() != null ? " " + user.getLastname() : "") + 
                                ", de " + user.getCity() + ". ¿Es correcto? (Sí/No)";
                            nextChatbotState = "CONFIRM_DATA";
                        } else {
                            responseMessage = "¿En qué ciudad vives?";
                            nextChatbotState = "WAITING_CITY";
                        }
                    } else if (hasName && hasCity) {
                        // Si ya tiene nombre y ciudad, confirmar datos
                        responseMessage = "Confirmamos tus datos: " + user.getName() + 
                            (user.getLastname() != null ? " " + user.getLastname() : "") + 
                            ", de " + user.getCity() + ". ¿Es correcto? (Sí/No)";
                        nextChatbotState = "CONFIRM_DATA";
                    } else {
                        // Si no tiene datos, volver al inicio
                        responseMessage = "Entiendo. Vamos paso a paso. ¿Cuál es tu nombre?";
                        nextChatbotState = "WAITING_NAME";
                    }
                }
                break;
                
            case "NEW_USER":
                // Si el usuario tiene estado NEW_USER, tratarlo como nuevo usuario
                System.out.println("⚠️  WARNING: Usuario existente con estado NEW_USER, llamando handleNewUserIntro");
                return handleNewUserIntro(user, messageText);
                
            case "TELEGRAM_WAITING_PHONE_NUMBER":
                String rawNumberInput = messageText.trim();
                String cleanedNumber = rawNumberInput.replaceAll("[^\\d+]", "");
                String normalizedPhoneNumber;

                if (cleanedNumber.startsWith("+") && cleanedNumber.length() >= 10) {
                    normalizedPhoneNumber = cleanedNumber;
                    user.setPhone_code(normalizedPhoneNumber.substring(0, Math.min(normalizedPhoneNumber.length(), 4)));
                    System.out.println("DEBUG: Telegram number recognized with country code: " + normalizedPhoneNumber);
                } else if (cleanedNumber.matches("^\\d{7,10}$")) {
                    normalizedPhoneNumber = "+57" + cleanedNumber;
                    user.setPhone_code("+57");
                    System.out.println("DEBUG: Telegram number normalized to +57: " + normalizedPhoneNumber);
                } else {
                    responseMessage = "Eso no parece un número de teléfono válido. Por favor, asegúrate de que sea un número real, incluyendo el código de país si lo tienes (ej. +573001234567).";
                    nextChatbotState = "TELEGRAM_WAITING_PHONE_NUMBER";
                    return new ChatResponse(responseMessage, nextChatbotState);
                }

                if (!STRICT_PHONE_NUMBER_PATTERN.matcher(normalizedPhoneNumber).matches()) {
                    responseMessage = "El formato de número de teléfono no es válido después de la normalización. Por favor, asegúrate de que sea un número real (ej. +573001234567).";
                    nextChatbotState = "TELEGRAM_WAITING_PHONE_NUMBER";
                    return new ChatResponse(responseMessage, nextChatbotState);
                }

                Optional<User> existingUserByPhone = findUserByPhoneNumberField(normalizedPhoneNumber);

                if (existingUserByPhone.isPresent()) {
                    User foundUser = existingUserByPhone.get();
                    if (!foundUser.getId().equals(user.getId())) {
                        System.out.println("DEBUG: Conflicto de usuario detectado. Número '" + normalizedPhoneNumber
                                + "' ya registrado con ID de documento: "
                                + (foundUser.getPhone() != null ? foundUser.getPhone().substring(1)
                                        : foundUser.getId()));
                        System.out.println("DEBUG: Usuario actual (Telegram inicial) ID de documento: " + user.getId()
                                + " con chat_id: " + user.getTelegram_chat_id());

                        if (foundUser.getTelegram_chat_id() == null
                                || !foundUser.getTelegram_chat_id().equals(user.getTelegram_chat_id())) {
                            foundUser.setTelegram_chat_id(user.getTelegram_chat_id());
                            System.out.println("DEBUG: Vinculando Telegram Chat ID " + user.getTelegram_chat_id()
                                    + " a usuario existente.");
                        }

                        try {
                            firestore.collection("users").document(user.getId()).delete().get();
                            System.out.println("DEBUG: Documento temporal de Telegram (UUID: " + user.getId()
                                    + ") eliminado después de vincular.");
                        } catch (Exception e) {
                            System.err.println("ERROR al eliminar documento temporal de Telegram (UUID: " + user.getId()
                                    + "): " + e.getMessage());
                            e.printStackTrace();
                        }

                        user = foundUser;

                        responseMessage = "¡Ya estás registrado con ese número! Hemos vinculado tu cuenta de Telegram a tu perfil existente. Puedes continuar.";
                        nextChatbotState = foundUser.getChatbot_state();
                        return new ChatResponse(responseMessage, nextChatbotState);
                    }
                }

                user.setPhone(normalizedPhoneNumber);
                user.setPhone_code(normalizedPhoneNumber.substring(0, Math.min(normalizedPhoneNumber.length(), 4)));

                final String finalResponseMessage = "¡Gracias! Hemos registrado tu número de teléfono.";
                final String finalNextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                responseMessage = finalResponseMessage;
                nextChatbotState = finalNextChatbotState;
                
                // Enviar mensaje de privacidad con botones interactivos después de un retraso
                final String userPhone0 = user.getPhone();
                scheduler.schedule(() -> {
                    sendPrivacyMessageWithButtons(userPhone0);
                }, 5, TimeUnit.SECONDS);
                break;

            case "WAITING_TERMS_ACCEPTANCE":
                // Validar respuesta de botones interactivos o texto libre
                String normalizedMessage = messageText.toLowerCase().trim();
                boolean acceptedTerms = false;
                
                // Verificar respuestas de botones interactivos
                if (normalizedMessage.equals("✅ sí") || normalizedMessage.equals("sí") || 
                    normalizedMessage.equals("si") || normalizedMessage.equals("yes") ||
                    normalizedMessage.contains("acepto") || normalizedMessage.contains("aceptar")) {
                    acceptedTerms = true;
                    System.out.println("DEBUG: Usuario ACEPTÓ los términos (botón interactivo o texto afirmativo).");
                } else if (normalizedMessage.equals("❌ no") || normalizedMessage.equals("no") || 
                           normalizedMessage.contains("no acepto") || normalizedMessage.contains("rechazo")) {
                    // Usuario rechazó los términos, volver a preguntar
                    System.out.println("DEBUG: Usuario RECHAZÓ los términos, volviendo a preguntar.");
                    responseMessage = "Entiendo que no quieres aceptar los términos. Te explico nuevamente:";
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                    
                    // Enviar mensaje de privacidad con botones interactivos después de un retraso
                    final String userPhone1 = user.getPhone();
                    scheduler.schedule(() -> {
                        sendPrivacyMessageWithButtons(userPhone1);
                    }, 5, TimeUnit.SECONDS);
                    
                    return new ChatResponse(responseMessage, nextChatbotState);
                } else {
                    // Usar IA como fallback para respuestas no claras
                    acceptedTerms = geminiService.isAffirmativeResponse(messageText);
                    if (acceptedTerms) {
                        System.out.println("DEBUG: Usuario ACEPTÓ los términos (validado por IA).");
                    } else {
                        System.out.println("DEBUG: Usuario NO ACEPTÓ los términos (validado por IA).");
                        // Respuesta no clara, volver a preguntar
                        responseMessage = "No entendí tu respuesta. Por favor, usa los botones SÍ o NO para confirmar si aceptas nuestra política de privacidad.";
                        nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                        
                        // Enviar mensaje de privacidad con botones interactivos después de un retraso
                        final String userPhone2 = user.getPhone();
                        scheduler.schedule(() -> {
                            sendPrivacyMessageWithButtons(userPhone2);
                        }, 5, TimeUnit.SECONDS);
                        
                        return new ChatResponse(responseMessage, nextChatbotState);
                    }
                }
                
                if (acceptedTerms) {
                    
                    user.setAceptaTerminos(true); // Marcar que aceptó
                    System.out.println("DEBUG: Usuario ACEPTÓ los términos (validado por IA).");

                    // Verificar si ya tiene todos los datos necesarios
                    boolean hasName = user.getName() != null && !user.getName().isEmpty();
                    boolean hasCity = user.getCity() != null && !user.getCity().isEmpty();
                    
                    System.out.println("DEBUG: Usuario tiene nombre: " + hasName + " (nombre: " + user.getName() + ")");
                    System.out.println("DEBUG: Usuario tiene ciudad: " + hasCity + " (ciudad: " + user.getCity() + ")");
                    
                    if (hasName && hasCity) {
                        System.out.println("DEBUG: ✅ Usuario tiene todos los datos. Completando registro...");
                        // Si ya tiene nombre y ciudad, completar el registro
                        // IMPORTANTE: Si el usuario ya tiene referral_code (viene del reseteo), NO generar uno nuevo
                        String referralCode;
                        if (user.getReferral_code() != null && !user.getReferral_code().isEmpty()) {
                            referralCode = user.getReferral_code(); // Mantener el existente
                            System.out.println("DEBUG: ✅ Usuario reseteado, manteniendo referral_code existente: " + referralCode);
                        } else {
                            referralCode = generateUniqueReferralCode(); // Generar nuevo solo si no existe
                            user.setReferral_code(referralCode);
                            System.out.println("DEBUG: ✅ Generando nuevo referral_code: " + referralCode);
                        }

                        String whatsappInviteLink;
                        String telegramInviteLink;
                        List<String> additionalMessages = new ArrayList<>();

                        try {
                            String whatsappRawReferralText = String.format("Hola, vengo referido por %s, codigo: %s", user.getName(), referralCode);
                            String encodedWhatsappMessage = URLEncoder
                                    .encode(whatsappRawReferralText, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                            whatsappInviteLink = "https://wa.me/" + getWhatsAppInviteNumber() + "?text=" + encodedWhatsappMessage;

                            String encodedTelegramPayload = URLEncoder.encode(referralCode,
                                    StandardCharsets.UTF_8.toString());
                            telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start="
                                    + encodedTelegramPayload;

                            String friendsInviteMessage = String.format(
                                    "Amigos, soy %s y quiero invitarte a unirte a la campaña de Daniel Quintero a la Presidencia: %s",
                                    user.getName(),
                                    whatsappInviteLink);
                            additionalMessages.add(friendsInviteMessage);

                            // Enviar el video de bienvenida ANTES del mensaje de IA
                            try {
                                watiApiService.sendVideoMessage(user.getPhone(), welcomeVideoUrl, " ");
                                System.out.println("DEBUG: Video de bienvenida enviado a: " + user.getPhone());
                            } catch (Exception e) {
                                System.err.println("DEBUG: ⚠️ Error al enviar video de bienvenida: " + e.getMessage());
                                System.err.println("DEBUG: ⚠️ Continuando con flujo normal sin video...");
                                // No lanzar la excepción, continuar con el flujo normal
                            }

                        } catch (UnsupportedEncodingException e) {
                            System.err.println("ERROR: No se pudo codificar los códigos de referido. Causa: " + e.getMessage());
                            e.printStackTrace();
                            whatsappInviteLink = "https://wa.me/" + getWhatsAppInviteNumber() + "?text=Error%20al%20generar%20referido";
                            telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=Error";
                            additionalMessages.clear();
                            additionalMessages.add("Error al generar los mensajes de invitación.");
                        }

                        responseMessage = String.format(
                                "%s, gracias por unirte como voluntario. Tu primera misión es enviar el siguiente link a tus amigos de modo que más personas se sumen.",
                                user.getName()
                        );

                        Optional<String> termsSecondaryMessage = Optional.of(String.join("###SPLIT###", additionalMessages));
                        
                        // Notificar al referente si este usuario fue referido
                        if (user.getReferred_by_phone() != null && user.getReferred_by_code() != null) {
                            try {
                                // Formatear el teléfono del referente para la notificación
                                String referrerPhone = user.getReferred_by_phone();
                                
                                // --- INICIO DE LA NUEVA LÓGICA DE LIMPIEZA ---
                                // 1. Quitar todos los caracteres que no sean dígitos
                                String digitsOnly = referrerPhone.replaceAll("[^\\d]", "");
                                // 2. Si empieza con "57" y tiene 12 dígitos, es un número colombiano completo.
                                if (digitsOnly.startsWith("57") && digitsOnly.length() == 12) {
                                    // Tomar solo los últimos 10 dígitos y añadir el prefijo correcto.
                                    referrerPhone = "+57" + digitsOnly.substring(2);
                                } else if (digitsOnly.length() == 10) {
                                    // Si solo tiene 10 dígitos, es un número local. Añadir prefijo.
                                    referrerPhone = "+57" + digitsOnly;
                                } else if (!referrerPhone.startsWith("+")) {
                                    // Como última opción, si no empieza con +, añadirlo.
                                    referrerPhone = "+" + referrerPhone;
                                }
                                // --- FIN DE LA NUEVA LÓGICA DE LIMPIEZA ---
                                
                                System.out.println("DEBUG: Notificando al referente - Teléfono: " + referrerPhone + ", Código: " + user.getReferred_by_code());
                                
                                // Obtener el nombre del nuevo usuario
                                String newUserName = user.getName();
                                if (newUserName == null || newUserName.trim().isEmpty()) {
                                    newUserName = "Un nuevo voluntario";
                                }
                                
                                notifyReferrer(referrerPhone, newUserName, user.getReferred_by_code());
                            } catch (Exception e) {
                                System.err.println("ERROR: No se pudo notificar al referente: " + e.getMessage());
                            }
                        }
                        
                        nextChatbotState = "COMPLETED";
                        
                        // NO enviar el menú post-registro automáticamente - esperar a que el usuario escriba algo
                        System.out.println("DEBUG: ✅ Registro completado. Menú NO enviado automáticamente - esperando interacción del usuario.");
                        
                        return new ChatResponse(responseMessage, nextChatbotState, termsSecondaryMessage);
                    } else {
                        // Si no tiene todos los datos, continuar con el flujo normal
                        System.out.println("DEBUG: ⚠️ Usuario no tiene todos los datos. Continuando flujo...");
                        responseMessage = "¿Cuál es tu nombre?";
                        nextChatbotState = "WAITING_NAME";
                    }
                } else {
                    // El usuario no aceptó explícitamente.
                    System.out.println("DEBUG: Usuario NO aceptó los términos explícitamente (validado por IA). Mensaje: '" + messageText + "'");
                    responseMessage = "Entendido. Para continuar y unirte a nuestra campaña, es necesario que aceptes nuestra política de tratamiento de datos respondiendo 'Sí'. Si cambias de opinión, estaré aquí para ayudarte.";
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE"; // Se mantiene en el mismo estado.
                }
                break;
            // case "WAITING_CONTACT_SAVE": - ESTADO ELIMINADO - El bot ahora envía automáticamente el siguiente mensaje después de 5 segundos
            // En WAITING_CONTACT_SAVE esperamos confirmación de que guardó el contacto
            // System.out.println("DEBUG: Procesando respuesta en estado WAITING_CONTACT_SAVE");
            // System.out.println("DEBUG: 🔍 Mensaje original del usuario: '" + messageText + "'");
            
            // Procesar respuesta del usuario (puede ser texto libre o botón interactivo)
            // String lowerContactMessage = messageText.toLowerCase().trim();
            // System.out.println("DEBUG: 🔍 Mensaje normalizado: '" + lowerContactMessage + "'");
            
            // Verificar si es respuesta de botón interactivo o texto libre
            // if (lowerContactMessage.contains("ya guardé") || lowerContactMessage.contains("guardé") || 
            //     lowerContactMessage.contains("listo") || lowerContactMessage.contains("hecho") ||
            //     lowerContactMessage.contains("ok") || lowerContactMessage.contains("perfecto") ||
            //     lowerContactMessage.contains("si") || lowerContactMessage.contains("sí") ||
            //     lowerContactMessage.contains("ya") || lowerContactMessage.contains("completado")) {
            
            //     // Usuario confirmó que guardó el contacto
            //     System.out.println("DEBUG: ✅ Usuario confirmó que guardó el contacto, continuando al siguiente paso");
            //     responseMessage = "¿Me confirmas tu nombre para guardarte en mis contactos?";
            //     nextChatbotState = "WAITING_NAME";
            //     System.out.println("DEBUG: 🔄 Cambiando estado a WAITING_NAME");
            // } else if (lowerContactMessage.contains("necesito más tiempo") || lowerContactMessage.contains("más tiempo") ||
            //            lowerContactMessage.contains("espera") || lowerContactMessage.contains("esperar")) {
            
            //     // Usuario necesita más tiempo
            //     System.out.println("DEBUG: ⏰ Usuario necesita más tiempo");
            //     responseMessage = "No hay problema, tómate tu tiempo. Cuando hayas guardado el contacto, responde con 'Ya guardé' o simplemente escribe cualquier mensaje para continuar.";
            //     nextChatbotState = "WAITING_CONTACT_SAVE"; // Se mantiene en el mismo estado
            // } else if (lowerContactMessage.contains("no sé cómo") || lowerContactMessage.contains("no se como") ||
            //            lowerContactMessage.contains("ayuda") || lowerContactMessage.contains("cómo") ||
            //            lowerContactMessage.contains("como")) {
            
            //     // Usuario necesita ayuda
            //     System.out.println("DEBUG: ❓ Usuario necesita ayuda");
            //     responseMessage = "Te explico paso a paso:\n\n1️⃣ Abre tu aplicación de contactos\n2️⃣ Toca el botón '+' o 'Agregar contacto'\n3️⃣ En el campo 'Nombre' escribe: Daniel Quintero Presidente\n\nCuando termines, responde con 'Ya guardé' o cualquier mensaje para continuar.";
            //     nextChatbotState = "WAITING_CONTACT_SAVE"; // Se mantiene en el mismo estado
            // } else {
            //     // Cualquier otra respuesta se considera como confirmación
            //     System.out.println("DEBUG: ✅ Usuario respondió en WAITING_CONTACT_SAVE, continuando al siguiente paso");
            //     responseMessage = "¿Me confirmas tu nombre para guardarte en mis contactos?";
            //     nextChatbotState = "WAITING_NAME";
            //     System.out.println("DEBUG: 🔄 Cambiando estado a WAITING_NAME");
            // }
            // break;
            case "WAITING_NAME":
                // En WAITING_NAME usamos IA para detectar si es confirmación o nuevo nombre
                System.out.println("DEBUG: Procesando confirmación de nombre en estado WAITING_NAME con IA");
                
                try {
                    // Usar IA para detectar si es confirmación o nuevo nombre
                    UserDataExtractionResult extraction = geminiService.extractUserData(messageText, null, "WAITING_NAME");
                    
                    if (extraction.isSuccessful()) {
                        boolean dataUpdated = false;
                        
                        if (extraction.getName() != null) {
                            user.setName(extraction.getName());
                            System.out.println("DEBUG: IA extrajo nombre: " + extraction.getName());
                            dataUpdated = true;
                        }
                        
                        if (extraction.getLastname() != null) {
                            user.setLastname(extraction.getLastname());
                            System.out.println("DEBUG: IA extrajo apellido: " + extraction.getLastname());
                            dataUpdated = true;
                        }
                        
                        if (extraction.getIsConfirmation() != null && extraction.getIsConfirmation()) {
                            System.out.println("DEBUG: IA detectó confirmación del nombre existente: " + user.getName());
                        } else {
                            System.out.println("DEBUG: IA detectó nuevo nombre/apellido");
                        }
                        
                        // LÓGICA OBLIGATORIA: SIEMPRE pedir nombre completo (nombre + apellido)
                        System.out.println("DEBUG: 🔍 Evaluando lógica de nombre completo:");
                        System.out.println("DEBUG:   - Nombre actual: '" + user.getName() + "'");
                        System.out.println("DEBUG:   - Apellido actual: '" + user.getLastname() + "'");
                        System.out.println("DEBUG:   - ¿Tiene ambos?: " + (user.getName() != null && !user.getName().isEmpty() && user.getLastname() != null && !user.getLastname().isEmpty()));
                        System.out.println("DEBUG: 🔍 Mensaje original del usuario: '" + messageText + "'");
                        System.out.println("DEBUG: 🔍 Resultado de extracción de IA - Success: " + extraction.isSuccessful() + ", Name: " + extraction.getName() + ", Lastname: " + extraction.getLastname());
                        System.out.println("DEBUG: 🔍 Estado del usuario - Reset: " + user.isReset_from_deletion() + ", Referral: " + user.getReferral_code());
                        
                        // VERIFICACIÓN CRÍTICA: Si el usuario viene del reseteo, NO debería tener datos previos
                        // PERO si la IA acaba de extraer datos del mensaje actual, NO limpiarlos
                        if (user.isReset_from_deletion() && extraction.getName() == null && extraction.getLastname() == null) {
                            System.out.println("⚠️  WARNING: Usuario marcado como reseteo pero aún tiene datos previos. Limpiando...");
                            user.setName(null);
                            user.setLastname(null);
                            user.setCity(null);
                            user.setState(null);
                            user.setAceptaTerminos(false);
                            // NO limpiar referral_code - es su identificación única
                            // SÍ limpiar referred_by_phone y referred_by_code - son referencias de quién lo invitó
                            user.setReferred_by_phone(null);
                            user.setReferred_by_code(null);
                            user.setReset_from_deletion(false);
                            saveUser(user);
                            System.out.println("DEBUG: ✅ Datos previos limpiados forzadamente");
                        } else if (user.isReset_from_deletion()) {
                            System.out.println("DEBUG: Usuario viene del reseteo pero la IA extrajo datos del mensaje actual. NO limpiando datos extraídos.");
                            // NO resetear el flag aquí - mantenerlo para la lógica de evaluación
                            saveUser(user);
                        }
                        
                        // LÓGICA MEJORADA: Si el usuario viene del reseteo, siempre pedir apellido después del nombre
                        // para asegurar que los datos sean actuales y correctos
                        if (user.getName() != null && !user.getName().isEmpty() && user.getLastname() != null && !user.getLastname().isEmpty()) {
                            // Solo si tenemos AMBOS campos Y no viene del reseteo, ir a ciudad
                            if (!user.isReset_from_deletion()) {
                                System.out.println("DEBUG: ✅ Usuario tiene nombre Y apellido (no reseteo), yendo a ciudad");
                                responseMessage = "¿En qué ciudad vives?";
                                nextChatbotState = "WAITING_CITY";
                            } else {
                                // Usuario viene del reseteo, pedir apellido para confirmar/actualizar
                                System.out.println("DEBUG: ⚠️ Usuario viene del reseteo, pidiendo apellido para confirmar/actualizar");
                                responseMessage = "¿Cuál es tu apellido?";
                                nextChatbotState = "WAITING_LASTNAME";
                            }
                        } else {
                            // Si falta nombre O apellido, preguntar por apellido
                            if (user.getName() == null || user.getName().isEmpty()) {
                                // Si no hay nombre, preguntar por nombre completo
                                System.out.println("DEBUG: ⚠️ No hay nombre, pidiendo nombre completo");
                                responseMessage = "¿Cuál es tu nombre completo? (nombre y apellido)";
                                nextChatbotState = "WAITING_NAME";
                            } else {
                                // Si hay nombre pero no apellido, preguntar por apellido
                                System.out.println("DEBUG: ⚠️ Hay nombre pero NO apellido, pidiendo apellido");
                                responseMessage = "¿Cuál es tu apellido?";
                                nextChatbotState = "WAITING_LASTNAME";
                            }
                        }
                        
                        // Resetear el flag de reseteo después de la evaluación de la lógica
                        if (user.isReset_from_deletion()) {
                            System.out.println("DEBUG: Reseteando flag de reseteo después de la evaluación de la lógica");
                            user.setReset_from_deletion(false);
                            saveUser(user);
                        }
                    } else {
                        // Fallback: usar lógica tradicional si la IA falla
                        System.out.println("DEBUG: IA falló, usando lógica tradicional");
                        System.out.println("DEBUG: 🔍 Mensaje original del usuario: '" + messageText + "'");
                        System.out.println("DEBUG: 🔍 Estado del usuario (fallback) - Reset: " + user.isReset_from_deletion() + ", Referral: " + user.getReferral_code());
                        
                        // VERIFICACIÓN CRÍTICA: Si el usuario viene del reseteo, NO debería tener datos previos
                        // PERO si el mensaje actual contiene datos, NO limpiarlos
                        if (user.isReset_from_deletion()) {
                            System.out.println("DEBUG: Usuario viene del reseteo en fallback. Verificando si el mensaje actual contiene datos...");
                            // Solo limpiar si el mensaje actual no contiene datos útiles
                            if (messageText.trim().isEmpty() || messageText.trim().equals("si") || messageText.trim().equals("sí")) {
                                System.out.println("⚠️  WARNING: Usuario marcado como reseteo y mensaje sin datos útiles. Limpiando...");
                                user.setName(null);
                                user.setLastname(null);
                                user.setCity(null);
                                user.setState(null);
                                user.setAceptaTerminos(false);
                                // NO limpiar referral_code - es su identificación única
                                // SÍ limpiar referred_by_phone y referred_by_code - son referencias de quién lo invitó
                                user.setReferred_by_phone(null);
                                user.setReferred_by_code(null);
                                user.setReset_from_deletion(false);
                                saveUser(user);
                                System.out.println("DEBUG: ✅ Datos previos limpiados forzadamente (fallback)");
                            } else {
                                System.out.println("DEBUG: Usuario viene del reseteo pero mensaje actual contiene datos. NO limpiando datos.");
                                user.setReset_from_deletion(false); // Solo resetear el flag
                                saveUser(user);
                            }
                        }
                        
                        String lowerNameMessage = messageText.toLowerCase().trim();
                        
                        if (lowerNameMessage.equals("si") || lowerNameMessage.equals("sí") || 
                            lowerNameMessage.equals("correcto") || lowerNameMessage.equals("es correcto") ||
                            lowerNameMessage.contains("si es") || lowerNameMessage.contains("sí es") ||
                            lowerNameMessage.contains("si,") || lowerNameMessage.contains("sí,") ||
                            lowerNameMessage.contains(", es correcto") || lowerNameMessage.contains(",es correcto")) {
                            
                            System.out.println("DEBUG: Usuario confirmó el nombre existente: " + user.getName());
                            responseMessage = "¿Cuál es tu apellido?";
                            nextChatbotState = "WAITING_LASTNAME";
                        } else {
                            // Intentar extraer nombre y apellido del mensaje completo
                            String[] nameParts = messageText.trim().split("\\s+");
                            System.out.println("DEBUG: 🔍 Fallback - Palabras detectadas: " + nameParts.length + " - Contenido: [" + String.join(", ", nameParts) + "]");
                            
                            if (nameParts.length >= 2) {
                                // Si hay al menos 2 palabras, asumir que son nombre y apellido
                                user.setName(nameParts[0]);
                                user.setLastname(nameParts[1]);
                                System.out.println("DEBUG: ✅ Fallback - Extraído nombre: " + nameParts[0] + " y apellido: " + nameParts[1] + " - Yendo a ciudad");
                                responseMessage = "¿En qué ciudad vives?";
                                nextChatbotState = "WAITING_CITY";
                            } else {
                                // Si solo hay una palabra, asumir que es solo el nombre
                                user.setName(messageText.trim());
                                System.out.println("DEBUG: ⚠️ Fallback - Usuario proporcionó solo nombre: " + messageText.trim() + " - Yendo a apellido");
                                responseMessage = "¿Cuál es tu apellido?";
                                nextChatbotState = "WAITING_LASTNAME";
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: Fallo en extracción IA para WAITING_NAME: " + e.getMessage());
                    System.out.println("DEBUG: 🔍 Exception - Mensaje original del usuario: '" + messageText + "'");
                    System.out.println("DEBUG: 🔍 Estado del usuario (exception) - Reset: " + user.isReset_from_deletion() + ", Referral: " + user.getReferral_code());
                    
                    // VERIFICACIÓN CRÍTICA: Si el usuario viene del reseteo, NO debería tener datos previos
                    // PERO si el mensaje actual contiene datos, NO limpiarlos
                    if (user.isReset_from_deletion()) {
                        System.out.println("DEBUG: Usuario viene del reseteo en exception. Verificando si el mensaje actual contiene datos...");
                        // Solo limpiar si el mensaje actual no contiene datos útiles
                        if (messageText.trim().isEmpty() || messageText.trim().equals("si") || messageText.trim().equals("sí")) {
                            System.out.println("⚠️  WARNING: Usuario marcado como reseteo y mensaje sin datos útiles. Limpiando...");
                            user.setName(null);
                            user.setLastname(null);
                            user.setCity(null);
                            user.setState(null);
                            user.setAceptaTerminos(false);
                            // NO limpiar referral_code - es su identificación única
                            // SÍ limpiar referred_by_phone y referred_by_code - son referencias de quién lo invitó
                            user.setReferred_by_phone(null);
                            user.setReferred_by_code(null);
                            user.setReset_from_deletion(false);
                            saveUser(user);
                            System.out.println("DEBUG: ✅ Datos previos limpiados forzadamente (exception)");
                        } else {
                            System.out.println("DEBUG: Usuario viene del reseteo pero mensaje actual contiene datos. NO limpiando datos.");
                            user.setReset_from_deletion(false); // Solo resetear el flag
                            saveUser(user);
                        }
                    }
                    
                    // Fallback en caso de error
                    String lowerNameMessage = messageText.toLowerCase().trim();
                    
                    if (lowerNameMessage.equals("si") || lowerNameMessage.equals("sí") || 
                        lowerNameMessage.equals("correcto") || lowerNameMessage.equals("es correcto") ||
                        lowerNameMessage.contains("si es") || lowerNameMessage.contains("sí es") ||
                        lowerNameMessage.contains("si,") || lowerNameMessage.contains("sí,") ||
                        lowerNameMessage.contains(", es correcto") || lowerNameMessage.contains(",es correcto")) {
                        
                        System.out.println("DEBUG: Exception - Usuario confirmó el nombre existente: " + user.getName());
                        responseMessage = "¿Cuál es tu apellido?";
                        nextChatbotState = "WAITING_LASTNAME";
                    } else {
                        // Intentar extraer nombre y apellido del mensaje completo
                        String[] nameParts = messageText.trim().split("\\s+");
                        System.out.println("DEBUG: 🔍 Exception - Fallback - Palabras detectadas: " + nameParts.length + " - Contenido: [" + String.join(", ", nameParts) + "]");
                        
                        if (nameParts.length >= 2) {
                            // Si hay al menos 2 palabras, asumir que son nombre y apellido
                            user.setName(nameParts[0]);
                            user.setLastname(nameParts[1]);
                            System.out.println("DEBUG: ✅ Exception - Fallback - Extraído nombre: " + nameParts[0] + " y apellido: " + nameParts[1] + " - Yendo a ciudad");
                            responseMessage = "¿En qué ciudad vives?";
                            nextChatbotState = "WAITING_CITY";
                        } else {
                            // Si solo hay una palabra, asumir que es solo el nombre
                            user.setName(messageText.trim());
                            System.out.println("DEBUG: ⚠️ Exception - Fallback - Usuario proporcionó solo nombre: " + messageText.trim() + " - Yendo a apellido");
                            responseMessage = "¿Cuál es tu apellido?";
                            nextChatbotState = "WAITING_LASTNAME";
                        }
                    }
                }
                break;
            case "WAITING_LASTNAME":
                // En WAITING_LASTNAME usar extracción inteligente para apellido
                System.out.println("DEBUG: Procesando apellido en estado WAITING_LASTNAME con IA");
                System.out.println("DEBUG: 🔍 Mensaje original del usuario: '" + messageText + "'");
                System.out.println("DEBUG: 🔍 Estado del usuario - Reset: " + user.isReset_from_deletion() + ", Referral: " + user.getReferral_code());
                
                // VERIFICACIÓN CRÍTICA: Si el usuario viene del reseteo, NO debería tener datos previos
                if (user.isReset_from_deletion()) {
                    System.out.println("⚠️  WARNING: Usuario marcado como reseteo pero aún tiene datos previos en WAITING_LASTNAME. Limpiando...");
                    user.setName(null);
                    user.setLastname(null);
                    user.setCity(null);
                    user.setState(null);
                    user.setAceptaTerminos(false);
                    // NO limpiar referral_code - es su identificación única
                    // SÍ limpiar referred_by_phone y referred_by_code - son referencias de quién lo invitó
                    user.setReferred_by_phone(null);
                    user.setReferred_by_code(null);
                    user.setReset_from_deletion(false);
                    saveUser(user);
                    System.out.println("DEBUG: ✅ Datos previos limpiados forzadamente en WAITING_LASTNAME");
                }
                
                try {
                    UserDataExtractionResult extraction = geminiService.extractUserData(messageText, null, "WAITING_LASTNAME");
                    System.out.println("DEBUG: 🔍 Resultado de extracción de IA para apellido - Success: " + extraction.isSuccessful() + ", Lastname: " + extraction.getLastname());
                    
                    if (extraction.isSuccessful() && extraction.getLastname() != null) {
                        user.setLastname(extraction.getLastname());
                        System.out.println("DEBUG: ✅ IA extrajo apellido: " + extraction.getLastname() + " - Yendo a ciudad");
                        responseMessage = "¿En qué ciudad vives?";
                        nextChatbotState = "WAITING_CITY";
                    } else {
                        // Fallback: usar texto completo si la IA falla
                        System.out.println("DEBUG: ⚠️ IA falló, usando fallback para apellido");
                        user.setLastname(messageText.trim());
                        System.out.println("DEBUG: ✅ Apellido establecido (fallback): " + messageText.trim() + " - Yendo a ciudad");
                        responseMessage = "¿En qué ciudad vives?";
                        nextChatbotState = "WAITING_CITY";
                    }
                } catch (Exception e) {
                    System.err.println("Error en extracción IA para apellido: " + e.getMessage());
                    System.out.println("DEBUG: 🔍 Exception - Mensaje original del usuario: '" + messageText + "'");
                    // Fallback en caso de error
                    user.setLastname(messageText.trim());
                    System.out.println("DEBUG: ✅ Exception - Apellido establecido (fallback): " + messageText.trim() + " - Yendo a ciudad");
                    responseMessage = "¿En qué ciudad vives?";
                    nextChatbotState = "WAITING_CITY";
                }
                break;
            case "WAITING_CITY":
                // En WAITING_CITY usar extracción inteligente para ciudad
                System.out.println("DEBUG: Procesando ciudad en estado WAITING_CITY con IA");
                System.out.println("DEBUG: 🔍 Mensaje original del usuario: '" + messageText + "'");
                System.out.println("DEBUG: 🔍 Usuario actual - Nombre: '" + user.getName() + "', Apellido: '" + user.getLastname() + "'");
                System.out.println("DEBUG: 🔍 Estado del usuario - Reset: " + user.isReset_from_deletion() + ", Referral: " + user.getReferral_code());
                
                // VERIFICACIÓN CRÍTICA: Si el usuario viene del reseteo, NO debería tener datos previos
                if (user.isReset_from_deletion()) {
                    System.out.println("⚠️  WARNING: Usuario marcado como reseteo pero aún tiene datos previos en WAITING_CITY. Limpiando...");
                    user.setName(null);
                    user.setLastname(null);
                    user.setCity(null);
                    user.setState(null);
                    user.setAceptaTerminos(false);
                    // NO limpiar referral_code - es su identificación única
                    // SÍ limpiar referred_by_phone y referred_by_code - son referencias de quién lo invitó
                    user.setReferred_by_phone(null);
                    user.setReferred_by_code(null);
                    user.setReset_from_deletion(false);
                    saveUser(user);
                    System.out.println("DEBUG: ✅ Datos previos limpiados forzadamente en WAITING_CITY");
                }
                
                try {
                    UserDataExtractionResult extraction = geminiService.extractUserData(messageText, null, "WAITING_CITY");
                    System.out.println("DEBUG: 🔍 Resultado de extracción de IA para ciudad - Success: " + extraction.isSuccessful() + ", City: " + extraction.getCity() + ", State: " + extraction.getState());
                    
                    if (extraction.isSuccessful() && extraction.getCity() != null) {
                        user.setCity(extraction.getCity());
                        if (extraction.getState() != null) {
                            user.setState(extraction.getState());
                        }
                        System.out.println("DEBUG: ✅ IA extrajo ciudad: " + extraction.getCity() + 
                                         (extraction.getState() != null ? ", estado: " + extraction.getState() : ""));
                    } else {
                        // Fallback: usar texto completo si la IA falla
                        System.out.println("DEBUG: ⚠️ IA falló, usando fallback para ciudad");
                        user.setCity(messageText.trim());
                        System.out.println("DEBUG: ✅ Ciudad establecida (fallback): " + messageText.trim());
                    }
                    
                    // Construir nombre completo para mostrar
                    String fullName = user.getName();
                    if (user.getLastname() != null && !user.getLastname().trim().isEmpty()) {
                        fullName += " " + user.getLastname();
                    }
                    System.out.println("DEBUG: 🔍 Nombre completo construido: '" + fullName + "'");
                    
                    // Ir directamente a confirmación de datos sin mensaje intermedio
                    responseMessage = null; // Sin mensaje de transición
                    nextChatbotState = "CONFIRM_DATA";
                    System.out.println("DEBUG: ✅ Yendo a CONFIRM_DATA para confirmación");
                    
                    // Enviar resumen de datos para confirmación inmediatamente
                    final String userPhone3 = user.getPhone();
                    final User finalUser = user;
                    scheduler.schedule(() -> {
                        sendDataConfirmationMessage(userPhone3, finalUser);
                    }, 1, TimeUnit.SECONDS); // Reducido a 1 segundo
                } catch (Exception e) {
                    System.err.println("Error en extracción IA para ciudad: " + e.getMessage());
                    System.out.println("DEBUG: 🔍 Exception - Mensaje original del usuario: '" + messageText + "'");
                    System.out.println("DEBUG: 🔍 Estado del usuario (exception) - Reset: " + user.isReset_from_deletion() + ", Referral: " + user.getReferral_code());
                    
                    // VERIFICACIÓN CRÍTICA: Si el usuario viene del reseteo, NO debería tener datos previos
                    if (user.isReset_from_deletion()) {
                        System.out.println("⚠️  WARNING: Usuario marcado como reseteo pero aún tiene datos previos en WAITING_CITY (exception). Limpiando...");
                        user.setName(null);
                        user.setLastname(null);
                        user.setCity(null);
                        user.setState(null);
                        user.setAceptaTerminos(false);
                        // NO limpiar referral_code - es su identificación única
                        // SÍ limpiar referred_by_phone y referred_by_code - son referencias de quién lo invitó
                        user.setReferred_by_phone(null);
                        user.setReferred_by_code(null);
                        user.setReset_from_deletion(false);
                        saveUser(user);
                        System.out.println("DEBUG: ✅ Datos previos limpiados forzadamente en WAITING_CITY (exception)");
                    }
                    
                    // Fallback en caso de error
                    user.setCity(messageText.trim());
                    
                    String fullName = user.getName();
                    if (user.getLastname() != null && !user.getLastname().trim().isEmpty()) {
                        fullName += " " + user.getLastname();
                    }
                    System.out.println("DEBUG: 🔍 Exception - Nombre completo construido: '" + fullName + "'");
                    
                    responseMessage = null; // Sin mensaje de transición
                    nextChatbotState = "CONFIRM_DATA";
                    System.out.println("DEBUG: ✅ Exception - Yendo a CONFIRM_DATA para confirmación");
                    
                    // Enviar resumen de datos para confirmación inmediatamente
                    final String userPhone4 = user.getPhone();
                    final User finalUser2 = user;
                    scheduler.schedule(() -> {
                        sendDataConfirmationMessage(userPhone4, finalUser2);
                    }, 1, TimeUnit.SECONDS); // Reducido a 1 segundo
                }
                break;
            case "CONFIRM_DATA":
                if (messageText.equalsIgnoreCase("Sí") || messageText.equalsIgnoreCase("Si") || messageText.equals("✅ SÍ")) {
                    // Verificar si ya aceptó los términos
                    if (!user.isAceptaTerminos()) {
                        // Si no aceptó términos, pedirle que los acepte con botones interactivos
                        nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                        
                        // Enviar mensaje de privacidad con botones interactivos después de un retraso
                        final String userPhone5 = user.getPhone();
                        scheduler.schedule(() -> {
                            sendPrivacyMessageWithButtons(userPhone5);
                        }, 5, TimeUnit.SECONDS);
                        
                        return new ChatResponse("", nextChatbotState);
                } else if (messageText.equalsIgnoreCase("No") || messageText.equals("❌ NO")) {
                    // Usuario necesita corregir datos. Repitiendo desde tomar el nombre.
                    responseMessage = "Entendido. Empecemos de nuevo. ¿Cuál es tu nombre?";
                    nextChatbotState = "WAITING_NAME";
                    
                    // Limpiar datos del usuario para empezar de nuevo
                    user.setName(null);
                    user.setLastname(null);
                    user.setCity(null);
                    user.setState(null);
                    
                    return new ChatResponse(responseMessage, nextChatbotState);
                }
                
                // Si ya aceptó términos, completar el registro
                    // IMPORTANTE: Si el usuario ya tiene referral_code (viene del reseteo), NO generar uno nuevo
                    String referralCode;
                    if (user.getReferral_code() != null && !user.getReferral_code().isEmpty()) {
                        referralCode = user.getReferral_code(); // Mantener el existente
                        System.out.println("DEBUG: ✅ Usuario reseteado en CONFIRM_DATA, manteniendo referral_code existente: " + referralCode);
                    } else {
                        referralCode = generateUniqueReferralCode(); // Generar nuevo solo si no existe
                        user.setReferral_code(referralCode);
                        System.out.println("DEBUG: ✅ Generando nuevo referral_code en CONFIRM_DATA: " + referralCode);
                    }

                    String whatsappInviteLink;
                    String telegramInviteLink;

                    List<String> additionalMessages = new ArrayList<>();

                    try {
                        String whatsappRawReferralText = String.format("Hola, vengo referido por %s, codigo: %s", user.getName(), referralCode);
                        System.out.println("Texto crudo antes de codificar: '" + whatsappRawReferralText + "'");
                        String encodedWhatsappMessage = URLEncoder
                                .encode(whatsappRawReferralText, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                        whatsappInviteLink = "https://wa.me/" + getWhatsAppInviteNumber() + "?text=" + encodedWhatsappMessage;

                        String encodedTelegramPayload = URLEncoder.encode(referralCode,
                                StandardCharsets.UTF_8.toString());
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start="
                                + encodedTelegramPayload;

                        String friendsInviteMessage = String.format(
                                "Amigos, soy %s y quiero invitarte a unirte a la campaña de Daniel Quintero a la Presidencia: %s",
                                user.getName(),
                                whatsappInviteLink);
                        additionalMessages.add(friendsInviteMessage);

                        // Enviar el video de bienvenida ANTES del mensaje de IA
                        try {
                            watiApiService.sendVideoMessage(user.getPhone(), welcomeVideoUrl, "Video de bienvenida a la campaña");
                            System.out.println("DEBUG: Video de bienvenida enviado a: " + user.getPhone());
                        } catch (Exception e) {
                            System.err.println("DEBUG: ⚠️ Error al enviar video de bienvenida: " + e.getMessage());
                            System.err.println("DEBUG: ⚠️ Continuando con flujo normal sin video...");
                            // No lanzar la excepción, continuar con el flujo normal
                        }

                    } catch (UnsupportedEncodingException e) {
                        System.err.println(
                                "ERROR: No se pudo codificar los códigos de referido. Causa: " + e.getMessage());
                        e.printStackTrace();
                        whatsappInviteLink = "https://wa.me/" + getWhatsAppInviteNumber() + "?text=Error%20al%20generar%20referido";
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=Error";
                        additionalMessages.clear();
                        additionalMessages.add("Error al generar los mensajes de invitación.");
                    }

                    responseMessage = String.format(
                            "%s, gracias por unirte como voluntario. Tu primera misión es enviar el siguiente link a tus amigos de modo que más personas se sumen.",
                            user.getName()
                    );

                    secondaryMessage = Optional.of(String.join("###SPLIT###", additionalMessages));

                    // Notificar al referente si este usuario fue referido
                    if (user.getReferred_by_phone() != null && user.getReferred_by_code() != null) {
                        try {
                            // Formatear el teléfono del referente para la notificación
                            String referrerPhone = user.getReferred_by_phone();
                            
                            // --- INICIO DE LA NUEVA LÓGICA DE LIMPIEZA ---
                            // 1. Quitar todos los caracteres que no sean dígitos
                            String digitsOnly = referrerPhone.replaceAll("[^\\d]", "");
                            // 2. Si empieza con "57" y tiene 12 dígitos, es un número colombiano completo.
                            if (digitsOnly.startsWith("57") && digitsOnly.length() == 12) {
                                // Tomar solo los últimos 10 dígitos y añadir el prefijo correcto.
                                referrerPhone = "+57" + digitsOnly.substring(2);
                            } else if (digitsOnly.length() == 10) {
                                // Si solo tiene 10 dígitos, es un número local. Añadir prefijo.
                                referrerPhone = "+57" + digitsOnly;
                            } else if (!referrerPhone.startsWith("+")) {
                                // Como última opción, si no empieza con +, añadirlo.
                                referrerPhone = "+" + referrerPhone;
                            }
                            // --- FIN DE LA NUEVA LÓGICA DE LIMPIEZA ---
                            
                            System.out.println("DEBUG: Notificando al referente - Teléfono: " + referrerPhone + ", Código: " + user.getReferred_by_code());
                            
                            // Obtener el nombre del nuevo usuario
                            String newUserName = user.getName();
                            if (newUserName == null || newUserName.trim().isEmpty()) {
                                newUserName = "Un nuevo voluntario";
                            }
                            
                            notifyReferrer(referrerPhone, newUserName, user.getReferred_by_code());
                        } catch (Exception e) {
                            System.err.println("ERROR: No se pudo notificar al referente: " + e.getMessage());
                        }
                    }

                    nextChatbotState = "COMPLETED";
                    
                    // Enviar el menú post-registro después de completar el registro
                    // NO enviar el menú post-registro automáticamente - esperar a que el usuario escriba algo
                    System.out.println("DEBUG: ✅ Registro completado. Menú NO enviado automáticamente - esperando interacción del usuario.");
                } else {
                    // Usuario dijo "No" - repetir desde tomar el nombre
                    System.out.println("DEBUG: Usuario necesita corregir datos. Repitiendo desde tomar el nombre.");
                    
                    // Limpiar datos y volver a empezar desde el nombre
                    user.setName(null);
                    user.setLastname(null);
                    user.setCity(null);
                    user.setState(null);
                    
                    responseMessage = "Entendido. Empecemos de nuevo. ¿Cuál es tu nombre?";
                    nextChatbotState = "WAITING_NAME";
                }
                break;
            // case "WAITING_CORRECTION_TYPE": - ESTADO ELIMINADO - Ahora se repite desde el nombre cuando el usuario dice "No"
            case "COMPLETED":
                System.out.println("ChatbotService: Usuario COMPLETED. Verificando configuración del sistema...");
                
                // NUEVA LÓGICA: Verificar si es una respuesta de botón del menú post-registro
                if (isPostRegistrationMenuButton(messageText)) {
                    System.out.println("ChatbotService: Detectada respuesta de botón del menú post-registro: " + messageText);
                    
                    // Obtener el número de teléfono del usuario
                    String phoneNumber = user.getPhone();
                    if (phoneNumber == null || phoneNumber.isEmpty()) {
                        phoneNumber = user.getTelegram_chat_id();
                    }
                    
                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        // Procesar la selección del botón
                        postRegistrationMenuService.handleMenuSelection(phoneNumber, messageText, user);
                        
                        // Mantener estado COMPLETED y no enviar respuesta adicional
                        nextChatbotState = "COMPLETED";
                        return new ChatResponse("", nextChatbotState, secondaryMessage);
                    }
                }
                
                // PRIMERO: Verificar si la IA está habilitada globalmente en el sistema
                if (systemConfigService.isAIEnabled()) {
                    System.out.println("ChatbotService: IA del sistema HABILITADA. Verificando si DQBot está activo para este usuario...");
                    
                    // SEGUNDO: Verificar si DQBot está activo para este usuario específico
                    if (postRegistrationMenuService.isDQBotActive(user)) {
                        System.out.println("ChatbotService: DQBot activo para usuario COMPLETED. Procesando mensaje con IA...");
                        
                        // Obtener el número de teléfono del usuario
                        String phoneNumber = user.getPhone();
                        if (phoneNumber == null || phoneNumber.isEmpty()) {
                            phoneNumber = user.getTelegram_chat_id();
                        }
                        
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            // Procesar mensaje con DQBot
                            postRegistrationMenuService.processDQBotMessage(phoneNumber, messageText, user);
                            
                            // Mantener estado COMPLETED y no enviar respuesta adicional
                            nextChatbotState = "COMPLETED";
                            return new ChatResponse("", nextChatbotState, secondaryMessage);
                        }
                    } else {
                        System.out.println("ChatbotService: IA habilitada pero DQBot NO activo para este usuario. Delegando al menú principal...");
                    }
                } else {
                    System.out.println("ChatbotService: IA del sistema DESHABILITADA. Todos los usuarios serán atendidos por agentes humanos.");
                }
                
                // Obtener session ID para el análisis
                String sessionId = user.getPhone();
                if ((sessionId == null || sessionId.isEmpty()) && user.getTelegram_chat_id() != null) {
                    System.err.println("ADVERTENCIA: Usuario COMPLETED sin teléfono. Usando Telegram Chat ID ("
                            + user.getTelegram_chat_id() + ") como fallback para la sesión de IA. Doc ID: "
                            + user.getId());
                    sessionId = user.getTelegram_chat_id();
                }

                if (sessionId != null && !sessionId.isEmpty()) {
                    // PRIMERO verificar si es una solicitud de eliminación - ESTO DEBE FUNCIONAR SIEMPRE
                    DeleteRequestResult deleteResult = isDeleteRequest(messageText);
                    if (deleteResult.isDeleteRequest()) {
                        System.out.println("ChatbotService: Detectada solicitud de eliminación tipo: " + deleteResult.getDeleteType());
                        
                        // Construir la URL del endpoint de reset usando la configuración del servidor
                        String baseUrl = "http://localhost:" + serverPort;
                        if (contextPath != null && !contextPath.isEmpty()) {
                            baseUrl += contextPath;
                        }
                        
                        try {
                            if ("PERSONAL".equals(deleteResult.getDeleteType())) {
                                // Eliminación personal - solo resetear el usuario
                                String resetUrl = baseUrl + "/api/admin/reset/" + user.getPhone().substring(1);
                                System.out.println("ChatbotService: Llamando al endpoint de reset personal: " + resetUrl);
                                
                                ResponseEntity<Map> resetResponse = restTemplate.exchange(resetUrl, org.springframework.http.HttpMethod.DELETE, null, Map.class);
                                
                                if (resetResponse.getStatusCode() == HttpStatus.OK) {
                                    // Actualizar el estado del usuario en la sesión actual para que no continúe como COMPLETED
                                    user.setChatbot_state("NEW");
                                    user.setAceptaTerminos(false);
                                    user.setUpdated_at(Timestamp.now());
                                    // Marcar que viene del reseteo para pedir datos nuevamente
                                    user.setReset_from_deletion(true);
                                    
                                    responseMessage = "Tu solicitud de eliminación ha sido procesada exitosamente. Tu cuenta ha sido reseteada y puedes volver a comenzar el proceso cuando quieras.";
                                    nextChatbotState = "NEW";
                                    System.out.println("ChatbotService: Usuario eliminado/reseteado exitosamente, estado cambiado a NEW con flag de reseteo");
                                } else {
                                    responseMessage = "Lo siento, hubo un problema al procesar tu solicitud de eliminación. Por favor, intenta de nuevo más tarde.";
                                    nextChatbotState = "COMPLETED";
                                    System.out.println("ChatbotService: Error al eliminar/resetear usuario");
                                }
                            } else if ("TRIBU".equals(deleteResult.getDeleteType())) {
                                // Eliminación de tribu - resetear usuario y todos sus referidos
                                System.out.println("ChatbotService: Procesando eliminación de tribu completa...");
                                
                                // Primero resetear al usuario principal
                                String resetUrl = baseUrl + "/api/admin/reset/" + user.getPhone().substring(1);
                                ResponseEntity<Map> resetResponse = restTemplate.exchange(resetUrl, org.springframework.http.HttpMethod.DELETE, null, Map.class);
                                
                                if (resetResponse.getStatusCode() == HttpStatus.OK) {
                                    // Buscar y resetear todos los usuarios referidos
                                    int referredUsersReset = resetReferredUsers(user.getReferral_code());
                                    System.out.println("ChatbotService: Usuarios referidos reseteados: " + referredUsersReset);
                                    
                                    // Actualizar el estado del usuario en la sesión actual
                                    user.setChatbot_state("NEW");
                                    user.setAceptaTerminos(false);
                                    user.setUpdated_at(Timestamp.now());
                                    // Marcar que viene del reseteo para pedir datos nuevamente
                                    user.setReset_from_deletion(true);
                                    
                                    responseMessage = "Tu solicitud de eliminación de tribu ha sido procesada exitosamente. Tu cuenta y la de " + referredUsersReset + " usuarios referidos han sido reseteadas.";
                                    nextChatbotState = "NEW";
                                    System.out.println("ChatbotService: Tribu eliminada exitosamente, " + referredUsersReset + " usuarios referidos reseteados con flag de reseteo");
                                } else {
                                    responseMessage = "Lo siento, hubo un problema al procesar tu solicitud de eliminación de tribu. Por favor, intenta de nuevo más tarde.";
                                    nextChatbotState = "COMPLETED";
                                    System.out.println("ChatbotService: Error al eliminar tribu");
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("ERROR: Error al llamar al endpoint de reset: " + e.getMessage());
                            responseMessage = "Lo siento, hubo un problema al procesar tu solicitud de eliminación. Por favor, intenta de nuevo más tarde.";
                            nextChatbotState = "COMPLETED";
                        }
                        
                        return new ChatResponse(responseMessage, nextChatbotState, secondaryMessage);
                    }
                    
                    // Si no es eliminación y no es DQBot, delegar la selección de botones al PostRegistrationMenuService
                    System.out.println("ChatbotService: Usuario COMPLETED sin DQBot activo. Delegando selección de botones al PostRegistrationMenuService...");
                    
                    // Obtener el número de teléfono del usuario
                    String phoneNumber = user.getPhone();
                    if (phoneNumber == null || phoneNumber.isEmpty()) {
                        phoneNumber = user.getTelegram_chat_id();
                    }
                    
                    if (phoneNumber != null && !phoneNumber.isEmpty()) {
                        // Delegar la selección de botones al PostRegistrationMenuService
                        postRegistrationMenuService.handleMenuSelection(phoneNumber, messageText, user);
                    }
                    
                    // Mantener estado COMPLETED
                    nextChatbotState = "COMPLETED";
                    return new ChatResponse("", nextChatbotState, secondaryMessage);
                } else {
                    System.err.println(
                            "ERROR CRÍTICO: Usuario COMPLETED sin un ID de sesión válido (ni teléfono, ni Telegram ID). Doc ID: "
                                    + user.getId());
                    responseMessage = "Lo siento, hemos encontrado un problema con tu registro y no puedo continuar la conversación. Por favor, contacta a soporte.";
                    nextChatbotState = "COMPLETED";
                }
                break;
                
            case "NEW":
                // Si el usuario está en estado NEW, verificar si viene del reseteo
                System.out.println("DEBUG handleExistingUserMessage: Usuario en estado NEW, verificando si viene del reseteo");
                System.out.println("DEBUG: 🔍 Usuario en estado NEW - Nombre: '" + user.getName() + "', Apellido: '" + user.getLastname() + "', Ciudad: '" + user.getCity() + "', Reset: " + user.isReset_from_deletion());
                
                // PRIMERO: Verificar si el mensaje contiene código de referido (independientemente del reseteo)
                System.out.println("DEBUG handleExistingUserMessage: Usuario en estado NEW, verificando si contiene código de referido");
                System.out.println("DEBUG: 🔍 Mensaje original del usuario: '" + messageText + "'");
                
                // Usar el mismo patrón que se usa para usuarios nuevos
                Matcher newMatcher = REFERRAL_MESSAGE_PATTERN.matcher(messageText.trim());
                System.out.println("DEBUG handleExistingUserMessage: Resultado de la coincidencia del patrón Regex: " + newMatcher.matches());
                
                if (newMatcher.matches()) {
                    // El nuevo patrón tiene dos grupos: uno para "referido por" y otro para "codigo"
                    String referralCode = newMatcher.group(1) != null ? newMatcher.group(1) : newMatcher.group(2);
                    System.out.println("DEBUG handleExistingUserMessage: 🔍 Código de referido detectado en estado NEW: " + referralCode);
                    System.out.println("DEBUG: 🔍 Mensaje original del usuario: '" + messageText + "'");
                        
                    // Buscar el usuario referente
                    Optional<User> referrerUser = getUserByReferralCode(referralCode);
                        
                    if (referrerUser.isPresent()) {
                        // Establecer los campos de referido
                        String referrerPhone = referrerUser.get().getPhone();
                        System.out.println("DEBUG handleExistingUserMessage: 🔍 Número original del referente: " + referrerPhone);
                            
                        // Extraer solo la parte local del número (sin código de país)
                        if (referrerPhone != null) {
                            if (referrerPhone.startsWith("+57")) {
                                referrerPhone = referrerPhone.substring(3); // Quitar +57
                                System.out.println("DEBUG handleExistingUserMessage: 🔍 Número después de quitar +57: " + referrerPhone);
                            } else if (referrerPhone.startsWith("57")) {
                                referrerPhone = referrerPhone.substring(2); // Quitar 57
                                System.out.println("DEBUG handleExistingUserMessage: 🔍 Número después de quitar 57: " + referrerPhone);
                            } else if (referrerPhone.startsWith("+")) {
                                referrerPhone = referrerPhone.substring(1); // Quitar +
                                System.out.println("DEBUG handleExistingUserMessage: 🔍 Número después de quitar +: " + referrerPhone);
                            } else {
                                System.out.println("DEBUG handleExistingUserMessage: 🔍 Número sin procesar (no empieza con +57, 57 o +): " + referrerPhone);
                            }
                        }
                            
                        System.out.println("DEBUG handleExistingUserMessage: 🔍 Número final a guardar: " + referrerPhone);
                        user.setReferred_by_phone(referrerPhone);
                        user.setReferred_by_code(referralCode);
                            
                        System.out.println("DEBUG handleExistingUserMessage: ✅ Referido establecido - Phone: " + user.getReferred_by_phone() + ", Code: " + user.getReferred_by_code());
                            
                        // Guardar usuario con referido
                        saveUser(user);
                            
                        // Continuar con el flujo normal (pedir datos básicos primero)
                        System.out.println("DEBUG handleExistingUserMessage: Código de referido procesado, continuando con flujo de datos básicos");
                        // Redirigir a handleNewUserIntro para seguir el flujo estándar
                        return handleNewUserIntro(user, messageText);
                    } else {
                        System.out.println("DEBUG handleExistingUserMessage: ⚠️ Código de referido no encontrado: " + referralCode);
                        System.out.println("DEBUG: 🔍 Continuando con flujo normal sin referido válido");
                    }
                }
                
                // SEGUNDO: Si no hay código de referido o no se pudo procesar, verificar si viene del reseteo
                System.out.println("DEBUG: 🔍 Verificando si usuario viene del reseteo: " + user.isReset_from_deletion());
                if (user.isReset_from_deletion()) {
                    System.out.println("DEBUG handleExistingMessage: Usuario viene del reseteo, pidiendo datos nuevamente");
                    System.out.println("DEBUG: 🔍 Usuario antes del reseteo - Nombre: '" + user.getName() + "', Apellido: '" + user.getLastname() + "', Ciudad: '" + user.getCity() + "'");
                    
                    // IMPORTANTE: Limpiar datos personales y referencias, MANTENER el referral_code del usuario
                    // El referral_code es su identificación única y NUNCA debe resetearse
                    user.setName(null);
                    user.setLastname(null);
                    user.setCity(null);
                    user.setState(null);
                    user.setAceptaTerminos(false);
                    // NO limpiar referral_code - es su identificación única
                    // SÍ limpiar referred_by_phone y referred_by_code - son referencias de quién lo invitó
                    user.setReferred_by_phone(null);
                    user.setReferred_by_code(null);
                    user.setReset_from_deletion(false); // Resetear el flag
                    System.out.println("DEBUG: ✅ Usuario reseteado - datos personales limpiados, manteniendo referral_code");
                    System.out.println("DEBUG: 🔍 Usuario después del reseteo - Nombre: '" + user.getName() + "', Apellido: '" + user.getLastname() + "', Ciudad: '" + user.getCity() + "', Referral: '" + user.getReferral_code() + "'");
                    saveUser(user);
                    
                    // Enviar mensaje de bienvenida primero, y mensaje de contacto como secundario
                    responseMessage = "Hola. Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
                                nextChatbotState = "WAITING_NAME";
            System.out.println("DEBUG: 🔄 Usuario reseteado - Cambiando estado a WAITING_NAME");
                    
                    // Crear ChatResponse con mensaje secundario usando el constructor correcto
                    return new ChatResponse(responseMessage, nextChatbotState, 
                        Optional.of(ADD_CONTACT_CTA));
                }
                
                // TERCERO: Si no hay código de referido y no viene del reseteo, continuar con el flujo normal
                System.out.println("DEBUG handleExistingUserMessage: Continuando con flujo normal para usuario NEW");
                System.out.println("DEBUG: 🔍 Usuario sin código de referido ni reseteo - Nombre: '" + user.getName() + "', Apellido: '" + user.getLastname() + "', Ciudad: '" + user.getCity() + "'");
                // Para usuarios en estado NEW sin código de referido, usar el flujo de bienvenida estándar
                System.out.println("DEBUG handleExistingUserMessage: Redirigiendo a handleNewUserIntro para flujo estándar");
                System.out.println("DEBUG: 🔄 Estado final: " + nextChatbotState);
                return handleNewUserIntro(user, messageText);
                
                
            case "UNKNOWN_STATE":
                // Caso especial para estados no manejados
                System.out.println("⚠️  WARNING: Usuario en estado no manejado ('" + currentChatbotState
                        + "'). Redirigiendo al flujo de inicio.");
                System.out.println("⚠️  WARNING: Llamando handleNewUserIntro desde estado no manejado para usuario existente");
                return handleNewUserIntro(user, messageText);
                
            default:
                // Caso por defecto para cualquier estado no manejado
                System.out.println("⚠️  WARNING: Usuario en estado no manejado ('" + currentChatbotState
                        + "'). Redirigiendo al flujo de inicio.");
                System.out.println("⚠️  WARNING: Llamando handleNewUserIntro desde estado no manejado para usuario existente");
                return handleNewUserIntro(user, messageText);
        }
        
        return new ChatResponse(responseMessage, nextChatbotState, secondaryMessage);
    }
    

    // --- Métodos Auxiliares para búsqueda de usuario ---

    /**
     * Busca un usuario por su campo 'phone'.
     * Requiere que el campo 'phone' esté indexado en Firestore.
     */
    private Optional<User> findUserByPhoneNumberField(String phoneNumber) {
        try {
            System.out.println("DEBUG: Iniciando búsqueda en Firestore por campo 'phone': " + phoneNumber);
            ApiFuture<QuerySnapshot> future = firestore.collection("users")
                    .whereEqualTo("phone", phoneNumber)
                    .limit(1)
                    .get();
            System.out.println("DEBUG: Consulta Firestore enviada, esperando respuesta...");
            QuerySnapshot querySnapshot = future.get();
            System.out.println("DEBUG: Respuesta de Firestore recibida, procesando...");

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                System.out.println("DEBUG: Documento encontrado en Firestore, convirtiendo a objeto User...");
                return Optional.ofNullable(document.toObject(User.class));
            } else {
                System.out.println("DEBUG: No se encontraron documentos en Firestore para el teléfono: " + phoneNumber);
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println(
                    "ERROR al buscar usuario por campo 'phone' en Firestore (" + phoneNumber + "): " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Busca un usuario por su campo 'telegram_chat_id'.
     * Requiere que el campo 'telegram_chat_id' esté indexado en Firestore.
     */
    private Optional<User> findUserByTelegramChatIdField(String telegramChatId) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("users")
                    .whereEqualTo("telegram_chat_id", telegramChatId)
                    .limit(1)
                    .get();
            QuerySnapshot querySnapshot = future.get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.ofNullable(document.toObject(User.class));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("ERROR al buscar usuario por campo 'telegram_chat_id' en Firestore (" + telegramChatId
                    + "): " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Busca un usuario por su ID de documento.
     * Útil si se guarda por phone number sin el '+', o por UUID.
     */
    private Optional<User> findUserByDocumentId(String documentId) {
        try {
            ApiFuture<DocumentSnapshot> future = firestore.collection("users").document(documentId).get();
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return Optional.ofNullable(document.toObject(User.class));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println(
                    "ERROR al buscar usuario por ID de documento en Firestore (" + documentId + "): " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Unifica la búsqueda de usuario, intentando por número de teléfono o por chat
     * ID de Telegram.
     * Esta es la función principal que debe usarse para encontrar un usuario
     * existente.
     */
    private Optional<User> findUserByAnyIdentifier(String fromId, String channelType) {
        Optional<User> user = Optional.empty();

        String cleanedFromId = fromId.replaceAll("[^\\d+]", "");

        String phoneNumberToSearch = "";

        if (cleanedFromId.startsWith("+") && STRICT_PHONE_NUMBER_PATTERN.matcher(cleanedFromId).matches()) {
            phoneNumberToSearch = cleanedFromId;
        } else if (cleanedFromId.matches("^\\d{10,15}$")) {
            phoneNumberToSearch = "+" + cleanedFromId;
        }

        System.out.println("DEBUG findUserByAnyIdentifier: fromId='" + fromId + "', cleanedFromId='" + cleanedFromId + "', phoneNumberToSearch='" + phoneNumberToSearch + "'");

        if (!phoneNumberToSearch.isEmpty() && STRICT_PHONE_NUMBER_PATTERN.matcher(phoneNumberToSearch).matches()) {
            System.out.println("DEBUG: Buscando usuario por campo 'phone': " + phoneNumberToSearch);
            user = findUserByPhoneNumberField(phoneNumberToSearch);
            System.out.println("DEBUG: Búsqueda por 'phone' completada, resultado: " + (user.isPresent() ? "ENCONTRADO" : "NO ENCONTRADO"));
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por campo 'phone': " + phoneNumberToSearch);
                return user;
            } else {
                System.out.println("DEBUG: Usuario NO encontrado por campo 'phone': " + phoneNumberToSearch);
            }
            
            // BÚSQUEDA ADICIONAL: Buscar también por ID de documento sin el '+'
            String docIdWithoutPlus = phoneNumberToSearch.startsWith("+") ? phoneNumberToSearch.substring(1) : phoneNumberToSearch;
            System.out.println("DEBUG: Búsqueda adicional por ID de documento (sin '+'): " + docIdWithoutPlus);
            user = findUserByDocumentId(docIdWithoutPlus);
            System.out.println("DEBUG: Búsqueda por ID de documento (sin '+') completada, resultado: " + (user.isPresent() ? "ENCONTRADO" : "NO ENCONTRADO"));
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por ID de documento (sin '+'): " + docIdWithoutPlus);
                return user;
            } else {
                System.out.println("DEBUG: Usuario NO encontrado por ID de documento (sin '+'): " + docIdWithoutPlus);
            }
        } else {
            System.out.println("DEBUG: FromId '" + fromId + "' normalizado a '" + phoneNumberToSearch
                    + "' no es un formato de teléfono válido para búsqueda por 'phone'.");
        }

        System.out.println("DEBUG: Continuando con búsqueda por ID de documento original...");

        if (!user.isPresent()) {
            System.out.println("DEBUG: Buscando usuario por ID de documento original: " + fromId);
            user = findUserByDocumentId(fromId);
            System.out.println("DEBUG: Búsqueda por ID de documento original completada, resultado: " + (user.isPresent() ? "ENCONTRADO" : "NO ENCONTRADO"));
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por ID de documento original: " + fromId);
                return user;
            } else {
                System.out.println("DEBUG: Usuario NO encontrado por ID de documento original: " + fromId);
            }
        }

        System.out.println("DEBUG: Continuando con búsqueda por Telegram Chat ID...");

        if (!user.isPresent() && "TELEGRAM".equalsIgnoreCase(channelType)) {
            System.out.println("DEBUG: Buscando usuario por campo 'telegram_chat_id': " + fromId);
            user = findUserByTelegramChatIdField(fromId);
            System.out.println("DEBUG: Búsqueda por 'telegram_chat_id' completada, resultado: " + (user.isPresent() ? "ENCONTRADO" : "NO ENCONTRADO"));
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por campo 'telegram_chat_id': " + fromId);
                return user;
            } else {
                System.out.println("DEBUG: Usuario NO encontrado por campo 'telegram_chat_id': " + fromId);
            }
        }

        System.out.println("DEBUG: Usuario no encontrado por ningún identificador conocido para fromId: " + fromId
                + " en canal: " + channelType);
        return Optional.empty();
    }


    private Optional<User> getUserByReferralCode(String referralCode) {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("users")
                    .whereEqualTo("referral_code", referralCode)
                    .limit(1)
                    .get();
            QuerySnapshot querySnapshot = future.get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.ofNullable(document.toObject(User.class));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("ERROR al buscar usuario por código de referido en Firestore: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Método unificado para guardar un objeto User en Firestore.
     * Determina el ID del documento basado en la existencia de un número de
     * teléfono.
     * Si 'user.phone' está presente, usa el número de teléfono (sin '+') como ID
     * del documento.
     * Si 'user.phone' no está presente, usa user.getId() (UUID) como ID del
     * documento.
     */
    public void saveUser(User user) {
        String docIdToUse;
        String oldDocId = null;

        if (user.getId() == null || user.getId().isEmpty()) {
            System.err.println(
                    "ERROR: Intentando guardar usuario, pero user.getId() es nulo/vacío. Generando un nuevo UUID y usando ese.");
            user.setId(UUID.randomUUID().toString());
        }

        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            docIdToUse = user.getPhone().startsWith("+") ? user.getPhone().substring(1) : user.getPhone();
            System.out.println("DEBUG: Guardando usuario con ID de documento (teléfono sin '+'): " + docIdToUse);

            if (!docIdToUse.equals(user.getId())) {
                oldDocId = user.getId();
                System.out.println("DEBUG: Detectada migración de ID de documento de UUID (" + oldDocId
                        + ") a teléfono (" + docIdToUse + ").");
            }
        } else {
            docIdToUse = user.getId();
            System.out.println("DEBUG: Guardando usuario con ID de documento (UUID): " + docIdToUse);
        }

        try {
            if (oldDocId != null) {
                firestore.collection("users").document(oldDocId).delete().get();
                System.out.println(
                        "DEBUG: Documento antiguo (UUID: " + oldDocId + ") eliminado exitosamente para migración.");
            }

            firestore.collection("users").document(docIdToUse).set(user).get();
            System.out.println("DEBUG: Usuario guardado/actualizado en Firestore con ID de documento: " + docIdToUse);
        } catch (Exception e) {
            System.err.println("ERROR al guardar/actualizar/migrar usuario en Firestore con ID " + docIdToUse
                    + " (antiguo ID: " + oldDocId + "): " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Extrae el nombre de la ciudad de un mensaje que puede contener texto adicional.
     * Maneja casos como "Perdon, es Barbosa", "Es Barbosa", "Barbosa", etc.
     */
    private String extractCityFromMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        
        String trimmedMessage = message.trim();
        
        // Patrones comunes para extraer la ciudad
        String[] patterns = {
            ".*\\b(?:es|soy de|vivo en|estoy en)\\s+([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$",  // "es Barbosa", "soy de Bogotá"
            ".*\\b(?:perdón|perdon|disculpa)\\s*,?\\s*(?:es|soy de|vivo en)\\s+([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$",  // "perdón, es Barbosa"
            "^([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$"  // Solo el nombre de la ciudad
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = regex.matcher(trimmedMessage);
            
            if (matcher.find()) {
                String extractedCity = matcher.group(1).trim();
                if (!extractedCity.isEmpty()) {
                    return extractedCity;
                }
            }
        }
        
        // Si no coincide con ningún patrón, devolver el mensaje original
        return trimmedMessage;
    }

    private String generateUniqueReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Extrae el nombre de un usuario de un mensaje que puede contener texto adicional.
     * Maneja casos como "Perdon, es Juan", "Es Juan", "Juan", etc.
     */
    private String extractNameFromCorrectionMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        
        String trimmedMessage = message.trim();
        
        // Patrones comunes para extraer el nombre
        String[] patterns = {
            ".*\\b(?:es|soy|me llamo)\\s+([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$",  // "es Juan", "soy Juan", "me llamo Juan"
            ".*\\b(?:perdón|perdon|disculpa)\\s*,?\\s*(?:es|soy|me llamo)\\s+([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$",  // "perdón, es Juan"
            "^([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$"  // Solo el nombre
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = regex.matcher(trimmedMessage);
            
            if (matcher.find()) {
                String extractedName = matcher.group(1).trim();
                if (!extractedName.isEmpty()) {
                    return extractedName;
                }
            }
        }
        
        // Si no coincide con ningún patrón, devolver el mensaje original
        return trimmedMessage;
    }

    /**
     * Extrae la ciudad de un usuario de un mensaje que puede contener texto adicional.
     * Maneja casos como "Perdon, es Medellín", "Es Medellín", "Medellín", etc.
     */
    private String extractCityFromCorrectionMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        
        String trimmedMessage = message.trim();
        
        // Patrones comunes para extraer la ciudad
        String[] patterns = {
            ".*\\b(?:es|soy de|vivo en|estoy en)\\s+([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$",  // "es Medellín", "soy de Bogotá"
            ".*\\b(?:perdón|perdon|disculpa)\\s*,?\\s*(?:es|soy de|vivo en)\\s+([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$",  // "perdón, es Medellín"
            "^([A-Za-zÁáÉéÍíÓóÚúÑñ\\s]+)$"  // Solo el nombre de la ciudad
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = regex.matcher(trimmedMessage);
            
            if (matcher.find()) {
                String extractedCity = matcher.group(1).trim();
                if (!extractedCity.isEmpty()) {
                    return extractedCity;
                }
            }
        }
        
        // Si no coincide con ningún patrón, devolver el mensaje original
        return trimmedMessage;
    }

    /**
     * Verifica si el mensaje del usuario es una solicitud de link de tribu.
     * Compara el mensaje normalizado con los patrones predefinidos.
     *
     * @param messageText El mensaje del usuario
     * @return true si el mensaje es una solicitud de link de tribu, false en caso contrario
     */
    private boolean isTribalLinkRequest(String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) {
            return false;
        }
        
        // Normalizar el mensaje: convertir a minúsculas y remover acentos
        String normalizedMessage = normalizeText(messageText.trim());
        
        // Verificar si coincide con alguno de los patrones
        for (String pattern : TRIBAL_LINK_PATTERNS) {
            if (normalizedMessage.contains(pattern)) {
                System.out.println("ChatbotService: Coincidencia encontrada con patrón: '" + pattern + "'");
                return true;
            }
        }
        
        return false;
    }

    /**
     * Verifica si un mensaje es una respuesta de botón del menú post-registro
     */
    private boolean isPostRegistrationMenuButton(String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) {
            return false;
        }
        
        String normalizedMessage = messageText.trim();
        
        // Botones del menú post-registro y submenú
        return normalizedMessage.equals("✅ ¿Cómo voy?") ||
               normalizedMessage.equals("📣 Compartir link") ||
               normalizedMessage.equals("🤖 Más opciones") ||
               normalizedMessage.equals("Habla con DQBot") ||
               normalizedMessage.equals("Habla con Voluntario") ||
               normalizedMessage.equals("↩️ Volver");
    }

    /**
     * Verifica si el mensaje del usuario es una solicitud de eliminación.
     * Compara el mensaje exactamente con los patrones predefinidos, sin distinguir mayúsculas o minúsculas.
     *
     * @param messageText El mensaje del usuario
     * @return DeleteRequestResult con información sobre el tipo de eliminación solicitada
     */
    private DeleteRequestResult isDeleteRequest(String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) {
            return new DeleteRequestResult(false, null);
        }
        
        // Normalizar el mensaje para comparación
        String normalizedMessage = messageText.trim().toLowerCase();
        
        // Verificar "eliminarme 2026"
        if (normalizedMessage.equals("eliminarme 2026")) {
            System.out.println("ChatbotService: Coincidencia exacta encontrada con patrón de eliminación personal: 'eliminarme 2026'");
            return new DeleteRequestResult(true, "PERSONAL");
        }
        
        // Verificar "eliminar mi tribu 2026"
        if (normalizedMessage.equals("eliminar mi tribu 2026")) {
            System.out.println("ChatbotService: Coincidencia exacta encontrada con patrón de eliminación de tribu: 'eliminar mi tribu 2026'");
            return new DeleteRequestResult(true, "TRIBU");
        }
        
        return new DeleteRequestResult(false, null);
    }

    /**
     * Clase interna para representar el resultado de la verificación de eliminación
     */
    private static class DeleteRequestResult {
        private final boolean isDeleteRequest;
        private final String deleteType; // "PERSONAL" o "TRIBU"

        public DeleteRequestResult(boolean isDeleteRequest, String deleteType) {
            this.isDeleteRequest = isDeleteRequest;
            this.deleteType = deleteType;
        }

        public boolean isDeleteRequest() {
            return isDeleteRequest;
        }

        public String getDeleteType() {
            return deleteType;
        }
    }

    /**
     * Normaliza el texto removiendo acentos y convirtiendo a minúsculas.
     *
     * @param text El texto a normalizar
     * @return El texto normalizado
     */
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        
        // Convertir a minúsculas
        String normalized = text.toLowerCase();
        
        // Remover acentos (mapping básico de caracteres acentuados)
        normalized = normalized.replace("á", "a")
                              .replace("é", "e")
                              .replace("í", "i")
                              .replace("ó", "o")
                              .replace("ú", "u")
                              .replace("ñ", "n")
                              .replace("ü", "u");
        
        return normalized;
    }

    /**
     * Envía un mensaje de WhatsApp de forma síncrona para garantizar el orden de los mensajes.
     * Este método bloquea hasta que el mensaje se envía completamente.
     *
     * @param toPhoneNumber El número de teléfono del destinatario
     * @param messageText El texto del mensaje a enviar
     */
    private void sendWhatsAppMessageSync(String toPhoneNumber, String messageText) {
        try {
            // Usar el método síncrono del WatiApiService
            watiApiService.sendWhatsAppMessageSync(toPhoneNumber, messageText);
            
            // Agregar un pequeño delay para asegurar que los mensajes se procesen en orden
            // Esto es necesario porque Wati puede procesar mensajes muy rápidamente
            Thread.sleep(500); // 500ms de delay entre mensajes
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ERROR: Interrupción durante el envío síncrono de mensaje WhatsApp: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERROR: Error al enviar mensaje WhatsApp de forma síncrona: " + e.getMessage());
        }
    }

    /**
     * Resetea todos los usuarios que fueron referidos por un código de referido específico.
     * Llama al endpoint de reset para cada usuario referido encontrado.
     *
     * @param referralCode El código de referido del usuario principal
     * @return El número de usuarios referidos que fueron reseteados exitosamente
     */
    private int resetReferredUsers(String referralCode) {
        if (referralCode == null || referralCode.isEmpty()) {
            System.out.println("ChatbotService: No hay código de referido para resetear usuarios referidos");
            return 0;
        }

        try {
            // Buscar todos los usuarios que tienen este referralCode en referred_by_code
            ApiFuture<QuerySnapshot> future = firestore.collection("users")
                    .whereEqualTo("referred_by_code", referralCode)
                    .get();
            
            QuerySnapshot querySnapshot = future.get();
            int resetCount = 0;

            if (!querySnapshot.isEmpty()) {
                System.out.println("ChatbotService: Encontrados " + querySnapshot.size() + " usuarios referidos para resetear");
                
                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                    try {
                        User referredUser = document.toObject(User.class);
                        if (referredUser != null && referredUser.getPhone() != null) {
                            // Construir URL para resetear este usuario referido
                            String baseUrl = "http://localhost:" + serverPort;
                            if (contextPath != null && !contextPath.isEmpty()) {
                                baseUrl += contextPath;
                            }
                            String resetUrl = baseUrl + "/api/admin/reset/" + referredUser.getPhone().substring(1);
                            
                            System.out.println("ChatbotService: Reseteando usuario referido: " + referredUser.getPhone());
                            
                            // Llamar al endpoint de reset para este usuario referido
                            ResponseEntity<Map> resetResponse = restTemplate.exchange(resetUrl, org.springframework.http.HttpMethod.DELETE, null, Map.class);
                            
                            if (resetResponse.getStatusCode() == HttpStatus.OK) {
                                resetCount++;
                                System.out.println("ChatbotService: Usuario referido reseteado exitosamente: " + referredUser.getPhone());
                            } else {
                                System.err.println("ChatbotService: Error al resetear usuario referido: " + referredUser.getPhone());
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR: Error al resetear usuario referido: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("ChatbotService: No se encontraron usuarios referidos para el código: " + referralCode);
            }

            return resetCount;
            
        } catch (Exception e) {
            System.err.println("ERROR: Error al buscar usuarios referidos: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Cuenta cuántos usuarios han sido referidos por un usuario específico
     *
     * @param referralCode El código de referido del usuario
     * @return El número de usuarios referidos
     */
    private int countUserReferrals(String referralCode) {
        if (referralCode == null || referralCode.isEmpty()) {
            return 0;
        }

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("users")
                    .whereEqualTo("referred_by_code", referralCode)
                    .get();
            
            QuerySnapshot querySnapshot = future.get();
            return querySnapshot.size();
            
        } catch (Exception e) {
            System.err.println("ERROR: Error al contar referidos para código " + referralCode + ": " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Notifica al referente cuando alguien se registra usando su código
     *
     * @param referrerPhone El teléfono del referente (con formato +57XXXXXXXXX)
     * @param newUserFirstName El nombre del nuevo usuario registrado
     * @param referralCode El código de referido usado
     */
    private void notifyReferrer(String referrerPhone, String newUserFirstName, String referralCode) {
        try {
            // Contar total de referidos del referente
            int totalReferrals = countUserReferrals(referralCode);
            
            // Enviar notificación
            notificationService.sendReferralNotification(referrerPhone, newUserFirstName, totalReferrals);
            
            System.out.println("ChatbotService: Notificación de referido enviada a " + referrerPhone + 
                             " para nuevo usuario " + newUserFirstName + 
                             " (total referidos: " + totalReferrals + ")");
            
        } catch (Exception e) {
            System.err.println("ERROR: Error al notificar referente " + referrerPhone + ": " + e.getMessage());
        }
    }
}