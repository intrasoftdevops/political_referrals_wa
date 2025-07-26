package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.politicalreferralswa.model.User;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.DocumentSnapshot;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.politicalreferralswa.service.WatiApiService;
import com.politicalreferralswa.service.TelegramApiService;
import com.politicalreferralswa.service.AIBotService;
import com.politicalreferralswa.service.ChatResponse; // Asegúrate de que esta clase exista

@Service
public class ChatbotService {

    private final Firestore firestore;
    private final WatiApiService watiApiService;
    private final TelegramApiService telegramApiService;
    private final AIBotService aiBotService;

    private static final Pattern REFERRAL_MESSAGE_PATTERN = Pattern.compile("Hola, vengo referido por: ([A-Za-z0-9]{8})");
    private static final String TELEGRAM_BOT_USERNAME = "ResetPoliticaBot";
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^\\+\\d{10,15}$"); // Patrón para validar número de teléfono (+[código_país][número])

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

        Optional<User> existingUser = getUserByPhoneNumber(testPhoneNumber);
        if (existingUser.isPresent()) {
            System.out.println("DEBUG: Usuario referente de prueba '" + testPhoneNumber + "' ya existe. No se creará de nuevo.");
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

        try {
            firestore.collection("users").document(testUser.getPhone()).set(testUser).get();
            System.out.println("DEBUG: Usuario referente de prueba '" + testUser.getName() + "' con código '" + testUser.getReferral_code() + "' creado exitosamente en Firestore.");
        } catch (Exception e) {
            System.err.println("ERROR DEBUG: No se pudo crear el usuario de prueba en Firestore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa un mensaje entrante de un usuario.
     * Ahora este método maneja el envío de múltiples mensajes si handleExistingUserMessage lo indica.
     *
     * @param fromId El ID del remitente (número de teléfono para WhatsApp, chat ID para Telegram).
     * @param messageText El texto del mensaje recibido.
     * @param channelType El tipo de canal.
     * @return String La respuesta principal del chatbot (el primer mensaje enviado).
     */
    public String processIncomingMessage(String fromId, String messageText, String channelType) {
        System.out.println("ChatbotService: Procesando mensaje entrante de " + fromId + " (Canal: " + channelType + "): '" + messageText + "'");

        User user = getUserByPhoneNumber(fromId).orElse(null); // Busca por número de teléfono
        boolean isNewUser = (user == null);

        ChatResponse chatResponse = null; // Inicializar fuera de los bloques if/else

        if (isNewUser) {
            System.out.println("ChatbotService: Nuevo usuario detectado: " + fromId);
            user = new User();
            user.setId(UUID.randomUUID().toString());
            user.setCreated_at(Timestamp.now());
            user.setAceptaTerminos(false);

            // Determinar estado inicial y mensaje según el canal
            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                user.setPhone_code("+57"); // WhatsApp ya nos da el número completo con código
                user.setPhone(fromId);
                user.setChatbot_state("NEW_USER_INTRO"); // Inicia flujo de bienvenida/referido
                saveUser(user);
                chatResponse = handleExistingUserMessage(user, messageText); // Procesar el primer mensaje
            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                user.setTelegram_chat_id(fromId); // Guardar el chat ID de Telegram
                user.setChatbot_state("TELEGRAM_WAITING_PHONE_NUMBER"); // Nuevo estado para pedir el teléfono
                saveUser(user); // Guardar el usuario con su telegram_chat_id y estado inicial
                chatResponse = new ChatResponse(
                    "¡Hola! Para identificarte y continuar, por favor, envíame tu número de teléfono con el código de país (ej. +573001234567).",
                    "TELEGRAM_WAITING_PHONE_NUMBER"
                );
            } else {
                System.err.println("ChatbotService: Nuevo usuario de canal desconocido ('" + channelType + "'). No se pudo inicializar.");
                return "Lo siento, no puedo procesar tu solicitud desde este canal."; // Early exit
            }
        } else {
            System.out.println("ChatbotService: Usuario existente. Estado actual: " + user.getChatbot_state());
            chatResponse = handleExistingUserMessage(user, messageText);
        }

        // --- Lógica para enviar respuesta(s) al canal correcto ---
        if (chatResponse != null) { // Asegurarse de que chatResponse no sea nulo
            // Enviar el mensaje principal
            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                watiApiService.sendWhatsAppMessage(fromId, chatResponse.getPrimaryMessage());
            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                // Para Telegram, fromId es el chat ID
                telegramApiService.sendTelegramMessage(fromId, chatResponse.getPrimaryMessage());
            } else {
                System.err.println("ChatbotService: Canal desconocido ('" + channelType + "'). No se pudo enviar el mensaje principal.");
            }

            // Enviar el mensaje secundario (si existe)
            chatResponse.getSecondaryMessage().ifPresent(secondaryMsg -> {
                System.out.println("ChatbotService: Enviando mensaje secundario a " + fromId + " (Canal: " + channelType + "): '" + secondaryMsg + "'");
                if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                    watiApiService.sendWhatsAppMessage(fromId, secondaryMsg);
                } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                    telegramApiService.sendTelegramMessage(fromId, secondaryMsg);
                } else {
                    System.err.println("ChatbotService: Canal desconocido ('" + channelType + "'). No se pudo enviar el mensaje secundario.");
                }
            });

            // Actualizar el estado del usuario en Firestore (DESPUÉS DE ENVIAR LOS MENSAJES)
            user.setChatbot_state(chatResponse.getNextChatbotState());
            user.setUpdated_at(Timestamp.now());
            saveUser(user);

            return chatResponse.getPrimaryMessage(); // Devolver el mensaje principal (útil para logs del controlador)
        }
        return "ERROR: No se pudo generar una respuesta."; // Fallback si chatResponse es nulo (no debería ocurrir)
    }

    /**
     * Contiene la lógica de la máquina de estados del chatbot.
     * Ahora devuelve un objeto ChatResponse que incluye el mensaje(s) y el próximo estado.
     * @param user El objeto User actual.
     * @param messageText El texto del mensaje recibido.
     * @return Un objeto ChatResponse con los mensajes a enviar y el siguiente estado.
     */
    private ChatResponse handleExistingUserMessage(User user, String messageText) {
        String currentChatbotState = user.getChatbot_state();
        String responseMessage = "";
        String nextChatbotState = currentChatbotState;
        Optional<String> secondaryMessage = Optional.empty();

        Matcher matcher = REFERRAL_MESSAGE_PATTERN.matcher(messageText);

        switch (currentChatbotState) {
            case "TELEGRAM_WAITING_PHONE_NUMBER":
                if (PHONE_NUMBER_PATTERN.matcher(messageText).matches()) {
                    // Si el número es válido, se guarda y se avanza al siguiente paso del flujo principal
                    user.setPhone(messageText); // Guardar el número de teléfono completo
                    // Asume un código de país basado en los primeros caracteres si lo necesitas para fines internos, o haz una validación más robusta.
                    user.setPhone_code(messageText.substring(0, Math.min(messageText.length(), 4)));

                    responseMessage = """
                            ¡Gracias! Hemos registrado tu número de teléfono.
                            Ahora, para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra politica de tratamiento de datos, plasmadas aqui https://danielquinterocalle.com/privacidad. Si continuas está conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                            Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                            """;
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE"; // Directamente a aceptar términos
                } else {
                    responseMessage = "Ese no parece ser un número de teléfono válido con código de país. Por favor, asegúrate de incluir el '+', el código de país y tu número (ej. +573001234567).";
                    nextChatbotState = "TELEGRAM_WAITING_PHONE_NUMBER"; // Permanece en el mismo estado para reintentar
                }
                break;

            case "NEW_USER_INTRO":
                if (matcher.matches()) {
                    String incomingReferralCode = matcher.group(1);
                    System.out.println("ChatbotService: Mensaje contiene posible código de referido: " + incomingReferralCode);
                    Optional<User> referrerUser = getUserByReferralCode(incomingReferralCode);

                    if (referrerUser.isPresent()) {
                        user.setReferred_by_phone(referrerUser.get().getPhone());
                        responseMessage = """
                                ¡Hola! Veo que vienes referido por un amigo. ¡Qué emoción que te unas a esta ola de cambio para Colombia!
                                Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra politica de tratamiento de datos, plasmadas aqui https://danielquinterocalle.com/privacidad. Si continuas está conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                                Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                                """;
                        nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                    } else {
                        System.out.println("ChatbotService: Código de referido válido en formato, pero NO ENCONTRADO: " + incomingReferralCode);
                        responseMessage = String.format(
                                """
                                El código de referido '%s' no es válido o no fue encontrado.
                                Por favor, verifica el código e inténtalo de nuevo, o escribe 'INICIAR' para comenzar tu registro sin un referido.
                                """, incomingReferralCode
                        );
                        nextChatbotState = "WAITING_REFERRAL_RETRY_OR_PROCEED";
                    }
                } else {
                    responseMessage = """
                            ¡Hola!
                            Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.
                            Si vienes referido por alguien, por favor, escribe el mensaje exacto: 'Hola, vengo referido por: [TU_CODIGO_DE_8_CARACTERES]'.
                            Si no tienes un código o prefieres empezar sin uno, escribe 'INICIAR' para comenzar tu registro.
                            """;
                    nextChatbotState = "WAITING_REFERRAL_RETRY_OR_PROCEED";
                }
                break;

            case "WAITING_REFERRAL_RETRY_OR_PROCEED":
                if (matcher.matches()) {
                    String incomingReferralCode = matcher.group(1);
                    System.out.println("ChatbotService: Reintento de mensaje de referido: " + incomingReferralCode);
                    Optional<User> referrerUser = getUserByReferralCode(incomingReferralCode);

                    if (referrerUser.isPresent()) {
                        user.setReferred_by_phone(referrerUser.get().getPhone());
                        responseMessage = """
                                ¡Excelente! Veo que vienes referido por un amigo. ¡Qué emoción que te unas a esta ola de cambio para Colombia!
                                Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra politica de tratamiento de datos, plasmadas aqui https://danielquinterocalle.com/privacidad. Si continuas está conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                                Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                                """;
                        nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                    } else {
                        System.out.println("ChatbotService: Código de referido válido en formato, pero NO ENCONTRADO en reintento: " + incomingReferralCode);
                        responseMessage = String.format(
                                """
                                El código de referido '%s' no es válido o no fue encontrado.
                                Por favor, verifica el código e inténtalo de nuevo, o escribe 'INICIAR' para comenzar tu registro sin un referido.
                                """, incomingReferralCode
                        );
                        nextChatbotState = "WAITING_REFERRAL_RETRY_OR_PROCEED";
                    }
                } else if (messageText.equalsIgnoreCase("INICIAR")) {
                    responseMessage = """
                            ¡Perfecto! Iniciemos tu registro.
                            Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra politica de tratamiento de datos, plasmadas aqui https://danielquinterocalle.com/privacidad. Si continuas está conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                            Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                            """;
                    nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                } else {
                    responseMessage = """
                            No entendí tu respuesta. Por favor, escribe el mensaje exacto: 'Hola, vengo referido por: [TU_CODIGO_DE_8_CARACTERES]'
                            O escribe 'INICIAR' para comenzar tu registro sin un referido.
                            """;
                    nextChatbotState = "WAITING_REFERRAL_RETRY_OR_PROCEED";
                }
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
                    responseMessage = "Confirmamos tus datos: " + user.getName() + ", de " + user.getCity() + ". ¿Es correcto? (Sí/No)";
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

                    try {
                        // 1. Prepara el texto sin codificar los espacios con %20.
                        // Usa espacios normales y luego el formato %s para el código.
                        String whatsappRawReferralText = String.format("Hola, vengo referido por:%s", referralCode);
                        
                        // 2. Ahora, codifica *toda* la cadena para que sea segura en la URL.
                        String encodedWhatsappMessage = URLEncoder.encode(whatsappRawReferralText, StandardCharsets.UTF_8.toString());

                        // 3. Construye el enlace de WhatsApp usando el texto codificado.
                        whatsappInviteLink = "https://wa.me/573150841309?text=" + encodedWhatsappMessage;

                        // Enlace de Telegram (este está correcto y no necesita cambios)
                        String encodedTelegramPayload = URLEncoder.encode(referralCode, StandardCharsets.UTF_8.toString());
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=" + encodedTelegramPayload;

                    } catch (UnsupportedEncodingException e) {
                        System.err.println("ERROR: No se pudo codificar los códigos de referido. Causa: " + e.getMessage());
                        e.printStackTrace();
                        whatsappInviteLink = "https://wa.me/573150841309?text=Error%20al%20generar%20referido";
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=Error";
                    }

                    // Mensaje principal de confirmación de registro y enlaces de referido
                    responseMessage = String.format(
                        """
                        %s Gracias por unirte a la ola de cambio que estamos construyendo para Colombia.
                        Tu decisión de registrarte en la plataforma es un paso fundamental para construir el país que soñamos: un país más justo, equitativo y próspero para todos los colombianos.
                        Sé que muchos comparten la misma visión de un futuro mejor, y por eso quiero invitarte a que compartas este proyecto con tus amigos, familiares y conocidos. Juntos podemos lograr una transformación real y profunda.

                        Aquí tienes tus enlaces de referido:
                        WhatsApp: %s
                        Telegram: %s

                        Comparte estos enlaces y ayúdanos a llegar a más personas.
                        Cada persona que se registre a través de tu enlace nos acerca a la meta de construir una Colombia innovadora, incluyente y con oportunidades para todos.
                        Recuerda, el cambio empieza con cada uno de nosotros. ¡Gracias por ser parte de este movimiento!

                        Con esperanza y convicción,
                        Daniel Quintero Calle.
                        Candidato Presidencial 2026.
                        #ColombiaEnMarcha #UnidosPorElCambio #FuturoParaTodos
                        """,
                        user.getName(),
                        whatsappInviteLink,
                        telegramInviteLink
                    );

                    // Mensaje secundario: la introducción a la IA
                    secondaryMessage = Optional.of(
                        """
                        ¡Atención! Ahora entrarás en conversación con una inteligencia artificial.
                        Soy Daniel Quintero Calle, en mi versión de IA de prueba para este proyecto.
                        Mi objetivo es simular mis respuestas basadas en información clave y mi visión política.
                        Ten en cuenta que aún estoy en etapa de prueba y mejora continua.
                        ¡Hazme tu pregunta!
                        """
                    );

                    nextChatbotState = "COMPLETED"; // El usuario pasa al estado COMPLETED
                } else {
                    responseMessage = "Por favor, vuelve a escribir tu nombre completo para corregir tus datos.";
                    nextChatbotState = "WAITING_NAME";
                }
                break;
            case "COMPLETED":
                // PASAR EL CONTROL AL BOT DE IA
                System.out.println("ChatbotService: Usuario COMPLETED. Pasando consulta a AI Bot.");
                responseMessage = aiBotService.getAIResponse(user.getPhone(), messageText);
                nextChatbotState = "COMPLETED"; // El estado permanece COMPLETED
                break;
            default:
                responseMessage = """
                        ¡Hola!
                        Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.
                        Si vienes referido por alguien, por favor, escribe el mensaje exacto: 'Hola, vengo referido por: [TU_CODIGO_DE_8_CARACTERES]'.
                        Si no tienes un código o prefieres empezar sin uno, escribe 'INICIAR' para comenzar tu registro.
                        """;
                nextChatbotState = "WAITING_REFERRAL_RETRY_OR_PROCEED";
                break;
        }

        // Devolver el objeto ChatResponse con el/los mensaje(s) y el siguiente estado
        return new ChatResponse(responseMessage, nextChatbotState, secondaryMessage);
    }

    // --- Métodos Auxiliares (sin cambios) ---

    private Optional<User> getUserByPhoneNumber(String phoneNumber) {
        try {
            return Optional.ofNullable(firestore.collection("users").document(phoneNumber).get().get().toObject(User.class));
        } catch (Exception e) {
            System.err.println("ERROR al buscar usuario por número de teléfono en Firestore: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<User> getUserByReferralCode(String referralCode) {
        try {
            QuerySnapshot querySnapshot = firestore.collection("users")
                                            .whereEqualTo("referral_code", referralCode)
                                            .limit(1)
                                            .get()
                                            .get();

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

    private void saveUser(User user) {
        try {
            firestore.collection("users").document(user.getPhone()).set(user).get();
        } catch (Exception e) {
            System.err.println("ERROR al guardar usuario en Firestore: " + e.getMessage());
        }
    }

    private String generateUniqueReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}