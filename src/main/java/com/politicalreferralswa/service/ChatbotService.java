package com.politicalreferralswa.service;

import com.google.cloud.firestore.Firestore;
import com.politicalreferralswa.model.User; // Asegúrate de que User.java tiene campos: id (String UUID), phone (String), telegram_chat_id (String)
import org.springframework.stereotype.Service;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.api.core.ApiFuture;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList; // Importar ArrayList
import java.util.List;     // Importar List
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Asegúrate de que ChatResponse existe y tiene los métodos getPrimaryMessage(), getNextChatbotState(), getSecondaryMessage()
// Y un constructor adecuado: ChatResponse(String primaryMessage, String nextState, Optional<String> secondaryMessage)
// O ChatResponse(String primaryMessage, String nextState) si no hay mensaje secundario

@Service
public class ChatbotService {

    private final Firestore firestore;
    private final WatiApiService watiApiService;
    private final TelegramApiService telegramApiService;
    private final AIBotService aiBotService;

    private static final Pattern REFERRAL_MESSAGE_PATTERN = Pattern.compile("Hola, vengo referido por: ([A-Za-z0-9]{8})");
    private static final String TELEGRAM_BOT_USERNAME = "ResetPoliticaBot";
    // Patrón estricto para la validación final del número de teléfono normalizado (con +)
    // Requiere un '+' seguido de 10 a 15 dígitos.
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
        String testPhoneNumber = "+573100000001"; // Este ya tiene el '+'
        String testReferralCode = "TESTCODE";

        // Usar la búsqueda unificada principal
        Optional<User> existingUser = findUserByAnyIdentifier(testPhoneNumber, "WHATSAPP"); 
        if (existingUser.isPresent()) {
            System.out.println("DEBUG: Usuario referente de prueba '" + testPhoneNumber + "' ya existe. No se creará de nuevo.");
            return;
        }

        User testUser = new User();
        testUser.setId(UUID.randomUUID().toString()); // Este ID se usará internamente
        testUser.setPhone_code("+57");
        testUser.setPhone(testPhoneNumber); // Se guarda con el '+'
        testUser.setName("Referente de Prueba");
        testUser.setCity("Bogota");
        testUser.setChatbot_state("COMPLETED");
        testUser.setAceptaTerminos(true);
        testUser.setReferral_code(testReferralCode);
        testUser.setCreated_at(Timestamp.now());
        testUser.setUpdated_at(Timestamp.now());
        testUser.setReferred_by_phone(null);

        try {
            saveUser(testUser); // Usar el método unificado saveUser
            System.out.println("DEBUG: Usuario referente de prueba '" + testUser.getName() + "' con código '" + testUser.getReferral_code() + "' creado exitosamente en Firestore.");
        } catch (Exception e) {
            System.err.println("ERROR DEBUG: No se pudo crear el usuario de prueba en Firestore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Procesa un mensaje entrante de un usuario.
     *
     * @param fromId El ID del remitente (número de teléfono para WhatsApp, chat ID para Telegram).
     * @param messageText El texto del mensaje recibido.
     * @param channelType El tipo de canal.
     * @return String La respuesta principal del chatbot (el primer mensaje enviado).
     */
    public String processIncomingMessage(String fromId, String messageText, String channelType) {
        System.out.println("ChatbotService: Procesando mensaje entrante de " + fromId + " (Canal: " + channelType + "): '" + messageText + "'");

        User user = findUserByAnyIdentifier(fromId, channelType).orElse(null);
        boolean isNewUser = (user == null);
        ChatResponse chatResponse = null;

        // Normalizar el fromId de WhatsApp para guardar o actualizar el campo 'phone'
        String normalizedPhoneForWhatsapp = "";
        if ("WHATSAPP".equalsIgnoreCase(channelType)) {
            String cleanedId = fromId.replaceAll("[^\\d+]", ""); // Limpia caracteres no deseados
            if (cleanedId.startsWith("+") && STRICT_PHONE_NUMBER_PATTERN.matcher(cleanedId).matches()) {
                normalizedPhoneForWhatsapp = cleanedId; // Ya viene correcto, ej. "+573123456789"
            } else if (cleanedId.matches("^\\d{10,15}$")) { 
                // Asumimos que es un número completo sin el '+' inicial. Para Colombia: "573227281752"
                normalizedPhoneForWhatsapp = "+" + cleanedId; // Se convierte en "+573227281752"
            }
            // Si no cumple estos patrones, normalizedPhoneForWhatsapp se queda vacío.
        }

        if (isNewUser) {
            System.out.println("ChatbotService: Nuevo usuario detectado: " + fromId);
            user = new User();
            user.setId(UUID.randomUUID().toString()); // Se usa como ID temporal para Telegram, o como campo para WhatsApp
            user.setCreated_at(Timestamp.now());
            user.setAceptaTerminos(false);

            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                user.setPhone_code("+57"); 
                user.setPhone(normalizedPhoneForWhatsapp); // Guarda el número normalizado con '+'
                
                // Antes de establecer el estado, intentar detectar el referido en el primer mensaje
                chatResponse = handleNewUserIntro(user, messageText); // <-- NUEVA LÓGICA PARA NUEVOS USUARIOS
                user.setChatbot_state(chatResponse.getNextChatbotState()); // Establecer el estado según el resultado de la detección
                saveUser(user); // Guarda el nuevo usuario (con el número de teléfono como ID del documento)
                
            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                user.setTelegram_chat_id(fromId);
                user.setChatbot_state("TELEGRAM_WAITING_PHONE_NUMBER");
                saveUser(user); // Guarda el nuevo usuario (con UUID como ID de documento temporal)
                chatResponse = new ChatResponse(
                    "¡Hola! 👋 Soy el bot de *Reset a la Política*. Para identificarte y continuar, por favor, envíame tu número de teléfono.",
                    "TELEGRAM_WAITING_PHONE_NUMBER"
                );
            } else {
                System.err.println("ChatbotService: Nuevo usuario de canal desconocido ('" + channelType + "'). No se pudo inicializar.");
                return "Lo siento, no puedo procesar tu solicitud desde este canal.";
            }
        } else {
            // Usuario existente encontrado. Ahora, verifica si necesitamos vincular un nuevo identificador.
            System.out.println("ChatbotService: Usuario existente. Estado actual: " + user.getChatbot_state() + ". ID del documento: " + (user.getPhone() != null ? user.getPhone().substring(1) : user.getId()));
            
            boolean userUpdated = false;
            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                // Si el usuario fue encontrado por Telegram ID (ej.) y el campo 'phone' está vacío o es diferente
                if (user.getPhone() == null || !user.getPhone().equals(normalizedPhoneForWhatsapp)) {
                    user.setPhone(normalizedPhoneForWhatsapp);
                    user.setPhone_code("+57"); 
                    userUpdated = true;
                    System.out.println("DEBUG: Actualizando número de teléfono de usuario existente: " + normalizedPhoneForWhatsapp);
                }
            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                // Si el usuario fue encontrado por número de teléfono (ej.) y el 'telegram_chat_id' está vacío o es diferente
                if (user.getTelegram_chat_id() == null || !user.getTelegram_chat_id().equals(fromId)) {
                    user.setTelegram_chat_id(fromId);
                    userUpdated = true;
                    System.out.println("DEBUG: Actualizando Telegram Chat ID de usuario existente: " + fromId);
                }
            }

            if (userUpdated) {
                // Guardar el objeto de usuario actualizado. saveUser gestionará el ID del documento.
                saveUser(user); 
            }

            chatResponse = handleExistingUserMessage(user, messageText);
        }

        if (chatResponse != null) {
            // Enviar el mensaje principal
            if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                watiApiService.sendWhatsAppMessage(fromId, chatResponse.getPrimaryMessage());
            } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                telegramApiService.sendTelegramMessage(fromId, chatResponse.getPrimaryMessage());
            } else {
                System.err.println("ChatbotService: Canal desconocido ('" + channelType + "'). No se pudo enviar el mensaje principal.");
            }

            // Enviar mensajes secundarios si existen
            chatResponse.getSecondaryMessage().ifPresent(secondaryMsg -> {
                // Si el mensaje secundario contiene separadores de mensajes (por ejemplo, "###SPLIT###"), divídelos y envíalos individualmente
                String[] messagesToSend = secondaryMsg.split("###SPLIT###");
                for (String msg : messagesToSend) {
                    msg = msg.trim();
                    if (!msg.isEmpty()) {
                        System.out.println("ChatbotService: Enviando mensaje secundario a " + fromId + " (Canal: " + channelType + "): '" + msg + "'");
                        if ("WHATSAPP".equalsIgnoreCase(channelType)) {
                            watiApiService.sendWhatsAppMessage(fromId, msg);
                        } else if ("TELEGRAM".equalsIgnoreCase(channelType)) {
                            telegramApiService.sendTelegramMessage(fromId, msg);
                        } else {
                            System.err.println("ChatbotService: Canal desconocido ('" + channelType + "'). No se pudo enviar el mensaje secundario.");
                        }
                    }
                }
            });

            user.setChatbot_state(chatResponse.getNextChatbotState());
            user.setUpdated_at(Timestamp.now());
            // Guarda los cambios de estado y datos. saveUser gestionará el ID del documento.
            saveUser(user); 

            return chatResponse.getPrimaryMessage();
        }
        return "ERROR: No se pudo generar una respuesta.";
    }

    /**
     * Maneja la lógica de inicio para nuevos usuarios.
     * Intenta detectar un código de referido y, si no, procede con la bienvenida general.
     * @param user El objeto User del nuevo usuario.
     * @param messageText El primer mensaje enviado por el usuario.
     * @return ChatResponse con el mensaje y el siguiente estado.
     */
    private ChatResponse handleNewUserIntro(User user, String messageText) {
        Matcher matcher = REFERRAL_MESSAGE_PATTERN.matcher(messageText.trim());

        if (matcher.matches()) {
            String incomingReferralCode = matcher.group(1);
            System.out.println("ChatbotService: Primer mensaje contiene posible código de referido: " + incomingReferralCode);
            Optional<User> referrerUser = getUserByReferralCode(incomingReferralCode);

            if (referrerUser.isPresent()) {
                user.setReferred_by_phone(referrerUser.get().getPhone());
                return new ChatResponse(
                    """
                    ¡Hola! 👋 Soy el bot de **Reset a la Política**.
                    ¡Qué emoción que te unas a esta ola de cambio para Colombia! Veo que vienes referido por un amigo.

                    Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                    Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                    Responde: Sí o No.
                    """,
                    "WAITING_TERMS_ACCEPTANCE"
                );
            } else {
                System.out.println("ChatbotService: Código de referido válido en formato, pero NO ENCONTRADO en el primer mensaje: " + incomingReferralCode);
                // Si el código no es válido, se procede con la bienvenida general
                return new ChatResponse(
                    """
                    ¡Hola! 👋 Soy el bot de **Reset a la Política**.
                    Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.
                    Parece que el código de referido que me enviaste no es válido, pero no te preocupes, ¡podemos continuar!

                    Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                    Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                    Responde: Sí o No.
                    """,
                    "WAITING_TERMS_ACCEPTANCE"
                );
            }
        } else {
            System.out.println("ChatbotService: Primer mensaje no contiene código de referido. Iniciando flujo general.");
            return new ChatResponse(
                """
                ¡Hola! 👋 Soy el bot de **Reset a la Política**.
                Te doy la bienvenida a este espacio de conversación, donde construimos juntos el futuro de Colombia.

                Para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?
                Responde: Sí o No.
                """,
                "WAITING_TERMS_ACCEPTANCE"
            );
        }
    }


    private ChatResponse handleExistingUserMessage(User user, String messageText) {
        messageText = messageText.trim(); 

        String currentChatbotState = user.getChatbot_state();
        String responseMessage = "";
        String nextChatbotState = currentChatbotState;
        Optional<String> secondaryMessage = Optional.empty(); // Ahora puede contener múltiples mensajes separados

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
                }
                else if (cleanedNumber.matches("^\\d{7,10}$")) { 
                    normalizedPhoneNumber = "+57" + cleanedNumber; 
                    user.setPhone_code("+57");
                    System.out.println("DEBUG: Telegram number normalized to +57: " + normalizedPhoneNumber);
                }
                else {
                    responseMessage = "Eso no parece un número de teléfono válido. Por favor, asegúrate de que sea un número real, incluyendo el código de país si lo tienes (ej. +573001234567).";
                    nextChatbotState = "TELEGRAM_WAITING_PHONE_NUMBER"; 
                    return new ChatResponse(responseMessage, nextChatbotState);
                }

                if (!STRICT_PHONE_NUMBER_PATTERN.matcher(normalizedPhoneNumber).matches()) {
                    responseMessage = "El formato de número de teléfono no es válido después de la normalización. Por favor, asegúrate de que sea un número real (ej. +573001234567).";
                    nextChatbotState = "TELEGRAM_WAITING_PHONE_NUMBER";
                    return new ChatResponse(responseMessage, nextChatbotState);
                }

                // *** LÓGICA DE DETECCIÓN Y FUSIÓN DE CUENTAS POR NÚMERO DE TELÉFONO ***
                Optional<User> existingUserByPhone = findUserByPhoneNumberField(normalizedPhoneNumber);

                if (existingUserByPhone.isPresent()) {
                    User foundUser = existingUserByPhone.get();
                    // Si el usuario ya existe por su número de teléfono (e.g., registro por WhatsApp)
                    // Y no es el mismo documento de usuario actual (es decir, el actual tiene un UUID temporal)
                    if (!foundUser.getId().equals(user.getId())) { // Compara los IDs de documento
                        System.out.println("DEBUG: Conflicto de usuario detectado. Número '" + normalizedPhoneNumber + "' ya registrado con ID de documento: " + (foundUser.getPhone() != null ? foundUser.getPhone().substring(1) : foundUser.getId()));
                        System.out.println("DEBUG: Usuario actual (Telegram inicial) ID de documento: " + user.getId() + " con chat_id: " + user.getTelegram_chat_id());

                        // Vincula el Telegram chat ID al usuario existente encontrado por número (si aún no lo tiene)
                        if (foundUser.getTelegram_chat_id() == null || !foundUser.getTelegram_chat_id().equals(user.getTelegram_chat_id())) {
                            foundUser.setTelegram_chat_id(user.getTelegram_chat_id());
                            System.out.println("DEBUG: Vinculando Telegram Chat ID " + user.getTelegram_chat_id() + " a usuario existente.");
                        }
                        
                        // Elimina el documento de usuario temporal que se creó para la interacción inicial de Telegram (el del UUID)
                        try {
                            firestore.collection("users").document(user.getId()).delete().get();
                            System.out.println("DEBUG: Documento temporal de Telegram (UUID: " + user.getId() + ") eliminado después de vincular.");
                        } catch (Exception e) {
                            System.err.println("ERROR al eliminar documento temporal de Telegram (UUID: " + user.getId() + "): " + e.getMessage());
                            e.printStackTrace();
                        }

                        // ¡IMPORTANTE! Asignamos el foundUser al 'user' actual para que el resto del flujo lo use y el 'saveUser' final lo actualice.
                        user = foundUser; 
                        
                        // Mensaje de que ya está registrado y se vinculó la cuenta
                        responseMessage = "¡Ya estás registrado con ese número! Hemos vinculado tu cuenta de Telegram a tu perfil existente. Puedes continuar.";
                        nextChatbotState = foundUser.getChatbot_state(); // Regresa al estado en el que estaba el usuario ya existente
                        return new ChatResponse(responseMessage, nextChatbotState);
                    }
                    // Si foundUser.getId().equals(user.getId()), significa que el usuario de Telegram está actualizando su propio número.
                    // Esto se maneja en el flujo normal de abajo.
                }

                user.setPhone(normalizedPhoneNumber); // Guarda el número normalizado
                user.setPhone_code(normalizedPhoneNumber.substring(0, Math.min(normalizedPhoneNumber.length(), 4))); // Set phone_code

                responseMessage = """
                        ¡Gracias! Hemos registrado tu número de teléfono.
                        Ahora, para seguir adelante y unirnos en esta gran tarea de transformación nacional, te invito a que revises nuestra política de tratamiento de datos, plasmadas aquí https://danielquinterocalle.com/privacidad. Si continuas esta conversación estás de acuerdo y aceptas los principios con los que manejamos la información.

                        Acompáñame hacia una Colombia más justa, equitativa y próspera para todos. ¿Aceptas el reto de resetear la política?

                        Responde: Sí o No.
                        """;
                nextChatbotState = "WAITING_TERMS_ACCEPTANCE";
                break;

            // ELIMINAMOS LOS ESTADOS "NEW_USER_INTRO" Y "WAITING_REFERRAL_RETRY_OR_PROCEED" DEL SWITCH
            // La lógica inicial de detección de referido se mueve a handleNewUserIntro()
            // Y si no hay referido, el flujo avanza directamente a WAITING_TERMS_ACCEPTANCE.

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
                    
                    // Se crea una lista para almacenar los mensajes secundarios
                    List<String> additionalMessages = new ArrayList<>();

                    try {
                        String whatsappRawReferralText = String.format("Hola, vengo referido por:%s", referralCode);
                        System.out.println("Texto crudo antes de codificar: '" + whatsappRawReferralText + "'");
                        // Codifica los espacios con '%20' para la URL de WhatsApp
                        String encodedWhatsappMessage = URLEncoder.encode(whatsappRawReferralText, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                        // NOTA: Se actualiza el número de WhatsApp para el enlace de invitación de WhatsApp.
                        whatsappInviteLink = "https://wa.me/573224029924?text=" + encodedWhatsappMessage;

                        String encodedTelegramPayload = URLEncoder.encode(referralCode, StandardCharsets.UTF_8.toString());
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=" + encodedTelegramPayload;

                        // **MENSAJE ADICIONAL 1: "Amigos, los invito..." (Este debe ser el primer secundario)**
                        String friendsInviteMessage = String.format(
                            "Amigos, los invito a unirse a la campaña de Daniel Quintero a la Presidencia: https://wa.me/573224029924?text=%s", // NOTA: También se actualiza aquí el número de WhatsApp.
                            URLEncoder.encode(String.format("Hola, vengo referido por:%s", referralCode), StandardCharsets.UTF_8.toString()).replace("+", "%20")
                        );
                        additionalMessages.add(friendsInviteMessage);

                        // **MENSAJE ADICIONAL 2: "¡Atención! Ahora entrarás..." (Este debe ser el segundo secundario)**
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
                        whatsappInviteLink = "https://wa.me/573224029924?text=Error%20al%20generar%20referido"; // Actualizado el número
                        telegramInviteLink = "https://t.me/" + TELEGRAM_BOT_USERNAME + "?start=Error";
                        additionalMessages.clear(); // Limpiar si hubo error para no enviar mensajes parciales
                        additionalMessages.add("Error al generar los mensajes de invitación."); // Mensaje de fallback
                    }

                    // MENSAJE PRINCIPAL: "Gracias por unirte..."
                    responseMessage = String.format(
                        """
                        %s, gracias por unirte a la ola de cambio que estamos construyendo para Colombia. Hasta ahora tienes 0 personas referidas. Ayudanos a crecer y gana puestos dentro de la campaña.
                        
                        Sabemos que muchos comparten la misma visión de un futuro mejor, y por eso quiero invitarte a que compartas este proyecto con tus amigos, familiares y conocidos. Juntos podemos lograr una transformación real y profunda.

                        Envíales el siguiente enlace de referido:
                        """,
                        user.getName()// El enlace de WhatsApp se incluye en el mensaje principal
                    );

                    // Unir los mensajes adicionales con un separador especial para enviarlos individualmente
                    secondaryMessage = Optional.of(String.join("###SPLIT###", additionalMessages));

                    nextChatbotState = "COMPLETED";
                } else {
                    responseMessage = "Por favor, vuelve a escribir tu nombre completo para corregir tus datos.";
                    nextChatbotState = "WAITING_NAME";
                }
                break;
            case "COMPLETED":
                System.out.println("ChatbotService: Usuario COMPLETED. Pasando consulta a AI Bot.");

                // Determina el ID de sesión más fiable. El teléfono es la clave para unificar canales.
                String sessionId = user.getPhone();

                // Fallback: Si el teléfono es nulo (estado inconsistente), pero tenemos un ID de Telegram, úsalo para no cortar la conversación.
                if ((sessionId == null || sessionId.isEmpty()) && user.getTelegram_chat_id() != null) {
                    System.err.println("ADVERTENCIA: Usuario COMPLETED sin teléfono. Usando Telegram Chat ID (" + user.getTelegram_chat_id() + ") como fallback para la sesión de IA. Doc ID: " + user.getId());
                    sessionId = user.getTelegram_chat_id();
                }

                if (sessionId != null && !sessionId.isEmpty()) {
                    responseMessage = aiBotService.getAIResponse(sessionId, messageText);
                    nextChatbotState = "COMPLETED";
                } else {
                    // Esto es un error crítico de datos. El usuario no tiene identificador.
                    System.err.println("ERROR CRÍTICO: Usuario COMPLETED sin un ID de sesión válido (ni teléfono, ni Telegram ID). Doc ID: " + user.getId());
                    responseMessage = "Lo siento, hemos encontrado un problema con tu registro y no puedo continuar la conversación. Por favor, contacta a soporte.";
                    nextChatbotState = "COMPLETED"; // Se mantiene en el estado, pero con un error.
                }
                break;
            default:
                // Si el usuario llega a un estado desconocido, lo redirigimos al inicio sin pedir "INICIAR"
                // e intentando la detección de referido si es un mensaje nuevo.
                System.out.println("ChatbotService: Usuario en estado desconocido ('" + currentChatbotState + "'). Redirigiendo al flujo de inicio.");
                return handleNewUserIntro(user, messageText); // Reutilizar la lógica de inicio
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
            System.err.println("ERROR al buscar usuario por campo 'phone' en Firestore (" + phoneNumber + "): " + e.getMessage());
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
            System.err.println("ERROR al buscar usuario por campo 'telegram_chat_id' en Firestore (" + telegramChatId + "): " + e.getMessage());
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
            System.err.println("ERROR al buscar usuario por ID de documento en Firestore (" + documentId + "): " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Unifica la búsqueda de usuario, intentando por número de teléfono o por chat ID de Telegram.
     * Esta es la función principal que debe usarse para encontrar un usuario existente.
     */
    private Optional<User> findUserByAnyIdentifier(String fromId, String channelType) {
        Optional<User> user = Optional.empty();

        String cleanedFromId = fromId.replaceAll("[^\\d+]", ""); 

        // 1. Intentar buscar por el campo 'phone'.
        String phoneNumberToSearch = "";
        
        if (cleanedFromId.startsWith("+") && STRICT_PHONE_NUMBER_PATTERN.matcher(cleanedFromId).matches()) {
            phoneNumberToSearch = cleanedFromId; // Ya viene correcto, ej. "+573123456789"
        } 
        else if (cleanedFromId.matches("^\\d{10,15}$")) { 
            phoneNumberToSearch = "+" + cleanedFromId; // Convierte "573227281752" a "+573227281752"
        }
        
        if (!phoneNumberToSearch.isEmpty() && STRICT_PHONE_NUMBER_PATTERN.matcher(phoneNumberToSearch).matches()) {
            user = findUserByPhoneNumberField(phoneNumberToSearch);
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por campo 'phone': " + phoneNumberToSearch);
                return user;
            }
        } else {
            System.out.println("DEBUG: FromId '" + fromId + "' normalizado a '" + phoneNumberToSearch + "' no es un formato de teléfono válido para búsqueda por 'phone'.");
        }


        // 2. Si no se encontró por número de teléfono, intentar buscar por el ID de documento (si el fromId coincide con un UUID o un número sin '+')
        // Esto es crucial para Telegram que guarda inicialmente por UUID, y para WhatsApp si el doc ID es solo el número.
        if (!user.isPresent()) {
            // Intentar buscar por fromId como ID de documento directamente
            user = findUserByDocumentId(fromId); 
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por ID de documento: " + fromId);
                return user;
            }
        }


        // 3. Si aún no se encontró, y el canal es Telegram, buscar por campo 'telegram_chat_id'.
        if (!user.isPresent() && "TELEGRAM".equalsIgnoreCase(channelType)) {
            user = findUserByTelegramChatIdField(fromId); // fromId para Telegram es el chat ID
            if (user.isPresent()) {
                System.out.println("DEBUG: Usuario encontrado por campo 'telegram_chat_id': " + fromId);
                return user;
            }
        }
        
        System.out.println("DEBUG: Usuario no encontrado por ningún identificador conocido para fromId: " + fromId + " en canal: " + channelType);
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
     * Determina el ID del documento basado en la existencia de un número de teléfono.
     * Si 'user.phone' está presente, usa el número de teléfono (sin '+') como ID del documento.
     * Si 'user.phone' no está presente, usa user.getId() (UUID) como ID del documento.
     */
    private void saveUser(User user) {
        String docIdToUse;
        String oldDocId = null; 

        if (user.getId() == null || user.getId().isEmpty()) {
            System.err.println("ERROR: Intentando guardar usuario, pero user.getId() es nulo/vacío. Generando un nuevo UUID y usando ese.");
            user.setId(UUID.randomUUID().toString()); 
        }

        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            // Elimina el '+' si está presente para usar el número como ID del documento
            docIdToUse = user.getPhone().startsWith("+") ? user.getPhone().substring(1) : user.getPhone();
            System.out.println("DEBUG: Guardando usuario con ID de documento (teléfono sin '+'): " + docIdToUse);

            // Si el ID del documento actual del objeto (user.getId()) no es el número de teléfono,
            // significa que el documento original fue guardado con un UUID (ej. usuario inicial de Telegram).
            // En este caso, necesitamos migrar el ID del documento.
            if (!docIdToUse.equals(user.getId())) { 
                oldDocId = user.getId(); // El UUID original que era el ID del documento.
                System.out.println("DEBUG: Detectada migración de ID de documento de UUID (" + oldDocId + ") a teléfono (" + docIdToUse + ").");
            }
        } else {
            docIdToUse = user.getId(); // Si no hay teléfono, usa el UUID como ID del documento.
            System.out.println("DEBUG: Guardando usuario con ID de documento (UUID): " + docIdToUse);
        }

        try {
            // Si hay un 'oldDocId' (UUID) que es diferente del nuevo docIdToUse (teléfono),
            // significa que estamos migrando el ID del documento.
            if (oldDocId != null) {
                // Leer el documento existente por el oldDocId para asegurar que tenemos la última versión,
                // luego eliminarlo antes de escribir el nuevo.
                // Aunque user ya es el objeto recuperado/actualizado, el delete/set debe ser atómico si posible.
                // Para simplificar, confiamos en que user tiene los datos correctos.
                firestore.collection("users").document(oldDocId).delete().get();
                System.out.println("DEBUG: Documento antiguo (UUID: " + oldDocId + ") eliminado exitosamente para migración.");
            }
            
            // Luego, creamos/actualizamos el documento con el nuevo ID (teléfono o UUID final)
            firestore.collection("users").document(docIdToUse).set(user).get();
            System.out.println("DEBUG: Usuario guardado/actualizado en Firestore con ID de documento: " + docIdToUse);
        } catch (Exception e) {
            System.err.println("ERROR al guardar/actualizar/migrar usuario en Firestore con ID " + docIdToUse + " (antiguo ID: " + oldDocId + "): " + e.getMessage());
            e.printStackTrace();
        }
    }


    private String generateUniqueReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}