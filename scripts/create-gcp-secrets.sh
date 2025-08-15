#!/bin/bash

# ================================================
# SCRIPT PARA CREAR SECRETOS EN GCP SECRET MANAGER
# ================================================
# Este script crea todos los secretos necesarios para el despliegue

echo "🔐 Creando secretos en GCP Secret Manager..."

# Configurar proyecto
PROJECT_ID="intreasoft-daniel"
echo "📋 Proyecto: $PROJECT_ID"

# Crear secretos
echo "✅ Creando secretos..."

# 1. Project ID
echo "webhook-verify-token" | gcloud secrets create webhook-verify-token --data-file=- --project=$PROJECT_ID

# 2. Telegram Bot Token
echo "7350149841:AAHsujWqzvh9azw2dMlwby6iZdlEkmisSv4" | gcloud secrets create telegram-bot-token --data-file=- --project=$PROJECT_ID

# 3. Telegram Bot Username
echo "ResetPoliticaBot" | gcloud secrets create telegram-bot-username --data-file=- --project=$PROJECT_ID

# 4. Wati Tenant ID
echo "473173" | gcloud secrets create wati-tenant-id --data-file=- --project=$PROJECT_ID

# 5. Wati API Token
echo "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI5ZWViNzJlZi01NmVmLTQzZjUtYjBmOC00NWFjZTVjZjBiZjgiLCJ1bmlxdWVfbmFtZSI6ImludHJhc29mdGRldm9wc0BnbWFpbC5jb20iLCJuYW1laWQiOiJpbnRyYXNvZnRkZXZvcHNAZ21haWwuY29tIiwiZW1haWwiOiJpbnRyYXNvZnRkZXZvcHNAZ21haWwuY29tIiwiYXV0aF90aW1lIjoiMDcvMjUvMjAyNSAyMzo0MzowOSIsInRlbmFudF9pZCI6IjQ3MzE3MyIsImRiX25hbWUiOiJtdC1wcm9kLVRlbmFudHMiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiOiJBRE1JTklTVFJBVE9SIiwiZXhwIjoyNTM0MDIzMDA4MDAsImlzcyI6IkNsYXJlX0FJIiwiYXVkIjoiQ2xhcmVfQUkifQ._mgHJCUNWWmdVueTQmoEAEtaIZS9uTkOwh28UffXFDg" | gcloud secrets create wati-api-token --data-file=- --project=$PROJECT_ID

# 6. Gemini API Key
echo "AIzaSyA73v4PVS8kaID6TWQcW-F31qPk2BiBNHo" | gcloud secrets create gemini-api-key --data-file=- --project=$PROJECT_ID

# 7. Analytics JWT Secret
echo "z4PiqjH5bJEUTcDLz4q//FX4MZXvrN7vQi+38KK5r1g=" | gcloud secrets create analytics-jwt-secret --data-file=- --project=$PROJECT_ID

# 8. Wati Notification Group ID
echo "" | gcloud secrets create wati-notification-group-id --data-file=- --project=$PROJECT_ID

# 9. Wati Notification Phones
echo "573227281752" | gcloud secrets create wati-notification-phones --data-file=- --project=$PROJECT_ID

echo "✅ Todos los secretos creados exitosamente!"
echo "📋 Para ver los secretos: gcloud secrets list --project=$PROJECT_ID"
