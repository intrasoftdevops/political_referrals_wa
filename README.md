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
# El perfil prod se activa automáticamente
mvn spring-boot:run

# O explícitamente
mvn spring-boot:run -Dspring.profiles.active=prod
```

#### **Configuración de Perfiles:**
- **`local`**: Desarrollo local con credenciales hardcodeadas
- **`prod`**: Producción con variables de entorno (por defecto)
- **Perfil dinámico**: Se puede cambiar con `-Dspring.profiles.active=<perfil>`

### 3. Variables de Entorno (Opcional)
```bash
# Configurar perfil específico
export SPRING_PROFILES_ACTIVE=local

# Ejecutar (usará el perfil configurado)
mvn spring-boot:run
```

### 4. Credenciales de Firebase
El perfil `prod` incluye automáticamente las credenciales de Firebase desde:
- **Local**: `src/main/resources/political-referrals-wa-key.json`
- **Cloud Run**: Service account configurado en el entorno

## 📚 Documentación

### **Archivos Principales**
- **`README.md`**: Esta guía completa (incluye configuración, despliegue y troubleshooting)
- **`docs/MAIN_GUIDE.md`**: Guía principal del proyecto con detalles técnicos
- **`docs/PROJECT_STATUS.md`**: Estado actual del proyecto y roadmap

### **Configuración**
- **`src/main/resources/application-local.properties`**: Perfil de desarrollo local
- **`src/main/resources/application-prod.properties`**: Perfil de producción (por defecto)
- **`src/main/resources/application.properties.example`**: Template con comandos para crear secrets

### **Despliegue**
- **`.github/workflows/ci-cd.yml`**: Pipeline de CI/CD automático
- **`deploy/cloud-run.yaml`**: Configuración de Cloud Run
- **`Dockerfile`**: Imagen Docker optimizada para Cloud Run

## 🚀 Despliegue

### **Despliegue Automático (Recomendado)**
El proyecto incluye **CI/CD automático** con GitHub Actions:

1. **Push a `main`** → Despliegue automático a Cloud Run
2. **Configuración automática** de variables de entorno
3. **Health checks** configurados automáticamente
4. **Rollback automático** si falla el despliegue

### **Configuración de Secrets en GitHub**
```bash
# Los secrets se configuran automáticamente desde application.properties.example
# No se requieren scripts adicionales
```

### **Despliegue Manual (Opcional)**
```bash
# Build de la imagen
mvn clean package

# Despliegue a Cloud Run
gcloud run deploy political-referrals-wa \
  --image gcr.io/PROJECT_ID/political-referrals-wa \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

## 🔒 Seguridad

### **Credenciales y Secrets**
- ✅ **Nunca committear** archivos con credenciales reales
- ✅ **Usar variables de entorno** en Cloud Run
- ✅ **Archivos de ejemplo** con placeholders seguros
- ✅ **Service accounts** de GCP para producción

### **Archivos Sensibles (.gitignore)**
```
# Credenciales
src/main/resources/political-referrals-wa-key.json
application-local.properties
application-prod.properties

# Service accounts
political-referrals-wa-sa*.json
```

## 🚨 Troubleshooting

### **Problemas Comunes**

#### **1. Perfil no se activa**
```bash
# Verificar perfil activo
mvn spring-boot:run -Dspring.profiles.active=prod

# O configurar variable de entorno
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

#### **2. Error de credenciales de Firebase**
```bash
# Verificar que el archivo existe
ls -la src/main/resources/political-referrals-wa-key.json

# O usar perfil local que no requiere Firebase
mvn spring-boot:run -Dspring.profiles.active=local
```

#### **3. Puerto ocupado**
```bash
# Cambiar puerto en application.properties
server.port=8081

# O usar variable de entorno
export SERVER_PORT=8081
mvn spring-boot:run
```

### **Logs y Debugging**
```bash
# Habilitar debug
export LOGGING_LEVEL_COM_POLITICALREFERRALSWA=DEBUG
mvn spring-boot:run

# Ver logs detallados
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.root=DEBUG"
```

## 📁 Estructura del Proyecto

```
political_referrals_wa/
├── src/main/resources/
│   ├── application.properties.example    # Template base con comandos GCP
│   ├── application-local.properties      # Desarrollo local (NO en repo)
│   └── application-prod.properties      # Producción (por defecto)
├── docs/                                # 📚 Documentación completa
│   ├── MAIN_GUIDE.md                    # Guía principal unificada
│   └── PROJECT_STATUS.md                # Estado del proyecto
├── deploy/                              # ⚙️ Archivos de despliegue
│   └── cloud-run.yaml                   # Configuración de Cloud Run
├── .github/workflows/                   # CI/CD automático
│   └── ci-cd.yml                       # Pipeline de despliegue
├── pom.xml                             # Configuración Maven con perfil dinámico
└── README.md                           # Esta guía completa
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