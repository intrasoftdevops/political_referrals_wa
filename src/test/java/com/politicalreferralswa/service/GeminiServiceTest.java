package com.politicalreferralswa.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import reactor.core.publisher.Mono;

class GeminiServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private MetricsService metricsService;

    private GeminiService geminiService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        geminiService = new GeminiService(webClientBuilder, objectMapper, metricsService);
    }

    @Test
    void testExtractUserData_CompleteInformation() {
        // Arrange
        String userMessage = "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, acepto sus términos, vengo por +573001234567";
        String expectedResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"name\\":\\"Dr. Miguel Rodríguez\\",\\"city\\":\\"Barranquilla\\",\\"acceptsTerms\\":true,\\"referredByPhone\\":\\"+573001234567\\",\\"confidence\\":0.98}"
                  }]
                }
              }]
            }
            """;

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("Dr. Miguel Rodríguez", result.getName());
        assertEquals("Barranquilla", result.getCity());
        assertTrue(result.getAcceptsTerms());
        assertEquals("+573001234567", result.getReferredByPhone());
        assertEquals(0.98, result.getConfidence());
        assertTrue(result.isSuccessful());
        assertFalse(result.needsClarification());
    }

    @Test
    void testExtractUserData_NeedsClarification() {
        // Arrange
        String userMessage = "Vivo en La Dorada y trabajo en educación";
        String expectedResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"city\\":\\"La Dorada\\",\\"needsClarification\\":{\\"city\\":\\"La Dorada existe en Caldas y Putumayo. ¿Cuál es?\\"},\\"confidence\\":0.75}"
                  }]
                }
              }]
            }
            """;

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("La Dorada", result.getCity());
        assertEquals(0.75, result.getConfidence());
        assertTrue(result.needsClarification());
        assertEquals("La Dorada existe en Caldas y Putumayo. ¿Cuál es?", result.getClarificationMessage());
    }

    @Test
    void testExtractUserData_PartialInformation() {
        // Arrange
        String userMessage = "Hola, me llamo Ana";
        String expectedResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"name\\":\\"Ana\\",\\"city\\":null,\\"acceptsTerms\\":null,\\"confidence\\":0.95}"
                  }]
                }
              }]
            }
            """;

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, null, null);

        // Assert
        assertNotNull(result);
        assertEquals("Ana", result.getName());
        assertNull(result.getCity());
        assertNull(result.getAcceptsTerms());
        assertEquals(0.95, result.getConfidence());
        assertTrue(result.isSuccessful());
        assertFalse(result.needsClarification());
    }

    @Test
    void testExtractUserData_WithContext() {
        // Arrange
        String userMessage = "La de Caldas";
        String previousContext = "¿Te refieres a La Dorada en Caldas o Putumayo?";
        String expectedResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"city\\":\\"La Dorada, Caldas\\",\\"confidence\\":0.95}"
                  }]
                }
              }]
            }
            """;

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, previousContext, null);

        // Assert
        assertNotNull(result);
        assertEquals("La Dorada, Caldas", result.getCity());
        assertEquals(0.95, result.getConfidence());
        assertTrue(result.isSuccessful());
        assertFalse(result.needsClarification());
    }

    @Test
    void testExtractUserData_EmptyResponse() {
        // Arrange
        String userMessage = "Hola";
        String expectedResponse = "{}";

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getConfidence());
        assertFalse(result.isSuccessful());
    }

    @Test
    void testExtractUserData_ErrorHandling() {
        // Arrange
        String userMessage = "Hola";

        // Act & Assert
        assertDoesNotThrow(() -> {
            UserDataExtractionResult result = geminiService.extractUserData(userMessage, null, null);
            assertNotNull(result);
        });
    }

    @Test
    void testExtractUserData_TermsAcceptance() {
        // Arrange
        String userMessage = "Sí";
        String expectedResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"acceptsTerms\\":true,\\"confidence\\":0.95}"
                  }]
                }
              }]
            }
            """;

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, "WAITING_TERMS_ACCEPTANCE", "WAITING_TERMS_ACCEPTANCE");

        // Assert
        assertNotNull(result);
        assertTrue(result.getAcceptsTerms());
        assertEquals(0.95, result.getConfidence());
        assertTrue(result.isSuccessful());
        assertFalse(result.needsClarification());
    }

    @Test
    void testExtractUserData_TermsAcceptanceWithoutAccent() {
        // Arrange
        String userMessage = "Si";
        String expectedResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"acceptsTerms\\":true,\\"confidence\\":0.95}"
                  }]
                }
              }]
            }
            """;

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, "WAITING_TERMS_ACCEPTANCE", "WAITING_TERMS_ACCEPTANCE");

        // Assert
        assertNotNull(result);
        assertTrue(result.getAcceptsTerms());
        assertEquals(0.95, result.getConfidence());
        assertTrue(result.isSuccessful());
        assertFalse(result.needsClarification());
    }

    @Test
    void testExtractUserData_TermsRejection() {
        // Arrange
        String userMessage = "No";
        String expectedResponse = """
            {
              "candidates": [{
                "content": {
                  "parts": [{
                    "text": "{\\"acceptsTerms\\":false,\\"confidence\\":0.95}"
                  }]
                }
              }]
            }
            """;

        // Act
        UserDataExtractionResult result = geminiService.extractUserData(userMessage, "WAITING_TERMS_ACCEPTANCE", "WAITING_TERMS_ACCEPTANCE");

        // Assert
        assertNotNull(result);
        assertFalse(result.getAcceptsTerms());
        assertEquals(0.95, result.getConfidence());
        assertTrue(result.isSuccessful());
        assertFalse(result.needsClarification());
    }


} 