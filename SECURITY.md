# 🔒 Configuración de Seguridad

## 📋 Resumen

Este proyecto usa **GCP Secret Manager para TODAS las variables**, maximizando la seguridad y siguiendo las mejores prácticas empresariales.

## 🔐 Secretos en GCP Secret Manager

### Todas las Variables (100% seguras):
- `gcp-project-id` → ID del proyecto GCP
- `webhook-verify-token` → Token de verificación de webhook
- `telegram-bot-token` → Token del bot de Telegram
- `telegram-bot-username` → Username del bot
- `wati-tenant-id` → ID del tenant de Wati
- `wati-api-token` → Token de API de Wati
- `gemini-api-key` → Clave de API de Gemini AI
- `analytics-jwt-secret` → Secreto JWT para analytics
- `wati-notification-enabled` → Habilitar notificaciones
- `wati-notification-group-id` → ID del grupo de notificaciones
- `wati-notification-phones` → Teléfonos para notificaciones

## 🚀 Para Desarrolladores

```bash
git push origin main → ✅ Deploy automático
```

**No necesita configurar nada más.**

## 🔧 Configuración de Secretos (Solo Administradores)

### Crear Secretos:
```bash
echo "tu-valor" | gcloud secrets create nombre-del-secreto --data-file=- --project=intreasoft-daniel
```

### Listar Secretos:
```bash
gcloud secrets list --project=intreasoft-daniel
```

## 📁 Archivos

- `deploy/cloud-run.yaml` → Configuración de producción (solo referencias a secretos)
- `deploy/cloud-run.example.yaml` → Template de ejemplo
- `.gitignore` → Protege credenciales locales

## ⚠️ Importante

- **NO hay credenciales** en el repositorio
- **TODAS las variables** están en GCP Secret Manager
- **Máxima seguridad** para producción
- **Rotación automática** de secretos
- **Auditoría completa** de acceso

## 🏆 Beneficios

1. **Seguridad máxima** - nada expuesto en el repo
2. **Escalabilidad** - fácil agregar nuevas variables
3. **Compliance** - cumple estándares empresariales
4. **Mantenimiento** - centralizado en GCP
5. **Rotación** - automática de credenciales
