package com.politicalreferralswa.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotación para marcar endpoints que requieren autenticación por API Key
 * Los endpoints marcados con esta anotación deben incluir el header 'X-API-Key'
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresApiKey {
    
    /**
     * Descripción del endpoint protegido (opcional)
     */
    String value() default "";
    
    /**
     * Si es true, requiere que la API key sea válida
     */
    boolean required() default true;
}
