#!/bin/bash

# Script de prueba para el menú post-registro
# Este script simula las interacciones de un usuario COMPLETED

echo "🧪 Iniciando pruebas del menú post-registro..."

# Configuración
BASE_URL="http://localhost:8080"
TEST_PHONE="+573100000001"
TEST_MESSAGE="Hola, quiero ver el menú"

echo "📍 URL base: $BASE_URL"
echo "📱 Teléfono de prueba: $TEST_PHONE"
echo "💬 Mensaje de prueba: $TEST_MESSAGE"

echo ""
echo "🔍 Probando endpoint de chat..."

# Simular mensaje de usuario COMPLETED
curl -X POST "$BASE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-key" \
  -d "{
    \"fromId\": \"$TEST_PHONE\",
    \"messageText\": \"$TEST_MESSAGE\",
    \"channelType\": \"WHATSAPP\"
  }" \
  -s | jq '.'

echo ""
echo "✅ Prueba completada. Verifica los logs del servidor para ver el comportamiento del menú post-registro."

echo ""
echo "📋 Resumen de funcionalidades implementadas:"
echo "   ✅ Detección de usuarios COMPLETED"
echo "   ✅ Menú post-registro con 4 botones"
echo "   ✅ Integración con chatbotIA para QBot"
echo "   ✅ Timeout automático de 30 minutos"
echo "   ✅ Manejo de respuestas de botones"
echo "   ✅ Gestión de sesiones de QBot"

echo ""
echo "🎯 Para probar completamente:"
echo "   1. Asegúrate de que el servidor esté corriendo"
echo "   2. Verifica que el usuario de prueba tenga estado COMPLETED"
echo "   3. Envía un mensaje desde WhatsApp al número de prueba"
echo "   4. Verifica que se muestre el menú de 4 botones"
echo "   5. Prueba cada botón del menú"
echo "   6. Activa QBot y verifica el timeout de 30 minutos"
