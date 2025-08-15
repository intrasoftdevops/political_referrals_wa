# 🚀 Guía Completa - Political Referrals WA

## 📋 Tabla de Contenidos

1. [🎯 Descripción del Proyecto](#-descripción-del-proyecto)
2. [⚙️ Configuración y Perfiles](#️-configuración-y-perfiles)
3. [🔐 Configuración de Seguridad](#-configuración-de-seguridad)
4. [🚀 Despliegue y CI/CD](#-despliegue-y-cicd)
5. [📱 Configuración de Notificaciones](#-configuración-de-notificaciones)
6. [🐛 Troubleshooting](#-troubleshooting)
7. [📚 Recursos Adicionales](#-recursos-adicionales)

---

## 🎯 Descripción del Proyecto

**Political Referrals WA** es un chatbot político colombiano con **extracción inteligente de datos** usando Gemini AI.

### ✨ Funcionalidades Principales
- **Inputs inteligentes** con Gemini AI para extracción automática de datos
- **Multi-plataforma**: WhatsApp (Wati API) y Telegram
- **Inteligencia emocional** y análisis semántico
- **Sistema de referidos** con códigos únicos
- **Base de datos** Firebase Firestore
- **Despliegue automático** en Google Cloud Run

---

## ⚙️ Configuración y Perfiles

### 🔧 Perfiles de Spring Boot

Este proyecto utiliza **perfiles de Spring Boot** para manejar diferentes entornos de manera segura.

#### **1. Perfil `local` - Desarrollo Local**
- **Archivo**: `application-local.properties`
- **Uso**: Desarrollo y testing local
- **Características**:
  - Credenciales hardcodeadas para desarrollo
  - Logging detallado (DEBUG)
  - Swagger UI habilitado
  - Actuator con endpoints adicionales

#### **2. Perfil `prod` - Producción (Cloud Run)**
- **Archivo**: `application-prod.properties`
- **Uso**: Despliegue en Cloud Run
- **Características**:
  - Variables de entorno para credenciales
  - Logging optimizado (INFO)
  - Configuraciones de rendimiento
  - Health checks optimizados

### 🚀 Cómo Usar los Perfiles

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
└── secrets-example.yaml            # Ejemplo de secretos

docs/
├── MAIN_GUIDE.md                   # Esta guía
└── PROJECT_STATUS.md               # Estado del proyecto
```

---

## 🔐 Configuración de Seguridad

### 🚨 Importante: Seguridad de Credenciales

**NUNCA** subas archivos con credenciales reales al repositorio. El proyecto usa:
- **Variables de entorno** para producción
- **Secretos de GCP** para credenciales sensibles
- **Perfiles separados** para cada entorno

### 🔑 Variables de Entorno Requeridas

#### **Google Cloud**
```bash
SPRING_CLOUD_GCP_PROJECT_ID=tu-project-id
```

#### **Telegram Bot**
```bash
TELEGRAM_BOT_TOKEN=tu-telegram-token
TELEGRAM_BOT_USERNAME=tu-telegram-username
```

#### **Wati API (WhatsApp)**
```bash
WATI_API_TOKEN=tu-wati-token
WATI_TENANT_ID=tu-tenant-id
WEBHOOK_VERIFY_TOKEN=tu-webhook-token
```

#### **Gemini AI**
```bash
GEMINI_API_KEY=tu-gemini-key
```

#### **Analytics**
```bash
ANALYTICS_JWT_SECRET=tu-jwt-secret
```

#### **Notificaciones WhatsApp**
```bash
WATI_NOTIFICATION_GROUP_ID=tu-whatsapp-group-id
WATI_NOTIFICATION_PHONES=tu-whatsapp-phones
```

#### **URLs de Servicios**
```bash
AI_BOT_ENDPOINT=tu-ai-bot-url
CHATBOT_IA_URL=tu-chatbot-url
ANALYTICS_ENDPOINT_URL=tu-analytics-url
WELCOME_VIDEO_URL=tu-video-url
```

### 🛡️ Configuración de Google Cloud Secrets

#### **1. Crear Secretos en GCP**
```bash
# Crear secretos en Cloud Secret Manager
gcloud secrets create gcp-project-id --data-file=<(echo -n "tu-project-id")
gcloud secrets create gemini-api-key --data-file=<(echo -n "tu-gemini-key")
gcloud secrets create telegram-bot-token --data-file=<(echo -n "tu-telegram-token")
# ... crear todos los secretos necesarios
```

#### **2. Verificar Secretos Configurados**
```bash
# Listar todos los secretos
gcloud secrets list --project=tu-project-id

# Ver versiones de un secret
gcloud secrets versions list gcp-project-id --project=tu-project-id
```

---

## 🚀 Despliegue y CI/CD

### 🔧 Prerrequisitos

#### **Software Requerido**
- **Java 21+** - [Descargar aquí](https://adoptium.net/)
- **Maven 3.6+** - [Descargar aquí](https://maven.apache.org/download.cgi)
- **Docker** - [Descargar aquí](https://docs.docker.com/get-docker/)
- **Google Cloud CLI** - [Descargar aquí](https://cloud.google.com/sdk/docs/install)

#### **Cuentas y Servicios**
- **GitHub** - Para el repositorio y GitHub Actions
- **Google Cloud Platform** - Para Cloud Run y Secret Manager

### ⚙️ Configuración de GitHub Secrets

#### **1. Ir a GitHub Repository Settings**
- Navega a tu repositorio en GitHub
- Ve a **Settings** → **Secrets and variables** → **Actions**

#### **2. Agregar los Siguientes Secrets**

##### **GCP Configuration**
```
GCP_PROJECT_ID=tu-proyecto-id
GCP_SA_KEY=contenido-del-archivo-json-de-service-account
```

##### **Notificaciones WhatsApp (Opcional)**
```
WHATSAPP_NOTIFICATION_PHONE=tu-numero
WHATSAPP_GROUP_ID=id-del-grupo
```

### 🚀 Pipeline de CI/CD

El proyecto incluye un **pipeline automático** de GitHub Actions que:

1. **Build y Testing** - Compila y ejecuta tests
2. **Análisis de Código** - Verifica calidad del código
3. **Construcción de Docker** - Crea imagen de contenedor
4. **Despliegue Automático** - Despliega a Cloud Run
5. **Notificaciones** - Envía confirmación por WhatsApp

### 📋 Despliegue Manual (Opcional)

#### **1. Construir la Aplicación**
```bash
mvn clean package -DskipTests
```

#### **2. Construir Imagen Docker**
```bash
docker build -t political-referrals-wa .
```

#### **3. Desplegar a Cloud Run**
```bash
gcloud run deploy political-referrals-wa \
  --image gcr.io/tu-proyecto/political-referrals-wa:latest \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory 2Gi \
  --cpu 2
```

---

## 📱 Configuración de Notificaciones

### 🔔 Notificaciones de WhatsApp

El pipeline CI/CD puede enviar notificaciones automáticas sobre el estado del despliegue.

#### **Configuración Requerida**
1. **Configurar secretos en GitHub** (ver sección anterior)
2. **Habilitar notificaciones** en el workflow
3. **Verificar configuración** de Wati API

#### **Tipos de Notificaciones**
- ✅ **Despliegue exitoso** - Confirmación de éxito
- ❌ **Despliegue fallido** - Alerta de error
- 🔄 **Rollback** - Notificación de reversión

---

## 🐛 Troubleshooting

### **Problema**: La aplicación no inicia
**Solución**: Verificar que el perfil esté configurado correctamente

### **Problema**: Credenciales no encontradas
**Solución**: Verificar variables de entorno en Cloud Run

### **Problema**: Health checks fallan
**Solución**: Verificar configuración de Actuator y endpoints

### **Problema**: Despliegue falla en Cloud Run
**Solución**: 
1. Verificar logs en Cloud Run
2. Verificar configuración de secretos
3. Verificar variables de entorno

### **Problema**: Notificaciones no se envían
**Solución**:
1. Verificar secretos de GitHub
2. Verificar configuración de Wati API
3. Verificar permisos de notificaciones

---

## 📚 Recursos Adicionales

### **Documentación Oficial**
- [Spring Boot Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Cloud Run Environment Variables](https://cloud.google.com/run/docs/configuring/environment-variables)
- [GCP Secret Manager](https://cloud.google.com/secret-manager)
- [GitHub Actions](https://docs.github.com/en/actions)

### **Archivos de Referencia**
- **`deploy/cloud-run.yaml`** - Configuración de Cloud Run
- **`deploy/secrets-example.yaml`** - Ejemplo de configuración de secretos
- **`docs/PROJECT_STATUS.md`** - Estado actual del proyecto

### **Soporte**
- 📧 **Issues**: [GitHub Issues del proyecto]
- 💬 **Discusiones**: [GitHub Discussions]
- 📖 **Wiki**: [Documentación del proyecto]

---

## 🔒 Seguridad y Mejores Prácticas

### **✅ BUENAS PRÁCTICAS**
- ✅ Usar variables de entorno en producción
- ✅ Usar secretos de GCP para credenciales
- ✅ Perfil `local` solo para desarrollo
- ✅ Perfil `prod` para despliegue
- ✅ Nunca hardcodear credenciales
- ✅ Rotar credenciales regularmente

### **❌ MALAS PRÁCTICAS**
- ❌ NO hardcodear credenciales en archivos
- ❌ NO subir archivos con credenciales al repositorio
- ❌ NO usar perfil `local` en producción
- ❌ NO compartir secretos por canales no seguros

---

*Última actualización: Agosto 2025*
*Versión: 2.0 - Unificada*
