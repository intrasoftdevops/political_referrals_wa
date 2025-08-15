# 🚀 Guía de Configuración - Political Referrals WA

## 📋 Perfiles de Configuración

Este proyecto utiliza **perfiles de Spring Boot** para manejar diferentes entornos de manera segura y eficiente.

### 🔧 Perfiles Disponibles

#### 1. **`local` - Desarrollo Local**
- **Archivo**: `application-local.properties`
- **Uso**: Desarrollo y testing local
- **Características**:
  - Credenciales hardcodeadas para desarrollo
  - Logging detallado (DEBUG)
  - Configuraciones de desarrollo
  - Swagger UI habilitado
  - Actuator con endpoints adicionales

#### 2. **`prod` - Producción (Cloud Run)**
- **Archivo**: `application-prod.properties`
- **Uso**: Despliegue en Cloud Run
- **Características**:
  - Variables de entorno para credenciales
  - Logging optimizado (INFO)
  - Configuraciones de rendimiento
  - Health checks optimizados
  - Sin credenciales hardcodeadas

### 🚀 Cómo Usar

#### **Desarrollo Local**
```bash
# Opción 1: Variable de entorno
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run

# Opción 2: Argumento JVM
mvn spring-boot:run -Dspring.profiles.active=local

# Opción 3: IDE
# Configurar VM Options: -Dspring.profiles.active=local
```

#### **Producción (Cloud Run)**
```bash
# Se configura automáticamente en el Dockerfile
# Perfil: prod
# Variables de entorno se configuran en Cloud Run
```

### 📁 Estructura de Archivos

```
src/main/resources/
├── application.properties.example    # Template base (NO usar directamente)
├── application-local.properties     # Desarrollo local (NO subir al repo)
└── application-prod.properties     # Producción (NO subir al repo)

deploy/
├── cloud-run.yaml                  # Configuración de Cloud Run
└── secrets-example.yaml            # Ejemplo de secretos (NO subir al repo)
```

### 🔐 Configuración de Secretos

#### **Variables de Entorno Requeridas para Producción**

```bash
# Google Cloud
SPRING_CLOUD_GCP_PROJECT_ID=tu-project-id

# Telegram Bot
TELEGRAM_BOT_TOKEN=tu-telegram-token
TELEGRAM_BOT_USERNAME=tu-telegram-username

# Wati API
WATI_API_TOKEN=tu-wati-token
WATI_TENANT_ID=tu-tenant-id
WEBHOOK_VERIFY_TOKEN=tu-webhook-token

# Gemini AI
GEMINI_API_KEY=tu-gemini-key

# Analytics
ANALYTICS_JWT_SECRET=tu-jwt-secret

# Notificaciones WhatsApp
WATI_NOTIFICATION_GROUP_ID=tu-whatsapp-group-id
WATI_NOTIFICATION_PHONES=tu-whatsapp-phones

# URLs de Servicios
AI_BOT_ENDPOINT=tu-ai-bot-url
CHATBOT_IA_URL=tu-chatbot-url
ANALYTICS_ENDPOINT_URL=tu-analytics-url
WELCOME_VIDEO_URL=tu-video-url
```

### 🚀 Despliegue

#### **1. Configurar Secretos en GCP**
```bash
# Crear secretos en Cloud Secret Manager
gcloud secrets create gcp-project-id --data-file=<(echo -n "tu-project-id")
gcloud secrets create telegram-bot-token --data-file=<(echo -n "tu-token")
# ... crear todos los secretos necesarios
```

#### **2. Desplegar a Cloud Run**
```bash
# El workflow de GitHub Actions se encarga automáticamente
# Usa el perfil 'prod' por defecto
```

### 🔒 Seguridad

#### **✅ BUENAS PRÁCTICAS**
- ✅ Usar variables de entorno en producción
- ✅ Usar secretos de GCP para credenciales
- ✅ Perfil `local` solo para desarrollo
- ✅ Perfil `prod` para despliegue

#### **❌ MALAS PRÁCTICAS**
- ❌ NO hardcodear credenciales en archivos de configuración
- ❌ NO subir archivos con credenciales al repositorio
- ❌ NO usar perfil `local` en producción

### 🐛 Troubleshooting

#### **Problema**: La aplicación no inicia
**Solución**: Verificar que el perfil esté configurado correctamente

#### **Problema**: Credenciales no encontradas
**Solución**: Verificar variables de entorno en Cloud Run

#### **Problema**: Health checks fallan
**Solución**: Verificar configuración de Actuator y endpoints

### 📚 Recursos Adicionales

- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Cloud Run Environment Variables](https://cloud.google.com/run/docs/configuring/environment-variables)
- [GCP Secret Manager](https://cloud.google.com/secret-manager)
