# 📋 Documentación Técnica - Political Referrals WA

## 🎯 **Funcionalidades Implementadas**

### **1. Inputs Inteligentes con Gemini AI**
- ✅ **Extracción automática** de datos en conversación natural
- ✅ **Campos completos**: nombre, apellido, ciudad, departamento, términos, referidos
- ✅ **Conocimiento colombiano** con ciudades y departamentos
- ✅ **Manejo de ambigüedades** (Armenia, La Dorada, Barbosa)
- ✅ **Configuración especializada** para formularios políticos

### **2. Captura del nombre de WhatsApp**
- ✅ **Extracción de `senderName`** del webhook de Wati
- ✅ **Personalización del saludo**: "¡Hola Miguel! 👋 ¿Te llamas Miguel cierto?"
- ✅ **Guardado automático** del nombre en el usuario

### **3. Manejo de Correcciones Naturales**
- ✅ **Detección automática** de correcciones
- ✅ **Mensajes contextuales** de confirmación
- ✅ **Actualización de datos** con historial

### **4. Sistema de Métricas**
- ✅ **Tracking completo** de precisión, velocidad, confianza
- ✅ **Endpoint de métricas** `/api/metrics/gemini`
- ✅ **Registro automático** de extracciones

### **5. Flujo Conversacional Inteligente**
- ✅ **Registro en un intercambio** si se extraen todos los datos
- ✅ **Extracción parcial** con continuación inteligente
- ✅ **Fallback tradicional** si falla la extracción
- ✅ **Estados dinámicos** basados en datos extraídos

## 🏗️ **Arquitectura del Sistema**

### **Componentes Principales:**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   WhatsApp      │    │    Telegram     │    │   Webhook       │
│   (Wati API)    │───▶│   (Bot API)     │───▶│   Controllers   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Gemini AI     │◀───│  ChatbotService │───▶│   Firestore     │
│   (Extracción)  │    │   (Lógica)      │    │   (Usuarios)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │  MetricsService │
                       │   (Métricas)    │
                       └─────────────────┘
```

### **Flujo de Datos:**

1. **Recepción de Mensaje**: Webhook recibe mensaje de WhatsApp/Telegram
2. **Extracción de Datos**: Gemini AI analiza el mensaje y extrae información
3. **Procesamiento**: ChatbotService determina el siguiente estado
4. **Persistencia**: Datos guardados en Firestore
5. **Respuesta**: Mensaje enviado de vuelta al usuario
6. **Métricas**: Registro automático de estadísticas

## 🔧 **Configuración Técnica**

### **Dependencias Principales:**

```xml
<dependencies>
    <!-- Spring Boot WebFlux para WebClient -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- Firebase Admin SDK -->
    <dependency>
        <groupId>com.google.firebase</groupId>
        <artifactId>firebase-admin</artifactId>
    </dependency>
    
    <!-- Lombok para reducir boilerplate -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

### **Variables de Entorno:**

```properties
# Gemini AI Configuration
gemini.api.key=YOUR_GEMINI_API_KEY
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent

# Wati API Configuration
wati.api.token=YOUR_WATI_TOKEN
wati.api.url=https://api.wati.io

# Telegram Configuration
telegram.bot.token=YOUR_TELEGRAM_BOT_TOKEN

# Firebase Configuration
spring.cloud.gcp.project-id=YOUR_PROJECT_ID
```

## 📊 **Sistema de Métricas**

### **Métricas Recolectadas:**

- **Total de extracciones**: Número total de intentos de extracción
- **Extracciones exitosas**: Número de extracciones con confianza > 0.3
- **Precisión**: Ratio de extracciones exitosas vs total
- **Tiempo promedio de respuesta**: Tiempo promedio de respuesta de Gemini API
- **Confianza promedio**: Nivel de confianza promedio de extracciones exitosas
- **Extracciones por campo**: Conteo de extracciones por tipo de campo

### **Endpoint de Métricas:**

```
GET /api/metrics/gemini
```

**Respuesta:**
```json
{
  "totalExtractions": 150,
  "successfulExtractions": 142,
  "precision": 0.946,
  "averageResponseTimeMs": 1250.5,
  "averageConfidence": 0.87,
  "fieldExtractions": {
    "name": 120,
    "city": 95,
    "acceptsTerms": 80,
    "lastname": 85,
    "state": 90
  }
}
```

## 🤖 **Integración con Gemini AI**

### **Prompt de Extracción:**

El sistema utiliza un prompt especializado que incluye:

- **Contexto político colombiano**
- **Conocimiento geográfico** de ciudades y departamentos
- **Reglas de ambigüedad** para ciudades con nombres duplicados
- **Ejemplos de correcciones** naturales
- **Formato JSON** estructurado para respuestas

### **Configuración de Gemini:**

```java
Map<String, Object> generationConfig = new HashMap<>();
generationConfig.put("temperature", 0.1);        // Baja temperatura para consistencia
generationConfig.put("topK", 1);                 // Respuesta más determinística
generationConfig.put("topP", 1.0);               // Sin filtrado de probabilidad
generationConfig.put("maxOutputTokens", 512);    // Límite de tokens de salida
```

### **Campos Extraídos:**

- **name**: Nombre del usuario (sin apellido)
- **lastname**: Apellido(s) completo(s)
- **city**: Ciudad colombiana específica
- **state**: Departamento/Estado colombiano
- **acceptsTerms**: Aceptación explícita de términos
- **referredByPhone**: Número de teléfono del referido
- **referralCode**: Código alfanumérico de referido
- **correction**: Indicador de corrección
- **previousValue**: Valor anterior corregido

## 🔄 **Flujos de Conversación**

### **Escenario 1: Registro Completo**

```
Usuario: "Hola! Soy Dr. Miguel Rodríguez de Barranquilla, acepto sus términos"

1. Gemini extrae: {name: "Miguel", lastname: "Rodríguez", city: "Barranquilla", 
                   state: "Atlántico", acceptsTerms: true}
2. ChatbotService confirma datos
3. Usuario va a conversación con IA
```

### **Escenario 2: Corrección Natural**

```
Usuario: "Me equivoqué, no soy de Medellín sino de Envigado"

1. Gemini detecta corrección: {city: "Envigado", correction: true, 
                               previousValue: "Medellín"}
2. UserDataExtractor actualiza datos
3. ChatbotService confirma cambio
```

### **Escenario 3: Ambigüedad Geográfica**

```
Usuario: "Soy de Armenia"

1. Gemini detecta ambigüedad: {city: "Armenia", needsClarification: 
                               {city: "Hay varias Armenia en Colombia..."}}
2. ChatbotService pide aclaración
3. Usuario especifica departamento
```

## 🧪 **Testing**

### **Tests Unitarios:**

- **GeminiServiceTest**: Pruebas de extracción de datos
- **UserDataExtractorTest**: Pruebas de procesamiento
- **ChatbotServiceTest**: Pruebas de flujo conversacional

### **Scripts de Integración:**

- **test_gemini_integration.sh**: Pruebas básicas de extracción
- **test_gemini_extended_integration.sh**: Pruebas con nombre y apellido
- **test_corrections_integration.sh**: Pruebas de correcciones

### **Ejemplo de Test:**

```bash
# Ejecutar pruebas de correcciones
./test_corrections_integration.sh

# Verificar métricas
curl http://localhost:8081/api/metrics/gemini
```

## 🔒 **Seguridad**

### **Medidas Implementadas:**

- **Credenciales protegidas**: API keys en `application.properties`
- **Validación de entrada**: Sanitización de mensajes de usuario
- **Manejo de errores**: Logging detallado sin exponer información sensible
- **Rate limiting**: Procesamiento asíncrono para evitar bloqueos

### **Buenas Prácticas:**

- **Rotación de API keys**: Cambio periódico de credenciales
- **Monitoreo**: Logs detallados para auditoría
- **Backup**: Respaldo automático de datos en Firestore

## 📈 **Métricas de Rendimiento**

### **KPIs Técnicos:**

- **Precisión**: >95% en extracción de nombres
- **Cobertura**: >90% detección de ciudades colombianas
- **Velocidad**: <2 segundos respuesta Gemini API
- **Confianza**: >0.8 promedio en extracciones exitosas

### **KPIs de Usuario:**

- **Reducción de pasos**: De 4 pasos a 1-2 intercambios
- **Tasa de abandono**: <5% vs 30% actual
- **Satisfacción**: >4.5/5 en facilidad de registro

## 🚀 **Despliegue**

### **Requisitos de Sistema:**

- **Java 21+**
- **Maven 3.6+**
- **Memoria**: Mínimo 512MB RAM
- **CPU**: 1 core mínimo

### **Variables de Entorno de Producción:**

```bash
export GEMINI_API_KEY="your-production-key"
export WATI_API_TOKEN="your-production-token"
export TELEGRAM_BOT_TOKEN="your-production-token"
export FIREBASE_PROJECT_ID="your-production-project"
```

### **Comandos de Despliegue:**

```bash
# Compilar
mvn clean package -DskipTests

# Ejecutar
java -jar target/political_referrals_wa-0.0.1-SNAPSHOT.jar

# Con variables de entorno
GEMINI_API_KEY=xxx WATI_API_TOKEN=xxx java -jar target/political_referrals_wa-0.0.1-SNAPSHOT.jar
```

## 🎉 **Conclusión**

**El proyecto incluye las siguientes funcionalidades principales:**

- ✅ **Inputs inteligentes** para recibir información
- ✅ **Captura del nombre de WhatsApp** para personalización
- ✅ **Manejo de correcciones naturales** 
- ✅ **Sistema de métricas** para monitoreo
- ✅ **Flujo conversacional inteligente**

**¡Listo para producción!** 🚀 