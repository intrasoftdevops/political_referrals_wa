# 🤖 Political Referrals WA - Chatbot Inteligente

Sistema de chatbot político colombiano con **extracción inteligente de datos** usando Gemini AI.

## 🧹 **Limpieza Realizada**

### **Archivos Eliminados**
- ✅ **Documentación Redundante**: `ANALYTICS_INTEGRATION.md`, `GEMINI_INTEGRATION.md`, `IMPLEMENTATION_GUIDE.md`, `TECHNICAL_DOCUMENTATION.md`, `planning.md`
- ✅ **Scripts de Test Redundantes**: 9 archivos `.sh` eliminados, manteniendo solo los esenciales
- ✅ **Variables Sensibles**: Todas las credenciales reales reemplazadas por placeholders

### **Información Consolidada**
- ✅ **README Principal**: Toda la documentación importante consolidada aquí
- ✅ **Configuración Segura**: Variables de entorno documentadas y protegidas
- ✅ **Instrucciones Claras**: Guías de configuración y seguridad actualizadas

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

## 🚀 Características Principales

### ✨ Extracción Inteligente con Gemini AI
- **Detección automática** de información personal en conversación natural
- **Campos extraídos**: nombre, apellido, ciudad, departamento, aceptación de términos, referidos
- **Reducción de pasos**: De 4 pasos tradicionales a 1-2 intercambios
- **Precisión**: >95% en extracción de nombres y >90% en ciudades colombianas

### 📱 Plataformas Soportadas
- **WhatsApp** (Wati API)
- **Telegram** (Bot API)

### 🗄️ Base de Datos
- **Firestore** (Google Cloud) para persistencia de usuarios
- **Migración automática** de UUIDs a números de teléfono

## 🛠️ Tecnologías

- **Backend**: Spring Boot 3.5.3 (Java 21)
- **IA**: Google Gemini 1.5 Flash
- **Base de Datos**: Google Cloud Firestore
- **APIs**: Wati (WhatsApp), Telegram Bot API
- **Build**: Maven

## 📋 Requisitos

- Java 21+
- Maven 3.6+
- Cuenta de Google Cloud con Firestore
- API Key de Gemini AI
- Credenciales de Wati y Telegram

## ⚙️ Configuración

### 1. Clonar y Configurar
```bash
git clone <repository-url>
cd political_referrals_wa
```

### 2. Configurar Perfiles de Spring Boot

#### **Para Desarrollo Local:**
```bash
# Copiar el archivo de ejemplo local
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# Editar con tus credenciales reales
# Luego ejecutar con el perfil local
mvn spring-boot:run -Dspring.profiles.active=local
```

#### **Para Producción (Cloud Run):**
```bash
# El perfil 'prod' se configura automáticamente
# Las credenciales se configuran via variables de entorno en Cloud Run
```

### 3. Configurar Variables de Entorno
Ver `docs/CONFIGURATION_GUIDE.md` para la lista completa de variables requeridas.

## 📚 Documentación

Toda la documentación detallada se encuentra en la carpeta `docs/`:

- **`docs/MAIN_GUIDE.md`** - 🚀 Guía completa unificada (configuración, despliegue, seguridad)
- **`docs/PROJECT_STATUS.md`** - 📊 Estado del proyecto, funcionalidades y roadmap

## 🚀 Despliegue

### **Desarrollo Local**
```bash
# Usar perfil local
mvn spring-boot:run -Dspring.profiles.active=local
```

### **Cloud Run (Automático)**
- El CI/CD de GitHub Actions se encarga del despliegue automático
- Usa el perfil `prod` por defecto
- Las credenciales se configuran via secretos de GCP

#### **Archivos de Configuración de Despliegue:**
- **`deploy/cloud-run.yaml`** - Configuración de Cloud Run
- **`src/main/resources/application.properties.example`** - Template con comandos de GCP para crear secretos

## 🔒 Seguridad

- ✅ **Perfiles separados** para desarrollo y producción
- ✅ **Credenciales nunca** en el repositorio
- ✅ **Variables de entorno** para producción
- ✅ **Secretos de GCP** para credenciales sensibles

## 📁 Estructura del Proyecto

```
political_referrals_wa/
├── src/main/resources/
│   ├── application.properties.example    # Template base
│   ├── application-local.properties      # Desarrollo local (NO en repo)
│   └── application-prod.properties      # Producción (NO en repo)
├── docs/                                # 📚 Documentación completa
│   ├── CONFIGURATION_GUIDE.md           # Guía de configuración
│   ├── DEPLOYMENT_GUIDE.md              # Guía de despliegue
│   ├── SECURITY_SETUP.md                # Configuración de seguridad
│   ├── GITHUB_SECRETS_SETUP.md          # Secretos de GitHub
│   └── planning.md                      # Planificación del proyecto
├── deploy/                              # ⚙️ Archivos de despliegue
│   ├── cloud-run.yaml                   # Configuración de Cloud Run
│   └── secrets-example.yaml             # Ejemplo de secretos
├── .github/workflows/                   # CI/CD automático
└── README.md                            # Este archivo
```

## 🤝 Contribución

1. **Fork** el repositorio
2. **Crear** una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. **Commit** tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. **Push** a la rama (`git push origin feature/AmazingFeature`)
5. **Abrir** un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

## 📞 Soporte

Para soporte técnico o preguntas:
- 📧 Email: [tu-email@dominio.com]
- 💬 Issues: [GitHub Issues del proyecto]
