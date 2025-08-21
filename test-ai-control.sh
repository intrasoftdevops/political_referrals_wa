#!/bin/bash

# Script para probar los endpoints de control de IA del sistema
# Uso: ./test-ai-control.sh

BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api/system"

echo "🤖 ========================================"
echo "🤖 PRUEBA DE CONTROL DE IA DEL SISTEMA"
echo "🤖 ========================================"
echo ""

# Función para hacer requests HTTP
make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$endpoint")
    else
        if [ -n "$data" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$data" "$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$endpoint")
        fi
    fi
    
    # Separar respuesta y código HTTP
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    echo "📡 $method $endpoint"
    echo "📊 Código HTTP: $http_code"
    echo "📄 Respuesta:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
}

# Función para esperar a que el servidor esté listo
wait_for_server() {
    echo "⏳ Esperando a que el servidor esté listo..."
    while ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; do
        sleep 2
        echo "   Servidor no está listo aún..."
    done
    echo "✅ Servidor está listo!"
    echo ""
}

# Verificar si jq está instalado
if ! command -v jq &> /dev/null; then
    echo "⚠️  jq no está instalado. Instalando..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install jq
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        sudo apt-get update && sudo apt-get install -y jq
    else
        echo "❌ No se pudo instalar jq automáticamente. Por favor instálalo manualmente."
        exit 1
    fi
fi

# Esperar a que el servidor esté listo
wait_for_server

echo "🚀 INICIANDO PRUEBAS..."
echo ""

# 1. Obtener estado inicial
echo "1️⃣  OBTENIENDO ESTADO INICIAL DE LA IA"
make_request "GET" "$API_BASE/ai/status"

# 2. Deshabilitar IA
echo "2️⃣  DESHABILITANDO LA IA"
make_request "POST" "$API_BASE/ai/disable"

# 3. Verificar estado después de deshabilitar
echo "3️⃣  VERIFICANDO ESTADO DESPUÉS DE DESHABILITAR"
make_request "GET" "$API_BASE/ai/status"

# 4. Cambiar estado (toggle)
echo "4️⃣  CAMBIANDO ESTADO (TOGGLE)"
make_request "POST" "$API_BASE/ai/toggle"

# 5. Verificar estado después del toggle
echo "5️⃣  VERIFICANDO ESTADO DESPUÉS DEL TOGGLE"
make_request "GET" "$API_BASE/ai/status"

# 6. Establecer estado específico
echo "6️⃣  ESTABLECIENDO ESTADO ESPECÍFICO (false)"
make_request "POST" "$API_BASE/ai/set" '{"enabled": false}'

# 7. Verificar estado final
echo "7️⃣  VERIFICANDO ESTADO FINAL"
make_request "GET" "$API_BASE/ai/status"

# 8. Habilitar IA nuevamente
echo "8️⃣  HABILITANDO LA IA NUEVAMENTE"
make_request "POST" "$API_BASE/ai/enable"

# 9. Verificar estado final
echo "9️⃣  VERIFICANDO ESTADO FINAL"
make_request "GET" "$API_BASE/ai/status"

echo "🎉 ========================================"
echo "🎉 PRUEBAS COMPLETADAS"
echo "🎉 ========================================"
echo ""
echo "📋 RESUMEN DE ENDPOINTS PROBADOS:"
echo "   ✅ GET  /api/system/ai/status"
echo "   ✅ POST /api/system/ai/disable"
echo "   ✅ POST /api/system/ai/toggle"
echo "   ✅ POST /api/system/ai/set"
echo "   ✅ POST /api/system/ai/enable"
echo ""
echo "🔗 Swagger UI disponible en: $BASE_URL/swagger-ui.html"
echo "📚 Documentación completa en: README-IA-CONTROL.md"
