#!/bin/bash

# Script de pruebas para verificar el manejo de correcciones naturales
# en el sistema de inputs inteligentes con Gemini AI

echo "🧪 INICIANDO PRUEBAS DE CORRECCIONES NATURALES"
echo "=============================================="

BASE_URL="http://localhost:8081"
PHONE="+573001234567"

# Función para hacer peticiones POST
make_request() {
    local message="$1"
    local test_name="$2"
    
    echo "📝 Prueba: $test_name"
    echo "Mensaje: $message"
    
    response=$(curl -s -X POST "$BASE_URL/api/wati-webhook" \
        -H "Content-Type: application/json" \
        -d "{
            \"eventType\": \"message\",
            \"type\": \"text\",
            \"waId\": \"$PHONE\",
            \"senderName\": \"Miguel\",
            \"text\": \"$message\"
        }")
    
    echo "Respuesta: $response"
    echo "---"
}

# Función para consultar métricas
check_metrics() {
    echo "📊 Consultando métricas..."
    metrics=$(curl -s "$BASE_URL/api/metrics/gemini")
    echo "Métricas actuales: $metrics"
    echo "---"
}

echo "🔄 PRUEBA 1: Corrección de nombre"
make_request "Perdón, mi nombre es Carlos no Juan" "Corrección de nombre"

echo "🔄 PRUEBA 2: Corrección de ciudad"
make_request "Me equivoqué, no soy de Medellín sino de Envigado" "Corrección de ciudad"

echo "🔄 PRUEBA 3: Corrección de ciudad con ambigüedad"
make_request "Es Barbosa no Armenia" "Corrección de ciudad con ambigüedad"

echo "🔄 PRUEBA 4: Corrección múltiple"
make_request "Me equivoqué en todo, soy Carlos Pérez de Bucaramanga no Juan García de Medellín" "Corrección múltiple"

echo "🔄 PRUEBA 5: Corrección con contexto"
make_request "Perdón, es Bogotá no Cali" "Corrección con contexto"

echo "🔄 PRUEBA 6: Corrección de términos"
make_request "Sí acepto los términos, antes dije que no pero me equivoqué" "Corrección de términos"

echo "🔄 PRUEBA 7: Corrección de referido"
make_request "Mi referido es +573001234567 no +573009876543" "Corrección de referido"

echo "🔄 PRUEBA 8: Corrección de código de referido"
make_request "Mi código es ABC12345 no XYZ98765" "Corrección de código"

echo "🔄 PRUEBA 9: Corrección con disculpas"
make_request "Disculpa, me equivoqué, soy de Pereira no de Manizales" "Corrección con disculpas"

echo "🔄 PRUEBA 10: Corrección de departamento"
make_request "Soy de Antioquia no de Caldas" "Corrección de departamento"

echo "📊 CONSULTANDO MÉTRICAS FINALES"
check_metrics

echo "✅ PRUEBAS DE CORRECCIONES COMPLETADAS"
echo "====================================="
echo ""
echo "📋 RESUMEN DE PRUEBAS:"
echo "- 10 pruebas de correcciones ejecutadas"
echo "- Verificar en logs que se detecten las correcciones"
echo "- Verificar que los mensajes incluyan confirmación de cambios"
echo "- Verificar que las métricas se actualicen correctamente"
echo ""
echo "🔍 PARA VERIFICAR MANUALMENTE:"
echo "1. Revisar logs de la aplicación"
echo "2. Verificar que aparezca: 'Corrección detectada'"
echo "3. Verificar que los mensajes incluyan: 'actualicé tu...'"
echo "4. Consultar métricas: curl $BASE_URL/api/metrics/gemini" 