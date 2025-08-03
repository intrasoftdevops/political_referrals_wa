package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.politicalreferralswa.model.User; // Asegúrate de que User.java tiene campos: id (String UUID), phone (String), telegram_chat_id (String), Y AHORA referred_by_code (String)
import org.springframework.stereotype.Service;
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

@Service
public class ChatbotService {

    private final Firestore firestore;
    private final WatiApiService watiApiService;
    private final TelegramApiService telegramApiService;
    private final AIBotService aiBotService;
    private final UserDataExtractor userDataExtractor;
    private final NameValidationService nameValidationService;

    private static final Pattern REFERRAL_MESSAGE_PATTERN = Pattern
            .compile("Hola, vengo referido por:\\s*([A-Za-z0-9]{8})");
    private static final String TELEGRAM_BOT_USERNAME = "ResetPoliticaBot";
    private static final Pattern STRICT_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+\\d{10,15}$");

    public ChatbotService(Firestore firestore, WatiApiService watiApiService,
                          TelegramApiService telegramApiService, AIBotService aiBotService,
                          UserDataExtractor userDataExtractor, NameValidationService nameValidationService) {
        this.firestore = firestore;
        this.watiApiService = watiApiService;
        this.telegramApiService = telegramApiService;
        this.aiBotService = aiBotService;
        this.userDataExtractor = userDataExtractor;
        this.nameValidationService = nameValidationService;
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
                    // Validar el nombre con IA de forma síncrona
                    NameValidationService.NameValidationResult validationResult = 
                        nameValidationService.validateName(senderName.trim()).get();
                    
                    if (validationResult.isValid()) {
                        user.setName(senderName.trim());
                        System.out.println("ChatbotService: ✅ Nombre de WhatsApp validado y guardado: " + senderName);
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

            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                watiApiService.sendWhatsAppMessage(fromId, chatResponse.getPrimaryMessage());
            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                telegramApiService.sendTelegramMessage(fromId, chatResponse.getPrimaryMessage());
            } else {
                System.err.println("ChatbotService: Canal desconocido ('" + channelType
                        + "'). No se pudo enviar el mensaje principal.");
            }

            chatResponse.getSecondaryMessage().ifPresent(secondaryMsg -> {
                String[] messagesToSend = secondaryMsg.split("###SPLIT###");
                for (String msg : messagesToSend) {
                    msg = msg.trim();
                    if (!msg.isEmpty()) {
                        System.out.println("ChatbotService: Enviando mensaje secundario a " + fromId + " (Canal: "
                                + channelType + "): '" + msg + "'");
                        if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                            watiApiService.sendWhatsAppMessage(fromId, msg);
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

            return chatResponse.getPrimaryMessage();
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
                
                // Construir mensaje personalizado con política de privacidad
                String personalizedMessage = extractionResult.getMessage() + 
                    "\n\nPara continuar, necesito que confirmes que has leído y aceptas nuestra política de privacidad: " +
                    "https://danielquinterocalle.com/privacidad. ¿Aceptas? (Sí/No)";
                
                return new ChatResponse(personalizedMessage, "WAITING_TERMS_ACCEPTANCE");
            } else {
                // Si se extrajo parcialmente, usar el mensaje de extracción sin incluir política de privacidad
                System.out.println("DEBUG handleNewUserIntro: Usando extracción inteligente - Parcial, sin política de privacidad");
                
                String welcomeMessage = """
                    ¡Hola! 👋 Soy el bot de Reset a la Política.
                    Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.
                    
                    """ + extractionResult.getMessage();
                
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida en extracción inteligente parcial");
                return new ChatResponse(welcomeMessage, extractionResult.getNextState());
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
                user.setReferred_by_phone(referrerUser.get().getPhone());
                user.setReferred_by_code(incomingReferralCode); // <-- AÑADIDO: Guardar el código de referido
                System.out.println("DEBUG handleNewUserIntro: Estableciendo referred_by_phone: '" + user.getReferred_by_phone() + "' y referred_by_code: '" + user.getReferred_by_code() + "'");


                // Personalizar saludo si tenemos el nombre de WhatsApp validado
                String personalizedGreeting = "";
                if (user.getName() != null && !user.getName().trim().isEmpty()) {
                    personalizedGreeting = "¡Hola " + user.getName().trim() + "! 👋 ¿Te llamas " + user.getName().trim() + " cierto?\n\n";
                }
                
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida con código de referido válido");
                return new ChatResponse(
                        personalizedGreeting + """
                                ¡Hola! 👋 Soy el bot de Reset a la Política.
                                Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.
                                ¡Qué emoción que te unas a esta ola de cambio para Colombia! Veo que vienes referido por un amigo.

                                Para continuar con tu registro, necesito algunos datos. ¿Cuál es tu nombre?
                                """,
                        "WAITING_NAME");
            } else {
                System.out.println(
                        "ChatbotService: Código de referido válido en formato, pero NO ENCONTRADO en el primer mensaje: "
                                + incomingReferralCode);
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida con código de referido inválido");
                return new ChatResponse(
                        """
                                ¡Hola! 👋 Soy el bot de Reset a la Política.
                                Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.
                                Parece que el código de referido que me enviaste no es válido, pero no te preocupes, ¡podemos continuar!

                                Para continuar con tu registro, necesito algunos datos. ¿Cuál es tu nombre?
                                """,
                        "WAITING_NAME");
            }
        } else {
            System.out.println("DEBUG handleNewUserIntro: El mensaje no coincide con el patrón de referido.");

            System.out
                    .println("ChatbotService: Primer mensaje no contiene código de referido. Iniciando flujo general.");
            
            // Verificar si ya tenemos un nombre validado
            if (user.getName() != null && !user.getName().trim().isEmpty()) {
                System.out.println("⚠️  WARNING: Generando mensaje con nombre ya validado: " + user.getName());
                return new ChatResponse(
                        String.format("""
                                ¡Hola! 👋 Soy el bot de Reset a la Política.
                                Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.

                                Veo que te llamas %s. ¿En qué ciudad vives?
                                """, user.getName()),
                        "WAITING_CITY");
            } else {
                System.out.println("⚠️  WARNING: Generando mensaje de bienvenida general (sin código de referido)");
                return new ChatResponse(
                        """
                                ¡Hola! 👋 Soy el bot de Reset a la Política.
                                Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.

                                Para continuar con tu registro, necesito algunos datos. ¿Cuál es tu nombre?
                                """,
                        "WAITING_NAME");
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
                        Ahora, para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                        Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?

                        Responde: Sí o No.
                        """;
                nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                break;

            case "WAITING_TERMS_ACCEPTANCE":
                if (messageText.equalsIgnoreCase("Sí") || messageText.equalsIgnoreCase("Si")) {
                    user.setAceptaTerminos(true);
                    
                    // SIEMPRE validar que el usuario haya aceptado los términos antes de completar
                    System.out.println("DEBUG: Usuario aceptó términos de privacidad. Validando datos completos...");
                    
                    // Verificar si ya tiene todos los datos necesarios
                    boolean hasName = user.getName() != null && !user.getName().isEmpty();
                    boolean hasCity = user.getCity() != null && !user.getCity().isEmpty();
                    
                    System.out.println("DEBUG: Usuario tiene nombre: " + hasName + " (nombre: " + user.getName() + ")");
                    System.out.println("DEBUG: Usuario tiene ciudad: " + hasCity + " (ciudad: " + user.getCity() + ")");
                    
                    if (hasName && hasCity) {
                        System.out.println("DEBUG: Usuario tiene todos los datos. Completando registro...");
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
                                    "Amigos, los invito a unirse a la campaña de Daniel Quintero a la Presidencia: https://wa.me/573224029924?text=%s",
                                    URLEncoder.encode(String.format("Hola, vengo referido por:%s", referralCode),
                                            StandardCharsets.UTF_8.toString()).replace("+", "%20"));
                            additionalMessages.add(friendsInviteMessage);

                            String aiBotIntroMessage = """
                                    ¡Atención! Ahora entrarás en conversación con una inteligencia artificial.
                                    Soy Daniel Quintero Bot, en mi versión de IA de prueba para este proyecto.
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
                        System.out.println("DEBUG: Usuario no tiene todos los datos. Continuando flujo...");
                        responseMessage = "¿Cuál es tu nombre?";
                        nextChatbotState = "WAITING_NAME";
                    }
                } else {
                    System.out.println("DEBUG: Usuario no aceptó términos. Pidiendo confirmación...");
                    responseMessage = "Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.\n\nAcompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?";
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                }
                break;
            case "WAITING_NAME":
                // Intentar extracción inteligente primero
                UserDataExtractor.ExtractionResult nameExtractionResult = userDataExtractor.extractAndUpdateUser(user, messageText, null);
                
                if (nameExtractionResult.isSuccess()) {
                    // Guardar usuario actualizado después de la extracción
                    saveUser(user);
                    
                    if (nameExtractionResult.needsClarification()) {
                        // Si necesita aclaración
                        responseMessage = nameExtractionResult.getMessage();
                        nextChatbotState = "WAITING_CLARIFICATION";
                    } else if (nameExtractionResult.isCompleted()) {
                        // Si se completó la extracción
                        responseMessage = nameExtractionResult.getMessage();
                        nextChatbotState = "CONFIRM_DATA";
                    } else {
                        // Si se extrajo parcialmente
                        responseMessage = nameExtractionResult.getMessage();
                        nextChatbotState = nameExtractionResult.getNextState();
                    }
                } else {
                    // Si falló la extracción, usar método tradicional
                    if (messageText != null && !messageText.trim().isEmpty()) {
                        user.setName(messageText.trim());
                        responseMessage = "¿En qué ciudad vives?";
                        nextChatbotState = "WAITING_CITY";
                    } else {
                        responseMessage = "Por favor, ingresa un nombre válido.";
                    }
                }
                break;
            case "WAITING_CITY":
                // Intentar extracción inteligente primero
                UserDataExtractor.ExtractionResult cityExtractionResult = userDataExtractor.extractAndUpdateUser(user, messageText, null);
                
                if (cityExtractionResult.isSuccess()) {
                    // Guardar usuario actualizado después de la extracción
                    saveUser(user);
                    
                    if (cityExtractionResult.needsClarification()) {
                        // Si necesita aclaración
                        responseMessage = cityExtractionResult.getMessage();
                        nextChatbotState = "WAITING_CLARIFICATION";
                    } else if (cityExtractionResult.isCompleted()) {
                        // Si se completó la extracción
                        responseMessage = cityExtractionResult.getMessage();
                        nextChatbotState = "CONFIRM_DATA";
                    } else {
                        // Si se extrajo parcialmente
                        responseMessage = cityExtractionResult.getMessage();
                        nextChatbotState = cityExtractionResult.getNextState();
                    }
                } else {
                    // Si falló la extracción, usar método tradicional
                    if (messageText != null && !messageText.trim().isEmpty()) {
                        user.setCity(messageText.trim());
                        responseMessage = "Confirmamos tus datos: " + user.getName() + ", de " + user.getCity()
                                + ". ¿Es correcto? (Sí/No)";
                        nextChatbotState = "CONFIRM_DATA";
                    } else {
                        responseMessage = "Por favor, ingresa una ciudad válida.";
                    }
                }
                break;
            case "CONFIRM_DATA":
                if (messageText.equalsIgnoreCase("Sí") || messageText.equalsIgnoreCase("Si")) {
                    // Verificar si ya aceptó los términos
                    if (!user.isAceptaTerminos()) {
                        // Si no aceptó términos, pedirle que los acepte
                        responseMessage = "Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.\n\nAcompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?";
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
                                "Amigos, los invito a unirse a la campaña de Daniel Quintero a la Presidencia: https://wa.me/573224029924?text=%s",
                                URLEncoder.encode(String.format("Hola, vengo referido por:%s", referralCode),
                                        StandardCharsets.UTF_8.toString()).replace("+", "%20"));
                        additionalMessages.add(friendsInviteMessage);

                        String aiBotIntroMessage = """
                                ¡Atención! Ahora entrarás en conversación con una inteligencia artificial.
                                Soy Daniel Quintero Bot, en mi versión de IA de prueba para este proyecto.
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
                System.out.println("ChatbotService: Usuario COMPLETED. Pasando consulta a AI Bot.");

                String sessionId = user.getPhone();

                if ((sessionId == null || sessionId.isEmpty()) && user.getTelegram_chat_id() != null) {
                    System.err.println("ADVERTENCIA: Usuario COMPLETED sin teléfono. Usando Telegram Chat ID ("
                            + user.getTelegram_chat_id() + ") como fallback para la sesión de IA. Doc ID: "
                            + user.getId());
                    sessionId = user.getTelegram_chat_id();
                }

                if (sessionId != null && !sessionId.isEmpty()) {
                    responseMessage = aiBotService.getAIResponse(sessionId, messageText);
                    nextChatbotState = "COMPLETED";
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
            ApiFuture<QuerySnapshot> future = firestore.collection("users")
                    .whereEqualTo("phone", phoneNumber)
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
            System.err.println(
                    "ERROR al buscar usuario por campo 'phone' en Firestore (" + phoneNumber + "): " + e.getMessage());
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
            user = findUserByPhoneNumberField(phoneNumberToSearch);
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


        if (!user.isPresent()) {
            user = findUserByDocumentId(fromId);
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por ID de documento: " + fromId);
                return user;
            } else {
                System.out.println("DEBUG: Usuario NO encontrado por ID de documento: " + fromId);
            }
        }


        if (!user.isPresent() && "TELEGRAM".equalsIgnoreCase(channelType)) {
            user = findUserByTelegramChatIdField(fromId);
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
}