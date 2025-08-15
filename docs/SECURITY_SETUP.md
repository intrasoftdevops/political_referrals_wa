# 🔒 Configuración de Seguridad - Political Referrals WA

## 📋 Resumen

Este documento describe cómo configurar de forma segura las credenciales y variables de entorno para el proyecto Political Referrals WA, utilizando Google Cloud Secrets en lugar de credenciales hardcodeadas en el repositorio.

## 🚨 Problema de Seguridad Identificado

**ANTES**: Las credenciales estaban hardcodeadas en archivos de propiedades, exponiendo información sensible en el repositorio público.

**DESPUÉS**: Todas las credenciales se manejan a través de Google Cloud Secrets y variables de entorno.

## 🔐 Configuración de Google Cloud Secrets

### 1. Ejecutar el script de configuración

```bash
# Desde el directorio raíz del proyecto
chmod +x scripts/setup-gcp-secrets.sh
./scripts/setup-gcp-secrets.sh
```

### 2. Secrets configurados automáticamente

El script configurará los siguientes secrets en Google Cloud:

- `gcp-project-id` - ID del proyecto de Google Cloud
- `gemini-api-key` - Clave de API de Gemini AI
- `telegram-bot-token` - Token del bot de Telegram
- `telegram-bot-username` - Username del bot de Telegram
- `wati-api-token` - Token de la API de Wati
- `wati-tenant-id` - ID del tenant de Wati
- `webhook-verify-token` - Token de verificación de webhooks
- `analytics-jwt-secret` - Secreto JWT para analytics

## 🌍 Variables de Entorno en Cloud Run

### Configuración actualizada en `deploy/cloud-run.yaml`

```yaml
env:
- name: SPRING_PROFILES_ACTIVE
  value: "prod"
- name: PORT
  value: "8080"
- name: SPRING_CLOUD_GCP_PROJECT_ID
  valueFrom:
    secretKeyRef:
      name: political-referrals-wa-secrets
      key: gcp-project-id
- name: GEMINI_API_KEY
  valueFrom:
    secretKeyRef:
      name: political-referrals-wa-secrets
      key: gemini-api-key
# ... más variables de entorno
```

## 📁 Archivos de Configuración

### Archivos que NO se suben al repositorio (excluidos en .gitignore)

- `src/main/resources/application.properties` - Configuración principal con credenciales
- `src/main/resources/application-prod.properties` - Configuración de producción con credenciales
- `src/main/resources/application-notifications.properties` - Configuración de notificaciones

### Archivos que SÍ se suben al repositorio (ejemplos)

- `src/main/resources/application.properties.example` - Ejemplo de configuración principal
- `src/main/resources/application-prod.properties.example` - Ejemplo de configuración de producción
- `src/main/resources/application-notifications.properties.example` - Ejemplo de configuración de notificaciones

## 🔄 Flujo de Despliegue Seguro

1. **Desarrollo Local**: Usar archivos `.local` con credenciales de desarrollo
2. **CI/CD**: El pipeline construye la imagen usando archivos de ejemplo
3. **Cloud Run**: Las credenciales se inyectan via variables de entorno desde Google Cloud Secrets
4. **Aplicación**: Spring Boot usa las variables de entorno para configurar las credenciales

## 🛡️ Beneficios de Seguridad

- ✅ **Sin credenciales en el repositorio**: No hay riesgo de exposición pública
- ✅ **Rotación de credenciales**: Fácil actualización sin cambios en el código
- ✅ **Auditoría**: Google Cloud registra acceso a los secrets
- ✅ **Separación de entornos**: Diferentes credenciales para desarrollo, staging y producción
- ✅ **Cumplimiento**: Mejores prácticas de seguridad para aplicaciones en la nube

## 🚀 Comandos Útiles

### Ver secrets configurados
```bash
gcloud secrets list --project=intreasoft-daniel
```

### Ver versiones de un secret
```bash
gcloud secrets versions list political-referrals-wa-secrets --project=intreasoft-daniel
```

### Actualizar un secret
```bash
echo -n "nuevo_valor" | gcloud secrets versions add political-referrals-wa-secrets --data-file=-
```

### Ver en la consola web
```
https://console.cloud.google.com/security/secret-manager?project=intreasoft-daniel
```

## 📝 Notas Importantes

- **Nunca** subir archivos con credenciales reales al repositorio
- **Siempre** usar archivos `.example` para documentar la configuración
- **Verificar** que `.gitignore` excluya archivos con credenciales
- **Revisar** regularmente el acceso a los secrets en Google Cloud
- **Rotar** las credenciales periódicamente por seguridad

## 🔗 Enlaces Útiles

- [Google Cloud Secret Manager](https://cloud.google.com/secret-manager)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Cloud Run Environment Variables](https://cloud.google.com/run/docs/configuring/environment-variables)
