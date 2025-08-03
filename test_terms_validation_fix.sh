#!/bin/bash

# Script para probar que la validación de términos de privacidad funciona correctamente

echo "=== PRUEBA DE VALIDACIÓN DE TÉRMINOS DE PRIVACIDAD ==="

# URL del webhook
WEBHOOK_URL="http://localhost:8081/api/wati-webhook"

# Simular flujo completo para verificar que SIEMPRE se valide la política de privacidad
echo "1. Primer mensaje - Usuario nuevo..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281753",
    "text": "Hola",
    "senderName": "María"
  }'

echo -e "\n\n2. Segundo mensaje - Nombre..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281753",
    "text": "María",
    "senderName": "María"
  }'

echo -e "\n\n3. Tercer mensaje - Ciudad..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281753",
    "text": "Medellín",
    "senderName": "María"
  }'

echo -e "\n\n4. Cuarto mensaje - Confirmación de datos..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281753",
    "text": "Sí",
    "senderName": "María"
  }'

echo -e "\n\n5. Quinto mensaje - Debería pedir términos de privacidad..."
curl -X POST "$WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "message",
    "type": "text",
    "waId": "573227281753",
    "text": "Sí",
    "senderName": "María"
  }'

echo -e "\n\n=== PRUEBA COMPLETADA ==="
echo "Verifica en los logs que:"
echo "1. El usuario pasó por todos los estados correctamente"
echo "2. Se validó la política de privacidad antes de completar el registro"
echo "3. No se envió al bot de IA sin validar términos" 