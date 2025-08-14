#!/bin/bash

# Script para configurar secrets en Google Cloud para Political Referrals WA
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

# Verificar que gcloud esté instalado
if ! command -v gcloud &> /dev/null; then
    error "Google Cloud CLI no está instalado"
    error "Instala desde: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Verificar autenticación
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" | grep -q .; then
    error "No estás autenticado en Google Cloud"
    log "Ejecuta: gcloud auth login"
    exit 1
fi

# Obtener proyecto actual
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)
if [ -z "$PROJECT_ID" ]; then
    error "No hay un proyecto configurado"
    log "Ejecuta: gcloud config set project PROJECT_ID"
    exit 1
fi

log "🔐 Configurando secrets para proyecto: $PROJECT_ID"

# Verificar que la API de Secret Manager esté habilitada
log "📋 Verificando API de Secret Manager..."
if ! gcloud services list --enabled --filter="name:secretmanager.googleapis.com" | grep -q secretmanager; then
    log "🚀 Habilitando API de Secret Manager..."
    gcloud services enable secretmanager.googleapis.com
    success "✅ API habilitada"
else
    success "✅ API ya habilitada"
fi

# Crear secrets
SECRETS=(
    "gcp-project-id:$PROJECT_ID"
    "gemini-api-key:"
    "telegram-bot-token:"
    "wati-api-token:"
    "webhook-verify-token:"
    "firebase-credentials:"
)

for secret in "${SECRETS[@]}"; do
    SECRET_NAME=$(echo $secret | cut -d: -f1)
    SECRET_VALUE=$(echo $secret | cut -d: -f2)
    
    log "🔑 Configurando secret: $SECRET_NAME"
    
    # Verificar si el secret ya existe
    if gcloud secrets describe "$SECRET_NAME" --project="$PROJECT_ID" >/dev/null 2>&1; then
        warning "⚠️  El secret '$SECRET_NAME' ya existe"
        read -p "¿Deseas actualizarlo? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            log "🔄 Actualizando secret..."
            if [ -n "$SECRET_VALUE" ]; then
                echo "$SECRET_VALUE" | gcloud secrets versions add "$SECRET_NAME" --data-file=-
            else
                echo "Por favor, ingresa el valor para $SECRET_NAME:"
                read -s SECRET_INPUT
                echo "$SECRET_INPUT" | gcloud secrets versions add "$SECRET_NAME" --data-file=-
            fi
            success "✅ Secret actualizado"
        else
            log "⏭️  Saltando actualización"
        fi
    else
        log "🆕 Creando nuevo secret..."
        if [ -n "$SECRET_VALUE" ]; then
            echo "$SECRET_VALUE" | gcloud secrets create "$SECRET_NAME" --data-file=-
        else
            echo "Por favor, ingresa el valor para $SECRET_NAME:"
            read -s SECRET_INPUT
            echo "$SECRET_INPUT" | gcloud secrets create "$SECRET_NAME" --data-file=-
        fi
        success "✅ Secret creado"
    fi
done

# Crear archivo de configuración para Cloud Run
log "📝 Creando archivo de configuración para Cloud Run..."

# Crear directorio deploy si no existe
mkdir -p deploy

# Actualizar el archivo cloud-run.yaml con el PROJECT_ID real
sed "s/PROJECT_ID/$PROJECT_ID/g" deploy/cloud-run.yaml > deploy/cloud-run-configured.yaml

success "✅ Archivo de configuración creado: deploy/cloud-run-configured.yaml"

# Mostrar resumen
log "📊 Resumen de configuración:"
echo "Proyecto: $PROJECT_ID"
echo "Secrets configurados:"
for secret in "${SECRETS[@]}"; do
    SECRET_NAME=$(echo $secret | cut -d: -f1)
    echo "  - $SECRET_NAME"
done

# Mostrar comandos útiles
log "🚀 Comandos útiles:"
echo "Ver secrets: gcloud secrets list"
echo "Ver versión de un secret: gcloud secrets versions list SECRET_NAME"
echo "Desplegar a Cloud Run: gcloud run services replace deploy/cloud-run-configured.yaml"

success "✅ Configuración de secrets completada" 