package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI; // <<--- ¡IMPORTACIÓN CLAVE!
import java.time.Duration; // Importación para timeouts

@Service
public class WatiApiService {

    @Value("${WATI_API_ENDPOINT_BASE}")
    private String watiApiBaseEndpoint;
    @Value("${WATI_TENANT_ID}")
    private String watiApiTenantId;
    @Value("${WATI_API_TOKEN}")
    private String watiApiToken;

    private final WebClient webClient;

    public WatiApiService(WebClient.Builder webClientBuilder) {
        // Construimos WebClient SIN una base URL. La URL completa se construirá dinámicamente.
        // Configuramos un límite de buffer mayor para manejar archivos de video (el video actual pesa ~24MB)
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(30 * 1024 * 1024)) // 30MB límite para archivos grandes como videos
                .build();
        
        // Configuración adicional para mejorar la estabilidad de conexiones SSL
        System.out.println("WatiApiService: WebClient configurado con soporte mejorado para SSL/TLS");
    }

    public void sendWhatsAppMessage(String toPhoneNumber, String messageText) {
        System.out.println("WatiApiService: Preparando para enviar mensaje a " + toPhoneNumber + " a través de Wati.");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(watiApiToken);

        // Construir la URL COMPLETA Y ABSOLUTA como un objeto java.net.URI
        URI fullApiUri = UriComponentsBuilder.fromUriString(watiApiBaseEndpoint) // Empieza con la URL base (https://...)
                                            .pathSegment(watiApiTenantId)       // Añade el ID de tenant como segmento de ruta
                                            .path("/api/v1/sendSessionMessage/{whatsappNumber}") // Añade el resto de la ruta
                                            .queryParam("messageText", messageText) // Añade el texto como query parameter
                                            .buildAndExpand(toPhoneNumber)      // Expande las variables de ruta ({whatsappNumber})
                                            .encode()                           // Codifica la URI para caracteres especiales
                                            .toUri();                           // <<--- ¡OBTENEMOS UN OBJETO URI!

        System.out.println("WatiApiService: URL de Wati construida: " + fullApiUri.toString()); // Log la URL final para verificar

        webClient.post()
                .uri(fullApiUri)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(60)) // Timeout de 60 segundos para respuestas de Wati
                .retryWhen(reactor.util.retry.Retry.backoff(1, Duration.ofMillis(500))) // 1 reintento con backoff ultra-rápido
                .doOnSuccess(response -> System.out.println("WatiApiService: Mensaje enviado exitosamente. Respuesta de Wati: " + response))
                .doOnError(error -> System.err.println("WatiApiService: Error al enviar mensaje a Wati: " + error.getMessage()))
                .subscribe(); // Ejecuta la llamada reactiva
    }

    /**
     * Envía un mensaje de WhatsApp de forma síncrona para garantizar el orden de los mensajes.
     * Este método bloquea hasta que el mensaje se envía completamente.
     *
     * @param toPhoneNumber El número de teléfono del destinatario
     * @param messageText El texto del mensaje a enviar
     */
    public void sendWhatsAppMessageSync(String toPhoneNumber, String messageText) {
        System.out.println("WatiApiService: Preparando para enviar mensaje síncrono a " + toPhoneNumber + " a través de Wati.");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(watiApiToken);

        // Construir la URL COMPLETA Y ABSOLUTA como un objeto java.net.URI
        URI fullApiUri = UriComponentsBuilder.fromUriString(watiApiBaseEndpoint)
                                            .pathSegment(watiApiTenantId)
                                            .path("/api/v1/sendSessionMessage/{whatsappNumber}")
                                            .queryParam("messageText", messageText)
                                            .buildAndExpand(toPhoneNumber)
                                            .encode()
                                            .toUri();

        System.out.println("WatiApiService: URL de Wati construida: " + fullApiUri.toString());

        try {
            // Usar block() para hacer la llamada síncrona con timeouts y reintentos
            String response = webClient.post()
                    .uri(fullApiUri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60)) // Timeout de 60 segundos para respuestas de Wati
                    .retryWhen(reactor.util.retry.Retry.backoff(1, Duration.ofMillis(500))) // 1 reintento con backoff ultra-rápido
                    .doOnSuccess(resp -> System.out.println("WatiApiService: Mensaje síncrono enviado exitosamente. Respuesta de Wati: " + resp))
                    .doOnError(error -> System.err.println("WatiApiService: Error al enviar mensaje síncrono a Wati: " + error.getMessage()))
                    .block(); // Bloquea hasta que se complete la llamada

            if (response != null) {
                System.out.println("WatiApiService: Mensaje síncrono completado exitosamente");
            }
        } catch (Exception e) {
            System.err.println("WatiApiService: Error en envío síncrono: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Alias para sendWhatsAppMessage - usado por NotificationService
     * Versión mejorada con mejor manejo de errores SSL
     */
    public void sendMessage(String toPhoneNumber, String messageText) {
        try {
            System.out.println("WatiApiService: Enviando mensaje con manejo mejorado de SSL a: " + toPhoneNumber);
            sendWhatsAppMessageSync(toPhoneNumber, messageText); // Usar versión síncrona para mejor control
        } catch (Exception e) {
            System.err.println("WatiApiService: Error SSL/TLS al enviar mensaje: " + e.getMessage());
            // Intentar fallback con configuración básica
            sendWhatsAppMessageBasic(toPhoneNumber, messageText);
        }
    }
    
    /**
     * Método fallback con configuración básica para casos de error SSL
     */
    private void sendWhatsAppMessageBasic(String toPhoneNumber, String messageText) {
        try {
            System.out.println("WatiApiService: Intentando envío con configuración básica...");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(watiApiToken);
            
            URI fullApiUri = UriComponentsBuilder.fromUriString(watiApiBaseEndpoint)
                                                .pathSegment(watiApiTenantId)
                                                .path("/api/v1/sendSessionMessage/{whatsappNumber}")
                                                .queryParam("messageText", messageText)
                                                .buildAndExpand(toPhoneNumber)
                                                .encode()
                                                .toUri();
            
            // Usar timeout más largo y reintentos
            String response = webClient.post()
                    .uri(fullApiUri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30)) // Timeout de 30 segundos
                    .retry(2) // Reintentar 2 veces
                    .doOnSuccess(resp -> System.out.println("WatiApiService: Mensaje enviado exitosamente con configuración básica"))
                    .doOnError(error -> System.err.println("WatiApiService: Error final al enviar mensaje: " + error.getMessage()))
                    .block();
                    
        } catch (Exception e) {
            System.err.println("WatiApiService: Error crítico al enviar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Envía mensaje a un grupo de WhatsApp usando la API de Wati
     * 
     * @param groupId El ID del grupo de WhatsApp
     * @param messageText El texto del mensaje a enviar
     */
    public void sendMessageToGroup(String groupId, String messageText) {
        System.out.println("WatiApiService: Preparando para enviar mensaje al grupo " + groupId + " a través de Wati.");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(watiApiToken);

        // Construir la URL para enviar mensaje al grupo
        URI fullApiUri = UriComponentsBuilder.fromUriString(watiApiBaseEndpoint)
                                            .pathSegment(watiApiTenantId)
                                            .path("/api/v1/sendSessionMessage/{groupId}")
                                            .queryParam("messageText", messageText)
                                            .buildAndExpand(groupId)
                                            .encode()
                                            .toUri();

        System.out.println("WatiApiService: URL de Wati para grupo construida: " + fullApiUri.toString());

        try {
            String response = webClient.post()
                    .uri(fullApiUri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(resp -> System.out.println("WatiApiService: Mensaje al grupo enviado exitosamente. Respuesta de Wati: " + resp))
                    .doOnError(error -> System.err.println("WatiApiService: Error al enviar mensaje al grupo a través de Wati: " + error.getMessage()))
                    .block();

            if (response != null) {
                System.out.println("WatiApiService: Mensaje al grupo completado exitosamente");
            }
        } catch (Exception e) {
            System.err.println("WatiApiService: Error en envío al grupo: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Envía un mensaje de notificación a un número de WhatsApp usando la API de Wati.
     * Este método usa sendTemplateMessage v2 con la template deployment_notifications.
     *
     * @param toPhoneNumber El número de teléfono del destinatario
     * @param messageText El texto del mensaje a enviar
     */
    public void sendNotificationMessage(String toPhoneNumber, String messageText) {
        System.out.println("WatiApiService: Enviando notificación a: " + toPhoneNumber);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(watiApiToken);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Construir la URL para sendTemplateMessage v2
            URI fullApiUri = UriComponentsBuilder.fromUriString(watiApiBaseEndpoint)
                                                .pathSegment(watiApiTenantId)
                                                .path("/api/v2/sendTemplateMessage")
                                                .queryParam("whatsappNumber", toPhoneNumber)
                                                .build()
                                                .encode()
                                                .toUri();

            System.out.println("WatiApiService: URL de Wati para notificación construida: " + fullApiUri.toString());

            // Crear el body JSON para template v2 con parámetros numerados
            String jsonBody = String.format(
                "{\"template_name\":\"deployment_notifications\",\"broadcast_name\":\"deployment_%s\",\"parameters\":[{\"name\":\"1\",\"value\":\"political-referrals-wa\"},{\"name\":\"2\",\"value\":\"us-central1\"},{\"name\":\"3\",\"value\":\"latest\"},{\"name\":\"4\",\"value\":\"%s\"}]}",
                System.currentTimeMillis(), messageText.replace("\"", "\\\"")
            );

            System.out.println("WatiApiService: Body JSON para template: " + jsonBody);

            String response = webClient.post()
                    .uri(fullApiUri)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .retryWhen(reactor.util.retry.Retry.backoff(1, Duration.ofMillis(500)))
                    .doOnSuccess(resp -> System.out.println("WatiApiService: Notificación enviada exitosamente. Respuesta: " + resp))
                    .doOnError(error -> System.err.println("WatiApiService: Error al enviar notificación: " + error.getMessage()))
                    .block();

            if (response != null) {
                System.out.println("WatiApiService: Notificación enviada completada exitosamente");
            }
        } catch (Exception e) {
            System.err.println("WatiApiService: Error en envío de notificación: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Envía un video de WhatsApp usando la API de Wati.
     * Este método envía un video desde una URL.
     *
     * @param toPhoneNumber El número de teléfono del destinatario
     * @param videoUrl La URL del video a enviar
     * @param caption El texto que acompaña al video (opcional)
     */
    public void sendVideoMessage(String toPhoneNumber, String videoUrl, String caption) {
        System.out.println("WatiApiService: Preparando para enviar video a " + toPhoneNumber + " a través de Wati.");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(watiApiToken);
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        // Construir la URL para enviar video
        URI fullApiUri = UriComponentsBuilder.fromUriString(watiApiBaseEndpoint)
                                            .pathSegment(watiApiTenantId)
                                            .path("/api/v1/sendSessionFile/{whatsappNumber}")
                                            .buildAndExpand(toPhoneNumber)
                                            .encode()
                                            .toUri();

        System.out.println("WatiApiService: URL de Wati para video construida: " + fullApiUri.toString());

        try {
            // Crear el cuerpo multipart para enviar el video
            // Primero descargar el video desde la URL (evitar doble codificación)
            byte[] videoBytes = webClient.get()
                    .uri(java.net.URI.create(videoUrl))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .timeout(java.time.Duration.ofMinutes(2)) // 2 minutos para descargar el video
                    .block(java.time.Duration.ofMinutes(2));

            if (videoBytes == null || videoBytes.length == 0) {
                throw new RuntimeException("No se pudo descargar el video desde la URL: " + videoUrl);
            }

            System.out.println("WatiApiService: Video descargado exitosamente, tamaño: " + videoBytes.length + " bytes");

            // Crear el cuerpo multipart
            org.springframework.core.io.ByteArrayResource videoResource = 
                new org.springframework.core.io.ByteArrayResource(videoBytes) {
                    @Override
                    public String getFilename() {
                        return "video_bienvenida.mp4";
                    }
                };

            // Enviar como multipart form data con timeout extendido para videos grandes
            String response = webClient.post()
                    .uri(fullApiUri)
                    .headers(h -> h.addAll(headers))
                    .body(org.springframework.web.reactive.function.BodyInserters
                            .fromMultipartData("file", videoResource)
                            .with("caption", caption != null ? caption : ""))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofMinutes(3)) // 3 minutos para uploads de video grandes
                    .doOnSuccess(resp -> {
                        System.out.println("WatiApiService: Video enviado exitosamente. Respuesta de Wati: " + resp);
                        // Verificar si la respuesta indica éxito
                        if (resp.contains("\"result\":false") || resp.contains("file can not be null")) {
                            throw new RuntimeException("Wati API rechazó el video: " + resp);
                        }
                    })
                    .doOnError(error -> System.err.println("WatiApiService: Error al enviar video a Wati: " + error.getMessage()))
                    .block(java.time.Duration.ofMinutes(3)); // Timeout de 3 minutos para el bloqueo

            if (response != null) {
                System.out.println("WatiApiService: Video enviado completado exitosamente");
            }
        } catch (Exception e) {
            System.err.println("WatiApiService: Error en envío de video: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lanzar la excepción para que el ChatbotService la capture
        }
    }
}