package com.politicalreferralswa.controllers;

import com.politicalreferralswa.service.ChatbotService;
import com.politicalreferralswa.service.TelegramApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Controlador REST para recibir los webhooks de mensajes entrantes de Telegram.
 * La URL configurada en Telegram debe coincidir con el valor de @PostMapping.
 */
@RestController
public class TelegramWebhookController {

    private final ChatbotService chatbotService;
    private final TelegramApiService telegramApiService; // Mantenemos la inyección si se usa en otros lugares, aunque no para el envío principal aquí

    @Value("${telegram.bot.username}")
    private String botUsername; // El username del bot

    @Autowired
    public TelegramWebhookController(ChatbotService chatbotService, TelegramApiService telegramApiService) {
        this.chatbotService = chatbotService;
        this.telegramApiService = telegramApiService;
    }

    /**
     * Endpoint POST para recibir actualizaciones (mensajes) de Telegram.
     * Este es el webhook principal que Telegram llamará.
     *
     * @param update El objeto Update de Telegram que contiene la información del mensaje.
     * @return String "OK" para confirmar la recepción a Telegram.
     */
    @PostMapping("/telegram_webhook")
    public String onUpdateReceived(@RequestBody Update update) {
        System.out.println("\n--- TELEGRAM WEBHOOK RECIBIDO (POST) ---");
        // System.out.println("Payload completo (RAW): " + update.toString()); // Descomentar para depurar el JSON de Telegram

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            System.out.println("TelegramController: Mensaje de Telegram. De: " + chatId + ", Contenido: '" + messageText + "'");

            // Llama al ChatbotService para procesar el mensaje.
            // El ChatbotService ya se encarga de enviar la respuesta al canal adecuado.
            String botResponse = chatbotService.processIncomingMessage(chatId, messageText, "TELEGRAM");

            System.out.println("TelegramController: Respuesta del bot procesada: '" + botResponse + "'");

            // <<--- ¡¡¡ELIMINA O COMENTA LA SIGUIENTE LÍNEA!!! ---
            // telegramApiService.sendTelegramMessage(chatId, botResponse); // <--- ¡¡¡ESTA ES LA LLAMADA DUPLICADA!!!

            return "OK"; // Telegram espera un String "OK" como respuesta exitosa
        }
        return "NOT_PROCESSED";
    }

    /**
     * Endpoint GET para la configuración inicial del webhook en Telegram.
     * Telegram lo llama para verificar que la URL es válida.
     *
     * @return String "OK" para confirmar la validación del webhook.
     */
    @GetMapping("/telegram_webhook")
    public String verifyTelegramWebhook() {
        System.out.println("TelegramController: Recibida petición GET a /telegram_webhook. Retornando OK.");
        return "OK";
    }
}