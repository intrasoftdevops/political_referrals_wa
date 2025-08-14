#!/bin/bash

# Script de despliegue local para Political Referrals WA
# Uso: ./scripts/deploy-local.sh [environment]

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

# Configuración por defecto
ENVIRONMENT=${1:-local}
PROFILE=${ENVIRONMENT}

log "🚀 Iniciando despliegue local para entorno: $ENVIRONMENT"

# Verificar dependencias
log "📋 Verificando dependencias..."
if ! command -v java &> /dev/null; then
    error "Java no está instalado o no está en el PATH"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    error "Maven no está instalado o no está en el PATH"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    error "Se requiere Java 21 o superior. Versión actual: $JAVA_VERSION"
    exit 1
fi

success "✅ Dependencias verificadas"

# Limpiar y compilar
log "🔨 Compilando proyecto..."
mvn clean compile -q
success "✅ Compilación exitosa"

# Ejecutar tests
log "🧪 Ejecutando tests..."
if mvn test -q; then
    success "✅ Tests exitosos"
else
    warning "⚠️  Algunos tests fallaron, continuando..."
fi

# Construir JAR
log "📦 Construyendo JAR..."
mvn package -DskipTests -q
success "✅ JAR construido exitosamente"

# Verificar que el JAR existe
JAR_FILE="target/political_referrals_wa-0.0.1-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    error "No se pudo encontrar el archivo JAR: $JAR_FILE"
    exit 1
fi

# Verificar configuración
log "⚙️  Verificando configuración..."
if [ ! -f "src/main/resources/application.properties" ]; then
    warning "⚠️  No se encontró application.properties, usando application.properties.example"
    if [ -f "src/main/resources/application.properties.example" ]; then
        cp src/main/resources/application.properties.example src/main/resources/application.properties
        warning "⚠️  Por favor, configura las variables en application.properties antes de continuar"
        read -p "¿Deseas continuar? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log "Despliegue cancelado"
            exit 0
        fi
    else
        error "No se encontró application.properties.example"
        exit 1
    fi
fi

# Verificar credenciales de Firebase
if [ ! -f "src/main/resources/political-referrals-wa-key.json" ]; then
    warning "⚠️  No se encontró el archivo de credenciales de Firebase"
    warning "⚠️  La aplicación puede fallar al conectarse a Firestore"
fi

# Iniciar aplicación
log "🚀 Iniciando aplicación..."
log "📱 Perfil activo: $PROFILE"
log "🌐 Puerto: 8080"
log "📁 JAR: $JAR_FILE"

# Configurar variables de entorno
export SPRING_PROFILES_ACTIVE=$PROFILE
export SERVER_PORT=8080

# Ejecutar aplicación
log "🎯 Ejecutando aplicación..."
java -jar "$JAR_FILE" &
APP_PID=$!

# Esperar a que la aplicación esté lista
log "⏳ Esperando a que la aplicación esté lista..."
sleep 10

# Verificar que la aplicación esté ejecutándose
if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    success "✅ Aplicación iniciada exitosamente"
    log "🌐 URL: http://localhost:8080"
    log "📊 Health Check: http://localhost:8080/actuator/health"
    log "📚 API Docs: http://localhost:8080/swagger-ui.html"
    
    # Mostrar logs
    log "📝 Logs de la aplicación (Ctrl+C para detener):"
    tail -f logs/spring.log 2>/dev/null || echo "No se encontraron logs específicos"
    
else
    error "❌ La aplicación no responde en http://localhost:8080"
    kill $APP_PID 2>/dev/null || true
    exit 1
fi

# Función de limpieza al salir
cleanup() {
    log "🛑 Deteniendo aplicación..."
    kill $APP_PID 2>/dev/null || true
    success "✅ Aplicación detenida"
    exit 0
}

# Capturar señales de interrupción
trap cleanup SIGINT SIGTERM

# Mantener el script ejecutándose
wait $APP_PID 