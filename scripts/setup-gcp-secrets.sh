#!/bin/bash

# Script para configurar Google Cloud Secrets para Political Referrals WA
# Uso: ./scripts/setup-gcp-secrets.sh

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para logging
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Verificar que estamos en el directorio correcto
if [ ! -f "pom.xml" ]; then
    error "Este script debe ejecutarse desde el directorio raíz del proyecto"
    exit 1
fi

# Obtener el proyecto actual
PROJECT_ID=$(gcloud config get-value project)
if [ -z "$PROJECT_ID" ]; then
    error "No se pudo obtener el PROJECT_ID. Ejecuta 'gcloud config set project PROJECT_ID' primero"
    exit 1
fi

log "🚀 Configurando Google Cloud Secrets para el proyecto: $PROJECT_ID"

# Nombre del secret manager
SECRET_NAME="political-referrals-wa-secrets"

# Verificar si el secret manager ya existe
if gcloud secrets describe "$SECRET_NAME" --project="$PROJECT_ID" >/dev/null 2>&1; then
    warning "El secret manager '$SECRET_NAME' ya existe. Se actualizarán las versiones existentes."
else
    log "📝 Creando secret manager: $SECRET_NAME"
    gcloud secrets create "$SECRET_NAME" --project="$PROJECT_ID" --replication-policy="automatic"
    success "✅ Secret manager creado exitosamente"
fi

# Función para crear/actualizar un secret
create_secret() {
    local key=$1
    local value=$2
    local description=$3
    
    log "🔐 Configurando secret: $key"
    
    # Verificar si el secret ya existe
    if gcloud secrets versions list "$SECRET_NAME" --project="$PROJECT_ID" --filter="labels.secret=$key" --limit=1 | grep -q "$key"; then
        log "📝 Actualizando versión existente del secret: $key"
    else
        log "📝 Creando nueva versión del secret: $key"
    fi
    
    # Crear/actualizar el secret
    echo -n "$value" | gcloud secrets versions add "$SECRET_NAME" --project="$PROJECT_ID" --data-file=- --labels="secret=$key" --description="$description"
    
    if [ $? -eq 0 ]; then
        success "✅ Secret '$key' configurado exitosamente"
    else
        error "❌ Error al configurar secret '$key'"
        return 1
    fi
}

# Configurar todos los secrets necesarios
log "🔐 Configurando secrets..."

# Project ID
create_secret "gcp-project-id" "$PROJECT_ID" "Google Cloud Project ID"

# Gemini AI
create_secret "gemini-api-key" "AIzaSyA73v4PVS8kaID6TWQcW-F31qPk2BiBNHo" "Gemini AI API Key"

# Telegram Bot
create_secret "telegram-bot-token" "7350149841:AAHsujWqzvh9azw2dMlwby6iZdlEkmisSv4" "Telegram Bot Token"

# Wati API
create_secret "wati-api-token" "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiI5ZWViNzJlZi01NmVmLTQzZjUtYjBmOC00NWFjZTVjZjBiZjgiLCJ1bmlxdWVfbmFtZSI6ImludHJhc29mdGRldm9wc0BnbWFpbC5jb20iLCJuYW1laWQiOiJpbnRyYXNvZnRkZXZvcHNAZ21haWwuY29tIiwiZW1haWwiOiJpbnRyYXNvZnRkZXZvcHNAZ21haWwuY29tIiwiYXV0aF90aW1lIjoiMDcvMjUvMjAyNSAyMzo0MzowOSIsInRlbmFudF9pZCI6IjQ3MzE3MyIsImRiX25hbWUiOiJtdC1wcm9kLVRlbmFudHMiLCJodHRwOi8vc2NoZW1hcy5taWNyb3NvZnQuY29tL3dzLzIwMDgvMDYvaWRlbnRpdHkvY2xhaW1zL3JvbGUiOiJBRE1JTklTVFJBVE9SIiwiZXhwIjoyNTM0MDIzMDA4MDAsImlzcyI6IkNsYXJlX0FJIiwiYXVkIjoiQ2xhcmVfQUkifQ._mgHJCUNWWmdVueTQmoEAEtaIZS9uTkOwh28UffXFDg" "Wati API Token"

# Webhook Verify Token
create_secret "webhook-verify-token" "qaRZjTs8tFTfo5pLt6JwyFtGFPtuxLF6JBYqu0YcXsQ" "Webhook Verify Token"

# Analytics JWT Secret
create_secret "analytics-jwt-secret" "z4PiqjH5bJEUTcDLz4q//FX4MZXvrN7vQi+38KK5r1g=" "Analytics JWT Secret"

# Wati Tenant ID
create_secret "wati-tenant-id" "473173" "Wati Tenant ID"

# Telegram Bot Username
create_secret "telegram-bot-username" "ResetPoliticaBot" "Telegram Bot Username"

success "🎉 Todos los secrets han sido configurados exitosamente!"

log "📋 Resumen de secrets configurados:"
gcloud secrets versions list "$SECRET_NAME" --project="$PROJECT_ID" --format="table(name,labels.secret,createTime,state)"

log "🔗 Para ver los secrets en la consola de Google Cloud:"
echo "https://console.cloud.google.com/security/secret-manager?project=$PROJECT_ID"

log "📝 Para usar estos secrets en Cloud Run, asegúrate de que tu archivo cloud-run.yaml los referencie correctamente" 