package com.politicalreferralswa.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configurar el proveedor de conexiones con timeouts
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom-connection-provider")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofSeconds(300))
                .pendingAcquireTimeout(Duration.ofSeconds(30))
                .build();

        // Configurar HttpClient con timeouts optimizados para respuestas rápidas
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10 segundos timeout de conexión
                .responseTimeout(Duration.ofSeconds(15)) // 15 segundos timeout de respuesta
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(15, TimeUnit.SECONDS)) // 15 segundos timeout de lectura
                        .addHandlerLast(new WriteTimeoutHandler(15, TimeUnit.SECONDS)) // 15 segundos timeout de escritura
                )
                .keepAlive(true)
                .compress(true)
                .metrics(true, uri -> uri); // Habilitar métricas para monitoreo

        // Crear WebClient.Builder con la configuración personalizada
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
