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

### **3. `application.properties.example` - EJEMPLO**
- **Uso**: Documentación de configuración
- **Credenciales**: Placeholders sin valores reales
- **Repositorio**: ✅ SÍ se sube

## 🚀 Cómo Usar

### **Para Desarrollo Local:**
```bash
# Copiar archivo local
cp src/main/resources/application-local.properties src/main/resources/application.properties

# Ejecutar aplicación
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
