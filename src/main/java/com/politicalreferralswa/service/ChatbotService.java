package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.politicalreferralswa.model.User; // Asegúrate de que User.java tiene campos: id (String UUID), phone (String), telegram_chat_id (String), Y AHORA referred_by_code (String)
import com.politicalreferralswa.service.UserDataExtractionResult;
import com.politicalreferralswa.service.GeminiService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.api.core.ApiFuture;

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

    @Value("${WELCOME_VIDEO_URL}")
    private String welcomeVideoUrl;

    private static final Pattern REFERRAL_MESSAGE_PATTERN = Pattern
            .compile("Hola, vengo referido por:\\s*([A-Za-z0-9]{8})");
    private static final String TELEGRAM_BOT_USERNAME = "ResetPoliticaBot";
    private static final Pattern STRICT_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+\\d{10,15}$");

    // Nuevos mensajes de la campaña
    private static final String WELCOME_MESSAGE_BASE = "Hola. Te doy la bienvenida a nuestra campaña: Daniel Quintero Presidente!!!";
    
    private static final String PRIVACY_MESSAGE = """
        Respetamos la ley y cuidamos tu información, vamos a mantenerla de forma confidencial, esta es nuestra política de seguridad https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo con ella.""";

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
        "ayúdame con el link de la tribu",
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
        // Patrones adicionales para casos específicos
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

    public ChatbotService(Firestore firestore, WatiApiService watiApiService,
                          TelegramApiService telegramApiService, AIBotService aiBotService,
                          UserDataExtractor userDataExtractor, GeminiService geminiService,
                          NameValidationService nameValidationService,
                          TribalAnalysisService tribalAnalysisService, AnalyticsService analyticsService,
                          SystemConfigService systemConfigService) {
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
    }

    /**
     * MÉTODO DE UTILIDAD PARA CREAR UN USUARIO REFERENTE DE PRUEBA
     */
    public void createTestReferrerUser() {
        String testPhoneNumber = "+573100000001";
        String testReferralCode = "TESTCODE";

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
        testUser.setReferred_by_code(null); // Asegúrate de inicializarlo si no lo haces en el constructor de User

        try {
            saveUser(testUser);
            System.out.println("DEBUG: Usuario referente de prueba '" + testUser.getName() + "' con código '"
                    + testUser.getReferral_code() + "' creado exitosamente en Firestore.");
        } catch (Exception e) {
            System.err.println("ERROR DEBUG: No se pudo crear el usuario de prueba en Firestore: " + e.getMessage());
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
                    
                    // Intentar recuperar el estado basado en los datos disponibles
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
                if (primaryMessage.startsWith("MULTI:")) {
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
                            // Enviar de forma síncrona para garantizar el orden
                            sendWhatsAppMessageSync(fromId, msg);
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
            if (returnMessage.startsWith("MULTI:")) {
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
                
                // Preparar múltiples mensajes
                String finalMessage = extractionResult.getMessage() + "\n\n" + PRIVACY_MESSAGE;
                return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + finalMessage, "WAITING_TERMS_ACCEPTANCE");
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
                    return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + finalMessage, "WAITING_NAME");
                } else {
                    // Si no hay código de referido, usar el mensaje de extracción de la IA
                    System.out.println("DEBUG handleNewUserIntro: Usando extracción inteligente - Parcial, sin política de privacidad");
                    
                    // Preparar múltiples mensajes para extracción parcial
                    System.out.println("⚠️  WARNING: Generando mensaje de bienvenida en extracción inteligente parcial");
                    return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + extractionResult.getMessage(), extractionResult.getNextState());
                }
            }
        }
        
        System.out.println("DEBUG handleNewUserIntro: Extracción inteligente falló, usando método tradicional");

        // Si la extracción falló, usar el método tradicional
        Matcher matcher = REFERRAL_MESSAGE_PATTERN.matcher(messageText.trim());

        System.out.println(
                "DEBUG handleNewUserIntro: Resultado de la coincidencia del patrón Regex: " + matcher.matches());

        if (matcher.matches()) {
            String incomingReferralCode = matcher.group(1);
            System.out.println("DEBUG handleNewUserIntro: Código de referido extraído: '" + incomingReferralCode + "'");

            System.out.println(
                    "ChatbotService: Primer mensaje contiene posible código de referido: " + incomingReferralCode);
            Optional<User> referrerUser = getUserByReferralCode(incomingReferralCode);

            if (referrerUser.isPresent()) {
                // MODIFICACIÓN CLAVE AQUÍ: Guardar el código de referido también
                // Guardar el número del referente sin el símbolo "+"
                String referrerPhone = referrerUser.get().getPhone();
                if (referrerPhone != null && referrerPhone.startsWith("+")) {
                    referrerPhone = referrerPhone.substring(1);
                }
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
                
                // Preparar múltiples mensajes para usuario con referido
                String finalMessage;
                if (user.getName() != null && !user.getName().trim().isEmpty()) {
                    finalMessage = personalizedGreeting + "¿Me confirmas si tu nombre es el que aparece en WhatsApp " + user.getName().trim() + " o me dices cómo te llamas para guardarte en mis contactos?";
                } else {
                    finalMessage = personalizedGreeting + "¿Me confirmas tu nombre para guardarte en mis contactos?";
                }
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida con código de referido válido");
                return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + finalMessage, "WAITING_NAME");
            } else {
                System.out.println(
                        "ChatbotService: Código de referido válido en formato, pero NO ENCONTRADO en el primer mensaje: "
                                + incomingReferralCode);
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida con código de referido inválido");
                // Preparar múltiples mensajes para código de referido inválido
                String finalMessage;
                if (user.getName() != null && !user.getName().trim().isEmpty()) {
                    finalMessage = "Parece que el código de referido que me enviaste no es válido, pero no te preocupes, ¡podemos continuar!\n\n" +
                        "¿Me confirmas si tu nombre es el que aparece en WhatsApp " + user.getName().trim() + " o me dices cómo te llamas para guardarte en mis contactos?";
                } else {
                    finalMessage = "Parece que el código de referido que me enviaste no es válido, pero no te preocupes, ¡podemos continuar!\n\n" +
                        "¿Me confirmas tu nombre para guardarte en mis contactos?";
                }
                return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + finalMessage, "WAITING_NAME");
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
                    // Preparar múltiples mensajes para usuario sin apellido
                    String finalMessage = String.format("Veo que te llamas %s. ¿Cuál es tu apellido?", user.getName());
                    return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + finalMessage, "WAITING_LASTNAME");
                } else {
                    // Si ya tiene nombre y apellido, preguntar ciudad
                    // Preparar múltiples mensajes para usuario con nombre y apellido
                    String finalMessage = String.format("Veo que te llamas %s. ¿En qué ciudad vives?", fullName);
                    return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + finalMessage, "WAITING_CITY");
                }
            } else {
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida general (sin código de referido)");
                // Preparar múltiples mensajes para usuario general
                String finalMessage;
                if (user.getName() != null && !user.getName().trim().isEmpty()) {
                    finalMessage = "¿Me confirmas si tu nombre es el que aparece en WhatsApp " + user.getName().trim() + " o me dices cómo te llamas para guardarte en mis contactos?";
                } else {
                    finalMessage = "¿Me confirmas tu nombre para guardarte en mis contactos?";
                }
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida general (sin código de referido)");
                return new ChatResponse("MULTI:" + WELCOME_MESSAGE_BASE + "|" + finalMessage, "WAITING_NAME");
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

                responseMessage = """
                        ¡Gracias! Hemos registrado tu número de teléfono.
                        Respetamos la ley y cuidamos tu información, vamos a mantenerla de forma confidencial, esta es nuestra política de seguridad https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo con ella.

                        Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?

                        Responde: Sí o No.
                        """;
                nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                break;

            case "WAITING_TERMS_ACCEPTANCE":
                // Usar extracción inteligente para detectar respuestas afirmativas
                System.out.println("DEBUG: Analizando respuesta de términos con IA: '" + messageText + "'");
                UserDataExtractor.ExtractionResult termsExtractionResult = userDataExtractor.extractAndUpdateUser(user, messageText, "WAITING_TERMS_ACCEPTANCE");
                
                // Verificar si la extracción fue exitosa y detectó aceptación de términos
                if (termsExtractionResult.isSuccess()) {
                    // Si la extracción fue exitosa, verificar si aceptó términos
                    // El UserDataExtractor ya actualiza el usuario si detecta aceptación
                    if (user.isAceptaTerminos()) {
                        System.out.println("DEBUG: ✅ Usuario aceptó términos de privacidad (detectado por IA). Validando datos completos...");
                    
                        // Verificar si ya tiene todos los datos necesarios
                        boolean hasName = user.getName() != null && !user.getName().isEmpty();
                        boolean hasCity = user.getCity() != null && !user.getCity().isEmpty();
                        
                        System.out.println("DEBUG: Usuario tiene nombre: " + hasName + " (nombre: " + user.getName() + ")");
                        System.out.println("DEBUG: Usuario tiene ciudad: " + hasCity + " (ciudad: " + user.getCity() + ")");
                        
                        if (hasName && hasCity) {
                            System.out.println("DEBUG: ✅ Usuario tiene todos los datos. Completando registro...");
                            // Si ya tiene nombre y ciudad, completar el registro
                            String referralCode = generateUniqueReferralCode();
                            user.setReferral_code(referralCode);

                            String whatsappInviteLink;
                            String telegramInviteLink;
                            List<String> additionalMessages = new ArrayList<>();

                            try {
                                String whatsappRawReferralText = String.format("Hola, vengo referido por:%s", referralCode);
                                String encodedWhatsappMessage = URLEncoder
                                        .encode(whatsappRawReferralText, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                                whatsappInviteLink = "https://wa.me/573224029924?text=" + encodedWhatsappMessage;

                                String encodedTelegramPayload = URLEncoder.encode(referralCode,
                                        StandardCharsets.UTF_8.toString());
                                telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start="
                                        + encodedTelegramPayload;

                                String friendsInviteMessage = String.format(
                                        "Amigos, soy %s y quiero invitarte a unirse a la campaña de Daniel Quintero a la Presidencia: https://wa.me/573224029924?text=%s",
                                        user.getName(),
                                        URLEncoder.encode(String.format("Hola, vengo referido por:%s", referralCode),
                                                StandardCharsets.UTF_8.toString()).replace("+", "%20"));
                                additionalMessages.add(friendsInviteMessage);

                                // Enviar el video de bienvenida ANTES del mensaje de IA
                                try { 
                                    // Enviar el video real usando la URL configurada
                                    watiApiService.sendVideoMessage(user.getPhone(), welcomeVideoUrl, "Video de bienvenida a la campaña");
                                    System.out.println("DEBUG: Video de bienvenida enviado a: " + user.getPhone());
                                } catch (Exception e) {
                                    System.err.println("ERROR: No se pudo enviar el video de bienvenida: " + e.getMessage());
                                    // Fallback: enviar el enlace si falla el envío del video
                                    String welcomeVideoMessage = "🎥 Video de bienvenida: " + welcomeVideoUrl;
                                    additionalMessages.add(welcomeVideoMessage);
                                }

                                String aiBotIntroMessage = """
                                        ¡Atención! Ahora entrarás en conversación con una inteligencia artificial.
                                        Soy una IA de prueba para este proyecto.
                                        Mi objetivo es simular mis respuestas basadas en información clave y mi visión política.
                                        Ten en cuenta que aún estoy en etapa de prueba y mejora continua.
                                        ¡Hazme tu pregunta!
                                        """;
                                additionalMessages.add(aiBotIntroMessage);

                            } catch (UnsupportedEncodingException e) {
                                System.err.println("ERROR: No se pudo codificar los códigos de referido. Causa: " + e.getMessage());
                                e.printStackTrace();
                                whatsappInviteLink = "https://wa.me/573224029924?text=Error%20al%20generar%20referido";
                                telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=Error";
                                additionalMessages.clear();
                                additionalMessages.add("Error al generar los mensajes de invitación.");
                            }

                            responseMessage = String.format(
                                    """
                                            %s, gracias por unirte a la ola de cambio que estamos construyendo para Colombia. Hasta ahora tienes 0 personas referidas. Ayudanos a crecer y gana puestos dentro de la campaña.

                                            Sabemos que muchos comparten la misma visión de un futuro mejor, y por eso quiero invitarte a que compartas este proyecto con tus amigos, familiares y conocidos. Juntos podemos lograr una transformación real y profunda.

                                            Envíales el siguiente enlace de referido:
                                            """,
                                    user.getName()
                            );

                            Optional<String> termsSecondaryMessage = Optional.of(String.join("###SPLIT###", additionalMessages));
                            nextChatbotState = "COMPLETED";
                            return new ChatResponse(responseMessage, nextChatbotState, termsSecondaryMessage);
                        } else {
                            // Si no tiene todos los datos, continuar con el flujo normal
                            System.out.println("DEBUG: ⚠️ Usuario no tiene todos los datos. Continuando flujo...");
                            responseMessage = "¿Cuál es tu nombre?";
                            nextChatbotState = "WAITING_NAME";
                        }
                    } else {
                        System.out.println("DEBUG: ❌ Usuario no aceptó términos (detectado por IA). Pidiendo confirmación...");
                        responseMessage = "Respetamos la ley y cuidamos tu información, vamos a mantenerla de forma confidencial, esta es nuestra política de seguridad https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo con ella.\n\nAcompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?";
                        nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                    }
                } else {
                    System.out.println("DEBUG: ❌ Extracción de términos falló. Pidiendo confirmación...");
                    responseMessage = "Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.\n\nAcompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?";
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                }
                break;
            case "WAITING_NAME":
                // En WAITING_NAME usamos IA para detectar si es confirmación o nuevo nombre
                System.out.println("DEBUG: Procesando confirmación de nombre en estado WAITING_NAME con IA");
                
                try {
                    // Usar IA para detectar si es confirmación o nuevo nombre
                    UserDataExtractionResult extraction = geminiService.extractUserData(messageText, null, "WAITING_NAME");
                    
                    if (extraction.isSuccessful() && extraction.getIsConfirmation() != null) {
                        if (extraction.getIsConfirmation()) {
                            // Usuario confirma el nombre existente
                            System.out.println("DEBUG: IA detectó confirmación del nombre existente: " + user.getName());
                            responseMessage = "¿Cuál es tu apellido?";
                            nextChatbotState = "WAITING_LASTNAME";
                        } else {
                            // Usuario proporciona un nombre diferente
                            String newName = extraction.getName() != null ? extraction.getName() : messageText.trim();
                            user.setName(newName);
                            System.out.println("DEBUG: IA detectó nuevo nombre: " + newName);
                            responseMessage = "¿Cuál es tu apellido?";
                            nextChatbotState = "WAITING_LASTNAME";
                        }
                    } else {
                        // Fallback: usar lógica tradicional si la IA falla
                        System.out.println("DEBUG: IA falló, usando lógica tradicional");
                        String lowerNameMessage = messageText.toLowerCase().trim();
                        
                        if (lowerNameMessage.equals("si") || lowerNameMessage.equals("sí") || 
                            lowerNameMessage.equals("correcto") || lowerNameMessage.equals("es correcto") ||
                            lowerNameMessage.contains("si es") || lowerNameMessage.contains("sí es") ||
                            lowerNameMessage.contains("si,") || lowerNameMessage.contains("sí,") ||
                            lowerNameMessage.contains(", es correcto") || lowerNameMessage.contains(",es correcto")) {
                            
                            // Usuario confirma el nombre existente
                            System.out.println("DEBUG: Usuario confirmó el nombre existente: " + user.getName());
                            responseMessage = "¿Cuál es tu apellido?";
                            nextChatbotState = "WAITING_LASTNAME";
                        } else {
                            // Usuario proporciona un nombre diferente
                            user.setName(messageText.trim());
                            System.out.println("DEBUG: Usuario proporcionó nuevo nombre: " + messageText.trim());
                            responseMessage = "¿Cuál es tu apellido?";
                            nextChatbotState = "WAITING_LASTNAME";
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ERROR: Fallo en extracción IA para WAITING_NAME: " + e.getMessage());
                    // Fallback: usar lógica tradicional
                    String lowerNameMessage = messageText.toLowerCase().trim();
                    
                    if (lowerNameMessage.equals("si") || lowerNameMessage.equals("sí") || 
                        lowerNameMessage.equals("correcto") || lowerNameMessage.equals("es correcto") ||
                        lowerNameMessage.contains("si es") || lowerNameMessage.contains("sí es") ||
                        lowerNameMessage.contains("si,") || lowerNameMessage.contains("sí,") ||
                        lowerNameMessage.contains(", es correcto") || lowerNameMessage.contains(",es correcto")) {
                        
                        // Usuario confirma el nombre existente
                        System.out.println("DEBUG: Usuario confirmó el nombre existente: " + user.getName());
                        responseMessage = "¿Cuál es tu apellido?";
                        nextChatbotState = "WAITING_LASTNAME";
                    } else {
                        // Usuario proporciona un nombre diferente
                        user.setName(messageText.trim());
                        System.out.println("DEBUG: Usuario proporcionó nuevo nombre: " + messageText.trim());
                        responseMessage = "¿Cuál es tu apellido?";
                        nextChatbotState = "WAITING_LASTNAME";
                    }
                }
                break;
            case "WAITING_LASTNAME":
                // En WAITING_LASTNAME solo procesamos apellido
                System.out.println("DEBUG: Procesando apellido en estado WAITING_LASTNAME");
                
                if (messageText != null && !messageText.trim().isEmpty()) {
                    user.setLastname(messageText.trim());
                    System.out.println("DEBUG: Apellido establecido: " + messageText.trim());
                    responseMessage = "¿En qué ciudad vives?";
                    nextChatbotState = "WAITING_CITY";
                } else {
                    responseMessage = "Por favor, ingresa un apellido válido.";
                    nextChatbotState = "WAITING_LASTNAME";
                }
                break;
            case "WAITING_CITY":
                // En WAITING_CITY solo procesamos ciudad, NO departamento completo
                System.out.println("DEBUG: Procesando ciudad en estado WAITING_CITY");
                
                if (messageText != null && !messageText.trim().isEmpty()) {
                    user.setCity(messageText.trim());
                    System.out.println("DEBUG: Ciudad establecida: " + messageText.trim());
                    
                    // Construir nombre completo para mostrar
                    String fullName = user.getName();
                    if (user.getLastname() != null && !user.getLastname().trim().isEmpty()) {
                        fullName += " " + user.getLastname();
                    }
                    
                    // Enviar mensaje completo de la política de privacidad
                    responseMessage = "Perfecto " + fullName + ". Ahora necesito que aceptes nuestra política de privacidad para continuar.\n\n" +
                        "Respetamos la ley y cuidamos tu información, vamos a mantenerla de forma confidencial, esta es nuestra política de seguridad https://danielquintero.com/privacidad. Si continuas esta conversación estás de acuerdo con ella.\n\n" +
                        "Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política? (Sí/No)";
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                } else {
                    responseMessage = "Por favor, ingresa una ciudad válida.";
                    nextChatbotState = "WAITING_CITY";
                }
                break;
            case "CONFIRM_DATA":
                if (messageText.equalsIgnoreCase("Sí") || messageText.equalsIgnoreCase("Si")) {
                    // Verificar si ya aceptó los términos
                    if (!user.isAceptaTerminos()) {
                        // Si no aceptó términos, pedirle que los acepte
                        responseMessage = "Respetamos la ley y cuidamos tu información, vamos a mantenerla de forma confidencial, esta es nuestra política de seguridad https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo con ella.\n\nAcompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?";
                        nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                        return new ChatResponse(responseMessage, nextChatbotState);
                    }
                    
                    // Si ya aceptó términos, completar el registro
                    String referralCode = generateUniqueReferralCode();
                    user.setReferral_code(referralCode);

                    String whatsappInviteLink;
                    String telegramInviteLink;

                    List<String> additionalMessages = new ArrayList<>();

                    try {
                        String whatsappRawReferralText = String.format("Hola, vengo referido por:%s", referralCode);
                        System.out.println("Texto crudo antes de codificar: '" + whatsappRawReferralText + "'");
                        String encodedWhatsappMessage = URLEncoder
                                .encode(whatsappRawReferralText, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                        whatsappInviteLink = "https://wa.me/573224029924?text=" + encodedWhatsappMessage;

                        String encodedTelegramPayload = URLEncoder.encode(referralCode,
                                StandardCharsets.UTF_8.toString());
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start="
                                + encodedTelegramPayload;

                        String friendsInviteMessage = String.format(
                                "Amigos, soy %s y quiero invitarte a unirse a la campaña de Daniel Quintero a la Presidencia: https://wa.me/573224029924?text=%s",
                                user.getName(),
                                URLEncoder.encode(String.format("Hola, vengo referido por:%s", referralCode),
                                        StandardCharsets.UTF_8.toString()).replace("+", "%20"));
                        additionalMessages.add(friendsInviteMessage);

                        // Enviar el video de bienvenida ANTES del mensaje de IA
                        try {
                            // Enviar el video real usando la URL configurada
                            watiApiService.sendVideoMessage(user.getPhone(), welcomeVideoUrl, "Video de bienvenida a la campaña");
                            System.out.println("DEBUG: Video de bienvenida enviado a: " + user.getPhone());
                        } catch (Exception e) {
                            System.err.println("ERROR: No se pudo enviar el video de bienvenida: " + e.getMessage());
                            // Fallback: enviar el enlace si falla el envío del video
                            String welcomeVideoMessage = "🎥 Video de bienvenida: " + welcomeVideoUrl;
                            additionalMessages.add(welcomeVideoMessage);
                        }

                        String aiBotIntroMessage = """
                                ¡Atención! Ahora entrarás en conversación con una inteligencia artificial.
                                Soy una IA de prueba para este proyecto.
                                Mi objetivo es simular mis respuestas basadas en información clave y mi visión política.
                                Ten en cuenta que aún estoy en etapa de prueba y mejora continua.
                                ¡Hazme tu pregunta!
                                """;
                        additionalMessages.add(aiBotIntroMessage);

                    } catch (UnsupportedEncodingException e) {
                        System.err.println(
                                "ERROR: No se pudo codificar los códigos de referido. Causa: " + e.getMessage());
                        e.printStackTrace();
                        whatsappInviteLink = "https://wa.me/573224029924?text=Error%20al%20generar%20referido";
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=Error";
                        additionalMessages.clear();
                        additionalMessages.add("Error al generar los mensajes de invitación.");
                    }

                    responseMessage = String.format(
                            """
                                    %s, gracias por unirte a la ola de cambio que estamos construyendo para Colombia. Hasta ahora tienes 0 personas referidas. Ayudanos a crecer y gana puestos dentro de la campaña.

                                    Sabemos que muchos comparten la misma visión de un futuro mejor, y por eso quiero invitarte a que compartas este proyecto con tus amigos, familiares y conocidos. Juntos podemos lograr una transformación real y profunda.

                                    Envíales el siguiente enlace de referido:
                                    """,
                            user.getName()
                    );

                    secondaryMessage = Optional.of(String.join("###SPLIT###", additionalMessages));

                    nextChatbotState = "COMPLETED";
                } else {
                    // Usuario dijo "No" - necesita corregir datos
                    System.out.println("DEBUG: Usuario necesita corregir datos. Mensaje: '" + messageText + "'");
                    
                    // Intentar extraer información del mensaje para identificar qué corregir
                    String lowerMessage = messageText.toLowerCase();
                    
                    if (lowerMessage.contains("ciudad") || lowerMessage.contains("vivo") || lowerMessage.contains("soy de") || 
                        lowerMessage.contains("bogota") || lowerMessage.contains("medellin") || lowerMessage.contains("cali") ||
                        lowerMessage.contains("barranquilla") || lowerMessage.contains("cartagena") || lowerMessage.contains("pereira") ||
                        lowerMessage.contains("manizales") || lowerMessage.contains("bucaramanga") || lowerMessage.contains("villavicencio") ||
                        lowerMessage.contains("ibague") || lowerMessage.contains("past") || lowerMessage.contains("neiva") ||
                        lowerMessage.contains("monteria") || lowerMessage.contains("valledupar") || lowerMessage.contains("popayan") ||
                        lowerMessage.contains("tunja") || lowerMessage.contains("florencia") || lowerMessage.contains("yopal") ||
                        lowerMessage.contains("mocoa") || lowerMessage.contains("leticia") || lowerMessage.contains("mit") ||
                        lowerMessage.contains("quibdo") || lowerMessage.contains("arauca") || lowerMessage.contains("inirida") ||
                        lowerMessage.contains("puerto carreno") || lowerMessage.contains("san andres") || lowerMessage.contains("providencia")) {
                        
                        System.out.println("DEBUG: Usuario quiere corregir la ciudad");
                        
                        // Intentar extraer la nueva ciudad del mensaje
                        String newCity = extractCityFromCorrectionMessage(messageText);
                        if (newCity != null && !newCity.isEmpty()) {
                            System.out.println("DEBUG: Ciudad extraída automáticamente: '" + newCity + "'");
                            user.setCity(newCity);
                            responseMessage = "Confirmamos tus datos: " + user.getName() + ", de " + user.getCity()
                                    + ". ¿Es correcto? (Sí/No)";
                            nextChatbotState = "CONFIRM_DATA";
                        } else {
                            responseMessage = "¿En qué ciudad vives?";
                            nextChatbotState = "WAITING_CITY";
                        }
                    } else if (lowerMessage.contains("nombre") || lowerMessage.contains("me llamo") || lowerMessage.contains("soy")) {
                        System.out.println("DEBUG: Usuario quiere corregir el nombre");
                        
                        // Intentar extraer el nuevo nombre del mensaje
                        String newName = extractNameFromCorrectionMessage(messageText);
                        if (newName != null && !newName.isEmpty()) {
                            System.out.println("DEBUG: Nombre extraído automáticamente: '" + newName + "'");
                            user.setName(newName);
                            responseMessage = "Confirmamos tus datos: " + user.getName() + ", de " + user.getCity()
                                    + ". ¿Es correcto? (Sí/No)";
                            nextChatbotState = "CONFIRM_DATA";
                        } else {
                            responseMessage = "¿Cuál es tu nombre?";
                            nextChatbotState = "WAITING_NAME";
                        }
                    } else {
                        // Si no se puede identificar específicamente, preguntar qué quiere corregir
                        System.out.println("DEBUG: No se pudo identificar qué corregir, preguntando al usuario");
                        responseMessage = "¿Qué dato quieres corregir? Escribe 'nombre' o 'ciudad'.";
                        nextChatbotState = "WAITING_CORRECTION_TYPE";
                    }
                }
                break;
            case "WAITING_CORRECTION_TYPE":
                String correctionType = messageText.toLowerCase().trim();
                System.out.println("DEBUG: Usuario especificó tipo de corrección: '" + correctionType + "'");
                
                if (correctionType.contains("nombre") || correctionType.equals("n")) {
                    System.out.println("DEBUG: Usuario quiere corregir el nombre");
                    responseMessage = "¿Cuál es tu nombre?";
                    nextChatbotState = "WAITING_NAME";
                } else if (correctionType.contains("ciudad") || correctionType.equals("c")) {
                    System.out.println("DEBUG: Usuario quiere corregir la ciudad");
                    responseMessage = "¿En qué ciudad vives?";
                    nextChatbotState = "WAITING_CITY";
                } else {
                    System.out.println("DEBUG: Tipo de corrección no reconocido: '" + correctionType + "'");
                    responseMessage = "Por favor, escribe 'nombre' o 'ciudad' para especificar qué quieres corregir.";
                    nextChatbotState = "WAITING_CORRECTION_TYPE";
                }
                break;
            case "COMPLETED":
                System.out.println("ChatbotService: Usuario COMPLETED. Verificando configuración del sistema...");
                
                // Verificar si la IA está habilitada globalmente en el sistema
                if (!systemConfigService.isAIEnabled()) {
                    System.out.println("ChatbotService: IA del sistema DESHABILITADA. Redirigiendo a agente humano...");
                    
                    // No enviar mensaje automático, el agente responderá directamente
                    nextChatbotState = "COMPLETED";
                    
                    // Aquí podrías implementar la lógica para enviar el mensaje a WATI para que lo vean los agentes humanos
                    // Retornar sin mensaje para que el agente responda
                    return new ChatResponse("", nextChatbotState, secondaryMessage);
                }
                
                System.out.println("ChatbotService: IA del sistema HABILITADA. Procesando con IA...");

                // Obtener session ID para el análisis
                String sessionId = user.getPhone();
                if ((sessionId == null || sessionId.isEmpty()) && user.getTelegram_chat_id() != null) {
                    System.err.println("ADVERTENCIA: Usuario COMPLETED sin teléfono. Usando Telegram Chat ID ("
                            + user.getTelegram_chat_id() + ") como fallback para la sesión de IA. Doc ID: "
                            + user.getId());
                    sessionId = user.getTelegram_chat_id();
                }

                if (sessionId != null && !sessionId.isEmpty()) {
                    // Primero verificar si es una pregunta de analytics
                    if (analyticsService.isAnalyticsQuestion(messageText)) {
                        System.out.println("ChatbotService: Detectada pregunta de analytics. Obteniendo métricas...");
                        
                        // Obtener métricas del usuario
                        Optional<AnalyticsService.AnalyticsData> analyticsData = 
                            analyticsService.getUserStats(sessionId, sessionId);
                        
                        if (analyticsData.isPresent()) {
                            // Enviar datos de analytics al chatbot IA para generar respuesta
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", user.getName());
                            userData.put("referral_code", user.getReferral_code());
                            userData.put("city", user.getCity());
                            userData.put("phone", user.getPhone());
                            userData.put("analytics_data", analyticsData.get());
                            
                            // Usar el chatbot IA con datos de analytics
                            responseMessage = aiBotService.getAIResponseWithAnalytics(sessionId, messageText, userData);
                            nextChatbotState = "COMPLETED";
                            System.out.println("ChatbotService: Respuesta de analytics enviada");
                        } else {
                            // Fallback si no se pueden obtener las métricas
                            responseMessage = "¡Hola! Veo que quieres saber sobre tu rendimiento. En este momento no puedo acceder a tus métricas, pero te puedo ayudar con otras preguntas sobre la campaña. ¿En qué más puedo ayudarte?";
                            nextChatbotState = "COMPLETED";
                        }
                    } else {
                        // No es pregunta de analytics, continuar con el flujo normal
                        // Preparar datos del usuario para el análisis
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("name", user.getName());
                        userData.put("referral_code", user.getReferral_code());
                        userData.put("city", user.getCity());
                        userData.put("phone", user.getPhone());
                        
                        // Analizar la consulta con el servicio de IA
                        Optional<TribalAnalysisService.TribalAnalysisResult> analysisResult = 
                            tribalAnalysisService.analyzeTribalRequest(messageText, sessionId, userData);
                        
                        if (analysisResult.isPresent()) {
                            TribalAnalysisService.TribalAnalysisResult result = analysisResult.get();
                            
                            if (result.isTribalRequest()) {
                                System.out.println("ChatbotService: IA detectó solicitud de tribu. Generando link...");
                                
                                // Generar el link de referido para el usuario
                                String referralCode = user.getReferral_code();
                                if (referralCode == null || referralCode.isEmpty()) {
                                    referralCode = generateUniqueReferralCode();
                                    user.setReferral_code(referralCode);
                                    saveUser(user);
                                }
                                
                                try {
                                    String tribalLinkMessage = String.format(
                                        "Amigos, soy %s y quiero invitarte a unirse a la campaña de Daniel Quintero a la Presidencia: https://wa.me/573224029924?text=%s",
                                        user.getName(),
                                        URLEncoder.encode(String.format("Hola, vengo referido por:%s", referralCode),
                                                StandardCharsets.UTF_8.toString()).replace("+", "%20")
                                    );

                                    // Enviar SIEMPRE en dos mensajes: 1) saludo/explicación, 2) mensaje de 'Amigos...'
                                    secondaryMessage = Optional.of(tribalLinkMessage);
                                    responseMessage = result.getAiResponse();
                                    nextChatbotState = "COMPLETED";
                                    System.out.println("ChatbotService: Respuesta de tribu con IA enviada (2 mensajes)");
                                    return new ChatResponse(responseMessage, nextChatbotState, secondaryMessage);
                                } catch (UnsupportedEncodingException e) {
                                    System.err.println("ERROR: No se pudo codificar el link de tribu: " + e.getMessage());
                                    responseMessage = result.getAiResponse() + "\n\nLo siento, tuve un problema al generar el link. Por favor, intenta de nuevo.";
                                    nextChatbotState = "COMPLETED";
                                }
                                                    } else {
                            // No es solicitud de tribu, usar respuesta IA normal con timeout optimizado
                            System.out.println("ChatbotService: IA detectó consulta normal. Procesando con AI Bot...");
                            try {
                                // Crear variables finales para el lambda
                                final String finalSessionId = sessionId;
                                final String finalMessageText = messageText;
                                
                                // Usar CompletableFuture con timeout para evitar bloqueos largos
                                CompletableFuture<String> aiResponseFuture = CompletableFuture.supplyAsync(() -> 
                                    aiBotService.getAIResponse(finalSessionId, finalMessageText)
                                );
                                
                                // Timeout de 20 segundos para respuestas realistas de ChatbotIA
                                responseMessage = aiResponseFuture.get(20, TimeUnit.SECONDS);
                                nextChatbotState = "COMPLETED";
                                System.out.println("ChatbotService: Respuesta de AI Bot obtenida exitosamente");
                            } catch (TimeoutException e) {
                                System.err.println("ChatbotService: Timeout en AI Bot después de 20 segundos, usando fallback");
                                responseMessage = "Lo siento, tuve un problema al conectar con la inteligencia artificial. Por favor, intenta de nuevo más tarde.";
                                nextChatbotState = "COMPLETED";
                            } catch (Exception e) {
                                System.err.println("ChatbotService: Error en AI Bot: " + e.getMessage());
                                responseMessage = "Lo siento, tuve un problema al conectar con la inteligencia artificial. Por favor, intenta de nuevo más tarde.";
                                nextChatbotState = "COMPLETED";
                            }
                        }
                        } else {
                            // Fallback si el análisis falla
                            System.err.println("ChatbotService: Fallback - Análisis de IA falló, usando detección tradicional");
                            if (isTribalLinkRequest(messageText)) {
                                // Lógica tradicional de tribus
                                String referralCode = user.getReferral_code();
                                if (referralCode == null || referralCode.isEmpty()) {
                                    referralCode = generateUniqueReferralCode();
                                    user.setReferral_code(referralCode);
                                    saveUser(user);
                                }
                                
                                try {
                                    String tribalLinkMessage = String.format(
                                        "Amigos, soy %s y quiero invitarte a unirse a la campaña de Daniel Quintero a la Presidencia: https://wa.me/573224029924?text=%s",
                                        user.getName(),
                                        URLEncoder.encode(String.format("Hola, vengo referido por:%s", referralCode),
                                                StandardCharsets.UTF_8.toString()).replace("+", "%20")
                                    );

                                    // Fallback tradicional: también en dos mensajes
                                    String greeting;
                                    if (user.getName() != null && !user.getName().trim().isEmpty()) {
                                        greeting = "Hola " + user.getName().trim() + ", aquí tienes el link de tu tribu.";
                                    } else {
                                        greeting = "Hola, aquí tienes el link de tu tribu.";
                                    }
                                    secondaryMessage = Optional.of(tribalLinkMessage);
                                    responseMessage = greeting;
                                    nextChatbotState = "COMPLETED";
                                    return new ChatResponse(responseMessage, nextChatbotState, secondaryMessage);
                                } catch (UnsupportedEncodingException e) {
                                    System.err.println("ERROR: No se pudo codificar el link de tribu: " + e.getMessage());
                                    responseMessage = "Lo siento, tuve un problema al generar el link. Por favor, intenta de nuevo.";
                                    nextChatbotState = "COMPLETED";
                                }
                            } else {
                                responseMessage = aiBotService.getAIResponse(sessionId, messageText);
                                nextChatbotState = "COMPLETED";
                            }
                        }
                    }
                } else {
                    System.err.println(
                            "ERROR CRÍTICO: Usuario COMPLETED sin un ID de sesión válido (ni teléfono, ni Telegram ID). Doc ID: "
                                    + user.getId());
                    responseMessage = "Lo siento, hemos encontrado un problema con tu registro y no puedo continuar la conversación. Por favor, contacta a soporte.";
                    nextChatbotState = "COMPLETED";
                }
                break;
            default:
                System.out.println("⚠️  WARNING: Usuario en estado desconocido ('" + currentChatbotState
                        + "'). Redirigiendo al flujo de inicio.");
                System.out.println("⚠️  WARNING: Llamando handleNewUserIntro desde estado desconocido para usuario existente");
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
        } else {
            System.out.println("DEBUG: FromId '" + fromId + "' normalizado a '" + phoneNumberToSearch
                    + "' no es un formato de teléfono válido para búsqueda por 'phone'.");
        }

        System.out.println("DEBUG: Continuando con búsqueda por ID de documento...");

        if (!user.isPresent()) {
            System.out.println("DEBUG: Buscando usuario por ID de documento: " + fromId);
            user = findUserByDocumentId(fromId);
            System.out.println("DEBUG: Búsqueda por ID de documento completada, resultado: " + (user.isPresent() ? "ENCONTRADO" : "NO ENCONTRADO"));
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por ID de documento: " + fromId);
                return user;
            } else {
                System.out.println("DEBUG: Usuario NO encontrado por ID de documento: " + fromId);
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
}