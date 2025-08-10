package com.politicalreferralswa.service;

import java.util.Optional; // Importar Optional

// Clase simple para encapsular la respuesta del chatbot
public class ChatResponse {
    private final String primaryMessage; // El mensaje principal a enviar
    private final String nextChatbotState; // El siguiente estado del chatbot
    private final Optional<String> secondaryMessage; // Un mensaje secundario opcional (como el intro de IA)

    public ChatResponse(String primaryMessage, String nextChatbotState) {
        this(primaryMessage, nextChatbotState, Optional.empty());
    }

    public ChatResponse(String primaryMessage, String nextChatbotState, Optional<String> secondaryMessage) {
        this.primaryMessage = primaryMessage;
        this.nextChatbotState = nextChatbotState;
        this.secondaryMessage = secondaryMessage;
    }

    public String getPrimaryMessage() {
        return primaryMessage;
    }

    public String getNextChatbotState() {
        return nextChatbotState;
    }

    public Optional<String> getSecondaryMessage() {
        return secondaryMessage;
    }
}