# ⚙️ Guía de Configuración - Political Referrals WA

## 📁 Estructura de Archivos de Configuración

### **1. `application.properties` - PRODUCCIÓN**
- **Uso**: Configuración por defecto para Cloud Run
- **Credenciales**: Variables de entorno desde Google Cloud Secrets
- **Repositorio**: ✅ SÍ se sube (sin credenciales reales)

### **2. `application-local.properties` - DESARROLLO LOCAL**
- **Uso**: Configuración para desarrollo local
- **Credenciales**: Credenciales reales hardcodeadas
- **Repositorio**: ❌ NO se sube (excluido en .gitignore)

### **3. `application.properties.example` - EJEMPLO PRODUCCIÓN**
- **Uso**: Documentación de configuración de producción
- **Credenciales**: Placeholders sin valores reales
- **Repositorio**: ✅ SÍ se sube

### **4. `application-local.properties.example` - EJEMPLO DESARROLLO LOCAL**
- **Uso**: Documentación de configuración para desarrollo local
- **Credenciales**: Placeholders sin valores reales
- **Repositorio**: ✅ SÍ se sube

## 🚀 Cómo Usar

### **Para Desarrollo Local:**
```bash
# 1. Copiar archivo de ejemplo local
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties

# 2. Editar con tus credenciales reales
# (editar application-local.properties)

# 3. Copiar para desarrollo
cp src/main/resources/application-local.properties src/main/resources/application.properties

# 4. Ejecutar aplicación
mvn spring-boot:run
```

### **Para Producción (Cloud Run):**
- El archivo `application.properties` se usa por defecto
- Las credenciales se inyectan via variables de entorno
- No se necesita `SPRING_PROFILES_ACTIVE`

### **Para CI/CD:**
- GitHub Actions construye usando `application.properties`
- Las credenciales se inyectan en Cloud Run

## 🔐 Configuración de Credenciales

### **En Google Cloud Secrets:**
```bash
# Ejecutar script de configuración
./scripts/setup-gcp-secrets.sh
```

### **Scripts Disponibles:**
- **`setup-gcp-secrets.sh`** - Configura Google Cloud Secrets para producción

### **Variables de Entorno Configuradas:**
- `GEMINI_API_KEY`
- `TELEGRAM_BOT_TOKEN`
- `WATI_API_TOKEN`
- `WEBHOOK_VERIFY_TOKEN`
- `ANALYTICS_JWT_SECRET`
- Y más...

## 📝 Notas Importantes

- **Nunca** subir `application-local.properties` al repositorio
- **Siempre** usar variables de entorno en producción
- **Verificar** que `.gitignore` excluya archivos con credenciales
- **Documentar** cambios en `application.properties.example`
