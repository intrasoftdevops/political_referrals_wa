# Guía de Implementación: Inputs Inteligentes con Gemini AI

## Resumen de la Implementación

Se ha implementado exitosamente la funcionalidad de inputs inteligentes usando Gemini AI en el proyecto `political_referrals_wa`. Esta implementación permite que los usuarios se registren de forma natural sin seguir un formulario paso a paso.

## Archivos Creados/Modificados

### Nuevos Archivos
1. **`GeminiService.java`** - Servicio para comunicación con Gemini API
2. **`UserDataExtractionResult.java`** - Modelo para resultados de extracción
3. **`UserDataExtractor.java`** - Coordinador de extracción de datos
4. **`GeminiServiceTest.java`** - Tests unitarios
5. **`GEMINI_INTEGRATION.md`** - Documentación técnica
6. **`IMPLEMENTATION_GUIDE.md`** - Esta guía

### Archivos Modificados
1. **`ChatbotService.java`** - Integración de extracción inteligente
2. **`application.properties.example`** - Configuración de Gemini

## Pasos para Activar la Funcionalidad

### 1. Configurar API Key de Gemini

```bash
# Obtener API Key desde Google AI Studio
# https://makersuite.google.com/app/apikey

# Crear archivo application.properties
cp src/main/resources/application.properties.example src/main/resources/application.properties

# Editar application.properties y agregar:
gemini.api.key=TU_API_KEY_AQUI
```

### 2. Verificar Dependencias

Las dependencias necesarias ya están en `pom.xml`:
- `spring-boot-starter-webflux` (para WebClient)
- `jackson-databind` (para JSON parsing)
- `lombok` (para anotaciones)

### 3. Compilar y Ejecutar

```bash
# Compilar el proyecto
mvn clean compile

# Ejecutar tests
mvn test

# Ejecutar la aplicación
mvn spring-boot:run
```

## Flujo de Funcionamiento

### Escenario 1: Registro Completo en un Mensaje
```
Usuario: "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, acepto sus términos, vengo por +573001234567"

1. GeminiService extrae datos automáticamente
2. UserDataExtractor actualiza el User
3. ChatbotService confirma datos
4. Usuario va directamente a conversación con IA
```

### Escenario 2: Aclaración de Ambigüedad
```
Usuario: "Vivo en La Dorada"

1. Gemini detecta ambigüedad (Caldas vs Putumayo)
2. Bot pregunta específicamente por la aclaración
3. Usuario responde "La de Caldas"
4. Gemini actualiza datos y continúa
```

### Escenario 3: Extracción Parcial
```
Usuario: "Me llamo Ana"

1. Gemini extrae solo el nombre
2. Bot pregunta por ciudad faltante
3. Usuario responde "Bogotá"
4. Bot pregunta por aceptación de términos
5. Flujo continúa hasta completar registro
```

## Configuración Avanzada

### Personalizar Prompt de Gemini

Editar en `GeminiService.java`:

```java
private String buildExtractionPrompt(String userMessage, String previousContext) {
    // Personalizar el prompt según necesidades específicas
    return String.format("""
        Eres un extractor especializado en formularios políticos colombianos.
        
        // Agregar conocimiento específico de tu región
        CONOCIMIENTO ADICIONAL:
        - Tu ciudad específica: información local
        - Tu campaña: detalles particulares
        
        // ... resto del prompt
        """, userMessage, previousContext);
}
```

### Ajustar Configuración de Gemini

```java
// En GeminiService.java
Map<String, Object> generationConfig = new HashMap<>();
generationConfig.put("temperature", 0.1);  // Más determinístico
generationConfig.put("topK", 1);           // Respuesta más consistente
generationConfig.put("topP", 1.0);         // Sin muestreo aleatorio
generationConfig.put("maxOutputTokens", 512); // Suficiente para JSON
```

### Agregar Nuevos Campos

1. **Modificar `UserDataExtractionResult.java`**:
```java
private String newField;
// Agregar getter/setter
```

2. **Actualizar prompt en `GeminiService.java`**:
```java
// Agregar en CAMPOS A EXTRAER:
- newField: Descripción del nuevo campo
```

3. **Actualizar `UserDataExtractor.java`**:
```java
// En updateUserWithExtractedData()
if (extraction.getNewField() != null) {
    user.setNewField(extraction.getNewField());
    updated = true;
}
```

## Monitoreo y Logs

### Logs Importantes

```java
// En GeminiService.java
System.out.println("GeminiService: Enviando consulta: " + userMessage);
System.out.println("GeminiService: Respuesta recibida: " + response);

// En UserDataExtractor.java
System.out.println("UserDataExtractor: Datos extraídos: " + extraction);
System.out.println("UserDataExtractor: Usuario actualizado: " + userUpdated);
```

### Métricas a Monitorear

1. **Tasa de éxito de extracción**: `confidence > 0.7`
2. **Tiempo de respuesta**: `< 2 segundos`
3. **Tasa de aclaraciones**: `needsClarification()`
4. **Fallback al flujo tradicional**: Cuando extracción falla

## Troubleshooting

### Problema: API Key no válida
```
Error: 403 Forbidden
Solución: Verificar API key en Google AI Studio
```

### Problema: Respuesta malformada
```
Error: JSON parsing exception
Solución: Verificar prompt y formato de respuesta
```

### Problema: Timeout
```
Error: Connection timeout
Solución: Verificar conectividad y configuración de red
```

### Problema: Baja confianza
```
Confidence: 0.3
Solución: Revisar prompt y agregar más contexto
```

## Pruebas

### Ejecutar Tests Unitarios
```bash
mvn test -Dtest=GeminiServiceTest
```

### Probar Casos de Uso
```bash
# Caso 1: Extracción completa
curl -X POST http://localhost:8081/webhook/wati \
  -H "Content-Type: application/json" \
  -d '{"message": "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, acepto sus términos"}'

# Caso 2: Ambigüedad
curl -X POST http://localhost:8081/webhook/wati \
  -H "Content-Type: application/json" \
  -d '{"message": "Vivo en La Dorada"}'

# Caso 3: Extracción parcial
curl -X POST http://localhost:8081/webhook/wati \
  -H "Content-Type: application/json" \
  -d '{"message": "Me llamo Ana"}'
```

## Próximos Pasos

1. **Desplegar en producción** con API key real
2. **Monitorear métricas** de éxito y rendimiento
3. **Ajustar prompts** basado en feedback real
4. **Agregar más campos** según necesidades
5. **Implementar cache** para respuestas frecuentes
6. **Agregar validación** cruzada de datos

## Soporte

Para problemas técnicos:
1. Revisar logs de la aplicación
2. Verificar configuración de API key
3. Probar con casos de uso simples
4. Consultar documentación de Gemini API
5. Revisar tests unitarios para ejemplos 