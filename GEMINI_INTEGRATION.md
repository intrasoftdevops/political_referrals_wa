# Integración de Gemini AI para Inputs Inteligentes

## Descripción

Esta integración permite que el sistema use Gemini AI para detectar automáticamente la información personal del usuario en conversación natural, eliminando la necesidad de seguir un formulario paso a paso.

## Funcionalidades Implementadas

### 1. Extracción Automática de Datos
- **Nombre completo**: Detecta nombres con títulos (Dr., Ing., etc.)
- **Ciudad**: Identifica ciudades colombianas con conocimiento geográfico
- **Aceptación de términos**: Detecta aceptación explícita de términos
- **Teléfono de referido**: Extrae números de teléfono colombianos (+57XXXXXXXXX)
- **Código de referido**: Identifica códigos alfanuméricos de 8 dígitos

### 2. Manejo de Ambigüedades
- **Ciudades con nombres duplicados**: Armenia (Quindío vs Antioquia), La Dorada (Caldas vs Putumayo)
- **Aclaraciones inteligentes**: Pregunta específicamente por la ambigüedad detectada
- **Correcciones naturales**: Permite al usuario corregir información previamente extraída

### 3. Flujo Conversacional Inteligente
- **Registro en un intercambio**: Si se extraen todos los datos necesarios
- **Extracción parcial**: Continúa preguntando solo por datos faltantes
- **Fallback tradicional**: Si la extracción falla, usa el flujo paso a paso

## Configuración

### 1. Variables de Entorno
```properties
# Gemini AI Configuration
gemini.api.key=YOUR_GEMINI_API_KEY
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
```

### 2. Obtener API Key de Gemini
1. Ve a [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Crea una nueva API key
3. Configura la key en `application.properties`

## Ejemplos de Uso

### Escenario 1: Extracción Completa Exitosa
```
Usuario: "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, acepto sus términos, vengo por +573001234567"

Gemini extrae:
{
  "name": "Dr. Miguel Rodríguez",
  "city": "Barranquilla", 
  "acceptsTerms": true,
  "referredByPhone": "+573001234567",
  "confidence": 0.98
}

Bot responde: "¡Bienvenido Dr. Rodríguez! Confirmamos tus datos: Dr. Miguel Rodríguez, de Barranquilla. ¿Es correcto? (Sí/No)"
```

### Escenario 2: Manejo de Ambigüedad
```
Usuario: "Vivo en La Dorada y trabajo en educación"

Gemini detecta:
{
  "city": "La Dorada",
  "needsClarification": {
    "city": "La Dorada existe en Caldas y Putumayo. ¿Cuál es?"
  },
  "confidence": 0.75
}

Bot responde: "¿Te refieres a La Dorada en Caldas (cerca del Magdalena) o hay otra La Dorada que conozcas?"
```

### Escenario 3: Extracción Parcial
```
Usuario: "Hola, me llamo Ana"

Gemini extrae:
{
  "name": "Ana",
  "city": null,
  "acceptsTerms": null,
  "confidence": 0.95
}

Bot responde: "¿En qué ciudad vives?"
```

## Arquitectura Técnica

### Componentes Principales

1. **GeminiService**: Maneja la comunicación con la API de Gemini
2. **UserDataExtractionResult**: Modelo para los resultados de extracción
3. **UserDataExtractor**: Coordina la extracción y actualización de datos
4. **ChatbotService**: Integra la extracción inteligente en el flujo principal

### Flujo de Datos

```
Usuario envía mensaje
    ↓
UserDataExtractor.extractAndUpdateUser()
    ↓
GeminiService.extractUserData()
    ↓
Gemini API procesa con prompt especializado
    ↓
UserDataExtractionResult con datos extraídos
    ↓
Actualización automática del User en Firebase
    ↓
Respuesta contextual del bot
```

### Configuración de Gemini

```java
// Configuración específica para extracción precisa
temperature: 0.1        // Baja creatividad
topK: 1                // Respuesta más determinística  
topP: 1                // Sin muestreo aleatorio
maxOutputTokens: 512   // Suficiente para JSON respuesta
```

## Prompt de Extracción

El sistema usa un prompt especializado que incluye:

- **Contexto político colombiano**
- **Conocimiento geográfico específico**
- **Patrones de números móviles colombianos**
- **Formato JSON estructurado**
- **Manejo de ambigüedades**

## Métricas de Éxito

### KPIs Técnicos
- **Precisión**: >95% en extracción de nombres
- **Cobertura**: >90% detección de ciudades colombianas
- **Velocidad**: <2 segundos respuesta Gemini API
- **Confianza**: >0.8 promedio en extracciones exitosas

### KPIs de Usuario
- **Reducción de pasos**: De 4 pasos a 1-2 intercambios
- **Tasa de abandono**: <5% vs 30% actual
- **Satisfacción**: >4.5/5 en facilidad de registro

## Estados del Chatbot

### Nuevos Estados
- **WAITING_CLARIFICATION**: Esperando aclaración de ambigüedad
- **CONFIRM_DATA**: Confirmando datos extraídos automáticamente

### Flujo Inteligente
1. **Extracción automática** en primer mensaje
2. **Aclaración** si hay ambigüedad
3. **Confirmación** si se extraen todos los datos
4. **Fallback** al flujo tradicional si falla

## Manejo de Errores

### Casos de Error
1. **API de Gemini no disponible**: Fallback al flujo tradicional
2. **Respuesta malformada**: Reintento con prompt simplificado
3. **Confianza baja**: Solicitud de aclaración manual
4. **Timeout**: Respuesta inmediata con flujo tradicional

### Logging
- Todos los intentos de extracción se registran
- Errores de API se capturan y reportan
- Métricas de éxito se almacenan para análisis

## Próximas Mejoras

1. **Aprendizaje continuo**: Mejorar prompts basado en feedback
2. **Validación cruzada**: Verificar datos extraídos con fuentes externas
3. **Personalización**: Adaptar prompts según región/contexto
4. **Análisis de sentimiento**: Detectar intención del usuario
5. **Multilingüe**: Soporte para idiomas indígenas colombianos 