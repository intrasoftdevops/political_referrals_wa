package com.politicalreferralswa.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI; // <<--- ¡IMPORTACIÓN CLAVE!

@Service
public class WatiApiService {

    @Value("${wati.api.endpoint.base}")
    private String watiApiBaseEndpoint;
    @Value("${wati.api.tenant-id}")
    private String watiApiTenantId;
    @Value("${wati.api.token}")
    private String watiApiToken;

    private final WebClient webClient;

    public WatiApiService(WebClient.Builder webClientBuilder) {
        // Construimos WebClient SIN una base URL. La URL completa se construirá dinámicamente.
        this.webClient = webClientBuilder.build();
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
                .uri(fullApiUri) // <<--- ¡PASAMOS EL OBJETO URI ABSOLUTO DIRECTAMENTE!
                .headers(h -> h.addAll(headers))
                .retrieve()
                .bodyToMono(String.class) // Espera una respuesta de String
                .doOnSuccess(response -> System.out.println("WatiApiService: Mensaje enviado exitosamente. Respuesta de Wati: " + response))
                .doOnError(error -> System.err.println("WatiApiService: Error al enviar mensaje a Wati: " + error.getMessage()))
                .subscribe(); // Ejecuta la llamada reactiva
    }
}