# 🔒 Configuración de Seguridad

## 📋 Resumen

Este proyecto usa **GCP Secret Manager** para credenciales sensibles y **variables hardcodeadas** para configuración general.

## 🔐 Secretos en GCP Secret Manager

### Credenciales Sensibles:
- `webhook-verify-token`
- `telegram-bot-token`
- `wati-api-token`
- `gemini-api-key`
- `analytics-jwt-secret`

### Variables de Configuración (cloud-run.yaml):
- `SPRING_CLOUD_GCP_PROJECT_ID`
- `TELEGRAM_BOT_USERNAME`
- `WATI_TENANT_ID`
- `WATI_NOTIFICATION_ENABLED`
- `WATI_NOTIFICATION_GROUP_ID`
- `WATI_NOTIFICATION_PHONES`

## 🚀 Para Desarrolladores

```bash
git push origin main → ✅ Deploy automático
```

**No necesita configurar nada más.**

## 🔧 Configuración de Secretos (Solo Administradores)

### Crear Secretos:
```bash
echo "tu-token" | gcloud secrets create webhook-verify-token --data-file=- --project=intreasoft-daniel
```

### Listar Secretos:
```bash
gcloud secrets list --project=intreasoft-daniel
```

## 📁 Archivos

- `deploy/cloud-run.yaml` → Configuración de producción
- `deploy/cloud-run.example.yaml` → Template de ejemplo
- `.gitignore` → Protege credenciales locales

## ⚠️ Importante

- **NO subir credenciales** al repositorio
- **NO hardcodear** tokens sensibles
- **Usar Secret Manager** para credenciales
- **Variables de entorno** para configuración general
