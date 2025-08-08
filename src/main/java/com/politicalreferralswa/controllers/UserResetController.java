package com.politicalreferralswa.controllers;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/admin")
public class UserResetController {

    @Autowired
    private Firestore firestore;

    /**
     * Endpoint para resetear el estado de un usuario (delete lógico)
     * Permite que el usuario vuelva a probar el flujo completo del chatbot
     * 
     * @param phoneNumber Número de teléfono del usuario a resetear (con o sin +)
     * @return Respuesta con el resultado de la operación
     */
    @PostMapping("/reset-user/{phoneNumber}")
    public ResponseEntity<Map<String, Object>> resetUser(@PathVariable String phoneNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Normalizar el número de teléfono
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            String documentId = normalizedPhone.substring(1); // Remover el '+'
            
            // Buscar el usuario en Firestore
            DocumentReference userRef = firestore.collection("users").document(documentId);
            DocumentSnapshot userSnapshot = userRef.get().get();
            
            if (!userSnapshot.exists()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado con el número: " + phoneNumber);
                response.put("phone", phoneNumber);
                return ResponseEntity.notFound().build();
            }
            
            // Obtener datos actuales del usuario
            Map<String, Object> userData = userSnapshot.getData();
            String currentName = (String) userData.get("name");
            String currentCity = (String) userData.get("city");
            
            // Resetear campos del chatbot manteniendo datos básicos
            Map<String, Object> resetData = new HashMap<>();
            resetData.put("chatbot_state", "NEW"); // Volver al estado inicial
            resetData.put("aceptaTerminos", false); // Resetear términos
            resetData.put("updated_at", Timestamp.now());
            
            // Mantener datos básicos si existen
            if (currentName != null) {
                resetData.put("name", currentName);
            }
            if (currentCity != null) {
                resetData.put("city", currentCity);
            }
            
            // Actualizar en Firestore
            WriteResult result = userRef.update(resetData).get();
            
            response.put("success", true);
            response.put("message", "Usuario reseteado exitosamente. Puede volver a probar el flujo completo.");
            response.put("phone", normalizedPhone);
            response.put("documentId", documentId);
            response.put("resetFields", resetData.keySet());
            response.put("timestamp", result.getUpdateTime().toString());
            
            if (currentName != null) {
                response.put("userName", currentName);
            }
            if (currentCity != null) {
                response.put("userCity", currentCity);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (InterruptedException | ExecutionException e) {
            response.put("success", false);
            response.put("message", "Error al resetear usuario: " + e.getMessage());
            response.put("phone", phoneNumber);
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            response.put("phone", phoneNumber);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para obtener el estado actual de un usuario
     * Útil para verificar antes y después del reset
     * 
     * @param phoneNumber Número de teléfono del usuario
     * @return Estado actual del usuario
     */
    @GetMapping("/user-status/{phoneNumber}")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable String phoneNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Normalizar el número de teléfono
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            String documentId = normalizedPhone.substring(1); // Remover el '+'
            
            // Buscar el usuario en Firestore
            DocumentReference userRef = firestore.collection("users").document(documentId);
            DocumentSnapshot userSnapshot = userRef.get().get();
            
            if (!userSnapshot.exists()) {
                response.put("success", false);
                response.put("message", "Usuario no encontrado con el número: " + phoneNumber);
                response.put("phone", phoneNumber);
                return ResponseEntity.notFound().build();
            }
            
            // Obtener datos del usuario
            Map<String, Object> userData = userSnapshot.getData();
            
            response.put("success", true);
            response.put("phone", normalizedPhone);
            response.put("documentId", documentId);
            response.put("chatbot_state", userData.get("chatbot_state"));
            response.put("aceptaTerminos", userData.get("aceptaTerminos"));
            response.put("name", userData.get("name"));
            response.put("city", userData.get("city"));
            response.put("created_at", userData.get("created_at"));
            response.put("updated_at", userData.get("updated_at"));
            response.put("referral_code", userData.get("referral_code"));
            
            return ResponseEntity.ok(response);
            
        } catch (InterruptedException | ExecutionException e) {
            response.put("success", false);
            response.put("message", "Error al obtener estado del usuario: " + e.getMessage());
            response.put("phone", phoneNumber);
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            response.put("phone", phoneNumber);
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint simplificado para reset rápido - solo requiere el número
     * 
     * @param phoneNumber Número de teléfono del usuario a resetear
     * @return Respuesta simple del reset
     */
    @DeleteMapping("/reset/{phoneNumber}")
    public ResponseEntity<Map<String, Object>> quickReset(@PathVariable String phoneNumber) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            String documentId = normalizedPhone.substring(1);
            
            DocumentReference userRef = firestore.collection("users").document(documentId);
            DocumentSnapshot userSnapshot = userRef.get().get();
            
            if (!userSnapshot.exists()) {
                response.put("success", false);
                response.put("message", "❌ Usuario no encontrado: " + phoneNumber);
                return ResponseEntity.notFound().build();
            }
            
            // Reset simple - solo los campos críticos del chatbot
            Map<String, Object> resetData = new HashMap<>();
            resetData.put("chatbot_state", "NEW");
            resetData.put("aceptaTerminos", false);
            resetData.put("updated_at", Timestamp.now());
            
            userRef.update(resetData).get();
            
            response.put("success", true);
            response.put("message", "✅ Usuario reseteado: " + normalizedPhone);
            response.put("info", "El usuario puede volver a probar el flujo completo del chatbot");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para listar todos los usuarios y sus estados
     * Útil para administración y debugging
     * 
     * @return Lista de usuarios con información básica
     */
    @GetMapping("/users-list")
    public ResponseEntity<Map<String, Object>> getUsersList() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            var querySnapshot = firestore.collection("users").get().get();
            var users = new java.util.ArrayList<Map<String, Object>>();
            
            querySnapshot.getDocuments().forEach(doc -> {
                Map<String, Object> userData = doc.getData();
                Map<String, Object> userInfo = new HashMap<>();
                
                userInfo.put("documentId", doc.getId());
                userInfo.put("phone", userData.get("phone"));
                userInfo.put("name", userData.get("name"));
                userInfo.put("city", userData.get("city"));
                userInfo.put("chatbot_state", userData.get("chatbot_state"));
                userInfo.put("aceptaTerminos", userData.get("aceptaTerminos"));
                userInfo.put("created_at", userData.get("created_at"));
                userInfo.put("updated_at", userData.get("updated_at"));
                
                users.add(userInfo);
            });
            
            response.put("success", true);
            response.put("message", "Lista de usuarios obtenida exitosamente");
            response.put("totalUsers", users.size());
            response.put("users", users);
            
            return ResponseEntity.ok(response);
            
        } catch (InterruptedException | ExecutionException e) {
            response.put("success", false);
            response.put("message", "Error al obtener lista de usuarios: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error inesperado: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Normaliza el número de teléfono agregando el código de país si no existe
     * 
     * @param phoneNumber Número de teléfono a normalizar
     * @return Número normalizado con formato +57XXXXXXXXXX
     */
    private String normalizePhoneNumber(String phoneNumber) {
        // Limpiar el número
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        
        // Si ya tiene el código de país (+57)
        if (phoneNumber.startsWith("+57")) {
            return phoneNumber;
        }
        
        // Si tiene 13 dígitos (57XXXXXXXXXX)
        if (cleaned.length() == 12 && cleaned.startsWith("57")) {
            return "+" + cleaned;
        }
        
        // Si tiene 10 dígitos (XXXXXXXXXX)
        if (cleaned.length() == 10) {
            return "+57" + cleaned;
        }
        
        // Si no se puede normalizar, retornar como vino
        return phoneNumber.startsWith("+") ? phoneNumber : "+" + phoneNumber;
    }
}