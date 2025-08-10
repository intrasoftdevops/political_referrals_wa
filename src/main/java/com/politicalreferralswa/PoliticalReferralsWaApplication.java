package com.politicalreferralswa;

import com.politicalreferralswa.service.ChatbotService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PoliticalReferralsWaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoliticalReferralsWaApplication.class, args);
    }

    /**
     * Este bean se ejecuta una vez que la aplicación Spring Boot ha arrancado completamente.
     * Sirve para realizar tareas de inicialización, como la creación de datos de prueba en la DB.
     *
     * ¡IMPORTANTE! Comenta o elimina este bloque para producción
     * después de haber creado el usuario de prueba exitosamente.
     */
    @Bean
    public CommandLineRunner commandLineRunner(ChatbotService chatbotService) {
        return args -> {
            System.out.println("Ejecutando lógica de inicio de aplicación para crear usuario de ejemplo...");
            // Llama al método para crear el usuario de prueba.
            // El método en ChatbotService ya verifica si el usuario existe para evitar duplicados.
            chatbotService.createTestReferrerUser();
            System.out.println("Lógica de inicio completada. Usuario de prueba de referido creado (si no existía).");
        };
    }
}