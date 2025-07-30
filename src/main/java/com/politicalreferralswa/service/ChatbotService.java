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

    private static final Pattern REFERRAL_MESSAGE_PATTERN = Pattern
            .compile("Hola, vengo referido por:\\s*([A-Za-z0-9]{8})");
    private static final String TELEGRAM_BOT_USERNAME = "ResetPoliticaBot";
    private static final Pattern STRICT_PHONE_NUMBER_PATTERN = Pattern.compile("^\\+\\d{10,15}$");

    public ChatbotService(Firestore firestore, WatiApiService watiApiService,
                          TelegramApiService telegramApiService, AIBotService aiBotService) {
        this.firestore = firestore;
        this.watiApiService = watiApiService;
        this.telegramApiService = telegramApiService;
        this.aiBotService = aiBotService;
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
        System.out.println("ChatbotService: Procesando mensaje entrante de " + fromId + " (Canal: " + channelType
                + "): '" + messageText + "'");

        User user = findUserByAnyIdentifier(fromId, channelType).orElse(null);
        boolean isNewUser = (user == null);
        ChatResponse chatResponse = null;

        String normalizedPhoneForWhatsapp = "";
        if ("WHATSAPP".equalsIgnoreCase(channelType)) {
            String cleanedId = fromId.replaceAll("[^\\d+]", "");
            if (cleanedId.startsWith("+") && STRICT_PHONE_NUMBER_PATTERN.matcher(cleanedId).matches()) {
                normalizedPhoneForWhatsapp = cleanedId;
            } else if (cleanedId.matches("^\\d{10,15}$")) {
                normalizedPhoneForWhatsapp = "+" + cleanedId;
            }
        }

        if (isNewUser) {
            System.out.println("ChatbotService: Nuevo usuario detectado: " + fromId);
            user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setCreated_at(Timestamp.now());
            user.setAceptaTerminos(false);
            user.setReferred_by_phone(null); // Asegúrate de inicializarlo
            user.setReferred_by_code(null); // Asegúrate de inicializarlo

            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                user.setPhone_code("+57");
                user.setPhone(normalizedPhoneForWhatsapp);

                chatResponse = handleNewUserIntro(user, messageText);
                user.setChatbot_state(chatResponse.getNextChatbotState());
                saveUser(user);

            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                user.setTelegram_chat_id(fromId);
                user.setChatbot_state("TELEGRAM_WAITING_PHONE_NUMBER");
                saveUser(user);
                chatResponse = new ChatResponse(
                        "¡Hola! 👋 Soy el bot de *Reset a la Política*. Para identificarte y continuar, por favor, envíame tu número de teléfono.",
                        "TELEGRAM_WAITING_PHONE_NUMBER");
            } else {
                System.err.println("ChatbotService: Nuevo usuario de canal desconocido ('" + channelType
                        + "'). No se pudo inicializar.");
                return "Lo siento, no puedo procesar tu solicitud desde este canal.";
            }
        } else {
            System.out.println("ChatbotService: Usuario existente. Estado actual: " + user.getChatbot_state()
                    + ". ID del documento: " + (user.getPhone() != null ? user.getPhone().substring(1) : user.getId()));

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

            chatResponse = handleExistingUserMessage(user, messageText);
        }

        if (chatResponse != null) {
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
     * Maneja la lógica de inicio para nuevos usuarios.
     * Intenta detectar un código de referido y, si no, procede con la bienvenida general.
     *
     * @param user        El objeto User del nuevo usuario.
     * @param messageText El primer mensaje enviado por el usuario.
     * @return ChatResponse con el mensaje y el siguiente estado.
     */
    private ChatResponse handleNewUserIntro(User user, String messageText) {
        System.out.println("DEBUG handleNewUserIntro: Mensaje entrante recibido: '" + messageText + "'");

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


                return new ChatResponse(
                        """
                                ¡Hola! 👋 Soy el bot de **Reset a la Política**.
                                ¡Qué emoción que te unas a esta ola de cambio para Colombia! Veo que vienes referido por un amigo.

                                Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                                Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                                Responde: Sí o No.
                                """,
                        "WAITING_TERMS_ACCEPTANCE");
            } else {
                System.out.println(
                        "ChatbotService: Código de referido válido en formato, pero NO ENCONTRADO en el primer mensaje: "
                                + incomingReferralCode);
                return new ChatResponse(
                        """
                                ¡Hola! 👋 Soy el bot de **Reset a la Política**.
                                Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.
                                Parece que el código de referido que me enviaste no es válido, pero no te preocupes, ¡podemos continuar!

                                Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                                Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                                Responde: Sí o No.
                                """,
                        "WAITING_TERMS_ACCEPTANCE");
            }
        } else {
            System.out.println("DEBUG handleNewUserIntro: El mensaje no coincide con el patrón de referido.");

            System.out
                    .println("ChatbotService: Primer mensaje no contiene código de referido. Iniciando flujo general.");
            return new ChatResponse(
                    """
                            ¡Hola! 👋 Soy el bot de **Reset a la Política**.
                            Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.

                            Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                            Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                            Responde: Sí o No.
                            """,
                    "WAITING_TERMS_ACCEPTANCE");
        }
    }


    private ChatResponse handleExistingUserMessage(User user, String messageText) {
        messageText = messageText.trim();

        String currentChatbotState = user.getChatbot_state();
        String responseMessage = "";
        String nextChatbotState = currentChatbotState;
        Optional<String> secondaryMessage = Optional.empty();

        Matcher matcher = REFERRAL_MESSAGE_PATTERN.matcher(messageText);

        switch (currentChatbotState) {
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
                    responseMessage = "¿Cuál es tu nombre?";
                    nextChatbotState = "WAITING_NAME";
                } else {
                    responseMessage = "Para continuar, debes aceptar los términos. ¿Aceptas? (Sí/No)";
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                }
                break;
            case "WAITING_NAME":
                if (messageText != null && !messageText.trim().isEmpty()) {
                    user.setName(messageText.trim());
                    responseMessage = "¿En qué ciudad vives?";
                    nextChatbotState = "WAITING_CITY";
                } else {
                    responseMessage = "Por favor, ingresa un nombre válido.";
                }
                break;
            case "WAITING_CITY":
                if (messageText != null && !messageText.trim().isEmpty()) {
                    user.setCity(messageText.trim());
                    responseMessage = "Confirmamos tus datos: " + user.getName() + ", de " + user.getCity()
                            + ". ¿Es correcto? (Sí/No)";
                    nextChatbotState = "CONFIRM_DATA";
                } else {
                    responseMessage = "Por favor, ingresa una ciudad válida.";
                }
                break;
            case "CONFIRM_DATA":
                if (messageText.equalsIgnoreCase("Sí") || messageText.equalsIgnoreCase("Si")) {
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
                    responseMessage = "Por favor, vuelve a escribir tu nombre completo para corregir tus datos.";
                    nextChatbotState = "WAITING_NAME";
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
                System.out.println("ChatbotService: Usuario en estado desconocido ('" + currentChatbotState
                        + "'). Redirigiendo al flujo de inicio.");
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

        if (!phoneNumberToSearch.isEmpty() && STRICT_PHONE_NUMBER_PATTERN.matcher(phoneNumberToSearch).matches()) {
            user = findUserByPhoneNumberField(phoneNumberToSearch);
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por campo 'phone': " + phoneNumberToSearch);
                return user;
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
            }
        }


        if (!user.isPresent() && "TELEGRAM".equalsIgnoreCase(channelType)) {
            user = findUserByTelegramChatIdField(fromId);
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por campo 'telegram_chat_id': " + fromId);
                return user;
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
    private void saveUser(User user) {
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


    private String generateUniqueReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}