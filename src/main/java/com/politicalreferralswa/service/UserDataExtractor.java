package com.politicalreferralswa.service;

import com.politicalreferralswa.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDataExtractor {

    private final GeminiService geminiService;

    public UserDataExtractor(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * Extrae datos de usuario de un mensaje y actualiza el usuario
     * 
     * @param user El usuario actual
     * @param messageText El mensaje del usuario
     * @param conversationHistory Historial de conversación (opcional)
     * @return ExtractionResult con el resultado de la extracción
     */
    public ExtractionResult extractAndUpdateUser(User user, String userMessage, String previousContext) {
        System.out.println("DEBUG EXTRACTOR: ===== LLAMADA A extractAndUpdateUser =====");
        System.out.println("DEBUG EXTRACTOR: Estado actual: " + user.getChatbot_state());
        System.out.println("DEBUG EXTRACTOR: Mensaje: '" + userMessage + "'");
        System.out.println("DEBUG EXTRACTOR: Usuario actual - Nombre: '" + user.getName() + "', Ciudad: '" + user.getCity() + "', AceptaTerminos: " + user.isAceptaTerminos());
        
        try {
            // Construir contexto de conversación previa si no se proporcionó
            String contextToUse = previousContext != null ? previousContext : "";
            
            // Extraer datos usando Gemini
            UserDataExtractionResult extraction = geminiService.extractUserData(userMessage, contextToUse);
            
            if (!extraction.isSuccessful()) {
                return ExtractionResult.failed("No se pudieron extraer datos del mensaje");
            }
            
            // Si necesita aclaración, SIEMPRE pedir aclaración primero
            if (extraction.needsClarification()) {
                // Solo usar datos extraídos si NO hay ambigüedad geográfica
                boolean hasGeographicAmbiguity = extraction.getNeedsClarification() != null && 
                                               extraction.getNeedsClarification().getCity() != null;
                
                if (!hasGeographicAmbiguity) {
                    // Si la aclaración no es geográfica, usar datos extraídos
                    boolean hasUsefulData = (extraction.getName() != null) || 
                                           (extraction.getAcceptsTerms() != null) ||
                                           (extraction.getReferredByPhone() != null);
                    
                    if (hasUsefulData) {
                        boolean userUpdated = updateUserWithExtractedData(user, extraction);
                        if (userUpdated) {
                            return determineNextState(user, extraction);
                        }
                    }
                }
                
                // Para ambigüedades geográficas, SIEMPRE pedir aclaración
                return ExtractionResult.needsClarification(extraction.getClarificationMessage());
            }
            
            // Actualizar usuario con los datos extraídos
            boolean userUpdated = updateUserWithExtractedData(user, extraction);
            
            if (userUpdated) {
                // Determinar el siguiente estado y mensaje
                return determineNextState(user, extraction);
            } else {
                return ExtractionResult.failed("No se pudieron actualizar los datos del usuario");
            }
            
        } catch (Exception e) {
            System.err.println("Error en extracción de datos: " + e.getMessage());
            e.printStackTrace();
            return ExtractionResult.failed("Error interno en la extracción de datos");
        }
    }

    private String buildConversationContext(List<String> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "";
        }
        
        // Tomar los últimos 5 mensajes para contexto
        int startIndex = Math.max(0, conversationHistory.size() - 5);
        List<String> recentMessages = conversationHistory.subList(startIndex, conversationHistory.size());
        
        return String.join(" | ", recentMessages);
    }

    private boolean updateUserWithExtractedData(User user, UserDataExtractionResult extraction) {
        boolean updated = false;
        
        // Manejar correcciones si se detectaron
        if (extraction.getCorrection() != null && extraction.getCorrection()) {
            System.out.println("UserDataExtractor: Corrección detectada - Campo: " + 
                             (extraction.getName() != null ? "name" : 
                              extraction.getCity() != null ? "city" : "unknown") + 
                             ", Valor anterior: " + extraction.getPreviousValue());
        }
        
        // Actualizar nombre si se extrajo (siempre actualizar si hay datos nuevos)
        if (extraction.getName() != null) {
            user.setName(extraction.getName());
            updated = true;
        }
        
        // Actualizar apellido si se extrajo
        if (extraction.getLastname() != null) {
            user.setLastname(extraction.getLastname());
            updated = true;
        }
        
        // Actualizar ciudad si se extrajo (siempre actualizar si hay datos nuevos)
        if (extraction.getCity() != null) {
            user.setCity(extraction.getCity());
            updated = true;
        }
        
        // Actualizar departamento/estado si se extrajo
        if (extraction.getState() != null) {
            user.setState(extraction.getState());
            updated = true;
        }
        
        // Actualizar aceptación de términos si se extrajo
        if (extraction.getAcceptsTerms() != null) {
            user.setAceptaTerminos(extraction.getAcceptsTerms());
            updated = true;
        }
        
        // Actualizar teléfono de referido si se extrajo
        if (extraction.getReferredByPhone() != null) {
            user.setReferred_by_phone(extraction.getReferredByPhone());
            updated = true;
        }
        
        // Actualizar código de referido si se extrajo
        if (extraction.getReferralCode() != null) {
            user.setReferred_by_code(extraction.getReferralCode());
            updated = true;
        }
        
        return updated;
    }

    private ExtractionResult determineNextState(User user, UserDataExtractionResult extraction) {
        // Verificar qué datos tenemos disponibles
        boolean hasName = user.getName() != null && !user.getName().isEmpty();
        boolean hasLastname = user.getLastname() != null && !user.getLastname().isEmpty();
        boolean hasCity = user.getCity() != null && !user.getCity().isEmpty();
        boolean hasState = user.getState() != null && !user.getState().isEmpty();
        boolean hasAcceptedTerms = user.isAceptaTerminos();
        
        // Crear mensaje base con contexto emocional si está disponible
        String emotionalPrefix = buildEmotionalMessage(extraction.getEmotionalContext());
        
        // Manejar correcciones con mensajes específicos
        if (extraction.getCorrection() != null && extraction.getCorrection()) {
            String correctionMessage = "";
            if (extraction.getName() != null && extraction.getPreviousValue() != null) {
                correctionMessage = "Perfecto, actualicé tu nombre de '" + extraction.getPreviousValue() + 
                                  "' a '" + extraction.getName() + "'. ";
            } else if (extraction.getCity() != null && extraction.getPreviousValue() != null) {
                correctionMessage = "Perfecto, actualicé tu ciudad de '" + extraction.getPreviousValue() + 
                                  "' a '" + extraction.getCity() + "'. ";
            }
            
            // Continuar con el flujo normal después de la corrección
            if (hasName && hasCity && hasAcceptedTerms) {
                user.setChatbot_state("COMPLETED_REGISTRATION");
                String displayName = hasName ? user.getName() : "";
                if (hasLastname) displayName += " " + user.getLastname();
                String displayLocation = hasCity ? user.getCity() : "";
                if (hasState) displayLocation += ", " + user.getState();
                return ExtractionResult.completed(correctionMessage + "¡Perfecto " + displayName + " de " + displayLocation + 
                    "! Tu registro está completo. Te enviaré los enlaces para compartir con tus amigos.");
            }
        }
        
        // Construir nombre completo para mostrar
        String fullName = hasName ? user.getName() : "";
        if (hasLastname) {
            fullName += (hasName ? " " : "") + user.getLastname();
        }
        
        // Construir ubicación completa
        String location = hasCity ? user.getCity() : "";
        if (hasState) {
            location += (hasCity ? ", " : "") + user.getState();
        }
        
        // Si tenemos todos los datos, completar el registro directamente
        if (hasName && hasCity && hasAcceptedTerms) {
            user.setChatbot_state("COMPLETED_REGISTRATION");
            String displayName = fullName.isEmpty() ? user.getName() : fullName;
            String displayLocation = location.isEmpty() ? user.getCity() : location;
            return ExtractionResult.completed("¡Perfecto " + displayName + " de " + displayLocation + 
                "! Tu registro está completo. Te enviaré los enlaces para compartir con tus amigos.");
        }
        
        // Casos parciales - usar datos ya extraídos de forma inteligente
        if (hasName && hasCity && !hasAcceptedTerms) {
            // Tiene nombre y ciudad, ir directo a términos
            user.setChatbot_state("WAITING_TERMS_ACCEPTANCE");
            String displayName = fullName.isEmpty() ? user.getName() : fullName;
            String displayLocation = location.isEmpty() ? user.getCity() : location;
            return ExtractionResult.incomplete(emotionalPrefix + "¡Perfecto " + displayName + " de " + displayLocation + 
                "! Para completar tu registro, confirma que aceptas nuestra política de privacidad: " +
                "https://danielquinterocalle.com/privacidad. ¿Aceptas? (Sí/No)");
        }
        
        if (hasName && !hasCity && hasAcceptedTerms) {
            // Tiene nombre y aceptó términos, falta ciudad
            user.setChatbot_state("WAITING_CITY");
            String displayName = fullName.isEmpty() ? user.getName() : fullName;
            return ExtractionResult.incomplete(emotionalPrefix + "¡Perfecto " + displayName + "! Ya aceptaste los términos. " +
                "¿En qué ciudad vives?");
        }
        
        if (!hasName && hasCity && hasAcceptedTerms) {
            // Tiene ciudad y aceptó términos, falta nombre
            user.setChatbot_state("WAITING_NAME");
            String displayLocation = location.isEmpty() ? user.getCity() : location;
            return ExtractionResult.incomplete("¡Excelente! Ya aceptaste los términos y veo que eres de " + displayLocation + 
                ". ¿Cuál es tu nombre?");
        }
        
        if (hasName && !hasCity && !hasAcceptedTerms) {
            // Solo tiene nombre
            user.setChatbot_state("WAITING_CITY");
            String displayName = fullName.isEmpty() ? user.getName() : fullName;
            return ExtractionResult.incomplete(emotionalPrefix + "¡Hola " + displayName + "! ¿En qué ciudad vives?");
        }
        
        if (!hasName && hasCity && !hasAcceptedTerms) {
            // Solo tiene ciudad
            user.setChatbot_state("WAITING_NAME");
            String displayLocation = location.isEmpty() ? user.getCity() : location;
            return ExtractionResult.incomplete(emotionalPrefix + "¡Hola! Veo que eres de " + displayLocation + ". ¿Cuál es tu nombre?");
        }
        
        if (!hasName && !hasCity && hasAcceptedTerms) {
            // Solo aceptó términos
            System.err.println("DEBUG EXTRACTOR: PROBLEMA DETECTADO - Usuario solo tiene términos aceptados");
            System.err.println("DEBUG EXTRACTOR: hasName=" + hasName + ", hasCity=" + hasCity + ", hasAcceptedTerms=" + hasAcceptedTerms);
            System.err.println("DEBUG EXTRACTOR: Nombre: '" + user.getName() + "', Ciudad: '" + user.getCity() + "'");
            System.err.println("DEBUG EXTRACTOR: ENVIANDO DE VUELTA A WAITING_NAME - POSIBLE CAUSA DEL CICLO");
            user.setChatbot_state("WAITING_NAME");
            return ExtractionResult.incomplete("¡Perfecto! Ya aceptaste los términos. ¿Cuál es tu nombre?");
        }
        
        // Caso por defecto - no tiene datos, empezar por nombre
        System.out.println("DEBUG EXTRACTOR: Caso por defecto - enviando a WAITING_NAME");
        System.out.println("DEBUG EXTRACTOR: hasName=" + hasName + ", hasCity=" + hasCity + ", hasAcceptedTerms=" + hasAcceptedTerms);
        user.setChatbot_state("WAITING_NAME");
        return ExtractionResult.incomplete(emotionalPrefix + "Para continuar con tu registro, necesito algunos datos. ¿Cuál es tu nombre?");
    }

    /**
     * Construye un mensaje empático basado en el contexto emocional detectado
     */
    private String buildEmotionalMessage(String emotionalContext) {
        if (emotionalContext != null && !emotionalContext.trim().isEmpty()) {
            return emotionalContext + " ";
        }
        return "";
    }

    /**
     * Resultado de la extracción de datos
     */
    public static class ExtractionResult {
        private final boolean success;
        private final String message;
        private final String nextState;
        private final boolean needsClarification;
        private final boolean isCompleted;

        private ExtractionResult(boolean success, String message, String nextState, 
                               boolean needsClarification, boolean isCompleted) {
            this.success = success;
            this.message = message;
            this.nextState = nextState;
            this.needsClarification = needsClarification;
            this.isCompleted = isCompleted;
        }

        public static ExtractionResult completed(String message) {
            return new ExtractionResult(true, message, "COMPLETED_REGISTRATION", false, true);
        }

        public static ExtractionResult incomplete(String message) {
            return new ExtractionResult(true, message, null, false, false);
        }

        public static ExtractionResult needsClarification(String message) {
            return new ExtractionResult(true, message, null, true, false);
        }

        public static ExtractionResult failed(String message) {
            return new ExtractionResult(false, message, null, false, false);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getNextState() { return nextState; }
        public boolean needsClarification() { return needsClarification; }
        public boolean isCompleted() { return isCompleted; }
    }
} 