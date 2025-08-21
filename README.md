# 🚀 Political Referrals WA - Chatbot Político Colombiano

## 📋 Descripción

**Political Referrals WA** es un chatbot político colombiano con **extracción inteligente de datos** usando Gemini AI, integrado con WhatsApp (Wati API) y Telegram.

## ✨ Funcionalidades Principales

- **Inputs inteligentes** con Gemini AI para extracción automática de datos
- **Multi-plataforma**: WhatsApp (Wati API) y Telegram
- **Inteligencia emocional** y análisis semántico
- **Sistema de referidos** con códigos únicos
- **Base de datos** Firebase Firestore
- **Despliegue automático** en Google Cloud Run

## 🚀 Despliegue Rápido

### Para Desarrolladores:
```bash
git push origin main → ✅ Deploy automático
```

**No necesita:**
- ❌ Configurar secretos manualmente
- ❌ Ejecutar scripts
- ❌ Hacer nada más que push

## ⚙️ Configuración Local

### Desarrollo Local:
```bash
# Compilar
mvn clean install -DskipTests

# Ejecutar con perfil local
mvn spring-boot:run -Dspring.profiles.active=local

# O ejecutar JAR
java -jar target/political_referrals_wa-0.0.1-SNAPSHOT.jar
```

### Perfiles Disponibles:
- **`local`**: Desarrollo con credenciales locales
- **`prod`**: Producción (se activa automáticamente en Cloud Run)

## 🔐 Seguridad

### Credenciales Sensibles (GCP Secret Manager):
- `webhook-verify-token`
- `telegram-bot-token`
- `wati-api-token`
- `gemini-api-key`
- `analytics-jwt-secret`

### Variables de Configuración (cloud-run.yaml):
- IDs de proyecto y tenant
- URLs de endpoints
- Configuración de notificaciones

## 📁 Estructura del Proyecto

```
src/main/resources/
├── application.properties          # Perfil por defecto (prod)
├── application-local.properties   # Desarrollo local
└── application-prod.properties    # Producción

deploy/
├── cloud-run.yaml                 # Configuración de Cloud Run
└── cloud-run.example.yaml         # Template de ejemplo

docs/
├── MAIN_GUIDE.md                  # Guía completa
└── PROJECT_STATUS.md              # Estado del proyecto
```

## 🛠️ Tecnologías

- **Backend**: Spring Boot 3.x (Java 21)
- **Base de Datos**: Firebase Firestore
- **IA**: Gemini AI (Google)
- **APIs**: Wati (WhatsApp), Telegram Bot
- **Cloud**: Google Cloud Run
- **CI/CD**: GitHub Actions
- **Seguridad**: GCP Secret Manager

## 📊 Estado del Proyecto

### ✅ Completado:
- Sistema de referidos funcional
- Integración multi-plataforma
- IA para extracción de datos
- Despliegue automático
- Configuración de seguridad

### 🚧 En Desarrollo:
- Optimizaciones de rendimiento
- Nuevas funcionalidades de IA

## 🎛️ Control de IA del Sistema

El sistema incluye **endpoints de control global** para activar/desactivar la IA en tiempo real:

### **Endpoints Principales:**
- **`GET /api/system/ai/status`** - Ver estado actual
- **`POST /api/system/ai/disable`** - Deshabilitar IA
- **`POST /api/system/ai/enable`** - Habilitar IA
- **`POST /api/system/ai/toggle`** - Cambiar estado

### **Pruebas Rápidas:**
```bash
# Ver estado
curl http://localhost:8080/api/system/ai/status

# Deshabilitar IA
curl -X POST http://localhost:8080/api/system/ai/disable

# Habilitar IA
curl -X POST http://localhost:8080/api/system/ai/enable
```

📖 **Documentación completa**: [README-IA-CONTROL.md](README-IA-CONTROL.md)

## 🔧 Troubleshooting

### Problema: Firebase no conecta localmente
**Solución**: Asegúrate de tener `political-referrals-wa-key.json` en `src/main/resources/`

### Problema: Deploy falla en Cloud Run
**Solución**: Verifica que los secretos estén configurados en GCP Secret Manager

## 📞 Soporte

Para problemas técnicos o preguntas sobre el proyecto, revisa:
1. **Logs de Cloud Run** en GCP Console
2. **GitHub Actions** para errores de CI/CD
3. **Documentación completa** en `docs/MAIN_GUIDE.md`

## 📚 Documentación Completa

- **Guía Principal**: `docs/MAIN_GUIDE.md`
- **Estado del Proyecto**: `docs/PROJECT_STATUS.md`
- **Configuración de Seguridad**: `docs/SECURITY_SETUP.md`

---

**Political Referrals WA** - Chatbot político inteligente para Colombia 🇨🇴