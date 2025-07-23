package com.politicalreferralswa.controllers;

import com.politicalreferralswa.service.ChatbotService;
import com.politicalreferralswa.service.TelegramApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.telegram.telegrambots.meta.api.objects.Update; 

@RestController
public class TelegramWebhookController {

    private final ChatbotService chatbotService;
    private final TelegramApiService telegramApiService;

    @Value("${telegram.bot.username}")
    private String botUsername; // El username del bot

    @Autowired
    public TelegramWebhookController(ChatbotService chatbotService, TelegramApiService telegramApiService) {
        this.chatbotService = chatbotService;
        this.telegramApiService = telegramApiService;
    }

    // Endpoint POST para recibir actualizaciones (mensajes) de Telegram
    @PostMapping("/telegram_webhook") 
    public String onUpdateReceived(@RequestBody Update update) {
        System.out.println("\n--- TELEGRAM WEBHOOK RECIBIDO (POST) ---");
        // System.out.println("Payload completo (RAW): " + update.toString()); // Descomentar para depurar

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString(); 

            System.out.println("TelegramController: Mensaje de Telegram. De: " + chatId + ", Contenido: '" + messageText + "'");

            // Llama al ChatbotService para procesar el mensaje
            // En este punto, tu ChatbotService puede ser llamado con el chatId como phone_number
            String botResponse = chatbotService.processIncomingMessage(chatId, messageText);

            System.out.println("TelegramController: Respuesta del bot procesada: '" + botResponse + "'");

            // Envía la respuesta de vuelta a Telegram
            telegramApiService.sendTelegramMessage(chatId, botResponse);

            return "OK"; // Telegram espera un String "OK" como respuesta exitosa
        }
        return "NOT_PROCESSED"; // Si no es un mensaje de texto, no lo procesamos
    }

    // Endpoint GET para la configuración inicial del webhook en Telegram (opcional)
    // Telegram lo llama para verificar que la URL es válida.
    @GetMapping("/telegram_webhook")
    public String verifyTelegramWebhook() {
        System.out.println("TelegramController: Recibida petición GET a /telegram_webhook. Retornando OK.");
        return "OK"; 
    }
}