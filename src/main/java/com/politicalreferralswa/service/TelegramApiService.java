package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class TelegramApiService extends DefaultAbsSender {

    @Value("${telegram.bot.token}")
    private String botToken;

    public TelegramApiService() {
        super(new DefaultBotOptions());
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    public void sendTelegramMessage(String chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId); // El ID del chat del usuario en Telegram
        message.setText(messageText);

        try {
            execute(message); // Usa el método execute de DefaultAbsSender
            System.out.println("TelegramApiService: Mensaje enviado exitosamente a chat ID " + chatId);
        } catch (TelegramApiException e) {
            System.err.println("TelegramApiService: ERROR al enviar mensaje a Telegram: " + e.getMessage());
            e.printStackTrace();
        }
    }
}