package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generar tokens JWT para autenticación con el endpoint de analytics
 */
@Service
public class JWTService {
    
    @Value("${ANALYTICS_JWT_SECRET:your-secret-key}")
    private String jwtSecret;
    
    @Value("${ANALYTICS_JWT_EXPIRATION_MINUTES:60}")
    private int expirationMinutes;
    
    /**
     * Genera un token JWT para un usuario específico
     * 
     * @param userId ID del usuario (número de teléfono)
     * @return Token JWT como String
     */
    public String generateToken(String userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + (expirationMinutes * 60 * 1000));
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", userId);
        claims.put("type", "access");
        
        // Convertir el secret a bytes para evitar problemas con caracteres especiales
        byte[] secretBytes = jwtSecret.getBytes();
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(SignatureAlgorithm.HS256, secretBytes)
                .compact();
    }
    
    /**
     * Valida un token JWT
     * 
     * @param token Token JWT a validar
     * @return true si el token es válido
     */
    public boolean validateToken(String token) {
        try {
            byte[] secretBytes = jwtSecret.getBytes();
            Jwts.parser()
                .setSigningKey(secretBytes)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extrae el user_id de un token JWT
     * 
     * @param token Token JWT
     * @return user_id extraído del token
     */
    public String extractUserId(String token) {
        try {
            byte[] secretBytes = jwtSecret.getBytes();
            return Jwts.parser()
                .setSigningKey(secretBytes)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("user_id", String.class);
        } catch (Exception e) {
            return null;
        }
    }
} 