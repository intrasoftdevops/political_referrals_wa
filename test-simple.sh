#!/bin/bash

# Script simple para probar el control de IA solo con endpoints
# Uso: ./test-simple.sh

BASE_URL="http://localhost:8080"
API_BASE="$BASE_URL/api/system"

echo "🤖 CONTROL DE IA DEL SISTEMA - SOLO ENDPOINTS"
echo "=============================================="
echo ""

# Función para hacer requests
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    echo "📡 $method $endpoint"
    
    if [ "$method" = "GET" ]; then
        curl -s "$endpoint" | jq '.' 2>/dev/null || curl -s "$endpoint"
    else
        if [ -n "$data" ]; then
            curl -s -X "$method" -H "Content-Type: application/json" -d "$data" "$endpoint" | jq '.' 2>/dev/null || curl -s -X "$method" -H "Content-Type: application/json" -d "$data" "$endpoint"
        else
            curl -s -X "$method" "$endpoint" | jq '.' 2>/dev/null || curl -s -X "$method" "$endpoint"
        fi
    fi
    echo ""
}

# Esperar a que el servidor esté listo
echo "⏳ Esperando servidor..."
while ! curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; do
    sleep 2
done
echo "✅ Servidor listo!"
echo ""

# Pruebas
echo "1️⃣  Estado inicial:"
test_endpoint "GET" "$API_BASE/ai/status"

echo "2️⃣  Deshabilitar IA:"
test_endpoint "POST" "$API_BASE/ai/disable"

echo "3️⃣  Verificar estado:"
test_endpoint "GET" "$API_BASE/ai/status"

echo "4️⃣  Habilitar IA:"
test_endpoint "POST" "$API_BASE/ai/enable"

echo "5️⃣  Estado final:"
test_endpoint "GET" "$API_BASE/ai/status"

echo "🎉 Pruebas completadas!"
echo "🔗 Swagger: $BASE_URL/swagger-ui.html"
