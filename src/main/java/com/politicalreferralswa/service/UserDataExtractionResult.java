package com.politicalreferralswa.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDataExtractionResult {
    private String name;
    private String lastname;
    private String city;
    private String state;
    private Boolean acceptsTerms;
    private String referredByPhone;
    private String referralCode;
    private Boolean correction;
    private String previousValue;
    private ClarificationNeeded needsClarification;
    private Double confidence;
    private String emotionalContext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClarificationNeeded {
        private String city;
        private String other;
    }

    /**
     * Crea un resultado vacío
     */
    public static UserDataExtractionResult empty() {
        return new UserDataExtractionResult(null, null, null, null, null, null, null, null, null, null, 0.0, null);
    }

    /**
     * Crea un resultado desde JSON
     */
    public static UserDataExtractionResult fromJson(JsonNode json) {
        UserDataExtractionResult result = new UserDataExtractionResult();
        
        result.setName(getStringOrNull(json, "name"));
        result.setLastname(getStringOrNull(json, "lastname"));
        result.setCity(getStringOrNull(json, "city"));
        result.setState(getStringOrNull(json, "state"));
        result.setAcceptsTerms(getBooleanOrNull(json, "acceptsTerms"));
        result.setReferredByPhone(getStringOrNull(json, "referredByPhone"));
        result.setReferralCode(getStringOrNull(json, "referralCode"));
        result.setCorrection(getBooleanOrNull(json, "correction"));
        result.setPreviousValue(getStringOrNull(json, "previousValue"));
        result.setEmotionalContext(getStringOrNull(json, "emotionalContext"));
        result.setConfidence(getDoubleOrNull(json, "confidence"));
        
        JsonNode clarificationNode = json.path("needsClarification");
        if (!clarificationNode.isMissingNode() && !clarificationNode.isNull()) {
            ClarificationNeeded clarification = new ClarificationNeeded();
            clarification.setCity(getStringOrNull(clarificationNode, "city"));
            clarification.setOther(getStringOrNull(clarificationNode, "other"));
            result.setNeedsClarification(clarification);
        }
        
        return result;
    }

    private static String getStringOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.path(field);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asText();
    }

    private static Boolean getBooleanOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.path(field);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asBoolean();
    }

    private static Double getDoubleOrNull(JsonNode node, String field) {
        JsonNode fieldNode = node.path(field);
        return fieldNode.isMissingNode() || fieldNode.isNull() ? null : fieldNode.asDouble();
    }

    /**
     * Verifica si la extracción fue exitosa
     */
    public boolean isSuccessful() {
        return confidence != null && confidence >= 0.3;
    }

    /**
     * Verifica si necesita aclaración
     */
    public boolean needsClarification() {
        return needsClarification != null && 
               (needsClarification.getCity() != null || needsClarification.getOther() != null);
    }

    /**
     * Obtiene el mensaje de aclaración
     */
    public String getClarificationMessage() {
        if (needsClarification == null) return null;
        
        if (needsClarification.getCity() != null) {
            return needsClarification.getCity();
        }
        
        return needsClarification.getOther();
    }
} 